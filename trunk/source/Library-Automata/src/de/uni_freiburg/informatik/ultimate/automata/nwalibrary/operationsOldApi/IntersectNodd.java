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
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonOldApi;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;


/**
 * Implementation of Intersection. Result contains only states that are
 * connected to an initial state in the graph representation of the product
 * automaton. Some of this states may not be reachable because runs may not
 * satisfy the stack discipline.
 * @author heizmann@informatik.uni-freiburg.de
 */
public class IntersectNodd<LETTER,STATE> implements IOperation<LETTER,STATE> {
	
	private final AutomataLibraryServices m_Services;
	private final ILogger m_Logger;
	
	
	@Override
	public String operationName() {
		return "intersectNodd";
	}
	
	@Override
	public String startMessage() {
			return "Start " + operationName() + ". First operand " + 
			fstOperand.sizeInformation() + ". Second operand " + 
			sndOperand.sizeInformation();	
	}

	@Override
	public String exitMessage() {
		return "Finished " + operationName() + " Result " + 
		fstOperand.sizeInformation();
	}
	
	
	private INestedWordAutomatonOldApi<LETTER,STATE> fstOperand;

	public IntersectNodd (AutomataLibraryServices services, 
			INestedWordAutomatonOldApi<LETTER,STATE> nwa, INestedWordAutomatonOldApi<LETTER,STATE> nwa2) {
		m_Services = services;
		m_Logger = m_Services.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		this.fstOperand = nwa;
		this.sndOperand = nwa2;
		m_Logger.info(startMessage());
		buildProduct();
		m_Logger.info(exitMessage());
	}
	
	public INestedWordAutomatonOldApi<LETTER,STATE> getResult() throws AutomataLibraryException {
		return m_result;
	}
	
	/**
	 * Maps a pair of states (s1,s2) to the product state ps that represents
	 * the pair.
	 */
	Map<STATE,Map<STATE,STATE>> m_pair2ps = 
				new HashMap<STATE, Map<STATE, STATE>>();
	/**
	 * Maps a product state ps=(s1,s2) to its first component.
	 */
	Map<STATE,STATE> m_psTos1 = 
				new HashMap<STATE,STATE>();
	/**
	 * Maps a product state ps=(s1,s2) to its second component.
	 */
	Map<STATE,STATE> m_psTos2 = 
				new HashMap<STATE,STATE>();
	
	Map<STATE,Map<STATE,Set<ReturnTransition>>> m_ReturnTransitionQueue =
				new HashMap<STATE,Map<STATE,Set<ReturnTransition>>>();
	
	LinkedList<STATE> m_toprocess = new LinkedList<STATE>();
	Set<STATE> m_visited = new HashSet<STATE>();
	
	INestedWordAutomatonOldApi<LETTER,STATE> sndOperand;
	NestedWordAutomaton<LETTER,STATE> m_result;
	
	private void buildProduct() {
		// Intersect alphabets
		Set<LETTER> newInternals = new HashSet<LETTER>();
		newInternals.addAll(fstOperand.getInternalAlphabet());
		newInternals.retainAll(sndOperand.getInternalAlphabet());
		Set<LETTER> newCalls = new HashSet<LETTER>();
		newCalls.addAll(fstOperand.getCallAlphabet());
		newCalls.retainAll(sndOperand.getCallAlphabet());
		Set<LETTER> newReturns = new HashSet<LETTER>();
		newReturns.addAll(fstOperand.getReturnAlphabet());
		newReturns.retainAll(sndOperand.getReturnAlphabet());
		
		
		m_result = new NestedWordAutomaton<LETTER,STATE>(m_Services, 
				newInternals, newCalls, newReturns, fstOperand.getStateFactory());
		for (STATE fst : fstOperand.getInitialStates()) {
			for (STATE snd : sndOperand.getInitialStates()) {
				STATE ps = getProductState(fst, snd);
				m_toprocess.add(ps);
				m_visited.add(ps);
			}
		}
		
		
		while (!m_toprocess.isEmpty()) {
			STATE ps = m_toprocess.removeFirst();
			STATE fst = m_psTos1.get(ps);
			STATE snd = m_psTos2.get(ps);
			for (LETTER symbol : fstOperand.lettersInternal(fst)) {
				Iterable<STATE> succSet1 = fstOperand.succInternal(fst, symbol);
				Iterable<STATE> succSet2 = sndOperand.succInternal(snd, symbol);
				addInternalTransition(ps, symbol, succSet1, succSet2);
			}
			for (LETTER symbol : fstOperand.lettersCall(fst)) {
				Iterable<STATE> succSet1 = fstOperand.succCall(fst, symbol);
				Iterable<STATE> succSet2 = sndOperand.succCall(snd, symbol);
				addCallTransition(ps, symbol, succSet1, succSet2);
			}
			for (LETTER symbol : fstOperand.lettersReturn(fst)) {
				Iterable<STATE> linPredSet1 = fstOperand.hierPred(fst, symbol); 
				Iterable<STATE> linPredSet2 = sndOperand.hierPred(snd, symbol);
				addReturns(ps, fst, snd, symbol, linPredSet1, linPredSet2);
			}
		}
		m_Logger.debug("Processing at least "+m_ReturnTransitionQueue.size()+ " return transitions.");
		for (STATE s1linPred : m_ReturnTransitionQueue.keySet()) {
			Map<STATE, STATE> s2linPred2ps = m_pair2ps.get(s1linPred);
			if (s2linPred2ps != null) {
				for (STATE s2linPred : m_ReturnTransitionQueue.get(s1linPred).keySet()) {
					STATE plinPred = s2linPred2ps.get(s2linPred);
					if (plinPred != null) {
						for (ReturnTransition retTrans : m_ReturnTransitionQueue.get(s1linPred).get(s2linPred)) {
							m_result.addReturnTransition(retTrans.getPred(), plinPred, retTrans.getSymbol(), retTrans.getSucc());
						}
					}
				}
			}
		}
	}

