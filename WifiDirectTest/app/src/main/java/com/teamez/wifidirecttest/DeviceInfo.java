package com.teamez.wifidirecttest;

import android.net.wifi.p2p.WifiP2pDevice;

public class DeviceInfo extends WifiP2pDevice {

    private transient boolean isConnected = false;

    //public DeviceInfo(){}
    DeviceInfo(String Name, String IP) {
        deviceName = Name;
        deviceAddress = IP;
    }
    DeviceInfo(String Name, String IP, boolean connected) {
        deviceName = Name;
        deviceAddress = IP;
        if(connected)
            setConnected(true);
    }


    boolean isConnected() {
        return isConnected;
    }

    void setConnected(Boolean connected) {
        isConnected = connected;
    }
}


