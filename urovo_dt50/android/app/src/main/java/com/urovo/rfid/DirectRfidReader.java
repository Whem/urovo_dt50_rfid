package com.urovo.rfid;

import android.os.SystemClock;
import android.util.Log;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import com.rfiddevice.serialport.SerialPort;

public class DirectRfidReader {
    private static final String TAG = "DirectRfidReader";
    private static final String DEFAULT_PORT = "/dev/ttyHSL0";
    private static final int DEFAULT_BAUD = 115200;
    
    private SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean connected = false;
    private volatile boolean scanning = false;
    private TagCallback tagCallback;
    private Thread readThread;
    
    public interface TagCallback {
        void onTagRead(String epc, int rssi);
        void onInventoryEnd();
        void onError(String error);
    }
    
    public void setTagCallback(TagCallback callback) {
        this.tagCallback = callback;
    }
    
    public boolean connect() {
        return connect(DEFAULT_PORT, DEFAULT_BAUD);
    }
    
    public boolean connect(String port, int baudRate) {
        try {
            // Power on the RFID module first
            powerOn();
            SystemClock.sleep(1500); // Wait for module to initialize
            
            Log.d(TAG, "Connecting to " + port + " at " + baudRate);
            serialPort = new SerialPort(new File(port), baudRate, 0);
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            connected = true;
            Log.d(TAG, "Connected successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Connect failed: " + e.getMessage(), e);
            connected = false;
            return false;
        }
    }
    
    private void powerOn() {
        // Try Urovo DeviceManager API first
        boolean powered = UrovoPowerManager.powerOn();
        Log.d(TAG, "UrovoPowerManager.powerOn() = " + powered);
    }
    
