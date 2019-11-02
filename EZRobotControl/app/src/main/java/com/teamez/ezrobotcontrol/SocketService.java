package com.teamez.ezrobotcontrol;

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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SocketService extends Service {

    public static String SERVERIP = "";
    public static final int SERVERPORT = 5000;
    Socket socket;
    InetAddress serverAddr;
    ArrayList<Messenger> mClients = new ArrayList<>();
    String TAG = "Socket Service";

    static final int MSG_SET_TARGET_LOCATION = 1;
    static final int MSG_REGISTER_CLIENT = 2;
    static final int MSG_UNREGISTER_CLIENT = 3;
    static final int MSG_STRING_VAL = 4;
    static final int MSG_MOVE_ROBOT = 5;
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
        Log.d(TAG, "SocketService started");
        return START_STICKY;
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
            }

            super.handleMessage(msg);
        }
    }

    private void sendMessageToUI(String stringToSend) {
        for (int i = mClients.size()- 1; i >= 0; i--) {
            try {
                Message msg = Message.obtain(null, MSG_STRING_VAL);
                msg.obj = stringToSend;
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
                mClients.remove(i); //client is dead
            }
        }
    }

    class connectSocket implements Runnable {
        DataInputStream inputStream = null;
        DataOutputStream outputStream = null;

        @Override
        public void run() {
            if(SERVERIP.equals("server")) {
                try {
                    ServerSocket serverSocket = new ServerSocket(SERVERPORT);
                    socket = serverSocket.accept();
                    sendMessageToUI("Server");
                    Log.d(TAG, "Server socket is open");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if(!SERVERIP.equals("")) {

                try {
                    serverAddr = InetAddress.getByName(SERVERIP);
                    socket = new Socket(serverAddr, SERVERPORT);
                    sendMessageToUI("Client");
                    Log.d(TAG, "Client socket connected to server");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
                while(true) {
                    if (socket != null) {

                        try {
                            inputStream = new DataInputStream(socket.getInputStream());
                            byte[] buffer = new byte[1024];
                            int bytesToRead = inputStream.available();

                            if (bytesToRead > 0) {
                                inputStream.read(buffer, 0, bytesToRead);


                                String line = new String(buffer);
                                String[] commands = line.split(":");

                                for(String s : commands) {
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
