package com.rfiddevice.serialport;

import android.util.Log;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialPort {
    private static final String TAG = "SerialPort";
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    private static native FileDescriptor open(String path, int baudrate, int flags);
    public native void close();

    static {
        System.loadLibrary("serial_port_driver");
    }

    public SerialPort(File device, int baudrate, int flags) throws SecurityException, IOException {
        mFd = open(device.getAbsolutePath(), baudrate, flags);
        if (mFd == null) {
            Log.e(TAG, "native open returns null for " + device.getAbsolutePath());
            throw new IOException("Failed to open " + device.getAbsolutePath());
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }
}
