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

package com.cburch.logisim.util;

// A simple daemon thread with event queue, intended for cases where
// new events are usually placed at the end of the queue (e.g. all events are
// scheduled for some fixed delta in the future). Events derive from QNode, and
// QNode.key is used as the millisecond timestamp for scheduling.
public abstract class EventScheduler<T extends QNode> extends UniquelyNamedThread {

  private LinkedQueue<T> q = new LinkedQueue<>();
  private Object lock = new Object();

  public EventScheduler(String threadNamePrefix) {
    super(threadNamePrefix);
  }

  // This is called for each event when that event's time arrives.
  // Subclasses should override this to do something with the event.
  public abstract void fire(T event);

  @Override
  public void run() {
    while (true) {

      T event = null;
      synchronized(lock) {
        while (true) {

          if (q.isEmpty()) {
            try { lock.wait(); }
            catch (InterruptedException e) { }
          } else {
            event = q.peek();
            long now = System.currentTimeMillis();
            if (event.key <= now) {
              q.remove();
              break;
            }
            try { lock.wait(event.key - now); }
            catch (InterruptedException e) { }
          }

        }
      }
      fire(event);

    }
  }

  public void schedule(T event) {
    synchronized(lock) {
      boolean isFirst = q.add(event);
      if (isFirst)
        lock.notifyAll();
    }
  }

}
