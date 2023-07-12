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

import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import com.cburch.logisim.Main;

public class InputEventUtil {
  public static int fromDisplayString(String str) {
    int ret = 0;
    StringTokenizer toks = new StringTokenizer(str);
    while (toks.hasMoreTokens()) {
      String s = toks.nextToken();
      if (s.equals(S.get("ctrlMod")))
        ret |= InputEvent.CTRL_DOWN_MASK;
      else if (s.equals(S.get("altMod")))
        ret |= InputEvent.ALT_DOWN_MASK;
      else if (s.equals(S.get("shiftMod")))
        ret |= InputEvent.SHIFT_DOWN_MASK;
      else if (s.equals(S.get("button1Mod")))
        ret |= InputEvent.BUTTON1_DOWN_MASK;
      else if (s.equals(S.get("button2Mod")))
        ret |= InputEvent.BUTTON2_DOWN_MASK;
      else if (s.equals(S.get("button3Mod")))
        ret |= InputEvent.BUTTON3_DOWN_MASK;
      else
        throw new NumberFormatException("InputEventUtil");
    }
    return ret;
  }

  public static int fromXMLString(String str) {
    int ret = 0;
    StringTokenizer toks = new StringTokenizer(str);
    while (toks.hasMoreTokens()) {
      String s = toks.nextToken();
      if (s.equals(XML_CTRL))
        ret |= InputEvent.CTRL_DOWN_MASK;
      else if (s.equals(XML_SHIFT))
        ret |= InputEvent.SHIFT_DOWN_MASK;
      else if (s.equals(XML_ALT))
        ret |= InputEvent.ALT_DOWN_MASK;
      else if (s.equals(XML_BUTTON1))
        ret |= InputEvent.BUTTON1_DOWN_MASK;
      else if (s.equals(XML_BUTTON2))
        ret |= InputEvent.BUTTON2_DOWN_MASK;
      else if (s.equals(XML_BUTTON3))
        ret |= InputEvent.BUTTON3_DOWN_MASK;
      else
        throw new NumberFormatException("InputEventUtil");
    }
    return ret;
  }

  public static String toDisplayString(int mods) {
    ArrayList<String> arr = new ArrayList<String>();
    if ((mods & InputEvent.CTRL_DOWN_MASK) != 0)
      arr.add(S.get("ctrlMod"));
    if ((mods & InputEvent.ALT_DOWN_MASK) != 0)
      arr.add(S.get("altMod"));
    if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0)
      arr.add(S.get("shiftMod"));
    if ((mods & InputEvent.BUTTON1_DOWN_MASK) != 0)
      arr.add(S.get("button1Mod"));
    if ((mods & InputEvent.BUTTON2_DOWN_MASK) != 0)
      arr.add(S.get("button2Mod"));
    if ((mods & InputEvent.BUTTON3_DOWN_MASK) != 0)
      arr.add(S.get("button3Mod"));

    if (arr.isEmpty())
      return "";

    Iterator<String> it = arr.iterator();
    if (it.hasNext()) {
      StringBuilder ret = new StringBuilder();
      ret.append(it.next());
      while (it.hasNext()) {
        ret.append(" ");
        ret.append(it.next());
      }
      return ret.toString();
    } else {
      return "";
    }
  }

  public static String toKeyDisplayString(Character key, int mods) {
    ArrayList<String> arr = new ArrayList<String>();
    if (Main.MacOS) {
      if ((mods & InputEvent.META_DOWN_MASK) != 0)
        arr.add("\u2318"); // MacOS Command key
      if ((mods & InputEvent.CTRL_DOWN_MASK) != 0)
        arr.add("\u2303");
      if ((mods & InputEvent.ALT_DOWN_MASK) != 0)
        arr.add("\u2325"); // MacOS Option key
      if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0)
        arr.add("\u21E7");
    } else {
      if ((mods & InputEvent.META_DOWN_MASK) != 0)
        arr.add(S.get("metaMod"));
      if ((mods & InputEvent.CTRL_DOWN_MASK) != 0)
        arr.add(S.get("ctrlMod"));
      if ((mods & InputEvent.ALT_DOWN_MASK) != 0)
        arr.add(S.get("altMod"));
      if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0)
        arr.add(S.get("shiftMod"));
    }

    Iterator<String> it = arr.iterator();
    if (it.hasNext()) {
      StringBuilder ret = new StringBuilder();
      ret.append(it.next());
      while (it.hasNext()) {
        if (Main.MacOS)
          ret.append(" ");
        else
          ret.append("+");
        ret.append(it.next());
      }
      if (Main.MacOS)
        return ret.toString() + " " + key; // "^ A"
      else
        return ret.toString() + "-" + key; // "Ctrl+Shift-A"
    } else {
      return "";
    }
  }

  public static String toXMLString(int mods) {
    ArrayList<String> arr = new ArrayList<String>();
    if ((mods & InputEvent.CTRL_DOWN_MASK) != 0)
      arr.add(XML_CTRL);
    if ((mods & InputEvent.ALT_DOWN_MASK) != 0)
      arr.add(XML_ALT);
    if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0)
      arr.add(XML_SHIFT);
    if ((mods & InputEvent.BUTTON1_DOWN_MASK) != 0)
      arr.add(XML_BUTTON1);
    if ((mods & InputEvent.BUTTON2_DOWN_MASK) != 0)
      arr.add(XML_BUTTON2);
    if ((mods & InputEvent.BUTTON3_DOWN_MASK) != 0)
      arr.add(XML_BUTTON3);

    Iterator<String> it = arr.iterator();
    if (it.hasNext()) {
      StringBuilder ret = new StringBuilder();
      ret.append(it.next());
      while (it.hasNext()) {
        ret.append(" ");
        ret.append(it.next());
      }
      return ret.toString();
    } else {
      return "";
    }
  }

  public static String XML_CTRL = "Ctrl";

  public static String XML_SHIFT = "Shift";

  public static String XML_ALT = "Alt";

  public static String XML_BUTTON1 = "Button1";

  public static String XML_BUTTON2 = "Button2";

  public static String XML_BUTTON3 = "Button3";

  private InputEventUtil() {
  }
}
