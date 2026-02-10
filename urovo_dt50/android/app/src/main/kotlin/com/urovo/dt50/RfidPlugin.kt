package com.urovo.dt50

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import com.urovo.rfid.RfidServiceManager
import com.urovo.rfid.RfidManagerWrapper
import com.urovo.rfid.aidl.IRfidCallback
import com.urovo.rfid.aidl.RfidDate

class RfidPlugin(private val context: Context) : RfidServiceManager.StatusListener {
    
    companion object {
        private const val TAG = "RfidPlugin"
    }
    
    private var methodChannel: MethodChannel? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var rfidManager: RfidManagerWrapper? = null
    private var readId: Byte = 0
    private var isConnected = false
    private var isScanning = false
    private var lastInventoryStartMs: Long = 0
    private var pendingInventory: Boolean = false
    private var pendingInventoryState: Byte = 0
    private var lastTagSeenMs: Long = 0
    private var lastConfigChangeMs: Long = 0
    private var tuningIndex: Int = 0

    private enum class PendingOpType { READ, WRITE }

    private data class PendingOp(
        val type: PendingOpType,
        val result: MethodChannel.Result,
        val wasScanning: Boolean,
        val startedMs: Long
    )

    private var pendingOp: PendingOp? = null
    private var writeCompletionArmed: Boolean = false
    private val opTimeoutRunnable = Runnable {
        val op = pendingOp ?: return@Runnable
        pendingOp = null
        writeCompletionArmed = false
        try {
            rfidManager?.cancelAccessEpcMatch(readId)
        } catch (e: Exception) {
        }
        if (op.wasScanning) {
            isScanning = true
            startInventory(0)
            mainHandler.removeCallbacks(tuneRunnable)
            mainHandler.postDelayed(tuneRunnable, 1500)
        }
        op.result.success(null)
    }

    private fun normalizeHex(input: String?): String {
        return (input ?: "").replace(" ", "").trim()
    }

