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
import java.awt.Graphics;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.main.ExportImage;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.Errors;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.JInputDialog;

public class Slideshow extends InstanceFactory {

  private static class State implements InstanceData, Cloneable {
    int w, h, n;
    Slide[] slides; // always large enough for MAX_SLIDES
    int cur;

    State(int w, int h, int n) {
      this.w = w;
      this.h = h;
      this.n = n;
      this.slides = new Slide[MAX_SLIDES];
      this.cur = -1;
    }

    void updateSize(int w, int h, int n) {
      this.w = w;
      this.h = h;
      this.n = n;
      if (this.cur >= n)
        this.cur = -1;
    }

    void updateSlide(int i, Slide s) {
      if (i < 0 | i >= n)
        return;
      slides[i] = s;
    }

    void deselect() { 
      cur = -1;
    }

    void selectSlide(int i) {
      cur = i % n;
    }

    BufferedImage getImage() {
      if (cur < 0 || slides[cur] == null) {
        return null;
      }
      return slides[cur].getImage();
    }

    @Override
    public Object clone() {
      try {
        State other = (State) super.clone();
        other.slides = new Slide[MAX_SLIDES];
        for (int i = 0; i < n; i++)
          other.slides[i] = new Slide(this.slides[i]);
        return other;
      } catch (CloneNotSupportedException e) {
        e.printStackTrace();
        return null;
      }
    }
  }

  public static class Slide {
    String format; // "PNG" or "JPG"
    Attributes.LinkedFile source;
    byte[] imgData;
    BufferedImage img;
    long timestamp;

    Slide(Attributes.LinkedFile src) {
      this.source = src;
      if (src.relative.toString().toLowerCase().endsWith(".png"))
        format = "PNG";
      else
        format = "JPG";
    }

    Slide(Slide other) {
      this.format = other.format;
      this.source = other.source;
      this.imgData = other.imgData;
      this.img = other.img;
      this.timestamp = other.timestamp;
    }

    Slide(File f) throws IOException {
      imgData = Files.readAllBytes(f.toPath());
      ByteArrayInputStream stream = new ByteArrayInputStream(imgData);
      img = ImageIO.read(stream);
      stream.close();
      if (f.toString().toLowerCase().endsWith(".png"))
        format = "PNG";
      else
        format = "JPG";
    }

    Slide(String format, BufferedImage img, byte[] imgData) {
      this.format = format;
      this.img = img;
      this.imgData = imgData;
    }

    BufferedImage getImage() {
      if (source == null)
        return img;
      long ts = source.absolute.lastModified();
      if (ts == timestamp)
        return img;
      timestamp = ts;
      try {
        img = ImageIO.read(source.absolute);
      } catch (IOException e) {
        img = null;
        // maybe don't warn?
        System.out.println("image file access error: " + source.relative);
      }
      return img;
    }

  }

  static final AttributeOption SCALE = new AttributeOption("scale",
      S.getter("ioSlideshowScaleOption"));
  static final AttributeOption SCALE_CROP = new AttributeOption("scale-crop",
      S.getter("ioSlideshowScaleCropOption"));
  static final AttributeOption STRETCH = new AttributeOption("stretch",
      S.getter("ioSlideshowStretchOption"));
  static final AttributeOption CROP = new AttributeOption("crop",
      S.getter("ioSlideshowCropOption"));

  static final Attribute<AttributeOption> ATTR_FIT = 
      Attributes.forOption("fit", S.getter("ioSlideshowFit"),
          new AttributeOption[] { SCALE, SCALE_CROP, STRETCH, CROP });

  private static final Attribute<Attributes.LinkedFile> ATTR_FILENAME_SINGLETON =
      Attributes.forFilename("filename", 
          S.getter("ioSlideshowFilename"), S.getter("ioSlideshowLoadDialogTitle"),
          ExportImage.PNG_FILTER, ExportImage.JPG_FILTER);

  private static class SlideAttribute extends Attribute<Slide> {
    int idx;
    public SlideAttribute(int i) {
      super("image"+i, S.getter("ioSlideshowImage", ""+i));
      idx = i;
    }

    @Override
    public java.awt.Component getCellEditor(Window source, Slide s) {
      return new FileChooser((Frame)source, s);
    }

