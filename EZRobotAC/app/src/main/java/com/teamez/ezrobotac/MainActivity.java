package com.teamez.ezrobotac;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements NetworkDialogCloseListener{

    private static final int REQUEST_LOCATION = 2;
    List<WifiConfiguration> list = new ArrayList<>();
    List<DeviceInfo> deviceList = new ArrayList<>();
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    public DeviceListAdapter mAdapter;

    private final IntentFilter intentFilter = new IntentFilter();

    WifiManager wifiManager;
    BroadcastReceiver mReceiver;

    MainActivity x = this;
    String TAG = "Main Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        startService();

        mLayoutManager = new LinearLayoutManager(this);

        mRecyclerView = findViewById(R.id.network_list);

        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new DeviceListAdapter(this, R.layout.device_list_item, deviceList, mRecyclerView);
        mRecyclerView.setAdapter(mAdapter);

        initIntents();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        list = wifiManager.getConfiguredNetworks();

        /*for(WifiConfiguration net : list) {
            if(net.SSID.contains("EZRobot-"))
                wifiManager.removeNetwork(net.networkId);
        }*/
        list = wifiManager.getConfiguredNetworks();
        for(WifiConfiguration net : list) {
            if(net.SSID.contains("EZRobot-")) {

                //mAdapter.addElement(new DeviceInfo(net.SSID, net.preSharedKey, net));
            }
        }

        enableWifi();
    }

    public void connectToAP(String ssid, String passkey) {
        Log.i(TAG, "* connectToAP");

        WifiConfiguration wifiConfiguration = new WifiConfiguration();

        String networkSSID = ssid;
        String networkPass = passkey;
        List<ScanResult> scanResultList = wifiManager.getScanResults();

        Log.d(TAG, "# password " + networkPass);

        for (ScanResult result : scanResultList) {
            if (result.SSID.equals(networkSSID)) {

                String securityMode = getScanResultSecurity(result);

                if (securityMode.equalsIgnoreCase("OPEN")) {

                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    int res = wifiManager.addNetwork(wifiConfiguration);
                    Log.d(TAG, "# add Network returned " + res);

                    boolean b = wifiManager.enableNetwork(res, true);
                    Log.d(TAG, "# enableNetwork returned " + b);

                    wifiManager.setWifiEnabled(true);

                } else if (securityMode.equalsIgnoreCase("WEP")) {

                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.wepKeys[0] = "\"" + networkPass + "\"";
                    wifiConfiguration.wepTxKeyIndex = 0;
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    int res = wifiManager.addNetwork(wifiConfiguration);
                    Log.d(TAG, "### 1 ### add Network returned " + res);

                    boolean b = wifiManager.enableNetwork(res, true);
                    Log.d(TAG, "# enableNetwork returned " + b);

                    wifiManager.setWifiEnabled(true);
                }

                wifiConfiguration.SSID = networkSSID;//"\"" + networkSSID + "\"";
                wifiConfiguration.preSharedKey = "\"" + networkPass + "\"";
                wifiConfiguration.hiddenSSID = true;
                wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

                int res = wifiManager.addNetwork(wifiConfiguration);
                Log.d(TAG, "### 2 ### add Network returned " + res);

                wifiManager.enableNetwork(res, true);

                boolean changeHappen = wifiManager.saveConfiguration();

                if(res != -1 && changeHappen){
                    Log.d(TAG, "### Change happen");

                    //AppStaticVar.connectedSsidName = networkSSID;

                }else{
                    Log.d(TAG, "*** Change NOT happen");
                }

                wifiManager.setWifiEnabled(true);
            }
        }
    }
    public void updateNetList() {
        List<ScanResult> scanResultList = wifiManager.getScanResults();
        List<DeviceInfo> savedList = mAdapter.getList();
        mAdapter.resetAvailability();

        for(ScanResult result : scanResultList) {
            for(DeviceInfo conf : savedList) {
                if(conf.mConnf.SSID.contains(result.SSID)) {
                    conf.isAvailable = true;
                }
            }
        }
        mAdapter.setList(savedList);
        mAdapter.notifyDataSetChanged();
    }

    public String getScanResultSecurity(ScanResult scanResult) {
        Log.i(TAG, "* getScanResultSecurity");

        final String cap = scanResult.capabilities;
        final String[] securityModes = { "WEP", "PSK", "EAP" };

        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }

        return "OPEN";
    }

    @Override
    public void onResume() {
        super.onResume();
        mReceiver = new WifiDirectBroadcastReceiver(wifiManager, this);
        registerReceiver(mReceiver, intentFilter);
        doBindService();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        doUnbindService();
    }

    public void enableWifi() {
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);

            Toast.makeText(x, "Don't worry! I turned WIFI on for you ;)", Toast.LENGTH_LONG).show();
        } else {

            //Toast.makeText(x, "Wifi already enabled", Toast.LENGTH_LONG).show();
        }
    }

    boolean mIsBound = false;
    Messenger mService = null;
    final Messenger mMessenger = new Messenger(new MainActivity.IncomingHandler());

    @Override
    public void handleDialogClose(DialogInterface dialogInterface) {
        //Toast.makeText(x, "works", Toast.LENGTH_LONG).show();
        List<WifiConfiguration> savedNetlist = wifiManager.getConfiguredNetworks();
        mAdapter.clear();
        for(WifiConfiguration conf : savedNetlist) {
            if(conf.SSID.contains("EZRobot-")) {
                //mAdapter.addElement(conf);
                mAdapter.addElement(new DeviceInfo(conf.SSID, conf.preSharedKey, conf));
                //connectToAP(dialogInterface.);
            }
        }

        mAdapter.notifyDataSetChanged();
    }

    private void startService() {
        Intent serviceIntent = new Intent(x, CommService.class);
        x.startService(serviceIntent);
    }

    @SuppressLint("HandlerLeak")
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case CommService.MSG_STRING_VAL:
                    Toast.makeText(x, (String) msg.obj, Toast.LENGTH_LONG).show();
                    break;

                case CommService.MSG_ROBOT_BROADCAST:
                    //Toast.makeText(x, msg.obj.toString(), Toast.LENGTH_LONG).show();
                    DeviceInfo di = new DeviceInfo(msg.obj.toString());
                    di.isAvailable = true;
                    List<DeviceInfo> list = mAdapter.getList();
                    if(list.isEmpty()) {
                        mAdapter.addElement(di);
                    } else {
                        for (DeviceInfo d : list) {
                            if (!d.mSSID.contains(di.mSSID)) {
                                mAdapter.addElement(di);
                            }
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);

            try {
                Message msg = Message.obtain(null, CommService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
                //Toast.makeText(x, "Connected", Toast.LENGTH_SHORT).show();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private void doBindService() {
        bindService(new Intent(MainActivity.this, CommService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;

    }

    private void doUnbindService() {
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    public void sendTest(View view) {
        try {
            Message msg = Message.obtain(null, CommService.MSG_MOVE_ROBOT);
            msg.replyTo = mMessenger;
            //msg.obj = "MOVE,10,:";
            msg.obj = MapActivity.MSG_ROBOT_ROTATE +",10,:";
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public void mapShow(View view) {
        Intent i = new Intent(MainActivity.this, MapActivity.class);
        MainActivity.this.startActivity(i);
    }

    public void NetworkDialog(View view) {
        //Intent i = new Intent(getApplicationContext(), CustomDialogClass.class);
        //startActivity(i);
        CustomDialogClass customDialogClass = new CustomDialogClass();
        customDialogClass.show(getSupportFragmentManager(), "");

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to

            } else {
                Toast.makeText(x, "Failed to get permission", Toast.LENGTH_LONG).show();
                // Permission was denied or request was cancelled
            }
        }
    }

    public void initIntents() {
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    }
}
