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
package de.uni_freiburg.informatik.ultimate.automata.petrinet.julian;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.ResultChecker;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonOldApi;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.DifferenceDD;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.ITransition;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.PetriNet2FiniteAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.Place;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;

public class DifferenceBlackAndWhite<S,C> implements IOperation<S,C> {
	
	private final AutomataLibraryServices mServices;
	
	@Override
	public String operationName() {
		return "differenceBlackAndWhite";
	}
	
	private final ILogger mLogger;
	
	
	
	private final PetriNetJulian<S,C> mNet;
	private final NestedWordAutomaton<S,C> mNwa;
	private final StateFactory<C> mContentFactory;
	
	PetriNetJulian<S,C> mResult;
	
	private final Map<Place<S,C>,Place<S,C>> mOldPlace2NewPlace =
		new HashMap<Place<S,C>,Place<S,C>>();
	
	private final Map<S,Set<C>> mSelfloop = 
		new HashMap<S,Set<C>>();
	private final Map<S,Set<C>> mStateChanger = 
		new HashMap<S,Set<C>>();
	
	private final Map<C,Place<S,C>> mWhitePlace =
		new HashMap<C,Place<S,C>>();
	
	private final Map<C,Place<S,C>> mBlackPlace =
		new HashMap<C,Place<S,C>>();
	
	

	
	@Override
	public String startMessage() {
		return "Start " + operationName() +
			"First Operand " + mNet.sizeInformation() +
			"Second Operand " + mNwa.sizeInformation();
	}
	
	@Override
	public String exitMessage() {
		return "Finished " + operationName() +
			" Result " + mResult.sizeInformation();
	}
	
	public DifferenceBlackAndWhite(AutomataLibraryServices services,
									PetriNetJulian<S,C> net, 
								   NestedWordAutomaton<S,C> nwa) {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		mNet = net;
		mNwa = nwa;
		mContentFactory = net.getStateFactory();
		
		mLogger.info(startMessage());
		
		final Collection<S> netAlphabet = new HashSet<S>(net.getAlphabet());
		final Collection<S> nwaAlpahbet = new HashSet<S>(nwa.getInternalAlphabet());
		if (!netAlphabet.equals(nwaAlpahbet)) {
			throw new IllegalArgumentException("net and nwa must use same" +
					" alphabet");
		}
		if (nwa.getInitialStates().size() != 1) {
			throw new UnsupportedOperationException("DifferenceBlackAndWhite" +
					" needs an automaton with exactly one inital state");
		}
		if(!nwa.finalIsTrap()) {
			throw new UnsupportedOperationException("Second operand has to" +
					"closed under concatenation with sigma star");
			//otherwise the result won't be the intersection of languages
		}
		final C nwaInitialState = nwa.getInitialStates().iterator().next();
		classifySymbols();
//		mSymbol2AutomatonTransition = createSymbol2AutomatonTransitionMap();
		if(nwa.isFinal(nwaInitialState)) {
			// case where nwa accepts everything. Result will be a net that
			// accepts the empty language
			mResult = new PetriNetJulian<S,C>(mServices, mNet.getAlphabet(),
					mNet.getStateFactory(),
					true);
			final C sinkContent = mContentFactory.createSinkStateContent();
			mResult.addPlace(sinkContent, true, false);
		}
		else {		
			copyNet_StatesOnly();
			addBlackAndWhitePlaces();
			addTransitions();
		}
		mLogger.info(exitMessage());
	}
	
	
	
