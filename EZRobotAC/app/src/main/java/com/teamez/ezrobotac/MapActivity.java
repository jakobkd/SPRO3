package com.teamez.ezrobotac;


import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.res.ResourcesCompat;

public class MapActivity extends AppCompatActivity {

    AppCompatImageView robotImage;
    AppCompatImageView mMap;
    AppCompatImageView mMapMarker;
    RelativeLayout map_layout;

    private Paint mPaint = new Paint();
    protected Bitmap mBitmap;
    protected Bitmap mBitmapMarker;
    public Point mSize = new Point();

    private RobotPosition mRobotPosition;
    private Point clickCoords = new Point();

    MapActivity x = this;

    View.OnClickListener mapClickListener;
    View.OnTouchListener mapTouchListener;

    final static int MSG_ROBOT_MOVE = 1;
    final static int MSG_ROBOT_ROTATE = 2;
    final static int MSG_ROBOT_TARGET = 3;

    boolean mIsBound = false;
    Messenger mService = null;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @SuppressLint("HandlerLeak")
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case CommService.MSG_STRING_VAL:

                    String[] strArray = msg.obj.toString().split(",");
                    String[] commands = new String[strArray.length];
                    for (int i = 0; i < strArray.length; i++) {
                        commands[i] = strArray[i].replaceAll("[^\\d.]", "");
                    }

                    if(commands.length > 0) {
                        float caseInt = 0;
                        try {
                            //Toast.makeText(x, (String)msg.obj, Toast.LENGTH_SHORT).show();
                            caseInt = Float.parseFloat(commands[0]);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    switch ((int)caseInt) {
                        case MSG_ROBOT_MOVE:
                            float dist = Float.parseFloat(commands[1]);
                            //robotImage.setX(robotImage.getX()-dist);
                            robotImage.setY(robotImage.getY() - dist);
                            break;
                        case MSG_ROBOT_ROTATE:
                            float angle = Float.parseFloat(commands[1]);
                            robotImage.setRotation(robotImage.getRotation() + angle);
                            break;
                        case MSG_ROBOT_TARGET:
                            float level = Float.parseFloat(commands[1]);

                            if(level == 0) {
                                robotImage.setX(mSize.x/2f-robotImage.getWidth()/2f);
                                robotImage.setY(((float) mSize.y / 7) * 6f - robotImage.getHeight()/2f);

                            }

                            if(level == 1) {

                                robotImage.setY((((float)mSize.y/7)*4.5f + 30)- robotImage.getHeight()/2f);
                                float percent = Float.parseFloat(commands[2]);
                                float lineLength = mRobotPosition.getPosition(1,4).x - mRobotPosition.getPosition(1, 1).x;
                                robotImage.setX(mRobotPosition.getPosition(1,1).x+lineLength*percent - robotImage.getWidth()/2f);
                            }

                            if(level == 2) {
                                float x = Float.parseFloat(commands[2]);
                                float percent = Float.parseFloat(commands[3]);
                                robotImage.setX((((float) mSize.x / 8f) * (1f + (x - 1) * 2))- robotImage.getWidth()/2f);
                                float lineLength = mRobotPosition.getPosition(1,1).y - mRobotPosition.getPosition(2,1).y;
                                robotImage.setY((((float)mSize.y/7)*1f)+lineLength*percent-robotImage.getHeight()/2f);

                            }

                            break;
                    }

                    }
                    //Toast.makeText(x, (String) msg.obj, Toast.LENGTH_LONG).show();
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
            //Toast.makeText(x, "Connected", Toast.LENGTH_SHORT).show();
            try{
                Message msg = Message.obtain(null, CommService.MSG_REGISTER_CLIENT);
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
        bindService(new Intent(MapActivity.this, CommService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;

    }

    private void doUnbindService() {
        if(mIsBound) {
            unbindService(mConnection);

        }
    }


    public MapActivity() {
        mapTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    clickCoords.x = (int)event.getX();
                    clickCoords.y = (int)event.getY();

                }
                return false;
            }
        };
        mapClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(x, "X:" + clickCoords.x + " Y: " + clickCoords.y, Toast.LENGTH_SHORT).show();

                if(clickCoords.y >= mRobotPosition.getPosition(2,1).y && clickCoords.y <= mRobotPosition.getPosition(1, 1).y)  {
                    for(int i = 1; i < 5; i++) {
                        if((int)(((float) mSize.x/8f) *(1f + (i-1)*2)) - 50 <= clickCoords.x && (int)(((float) mSize.x/8f) *(1f + (i-1)*2)) + 50 >= clickCoords.x) {
                            mPaint.setStyle(Paint.Style.STROKE);
                            drawMarker(new Point((int)(((float) mSize.x/8f) *(1f + (i-1)*2)), clickCoords.y));
                            try {
                                float len = (((float) mSize.y / 7f) * 4.5f + 30 - (((float) mSize.y / 7) * 1f));
                                float relativePosition = (clickCoords.y-(((float) mSize.y / 7) * 1f))/len;
                                Message msg = Message.obtain(null, CommService.MSG_SET_TARGET_LOCATION);
                                msg.replyTo = mMessenger;
                                //msg.obj = "MOVE,10,:";
                                msg.obj = MSG_ROBOT_TARGET + "," + "2," +
                                        i + "," +
                                        relativePosition +":";
                                mService.send(msg);

                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                            //send location to Raspberry Pi
                        }
                    }
                } else if(clickCoords.y >= (int)(((float)mSize.y/7f) *4.5f + 30) - 50
                        && clickCoords.y <= (int)(((float)mSize.y/7f) *4.5f + 30) + 50
                        && clickCoords.x >= (int)(((float) mSize.x/8f) *1f) - 50
                        && clickCoords.x <= (int)(((float) mSize.x/8f) *(1f + (4-1)*2)) + 50) {
                    float len = (((float) mSize.x/8f)*(1f + (4-1)*2)) - (((float) mSize.x /8f) * 1f);
                    float relativePosition = (clickCoords.x-(((float) mSize.x / 8f) * 1f))/len;
                    Point p = new Point(clickCoords.x, (int)(((float)mSize.y/7) *4.5f +30));
                    drawMarker(p);
                    try {
                        Message msg = Message.obtain(null, CommService.MSG_SET_TARGET_LOCATION);
                        msg.replyTo = mMessenger;
                        //msg.obj = "MOVE,10,:";
                        msg.obj = MSG_ROBOT_TARGET  + "," +
                                "1," +
                                relativePosition +":";
                        mService.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                }

            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        map_layout = findViewById(R.id.map_layout);
        mMap = findViewById(R.id.map);
        mMapMarker = findViewById(R.id.mapMarker);
        robotImage = findViewById(R.id.robot);

        map_layout.setClickable(true);
        map_layout.setOnTouchListener(mapTouchListener);
        map_layout.setOnClickListener(mapClickListener);

        //wait for containers to be initialized to get correct width
        map_layout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout() {
                map_layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mSize.x = map_layout.getWidth();
                mSize.y = map_layout.getHeight();
                mRobotPosition = new RobotPosition(mSize);

                drawMap();
            }
        });
        doBindService();

    }



