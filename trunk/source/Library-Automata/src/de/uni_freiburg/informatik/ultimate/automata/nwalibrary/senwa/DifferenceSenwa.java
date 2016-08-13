/*
 * Copyright (C) 2012-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.senwa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.AutomatonDefinitionPrinter;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.Senwa;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.IStateDeterminizer;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.IsIncluded;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.PowersetDeterminizer;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.StateDeterminizerCache;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.DeterminizedState;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.DifferenceSadd;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.DifferenceState;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.IOpWithDelayedDeadEndRemoval;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.senwa.SenwaWalker.ISuccessorVisitor;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingReturnTransition;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;

public class DifferenceSenwa<LETTER, STATE> implements 
								ISuccessorVisitor<LETTER, STATE>,
								IOperation<LETTER, STATE>,
								IOpWithDelayedDeadEndRemoval<LETTER, STATE>{
	
	private final AutomataLibraryServices mServices;
	private final ILogger mLogger;
		
	private final INestedWordAutomaton<LETTER,STATE> mMinuend;
	private final INestedWordAutomaton<LETTER,STATE> mSubtrahend;
	
	private final IStateDeterminizer<LETTER,STATE> mStateDeterminizer;
	
	private final StateFactory<STATE> mContentFactory;

	private final Senwa<LETTER, STATE> mSenwa;
	
	private final SenwaWalker<LETTER, STATE> mSenwaWalker;
	
	
	
	
	
	/**
	 * Maps a state in resulting automaton to the DifferenceState for which it
	 * was created.
	 */
	private final Map<STATE,DifferenceState<LETTER,STATE>> mResult2Operand = 
			new HashMap<STATE,DifferenceState<LETTER,STATE>>();
	
	/**
	 * Maps a DifferenceState and an entry state to its representative in the
	 * resulting automaton.
	 */
	private final Map<DifferenceState<LETTER,STATE>,Map<DifferenceState<LETTER,STATE>,STATE>> mEntry2Operand2Result = 
			new HashMap<DifferenceState<LETTER,STATE>,Map<DifferenceState<LETTER,STATE>,STATE>>();
	
	
	public DifferenceSenwa(
			final AutomataLibraryServices services,
			final INestedWordAutomaton<LETTER,STATE> minuend,
			final INestedWordAutomaton<LETTER,STATE> subtrahend)
					throws AutomataLibraryException {
		this(services, minuend, subtrahend,
				new PowersetDeterminizer<LETTER,STATE>(
						subtrahend, true, minuend.getStateFactory()),
				true);
	}
	
	
	public DifferenceSenwa(
			final AutomataLibraryServices services,
			final INestedWordAutomaton<LETTER,STATE> minuend,
			final INestedWordAutomaton<LETTER,STATE> subtrahend,
			final IStateDeterminizer<LETTER,STATE> stateDeterminizer,
			final boolean removeDeadEndsImmediately)
					throws AutomataLibraryException {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		mContentFactory = minuend.getStateFactory();
		
		this.mMinuend = (INestedWordAutomaton<LETTER, STATE>) minuend;
		this.mSubtrahend = (INestedWordAutomaton<LETTER, STATE>) subtrahend;
		mLogger.info(startMessage());
		
		
		this.mStateDeterminizer = new StateDeterminizerCache<LETTER, STATE>(
				stateDeterminizer); 
		
		mSenwa = new Senwa<LETTER, STATE>(mServices,
				minuend.getInternalAlphabet(), minuend.getCallAlphabet(), 
				minuend.getReturnAlphabet(), minuend.getStateFactory());
		mSenwaWalker = new SenwaWalker<LETTER, STATE>(mServices, mSenwa, this, removeDeadEndsImmediately);
		mLogger.info(exitMessage());
	}
	
	
	
	
	
	private STATE getOrConstructResultState(
			final DifferenceState<LETTER,STATE> diffEntry, 
			final DifferenceState<LETTER,STATE> diffState, final boolean isInitial) {
		assert mMinuend.getStates().contains(diffState.getMinuendState());
		assert mMinuend.getStates().contains(diffEntry.getMinuendState());
		Map<DifferenceState<LETTER,STATE>, STATE> op2res = mEntry2Operand2Result.get(diffEntry);
		if (op2res == null) {
			op2res = new HashMap<DifferenceState<LETTER,STATE>, STATE>();
			mEntry2Operand2Result.put(diffEntry, op2res);
		}
		STATE resState = op2res.get(diffState);
		if (resState == null) {
			
			resState = mContentFactory.senwa(
					diffEntry.getState(mContentFactory, mStateDeterminizer), 
					diffState.getState(mContentFactory, mStateDeterminizer));
			op2res.put(diffState, resState);
			mResult2Operand.put(resState, diffState);
			final STATE resEntry = op2res.get(diffEntry);
			assert resEntry != null;
			mSenwa.addState(resState, isInitial, diffState.isFinal(), resEntry);
		}
		return resState;
	}
	
	private DifferenceState<LETTER,STATE> getOperandState(final STATE resState) {
		assert mSenwa.getStates().contains(resState);
		final DifferenceState<LETTER,STATE> opState = mResult2Operand.get(resState);
		assert opState != null;
		return opState;
	}
	

	@Override
	public Iterable<STATE> getInitialStates() {
		
		final ArrayList<STATE> resInitials = 
				new ArrayList<STATE>(mSubtrahend.getInitialStates().size());
		final DeterminizedState<LETTER,STATE> detState = mStateDeterminizer.initialState();
		for (final STATE minuState : mMinuend.getInitialStates()) {
			final boolean isFinal = mMinuend.isFinal(minuState) &&
											!detState.containsFinal();
			final DifferenceState<LETTER,STATE> diffState = 
				new DifferenceState<LETTER,STATE>(minuState, detState, isFinal);
			final STATE resState = getOrConstructResultState(diffState, diffState, true); 
			resInitials.add(resState);
		}
		return resInitials;
	}

	@Override
	public Iterable<STATE> visitAndGetInternalSuccessors(final STATE resState) {
		final STATE resEntry = mSenwa.getEntry(resState);
		final DifferenceState<LETTER,STATE> diffEntry = getOperandState(resEntry);
		final Set<STATE> resSuccs = new HashSet<STATE>();
		final DifferenceState<LETTER,STATE> diffState = getOperandState(resState);
		final STATE minuState = diffState.getMinuendState();
		final DeterminizedState<LETTER,STATE> subtrState = diffState.getSubtrahendDeterminizedState();
		for (final LETTER letter : mMinuend.lettersInternal(minuState)) {
			for (final OutgoingInternalTransition<LETTER, STATE> trans :
					mMinuend.internalSuccessors(minuState, letter)) {
				final STATE minuSucc = trans.getSucc();
				final DeterminizedState<LETTER, STATE> subtrSucc = mStateDeterminizer.internalSuccessor(subtrState, letter);
				final boolean isFinal = mMinuend.isFinal(minuSucc) &&
						!subtrSucc.containsFinal();
				final DifferenceState<LETTER, STATE> diffSucc = new DifferenceState<LETTER,STATE>(minuSucc, subtrSucc, isFinal);		
				
				final STATE resSucc = getOrConstructResultState(diffEntry, diffSucc, false);
				resSuccs.add(resSucc);
				mSenwa.addInternalTransition(resState, letter, resSucc);
			}
		}
		return resSuccs;
	}

	@Override
	public Iterable<STATE> visitAndGetCallSuccessors(final STATE resState) {
		final Set<STATE> resSuccs = new HashSet<STATE>();
		final DifferenceState<LETTER,STATE> diffState = getOperandState(resState);
		final STATE minuState = diffState.getMinuendState();
		final DeterminizedState<LETTER,STATE> subtrState = 
									diffState.getSubtrahendDeterminizedState();
		for (final LETTER letter : mMinuend.lettersCall(minuState)) {
			for (final OutgoingCallTransition<LETTER, STATE> trans :
					mMinuend.callSuccessors(minuState, letter)) {
				final STATE minuSucc = trans.getSucc();
				final DeterminizedState<LETTER, STATE> subtrSucc = 
						mStateDeterminizer.callSuccessor(subtrState, letter);
				final boolean isFinal = mMinuend.isFinal(minuSucc) &&
						!subtrSucc.containsFinal();
				final DifferenceState<LETTER, STATE> diffSucc = new 
						DifferenceState<LETTER,STATE>(minuSucc, subtrSucc, isFinal);		
				final STATE resSucc = getOrConstructResultState(diffSucc, diffSucc, false);
				resSuccs.add(resSucc);
				mSenwa.addCallTransition(resState, letter, resSucc);
			}
		}
		return resSuccs;
	}

	@Override
	public Iterable<STATE> visitAndGetReturnSuccessors(final STATE resState,
			final STATE resHier) {
		final Set<STATE> resSuccs = new HashSet<STATE>();
		final DifferenceState<LETTER,STATE> diffState = getOperandState(resState);
		final STATE minuState = diffState.getMinuendState();
		final DeterminizedState<LETTER,STATE> subtrState = 
									diffState.getSubtrahendDeterminizedState();
		final DifferenceState<LETTER,STATE> diffHier = getOperandState(resHier);
		final STATE minuHier = diffHier.getMinuendState();
		final DeterminizedState<LETTER,STATE> subtrHier = 
									diffHier.getSubtrahendDeterminizedState();
		final STATE resHierEntry = mSenwa.getEntry(resHier);
		final DifferenceState<LETTER,STATE> diffHierEntry = getOperandState(resHierEntry);

		for (final LETTER letter : mMinuend.lettersReturn(minuState)) {
			for (final OutgoingReturnTransition<LETTER, STATE> trans :
					mMinuend.returnSuccessors(minuState, minuHier, letter)) {
				final STATE minuSucc = trans.getSucc();
				final DeterminizedState<LETTER, STATE> subtrSucc = 
						mStateDeterminizer.returnSuccessor(subtrState, subtrHier, letter);
				final boolean isFinal = mMinuend.isFinal(minuSucc) &&
						!subtrSucc.containsFinal();
				final DifferenceState<LETTER, STATE> diffSucc = new 
						DifferenceState<LETTER,STATE>(minuSucc, subtrSucc, isFinal);		
				final STATE resSucc = getOrConstructResultState(diffHierEntry, diffSucc, false);
				resSuccs.add(resSucc);
				mSenwa.addReturnTransition(resState, resHier, letter, resSucc);
			}
		}
		return resSuccs;
	}
	
	@Override
	public Senwa<LETTER,STATE> getResult() throws AutomataOperationCanceledException {
		return mSenwa;
	}
	
	
