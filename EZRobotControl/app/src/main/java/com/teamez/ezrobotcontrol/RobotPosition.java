package com.teamez.ezrobotcontrol;

import android.graphics.Point;

class RobotPosition extends Point {
    private Point mSize;

    RobotPosition(Point size){
        mSize = size;
    }


    Point getPosition(int level, int line) {
        Point p = new Point();
        if(level == 0) {
            p.x = mSize.x/2;
            p.y = (int)((float)(mSize.y/7)*6.5f);
        return p;
    } else {
        if(line == 0) {
            p.x = mSize.x/2;
        }
        if(line == 1) {
            p.x = (int)(((float) mSize.x/8f)*1f);
        } else {
            p.x = (int)(((float) mSize.x/8f)*(1f + (line-1)*2));
        }

        if (level == 1) {
            p.y = (int)(((float)mSize.y/7)*4.5f + 30);
        }
        if (level == 2) {
            p.y = (int)(((float)mSize.y/7)*1f);
        }
    }
    return p;
    }



}