    public void drawMarker(Point coord) {

        mBitmapMarker = Bitmap.createBitmap(mSize.x, mSize.y, Bitmap.Config.ARGB_8888);
        mMapMarker.setImageBitmap(mBitmapMarker);
        Canvas mRobotMarker = new Canvas(mBitmapMarker);

        mPaint.setStrokeWidth(7);
        mPaint.setColor(ResourcesCompat.getColor(getResources(), R.color.colorBlack, null));
        mPaint.setStyle(Paint.Style.STROKE);
        mRobotMarker.drawCircle(coord.x, coord.y, robotImage.getWidth()/2, mPaint);
    }

    public void drawMap () {


        mBitmap = Bitmap.createBitmap(mSize.x, mSize.y, Bitmap.Config.ARGB_8888);
        mMap.setImageBitmap(mBitmap);
        Canvas mCanvas = new Canvas(mBitmap);
        mPaint.setColor(ResourcesCompat.getColor(getResources(), R.color.colorAsphaltGrey, null));
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(20);

        //Base lines
        mCanvas.drawRect((float)mSize.x/2f-30,
                ((float)mSize.y/7f) *4.5f,
                (float)mSize.x/2f+30,
                ((float)mSize.y/7f) *6f,
                mPaint );

        mCanvas.drawRect(((float)mSize.x/8f) *1f -30,
                ((float)mSize.y/7f) *4.5f + 60,
                ((float)mSize.x/8f) *7f+30,
                ((float)mSize.y/7f) *4.5f,
                mPaint);

        //Sub lines
        for(int i = 1; i < 5; i++)
        {
            mCanvas.drawRect((int)(((float) mSize.x/8f) *(1f + (i-1)*2)) -30,
                    (int)(((float)mSize.y/7) *1f),
                    (int)(((float) mSize.x/8f) *(1f + (i-1)*2)) +30,
                    (int)(((float)mSize.y/7) *4.5f + 30),
                    mPaint);
        }

        //reset robot position
        robotImage.setX((float)mSize.x/2f- (float) robotImage.getWidth()/2);
        robotImage.setY(((float)mSize.y/7f) * 6f- (float) robotImage.getHeight()/2);
    }

    public void closeMap (View view) {
        this.finish();
    }

    public void homeRobot (View view) {
        if(!(robotImage.getX() + robotImage.getWidth() / 2f == (float) mSize.x / 2f
                && robotImage.getY() + robotImage.getHeight() / 2f == (float) mSize.y / 2f))
        {
            drawMarker(new Point((int) ((float) mSize.x / 2), (int) (((float) mSize.y / 7) * 6f)));
            try {
                Message msg = Message.obtain(null, CommService.MSG_SET_TARGET_LOCATION);
                msg.replyTo = mMessenger;
                //msg.obj = "MOVE,10,:";
                msg.obj = MSG_ROBOT_TARGET  + "," +
                        "0" +":";
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    public void animation (View view) {

        float positionY = robotImage.getY();
        float positionX = robotImage.getX();
        float rotation = robotImage.getRotation();

        robotImage.setPivotX(robotImage.getWidth()/2);
        robotImage.setPivotY(robotImage.getHeight()/2);

        if(positionY > mRobotPosition.getPosition(1, 0).y - (float)robotImage.getHeight()/2) {
            robotImage.setY(positionY - 10);
        } else if (positionY < mRobotPosition.getPosition(1, 0).y - (float)robotImage.getHeight()/2){
            robotImage.setY(positionY + 10);
        }
        else if(positionY == mRobotPosition.getPosition(1,0).y - (float)robotImage.getHeight()/2 && -90 < rotation && rotation <= 0 ) {
            robotImage.setRotation(rotation - 10);
        } else if (positionX >= mRobotPosition.getPosition(1,2).x  - (float) robotImage.getWidth()/2 && rotation == -90) {
            robotImage.setX(positionX - 10);
        }
    }
}

