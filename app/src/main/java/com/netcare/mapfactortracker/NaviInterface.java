package com.netcare.mapfactortracker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;

//MapFactor Navigator Interface Class
//Creates and runs a thread communicating with Navi over TCPIP

public class NaviInterface {

	public Handler naviHandler;
	private HandlerThread naviHandlerThread;
	private Handler webHandler;
	private String errMsg = "OK";
    private String comm_version ="";
    private ScheduledExecutorService exec;
	public int state = UNKNOWN;
	public String SoftwareVersion = "";
	public String ProtocolVersion = "";
	
	private final static String TAG = "NaviHandler";
	public final static int StartNavi = 1;
	public final static int StopNavi = 0;
	public final static int UNKNOWN = 0;
	public final static int CONNECTED = 1;
	public final static int TRACKING = 2;
	
	private Logger logger = null;
	
	public class NaviResult {
		public int Code;
		public String Text;
	}
	
	
        // protected void onPostExecute(String result) 
        	// TextView statusView = (TextView) findViewById(R.id.statusView);
        	// statusView.setText(result);

    private class NaviListener implements Callback {
        // MapFactor Navigator Monitoring Thread
        
    	private NaviPoller poller = null;
    	 
    	private class NaviPoller implements Runnable {
    	    // Navigator IP Socket
    		private Socket naviSocket = null;

    		private PrintWriter naviWriter = null;
    		private BufferedReader naviReader = null;
 
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
        			catch (Exception e)
        			{
        			}
        			state = UNKNOWN;
        		}
 
    		}

    		public void run() {
				// Main poller loop, executed via newSingleThreadScheduledExecutor()
				NaviResult result;
				
				logger.Log(TAG, "naviPoller run..");
				// Work out FSM first
				switch (state) {
				case UNKNOWN:
					// We're in unknown state, navigator is down, so try to connect to it
					String shit = tryConnectNavi();
					logger.Log(TAG, String.format("Error on Connect: %s", shit));
					break;
				case CONNECTED:
					// We usually start here, hence fill software version
					result = sendNavi("$software_version");

					if (result.Code == 0) {
						state = TRACKING;
					    SoftwareVersion = result.Text;
					    logger.Log(TAG, String.format("NaviPoller.run(): state=%d", state));
					}
					break;
				case TRACKING:
					result = sendNavi("$last_position");

					logger.Log(TAG, String.format("NaviPoller.run(): position=%s", result.Text));
					// Now parse result text into object
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

					logger.Log(TAG, String.format("NaviPoller.run(): route_stats=%s", result.Text));
					
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
					
					logger.Log(TAG, String.format("NaviPoller.run(): obtainMessage", result.Text));
					Message msg = webHandler.obtainMessage();
					logger.Log(TAG, String.format("NaviPoller.run(): sendMessage", result.Text));
		    	    msg.what = WebTracker.TrackPosition;
		    	    msg.obj = pd;

		    	    webHandler.sendMessage(msg);

		    	    logger.Log(TAG, String.format("NaviPoller.run(): messageSent", result.Text));
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
                    logger.Log(TAG, "Before RL");
                    result.Text = naviReader.readLine();
                    logger.Log(TAG, "After RL");
                    result.Code = 0;

                    // return success
                    return result;
                }
                catch (Exception e) {
                    result.Text = e.getMessage();
                    result.Code = -1;
                    logger.Log(TAG, String.format("sendNavi(): Read Error = %s", result.Text) );

                    return result;
                }


            }

            private String tryConnectNavi() {
                // Attempts to connect to Navigator and query its status
                logger.Log(TAG, "tryConnectNavi(): Socket Open" );
                try {
                    naviSocket = new Socket("127.0.0.1", 4242);
                    naviSocket.setSoTimeout(1000);

                } catch (UnknownHostException e) {
                    errMsg = "Host not found!";
                    return errMsg;

                } catch (IOException e) {
                    errMsg = e.getMessage();
                    logger.Log(TAG, String.format("TryConnectNavi(): socketError=%s", errMsg));
                    return errMsg;
                }

                logger.Log(TAG, "tryConnectNavi(): ReadersWriters Open" );

                try {
                    naviWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(naviSocket.getOutputStream())), true);
                    naviReader = new BufferedReader(new InputStreamReader(naviSocket.getInputStream()));
                } catch (IOException e) {
                    errMsg = e.getMessage();
                    logger.Log(TAG, String.format("TryConnectNavi(): bufferError=%s", errMsg));

                    try {
                       naviSocket.close();
                    } catch (IOException x) {

                    }

                    return errMsg;
                }

                state = CONNECTED;

                logger.Log(TAG, "tryConnectNavi(): Protocol Version" );

                NaviResult result = sendNavi("$protocol_version");

                if (result.Code == 0) {
                   state = CONNECTED;
                   ProtocolVersion = result.Text;
                   logger.Log(TAG, String.format("tryConnectNavi(): Exit with Success = %s",ProtocolVersion) );
                   // return success
                   return "OK";
                } else {
                    state = UNKNOWN;
                    logger.Log(TAG, String.format("tryConnectNavi(): Exit with Failure = %s",result.Text) );

                    return result.Text;
                }
		    }
    	}

    	@Override
        public boolean handleMessage(Message msg) {
        	// Messenger Thread, accepts input from Main Activity via StartNavi/StopNavi msgs
        	logger.Log(TAG, String.format("NaviListener.handleMessage(): msg=%s", msg));
        	logger.Log(TAG, String.format("msg.what = %s", msg.what));

            switch (msg.what) {
        	case StartNavi:
        		// Schedule our main loop within this thread
    			exec = Executors.newSingleThreadScheduledExecutor();

    			try {
    			    poller = new NaviPoller();
    			    exec.scheduleAtFixedRate(poller, 1, 20, TimeUnit.SECONDS);
    			} catch (RejectedExecutionException g) {
    				logger.Log(TAG, String.format("Exception %s", g.getMessage()));
    			}

    			logger.Log(TAG, "Scheduler programmed");
    			break;
        	
        	case StopNavi:
        		logger.Log(TAG, String.format("Stop Tracking received, state = %d", state));
        		// Stop Navi thread and close all sockets
       			//shutdown main poller
        		logger.Log(TAG, "Stopping Poller/Closing Sockets..");

        		poller.StopPoller();

    			logger.Log(TAG, "Stopping Scheduler..");

    			exec.shutdown();
       			
        		break;
        			
        	}
        	return true;
            
        }
        
	}

	public NaviInterface (Handler webHndlr, Logger log) {
        logger = log;
        webHandler = webHndlr;
        // Arrange Message Handlers
        naviHandlerThread = new HandlerThread("NaviHandler");
        naviHandlerThread.start();

        logger.Log(TAG, "Waiting for looper...");

        while (!naviHandlerThread.isAlive()) {

        };

        naviHandler = new Handler(naviHandlerThread.getLooper(), new NaviListener());
        logger.Log(TAG, "Ready");
	}

}