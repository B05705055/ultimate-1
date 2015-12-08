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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.ResultChecker;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonOldApi;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.IStateDeterminizer;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.PowersetDeterminizer;
import de.uni_freiburg.informatik.ultimate.core.services.model.IUltimateServiceProvider;


/**
 * Given two nondeterministic NWAs nwa_minuend and nwa_subtrahend a
 * DifferenceAutomatonBuilder can compute a NWA nwa_difference
 * such that nwa_difference accepts all words that are accepted by nwa_minuend
 * but not by Psi(nwa_subtrahend), i.e. 
 * L(nwa_difference) = L(nwa_minuend) \ L( Psi(nwa_subtrahend) ),
 * where Psi is a transformation of the automaton nwa_subtrahend that is defined
 * by an implementation of IStateDeterminizer.
 * 
 * 
 * @author heizmann@informatik.uni-freiburg.de
 *
 * @param <LETTER> Symbol. Type of the elements of the alphabet over which the
 * automata are defined. 
 * @param <STATE> Content. Type of the labels that are assigned to the states of
 * automata. In many cases you want to use String as STATE and your states are
 * labeled e.g. with "q0", "q1", ... 
 */
public class DifferenceSadd<LETTER,STATE> implements IOperation<LETTER,STATE> {
	
	private final IUltimateServiceProvider m_Services;
	private final Logger m_Logger;
	
	private final INestedWordAutomatonOldApi<LETTER,STATE> minuend;
	private final INestedWordAutomatonOldApi<LETTER,STATE> subtrahend;
	private final NestedWordAutomaton<LETTER,STATE> difference;
	
	private final IStateDeterminizer<LETTER,STATE> stateDeterminizer;
	
	/**
	 * Maps a DifferenceState to its representative in the resulting automaton.
	 */
	private Map<DifferenceState,STATE> diff2res =
		new HashMap<DifferenceState, STATE>();
	
	/**
	 * Maps a state in resulting automaton to the DifferenceState for which it
	 * was created.
	 */
	private final Map<STATE,DifferenceState> res2diff =
		new HashMap<STATE, DifferenceState>();
	
	/**
	 * Summary states of the resulting automaton that have been visited so far.
	 * If the summary state (<i>caller</i>,<i>present</i>) has been visited,
	 * <i>present</i> is contained in the range of <i>caller</i>.
	 */
	private final Map<STATE,Set<STATE>> visited = 
		new HashMap<STATE, Set<STATE>>();
	
	/**
	 * Summary states of the resulting automaton that still have to be
	 * processed.
	 */
	private final List<SummaryState<LETTER,STATE>> worklist = 
		new LinkedList<SummaryState<LETTER,STATE>>();
	
	
	/**
	 * Pairs of states (q,q') of the resulting automaton such that q' is
	 * reachable from q via a well-matched nested word in which the first
	 * position is a call position and the last position is a return position. 
	 */
	private Map<STATE,Set<STATE>> summary = 
		new HashMap<STATE, Set<STATE>>();
	
	private final STATE auxilliaryEmptyStackState;
	
	private final StateFactory<STATE> contentFactory;
	

	
	@Override
	public String operationName() {
		return "differenceSadd";
	}
	
	@Override
	public String startMessage() {
			return "Start " + operationName() + ". Minuend " + 
			minuend.sizeInformation() + ". Subtrahend " + 
			subtrahend.sizeInformation();	
	}

	@Override
	public String exitMessage() {
		return "Finished " + operationName() + " Result " + 
		difference.sizeInformation();
	}
	
	public INestedWordAutomatonOldApi<LETTER,STATE> getResult() {
		return difference;
	}
	
	public DifferenceSadd(
			IUltimateServiceProvider services,
			INestedWordAutomatonOldApi<LETTER,STATE> minuend,
			INestedWordAutomatonOldApi<LETTER,STATE> subtrahend,
			IStateDeterminizer<LETTER,STATE> stateDeterminizer) throws AutomataLibraryException {
		m_Services = services;
		m_Logger = m_Services.getLoggingService().getLogger(LibraryIdentifiers.s_LibraryID);
		contentFactory = minuend.getStateFactory();
		this.minuend = minuend;
		this.subtrahend = subtrahend;
		if (!NestedWordAutomaton.sameAlphabet(this.minuend, this.subtrahend)) {
			throw new AutomataLibraryException(this.getClass(), "Unable to apply operation to automata with different alphabets.");
		}
		this.stateDeterminizer = stateDeterminizer;
		m_Logger.info(startMessage());
		difference = new NestedWordAutomaton<LETTER,STATE>(
				m_Services, 
				minuend.getInternalAlphabet(),
				minuend.getCallAlphabet(),
				minuend.getReturnAlphabet(),
				minuend.getStateFactory());
		auxilliaryEmptyStackState = difference.getEmptyStackState();
		computeDifference();
		m_Logger.info(exitMessage());
	}
	