	private void classifySymbols() {
		for (final S symbol : mNwa.getInternalAlphabet()) {
			final HashSet<C> selfloopStates = new HashSet<C>();
			final HashSet<C> changerStates = new HashSet<C>();
			for (final C state : mNwa.getStates()) {
				if (mNwa.isFinal(state)) {
					// we do not consider accepting states since they
					// do not occur in the result anyway
					continue;
				}
				final Collection<C> successors = mNwa.succInternal(state, symbol);
				if (successors.isEmpty()) {
					continue;
				}
				if (successors.size() > 1) {
					throw new IllegalArgumentException(
									"Only deterministic automata supported");
				}
				if (successors.contains(state)) {
					selfloopStates.add(state);
				}
				else {
					changerStates.add(state);
				}
			}
			mSelfloop.put(symbol,selfloopStates);
			mStateChanger.put(symbol, changerStates);
			mLogger.debug(symbol + " " + selfloopStates.size() + 
				" times selfloop " + changerStates.size() + " times changer");
		}
	}
	
//	private Map<S,Set<NestedWordAutomaton<S,C>.InternalTransition>> createSymbol2AutomatonTransitionMap() {
//		Map<S,Set<NestedWordAutomaton<S,C>.InternalTransition>> result = 
//			new HashMap<S,Set<NestedWordAutomaton<S,C>.InternalTransition>>();
//		for (NestedWordAutomaton<S,C>.InternalTransition trans : mNwa.getInternalTransitions()) {
//			S symbol = trans.getSymbol();
//			Set<NestedWordAutomaton<S,C>.InternalTransition> transitions = 
//				result.get(symbol);
//			if (transitions == null) {
//				transitions = new HashSet<NestedWordAutomaton<S,C>.InternalTransition>();
//				result.put(symbol,transitions);
//			}
//			transitions.add(trans);
//		}
//		return result;
//	}
	
	
//	private Map<S,Set<ITransition<S,C>>> createSymbol2TransitionMap(
//														PetriNetJulian<S,C> net) {
//		Map<S,Set<ITransition<S,C>>> result = 
//			new HashMap<S,Set<ITransition<S,C>>>();
//		for (S symbol : net.getAlphabet()) {
//			result.put(symbol, new HashSet<ITransition<S,C>>());
//		}
//		for (ITransition<S,C> transition : net.getTransitions()) {
//			result.get(transition.getSymbol()).add(transition);
//		}
//		return result;	
//	}
	
	
	private void copyNet_StatesOnly() {
		
		// difference black and white preserves the constantTokenAmount invariant
		final boolean constantTokenAmount = mNet.constantTokenAmount();
		mResult = new PetriNetJulian<S,C>(mServices, mNet.getAlphabet(),
											mNet.getStateFactory(),
											constantTokenAmount);
		
		for (final Place<S,C> oldPlace : mNet.getPlaces()) {
			final C content = oldPlace.getContent();
			final boolean isInitial = mNet.getInitialMarking().contains(oldPlace);
			final boolean isAccepting = mNet.getAcceptingPlaces().contains(oldPlace);
			final Place<S,C> newPlace = mResult.addPlace(content, isInitial, isAccepting);
			mOldPlace2NewPlace.put(oldPlace, newPlace);
		}
	}
	
	
	private void addBlackAndWhitePlaces() {
		for (final C state : mNwa.getStates()) {
			if (!mNwa.isFinal(state)) {
				final boolean isInitial = mNwa.getInitialStates().contains(state);
				final C stateContent = state;
				final C whiteContent = mContentFactory.getWhiteContent(stateContent);
				final Place<S,C> whitePlace = mResult.addPlace(whiteContent,isInitial,false);
				mWhitePlace.put(state,whitePlace);
				final C blackContent = mContentFactory.getBlackContent(stateContent);
				final Place<S,C> blackPlace = mResult.addPlace(blackContent,!isInitial,false);
				mBlackPlace.put(state,blackPlace);
			}
		}
	}
	
