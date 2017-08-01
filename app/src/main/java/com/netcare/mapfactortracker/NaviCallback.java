package com.netcare.mapfactortracker;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NaviCallback implements Handler.Callback, LocationListener {

    public final static int START_NAVI = 1;
    public final static int STOP_NAVI = 0;

    public final static String TAG = "NaviCallback";

    private ScheduledExecutorService exec;
    private NaviPollerRunnable poller;
    private Handler webhandler;
    private LocationManager mLocationManager;
    private LocationListener mLL;
    private TrackService mTrack;

    public NaviCallback(TrackService service, Handler webHandler) {
        this.webhandler = webHandler;
        this.mTrack = service;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case START_NAVI:
                mLocationManager = (LocationManager) mTrack.getSystemService(Context.LOCATION_SERVICE);
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);

                try {
                    this.mTrack.mMsg.send(TrackService.getMessage(TrackService.NAVI_RESP, TAG + ": Navi connected!"));
                }
                catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;

            case STOP_NAVI:
                mLocationManager.removeUpdates(this);
                mLocationManager = null;

                break;

        }

        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            this.mTrack.mMsg.send(TrackService.getMessage(TrackService.NAVI_RESP, TAG
                    + ": " + location.getLatitude()
                    + " " + location.getLongitude()));
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }

        PositionData pd = new PositionData();

        pd.latitude = Double.toString(location.getLatitude());
        pd.longtitude = Double.toString(location.getLongitude());
        pd.altitude = Double.toString(location.getAltitude());
        pd.bearing = Double.toString(location.getBearing());
        pd.speed = Double.toString(location.getSpeed());

        Message msg = webhandler.obtainMessage();
        msg.what = WebCallback.TrackPosition;
        msg.obj = pd;

        webhandler.sendMessage(msg);
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }
}






























