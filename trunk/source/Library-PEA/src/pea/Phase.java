/* $Id: Phase.java 307 2008-07-29 16:03:20Z jfaber $
 *
 * This file is part of the PEA tool set
 *
 * The PEA tool set is a collection of tools for Phase Event Automata
 * (PEA).
 *
 * Copyright (C) 2005-2006, Carl von Ossietzky University of Oldenburg
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package pea;

import pea.util.SimpleSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;


public class Phase implements Comparable<Phase> {
    int nr;

    // SR 2010-07-09
    public boolean isKernel;
    public boolean isInit;
    public boolean isEntry;
    public boolean isExit;
    public boolean isVisited;
    public boolean isDlSuspect;
    public boolean haslDl = true;
    public int flags;
    public CDD dlCheck;
    private Vector<Transition> incomming;
    String name;
    CDD stateInv;
    CDD clockInv;
    Set<String> stoppedClocks;
    List<Transition> transitions;
    public int ID;

    /** The phase bits used by the powerset construction.
     * This is only set for automata built from CounterExample traces.
     */
    PhaseBits phaseBits;

    public Phase(String name, CDD stateInv, CDD clockInv,
        Set<String> stoppedClocks) {
        this.name = name;
        this.stateInv = stateInv;
        this.clockInv = clockInv;
        this.transitions = new ArrayList<Transition>();
        this.stoppedClocks = stoppedClocks;
        dlCheck = clockInv;

        isKernel = false;
        isInit = false;
        isEntry = false;
        isExit = false;
        isVisited = false;
        isDlSuspect = false;
        flags = 0;
        incomming = new Vector<Transition>();
    }

    public Phase(String name, CDD stateInv, CDD clockInv) {
        this(name, stateInv, clockInv, new SimpleSet<String>(0));
    }

    public Phase(String name, CDD stateInv) {
        this(name, stateInv, CDD.TRUE);
    }

    public Phase(String name) {
        this(name, CDD.TRUE, CDD.TRUE);
    }

    public boolean isInit() {
        return isInit;
    }

    public void setInit(boolean isInit) {
        this.isInit = isInit;
    }

    public PhaseBits getPhaseBits() {
    	return phaseBits;
    }

    public CDD getStateInvariant() {
        return stateInv;
    }

    public void setStateInvariant(CDD inv) {
        stateInv = inv;
    }

    public CDD getClockInvariant() {
        return clockInv;
    }

    public Set<String> getStoppedClocks() {
        return stoppedClocks;
    }

    public boolean isStopped(String clock) {
        return stoppedClocks.contains(clock);
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    /** @return the transition added or modified */
    public Transition addTransition(Phase dest, CDD guard, String[] resets) {
        Iterator<Transition> it = transitions.iterator();

        while (it.hasNext()) {
            Transition t = (Transition) it.next();

            if ((t.getDest() == dest) && t.resets.equals(resets)) {
                t.guard = t.guard.or(guard);

                return t;
            }
        }

        Transition t = new Transition(this, guard, resets, dest);
        transitions.add(t);

        return t;
    }

    public String toString() {
        return name;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    public void dump() {
        System.err.println("  state " + this + " { ");

        if (stateInv != CDD.TRUE) {
            System.err.println("    predicate      " + stateInv);
        }

        if (clockInv != CDD.TRUE) {
            System.err.println("    clockinvariant " + clockInv);
        }

        for (String clock : stoppedClocks) {
            System.err.println("    stopped " + clock);
        }

        System.err.println("    transitions {");

        Iterator<Transition> it = transitions.iterator();

        while (it.hasNext())
            System.err.println("       " + it.next());

        System.err.println("    }");
        System.err.println("  }");
    }

    public void dumpDot() {
        System.out.println("  " + this.name + " [ label = \"" + stateInv +
            "\\n" + clockInv + "\" shape=ellipse ]");

        Iterator<Transition> it = transitions.iterator();

        while (it.hasNext()) {
            Transition t = it.next();
            System.out.println("  " + t.getSrc().name + " -> " +
                t.getDest().name + " [ label = \"" + t.guard + "\" ]");
        }
    }

    public String getFlags() {
        String flags = new String();

        if (isInit) {
            flags += " Init ";
        }

        if (isKernel) {
            flags += " Kernel ";
        }

        if (isEntry) {
            flags += " Entry ";
        }

        if (isExit) {
            flags += " Exit ";
        }

        return flags;
    }

    // jf
    public void setName(String name) {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Phase p) {
        return name.compareTo(p.name);
    }

    public void addIncomming(Transition trans) {
        incomming.add(trans);
    }

    public void removeIncomming(Transition trans) {
        incomming.remove(trans);
    }
    public void setID(int ID) {
    	this.ID = ID;
    }
    public int getID() {
    	return ID;
    }
}
