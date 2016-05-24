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

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonOldApi;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;


/**
 * Given a PetriNet, constructs a finite Automaton that recognizes the same
 * language.
 * @author heizmann@informatik.uni-freiburg.de
 *
 * @param <LETTER> Symbol
 * @param <STATE> Content
 */
public class ConcurrentProduct<LETTER,STATE> {
	
	private final AutomataLibraryServices mServices;
	
	private final ILogger mLogger;
	
	private final boolean mConcurrentPrefixProduct;

	private final INestedWordAutomatonOldApi<LETTER,STATE> mNwa1;
	private final INestedWordAutomatonOldApi<LETTER,STATE> mNwa2;
	private final NestedWordAutomaton<LETTER,STATE> mResult;
	
	/**
	 * List of state pairs from the input automata for which
	 * <ul>
	 * <li> there has already been a state in the result constructed
	 * <li> outgoing transitions of this state have not yet been constructed.
	 * </ul>
 	 */
	private final StatePairQueue mWorklist = new StatePairQueue();
	
	/**
	 * Map from state pairs of the input automata to corresponding states
	 * in the result automaton. 
	 */
	private final StatePair2StateMap inputPair2resultState = 
		new StatePair2StateMap();
	

	/**
	 * Common symbols of the Alphabets of the input automata.
	 */
	private final HashSet<LETTER> mSynchronizationAlphabet;

