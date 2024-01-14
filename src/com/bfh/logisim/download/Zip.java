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

package com.bfh.logisim.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.bfh.logisim.gui.Console;

public class Zip {

  // sanity/safety checks: file and directory names may not begin with "." or
  // "/", may not contain ".." or "//", and may only contain characters from
  // [a-zA-Z0-9_. /-].
  public static final String FILENAME_FORBIDDEN_CHARS = "[^a-zA-Z0-9_. /-]";

  public static boolean isValidEntryName(String name) {
    return name != null && name.length() > 0
      && name.charAt(0) != '.'
      && name.charAt(0) != '/'
      && !name.matches(FILENAME_FORBIDDEN_CHARS)
      && !name.contains("..")
      && !name.contains("//");
  }

  // Uncompress contents of given zip archive into given destination directory.
  // Returns true on success, false on failure. Errors are written to console.
  public static boolean uncompress(Console console, String zipname, String dst) {
    byte[] buffer = new byte[4096];
    try (FileInputStream infile = new FileInputStream(zipname);
        ZipInputStream zip = new ZipInputStream(infile)) {

      ZipEntry entry = zip.getNextEntry();
      while (entry != null) {

        String name = entry.getName();
        if (!isValidEntryName(name))
          throw new IOException("Zip file entry has unsafe name: " + name);

        if (entry.isDirectory()) {

          File newDir = new File(dst, name);
          if (!newDir.isDirectory() && !newDir.mkdirs())
            throw new IOException("Failed to create directory " + newDir);

        } else {

          File newFile = new File(dst, name);
          File parent = newFile.getParentFile();
          if (!parent.isDirectory() && !parent.mkdirs())
            throw new IOException("Failed to create directory " + parent);

          try (FileOutputStream outfile = new FileOutputStream(newFile)) {
            int len;
            while ((len = zip.read(buffer)) > 0)
              outfile.write(buffer, 0, len);
          }
        }

        entry = zip.getNextEntry();
      }

      zip.closeEntry();
      return true;
    } catch (IOException e) {
      console.printf(console.ERROR, "Can't unzip: " + e.getMessage());
      return false;
    }
  }

  // Make zip file by compressing given source directory (recursively) or file.
  // Returns true on success, false on failure. Errors are written to console.
  public static boolean compress(Console console, String zipname, String src) {
    try (FileOutputStream outfile = new FileOutputStream(zipname);
        ZipOutputStream zip = new ZipOutputStream(outfile)) {
      if (src.endsWith("/")) {
        // Don't include top-level directory name
        File f = new File(src);
        File[] children = f.listFiles();
        for (File child : children)
          addEntry(zip, "", child);
      } else {
        addEntry(zip, "", new File(src));
      }
      return true;
    } catch (IOException e) {
      console.printf(console.ERROR, "Can't zip: " + e.getMessage());
      return false;
    }
  }

  private static void addEntry(ZipOutputStream zip, String path, File f) throws IOException {
    if (f.isHidden())
      return;
    if (!path.equals("") && !path.endsWith("/"))
      path += "/";
    if (f.isDirectory()) {
      path += f.getName() + "/";
      zip.putNextEntry(new ZipEntry(path));
      zip.closeEntry();
      File[] children = f.listFiles();
      for (File child : children)
        addEntry(zip, path, child);
    } else {
      try (FileInputStream infile = new FileInputStream(f)) {
        ZipEntry entry = new ZipEntry(path + f.getName());
        zip.putNextEntry(entry);
        byte[] bytes = new byte[4096];
        int len;
        while ((len = infile.read(bytes)) >= 0)
          zip.write(bytes, 0, len);
      }
    }
  }

}
