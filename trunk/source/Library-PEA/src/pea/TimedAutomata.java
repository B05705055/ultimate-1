/* $Id: TimedAutomata.java 227 2006-10-19 07:29:43Z jfaber $ 
 *
 * This file is part of the PEA tool set
 * 
 * The PEA tool set is a collection of tools for 
 * Phase Event Automata (PEA). See
 * http://csd.informatik.uni-oldenburg.de/projects/peatools.html
 * for more information.
 * 
 * Copyright (C) 2005-2006, Department for Computing Science,
 *                          University of Oldenburg
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
import java.util.*;

public class TimedAutomata {
    public static final int OP_LT      = -2;
    public static final int OP_LTEQ    = -1;
    public static final int OP_EQ      =  0;
    public static final int OP_GTEQ    =  1;
    public static final int OP_GT      =  2;
    public static final int OP_NEQ     =  4;

    class Edge {
	State src;
	Guard[]  guard;
	String[] resets;
	State dest;
    }

    class State {
	int    nr;
	String props;
	Guard[]  clockInv;
	Edge[]   edges;
    }

    class Guard {
	String clock;
	int cmp;
	int value;

	public String toString() {
	    String op = "??";
	    switch(cmp) {
	    case OP_LT:
		op = "<";
		break;
	    case OP_LTEQ:
		op = "<=";
		break;
	    case OP_EQ:
		op = "==";
		break;
	    case OP_GT:
		op = ">";
		break;
	    case OP_GTEQ:
		op = ">=";
		break;
	    case OP_NEQ:
		op = "!=";
		break;
	    }
	    return clock + op + value;
	}
    }
    
    Collection<String> clocks;
    State[]  states;

    public TimedAutomata(PhaseEventAutomata pea, CDD[] preds, String[] predNames) {
	states = new State[pea.phases.length];
	clocks = new TreeSet<String>();
	for (int i = 0; i < pea.phases.length; i++) {
	    pea.phases[i].nr = i;
	    states[i] = new State();
	    states[i].nr = i;
	    states[i].props = "dummy";
	    states[i].clockInv = filterCDD(pea.phases[i].clockInv)[0];
	    addClocks(states[i].clockInv);
	    for (int j = 0; j < preds.length; j++) {
		if (pea.phases[i].getStateInvariant().and(preds[j])
		    != CDD.FALSE)
		    states[i].props += " " + predNames[j];
	    }
	}
	for (int i = 0; i < pea.init.length; i++) {
	    states[pea.init[i].nr].props += " init";
	}
	for (int i = 0; i < pea.phases.length; i++) {
	    Iterator it = pea.phases[i].transitions.iterator();
	    Collection<Edge> edges = new ArrayList<Edge>();
	    while (it.hasNext()) {
		Transition t = (Transition) it.next();
		Guard[][] allGuards = filterCDD(t.guard);
		for (int k = 0; k < allGuards.length; k++) {
		    Edge e = new Edge();
		    e.guard = allGuards[k];
		    e.resets = t.resets;
		    addClocks(e.guard);
		    addClocks(e.resets);
		    e.dest = states[t.dest.nr];
		    edges.add(e);
		}
	    }
	    states[i].edges = (Edge[]) edges.toArray(new Edge[edges.size()]);
	}
    }

    private Guard[][] filterCDD(Guard[] gs, CDD cdd) {
	if ((cdd.decision instanceof RangeDecision)
	    && ((RangeDecision) cdd.decision).var.indexOf("_X") >= 0) {
	    ArrayList<Guard[]> myGuards = new ArrayList<Guard[]>();
	    String clk = ((RangeDecision) cdd.decision).var;
	    int[] limits = ((RangeDecision) cdd.decision).limits;
	    for (int i = 0; i < cdd.childs.length; i++) {
		if (cdd.childs[i] == CDD.FALSE)
		    continue;
		
		boolean isEqual = (i > 0 && i < cdd.childs.length - 1
				   && limits[i-1]/2 == limits[i]/2);

		Guard[] ngs = new Guard[gs.length
					+ (i == 0 || 
					   i == cdd.childs.length - 1 ||
					   isEqual ? 1 : 2)];
		System.arraycopy(gs, 0, ngs, 0, gs.length);
		int j = gs.length;
		if (i > 0) {
		    ngs[j] = new Guard();
		    ngs[j].clock = clk;
		    ngs[j].cmp = (isEqual ? OP_EQ
				 : (limits[i-1] & 1) == 1 ? OP_GT
				 : OP_GTEQ);
		    ngs[j].value = limits[i-1] / 2;
		    j++;
		}
		if (i < cdd.childs.length - 1 && !isEqual) {
		    ngs[j] = new Guard();
		    ngs[j].clock = clk;
		    ngs[j].cmp = ((limits[i] & 1) == 0 ? OP_LT : OP_LTEQ);
		    ngs[j].value = limits[i] / 2;
		}
		Guard[][] childGuards = filterCDD(ngs, cdd.childs[i]);
		for (j = 0; j < childGuards.length; j++)
		    myGuards.add(childGuards[j]);
	    }
	    return myGuards.toArray(new Guard[0][]);
	} else {
	    if (cdd == CDD.FALSE)
		return new Guard[0][0];
	    if (cdd == CDD.TRUE) {
		Guard[][] result = new Guard[1][];
		result[0] = gs;
		return result;
	    }
	    CDD newcdd = CDD.FALSE;
	    for (int i = 0; i < cdd.childs.length; i++)
		newcdd = newcdd.or(cdd.childs[i]);
	    return filterCDD(gs, newcdd);
	}
    }

    private Guard[][] filterCDD(CDD cdd) {
	return filterCDD(new Guard[0], cdd);
    }

    private void addClocks(String[] carr) {
	for (int i = 0; i < carr.length; i++)
	    clocks.add(carr[i]);
    }
    private void addClocks(Guard[] guard) {
	for (int i = 0; i< guard.length; i++)
	    clocks.add(guard[i].clock);
    }

    private String dumpGuard(Guard[] guard) {
	if (guard.length == 0)
	    return "TRUE";

	StringBuffer sb = new StringBuffer();
	String delim="";
	for (int i = 0; i < guard.length; i++) {
	    sb.append(delim).append(guard[i]);
	    delim = " and ";
	}
	return sb.toString();
    }

    private String dumpResets(String[] resets) {
	if (resets.length == 0)
	    return "";
	StringBuffer sb = new StringBuffer("RESET{");
	for (int i = 0; i < resets.length; i++)
	    sb.append(" ").append(resets[i]);
	return sb.append(" }").toString();
    }

    public void dumpKronos() {
	System.out.println("/* Complete System */");
	System.out.println("#locs "+states.length);
	int numEdges = 0;
	for (int i = 0; i < states.length; i++) {
	    numEdges += states[i].edges.length;
	}
	System.out.println("#trans "+numEdges);
	System.out.print("#clocks "+clocks.size());
	Iterator it = clocks.iterator();
	while (it.hasNext()) {
	    System.out.print(" "+it.next());
	}
	System.out.println();
	System.out.println("#sync");
	for (int i = 0; i < states.length; i++) {
	    System.out.println();
	    System.out.println("loc: "+i);
	    System.out.println("prop: "+states[i].props);
	    System.out.println("invar: "+dumpGuard(states[i].clockInv));
	    System.out.println("trans: ");
	    for (int j = 0; j < states[i].edges.length; j++) {
		Edge e = states[i].edges[j];
		System.out.println(dumpGuard(e.guard) + " => ; "+
				   dumpResets(e.resets) + "; goto "+e.dest.nr);
	    }
	}
    }
}
