package com.urovo.rfid;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.urovo.rfid.aidl.IRfidCallback;
import com.urovo.rfid.aidl.IRfidManager;

public class RfidManagerWrapper {
    private static final String TAG = "RfidManagerWrapper";
    private IRfidManager mRfidManager;
    private int mCallbackKey = 0;
    
    public RfidManagerWrapper(IBinder binder) {
        mRfidManager = IRfidManager.Stub.asInterface(binder);
        Log.d(TAG, "RfidManagerWrapper created, binder: " + (binder != null));
    }
    
    public boolean connectCom(String port, int baudrate) {
        if (mRfidManager != null) {
            try {
                return mRfidManager.connectCom(port, baudrate);
            } catch (RemoteException e) {
                Log.e(TAG, "connectCom error", e);
            }
        }
        return false;
    }
    
    public void disConnect() {
        if (mRfidManager != null) {
            try {
                mRfidManager.disConnect();
            } catch (RemoteException e) {
                Log.e(TAG, "disConnect error", e);
            }
        }
    }
    
    public boolean isConnected() {
        if (mRfidManager != null) {
            try {
                return mRfidManager.isConnected();
            } catch (RemoteException e) {
                Log.e(TAG, "isConnected error", e);
            }
        }
        return false;
    }
    
    public byte getReadId() {
        if (mRfidManager != null) {
            try {
                return mRfidManager.getReadId();
            } catch (RemoteException e) {
                Log.e(TAG, "getReadId error", e);
            }
        }
        return 0;
    }
    
    public int customizedSessionTargetInventory(byte readId, byte session, byte target, byte repeat) {
        if (mRfidManager != null) {
            try {
                return mRfidManager.customizedSessionTargetInventory(readId, session, target, repeat);
            } catch (RemoteException e) {
                Log.e(TAG, "customizedSessionTargetInventory error", e);
            }
        }
        return -1;
    }
    
    public int setOutputPower(byte readId, byte power) {
        if (mRfidManager != null) {
            try {
                return mRfidManager.setOutputPower(readId, power);
            } catch (RemoteException e) {
                Log.e(TAG, "setOutputPower error", e);
            }
        }
        return -1;
    }
    
    public int getOutputPower(byte readId) {
        if (mRfidManager != null) {
            try {
                return mRfidManager.getOutputPower(readId);
            } catch (RemoteException e) {
                Log.e(TAG, "getOutputPower error", e);
            }
        }
        return -1;
    }

    public int setFrequencyRegion(byte readId, byte region, byte startFreq, byte endFreq) {
        if (mRfidManager != null) {
            try {
                return mRfidManager.setFrequencyRegion(readId, region, startFreq, endFreq);
            } catch (RemoteException e) {
                Log.e(TAG, "setFrequencyRegion error", e);
            }
        }
        return -1;
    }

    public int setWorkAntenna(byte readId, byte ant) {
        if (mRfidManager != null) {
            try {
                return mRfidManager.setWorkAntenna(readId, ant);
            } catch (RemoteException e) {
                Log.e(TAG, "setWorkAntenna error", e);
            }
        }
        return -1;
    }

    public int setTrigger(boolean enable) {
        if (mRfidManager != null) {
            try {
                return mRfidManager.setTrigger(enable);
            } catch (RemoteException e) {
                Log.e(TAG, "setTrigger error", e);
            }
        }
        return -1;
    }

    public int setAccessEpcMatch(byte readId, byte epcLen, byte[] epc) {
        if (mRfidManager != null) {
            try {
                return mRfidManager.setAccessEpcMatch(readId, epcLen, epc);
            } catch (RemoteException e) {
                Log.e(TAG, "setAccessEpcMatch error", e);
            }
        }
        return -1;
    }

    public int cancelAccessEpcMatch(byte readId) {
        if (mRfidManager != null) {
            try {
                return mRfidManager.cancelAccessEpcMatch(readId);
            } catch (RemoteException e) {
                Log.e(TAG, "cancelAccessEpcMatch error", e);
            }
        }
        return -1;
    }

    public int readTag(byte readId, byte bank, byte start, byte len, byte[] password) {
        if (mRfidManager != null) {
            try {
                return mRfidManager.readTag(readId, bank, start, len, password);
            } catch (RemoteException e) {
                Log.e(TAG, "readTag error", e);
            }
        }
        return -1;
    }

    public int writeTag(byte readId, byte[] password, byte bank, byte start, byte len, byte[] data) {
        if (mRfidManager != null) {
            try {
                return mRfidManager.writeTag(readId, password, bank, start, len, data);
            } catch (RemoteException e) {
                Log.e(TAG, "writeTag error", e);
            }
        }
        return -1;
    }
    
    public void registerCallback(IRfidCallback callback) {
        if (mRfidManager != null) {
            try {
                int key = callback != null ? callback.hashCode() : 0;
                mCallbackKey = key;
                mRfidManager.registerCallback(callback, key);
                Log.d(TAG, "Callback registered with key: " + key);
            } catch (RemoteException e) {
                Log.e(TAG, "registerCallback error", e);
            }
        }
    }
    
    public void unregisterCallback(IRfidCallback callback) {
        if (mRfidManager != null) {
            try {
                int key = callback != null ? callback.hashCode() : mCallbackKey;
                mRfidManager.unregisterCallback(callback, key);
            } catch (RemoteException e) {
                Log.e(TAG, "unregisterCallback error", e);
            }
        }
    }
}
