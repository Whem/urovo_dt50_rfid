package com.urovo.rfid.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IRfidCallback extends IInterface {
    String DESCRIPTOR = "com.ubx.usdk.rfid.aidl.IRfidCallback";

    void onInventoryTag(byte ant, String pc, String epc, String rssi, byte freq, String tid, String userData, int epcLen, int tidLen, String userDataLen) throws RemoteException;
    void onInventoryTagEnd(int ant, int tagNum, int readRate, int totalCount, byte flag) throws RemoteException;
    void onOperationTag(String tagType, String pc, String epc, String data, int dataLen, byte ant, byte state) throws RemoteException;
    void onOperationTagEnd(int count) throws RemoteException;
    void refreshSetting(RfidDate rfidDate) throws RemoteException;
    void onExeCMDStatus(byte cmd, byte status) throws RemoteException;

    abstract class Stub extends Binder implements IRfidCallback {
        static final int TRANSACTION_onInventoryTag = 1;
        static final int TRANSACTION_onInventoryTagEnd = 2;
        static final int TRANSACTION_onOperationTag = 3;
        static final int TRANSACTION_onOperationTagEnd = 4;
        static final int TRANSACTION_refreshSetting = 5;
        static final int TRANSACTION_onExeCMDStatus = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRfidCallback asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin instanceof IRfidCallback) {
                return (IRfidCallback) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == 0x5f4e5446) {
                if (reply != null) {
                    reply.writeString(DESCRIPTOR);
                }
                return true;
            }
            switch (code) {
                case TRANSACTION_onInventoryTag:
                    data.enforceInterface(DESCRIPTOR);
                    onInventoryTag(data.readByte(), data.readString(), data.readString(), data.readString(),
                            data.readByte(), data.readString(), data.readString(), data.readInt(), data.readInt(), data.readString());
                    if (reply != null) reply.writeNoException();
                    return true;
                case TRANSACTION_onInventoryTagEnd:
                    data.enforceInterface(DESCRIPTOR);
                    onInventoryTagEnd(data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readByte());
                    if (reply != null) reply.writeNoException();
                    return true;
                case TRANSACTION_onOperationTag:
                    data.enforceInterface(DESCRIPTOR);
                    onOperationTag(data.readString(), data.readString(), data.readString(), data.readString(),
                            data.readInt(), data.readByte(), data.readByte());
                    if (reply != null) reply.writeNoException();
                    return true;
                case TRANSACTION_onOperationTagEnd:
                    data.enforceInterface(DESCRIPTOR);
                    onOperationTagEnd(data.readInt());
                    if (reply != null) reply.writeNoException();
                    return true;
                case TRANSACTION_refreshSetting:
                    data.enforceInterface(DESCRIPTOR);
                    RfidDate rfidDate = null;
                    if (data.readInt() != 0) {
                        rfidDate = RfidDate.CREATOR.createFromParcel(data);
                    }
                    refreshSetting(rfidDate);
                    if (reply != null) reply.writeNoException();
                    return true;
                case TRANSACTION_onExeCMDStatus:
                    data.enforceInterface(DESCRIPTOR);
                    onExeCMDStatus(data.readByte(), data.readByte());
                    if (reply != null) reply.writeNoException();
                    return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IRfidCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            @Override
            public void onInventoryTag(byte ant, String pc, String epc, String rssi, byte freq, String tid, String userData, int epcLen, int tidLen, String userDataLen) throws RemoteException {
                Parcel data = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeByte(ant);
                    data.writeString(pc);
                    data.writeString(epc);
                    data.writeString(rssi);
                    data.writeByte(freq);
                    data.writeString(tid);
                    data.writeString(userData);
                    data.writeInt(epcLen);
                    data.writeInt(tidLen);
                    data.writeString(userDataLen);
                    mRemote.transact(TRANSACTION_onInventoryTag, data, null, FLAG_ONEWAY);
                } finally {
                    data.recycle();
                }
            }

            @Override
            public void onInventoryTagEnd(int ant, int tagNum, int readRate, int totalCount, byte flag) throws RemoteException {
                Parcel data = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(ant);
                    data.writeInt(tagNum);
                    data.writeInt(readRate);
                    data.writeInt(totalCount);
                    data.writeByte(flag);
                    mRemote.transact(TRANSACTION_onInventoryTagEnd, data, null, FLAG_ONEWAY);
                } finally {
                    data.recycle();
                }
            }

            @Override
            public void onOperationTag(String tagType, String pc, String epc, String data2, int dataLen, byte ant, byte state) throws RemoteException {
                Parcel data = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(tagType);
                    data.writeString(pc);
                    data.writeString(epc);
                    data.writeString(data2);
                    data.writeInt(dataLen);
                    data.writeByte(ant);
                    data.writeByte(state);
                    mRemote.transact(TRANSACTION_onOperationTag, data, null, FLAG_ONEWAY);
                } finally {
                    data.recycle();
                }
            }

            @Override
            public void onOperationTagEnd(int count) throws RemoteException {
                Parcel data = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(count);
                    mRemote.transact(TRANSACTION_onOperationTagEnd, data, null, FLAG_ONEWAY);
                } finally {
                    data.recycle();
                }
            }

            @Override
            public void refreshSetting(RfidDate rfidDate) throws RemoteException {
                Parcel data = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    if (rfidDate != null) {
                        data.writeInt(1);
                        rfidDate.writeToParcel(data, 0);
                    } else {
                        data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_refreshSetting, data, null, FLAG_ONEWAY);
                } finally {
                    data.recycle();
                }
            }

            @Override
            public void onExeCMDStatus(byte cmd, byte status) throws RemoteException {
                Parcel data = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeByte(cmd);
                    data.writeByte(status);
                    mRemote.transact(TRANSACTION_onExeCMDStatus, data, null, FLAG_ONEWAY);
                } finally {
                    data.recycle();
                }
            }
        }
    }
}