	private STATE getProductState(STATE s1, STATE s2) {
		Map<STATE, STATE> s2ToPs;
		if (m_pair2ps.containsKey(s1)) {
			s2ToPs = m_pair2ps.get(s1);
			if (s2ToPs.containsKey(s2)) {
				return s2ToPs.get(s2);
			}
		}
		else {
			s2ToPs = new HashMap<STATE, STATE>();
			m_pair2ps.put(s1, s2ToPs);
		}
		STATE newState = fstOperand.getStateFactory().intersection(s1, s2);
		boolean isFinal = fstOperand.isFinal(s1) && sndOperand.isFinal(s2);
		boolean isInitial = fstOperand.getInitialStates().contains(s1) && sndOperand.getInitialStates().contains(s2);
		m_result.addState(isInitial, isFinal, newState);
		s2ToPs.put(s2,newState);
		m_psTos1.put(newState, s1);
		m_psTos2.put(newState, s2);
		return newState;			
	}
	
	
	private void addInternalTransition(STATE state,LETTER symbol,
			Iterable<STATE> succSet1,
			Iterable<STATE> succSet2) {
		assert (succSet1 != null);
		if (succSet2 == null) {
			return;
		}
		for (STATE s1 : succSet1) {
			for (STATE s2 : succSet2) {
				STATE ps = getProductState(s1, s2);
				m_result.addInternalTransition(state, symbol, ps);
				if (!m_visited.contains(ps)) {
					m_visited.add(ps);
					m_toprocess.add(ps);
				}
			}
		}
	}
	
	private void addCallTransition(STATE state,
								LETTER symbol,
								Iterable<STATE> succSet1,
								Iterable<STATE> succSet2) {
		assert (succSet1 != null);
		if (succSet2 == null) {
			return;
		}
		for (STATE s1 : succSet1) {
			for (STATE s2 : succSet2) {
				STATE ps = getProductState(s1, s2);
				m_result.addCallTransition(state, symbol, ps);
				if (!m_visited.contains(ps)) {
					m_visited.add(ps);
					m_toprocess.add(ps);
				}
			}
		}
	}

	
	private void addReturns(STATE ps, 
							STATE fst,STATE snd, 
							LETTER symbol,
							Iterable<STATE> linPredSet1,
							Iterable<STATE> linPredSet2) {
		assert (linPredSet1 != null);
		if (linPredSet2 == null) {
			return;
		}
		for (STATE s1linPred : linPredSet1) {
			for (STATE s2linPred : linPredSet2) {
//				IState<LETTER,STATE> psLinPred = getProductState(s1linPred, s2linPred);
				Iterable<STATE> succSet1 = fstOperand.succReturn(fst, s1linPred, symbol);
				Iterable<STATE> succSet2 = sndOperand.succReturn(snd, s2linPred, symbol);
				addReturnTransitionIfNecessary(ps, s1linPred, s2linPred, symbol, succSet1, succSet2);
				}
			}
		}

	private void addReturnTransitionIfNecessary(STATE ps,
									 STATE s1linPred,
									 STATE s2linPred,
									 LETTER symbol,
									 Iterable<STATE> succSet1,
									 Iterable<STATE> succSet2) {
		for (STATE s1 : succSet1) {
			for (STATE s2 : succSet2) {
				STATE psSucc = getProductState(s1, s2);
				enqueueReturnTransition(s1linPred,s2linPred, new ReturnTransition(ps,symbol,psSucc));
//				m_result.addReturnTransition(ps, psLinPred,symbol, psSucc);
				if (!m_visited.contains(psSucc)) {
					m_visited.add(ps);
					m_toprocess.add(psSucc);
				}
			}
		}
	}
	
	private void enqueueReturnTransition(STATE s1linPred,
			STATE s2linPred, ReturnTransition returnTransition) {
		Map<STATE, Set<ReturnTransition>> s2linPredToReturnTransition = m_ReturnTransitionQueue.get(s1linPred);
		if (s2linPredToReturnTransition == null) {
			s2linPredToReturnTransition = new HashMap<STATE, Set<ReturnTransition>>();
			m_ReturnTransitionQueue.put(s1linPred, s2linPredToReturnTransition);
		}
		Set<ReturnTransition> returnTransitions = s2linPredToReturnTransition.get(s2linPred);
		if (returnTransitions == null) {
			returnTransitions = new HashSet<ReturnTransition>();
			s2linPredToReturnTransition.put(s2linPred, returnTransitions);
		}
		returnTransitions.add(returnTransition);
	}

	private class ReturnTransition {
		private final STATE pred;
		private final LETTER symbol;
		private final STATE succ;
		
		public ReturnTransition(STATE pred, LETTER symbol,
				STATE succ) {
			this.pred = pred;
			this.symbol = symbol;
			this.succ = succ;
		}
		/**
		 * @return the pred
		 */
		public STATE getPred() {
			return pred;
		}
		/**
		 * @return the symbol
		 */
		public LETTER getSymbol() {
			return symbol;
		}
		/**
		 * @return the succ
		 */
		public STATE getSucc() {
			return succ;
		}
		

	}

	@Override
	public boolean checkResult(StateFactory<STATE> stateFactory)
			throws AutomataLibraryException {
		m_Logger.warn("Correctness of IntersectNodd not checked at the moment.");
		return true;
	}
}
