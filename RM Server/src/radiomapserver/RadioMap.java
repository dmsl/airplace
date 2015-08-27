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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package radiomapserver;

import Jama.Matrix;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 *
 * Class that constructs the radio map, reading all RSS files in RSS folder.
 * Writes to disk the radio map, radio map mean and parameters.
 */
public class RadioMap {

    private final HashMap<String, HashMap<String, ArrayList<Integer>>> RadioMap;
    private final boolean isIndoor;
    private final File rss_folder;
    private final String radiomap_filename;
    private final String radiomap_mean_filename;
    private final String radiomap_parameters_filename;
    private final int Algoritmhs_num = 4;
    private final int defaultNaNValue;
    private int K_KNN = -1;
    private int K_WKNN = -1;
    private double S_MAP = -1;
    private double S_MMSE = -1;
    private int MIN_RSS = Integer.MAX_VALUE;
    private int MAX_RSS = Integer.MIN_VALUE;

    /**
     * Constructor of the RadioMap class
     *
     * @param rss_folder
     *            the folder contains all RSS log files
     *
     * @param radiomap_filename
     *            the filename to write the radio map and use it to write other files
     *
     * */
    public RadioMap(File rss_folder, String radiomap_filename, int defaultNaNValue) {
        RadioMap = new HashMap<String, HashMap<String, ArrayList<Integer>>>();
        this.rss_folder = rss_folder;
        this.radiomap_filename = radiomap_filename;
        this.radiomap_mean_filename = radiomap_filename.replace(".", "-mean.");
        this.radiomap_parameters_filename = radiomap_filename.replace(".", "-parameters.");
        this.defaultNaNValue = defaultNaNValue;
        this.isIndoor = this.radiomap_filename.contains("indoor");
    }

    /**
     * Creates and writes the radio map to disk.
     *
     * @return
     *          true if radio map constructed and wrote to disk successfully, otherwise false
     * */
    public boolean createRadioMap() {

        if (!rss_folder.exists() || !rss_folder.isDirectory()) {
            return false;
        }

        RadioMap.clear();

        createRadioMapFromPath(rss_folder);

        //createRadioMapUrgent("radio-map.txt");

        if (!writeRadioMap()) {
            return false;
        }
        return true;
    }

