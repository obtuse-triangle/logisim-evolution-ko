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
import java.util.ArrayList;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;

public class MidiIn extends InstanceFactory {

  static final AttributeOption IFACE_SERIAL = MidiSink.IFACE_SERIAL;
  static final AttributeOption IFACE_PARALLEL3 = MidiSink.IFACE_PARALLEL3;
  static final AttributeOption IFACE_PARALLEL4 = MidiSink.IFACE_PARALLEL4;
  static final AttributeOption IFACE_LOGISIM1 = MidiSink.IFACE_LOGISIM1;
  static final AttributeOption IFACE_LOGISIM5 = MidiSink.IFACE_LOGISIM5;
  static final Attribute<AttributeOption> ATTR_IFACE = MidiSink.ATTR_IFACE;
  
  static final int CK = 0; // all interfaces
  static final int RE = 1; // all interfaces
  static final int RDY = 2; // all interfaces
  static final int OUT = 3; // serial, parallel3, parallel4, logisim1
  static final int NUM_PORTS_OTHER = 4;
  static final int NOTE = 3; // logisim5
  static final int VELO = 4; // logisim5
  static final int DAMP = 5; // logisim5
  static final int CHAN = 6; // logisim5
  static final int INST = 7; // logisim5
  static final int NUM_PORTS_LOGISIM5 = 8;

  public MidiIn() {
    super("MidiIn", S.getter("audioMidiInComponent"));
    setIconName("midiin.gif");
    setOffsetBounds(Bounds.create(-30, -30, 30, 60));
    setPorts(makePorts(IFACE_SERIAL));
  }

