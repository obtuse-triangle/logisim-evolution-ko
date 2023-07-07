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

package com.cburch.logisim.comp;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

import com.cburch.logisim.Main;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.CaretEvent;
import com.cburch.logisim.tools.CaretListener;

class TextFieldCaret implements Caret, TextFieldListener {

  public static final Color EDIT_BACKGROUND = new Color(0xff, 0xff, 0x99);
  public static final Color EDIT_BORDER = Color.DARK_GRAY;
  public static final Color SELECTION_BACKGROUND = new Color(0x99, 0xcc, 0xff);

  private LinkedList<CaretListener> listeners = new LinkedList<CaretListener>();
  protected TextField field;
  protected Graphics g;
  protected String oldText;
  protected String curText;
  protected int pos, end;

  public TextFieldCaret(TextField field, Graphics g, int pos) {
    this.field = field;
    this.g = g;
    this.oldText = field.getText();
    this.curText = field.getText();
    this.pos = pos;
    this.end = pos;

    field.addTextFieldListener(this);
  }

  public TextFieldCaret(TextField field, Graphics g, int x, int y) {
    this(field, g, 0);
    pos = end = findCaret(x, y);
  }

  public void addCaretListener(CaretListener l) {
    listeners.add(l);
  }

  public void cancelEditing() {
    CaretEvent e = new CaretEvent(this, oldText, oldText);
    curText = oldText;
    pos = curText.length();
    end = pos;
    for (CaretListener l : new ArrayList<CaretListener>(listeners)) {
      l.editingCanceled(e);
    }
    field.removeTextFieldListener(this);
  }

  public void commitText(String text) {
    curText = text;
    pos = curText.length();
    end = pos;
    field.setText(text);
  }

  public void draw(Graphics g) {
    int x = field.getX();
    int y = field.getY();
    int halign = field.getHAlign();
    int valign = field.getVAlign();
    Font font = field.getFont();
    if (font != null)
      g.setFont(font);

    // draw boundary
    Bounds box = getBounds(g);
    g.setColor(EDIT_BACKGROUND);
    g.fillRect(box.getX(), box.getY(), box.getWidth(), box.getHeight());
    g.setColor(EDIT_BORDER);
    g.drawRect(box.getX(), box.getY(), box.getWidth(), box.getHeight());

    // draw selection
    if (pos != end) {
      g.setColor(SELECTION_BACKGROUND);
      Rectangle p = GraphicsUtil.getTextCursor(g, font, curText, x, y, pos < end ? pos : end, halign, valign);
      Rectangle e = GraphicsUtil.getTextCursor(g, font, curText, x, y, pos < end ? end : pos, halign, valign);
      g.fillRect(p.x, p.y - 1, e.x - p.x + 1, e.height + 2);
    }

    // draw text
    g.setColor(Color.BLACK);
    GraphicsUtil.drawText(g, curText, x, y, halign, valign);

    // draw cursor
    if (pos == end) {
      Rectangle p = GraphicsUtil.getTextCursor(g, font, curText, x, y, pos, halign, valign);
      g.drawLine(p.x, p.y, p.x, p.y + p.height);
    }
  }

  public String getText() {
    return curText;
  }

  public Bounds getBounds(Graphics g) {
    int x = field.getX();
    int y = field.getY();
    int halign = field.getHAlign();
    int valign = field.getVAlign();
    Font font = field.getFont();
    Bounds bds = Bounds.create(GraphicsUtil.getTextBounds(g, font, curText, x, y, halign, valign));
    Bounds box = bds.add(field.getBounds(g)).expand(3);
    return box;
  }

