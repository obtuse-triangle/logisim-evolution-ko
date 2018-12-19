/* ProjectExplorerFalseRootNode
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

package com.cburch.logisim.gui.generic;

import java.util.Enumeration;

import com.cburch.logisim.tools.Library;
import com.cburch.logisim.std.base.Base;

public class ProjectExplorerRootNode extends ProjectExplorerModel.Node<Library> {

  private static final long serialVersionUID = 1L;

  private static Base getBaseLib(Library projLib) {
    for (Library lib : projLib.getLibraries())
      if (lib instanceof Base)
        return (Base)lib;
    return null;
  }

  ProjectExplorerRootNode(ProjectExplorerModel model, Library lib) {
    super(model, lib, null);
    add(new ProjectExplorerLibraryNode(model, getBaseLib(lib), this));
    add(new ProjectExplorerLibraryNode(model, lib, this));
  }

  @Override
  void decommission() {
    for (Enumeration<?> en = children(); en.hasMoreElements();) {
      Object n = en.nextElement();
      if (n instanceof ProjectExplorerModel.Node<?>)
        ((ProjectExplorerModel.Node<?>) n).decommission();
    }
  }

}
