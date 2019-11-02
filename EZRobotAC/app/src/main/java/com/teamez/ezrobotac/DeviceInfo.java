package com.teamez.ezrobotac;


import android.net.wifi.WifiConfiguration;

public class DeviceInfo {

    public transient boolean isAvailable = false;
    public String mSSID;
    public String mPassword;
    public WifiConfiguration mConnf;

    //public DeviceInfo(){}
    DeviceInfo(String SSID, String Password, WifiConfiguration connf) {
        this.mSSID = SSID;
        this.mPassword = Password;
        this.mConnf = connf;
    }

    DeviceInfo(String UDPPacket) {
        String[] host = UDPPacket.split(",");
        this.mSSID = host[1];
        this.mPassword = host[0];
    }
}