    private void createRadioMapUrgent(String inFile) {

        BufferedReader reader = null;
        int count = 0;
        float sum[] = new float[9];
        String temp_loc = "";
        String location = "";
        FileOutputStream fos;

        try {
            fos = new FileOutputStream(radiomap_mean_filename, false);
        } catch (FileNotFoundException e) {
            System.err.println("Error create Radio map urgent: " + e.getMessage());
            return;
        }

        for (int i = 0; i < 9; ++i) {
            sum[i] = 0;
        }
        try {
            String line;
            FileReader fr = new FileReader(inFile);
            reader = new BufferedReader(fr);

            while ((line = reader.readLine()) != null) {

                /* Ignore the labels */
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }

                /* Remove commas */
                line = line.replace(", ", " ");

                /* Split fields */
                String[] temp = line.split(" ");

                location = temp[0] + ", " + temp[1];

                if (temp_loc.equalsIgnoreCase("") || temp_loc.equalsIgnoreCase(location)) {

                    for (int i = 2; i < temp.length; ++i) {
                        sum[i - 2] += Float.parseFloat(temp[i]);
                    }
                    count++;
                    temp_loc = temp[0] + ", " + temp[1];
                    continue;
                } else {

                    fos.write(temp_loc.getBytes());
                    for (int i = 0; i < 9; ++i) {
                        fos.write((", " + (sum[i] / count)).getBytes());
                    }
                    fos.write("\n".getBytes());
                    for (int i = 0; i < 9; ++i) {
                        sum[i] = 0;
                    }

                    for (int i = 2; i < temp.length; ++i) {
                        sum[i - 2] += Float.parseFloat(temp[i]);
                    }

                    temp_loc = temp[0] + ", " + temp[1];
                    count = 1;
                }

            }
            fos.write(temp_loc.getBytes());
            for (int i = 0; i < 9; ++i) {
                fos.write((", " + (sum[i] / count)).getBytes());
            }

            fos.close();
            fr.close();
            reader.close();
        } catch (Exception e) {
            System.err.println("Error create Radio map urgent: " + e.getMessage());
        }
    }

    /**
     * Creates recursively the Radio map, reading all files in Folder
     *
     * @param inFile
     *             the new file to read
     * */
    private void createRadioMapFromPath(File inFile) {

        if (inFile.exists()) {

            // If is folder
            if (inFile.canExecute() && inFile.isDirectory()) {
                String[] list = inFile.list();

                // Read recursively the path
                if (list != null) {
                    for (int i = 0; i < list.length; i++) {
                        createRadioMapFromPath(new File(inFile, list[i]));
                    }
                }
            } // Parse all files
            else if (inFile.canRead() && inFile.isFile()) {
                parseLogFileToRadioMap(inFile);
            }
        }
    }

    /**
     * Parses an RSS log file and store it to radio map structure.
     *
     * @param inFile
     *             the new file to read
     * */
    private void parseLogFileToRadioMap(File inFile) {

        int line_num = 0;
        BufferedReader reader = null;
        HashMap<String, ArrayList<Integer>> MACAddressMap = null;
        ArrayList<Integer> RSS_Values = null;
        String key = "";

        // Check that RSS file is OK
        if (!authenticateRSSlogFile(inFile)) {
            return;
        }

        try {
            String line = null;
            int RSS_Value = 0;
            FileReader fr = new FileReader(inFile);
            reader = new BufferedReader(fr);

            while ((line = reader.readLine()) != null) {

                line_num++;

                // Ignore the labels
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }

                // Remove commas
                line = line.replace(", ", " ");

                // Split fields
                String[] temp = line.split(" ");

                // Test and set RSS value is integer
                RSS_Value = Integer.parseInt(temp[4]);

                // Key of location X,Y
                key = temp[1] + " " + temp[2];

                // Get the current geolocation value
                MACAddressMap = RadioMap.get(key);

                // Geolocation first read so far
                if (MACAddressMap == null) {
                    MACAddressMap = new HashMap<String, ArrayList<Integer>>();
                    RSS_Values = new ArrayList<Integer>();
                    RSS_Values.add(RSS_Value);
                    MACAddressMap.put(temp[3], RSS_Values);
                    RadioMap.put(key, MACAddressMap);
                    continue;
                }

                // Get the RSS Values of MAC address
                RSS_Values = MACAddressMap.get(temp[3]);

                // MAC Address first read so far
                if (RSS_Values == null) {
                    RSS_Values = new ArrayList<Integer>();
                    RSS_Values.add(RSS_Value);
                    MACAddressMap.put(temp[3], RSS_Values);
                    continue;
                }

                // MAC Address already exists. Just insert to array list the new RSS value
                RSS_Values.add(RSS_Value);
            }
            fr.close();
            reader.close();
        } catch (Exception e) {
            System.err.println("Error while parsing RSS log file " + inFile.getAbsolutePath() + ": " + e.getMessage());
        }

    }

    /**
     * Parses an RSS log file and authenticates it
     *
     * @param inFile
     *             the RSS log file to read
     *
     * @return
     *              true if is authenticated, otherwise false
     *
     * */
    private boolean authenticateRSSlogFile(File inFile) {

        int line_num = 0;
        BufferedReader reader = null;

        try {
            String line = null;
            FileReader fr = new FileReader(inFile);
            reader = new BufferedReader(fr);

            while ((line = reader.readLine()) != null) {

                line_num++;

                // Check X, Y or Latitude, Longitude
                if (line.startsWith("#")) {

                    line = line.replace(", ", " ");
                    String[] temp = line.split(" ");

                    if (temp.length < 3) {
                        return false;
                    } // Must be # Timestamp, X, Y
                    else if (this.isIndoor && (!temp[2].trim().equalsIgnoreCase("X") || !temp[3].trim().equalsIgnoreCase("Y"))) {
                        return false;
                    } // Must be # Timestamp, Latitude, Longitude
                    else if (!this.isIndoor && (!temp[2].trim().equalsIgnoreCase("Latitude") || !temp[3].trim().equalsIgnoreCase("Longitude"))) {
                        return false;
                    }
                    continue;

                } else if (line.trim().isEmpty()) {
                    continue;
                }

                // Remove commas
                line = line.replace(", ", " ");

                // Split fields
                String[] temp = line.split(" ");

                // The file may be corrupted so ignore reading it
                if (temp.length != 5) {
                    throw new Exception("Line " + line_num + " length is not equal to 5.");
                }

                // Test that X, Y are floats
                Float.parseFloat(temp[1]);
                Float.parseFloat(temp[2]);

                // MAC address validation
                if (!temp[3].matches("[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}")) {
                    throw new Exception("Line " + line_num + " MAC Address is not valid.");
                }

                // Test RSS value is integer
                Integer.parseInt(temp[4]);
            }
            fr.close();
            reader.close();
        } catch (NumberFormatException nfe) {
            System.err.println("Error while authenticating RSS log file " + inFile.getAbsolutePath() + ": Line " + line_num + " " + nfe.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Error while authenticating RSS log file " + inFile.getAbsolutePath() + ": " + e.getMessage());
            return false;
        }

        return true;
    }

    /****************************************************************************************************************/
    /****************************************************************************************************************/
    /****************************************************************************************************************/
    public boolean writeParameters(String inFile) {
        FileOutputStream fos = null;

        RadioMapMean RM = new RadioMapMean(this.isIndoor, this.defaultNaNValue);

        if (!RM.ConstructRadioMap(new File(radiomap_mean_filename))) {
            return false;
        }

        if (!find_MIN_MAX_Values()) {
            return false;
        }

        for (int i = 1; i <= this.Algoritmhs_num; ++i) {

            if (!calculateAlgorithmParameter(RM, inFile, i)) {
                return false;
            }
        }

        File radiomap_parameters_file = new File(radiomap_parameters_filename);
        try {
            fos = new FileOutputStream(radiomap_parameters_file, false);
        } catch (Exception e) {
            System.err.println("Error while writing parameters: " + e.getMessage());
            radiomap_parameters_file.delete();
            return false;
        }

        try {
            /* Start the print out to Parameters file */
            fos.write(("NaN:" + this.defaultNaNValue).getBytes());
            fos.write(("\nKNN:" + this.K_KNN).getBytes());
            fos.write(("\nWKNN:" + this.K_WKNN).getBytes());
            fos.write(("\nMAP:" + this.S_MAP).getBytes());
            fos.write(("\nMMSE:" + this.S_MMSE).getBytes());

            fos.close();

        } catch (Exception e) {
            System.err.println("Error while writing parameters: " + e.getMessage());
            radiomap_parameters_file.delete();
            return false;
        }
        return true;
    }

    /**
     * Find Min and Max RSS value
     */
    private boolean find_MIN_MAX_Values() {

        FileReader frRadiomap = null;
        BufferedReader readerRadiomap = null;

        try {
            String radiomapLine = null;

            frRadiomap = new FileReader(radiomap_filename);
            readerRadiomap = new BufferedReader(frRadiomap);


            while ((radiomapLine = readerRadiomap.readLine()) != null) {

                // Check X, Y or Latitude, Longitude
                if (radiomapLine.startsWith("#")) {
                    radiomapLine = radiomapLine.replace(", ", " ");
                    String[] temp = radiomapLine.split(" ");

                    if (temp.length < 2) {
                        return false;
                    } // Must be # Timestamp, X, Y
                    else if (this.isIndoor && (!temp[1].trim().equalsIgnoreCase("X") || !temp[2].trim().equalsIgnoreCase("Y"))) {
                        return false;
                    } // Must be # Timestamp, Latitude, Longitude
                    else if (!this.isIndoor && (!temp[1].trim().equalsIgnoreCase("Latitude") || !temp[2].trim().equalsIgnoreCase("Longitude"))) {
                        return false;
                    }
                    // Ignore the labels
                    continue;

                } else if (radiomapLine.trim().isEmpty()) {
                    // Ignore the labels
                    continue;
                }

                // Remove commas
                radiomapLine = radiomapLine.replace(", ", " ");

                // Split fields
                String[] temp = radiomapLine.split(" ");

                // The file may be corrupted so ignore reading it
                if (temp.length < 3) {
                    return false;
                }

                for (int i = 2; i < temp.length; ++i) {
                    set_MIN_MAX_RSS(Integer.parseInt(temp[i]));
                }
            }

            frRadiomap.close();
            readerRadiomap.close();

        } catch (Exception e) {
            System.err.println("Error while finding min and max RSS values: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Sets MIN and MAX RSS Values to compute the parameters later for SNAP
     *
     * @param RSS_Value
     *                  the current RSS value
     * */
    private void set_MIN_MAX_RSS(int RSS_Value) {

        if (MIN_RSS > RSS_Value && RSS_Value != this.defaultNaNValue) {
            MIN_RSS = RSS_Value;
        }

        if (MAX_RSS < RSS_Value) {
            MAX_RSS = RSS_Value;
        }
    }

    private boolean calculateAlgorithmParameter(RadioMapMean RM, String inFile, int algorithm_choice) {


        int start = 0;
        int end = 0;

        if (RM == null) {
            return false;
        }

        switch (algorithm_choice) {

            case 1:
                System.out.print("Calculating KNN parameter");
                start = 1;
                end = 15;
                break;
            case 2:
                System.out.print("Calculating WKNN parameter");
                start = 1;
                end = 15;
                break;
            case 3:
                System.out.print("Calculating MAP parameter");
                start = 1;
                end = 10;
                break;
            case 4:
                System.out.print("Calculating MMSE parameter");
                start = 1;
                end = 10;
                break;
            default:
                return false;
        }

        BufferedReader reader = null;
        String line = null;
        String[] temp = null;

        ArrayList<String> MacAdressList = new ArrayList<String>();
        ArrayList<LogRecord> OfflineScanList = new ArrayList<LogRecord>();

        String test_geo = null;
        double pos_error = 0.0d;
        double sum_pos_error = 0.0d;
        int count_pos = 0;
        double average_pos_err_cur = Double.POSITIVE_INFINITY;
        double average_pos_err_best = Double.POSITIVE_INFINITY;

        for (int parameter = start; parameter <= end; ++parameter) {

            OfflineScanList.clear();
            MacAdressList.clear();
            sum_pos_error = 0;
            count_pos = 0;

            try {

                reader = new BufferedReader(new FileReader(inFile));

                // Read the first line
                line = reader.readLine();

                // Must exists
                if (line == null) {
                    return false;
                }

                // Store the Mac Addresses
                if (line.startsWith("#")) {
                    line = line.replace(", ", " ");
                    temp = line.split(" ");

                    // Must have more than 3 fields
                    if (temp.length < 4) {
                        return false;
                    } // Must be # Timestamp, X, Y
                    else if (this.isIndoor && (!temp[1].trim().equalsIgnoreCase("X") || !temp[2].trim().equalsIgnoreCase("Y"))) {
                        return false;
                    } // Must be # Timestamp, Latitude, Longitude
                    else if (!this.isIndoor && (!temp[1].trim().equalsIgnoreCase("Latitude") || !temp[2].trim().equalsIgnoreCase("Longitude"))) {
                        return false;
                    }

                    // Store all Mac Addresses
                    for (int i = 3; i < temp.length; ++i) {
                        if (!temp[i].matches("[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}")) {
                            return false;
                        }
                        MacAdressList.add(temp[i]);
                    }
                } else {
                    return false;
                }

                while ((line = reader.readLine()) != null) {

                    line = line.trim().replace(", ", " ");
                    temp = line.split(" ");

                    if (temp.length < 3) {
                        return false;
                    }

                    if (MacAdressList.size() != temp.length - 2) {
                        return false;
                    }

                    for (int i = 2; i < temp.length; ++i) {
                        LogRecord lr = new LogRecord(MacAdressList.get(i - 2), Integer.parseInt(temp[i]));
                        OfflineScanList.add(lr);
                    }

                    if (algorithm_choice == 6) {
                        test_geo = Algorithms.ProcessingAlgorithms(OfflineScanList, RM, algorithm_choice, String.valueOf(parameter + ", " + MIN_RSS + ", " + MAX_RSS));
                    } else {
                        test_geo = Algorithms.ProcessingAlgorithms(OfflineScanList, RM, algorithm_choice, String.valueOf(parameter));
                    }

                    if (test_geo == null) {
                        return false;
                    }

                    OfflineScanList.clear();

                    pos_error = calculateEuclideanDistance(temp[0] + " " + temp[1], test_geo);

                    if (pos_error != -1) {
                        sum_pos_error += pos_error;
                        count_pos++;
                    }
                }
                reader.close();
            } catch (Exception e) {
                System.err.println("Error while calculating parameters: " + e.getMessage());
                return false;
            }

            average_pos_err_cur = sum_pos_error / (double) count_pos;

            System.out.print("\nParameter: " + parameter + " Positions: " + count_pos + " Avg. Error: ");
            System.out.println(average_pos_err_cur);


            if (average_pos_err_cur < average_pos_err_best) {

                switch (algorithm_choice) {

                    case 1:
                        this.K_KNN = parameter;
                        break;
                    case 2:
                        this.K_WKNN = parameter;
                        break;
                    case 3:
                        this.S_MAP = parameter;
                        break;
                    case 4:
                        this.S_MMSE = parameter;
                        break;
                    default:
                        return false;
                }
                average_pos_err_best = average_pos_err_cur;
            }
        }

        System.out.println("Done!");
        return true;
    }

    private double calculateEuclideanDistance(String real, String estimate) {

        double pos_error;
        String[] temp_real;
        String[] temp_estimate;
        double x1, x2;

        temp_real = real.split(" ");
        temp_estimate = estimate.split(" ");

        try {
            x1 = Math.pow((Double.parseDouble(temp_real[0]) - Double.parseDouble(temp_estimate[0])), 2);
            x2 = Math.pow((Double.parseDouble(temp_real[1]) - Double.parseDouble(temp_estimate[1])), 2);
        } catch (Exception e) {
            System.err.println("Error while calculating Euclidean distance: " + e.getMessage());
            return -1;
        }

        pos_error = Math.sqrt((x1 + x2));

        return pos_error;

    }

    /****************************************************************************************************************/
    /****************************************************************************************************************/
    /****************************************************************************************************************/
    /**
     * Write the new Radio Map
     * 
     * @return
     *              true if is written to disk, otherwise false
     * */
    private boolean writeRadioMap() {

        DecimalFormat dec = new DecimalFormat("###.#");
        HashMap<String, ArrayList<Integer>> MACAddressMap = null;
        ArrayList<Integer> RSS_Values = null;
        FileOutputStream fos = null;
        FileOutputStream fos_mean = null;
        String out = null;

        File radiomap_file = new File(radiomap_filename);
        File radiomap_mean_file = new File(radiomap_mean_filename);

        // If is empty no RSS log file parsed
        if (RadioMap.isEmpty()) {
            return false;
        }

        // Open output streams
        try {
            fos = new FileOutputStream(radiomap_file, false);
            fos_mean = new FileOutputStream(radiomap_mean_file, false);
        } catch (FileNotFoundException e) {
            System.err.println("Error while writing radio map: " + e.getMessage());
            radiomap_file.delete();
            radiomap_mean_file.delete();
            return false;
        }


        try {
            // Store all unique MAC addresses
            HashSet<String> s = new HashSet<String>();

            for (String Geolocation : RadioMap.keySet()) {
                MACAddressMap = RadioMap.get(Geolocation);
                s.addAll(MACAddressMap.keySet());
            }

            // Start the print out to Radio Map files
            if (isIndoor) {
                fos.write("# X, Y".getBytes());
                fos_mean.write("# X, Y".getBytes());
            } else {
                fos.write("# Latitude, Longitude".getBytes());
                fos_mean.write("# Latitude, Longitude".getBytes());
            }

            // Write MAC Addresses
            Iterator<String> iterator = s.iterator();
            while (iterator.hasNext()) {
                out = iterator.next();
                fos.write((", " + out).getBytes());
                fos_mean.write((", " + out).getBytes());
            }
            fos.write("\n".getBytes());
            fos_mean.write("\n".getBytes());


            // For each location print the Average RSS of every single MAC Address
            iterator = s.iterator();
            String MacAddress = null;
            for (String location : RadioMap.keySet()) {

                fos.write(location.replace(" ", ", ").getBytes());
                fos_mean.write(location.replace(" ", ", ").getBytes());

                MACAddressMap = RadioMap.get(location);

                iterator = s.iterator();

                int max = 0;

                // Find the maximum number of RSS Values recorded
                while (iterator.hasNext()) {
                    MacAddress = iterator.next();
                    RSS_Values = MACAddressMap.get(MacAddress);
                    if (RSS_Values != null && max < RSS_Values.size()) {
                        max = RSS_Values.size();
                    }
                }
                int i = 0;
                iterator = s.iterator();
                while (iterator.hasNext()) {

                    MacAddress = iterator.next();

                    RSS_Values = MACAddressMap.get(MacAddress);

                    // Write NaN RSS Value if no sample recorded for this mac address
                    if (RSS_Values == null || i > RSS_Values.size() - 1) {
                        fos.write((", " + this.defaultNaNValue).getBytes());
                    } // Write the value
                    else {
                        fos.write((", " + dec.format(RSS_Values.get(i))).getBytes());
                    }

                    // If this is the last mac address
                    if (!iterator.hasNext()) {

                        // Break if we reach the max number of samples in this location
                        if (i == max - 1) {
                            break;
                        } // Restart from the next RSS Value
                        else {
                            iterator = s.iterator();
                            ++i;
                            fos.write("\n".getBytes());
                            fos.write(location.replace(" ", ", ").getBytes());
                        }
                    }
                }

                fos.write("\n".getBytes());

                // For every MAC Address print average RSS value, or NaN Value if does not exist
                iterator = s.iterator();

                while (iterator.hasNext()) {

                    MacAddress = iterator.next();

                    RSS_Values = MACAddressMap.get(MacAddress);

                    // Write NaN RSS Value if no sample recorded for this mac address
                    if (RSS_Values == null) {
                        fos_mean.write((", " + this.defaultNaNValue).getBytes());
                    } // Calculate Average RSS Value for this location
                    else {
                        float rss_sum = (float) 0.0;
                        float rss_avg = (float) 0.0;
                        for (i = 0; i < RSS_Values.size(); ++i) {
                            rss_sum += RSS_Values.get(i);
                        }

                        // Add NaN value for those samples not recorded
                        for (; i < max; ++i) {
                            rss_sum += this.defaultNaNValue;
                        }

                        rss_avg = rss_sum / max;
                        fos_mean.write((", " + dec.format(rss_avg)).getBytes());
                    }
                }
                fos_mean.write("\n".getBytes());
            }

            fos.close();
            fos_mean.close();

        } catch (Exception e) {
            System.err.println("Error while writing radio map: " + e.getMessage());
            radiomap_file.delete();
            radiomap_mean_file.delete();
            return false;
        }
        return true;
    }
}
