/*
 * Copyright (C) 2011-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2009-2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE Automata Library.
 * 
 * The ULTIMATE Automata Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE Automata Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Automata Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Automata Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE Automata Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonOldApi;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;


/**
 * Given a PetriNet, constructs a finite Automaton that recognizes the same
 * language.
 * @author heizmann@informatik.uni-freiburg.de
 *
 * @param <LETTER> Symbol
 * @param <STATE> Content
 */
public class ConcurrentProduct<LETTER,STATE> {
	
	private final IUltimateServiceProvider m_Services;
	
	private final Logger m_Logger;
	
	private final boolean m_ConcurrentPrefixProduct;

	private final INestedWordAutomatonOldApi<LETTER,STATE> M_Nwa1;
	private final INestedWordAutomatonOldApi<LETTER,STATE> M_Nwa2;
	private final NestedWordAutomaton<LETTER,STATE> m_Result;
	
	/**
	 * List of state pairs from the input automata for which
	 * <ul>
	 * <li> there has already been a state in the result constructed
	 * <li> outgoing transitions of this state have not yet been constructed.
	 * </ul>
 	 */
	private final StatePairQueue m_Worklist = new StatePairQueue();
	
	/**
	 * Map from state pairs of the input automata to corresponding states
	 * in the result automaton. 
	 */
	private final StatePair2StateMap inputPair2resultState = 
		new StatePair2StateMap();
	

	/**
	 * Common symbols of the Alphabets of the input automata.
	 */
	private final HashSet<LETTER> m_SynchronizationAlphabet;

	private final StateFactory<STATE> m_ContentFactory;
	

	

	
	
	/**
	 * Returns the automaton state that represents the state pair
	 * (state1,state2). If this state is not yet constructed, construct it
	 * and enqueue the pair (state1,state2). If it has to be
	 * constructed it is an initial state iff isInitial is true. 
	 */
	private STATE getState(STATE state1, STATE state2,
															boolean isInitial) {
		STATE state = 
					inputPair2resultState.get(state1, state2);
		if (state == null) {
			boolean isFinal;
			if (m_ConcurrentPrefixProduct) {
				isFinal = M_Nwa1.isFinal(state1) || M_Nwa2.isFinal(state2);
			}
			else {			
				isFinal = M_Nwa1.isFinal(state1) && M_Nwa2.isFinal(state2);
			}
			STATE content1 = state1;
			STATE content2 = state2;
			state = m_ContentFactory.getContentOnConcurrentProduct(
															content1,content2);
			m_Result.addState(isInitial, isFinal, state);
			inputPair2resultState.put(state1,state2,state);
			m_Worklist.enqueue(state1, state2);
		}
		return state;
	}
	

	
	
	private void constructOutgoingTransitions(STATE state1,	STATE state2) {
		STATE state = getState(state1,state2,false);
		HashSet<LETTER> commonOutSymbols = 
				new HashSet<LETTER>(M_Nwa1.lettersInternal(state1));
		commonOutSymbols.retainAll(M_Nwa2.lettersInternal(state2));
		HashSet<LETTER> state1OnlySymbols = 
				new HashSet<LETTER>(M_Nwa1.lettersInternal(state1));
		state1OnlySymbols.removeAll(m_SynchronizationAlphabet);
		HashSet<LETTER> state2OnlySymbols = 
				new HashSet<LETTER>(M_Nwa2.lettersInternal(state2));
		state2OnlySymbols.removeAll(m_SynchronizationAlphabet);
		
		for (LETTER symbol : commonOutSymbols) {
			Iterable<STATE> succ1s = M_Nwa1.succInternal(state1, symbol);
			Iterable<STATE> succ2s = M_Nwa2.succInternal(state2, symbol);
			for (STATE succ1 : succ1s) {
				for (STATE succ2 : succ2s) {
					STATE succ = getState(succ1, succ2, false);
					m_Result.addInternalTransition(state, symbol, succ);
				}
			}
		}
		
		for (LETTER symbol : state1OnlySymbols) {
			Iterable<STATE> succ1s = M_Nwa1.succInternal(state1, symbol);
			for (STATE succ1 : succ1s) {
					STATE succ = getState(succ1, state2, false);
					m_Result.addInternalTransition(state, symbol, succ);
			}
		}
		
		for (LETTER symbol : state2OnlySymbols) {
			Iterable<STATE> succ2s = M_Nwa2.succInternal(state2, symbol);
			for (STATE succ2 : succ2s) {
					STATE succ = getState(state1, succ2, false);
					m_Result.addInternalTransition(state, symbol, succ);
			}
		}

		
	}

