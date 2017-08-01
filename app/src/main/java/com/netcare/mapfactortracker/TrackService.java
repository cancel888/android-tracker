package com.netcare.mapfactortracker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class TrackService extends Service {

    private NotificationManager mNM;
    private int NOTIFICATION = R.string.service_run;
    private Handler mNaviHandler;
    private Handler mWebHandler;
    private HandlerThread mNaviThread;
    private HandlerThread mWebThread;

    public final static int PIT_STOP = 1;
    public final static int END_PIT_STOP = 2;
    public final static int NAVI_RESP = 3;
    public final static int WEB_RESP = 4;

    public Messenger mMsg;

    private String response;

    public TrackService() {

    }

    public static Message getMessage(int code, String resp) {
        Message msg = Message.obtain(null, code);
        Bundle bData = new Bundle();
        bData.putString("data", resp);
        msg.setData(bData);

        return msg;
    }

    private class TrackHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            int msgType = msg.what;

            switch (msgType) {
                case PIT_STOP:

                    break;
                case END_PIT_STOP:

                    break;
                case NAVI_RESP:
                    response = msg.getData().getString("data");

                    try {
                        if (MainActivity.activityMsg != null) {
                            MainActivity.activityMsg.send(getMessage(MainActivity.PRINT, response));
                        }
                    }
                    catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case WEB_RESP:
                    response = msg.getData().getString("data");

                    try {
                        if (MainActivity.activityMsg != null) {
                            MainActivity.activityMsg.send(getMessage(MainActivity.PRINT, response));
                        }
                    }
                    catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    break;
            }
        }
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showNotification();
    }

    @Override
    public void onDestroy() {
        mNM.cancel(NOTIFICATION);

        mNaviHandler.sendEmptyMessage(NaviCallback.STOP_NAVI);

        mNaviThread.quitSafely();
        mWebThread.quitSafely();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mMsg = new Messenger(new TrackHandler());

        try {
            if (MainActivity.activityMsg != null) {
                MainActivity.activityMsg.send(getMessage(MainActivity.PRINT, "Service created"));
            }
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }

        mNaviThread = new HandlerThread("NaviThread");
        mNaviThread.start();

        mWebThread = new HandlerThread("WebThread");
        mWebThread.start();

        mWebHandler = new Handler(mWebThread.getLooper(), new WebCallback(this));
        mNaviHandler = new Handler(mNaviThread.getLooper(), new NaviCallback(this, mWebHandler));

        mWebHandler.sendEmptyMessage(WebCallback.StartTracking);
        mNaviHandler.sendEmptyMessage(NaviCallback.START_NAVI);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {


        return mMsg.getBinder();
    }

    private void showNotification() {

        CharSequence text = getText(R.string.service_run);

        Notification notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        notification.setLatestEventInfo(this, getText(R.string.app_name), text, contentIntent);

        mNM.notify(NOTIFICATION, notification);
    }
}
























