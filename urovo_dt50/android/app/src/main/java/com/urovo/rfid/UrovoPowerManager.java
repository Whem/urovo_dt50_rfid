package com.urovo.rfid;

import android.util.Log;
import java.lang.reflect.Method;

public class UrovoPowerManager {
    private static final String TAG = "UrovoPowerManager";
    private static Object deviceManager;
    
    public static boolean powerOn() {
        try {
            // Try to use Urovo DeviceManager via reflection
            Class<?> dmClass = Class.forName("android.device.DeviceManager");
            deviceManager = dmClass.newInstance();
            
            // Get project name
            Method getSettingProperty = dmClass.getMethod("getSettingProperty", String.class);
            String projectName = (String) getSettingProperty.invoke(deviceManager, "pwv.project");
            String node5v = (String) getSettingProperty.invoke(deviceManager, "persist.sys.pogopin.otg5v.en");
            
            Log.d(TAG, "Project: " + projectName + ", node5v: " + node5v);
            
            // Try to write to power node
            if (node5v != null && !node5v.isEmpty()) {
                return writePowerNode(node5v, true);
            }
            
            // Try common nodes based on project
            String[] commonNodes = {
                "/sys/devices/soc/soc:sectrl/ugp_ctrl/gp_pogo_5v_ctrl/enable",
                "/sys/devices/platform/otg_typecdig/pogo_5v",
                "/sys/devices/platform/otg_iddig/pogo_5v"
            };
            
            for (String node : commonNodes) {
                if (writePowerNode(node, true)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            Log.e(TAG, "PowerOn error: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean writePowerNode(String path, boolean enable) {
        try {
            java.io.FileOutputStream fos = new java.io.FileOutputStream(path);
            fos.write(enable ? new byte[]{'1'} : new byte[]{'0'});
            fos.close();
            Log.d(TAG, "Power node written: " + path + " = " + enable);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Cannot write " + path + ": " + e.getMessage());
            return false;
        }
    }
}
