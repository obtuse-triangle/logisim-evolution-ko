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

package com.cburch.logisim.std.base;
import static com.cburch.logisim.std.Strings.S;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.EditTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.MenuTool;
import com.cburch.logisim.tools.PokeTool;
import com.cburch.logisim.tools.SelectTool;
import com.cburch.logisim.tools.TextTool;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.tools.WiringTool;

public class Base extends Library {
  private final List<Tool> tools;
  private final AddTool textAdder = new AddTool(Text.FACTORY);
  private final SelectTool selectTool = new SelectTool();

  public Base() {
    WiringTool wiring = new WiringTool();

    tools = Arrays.asList(new Tool[] {
      new PokeTool(),
      new EditTool(selectTool, wiring),
      // Select by itself is kind of useless. It can select and move things, or
      // click to edit attributes. But it can't modify wires like EditTool can.
      /* select, */
      wiring,
      new TextTool(),
      new MenuTool(),
      // TextTool uses internall Text.FACTORY, but also supports click-to-edit,
      // custom cursor, etc. A dedicated "add text tool" is useless.
      /* new AddTool(Text.FACTORY), */
    });
  }

  @Override
  public Tool getTool(String name) {
    Tool t = super.getTool(name);
    if (t == null) {
      if (name.equals("Text"))
        return textAdder; // needed by XmlCircuitReader
      if (name.equals("Select Tool"))
        return selectTool; // not sure if this is necessary
    }
    return t;
  }

  @Override
  public String getDisplayName() {
    return S.get("baseLibrary");
  }

  @Override
  public String getName() {
    return "Base";
  }

  @Override
  public List<Tool> getTools() {
    return tools;
  }
}
