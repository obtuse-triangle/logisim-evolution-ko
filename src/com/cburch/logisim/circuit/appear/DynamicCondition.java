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

package com.cburch.logisim.circuit.appear;
import static com.cburch.logisim.gui.main.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceComponent;

public class DynamicCondition {

  public static final String[] OPERATIONS = { "<", "<=", "==", "!=", ">=", ">", "has" };

  public static final Status[] STATUSVALS = {
    Status.NOERRORS, Status.SOMEERRORS, Status.ALLERRORS,
    Status.NOFLOATING, Status.SOMEFLOATING, Status.ALLFLOATING,
    Status.NODEFINED, Status.SOMEDEFINED, Status.ALLDEFINED
  };

  public static enum Status {
    NOERRORS("no errors", "dynamicConditionDialogStatusNoErrors"),
    SOMEERRORS("some errors", "dynamicConditionDialogStatusSomeErrors"),
    ALLERRORS("all errors", "dynamicConditionDialogStatusAllErrors"),

    NOFLOATING("no floating", "dynamicConditionDialogStatusNoFloating"),
    SOMEFLOATING("some floating", "dynamicConditionDialogStatusSomeFloating"),
    ALLFLOATING("all floating", "dynamicConditionDialogStatusAllFloating"),

    NODEFINED("no defined", "dynamicConditionDialogStatusNoDefined"),
    SOMEDEFINED("some defined", "dynamicConditionDialogStatusSomeDefined"),
    ALLDEFINED("all defined", "dynamicConditionDialogStatusAllDefined");

    public final String tag, key;
    private Status(String t, String k) { tag = t; key = k; }
    public String toString() { return toSvgString(); }
    public String toSvgString() { return S.get(key); }
    public String toDisplayString() { return S.get(key); }
  }
 
  // This is a sentinel value to represent "no condition"
  public static final DynamicCondition NONE = new DynamicCondition();
  private DynamicCondition() { }


  // DynamicCondition is always bound to a circuit, and is never null.
  // That way the circuit can be propagated from one element to another.
  private DynamicElement.Path path;
  private String op;
  private int numericValue;
  private Status statusValue;

  public DynamicCondition(DynamicElement.Path p, Status s) {
    this.path = p;
    this.op = "has";
    this.statusValue = s;
  }

  public DynamicCondition(DynamicElement.Path p, String op, int v) {
    this.path = p;
    this.op = op;
    this.numericValue = v;
  }

  public DynamicElement.Path getPath() { return path; }
  public String getOperation() { return op; }
  public Status getStatusValue() { return statusValue; }
  public int getNumericValue() { return numericValue; }

  public static DynamicCondition fromSvgString(String svgString, Circuit circuit) {
    if (svgString == null)
      return NONE; // null?
    String x = svgString.trim();
    if (x.equals(""))
      return NONE; // null?

    int numericValue = 0;
    Status statusValue = null;
    String op = null;

    for (Status s : STATUSVALS) {
      String suffix = " " + s.toSvgString();
      if (x.endsWith(suffix)) {
        statusValue = s;
        op = "has";
        x = x.substring(0, x.length() - suffix.length()).trim();
        break;
      }
    }
    if (statusValue == null) {
      int idx = x.lastIndexOf(' ');
      if (idx < 0)
        throw new IllegalArgumentException("Missing space in '" + svgString + "'");
      try {
        numericValue = Attributes.parseInteger(x.substring(idx+1));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Bad numerical value '" + x.substring(idx+1) + "' in '" + svgString +"'");
      }
      x = x.substring(0, idx).trim();
      for (String o : OPERATIONS) {
        if (o.equals("has"))
          continue;
        String suffix = " " + o;
        if (x.endsWith(suffix)) {
          op = o;
          x = x.substring(0, x.length() - suffix.length()).trim();
          break;
        }
      }
      if (op == null)
        throw new IllegalArgumentException("Unrecognized operation in '" + svgString +"'");
    }

    DynamicElement.Path path = DynamicElement.Path.fromSvgString(x, circuit);

    if (statusValue != null)
      return new DynamicCondition(path, statusValue);
    else
      return new DynamicCondition(path, op, numericValue);
  }

