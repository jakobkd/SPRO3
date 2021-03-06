package com.teamez.ezrobotac;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

private WifiManager mManager;
private MainActivity mActivity;

public WifiDirectBroadcastReceiver(WifiManager mManager, MainActivity mainActivity) {
    this.mManager = mManager;
    this.mActivity = mainActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {

        }
        if(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            mActivity.updateNetList();
        }
        /*if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Determine if Wifi P2P mode is enabled or not, alert
            // the Activity.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                //mActivity.setIsWifiP2pEnabled
                //Toast.makeText(mActivity, "p2p enabled", Toast.LENGTH_LONG).show();
                //wifi is on
            } else {
                //wifi is off
                //Toast.makeText(mActivity, "p2p not enabled", Toast.LENGTH_LONG).show();
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // The peer list has changed! We should probably do something about
            // that.
            if(mManager != null) {
                mManager.requestPeers(mChannel, mActivity.peerListListener);
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            // Connection state changed! We should probably do something about
            // that.
            if(mManager == null) {
                return;
            }

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if(networkInfo.isConnected()) {
                mManager.requestConnectionInfo(mChannel, mActivity.connectionInfoListener);
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            //DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
                    //.findFragmentById(R.id.frag_list);
            //fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                    //WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));

        }*/


    }
}
