/*
* Copyright (c) 2011, KIOS Research Center and Data Management Systems Lab,
* University of Cyprus. All rights reserved.
*
* Redistribution and use in source and binary forms, with or without modification,
* are permitted provided that the following conditions are met:
*
*    * Redistributions of source code must retain the above copyright notice, this
*      list of conditions and the following disclaimer.
*    * Redistributions in binary form must reproduce the above copyright notice,
*      this list of conditions and the following disclaimer in the documentation
*      and/or other materials provided with the distribution.
*    * Neither the name of the Sony Ericsson Mobile Communication AB nor the names
*      of its contributors may be used to endorse or promote products derived from
*      this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
* ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
* IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
* INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
* BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
* DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
* OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
* OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package cy.com.airplacersslogger.FileBrowser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import cy.com.airplacersslogger.R;
import cy.com.Uploading.UploadingSettings;
import cy.com.airplacersslogger.RSSLogger;

public class AndroidFileBrowser extends ListActivity implements OnClickListener {
	// Enum For The Display Mode You Want
	private enum DISPLAYMODE {
		ABSOLUTE, RELATIVE;
	}

	private final DISPLAYMODE displayMode = DISPLAYMODE.ABSOLUTE;

	private List<String> directoryEntries = new ArrayList<String>();
	private File currentDirectory;
	private File homeDirectory;
	private File file;
	private TextView pwd;
	private Button select_file_folder;
	private boolean selectFolder = true;
	private String IP;
	private String PORT;
	private String folder_path;
	private String file_path;
	private SharedPreferences sharedPreferences;

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		setContentView(R.layout.main_choose_file_or_directory);

		pwd = (TextView) findViewById(R.id.pwd);
		select_file_folder = (Button) findViewById(R.id.select_file_folder);
		select_file_folder.setOnClickListener(this);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			selectFolder = extras.getBoolean("to_Browse");
		}

		sharedPreferences = getSharedPreferences(RSSLogger.SHARED_PREFS_NAME, MODE_PRIVATE);

		folder_path = sharedPreferences.getString("folder_browser", "");
		file_path = sharedPreferences.getString("upload_file", "");
		IP = sharedPreferences.getString("serverIP", "");
		PORT = sharedPreferences.getString("serverPORT", "");

		if (selectFolder)
			select_file_folder.setText("Save in this folder");
		else {
			select_file_folder.setText("Upload File");

			if (file_path != null && !file_path.trim().equals("") && new File(file_path).canRead())
				pwd.setText(file_path);
		}

		if (folder_path == null || folder_path.equals("")) {
			browseToHome();
		} else {
			currentDirectory = new File(makePath(folder_path));
			homeDirectory = new File("/");
			browseTo(currentDirectory);
		}
	}

	// This Function Browses To The Root-Directory Of The File-System.
	private void browseToHome() {

		currentDirectory = new File(makePath("/"));

		if (currentDirectory == null || isMount())
			currentDirectory = new File("/");

		homeDirectory = currentDirectory.getAbsoluteFile();
		browseTo(currentDirectory);

	}

	// check that the sdcard is mounted
	static public boolean isMount() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	private String makePath(String path) {
		if (path.length() == 0) {
			return "/";
		}

		File check = new File(path);
		if (!check.exists() || !check.canRead()) {
			return "/";
		}
		if (check.isDirectory())
			return path;
		else {
			return check.getParent();
		}
	}

	private void browseTo(final File aDirectory) {
		if (aDirectory.isDirectory()) {
			this.currentDirectory = aDirectory;
			fill(aDirectory.listFiles());

			if (selectFolder)
				pwd.setText(currentDirectory.getAbsolutePath());
		}
	}

	private void fill(File[] files) {
		this.directoryEntries.clear();
		// Add the "~" == "home directory"
		// And the ".." == 'Up one level'
		this.directoryEntries.add(getString(R.string.homeDir));
		if (this.currentDirectory.getParent() != null)
			this.directoryEntries.add(getString(R.string.parentDir));

		switch (this.displayMode) {

		case ABSOLUTE:
			for (File file : files) {
				this.directoryEntries.add(file.getAbsolutePath());
			}
			break;
		case RELATIVE: // On relative Mode, we have to add the current-path to
			// the beginning
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
		// TODO Auto-generated method stub
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
		} else if (!file.isDirectory() && selectFolder) {
			showAlert("Not a directory", "Warning", this);
		} else {
			pwd.setText(file.getAbsolutePath());
		}
		super.onListItemClick(l, v, position, id);

	}

	public static void showAlert(String message, String title, Context ctx) {
		// Create a builder
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(title);
		// add buttons and listener

		builder.setMessage(message).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});

		// Create the dialog
		AlertDialog ad = builder.create();
		// show
		ad.show();
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {

		case R.id.select_file_folder:

			if (selectFolder) {

				if (currentDirectory.canWrite()) {
					Intent data = new Intent();
					data.setData(Uri.parse(currentDirectory.getAbsolutePath()));
					setResult(RESULT_OK, data);
					finish();
				} else
					showAlert("Write Permission Denied", "Warning", this);

			} else {
				file = new File(pwd.getText().toString());
				if (file.exists()) {
					if (file.canRead()) {

						Intent data = new Intent();
						data.setData(Uri.parse(pwd.getText().toString()));
						setResult(RESULT_OK, data);

						ProgressDialog dialog = ProgressDialog.show(AndroidFileBrowser.this, "", "Uploading. Please wait...", true, false);

						UploadingSettings us = new UploadingSettings(IP, PORT, file.getAbsolutePath(), dialog);

						if (us.connect()) {
							us.start();
						} else {
							showAlert(us.getErrMsg(), "Error", this);
							dialog.dismiss();
						}
					} else
						showAlert("Read Permission Denied", "Warning", this);
				} else
					showAlert("No file selected", "Warning", this);
				break;
			}
		}
	}

	/**
	 * Control when back button is pressed
	 * */
	@Override
	public void onBackPressed() {
		finish();
	}

	// Public Inner Class Which Help Me To Put Png Image For Each File That Is
	// Different Type
	public class MyCustomAdapter extends ArrayAdapter<String> {
		List<String> myList;

		public MyCustomAdapter(Context context, int textViewResourceId, List<String> objects) {

			super(context, textViewResourceId, objects);
			myList = objects;
			// TODO Auto-generated constructor stub
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			// super.getView(position, convertView, parent);
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

		// Find The Right Icon For Each File Type
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
