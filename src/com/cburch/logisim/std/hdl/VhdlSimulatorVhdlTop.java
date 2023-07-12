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
package com.cburch.logisim.std.hdl;

// This code is disabled as it is not maintained and does not currently work.
public class VhdlSimulatorVhdlTop { }

// import java.io.FileNotFoundException;
// import java.io.IOException;
// import java.io.PrintWriter;
// import java.io.UnsupportedEncodingException;
// import java.util.Date;
// 
// import com.cburch.logisim.comp.Component;
// import com.cburch.logisim.instance.InstanceState;
// import com.cburch.logisim.util.FileUtil;
// import com.cburch.logisim.util.LocaleManager;
// 
// /**
//  * Generates a simulation top file. This file contains all the interfaces to the
//  * entities (in and out pins) so the simulation is run on a single top
//  * component. It allows us to have only one instance of Questasim running.
//  *
//  * @author christian.mueller@heig-vd.ch
//  */
// public class VhdlSimulatorVhdlTop {
// 
//   private boolean valid = false;
//   private VhdlSimulator vhdlSimulator;
// 
//   VhdlSimulatorVhdlTop(VhdlSimulator vs) {
//     vhdlSimulator = vs;
//   }
// 
//   public void fireInvalidated() {
//     valid = false;
//   }
// 
// 
//   public void generate() {
// 
//     /* Do not generate if file is already valid */
//     if (valid)
//       return;
// 
//     StringBuilder ports = new StringBuilder();
//     ports.append("Autogenerated by logisim --");
//     ports.append(System.getProperty("line.separator"));
// 
//     StringBuilder components = new StringBuilder();
//     components.append("Autogenerated by logisim --");
//     components.append(System.getProperty("line.separator"));
// 
//     StringBuilder map = new StringBuilder();
//     map.append("Autogenerated by logisim --");
//     map.append(System.getProperty("line.separator"));
// 
//     Boolean firstPort = true, firstComp = true, firstMap = true;
// 
//     /* For each vhdl entity */
//     for (Component comp : VhdlSimulator.getVhdlComponents(vhdlSimulator
//           .getProject().getCircuitState())) {
//       if (comp.getFactory().getClass().equals(VhdlEntity.class)) {
// 
//         InstanceState state = vhdlSimulator.getProject()
//             .getCircuitState().getInstanceState(comp);
// 
//         VhdlContent content = ((VhdlEntityAttributes)state.getAttributeSet()).getContent();
// 
//         VhdlEntity vhdlEntity = (VhdlEntity)comp.getFactory();
//         String vhdlEntityName = vhdlEntity.getHDLNameForInstanceSimulation(state.getInstance().getAttributeSet());
// 
//         /*
//          * Create ports
//          */
//         for (VhdlParser.PortDescription port : content.getPorts()) {
// 
//           if (!firstPort) {
//             ports.append(";");
//             ports.append(System.getProperty("line.separator"));
//           } else {
//             firstPort = false;
//           }
// 
//           String portName = vhdlEntityName + "_" + port.getName();
//           ports.append("    " + portName + " : " + port.getVhdlType()
//               + " std_logic");
// 
//           int width = port.getWidth().getWidth();
//           if (width > 1) {
//             ports.append("_vector(" + (width - 1) + " downto 0)");
//           }
//         }
// 
//         /*
//          * Create components
//          */
//         components.append("  component " + vhdlEntityName);
//         components.append(System.getProperty("line.separator"));
// 
//         components.append("    port (");
//         components.append(System.getProperty("line.separator"));
// 
//         firstComp = true;
//         for (VhdlParser.PortDescription port : content.getPorts()) {
//           if (!firstComp) {
//             components.append(";");
//             components.append(System.getProperty("line.separator"));
//           } else
//             firstComp = false;
// 
//           components.append("      " + port.getName() + " : "
//               + port.getVhdlType() + " std_logic");
// 
//           int width = port.getWidth().getWidth();
//           if (width > 1) {
//             components.append("_vector(" + (width - 1)
//                 + " downto 0)");
//           }
//         }
// 
//         components.append(System.getProperty("line.separator"));
//         components.append("    );");
//         components.append(System.getProperty("line.separator"));
// 
//         components.append("  end component ;");
//         components.append(System.getProperty("line.separator"));
// 
//         components.append("  ");
//         components.append(System.getProperty("line.separator"));
// 
//         /*
//          * Create port map
//          */
//         map.append("  " + vhdlEntityName + "_map : " + vhdlEntityName + " port map (");
//         map.append(System.getProperty("line.separator"));
// 
//         firstMap = true;
//         for (VhdlParser.PortDescription port : content.getPorts()) {
// 
//           if (!firstMap) {
//             map.append(",");
//             map.append(System.getProperty("line.separator"));
//           } else
//             firstMap = false;
// 
//           map.append("    " + port.getName() + " => " + vhdlEntityName + "_" + port.getName());
//         }
//         map.append(System.getProperty("line.separator"));
//         map.append("  );");
//         map.append(System.getProperty("line.separator"));
//         map.append("  ");
//         map.append(System.getProperty("line.separator"));
// 
//       }
// 
//     }
// 
//     ports.append(System.getProperty("line.separator"));
//     ports.append("    ---------------------------");
//     ports.append(System.getProperty("line.separator"));
// 
//     components.append("  ---------------------------");
//     components.append(System.getProperty("line.separator"));
// 
//     map.append("  ---------------------------");
//     map.append(System.getProperty("line.separator"));
// 
//     /*
//      * Replace template blocks by generated datas
//      */
//     String template;
//     try {
//       template = new String(
//           FileUtil.getBytes(this.getClass()
//             .getResourceAsStream(
//               VhdlSimulator.VHDL_TEMPLATES_PATH
//               + "top_sim.templ")));
//     } catch (IOException e) {
//       System.err.printf("Could not read template : %s\n", e.getMessage());
//       return;
//     }
// 
//     template = template.replaceAll("%date%",
//         LocaleManager.parserSDF.format(new Date()));
//     template = template.replaceAll("%ports%", ports.toString());
//     template = template.replaceAll("%components%", components.toString());
//     template = template.replaceAll("%map%", map.toString());
// 
//     PrintWriter writer;
//     try {
//       writer = new PrintWriter(VhdlSimulator.SIM_SRC_PATH
//           + VhdlSimulator.SIM_TOP_FILENAME, "UTF-8");
//       writer.print(template);
//       writer.close();
//     } catch (FileNotFoundException e) {
//       System.err.printf("Could not create top_sim file : %s\n", e.getMessage());
//       e.printStackTrace();
//       return;
//     } catch (UnsupportedEncodingException e) {
//       System.err.printf("Could not create top_sim file : %s\n", e.getMessage());
//       e.printStackTrace();
//       return;
//     }
// 
//     valid = true;
//   }
// 
// }
