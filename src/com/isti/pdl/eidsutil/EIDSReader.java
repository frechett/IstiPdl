package com.isti.pdl.eidsutil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.NotificationReceiver;
import gov.usgs.util.Config;;

public class EIDSReader extends EIDSNotifications implements Runnable
{
  /**
   * The processed directory key. If the processed directory is not specified
   * files are deleted rather than moved after they are processed.
   */
  public static final String KEY_PROCESSEDDIR = "processedDir";

  /** The configuration keys. */
  public static final String[] KEY_VALUES =
  { KEY_PROCESSEDDIR };
  /** Logging object. */
  private static final Logger LOGGER = Logger
      .getLogger(EIDSReader.class.getName());
  /* For autonumbering threads. */
  private static int threadInitNumber;

  /**
   * The EIDS Reader.
   * 
   * @param args the program arguments.
   */
  public static void main(final String[] args)
  {
    final EIDSReader reader = new EIDSReader();
    try
    {
      reader.configure(args);
      reader.startup();
    }
    catch (Exception ex)
    {
      LOGGER.log(Level.SEVERE,
          reader.getLogMessage("program terminated abnormally"), ex);
    }
  }

  private static synchronized int nextThreadNum()
  {
    return threadInitNumber++;
  }

  /** The file count. */
  private long fileCount;
  /** The filenames. */
  private final List<Path> notificationFiles = new ArrayList<Path>();
  /** The notification path. */
  private Path notificationPath;
  /** The notification receiver. */
  private NotificationReceiver notificationReceiver;
  /** The processed directory. */
  private File processedDir;
  /** The running flag. */
  private volatile boolean running;
  /** The watch service thread. */
  private Thread watchServiceThread;

  /**
   * Create the EIDS reader.
   */
  public EIDSReader()
  {
    super(EIDSReader.class.getSimpleName());
  }

