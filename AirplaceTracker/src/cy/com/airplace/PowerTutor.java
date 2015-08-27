/*
 * AirPlace:  The Airplace Project is an OpenSource Indoor and Outdoor
 * Localization solution using WiFi RSS (Receive Signal Strength).
 * The AirPlace Project consists of three parts:
 *
 *  1) The AirPlace Logger (Ideal for collecting RSS logs)
 *  2) The AirPlace Server (Ideal for transforming the collected RSS logs
 *  to meaningful RadioMap files)
 *  3) The AirPlace Tracker (Ideal for using the RadioMap files for
 *  indoor localization)
 *
 * It is ideal for spaces where GPS signal is not sufficient.
 *
 * Authors:
 *
 * C. Laoudias, G. Constantinou, M. Constantinides, S. Nicolaou,
 * D. Zeinalipour-Yazti and C. G. Panayiotou
 *
 * Copyright (c) 2011, KIOS Research Center and Data Management Systems Lab,
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
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class PowerTutor {

	public static String getLastFilePowerTutor(String path) {
		File dir = new File(path);

		Long l = -1l;
		Long curLogTime;

		String[] children = dir.list();
		if (children == null) {
			return null;
		} else {
			for (int i = 0; i < children.length; i++) {
				// Get filename of file or directory
				if (children[i].contains("PowerTrace")) {
					curLogTime = Long.parseLong((String) children[i].subSequence(10, 23));
					if (curLogTime > l)
						l = curLogTime;
				}
			}
		}
		if (l == -1)
			return null;
		else
			return "PowerTrace" + l + ".log";

	}

	public static Power getPower(String file) {

		String strLine;
		String pID = null;
		int wifi = 0, cpu = 0;

		try {

			FileInputStream fstream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			while ((strLine = br.readLine()) != null) {

				if (pID == null && strLine.contains("cy.com.findme")) {
					pID = getProcessId(strLine.split(" "));
					continue;
				}

				if (pID != null) {
					String[] str = strLine.split(" ");
					if (str.length == 2) {
						if (str[0].contains(pID) && str[0].contains("CPU-" + pID)) {
							cpu = cpu + Integer.parseInt(str[1]);
						}
						if (str[0].contains(String.valueOf(pID)) && str[0].contains("Wifi-" + pID)) {
							wifi = wifi + Integer.parseInt(str[1]);
						}
					}
				}
			}

			in.close();

		} catch (Exception e) {
			return null;

		}

		return new Power(cpu, wifi);

	}

	public static String getProcessId(String[] str) {

		if (str.length == 3) // org.com.clientpositioning
			return str[1];

		return null;
	}

	public static void writePowerInFile(ArrayList<String[]> str, String id, String Path) throws IOException {

		FileWriter fstream = new FileWriter(Path);

		BufferedWriter out = new BufferedWriter(fstream);

		out.write("CPU: \n");
		for (int i = 0; i < str.size(); ++i) {
			if (str.get(i).length == 2) {
				// System.out.println(str.get(i)[0]);
				if (str.get(i)[0].contains(id) && str.get(i)[0].contains("CPU-" + id)) {
					out.write(str.get(i)[1] + "\n");

				}
			}
		}

		out.write("\nWIFI: \n");
		for (int i = 0; i < str.size(); ++i) {
			if (str.get(i).length == 2) {
				// System.out.println(str.get(i)[0]);
				if (str.get(i)[0].contains(id) && str.get(i)[0].contains("Wifi-" + id)) {
					out.write(str.get(i)[1] + "\n");

				}
			}
		}
		out.close();
	}

}

class Power {

	int CPU;
	int WIFI;

	Power(int cpu, int wifi) {
		CPU = cpu;
		WIFI = wifi;
	}
}