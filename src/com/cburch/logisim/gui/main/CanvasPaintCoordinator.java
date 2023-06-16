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

package com.cburch.logisim.gui.main;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


// This class forms a bridge between the simulation threads, which recompute
// circuit values and update circuit state, and the AWT thread, which redraws
// the screen. 
//  (1) The sim thread calls requestRepaint() whenever the circuit has state
//      been updated and needs to be redrawn. This may happen as frequently as
//      every few milliseconds or faster, or as slowly as once per few seconds,
//      depending on the user's chosen tick frequency.
//  (2) CanvasPaintCoordinator keeps track of those requests, and periodically
//      invokes canvas.repaint(), which enqueues work on the AWT thread. The
//      repaint() calls are metered so they occur at most once per 50
//      milliseconds (so 20 redraws per second), and so there is never more than
//      one repaint() outstanding at a time.
//  (3) The ATW thread services the repaint() requests performs the drawing. It
//      also invokes repaintCompleted() as a callback to notify
//      CanvasPaintCoordinator that the repaining is finished, so that another
//      repaint() can be issued, if and when needed.

class CanvasPaintCoordinator {
  private static final int REPAINT_TIMESPAN = 50; // 50 ms between repaints

  private Canvas canvas;

  private volatile long tDirtied; // timestamp at which canvas was last dirtied
  private volatile long tCleaned; // timestamp at which last canvas cleaning started
  private volatile long sDirtied; // sequence number updated when canvas was last dirtied
  private volatile long sCleaned; // sequence number at which last canvas cleaning started
  private volatile boolean cleaning; // repaint is curently scheduled or in progress

  private Timer timer;
  private Object lock;

  public CanvasPaintCoordinator(Canvas canvas) {
    this.canvas = canvas;
    lock = new Object();
    tCleaned = tDirtied = System.currentTimeMillis();
    sDirtied = sCleaned = 0;
    cleaning = false;
    timer = new Timer(1, new ActionListener() {
      public void actionPerformed(ActionEvent e) { 
        synchronized(lock) {
          sCleaned = sDirtied;
          tCleaned = tDirtied;
        }
        canvas.repaint();
      }
    });
    timer.setRepeats(false);
  }

  public void requestRepaint() {
    long now = System.currentTimeMillis();
    boolean repaintNow = false;
    long repaintSoon = 0;
    synchronized (lock) {
      sDirtied++;
      tDirtied = now;
      long ago = now - tCleaned;
      if (!cleaning && ago >= REPAINT_TIMESPAN) {
        // it's been a while, so repaint immediately
        cleaning = true;
        sCleaned = sDirtied;
        tCleaned = tDirtied;
        repaintNow = true;
      } else if (!cleaning) {
        // we repainted too recently, so repaint in a little while
        cleaning = true;
        repaintSoon = REPAINT_TIMESPAN - ago;
      }
    }
    if (repaintNow) {
      canvas.repaint();
    } else if (repaintSoon > 0) {
      timer.setInitialDelay((int)repaintSoon);
      timer.start();
    }
  }

  public void repaintCompleted() {
    long now = System.currentTimeMillis();
    boolean repaintNow = false;
    long repaintSoon = 0;
    synchronized (lock) {
      cleaning = false;
      long ago = now - tCleaned;
      if (sCleaned < sDirtied && ago >= REPAINT_TIMESPAN) {
        // it's been a while, so repaint immediately
        cleaning = true;
        sCleaned = sDirtied;
        tCleaned = tDirtied;
        repaintNow = true;
      } else if (sCleaned < sDirtied) {
        // we repainted too recently, so repaint in a little while
        cleaning = true;
        repaintSoon = REPAINT_TIMESPAN - ago;
      }
    }
    if (repaintNow) {
      canvas.repaint();
    } else if (repaintSoon > 0) {
      timer.setInitialDelay((int)repaintSoon);
      timer.start();
    }
  }

}
