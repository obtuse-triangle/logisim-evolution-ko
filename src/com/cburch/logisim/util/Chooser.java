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
import static com.cburch.logisim.util.Strings.S;

import java.io.File;
import java.io.IOException;

import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;

import org.kwalsh.BetterFileDialog;

import com.cburch.logisim.prefs.AppPreferences;


/**
 * Chooser extends BetterFileDialog to keep track of the most recent successful
 * directory and use that for future dialogs, it tries to be a bit more robust
 * about the initial directory, and it can optionally give an overwrite warning
 * dialog.
 *
 * If Logisim is installed under a system administrator account and then is
 * attempted to run as a regular user, this can sometimes cause security
 * exceptions (23 Feb 2021). We also want to use AppPreferences as the default
 * location in most cases unless there is a more recent directory to use.
 *
 * We try a series of directories and use the first one that is readable:
 *   Most recent -- the last successful directory used, if available
 *   AppPreferences.DIALOG_DIRECTORY -- saved from user's previous history
 *   user.home/Documents -- maybe works on Windows 10, MacOS X, and most Linux platforms
 *   user.home -- seems reasonable, though not the typical default
 *   user.dir -- current working directory, sensible for command-line usage
 *   empty -- a platform-dependent default
 *   java.io.tmpdir -- a last restort if none of the above work
 * Upon success, we record the user's chosen directory for use in the next
 * call. Eventually, this also gets saved into AppPreferences.DIALOG_DIRECTORY
 * for later executions.
 */
public class Chooser {
 
  // A localized version of BetterFileDialog.Filter
  public static class LFilter {
    StringGetter nameGetter;
    String name;
    String[] ext;
    Object lock;
    BetterFileDialog.Filter filter;

    public LFilter(StringGetter name, String... ext) {
      this.nameGetter = name;
      this.ext = ext;
      this.lock = new Object();
    }

    public LFilter(String name, String... ext) {
      this.filter = new BetterFileDialog.Filter(name, ext);
    }

    public BetterFileDialog.Filter get() {
      if (nameGetter == null)
        return filter;
      synchronized(lock) {
        String newName = nameGetter.toString();
        if (!newName.equals(name)) {
          name = newName;
          filter = new BetterFileDialog.Filter(name, ext);
        }
        return filter;
      }
    }
  }

  private static BetterFileDialog.Filter[] convert(LFilter[] lfilters) {
    int n = lfilters == null ? 0 : lfilters.length;
    if (n == 0)
      return null;
    BetterFileDialog.Filter[] filters = new BetterFileDialog.Filter[n];
    for (int i = 0; i < n; i++)
      filters[i] = lfilters[i].get();
    return filters;
  }
    
  // FIXME: test circ.xml double extension on all platforms

  private Chooser() { }
  
  private static String recentDirectory = null;

  public static String getRecentDirectory() {
    return recentDirectory != null ? recentDirectory : AppPreferences.DIALOG_DIRECTORY.get();
  }
  public static void setRecentDirectory(String dir) {
    recentDirectory = dir;
  }

  private static File normalizeDirectory(File dir) {
    if (dir != null && !dir.isDirectory())
      dir = dir.getParentFile();
    if ((dir == null || !dir.canRead()) && recentDirectory != null)
      dir = new File(recentDirectory);
    if (dir == null || !dir.canRead())
      dir = new File(AppPreferences.DIALOG_DIRECTORY.get());
    if (dir == null || !dir.canRead())
      dir = new File(System.getProperty("user.home"), "Documents");
    if (dir == null || !dir.canRead())
      dir = new File(System.getProperty("user.home"));
    if (dir == null || !dir.canRead())
      dir = new File(System.getProperty("user.dir"));
    if (dir == null || !dir.canRead())
      dir = new File(System.getProperty("java.home"));
    return dir;
  }

  private static File normalizePath(File path) {
    if (path == null || path.isDirectory())
      return normalizeDirectory(path);
    File dir = normalizeDirectory(path.getParentFile());
    return new File(dir, path.getName());
  }

