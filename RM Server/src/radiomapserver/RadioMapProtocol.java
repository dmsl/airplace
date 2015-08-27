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
package radiomapserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class RadioMapProtocol {

    public enum STATES {

        WAITING, SENT_READY_MSG, SENDING_RADIOMAP, SENDING_PARAMETERS, UPLOADING_RSSFILE, DONE
    }
    private STATES state = STATES.WAITING;
    // Files to transfer
    private final File radiomap_file;
    private final File parameters_file;
    // A single line in files
    private String line = null;
    // Folder to store RSS log files
    private final File RSSFolder;
    // File to store RSS Log
    private File outFile = null;
    // To read files
    private BufferedReader reader = null;
    // To write file
    private BufferedWriter writer = null;
    // Server answers
    public static final String[] answers = {"+OK READY", "RADIOMAP", "+OK UPLOAD", "BUSY"};
    // Client requests
    public static final String[] requests = {"GET radiomap", "UPLOAD rsslog"};

    /**
     * Constructor of the protocol
     *
     * @param radiomap_file
     *            the radio map mean file to sent
     *
     * @param parameters_file
     *            the parameters file of algorithms to sent
     *
     * @param rbf_weights_file
     *            the RBF weights file to sent
     *
     * @param RSSFolder
     *            the folder to store all RSS files
     *
     * */
    RadioMapProtocol(File radiomap_file, File parameters_file, File RSSFolder) {
        super();
        this.radiomap_file = radiomap_file;
        this.parameters_file = parameters_file;
        this.RSSFolder = RSSFolder;
    }

    /**
     * Processes the input data and returns the output data.
     * Hold the states of server to know what the next move is.
     *
     * @param theInput
     *            the input data from clients
     *
     * @return
     *            the output data to clients
     * */
    public String processInput(String theInput) {
        String theOutput = null;

        // Send "+OK READY"
        if (state == STATES.WAITING) {
            theOutput = answers[0];
            state = STATES.SENT_READY_MSG;

        } // Check for download Radiomap, Upload RSS file or unrecognized command
        else if (state == STATES.SENT_READY_MSG) {

            // Received a "GET radiomap"
            if (theInput.equalsIgnoreCase(requests[0])) {
                // Check files that are available for distribution
                if (!OK_files()) {
                    // Send BUSY
                    theOutput = answers[3];
                    state = STATES.DONE;
                } else {
                    // Send RADIOMAP and first line of file
                    theOutput = answers[1];
                    state = STATES.SENDING_RADIOMAP;
                    try {
                        reader = new BufferedReader(new FileReader(radiomap_file));
                        line = reader.readLine().trim();
                        // Empty file
                        if (line == null) {
                            reader.close();
                            theOutput = "UNAVAILABLE: Radio map file is currently unavailable. Please try later.";
                            state = STATES.DONE;
                        } else {
                            theOutput += " " + line;
                        }
                    } catch (Exception fnf) {
                        theOutput = "UNAVAILABLE: Radio map file is currently unavailable. Please try later.";
                        state = STATES.DONE;
                    }
                }

            } // Received a "UPLOAD rsslog"
            else if (theInput.equalsIgnoreCase(requests[1])) {
                // Check folder that is available to store new files
                if (!OK_RSS_Folder()) {
                    // Send BUSY
                    theOutput = answers[3];
                    state = STATES.DONE;
                } else {
                    // Send "+OK UPLOAD"
                    theOutput = answers[2];
                    state = STATES.UPLOADING_RSSFILE;
                    // Find a filename that does not exist
                    outFile = findFilename();
                    try {
                        writer = new BufferedWriter(new FileWriter(outFile));
                    } catch (Exception fnf) {
                        System.out.println(fnf.getMessage());
                        theOutput = "UNAVAILABLE: Server is currently unavailable. Please try later.";
                        state = STATES.DONE;
                    }
                }
            } // Received an unrecognized command
            else {
                theOutput = "ERROR: Unrecognized command! Try again.";
                state = STATES.SENT_READY_MSG;
            }

        } // Sending radio map state
        else if (state == STATES.SENDING_RADIOMAP) {

            try {
                // Send a single line of radio map
                if ((line = reader.readLine()) != null) {
                    theOutput = line;
                } // Radiomap sent. Now send the parameters
                else {
                    reader.close();
                    reader = new BufferedReader(new FileReader(parameters_file));
                    theOutput = "PARAMETERS";
                    state = STATES.SENDING_PARAMETERS;
                }
            } catch (Exception e) {
                theOutput = "CORRUPTED: Corrupted file. Please try later.";
                state = STATES.DONE;
            }

        } // Retrieving RSS file
        else if (state == STATES.UPLOADING_RSSFILE) {

            try {
                // Write a single line of RSS file
                if (theInput != null) {
                    writer.write(theInput + "\n");
                } // RSS file stored. We are done.
                else {
                    writer.close();
                    state = STATES.DONE;
                }
                theOutput = null;

            } catch (Exception e) {
                theOutput = "ERROR: I/O error occured. Please try later.";
                state = STATES.DONE;
            }


        } // Sending parameters state
        else if (state == STATES.SENDING_PARAMETERS) {
            try {
                // Send a single line of parameters
                if ((line = reader.readLine()) != null) {
                    theOutput = line;
                } // Parameters sent. Now send the RBF weights
                else {
                    reader.close();
                }
            } catch (Exception e) {
                state = STATES.DONE;
            }
        } // Sending RBF weights state
         // Done state
        else if (state == STATES.DONE) {
            theOutput = null;
        }

        // Return the data
        return theOutput;
    }

    /**
     * Used to determine if the work is done
     * to close the connection with client
     *
     * @return
     *            true if sending or uploading file is done, or there was a problem (busy, corrupted file)
     *            , otherwise false
     * */
    public boolean isCompleted() {
        return state == STATES.DONE;
    }

    /**
     * Used to determine if all files uploading are readable
     *
     * @return
     *            true if all files are readable, otherwise false
     * */
    private boolean OK_files() {
        if (radiomap_file.canRead() && parameters_file.canRead()) {
            return true;
        }
        return false;
    }

    /**
     * Used to determine if folder is writable to store new RSS log files
     *
     * @return
     *            true if folder is writable, otherwise false
     * */
    private boolean OK_RSS_Folder() {
        if (RSSFolder.canWrite()) {
            return true;
        }
        return false;
    }

    /**
     * Getter of the current state
     *
     * @return
     *            the current protocol state
     * */
    STATES getState() {
        return state;
    }

    /**
     * Finds the first available RSS filename to store the new RSS log file
     *
     * @return
     *            the File to store new RSS log file
     * */
    private File findFilename() {

        File f = null;
        int RSS_Counter = 1;

        while (true) {
            f = new File(RSSFolder, "rsslog" + RSS_Counter + ".txt");

            try {
                if (f.createNewFile()) {
                    return f;
                }
            } catch (Exception ex) {
                return null;
            }

            RSS_Counter++;
        }
    }
}
