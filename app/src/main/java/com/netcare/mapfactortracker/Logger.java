package com.netcare.mapfactortracker;

import java.util.concurrent.CopyOnWriteArrayList;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

public class Logger {
	private int buflen = 50;
	private TextView view = null;
	private Activity activity = null;
	
	public CopyOnWriteArrayList<String> LogData = null;
	
	public Logger (Activity app, TextView list, int bufsize) {
		buflen = bufsize;
		view = list;
		activity = app;
		LogData = new CopyOnWriteArrayList<String>();
	}
	
	public Logger(Activity app, TextView list){
		this(app, list, 50);
	}
	
	public void Log(String TAG, String Line){
		LogData.add(TAG + ": " + Line);
		if (LogData.size() > buflen) {
			LogData.remove(0);
		}
		StringBuilder sb = new StringBuilder();
		for (String s : LogData)
		{
		    sb.append(s);
		    sb.append(System.getProperty("line.separator"));
		}
		final String buffer = sb.toString();
		activity.runOnUiThread(new Runnable() {
		     public void run() {
		    	 view.setText(buffer);
		     }
		});
		Log.d(TAG, Line);
	}

}