  @Override
  public AttributeSet createAttributeSet() {
    // We defer init to here, so that Midi is only initialized when being used
    if (init()) {
      setAttributes(new Attribute[] { StdAttr.EDGE_TRIGGER, ATTR_IFACE},
          new Object[] { StdAttr.TRIG_RISING, IFACE_SERIAL });
    }
    return super.createAttributeSet();
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.setPorts(makePorts(instance.getAttributeValue(ATTR_IFACE)));
    instance.addAttributeListener();
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_IFACE) {
      instance.setPorts(makePorts(instance.getAttributeValue(ATTR_IFACE)));
    }
  }

  private static Port[] makePorts(AttributeOption iface) {
    Port[] ps;
    if (iface == IFACE_LOGISIM5) {
      ps = new Port[8];
      ps[CK] = new Port(-10, 30, Port.INPUT, BitWidth.ONE);
      ps[CK].setToolTip(S.getter("midiClock"));
      ps[RE] = new Port(-20, 30, Port.INPUT, BitWidth.ONE);
      ps[RE].setToolTip(S.getter("midiReadEnable"));
      ps[RDY] = new Port(-10, -30, Port.OUTPUT, BitWidth.ONE);
      ps[RDY].setToolTip(S.getter("midiOutputReady"));
      ps[NOTE] = new Port(0, 0, Port.OUTPUT, BitWidth.EIGHT);
      ps[NOTE].setToolTip(S.getter("midiNote"));
      ps[VELO] = new Port(0, 10, Port.OUTPUT, BitWidth.SEVEN);
      ps[VELO].setToolTip(S.getter("midiVelocity"));
      ps[DAMP] = new Port(0, -10, Port.OUTPUT, BitWidth.ONE);
      ps[DAMP].setToolTip(S.getter("midiDamping"));
      ps[CHAN] = new Port(0, -20, Port.OUTPUT, BitWidth.FOUR);
      ps[CHAN].setToolTip(S.getter("midiChannel"));
      ps[INST] = new Port(0, 20, Port.OUTPUT, BitWidth.EIGHT);
      ps[INST].setToolTip(S.getter("midiInstrument"));
    } else { // IFACE_SERIAL, IFACE_PARALLEL3, IFACE_PARALLEL4, IFACE_LOGISIM1
      ps = new Port[4];
      ps[CK] = new Port(-10, 30, Port.INPUT, BitWidth.ONE);
      ps[CK].setToolTip(S.getter("midiClock"));
      ps[RE] = new Port(-20, 30, Port.INPUT, BitWidth.ONE);
      ps[RE].setToolTip(S.getter("midiEnable"));
      ps[RDY] = new Port(-10, -30, Port.OUTPUT, BitWidth.ONE);
      ps[RDY].setToolTip(S.getter("midiOutputReady"));
      if (iface == IFACE_SERIAL) {
        ps[OUT] = new Port(0, 0, Port.OUTPUT, BitWidth.of(8));
        ps[OUT].setToolTip(S.getter("midiOutputSerial"));
      } else if (iface == IFACE_PARALLEL3) {
        ps[OUT] = new Port(-30, 0, Port.OUTPUT, BitWidth.of(24));
        ps[OUT].setToolTip(S.getter("midiOutputParallel3"));
      } else if (iface == IFACE_PARALLEL4) {
        ps[OUT] = new Port(-30, 0, Port.OUTPUT, BitWidth.of(32));
        ps[OUT].setToolTip(S.getter("midiOutputParallel4"));
      } else { /* IFACE_LOGISIM1 */
        ps[OUT] = new Port(-30, 0, Port.OUTPUT, BitWidth.of(32));
        ps[OUT].setToolTip(S.getter("midiOutputLogisim1"));
      }
    }
    return ps;
  }

  @Override
  public void propagate(InstanceState state) {
    if (midiFailed)
      return;

    Object trigger = state.getAttributeValue(StdAttr.EDGE_TRIGGER);
    State data = getState(state);

    Value enable = state.getPortValue(RE);
    Value clock = state.getPortValue(CK);
    Value lastClock = data.setLastClock(clock);

    boolean advance;
    if (trigger == StdAttr.TRIG_FALLING) {
      advance = enable == Value.TRUE && lastClock == Value.TRUE && clock == Value.FALSE;
    } else {
      advance = enable == Value.TRUE && lastClock == Value.FALSE && clock == Value.TRUE;
    }
    
    AttributeOption iface = state.getAttributeValue(ATTR_IFACE);

    // check if new data is available, either 1 byte (serial) or 1-3 bytes (others)
    if (iface == IFACE_SERIAL) {
      boolean avail = false;
      int b0 = -1;
      synchronized (midiLock) {
        if (queue.n > 0) {
          b0 = advance ? queue.remove() : queue.peek(0);
          avail = true;
        }
      }
      state.setPort(RDY, avail ? Value.TRUE : Value.FALSE, 1);
      if (avail)
        state.setPort(OUT, Value.createKnown(BitWidth.of(8), b0), 1);
    } else {
      // todo
    }

  }

  @Override
  public void paintInstance(InstancePainter painter) {
    painter.drawBounds();
    int numPorts;
    if (painter.getAttributeValue(ATTR_IFACE) == IFACE_LOGISIM5)
      numPorts = NUM_PORTS_LOGISIM5;
    else
      numPorts = NUM_PORTS_OTHER;

    painter.drawClock(CK, Direction.NORTH); // port number 0
    for (int i = 1; i < numPorts; i++)
      painter.drawPort(i);

    Bounds bds = painter.getBounds();
    Graphics g = painter.getGraphics();

    int x = bds.x + (bds.width-24)/2;
    int y = bds.y + (bds.height-24)/2 - 10;
    g.setColor(Color.BLACK);
    g.drawOval(x, y, 24, 24);
    g.drawOval(x+2, y+11, 4, 4);
    g.drawOval(x+19, y+11, 4, 4);
    g.drawOval(x+4, y+5, 4, 4);
    g.drawOval(x+17, y+5, 4, 4);
    g.drawOval(x+12, y+2, 4, 4);
    g.drawArc(x+9, y+21, 6, 7, 10, 160);

    if (!midiFailed)
      g.setColor(Color.BLUE);
    else
      g.setColor(Color.RED);
    g.drawLine(x+12, y+27, x+12, y+36);
    g.drawLine(x+12, y+36, x+24, y+36);
    g.drawLine(x+24, y+36, x+19, y+31);
    g.drawLine(x+24, y+36, x+19, y+41);
  }

  // Global state, since the underlying midi system is global anyway
  static Object midiLock = new Object();
  static boolean midiFailed = false;
  
  // static Synthesizer midiSynth = null;
  // static Releaser midiReleaser;
  // static Instrument[] midiInstrument;
  // static MidiChannel[] midiChannel;
  // static int numChannels;
  // static int[] midiChannelInstrument;

  private static boolean init() {
    synchronized(midiLock) {
      if (midiFailed || queue != null)
        return false;
      try {
        MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
        if (info == null || info.length == 0)
          throw new MidiUnavailableException("No midi devices available");

        System.out.printf("%d MIDI devices found\n", info.length);
        for (int i = info.length - 1; i >= 0; i--) {
          System.out.printf("Querying MIDI device %d: %s\n", i, info[i]);
          MidiDevice dev = MidiSystem.getMidiDevice(info[i]);
          if (dev.getMaxTransmitters() == 0)
            continue;

          try {
            dev.open();
          } catch (Exception e) {
            System.out.printf(" * device not available: %s\n", e.getMessage());
            continue;
          }
          openedDevices.add(dev);
          System.out.printf(" * opened device\n");

          if (queue == null)
            queue = new Queue();
          try {
            Transmitter t = dev.getTransmitter();
            t.setReceiver(new Rx(dev, t));
          } catch (Exception e) {
            try {
              dev.close();
              openedDevices.remove(dev);
            } catch (Exception e2) { }
            System.out.printf(" * device failed: %s\n", e.getMessage());
            continue;
          }
        }
       
        if (openedDevices.isEmpty()) {
          queue = null;
          midiFailed = true;
          System.out.println("No MIDI input devices available");
        } else {
          midiFailed = false;
          System.out.println("MIDI input from " + openedDevices.size() + " devices");
        }
      } catch (MidiUnavailableException e) {
        midiFailed = true;
      }
      return true;
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

  // A single, global queue for all MidiIn components, all devices, etc.
  static Queue queue;
  static ArrayList<MidiDevice> openedDevices = new ArrayList<>();

  static class Queue {
    byte[] queue = new byte[1000];
    int s = 0, e = 0, n = 0;

    Queue() { }

    void add(byte[] b) {
      if (n+b.length > queue.length)
        return;
      for (int i = 0; i < b.length; i++) {
        queue[e] = b[i];
        e = (e + 1) % queue.length;
        n++;
      }
    }

    int remove() {
      if (n == 0) return -1; // should never happen
      int b = queue[s];
      s = (s + 1) % queue.length;
      n--;
      return b;
    }

    int peek(int i) {
      if (i >= n) return -1; // should never happen
      return queue[(s + i) % queue.length];
    }
  }

  static class Rx implements Receiver {
    MidiDevice dev;
    Transmitter tx;

    Rx(MidiDevice dev, Transmitter tx) {
      this.dev = dev;
      this.tx = tx;
    }

    public void close() {
      synchronized(midiLock) {
        try { dev.close(); }
        catch (Exception e) { }
        openedDevices.remove(dev);
      }
    }

    public void send(MidiMessage message, long timeStamp) {
      byte[] msg = message.getMessage();
      String s = String.format("%d: (%d bytes)", timeStamp, msg.length);
      for (int i = 0; i < msg.length; i++)
        s += String.format(" %02x", msg[i]);
      System.out.println(s);
      synchronized(midiLock) {
        queue.add(msg);
      }
    }
  }

  static class State implements InstanceData, Cloneable {
    private Value lastClock = Value.UNKNOWN;
    // private int curData = -1;

    public State() { }

    @Override
    public State clone() {
      try { return (State) super.clone(); }
      catch (CloneNotSupportedException e) { return null; }
    }

    public Value setLastClock(Value newClock) {
      Value ret = lastClock;
      lastClock = newClock;
      return ret;
    }

  }


}