    @Override
    public String toDisplayString(Slide value) {
      if (value == null)
        return S.get("ioSlideshowClickToLoad");
      else if (value.source != null)
        return value.source.relative + " [" + value.source.absolute + "]";
      else
        return String.format(S.get("ioSlideshowImageInfo"), value.format, value.imgData.length);
    }

    @Override
    public String toStandardString(Slide value) {
      return toStandardStringRelative(value, null);
    }
    
    @Override
    public String toStandardStringRelative(Slide value, String outFilename) {
      if (value == null) {
        return "";
      } else if (value.source != null) {
        return "file:" + ATTR_FILENAME_SINGLETON.toStandardStringRelative(value.source, outFilename);
      } else {
        try {
          ByteArrayOutputStream result = new ByteArrayOutputStream();
          result.write((value.format+":\n").getBytes("UTF-8"));
          byte[] LF = System.lineSeparator().getBytes("UTF-8"); // "\n" or "\r\n"
          OutputStream encoded = Base64.getMimeEncoder(76, LF).wrap(result);
          encoded.write(value.imgData, 0, value.imgData.length);
          try { encoded.flush(); } catch (IOException e) { e.printStackTrace(); }
          try { result.flush(); } catch (IOException e) { e.printStackTrace(); }
          try { encoded.close(); } catch (IOException e) { e.printStackTrace(); }
          try { result.close(); } catch (IOException e) { e.printStackTrace(); }
          return new String(result.toByteArray(), "UTF-8");
        } catch (Exception e) {
          e.printStackTrace();
          return "";
        }
      }
    }

    @Override
    public Slide parse(String str) {
      throw new UnsupportedOperationException("parse filename without source");
    }

    @Override
    public Slide parseFromUser(Window source, String value) {
      throw new UnsupportedOperationException("parse filename without source");
    }

    @Override
    public Slide parseFromFilesystem(File directory, String value) {
      if (value == null || value.equals(""))
        return null;
      if (value.startsWith("file:")) {
        return new Slide(ATTR_FILENAME_SINGLETON.parseFromFilesystem(directory, value.substring(5)));
      } else if (value.startsWith("PNG:\n") || value.startsWith("JPG:\n")) {
        String format = value.substring(0, 3);
        try {
          byte[] bytes = value.getBytes("UTF-8");
          ByteArrayInputStream input = new ByteArrayInputStream(bytes, 5, bytes.length-5);
          InputStream decoded = Base64.getMimeDecoder().wrap(input);
          bytes = decoded.readAllBytes();
          decoded.close();
          input.close();
          input = new ByteArrayInputStream(bytes);
          BufferedImage img = ImageIO.read(input);
          input.close();
          return new Slide(format, img, bytes);
        } catch (IOException e) {
          e.printStackTrace();
          return null;
        }
      } else {
        throw new IllegalArgumentException("Bad image data for slide " + idx);
      }
    }
  }

  private static class FileChooser extends java.awt.Component implements JInputDialog<Slide> {
    Frame parent;
    Slide result;

    FileChooser(Frame parent, Slide r) {
      this.parent = parent;
      this.result = r;
    }

    public void setValue(Slide r) { result = r; }
    public Slide getValue() { return result; }

    public void setVisible(boolean b) {
      if (!b)
        return;
      JInputDialog<Attributes.LinkedFile> chooser =
        (JInputDialog<Attributes.LinkedFile>)
        ATTR_FILENAME_SINGLETON.getCellEditor(parent, result == null ? null : result.source);

      chooser.setVisible(true);
      Attributes.LinkedFile lf = chooser.getValue();

      if (lf == null)
        return;

      long size;
      try {
        size = Files.size(lf.absolute.toPath());
      } catch (IOException ex) {
        Errors.title(S.get("ioSlideshowErrorTitle")).show(S.get("ioSlideshowErrorMessage"), ex);
        return;
      }

      String[] options = {
        S.get("ioSlideshowEmbedOption"),
        S.get("ioSlideshowLinkOption"),
        S.get("ioSlideshowCancelOption") };
      int choice = JOptionPane.showOptionDialog(parent,
          String.format(S.get("ioSlideshowStorageDialogQuestion"), size),
          S.get("ioSlideshowStorageDialogTitle"), 0,
          JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
      if (choice == 0) {
        try {
          result = new Slide(lf.absolute);
        } catch (IOException ex) {
          Errors.title(S.get("ioSlideshowErrorTitle")).show(S.get("ioSlideshowErrorMessage"), ex);
          return;
        }
      } else if (choice == 1) {
          result = new Slide(lf);
      } else {
        result = null;
      }
    }
  }

