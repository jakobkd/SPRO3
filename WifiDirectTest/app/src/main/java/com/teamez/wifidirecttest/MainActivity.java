package com.teamez.wifidirecttest;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    com.google.android.material.textfield.TextInputEditText ipInput;
    TextView localIPLabel;
    ToggleButton toggleButton;
    boolean server = false;
    public static final int SERVERPORT = 8080;
    String localIP;

    ServerSocket serverSocket;
    Handler handler = new Handler();
    MainActivity x = this;
    String TAGserver = "ServerActivity";


    private boolean connected = false;
    String serverIP = "";
    String TAGclient = "ClientActivity";


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
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

    static final int MESSAGE_READ = 1;

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    public void onResume() {
        super.onResume();


        mReceiver = new WifiDirectBroadcastReceiver(P2pManager, channel, this);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
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
                    Toast.makeText(x, "No devices found", Toast.LENGTH_LONG).show();
                }
                mAdapter.notifyDataSetChanged();

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ipInput = findViewById(R.id.edit_ip);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        localIPLabel = findViewById(R.id.localIp_label);
        toggleButton = findViewById(R.id.server_toggle);
        final Thread fst = new Thread(new ServerThread());

        mLayoutManager = new LinearLayoutManager(this);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    toggleButton.setTextOn("Server");
                    server = true;
                    //fst.start();
                } else {
                    toggleButton.setTextOff("Client");
                    server = false;
                    //fst.interrupt();
                }
            }
        });


        AppCompatButton connectButton = findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!connected) {
                    serverIP = ipInput.getText().toString();
                    if(!serverIP.equals("")) {
                        Thread cThread = null;
                        try {
                            cThread = new Thread(new ClientThread(InetAddress.getByName(serverIP)));
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                        cThread.start();
                    }
                }
            }
        });

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);

        localIP = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        localIPLabel.setText(localIP);

        mRecyclerView = findViewById(R.id.device_list);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new DeviceListAdapter(this, R.layout.device_list_item, list, mRecyclerView);
        mRecyclerView.setAdapter(mAdapter);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        assert wifiManager != null;
        if(wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            if(wifiInfo.getNetworkId() != -1) {
                wifiManager.disableNetwork(wifiInfo.getNetworkId());
                wifiManager.disconnect();
            }
        }


        P2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        assert P2pManager != null;
        channel = P2pManager.initialize(this, getMainLooper(), null);

        mReceiver = new WifiDirectBroadcastReceiver(P2pManager, channel, this);
        init_Intents();
        P2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(x, "Group removed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }

    public void connectDevice(View view, final DeviceInfo mDeviceInfo) {

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = mDeviceInfo.deviceAddress;
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
            Thread comThread;
            if (info.groupFormed && info.isGroupOwner) {
                //serverClass = new ServerClass();
                //serverClass.start();
                comThread = new Thread(new ServerThread());
                comThread.start();

                //You are host
            } else if (info.groupFormed) {
                //You are client
                //clientClass = new ClientClass(groupOwnerAddress);
                //clientClass.start();

                comThread = new Thread(new ClientThread(groupOwnerAddress));
                comThread.start();
            }
        }
    };

    public void discoverPeers(View view) {

        if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method

        }else{
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
            if(grantResults.length == 1
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


    public class ServerThread implements Runnable {
        @Override
        public void run() {
            try {
                if(localIP != null) {
                    serverSocket = new ServerSocket(SERVERPORT);
                    while(true) {
                        if(Thread.interrupted())
                            throw new InterruptedException();
                        Socket client = serverSocket.accept();

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(x, "Connected", Toast.LENGTH_SHORT).show();
                            }
                        });

                        try {
                            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                            String line = null;
                            while ((line = in.readLine()) != null) {

                                Log.d(TAGserver, line);
                                final String finalLine = line;
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(x, finalLine, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            break;
                    } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Log.d(TAGserver, "No connection detected");
                }
            } catch (IOException e) {
                    e.printStackTrace();
            } catch (InterruptedException e) {
                Log.d(TAGserver, "server stopped");
            }

        }
    }

    public class ClientThread implements Runnable{
        InetAddress serverAddr;
        ClientThread(InetAddress serverAddress) {
            serverAddr = serverAddress;
        }
        @Override
        public void run() {
            try{
                //InetAddress serverAddr = InetAddress.getByName(serverIP);
                if(serverAddr == null)
                    return;
                Socket socket = new Socket(serverAddr, MainActivity.SERVERPORT);
                connected = true;
                if(connected) {
                    try {
                        Log.d(TAGclient, "sending command");
                        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                        out.println("hola");
                        Log.d(TAGclient, "Command sent");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if(socket.isConnected())
                socket.close();
                Log.d(TAGclient, "socket closed");
            } catch (Exception e) {
                Log.e(TAGclient, "C: Error");
                connected = false;
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