    private fun hexToBytesOrNull(hexIn: String?): ByteArray? {
        var hex = normalizeHex(hexIn)
        if (hex.isEmpty()) return ByteArray(0)
        if (hex.length % 2 != 0) hex += "0"
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val idx = i * 2
            val b = hex.substring(idx, idx + 2).toIntOrNull(16) ?: return null
            out[i] = b.toByte()
        }
        return out
    }

    private fun passwordBytes(passwordHex: String?): ByteArray? {
        val raw = hexToBytesOrNull(passwordHex) ?: return null
        val out = ByteArray(4)
        for (i in 0 until 4) {
            out[i] = if (i < raw.size) raw[i] else 0
        }
        return out
    }

    private fun beginOp(type: PendingOpType, result: MethodChannel.Result): PendingOp? {
        if (!isConnected || rfidManager == null) {
            result.success(null)
            return null
        }
        if (pendingOp != null) {
            result.error("BUSY", "Another RFID operation is in progress", null)
            return null
        }
        writeCompletionArmed = false
        val wasScanning = isScanning
        isScanning = false
        mainHandler.removeCallbacks(tuneRunnable)
        val op = PendingOp(type, result, wasScanning, SystemClock.elapsedRealtime())
        pendingOp = op
        mainHandler.removeCallbacks(opTimeoutRunnable)
        mainHandler.postDelayed(opTimeoutRunnable, 4000)
        return op
    }

    private fun endOp() {
        val op = pendingOp
        pendingOp = null
        writeCompletionArmed = false
        mainHandler.removeCallbacks(opTimeoutRunnable)
        try {
            rfidManager?.cancelAccessEpcMatch(readId)
        } catch (e: Exception) {
        }
        if (op?.wasScanning == true) {
            isScanning = true
            startInventory(0)
            mainHandler.removeCallbacks(tuneRunnable)
            mainHandler.postDelayed(tuneRunnable, 1500)
        }
    }

    private data class FreqConfig(val region: Byte, val start: Byte, val end: Byte)

    private val powerCandidates = byteArrayOf(30.toByte(), 33.toByte())
    private val antennaCandidates = byteArrayOf(0.toByte(), 1.toByte())
    private val triggerCandidates = booleanArrayOf(false, true)
    private val freqCandidates = listOf(
        FreqConfig(0.toByte(), 0.toByte(), 6.toByte()),
        FreqConfig(1.toByte(), 0.toByte(), 10.toByte()),
        FreqConfig(2.toByte(), 0.toByte(), 6.toByte()),
        FreqConfig(3.toByte(), 0.toByte(), 52.toByte())
    )

    private fun configCount(): Int {
        return powerCandidates.size * antennaCandidates.size * triggerCandidates.size * freqCandidates.size
    }

    private fun applyConfig(index: Int) {
        if (!isConnected || rfidManager == null) return
        val pSize = powerCandidates.size
        val aSize = antennaCandidates.size
        val tSize = triggerCandidates.size
        val fSize = freqCandidates.size

        val power = powerCandidates[index % pSize]
        val ant = antennaCandidates[(index / pSize) % aSize]
        val trig = triggerCandidates[(index / (pSize * aSize)) % tSize]
        val freq = freqCandidates[(index / (pSize * aSize * tSize)) % fSize]

        val powerRet = rfidManager?.setOutputPower(readId, power) ?: -1
        val triggerRet = rfidManager?.setTrigger(trig) ?: -1
        val antRet = rfidManager?.setWorkAntenna(readId, ant) ?: -1
        val freqRet = rfidManager?.setFrequencyRegion(readId, freq.region, freq.start, freq.end) ?: -1

        Log.d(
            TAG,
            "applyConfig idx=$index power=$power ant=$ant trig=$trig freqRegion=${freq.region} start=${freq.start} end=${freq.end} retP=$powerRet retA=$antRet retT=$triggerRet retF=$freqRet"
        )
        lastConfigChangeMs = SystemClock.elapsedRealtime()
    }

    private val tuneRunnable = object : Runnable {
        override fun run() {
            if (!isScanning || !isConnected) return

            val now = SystemClock.elapsedRealtime()
            val noTagMs = now - lastTagSeenMs
            val sinceCfgMs = now - lastConfigChangeMs
            if (noTagMs > 4000 && sinceCfgMs > 4000) {
                val cnt = configCount()
                tuningIndex = (tuningIndex + 1) % cnt
                applyConfig(tuningIndex)
                startInventory(0)
            }
            mainHandler.postDelayed(this, 1500)
        }
    }

    private fun looksLikeHexBytes(value: String): Boolean {
        val v = value.trim()
        if (v.isEmpty()) return false
        val spaced = Regex("^[0-9A-Fa-f]{2}(\\s[0-9A-Fa-f]{2})+$")
        val compact = Regex("^[0-9A-Fa-f]{8,}$")
        return spaced.matches(v) || compact.matches(v)
    }

    private fun startInventory(state: Byte) {
        if (!isConnected || rfidManager == null) return
        val minIntervalMs = 400L
        val now = SystemClock.elapsedRealtime()
        val delta = now - lastInventoryStartMs
        if (delta < minIntervalMs) {
            pendingInventoryState = state
            if (pendingInventory) return
            pendingInventory = true
            val delay = minIntervalMs - delta
            mainHandler.postDelayed({
                pendingInventory = false
                startInventory(pendingInventoryState)
            }, delay)
            return
        }

        lastInventoryStartMs = now
        val ret = rfidManager?.customizedSessionTargetInventory(readId, 1, state, 1) ?: -1
        Log.d(TAG, "inventory(state=$state) ret=$ret")
    }
    
    private val rfidCallback = object : IRfidCallback.Stub() {
        override fun onInventoryTag(ant: Byte, pc: String?, epc: String?, rssi: String?,
                freq: Byte, tid: String?, userData: String?, epcLen: Int, tidLen: Int, 
                userDataLen: String?) {
            lastTagSeenMs = SystemClock.elapsedRealtime()
            Log.d(TAG, "onInventoryTag raw: ant=$ant pc=$pc epc=$epc rssi=$rssi freq=$freq tid=$tid userData=$userData epcLen=$epcLen tidLen=$tidLen userDataLen=$userDataLen")

            val epcFields = listOf(pc, epc, rssi, userData).filterNotNull().map { it.trim() }
            val epcRaw = epcFields
                .filter { looksLikeHexBytes(it) }
                .maxByOrNull { it.replace(" ", "").length }
                ?: (rssi ?: epc ?: pc ?: "")
            val epcOut = epcRaw.replace(" ", "")
            if (epcOut.isEmpty()) return

            val rssiInt = tid?.trim()?.toIntOrNull()
            val rssiValue = if (rssiInt != null) {
                if (rssiInt in 0..255) rssiInt - 129 else rssiInt
            } else {
                -70
            }

            mainHandler.post {
                Log.d(TAG, "emit onTagRead: epc=$epcOut tid=${tid ?: ""} rssi=$rssiValue")
                methodChannel?.invokeMethod("onTagRead", mapOf(
                    "epc" to epcOut,
                    "tid" to (tid ?: ""),
                    "rssi" to rssiValue
                ))
            }
        }
        
        override fun onInventoryTagEnd(ant: Int, tagNum: Int, readRate: Int, totalCount: Int, flag: Byte) {
            Log.d(TAG, "onInventoryTagEnd: ant=$ant tagNum=$tagNum readRate=$readRate totalCount=$totalCount flag=$flag")
            if (isScanning && rfidManager != null) {
                val now = SystemClock.elapsedRealtime()
                val delayMs = if (tagNum <= 0 && (now - lastTagSeenMs) > 1500) 800L else 80L
                mainHandler.postDelayed({
                    if (isScanning) startInventory(1)
                }, delayMs)
            }
        }
        
        override fun onOperationTag(tagType: String?, pc: String?, epc: String?, data: String?, 
                dataLen: Int, ant: Byte, state: Byte) {
            Log.d(TAG, "onOperationTag: tagType=$tagType pc=$pc epc=$epc data=$data dataLen=$dataLen ant=$ant state=$state")
            val op = pendingOp ?: return
            if (op.type == PendingOpType.READ) {
                pendingOp = null
                mainHandler.removeCallbacks(opTimeoutRunnable)
                try {
                    rfidManager?.cancelAccessEpcMatch(readId)
                } catch (e: Exception) {
                }
                if (op.wasScanning) {
                    isScanning = true
                    startInventory(0)
                    mainHandler.removeCallbacks(tuneRunnable)
                    mainHandler.postDelayed(tuneRunnable, 1500)
                }
                op.result.success(data?.replace(" ", "") ?: "")
            }
        }
        
        override fun onOperationTagEnd(count: Int) {
            Log.d(TAG, ">>> onOperationTagEnd")
        }
        
        override fun onExeCMDStatus(cmd: Byte, status: Byte) {
            Log.d(TAG, "onExeCMDStatus: cmd=$cmd status=$status")
            val op = pendingOp ?: return
            if (op.type != PendingOpType.WRITE) return
            if (!writeCompletionArmed) return

            val st = status.toInt() and 0xFF
            val ok = st == 0x10
            pendingOp = null
            writeCompletionArmed = false
            mainHandler.removeCallbacks(opTimeoutRunnable)
            try {
                rfidManager?.cancelAccessEpcMatch(readId)
            } catch (e: Exception) {
            }
            if (op.wasScanning) {
                isScanning = true
                startInventory(0)
                mainHandler.removeCallbacks(tuneRunnable)
                mainHandler.postDelayed(tuneRunnable, 1500)
            }
            op.result.success(ok)
        }
        
        override fun refreshSetting(rfidDate: RfidDate?) {
            Log.d(TAG, ">>> refreshSetting")
        }
    }
    
    override fun onStatus(status: RfidServiceManager.STATUS, manager: RfidManagerWrapper?) {
        Log.d(TAG, "onStatus: $status")
        if (status == RfidServiceManager.STATUS.SUCCESS && manager != null) {
            rfidManager = manager
            val connected = if (rfidManager!!.isConnected) true else rfidManager!!.connectCom("/dev/ttyHSL0", 115200)
            Log.d(TAG, "connectCom: $connected")
            if (connected) {
                readId = rfidManager!!.readId
                isConnected = true
                try { rfidManager!!.unregisterCallback(rfidCallback) } catch (e: Exception) { }
                rfidManager!!.registerCallback(rfidCallback)
                Log.d(TAG, "Connected! readId=$readId, callback registered")
                mainHandler.post { methodChannel?.invokeMethod("onConnectionChanged", true) }

                val powerRet = rfidManager?.setOutputPower(readId, 30.toByte()) ?: -1
                Log.d(TAG, "setOutputPower(30) ret=$powerRet")

                val triggerRet = rfidManager?.setTrigger(false) ?: -1
                Log.d(TAG, "setTrigger(false) ret=$triggerRet")

                val antRet = rfidManager?.setWorkAntenna(readId, 1.toByte()) ?: -1
                Log.d(TAG, "setWorkAntenna(1) ret=$antRet")

                val freqRet = rfidManager?.setFrequencyRegion(readId, 2.toByte(), 0.toByte(), 6.toByte()) ?: -1
                Log.d(TAG, "setFrequencyRegion(region=2,start=0,end=6) ret=$freqRet")

                // Do NOT auto-start scanning on connect
                isScanning = false
                lastTagSeenMs = SystemClock.elapsedRealtime()
                lastConfigChangeMs = 0
                tuningIndex = 0
            }
        } else if (status == RfidServiceManager.STATUS.NO_SERVICE) {
            Log.e(TAG, "NO_SERVICE - RFID service not available")
            mainHandler.post { methodChannel?.invokeMethod("onConnectionChanged", false) }
        }
    }
    
    fun setMethodChannel(channel: MethodChannel) { this.methodChannel = channel }
    
    fun initialize() {
        Log.d(TAG, "Initializing RfidServiceManager...")
        RfidServiceManager.getInstance(context).connect(this)
    }
    
    fun handleMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "connect" -> {
                if (isConnected) result.success(true)
                else { initialize(); result.success(true) }
            }
            "disconnect" -> {
                isScanning = false
                mainHandler.removeCallbacks(tuneRunnable)
                rfidManager?.unregisterCallback(rfidCallback)
                rfidManager?.disConnect()
                RfidServiceManager.getInstance(context).release()
                isConnected = false
                result.success(true)
            }
            "startInventory" -> {
                if (!isConnected) { result.success(false); return }
                isScanning = true
                lastTagSeenMs = SystemClock.elapsedRealtime()
                mainHandler.removeCallbacks(tuneRunnable)
                mainHandler.postDelayed(tuneRunnable, 1500)
                val ret = rfidManager?.customizedSessionTargetInventory(readId, 1, 0, 1) ?: -1
                Log.d(TAG, "startInventory: $ret")
                mainHandler.post { methodChannel?.invokeMethod("onScanningStateChanged", true) }
                result.success(ret >= 0)
            }
            "stopInventory" -> {
                isScanning = false
                mainHandler.removeCallbacks(tuneRunnable)
                result.success(true)
            }
            "setOutputPower" -> {
                val power = call.argument<Int>("power") ?: 30
                val ret = rfidManager?.setOutputPower(readId, power.toByte()) ?: -1
                result.success(ret >= 0)
            }
            "readMemory" -> {
                val op = beginOp(PendingOpType.READ, result) ?: return
                val epc = call.argument<String>("epc")
                val memBank = call.argument<Int>("memBank") ?: 1
                val startAddr = call.argument<Int>("startAddr") ?: 2
                val length = call.argument<Int>("length") ?: 6
                val pwd = passwordBytes(call.argument<String>("password"))
                val epcBytes = hexToBytesOrNull(epc)
                if (pwd == null || epcBytes == null) {
                    endOp()
                    result.success(null)
                    return
                }

                val setRet = rfidManager?.setAccessEpcMatch(readId, (epcBytes.size and 0xFF).toByte(), epcBytes) ?: -1
                Log.d(TAG, "setAccessEpcMatch ret=$setRet")
                val ret = rfidManager?.readTag(readId, memBank.toByte(), startAddr.toByte(), length.toByte(), pwd) ?: -1
                Log.d(TAG, "readTag ret=$ret")
                if (ret < 0) {
                    endOp()
                    op.result.success(null)
                }
            }
            "writeMemory" -> {
                val op = beginOp(PendingOpType.WRITE, result) ?: return
                val epc = call.argument<String>("epc")
                val memBank = call.argument<Int>("memBank") ?: 1
                val startAddr = call.argument<Int>("startAddr") ?: 2
                val length = call.argument<Int>("length") ?: 6
                val dataHex = call.argument<String>("data")
                val pwd = passwordBytes(call.argument<String>("password"))
                val epcBytes = hexToBytesOrNull(epc)
                val dataBytesRaw = hexToBytesOrNull(dataHex)
                if (pwd == null || epcBytes == null || dataBytesRaw == null) {
                    endOp()
                    result.success(false)
                    return
                }

                val expectedBytes = (length * 2).coerceAtLeast(0)
                val dataBytes = ByteArray(expectedBytes)
                val copyLen = minOf(expectedBytes, dataBytesRaw.size)
                System.arraycopy(dataBytesRaw, 0, dataBytes, 0, copyLen)

                val setRet = rfidManager?.setAccessEpcMatch(readId, (epcBytes.size and 0xFF).toByte(), epcBytes) ?: -1
                Log.d(TAG, "setAccessEpcMatch ret=$setRet")
                val ret = rfidManager?.writeTag(readId, pwd, memBank.toByte(), startAddr.toByte(), length.toByte(), dataBytes) ?: -1
                Log.d(TAG, "writeTag ret=$ret")
                writeCompletionArmed = ret >= 0
                if (ret < 0) {
                    endOp()
                    op.result.success(false)
                }
            }
            "writeEpc" -> {
                val op = beginOp(PendingOpType.WRITE, result) ?: return
                val targetEpc = call.argument<String>("targetEpc")
                val newEpc = call.argument<String>("newEpc")
                val pwd = passwordBytes(call.argument<String>("password"))
                val epcBytes = hexToBytesOrNull(targetEpc)
                val dataBytesRaw = hexToBytesOrNull(newEpc)
                if (pwd == null || epcBytes == null || dataBytesRaw == null) {
                    endOp()
                    result.success(false)
                    return
                }

                val startAddr = 2
                val lengthWords = ((dataBytesRaw.size + 1) / 2).coerceAtLeast(1)
                val dataBytes = ByteArray(lengthWords * 2)
                System.arraycopy(dataBytesRaw, 0, dataBytes, 0, minOf(dataBytesRaw.size, dataBytes.size))

                val setRet = rfidManager?.setAccessEpcMatch(readId, (epcBytes.size and 0xFF).toByte(), epcBytes) ?: -1
                Log.d(TAG, "setAccessEpcMatch ret=$setRet")
                val ret = rfidManager?.writeTag(readId, pwd, 1.toByte(), startAddr.toByte(), lengthWords.toByte(), dataBytes) ?: -1
                Log.d(TAG, "writeEpc(writeTag) ret=$ret")
                writeCompletionArmed = ret >= 0
                if (ret < 0) {
                    endOp()
                    op.result.success(false)
                }
            }
            else -> result.notImplemented()
        }
    }
    
    fun onTriggerPressed() {
        Log.d(TAG, "onTriggerPressed: isConnected=$isConnected, isScanning=$isScanning, rfidManager=${rfidManager != null}")
        if (!isConnected || isScanning) return
        isScanning = true
        startInventory(0)
        mainHandler.post { methodChannel?.invokeMethod("onScanningStateChanged", true) }
    }
    
    fun onTriggerReleased() {
        if (!isScanning) return
        isScanning = false
        mainHandler.removeCallbacks(tuneRunnable)
        mainHandler.post { methodChannel?.invokeMethod("onScanningStateChanged", false) }
    }
    
    fun release() {
        isScanning = false
        mainHandler.removeCallbacks(tuneRunnable)
        rfidManager?.unregisterCallback(rfidCallback)
        rfidManager?.disConnect()
        RfidServiceManager.getInstance(context).release()
        isConnected = false
    }
}
