package com.teamez.ezrobotac;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class CommService extends Service {

    public static String SERVERIP = "";
    public static final int SERVERPORT = 5000;
    Socket socket;
    InetAddress serverAddr;
    ArrayList<Messenger> mClients = new ArrayList<>();
    String TAG = "Socket Service";
    Boolean socketStop = false;

    DatagramSocket UDPSocket;

    static final int MSG_SET_TARGET_LOCATION = 1;
    static final int MSG_REGISTER_CLIENT = 2;
    static final int MSG_UNREGISTER_CLIENT = 3;
    static final int MSG_STRING_VAL = 4;
    static final int MSG_MOVE_ROBOT = 5;
    static final int MSG_ROBOT_BROADCAST = 6;
    static final int MSG_REINIT_CONN = 0;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    List<String> outQueue = new ArrayList<>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Runnable connect = new connectSocket();
        new Thread(connect).start();
        Runnable broadcastReceiver = new listenBroadcast();
        new Thread(broadcastReceiver).start();
        Log.d(TAG, "CommService started");
        return START_STICKY;
    }

    public void setConnectionThread(String ip) {
        socketStop = !SERVERIP.equals("");
        while (socketStop || socket != null);
        SERVERIP = ip;
    }

    @SuppressLint("HandlerLeak")
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_SET_TARGET_LOCATION:
                    outQueue.add((String)msg.obj);
                    break;
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_MOVE_ROBOT:
                    outQueue.add((String)msg.obj);
                    break;
                case MSG_REINIT_CONN:
                    setConnectionThread(msg.obj.toString());
                    //sendMessageToUI("got message");
            }

            super.handleMessage(msg);
        }
    }

    private void sendMessageToUI(String stringToSend) {
        for (int i = mClients.size()- 1; i >= 0; i--) {
            try {
                Message msg;
                if(stringToSend.startsWith("#")) {
                    msg = Message.obtain(null, MSG_ROBOT_BROADCAST);
                    msg.obj = stringToSend.substring(1);
                } else {
                    msg = Message.obtain(null, MSG_STRING_VAL);
                    msg.obj = stringToSend;
                }
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
                mClients.remove(i); //client is dead
            }
        }
    }

    class listenBroadcast implements Runnable{
        byte[] recvbuf = new byte[1024];

        @Override
        public void run() {
            try {
                UDPSocket = new DatagramSocket(37020);
                //UDPSocket.setBroadcast(true);
                //UDPSocket.bind(new InetSocketAddress( 37020));
                Log.d(TAG, "UDP Listener started");
                while(true) {
                    DatagramPacket packet = new DatagramPacket(recvbuf, recvbuf.length);
                    UDPSocket.receive(packet);
                    String s = "#" + packet.getAddress().getHostAddress()
                            + "," + new String(packet.getData(), 0, packet.getLength());
                    sendMessageToUI(s);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class connectSocket implements Runnable {
        DataInputStream inputStream = null;
        DataOutputStream outputStream = null;

        void stop() {
            if(socket != null && !socket.isClosed()) {
                try {
                    SERVERIP = "";
                    socket.close();
                    socket = null;
                    socketStop = false;
                    Log.d(TAG, "Connection socket closed");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "Connection socket already closed");
            }

        }

        @Override
        public void run() {
            while(true) {
                if(socket == null && !SERVERIP.equals("")) {
                    try {
                        serverAddr = InetAddress.getByName(SERVERIP);
                        socket = new Socket(serverAddr, SERVERPORT);
                        //sendMessageToUI("Client");
                        Log.d(TAG, "Client socket connected to server");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (socket != null && socketStop) {
                    stop();
                } else if(socket != null) {
                    try {
                        inputStream = new DataInputStream(socket.getInputStream());
                        byte[] buffer = new byte[1024];
                        int bytesToRead = inputStream.available();
                        if (bytesToRead > 0) {
                            inputStream.read(buffer, 0, bytesToRead);
                            String line = new String(buffer);
                            String[] commands = line.split(":");

                            for (String s : commands) {
                                sendMessageToUI(s);
                                Log.d(TAG, "Msg received");
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                    try {
                        outputStream = new DataOutputStream(socket.getOutputStream());
                        if (outQueue.size() != 0) {
                            for (String s : outQueue) {
                                outputStream.write(s.getBytes(), 0, s.getBytes().length);
                                Log.d(TAG, "Msg sent");
                            }
                            outQueue.clear();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