	private final StateFactory<STATE> mContentFactory;
	

	

	
	
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
			if (mConcurrentPrefixProduct) {
				isFinal = mNwa1.isFinal(state1) || mNwa2.isFinal(state2);
			}
			else {			
				isFinal = mNwa1.isFinal(state1) && mNwa2.isFinal(state2);
			}
			STATE content1 = state1;
			STATE content2 = state2;
			state = mContentFactory.getContentOnConcurrentProduct(
															content1,content2);
			mResult.addState(isInitial, isFinal, state);
			inputPair2resultState.put(state1,state2,state);
			mWorklist.enqueue(state1, state2);
		}
		return state;
	}
	

	
	
	private void constructOutgoingTransitions(STATE state1,	STATE state2) {
		STATE state = getState(state1,state2,false);
		HashSet<LETTER> commonOutSymbols = 
				new HashSet<LETTER>(mNwa1.lettersInternal(state1));
		commonOutSymbols.retainAll(mNwa2.lettersInternal(state2));
		HashSet<LETTER> state1OnlySymbols = 
				new HashSet<LETTER>(mNwa1.lettersInternal(state1));
		state1OnlySymbols.removeAll(mSynchronizationAlphabet);
		HashSet<LETTER> state2OnlySymbols = 
				new HashSet<LETTER>(mNwa2.lettersInternal(state2));
		state2OnlySymbols.removeAll(mSynchronizationAlphabet);
		
		for (LETTER symbol : commonOutSymbols) {
			Iterable<STATE> succ1s = mNwa1.succInternal(state1, symbol);
			Iterable<STATE> succ2s = mNwa2.succInternal(state2, symbol);
			for (STATE succ1 : succ1s) {
				for (STATE succ2 : succ2s) {
					STATE succ = getState(succ1, succ2, false);
					mResult.addInternalTransition(state, symbol, succ);
				}
			}
		}
		
		for (LETTER symbol : state1OnlySymbols) {
			Iterable<STATE> succ1s = mNwa1.succInternal(state1, symbol);
			for (STATE succ1 : succ1s) {
					STATE succ = getState(succ1, state2, false);
					mResult.addInternalTransition(state, symbol, succ);
			}
		}
		
		for (LETTER symbol : state2OnlySymbols) {
			Iterable<STATE> succ2s = mNwa2.succInternal(state2, symbol);
			for (STATE succ2 : succ2s) {
					STATE succ = getState(state1, succ2, false);
					mResult.addInternalTransition(state, symbol, succ);
			}
		}

		
	}

	public void constructInitialStates() {
		for (STATE state1 : mNwa1.getInitialStates()) {
			for (STATE state2 : mNwa2.getInitialStates()) {
				getState(state1, state2, true);
			}
		}
	}
	
	
	public ConcurrentProduct(AutomataLibraryServices services, 
			INestedWordAutomatonOldApi<LETTER,STATE> nwa1,
			INestedWordAutomatonOldApi<LETTER,STATE> nwa2, boolean concurrentPrefixProduct) {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		mConcurrentPrefixProduct = concurrentPrefixProduct;
		mNwa1 = nwa1;
		mNwa2 = nwa2;
		mContentFactory = nwa1.getStateFactory();
//FIXME
//		if (mContentFactory != nwa2.getContentFactory()) {
//			throw new IllegalArgumentException("Both NWAs have to use" +
//					"same ContentFactory");
//		}
		
		if (!nwa1.getCallAlphabet().isEmpty() ||
				!nwa1.getReturnAlphabet().isEmpty() ||
				!nwa2.getCallAlphabet().isEmpty() ||
				!nwa2.getReturnAlphabet().isEmpty()) {
			mLogger.warn("Call alphabet and return alphabet are ignored.");
		}
		mSynchronizationAlphabet = new HashSet<LETTER>(nwa1.getInternalAlphabet());
		mSynchronizationAlphabet.retainAll(nwa2.getInternalAlphabet());
		Set<LETTER> commonAlphabet = new HashSet<LETTER>(nwa1.getInternalAlphabet());
		commonAlphabet.addAll(nwa2.getInternalAlphabet());
		mResult = new NestedWordAutomaton<LETTER,STATE>(
				mServices, commonAlphabet,
									 new HashSet<LETTER>(0),
									 new HashSet<LETTER>(0),
									 mContentFactory);
		constructInitialStates();
		while (!mWorklist.isEmpty()) {
			mWorklist.dequeuePair();
			STATE state1 = mWorklist.getDequeuedPairFst();
			STATE state2 = mWorklist.getDequeuedPairSnd();
			constructOutgoingTransitions(state1,state2);
		}
	}

	public INestedWordAutomatonOldApi<LETTER,STATE> getResult() {
		return mResult;
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
		Map<STATE,Set<STATE>> mQueue =
			new HashMap<STATE,Set<STATE>>();
		
		STATE mDequeuedPairFst;
		STATE mDequeuedPairSnd;
		
		public void enqueue(STATE state1, STATE state2) {
			Set<STATE> secondComponets = mQueue.get(state1);
			if (secondComponets == null) {
				secondComponets = new HashSet<STATE>();
				mQueue.put(state1,secondComponets);
			}
			secondComponets.add(state2);
		}
		
		public void dequeuePair() {
			assert (mDequeuedPairFst == null && mDequeuedPairSnd == null) : 
				"Results from last dequeue not yet collected!";
			Iterator<STATE> it1 = mQueue.keySet().iterator();
			mDequeuedPairFst = it1.next();
			assert (mQueue.get(mDequeuedPairFst) !=  null);
			Set<STATE> secondComponets = mQueue.get(mDequeuedPairFst);
			assert (secondComponets != null);
			Iterator<STATE> it2 = secondComponets.iterator();
			mDequeuedPairSnd = it2.next();
			
			secondComponets.remove(mDequeuedPairSnd);
			if (secondComponets.isEmpty()) {
				mQueue.remove(mDequeuedPairFst);
			}
		}
		
		public STATE getDequeuedPairFst() {
			assert (mDequeuedPairFst != null) : "No pair dequeued";
			STATE result = mDequeuedPairFst;
			mDequeuedPairFst = null;
			return result;
		}

		public STATE getDequeuedPairSnd() {
			assert (mDequeuedPairSnd != null) : "No pair dequeued";
			STATE result = mDequeuedPairSnd;
			mDequeuedPairSnd = null;
			return result;
		}

		public boolean isEmpty() {
			return mQueue.isEmpty();
		}
	}

}