    private void powerOff() {
        String[] powerNodes = {
            "/sys/devices/soc/soc:sectrl/ugp_ctrl/gp_pogo_5v_ctrl/enable",
            "/sys/class/rfid_ctrl/enable",
            "/sys/class/rfid_ctrl/power"
        };
        
        for (String node : powerNodes) {
            try {
                File f = new File(node);
                if (f.exists()) {
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
                    fos.write(new byte[]{'0'});
                    fos.close();
                    Log.d(TAG, "Power off via: " + node);
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    public void disconnect() {
        stopInventory();
        connected = false;
        if (serialPort != null) {
            try {
                serialPort.close();
            } catch (Exception e) {
                Log.e(TAG, "Disconnect error: " + e.getMessage());
            }
            serialPort = null;
        }
        inputStream = null;
        outputStream = null;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public boolean startInventory() {
        if (!connected) {
            Log.e(TAG, "Not connected");
            return false;
        }
        
        scanning = true;
        readThread = new Thread(this::inventoryLoop);
        readThread.start();
        return true;
    }
    
    public void stopInventory() {
        scanning = false;
        if (readThread != null) {
            try {
                sendStopCommand();
                readThread.join(1000);
            } catch (Exception e) {
                Log.e(TAG, "Stop error: " + e.getMessage());
            }
            readThread = null;
        }
    }
    
    private void inventoryLoop() {
        Log.d(TAG, "Inventory loop started");
        byte[] buffer = new byte[2048];
        int bufferLen = 0;
        
        while (scanning && connected) {
            try {
                // Send inventory command
                sendInventoryCommand();
                
                // Read responses for scan time
                long startTime = SystemClock.elapsedRealtime();
                while (scanning && (SystemClock.elapsedRealtime() - startTime) < 1000) {
                    if (inputStream.available() > 0) {
                        int len = inputStream.read(buffer, bufferLen, buffer.length - bufferLen);
                        if (len > 0) {
                            bufferLen += len;
                            Log.d(TAG, "Received " + len + " bytes, total: " + bufferLen);
                            
                            // Parse responses
                            bufferLen = parseResponses(buffer, bufferLen);
                        }
                    } else {
                        SystemClock.sleep(10);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Inventory loop error: " + e.getMessage());
                if (tagCallback != null) {
                    tagCallback.onError(e.getMessage());
                }
                break;
            }
        }
        
        if (tagCallback != null) {
            tagCallback.onInventoryEnd();
        }
        Log.d(TAG, "Inventory loop ended");
    }
    
    private void sendInventoryCommand() {
        // Command: [length, ComAddr, CMD, QValue, Session, Target, Ant, Scantime]
        // CMD 1 = Inventory_G2
        byte[] cmd = new byte[] {
            9,              // length
            (byte) 0xFF,    // ComAddr (broadcast)
            1,              // CMD = Inventory_G2
            4,              // QValue
            0,              // Session
            0,              // Target
            (byte) 0x80,    // Ant (0x80 = all)
            10              // Scantime (10 * 100ms = 1s)
        };
        
        // Calculate and append CRC
        getCRC(cmd, cmd.length - 2);
        
        try {
            outputStream.write(cmd);
            outputStream.flush();
            Log.d(TAG, "Sent inventory cmd: " + bytesToHex(cmd));
        } catch (Exception e) {
            Log.e(TAG, "Send inventory error: " + e.getMessage());
        }
    }
    
    private void sendStopCommand() {
        // Command: [length, ComAddr, CMD]
        // CMD 0x93 (-109) = StopInventory
        byte[] cmd = new byte[] {
            4,              // length
            (byte) 0xFF,    // ComAddr
            (byte) 0x93     // CMD = StopInventory
        };
        
        getCRC(cmd, cmd.length - 2);
        
        try {
            outputStream.write(cmd);
            outputStream.flush();
            Log.d(TAG, "Sent stop cmd: " + bytesToHex(cmd));
        } catch (Exception e) {
            Log.e(TAG, "Send stop error: " + e.getMessage());
        }
    }
    
    private int parseResponses(byte[] buffer, int length) {
        int index = 0;
        
        while (length - index >= 5) {
            int packetLen = buffer[index] & 0xFF;
            
            if (packetLen < 4 || packetLen > 250) {
                index++;
                continue;
            }
            
            if (length < index + packetLen + 1) {
                // Incomplete packet, keep remaining data
                break;
            }
            
            byte[] packet = new byte[packetLen + 1];
            System.arraycopy(buffer, index, packet, 0, packetLen + 1);
            
            if (checkCRC(packet)) {
                int cmd = packet[2] & 0xFF;
                int status = packet[3] & 0xFF;
                
                Log.d(TAG, "Packet: cmd=" + cmd + ", status=" + status + ", len=" + packetLen);
                
                if (cmd == 1) { // Inventory response
                    if (status == 1 || status == 2) {
                        // Tag data
                        parseTagData(packet, packetLen);
                    } else if (status == 0x01 || status == 0x02 || status == 0xFB) {
                        // Inventory end or no tag
                        Log.d(TAG, "Inventory status: " + status);
                    }
                }
                
                index += packetLen + 1;
            } else {
                index++;
            }
        }
        
        // Move remaining data to start of buffer
        if (index > 0 && length > index) {
            System.arraycopy(buffer, index, buffer, 0, length - index);
        }
        
        return length - index;
    }
    
    private void parseTagData(byte[] packet, int packetLen) {
        try {
            // Standard inventory response format:
            // [len, addr, cmd, status, ant, num, [pc(2), epc(12), rssi], ...]
            if (packetLen < 10) return;
            
            int status = packet[3] & 0xFF;
            int num = packet[4] & 0xFF;
            
            Log.d(TAG, "Tag data: status=" + status + ", num=" + num);
            
            if (num > 0 && packetLen >= 7) {
                int pos = 5;
                for (int i = 0; i < num && pos < packetLen - 2; i++) {
                    // PC (2 bytes) + EPC (variable) + RSSI (1 byte)
                    if (pos + 3 > packetLen) break;
                    
                    int epcLen = 12; // Default EPC length
                    
                    // Try to extract PC to determine EPC length
                    if (pos + 2 <= packetLen) {
                        int pc = ((packet[pos] & 0xFF) << 8) | (packet[pos + 1] & 0xFF);
                        epcLen = ((pc >> 11) & 0x1F) * 2;
                        if (epcLen <= 0 || epcLen > 62) epcLen = 12;
                    }
                    
                    if (pos + 2 + epcLen + 1 > packetLen) break;
                    
                    byte[] epcBytes = new byte[epcLen];
                    System.arraycopy(packet, pos + 2, epcBytes, 0, epcLen);
                    String epc = bytesToHex(epcBytes);
                    
                    int rssi = packet[pos + 2 + epcLen] & 0xFF;
                    if (rssi > 127) rssi = rssi - 256;
                    
                    Log.d(TAG, ">>> TAG: EPC=" + epc + ", RSSI=" + rssi);
                    
                    if (tagCallback != null) {
                        tagCallback.onTagRead(epc, rssi);
                    }
                    
                    pos += 2 + epcLen + 1;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse tag error: " + e.getMessage());
        }
    }
    
    private void getCRC(byte[] data, int len) {
        int crc = 0xFFFF;
        for (int i = 0; i < len; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >> 1) ^ 0x8408;
                } else {
                    crc >>= 1;
                }
            }
        }
        data[len] = (byte) (crc & 0xFF);
        data[len + 1] = (byte) ((crc >> 8) & 0xFF);
    }
    
    private boolean checkCRC(byte[] data) {
        int len = (data[0] & 0xFF) - 1;
        if (len < 2 || len >= data.length) return false;
        
        byte[] temp = new byte[len + 2];
        System.arraycopy(data, 0, temp, 0, len);
        getCRC(temp, len);
        
        return temp[len] == data[len] && temp[len + 1] == data[len + 1];
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
    
    public int setOutputPower(int power) {
        if (!connected) return -1;
        
        // Command: [length, ComAddr, CMD, power]
        // CMD 0x2F (47) = SetRfPower
        byte[] cmd = new byte[] {
            5,              // length
            (byte) 0xFF,    // ComAddr
            0x2F,           // CMD = SetRfPower
            (byte) power    // power (dBm)
        };
        
        getCRC(cmd, cmd.length - 2);
        
        try {
            outputStream.write(cmd);
            outputStream.flush();
            Log.d(TAG, "Set power to " + power);
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Set power error: " + e.getMessage());
            return -1;
        }
    }
}
