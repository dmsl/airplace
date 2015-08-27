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

package cy.com.Uploading;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;

public class UploadingSettings extends Thread {

	private String IP;
	private String PORT;
	private Socket socket = null;
	private String filename;

	private String errMsg;
	private ProgressDialog dialog;

	public UploadingSettings(String IP, String PORT, String filename, ProgressDialog dialog) {
		this.filename = filename;
		this.PORT = PORT;
		this.IP = IP;
		this.dialog = dialog;
	}

	public boolean connect() {
		try {
			socket = new Socket(IP, Integer.parseInt(PORT));
			return true;
		} catch (NumberFormatException e) {
			errMsg = "Specify IP/PORT to upload";
		} catch (Exception e) {
			errMsg = e.getMessage();
		}
		return false;
	}

	public String getErrMsg() {
		return errMsg;
	}

	public void run() {

		try {

			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			String inputLine, outputLine;

			inputLine = in.readLine();

			if (!inputLine.equalsIgnoreCase("+OK READY")) {
				out.close();
				in.close();
				socket.close();
				errMsg = "Server not available right now";
				handler.sendEmptyMessage(0);
				return;
			}

			outputLine = "UPLOAD rsslog";
			out.println(outputLine);

			inputLine = in.readLine();

			if (!inputLine.equalsIgnoreCase("+OK UPLOAD")) {
				out.close();
				in.close();
				socket.close();
				errMsg = "Server not available right now";
				handler.sendEmptyMessage(0);
				return;
			}

			BufferedReader reader = new BufferedReader(new FileReader(filename));

			while ((outputLine = reader.readLine()) != null) {
				out.println(outputLine);
			}

			reader.close();
			out.close();
			in.close();
			socket.close();
			dialog.dismiss();
			return;

		} catch (IOException e) {
			errMsg = e.getMessage();
			handler.sendEmptyMessage(0);
		}
	}

	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			dialog.dismiss();
		}
	};

}
