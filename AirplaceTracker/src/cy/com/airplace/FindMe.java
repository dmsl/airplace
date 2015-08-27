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

package cy.com.airplace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import Wifi.SimpleWifiManager;
import Wifi.WifiReceiver;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
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
import android.widget.ToggleButton;
import cy.com.CalculationModes.OfflineMode;
import cy.com.Downloading.DownloadingSettings;
import cy.com.airplace.R;

public class FindMe extends Activity implements OnClickListener, OnSharedPreferenceChangeListener, Observer {

	// Text views to show results
	private TextView title;

	// TextView showing the current scan results
	private TextView scanResults;
	private TextView LocX;
	private TextView LocY;
	private TextView latitudeTextView;
	private TextView longitudeTextView;

	private DecimalFormat myFormatter = new DecimalFormat("###0.00");

	// Button to download radiomap
	private Button btnDownload;

	// Button for positioning
	private Button btnFindMe;

	// Button for position error
	private Button btnPosError;

	// Button for tracking
	private ToggleButton tglBtnTrackMe;

	// Flag to show if there is an ongoing progress
	private Boolean inProgress;

	// The radiomap read
	private RadioMap RM;

	// The latest scan list of APs
	ArrayList<LogRecord> LatestScanList;

	// WiFi manager
	private SimpleWifiManager wifi;

	// WiFi Receiver
	private WifiReceiver receiverWifi;

	// Preferences name for indoor and outdoor
	public static final String SHARED_PREFS_INDOOR = "Indoor_Preferences";

	private SharedPreferences Preferences;

	// Path and filename to store radio-map file
	private String folder_path;

	private String imagePath;
	// Image width and height in meters
	private String building_width;
	private String building_height;

	// The filename of downloading radiomap
	private String filename_radiomap_download;

	// Filename of radiomap to use for positioning
	private String filename_radiomap;

	// Flag for offline or online mode
	private boolean isOffline;

	private String algorithmSelection;

	private String test_data = "";

	private ProgressDialog progressDialog;
	private OfflineMode offmode;
	private DownloadingSettings downloadSet;

	private final BooleanObservable trackMe = new BooleanObservable();

	private BooleanObservable withPosErr;
	private FindMeOnBuild fmob;

