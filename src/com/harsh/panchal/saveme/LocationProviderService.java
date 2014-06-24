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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

public class LocationProviderService extends Service implements LocationListener {
	
	// notification
	private NotificationManager notifmanager;
	private Notification notification;
	private final int NOTIF_ID_LOCATION = 2500;
	
	// parameters for GPS, Network positioning
	private static final long DISTANCE_BETWEEN_UPDATES = 10;	// 10 meters
	private static final long TIME_BETWEEN_UPDATES = 1000 * 30; // 30 seconds
	private LocationManager locationManager;
	private boolean isGPSenabled, isNetworkLocationEnabled;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate() {
		super.onCreate();
		notifmanager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notification = new Notification(R.drawable.ic_launcher, "Save Me", System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		Intent notifyIntent = new Intent(this, NotificationDialog.class);
		PendingIntent intent = PendingIntent.getActivity(LocationProviderService.this, NOTIF_ID_LOCATION, 
		            notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(this, "Save Me", "Sending Location Updates...", intent);
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		isGPSenabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		isNetworkLocationEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		startLocationListener();
		// show ongoing notification on statusbar
		notifmanager.notify(NOTIF_ID_LOCATION, notification);
	}
	
	private void startLocationListener() {
		if(isGPSenabled || isNetworkLocationEnabled) {
			if(isGPSenabled) {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, TIME_BETWEEN_UPDATES, DISTANCE_BETWEEN_UPDATES, this);
			} else if(isNetworkLocationEnabled) {
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, TIME_BETWEEN_UPDATES, DISTANCE_BETWEEN_UPDATES, this);
			}
		} else {
			Toast.makeText(getApplicationContext(), "GPS & Network Locations are disabled. Please enable any one to send location.", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onLocationChanged(Location location) {
		MainActivity.intermidiateLocation = location.getLatitude() + "," + location.getLongitude();
	}

	@Override
	public void onProviderDisabled(String provider) {
		// do nothing
	}

	@Override
	public void onProviderEnabled(String provider) {
		// do nothing
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// do nothing
	}
	
	@Override
	public void onDestroy() {
		locationManager.removeUpdates(this);
		notifmanager.cancel(NOTIF_ID_LOCATION);	//clear notification
		super.onDestroy();
	}

}
