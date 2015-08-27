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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Algorithms {

    /**
     *
     * @param latestScanList
     *            the current scan list of APs
     * @param RM
     *            the constructed Radio Map
     *
     * @param algorithm_choice
     *            choice of several algorithms
     *
     * @param WeightsList
     *             the RBF weights
     *
     * @return the location of user
     */
    public static String ProcessingAlgorithms(ArrayList<LogRecord> latestScanList, RadioMapMean RM, int algorithm_choice, String parameter) {

        int i = 0, j = 0;

        ArrayList<String> MacAdressList = RM.getMacAdressList();
        ArrayList<String> Observed_RSS_Values = new ArrayList<String>();
        LogRecord temp_LR = null;

        // Check which mac addresses of radio map, we are currently listening.
        for (i = 0; i < MacAdressList.size(); ++i) {

            for (j = 0; j < latestScanList.size(); ++j) {

                temp_LR = latestScanList.get(j);

                // MAC Address Matched
                if (MacAdressList.get(i).compareTo(temp_LR.getBssid()) == 0) {
                    Observed_RSS_Values.add(String.valueOf(temp_LR.getRss()));
                    break;
                }
            }
            // A MAC Address is missing so we place a small NaN value
            if (j == latestScanList.size()) {
                Observed_RSS_Values.add(String.valueOf(RM.getDefaultNaNValue()));
            }
        }

        switch (algorithm_choice) {

            case 1:
                return KNN_WKNN_Algorithm(RM, Observed_RSS_Values, parameter, false);
            case 2:
                return KNN_WKNN_Algorithm(RM, Observed_RSS_Values, parameter, true);
            case 3:
                return MAP_MMSE_Algorithm(RM, Observed_RSS_Values, parameter, false);
            case 4:
                return MAP_MMSE_Algorithm(RM, Observed_RSS_Values, parameter, true);
        }
        return null;

    }

    /**
     * Calculates user location based on Weighted/Not Weighted K Nearest
     * Neighbor (KNN) Algorithm
     *
     * @param RM
     *            The radio map structure
     *
     * @param Observed_RSS_Values
     *            RSS values currently observed
     * @param parameter
     *
     * @param isWeighted
     *            To be weighted or not
     *
     * @param K
     *            The number of locations used
     *
     * @return The estimated user location
     */
    private static String KNN_WKNN_Algorithm(RadioMapMean RM, ArrayList<String> Observed_RSS_Values, String parameter, boolean isWeighted) {

        ArrayList<String> RSS_Values;
        float curResult = 0;
        ArrayList<LocDistance> LocDistance_Results_List = new ArrayList<LocDistance>();
        String myLocation = null;
        int K;

        try {
            K = Integer.parseInt(parameter);
        } catch (Exception e) {
            System.err.println("Error parsing K in KNN-WKNN: " + e.getMessage());
            return null;
        }

        // Construct a list with locations-distances pairs for currently
        // observed RSS values
        for (String location : RM.getLocationRSS_HashMap().keySet()) {
            RSS_Values = RM.getLocationRSS_HashMap().get(location);
            curResult = calculateEuclideanDistance(RSS_Values, Observed_RSS_Values);

            if (curResult == Float.NEGATIVE_INFINITY) {
                return null;
            }

            LocDistance_Results_List.add(0, new LocDistance(curResult, location));
        }

        // Sort locations-distances pairs based on minimum distances
        Collections.sort(LocDistance_Results_List, new Comparator<LocDistance>() {

            public int compare(LocDistance gd1, LocDistance gd2) {
                return (gd1.getDistance() > gd2.getDistance() ? 1 : (gd1.getDistance() == gd2.getDistance() ? 0 : -1));
            }
        });

        if (!isWeighted) {
            myLocation = calculateAverageKDistanceLocations(LocDistance_Results_List, K);
        } else {
            myLocation = calculateWeightedAverageKDistanceLocations(LocDistance_Results_List, K);
        }

        return myLocation;

    }

    /**
     * Calculates user location based on Probabilistic Maximum A Posteriori
     * (MAP) Algorithm or Probabilistic Minimum Mean Square Error (MMSE)
     * Algorithm
     *
     * @param RM
     *            The radio map structure
     *
     * @param Observed_RSS_Values
     *            RSS values currently observed
     * @param parameter
     *
     * @param isWeighted
     *            To be weighted or not
     *
     * @return The estimated user location
     */
    private static String MAP_MMSE_Algorithm(RadioMapMean RM, ArrayList<String> Observed_RSS_Values, String parameter, boolean isWeighted) {

        ArrayList<String> RSS_Values;
        double curResult = 0.0d;
        String myLocation = null;
        double highestProbability = Double.NEGATIVE_INFINITY;
        ArrayList<LocDistance> LocDistance_Results_List = new ArrayList<LocDistance>();
        float sGreek;

        try {
            sGreek = Float.parseFloat(parameter);
        } catch (Exception e) {
            System.err.println("Error parsing parameter of MAP-MMSE: " + e.getMessage());
            return null;
        }

        // Find the location of user with the highest probability
        for (String location : RM.getLocationRSS_HashMap().keySet()) {

            RSS_Values = RM.getLocationRSS_HashMap().get(location);
            curResult = calculateProbability(RSS_Values, Observed_RSS_Values, sGreek);

            if (curResult == Double.NEGATIVE_INFINITY) {
                return null;
            } else if (curResult > highestProbability) {
                highestProbability = curResult;
                myLocation = location;
            }

            if (isWeighted) {
                LocDistance_Results_List.add(0, new LocDistance(curResult, location));
            }
        }

        if (isWeighted) {
            myLocation = calculateWeightedAverageProbabilityLocations(LocDistance_Results_List);
        }

        return myLocation;
    }
    
    /**
     * Calculates the Euclidean distance between the currently observed RSS
     * values and the RSS values for a specific location.
     *
     * @param l1
     *            RSS values of a location in radiomap
     * @param l2
     *            RSS values currently observed
     *
     * @return The Euclidean distance, or MIN_VALUE for error
     */
    private static float calculateEuclideanDistance(ArrayList<String> l1, ArrayList<String> l2) {

        float finalResult = 0;
        float v1;
        float v2;
        float temp;
        String str;

        for (int i = 0; i < l1.size(); ++i) {

            try {
                str = l1.get(i);
                v1 = Float.valueOf(str.trim()).floatValue();
                str = l2.get(i);
                v2 = Float.valueOf(str.trim()).floatValue();
            } catch (Exception e) {
                System.err.println("Error while computing Euclidean Distance: " + e.getMessage());
                return Float.NEGATIVE_INFINITY;
            }

            // do the procedure
            temp = v1 - v2;
            temp *= temp;

            // do the procedure
            finalResult += temp;
        }
        return ((float) Math.sqrt(finalResult));
    }

    /**
     * Calculates the Probability of the user being in the currently observed
     * RSS values and the RSS values for a specific location.
     *
     * @param l1
     *            RSS values of a location in radiomap
     * @param l2
     *            RSS values currently observed
     *
     * @return The Probability for this location, or MIN_VALUE for error
     */
    public static double calculateProbability(ArrayList<String> l1, ArrayList<String> l2, float sGreek) {

        double finalResult = 1;
        float v1;
        float v2;
        double temp;
        String str;

        for (int i = 0; i < l1.size(); ++i) {

            try {
                str = l1.get(i);
                v1 = Float.valueOf(str.trim()).floatValue();
                str = l2.get(i);
                v2 = Float.valueOf(str.trim()).floatValue();
            } catch (Exception e) {
                System.err.println("Error while computing Euclidean Distance: " + e.getMessage());
                return Double.NEGATIVE_INFINITY;
            }

            temp = v1 - v2;

            temp *= temp;

            temp = -temp;

            temp /= (double) (sGreek * sGreek);
            temp = (double) Math.exp(temp);

            finalResult *= temp;
        }
        return finalResult;
    }

    /**
     * Calculates the Average of the K locations that have the shortest
     * distances D
     *
     * @param LocDistance_Results_List
     *            Locations-Distances pairs sorted by distance
     * @param K
     *            The number of locations used
     * @return The estimated user location, or null for error
     */
    private static String calculateAverageKDistanceLocations(ArrayList<LocDistance> LocDistance_Results_List, int K) {

        float sumX = 0.0f;
        float sumY = 0.0f;

        String[] LocationArray = new String[2];
        float x, y;

        int K_Min = K < LocDistance_Results_List.size() ? K : LocDistance_Results_List.size();

        // Calculate the sum of X and Y
        for (int i = 0; i < K_Min; ++i) {
            LocationArray = LocDistance_Results_List.get(i).getLocation().split(" ");

            try {
                x = Float.valueOf(LocationArray[0].trim()).floatValue();
                y = Float.valueOf(LocationArray[1].trim()).floatValue();
            } catch (Exception e) {
                System.err.println("Error while calculating Average K Distance Locations: " + e.getMessage());
                return null;
            }

            sumX += x;
            sumY += y;
        }

        // Calculate the average
        sumX /= K_Min;
        sumY /= K_Min;

        return sumX + " " + sumY;

    }

    /**
     * Calculates the Weighted Average of the K locations that have the shortest
     * distances D
     *
     * @param LocDistance_Results_List
     *            Locations-Distances pairs sorted by distance
     * @param K
     *            The number of locations used
     * @return The estimated user location, or null for error
     */
    public static String calculateWeightedAverageKDistanceLocations(ArrayList<LocDistance> LocDistance_Results_List, int K) {

        double LocationWeight = 0.0f;
        double sumWeights = 0.0f;
        double WeightedSumX = 0.0f;
        double WeightedSumY = 0.0f;

        String[] LocationArray = new String[2];
        float x, y;

        int K_Min = K < LocDistance_Results_List.size() ? K : LocDistance_Results_List.size();

        // Calculate the weighted sum of X and Y
        for (int i = 0; i < K_Min; ++i) {

            LocationWeight = 1 / LocDistance_Results_List.get(i).getDistance();
            LocationArray = LocDistance_Results_List.get(i).getLocation().split(" ");

            try {
                x = Float.valueOf(LocationArray[0].trim()).floatValue();
                y = Float.valueOf(LocationArray[1].trim()).floatValue();
            } catch (Exception e) {
                System.err.println("Error while calculating Weighted Average K Distance Locations: " + e.getMessage());
                return null;
            }

            sumWeights += LocationWeight;
            WeightedSumX += LocationWeight * x;
            WeightedSumY += LocationWeight * y;

        }

        WeightedSumX /= sumWeights;
        WeightedSumY /= sumWeights;

        return WeightedSumX + " " + WeightedSumY;
    }

    /**
     * Calculates the Weighted Average over ALL locations where the weights are
     * the Normalized Probabilities
     *
     * @param LocDistance_Results_List
     *            Locations-Probability pairs
     *
     * @return The estimated user location, or null for error
     */
    public static String calculateWeightedAverageProbabilityLocations(ArrayList<LocDistance> LocDistance_Results_List) {

        double sumProbabilities = 0.0f;
        double WeightedSumX = 0.0f;
        double WeightedSumY = 0.0f;
        double NP;
        float x, y;
        String[] LocationArray = new String[2];

        // Calculate the sum of all probabilities
        for (int i = 0; i < LocDistance_Results_List.size(); ++i) {
            sumProbabilities += LocDistance_Results_List.get(i).getDistance();
        }

        // Calculate the weighted (Normalized Probabilities) sum of X and Y
        for (int i = 0; i < LocDistance_Results_List.size(); ++i) {
            LocationArray = LocDistance_Results_List.get(i).getLocation().split(" ");

            try {
                x = Float.valueOf(LocationArray[0].trim()).floatValue();
                y = Float.valueOf(LocationArray[1].trim()).floatValue();
            } catch (Exception e) {
                System.err.println("Error while calculating Weighted Average Probability Locations: " + e.getMessage());
                return null;
            }

            NP = LocDistance_Results_List.get(i).getDistance() / sumProbabilities;

            WeightedSumX += (x * NP);
            WeightedSumY += (y * NP);

        }

        return WeightedSumX + " " + WeightedSumY;

    }
}