	/**
	 * Called when the activity is first created.
	 * */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.building_layout);
		this.fmob = new FindMeOnBuild(this);
		this.fmob.setTrackMe(this.trackMe);

		LatestScanList = new ArrayList<LogRecord>();

		title = (TextView) findViewById(R.id.title);
		title.setText("Starting the Application");

		scanResults = (TextView) findViewById(R.id.scanResults);
		scanResults.setText("AP detected: " + 0);
		latitudeTextView = (TextView) findViewById(R.id.latitude);
		longitudeTextView = (TextView) findViewById(R.id.longitude);

		LocX = (TextView) findViewById(R.id.LatTitle);
		LocY = (TextView) findViewById(R.id.LonTitle);

		inProgress = new Boolean(false);

		// Create the Radio map
		RM = new RadioMap();

		// Button to download indoor radio map
		btnDownload = (Button) findViewById(R.id.downloadRadioMap);
		btnDownload.setOnClickListener(this);

		// Button to find user on map
		btnFindMe = (Button) findViewById(R.id.find_me);
		btnFindMe.setOnClickListener(this);

		btnPosError = (Button) findViewById(R.id.pos_error);
		btnPosError.setOnClickListener(this);

		btnPosError.setVisibility(View.INVISIBLE);

		tglBtnTrackMe = (ToggleButton) findViewById(R.id.trackme);
		tglBtnTrackMe.setOnClickListener(this);

		// WiFi manager to manage scans
		wifi = new SimpleWifiManager(getApplicationContext());
		wifi.setScanResultsTextView(scanResults);

		// Create new receiver to get broadcasts
		receiverWifi = new SimpleWifiReceiver();

		// Configure preferences
		Preferences = PreferenceManager.getDefaultSharedPreferences(this);

		PreferenceManager.setDefaultValues(this, SHARED_PREFS_INDOOR, MODE_PRIVATE, R.xml.preferences, true);
		Preferences = FindMe.this.getSharedPreferences(SHARED_PREFS_INDOOR, MODE_PRIVATE);
		LocX.setText("X:  ");
		LocY.setText("Y: ");

		Preferences.registerOnSharedPreferenceChangeListener(this);
		onSharedPreferenceChanged(Preferences, "modeIn");
		onSharedPreferenceChanged(Preferences, "image_custom");
	}

	/**
	 * The WifiReceiver is responsible to Receive Access Points results
	 * */
	public class SimpleWifiReceiver extends WifiReceiver {

		public void onReceive(Context c, Intent intent) {

			try {
				if (intent == null || c == null || intent.getAction() == null)
					return;

				String action = intent.getAction();

				if (!action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
					return;

				List<ScanResult> wifiList = wifi.getScanResults();
				scanResults.setText("AP detected: " + wifiList.size());

				// Set in progress (true)
				synchronized (inProgress) {
					if (inProgress == true)
						return;
					inProgress = true;
				}

				LatestScanList.clear();
				LogRecord lr = null;

				// If we receive results, add them to latest scan list
				if (wifiList != null && !wifiList.isEmpty()) {
					for (int i = 0; i < wifiList.size(); i++) {
						lr = new LogRecord(wifiList.get(i).BSSID, wifiList.get(i).level);
						LatestScanList.add(lr);
					}
				}

				// Unset in progress (false)
				synchronized (inProgress) {
					inProgress = false;
				}

				if (trackMe.get()) {
					if (!FindMe_Method()) {
						tglBtnTrackMe.setChecked(false);
						trackMe.setBoolean(false);
						trackMe.notifyObservers();
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
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * Handles menu choices
	 * 
	 * @param item
	 *            the item clicked from menu
	 * */
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		// Launch Preferences
		case R.id.Preferences:
			Intent prefs = new Intent(this, Preferences.class);
			startActivity(prefs);
			return true;
			// Launch Preferences to choose one of the algorithms implemented
		case R.id.Choose_Algorithm:
			Intent algorithm_prefs = new Intent(this, ChooseAnAlgorithm.class);
			startActivity(algorithm_prefs);
			return true;
			// Exit application
		case R.id.Exit_App:
			this.finish();
			return true;
		}
		return false;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

		if (key == null)
			return;

		if (key.equals("modeIn")) {
			isOffline = Preferences.getBoolean("modeIn", false);

			if (isOffline) {
				title.setText("Offline Mode");

				latitudeTextView.setVisibility(View.INVISIBLE);
				longitudeTextView.setVisibility(View.INVISIBLE);
				LocX.setVisibility(View.INVISIBLE);
				LocY.setVisibility(View.INVISIBLE);

				tglBtnTrackMe.setVisibility(View.INVISIBLE);

				tglBtnTrackMe.setChecked(false);
				this.trackMe.setBoolean(false);
				this.trackMe.notifyObservers();

				// Disables the WiFi if is in Offline Mode
				wifi.stopScan(receiverWifi);

				btnFindMe.setText("Statistics\nIndoor");

			} else {

				title.setText("Online Mode");

				latitudeTextView.setVisibility(View.VISIBLE);
				longitudeTextView.setVisibility(View.VISIBLE);
				LocX.setVisibility(View.VISIBLE);
				LocY.setVisibility(View.VISIBLE);

				tglBtnTrackMe.setVisibility(View.VISIBLE);

				// Enables the WiFi if is in Online Mode
				wifi.startScan(receiverWifi, "2000");

				btnFindMe.setText("Find Me\nIndoor");

			}
		} else if (key.equals("image_custom")) {

			imagePath = (String) Preferences.getString("image_custom", "").trim();

			if (imagePath.equals("")) {
				return;
			}

			building_width = null;
			building_height = null;

			if (!ReadWidthHeigthFromFile(new File(imagePath))) {
				popup_msg("Corrupted image configuration file.\nPlease set a different floor plan or previous floor plan will be used if available.",
						"Error", R.drawable.error);
				imagePath = null;
				building_width = null;
				building_height = null;
				return;
			}

			if (!fmob.setFloorPlan(imagePath, building_width, building_height)) {
				imagePath = null;
				building_width = null;
				building_height = null;
			} else {
				latitudeTextView.setText("");
				longitudeTextView.setText("");
			}
		}
	}

	/**
	 * Control the clicks on buttons
	 * 
	 * @param v
	 *            the view clicked
	 * */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		// Download new radiomap
		case R.id.downloadRadioMap:
			Download();
			break;
		// Positioning
		case R.id.find_me:
			tglBtnTrackMe.setChecked(false);
			this.trackMe.setBoolean(false);
			this.trackMe.notifyObservers();
			FindMe_Method();
			break;
		// Tracking
		case R.id.trackme:
			if (tglBtnTrackMe.isChecked()) {
				this.trackMe.setBoolean(true);
				this.trackMe.notifyObservers();
			} else {
				this.trackMe.setBoolean(false);
				this.trackMe.notifyObservers();
			}
			break;
		}

	}

	/**
	 * Downloads new radiomap
	 * */
	private void Download() {

		String serverAddress = null;
		String portNumber = null;

		serverAddress = Preferences.getString("serverIP", "").trim();
		portNumber = Preferences.getString("serverPORT", "").trim();
		folder_path = (String) Preferences.getString("folder_browser", "").trim();
		filename_radiomap_download = (String) Preferences.getString("radiomap_file_download", "").trim();

		// Check IP/Port of Server
		if (serverAddress.equals("")) {
			popup_msg("Unable to start connection with the server\n"
					+ "Go to Menu::Preferences::Radiomap Settings::RadioMap Download Settings::Connection Settings::Server Address", "User Error",
					R.drawable.error);
			return;
		} else if (portNumber.equals("")) {
			popup_msg("Unable to start connection with the server\n"
					+ "Go to Menu::Preferences::Radiomap Settings::RadioMap Download Settings::Connection Settings::Port number", "User Error",
					R.drawable.error);
			return;
		}

		// Check folder path to store radio map
		if (folder_path.equals("")) {
			popup_msg("Folder path is not specified\n"
					+ "Go to Menu::Preferences::Radiomap Settings::RadioMap Download Settings::Downloading Settings::Radio Map Folder", "User Error",
					R.drawable.error);
			return;
		} else if ((!(new File(folder_path).canWrite()))) {
			popup_msg("Folder path is not writable\n"
					+ "Go to Menu::Preferences::Radiomap Settings::RadioMap Download Settings::Downloading Settings::Radio Map Folder", "User Error",
					R.drawable.error);
			return;
		}

		// Check radiomap filename
		if (filename_radiomap_download.equals("")) {
			popup_msg("Filename of radio map not specified\n"
					+ "Go to Menu::Preferences::Radiomap Settings::RadioMap Download Settings::Downloading Settings::Radio Map Filename",
					"User Error", R.drawable.error);
			return;
		}

		toastPrint("Trying to connect to " + serverAddress + ":" + portNumber + "...", Toast.LENGTH_LONG);

		// Set in progress (true)
		synchronized (inProgress) {
			if (inProgress == true)
				return;
			inProgress = true;
		}

		progressDialog = ProgressDialog.show(FindMe.this, "", "Downloading. Please wait...", true, false);

		downloadSet = new DownloadingSettings(serverAddress, portNumber, folder_path, filename_radiomap_download, handler);

		downloadSet.start();

		// Unset in progress (false)
		synchronized (inProgress) {
			inProgress = false;
		}

	}

	/**
	 * Starts the appropriate positioning algorithm
	 * */
	private boolean FindMe_Method() {

		algorithmSelection = (String) Preferences.getString("Algorithms", "1").trim();
		filename_radiomap = (String) Preferences.getString("radiomap_file", "").trim();
		isOffline = Preferences.getBoolean("modeIn", false);
		test_data = Preferences.getString("test_data_file", "").trim();

		if (!isOffline) {

			if (!fmob.okBuildingSettings()) {
				popup_msg("Building floor plan not specified\nGo to Menu::Preferences::Building Settings::Floor Plan", "Error", R.drawable.error);
				return false;
			}
		}

		// Check that radiomap file is readable
		if (filename_radiomap.equals("")) {
			popup_msg("Radiomap file not specified\nGo to Menu::Preferences::Radiomap Settings::Radiomap File", "User Error", R.drawable.error);
			return false;

		} else if ((!(new File(filename_radiomap).canRead()))) {
			popup_msg("Radiomap file is not readable\nGo to Menu::Preferences::Radiomap Settings::Radiomap File", "User Error", R.drawable.error);
			return false;
		}

		// Check algorithm selection
		if (algorithmSelection.equals("") || Integer.parseInt(algorithmSelection) < 1 || Integer.parseInt(algorithmSelection) > 6) {
			popup_msg("Unable to find the location\nSpecify Algorithm", "User Error", R.drawable.error);
			return false;
		}

		if (isOffline && test_data.equals("")) {
			popup_msg("Test Data file not specified\nGo to Menu::Preferences::Mode::Test Data File", "User Error", R.drawable.error);
			return false;
		}

		// Set in progress (true)
		synchronized (inProgress) {
			if (inProgress == true)
				return false;
			inProgress = true;
		}

		// Error reading Radio Map
		if (!RM.ConstructRadioMap(new File(filename_radiomap))) {
			popup_msg("Error while reading radio map.\nDownload new Radio Map and try again", "User Error", R.drawable.error);

			// Unset in progress (false)
			synchronized (inProgress) {
				inProgress = false;
			}

			return false;
		}

		/**
		 * If is true then go offline, otherwise continue online mode
		 */
		if (isOffline) {
			goOffline(RM, new File(test_data), Integer.parseInt(algorithmSelection));
		} else {

			if (LatestScanList.isEmpty()) {
				popup_msg("No Access Point Received.\nWait for a scan first and try again.", "Warning", R.drawable.warning);

				// Unset in progress (false)
				synchronized (inProgress) {
					inProgress = false;
				}

				return false;
			}

			if (!calculatePosition(Integer.parseInt(algorithmSelection))) {
				popup_msg("Can't find location. Check that radio map file refers to the same area.", "Error", R.drawable.error);

				// Unset in progress (false)
				synchronized (inProgress) {
					inProgress = false;
				}

				return false;
			}

		}

		// Unset in progress (false)
		synchronized (inProgress) {
			inProgress = false;
		}

		return true;

	}

	private boolean ReadWidthHeigthFromFile(File imageFile) {

		BufferedReader reader = null;
		FileReader fr = null;
		String line = null;
		int i = 0;

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
				String[] temp = line.split(" ");

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

	/**
	 * Checks the building width and height
	 * 
	 * @param building_width
	 *            the width of the building to check
	 * 
	 * @param building_height
	 *            the height of the building to check
	 * 
	 * @return true if the width and height are ok, otherwise false
	 * 
	 * */
	private boolean checkBuildingDimensions(String building_width, String building_height) {

		if (building_height.equals("")) {
			popup_msg("Corrupted image configuration file", "User Error", R.drawable.error);
			return false;
		}

		try {
			Float.parseFloat(building_height);
		} catch (Exception e) {
			popup_msg("Error Building Height: " + e.getMessage(), "User Error", R.drawable.error);
			return false;
		}

		if (building_width.equals("")) {
			popup_msg("Corrupted image configuration file", "User Error", R.drawable.error);
			return false;
		}

		try {
			Float.parseFloat(building_width);
		} catch (Exception e) {
			popup_msg("Error Building Width: " + e.getMessage(), "User Error", R.drawable.error);
			return false;
		}

		return true;
	}

	private boolean calculatePosition(int choice) {

		String calculatedLocation = Algorithms.ProcessingAlgorithms(LatestScanList, RM, choice);

		if (calculatedLocation == null) {
			return false;
		}

		String[] x_y = calculatedLocation.split(" ");
		if (x_y.length != 2) {
			return false;
		}

		try {
			if (x_y[0].equals("NaN") || x_y[1].equals("NaN"))
				return false;
			latitudeTextView.setText(myFormatter.format(Float.parseFloat(x_y[0])) + "m");
			longitudeTextView.setText(myFormatter.format(Float.parseFloat(x_y[1])) + "m");
		} catch (Exception e) {
			return false;
		}

		fmob.setLocationOnFloorPlan(calculatedLocation);

		return true;

	}

	private void goOffline(RadioMap RM, File inFile, int algorithm_selection) {
		progressDialog = ProgressDialog.show(FindMe.this, "", "Calculating. Please wait...\n0%", true, false);

		offmode = new OfflineMode(RM, inFile, algorithm_selection, handler);

		offmode.start();
	}

	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {

			if (msg.what >= 0)
				progressDialog.setMessage("Calculating. Please wait...\n" + msg.what + "%");

			switch (msg.what) {
			case -1:
				progressDialog.dismiss();
				if (offmode.getErrMsg() != null)
					popup_msg(offmode.getErrMsg(), "Error", R.drawable.error);
				else {
					popup_statistics("Average Positioning Error: " + myFormatter.format(offmode.getAverage_pos_err()) + "m\nAverage Execution Time: "
							+ myFormatter.format(offmode.getAverage_exe_time()) + "ms", "Info", R.drawable.info);
				}
				break;

			case -2:
				progressDialog.dismiss();
				if (downloadSet.getErrMsg() != null)
					popup_msg(downloadSet.getErrMsg(), "Error", R.drawable.error);
				else
					popup_msg("Radio Map and Parameters Successfully Downloaded and Stored, on "
							+ Calendar.getInstance().getTime().toString(), "Info", R.drawable.info);
				break;
			}
		}
	};

	public String showStatistics() {
		/**
		 * Estimate the cpu consumption
		 */
		Power power;
		String myfile = "sdcard/";
		String file = PowerTutor.getLastFilePowerTutor("sdcard/");

		if (file == null) {
			return "No Power Tutor log file found";
		}

		power = PowerTutor.getPower(myfile + file);

		if (power != null)
			return "Total Power (CPU): " + myFormatter.format(power.CPU) + "mW";
		else
			return "Could not calculate Total Power (CPU)";
	}

	private void popup_msg(String msg, String title, int imageID) {

		AlertDialog.Builder alert_box = new AlertDialog.Builder(this);
		alert_box.setTitle(title);
		alert_box.setMessage(msg);
		alert_box.setIcon(imageID);

		alert_box.setNeutralButton("Hide", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		AlertDialog alert = alert_box.create();
		alert.show();
	}

	private void popup_statistics(final String msg, final String title, final int imageID) {

		final AlertDialog.Builder alert_box = new AlertDialog.Builder(this);
		alert_box.setTitle(title);
		alert_box.setMessage(msg);
		alert_box.setIcon(imageID);

		alert_box.setNeutralButton("Hide", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		}).setPositiveButton("Show Power", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				popup_msg(msg + "\n" + showStatistics(), title, imageID);
			}
		});

		AlertDialog alert = alert_box.create();
		alert.show();
	}

	/**
	 * Method used to print pop up message to user
	 * */
	private void toastPrint(String textMSG, int duration) {
		Toast.makeText(this, textMSG, duration).show();
	}

	/**
	 * Back button pressed to exit program
	 */
	@Override
	public void onBackPressed() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setMessage("Are you sure you want to exit?").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				FindMe.this.finish();
			}
		}).setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();

	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {

		wifi.stopScan(receiverWifi);

		super.onDestroy();
	}

	@Override
	public void update(Observable observable, Object data) {
		if (this.withPosErr != null && this.withPosErr.get())
			btnPosError.setVisibility(View.VISIBLE);
		else
			btnPosError.setVisibility(View.INVISIBLE);
	}
}