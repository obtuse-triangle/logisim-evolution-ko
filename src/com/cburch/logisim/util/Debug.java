/**
 * This file is part of Logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + Haute École Spécialisée Bernoise
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 *   + REDS Institute - HEIG-VD, Yverdon-les-Bains, Switzerland
 *     http://reds.heig-vd.ch
 * This version of the project is currently maintained by:
 *   + Kevin Walsh (kwalsh@holycross.edu, http://mathcs.holycross.edu/~kwalsh)
 */

package com.cburch.logisim.util;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Scanner;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.Projects;

public class Debug {

  static final String LOG_FILE = System.getProperty("user.home") + "/logisim_debug.log";

  static DebugThread debugThread;
  static Scanner stdin;
  static PrintStream log;

  public static void enable() {
    if (debugThread != null)
      return;

    try {
      log = new PrintStream(new TeeOutputStream(
            new FileOutputStream(FileDescriptor.out),
            new FileOutputStream(LOG_FILE, true)));
      System.setOut(log);
      System.setErr(log);
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.printf("\n\n===== Logisim debug log: %s =====\n", new Date());

    stdin = new Scanner(System.in);

    debugThread = new DebugThread();
    debugThread.start();

    if (log != null)
      Runtime.getRuntime().addShutdownHook(new Thread(() -> log.flush()));
  }

  public static int verbose = 0;

  public static void printf(int lvl, String fmt, Object... args) {
    if (verbose >= lvl)
      System.out.printf(Thread.currentThread().getName() + ": " + fmt, args);
  }
  
  public static void println(int lvl, String msg) {
    if (verbose >= lvl)
      System.out.println(Thread.currentThread().getName() + ": " + msg);
  }

  // public static void print(int lvl, String msg) {
  //   if (verbose >= lvl)
  //     System.out.print(Thread.currentThread().getName() + ": " + msg);
  // }

  static void doCmd(String cmd, String... args) {
    if (cmd.equals("verbose")) {
      verbose++;
      System.out.printf("verbosity is now %d\n", verbose);
    } else if (cmd.equals("quiet")) {
      verbose--;
      System.out.printf("verbosity is now %d\n", verbose);
    } else if (cmd.equals("rate")) {
      Project proj = Projects.getTopFrame().getProject();
      String name = proj.getLogisimFile().getName();
      if (args.length < 2) {
        double hz = proj.getSimulator().getTickFrequency();
        System.out.printf("tick rate for %s is %f Hz\n", name, hz);
      } else {
        double hz = Double.parseDouble(args[1]);
        proj.getSimulator().setTickFrequency(hz);
        System.out.printf("tick rate for %s is now %f Hz\n", name, hz);
      }
    } else {
      System.out.printf("unrecognized debug command: %s\n", cmd);
    }
  }

  private static class DebugThread extends UniquelyNamedThread {
    public DebugThread() {
      super("DebugThread");
    }
    @Override
    public void run() {
      System.out.printf("$ ");
      System.out.flush();
      while (stdin.hasNextLine()) {
        try {
          String line = stdin.nextLine();
          String[] args = line.trim().split("\\s+");
          if (args.length > 0)
            doCmd(args[0], args);
        } catch (Throwable t) {
          t.printStackTrace();
        }
        System.out.printf("$ ");
        System.out.flush();
      }
    }
  }

  static class TeeOutputStream extends OutputStream {
    private final OutputStream one;
    private final OutputStream two;

    public TeeOutputStream(OutputStream a, OutputStream b) {
      one = a;
      two = b;
    }

    @Override
    public void write(int b) throws IOException {
      one.write(b);
      two.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
      one.write(b);
      two.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      one.write(b, off, len);
      two.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
      one.flush();
      two.flush();
    }

    @Override
    public void close() throws IOException {
      try {
        one.close();
      } finally {
        two.close();
      }
    }
  }

}