//FIXME: Remove this
	public boolean removeStatesThatCanNotReachFinal(
			final boolean computeRemovedDoubleDeckersAndCallSuccessors) {
		return mSenwaWalker.removeStatesThatCanNotReachFinal(
								computeRemovedDoubleDeckersAndCallSuccessors);
	}

	
	@Override
	public long getDeadEndRemovalTime() {
		return mSenwaWalker.getDeadEndRemovalTime();
	}

	@Override
	public Iterable<UpDownEntry<STATE>> getRemovedUpDownEntry() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeDeadEnds() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public String operationName() {
		return "differenceSenwa";
	}
	
	@Override
	public String startMessage() {
			return "Start " + operationName() + ". Minuend " + 
			mMinuend.sizeInformation() + ". Subtrahend " + 
			mSubtrahend.sizeInformation();	
	}

	@Override
	public String exitMessage() {
		return "Finished " + operationName() + " Result " + 
		mSenwa.sizeInformation() + ". Max degree of Nondeterminism is " + 
		mStateDeterminizer.getMaxDegreeOfNondeterminism();
	}

	@Override
	public boolean checkResult(final StateFactory<STATE> stateFactory)
			throws AutomataLibraryException {
		boolean correct = true;
		if (mStateDeterminizer instanceof PowersetDeterminizer) {
			mLogger.info("Start testing correctness of " + operationName());

			final INestedWordAutomaton<LETTER,STATE> resultSadd =
					(new DifferenceSadd<LETTER,STATE>(mServices, stateFactory, mMinuend, mSubtrahend)).getResult();
			correct &= new IsIncluded<>(mServices, stateFactory, resultSadd, mSenwa).getResult();
			correct &= new IsIncluded<>(mServices, stateFactory, mSenwa, resultSadd).getResult();
			if (!correct) {
				AutomatonDefinitionPrinter.writeToFileIfPreferred(mServices,
						operationName() + "Failed", "language is different",
						mMinuend,mSubtrahend);
			}
			mLogger.info("Finished testing correctness of " + operationName());
		} else {
			mLogger.warn("Unable to test correctness if state determinzier is not the PowersetDeterminizer.");
		}
		return correct;
	}

}
