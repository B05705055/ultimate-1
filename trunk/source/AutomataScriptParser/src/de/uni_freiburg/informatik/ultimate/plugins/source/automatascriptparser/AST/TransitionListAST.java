/*
 * Copyright (C) 2013-2015 Betim Musa (musab@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE AutomataScriptParser plug-in.
 * 
 * The ULTIMATE AutomataScriptParser plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE AutomataScriptParser plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE AutomataScriptParser plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE AutomataScriptParser plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE AutomataScriptParser plug-in grant you additional permission 
 * to convey the resulting work.
 */
/**
 * 
 */
package de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AST;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.model.location.ILocation;
import de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AtsASTNode;

/**
 * Is used to hold transitions for nestedword automata (internal-, call-, and
 * return-transitions) and for Petri nets.
 * 
 * 
 * @author musab@informatik.uni-freiburg.de
 *
 */
public class TransitionListAST extends AtsASTNode {
	
	public class Pair<L, R> {
		public final L left;
		public final R right;
		
		public Pair(L left, R right) {
			this.left = left;
			this.right = right;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 7;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((left == null) ? 0 : left.hashCode());
			result = prime * result + ((right == null) ? 0 : right.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings("unchecked")
			Pair<L, R> other = (Pair<L, R>) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (left == null) {
				if (other.left != null)
					return false;
			} else if (!left.equals(other.left))
				return false;
			if (right == null) {
				if (other.right != null)
					return false;
			} else if (!right.equals(other.right))
				return false;
			return true;
		}

		private TransitionListAST getOuterType() {
			return TransitionListAST.this;
		}
		
		@Override
		public String toString() {
			return "(" + this.left + "," + this.right + ")";
		}
		
		
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 4468320445354864058L;
	private Map<Pair<String, String> , Set<String>> m_Transitions;
	private Map<String, Map<String, Map<String, Set<String>>>> m_ReturnTransitions;
	private List<PetriNetTransitionAST> m_netTransitions;
	
	
	public TransitionListAST(ILocation loc) {
		super(loc);
		m_Transitions = new HashMap<Pair<String,String>, Set<String>>();
		m_ReturnTransitions = new HashMap<String, Map<String, Map<String, Set<String>>>>();
		m_netTransitions = new ArrayList<PetriNetTransitionAST>();
	}
	
	
	/**
	 * Method to add an internal or call transition for nested word automaton.
	 * @param fromState
	 * @param label
	 * @param toState
	 */
	public void addTransition(String fromState, String label, String toState) {
		Pair<String, String> stateSymbolPair = new Pair<String, String>(fromState, label);
		if (m_Transitions.containsKey(stateSymbolPair)) {
			Set<String> succs = m_Transitions.get(stateSymbolPair);
			succs.add(toState);
			m_Transitions.put(stateSymbolPair, succs);
		} else {
			Set<String> succs = new HashSet<String>();
			succs.add(toState);
			m_Transitions.put(stateSymbolPair, succs);
		}
	}
	
	/**
	 * Method to add a return transition for a nested word automaton.
	 * @param fromState
	 * @param returnState
	 * @param label
	 * @param toState
	 */
	public void addTransition(String fromState, String returnState, String label, String toState) {
		Map<String, Map<String, Set<String>>> hier2letter2succs = m_ReturnTransitions.get(fromState);
		if (hier2letter2succs == null) {
			hier2letter2succs = new HashMap<String, Map<String, Set<String>>>();
			m_ReturnTransitions.put(fromState, hier2letter2succs);
		}
		Map<String, Set<String>> letter2succs = hier2letter2succs.get(returnState);
		if (letter2succs == null) {
			letter2succs = new HashMap<String, Set<String>>();
			hier2letter2succs.put(returnState, letter2succs);
		}
		Set<String> succs = letter2succs.get(label);
		if (succs == null) {
			succs = new HashSet<String>();
			letter2succs.put(label, succs);
		}
		succs.add(toState);
	}
	
	public void addTransition(IdentifierListAST idList) {
		List<String> ids = idList.getIdentifierList();
		if (ids.size() == 3) {
			addTransition(ids.get(0), ids.get(1), ids.get(2));
		} else if (ids.size() == 4) {
			addTransition(ids.get(0), ids.get(1), ids.get(2), ids.get(3));
		}
	}

	public Map<Pair<String, String>, Set<String>> getTransitions() {
		return m_Transitions;
	}
	
	public Map<String, Map<String, Map<String, Set<String>>>> getReturnTransitions() {
		return m_ReturnTransitions;
	}
	
	/**
	 * Method to add a transition for Petri nets.
	 * @param nt the transition of a Petri net
	 */
	public void addNetTransition(PetriNetTransitionAST nt) {
		m_netTransitions.add(nt);
	}

	public List<PetriNetTransitionAST> getNetTransitions() {
		return m_netTransitions;
	}

	
}