	public void constructInitialStates() {
		for (STATE state1 : M_Nwa1.getInitialStates()) {
			for (STATE state2 : M_Nwa2.getInitialStates()) {
				getState(state1, state2, true);
			}
		}
	}
	
	
	public ConcurrentProduct(IUltimateServiceProvider services, 
			INestedWordAutomatonOldApi<LETTER,STATE> nwa1,
			INestedWordAutomatonOldApi<LETTER,STATE> nwa2, boolean concurrentPrefixProduct) {
		m_Services = services;
		m_Logger = m_Services.getLoggingService().getLogger(LibraryIdentifiers.s_LibraryID);
		m_ConcurrentPrefixProduct = concurrentPrefixProduct;
		M_Nwa1 = nwa1;
		M_Nwa2 = nwa2;
		m_ContentFactory = nwa1.getStateFactory();
//FIXME
//		if (m_ContentFactory != nwa2.getContentFactory()) {
//			throw new IllegalArgumentException("Both NWAs have to use" +
//					"same ContentFactory");
//		}
		
		if (!nwa1.getCallAlphabet().isEmpty() ||
				!nwa1.getReturnAlphabet().isEmpty() ||
				!nwa2.getCallAlphabet().isEmpty() ||
				!nwa2.getReturnAlphabet().isEmpty()) {
			m_Logger.warn("Call alphabet and return alphabet are ignored.");
		}
		m_SynchronizationAlphabet = new HashSet<LETTER>(nwa1.getInternalAlphabet());
		m_SynchronizationAlphabet.retainAll(nwa2.getInternalAlphabet());
		Set<LETTER> commonAlphabet = new HashSet<LETTER>(nwa1.getInternalAlphabet());
		commonAlphabet.addAll(nwa2.getInternalAlphabet());
		m_Result = new NestedWordAutomaton<LETTER,STATE>(
				m_Services, commonAlphabet,
									 new HashSet<LETTER>(0),
									 new HashSet<LETTER>(0),
									 m_ContentFactory);
		constructInitialStates();
		while (!m_Worklist.isEmpty()) {
			m_Worklist.dequeuePair();
			STATE state1 = m_Worklist.getDequeuedPairFst();
			STATE state2 = m_Worklist.getDequeuedPairSnd();
			constructOutgoingTransitions(state1,state2);
		}
	}

	public INestedWordAutomatonOldApi<LETTER,STATE> getResult() {
		return m_Result;
	}
	

	/**
	 * Maps pairs of states to states. 
	 * @author heizmann@informatik.uni-freiburg.de
	 */
	private class StatePair2StateMap {
		Map<STATE,Map<STATE,STATE>> backingMap =
			new HashMap<STATE, Map<STATE,STATE>>();
		
		public STATE get(STATE state1, STATE state2) {
			Map<STATE,STATE> snd2result = backingMap.get(state1);
			if (snd2result == null) {
				return null;
			}
			else {
				return snd2result.get(state2);
			}
		}
		
		public void put(STATE state1,
						STATE state2,
						STATE state) {
			Map<STATE,STATE> snd2result = backingMap.get(state1);
			if (snd2result == null) {
				snd2result = new HashMap<STATE,STATE>();
				backingMap.put(state1,snd2result);
			}
			snd2result.put(state2,state);
		}
	}

	
	/**
	 * Queue for pairs of states. Pairs are not dequeued in the same order as
	 * inserted. 
	 * @author heizmann@informatik.uni-freiburg.de
	 */
	private class StatePairQueue {
		Map<STATE,Set<STATE>> m_Queue =
			new HashMap<STATE,Set<STATE>>();
		
		STATE m_DequeuedPairFst;
		STATE m_DequeuedPairSnd;
		
		public void enqueue(STATE state1, STATE state2) {
			Set<STATE> secondComponets = m_Queue.get(state1);
			if (secondComponets == null) {
				secondComponets = new HashSet<STATE>();
				m_Queue.put(state1,secondComponets);
			}
			secondComponets.add(state2);
		}
		
		public void dequeuePair() {
			assert (m_DequeuedPairFst == null && m_DequeuedPairSnd == null) : 
				"Results from last dequeue not yet collected!";
			Iterator<STATE> it1 = m_Queue.keySet().iterator();
			m_DequeuedPairFst = it1.next();
			assert (m_Queue.get(m_DequeuedPairFst) !=  null);
			Set<STATE> secondComponets = m_Queue.get(m_DequeuedPairFst);
			assert (secondComponets != null);
			Iterator<STATE> it2 = secondComponets.iterator();
			m_DequeuedPairSnd = it2.next();
			
			secondComponets.remove(m_DequeuedPairSnd);
			if (secondComponets.isEmpty()) {
				m_Queue.remove(m_DequeuedPairFst);
			}
		}
		
		public STATE getDequeuedPairFst() {
			assert (m_DequeuedPairFst != null) : "No pair dequeued";
			STATE result = m_DequeuedPairFst;
			m_DequeuedPairFst = null;
			return result;
		}

		public STATE getDequeuedPairSnd() {
			assert (m_DequeuedPairSnd != null) : "No pair dequeued";
			STATE result = m_DequeuedPairSnd;
			m_DequeuedPairSnd = null;
			return result;
		}

		public boolean isEmpty() {
			return m_Queue.isEmpty();
		}
	}

}