  @Override
  public void configure(Config config) throws Exception
  {
    super.configure(config);
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
    super.configure(args);
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

  /**
   * Get the notification receiver.
   * 
   * @return the notification receiver or null if none.
   */
  public NotificationReceiver getNotificationReceiver()
  {
    return notificationReceiver;
  }

  /**
   * Get the processed directory.
   * 
   * @return the processed directory or null if none.
   */
  public File getProcessedDir()
  {
    return processedDir;
  }

  private boolean isRunning()
  {
    return running;
  }

  private void processExistingFiles() throws IOException
  {
    long count = 0;
    notificationFiles.clear();
    if (notificationPath == null)
    {
      notificationPath = getNotificationDir().toPath();
    }
    while (!UtilFns.getNotificationFiles(notificationPath,
        getNotificationFilePrefix(), notificationFiles).isEmpty())
    {
      if (count == 0 && LOGGER.isLoggable(Level.FINE))
      {
        LOGGER.fine(getLogMessage("processExistingFiles started"));
      }
      count += notificationFiles.size();
      Collections.sort(notificationFiles);
      for (Path file : notificationFiles)
      {
        processNotificationFile(file);
      }
      notificationFiles.clear();
    }
    if (count > 0 && LOGGER.isLoggable(Level.FINE))
    {
      LOGGER.fine(
          getLogMessage("processExistingFiles compeleted (" + count + ")"));
    }
  }

  /**
   * Process the notification file.
   * 
   * @param file the notification file.
   */
  private void processNotificationFile(Path file)
  {
    if (!Files.exists(file))
    {
      return;
    }
    LOGGER.info(getLogMessage(
        "notification file (" + (++fileCount) + ", " + file + ")"));

    if (notificationReceiver != null)
    {
      // process the notification
      try (InputStream message = Files.newInputStream(file))
      {
        notificationReceiver.receiveNotification(UtilFns.parseXml(message));
      }
      catch (Exception ex)
      {
        LOGGER.log(Level.WARNING,
            getLogMessage("Error processing notification"), ex);
      }
    }

    if (processedDir == null)
    {
      try
      {
        Files.delete(file);
      }
      catch (NoSuchFileException ex)
      {
        LOGGER.info(getLogMessage(
            "could not delete file (" + file + "), no longer exists"));
      }
      catch (Exception ex)
      {
        LOGGER.severe(
            getLogMessage("could not delete file (" + file + "): " + ex));
        shutDownQuietly();
      }
    }
    else
    {
      try
      {
        final Path dest = Paths.get(processedDir.toString(),
            file.getFileName().toString());
        Files.move(file, dest);
      }
      catch (Exception ex)
      {
        LOGGER
            .severe(getLogMessage("could not move file (" + file + "): " + ex));
        shutDownQuietly();
      }
    }
  }

  @Override
  public void run()
  {
    if (LOGGER.isLoggable(Level.FINE))
      LOGGER.fine(getLogMessage("run started (" + getNotificationDir() + ")"));
    try
    {
      Object context;
      processExistingFiles();
      try (WatchService watchService = FileSystems.getDefault()
          .newWatchService())
      {
        final Path path = getNotificationDir().toPath();
        final WatchEvent.Kind<?>[] events =
        { StandardWatchEventKinds.ENTRY_CREATE };
        final WatchEvent.Modifier[] modifiers = {};
        path.register(watchService, events, modifiers);
        for (WatchKey key; isRunning(); key = null)
        {
          processExistingFiles();
          if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(getLogMessage("getting next watch key..."));
          key = watchService.take();
          if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(getLogMessage("got watch key, getting events..."));
          for (WatchEvent<?> event : key.pollEvents())
          {
            Kind<?> kind = event.kind();
            if (kind == StandardWatchEventKinds.OVERFLOW)
            {
              if (LOGGER.isLoggable(Level.FINE))
                LOGGER.fine(getLogMessage("event overflow occurred"));
              key.reset();
              processExistingFiles();
              continue;
            }
            context = event.context();
            if (context instanceof Path)
            {
              processNotificationFile(((Path) context));
            }
            else
            {
              LOGGER.severe(getLogMessage(
                  "unexpected context (" + context.toString() + ")"));
              shutDownQuietly();
            }
          }
          if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(getLogMessage("done procesing events"));
          key.reset();
        }
      }
    }
    catch (IOException ex)
    {
      LOGGER.warning(getLogMessage("run error: " + ex));
    }
    catch (InterruptedException ex)
    {
    }
    if (LOGGER.isLoggable(Level.FINE))
      LOGGER.fine(getLogMessage("run finished (" + getNotificationDir() + ")"));
  }

  /**
   * Set the notification receiver.
   * 
   * @param notificationReceiver the notification receiver or null if none.
   */
  public void setNotificationReceiver(NotificationReceiver notificationReceiver)
  {
    this.notificationReceiver = notificationReceiver;
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
    if (option.equalsIgnoreCase(KEY_PROCESSEDDIR))
    {
      setProcessedDir(new File(value));
    }
  }

  /**
   * Set the processed directory.
   * 
   * @param processedDir the processed directory or null if none.
   */
  public void setProcessedDir(File processedDir)
  {
    if (!processedDir.exists())
    {
      processedDir.mkdirs();
    }
    if (processedDir.exists())
    {
      this.processedDir = processedDir;
    }
  }

  @Override
  public void shutdown() throws Exception
  {
    super.shutdown();
    running = false;
    final Thread thread;
    synchronized (this)
    {
      thread = watchServiceThread;
      watchServiceThread = null;
    }
    if (thread != null)
    {
      synchronized (thread)
      {
        thread.interrupt();
      }
    }
  }

  private void shutDownQuietly()
  {
    try
    {
      shutdown();
    }
    catch (Exception ex)
    {
    }
  }

  @Override
  public void startup() throws Exception
  {
    running = true;
    final Thread thread;
    synchronized (this)
    {
      if (watchServiceThread == null)
      {
        thread = new Thread(this, getName() + nextThreadNum());
        watchServiceThread = thread;
      }
      else
      {
        thread = null;
      }
    }
    if (thread != null)
    {
      thread.start();
    }
    super.startup();
  }
}
