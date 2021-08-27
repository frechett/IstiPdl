package com.isti.pdl.eidsutil;

import java.io.File;

import gov.usgs.util.Config;
import gov.usgs.util.Configurable;

public class EIDSNotifications implements Configurable, EIDSConst
{
  /** The notification directory key. */
  public static final String KEY_NOTIFICATIONDIR = "notificationDir";
  /** The notification file prefix key. */
  public static final String KEY_NOTIFICATIONFILEPREFIX = "notificationFilePrefix";
  /** The configuration keys. */
  public static final String[] KEY_VALUES =
  { KEY_NOTIFICATIONDIR, KEY_NOTIFICATIONFILEPREFIX };

  /** Name of this configurable object. */
  private String name;
  /** The notification directory. */
  private File notificationDir;
  /** The notification file prefix. */
  private String notificationFilePrefix = DEFAULT_NOTIFICATIONFILEPREFIX;

  /**
   * Create the EIDS file configurable.
   * 
   * @param name the object name.
   */
  public EIDSNotifications(String name)
  {
    this.name = name;
  }

  @Override
  public void configure(Config config) throws Exception
  {
    String value;
    for (String key : KEY_VALUES)
    {
      value = UtilFns.getConfigProperty(config, key);
      if (value != null)
      {
        setOptionValue(key, value);
      }
    }
  }

  /**
   * Configure the EIDS writer.
   * 
   * @param args the program arguments.
   * @throws Exception if configuration exceptions occur.
   */
  public void configure(String[] args) throws Exception
  {
    String option;
    final StringBuilder sb = new StringBuilder();
    for (String arg : args)
    {
      sb.setLength(0);
      option = UtilFns.parseOption(arg, KEY_VALUES, sb);
      if (option != null)
      {
        setOptionValue(option, sb.toString());
      }
    }
  }

  /**
   * Get the filename for the current time.
   * 
   * @return the filename.
   */
  protected String getFilename()
  {
    return notificationFilePrefix + System.currentTimeMillis();
  }

  @Override
  public String getName()
  {
    return name;
  }

  /**
   * Get the notification directory.
   * 
   * @return the notification directory, not null.
   */
  public File getNotificationDir()
  {
    File dir = notificationDir;
    if (dir == null)
    {
      dir = UtilFns.getCurrentDirectory();
    }
    return dir;
  }

  /**
   * Get the unique notification file.
   * 
   * @param filename the base filename.
   * @return the notification file.
   */
  protected File getNotificationFile(String filename)
  {
    int i = 0;
    File notificationFile;
    do
    {
      if (i >= 1000)
      {
        return null;
      }
      notificationFile = new File(notificationDir,
          filename + "_" + Integer.toString(i++) + NOTIFICATION_FILE_EXT);
    }
    while (notificationFile.exists());
    return notificationFile;
  }

  /**
   * Get the notification file prefix.
   * 
   * @return the notification file prefix, not null.
   */
  public String getNotificationFilePrefix()
  {
    return notificationFilePrefix;
  }

  @Override
  public void setName(String name)
  {
    this.name = name;
  }

  /**
   * Set the notification directory.
   * 
   * @param notificationDir the notification directory or null if none.
   */
  public void setNotificationDir(File notificationDir)
  {
    if (!notificationDir.exists())
    {
      notificationDir.mkdirs();
    }
    if (notificationDir.exists())
    {
      this.notificationDir = notificationDir;
    }
  }

  /**
   * Set the notification file prefix.
   * 
   * @param s the notification file prefix or null or empty string for the
   *          default.
   */
  public void setNotificationFilePrefix(String s)
  {
    if (s == null || s.isEmpty())
    {
      s = DEFAULT_NOTIFICATIONFILEPREFIX;
    }
    notificationFilePrefix = s;
  }

  /**
   * Set the option value.
   * 
   * @param option the option.
   * @param value  the value.
   * @throws Exception if configuration exceptions occur.
   */
  private void setOptionValue(String option, String value) throws Exception
  {
    if (option.equalsIgnoreCase(KEY_NOTIFICATIONDIR))
    {
      setNotificationDir(new File(value));
    }
    if (option.equalsIgnoreCase(KEY_NOTIFICATIONFILEPREFIX))
    {
      setNotificationFilePrefix(value);
    }
  }

  @Override
  public void shutdown() throws Exception
  {
  }

  @Override
  public void startup() throws Exception
  {
  }
}
