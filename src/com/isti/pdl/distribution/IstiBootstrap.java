package com.isti.pdl.distribution;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.Bootstrap;
import gov.usgs.earthquake.distribution.Bootstrappable;
import gov.usgs.earthquake.distribution.ProductTracker;
import gov.usgs.util.Config;
import gov.usgs.util.Configurable;

/**
 * Override gov.usgs.earthquake.distribution.Bootstrap for RELEASE_VERSION and
 * to avoid using reflection if the main class is the normal ProductClient
 */
public class IstiBootstrap extends Bootstrap
{
  /** Default mainclass is "com.isti.pdl.distribution.IstiProductClient. */
  public static final String DEFAULT_MAINCLASS = "com.isti.pdl.distribution.IstiProductClient";

  /** Private logging object. */
  private static final Logger LOGGER = Logger
      .getLogger(Bootstrap.class.getName());

  public static void main(final String[] args)
  {
    try
    {
      StringBuffer argumentList = new StringBuffer();
      boolean configTest = false;

      String _className = null;

      // use default config file
      File configFile = new File(DEFAULT_CONFIGFILE);
      for (String arg : args)
      {
        argumentList.append(arg).append(" ");
        if (arg.startsWith(CONFIGFILE_ARGUMENT))
        {
          // unless config file argument provided
          configFile = new File(arg.replace(CONFIGFILE_ARGUMENT, ""));
        }
        else if (arg.equals(CONFIG_TEST_ARGUMENT))
        {
          configTest = true;
        }
        else if (arg.startsWith(MAINCLASS_ARGUMENT))
        {
          _className = arg.replace(MAINCLASS_ARGUMENT, "");
        }
        else if (arg.equals(VERSION_ARGUMENT))
        {
          System.err.println("Product Distribution Client");
          System.err.println(IstiProductClient.RELEASE_VERSION);
          System.exit(0);
        }
      }

      Bootstrap bootstrap = new Bootstrap();

      // load configuration file
      Config config = bootstrap.loadConfig(configFile);

      // set global config object
      Config.setConfig(config);

      // setup logging based on configuration
      bootstrap.setupLogging(config);

      // java and os information
      LOGGER.config("java.vendor = " + System.getProperty("java.vendor"));
      LOGGER.config("java.version = " + System.getProperty("java.version"));
      LOGGER.config("java.home = " + System.getProperty("java.home"));
      LOGGER.config("os.arch = " + System.getProperty("os.arch"));
      LOGGER.config("os.name = " + System.getProperty("os.name"));
      LOGGER.config("os.version = " + System.getProperty("os.version"));
      LOGGER.config("user.dir = " + System.getProperty("user.dir"));
      LOGGER.config("user.name = " + System.getProperty("user.name"));

      // log command line arguments
      LOGGER.fine("Command line arguments: " + argumentList.toString().trim());

      // configure whether tracker updates are sent.
      String enableTrackerProperty = config
          .getProperty(ENABLE_TRACKER_PROPERTY_NAME);
      if (Boolean.valueOf(enableTrackerProperty))
      {
        LOGGER.warning(
            "Enabled tracker updates," + " this is usually not a good idea.");
        ProductTracker.setTrackerEnabled(true);
      }

      // lookup main class
      if (_className == null)
      {
        // no argument specified, check configuration
        _className = config.getProperty(MAINCLASS_PROPERTY_NAME,
            DEFAULT_MAINCLASS);
      }

      final Bootstrappable main;
      if (DEFAULT_MAINCLASS.equals(_className))
      {
        LOGGER.config("Creating main class " + _className);
        main = new IstiProductClient();
      }
      else
      {
        // invoke main class main(String[] args) method
        LOGGER.config("Loading main class " + _className);
        try
        {
          main = (Bootstrappable) Class.forName(_className).getConstructor()
              .newInstance();
        }
        catch (ClassCastException cce)
        {
          LOGGER.log(Level.SEVERE,
              "Main class must implement the Bootstrappable interface", cce);
          System.exit(1);
          return;
        }
      }

      // use the configurable interface when available
      if (main instanceof Configurable)
      {
        Configurable configurable = ((Configurable) main);
        configurable.setName("main");
        try
        {
          configurable.configure(config);
        }
        catch (Exception e)
        {
          LOGGER.log(Level.SEVERE, "Exception loading configuration ", e);
          System.exit(1);
        }
      }

      // configuration loaded okay
      LOGGER.config("Configuration loaded");
      if (configTest)
      {
        // exit successfully
        System.exit(0);
      }

      // run main instance
      LOGGER.config("Bootstrap complete, running main class\n");
      try
      {
        main.run(args);
      }
      catch (Exception e)
      {
        LOGGER.log(Level.SEVERE, "Main class threw exception, exiting", e);
        System.exit(Bootstrappable.RUN_EXCEPTION_EXIT_CODE);
      }
    }
    catch (Exception ex)
    {
      LOGGER.log(Level.SEVERE, "Exception main", ex);
    }
  }
}