  public void keyPressed(KeyEvent e) {
    int ign;
    // Control unused on MacOS, but used as menuMask on Linux/Windows
    // Alt unused on Linux/Windows, but used for wordMask on MacOS
    // Meta unused on Linux/Windows, but used for menuMask on MacOS
    if (Main.MacOS)
      ign = InputEvent.CTRL_DOWN_MASK;
    else
      ign = InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK;
    if ((e.getModifiersEx() & ign) != 0)
      return;
    int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    int wordMask =  Main.MacOS
        ? InputEvent.ALT_DOWN_MASK /* MacOS Option keys, don't bother with ALT_GRAPH_DOWN_MASK */
        : menuMask; /* Windows/Linux wordMask == menuMask == CONTROL_DOWN_MASK */
    boolean shift = ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0);
    boolean menukey = ((e.getModifiersEx() & menuMask) != 0);
    boolean wordkey = ((e.getModifiersEx() & wordMask) != 0);
    processMovementKeys(e, shift, wordkey, menukey);
    if (e.isConsumed())
      return;
    if (menukey)
      menuShortcutKeyPressed(e, shift);
    else if (!wordkey)
      normalKeyPressed(e, shift);
  }

  protected boolean wordBoundary(int pos) {
    return (pos <= 0)
        || (pos >= curText.length())
        || (Character.isWhitespace(curText.charAt(pos-1))
          && !Character.isWhitespace(curText.charAt(pos)));
  }

  protected boolean allowedCharacter(char c) {
    return (c != KeyEvent.CHAR_UNDEFINED) && !Character.isISOControl(c);
  }

  protected void menuShortcutKeyPressed(KeyEvent e, boolean shift) {
    boolean cut = false;
    switch (e.getKeyCode()) {
    case KeyEvent.VK_A: // select all
      pos = 0;
      end = curText.length();
      e.consume();
      break;
    case KeyEvent.VK_CUT:
    case KeyEvent.VK_X: // cut
      cut = true;
      // fall through
    case KeyEvent.VK_COPY:
    case KeyEvent.VK_C: // copy
      if (end != pos) {
        int pp = (pos < end ? pos : end);
        int ee = (pos < end ? end : pos);
        String s = curText.substring(pp, ee);
        StringSelection sel = new StringSelection(s);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
        if (cut) {
          normalizeSelection();
          curText = curText.substring(0, pos) + (end < curText.length() ? curText.substring(end) : "");
          end = pos;
        }
      }
      e.consume();
      break;
    case KeyEvent.VK_INSERT:
    case KeyEvent.VK_PASTE:
    case KeyEvent.VK_V: // paste
      try {
        String s = (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        boolean lastWasSpace = false;
        for (int i = 0; i < s.length(); i++) {
          char c = s.charAt(i);
          if (!allowedCharacter(c)) {
            if (lastWasSpace)
              continue;
            c = ' ';
          }
          lastWasSpace = (c == ' ');
          normalizeSelection();
          if (end < curText.length()) {
            curText = curText.substring(0, pos) + c + curText.substring(end);
          } else {
            curText = curText.substring(0, pos) + c;
          }
          ++pos;
          end = pos;
        }
      } catch (Exception ex) {
      }
      e.consume();
      break;
    default:
      ; // ignore
    }
  }

  // Text field movement shortcuts...
  // For a multi-line text field are ten possible cursor movements:
  //    ______________________________________
  //   |(-5)                                  |   (+1) next char      (-1) prev char
  //   |                 (-4)                 |   (+2) next word      (-2) prev word
  //   |(-3)    (-2)   (-1)I(+1)   (+2)   (+3)|   (+3) end of line    (-3) start of line
  //   |                 (+4)             ____|   (+4) down a line    (-4) up a line
  //   |_____________________________(+5)|        (+5) end of text    (-5) start of text
  // 
  // When cursor is on first or last line, 4 degenerates to 5.
  // For a single-line text field the same holds except that 3, 4 and 5 are all equivalent.
  //
  //                                                   single-line          multi-line
  //          key          modifiers                   textfield action     textfield action
  // MacOS:
  //          left/right   -                           +/- 1                +/- 1            
  //          left/right   option/wordkey              +/- 2                +/- 2             
  //          left/right   command/menukey             +/- 5                +/- 3
  //          up/down      -                           +/- 5                +/- 4
  //          up/down      command/menukey             +/- 5                +/- 5
  //          home/end     -                           +/- 5                +/- 5
  //          pgup/pgdn    -                           +/- 5                +/- 5
  // Linux/Windows:
  //          left/right   -                           +/- 1                +/- 1            
  //          left/right   control/wordkey/menukey     +/- 2                +/- 2             
  //          up/down      -                           +/- 5                +/- 4
  //          up/down      control/wordkey/menukey     +/- 5                +/- 5
  //          home/end     -                           +/- 5                +/- 3
  //          home/end     control/wordkey/menukey     +/- 5                +/- 5
  //          pgup/pgdn    -                           +/- 5                +/- 5
  //
  // TODO: support for old style linux/apple movemet keys, like control-A / control-E ?

  protected void cancelSelection(int direction) {
    // selection is being canceled by left/right movement
    if (direction < 0) end = pos;
    else pos = end;
  }

  protected void moveCaret(int move, boolean shift) {
    if (!shift)
      normalizeSelection();

    if (move < -5 || move == 0 || move > 5) { // invalid
      return;
    } else if (move <= -3) { // start of line, up a line, start of text
      pos = 0;
    } else if (move >= +3) { // end of line, down a line, end of text
      pos = curText.length();
    } else { // next/prev char, next/prev word
      int dx = (move < 0 ? -1 : +1);
      boolean byword = (move == -2 || move == +2);
      if (!shift && pos != end) {
        // selection is being canceled by left/right movement,
        // so we count the cancellation as the first step
        cancelSelection(move);
      } else {
        // move one char left/right as the first step, if possible
        if (dx < 0 && pos > 0) pos--;
        else if (dx > 0 && pos < curText.length()) pos++;
      }
      if (byword) {
        while (!wordBoundary(pos))
          pos += dx;
      }
    }

    if (!shift)
      end = pos;
  }
  
  protected void processMovementKeys(KeyEvent e, boolean shift, boolean wordkey, boolean menukey) {
    int dir = +1;
    switch (e.getKeyCode()) {
    case KeyEvent.VK_LEFT:
    case KeyEvent.VK_KP_LEFT:
      dir = -1;
      // fall through
    case KeyEvent.VK_RIGHT:
    case KeyEvent.VK_KP_RIGHT:
      if (menukey && !wordkey)
        moveCaret(dir*3, shift); // MacOS start/end of line
      else if (wordkey)
        moveCaret(dir*2, shift); // prev/next word
      else 
        moveCaret(dir*1, shift); // prev/next char
      e.consume();
      break;
    case KeyEvent.VK_UP:
    case KeyEvent.VK_KP_UP:
      dir = -1;
      // fall through
    case KeyEvent.VK_DOWN:
    case KeyEvent.VK_KP_DOWN:
      if (menukey)
        moveCaret(dir*5, shift); // start/end of text
      else
        moveCaret(dir*4, shift); // up/down a line
      e.consume();
      break;
    case KeyEvent.VK_PAGE_UP:
      dir = -1;
      // fall through
    case KeyEvent.VK_PAGE_DOWN:
      moveCaret(dir*5, shift); // start/end of text
      e.consume();
      break;
    case KeyEvent.VK_HOME:
      dir = -1;
      // fall through
    case KeyEvent.VK_END:
      if (Main.MacOS)
        moveCaret(dir*5, shift); //  MacOS start/end of text
      else if (menukey)
        moveCaret(dir*5, shift); // start/end of text
      else 
        moveCaret(dir*3, shift); // start/end of line
      e.consume();
      break;
    default:
      break;
    }
  }

  protected void normalKeyPressed(KeyEvent e, boolean shift) {
    switch (e.getKeyCode()) {
    case KeyEvent.VK_ESCAPE:
    case KeyEvent.VK_CANCEL:
      cancelEditing();
      e.consume();
      break;
    case KeyEvent.VK_CLEAR:
      curText = "";
      end = pos = 0;
      e.consume();
      break;
    case KeyEvent.VK_ENTER:
      stopEditing();
      e.consume();
      break;
    case KeyEvent.VK_BACK_SPACE: // DELETE on MacOS?
      normalizeSelection();
      if (pos != end) {
        curText = curText.substring(0, pos) + curText.substring(end);
        end = pos;
      } else if (pos > 0) {
        curText = curText.substring(0, pos - 1)
            + curText.substring(pos);
        --pos;
        end = pos;
      }
      e.consume();
      break;
    case KeyEvent.VK_DELETE: // BACK_SPACE on MacOS?
      normalizeSelection();
      if (pos != end) {
        curText = curText.substring(0, pos) + (end < curText.length() ? curText.substring(end) : "");
        end = pos;
      } else if (pos < curText.length()) {
        curText = curText.substring(0, pos)
            + curText.substring(pos + 1);
      }
      e.consume();
      break;
    default:
      ; // ignore
    }
  }

  public void keyReleased(KeyEvent e) { }

  public void keyTyped(KeyEvent e) {
    int ign = InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK;
    if ((e.getModifiersEx() & ign) != 0)
      return;

    e.consume();
    char c = e.getKeyChar();
    if (allowedCharacter(c)) {
      normalizeSelection();
      if (end < curText.length()) {
        curText = curText.substring(0, pos) + c + curText.substring(end);
      } else {
        curText = curText.substring(0, pos) + c;
      }
      ++pos;
      end = pos;
    } else if (c == '\n') {
      stopEditing();
    }
  }

  protected void normalizeSelection() {
    if (pos > end) {
      int t = end;
      end = pos;
      pos = t;
    }
  }

  public void mouseDragged(MouseEvent e) {
    end = findCaret(e.getX(), e.getY());
  }

  public void mousePressed(MouseEvent e) {
    pos = end = findCaret(e.getX(), e.getY());
  }

  public void mouseReleased(MouseEvent e) {
    end = findCaret(e.getX(), e.getY());
  }

  protected int findCaret(int x, int y) {
    x -= field.getX();
    y -= field.getY();
    int halign = field.getHAlign();
    int valign = field.getVAlign();
    return GraphicsUtil.getTextPosition(g, field.getFont(), curText, x, y, halign, valign);
  }

  public void removeCaretListener(CaretListener l) {
    listeners.remove(l);
  }

  public void stopEditing() {
    CaretEvent e = new CaretEvent(this, oldText, curText);
    field.setText(curText);
    for (CaretListener l : new ArrayList<CaretListener>(listeners)) {
      l.editingStopped(e);
    }
    field.removeTextFieldListener(this);
  }

  public void textChanged(TextFieldEvent e) {
    curText = field.getText();
    oldText = curText;
    pos = curText.length();
    end = pos;
  }
}
