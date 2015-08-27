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

package cy.com.airplacersslogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import Wifi.SimpleWifiManager;
import Wifi.WifiReceiver;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import cy.com.airplacersslogger.R;
import cy.com.zoom.ClickPoint;
import cy.com.zoom.DynamicZoomControl;
import cy.com.zoom.ImageZoomView;
import cy.com.zoom.LongPressZoomListener;

/**
 * Activity controlling the indoor rss logger
 */
public class RSSLogger extends Activity implements OnClickListener, OnSharedPreferenceChangeListener, Observer {

	/** Current Location */
	private final PointF curLocationMeters = new PointF(-1, -1);
	private ClickPoint curLocationPixels;

	/** Button that records access points */
	private Button Record_APs_Button;

	/** Background of Record_APs_Button changes when clicked */
	private Drawable Record_APs_Button_Background;

	/** TextView showing the current scan results */
	private TextView scanResults;

	private DecimalFormat myFormatter = new DecimalFormat("###0.00m");
	private TextView latitudeTextView;
	private TextView longitudeTextView;

	/** Number of samples to record */
	private String samples_num = null;

	/** WiFi manager */
	private SimpleWifiManager wifi;

	/** WiFi Receiver */
	private WifiReceiver receiverWifi;

	/** Samples list */
	private final ArrayList<ArrayList<LogRecord>> samples = new ArrayList<ArrayList<LogRecord>>();

	/** Path to store rss file */
	private String folder_path;

	/** Filename to store rss records */
	private String filename_rss;

	public enum PREFS_STRINGS {
		image_custom, NOVALUE;

		public static PREFS_STRINGS toPREFS_STRINGS(String str) {
			try {
				return valueOf(str);
			} catch (Exception ex) {
				return NOVALUE;
			}
		}
	}

	private String building_width;
	private String building_height;

	public static final String SHARED_PREFS_NAME = "Indoor_Preferences";
	private SharedPreferences Preferences;

	private static final int PROGRESS_DIALOG = 0;
	private ProgressDialog progressDialog;

	/** Image zoom view */
	private ImageZoomView mZoomView;

	/** Zoom control */
	private DynamicZoomControl mZoomControl;

	/** Decoded bitmap image */
	private Bitmap mBitmap;

