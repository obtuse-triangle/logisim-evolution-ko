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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.bfh.logisim.gui.Console;
import com.cburch.logisim.Main;

public class HTTP {

  // Send an HTTP POST with key-value pairs to given url. Keys should be
  // strings, values can be String or File.
  // Returns true on success, false on failure. Writes reply and errors to console.
  public static boolean post(Console console, String url, Object... keyvals) {

    try {
      console.printf("POST " + url + "\n");

      String boundary = "logisim_fpga_"+Long.toHexString(System.currentTimeMillis());
      HttpURLConnection connection = (HttpURLConnection)(new URL(url)).openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
      connection.setRequestProperty("User-Agent", "Logisim-Evolution-"+Main.VERSION);
      connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

      // try (OutputStreamWriter outstream = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
      //     PrintWriter out = new PrintWriter(outstream)) {
      try (OutputStream out = connection.getOutputStream()) {
        for (int i = 0; i < keyvals.length; i += 2) {
          String key = (String)keyvals[i];
          Object val = keyvals[i+1];
          if (val == null) continue;
          console.printf("  "+key+": " + val + "\n");
          postPart(console, boundary, out, key, val);
        }
        print(out, "--" + boundary + "--\r\n");
      }

      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
        console.printf(console.ERROR, "HTTP Error, bad response (code = " + responseCode + ")");
        return false;
      }

      try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
          console.printf(inputLine);
        }
      }

      return true;

    } catch (IOException e) {
      console.printf(console.ERROR, "Can't perform HTTP POST: " + e.getMessage());
      return false;
    }

  }

  static void print(OutputStream out, String s) throws IOException {
    out.write(s.getBytes(StandardCharsets.UTF_8));
  }

  private static void postPart(Console console, String boundary, OutputStream out, String key, Object val) throws IOException {
    if (val instanceof String) {
      print(out, "--" + boundary + "\r\n");
      print(out, "Content-Disposition: form-data; name=\""+key+"\"\r\n");
      print(out, "\r\n");
      print(out, val + "\r\n");
    } else if (val instanceof File) {
      File file = (File)val;
      print(out, "--" + boundary + "\r\n");
      print(out, "Content-Disposition: form-data; name=\""+key+"\"; filename=\"" + file.getName() + "\"\r\n");
      if (file.getName().endsWith(".zip")) {
        print(out, "Content-Type: application/zip\r\n");
        print(out, "\r\n");
      } else {
        print(out, "Content-Type: application/octet-stream\r\n");
        print(out, "\r\n");
      }
      try (FileInputStream infile = new FileInputStream(file);
          BufferedInputStream instream = new BufferedInputStream(infile)) {
        long len = instream.transferTo(out);
        console.printf("Uploaded " + len + " bytes.\n");
      }
      print(out, "\r\n");
    } else {
      throw new IOException("invalid value type for HTTP POST form: " + val);
    }
  }

  // Send an HTTP GET. Keys should be
  // strings, values can be String or File.
  // Returns true on success, false on failure. Writes reply and errors to console.
  public static boolean get(Console console, String url, String filename) {

    try {
      console.printf("GET " + url + "\n");
      console.printf("Save as: " + filename + "\n");
      HttpURLConnection connection = (HttpURLConnection)(new URL(url)).openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("User-Agent", "Logisim-Evolution-"+Main.VERSION);

      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
        console.printf(console.ERROR, "HTTP Error, bad response (code = " + responseCode + ")");
        return false;
      }

      try (FileOutputStream outfile = new FileOutputStream(filename);
          InputStream instream = connection.getInputStream()) {
        long len = instream.transferTo(outfile);
        console.printf("Downloaded " + len + " bytes.\n");
      }

      return true;

    } catch (IOException e) {
      console.printf(console.ERROR, "Can't perform HTTP POST: " + e.getMessage());
      return false;
    }

  }

}
