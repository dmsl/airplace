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

import cy.com.airplace.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.View;

public class ChooseAnAlgorithm extends PreferenceActivity {

	final CharSequence[] items = { "KNN", "WKNN", "MAP", "MMSE" };
	final CharSequence[] alg_items = { "K Nearest Neighbor (KNN) Algorithm", "Weighted K Nearest Neighbor (WKNN) Algorithm",
			"Probabilistic Maximum A Posteriori (MAP) Algorithm", "Probabilistic Minimum Mean Square Error (MMSE) Algorithm" };

	private Preference pref;
	private AlertDialog alert;
	private View view;

	/**
	 * Called when the activity is first created.
	 * */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesName(FindMe.SHARED_PREFS_INDOOR);

		addPreferencesFromResource(R.xml.choose_algorithm);

		pref = (Preference) findPreference("Short_Desc");
		view = pref.getView(getCurrentFocus(), getListView());

		// Customize the description of algorithms
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				// TODO Auto-generated method stub
				AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

				builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						// Show something if does not exit the app
						dialog.dismiss();
					}
				});

				builder.setTitle("Algorithms Short Description");
				builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						switch (item) {
						case 0:
							popup_msg(
									"The KNN algorithm estimates the unknown user location as the average of K locations which have the shortest Euclidean distances between the currently observed RSS fingerprint and the respective mean value fingerprints in the radiomap. For more information see:\n\nP. Bahl and V. Padmanabhan, �RADAR: an in-building RF-based user location and tracking system,� in IEEE International Conference on Computer Communications INFOCOM, vol. 2, 2000, pp. 775�784.",
									"K-Nearest Neighbor (KNN)", 0);
							break;
						case 1:
							popup_msg(
									"The WKNN algorithm is a variant of KNN and estimates the unknown user location as the weighted average of K locations which have the shortest Euclidean distances between the currently observed RSS fingerprint and the respective mean value fingerprints in the radiomap. The weight of each location is set equal to the inverse of the distance. For more information see:\n\nB. Li, J. Salter, A. Dempster, and C. Rizos, �Indoor positioning techniques based on wireless LAN,� in 1st IEEE International Conference on Wireless Broadband and Ultra Wideband Communications, 2006, pp. 13�16.",
									"Weighted K-Nearest Neighbor (WKNN)", 0);
							break;
						case 2:
							popup_msg(
									"The MAP algorithm is a probabilistic approach that estimates the unknown user location by maximizing the conditional probability of each location in the radiomap given the currently observed fingerprint (posterior). For more information see:\n\nM. Youssef and A. Agrawala, �The Horus WLAN location determination system,� in 3rd ACM International Conference on Mobile systems, applications, and services, 2005, pp. 205�218.",
									"Maximum A Posteriori (MAP)", 0);
							break;
						case 3:
							popup_msg(
									"The MMSE algorithm is a probabilistic approach that estimates the unknown user location by calculating the expected value of the location, which is equivalent to the weighted sum of the loations in the radiomap while the weights are set equal to the conditional probability of each location in the radiomap given the currently observed fingerprint (posterior). For more information see:\n\nT. Roos, P. Myllymaki, H. Tirri, P. Misikangas, and J. Sievanen, �A probabilistic approach to WLAN user location estimation,� International Journal of Wireless Information Networks, vol. 9, no. 3, pp. 155�164, Jul. 2002.",
									"Minimum Mean Square Error (MMSE)", 0);
							break;
						}

					}
				});

				alert = builder.create();

				alert.show();
				return true;

			}
		});
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

}