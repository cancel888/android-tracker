package com.netcare.mapfactortracker;

import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebCallback implements Handler.Callback {

    public final static int StartTracking = 0;
    public final static int StopTracking = 1;
    public final static int TrackPosition = 2;
    public final static int TrackPitStart = 3;
    public final static int TrackPitEnd = 4;
    public final static int SendFreeText = 5;
    private final static String TAG = "WebCallback";
    private TrackService mService;

    public WebCallback(TrackService service) {
        this.mService = service;
    }

    public static InputStream getInputStreamFromUrl(String url) {
        InputStream content = null;
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(new HttpGet(url));
            content = response.getEntity().getContent();
        } catch (Exception e) {
            Log.d("[GET REQUEST]", "Network exception", e);
        }
        return content;
    }

    public static boolean SubmitUrl(String iurl) {
        HttpURLConnection urlConnection = null;
        URL url = null;

        try {
            url = new URL(iurl);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            return true;
        }
        catch (Exception e) {

            return false;
        }
        finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

    }
    @Override
    public boolean handleMessage(Message msg) {
        // Messenger Thread, accepts input from Navigator and Main Threads
        //logger.Log(TAG, String.format("WebTracker.handleMessage(): msg=%s", msg));

        switch (msg.what) {
            case StartTracking:
                Log.d("LOLOL", "START TRACK");
                //logger.Log(TAG, "WebTracker.StartTracking");
                break;

            case StopTracking:
                Log.d("LOLOL", "STOP TRACK");
                //logger.Log(TAG, "WebTracker.StopTracking");
                break;

            case TrackPosition:
                //logger.Log(TAG, "WebTracker.TrackPosition");
                Log.d("LOLOL", "TRACKING..");

                PositionData pd = (PositionData) msg.obj;

                String url = "http://track.freenet.ru/setUserPosition.php?user=nick&"
                        + "latitude=" + pd.latitude
                        + "&longitude=" + pd.longtitude
                        + "&alt=" + pd.altitude
                        + "&dir=" + pd.bearing
                        + "&speed=" + pd.speed;

                String message;

                if (SubmitUrl(url)) {
                    Log.d("WEB_CALLBACK", "TRACKING DONE");
                    message = "TRACKING DONE";
                }
                else {
                    Log.d("WEB_CALLBACK", "TRACKING ERR");
                    message = "TRACKING ERROR";
                }

                try {
                    this.mService.mMsg.send(TrackService.getMessage(TrackService.WEB_RESP, TAG + message));
                }
                catch (RemoteException e) {
                    e.printStackTrace();
                }

                break;

            case TrackPitStart:
                //logger.Log(TAG, "WebTracker.TrackPitStart");
                break;

            case TrackPitEnd:
                //logger.Log(TAG, "WebTracker.TrackPitEnd");
                break;

            case SendFreeText:
                //logger.Log(TAG, "WebTracker.SendFreeText");
                break;

        }

        return true;
    }
}