  public static final int MAX_SLIDES = 32;

  public static final Attribute<BitWidth> ATTR_WIDTH = Attributes.forBitWidth(
      "addrWidth", S.getter("ioSlideshowAddrWidth"), 1, 32);

  public static final Attribute<Integer> ATTR_COUNT =
      Attributes.forIntegerRange("count", S.getter("ioSlideshowCount"), 1, MAX_SLIDES);

  public static final Attribute<Integer> ATTR_IMG_WIDTH =
      Attributes.forIntegerRange("width", S.getter("ioSlideshowWidth"), 10, 640);

  public static final Attribute<Integer> ATTR_IMG_HEIGHT =
      Attributes.forIntegerRange("height", S.getter("ioSlideshowHeight"), 10, 640);
 
  public static final ArrayList<Attribute<Slide>> ATTR_SLIDE = new ArrayList<>();
  static {
    for (int i = 0; i < MAX_SLIDES; i++)
      ATTR_SLIDE.add(new SlideAttribute(i));
  }

  public Slideshow() {
    super("Slideshow", S.getter("ioSlideshowComponent"));
    setKeyConfigurator(new BitWidthConfigurator(ATTR_WIDTH, 1, 30));
    setIconName("slideshow.gif");
    setPorts(new Port[] { new Port(0, 0, Port.INPUT, ATTR_WIDTH) });
    // setInstancePoker(Poker.class);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new SlideshowAttributes();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    int w = attrs.getValue(ATTR_IMG_WIDTH).intValue();
    int h = attrs.getValue(ATTR_IMG_HEIGHT).intValue();
    return Bounds.create(0, -h+10, w, h);
  }

  private State getState(InstanceState state) {
    int w = state.getAttributeValue(ATTR_IMG_WIDTH).intValue();
    int h = state.getAttributeValue(ATTR_IMG_HEIGHT).intValue();
    int n = state.getAttributeValue(ATTR_COUNT).intValue();
      
    State data = (State) state.getData();
    if (data == null) {
      data = new State(w, h, n);
      state.setData(data);
    } else {
      data.updateSize(w, h, n);
    }
    for (int i = 0; i < n; i++)
      data.updateSlide(i, state.getAttributeValue(ATTR_SLIDE.get(i)));

    return data;
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_IMG_WIDTH || attr == ATTR_IMG_HEIGHT)
      instance.recomputeBounds();
    instance.fireInvalidated();
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    State data = getState(painter);
    Bounds bds = painter.getBounds();
    boolean showState = painter.getShowState();
    Graphics g = painter.getGraphics();
    AttributeOption fit = painter.getAttributeValue(ATTR_FIT);

    int w = data.w;
    int h = data.h;
    
    if (showState) {
      int x = bds.getX();
      int y = bds.getY();
      BufferedImage img = data.getImage();
      if (img != null) {
        if (fit == STRETCH) {
          g.drawImage(img,
              x, y, x+w, y+h,
              0, 0, img.getWidth(), img.getHeight(),
              null);
        } else if (fit == SCALE) {
          double f = Math.max(1.0*img.getWidth()/w, 1.0*img.getHeight()/h);
          g.drawImage(img,
              x, y, x+w, y+h,
              0, 0, (int)(f*w), (int)(f*h),
              null);
        } else if (fit == SCALE_CROP) {
          double f = Math.min(1.0*img.getWidth()/w, 1.0*img.getHeight()/h);
          g.drawImage(img,
              x, y, x+w, y+h,
              0, 0, (int)(f*w), (int)(f*h),
              null);
        } else { // crop
          g.drawImage(img,
              x, y, x+w, y+h,
              0, 0, w, h,
              null);
        }
      } else {
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(x, y, w, h);
      }
      // TODO: could add inputs to control rotation, scaling, translucency, etc.
    }

    g.setColor(Color.BLACK);
    GraphicsUtil.switchToWidth(g, 2);
    g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    GraphicsUtil.switchToWidth(g, 1);
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    int n = state.getAttributeValue(ATTR_COUNT).intValue();

    State data = getState(state);
    int v = state.getPortValue(0).toIntValue();
    if (v < 0)
      data.deselect();
    else
      data.selectSlide(v);
  }

}
