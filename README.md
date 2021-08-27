ISTI Extensions to the Product Distribution Layer (PDL)
================================

The extensions are:
- Stand alone program that will write PDL notification message files with the  **EIDSWriterClient** (com.isti.pdl.eidsutil.EIDSWriterClient) class.
- Stand alone test program that will read PDL notification message files with the  **EIDSReader** (com.isti.pdl.eidsutil.EIDSReader) class.
- Receiver with the added ability to write PDL notification message files with the **EIDSWriterNotificationReceiver** (com.isti.pdl.distribution.EIDSWriterNotificationReceiver) class.
- Receiver that reads PDL notification message files instead of receiving them from PDL feed with the  **EIDSReaderNotificationReceiver** (com.isti.pdl.distribution.EIDSReaderNotificationReceiver) class.

### EIDSWriterClient
The EIDSWriterClient program is run with the following command:
```
java -cp istipdl.jar com.isti.pdl.eidsutil.EIDSWriterClient --notificationDir=notifications
```
### EIDSReader
The EIDSReader test program is run with the following command:
```
java -cp istipdl.jar com.isti.pdl.eidsutil.EIDSReader --notificationDir=notifications
```
### EIDSWriterNotificationReceiver
The EIDSWriterNotificationReceiver class is meant to be a replacement for the  EIDSNotificationReceiver (gov.usgs.earthquake.distribution.EIDSNotificationReceiver) class. It has the same functionality but also writes PDL message notifictions.
The configuration is the same with the following addition:
```
[receiver_pdl]
type = com.isti.pdl.distribution.EIDSWriterNotificationReceiver
notificationDir = notifications
```
### EIDSReaderNotificationReceiver
The EIDSReaderNotificationReceiver class is meant to be a replacement for the  EIDSNotificationReceiver (gov.usgs.earthquake.distribution.EIDSNotificationReceiver) class. It reads PDL notification message files instead of receiving them from PDL feed.

Here is an example configuration:
```
[receiver_pdl]
type = com.isti.pdl.distribution.EIDSReaderNotificationReceiver
notificationDir = notifications
storage = receiver_storage
index = receiver_index
## how long to wait before checking for expired products
## 900000 milliseconds = 15 minutes
cleanupInterval = 900000
## how old products are before considered expired
## 900000 milliseconds = 15 minutes
storageage = 900000
```
## Building or Developing

This is a java project with an Ant build file.
To build the project, from the project directory run:
```
ant
```
