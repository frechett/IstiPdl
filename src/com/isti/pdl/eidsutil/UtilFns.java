package com.isti.pdl.eidsutil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import gov.usgs.earthquake.distribution.Notification;
import gov.usgs.earthquake.distribution.URLNotification;
import gov.usgs.earthquake.distribution.URLNotificationXMLConverter;
import gov.usgs.util.Config;
import gov.usgs.util.Configurable;

public class UtilFns implements EIDSConst
{
  /** The working directory. */
  private static final File WORKING_DIR;
  static
  {
    String s = new File(".").getAbsolutePath();
    if (s.endsWith(".")) // if path ends with "."
    {
      // remove trailing "."
      s = s.substring(0, s.length() - 1);
    }
    WORKING_DIR = new File(s);
  }

  /**
   * Get the configuration property.
   * 
   * @param config the configuration.
   * @param key    the property key.
   * @return the configuration property or null if none.
   */
  public static String getConfigProperty(Config config, String key)
  {
    String value = config.getProperty(key);
    if (value == null) // if no property
    {
      // try lower case key
      key = key.toLowerCase();
      value = config.getProperty(key);
    }
    return value;
  }

  /**
   * Get the current directory.
   * 
   * @return the current directory.
   */
  public static File getCurrentDirectory()
  {
    return WORKING_DIR;
  }

  /**
   * Get the log message.
   * 
   * @param configurable the configurable.
   * @param message      the message.
   * @return the log message.
   */
  public static String getLogMessage(Configurable configurable, String message)
  {
    return "[" + configurable.getName() + "] " + message;
  }

  /**
   * Get the notification files.
   * 
   * @param notificationPath       the notification path.
   * @param notificationFilePrefix the notification file prefix.
   * @param files                  the files.
   * @return the filenames.
   * @throws IOException if an I/O error occurs.
   */
  public static List<Path> getNotificationFiles(Path notificationPath,
      String notificationFilePrefix, List<Path> files) throws IOException
  {
    Files.list(notificationPath).forEach(file -> {
      if (!Files.exists(file))
      {
        return;
      }
      final String filename = file.getFileName().toString();
      if (!filename.startsWith(notificationFilePrefix))
      {
        return;
      }
      if (!filename.endsWith(NOTIFICATION_FILE_EXT))
      {
        return;
      }
      files.add(file);
    });
    return files;
  }

  /**
   * Parse the option.
   * 
   * @param arg the argument.
   * @param sb  the string builder for the option text or null for none.
   * @return the option or null if none.
   */
  public static String parseOption(String arg, String[] optionValues,
      StringBuilder sb)
  {
    if (arg.startsWith(OPTION_PREFIX))
    {
      int index;
      for (String option : optionValues)
      {
        if (!arg.regionMatches(true, OPTION_PREFIX.length(), option, 0,
            option.length()))
        {
          continue;
        }
        index = arg.indexOf('=');
        if (sb != null && index != -1)
        {
          sb.append(arg, index + 1, arg.length());
        }
        return option;
      }
    }
    return null;
  }

  /**
   * Parses an XML message into an URL notification.
   * 
   * @param message the XML message.
   * @return the URL notification.
   * @throws Exception if error.
   */
  public static URLNotification parseXml(final InputStream message)
      throws Exception
  {
    return URLNotificationXMLConverter.parseXML(message);
  }

  /**
   * Parses an XML message into an URL notification.
   * 
   * @param xml the XML for the message.
   * @return the URL notification.
   * @throws Exception if error.
   */
  public static URLNotification parseXml(final String xml) throws Exception
  {
    try (final InputStream message = new ByteArrayInputStream(
        xml.getBytes(StandardCharsets.UTF_8)))
    {
      return parseXml(message);
    }
  }

  /**
   * Converts a notification to XML.
   * 
   * @param notification the URL notification.
   * @return the XML or an empty string if error.
   */
  public static String toXML(final Notification notification)
  {
    try
    {
      return URLNotificationXMLConverter.toXML((URLNotification) notification);
    }
    catch (Exception ex)
    {
      return "";
    }
  }
}