	/** On touch listener for zoom view */
	private LongPressZoomListener mZoomListener;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_indoor);

		scanResults = (TextView) findViewById(R.id.scanResults);
		scanResults.setText("AP detected: " + 0);

		latitudeTextView = (TextView) findViewById(R.id.latitude);
		longitudeTextView = (TextView) findViewById(R.id.longitude);

		// Button for recording
		Record_APs_Button = (Button) findViewById(R.id.logChanges);
		Record_APs_Button.setOnClickListener(this);
		Record_APs_Button_Background = findViewById(R.id.logChanges).getBackground();

		// WiFi manager to manage scans
		wifi = new SimpleWifiManager(RSSLogger.this);
		wifi.setScanResultsTextView(scanResults);

		// Create new receiver to get broadcasts
		receiverWifi = new SimpleWifiReceiver();

		// Set Zooming and Panning Settings
		mZoomControl = new DynamicZoomControl();

		mZoomListener = new LongPressZoomListener(getApplicationContext());
		mZoomListener.setZoomControl(mZoomControl);

		mZoomView = (ImageZoomView) findViewById(R.id.zoomview);
		mZoomView.setZoomState(mZoomControl.getZoomState());
		mZoomView.setContext(this);
		mZoomView.setOnTouchListener(mZoomListener);
		mZoomView.setCurClick(mZoomListener.getClickPoint());

		curLocationPixels = mZoomView.getClickPoint();
		curLocationPixels.addObserver(this);

		mZoomControl.setAspectQuotient(mZoomView.getAspectQuotient());

		// Preferences
		PreferenceManager.setDefaultValues(this, SHARED_PREFS_NAME, MODE_PRIVATE, R.xml.preferences, true);
		Preferences = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
		Preferences.registerOnSharedPreferenceChangeListener(this);
		onSharedPreferenceChanged(Preferences, null);

		String filePath = Preferences.getString("image_custom", "Bad Image");

		if (!filePath.equals("Bad Image")) {

			if (!ReadWidthHeigthFromFile(new File(filePath))) {
				toastPrint("Corrupted image configuration file", Toast.LENGTH_LONG);
				return;
			}

			mBitmap = BitmapFactory.decodeFile(filePath);

			if (mBitmap == null)
				return;

			mZoomView.setImage(mBitmap);
			resetZoomState();
		}

	}

	/**
	 * The WifiReceiver is responsible to Receive Access Points results
	 * */
	public class SimpleWifiReceiver extends WifiReceiver {

		@Override
		public void onReceive(Context c, Intent intent) {
			try {
				if (intent == null || c == null || intent.getAction() == null)
					return;

				List<ScanResult> wifiList = wifi.getScanResults();
				scanResults.setText("AP detected: " + wifiList.size());

				if (Record_APs_Button.isClickable())
					return;

				ArrayList<LogRecord> Records = new ArrayList<LogRecord>();
				Records.clear();

				Date date = new Date();
				long timestamp = date.getTime();

				if (curLocationMeters != null && !wifiList.isEmpty()) {

					for (int i = 0; i < wifiList.size(); i++) {
						LogRecord lr = new LogRecord(timestamp, curLocationMeters.x, curLocationMeters.y, wifiList.get(i).BSSID,
								wifiList.get(i).level);
						Records.add(lr);
					}

					synchronized (samples) {

						samples.add(0, Records);

						Message msg = handler.obtainMessage();
						msg.arg1 = samples.size();
						handler.sendMessage(msg);

						if (samples.size() >= Integer.parseInt(samples_num)) {
							write_to_log();
							Record_APs_Button.setClickable(true);
							findViewById(R.id.logChanges).invalidateDrawable(Record_APs_Button_Background);
							Record_APs_Button_Background.clearColorFilter();
							mZoomView.setPoint();
						}
					}
				}
			} catch (RuntimeException e) {
				return;
			}
		}
	}

	/**
	 * Draw the menu
	 * 
	 * @param menu
	 *            the menu to add items for Indoor RSS
	 * */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_indoor, menu);
		return true;
	}

	/**
	 * Handles menu choices
	 * 
	 * @param item
	 *            the item clicked from menu
	 * */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		// Start Scanning
		case R.id.Start_Scan:
			wifi.startScan(receiverWifi, (String) Preferences.getString("samples_interval", "n/a"));
			return true;
			// Stop Scanning
		case R.id.Stop_Scan:
			wifi.stopScan(receiverWifi);
			return true;
			// Launch preferences
		case R.id.Preferences:
			Intent prefs = new Intent(this, Prefs.class);
			startActivity(prefs);
			return true;
			// Exit application
		case R.id.Exit_App:
			Intent data = new Intent();
			data.setData(Uri.parse("true"));
			setResult(RESULT_OK, data);
			finish();
			return true;
		}
		return false;
	}

	/**
	 * Reset zoom state and notify observers
	 */
	private void resetZoomState() {
		mZoomControl.getZoomState().setPanX(0.5f);
		mZoomControl.getZoomState().setPanY(0.5f);
		mZoomControl.getZoomState().setZoom(1f);
		mZoomControl.getZoomState().notifyObservers();
	}

	/**
	 * Change building image
	 * */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

		if (key == null)
			return;

		switch (PREFS_STRINGS.toPREFS_STRINGS(key)) {

		case image_custom:

			String filePath = prefs.getString("image_custom", "Bad Image");

			if (filePath.equals("Bad Image"))
				return;

			if (!ReadWidthHeigthFromFile(new File(filePath))) {
				toastPrint("Corrupted image configuration file", Toast.LENGTH_LONG);
				return;
			}

			if (mBitmap != null) {
				mBitmap.recycle();
			}

			System.gc();

			mBitmap = BitmapFactory.decodeFile(filePath);

			if (mBitmap == null)
				return;

			mZoomView.setImage(mBitmap);
			resetZoomState();
			resetLocation();

			break;
		default:
			break;
		}
	}

	private void resetLocation() {
		curLocationMeters.x = -1;
		curLocationMeters.y = -1;
		latitudeTextView.setText("");
		longitudeTextView.setText("");
	}

	/**
	 * Control the clicks on buttons
	 * 
	 * @param v
	 *            the view clicked
	 * */
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.logChanges:
			click_action_Record_Info();
			break;
		}
	}

	/**
	 * Record button pressed. Get samples number from preferences
	 * */
	private void click_action_Record_Info() {

		samples_num = (String) Preferences.getString("samples_num", "n/a");
		String samples_interval = (String) Preferences.getString("samples_interval", "n/a");

		if (mBitmap == null) {
			toastPrint("Building image not specified\nGo to Menu::Preferences::Building Settings::Building", Toast.LENGTH_LONG);
			return;
		}

		if (!checkBuildingDimensions(building_width, building_height))
			return;

		if (samples_num.equals("n/a") || samples_num.trim().equals("")) {
			toastPrint("Samples number not specified\nGo to Menu::Preferences::Sampling Settings::Samples Number", Toast.LENGTH_LONG);
			return;
		}

		folder_path = (String) Preferences.getString("folder_browser", "n/a");
		if (folder_path.equals("n/a") || folder_path.equals("")) {
			toastPrint("Folder path not specified\nGo to Menu::Preferences::Storing Settings::Folder", Toast.LENGTH_LONG);
			return;

		} else if ((!(new File(folder_path).canWrite()))) {
			toastPrint("Folder path is not writable\nGo to Menu::Preferences::Storing Settings::Folder", Toast.LENGTH_LONG);
			return;
		}

		filename_rss = (String) Preferences.getString("filename_log", "n/a");
		if (filename_rss.equals("n/a") || filename_rss.equals("")) {
			toastPrint("Filename of RSS log not specified\nGo to Menu::Preferences::Storing Settings::Filename", Toast.LENGTH_LONG);
			return;
		}

		if (curLocationMeters.x < 0 || curLocationMeters.y < 0) {
			toastPrint("Press a location on the map", Toast.LENGTH_LONG);
			return;
		}

		if (!wifi.getIsScanning()) {

			if (samples_interval.equals("n/a") || samples_interval.trim().equals("")) {
				toastPrint("Samples interval not specified\nGo to Menu::Preferences::Sampling Settings::Samples Interval", Toast.LENGTH_LONG);
				return;
			}
			wifi.startScan(receiverWifi, samples_interval);
		}

		PorterDuffColorFilter filter = new PorterDuffColorFilter(Color.GRAY, Mode.SRC_ATOP);
		Record_APs_Button_Background.setColorFilter(filter);
		Record_APs_Button.setClickable(false);
		showDialog(PROGRESS_DIALOG);
	}

	/**
	 * Create the dialog
	 * 
	 * @param id
	 *            the id of dialog to create
	 * */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_DIALOG:
			progressDialog = new ProgressDialog(RSSLogger.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMessage("Scanning in progress...");
			progressDialog.setCancelable(false);
			return progressDialog;
		default:
			return null;
		}
	}

	/**
	 * Prepare the dialog. Sets progress to 0 and max to samples number
	 * 
	 * @param id
	 *            the id of dialog to prepare
	 * @param dialog
	 * 
	 * */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case PROGRESS_DIALOG:
			progressDialog.setProgress(0);
			progressDialog.setMax(Integer.parseInt(samples_num));
			break;
		}
	}

	/**
	 * Handler that receives messages from the thread and update the progress
	 * dialog
	 */
	final Handler handler = new Handler() {

		public void handleMessage(Message msg) {
			int total = msg.arg1;
			progressDialog.setProgress(total);

			// Check to dismiss
			if (total >= progressDialog.getMax()) {
				dismissDialog(PROGRESS_DIALOG);
				progressDialog.setProgress(0);
			}
		}
	};

	/**
	 * Writes AP scan records to log file specified by the user
	 * */
	private void write_to_log() {

		String header = "# Timestamp, X, Y, MAC Address of AP, RSS\n";
		ArrayList<LogRecord> writeRecords;
		LogRecord writeLR;
		int N;

		synchronized (samples) {

			try {

				Boolean write_mode = Preferences.getBoolean("write_mode", false);

				N = Integer.parseInt(samples_num);

				File root = new File(folder_path);

				if (root.canWrite()) {

					FileOutputStream fos = new FileOutputStream(new File(root, filename_rss), !write_mode);

					for (int i = 0; i < N; ++i) {
						fos.write(header.getBytes());
						writeRecords = samples.get(i);
						for (int j = 0; j < writeRecords.size(); ++j) {
							writeLR = writeRecords.get(j);
							fos.write(writeLR.toString().getBytes());
						}
					}

					if (!samples.isEmpty()) {

						toastPrint(((N < samples.size()) ? N : samples.size()) + " Samples Recorded on\n"
								+ Calendar.getInstance().getTime().toString(), R.drawable.info);

						samples.clear();
					}

					fos.close();
				}
			} catch (ClassCastException cce) {
				toastPrint("Error: " + cce.getMessage(), R.drawable.error);
			} catch (NumberFormatException nfe) {
				toastPrint("Error: " + nfe.getMessage(), R.drawable.error);
			} catch (FileNotFoundException fnfe) {
				toastPrint("Error: " + fnfe.getMessage(), R.drawable.error);
			} catch (IOException ioe) {
				toastPrint("Error: " + ioe.getMessage(), R.drawable.error);
			}

		}
	}

	/**
	 * Method used to print pop up message to user
	 * */
	protected void toastPrint(String textMSG, int duration) {
		Toast.makeText(this, textMSG, duration).show();
	}

	/**
	 * Control when back button is pressed
	 * */
	@Override
	public void onBackPressed() {
		RSSLogger.this.finish();
	}

	/**
	 * Prepare RSS indoor logger to close
	 * */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mBitmap != null)
			mBitmap.recycle();
		mZoomView.setOnTouchListener(null);
		mZoomControl.getZoomState().deleteObservers();

		wifi.disableWifi();
	}

	@Override
	public void update(Observable observable, Object data) {

		if (!checkBuildingDimensions(building_width, building_height))
			return;

		curLocationMeters.x = curLocationPixels.get().x * Float.parseFloat(building_width) / (float) mBitmap.getWidth();
		curLocationMeters.y = curLocationPixels.get().y * Float.parseFloat(building_height) / (float) mBitmap.getHeight();

		latitudeTextView.setText(myFormatter.format(curLocationMeters.x));
		longitudeTextView.setText(myFormatter.format(curLocationMeters.y));
	}

	private boolean ReadWidthHeigthFromFile(File imageFile) {

		BufferedReader reader = null;
		FileReader fr = null;
		String line = null;
		int i = 0;
		String[] temp = null;

		try {
			String conf = imageFile.getAbsolutePath().replace(".jpg", ".config").replace(".JPG", ".config");
			fr = new FileReader(conf);
		} catch (Exception e) {
			return false;
		}

		reader = new BufferedReader(fr);

		try {

			while ((line = reader.readLine()) != null) {

				/* Ignore the labels */
				if (line.startsWith("#") || line.trim().equals("")) {
					continue;
				}

				line = line.replace(": ", " ");

				/* Split fields */
				temp = line.split(" ");

				if (temp.length != 2) {
					return false;
				}

				if (i == 0)
					building_width = temp[1];
				else
					building_height = temp[1];

				++i;

			}
		} catch (Exception e) {
			return false;
		}

		if (!checkBuildingDimensions(building_width, building_height)) {
			return false;
		}

		return true;
	}

	private boolean checkBuildingDimensions(String building_width, String building_height) {

		if (building_width.equals("n/a") || building_width.trim().equals("")) {
			toastPrint("Corrupted image configuration file", Toast.LENGTH_LONG);
			return false;
		}

		if (building_height.equals("n/a") || building_height.trim().equals("")) {
			toastPrint("Corrupted image configuration file", Toast.LENGTH_LONG);
			return false;
		}

		try {
			Float.parseFloat(building_width);
		} catch (Exception e) {
			toastPrint("Error Building Width: " + e.getMessage(), Toast.LENGTH_LONG);
			return false;
		}

		try {
			Float.parseFloat(building_height);
		} catch (Exception e) {
			toastPrint("Error Building Height: " + e.getMessage(), Toast.LENGTH_LONG);
			return false;
		}
		return true;
	}

}