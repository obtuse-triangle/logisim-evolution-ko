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

package com.cburch.logisim.gui.appear;
import static com.cburch.logisim.gui.main.Strings.S;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.appear.DynamicCondition;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicValueProvider;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.util.JInputDialog;

public class DynamicConditionDialog extends JDialog implements JInputDialog<DynamicCondition> {

  private static final long serialVersionUID = 1L;

  JButton ok, cancel;
  DefaultMutableTreeNode root;
  JTree tree;
  JComboBox<String> operation, enable;
  JTextField numericValue;
  JComboBox<DynamicCondition.Status> statusValue;
  JPanel valuePanel; // "status" or "numeric"

  String[] enableOptions;

  DynamicCondition result;

  public static DynamicConditionDialog makeDialog(Window parent, Circuit circuit, DynamicCondition dyn) {
    DefaultMutableTreeNode root = enumerate(circuit, null);
    if (root != null)
      return new DynamicConditionDialog(parent, circuit, root, dyn);
    JOptionPane.showMessageDialog(parent,
        S.fmt("dynamicConditionDialogEmptyMessage", circuit.getName()),
        S.get("dynamicConditionDialogEmptyTitle"),
        JOptionPane.ERROR_MESSAGE);
    return null;
  }

  private DynamicConditionDialog(Window parent, Circuit circuit,
      DefaultMutableTreeNode root, DynamicCondition dyn) {
    super(parent, Dialog.DEFAULT_MODALITY_TYPE);
    this.root = root;

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    setTitle(S.fmt("dynamicConditionDialogTitle", circuit.getName()));

    ok = new JButton(S.get("dynamicConditionDialogOkButton"));
    cancel = new JButton(S.get("dynamicConditionDialogCancelButton"));
    ok.addActionListener((e) -> apply(true));
    cancel.addActionListener((e) -> apply(false));
    JPanel buttonPanel = new JPanel();
    buttonPanel.add(ok);
    buttonPanel.add(cancel);
   
    enableOptions = new String[] {
      S.get("dynamicConditionDialogVisibleDynamic"),
      S.get("dynamicConditionDialogVisibleAlways") };
    enable = new JComboBox<>();
    for (String op : enableOptions)
      enable.addItem(op);
    enable.setSelectedItem(enableOptions[0]);
    enable.setEditable(false);
    enable.addItemListener((event) -> enableChanged((String)event.getItem()));
    enable.setPreferredSize(new Dimension(300, 24));

    JPanel topPanel = new JPanel();
    // topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.LINE_AXIS));
    topPanel.add(new JLabel(S.get("dynamicConditionDialogEnableTitle") + "  "));
    topPanel.add(enable);

    tree = new JTree(root);
    tree.addTreeSelectionListener((event) -> ok.setEnabled(validateAllInput()));
    tree.setSelectionModel(new DefaultTreeSelectionModel() {
      public void setSelectionPaths(final TreePath[] paths) {
        for (TreePath p : paths) {
          if (!validateTreePath(p))
            return;
        }
        super.setSelectionPaths(paths);
      }
    });
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    JScrollPane treePane = new JScrollPane(tree);

    // GridBagLayout gb = new GridBagLayout();
    GridBagConstraints gc = new GridBagConstraints();
    JPanel body = new JPanel(new GridBagLayout());

    gc.insets = new Insets(10, 10, 10, 10);
    gc.fill = GridBagConstraints.NONE;
    gc.weightx = gc.weighty = 0.0;
    gc.anchor = GridBagConstraints.NORTH;

    // Left panel
    
    gc.gridx = 0;
    gc.gridy = 0;
    body.add(new JLabel(S.get("dynamicConditionDialogNodeTitle")), gc);

    gc.gridx = 0;
    gc.gridy = 1;
    gc.fill = GridBagConstraints.BOTH;
    gc.weightx = gc.weighty = 1.0;
    body.add(treePane, gc);
    gc.weightx = gc.weighty = 0.0;
    gc.fill = GridBagConstraints.NONE;

    // Middle panel

    operation = new JComboBox<>();
    for (String op : DynamicCondition.OPERATIONS)
      operation.addItem(op);
    operation.setEditable(false);
    operation.addItemListener((event) -> operationChanged((String)event.getItem()));
    operation.setPreferredSize(new Dimension(100, 24));

    gc.gridx = 1;
    gc.gridy = 0;
    body.add(new JLabel(S.get("dynamicConditionDialogOperationTitle")), gc);

    gc.gridx = 1;
    gc.gridy = 1;
    body.add(operation, gc);

    // Numeric sub-panel
    
    numericValue = new JTextField();
    numericValue.setPreferredSize(new Dimension(100, 24));
    numericValue.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void removeUpdate(DocumentEvent e) { numericChanged(); }
      @Override
      public void insertUpdate(DocumentEvent e) { numericChanged(); }
      @Override
      public void changedUpdate(DocumentEvent e) { numericChanged(); }
    });
    JPanel rightN = new JPanel(new GridBagLayout());

    gc.gridx = 0;
    gc.gridy = 0;
    rightN.add(new JLabel(S.get("dynamicConditionDialogValueTitle")), gc);
    gc.gridx = 0;
    gc.gridy = 1;
    rightN.add(numericValue, gc);

    // Status sub-panel

    statusValue = new JComboBox<>();
    for (DynamicCondition.Status s : DynamicCondition.STATUSVALS)
      statusValue.addItem(s);
    statusValue.setEditable(false);
    operation.addItemListener((event) -> operationChanged((String)event.getItem()));
    JPanel rightS = new JPanel(new GridBagLayout());

    gc.gridx = 0;
    gc.gridy = 0;
    rightS.add(new JLabel(S.get("dynamicConditionDialogValueTitle")), gc);
    gc.gridx = 0;
    gc.gridy = 1;
    rightS.add(statusValue, gc);

    // Right panel

    valuePanel = new JPanel(new CardLayout());
    valuePanel.add(rightN, "numeric");
    valuePanel.add(rightS, "status");

    gc.insets = new Insets(0, 0, 0, 0);
    gc.gridx = 2;
    gc.gridy = 0;
    gc.gridheight = 2;
    body.add(valuePanel, gc);

    // condition = new JTextField();
    // JPanel top = new JPanel(new BorderLayout());
    // top.add(condition, BorderLayout.CENTER);

    Container contents = this.getContentPane();
    contents.setLayout(new BorderLayout());
    contents.add(topPanel, BorderLayout.PAGE_START);
    contents.add(body, BorderLayout.CENTER);
    contents.add(buttonPanel, BorderLayout.PAGE_END);
    this.pack();

    Dimension pref = contents.getPreferredSize();
    if (pref.width > 750 || pref.height > 550 || pref.width < 200 || pref.height < 150) {
      if (pref.width > 750)
        pref.width = 750;
      else if (pref.width < 200)
        pref.width = 200;
      if (pref.height > 550)
        pref.height = 550;
      else if (pref.height < 200)
        pref.height = 200;
      this.setSize(pref);
    }

    operation.setSelectedItem("!=");
    numericValue.setText("0x0");
    statusValue.setSelectedItem(DynamicCondition.Status.ALLDEFINED);
    ok.setEnabled(false);
    setValue(dyn);

    pack();
    setLocationRelativeTo(parent);
  }

  @Override
  public void setValue(DynamicCondition dyn) {
    if (dyn == null || dyn == DynamicCondition.NONE) {
      tree.clearSelection();
    } else {
      TreePath path = ShowStateDialog.toTreePath(root, dyn.getPath());
      if (path == null)
        tree.clearSelection();
      else
        tree.setSelectionPath(path);
      operation.setSelectedItem(dyn.getOperation());
      if (dyn.getOperation().equals("op"))
        statusValue.setSelectedItem(dyn.getStatusValue());
      else
        numericValue.setText(String.format("0x%x", dyn.getNumericValue()));
    }
    ok.setEnabled(validateAllInput());
  }

  @Override
  public DynamicCondition getValue() {
    return result;
  }
  
  private void enableChanged(String op) {
    boolean dynamic = enable.equals(enableOptions[0]);
    tree.setEnabled(dynamic);
    operation.setEnabled(dynamic);
    numericValue.setEnabled(dynamic);
    statusValue.setEnabled(dynamic);
    ok.setEnabled(validateAllInput());
  }

  private void operationChanged(String op) {
    CardLayout cards = (CardLayout)valuePanel.getLayout();
    cards.show(valuePanel, op.equals("has") ? "status" : "numeric");
    ok.setEnabled(validateAllInput());
  }

  // private void statusChanged(DynamicCondition.Status s) { }
  private void numericChanged() {
    ok.setEnabled(validateAllInput());
  }

  static final Color ERROR_COLOR = new Color(255, 204, 224);

  private boolean validateNumericInput() {
    String v = numericValue.getText();
    try {
      Attributes.parseInteger(v);
      numericValue.setBackground(Color.WHITE);
      return true;
    } catch (NumberFormatException e) {
      numericValue.setBackground(ERROR_COLOR);
      return false;
    }
  }

  private boolean validateAllInput() {
    if (!enable.getSelectedItem().equals(enableOptions[0]))
      return true;
    if (!validateTreePath(tree.getSelectionPath()))
      return false;
    String op = (String)operation.getSelectedItem();
    return op.equals("has") || validateNumericInput();
  }

  private boolean validateTreePath(TreePath path) {
    if (path == null)
      return false;
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
    if (!node.isLeaf())
      return false;
    ShowStateDialog.Ref r = (ShowStateDialog.Ref)node.getUserObject();
    if (r instanceof ShowStateDialog.CircuitRef)
      return false;
    return true;
  }

  private void apply(boolean ok) {
    if (!ok || root == null || !validateAllInput()) {
      dispose();
      return;
    }

    if (!enable.getSelectedItem().equals(enableOptions[0])) {
      result = DynamicCondition.NONE;
      dispose();
      return;
    }

    DynamicElement.Path path = ShowStateDialog.toComponentPath(tree.getSelectionPath());

    String op = (String)operation.getSelectedItem();
    if (op.equals("has")) {
      result = new DynamicCondition(path, (DynamicCondition.Status)statusValue.getSelectedItem());
    } else {
      try {
        result = new DynamicCondition(path, op, Attributes.parseInteger(numericValue.getText()));
      } catch (NumberFormatException e) {
        // do nothing
      }
    }
    dispose();
  }

  private static DefaultMutableTreeNode enumerate(Circuit circuit, InstanceComponent ic) {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode(new ShowStateDialog.CircuitRef(circuit, ic));
    for (Component c : circuit.getNonWires()) {
      if (c instanceof InstanceComponent) {
        InstanceComponent child = (InstanceComponent)c;
        ComponentFactory f = child.getFactory();
        // fixme: here allow LED, Input, Output, Probe, Register, FlipFlop,
        // and various other single-value components. DynamicValueProvider ?
        if (f instanceof DynamicValueProvider) {
          root.add(new DefaultMutableTreeNode(new ShowStateDialog.Ref(child)));
        } else if (f instanceof SubcircuitFactory) {
          DefaultMutableTreeNode node = enumerate(((SubcircuitFactory)f).getSubcircuit(), child);
          if (node != null)
            root.add(node);
        }
      }
    }
    if (root.getChildCount() == 0)
      return null;
    else
      return root;
  }
}

