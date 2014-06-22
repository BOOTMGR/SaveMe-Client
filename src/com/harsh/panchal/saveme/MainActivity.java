/*******************************************************************************
 * Copyright 2014 Harsh Panchal <panchal.harsh18@gmail.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.harsh.panchal.saveme;

import static com.harsh.panchal.saveme.Common.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	// variables for server
	private static ServerSocket socket;
	private static Socket client;
	
	// variables for client
	private static Socket server;
	
	// for threads
	private Handler mainHandler = new Handler();
	private Context mContext;
	
	// stores status which is used for visual indication
	private static int currentState = -1;
	
	// Acknowledgment server status
	private static boolean isAckServerRunning = false;
	
	// store position
	public static String intermidiateLocation = "999,999";
	
	// state of message loop thread
	private static boolean isMessageLoopStarted = false;
	
	// UI Components
	private Button hBeat;
	
	// starts a new server & listens for incoming messages from server
	private class AcknowledgeReceiver extends Thread {
		@Override
		public void run() {
			if(!isAckServerRunning) {
				try {
					socket = new ServerSocket(ACKSERVER_PORT);
				} catch (Exception e) {
					Log.e(TAG, "Error starting ACKSERVER: " + e);
					return;
				}
				// check whether network connection is available
				if(getIPAdd() == null) {
					Log.e(TAG, "AcknowledgeReceiver: Error while stating ACK SERVER");
					mainHandler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(mContext, "Error while starting ACK SERVER. Are you connected to Network?", Toast.LENGTH_LONG).show();
						}
					});
					return;
				}					
				Log.d(TAG, "Started ACKSERVER: " + getIPAdd() + ":" + ACKSERVER_PORT);
				isAckServerRunning = true;
			}
			while (true) {
				try {
					client = socket.accept();
					BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
					final String response = reader.readLine();
					mainHandler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(mContext, "SERVER: " + response, Toast.LENGTH_LONG).show();
							currentState = ACK_RECEIVED;
							hBeat.setBackgroundResource(R.drawable.custom_button_green);
						}
					});
					client.close();
					reader.close();
				} catch (IOException e) {
					Log.e(TAG, "Error in ACKSERVER: " + e);
				}
			}
		}
	}
	
	/**
	 * Sends message to the server. Must be run in separate thread
	 * Message format must be as follows
	 * 
	 *      +---------+--------------+
	 *      | Address |    Message	 |
	 *      +---------+--------------+
	 *
	 * with '|' as delimiter.
	 *
	 * @param msg message to be sent to server
	 */
	private void sendMsgToServer(String msg) {
		try {
			if(server == null || server.isClosed())
				server = new Socket("192.168.1.2", SERVER_PORT);
			PrintWriter writer = new PrintWriter(server.getOutputStream(), true);
			writer.write(msg);
			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					currentState = ACK_PENDING;
					hBeat.setBackgroundResource(R.drawable.custom_button_yellow);
				}
			});
			writer.flush();
			writer.close();
			server.close();
		} catch (Exception ex) {
			Log.e(TAG, "sendMsgToServer() " + ex.toString());
		}
	}
	
	/**
	 * Start a loop which sends location updates to server every
	 * 3 seconds until server send acknowledge message.
	 */
	private void startMsgLoop() {
		if(!isMessageLoopStarted) {
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					// keep sending message until acknowledge arrives
					while(currentState != ACK_RECEIVED) {
						sendMsgToServer(getIPAdd() + "|" + intermidiateLocation);
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							Log.d(TAG, "startMsgLoop() " + e.toString());
						}
					}
				}
			}, "MessageLooperThread").start();
		}
	}
	
	private OnClickListener beatSender = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			// start location receiver service
			startService(new Intent(MainActivity.this, LocationProviderService.class));
			hBeat.setBackgroundResource(R.drawable.custom_button_red);
			new Thread(new Runnable() {
				@Override
				public void run() {
					sendMsgToServer(getIPAdd() + "|" + intermidiateLocation);
				}
			}, "HeartBeatSenderThread").start();
			currentState = REQUEST_SENT;
			startMsgLoop();
			isMessageLoopStarted = true;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d(TAG, "SaveMe --> Created(" + System.currentTimeMillis() + ")");
		mContext = getApplicationContext();
		hBeat = (Button) findViewById(R.id.but_send_beat);
		TextView tv = (TextView) findViewById(R.id.textView1);
		tv.setText(getIPAdd());
		hBeat.setOnClickListener(beatSender);
		/* 
		 * To preserve state of hBeat button on rotation because
		 * android calls onCreate() method when device is rotated
		 */
		switch (currentState) {
			case REQUEST_SENT:
				hBeat.setBackgroundResource(R.drawable.custom_button_red);
				break;
			case ACK_PENDING:
				hBeat.setBackgroundResource(R.drawable.custom_button_yellow);
				break;
			case ACK_RECEIVED:
				hBeat.setBackgroundResource(R.drawable.custom_button_green);
				break;
			default:
					break;
		}
		// start Acknowledgment Receiver server if not started already
		if(!isAckServerRunning)
			new AcknowledgeReceiver().start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onDestroy() {
		// stop location receiver service
		stopService(new Intent(MainActivity.this, LocationProviderService.class));
		super.onDestroy();
	}
}
