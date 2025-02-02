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

package com.cburch.logisim.circuit;
import static com.cburch.logisim.circuit.Strings.S;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.bfh.logisim.hdlgenerator.HDLSupport;
import com.bfh.logisim.hdlgenerator.CircuitHDLGenerator;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.InstanceStateImpl;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;

public class SubcircuitFactory extends InstanceFactory {
  private class CircuitFeature
    implements StringGetter, MenuExtender, ActionListener {
    private Instance instance;
    private Project proj;

    public CircuitFeature(Instance instance) {
      this.instance = instance;
    }

    public void actionPerformed(ActionEvent e) {
      CircuitState superState = proj.getCircuitState();
      if (superState == null)
        return;
      CircuitState subState = getSubstate(superState, instance.getComponent());
      proj.setCircuitState(subState);
    }

    public void configureMenu(JPopupMenu menu, Project proj) {
      this.proj = proj;
      String name = instance.getFactory().getDisplayName();
      String text = S.fmt("subcircuitViewItem", name);
      JMenuItem item = new JMenuItem(text);
      item.addActionListener(this);
      menu.add(item);
    }

    public String toString() {
      return source.getName();
    }
  }

  private Circuit source;

  public SubcircuitFactory(Circuit source) {
    super("", null);
    this.source = source;
    setFacingAttribute(StdAttr.FACING);
    setDefaultToolTip(new CircuitFeature(null));
    setInstancePoker(SubcircuitPoker.class);
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC));
  }

  void computePorts(Instance instance) {
    Direction facing = instance.getAttributeValue(StdAttr.FACING);
    Map<Location, Instance> portLocs =
        source.getAppearance().getPortOffsets(facing);
    Port[] ports = new Port[portLocs.size()];
    Instance[] pins = new Instance[portLocs.size()];
    int i = -1;
    for (Map.Entry<Location, Instance> portLoc : portLocs.entrySet()) {
      i++;
      Location loc = portLoc.getKey();
      Instance pin = portLoc.getValue();
      String type = Pin.FACTORY.isInputPin(pin) ? Port.INPUT : Port.OUTPUT;
      BitWidth width = pin.getAttributeValue(StdAttr.WIDTH);
      ports[i] = new Port(loc.getX(), loc.getY(), type, width);
      pins[i] = pin;

      String label = pin.getAttributeValue(StdAttr.LABEL);
      if (label != null && label.length() > 0) {
        ports[i].setToolTip(StringUtil.constantGetter(label));
      }
    }

    CircuitAttributes attrs =
        (CircuitAttributes)instance.getAttributeSet();
    attrs.setPinInstances(pins);
    instance.setPorts(ports);
    instance.recomputeBounds();
    configureLabel(instance); // since this affects the circuit's bounds
  }

  private void configureLabel(Instance instance) {
    Bounds bds = instance.getBounds();
    Object loc = instance.getAttributeValue(StdAttr.LABEL_LOC);

    int x = bds.getX() + bds.getWidth() / 2;
    int y = bds.getY() + bds.getHeight() / 2;
    int ha = GraphicsUtil.H_CENTER;
    int va = GraphicsUtil.V_CENTER;
    if (loc == Direction.EAST) {
      x = bds.getX() + bds.getWidth() + 2;
      ha = GraphicsUtil.H_LEFT;
    } else if (loc == Direction.WEST) {
      x = bds.getX() - 2;
      ha = GraphicsUtil.H_RIGHT;
    } else if (loc == Direction.SOUTH) {
      y = bds.getY() + bds.getHeight() + 2;
      va = GraphicsUtil.V_TOP;
    } else if (loc == StdAttr.LABEL_CENTER) {
      ha = GraphicsUtil.H_CENTER;
      va = GraphicsUtil.V_CENTER;
    } else {
      y = bds.getY() - 2;
      va = GraphicsUtil.V_BASELINE;
    }
    instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, ha, va);
  }

  //
  // methods for configuring instances
  //
  @Override
  public void configureNewInstance(Instance instance) {
    CircuitAttributes attrs =
        (CircuitAttributes)instance.getAttributeSet();
    attrs.setSubcircuit(instance);

    instance.addAttributeListener();
    computePorts(instance);
    // configureLabel(instance); already done in computePorts
  }

  /**
   * Code taken from Cornell's version of Logisim:
   * http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  @Override
  public boolean contains(Location loc, AttributeSet attrs) {
    if (super.contains(loc, attrs)) {
      Direction facing = attrs.getValue(StdAttr.FACING);
      Direction defaultFacing = source.getAppearance().getFacing();
      Location query;

      if (facing.equals(defaultFacing)) {
        query = loc;
      } else {
        query = loc.rotate(facing, defaultFacing, 0, 0);
      }

      return source.getAppearance().contains(query);
    } else {
      return false;
    }
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new CircuitAttributes(source);
  }

  private void drawCircuitLabel(InstancePainter painter, Bounds bds,
      Direction facing, Direction defaultFacing) {
    AttributeSet staticAttrs = source.getStaticAttributes();
    String label =
        staticAttrs.getValue(CircuitAttributes.CIRCUIT_LABEL_ATTR);
    if (label != null && !label.equals("")) {
      Direction up =
          staticAttrs.getValue(CircuitAttributes.CIRCUIT_LABEL_FACING_ATTR);
      Font font =
          staticAttrs.getValue(CircuitAttributes.CIRCUIT_LABEL_FONT_ATTR);

      int back = label.indexOf('\\');
      int lines = 1;
      boolean backs = false;
      while (back >= 0 && back <= label.length() - 2) {
        char c = label.charAt(back + 1);
        if (c == 'n')
          lines++;
        else if (c == '\\')
          backs = true;
        back = label.indexOf('\\', back + 2);
      }

      int x = bds.getX() + bds.getWidth() / 2;
      int y = bds.getY() + bds.getHeight() / 2;
      Graphics g = painter.getGraphics().create();
      double angle = Math.PI / 2
          - (up.toRadians() - defaultFacing.toRadians())
          - facing.toRadians();
      if (Math.abs(angle) > 0.01) {
        Graphics2D g2 = (Graphics2D) g;
        g2.rotate(angle, x, y);
      }
      g.setFont(font);
      if (lines == 1 && !backs) {
        GraphicsUtil.drawCenteredText(g, label, x, y);
      } else {
        FontMetrics fm = g.getFontMetrics();
        int height = fm.getHeight();
        y = y - (height * lines - fm.getLeading()) / 2 + fm.getAscent();
        back = label.indexOf('\\');
        while (back >= 0 && back <= label.length() - 2) {
          char c = label.charAt(back + 1);
          if (c == 'n') {
            String line = label.substring(0, back);
            GraphicsUtil.drawText(g, line, x, y,
                GraphicsUtil.H_CENTER, GraphicsUtil.V_BASELINE);
            y += height;
            label = label.substring(back + 2);
            back = label.indexOf('\\');
          } else if (c == '\\') {
            label = label.substring(0, back)
                + label.substring(back + 1);
            back = label.indexOf('\\', back + 1);
          } else {
            back = label.indexOf('\\', back + 2);
          }
        }
        GraphicsUtil.drawText(g, label, x, y, GraphicsUtil.H_CENTER,
            GraphicsUtil.V_BASELINE);
      }
      g.dispose();
    }
  }

  @Override
  public StringGetter getDisplayGetter() {
    return StringUtil.constantGetter(source.getName());
  }

  @Override
  public Object getInstanceFeature(Instance instance, Object key) {
    if (key == MenuExtender.class)
      return new CircuitFeature(instance);
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public String getName() {
    return source.getName();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);
    Direction defaultFacing = source.getAppearance().getFacing();
    Bounds bds = source.getAppearance().getOffsetBounds();
    return bds.rotate(defaultFacing, facing, 0, 0);
  }

  public Circuit getSubcircuit() {
    return source;
  }

  public CircuitState getSubstate(CircuitState superState, Component comp) {
    //  return getSubstate(createInstanceState(superState, comp));
    CircuitState subState = (CircuitState)superState.getData(comp);
    if (subState == null) {
      subState = superState.createCircuitSubstateFor(comp, source);
      if (comp instanceof InstanceComponent)
        ((InstanceComponent) comp).fireInvalidated();
      else
        System.out.println("wrong kind ?!?!  ... " + comp);
    }
    return subState;
  }

  // public CircuitState getSubstate(CircuitState superState, Instance instance) {
  //   return getSubstate(createInstanceState(superState, instance));
  // }

  private CircuitState getSubstate(InstanceState stateInContext) {
    if (stateInContext instanceof InstanceStateImpl)
      return getSubstate(((InstanceStateImpl)stateInContext).getCircuitState(),
          ((InstanceStateImpl)stateInContext).getComponent());
    else
      throw new IllegalArgumentException("getSubstate on wrong type " + stateInContext);
    // CircuitState subState = (CircuitState)instanceState.getData();
    // if (subState == null) {
    //   subState = instanceState.createCircuitSubstateFor(source);
    //   instanceState.fireInvalidated();
    // }
    // return subState;
  }

  @Override
  public HDLSupport getHDLSupport(HDLSupport.ComponentContext ctx) {
    // don't need attrs, since circuit doesn't use them anyway
    return new CircuitHDLGenerator(ctx, this.source);
  }

  @Override
  public String getHDLNamePrefix(Component comp) {
    String s = "Subcircuit_" + source.getName();
    s = s.replaceAll("[^a-zA-Z0-9]{1,}", "_");
    s = s.replaceAll("^_", "");
    s = s.replaceAll("_$", "");
    return s;
  }

  @Override
  public void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      computePorts(instance);
    } else if (attr == StdAttr.LABEL_LOC) {
      configureLabel(instance);
    } else if (attr == CircuitAttributes.APPEARANCE_ATTR) {
      // final Circuit src = source;
      // System.out.println("subcirc instance changed appearance for " + source);
      // Thread.dumpStack();
      CircuitTransaction xn = new ChangeAppearanceTransaction();
      source.getLocker().execute(xn);
    }
  }

  private class ChangeAppearanceTransaction extends CircuitTransaction {
    ChangeAppearanceTransaction() { }
    @Override
    protected Map<Circuit, Integer> getAccessedCircuits() {
      Map<Circuit, Integer> accessMap = new HashMap<Circuit, Integer>();
      for (Circuit supercirc : source.getCircuitsUsingThis()) {
        accessMap.put(supercirc, READ_WRITE);
      }
      return accessMap;
    }
    @Override
    protected void run(CircuitMutator mutator) {
      source.getAppearance().recomputeDefaultAppearance();
    }
  }

  private void paintBase(InstancePainter painter, Graphics g) {
    CircuitAttributes attrs = (CircuitAttributes) painter.getAttributeSet();
    Direction facing = attrs.getFacing();
    Direction defaultFacing = source.getAppearance().getFacing();
    Location loc = painter.getLocation();
    g.translate(loc.getX(), loc.getY());
    source.getAppearance().paintSubcircuit(painter, g, facing);
    drawCircuitLabel(painter, getOffsetBounds(attrs), facing, defaultFacing);
    g.translate(-loc.getX(), -loc.getY());
    painter.drawLabel();
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    Color fg = g.getColor();
    int v = fg.getRed() + fg.getGreen() + fg.getBlue();
    Composite oldComposite = null;
    if (v > 50) {
      oldComposite = ((Graphics2D) g).getComposite();
      Composite c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
          0.5f);
      ((Graphics2D) g).setComposite(c);
    }
    paintBase(painter, g);
    if (oldComposite != null) {
      ((Graphics2D) g).setComposite(oldComposite);
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    paintBase(painter, painter.getGraphics());
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState stateInContext) {
    CircuitState subState = getSubstate(stateInContext);

    CircuitAttributes attrs =
        (CircuitAttributes)stateInContext.getAttributeSet();
    Instance[] pins = attrs.getPinInstances();
    for (int i = 0; i < pins.length; i++) {
      Instance pin = pins[i];
      InstanceState pinState = subState.getInstanceState(pin);
      if (Pin.FACTORY.isInputPin(pin)) {
        Value newVal = stateInContext.getPortValue(i);
        Value oldVal = Pin.FACTORY.getValue(pinState);
        if (!newVal.equals(oldVal)) {
          Pin.FACTORY.driveInputPin(pinState, newVal);
          Pin.FACTORY.propagate(pinState);
        }
      } else { // it is output-only
        Value val = pinState.getPortValue(0);
        stateInContext.setPort(i, val, 1);
      }
    }
  }

  /*
   * TODO public String getToolTip(ComponentUserEvent e) { return
   * S.fmt("subcircuitCircuitTip", source.getDisplayName()); }
   */
}
