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

package com.cburch.logisim.std.io;
import static com.cburch.logisim.std.Strings.S;

import java.awt.Color;
import java.awt.event.KeyEvent;

import com.bfh.logisim.hdlgenerator.HDLSupport;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.DirectionConfigurator;

public class HexDigit extends InstanceFactory implements DynamicElementProvider {

  protected static final int HEX = 0;
  protected static final int DP = 1;

  public HexDigit() {
    super("Hex Digit Display", S.getter("hexDigitComponent"));
    setAttributes(new Attribute[] {
      Io.ATTR_ON_COLOR, Io.ATTR_OFF_COLOR, Io.ATTR_BACKGROUND,
          StdAttr.LABEL, StdAttr.LABEL_LOC, StdAttr.LABEL_FONT},
          new Object[] {
            new Color(240, 0, 0), SevenSegment.DEFAULT_OFF, Io.DEFAULT_BACKGROUND,
            "", Direction.NORTH, StdAttr.DEFAULT_LABEL_FONT});
    Port[] ps = new Port[2];
    ps[HEX] = new Port(0, 0, Port.INPUT, 4);
    ps[DP] = new Port(20, 0, Port.INPUT, 1);
    ps[HEX].setToolTip(S.getter("hexDigitDataTip"));
    ps[DP].setToolTip(S.getter("hexDigitDPTip"));
    setPorts(ps);
    setOffsetBounds(Bounds.create(-15, -60, 40, 60));
    setIconName("hexdig.gif");
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC));
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    instance.computeLabelTextField(0);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(0);
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    SevenSegment.drawBase(painter, true);
  }

  @Override
  public void propagate(InstanceState state) {
    int summary = 0;
    Value baseVal = state.getPortValue(HEX);
    if (baseVal == null)
      baseVal = Value.createUnknown(BitWidth.create(4));
    Value dpVal = state.getPortValue(DP);
    int segs; // each nibble is one segment, in top-down, left-to-right
    // order: middle three nibbles are the three horizontal segments
    switch (baseVal.toIntValue()) {
    case 0:
      segs = 0x1110111;
      break;
    case 1:
      segs = 0x0000011;
      break;
    case 2:
      segs = 0x0111110;
      break;
    case 3:
      segs = 0x0011111;
      break;
    case 4:
      segs = 0x1001011;
      break;
    case 5:
      segs = 0x1011101;
      break;
    case 6:
      segs = 0x1111101;
      break;
    case 7:
      segs = 0x0010011;
      break;
    case 8:
      segs = 0x1111111;
      break;
    case 9:
      segs = 0x1011011;
      break;
    case 10:
      segs = 0x1111011;
      break;
    case 11:
      segs = 0x1101101;
      break;
    case 12:
      segs = 0x1110100;
      break;
    case 13:
      segs = 0x0101111;
      break;
    case 14:
      segs = 0x1111100;
      break;
    case 15:
      segs = 0x1111000;
      break;
    default:
      segs = 0x0001000;
      break; // a dash '-'
    }
    if ((segs & 0x1) != 0)
      summary |= 4; // vertical seg in bottom right
    if ((segs & 0x10) != 0)
      summary |= 2; // vertical seg in top right
    if ((segs & 0x100) != 0)
      summary |= 8; // horizontal seg at bottom
    if ((segs & 0x1000) != 0)
      summary |= 64; // horizontal seg at middle
    if ((segs & 0x10000) != 0)
      summary |= 1; // horizontal seg at top
    if ((segs & 0x100000) != 0)
      summary |= 16; // vertical seg at bottom left
    if ((segs & 0x1000000) != 0)
      summary |= 32; // vertical seg at top left
    if (dpVal != null && dpVal.toIntValue() == 1)
      summary |= 128; // decimal point

    Object value = Integer.valueOf(summary);
    InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
    if (data == null) {
      state.setData(new InstanceDataSingleton(value));
    } else {
      data.setValue(value);
    }
  }

  @Override
  public HDLSupport getHDLSupport(HDLSupport.ComponentContext ctx) {
    return new HexDigitHDLGenerator(ctx);
  }

  @Override
  public String getHDLNamePrefix(Component comp) { return "HexDigit"; }

  public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path) {
    return new HexDigitShape(x, y, path);
  }
}
