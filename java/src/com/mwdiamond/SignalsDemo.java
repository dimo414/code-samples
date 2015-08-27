package com.mwdiamond;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates how to signal handling works in Java, along with different edge cases.
 */
public class SignalsDemo {
  private static final long START_TIME = System.nanoTime();
  private static final Signal SIGINT = new Signal("INT");

  /** Dispatches to the other public static methods in this class */
  public static void main(String[] args) throws Throwable {
    if (args.length == 0) {
      System.err.println("Must specify a method name");
      System.exit(1);
    }

    String[] leftoverArgs = new String[args.length - 1];
    System.arraycopy(args, 1, leftoverArgs, 0, leftoverArgs.length);

    Method m = SignalsDemo.class.getMethod(args[0], String[].class);

    try {
      m.invoke(null, (Object) leftoverArgs);
    } catch (InvocationTargetException e) {
      throw e.getCause() != null ? e.getCause() : e;
    }
  }

  /**
   * Demos what happens when interrupts are sent to the JVM.
   *
   * args:
   *   0: a number of background threads to kick off, 0 for none
   *   1: a number of seconds a shutdown hook should sleep for, 0 for no hook
   *   2: if set, marks the shutdown thread a daemon
   *
   * Notice that no threads are interrupted or otherwise affected by the signal.  Instead,
   * the shutdown hooks are started and the JVM waits for them to finish.  If the other threads
   * finish in time, the JVM waits for the shutdown hooks to finish, even if they're daemons.
   * As soon as the shutdown hooks finish, the JVM forcibly exits with exit code 128+SIGNAL,
   * stopping any remaining threads.
   */
  public static void standardInterrupt(String[] args) {
    int threads = 0;
    if (args.length > 0) {
      threads = Integer.parseInt(args[0]);
    }

    for (int i = 0; i < threads; i++) {
      new DelayRunnable(10).thread("Background").start();
    }

    if (args.length > 1) {
      int shutdownDelay = Integer.parseInt(args[1]);
      Thread shutdown = new DelayRunnable(shutdownDelay).thread("Shutdown");
      if (args.length > 2) {
        shutdown.setDaemon(true);
      }
      Runtime.getRuntime().addShutdownHook(shutdown);
    }

    safeSleep(1);
    send(SIGINT);
    safeSleep(5);
    log("Main completed successfully.");
  }

  /**
   * Registers a custom handler that exits on signals,
   * then runs the same behavior as standardInterrupt.
   *
   * args:
   *   same as standardInterrupt
   *
   * Notice System.exit() does the same things as the default signal handler, it starts
   * the shutdown hooks and exits abruptly once they're done.
   */
  public static void captureInterrupt(String[] args) {
    Signal.handle(SIGINT, new SignalHandler() {
      @Override
      public void handle(Signal signal) {
        log("Captured " + signal);
        System.exit(100 + signal.getNumber());
      }
    });

    standardInterrupt(args);
  }

  /**
   * Registers a custom handler that ignores signals,
   * then runs the same behavior as standardInterrupt.
   *
   * args:
   *   same as standardInterrupt
   *
   * Notice by suppressing the interrupt the shutdown hooks are not triggered until after the main
   * method returns normally.
   */
  public static void suppressInterrupt(String[] args) {
    Signal.handle(SIGINT, new SignalHandler() {
      @Override
      public void handle(Signal signal) {
        log("Suppressing " + signal);
      }
    });

    standardInterrupt(args);
  }

  /**
   * If a signal is suppressed, the handler will be reused. You can safely swap out the handler
   * while another handler is running (though there is a race condition, so do the swap early).
   *
   * args:
   *   0: the number of times to suppress the signal; defaults to two
   */
  public static void resignal(String[] args) {
    final int numSignals = args.length > 0 ? Integer.parseInt(args[0]) : 2;
    Signal.handle(SIGINT, new SignalHandler() {
      int signalsLeft = numSignals;
      @Override
      public void handle(Signal signal) {
        if (signalsLeft-- > 1) {
          log("Suppressing " + signal + ", " + signalsLeft + " left.");
          return;
        }
        log("Suppressing " + signal + ", registering new handler.");
        // It seems we can't use SignalHandler.SIG_DFL with Signal.raise()
        // Signal.handle(SIGINT, SignalHandler.SIG_DFL);
        Signal.handle(SIGINT, new SignalHandler() {
          @Override
          public void handle(Signal signal) {
            log("Caught " + signal + ", exiting!");
            System.exit(100 + signal.getNumber());
          }
        });
      }
    });

    while (true) {
      safeSleep(1);
      send(SIGINT);
    }
  }

  /**
   * Just for testing the default behavior.
   */
  public static void manual(@SuppressWarnings("unused") String[] args) {
    // Interestingly, uncommenting this line causes a error, though it should be a no-op.
    //Signal.handle(SIGINT, SignalHandler.SIG_DFL);
    send(SIGINT);
    log("Sleeping, send a SIGINT");
    safeSleep(100);
  }

  /**
   * This triggers a SEGV on my machine, apparently you can't trigger the default behavior manually.
   */
  public static void segv(@SuppressWarnings("unused") String[] args) {
    SignalHandler.SIG_DFL.handle(SIGINT);
  }

  //
  // Helpers
  //

  private abstract static class NamedRunnable implements Runnable {
    private static AtomicInteger index = new AtomicInteger();

    Thread thread(String name) {
      return new Thread(this, name + ":" + index.getAndIncrement());
    }
  }

  private static class DelayRunnable extends NamedRunnable {
    private final int delay;

    private DelayRunnable(int delay) {
      this.delay = delay;
    }

    @Override
    public void run() {
      log("Starting " + Thread.currentThread().getName());
      safeSleep(delay);
      log("Thread " + Thread.currentThread().getName() + " completed successfully.");
    }
  }

  private static void send(Signal signal) {
    log("Sending " + signal);
    Signal.raise(signal);
  }

  private static void safeSleep(long seconds) {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    } catch (InterruptedException e) {
      log("Interrupted!");
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  private static synchronized void log(String msg) {
    System.out.printf("%d:\t%s\n",
        TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - START_TIME), msg);
  }
}