	/**
	 * Constructor where powerset determinizer is used.
	 * @param minuend
	 * @param subtrahend
	 * @throws AutomataLibraryException 
	 */	
	public DifferenceSadd(
			IUltimateServiceProvider services,
			StateFactory<STATE> stateFactory,
			INestedWordAutomatonOldApi<LETTER,STATE> minuend,
			INestedWordAutomatonOldApi<LETTER,STATE> subtrahend) throws AutomataLibraryException {
		m_Services = services;
		m_Logger = m_Services.getLoggingService().getLogger(LibraryIdentifiers.s_LibraryID);
		contentFactory = minuend.getStateFactory();
		this.minuend = minuend;
		this.subtrahend = subtrahend;
		if (!NestedWordAutomaton.sameAlphabet(this.minuend, this.subtrahend)) {
			throw new AutomataLibraryException(this.getClass(), "Unable to apply operation to automata with different alphabets.");
		}
		this.stateDeterminizer = new PowersetDeterminizer<LETTER,STATE>(subtrahend, true, stateFactory);
		m_Logger.info(startMessage());
		difference = new NestedWordAutomaton<LETTER,STATE>(
				m_Services, 
				minuend.getInternalAlphabet(),
				minuend.getCallAlphabet(),
				minuend.getReturnAlphabet(),
				minuend.getStateFactory());
		auxilliaryEmptyStackState = difference.getEmptyStackState();
		computeDifference();
		m_Logger.info(exitMessage());
	}
	
	
	
	
	
	public boolean wasVisited(STATE callerState, STATE state) {
		Set<STATE> callerStates = visited.get(state);
		if (callerStates == null) {
			return false;
		}
		else {
			return callerStates.contains(callerState);
		}
	}
	
	public void markVisited(STATE callerState, STATE state) {
		Set<STATE> callerStates = visited.get(state);
		if (callerStates == null) {
			callerStates = new HashSet<STATE>();
			visited.put(state, callerStates);
		}
		callerStates.add(callerState);
	}
	

	public void enqueueAndMark(STATE callerState, STATE state) {
		if (!wasVisited(callerState, state)) {
			markVisited(callerState, state);
			SummaryState<LETTER,STATE> statePair = new SummaryState<LETTER,STATE>(state,callerState);
			worklist.add(statePair);
		}
	}
	
	
	