  public void replaceInstance(InstanceComponent c, InstanceComponent r) {
    if (path != null)
      path.replaceInstance(c, r);
  }

  public boolean evaluateCondition(CircuitState state) {
    if (this == NONE)
      return true;
    if (state == null)
      return false; // invisible by default during any errors
    Object data = DynamicElement.getData(path, state);
    InstanceComponent child = path.leaf();
    ComponentFactory f = child.getFactory();
    // Note: Path stores InstanceComponent, but its cojoined twin Instance seems
    // more common in factories, so let's use Instance for getDynamicValue().
    Value v = ((DynamicValueProvider)f).getDynamicValue(child.getInstance(), data);
    if (v == null || v.getWidth() == 0 || v == Value.NIL)
      return false; // invisible by default during any errors
    if (statusValue != null) {
      if (statusValue == Status.NOERRORS)
        return noneEqual(v.getAll(), Value.ERROR);
      if (statusValue == Status.SOMEERRORS)
        return someEqual(v.getAll(), Value.ERROR);
      if (statusValue == Status.ALLERRORS)
        return allEqual(v.getAll(), Value.ERROR);
      if (statusValue == Status.NOFLOATING)
        return noneEqual(v.getAll(), Value.UNKNOWN);
      if (statusValue == Status.SOMEFLOATING)
        return someEqual(v.getAll(), Value.UNKNOWN);
      if (statusValue == Status.ALLFLOATING)
        return allEqual(v.getAll(), Value.UNKNOWN);
      if (statusValue == Status.NODEFINED)
        return noneDefined(v.getAll());
      if (statusValue == Status.SOMEDEFINED)
        return someDefined(v.getAll());
      if (statusValue == Status.ALLDEFINED)
        return allDefined(v.getAll());
      return false; // should not be possible
    } else {
      if (!v.isFullyDefined())
        return false; // invisible by default
      int val = v.toIntValue();
      if (op.equals("=="))
        return (val == numericValue);
      if (op.equals("!="))
        return (val != numericValue);
      if (op.equals("<="))
        return (val <= numericValue);
      if (op.equals(">="))
        return (val >= numericValue);
      if (op.equals("<"))
        return (val < numericValue);
      if (op.equals(">"))
        return (val > numericValue);
      return false; // should not be possible
    }
  }

  static boolean noneEqual(Value[] as, Value b) {
    for (Value a: as)
      if (a == b)
        return false;
    return true;
  }

  static boolean someEqual(Value[] as, Value b) {
    for (Value a: as)
      if (a == b)
        return true;
    return false;
  }

  static boolean allEqual(Value[] as, Value b) {
    for (Value a: as)
      if (a != b)
        return false;
    return true;
  }

  static boolean noneDefined(Value[] as) {
    for (Value a: as)
      if (a == Value.TRUE || a == Value.FALSE)
        return false;
    return true;
  }

  static boolean someDefined(Value[] as) {
    for (Value a: as)
      if (a == Value.TRUE || a == Value.FALSE)
        return true;
    return false;
  }

  static boolean allDefined(Value[] as) {
    for (Value a: as)
      if (a != Value.TRUE && a != Value.FALSE)
        return false;
    return true;
  }

// evaluate dynamic value...
// com.cburch.logisim.instance.InstanceDataSingleton
//     data = (com.cburch.logisim.instance.InstanceDataSingleton)getData(path, state);
//     return data == null ? com.cburch.logisim.data.Value.FALSE : (com.cburch.logisim.data.Value) data.getValue();

  public String toString() { return toSvgString(); }

  public String toSvgString() {
    if (this == NONE)
      return "";
    if (op.equals("has"))
      return path.toSvgString() + " " + op + " " + statusValue.toSvgString();
    else
      return path.toSvgString() + " " + op + " " + String.format("0x%x", numericValue);
  }

  public String toDisplayString() {
    if (this == NONE)
      return "None";
    if (op.equals("has"))
      return path.toSvgString() + " " + op + " " + statusValue.toDisplayString();
    else
      return path.toSvgString() + " " + op + " " + String.format("0x%x", numericValue);
  }

}