  public static void saveFailed(Component parent, Exception e) {
    JOptionPane.showMessageDialog(parent,
        e.getMessage(),
        S.get("saveErrorTitle"),
        JOptionPane.ERROR_MESSAGE);
  }

  // If a suggested file with parent directory is given, those will be used.
  // If a suggested file without parent directory is given, then a reasonable
  // directory will be used along with the given filename.
  // If no suggested file is given, then a reasonable directory and blank name
  // will be used.
  // Gives error popup if chosen path is a directory or not writable.
  // Gives warning/cancellation popup if chosen path exists.
  // Typically a least one filter should be given, and no ANY filters, otherwise
  // there is no guarantee the filename will have an appropriate extension.
  public static File savePopup(Component parent, String title,
      File suggest, LFilter... filters) {
    File path = normalizePath(suggest);
    File file = BetterFileDialog.saveFile(parent, title, path, convert(filters));

    if (file == null) // cancelled
      return null;

    recentDirectory = file.getParent();
    return file;
  }

  @FunctionalInterface
  public interface SaveCallback {
    public void save(File file) throws IOException;
  }

  public static File savePopup(SaveCallback callback,
      Component parent, String title, File suggest, LFilter... filters) {
    File file = savePopup(parent, title, suggest, filters);
    if (file != null) {
      try {
        callback.save(file);
      } catch (IOException e) {
        saveFailed(parent, e);
        return null;
      }
    }
    return file;
  }


  public static void loadFailed(Component parent, Exception e) {
    JOptionPane.showMessageDialog(parent,
        e.getMessage(),
        S.get("openErrorTitle"),
        JOptionPane.ERROR_MESSAGE);
  }

  // If a suggested file with parent directory is given, those will be used.
  // If a suggested file without parent directory is given, then a reasonable
  // directory will be used with the given filename.
  // If no suggested file is given, then a reasonable directory and blank name
  // will be used.
  // Gives error popup if chosen path is a directory or not readable.
  public static File loadPopup(Component parent, String title,
      File suggest, LFilter... filters) {
    File path = normalizePath(suggest);
    File file = BetterFileDialog.openFile(parent, title, path, convert(filters));

    if (file == null) // cancelled
      return null;

    recentDirectory = file.getParent();

    if (file.isDirectory()) {
      JOptionPane.showMessageDialog(parent,
          S.fmt("notFileMessage", file.getName()),
          S.get("openErrorTitle"), JOptionPane.OK_OPTION);
      return null;
    }
    if (!file.exists() || !file.canRead()) {
      JOptionPane.showMessageDialog(parent,
          S.fmt("cantReadMessage", file.getName()),
          S.get("openErrorTitle"), JOptionPane.OK_OPTION);
      return null;
    }

    return file;
  }

  @FunctionalInterface
  public interface LoadCallback {
    public void load(File file) throws IOException;
  }

  public static File loadPopup(LoadCallback callback,
      Component parent, String title, File suggest, LFilter... filters) {
    File file = loadPopup(parent, title, suggest, filters);
    if (file != null) {
      try {
        callback.load(file);
      } catch (IOException e) {
        loadFailed(parent, e);
        return null;
      }
    }
    return file;
  }

  public static File dirPopup(Component parent, String title,
      File suggest) {
    File initialDir = normalizeDirectory(suggest);
    File dir = BetterFileDialog.pickDir(parent, title, initialDir);
    if (dir != null)
      recentDirectory = dir.getPath();
    return dir;
  }

  // gui.hex.HexFile relies on preview pane of JFileChooser
  public static JFileChooser createJFileChooser(File suggest) {
    File dir = normalizeDirectory(suggest);
    JFileChooser chooser;
    if (dir == null)
      chooser = new JFileChooser();
    else
      chooser = new JFileChooser(dir);
    if (suggest != null && !suggest.isDirectory())
      chooser.setSelectedFile(suggest);
    return chooser;
  }

}
