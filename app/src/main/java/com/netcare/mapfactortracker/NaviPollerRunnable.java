package com.netcare.mapfactortracker;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class NaviPollerRunnable implements Runnable {

    private Socket naviSocket = null;

    private PrintWriter naviWriter = null;
    private BufferedReader naviReader = null;
    private Handler webHandler;

    private String errMsg = "OK";
    public String SoftwareVersion = "";
    public String ProtocolVersion = "";
    public final static int UNKNOWN = 0;
    public final static int CONNECTED = 1;
    public final static int TRACKING = 2;

    public int state = UNKNOWN;

    public class NaviResult {
        public int Code;
        public String Text;
    }

    public NaviPollerRunnable(Handler webHandler) {
        this.webHandler = webHandler;
    }

    public void StopPoller () {
        switch (state){
            case UNKNOWN:
                break;
            case CONNECTED:
            case TRACKING:
                state = UNKNOWN;

                try {
                    naviReader.close();
                    naviWriter.close();
                    naviSocket.close();
                }
                catch (Exception e) {

                }

                state = UNKNOWN;
        }

    }

    @Override
    public void run() {
        // Main poller loop, executed via newSingleThreadScheduledExecutor()
        NaviResult result;

        //logger.Log(TAG, "naviPoller run..");
        // Work out FSM first
        switch (state) {
            case UNKNOWN:
                // We're in unknown state, navigator is down, so try to connect to it
                String shit = tryConnectNavi();
                //logger.Log(TAG, String.format("Error on Connect: %s", shit));
                break;
            case CONNECTED:
                // We usually start here, hence fill software version
                result = sendNavi("$software_version");

                if (result.Code == 0) {
                    state = TRACKING;
                    SoftwareVersion = result.Text;
                    //logger.Log(TAG, String.format("NaviPoller.run(): state=%d", state));
                }

                Log.d("LOLOL", "NAVI CONNECTED");

                break;
            case TRACKING:
                result = sendNavi("$last_position");

                //logger.Log(TAG, String.format("NaviPoller.run(): position=%s", result.Text));
                // Now parse result text into object
                Log.d("POSITION", result.Text);
                PositionData pd = new PositionData();

                if (result.Text.equals("unknown") || result.Text.equals("null") || result.Text.equals("")) {
                    pd.fix = false;
                }
                else {
                    String[] field = result.Text.split(",");

                    if (field.length != 5) {
                        pd.fix = false;
                    }
                    else {
                        pd.latitude = field[0];
                        pd.longtitude = field[1];
                        pd.altitude = field[2];
                        pd.speed = field[3];
                        pd.bearing = field[4];
                        pd.fix = true;
                    }
                }

                result = sendNavi("$navigation_statistics");

                //logger.Log(TAG, String.format("NaviPoller.run(): route_stats=%s", result.Text));

                if (result.Text.equals("not navigating") || result.Text.equals("null") || result.Text.equals("")) {
                    pd.navigating = false;
                }
                else {
                    String[] field = result.Text.split(",");

                    if (field.length != 4) {
                        pd.navigating = false;
                    }
                    else {
                        pd.wpDistance = field[0];
                        pd.wpTime = field[1];
                        pd.dtDistance = field[2];
                        pd.dtTime = field[3];
                        pd.navigating = true;
                    }
                }

                //logger.Log(TAG, String.format("NaviPoller.run(): obtainMessage", result.Text));
                Message msg = webHandler.obtainMessage();
                //logger.Log(TAG, String.format("NaviPoller.run(): sendMessage", result.Text));
                msg.what = WebTracker.TrackPosition;
                msg.obj = pd;

                webHandler.sendMessage(msg);

                /*try {
                    //this.mmMsg.send(TrackService.getMessage(TrackService.NAVI_RESP, NaviCallback.TAG + ": TRACKING.."));
                }
                catch (RemoteException e) {
                    e.printStackTrace();
                }*/

                //logger.Log(TAG, String.format("NaviPoller.run(): messageSent", result.Text));
                break;
        }
        // Message msg = Message.obtain();
        // msg.what = 999;
        // MyMap.this._handler.sendMessage(msg);
    };

    private NaviResult sendNavi(String Command) {
        // Send a command to navigator, return tuple
        // Assumes all commands return single string as a result
        // Adds \r\n automagically to commands

        NaviResult result = new NaviResult();

        // First validate the connection is here

        if (state < CONNECTED) {
            result.Code = -1;
            result.Text = "Navigator not connected";

            return result;
        }

        naviWriter.flush();
        naviWriter.print(Command + "\r\n");
        naviWriter.flush();

        if (naviWriter.checkError()) {
            // Socket is probably gone..
            state = UNKNOWN;

            try {
                naviReader.close();
                naviWriter.close();
                naviSocket.close();
            }
            catch (Exception e) {

            }

            state = UNKNOWN;
            result.Code = -1;
            result.Text = "Socket closed, returning to UNKNOWN";
        }

        try {
            //logger.Log(TAG, "Before RL");
            result.Text = naviReader.readLine();
            //logger.Log(TAG, "After RL");
            result.Code = 0;

            // return success
            return result;
        }
        catch (Exception e) {
            result.Text = e.getMessage();
            result.Code = -1;
            //logger.Log(TAG, String.format("sendNavi(): Read Error = %s", result.Text) );

            return result;
        }


    }

    private String tryConnectNavi() {
        // Attempts to connect to Navigator and query its status
        //logger.Log(TAG, "tryConnectNavi(): Socket Open" );
        try {
            naviSocket = new Socket("127.0.0.1", 4242);
            naviSocket.setSoTimeout(1000);

        } catch (UnknownHostException e) {
            errMsg = "Host not found!";
            return errMsg;

        } catch (IOException e) {
            errMsg = e.getMessage();
            //logger.Log(TAG, String.format("TryConnectNavi(): socketError=%s", errMsg));
            return errMsg;
        }

        //logger.Log(TAG, "tryConnectNavi(): ReadersWriters Open" );

        try {
            naviWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(naviSocket.getOutputStream())), true);
            naviReader = new BufferedReader(new InputStreamReader(naviSocket.getInputStream()));
        } catch (IOException e) {
            errMsg = e.getMessage();
            //logger.Log(TAG, String.format("TryConnectNavi(): bufferError=%s", errMsg));

            try {
                naviSocket.close();
            } catch (IOException x) {

            }

            return errMsg;
        }

        state = CONNECTED;

        //logger.Log(TAG, "tryConnectNavi(): Protocol Version" );

        NaviResult result = sendNavi("$protocol_version");

        if (result.Code == 0) {
            state = CONNECTED;
            ProtocolVersion = result.Text;
            //logger.Log(TAG, String.format("tryConnectNavi(): Exit with Success = %s",ProtocolVersion) );
            // return success
            return "OK";
        } else {
            state = UNKNOWN;
            //logger.Log(TAG, String.format("tryConnectNavi(): Exit with Failure = %s",result.Text) );

            return result.Text;
        }
    }
}