	public void addSummary(STATE summaryPred, STATE summarySucc) {
		Set<STATE> summarySuccessors = summary.get(summaryPred);
		if (summarySuccessors == null) {
			summarySuccessors = new HashSet<STATE>();
			summary.put(summaryPred, summarySuccessors);
		}
		summarySuccessors.add(summarySucc);
	}

	
	/**
	 * Get all states <i>resCaller</i> of the resulting automaton (computed so
	 * far) such that the summary state (<i>resCaller</i>,<i>resPresent</i>) has
	 * been visited so far.
	 */
	private Set<STATE> getKnownCallerStates(STATE resPresent) {
		Set<STATE> callerStates = visited.get(resPresent);
		if (callerStates == null) {
			return new HashSet<STATE>(0);
		}
		else {
			return callerStates;
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	public void computeDifference() {
		DeterminizedState<LETTER,STATE> detState = stateDeterminizer.initialState(); 
		for (STATE minuState : minuend.getInitialStates()) {
			DifferenceState macrState = 
				new DifferenceState(minuState, detState);
			STATE diffState = contentFactory.intersection(
					macrState.minuendState, 
					stateDeterminizer.getState(macrState.subtrahendDeterminizedState));
			difference.addState(true, macrState.isFinal(), diffState);
			diff2res.put(macrState,diffState);
			res2diff.put(diffState, macrState);
			enqueueAndMark(auxilliaryEmptyStackState, diffState);
		}
		
		while(!worklist.isEmpty()) {
			SummaryState<LETTER,STATE> statePair = worklist.remove(0);
//			m_Logger.debug("Processing: "+ statePair);
			processSummaryState(statePair);
			if (summary.containsKey(statePair.presentState)) {
				for (STATE summarySucc : summary.get(statePair.presentState)) {
					enqueueAndMark(statePair.getCallerState(), summarySucc);
				}
			}
		}
	}
	

	

	/**
	 * Let resSummaryState=(<i>caller</i>,<i>present</i>). Extend the
	 * construction of the resulting automaton at <i>present</i> by outgoing
	 * transitions. To decide if a return transition can be added <i>caller</i>
	 * is taken into account. 
	 */
	private void processSummaryState(SummaryState<LETTER,STATE> resSummaryState) {
		STATE resState = resSummaryState.getPresentState();
		DifferenceState diffState = res2diff.get(resState);
		STATE minuState = 
				diffState.getMinuendState();
		DeterminizedState<LETTER,STATE> detState = 
				diffState.getSubtrahendDeterminizedState(); 
		
		for (LETTER symbol : minuend.lettersInternal(minuState)) {
			if (!subtrahend.getInternalAlphabet().contains(symbol)) {
				continue;
			}
			DeterminizedState<LETTER,STATE> detSucc = 
					stateDeterminizer.internalSuccessor(detState, symbol);
			for (STATE minuSucc : minuend.succInternal(minuState, symbol)) {
				DifferenceState diffSucc = 
						new DifferenceState(minuSucc, detSucc);
				STATE resSucc = getResState(diffSucc);
				difference.addInternalTransition(resState, symbol, resSucc);
				enqueueAndMark(resSummaryState.getCallerState(),resSucc);
			}
		}
		
		for (LETTER symbol : minuend.lettersCall(minuState)) {
			if (!subtrahend.getCallAlphabet().contains(symbol)) {
				continue;
			}
			DeterminizedState<LETTER,STATE> detSucc = 
					stateDeterminizer.callSuccessor(detState, symbol);
			for (STATE minuSucc : minuend.succCall(minuState, symbol)) {
				DifferenceState diffSucc = 
						new DifferenceState(minuSucc, detSucc);
				STATE resSucc = getResState(diffSucc);
				difference.addCallTransition(resState, symbol, resSucc);
				enqueueAndMark(resState, resSucc);
			}
		}

		for (LETTER symbol : minuend.lettersReturn(minuState)) {
			if (!subtrahend.getReturnAlphabet().contains(symbol)) {
				continue;
			}
			STATE resLinPred = resSummaryState.getCallerState();
			if (resLinPred == auxilliaryEmptyStackState) {
				continue;
			}
			DifferenceState diffLinPred = res2diff.get(resLinPred);
			STATE minuLinPred = diffLinPred.getMinuendState();
			DeterminizedState<LETTER,STATE> detLinPred = 
					diffLinPred.getSubtrahendDeterminizedState();
			
			Iterable<STATE> minuSuccs = 
					minuend.succReturn(minuState, minuLinPred, symbol);
//			if (minuSuccs.isEmpty()) continue;
			DeterminizedState<LETTER,STATE> detSucc = 
				stateDeterminizer.returnSuccessor(detState, detLinPred, symbol);
			for (STATE minuSucc : minuSuccs) {
				DifferenceState diffSucc = 
					new DifferenceState(minuSucc, detSucc);
				STATE resSucc = getResState(diffSucc);
				difference.addReturnTransition(
										resState, resLinPred, symbol, resSucc);
				addSummary(resLinPred, resSucc);
				for (STATE resLinPredCallerState : 
											getKnownCallerStates(resLinPred)) {
					enqueueAndMark(resLinPredCallerState, resSucc);
				}
			}
		}
	}
	
	
	
	
	
	
	
	
	
	

	
	
	/**
	 * Get the state in the resulting automaton that represents a
	 * DifferenceState. If this state in the resulting automaton does not exist
	 * yet, construct it.
	 */
	STATE getResState(DifferenceState diffState) {
		if (diff2res.containsKey(diffState)) {
			return diff2res.get(diffState);
		}
		else {
			STATE resState = contentFactory.intersection(
					diffState.minuendState, 
					stateDeterminizer.getState(diffState.subtrahendDeterminizedState));
			difference.addState(false, diffState.isFinal(), resState);
			diff2res.put(diffState,resState);
			res2diff.put(resState,diffState);
			return resState;
		}
	}
	
	





/**
 * State of an NWA that accepts the language difference of two NWAs.
 * A DifferenceState is a pair whose first entry is a state of the minuend, the
 * second entry is a DeterminizedState of the subtrahend. A DifferenceState is
 * final iff the minuend state is final and the subtrahend state is not final. 
 * 
 * @author heizmann@informatik.uni-freiburg.de
 *
 * @param <LETTER> Symbol
 * @param <STATE> Content
 */
	private class DifferenceState {
		final STATE minuendState;
		final DeterminizedState<LETTER,STATE> subtrahendDeterminizedState;
		final boolean isFinal;
		final int m_hashCode; 
		
		
		public DifferenceState(	
				STATE minuendState, 
				DeterminizedState<LETTER,STATE> subtrahendDeterminizedState) {
			
			this.minuendState = minuendState;
			this.subtrahendDeterminizedState = subtrahendDeterminizedState;
			this.isFinal = minuend.isFinal(minuendState) &&
										!subtrahendDeterminizedState.containsFinal();
			m_hashCode = 3 * minuendState.hashCode() +
									5 * subtrahendDeterminizedState.hashCode();
			//FIXME: hasCode of StatePairList may change over time!
		}
		
		public STATE getMinuendState() {
			return minuendState;
		}

		public DeterminizedState<LETTER,STATE> getSubtrahendDeterminizedState() {
			return subtrahendDeterminizedState;
		}

		public boolean isFinal() {
			return this.isFinal;
		}
		
		/**
		 * Two DifferenceStates are equivalent iff each, their minuend states
		 * and their subtrahend states are equivalent.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DifferenceSadd.DifferenceState) {
				DifferenceState diffState = (DifferenceState) obj;
				return diffState.minuendState.equals(this.minuendState)
					&& this.subtrahendDeterminizedState.equals(
										diffState.subtrahendDeterminizedState);
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return m_hashCode;
		}
		
		@Override
		public String toString() {
			return "<[< " + minuendState.toString() + " , "
					+ subtrahendDeterminizedState.toString() + ">]>";
		}	
	}
	
	



	
	
	private class SummaryState<LETTER,STATE> {
		private final STATE callerState;
		private final STATE presentState;
		private final int hashCode;
		public SummaryState(STATE presentState, STATE callerState) {
			this.callerState = callerState;
			this.presentState = presentState;
			this.hashCode = 
				3 * callerState.hashCode() + 5 * presentState.hashCode(); 
		}
		
		public STATE getCallerState() {
			return callerState;
		}


		public STATE getPresentState() {
			return presentState;
		}



		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof SummaryState) {
				SummaryState<LETTER,STATE> summaryState = (SummaryState<LETTER,STATE>) obj;
				return presentState.equals(summaryState.presentState) && 
								callerState.equals(summaryState.callerState);
			}
			else {
				return false;
			}
		}
		
		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public String toString() {
			return "CallerState: " + callerState + "  State: "+ presentState;
		}
		
	}







	@Override
	public boolean checkResult(StateFactory<STATE> stateFactory)
			throws AutomataLibraryException {
		boolean correct = true;
		if (stateDeterminizer instanceof PowersetDeterminizer) {
			m_Logger.info("Start testing correctness of " + operationName());

			INestedWordAutomatonOldApi<LETTER,STATE> resultDD = 
					(new DifferenceDD<LETTER,STATE>(m_Services, stateFactory, minuend, subtrahend)).getResult();
			correct &= (ResultChecker.nwaLanguageInclusion(m_Services, resultDD, difference, stateFactory) == null);
			correct &= (ResultChecker.nwaLanguageInclusion(m_Services, difference, resultDD, stateFactory) == null);
			if (!correct) {
			ResultChecker.writeToFileIfPreferred(m_Services, operationName() + "Failed", "", minuend,subtrahend);
			}
			m_Logger.info("Finished testing correctness of " + operationName());
		} else {
			m_Logger.warn("Unable to test correctness if state determinzier is not the PowersetDeterminizer.");
		}
		return correct;
	}
	
	
	

}
