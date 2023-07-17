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

package com.cburch.logisim.std.audio;
import static com.cburch.logisim.std.Strings.S;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.midi.*;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;

public class MidiOut extends InstanceFactory {

  private static final BitWidth BIT_WIDTH1 = BitWidth.create(1);
  private static final BitWidth BIT_WIDTH4 = BitWidth.create(4);
  private static final BitWidth BIT_WIDTH7 = BitWidth.create(7);
  private static final BitWidth BIT_WIDTH8 = BitWidth.create(8);

  static AttributeOption instlist[];
  static Attribute<AttributeOption> ATTR_INSTR;
  static Attribute<Integer> ATTR_CHAN;
  static Attribute<Integer> ATTR_HOLD;

  static final int NOTE = 0;
  static final int VELO = 1;
  static final int DAMP = 2;
  static final int CHAN = 3;
  static final int INST = 4;
  static final int WE = 5;
  static final int CK = 6;
  static final int NUM_PORTS = 7;


  public MidiOut() {
    super("MidiOut", S.getter("audioMidiOutComponent"));
    init();
    setAttributes(new Attribute[] { StdAttr.EDGE_TRIGGER, ATTR_INSTR, ATTR_CHAN, ATTR_HOLD },
        new Object[] { StdAttr.TRIG_RISING, instlist[0], 0, 250 });
    setOffsetBounds(Bounds.create(-30, -30, 30, 60));
    Port[] ps = new Port[7];
    ps[NOTE] = new Port(-30, 0, Port.INPUT, BIT_WIDTH8);
    ps[NOTE].setToolTip(S.getter("midiNote"));
    ps[VELO] = new Port(-30, 10, Port.INPUT, BIT_WIDTH7);
    ps[VELO].setToolTip(S.getter("midiVelocity"));
    ps[DAMP] = new Port(-30, -10, Port.INPUT, 1);
    ps[DAMP].setToolTip(S.getter("midiDamping"));
    ps[CHAN] = new Port(-30, -20, Port.INPUT, BIT_WIDTH4);
    ps[CHAN].setToolTip(S.getter("midiChannel"));
    ps[INST] = new Port(-30, 20, Port.INPUT, BIT_WIDTH8);
    ps[INST].setToolTip(S.getter("midiInstrument"));
    ps[WE] = new Port(-20, 30, Port.INPUT, BIT_WIDTH1);
    ps[WE].setToolTip(S.getter("midiEnable"));
    ps[CK] = new Port(-10, 30, Port.INPUT, BIT_WIDTH1);
    ps[CK].setToolTip(S.getter("midiClock"));
    setPorts(ps);
  }

  @Override
  public void propagate(InstanceState circState) {

    Object trigger = circState.getAttributeValue(StdAttr.EDGE_TRIGGER);
    State data = getState(circState);

    Value enable = circState.getPortValue(WE);
    Value clock = circState.getPortValue(CK);
    Value lastClock = data.setLastClock(clock);
    if (enable == Value.FALSE)
      return;
    boolean go;
    if (trigger == StdAttr.TRIG_FALLING) {
      go = lastClock == Value.TRUE && clock == Value.FALSE;
    } else {
      go = lastClock == Value.FALSE && clock == Value.TRUE;
    }
    if (!go)
      return;
    
    int hold = circState.getAttributeValue(ATTR_HOLD);

    int note = circState.getPortValue(NOTE).toIntValue();

    int velo = circState.getPortValue(VELO).toIntValue();
    if (velo < 0)
      velo = 127;

    int damp = circState.getPortValue(DAMP).toIntValue();
    if (damp < 0)
      damp = 1;

    int chan = circState.getPortValue(CHAN).toIntValue();
    if (chan < 0)
      chan = circState.getAttributeValue(ATTR_CHAN);
    if (chan < 0) chan = 0;
    if (chan >= mChannels.length) chan = mChannels.length - 1;

    int inst = circState.getPortValue(INST).toIntValue();
    if (inst < 0) {
      // s = "###: name"
      String s = circState.getAttributeValue(ATTR_INSTR).toString();
      inst = Integer.parseInt(s.substring(0, s.indexOf(':')));
    }
    if (inst < 0) inst = 0;
    if (inst >= instr.length) inst = instr.length - 1;

    play(note, velo, damp, chan, inst, hold);
  }

