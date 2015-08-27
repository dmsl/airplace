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

package cy.com.FileBrowser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cy.com.airplace.FindMe;
import cy.com.airplace.R;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class AndroidFileBrowser extends ListActivity implements OnClickListener {

	// Enum for the Display Mode
	private enum DISPLAYMODE {
		ABSOLUTE, RELATIVE;
	}

	private final DISPLAYMODE displayMode = DISPLAYMODE.ABSOLUTE;
	private List<String> directoryEntries = new ArrayList<String>();
	private File currentDirectory = null;
	private final File homeDirectory = new File("/");
	private File file = null;
	private TextView pwd = null;
	private Button select_file_folder = null;
	private int selectFolder = 1;
	private String folder_path = null;
	private String file_path = null;
	private SharedPreferences sharedPreferences = null;

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		setContentView(R.layout.main_choose_file_or_directory);

		pwd = (TextView) findViewById(R.id.pwd);
		select_file_folder = (Button) findViewById(R.id.select_file_folder);
		select_file_folder.setOnClickListener(this);

		// Get flags for browsing and indoor or outdoor
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			selectFolder = extras.getInt("to_Browse");
		}

		sharedPreferences = getSharedPreferences(FindMe.SHARED_PREFS_INDOOR, MODE_PRIVATE);

		// Get folder path selected before
		folder_path = sharedPreferences.getString("folder_browser", "").trim();

		// Get file of radiomap selected before

		switch (selectFolder) {
		case 1:
			select_file_folder.setText("Save in this folder");
			break;

		case 2:

			file_path = sharedPreferences.getString("radiomap_file", "").trim();
			select_file_folder.setText("Use radiomap");

			if (file_path != null && !file_path.equals("") && new File(file_path).canRead())
				pwd.setText(file_path);

			break;

		case 3:

			file_path = sharedPreferences.getString("test_data_file", "").trim();
			select_file_folder.setText("Use test data");

			if (file_path != null && !file_path.equals("") && new File(file_path).canRead())
				pwd.setText(file_path);

			break;
		}

		if (folder_path == null || folder_path.equals("")) {
			browseToHome();
		} else {
			currentDirectory = new File(makePath(folder_path));
			browseTo(currentDirectory);
		}
	}

	/**
	 * Sets home directory (/) as the current directory
	 * 
	 * */
	private void browseToHome() {
		currentDirectory = new File("/");
		browseTo(homeDirectory);
	}

	/**
	 * Make a path if exists, can read and is a directory
	 * 
	 * @param path
	 *            the given path to check
	 * 
	 * 
	 * @return the constructed path
	 * */
	private String makePath(String path) {

		if (path.length() == 0) {
			return "/";
		}

		File check = new File(path);
		if (!check.exists() || !check.canRead() || !check.isDirectory()) {
			return "/";
		}

		if (check.isDirectory())
			return path;

		return "/";
	}

	/**
	 * Browses to aDirectory path in mobile device file system
	 * 
	 * @param aDirectory
	 *            the given directory to enter
	 * 
	 * */
	private void browseTo(final File aDirectory) {

		if (aDirectory.isDirectory()) {
			this.currentDirectory = aDirectory;
			fill(aDirectory.listFiles());

			if (selectFolder == 1)
				pwd.setText(currentDirectory.getAbsolutePath());
		}
	}

	/**
	 * Fills directoryEntries with the files in current path
	 * 
	 * @param files
	 *            in the current directory
	 * 
	 * */
	private void fill(File[] files) {

		// Clear list
		this.directoryEntries.clear();

		// Add the "~" for home directory
		this.directoryEntries.add(getString(R.string.homeDir));

		// And the ".." for parent directory
		if (this.currentDirectory.getParent() != null)
			this.directoryEntries.add(getString(R.string.parentDir));

		switch (this.displayMode) {

		case ABSOLUTE:
			for (File file : files) {
				this.directoryEntries.add(file.getAbsolutePath());
			}
			break;
		case RELATIVE:
			int currentPathStringLenght = this.currentDirectory.getAbsolutePath().length();
			for (File file : files) {
				this.directoryEntries.add(file.getAbsolutePath().substring(currentPathStringLenght));
			}
			break;
		}

		MyCustomAdapter directoryList = new MyCustomAdapter(this, R.xml.file_row, this.directoryEntries);

		this.setListAdapter(directoryList);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		file = new File(directoryEntries.get(position));

		if (file.isDirectory()) {
			if (file.canRead()) {
				if (file.getName().equals(getString(R.string.parentDir))) {
					browseTo(currentDirectory.getParentFile());
				} else if (file.getName().equals(getString(R.string.homeDir))) {
					browseTo(homeDirectory);
				} else
					browseTo(file);
			} else {
				showAlert("Read Permission Denied", "Warning", this);
			}
		} else if (!file.isDirectory() && selectFolder == 1) {
			showAlert("Not a directory", "Warning", this);
		} else {
			pwd.setText(file.getAbsolutePath());
		}
		super.onListItemClick(l, v, position, id);
	}

	/**
	 * Creates an Alert box
	 * 
	 * @param message
	 *            the message to show to user
	 * 
	 * @param title
	 *            the title of alert box
	 * 
	 * @param ctx
	 *            the context
	 * */
	private void showAlert(String message, String title, Context ctx) {
		// Create a builder
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(title);

		// Add buttons and listener
		builder.setMessage(message).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});

		// Create the dialog
		AlertDialog ad = builder.create();

		// Show
		ad.show();
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {

		case R.id.select_file_folder:

			// Select a path mode
			if (selectFolder == 1) {

				if (currentDirectory.canWrite()) {
					Intent data = new Intent();
					data.setData(Uri.parse(currentDirectory.getAbsolutePath()));
					setResult(RESULT_OK, data);
					AndroidFileBrowser.this.finish();
				} else
					showAlert("Write Permission Denied", "Warning", this);

			}
			// Select a file mode
			else {
				file = new File(pwd.getText().toString());
				if (file.exists()) {
					if (file.canRead()) {
						Intent data = new Intent();
						data.setData(Uri.parse(pwd.getText().toString()));
						setResult(RESULT_OK, data);
						AndroidFileBrowser.this.finish();
					} else
						showAlert("Read Permission Denied", "Warning", this);
				} else
					showAlert("File not available", "Warning", this);
			}
			break;
		}
	}

	/**
	 * Control when back button is pressed
	 * */
	@Override
	public void onBackPressed() {
		finish();
	}

	/**
	 * Public Inner Class which help to put images for each file that is
	 * different type
	 * 
	 * */
	public class MyCustomAdapter extends ArrayAdapter<String> {

		List<String> myList;

		/**
		 * Constructor
		 */
		public MyCustomAdapter(Context context, int textViewResourceId, List<String> objects) {

			super(context, textViewResourceId, objects);
			myList = objects;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View row = convertView;
			// Check If Row Is Null
			if (row == null) {
				// Make New Layoutinflater
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				row = vi.inflate(R.xml.file_row, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.file);
			String stringFile = myList.get(position).toString();

			// Change The Symbol Of The Home Directory
			if (stringFile.equals(getString(R.string.homeDir)))
				label.setText("~");
			else
				label.setText(stringFile);

			File file = new File(stringFile);
			ImageView icon = (ImageView) row.findViewById(R.id.icon);

			if (file.isDirectory())
				icon.setImageResource(R.drawable.directory);
			else
				icon.setImageResource(FindDrawable(stringFile));

			return row;
		}

		/**
		 * Find the right icon for each file type
		 * 
		 * @param file
		 *            the file to get image source
		 * 
		 * @return the id of image source
		 * */
		// 
		private int FindDrawable(String file) {
			if (file.endsWith(".txt")) {
				return R.drawable.txt;
			} else if (file.endsWith(".pdf")) {
				return R.drawable.pdf;
			} else if (file.endsWith(".exe")) {
				return R.drawable.exe;
			} else if (file.endsWith(".apk")) {
				return R.drawable.apk;
			} else if (file.endsWith(".png")) {
				return R.drawable.png;
			} else if (file.endsWith(".gif")) {
				return R.drawable.gif;
			} else if (file.endsWith(".jpg")) {
				return R.drawable.jpg;
			} else if (file.endsWith(".rar")) {
				return R.drawable.rar;
			} else if (file.endsWith(".zip")) {
				return R.drawable.zip;
			} else if (file.endsWith(".gz")) {
				return R.drawable.gz;
			} else if (file.endsWith(".mp3") || file.endsWith(".wav") || file.endsWith(".amp")) {
				return R.drawable.sound;
			} else if (file.endsWith(".mp4") || file.endsWith(".avi") || file.endsWith(".flv")) {
				return R.drawable.video;
			} else
				return R.drawable.txt;
		}
	}
}
