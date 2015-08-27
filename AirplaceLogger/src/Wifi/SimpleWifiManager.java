/*
 * AirPlace:  The Airplace Project is an OpenSource Indoor and Outdoor
 * Localization solution using WiFi RSS (Receive Signal Strength).
 * The AirPlace Project consists of three parts:
 *
 *  1) The AirPlace Logger (Ideal for collecting RSS Logs)
 *  2) The AirPlace Server (Ideal for transforming the collected RSS logs
 *  to meaningful RadioMap files)
 *  3) The AirPlace Tracker (Ideal for using the RadioMap files for
 *  indoor localization)
 *
 * It is ideal for spaces where GPS signal is not sufficient.
 *
 * Authors:
 * C. Laoudias, G.Larkou, G. Constantinou, M. Constantinides, S. Nicolaou,
 *
 * Supervisors:
 * D. Zeinalipour-Yazti and C. G. Panayiotou
 *
 * Copyright (c) 2011, KIOS Research Center and Data Management Systems Lab (DMSL),
 * University of Cyprus. All rights reserved.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Υou should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package Wifi;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.widget.TextView;
import android.widget.Toast;

public class SimpleWifiManager {

	/** WiFi manager used to scan and get scan results */
	private final WifiManager mainWifi;

	/**
	 * Intent with the SCAN_RESULTS_AVAILABLE_ACTION action will be broadcast to
	 * asynchronously announce that the scan is complete and results are
	 * available.
	 */
	private final IntentFilter wifiFilter;

	/** Timer to perform new scheduled scans */
	private final Timer timer;

	/** Task to perform a scan */
	private TimerTask WifiTask;

	/** Application context */
	private final Context mContext;

	/** Text View to show the scan results */
	private TextView scanResults;

	/** If Scanning, true or false */
	private Boolean isScanning;

	/**
	 * Creates a new instance
	 * 
	 * @param context
	 *            Application context
	 */
	public SimpleWifiManager(Context context) {
		mContext = context;
		isScanning = new Boolean(false);
		mainWifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		wifiFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		timer = new Timer();
	}

	/**
	 * Sets the TextView to show scan results
	 * 
	 * @param scanResults
	 *            TextView to set for scan results
	 * */
	public void setScanResultsTextView(TextView scanResults) {
		this.scanResults = scanResults;
	}

	/**
	 * @return if the WiFi manager performs a scan
	 * */
	public Boolean getIsScanning() {
		return isScanning;
	}

	/**
	 * Sets isScanning
	 * 
	 * @param isScanning
	 *            Boolean to set for isScanning
	 * */
	public void setIsScanning(Boolean isScanning) {

		synchronized (isScanning) {
			this.isScanning = isScanning;
		}
	}

	/**
	 * @return the results of the current scan
	 * */
	public List<ScanResult> getScanResults() {
		return mainWifi.getScanResults();
	}

	/**
	 * Starts the Access Points Scanning
	 * 
	 * @param receiverWifi
	 *            WifiReceiver to register with WiFi manager
	 * @param samples_interval
	 *            Interval used to perform a new scan
	 * */
	public void startScan(WifiReceiver receiverWifi, String samples_interval) {

		synchronized (isScanning) {
			if (isScanning) {
				toastPrint("Already Scanning...", Toast.LENGTH_SHORT);
				return;
			}
			isScanning = true;
		}

		if (samples_interval.equals("n/a") || samples_interval.equals("")) {
			toastPrint("Samples interval not specified\nGo to Menu->Preferences->Sampling Settings", Toast.LENGTH_LONG);

			synchronized (isScanning) {
				isScanning = false;
			}
			return;
		}

		toastPrint("Start scanning for Access Points...", Toast.LENGTH_SHORT);

		enableWifi();

		mContext.registerReceiver(receiverWifi, wifiFilter);

		if (WifiTask != null) {
			WifiTask.cancel();
		}

		if (timer != null) {
			timer.purge();
		}

		WifiTask = new TimerTask() {

			@Override
			public void run() {
				mainWifi.startScan();
			}
		};

		timer.schedule(WifiTask, 0, Long.parseLong(samples_interval));

	}

	/**
	 * Stop the Access Points Scanning
	 * 
	 * @param receiverWifi
	 *            WifiReceiver to unregister
	 * */
	public void stopScan(WifiReceiver receiverWifi) {

		synchronized (isScanning) {
			if (!isScanning) {
				toastPrint("Scanning already stopped!", Toast.LENGTH_SHORT);
				return;
			}
			isScanning = false;
		}

		disableWifi();

		mContext.unregisterReceiver(receiverWifi);
		toastPrint("Scanning Stopped", Toast.LENGTH_SHORT);
		if (scanResults != null)
			scanResults.setText("AP detected: " + 0);

	}

	/**
	 * Enables WiFi
	 * */
	public void enableWifi() {
		if (!mainWifi.isWifiEnabled())
			if (mainWifi.getWifiState() != WifiManager.WIFI_STATE_ENABLING)
				mainWifi.setWifiEnabled(true);
	}

	/**
	 * Disables WiFi
	 * */
	public void disableWifi() {
		if (mainWifi.isWifiEnabled())
			if (mainWifi.getWifiState() != WifiManager.WIFI_STATE_DISABLING)
				mainWifi.setWifiEnabled(false);
	}

	/**
	 * Print toast message to user
	 * */
	protected void toastPrint(String textMSG, int duration) {
		Toast.makeText(this.mContext, textMSG, duration).show();
	}

}
