package com.isti.pdl.distribution;

import com.isti.pdl.eidsutil.EIDSReader;

import gov.usgs.earthquake.distribution.DefaultNotificationReceiver;
import gov.usgs.util.Config;

public class EIDSReaderNotificationReceiver extends DefaultNotificationReceiver
{
  private final EIDSReader reader;

  /**
   * Create the EIDS reader notification receiver.
   */
  public EIDSReaderNotificationReceiver()
  {
    setName(EIDSReaderNotificationReceiver.class.getSimpleName());
    reader = new EIDSReader();
    reader.setNotificationReceiver(this);
  }

  @Override
  public void configure(Config config) throws Exception
  {
    reader.configure(config);
    super.configure(config);
  }

  @Override
  public void shutdown() throws Exception
  {
    reader.shutdown();
    super.shutdown();
  }

  @Override
  public void startup() throws Exception
  {
    super.startup();
    reader.startup();
  }
}