  public void paintInstance(InstancePainter painter) {
    painter.drawBounds();
    for (int i = 0; i < NUM_PORTS - 1; i++)
      painter.drawPort(i);
    painter.drawClock(CK, Direction.NORTH);

    Bounds bds = painter.getBounds();
    Graphics g = painter.getGraphics();

    int x = bds.x + (bds.width-24)/2;
    int y = bds.y + (bds.height-24)/2;
    g.setColor(Color.BLACK);
    g.drawRect(x+1, y+7, 6, 10);
    int[] bx = new int[] { x+7, x+13, x+14, x+14, x+13, x+7 };
    int[] by = new int[] { y+7, y+1, y+1, y+23, y+23, y+17 };
    if (midiAvailable)
      g.setColor(Color.BLUE);
    else
      g.setColor(Color.RED);
    g.drawPolyline(bx, by, 6);
    g.drawArc(x+14, y+1, 9, 22, -60, 120);
    g.drawArc(x+10, y+5, 9, 12, -60, 120);
  }

  boolean midiAvailable;
  Synthesizer midiSynth;
  Instrument[] instr;
  MidiChannel[] mChannels;
  int[] mInstruments;

  public void init()
  {
    try {
      midiSynth = MidiSystem.getSynthesizer(); 
      midiSynth.open();
      // get and load default instrument and channel lists
      instr = midiSynth.getDefaultSoundbank().getInstruments();
      instlist = new AttributeOption[instr.length];
      for (int i = 0 ; i < instr.length ; i++) {
        String name = i + ": " + instr[i].getName();
        instlist[i] = new AttributeOption(name, name, S.unlocalized(name));
      }
      ATTR_INSTR = Attributes.forOption("instrument", S.getter("midiInstrument"), instlist);
      mChannels = midiSynth.getChannels();
      ATTR_CHAN = Attributes.forIntegerRange("channel", S.getter("midiChannel"), 0, mChannels.length-1);
      ATTR_HOLD = Attributes.forIntegerRange("hold", S.getter("midiHold"), 0, 1000);
      mChannels[0].allNotesOff();
      mInstruments = new int[mChannels.length];
      for (int i = 0; i < mInstruments.length; i++)
        mInstruments[i] = -1;
      midiAvailable = true;
    } catch (MidiUnavailableException e) {
      midiAvailable = false;
    }
  }

  public void play(int note, int velocity, int damper, int chan, int inst, int hold) { 
    int volume = 127;

    if (damper == 1)
      mChannels[chan].allNotesOff();

    if (mInstruments[chan] != inst) {
      // switch instruments
      midiSynth.loadInstrument(instr[inst]);
      mChannels[chan].programChange(instr[inst].getPatch().getProgram());
      mChannels[chan].controlChange(7, volume);
      mInstruments[chan] = inst;
    }

    if (0 < note && note <= 128)
      mChannels[chan].noteOn(note, velocity);
    else
      mChannels[chan].noteOff(note, velocity);

    if (hold > 0) {
      new Timer().schedule(
          new TimerTask() {
            public void run() { mChannels[chan].noteOff(note, velocity); }
          }, hold );
    }
  }

  private State getState(InstanceState state) {
    State ret = (State) state.getData();
    if (ret == null) {
      ret = new State();
      state.setData(ret);
    } else {
      // ret.update(...)
    }
    return ret;
  }

  static class State implements InstanceData, Cloneable {
    private Value lastClock;

    public State() {
      lastClock = Value.UNKNOWN;
    }

    @Override
    public State clone() {
      try {
        State ret = (State) super.clone();
        return ret;
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    public Value setLastClock(Value newClock) {
      Value ret = lastClock;
      lastClock = newClock;
      return ret;
    }

  }

}
