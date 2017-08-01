package com.netcare.mapfactortracker;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;


public class MainActivity extends Activity {
	public final static String EXTRA_MESSAGE = "com.netcare.mapfactortracker.STATUSMSG";

    private TrackService mService;
    private boolean mBound = false;

	private NaviInterface myNavi;
	
	private HandlerThread webHandlerThread;
	public Handler webHandler;
	private final static String TAG = "MainActivity";
	private Logger logger;
    private int trackStatus = 0;
    private Messenger mMessenger;
    public static Messenger activityMsg;
    private ToggleButton btn;

    public final static int PRINT = 1;

    private class ResponseHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            int respCode = msg.what;

            switch (respCode) {
                case PRINT:
                    String response = msg.getData().getString("data");
                    logger.Log(TAG, response);
            }
        }
    }

    private boolean checkServiceRunning(Class<?> serviceClass) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    
        Authenticator.setDefault(new Authenticator() {
		     protected PasswordAuthentication getPasswordAuthentication() {
		       return new PasswordAuthentication("nick", "govno".toCharArray());
		     
		} });
        
        // Create a WebHandler right away
        TextView tv = (TextView) findViewById(R.id.LogView);
        logger = new Logger(this, tv);
        tv.setMovementMethod(new ScrollingMovementMethod());
        activityMsg = new Messenger(new ResponseHandler());

        logger.Log(TAG, "Activity created");

        btn = (ToggleButton) findViewById(R.id.TrackButton);

        if (checkServiceRunning(TrackService.class)) {
            trackStatus = 1;
        }
        else {
            trackStatus = 0;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!mBound && trackStatus != 0) {
            Intent intent = new Intent(this, TrackService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            mBound = true;
        }

        if (mBound && trackStatus == 1) {
            btn.setChecked(true);
        }
        else {
            btn.setChecked(false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
    
    public void toggleTracker(View view) {
    	if (trackStatus == 0) {
    		trackStatus = 1;

            Intent intent = new Intent(this, TrackService.class);
            startService(intent);

            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

            logger.Log(TAG, "Service started");
            logger.Log(TAG, "Activity bind to Service");
    	}
        else {
    		trackStatus = 0;

            if (mBound) {
                unbindService(mConnection);
                mBound = false;
            }

            stopService(new Intent(this, TrackService.class));

            logger.Log(TAG, "Service stop");
    	}
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mMessenger = new Messenger(iBinder);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mMessenger = null;
            mBound = false;
        }
    };
}




























