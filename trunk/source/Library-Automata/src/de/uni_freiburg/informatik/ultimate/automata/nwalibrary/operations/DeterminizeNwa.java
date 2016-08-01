/*
 * Copyright (C) 2013-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonSimple;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.DeterminizedState;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingReturnTransition;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;

public class DeterminizeNwa<LETTER, STATE> implements INestedWordAutomatonSimple<LETTER, STATE> {
	
	private final AutomataLibraryServices mServices;
	private final ILogger mLogger;
	
	private final INestedWordAutomatonSimple<LETTER, STATE> mOperand;
	private final NestedWordAutomaton<LETTER, STATE> mCache;
	private final IStateDeterminizer<LETTER, STATE> mStateDeterminizer;
	private final StateFactory<STATE> mStateFactory;
	private final Set<STATE> mPredefinedInitials;
	private final boolean mMakeAutomatonTotal;
	
	private final Map<STATE,DeterminizedState<LETTER, STATE>> mres2det =
			new HashMap<STATE,DeterminizedState<LETTER, STATE>>();
	private final Map<DeterminizedState<LETTER, STATE>, STATE> mdet2res =
			new HashMap<DeterminizedState<LETTER, STATE>, STATE>();
	
	public DeterminizeNwa(final AutomataLibraryServices services,
			final INestedWordAutomatonSimple<LETTER, STATE> operand, 
			final IStateDeterminizer<LETTER, STATE> stateDeterminizer, 
			final StateFactory<STATE> sf, final Set<STATE> predefinedInitials,
			final boolean makeAutomatonTotal) {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		mOperand = operand;
		mStateDeterminizer = stateDeterminizer;
		mStateFactory = sf;
		mCache = new NestedWordAutomaton<LETTER, STATE>(mServices, operand.getInternalAlphabet(), 
				operand.getCallAlphabet(), operand.getReturnAlphabet(), sf);
		mPredefinedInitials = predefinedInitials;
		mMakeAutomatonTotal = makeAutomatonTotal;

	}
	
	
	public DeterminizeNwa(final AutomataLibraryServices services,
			final INestedWordAutomatonSimple<LETTER, STATE> operand, 
			final IStateDeterminizer<LETTER, STATE> stateDeterminizer, 
			final StateFactory<STATE> sf) {
		this(services, operand, stateDeterminizer, sf, null, false);
	}
	
	public boolean isTotal() {
		return mMakeAutomatonTotal;
	}
	
	private void constructInitialState() {
		if (mPredefinedInitials == null) {
			final DeterminizedState<LETTER, STATE> initialDet = 
					mStateDeterminizer.initialState();
			final STATE initialState = mStateDeterminizer.getState(initialDet);
			mdet2res.put(initialDet, initialState);
			mres2det.put(initialState, initialDet);
			mCache.addState(true, initialDet.containsFinal(), initialState);
		} else {
			// add singleton DoubleDecker for each initial state of operand
			for (final STATE initialOperand : mPredefinedInitials) {
				final DeterminizedState<LETTER,STATE> initialDet =
						new DeterminizedState<LETTER,STATE>(mOperand);
				initialDet.addPair(mOperand.getEmptyStackState(), initialOperand, mOperand);
				final STATE initialState = mStateDeterminizer.getState(initialDet);
				mdet2res.put(initialDet, initialState);
				mres2det.put(initialState, initialDet);
				mCache.addState(true, initialDet.containsFinal(), initialState);
			}
		}
	}
	
	private STATE getOrConstructState(final DeterminizedState<LETTER, STATE> detState) {
		STATE state = mdet2res.get(detState);
		if (state == null) {
			state = mStateDeterminizer.getState(detState);
			mdet2res.put(detState, state);
			mres2det.put(state, detState);
			mCache.addState(false, detState.containsFinal(), state);
		}
		return state;
	}
	
	
	
	public Collection<STATE> succInternal(final STATE state, final LETTER letter) {
		final Collection<STATE> succs = mCache.succInternal(state, letter);
		if (succs == null) {
			final DeterminizedState<LETTER, STATE> detState = mres2det.get(state);
			assert (detState != null);
			final DeterminizedState<LETTER, STATE> detSucc = 
					mStateDeterminizer.internalSuccessor(detState, letter);
			final STATE succ = getOrConstructState(detSucc);
			mCache.addInternalTransition(state, letter, succ);
		}
		return mCache.succInternal(state, letter);
	}

	public Collection<STATE> succCall(final STATE state, final LETTER letter) {
		final Collection<STATE> succs = mCache.succCall(state, letter);
		if (succs == null) {
			final DeterminizedState<LETTER, STATE> detState = mres2det.get(state);
			assert (detState != null);
			final DeterminizedState<LETTER, STATE> detSucc = 
					mStateDeterminizer.callSuccessor(detState, letter);
			final STATE succ = getOrConstructState(detSucc);
			mCache.addCallTransition(state, letter, succ);
		}
		return mCache.succCall(state, letter);
	}

	public Collection<STATE> succReturn(final STATE state, final STATE hier, final LETTER letter) {
		final Collection<STATE> succs = mCache.succReturn(state, hier, letter);
		if (succs == null) {
			final DeterminizedState<LETTER, STATE> detState = mres2det.get(state);
			assert (detState != null);
			final DeterminizedState<LETTER, STATE> detHier = mres2det.get(hier);
			assert (detHier != null);
			final DeterminizedState<LETTER, STATE> detSucc = 
					mStateDeterminizer.returnSuccessor(detState, detHier, letter);
			final STATE succ = getOrConstructState(detSucc);
			mCache.addReturnTransition(state, hier, letter, succ);
		}
		return mCache.succReturn(state, hier, letter);
	}
	
	
	
	
	
	@Override
	public Iterable<STATE> getInitialStates() {
		if (mCache.getInitialStates().isEmpty()) {
			constructInitialState();
		}
		return mCache.getInitialStates();
	}


	@Override
	public Set<LETTER> getInternalAlphabet() {
		return mOperand.getInternalAlphabet();
	}

	@Override
	public Set<LETTER> getCallAlphabet() {
		return mOperand.getCallAlphabet();
	}

	@Override
	public Set<LETTER> getReturnAlphabet() {
		return mOperand.getReturnAlphabet();
	}

	@Override
	public StateFactory<STATE> getStateFactory() {
		return mStateFactory;
	}
	
	@Override
	public boolean isInitial(final STATE state) {
		return mCache.isInitial(state);
	}

	@Override
	public boolean isFinal(final STATE state) {
		return mCache.isFinal(state);
	}



	@Override
	public STATE getEmptyStackState() {
		return mCache.getEmptyStackState();
	}

	@Override
	public Set<LETTER> lettersInternal(final STATE state) {
		if (mMakeAutomatonTotal) {
			return getInternalAlphabet();
		} else {
			final Set<LETTER> result = new HashSet<LETTER>();
			final DeterminizedState<LETTER, STATE> detState = mres2det.get(state);
			for (final STATE down : detState.getDownStates()) {
				for (final STATE up : detState.getUpStates(down)) {
					result.addAll(mOperand.lettersInternal(up));
				}
			}
			return result;
		}
	}

	@Override
	public Set<LETTER> lettersCall(final STATE state) {
		if (mMakeAutomatonTotal) {
			return getCallAlphabet();
		} else {
			final Set<LETTER> result = new HashSet<LETTER>();
			final DeterminizedState<LETTER, STATE> detState = mres2det.get(state);
			for (final STATE down : detState.getDownStates()) {
				for (final STATE up : detState.getUpStates(down)) {
					result.addAll(mOperand.lettersCall(up));
				}
			}
			return result;
		}
	}

	@Override
	public Set<LETTER> lettersReturn(final STATE state) {
		if (mMakeAutomatonTotal) {
			return getReturnAlphabet();
		} else {
			final Set<LETTER> result = new HashSet<LETTER>();
			final DeterminizedState<LETTER, STATE> detState = mres2det.get(state);
			for (final STATE down : detState.getDownStates()) {
				for (final STATE up : detState.getUpStates(down)) {
					result.addAll(mOperand.lettersReturn(up));
				}
			}
			return result;
		}
	}


	@Override
	public Iterable<OutgoingInternalTransition<LETTER, STATE>> internalSuccessors(
			final STATE state, final LETTER letter) {
		final Collection<STATE> succs = mCache.succInternal(state, letter);
		if (succs == null || succs.isEmpty()) {
			final DeterminizedState<LETTER, STATE> detState = mres2det.get(state);
			assert (detState != null);
			final DeterminizedState<LETTER, STATE> detSucc = 
					mStateDeterminizer.internalSuccessor(detState, letter);
			final STATE succ = getOrConstructState(detSucc);
			mCache.addInternalTransition(state, letter, succ);
		}
		return mCache.internalSuccessors(state, letter);
	}

	@Override
	public Iterable<OutgoingInternalTransition<LETTER, STATE>> internalSuccessors(
			final STATE state) {
		for (final LETTER letter : lettersInternal(state)) {
			internalSuccessors(state, letter);
		}
		return mCache.internalSuccessors(state);
	}

	@Override
	public Iterable<OutgoingCallTransition<LETTER, STATE>> callSuccessors(
			final STATE state, final LETTER letter) {
		final Collection<STATE> succs = mCache.succCall(state, letter);
		if (succs == null || succs.isEmpty()) {
			final DeterminizedState<LETTER, STATE> detState = mres2det.get(state);
			assert (detState != null);
			final DeterminizedState<LETTER, STATE> detSucc = 
					mStateDeterminizer.callSuccessor(detState, letter);
			final STATE succ = getOrConstructState(detSucc);
			mCache.addCallTransition(state, letter, succ);
		}
		return mCache.callSuccessors(state, letter);
	}

	@Override
	public Iterable<OutgoingCallTransition<LETTER, STATE>> callSuccessors(
			final STATE state) {
		for (final LETTER letter : lettersCall(state)) {
			callSuccessors(state, letter);
		}
		return mCache.callSuccessors(state);
	}



	@Override
	public Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessors(
			final STATE state, final STATE hier, final LETTER letter) {
		final Collection<STATE> succs = mCache.succReturn(state, hier, letter);
		if (succs == null || succs.isEmpty()) {
			final DeterminizedState<LETTER, STATE> detState = mres2det.get(state);
			assert (detState != null);
			final DeterminizedState<LETTER, STATE> detHier = mres2det.get(hier);
			assert (detHier != null);
			final DeterminizedState<LETTER, STATE> detSucc = 
					mStateDeterminizer.returnSuccessor(detState, detHier, letter);
			final STATE succ = getOrConstructState(detSucc);
			mCache.addReturnTransition(state, hier, letter, succ);
		}
		return mCache.returnSuccessors(state, hier, letter);
	}

	@Override
	public Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessorsGivenHier(
			final STATE state, final STATE hier) {
		for (final LETTER letter : lettersReturn(state)) {
			returnSuccessors(state, hier, letter);
		}
		return mCache.returnSuccessorsGivenHier(state, hier);
	}

	@Override
	public int size() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<LETTER> getAlphabet() {
		throw new UnsupportedOperationException();	}

	@Override
	public String sizeInformation() {
		return "size Information not available";
	}


}
