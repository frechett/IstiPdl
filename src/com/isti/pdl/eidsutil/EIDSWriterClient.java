package com.isti.pdl.eidsutil;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.EIDSNotificationReceiver;
import gov.usgs.earthquake.distribution.URLNotification;
import gov.usgs.earthquake.distribution.URLNotificationParser;
import gov.usgs.earthquake.distribution.URLNotificationXMLConverter;
import gov.usgs.earthquake.eidsutil.EIDSClient;
import gov.usgs.earthquake.eidsutil.EIDSListener;
import gov.usgs.earthquake.eidsutil.EIDSMessageEvent;
import gov.usgs.util.Config;
import gov.usgs.util.Configurable;
import gov.usgs.util.StreamUtils;

public class EIDSWriterClient extends EIDSClient
    implements Configurable, EIDSListener
{
  private static final String DEFAULT_ALTERNATESERVERS = "prod02-pdl01.cr.usgs.gov:39977";
  private static final Long DEFAULT_CLIENTRESTARTINTERVAL = EIDSClient.DEFAULT_CLIENT_RESTART_INTERVAL;
  private static final String DEFAULT_HOST = "prod01-pdl01.cr.usgs.gov";
  private static final Double DEFAULT_MAXSERVEREVENTAGEDAYS = EIDSClient.DEFAULT_MAX_SERVER_EVENT_AGE_DAYS;
  private static final Integer DEFAULT_PORT = EIDSClient.DEFAULT_SERVER_PORT;
  private static final String DEFAULT_TRACKINGFILE = "EIDSWriter_tracking.dat";
  /** The alternate servers key. */
  public static final String KEY_ALTERNATESERVERS = EIDSNotificationReceiver.EIDS_ALTERNATE_SERVERS;
  /** The client restart interval key. */
  public static final String KEY_CLIENTRESTARTINTERVAL = "clientRestartInterval";
  /** The EIDS debug key. */
  public static final String KEY_EIDSDEBUG_KEY = EIDSNotificationReceiver.EIDS_DEBUG;
  /** The maximum server event age key. */
  public static final String KEY_MAXSERVEREVENTAGEDAYS = EIDSNotificationReceiver.EIDS_MAX_EVENT_AGE;
  /** The server host key. */
  public static final String KEY_SERVERHOST = EIDSNotificationReceiver.EIDS_SERVER_HOST_PROPERTY;
  /** The server port key. */
  public static final String KEY_SERVERPORT = EIDSNotificationReceiver.EIDS_SERVER_PORT;
  /** The tracking file key. */
  public static final String KEY_TRACKINGFILE = EIDSNotificationReceiver.EIDS_TRACKING_FILE;
  /** The configuration keys. */
  public static final String[] KEY_VALUES =
  { KEY_ALTERNATESERVERS, KEY_CLIENTRESTARTINTERVAL, KEY_EIDSDEBUG_KEY,
      KEY_MAXSERVEREVENTAGEDAYS, KEY_SERVERHOST, KEY_SERVERPORT,
      KEY_TRACKINGFILE };
  /** Logging object. */
  private static final Logger LOGGER = Logger
      .getLogger(EIDSWriterClient.class.getName());

  /**
   * The EIDS Writer Client.
   * 
   * @param args the program arguments.
   */
  public static void main(final String[] args)
  {
    final EIDSWriterClient writer = new EIDSWriterClient();
    try
    {
      writer.configure(args);
      writer.startup();
    }
    catch (Exception ex)
    {
      LOGGER.log(Level.SEVERE,
          writer.getLogMessage("program terminated abnormally"), ex);
    }
  }

  /** Name of this configurable object. */
  private String name;
  /** The EIDS writer. */
  private final EIDSWriter writer;

  /**
   * Create the EIDS Writer.
   */
  public EIDSWriterClient()
  {
    super(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_ALTERNATESERVERS,
        DEFAULT_MAXSERVEREVENTAGEDAYS, DEFAULT_TRACKINGFILE,
        DEFAULT_CLIENTRESTARTINTERVAL);
    name = EIDSWriterClient.class.getSimpleName();
    writer = new EIDSWriter();
    addListener(this);
    Runtime.getRuntime().addShutdownHook(new Thread()
    {
      @Override
      public void run()
      {
        shutdown();
      }
    });
  }

  @Override
  public void configure(Config config) throws Exception
  {
    writer.configure(config);
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
   * Configure the EIDS writer client.
   * 
   * @param args the program arguments.
   * @throws Exception if configuration exceptions occur.
   */
  public void configure(final String[] args) throws Exception
  {
    String option;
    writer.configure(args);
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
   * Get the log message.
   * 
   * @param message the message.
   * @return the log message.
   */
  private String getLogMessage(String message)
  {
    return UtilFns.getLogMessage(this, message);
  }

  @Override
  public String getName()
  {
    return name;
  }

  @Override
  public void onEIDSMessage(EIDSMessageEvent event)
  {
    if (URLNotificationParser.PRODUCT_XML_NAMESPACE
        .equals(event.getRootNamespace())
        && URLNotificationParser.NOTIFICATION_ELEMENT
            .equals(event.getRootElement()))
    {
      InputStream in = null;
      try
      {
        in = StreamUtils.getInputStream(event.getMessage());
        // this is a notification message
        URLNotification notification = URLNotificationXMLConverter.parseXML(in);
        // process the notification
        writer.processNotification(notification);
      }
      catch (Exception e)
      {
        LOGGER.log(Level.WARNING,
            getLogMessage("exception while parsing URLNotification"), e);
      }
      finally
      {
        StreamUtils.closeStream(in);
      }
    }
    else
    {
      LOGGER.info(getLogMessage("ignoring message type "
          + event.getRootNamespace() + ":" + event.getRootElement()));
      LOGGER.info(getLogMessage("message content: " + event.getMessage()));
    }
  }

  @Override
  public void setName(String name)
  {
    this.name = name;
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
    if (option.equalsIgnoreCase(KEY_ALTERNATESERVERS))
    {
      setAlternateServersList(value);
    }
    if (option.equalsIgnoreCase(KEY_CLIENTRESTARTINTERVAL))
    {
      setClientRestartInterval(Long.parseLong(value));
    }
    if (option.equalsIgnoreCase(KEY_EIDSDEBUG_KEY))
    {
      setDebug(!"false".equalsIgnoreCase(value));
    }
    if (option.equalsIgnoreCase(KEY_MAXSERVEREVENTAGEDAYS))
    {
      setMaxServerEventAgeDays(Double.parseDouble(value));
    }
    if (option.equalsIgnoreCase(KEY_SERVERHOST))
    {
      setServerHost(value);
    }
    if (option.equalsIgnoreCase(KEY_SERVERPORT))
    {
      setServerPort(Integer.parseInt(value));
    }
    if (option.equalsIgnoreCase(KEY_TRACKINGFILE))
    {
      setTrackingFileName(value);
    }
  }
}
