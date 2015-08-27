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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.MediaStore.MediaColumns;
import cy.com.airplacersslogger.R;
import cy.com.airplacersslogger.FileBrowser.AndroidFileBrowser;

public class Prefs extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private static final int SELECT_IMAGE = 7;
	private static final int SELECT_PATH = 8;
	private static final int SELECT_FILE = 9;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

	
			getPreferenceManager().setSharedPreferencesName(RSSLogger.SHARED_PREFS_NAME);
		

		addPreferencesFromResource(R.xml.preferences);
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		
			getPreferenceManager().findPreference("image_custom").setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					i.setType("image/*");
					startActivityForResult(i, SELECT_IMAGE);
					return true;
				}
			});

		getPreferenceManager().findPreference("folder_browser").setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				Intent i = new Intent(getBaseContext(), AndroidFileBrowser.class);

				Bundle extras = new Bundle();
				extras.putBoolean("to_Browse", true);

				i.putExtras(extras);

				startActivityForResult(i, SELECT_PATH);
				return true;
			}
		});

		getPreferenceManager().findPreference("upload_file").setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				Intent i = new Intent(getBaseContext(), AndroidFileBrowser.class);

				Bundle extras = new Bundle();
				extras.putBoolean("to_Browse", false);
	
				i.putExtras(extras);

				startActivityForResult(i, SELECT_FILE);
				return true;
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);

		SharedPreferences customSharedPreference;

	
			customSharedPreference = getSharedPreferences(RSSLogger.SHARED_PREFS_NAME, MODE_PRIVATE);
		

		switch (requestCode) {

		case SELECT_IMAGE:
			if (resultCode == Activity.RESULT_OK) {
				Uri selectedImage = data.getData();
				String RealPath;
				SharedPreferences.Editor editor = customSharedPreference.edit();
				RealPath = getRealPathFromURI(selectedImage);
				editor.putString("image_custom", RealPath);
				editor.commit();
			}
			break;
		case SELECT_PATH:
			if (resultCode == Activity.RESULT_OK) {
				Uri selectedFolder = data.getData();
				String path = selectedFolder.toString();
				SharedPreferences.Editor editor = customSharedPreference.edit();
				editor.putString("folder_browser", path);
				editor.commit();
			}
			break;
		case SELECT_FILE:
			if (resultCode == Activity.RESULT_OK) {
				Uri selectedFile = data.getData();
				String file = selectedFile.toString();
				SharedPreferences.Editor editor = customSharedPreference.edit();
				editor.putString("upload_file", file);
				editor.commit();
			}
			break;
		}
	}

	public String getRealPathFromURI(Uri contentUri) {
		String[] proj = { MediaColumns.DATA };
		Cursor cursor = managedQuery(contentUri, proj, null, null, null);
		int column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onDestroy() {
		// Unregister the listener whenever a key changes
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	}

}