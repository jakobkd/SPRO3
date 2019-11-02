package com.teamez.ezrobotcontrol;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION = 2;
    List<DeviceInfo> list = new ArrayList<>();
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    public DeviceListAdapter mAdapter;

    private final IntentFilter intentFilter = new IntentFilter();
    WifiP2pManager.Channel channel;
    WifiP2pManager P2pManager;

    WifiManager wifiManager;
    BroadcastReceiver mReceiver;

    List<WifiP2pDevice> peers = new ArrayList<>();

    MainActivity x = this;
    String TAG = "Main Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mLayoutManager = new LinearLayoutManager(this);

        mRecyclerView = findViewById(R.id.network_list);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new DeviceListAdapter(this, R.layout.device_list_item, list, mRecyclerView);
        mRecyclerView.setAdapter(mAdapter);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        assert wifiManager != null;
        if (wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            if (wifiInfo.getNetworkId() != -1) {
                wifiManager.disableNetwork(wifiInfo.getNetworkId());
                wifiManager.disconnect();
            }
        }

        P2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        assert P2pManager != null;
        channel = P2pManager.initialize(this, getMainLooper(), null);

        mReceiver = new WifiDirectBroadcastReceiver(P2pManager, channel, this);
        init_Intents();
        enableWifi();

        P2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //Toast.makeText(x, "Group removed", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "P2P group removed");
            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }


    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peersList) {

            if (!peersList.getDeviceList().equals(peers)) {
                peers.clear();
                mAdapter.clear();
                peers.addAll(peersList.getDeviceList());

                for (WifiP2pDevice device : peersList.getDeviceList()) {
                    DeviceInfo di = new DeviceInfo(device.deviceName, device.deviceAddress);
                    mAdapter.addElement(di);
                }
                if (peers.size() == 0) {
                    //Toast.makeText(x, "No devices found", Toast.LENGTH_LONG).show();
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mReceiver = new WifiDirectBroadcastReceiver(P2pManager, channel, this);
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

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case SocketService.MSG_STRING_VAL:
                    Toast.makeText(x, (String) msg.obj, Toast.LENGTH_LONG).show();
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);
            //Toast.makeText(x, "Connected", Toast.LENGTH_SHORT).show();
            try {
                Message msg = Message.obtain(null, SocketService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
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
        bindService(new Intent(MainActivity.this, SocketService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;

    }

    private void doUnbindService() {
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
    }


    public void connectDevice(View view, final DeviceInfo mDeviceInfo) {

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = mDeviceInfo.deviceAddress;
        //config.groupOwnerIntent = 0; //make phone client
        config.wps.setup = WpsInfo.PBC;

        P2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(x, "Connected to " + mDeviceInfo.deviceName, Toast.LENGTH_LONG).show();
                mDeviceInfo.setConnected(true);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(x, "Connection failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            final InetAddress groupOwnerAddress = info.groupOwnerAddress;

            if (info.groupFormed && info.isGroupOwner) {
                SocketService.SERVERIP = "server";
                //You are host
            } else if (info.groupFormed) {
                //You are client
                SocketService.SERVERIP = groupOwnerAddress.getHostAddress();
            }
            startService(new Intent(MainActivity.this, SocketService.class));
            doBindService();
        }
    };

    public void sendTest(View view) {
        try {
            Message msg = Message.obtain(null, SocketService.MSG_MOVE_ROBOT);
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

    public void discoverPeers(View view) {
        enableWifi();
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method

        } else {
            P2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(x, "Discovery Started", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(x, "Discovery Failed", Toast.LENGTH_LONG).show();
                }
            });
            //do something, permission was previously granted; or legacy device
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
                P2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(x, "Discovery Started", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(x, "Discovery Failed", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(x, "Failed to get permission", Toast.LENGTH_LONG).show();
                // Permission was denied or request was cancelled
            }
        }
    }

    public void init_Intents() {
        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }
}