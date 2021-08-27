package com.isti.pdl.eidsutil;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.Notification;
import gov.usgs.util.FileUtils;

public class EIDSWriter extends EIDSNotifications
{
  /** Logging object. */
  private static final Logger LOGGER = Logger
      .getLogger(EIDSWriter.class.getName());

  /** The file count. */
  private long fileCount;

  /**
   * Create the EIDS writer.
   */
  public EIDSWriter()
  {
    super(EIDSWriter.class.getSimpleName());
  }

  /**
   * Get the log message.
   * 
   * @param message the message.
   * @return the log message.
   */
  private String getLogMessage(String message)
  {
    return UtilFns.getLogMessage(this, message);
  }

  /**
   * Process the notification.
   * 
   * @param notification the notification.
   * @throws IOException
   */
  public void processNotification(Notification notification) throws IOException
  {
    // code lifted from gov.usgs.earthquake.distribution.EIDSNotificationSender

    // create a unique filename
    final String filename = getFilename();
    final String message = UtilFns.toXML(notification);
    if (message.isEmpty())
    {
      LOGGER.warning(
          getLogMessage("could not get XML for name (" + filename + ")"));
      return;
    }
    final File notificationFile = getNotificationFile(filename);
    if (notificationFile == null)
    {
      LOGGER.warning(getLogMessage(
          "could not get notification file for name (" + filename + ")"));
      return;
    }
    final File tempFile = File.createTempFile(filename, null);
    FileUtils.writeFile(tempFile, message.getBytes());
    tempFile.renameTo(notificationFile);
    LOGGER.log(Level.INFO, getLogMessage("saved notification to file ("
        + (++fileCount) + ", " + notificationFile.getName() + ")"));
  }
}
