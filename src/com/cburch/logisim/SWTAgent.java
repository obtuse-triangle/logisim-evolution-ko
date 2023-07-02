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

package com.cburch.logisim;

import java.io.File;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarFile;

import org.kwalsh.BetterFileDialog;

import com.cburch.logisim.gui.start.Startup;
import com.cburch.logisim.util.Debug;

/**
 * SWTAgent provides a shim to find and/or extract and load the
 * platform-specific SWT jar file needed by BetterFileDialog. This code executes
 * before Main.main() due to a few lines in the jar manifest:
 *   Launcher-Agent-Class: com.cburch.logisim.SWTAgent
 *   Premain-Class: com.cburch.logisim.SWTAgent
 *   Agent-Class: com.cburch.logisim.SWTAgent
 */
public class SWTAgent {
  public static Instrumentation instrumentation;

  public static void premain(String args, Instrumentation instrumentation) {
    SWTAgent.instrumentation = instrumentation;
  }

  public static void agentmain(String args, Instrumentation instrumentation) {
    SWTAgent.instrumentation = instrumentation;
  }

  private static String appendJar(File f) {
    if (instrumentation == null)
      return "Instrumentation not initialized";
    try {
      instrumentation.appendToSystemClassLoaderSearch(new JarFile(f));
      return null;
    } catch (Exception e) {
      e.printStackTrace();
      return "Error loading " + f.getPath() + "\n" + e.toString();
    }
  }

  private static String loadSWT(String jarname) {
    // First, see if jar is already on classpath
    String[] paths = System.getProperty("java.class.path").split(File.pathSeparator);
    for (String path: paths) {
      if (new File(path).getName().toLowerCase().equals(jarname.toLowerCase())) {
        Debug.println(1, "Found platform-specific SWT library: " + path);
        return null;
      }
    }

    // Second, check ~/.swt/jarname, extract if not found
    File destdir = new File(System.getProperty("user.home"), ".swt");
    File dest = new File(destdir, jarname);
    if (dest.exists()) {
      Debug.println(1, "Loading platform-specific SWT library: " + dest.getPath());
    } else {
      Debug.println(1, "Installing platform-specific SWT library: " + dest.getPath());
      try (InputStream inputStream = Main.class.getResourceAsStream("/"+jarname)) {
        if (inputStream == null) {
          return "SWT library missing from logisim jar: " + jarname;
        }
        Files.copy(inputStream, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
      } catch (Throwable e) {
        e.printStackTrace();
        return "Can't extract SWT jar to " + dest.getPath() + "\n" + e.toString();
      }
    }

    // Then load the extracted jar
    return appendJar(dest);
  }

  // Determine OS, and load appropriate SWT jar
  public static String loadPlatformSpecificSWTLibrary() {
    if (System.getProperty("os.name").toLowerCase().startsWith("mac"))
      return loadSWT("swt-mac.jar");
    else if (System.getProperty("os.name").toLowerCase().startsWith("windows"))
      return loadSWT("swt-windows.jar");
    else // linux
      return loadSWT("swt-linux.jar");
  }

  public static void main(final String[] args) throws Exception {
    // Pre-process basic args to get Debug.verbose, Main.headless, etc.
    Startup.preprocessArgs(args);

    // Install and load SWT
    String err = loadPlatformSpecificSWTLibrary();
    if (err != null)
      Startup.fail("Could not load platform-specific SWT Library.\n" + err);

    // Initialize BetterFileDialog and start main task
    BetterFileDialog.init(null, null, () -> Main.main(args));
  }

}
