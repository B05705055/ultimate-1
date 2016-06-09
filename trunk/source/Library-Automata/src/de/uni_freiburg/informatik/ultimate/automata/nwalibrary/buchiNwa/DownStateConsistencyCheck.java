/*
 * Copyright (C) 2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.buchiNwa;

import java.util.HashSet;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.IDoubleDeckerAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingReturnTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingReturnTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.SummaryReturnTransition;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;


/**
 * Check if down states of an automaton are stored consistent.
 * This operation is only useful for debugging.
 * @author heizmann@informatik.uni-freiburg.de
 */
public class DownStateConsistencyCheck<LETTER, STATE> implements IOperation<LETTER, STATE> {
	
	private final IDoubleDeckerAutomaton<LETTER, STATE> mOperand;
	private final boolean mResult;
	private final AutomataLibraryServices mServices;
	private final ILogger mLogger;
	
	public DownStateConsistencyCheck(AutomataLibraryServices services, 
			IDoubleDeckerAutomaton<LETTER, STATE> nwa) throws AutomataOperationCanceledException {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		mOperand = nwa;
		mResult = consistentForAll();
	}
	
	
	@Override
	public String operationName() {
		return "DownStateConsistencyCheck";
	}

	@Override
	public String startMessage() {
		return "Start " + operationName() + " Operand " + 
			mOperand.sizeInformation();
	}
	
	
	@Override
	public String exitMessage() {
		return "Finished " + operationName() + " Result " + 
				mResult;
	}

	@Override
	public Boolean getResult() {
		return mResult;
	}
	
	public boolean consistentForAll() throws AutomataOperationCanceledException {
		boolean result = true;
		result &= consistentForInitials();
		for (final STATE state : mOperand.getStates()) {
			if (!mServices.getProgressMonitorService().continueProcessing()) {
				throw new AutomataOperationCanceledException(this.getClass());
			}
			result &= consistentForState(state);
		}
		return result;
	}
	
	private boolean consistentForInitials() {
		boolean result = true;
		for (final STATE state : mOperand.getInitialStates()) {
			final Set<STATE> downStates = mOperand.getDownStates(state);
			result &= downStates.contains(mOperand.getEmptyStackState());
		}
		assert result : "down states inconsistent";
		return result;
	}

	private boolean consistentForState(STATE state) {
		boolean result = true;
		final Set<STATE> downStates = mOperand.getDownStates(state);
		result &= getIsComparison(state, downStates);
		result &= checkIfDownStatesArePassedToSuccessors(state, downStates);
		result &= checkIfEachDownStateIsJustified(state, downStates);
		assert result : "down states inconsistent";
		return result;
	}
	
	private boolean checkIfEachDownStateIsJustified(STATE state, Set<STATE> downStates) {
		downStates = new HashSet<STATE>(downStates);
		for (final IncomingInternalTransition<LETTER, STATE> t : mOperand.internalPredecessors(state)) {
			final Set<STATE> preDown = mOperand.getDownStates(t.getPred());
			downStates.removeAll(preDown);
		}

		for (final IncomingCallTransition<LETTER, STATE> t : mOperand.callPredecessors(state)) {
			downStates.remove(t.getPred());
		}

		for (final IncomingReturnTransition<LETTER, STATE> t : mOperand.returnPredecessors(state)) {
			final Set<STATE> predDownStates = mOperand.getDownStates(t.getLinPred());
			if (predDownStates.contains(t.getHierPred())) {
				final Set<STATE> hierDownStates = mOperand.getDownStates(t.getHierPred());
				downStates.removeAll(hierDownStates);
			} else {
				throw new AssertionError("unreachable return");
			}
		}
		if (mOperand.isInitial(state)) {
			downStates.remove(mOperand.getEmptyStackState());
		}
		if (!downStates.isEmpty()) {
			mLogger.warn("State " + state + " has unjustified down states " + downStates );
		}
		return downStates.isEmpty();
	}

	private boolean checkIfDownStatesArePassedToSuccessors(STATE state,
			Set<STATE> downStates) {
		boolean result = true;
		for (final OutgoingInternalTransition<LETTER, STATE> t : mOperand.internalSuccessors(state)) {
			final Set<STATE> succDownStates = mOperand.getDownStates(t.getSucc());
			result &= succDownStates.containsAll(downStates);
			assert result : "down states inconsistent";
		}
		for (final OutgoingCallTransition<LETTER, STATE> t : mOperand.callSuccessors(state)) {
			final Set<STATE> succDownStates = mOperand.getDownStates(t.getSucc());
			result &= succDownStates.contains(state);
			assert result : "down states inconsistent";
		}
		for (final OutgoingReturnTransition<LETTER, STATE> t : mOperand.returnSuccessors(state)) {
			final Set<STATE> succDownStates = mOperand.getDownStates(t.getSucc());
			final Set<STATE> hierDownStates = mOperand.getDownStates(t.getHierPred());
			if (downStates.contains(t.getHierPred())) {
				result &= succDownStates.containsAll(hierDownStates);
				assert result : "down states inconsistent";
			} else {
				// nothing to check, we cannot take this transition
			}
		}
		for (final SummaryReturnTransition<LETTER, STATE> t : mOperand.returnSummarySuccessor(state)) {
			final Set<STATE> succDownStates = mOperand.getDownStates(t.getSucc());
			result &= succDownStates.containsAll(downStates);
			assert result : "down states inconsistent";
		}
		return result;
	}

	/**
	 * Check if {@link IDoubleDeckerAutomaton#getDownStates(Object)} and 
	 * {@link IDoubleDeckerAutomaton#isDoubleDecker(Object, Object)} are
	 * consistent.
	 */
	private boolean getIsComparison(STATE state, Set<STATE> downStates) {
		return getIsComparison1(state, downStates) 
				&& getIsComparison2(state, downStates);
	}

	
	/**
	 * Check if doubleDeckers claimed by 
	 * {@link IDoubleDeckerAutomaton#isDoubleDecker(Object, Object)}
	 * are a superset of the doubleDeckers claimed by
	 * {@link IDoubleDeckerAutomaton#getDownStates(Object)}
	 */
	private boolean getIsComparison1(STATE state, Set<STATE> downStates) {
		boolean result = true;
		for (final STATE down : downStates) {
			result &= mOperand.isDoubleDecker(state, down);
		}
		return result;
	}
	
	/**
	 * Check if doubleDeckers claimed by 
	 * {@link IDoubleDeckerAutomaton#getDownStates(Object)}
	 * are a superset of the doubleDeckers claimed by
	 * {@link IDoubleDeckerAutomaton#isDoubleDecker(Object, Object)}
	 * This check is expensive, because we have to iterate over all states.
	 * 
	 */
	private boolean getIsComparison2(STATE state, Set<STATE> downStates) {
		boolean result = true;
		for (final STATE down : mOperand.getStates()) {
			if (mOperand.isDoubleDecker(state, down)) {
				result &= downStates.contains(down);
			}
		}
		return result;
	}



	@Override
	public boolean checkResult(StateFactory<STATE> stateFactory)
			throws AutomataLibraryException {
		// I don't know a useful check
		return true;
	}

}
