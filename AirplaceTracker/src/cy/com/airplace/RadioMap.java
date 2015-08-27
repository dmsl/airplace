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

package cy.com.airplace;/**
 *

 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class RadioMap {

	private File RadiomapMean_File = null;
	private ArrayList<String> MacAdressList = null;
	private HashMap<String, ArrayList<String>> LocationRSS_HashMap = null;
	private ArrayList<String> OrderList = null;

	public RadioMap() {
		super();
		MacAdressList = new ArrayList<String>();
		LocationRSS_HashMap = new HashMap<String, ArrayList<String>>();
		OrderList = new ArrayList<String>();
	}

	/**
	 * Getter of MAC Address list in file order
	 * 
	 * @return
	 *            the list of MAC Addresses
	 * */
	public ArrayList<String> getMacAdressList() {
		return MacAdressList;
	}

	/**
	 * Getter of HashMap Location-RSS Values list in no particular order
	 * 
	 * @return
	 *            the HashMap Location-RSS Values
	 * */
	public HashMap<String, ArrayList<String>> getLocationRSS_HashMap() {
		return LocationRSS_HashMap;
	}

	/**
	 * Getter of Location list in file order
	 * 
	 * @return
	 *            the Location list
	 * */
	public ArrayList<String> getOrderList() {
		return OrderList;
	}

	/**
	 * Getter of radio map mean filename
	 * 
	 * @return
	 *            the filename of radiomap mean used
	 * */
	public File getRadiomapMean_File() {
		return this.RadiomapMean_File;
	}
	
	/**
	 * Construct a radio map
	 * 
	 * @param inFile
	 *            the radio map file to read
	 * 
	 * @return
	 *            true if radio map constructed successfully, otherwise false
	 * */
	public boolean ConstructRadioMap(File inFile) {

		if (!inFile.exists() || !inFile.canRead()) {
			return false;
		}

		this.RadiomapMean_File = inFile;

		this.OrderList.clear();
		this.MacAdressList.clear();
		this.LocationRSS_HashMap.clear();

		ArrayList<String> RSS_Values = null;
		BufferedReader reader = null;
		String line = null;
		String[] temp = null;
		String key = null;

		try {

			reader = new BufferedReader(new FileReader(inFile));

			// Read the first line
			line = reader.readLine();

			// Must exists
			if (line == null)
				return false;

			line = line.replace(", ", " ");
			temp = line.split(" ");

			// Must have more than 4 fields 
			if (temp.length < 4)
				return false;

			// Store all Mac Addresses
			for (int i = 3; i < temp.length; ++i)
				this.MacAdressList.add(temp[i]);

			while ((line = reader.readLine()) != null) {
				
				if (line.trim().equals(""))
					continue;
				
				line = line.replace(", ", " ");
				temp = line.split(" ");

				if (temp.length < 3)
					return false;

				key = temp[0] + " " + temp[1];

				RSS_Values = new ArrayList<String>();

				for (int i = 2; i < temp.length; ++i)
					RSS_Values.add(temp[i]);

				// Equal number of MAC address and RSS Values
				if (this.MacAdressList.size() != RSS_Values.size())
					return false;

				this.LocationRSS_HashMap.put(key, RSS_Values);

				this.OrderList.add(key);
			}
			reader.close();
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

	public String toString() {
		String str = "MAC Adresses: ";
		ArrayList<String> temp;
		for (int i = 0; i < MacAdressList.size(); ++i)
			str += MacAdressList.get(i) + " ";

		str += "\nLocations\n";
		for (String location : LocationRSS_HashMap.keySet()) {
			str += location + " ";
			temp = LocationRSS_HashMap.get(location);
			for (int i = 0; i < temp.size(); ++i)
				str += temp.get(i) + " ";
			str += "\n";
		}

		return str;
	}
}
