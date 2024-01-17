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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;

import com.bfh.logisim.fpga.Chipset;
import com.bfh.logisim.fpga.PinBindings;
import com.bfh.logisim.fpga.PullBehavior;
import com.bfh.logisim.gui.Commander;
import com.bfh.logisim.gui.Console;
import com.bfh.logisim.gui.FPGAReport;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.netlist.Netlist;
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.hdl.Hdl;
import com.cburch.logisim.util.FileUtil;

public class ApioDownload extends FPGADownload {

  public ApioDownload() { super("Apio"); }

  @Override
  public boolean readyForDownload() {
    return new File(sandboxPath + "hardware.bin").exists();
  }

  String bin_apio;
  private ArrayList<String> apio(String ...args) {
    ArrayList<String> command = new ArrayList<>();
    command.add(bin_apio);
    for (String arg: args)
      command.add(arg);
    return command;
  }

  private String getApioVersion(String cmd) {
    try {
      Process process = Runtime.getRuntime().exec(cmd + " --version");
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream()));
      String line = reader.readLine();
      if (line.startsWith("apio, version"))
        return line.substring(6);
    } catch (Exception e) {
    }
    return null;
  }

  public boolean toolchainIsInstalled(Settings settings, FPGAReport err) {
    String helpmsg = " Either install apio to a system directory, or set"
      + " the toolchain path to point to the apio executable or a"
      + " directory (e.g. a python virtualenv) containing bin/apio.";
    String tool = settings.GetApioToolPath();
    if (tool == null) {
      String version = getApioVersion("apio");
      if (version != null) {
        err.AddInfo("Using system installed apio, version " + version);
        return true;
      }
      err.AddFatalError("Apio toolchain path is not configured, and apio"
          + " does not appear to be installed in a system directory. " + helpmsg);
      return false;
    }
    String prog = findApioExecutable(settings);
    if (prog != null)
      return true;
    err.AddFatalError("Apio toolchain path is set to " + tool + " but"
        + " this does not appear to be correct." + helpmsg);
    return false;
  }

  private String findApioExecutable(Settings settings) {
    String p = settings.GetApioToolPath();
    if (p != null) {
      File script = new File(p);
      if (script.exists() && !script.isDirectory() && script.canExecute())
        return p;
      if (script.exists() && script.isDirectory()) {
        String pp = p + "/bin/apio";
        script = new File(pp);
        if (script.exists() && !script.isDirectory() && script.canExecute())
          return pp;
      }
      err.AddFatalError("ApioToolsPath="+p+" is not executable, nor is it a directory"
          + " containing bin/apio. Please adjust FPGA Settings then try again.");
      return null;
    }
    // Try just using "apio", hope it is found on system path?
    return "apio";
  }

  @Override
  public boolean generateScripts(PinBindings ioResources, ArrayList<String> hdlFiles) {

    bin_apio = findApioExecutable(settings);
    if (bin_apio == null)
      return false;

    String board_name = board.name;
    if (board.apio_name != null && !board.apio_name.equals(""))
      board_name = board.apio_name;

    if (board.fpga.UnusedPinsBehavior != PullBehavior.UNKNOWN &&
        board.fpga.UnusedPinsBehavior != PullBehavior.PULL_UP) {
      err.AddSevereWarning("Design specifies " + board.fpga.UnusedPinsBehavior +
          " for unused pins, but apio toolchain maybe only supports pull-up.");
      err.AddSevereWarning("Unused pins will maybe be pulled high.");
    }

    File f;

    // Generate apio.ini
    Hdl out = new Hdl(lang, err);
    out.stmt("[env]");
    out.stmt("board = " + board_name);
    f = FileWriter.GetFilePointer(sandboxPath, "apio.ini", err);
    if (f == null || !FileWriter.WriteContents(f, out, err))
      return false;

    if (out.isVhdl) {
      err.AddSevereWarning("VHDL was chosen, but apio toolchain maybe only supports Verilog.");
      err.AddSevereWarning("Design will probably fail to compile.");
    }

    // Generate fpga.pcf
    Hdl out2 = new Hdl(lang, err);
    out2.stmt();
    ioResources.forEachPhysicalPin((pin, net, io, label) -> {
      out2.stmt("set_io --warn-no-port %s %s", net, pin);
      // todo: input pullups using SB_IO
      // todo: bidirectional using SB_IO
      // if (io.pull != PullBehavior.UNKNOWN)
      //   err.AddSevereWarning("FPGA pin %s pull behavior specified as %s, but apio does not support pull VHDL was chosen, but apio toolchain maybe only supports Verilog.");
    });
    f = FileWriter.GetFilePointer(sandboxPath, "fpga.pcf", err);
    if (f == null || !FileWriter.WriteContents(f, out2, err))
      return false;

    // Copy all HDL files to sandbox, renaming to avoid conflicts.
    HashSet<String> names = new HashSet<>();
    names.add("LogisimToplevelApioShell.v"); 
    for (String src : hdlFiles) {
      String relPath = Paths.get(circuitPath).relativize(Paths.get(src)).toString();
      String name = relPath.replaceAll("[^a-zA-Z0-9.]", "_").replaceAll("_+", "_").replaceAll("^_+", "");
      String prefix = "";
      int i = 0;
      while (names.contains(prefix+name)) {
        i++;
        prefix = "dup"+i+"_";
      }
      name = prefix + name;
      names.add(name);
      String dst = sandboxPath + name;
      try { FileUtil.copyFile(dst, src); }
      catch (IOException e) {
        err.AddFatalError(e.getMessage());
        return false;
      }
    }

    // Create LogisimToplevelApioShell.v
    // todo: SB_IO for pullups, tristates, etc.
    Hdl out3 = new Hdl(lang, err);
    Netlist.Int3 ioPinCount = ioResources.countFPGAPhysicalIOPins();
    int n = ioPinCount.size();
    out3.stmt("module LogisimToplevelApioShell(%s", (n == 0 ? " );" : ""));
		for (int i = 0; i < ioPinCount.in; i++)
      out3.stmt("              FPGA_INPUT_PIN_%d%s", i, (--n == 0 ? " );" : ","));
		for (int i = 0; i < ioPinCount.inout; i++)
      out3.stmt("              FPGA_INOUT_PIN_%d%s", i, (--n == 0 ? " );" : ","));
		for (int i = 0; i < ioPinCount.out; i++)
      out3.stmt("              FPGA_OUTPUT_PIN_%d%s", i, (--n == 0 ? " );" : ","));
		for (int i = 0; i < ioPinCount.in; i++)
      out3.stmt("  input FPGA_INPUT_PIN_%d;", i);
		for (int i = 0; i < ioPinCount.inout; i++)
      out3.stmt("  inout FPGA_INOUT_PIN_%d;", i);
		for (int i = 0; i < ioPinCount.out; i++)
      out3.stmt("  output FPGA_OUTPUT_PIN_%d;", i);
    out3.stmt();

    n = ioPinCount.size();
    if (ioResources.requiresOscillator) {
      out3.stmt("  wire FPGA_CLK;");
      out3.stmt("  SB_HFOSC inthosc(.CLKHFPU(1'b1), .CLKHFEN(1'b1), .CLKHF(FPGA_CLK));");
      out3.stmt();
      n++;
    }

    out3.stmt("  LogisimToplevelShell wrappedShell( %s", (n == 0 ? " );" : ""));
    if (ioResources.requiresOscillator)
      out3.stmt("              .FPGA_CLK(FPGA_CLK)%s", (--n == 0 ? " );" : ","));
		for (int i = 0; i < ioPinCount.in; i++)
      out3.stmt("              .FPGA_INPUT_PIN_%d(FPGA_INPUT_PIN_%d)%s", i, i, (--n == 0 ? " );" : ","));
		for (int i = 0; i < ioPinCount.inout; i++)
      out3.stmt("              .FPGA_INOUT_PIN_%d(FPGA_INOUT_PIN_%d)%s", i, i, (--n == 0 ? " );" : ","));
		for (int i = 0; i < ioPinCount.out; i++)
      out3.stmt("              .FPGA_OUTPUT_PIN_%d(FPGA_OUTPUT_PIN_%d)%s", i, i, (--n == 0 ? " );" : ","));
    out3.stmt();

    out3.stmt("endmodule");
    f = FileWriter.GetFilePointer(sandboxPath, "LogisimToplevelApioShell.v", err);
    if (f == null || !FileWriter.WriteContents(f, out3, err))
      return false;

    return true;
  }

  @Override
  public ArrayList<Stage> initiateDownload(Commander cmdr) {
    ArrayList<Stage> stages = new ArrayList<>();

    // synthesize
    if (!readyForDownload()) {
      stages.add(new Stage(
            "synthesis", "Synthesizing (may take a while)",
            apio("build","--top-module", "LogisimToplevelApioShell"),
            "Failed to synthesize design, cannot download"));
    }

    // upload
    stages.add(new Stage(
          "upload", "Uploading to FPGA", 
           apio("upload"),
          "Failed to upload design; did you connect the board?"));

    return stages;
  }

}
