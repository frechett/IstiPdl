package com.isti.pdl.distribution;

import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.CLIProductBuilder;
import gov.usgs.earthquake.distribution.ProductClient;
import gov.usgs.earthquake.distribution.ProductTracker;
import gov.usgs.earthquake.indexer.SearchCLI;

/**
 * Override gov.usgs.earthquake.distribution.ProductClient for RELEASE_VERSION
 */
public class IstiProductClient extends ProductClient
{
  /** Logging object. */
  private static final Logger LOGGER = Logger
      .getLogger(ProductClient.class.getName());
  /** The "release" version number. */
  public static final String RELEASE_VERSION = ProductClient.RELEASE_VERSION
      + " (1.0.0 2021-08-28)";

  @Override
  public String getVersion()
  {
    return RELEASE_VERSION;
  }

  /**
   * Entry point into Product Distribution.
   *
   * @param args argument
   */
  public void run(final String[] args) throws Exception
  {
    try
    {
      // default is show usage
      boolean receiveProducts = false;
      boolean buildProduct = false;
      boolean trackProduct = false;
      boolean searchProduct = false;
      boolean showUsage = false;

      // parse arguments
      for (String arg : args)
      {
        if (arg.equals(SEND_ARGUMENT) || arg.equals(BUILD_ARGUMENT))
        {
          buildProduct = true;
        }
        else if (arg.equals(RECEIVE_ARGUMENT))
        {
          receiveProducts = true;
        }
        else if (arg.equals(TRACK_ARGUMENT))
        {
          trackProduct = true;
        }
        else if (arg.equals(SEARCH_ARGUMENT))
        {
          searchProduct = true;
        }
        else if (arg.equals(USAGE_ARGUMENT))
        {
          showUsage = true;
        }
      }

      // output current version
      System.err.println("Product Distribution Client");
      System.err.println(getVersion());
      System.err.println();

      if (buildProduct)
      {
        if (showUsage)
        {
          System.err.println("Usage: ");
          System.err.println(
              "    java -jar ProductClient.jar --build [BUILD ARGUMENTS]");
          System.err.println();
          System.err.println(CLIProductBuilder.getUsage());
          System.exit(0);
        }
        LOGGER.info("Running Product Builder");
        // run builder main
        CLIProductBuilder.main(args);
        System.exit(0);
      }
      else if (trackProduct)
      {
        if (showUsage)
        {
          System.err.println("Usage: ");
          System.err.println(
              "    java -jar ProductClient.jar --track [TRACK ARGUMENTS]");
          System.err.println();
          System.err.println(ProductTracker.getUsage());
          System.exit(0);
        }
        LOGGER.info("Running Product Tracker");
        ProductTracker.main(args);
        System.exit(0);
      }
      else if (searchProduct)
      {
        // search needs to happen after track, since track also uses a
        // --search argument
        if (showUsage)
        {
          System.err.println("Usage: ");
          System.err.println(
              "    java -jar ProductClient.jar --search [SEARCH ARGUMENTS]");
          System.err.println();
          System.err.println(SearchCLI.getUsage());
          System.exit(0);
        }
        LOGGER.info("Running Product Search");
        SearchCLI.main(args);
        System.exit(0);
      }
      else if (receiveProducts && !showUsage)
      {
        // start processing
        LOGGER.info("Starting");
        try
        {
          startup();
        }
        catch (Exception e)
        {
          LOGGER.log(Level.SEVERE, "Exceptions while starting, shutting down",
              e);
          try
          {
            // this has been throwing exceptions, move into try
            shutdown();
          }
          finally
          {
            // exit no matter what
            System.exit(1);
          }
        }
        LOGGER.info("Started");

        // shutdown threads when control-c is pressed
        // otherwise, would continue running
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
          public void run()
          {
            try
            {
              LOGGER.info("Shutting down");
              shutdown();
              LOGGER.info("Shutdown complete");
            }
            catch (Exception e)
            {
              LOGGER.log(Level.WARNING, "Exception while shutting down", e);
            }
          }
        });
      }
      else
      {
        System.err.println("Usage: ");
        System.err.println("    java -jar ProductClient.jar [ARGUMENTS]");
        System.err.println();
        System.err.println(getUsage());
        System.exit(1);
      }

    }
    catch (Exception e)
    {
      LOGGER.log(Level.SEVERE, "Exception in main", e);
    }
  }
}