	private void addTransitions() {
		for (final ITransition<S, C> oldTrans : mNet.getTransitions()) {
			final S symbol = oldTrans.getSymbol();
			
			// A copy for each changer
			for (final C predState : mStateChanger.get(symbol)) {
				final Collection<C> succStates = mNwa.succInternal(predState, symbol); 
				assert (succStates.size() == 1);
				final C succState = succStates.iterator().next();	
				
				// omit transitions to final states
				if (mNwa.isFinal(succState)) {
					continue;
				}
				
				final Collection<Place<S,C>> predecessors = 
					new ArrayList<Place<S,C>>();
				for (final Place<S,C> oldPlace : oldTrans.getPredecessors()) {
					final Place<S,C> newPlace = mOldPlace2NewPlace.get(oldPlace);
					predecessors.add(newPlace);
				}
				assert(mWhitePlace.containsKey(predState));
				predecessors.add(mWhitePlace.get(predState));
				assert(mWhitePlace.containsKey(succState));
				predecessors.add(mBlackPlace.get(succState));
				
				final Collection<Place<S,C>> successors = 
					new ArrayList<Place<S,C>>();
				for (final Place<S,C> oldPlace : oldTrans.getSuccessors()) {
					final Place<S,C> newPlace = mOldPlace2NewPlace.get(oldPlace);
					successors.add(newPlace);
				}
				assert(mWhitePlace.containsKey(succState));
				successors.add(mWhitePlace.get(succState));
				assert(mBlackPlace.containsKey(predState));
				successors.add(mBlackPlace.get(predState));
				
				mResult.addTransition(oldTrans.getSymbol(), predecessors, successors);
			}
			
			// One copy for the selfloops
			if (!mSelfloop.isEmpty()) {
//				Collection<IState<S,C>> succStates = predState.getInternalSucc(symbol);
//				assert (succStates.size() == 1);
//				IState<S,C> succState = succStates.iterator().next();				
				final Collection<Place<S,C>> predecessors = 
					new ArrayList<Place<S,C>>();
				for (final Place<S,C> oldPlace : oldTrans.getPredecessors()) {
					final Place<S,C> newPlace = mOldPlace2NewPlace.get(oldPlace);
					predecessors.add(newPlace);
				}
//				predecessors.add(mWhitePlace.get(predState));
//				predecessors.add(mBlackPlace.get(succState));
				
				final Collection<Place<S,C>> successors = 
					new ArrayList<Place<S,C>>();
				for (final Place<S,C> oldPlace : oldTrans.getSuccessors()) {
					final Place<S,C> newPlace = mOldPlace2NewPlace.get(oldPlace);
					successors.add(newPlace);
				}
//				successors.add(mWhitePlace.get(succState));
//				successors.add(mBlackPlace.get(predState));
				
				for (final C state : mStateChanger.get(symbol)) {
					predecessors.add(mBlackPlace.get(state));
					successors.add(mBlackPlace.get(state));
				}
				
				mResult.addTransition(oldTrans.getSymbol(), predecessors, successors);
			}
		}
	}
		
		
	
		

	
//	private IState<S,C> getSuccessorState(IState<S,C> state, S symbol) {
//		Collection<IState<S, C>> successors = state.getInternalSucc(symbol);
//		if (successors.size() > 1) {
//			throw new IllegalArgumentException(
//							"Only deterministic automata supported");
//		}
//		for (IState<S,C> succ : successors) {
//			return succ;
//		}
//		return null;
//	}
	

	
	
	

	@Override
	public PetriNetJulian<S,C> getResult() throws AutomataLibraryException {
		assert (isPreSuccPlaceInNet(mResult));
		assert (isPreSuccTransitionInNet(mResult));
		return mResult;
	}
	
	
	
	private boolean isPreSuccPlaceInNet(PetriNetJulian<S,C> net) {
		for (final ITransition<S,C> trans : net.getTransitions()) {
			for (final Place<S,C> place : trans.getPredecessors()) {
				if(!net.getPlaces().contains(place)) {
					return false;
				}
			}
			for (final Place<S,C> place : trans.getSuccessors()) {
				if(!net.getPlaces().contains(place)) {
					return false;
				}
			}
		}
		return true;
	}
	
	
	
	private boolean isPreSuccTransitionInNet(PetriNetJulian<S,C> net) {
		for (final Place<S,C> place : net.getPlaces()) {
			for (final ITransition<S,C> trans : place.getPredecessors()) {
				if(!net.getTransitions().contains(trans)) {
					return false;
				}
			}
			for (final ITransition<S,C> trans : place.getSuccessors()) {
				if(!net.getTransitions().contains(trans)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean checkResult(StateFactory<C> stateFactory)
			throws AutomataLibraryException {
		mLogger.info("Testing correctness of differenceBlackAndWhite");

		final INestedWordAutomatonOldApi op1AsNwa = (new PetriNet2FiniteAutomaton(mServices, mNet)).getResult();
		final INestedWordAutomatonOldApi rcResult = (new DifferenceDD(mServices, stateFactory, op1AsNwa, mNwa)).getResult();
		final INestedWordAutomatonOldApi resultAsNwa = (new PetriNet2FiniteAutomaton(mServices, mResult)).getResult();
		boolean correct = true;
		correct &= (ResultChecker.nwaLanguageInclusion(mServices, resultAsNwa,rcResult,stateFactory) == null);
		correct &= (ResultChecker.nwaLanguageInclusion(mServices, rcResult,resultAsNwa,stateFactory) == null);

		mLogger.info("Finished testing correctness of differenceBlackAndWhite");
		return correct;
	}

}
