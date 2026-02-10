package com.urovo.rfid;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class RfidServiceManager {
    private static final String TAG = "RfidServiceManager";
    private static final String RFID_SERVICE_ACTION = "com.ubx.usdk.rfid.RfidService";
    private static final String RFID_SERVICE_PACKAGE = "com.ubx.usdk.rfid";
    
    private static RfidServiceManager sInstance;
    private Context mContext;
    private RfidManagerWrapper mRfidManager;
    private StatusListener mStatusListener;
    private boolean mBound = false;
    
    public enum STATUS {
        SUCCESS, NO_SERVICE, DISCONNECTED
    }
    
    public interface StatusListener {
        void onStatus(STATUS status, RfidManagerWrapper manager);
    }
    
    private RfidServiceManager(Context context) {
        mContext = context.getApplicationContext();
    }
    
    public static synchronized RfidServiceManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RfidServiceManager(context);
        }
        return sInstance;
    }
    
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected: " + name);
            mBound = true;
            mRfidManager = new RfidManagerWrapper(service);
            if (mStatusListener != null) {
                mStatusListener.onStatus(STATUS.SUCCESS, mRfidManager);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected: " + name);
            mBound = false;
            mRfidManager = null;
            if (mStatusListener != null) {
                mStatusListener.onStatus(STATUS.DISCONNECTED, null);
            }
        }
    };
    
    public void connect(StatusListener listener) {
        mStatusListener = listener;
        
        Intent intent = new Intent(RFID_SERVICE_ACTION);
        intent.setPackage(RFID_SERVICE_PACKAGE);
        
        try {
            boolean bound = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "bindService result: " + bound);
            
            if (!bound) {
                Log.e(TAG, "Failed to bind to RFID service");
                if (mStatusListener != null) {
                    mStatusListener.onStatus(STATUS.NO_SERVICE, null);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error binding service: " + e.getMessage(), e);
            if (mStatusListener != null) {
                mStatusListener.onStatus(STATUS.NO_SERVICE, null);
            }
        }
    }
    
    public RfidManagerWrapper getRfidManager() {
        return mRfidManager;
    }
    
    public void release() {
        if (mBound) {
            try {
                mContext.unbindService(mServiceConnection);
            } catch (Exception e) {
                Log.e(TAG, "Error unbinding service", e);
            }
            mBound = false;
        }
        mRfidManager = null;
    }
}
