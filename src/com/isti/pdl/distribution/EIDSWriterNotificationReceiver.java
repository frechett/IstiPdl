package com.isti.pdl.distribution;

import com.isti.pdl.eidsutil.EIDSWriter;

import gov.usgs.earthquake.distribution.EIDSNotificationReceiver;
import gov.usgs.earthquake.distribution.Notification;
import gov.usgs.util.Config;

public class EIDSWriterNotificationReceiver extends EIDSNotificationReceiver
{
  /** The EIDS writer. */
  private final EIDSWriter writer;

  /**
   * Create the EIDS writer notification receiver.
   */
  public EIDSWriterNotificationReceiver()
  {
    setName(EIDSWriterNotificationReceiver.class.getSimpleName());
    writer = new EIDSWriter();
  }

  @Override
  public void configure(Config config) throws Exception
  {
    writer.configure(config);
    super.configure(config);
  }

  @Override
  public void receiveNotification(Notification notification) throws Exception
  {
    super.receiveNotification(notification);
    writer.processNotification(notification);
  }

  @Override
  public void shutdown() throws Exception
  {
    super.shutdown();
    writer.shutdown();
  }

  @Override
  public void startup() throws Exception
  {
    writer.startup();
    super.startup();
  }
}
