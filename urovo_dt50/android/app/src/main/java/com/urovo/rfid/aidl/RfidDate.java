package com.urovo.rfid.aidl;

import android.os.Parcel;
import android.os.Parcelable;

public class RfidDate implements Parcelable {
    private String epc;
    private String pc;
    private String rssi;
    private String tid;
    private String userData;
    private byte ant;
    private byte freq;
    private int epcLen;
    private int tidLen;

    public RfidDate() {}

    protected RfidDate(Parcel in) {
        epc = in.readString();
        pc = in.readString();
        rssi = in.readString();
        tid = in.readString();
        userData = in.readString();
        ant = in.readByte();
        freq = in.readByte();
        epcLen = in.readInt();
        tidLen = in.readInt();
    }

    public static final Creator<RfidDate> CREATOR = new Creator<RfidDate>() {
        @Override
        public RfidDate createFromParcel(Parcel in) {
            return new RfidDate(in);
        }

        @Override
        public RfidDate[] newArray(int size) {
            return new RfidDate[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(epc);
        dest.writeString(pc);
        dest.writeString(rssi);
        dest.writeString(tid);
        dest.writeString(userData);
        dest.writeByte(ant);
        dest.writeByte(freq);
        dest.writeInt(epcLen);
        dest.writeInt(tidLen);
    }

    // Getters and setters
    public String getEpc() { return epc; }
    public void setEpc(String epc) { this.epc = epc; }
    public String getPc() { return pc; }
    public void setPc(String pc) { this.pc = pc; }
    public String getRssi() { return rssi; }
    public void setRssi(String rssi) { this.rssi = rssi; }
    public String getTid() { return tid; }
    public void setTid(String tid) { this.tid = tid; }
    public String getUserData() { return userData; }
    public void setUserData(String userData) { this.userData = userData; }
    public byte getAnt() { return ant; }
    public void setAnt(byte ant) { this.ant = ant; }
    public byte getFreq() { return freq; }
    public void setFreq(byte freq) { this.freq = freq; }
    public int getEpcLen() { return epcLen; }
    public void setEpcLen(int epcLen) { this.epcLen = epcLen; }
    public int getTidLen() { return tidLen; }
    public void setTidLen(int tidLen) { this.tidLen = tidLen; }
}
