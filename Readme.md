# Airplace
The Airplace Indoor Positioning Platform for Android Smartphones

## GNU GPL Licence 

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

Υou should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.

Authors:
C. Laoudias, G.Larkou, G. Constantinou, M. Constantinides, S. Nicolaou (University of Cyprus)

Supervisors:
D. Zeinalipour-Yazti and C. G. Panayiotou (University of Cyprus)

Copyright (c) 2011, KIOS Research Center and Data Management Systems Lab (DMSL),
University of Cyprus. All rights reserved.

## Preface 

This document covers several topics that are not addressed in the narrated video tutorial (available at http://www2.ucy.ac.cy/~laoudias/pages/platform.html). It is recommended to watch the video tutorial, as well as the demo video and screenshots included in this webpage, before proceeding with these instructions.

We hope that you find our Airplace Indoor Positioning Platform useful for your research activities.  We would like to have your feedback, comments and remarks and of course any experiences and test results from your own experimental setups. Currently, we can offer only limited support and assistance on the code, due to lack of resources, but we will try to get back to you as soon as possible. Questions and feedback may be sent to airplace.positioning@gmail.com

In case you have any publications resulting from the Airplace platform, please cite the following paper(s):

- "The Airplace Indoor Positioning Platform for Android Smartphones", C. 
Laoudias, G. Constantinou, M. Constantinides, S. Nicolaou, D. 
Zeinalipour-Yazti, C. G. Panayiotou, "Proceedings of the 13th IEEE 
International Conference on Mobile Data Management" (MDM '12), IEEE 
Computer Society, Pages: 312--315, Bangalore, India, ISBN: 
978-0-7695-4713-8, 2012. (Best Demo Award).

- "Demo: the Airplace Indoor positioning platform", C. Laoudias, G. 
Constantinou, M. Constantinides, S. Nicolaou, D. Zeinalipour-Yazti and 
C. G. Panayiotou, "Proceedings of the 10th International Conference on 
Mobile Systems, Applications and Services" (Mobisys '12), Pages: 
467-468, Low Wood Bay, Lake District UK, June 25 - 29, ISBN: 
978-1-4503-1301-8, 2012.

You might also consider checking out the open and free cloud-based 
version of Airplace, coined Anyplace, which allows a multitude of new 
features.

http://anyplace.cs.ucy.ac.cy/

Enjoy Airplace!

The Airplace Team 

## Components 

Short description of the contents included in this release of the Airplace Indoor Positioning Platform.

- AirplaceLogger: Source code for the Airplace Logger application
- AirplaceTracker: Source code for the Airplace Tracker application
- Documentation: Documentation covering issues not addressed in the video tutorial (available at http://www2.ucy.ac.cy/~laoudias/pages/platform.html)
- Example: Example files for a small scale test of the Airplace platform
- Executables: The executables for the Logger and Tracker applications (Android .apk) and the RadioMap Server (Java .jar)
- RM Server: Source code for the RadioMap Server

## Installing the Airplace Logger and Tracker applications

Copy the .apk files on the sdcard of the Android smartphone. Start a file manager application on the smartphone, move to the folder where the .apk files are stored and tap on each file to begin the installation.

The RadioMap Server can be executed on any PC running Java.

## Preparing the floorplan map for Airplace

Preparing a map for Airplace, to be used with the Logger and Tracker applications, is very easy. All is needed is a map of the floorplan in jpeg format (e.g., mymap.jpg) and an associated configuration file (e.g., mymap.config) that keeps the actual Width and Height of the floorplan in meters (Note: The filename before the file extension must be the same for both files. An example is included in folder \Example\Map that can be modified appropriately with your own values).

Both files should be stored together inside a folder on the smartphone’s sdcard.

Zooming in/out of the map in the Logger and Tracker applications is easy by pressing and holding the finger on the map and then dragging it up/down to the desired zoom level. 

Hint: The actual values for the dimensions of the floorplan are not so important; approximate (or even dummy) values could be used instead and doing so will not affect the performance of the positioning algorithms. The actual values are only required to see meaningful (X,Y) locations in the Logger and Tracker applications.


## Using Airplace without putting the RadioMap Server online 

This section describes how to manually prepare the required data files in order to use the Airplace Tracker application without putting the RadioMap Server online. (Note: The RadioMap server handles conveniently the processing of the RSS log files collected with the Logger, as well as the distribution of the resulting radiomap and algorithm-specific parameters to the Tracker.)

1.	Use the Aiplace Logger, as described in the video tutorial, to collect the RSS data which are stored locally on the smartphone sdcard in a log file with a user-defined filename (e.g., ‘rss-log-mymap’). An example log file that contains the samples collected at 4 locations (5 samples\location) inside a single room can be found in folder \Example\Log.
2.	Copy the ‘rss-log-mymap’ file to the ‘indoor-rss-logs’ folder of the RadioMap Server directory tree and rename it to ‘rsslog1.txt’. In case of crowdsourcing, do the same for the log files collected by other users, carrying the same smartphone, and rename them to ‘rsslog2.txt’, ‘rsslog3.txt’, etc. (Note: All folders in the directory tree are created on the first execution of the Radiomap Server)
3.	Press the “Create Indoor Radiomap” button to get the radiomap files in the ‘indoor’ folder. The ‘indoor-radiomap.txt’ file contains all the recorded RSS samples, while the ‘indoor-radiomap-mean.txt’ contains only the mean RSS for each WiFi Access Point (AP) at each location. The latter, which is much smaller in size, is the radiomap file that is used for positioning with the Tracker. Example files can be found in folder \Example\Radiomap (Note: To accommodate the missing RSS values whenever an AP is not detected at a location, a default RSS value is used, which is lower than the sensitivity level, e.g. -110 dBm. This value can be user-defined from within the RadioMap Server through menu Edit → Settings)
4.	Create the testing data that are required for calculating the algorithm-specific parameters as described in the next section “How to create Test Data”. 
5.	The ‘indoor’ folder should now contain 3 files (i.e., ‘indoor-radiomap.txt’, ‘indoor-radiomap-mean.txt’ and ‘test-data.txt’). Press the “Create Indoor Parameters” to create the algorithm-specific parameters. Example file can be found in folder \Example\Parameters (Note: This step may take some time depending on the size of the radiomap and test data files.)
6.	To run the Airplace Tracker application on the smartphone do the next steps:
•	Copy the same floorplan map that was used in the Airplace Logger application (e.g., ‘mymap.jpg’) and the associated configuration file with the actual dimensions in meters (e.g., ‘mymap.config’) on the smartphone’s sdcard
•	Copy the following files from the ‘indoor’ folder of the Radiomap Server to the smartphone’s sdcard and rename them according to these rules:
i.	indoor-radiomap-mean.txt  → radiomap_indoor-mymap (Note: No file extension)
ii.	indoor-radiomap-parameters.txt → radiomap_indoor-mymap-parameters (Note: No file extension)
iii.	test-data.txt → test-data.txt (Note: The filename is the same and this file is only required for the offline mode of the Airplace Tracker)
•	Select the ‘radiomap_indoor-mymap’ file as the radiomap file from within the Airplace Tracker Preferences menu, select a positioning algorithm from the Algorithms menu and start real-time tracking. (Note: The MMSE algorithm usually provides better accuracy.)

## How to create Test Data 

Test data may serve two purposes:
1.	Calculate the algorithm-specific parameters that are distributed by the RadioMap Server along with the radiomap file.
2.	Evaluate the positioning accuracy of different algorithms within the Tracker Application in the Offline Mode.

## Test data can be created in the following two ways:
1.	Quick, but not scientifically sound. Follow the steps 1-3 in the previous section and then
•	Copy the file ‘indoor-radiomap.txt’ to another location and rename it to ‘test-data.txt’
•	Copy the file ‘test-data.txt’ in folder ‘indoor’
(Note: This option is not recommended because the algorithm-specific parameters calculated in this way will actually fit the radiomap data and are not guaranteed to work well when the Tracker application is invoked after a long time. Moreover, using such test data in the Offline mode of the Tracker application will result to very low positioning error which does not reflect the accuracy that will be attained when the Tracker is used after a long time.)

2.	More time and additional RSS data required. Follow the steps 1-3 in the previous section and then
•	Move the files ‘indoor-radiomap.txt’ and ‘indoor-radiomap-mean.txt’ from the ‘indoor’ folder to another location temporarily e.g. folder ‘temp’
•	Remove the ‘rsslog1.txt’ file (that corresponds to the radiomap data) from ‘indoor-rss-logs’ folder
•	Use the Airplace Logger to collect additional test data (preferably some time after the data collection for the radiomap and using different test locations) and store them in a log file e.g. ‘rss-log-mymap-test’. Copy this file to the ‘indoor-rss-logs’ folder and rename it to ‘rsslog1.txt’ (Note: This should now be the only log file in the folder)
•	Press the “Create Indoor Radiomap” button to get the test files in the ‘indoor’ folder
•	The ‘indoor’ folder contains 2 new files ‘indoor-radiomap.txt’ and ‘indoor-radiomap-mean.txt’ that correspond to the test data. Remove the ‘indoor-radiomap-mean.txt’ file and rename the ‘indoor-radiomap.txt’ to ‘test-data.txt’.
•	Copy the original files ‘indoor-radiomap.txt’ and ‘indoor-radiomap-mean.txt’ from folder ‘temp’ back to folder ‘indoor’



## How to calculate statistics for the positioning error using the Airplace Tracker (Offline mode) 

1.	Run the Airplace Tracker application making sure that you have the following files on the smartphone’s sdcard:
•	radiomap_indoor-mymap
•	radiomap_indoor-mymap-parameters
•	test-data.txt
2.	From within the Airplace Tracker preferences change the mode using the ‘Online/Offline Mode’ checkbox in the ‘Mode’ section. Then, select the ‘test-data.txt’ file on your sdcard as the Test Data file.
3.	Go back to the main screen and press the ‘Statistics Indoor’ button on the lower right corner
4.	Depending on the size of the ‘test-data.txt’ file it may take several minutes to calculate the statistics and on the screen you can see the remaining time. At the end, the Average Positioning Error and the Average Execution Time are printed on the screen for the currently selected algorithm
5.	You can select another algorithm from the ‘Algorithms’ menu and repeat the process
6.	You may switch back to real-time positioning by using the ‘Online/Offline Mode’ checkbox in the Airplace Tracker preferences


