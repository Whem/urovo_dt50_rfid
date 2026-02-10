package com.urovo.rfid.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IRfidManager extends IInterface {
    String DESCRIPTOR = "com.ubx.usdk.rfid.aidl.IRfidManager";

    boolean connectCom(String port, int baudrate) throws RemoteException;
    void disConnect() throws RemoteException;
    boolean isConnected() throws RemoteException;
    byte getReadId() throws RemoteException;
    int customizedSessionTargetInventory(byte readId, byte session, byte target, byte repeat) throws RemoteException;
    int setOutputPower(byte readId, byte power) throws RemoteException;
    int getOutputPower(byte readId) throws RemoteException;
    int setFrequencyRegion(byte readId, byte region, byte startFreq, byte endFreq) throws RemoteException;
    int setWorkAntenna(byte readId, byte ant) throws RemoteException;
    int setTrigger(boolean enable) throws RemoteException;
    int setAccessEpcMatch(byte readId, byte epcLen, byte[] epc) throws RemoteException;
    int cancelAccessEpcMatch(byte readId) throws RemoteException;
    int readTag(byte readId, byte bank, byte start, byte len, byte[] password) throws RemoteException;
    int writeTag(byte readId, byte[] password, byte bank, byte start, byte len, byte[] data) throws RemoteException;
    void registerCallback(IRfidCallback callback, int key) throws RemoteException;
    void unregisterCallback(IRfidCallback callback, int key) throws RemoteException;

    abstract class Stub extends Binder implements IRfidManager {
        static final int TRANSACTION_connectCom = 0x1;
        static final int TRANSACTION_disConnect = 0x2;
        static final int TRANSACTION_isConnected = 0x3;
        static final int TRANSACTION_getReadId = 0x36;
        static final int TRANSACTION_customizedSessionTargetInventory = 0x8;
        static final int TRANSACTION_setOutputPower = 0x29;
        static final int TRANSACTION_getOutputPower = 0x12;
        static final int TRANSACTION_setFrequencyRegion = 0x27;
        static final int TRANSACTION_setWorkAntenna = 0x32;
        static final int TRANSACTION_setTrigger = 0x2f;
        static final int TRANSACTION_cancelAccessEpcMatch = 0x6;
        static final int TRANSACTION_setAccessEpcMatch = 0x25;
        static final int TRANSACTION_readTag = 0x21;
        static final int TRANSACTION_writeTag = 0x33;
        static final int TRANSACTION_registerCallback = 0x34;
        static final int TRANSACTION_unregisterCallback = 0x35;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRfidManager asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin instanceof IRfidManager) {
                return (IRfidManager) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        private static class Proxy implements IRfidManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            @Override
            public boolean connectCom(String port, int baudrate) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(port);
                    data.writeInt(baudrate);
                    mRemote.transact(TRANSACTION_connectCom, data, reply, 0);
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void disConnect() throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_disConnect, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean isConnected() throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_isConnected, data, reply, 0);
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public byte getReadId() throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getReadId, data, reply, 0);
                    reply.readException();
                    return reply.readByte();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public int customizedSessionTargetInventory(byte readId, byte session, byte target, byte repeat) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeByte(readId);
                    data.writeByte(session);
                    data.writeByte(target);
                    data.writeByte(repeat);
                    mRemote.transact(TRANSACTION_customizedSessionTargetInventory, data, reply, 0);
                    reply.readException();
                    return reply.readInt();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public int setOutputPower(byte readId, byte power) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeByte(readId);
                    data.writeByte(power);
                    mRemote.transact(TRANSACTION_setOutputPower, data, reply, 0);
                    reply.readException();
                    return reply.readInt();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public int getOutputPower(byte readId) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeByte(readId);
                    mRemote.transact(TRANSACTION_getOutputPower, data, reply, 0);
                    reply.readException();
                    return reply.readInt();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public int setFrequencyRegion(byte readId, byte region, byte startFreq, byte endFreq) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeByte(readId);
                    data.writeByte(region);
                    data.writeByte(startFreq);
                    data.writeByte(endFreq);
                    mRemote.transact(TRANSACTION_setFrequencyRegion, data, reply, 0);
                    reply.readException();
                    return reply.readInt();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public int setWorkAntenna(byte readId, byte ant) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeByte(readId);
                    data.writeByte(ant);
                    mRemote.transact(TRANSACTION_setWorkAntenna, data, reply, 0);
                    reply.readException();
                    return reply.readInt();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public int setTrigger(boolean enable) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(enable ? 1 : 0);
                    mRemote.transact(TRANSACTION_setTrigger, data, reply, 0);
                    reply.readException();
                    return reply.readInt();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public int setAccessEpcMatch(byte readId, byte epcLen, byte[] epc) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeByte(readId);
                    data.writeByte(epcLen);
                    data.writeByteArray(epc);
                    mRemote.transact(TRANSACTION_setAccessEpcMatch, data, reply, 0);
                    reply.readException();
                    return reply.readInt();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public int cancelAccessEpcMatch(byte readId) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeByte(readId);
                    mRemote.transact(TRANSACTION_cancelAccessEpcMatch, data, reply, 0);
                    reply.readException();
                    return reply.readInt();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public int readTag(byte readId, byte bank, byte start, byte len, byte[] password) throws RemoteException {
                if (password == null) password = new byte[0];
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeByte(readId);
                    data.writeByte(bank);
                    data.writeByte(start);
                    data.writeByte(len);
                    data.writeByteArray(password);
                    mRemote.transact(TRANSACTION_readTag, data, reply, 0);
                    reply.readException();
                    int ret = reply.readInt();
                    reply.readByteArray(password);
                    return ret;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public int writeTag(byte readId, byte[] password, byte bank, byte start, byte len, byte[] dataBytes) throws RemoteException {
                if (password == null) password = new byte[0];
                if (dataBytes == null) dataBytes = new byte[0];
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeByte(readId);
                    data.writeByteArray(password);
                    data.writeByte(bank);
                    data.writeByte(start);
                    data.writeByte(len);
                    data.writeByteArray(dataBytes);
                    mRemote.transact(TRANSACTION_writeTag, data, reply, 0);
                    reply.readException();
                    int ret = reply.readInt();
                    reply.readByteArray(password);
                    reply.readByteArray(dataBytes);
                    return ret;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void registerCallback(IRfidCallback callback, int key) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    data.writeInt(key);
                    mRemote.transact(TRANSACTION_registerCallback, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void unregisterCallback(IRfidCallback callback, int key) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    data.writeInt(key);
                    mRemote.transact(TRANSACTION_unregisterCallback, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }
    }
}
