package com.segway.robot.robotsample;

import android.app.Application;
import android.content.Context;

/**
 * Created by sgs on 2017/4/18.
 */

public class MainApplication extends Application {

    static Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static Context getContext() {
        return mContext;
    }
}
