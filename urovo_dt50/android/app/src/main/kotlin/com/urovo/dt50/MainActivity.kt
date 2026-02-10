package com.urovo.dt50

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val TAG = "MainActivity"
    private val CHANNEL = "com.urovo.dt50/rfid"
    private var rfidPlugin: RfidPlugin? = null
    private var methodChannel: MethodChannel? = null
    
    companion object {
        private const val KEYCODE_SCAN_TRIGGER = 523
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        Log.d(TAG, "configureFlutterEngine")
        
        rfidPlugin = RfidPlugin(this)
        
        methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).also { channel ->
            rfidPlugin?.setMethodChannel(channel)
            channel.setMethodCallHandler { call, result ->
                rfidPlugin?.handleMethodCall(call, result)
            }
        }
        
        rfidPlugin?.initialize()
    }
    
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        Log.v(TAG, "dispatchKeyEvent: keyCode=${event.keyCode}, action=${event.action}")
        
        if (event.keyCode == KEYCODE_SCAN_TRIGGER) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                Log.d(TAG, "Trigger pressed - starting inventory")
                rfidPlugin?.onTriggerPressed()
            } else if (event.action == KeyEvent.ACTION_UP) {
                Log.d(TAG, "Trigger released - stopping inventory")
                rfidPlugin?.onTriggerReleased()
            }
            return true
        }
        
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        rfidPlugin?.release()
        rfidPlugin = null
        methodChannel = null
        super.onDestroy()
    }
}
