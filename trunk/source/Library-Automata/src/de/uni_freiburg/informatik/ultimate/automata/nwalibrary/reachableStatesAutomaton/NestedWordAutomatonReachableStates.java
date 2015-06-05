/*
 * Copyright (C) 2009-2014 University of Freiburg
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
 * along with the ULTIMATE Automata Library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Automata Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE Automata Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.reachableStatesAutomaton;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.automata.AtsDefinitionPrinter;
import de.uni_freiburg.informatik.ultimate.automata.InCaReCounter;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.OperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.ResultChecker;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.IDoubleDeckerAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonOldApi;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonSimple;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedRun;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.buchiNwa.BuchiAccepts;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.buchiNwa.NestedLassoRun;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.Accepts;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.IOpWithDelayedDeadEndRemoval.UpDownEntry;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.reachableStatesAutomaton.StateContainer.DownStateProp;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingReturnTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingReturnTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.SummaryReturnTransition;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.util.DebugMessage;
import de.uni_freiburg.informatik.ultimate.util.HashRelation;
import de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException;

public class NestedWordAutomatonReachableStates<LETTER, STATE> implements INestedWordAutomatonOldApi<LETTER, STATE>,
		INestedWordAutomaton<LETTER, STATE>, IDoubleDeckerAutomaton<LETTER, STATE>,
		IAutomatonWithSccComputation<LETTER, STATE> {

	private final IUltimateServiceProvider m_Services;
	private final Logger m_Logger;

	private final INestedWordAutomatonSimple<LETTER, STATE> m_Operand;

	private final Set<LETTER> m_InternalAlphabet;
	private final Set<LETTER> m_CallAlphabet;
	private final Set<LETTER> m_ReturnAlphabet;

	protected final StateFactory<STATE> m_StateFactory;

	private final Set<STATE> m_initialStates = new HashSet<STATE>();
	private final Set<STATE> m_finalStates = new HashSet<STATE>();

	private final Map<STATE, StateContainer<LETTER, STATE>> m_States = new HashMap<STATE, StateContainer<LETTER, STATE>>();

	public static enum ReachProp {
		REACHABLE, NODEADEND_AD, NODEADEND_SD, FINANC, LIVE_AD, LIVE_SD
	};

	enum InCaRe {
		INTERNAL, CALL, RETURN, SUMMARY
	};

	/**
	 * Set of return transitions LinPREs x HierPREs x LETTERs x SUCCs stored as
	 * map HierPREs -> LETTERs -> LinPREs -> SUCCs
	 * 
	 */
	private Map<STATE, Map<LETTER, Map<STATE, Set<STATE>>>> m_ReturnSummary = new HashMap<STATE, Map<LETTER, Map<STATE, Set<STATE>>>>();

	InCaReCounter m_NumberTransitions = new InCaReCounter();

	// private
	// Map<StateContainer<LETTER,STATE>,Set<StateContainer<LETTER,STATE>>>
	// m_Summaries = new
	// HashMap<StateContainer<LETTER,STATE>,Set<StateContainer<LETTER,STATE>>>();

	private Set<LETTER> m_EmptySetOfLetters = Collections.emptySet();

	private AncestorComputation m_WithOutDeadEnds;
	private AncestorComputation m_OnlyLiveStates;
	private AcceptingSummariesComputation m_AcceptingSummaries;
	private AcceptingComponentsAnalysis<LETTER, STATE> m_AcceptingComponentsAnalysis;

	/**
	 * Construct a run for each accepting state. Use this only while
	 * developing/debugging/testing the construction of runs.
	 */
	private final static boolean m_ExtRunConstructionTesting = false;

	/**
	 * Construct a lasso for each accepting state/accepting summary. Use this
	 * only while developing/debugging/testing the construction of lassos.
	 */
	private final static boolean m_ExtLassoConstructionTesting = false;



	// private void addSummary(StateContainer<LETTER,STATE> callPred,
	// StateContainer<LETTER,STATE> returnSucc) {
	// Set<StateContainer<LETTER,STATE>> returnSuccs =
	// m_Summaries.get(callPred);
	// if (returnSuccs == null) {
	// returnSuccs = new HashSet<StateContainer<LETTER,STATE>>();
	// m_Summaries.put(callPred, returnSuccs);
	// }
	// returnSuccs.add(returnSucc);
	// }

	public NestedWordAutomatonReachableStates(IUltimateServiceProvider services,
			INestedWordAutomatonSimple<LETTER, STATE> operand)
			throws OperationCanceledException {
		m_Services = services;
		m_Logger = m_Services.getLoggingService().getLogger(LibraryIdentifiers.s_LibraryID);
		this.m_Operand = operand;
		m_InternalAlphabet = operand.getInternalAlphabet();
		m_CallAlphabet = operand.getCallAlphabet();
		m_ReturnAlphabet = operand.getReturnAlphabet();
		m_StateFactory = operand.getStateFactory();
		try {
			new ReachableStatesComputation();
			// computeDeadEnds();
			// new NonLiveStateComputation();
			if (m_ExtLassoConstructionTesting) {
				List<NestedLassoRun<LETTER, STATE>> runs = getOrComputeAcceptingComponents()
						.getAllNestedLassoRuns();
				for (NestedLassoRun<LETTER, STATE> nlr : runs) {
					STATE honda = nlr.getLoop().getStateAtPosition(0);
					m_Logger.debug(new DebugMessage("Test lasso construction for honda state {0}", honda));
					assert (new BuchiAccepts<LETTER, STATE>(m_Services, NestedWordAutomatonReachableStates.this,
							nlr.getNestedLassoWord())).getResult();
				}

			}
			m_Logger.info(stateContainerInformation());
			// assert (new TransitionConsitenceCheck<LETTER,
			// STATE>(this)).consistentForAll();

			assert (checkTransitionsReturnedConsistent());
		} catch (ToolchainCanceledException tce) {
			throw tce;
		} catch (OperationCanceledException oce) {
			throw oce;
		} catch (Error e) {
			String message = "// Problem with  removeUnreachable";
			ResultChecker.writeToFileIfPreferred(m_Services, "FailedremoveUnreachable", message, operand);
			throw e;
		} catch (RuntimeException e) {
			String message = "// Problem with  removeUnreachable";
			ResultChecker.writeToFileIfPreferred(m_Services, "FailedremoveUnreachable", message, operand);
			throw e;
		}
	}
	
	/**
	 * Returns the state container for a given state. 
	 * The visibility of this method is deliberately set package private. 
	 */
	StateContainer<LETTER, STATE> getStateContainer(STATE state) {
		return m_States.get(state);
	}

	private String stateContainerInformation() {
		int inMap = 0;
		int outMap = 0;
		for (STATE state : m_States.keySet()) {
			StateContainer<LETTER, STATE> cont = m_States.get(state);
			if (cont instanceof StateContainerFieldAndMap) {
				if (((StateContainerFieldAndMap<LETTER, STATE>) cont).mapModeIncoming()) {
					inMap++;
				}
				if (((StateContainerFieldAndMap<LETTER, STATE>) cont).mapModeOutgoing()) {
					outMap++;
				}
			}
		}
		return m_States.size() + " StateContainers " + inMap + " in inMapMode" + outMap + " in outMapMode";
	}

	// public boolean isDeadEnd(STATE state) {
	// ReachProp reachProp = m_States.get(state).getReachProp();
	// return reachProp == ReachProp.REACHABLE;
	// }

	public AncestorComputation getWithOutDeadEnds() {
		return m_WithOutDeadEnds;
	}

	public AncestorComputation getOnlyLiveStates() {
		return m_OnlyLiveStates;
	}

	public AcceptingComponentsAnalysis<LETTER, STATE> getOrComputeAcceptingComponents() {
		if (m_AcceptingComponentsAnalysis == null) {
			computeAcceptingComponents();
		}
		return m_AcceptingComponentsAnalysis;
	}
	
	public AcceptingSummariesComputation getAcceptingSummariesComputation() {
		return m_AcceptingSummaries;
	}

	StateContainer<LETTER, STATE> obtainSC(STATE state) {
		return m_States.get(state);
	}

	boolean isAccepting(Summary<LETTER, STATE> summary) {
		StateContainer<LETTER, STATE> callPred = summary.getHierPred();
		Set<Summary<LETTER, STATE>> summariesForHier = m_AcceptingSummaries.getAcceptingSummaries().getImage(callPred);
		if (summariesForHier == null) {
			return false;
		} else {
			return summariesForHier.contains(summary);
		}
	}

	@Override
	public int size() {
		return m_States.size();
	}

	@Override
	public Set<LETTER> getAlphabet() {
		return m_InternalAlphabet;
	}

	@Override
	public String sizeInformation() {
		int states = m_States.size();
		return states + " states and " + m_NumberTransitions + " transitions.";
	}

	@Override
	public Set<LETTER> getInternalAlphabet() {
		return m_InternalAlphabet;
	}

	@Override
	public Set<LETTER> getCallAlphabet() {
		return m_CallAlphabet;
	}

	@Override
	public Set<LETTER> getReturnAlphabet() {
		return m_ReturnAlphabet;
	}

	@Override
	public StateFactory<STATE> getStateFactory() {
		return m_StateFactory;
	}

	@Override
	public Set<STATE> getStates() {
		return m_States.keySet();
	}

	@Override
	public Set<STATE> getInitialStates() {
		return Collections.unmodifiableSet(m_initialStates);
	}

	@Override
	public Collection<STATE> getFinalStates() {
		return Collections.unmodifiableSet(m_finalStates);
	}

	@Override
	public boolean isInitial(STATE state) {
		return m_Operand.isInitial(state);
	}

	@Override
	public boolean isFinal(STATE state) {
		return m_Operand.isFinal(state);
	}

	@Override
	public STATE getEmptyStackState() {
		return m_Operand.getEmptyStackState();
	}

	@Override
	public Set<LETTER> lettersInternal(STATE state) {
		return m_States.get(state).lettersInternal();
	}

	@Override
	public Set<LETTER> lettersCall(STATE state) {
		return m_States.get(state).lettersCall();
	}

	@Override
	public Set<LETTER> lettersReturn(STATE state) {
		return m_States.get(state).lettersReturn();
	}

	@Override
	public Set<LETTER> lettersInternalIncoming(STATE state) {
		return m_States.get(state).lettersInternalIncoming();
	}

	@Override
	public Set<LETTER> lettersCallIncoming(STATE state) {
		return m_States.get(state).lettersCallIncoming();
	}

	@Override
	public Set<LETTER> lettersReturnIncoming(STATE state) {
		return m_States.get(state).lettersReturnIncoming();
	}

	@Override
	public Set<LETTER> lettersReturnSummary(STATE state) {
		if (!m_States.containsKey(state)) {
			throw new IllegalArgumentException("State " + state + " unknown");
		}
		Map<LETTER, Map<STATE, Set<STATE>>> map = m_ReturnSummary.get(state);
		return map == null ? new HashSet<LETTER>(0) : map.keySet();
	}

	@Override
	public Iterable<STATE> succInternal(STATE state, LETTER letter) {
		return m_States.get(state).succInternal(letter);
	}

	@Override
	public Iterable<STATE> succCall(STATE state, LETTER letter) {
		return m_States.get(state).succCall(letter);
	}

	@Override
	public Iterable<STATE> hierPred(STATE state, LETTER letter) {
		return m_States.get(state).hierPred(letter);
	}

	@Override
	public Iterable<STATE> succReturn(STATE state, STATE hier, LETTER letter) {
		return m_States.get(state).succReturn(hier, letter);
	}

	@Override
	public Iterable<STATE> predInternal(STATE state, LETTER letter) {
		return m_States.get(state).predInternal(letter);
	}

	@Override
	public Iterable<STATE> predCall(STATE state, LETTER letter) {
		return m_States.get(state).predCall(letter);
	}

	@Override
	public boolean finalIsTrap() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDeterministic() {
		return false;
	}

	@Override
	public boolean isTotal() {
		throw new UnsupportedOperationException();
	}

	private void addReturnSummary(STATE pred, STATE hier, LETTER letter, STATE succ) {
		Map<LETTER, Map<STATE, Set<STATE>>> letter2pred2succs = m_ReturnSummary.get(hier);
		if (letter2pred2succs == null) {
			letter2pred2succs = new HashMap<LETTER, Map<STATE, Set<STATE>>>();
			m_ReturnSummary.put(hier, letter2pred2succs);
		}
		Map<STATE, Set<STATE>> pred2succs = letter2pred2succs.get(letter);
		if (pred2succs == null) {
			pred2succs = new HashMap<STATE, Set<STATE>>();
			letter2pred2succs.put(letter, pred2succs);
		}
		Set<STATE> succS = pred2succs.get(pred);
		if (succS == null) {
			succS = new HashSet<STATE>();
			pred2succs.put(pred, succS);
		}
		succS.add(succ);
	}

	public Collection<LETTER> lettersSummary(STATE hier) {
		Map<LETTER, Map<STATE, Set<STATE>>> map = m_ReturnSummary.get(hier);
		return map == null ? m_EmptySetOfLetters : map.keySet();
	}

	@Override
	public Iterable<SummaryReturnTransition<LETTER, STATE>> returnSummarySuccessor(LETTER letter, STATE hier) {
		Set<SummaryReturnTransition<LETTER, STATE>> result = new HashSet<SummaryReturnTransition<LETTER, STATE>>();
		Map<LETTER, Map<STATE, Set<STATE>>> letter2pred2succ = m_ReturnSummary.get(hier);
		if (letter2pred2succ == null) {
			return result;
		}
		Map<STATE, Set<STATE>> pred2succ = letter2pred2succ.get(letter);
		if (pred2succ == null) {
			return result;
		}
		for (STATE pred : pred2succ.keySet()) {
			if (pred2succ.get(pred) != null) {
				for (STATE succ : pred2succ.get(pred)) {
					SummaryReturnTransition<LETTER, STATE> srt = new SummaryReturnTransition<LETTER, STATE>(pred,
							letter, succ);
					result.add(srt);
				}
			}
		}
		return result;
	}

	public Iterable<SummaryReturnTransition<LETTER, STATE>> returnSummarySuccessor(final STATE hier) {
		return new Iterable<SummaryReturnTransition<LETTER, STATE>>() {
			/**
			 * Iterates over all SummaryReturnTransition of hier.
			 */
			@Override
			public Iterator<SummaryReturnTransition<LETTER, STATE>> iterator() {
				Iterator<SummaryReturnTransition<LETTER, STATE>> iterator = new Iterator<SummaryReturnTransition<LETTER, STATE>>() {
					Iterator<LETTER> m_LetterIterator;
					LETTER m_CurrentLetter;
					Iterator<SummaryReturnTransition<LETTER, STATE>> m_CurrentIterator;
					{
						m_LetterIterator = lettersSummary(hier).iterator();
						nextLetter();
					}

					private void nextLetter() {
						if (m_LetterIterator.hasNext()) {
							do {
								m_CurrentLetter = m_LetterIterator.next();
								m_CurrentIterator = returnSummarySuccessor(m_CurrentLetter, hier).iterator();
							} while (!m_CurrentIterator.hasNext() && m_LetterIterator.hasNext());
							if (!m_CurrentIterator.hasNext()) {
								m_CurrentLetter = null;
								m_CurrentIterator = null;
							}
						} else {
							m_CurrentLetter = null;
							m_CurrentIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return m_CurrentLetter != null;
					}

					@Override
					public SummaryReturnTransition<LETTER, STATE> next() {
						if (m_CurrentLetter == null) {
							throw new NoSuchElementException();
						} else {
							SummaryReturnTransition<LETTER, STATE> result = m_CurrentIterator.next();
							if (!m_CurrentIterator.hasNext()) {
								nextLetter();
							}
							return result;
						}
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
				return iterator;
			}
		};
	}

	@Override
	public Iterable<IncomingInternalTransition<LETTER, STATE>> internalPredecessors(LETTER letter, STATE succ) {
		return m_States.get(succ).internalPredecessors(letter);
	}

	@Override
	public Iterable<IncomingInternalTransition<LETTER, STATE>> internalPredecessors(STATE succ) {
		return m_States.get(succ).internalPredecessors();
	}

	@Override
	public Iterable<IncomingCallTransition<LETTER, STATE>> callPredecessors(LETTER letter, STATE succ) {
		return m_States.get(succ).callPredecessors(letter);
	}

	@Override
	public Iterable<IncomingCallTransition<LETTER, STATE>> callPredecessors(STATE succ) {
		return m_States.get(succ).callPredecessors();
	}

	@Override
	public Iterable<OutgoingInternalTransition<LETTER, STATE>> internalSuccessors(STATE state, LETTER letter) {
		return m_States.get(state).internalSuccessors(letter);
	}

	@Override
	public Iterable<OutgoingInternalTransition<LETTER, STATE>> internalSuccessors(STATE state) {
		return m_States.get(state).internalSuccessors();
	}

	@Override
	public Iterable<OutgoingCallTransition<LETTER, STATE>> callSuccessors(STATE state, LETTER letter) {
		return m_States.get(state).callSuccessors(letter);
	}

	@Override
	public Iterable<OutgoingCallTransition<LETTER, STATE>> callSuccessors(STATE state) {
		return m_States.get(state).callSuccessors();
	}

	@Override
	public Iterable<IncomingReturnTransition<LETTER, STATE>> returnPredecessors(STATE hier, LETTER letter, STATE succ) {
		return m_States.get(succ).returnPredecessors(hier, letter);
	}

	@Override
	public Iterable<IncomingReturnTransition<LETTER, STATE>> returnPredecessors(LETTER letter, STATE succ) {
		return m_States.get(succ).returnPredecessors(letter);
	}

	@Override
	public Iterable<IncomingReturnTransition<LETTER, STATE>> returnPredecessors(STATE succ) {
		return m_States.get(succ).returnPredecessors();
	}

	@Override
	public Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSucccessors(STATE state, STATE hier, LETTER letter) {
		return m_States.get(state).returnSuccessors(hier, letter);
	}

	@Override
	public Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessors(STATE state, LETTER letter) {
		return m_States.get(state).returnSuccessors(letter);
	}

	@Override
	public Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessors(STATE state) {
		return m_States.get(state).returnSuccessors();
	}

	@Override
	public Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessorsGivenHier(STATE state, STATE hier) {
		return m_States.get(state).returnSuccessorsGivenHier(hier);
	}

	public void computeDeadEnds() {
		if (m_WithOutDeadEnds != null) {
			return;
			// throw new AssertionError("dead are already computed");
		}
		HashSet<StateContainer<LETTER, STATE>> acceptings = new HashSet<StateContainer<LETTER, STATE>>();
		for (STATE fin : getFinalStates()) {
			StateContainer<LETTER, STATE> cont = m_States.get(fin);
			assert cont.getReachProp() != ReachProp.NODEADEND_AD && cont.getReachProp() != ReachProp.NODEADEND_SD;
			acceptings.add(cont);
		}
		m_WithOutDeadEnds = new AncestorComputation(acceptings, ReachProp.NODEADEND_AD, ReachProp.NODEADEND_SD,
				DownStateProp.REACH_FINAL_ONCE, DownStateProp.REACHABLE_AFTER_DEADEND_REMOVAL);
	}

	public void computeAcceptingComponents() {
		if (m_AcceptingComponentsAnalysis != null) {
			throw new AssertionError("SCCs are already computed");
		}
		assert m_AcceptingSummaries == null;
		m_AcceptingSummaries = new AcceptingSummariesComputation();
		m_AcceptingComponentsAnalysis = new AcceptingComponentsAnalysis<LETTER, STATE>(
				this, m_AcceptingSummaries, m_Services, m_States.keySet(), m_initialStates);
	}

	public void computeNonLiveStates() {
		if (m_OnlyLiveStates != null) {
			return;
			// throw new AssertionError("non-live states are already computed");
		}
		if (getOrComputeAcceptingComponents() == null) {
			computeAcceptingComponents();
		}

		HashSet<StateContainer<LETTER, STATE>> nonLiveStartingSet = new HashSet<StateContainer<LETTER, STATE>>(
				m_AcceptingComponentsAnalysis.getStatesOfAllSCCs());
		m_OnlyLiveStates = new AncestorComputation(nonLiveStartingSet, ReachProp.LIVE_AD, ReachProp.LIVE_SD,
				DownStateProp.REACH_FINAL_INFTY, DownStateProp.REACHABLE_AFTER_NONLIVE_REMOVAL);
	}

	public Set<STATE> getDownStates(STATE state) {
		StateContainer<LETTER, STATE> cont = m_States.get(state);
		return cont.getDownStates().keySet();
	}

	@Override
	public boolean isDoubleDecker(STATE up, STATE down) {
		return getDownStates(up).contains(down);
	}

	// //////////////////////////////////////////////////////////////////////////

	/**
	 * Compute the set of reachable doubledeckers. Construct a state container
	 * for each reachable state, add both (state and StateContainer) to m_States
	 * and set the reachability down state information in the state container.
	 * 
	 */
	private class ReachableStatesComputation {
		private int m_NumberOfConstructedStates = 0;
		private final LinkedList<StateContainer<LETTER, STATE>> m_ForwardWorklist = new LinkedList<StateContainer<LETTER, STATE>>();
		private final LinkedList<StateContainer<LETTER, STATE>> m_DownPropagationWorklist = new LinkedList<StateContainer<LETTER, STATE>>();

		ReachableStatesComputation() throws OperationCanceledException {
			addInitialStates(m_Operand.getInitialStates());

			do {
				while (!m_ForwardWorklist.isEmpty()) {
					final StateContainer<LETTER, STATE> cont = m_ForwardWorklist.remove(0);
					cont.eraseUnpropagatedDownStates();
					Set<STATE> newDownStatesFormSelfloops = null;

					if (candidateForOutgoingReturn(cont.getState())) {
						for (STATE down : cont.getDownStates().keySet()) {
							if (down != getEmptyStackState()) {
								Set<STATE> newDownStates = addReturnsAndSuccessors(cont, down);
								if (newDownStates != null) {
									if (newDownStatesFormSelfloops == null) {
										newDownStatesFormSelfloops = new HashSet<STATE>();
									}
									newDownStatesFormSelfloops.addAll(newDownStates);
								}
							}
						}
					}
					addInternalsAndSuccessors(cont);
					{
						Set<STATE> newDownStates = addCallsAndSuccessors(cont);
						if (newDownStates != null) {
							if (newDownStatesFormSelfloops == null) {
								newDownStatesFormSelfloops = new HashSet<STATE>();
							}
							newDownStatesFormSelfloops.addAll(newDownStates);
						}
					}
					if (newDownStatesFormSelfloops != null) {
						assert !newDownStatesFormSelfloops.isEmpty();
						for (STATE down : newDownStatesFormSelfloops) {
							cont.addReachableDownState(down);
						}
						m_DownPropagationWorklist.add(cont);
					}
					if (!m_Services.getProgressMonitorService().continueProcessing()) {
						throw new OperationCanceledException(this.getClass());
					}
				}
				while (m_ForwardWorklist.isEmpty() && !m_DownPropagationWorklist.isEmpty()) {
					if (!m_Services.getProgressMonitorService().continueProcessing()) {
						// TODO: Check if this has a performance impact
						// This exception was included because of timeouts on
						// e.g.
						// svcomp/systemc/token_ring.07_false-unreach-call_false-termination.cil.c
						// (Settings:settings/TACASInterpolation2015/ForwardPredicates.epf,
						// Toolchain:toolchains/AutomizerC.xml)

						throw new OperationCanceledException(this.getClass());
					}

					StateContainer<LETTER, STATE> cont = m_DownPropagationWorklist.remove(0);
					propagateNewDownStates(cont);
				}

			} while (!m_DownPropagationWorklist.isEmpty() || !m_ForwardWorklist.isEmpty());
			assert (m_ForwardWorklist.isEmpty());
			assert (m_DownPropagationWorklist.isEmpty());
			assert checkTransitionsReturnedConsistent();

			if (m_ExtRunConstructionTesting) {
				for (STATE fin : getFinalStates()) {
					m_Logger.debug(new DebugMessage("Test if can find an accepting run for final state {0}", fin));
					NestedRun<LETTER, STATE> run = (new RunConstructor<LETTER, STATE>(
							m_Services, 
							NestedWordAutomatonReachableStates.this, m_States.get(fin))).constructRun();
					assert (new Accepts<LETTER, STATE>(m_Services, NestedWordAutomatonReachableStates.this, run.getWord()))
							.getResult();
				}
			}
		}

		private void addInitialStates(Iterable<STATE> initialStates) {
			for (STATE state : initialStates) {
				m_initialStates.add(state);
				HashMap<STATE, Integer> downStates = new HashMap<STATE, Integer>();
				downStates.put(getEmptyStackState(), 0);
				StateContainer<LETTER, STATE> sc = addState(state, downStates);
				m_States.put(state, sc);
			}
		}

		/**
		 * Construct State Container. Add to
		 * CommonEntriesComponent<LETTER,STATE>. Add to ForwardWorklist.
		 */
		private StateContainer<LETTER, STATE> addState(STATE state, HashMap<STATE, Integer> downStates) {
			assert !m_States.containsKey(state);
			if (m_Operand.isFinal(state)) {
				m_finalStates.add(state);
			}
			boolean canHaveOutgoingReturn = candidateForOutgoingReturn(state);
			StateContainer<LETTER, STATE> result = new StateContainerFieldAndMap<LETTER, STATE>(state,
					m_NumberOfConstructedStates, downStates, canHaveOutgoingReturn);
			m_NumberOfConstructedStates++;
			m_States.put(state, result);
			m_ForwardWorklist.add(result);
			return result;
		}

		private boolean candidateForOutgoingReturn(STATE state) {
			return !m_Operand.lettersReturn(state).isEmpty();
		}

		private void addInternalsAndSuccessors(StateContainer<LETTER, STATE> cont) {
			STATE state = cont.getState();
			for (OutgoingInternalTransition<LETTER, STATE> trans : m_Operand.internalSuccessors(state)) {
				STATE succ = trans.getSucc();
				StateContainer<LETTER, STATE> succSC = m_States.get(succ);
				if (succSC == null) {
					succSC = addState(succ, new HashMap<STATE, Integer>(cont.getDownStates()));
				} else {
					addNewDownStates(cont, succSC, cont.getDownStates().keySet());
				}
				assert (!containsCallTransition(state, trans.getLetter(), succ)) : "Operand contains transition twice: "
						+ state + trans.getSucc();
				cont.addInternalOutgoing(trans);
				succSC.addInternalIncoming(new IncomingInternalTransition<LETTER, STATE>(state, trans.getLetter()));
				m_NumberTransitions.incIn();
			}
		}

		private Set<STATE> addCallsAndSuccessors(StateContainer<LETTER, STATE> cont) {
			boolean addedSelfloop = false;
			STATE state = cont.getState();
			for (OutgoingCallTransition<LETTER, STATE> trans : m_Operand.callSuccessors(cont.getState())) {
				STATE succ = trans.getSucc();
				StateContainer<LETTER, STATE> succCont = m_States.get(succ);
				HashMap<STATE, Integer> succDownStates = new HashMap<STATE, Integer>();
				succDownStates.put(cont.getState(), 0);
				if (succCont == null) {
					succCont = addState(succ, succDownStates);
				} else {
					addNewDownStates(cont, succCont, succDownStates.keySet());
					if (cont == succCont) {
						addedSelfloop = true;
					}
				}
				assert (!containsCallTransition(state, trans.getLetter(), succ)) : "Operand contains transition twice: "
						+ state + trans.getSucc();
				cont.addCallOutgoing(trans);
				succCont.addCallIncoming(new IncomingCallTransition<LETTER, STATE>(state, trans.getLetter()));
				m_NumberTransitions.incCa();
			}
			if (addedSelfloop) {
				HashSet<STATE> newDownStates = new HashSet<STATE>(1);
				newDownStates.add(state);
				return newDownStatesSelfloop(cont, newDownStates);
			} else {
				return null;
			}
		}

		private Set<STATE> addReturnsAndSuccessors(StateContainer<LETTER, STATE> cont, STATE down) {
			boolean addedSelfloop = false;
			STATE state = cont.getState();
			StateContainer<LETTER, STATE> downCont = null;
			for (OutgoingReturnTransition<LETTER, STATE> trans : m_Operand.returnSuccessorsGivenHier(state, down)) {
				assert (down.equals(trans.getHierPred()));
				if (downCont == null) {
					downCont = m_States.get(down);
				}
				STATE succ = trans.getSucc();
				StateContainer<LETTER, STATE> succCont = m_States.get(succ);
				if (succCont == null) {
					succCont = addState(succ, new HashMap<STATE, Integer>(downCont.getDownStates()));
				} else {
					addNewDownStates(cont, succCont, downCont.getDownStates().keySet());
					if (cont == succCont) {
						addedSelfloop = true;
					}
				}
				assert (!containsReturnTransition(state, down, trans.getLetter(), succ)) : "Operand contains transition twice: "
						+ state + trans.getSucc();
				cont.addReturnOutgoing(trans);
				succCont.addReturnIncoming(new IncomingReturnTransition<LETTER, STATE>(cont.getState(), down, trans
						.getLetter()));
				addReturnSummary(state, down, trans.getLetter(), succ);
				m_NumberTransitions.incRe();
				// addSummary(downCont, succCont);
			}
			if (addedSelfloop) {
				return newDownStatesSelfloop(cont, downCont.getDownStates().keySet());
			} else {
				return null;
			}
		}

		private Set<STATE> newDownStatesSelfloop(StateContainer<LETTER, STATE> cont, Set<STATE> propagatedDownStates) {
			Set<STATE> newDownStates = null;
			for (STATE downs : propagatedDownStates) {
				if (!cont.getDownStates().keySet().contains(downs)) {
					if (newDownStates == null) {
						newDownStates = new HashSet<STATE>();
					}
					newDownStates.add(downs);
				}

			}
			return newDownStates;
		}

		private void addNewDownStates(StateContainer<LETTER, STATE> cont, StateContainer<LETTER, STATE> succCont,
				Set<STATE> potentiallyNewDownStates) {
			if (cont == succCont) {
				return;
			} else {
				boolean newDownStateWasPropagated = false;
				for (STATE down : potentiallyNewDownStates) {
					boolean newlyAdded = succCont.addReachableDownState(down);
					if (newlyAdded) {
						newDownStateWasPropagated = true;
					}
				}
				if (newDownStateWasPropagated) {
					m_DownPropagationWorklist.add(succCont);
				}
			}
		}

		private void propagateNewDownStates(StateContainer<LETTER, STATE> cont) {
			Set<STATE> unpropagatedDownStates = cont.getUnpropagatedDownStates();
			if (unpropagatedDownStates == null) {
				return;
			}
			for (OutgoingInternalTransition<LETTER, STATE> trans : cont.internalSuccessors()) {
				StateContainer<LETTER, STATE> succCont = m_States.get(trans.getSucc());
				addNewDownStates(cont, succCont, unpropagatedDownStates);
			}
			for (SummaryReturnTransition<LETTER, STATE> trans : returnSummarySuccessor(cont.getState())) {
				StateContainer<LETTER, STATE> succCont = m_States.get(trans.getSucc());
				addNewDownStates(cont, succCont, unpropagatedDownStates);
			}
			if (candidateForOutgoingReturn(cont.getState())) {
				HashSet<STATE> newDownStatesFormSelfloops = null;
				for (STATE down : cont.getUnpropagatedDownStates()) {
					if (down != getEmptyStackState()) {
						Set<STATE> newDownStates = addReturnsAndSuccessors(cont, down);
						if (newDownStates != null) {
							if (newDownStatesFormSelfloops == null) {
								newDownStatesFormSelfloops = new HashSet<STATE>();
							}
							newDownStatesFormSelfloops.addAll(newDownStates);
						}
					}
				}
				cont.eraseUnpropagatedDownStates();
				if (newDownStatesFormSelfloops != null) {
					assert !newDownStatesFormSelfloops.isEmpty();
					for (STATE down : newDownStatesFormSelfloops) {
						cont.addReachableDownState(down);
					}
					m_DownPropagationWorklist.add(cont);
				}
			} else {
				cont.eraseUnpropagatedDownStates();
			}

		}
	}

	// //////////////////////////////////////////////////////////////////////////////
	/**
	 * Compute all ancestor double deckers for a given set of states which we
	 * call the precious states. (In a dead end computation the precious states
	 * are the final states, in a non-live computation the precious states are
	 * all states of accepting SCCs).
	 * 
	 * If a state <i>up</i> can reach a precious state via a run without pending
	 * returns, we known that all double deckers <i>(up,down)</i> can reach a
	 * precious state and <i>up</i> gets the property "allDownProp".
	 * 
	 * If a state <i>up</i> can reach a precious state only via a run with
	 * pending calls we identify the down states such that the double decker
	 * <i>(up,down)</i> can reach a precious state. The up state gets the
	 * property "someDownProp", and the double decker gets the property
	 * "downStateProp" (this information is stored in the state container of
	 * <i>up</i>.
	 * 
	 */
	public class AncestorComputation {

		private final ReachProp m_rpAllDown;
		private final ReachProp m_rpSomeDown;
		/**
		 * Property stating that from this DoubleDecker precious states are
		 * reachable (resp reachable infinitely often).
		 */
		private final DownStateProp m_DspReachPrecious;
		/**
		 * Property stating that this DoubleDecker is reachable after removal of
		 * states.
		 */
		private final DownStateProp m_DspReachableAfterRemoval;

		private final Set<STATE> m_Ancestors = new HashSet<STATE>();
		private final Set<STATE> m_AncestorsInitial = new HashSet<STATE>();
		private final Set<STATE> m_AncestorsAccepting = new HashSet<STATE>();

		private ArrayDeque<StateContainer<LETTER, STATE>> m_NonReturnBackwardWorklist = new ArrayDeque<StateContainer<LETTER, STATE>>();
		private Set<StateContainer<LETTER, STATE>> m_HasIncomingReturn = new HashSet<StateContainer<LETTER, STATE>>();
		private ArrayDeque<StateContainer<LETTER, STATE>> m_PropagationWorklist = new ArrayDeque<StateContainer<LETTER, STATE>>();

		public Set<STATE> getStates() {
			return m_Ancestors;
		}

		public Set<STATE> getInitials() {
			return m_AncestorsInitial;
		}

		public Set<STATE> getFinals() {
			return m_AncestorsAccepting;
		}

		AncestorComputation(HashSet<StateContainer<LETTER, STATE>> preciousStates, ReachProp allDownProp,
				ReachProp someDownProp, DownStateProp downStatePropReachPrecious,
				DownStateProp downStatePropReachableAfterRemoval) {
			m_rpAllDown = allDownProp;
			m_rpSomeDown = someDownProp;
			m_DspReachPrecious = downStatePropReachPrecious;
			m_DspReachableAfterRemoval = downStatePropReachableAfterRemoval;

			for (StateContainer<LETTER, STATE> cont : preciousStates) {
				cont.setReachProp(m_rpAllDown);
				m_Ancestors.add(cont.getState());
				m_NonReturnBackwardWorklist.add(cont);
			}

			while (!m_NonReturnBackwardWorklist.isEmpty()) {
				StateContainer<LETTER, STATE> cont = m_NonReturnBackwardWorklist.removeFirst();
				if (m_initialStates.contains(cont.getState())) {
					m_AncestorsInitial.add(cont.getState());
				}
				if (isFinal(cont.getState())) {
					m_AncestorsAccepting.add(cont.getState());
				}

				for (IncomingInternalTransition<LETTER, STATE> inTrans : cont.internalPredecessors()) {
					STATE pred = inTrans.getPred();
					StateContainer<LETTER, STATE> predCont = m_States.get(pred);
					if (predCont.getReachProp() != m_rpAllDown) {
						predCont.setReachProp(m_rpAllDown);
						m_Ancestors.add(pred);
						m_NonReturnBackwardWorklist.add(predCont);
					}
				}
				for (IncomingReturnTransition<LETTER, STATE> inTrans : cont.returnPredecessors()) {
					STATE hier = inTrans.getHierPred();
					StateContainer<LETTER, STATE> hierCont = m_States.get(hier);
					if (hierCont.getReachProp() != m_rpAllDown) {
						hierCont.setReachProp(m_rpAllDown);
						m_Ancestors.add(hier);
						m_NonReturnBackwardWorklist.add(hierCont);
					}
					m_HasIncomingReturn.add(cont);
				}
				for (IncomingCallTransition<LETTER, STATE> inTrans : cont.callPredecessors()) {
					STATE pred = inTrans.getPred();
					StateContainer<LETTER, STATE> predCont = m_States.get(pred);
					if (predCont.getReachProp() != m_rpAllDown) {
						predCont.setReachProp(m_rpAllDown);
						m_Ancestors.add(pred);
						m_NonReturnBackwardWorklist.add(predCont);
					}
				}
			}

			for (StateContainer<LETTER, STATE> cont : m_HasIncomingReturn) {
				for (IncomingReturnTransition<LETTER, STATE> inTrans : cont.returnPredecessors()) {
					STATE lin = inTrans.getLinPred();
					StateContainer<LETTER, STATE> linCont = m_States.get(lin);
					if (linCont.getReachProp() != m_rpAllDown) {
						Set<STATE> potentiallyNewDownStates = new HashSet<STATE>(1);
						potentiallyNewDownStates.add(inTrans.getHierPred());
						addNewDownStates(null, linCont, potentiallyNewDownStates);
						// if (linCont.getUnpropagatedDownStates() == null) {
						// assert !m_PropagationWorklist.contains(linCont);
						// m_PropagationWorklist.addLast(linCont);
						// }
						// ReachProp oldValue =
						// linCont.modifyDownProp(inTrans.getHierPred(),m_rpSomeDown);
						// assert oldValue != m_rpAllDown;
					}
				}
			}

			while (!m_PropagationWorklist.isEmpty()) {
				StateContainer<LETTER, STATE> cont = m_PropagationWorklist.removeFirst();
				propagateBackward(cont);
			}
			removeUnnecessaryInitialStates();
			propagateReachableAfterRemovalDoubleDeckers();
		}

		private void removeUnnecessaryInitialStates() {
			Iterator<STATE> it = m_AncestorsInitial.iterator();
			while (it.hasNext()) {
				STATE state = it.next();
				StateContainer<LETTER, STATE> cont = m_States.get(state);
				if (cont.getReachProp() == m_rpAllDown) {
					continue;
				} else {
					boolean reachFinalOnce = cont.hasDownProp(getEmptyStackState(), DownStateProp.REACH_FINAL_ONCE);
					if (reachFinalOnce) {
						continue;
					} else {
						it.remove();
					}
				}
			}
		}

		private void propagateBackward(StateContainer<LETTER, STATE> cont) {
			Set<STATE> unpropagatedDownStates = cont.getUnpropagatedDownStates();
			cont.eraseUnpropagatedDownStates();
			Set<STATE> newUnpropagatedDownStatesSelfloop = null;
			for (IncomingInternalTransition<LETTER, STATE> inTrans : cont.internalPredecessors()) {
				STATE pred = inTrans.getPred();
				StateContainer<LETTER, STATE> predCont = m_States.get(pred);
				if (predCont.getReachProp() != m_rpAllDown) {
					addNewDownStates(cont, predCont, unpropagatedDownStates);
				}
			}
			for (IncomingReturnTransition<LETTER, STATE> inTrans : cont.returnPredecessors()) {
				STATE hier = inTrans.getHierPred();
				StateContainer<LETTER, STATE> hierCont = m_States.get(hier);
				if (hierCont.getReachProp() != m_rpAllDown) {
					addNewDownStates(cont, hierCont, unpropagatedDownStates);
				}
				STATE lin = inTrans.getLinPred();
				StateContainer<LETTER, STATE> linCont = m_States.get(lin);
				if (linCont.getReachProp() != m_rpAllDown) {
					if (atLeastOneOccursAsDownState(hierCont, unpropagatedDownStates)) {
						if (linCont == cont) {
							boolean hierAlreadyPropagated = cont.hasDownProp(hier, m_DspReachPrecious);
							if (!hierAlreadyPropagated) {
								if (newUnpropagatedDownStatesSelfloop == null) {
									newUnpropagatedDownStatesSelfloop = new HashSet<STATE>();
								}
								newUnpropagatedDownStatesSelfloop.add(hier);
							}
						} else {
							HashSet<STATE> potentiallyNewDownState = new HashSet<STATE>(1);
							potentiallyNewDownState.add(hier);
							addNewDownStates(cont, linCont, potentiallyNewDownState);
						}
					}

				}
			}
			if (newUnpropagatedDownStatesSelfloop != null) {
				for (STATE down : newUnpropagatedDownStatesSelfloop) {
					cont.setDownProp(down, m_DspReachPrecious);
				}
				assert !m_PropagationWorklist.contains(cont);
				m_PropagationWorklist.add(cont);
			}
		}

		private boolean atLeastOneOccursAsDownState(StateContainer<LETTER, STATE> hierCont,
				Set<STATE> unpropagatedDownStates) {
			for (STATE state : unpropagatedDownStates) {
				if (hierCont.getDownStates().containsKey(state)) {
					return true;
				}
			}
			return false;
		}

		private void addNewDownStates(StateContainer<LETTER, STATE> cont, StateContainer<LETTER, STATE> predCont,
				Set<STATE> potentiallyNewDownStates) {
			if (cont == predCont) {
				return;
			} else {
				boolean isAlreadyInWorklist = (predCont.getUnpropagatedDownStates() != null);
				assert (isAlreadyInWorklist == m_PropagationWorklist.contains(predCont));
				assert (!isAlreadyInWorklist || predCont.getReachProp() == m_rpSomeDown);
				boolean newDownStateWasPropagated = false;
				for (STATE down : potentiallyNewDownStates) {
					if (predCont.getDownStates().containsKey(down)) {
						boolean modified = predCont.setDownProp(down, m_DspReachPrecious);
						if (modified) {
							newDownStateWasPropagated = true;
						}

					}
				}
				if (newDownStateWasPropagated) {
					if (!isAlreadyInWorklist) {
						assert !m_PropagationWorklist.contains(predCont);
						m_PropagationWorklist.add(predCont);
					}
					if (predCont.getReachProp() != m_rpSomeDown) {
						assert predCont.getReachProp() != m_rpAllDown;
						predCont.setReachProp(m_rpSomeDown);
						assert !m_Ancestors.contains(predCont.getState());
						m_Ancestors.add(predCont.getState());
						if (isFinal(predCont.getState())) {
							m_AncestorsAccepting.add(predCont.getState());
						}
					}
				}
			}
		}

		/**
		 * Among all DoubleDeckers that cannot reach a precious state, there are
		 * some that are reachable (even after removing states that cannot reach
		 * precious states). This method marks all these DoubleDeckers with
		 * m_DspReachableAfterRemoval
		 */
		public void propagateReachableAfterRemovalDoubleDeckers() {
			LinkedHashSet<StateContainer<LETTER, STATE>> propagationWorklist = new LinkedHashSet<StateContainer<LETTER, STATE>>();
			Set<StateContainer<LETTER, STATE>> visited = new HashSet<>();

			// start only at states that are still initial after removal
			// of states
			for (STATE state : m_AncestorsInitial) {
				assert (isInitial(state));
				StateContainer<LETTER, STATE> sc = m_States.get(state);
				propagationWorklist.add(sc);
				visited.add(sc);
			}

			while (!propagationWorklist.isEmpty()) {
				StateContainer<LETTER, STATE> cont = propagationWorklist.iterator().next();
				propagationWorklist.remove(cont);
				for (OutgoingInternalTransition<LETTER, STATE> inTrans : cont.internalSuccessors()) {
					STATE succ = inTrans.getSucc();
					if (!m_Ancestors.contains(succ)) {
						// succ will be removed
						continue;
					}
					StateContainer<LETTER, STATE> succCont = m_States.get(succ);
					boolean modified = propagateReachableAfterRemovalProperty(cont, succCont);
					addToWorklistIfModfiedOrNotVisited(propagationWorklist, visited, modified, succCont);
				}
				for (SummaryReturnTransition<LETTER, STATE> inTrans : returnSummarySuccessor(cont.getState())) {
					STATE succ = inTrans.getSucc();
					if (!m_Ancestors.contains(succ)) {
						// succ will be removed
						continue;
					}
					STATE lin = inTrans.getLinPred();
					if (!m_Ancestors.contains(lin)) {
						// linear predecessor will be removed
						continue;
					}

					StateContainer<LETTER, STATE> succCont = m_States.get(succ);
					boolean modified = propagateReachableAfterRemovalProperty(cont, succCont);
					addToWorklistIfModfiedOrNotVisited(propagationWorklist, visited, modified, succCont);
				}
				for (OutgoingCallTransition<LETTER, STATE> inTrans : cont.callSuccessors()) {
					STATE succ = inTrans.getSucc();
					if (!m_Ancestors.contains(succ)) {
						// succ will be removed
						continue;
					}

					StateContainer<LETTER, STATE> succCont = m_States.get(succ);
					boolean modified = false;
					addToWorklistIfModfiedOrNotVisited(propagationWorklist, visited, modified, succCont);
				}
			}
		}

		private void addToWorklistIfModfiedOrNotVisited(
				LinkedHashSet<StateContainer<LETTER, STATE>> propagationWorklist,
				Set<StateContainer<LETTER, STATE>> visited, boolean modified, StateContainer<LETTER, STATE> succCont) {
			if (modified || !visited.contains(succCont)) {
				propagationWorklist.add(succCont);
				visited.add(succCont);
			}
		}

		/**
		 * Returns true if property was modified.
		 */
		private boolean propagateReachableAfterRemovalProperty(StateContainer<LETTER, STATE> cont,
				StateContainer<LETTER, STATE> succCont) throws AssertionError {
			boolean modified = false;
			if (succCont.getReachProp() == m_rpAllDown) {
				// do nothing
			} else if (succCont.getReachProp() == m_rpSomeDown) {
				for (STATE down : succCont.getDownStates().keySet()) {
					if (succCont.hasDownProp(down, m_DspReachPrecious)
							|| succCont.hasDownProp(down, m_DspReachableAfterRemoval)) {
						// do nothing
					} else {
						// check if we can propagate some down state
						if (cont.getDownStates().containsKey(down)) {
							if (cont.getReachProp() == m_rpAllDown) {
								modified |= succCont.setDownProp(down, m_DspReachableAfterRemoval);
							} else {
								if (cont.hasDownProp(down, m_DspReachPrecious)
										|| cont.hasDownProp(down, m_DspReachableAfterRemoval)) {
									modified |= succCont.setDownProp(down, m_DspReachableAfterRemoval);
								} else {
									// DoubleDecker (cont,down) has neither
									// m_DspReachPrecious nor
									// m_DspReachableAfterRemoval property
								}
							}

						}
					}
				}
			} else {
				throw new AssertionError("succ will be removed");
			}
			return modified;
		}

		// private void addToWorklistIfNotAlreadyVisited(
		// ArrayDeque<StateContainer<LETTER, STATE>> propagationWorklist,
		// Set<StateContainer<LETTER, STATE>> visited,
		// StateContainer<LETTER, STATE> succCont) {
		// boolean alreadyVisited = visited.contains(succCont);
		// if (!alreadyVisited) {
		// propagationWorklist.add(succCont);
		// visited.add(succCont);
		// }
		// // return alreadyVisited;
		// }

		/**
		 * Return true iff the DoubleDecker (up,down) is reachable in the
		 * original automaton (before removal of deadEnds or non-live states).
		 * This is a workaround to maintain backward compatibility with the old
		 * implementation. In the future we return true if (up,down) is
		 * reachable in the resulting automaton
		 */
		public boolean isDownState(STATE up, STATE down) {
			StateContainer<LETTER, STATE> cont = m_States.get(up);
			assert (cont.getReachProp() == m_rpAllDown || cont.getReachProp() == m_rpSomeDown);
			if (cont.getDownStates().containsKey(down)) {
				if (cont.getReachProp() == m_rpAllDown) {
					assert (cont.getDownStates().containsKey(down));
					return true;
				} else {
					assert cont.getReachProp() == m_rpSomeDown;
					boolean notRemoved = cont.hasDownProp(down, m_DspReachPrecious)
							|| cont.hasDownProp(down, m_DspReachableAfterRemoval);
					return notRemoved;
				}
			} else {
				return false;
			}
		}

		/**
		 * Returns the set of all down states such that (up,down) is reachable
		 * DoubleDecker in original automaton (before removal of deadEnds or
		 * non-live states). This is a workaround to maintain backward
		 * compatibility with the old implementation. In the future we return
		 * set of down states in resulting automaton.
		 */
		public Set<STATE> getDownStates(STATE state) {
			StateContainer<LETTER, STATE> cont = m_States.get(state);
			// return cont.getDownStates().keySet();
			Set<STATE> downStates;
			if (cont.getReachProp() == m_rpAllDown) {
				downStates = cont.getDownStates().keySet();
			} else {
				assert cont.getReachProp() == m_rpSomeDown;
				downStates = new HashSet<STATE>();
				for (STATE down : cont.getDownStates().keySet()) {
					boolean notRemoved = cont.hasDownProp(down, m_DspReachPrecious)
							|| cont.hasDownProp(down, m_DspReachableAfterRemoval);
					if (notRemoved) {
						downStates.add(down);
					}
				}
			}

			// for(Entry<LETTER,STATE> entry :
			// m_States.get(up).getCommonEntriesComponent().getEntries()) {
			// STATE entryState = entry.getState();
			// for (IncomingCallTransition<LETTER, STATE> trans :
			// callPredecessors(entryState)) {
			// STATE callPred = trans.getPred();
			// StateContainer<LETTER, STATE> callPredCont =
			// m_States.get(callPred);
			// if (callPredCont.getReachProp() != ReachProp.REACHABLE) {
			// downStates.add(callPred);
			// }
			// }
			// if (m_initialStatesAfterDeadEndRemoval.contains(entryState)) {
			// downStates.add(getEmptyStackState());
			// }
			// }
			return downStates;
		}

		/**
		 * Return true if the DoubleDecker (state,auxiliaryEmptyStackState) can
		 * reach a precious state (finals DeadEndComputation, accepting SSCs in
		 * non-live computation)
		 */
		public boolean isInitial(STATE state) {
			if (!m_initialStates.contains(state)) {
				throw new IllegalArgumentException("Not initial state");
			}
			StateContainer<LETTER, STATE> cont = m_States.get(state);
			if (cont.getReachProp() == m_rpAllDown) {
				// assert cont.getDownStates().get(getEmptyStackState()) ==
				// ReachProp.REACHABLE;
				return true;
			} else {
				if (cont.hasDownProp(getEmptyStackState(), m_DspReachPrecious)) {
					return true;
				} else {
					return false;
				}
			}
		}

		/**
		 * returns all triples (up,down,entry) such that from the DoubleDecker
		 * (up,down) the starting states of this ancestor computation (e.g.,
		 * final states in dead end computation) is reachable. This is a
		 * workaround to maintain backward compatibility. In the future we
		 * return triples reachable in resulting automaton.
		 * 
		 * @return
		 */
		public Iterable<UpDownEntry<STATE>> getRemovedUpDownEntry() {

			return new Iterable<UpDownEntry<STATE>>() {

				@Override
				public Iterator<UpDownEntry<STATE>> iterator() {
					return new Iterator<UpDownEntry<STATE>>() {
						private Iterator<STATE> m_UpIterator;
						private STATE m_Up;
						private Iterator<STATE> m_DownIterator;
						private STATE m_Down;
						boolean m_hasNext = true;
						private StateContainer<LETTER, STATE> m_StateContainer;

						{
							m_UpIterator = m_States.keySet().iterator();
							if (m_UpIterator.hasNext()) {
								m_Up = m_UpIterator.next();
								m_StateContainer = m_States.get(m_Up);
								m_DownIterator = m_StateContainer.getDownStates().keySet().iterator();
							} else {
								m_hasNext = false;
							}
							computeNextElement();

						}

						private void computeNextElement() {
							m_Down = null;
							while (m_Down == null && m_hasNext) {
								if (m_StateContainer.getReachProp() != m_rpAllDown && m_DownIterator.hasNext()) {
									STATE downCandidate = m_DownIterator.next();
									if (m_StateContainer.getReachProp() == ReachProp.REACHABLE) {
										m_Down = downCandidate;
									} else {
										assert m_StateContainer.getReachProp() == m_rpSomeDown;
										if (!m_StateContainer.hasDownProp(downCandidate, m_DspReachPrecious)) {
											m_Down = downCandidate;
										}
									}
								} else {
									if (m_UpIterator.hasNext()) {
										m_Up = m_UpIterator.next();
										m_StateContainer = m_States.get(m_Up);
										m_DownIterator = m_StateContainer.getDownStates().keySet().iterator();
									} else {
										m_hasNext = false;
									}
								}

							}
						}

						@Override
						public boolean hasNext() {
							return m_hasNext;
						}

						@Override
						public UpDownEntry<STATE> next() {
							if (!hasNext()) {
								throw new NoSuchElementException();
							}
							STATE entry;
							Set<STATE> callSuccs = computeState2CallSuccs(m_Down);
							if (callSuccs.size() > 1) {
								throw new UnsupportedOperationException("State has more than one call successor");
							} else if (callSuccs.size() == 1) {
								entry = callSuccs.iterator().next();
							} else {
								entry = null;
								assert m_Down == getEmptyStackState();
							}
							UpDownEntry<STATE> result = new UpDownEntry<STATE>(m_Up, m_Down, entry);
							computeNextElement();
							return result;
						}

						@Override
						public void remove() {
							throw new UnsupportedOperationException();
						}

					};

				}

				/**
				 * Compute call successors for a given set of states.
				 */
				private Set<STATE> computeState2CallSuccs(STATE state) {
					Set<STATE> callSuccs = new HashSet<STATE>();
					if (state != getEmptyStackState()) {
						for (LETTER letter : lettersCall(state)) {
							for (STATE succ : succCall(state, letter)) {
								callSuccs.add(succ);
							}
						}
					}
					return callSuccs;
				}

			};

		}

	}

	// //////////////////////////////////////////////////////////////////////////

	/**
	 * Detect which summaries are accepting. Find states q and q' such that q'
	 * is reachable from q via a run that
	 * <ul>
	 * <li>starts with a call
	 * <li>ends with a return
	 * <li>contains an accepting state
	 * </ul>
	 * The resulting map has call predecessors in its keySet and sets of return
	 * successors in its values.
	 */
	class AcceptingSummariesComputation {
		private final ArrayDeque<StateContainer<LETTER, STATE>> m_FinAncWorklist = new ArrayDeque<StateContainer<LETTER, STATE>>();
		private final HashRelation<StateContainer<LETTER, STATE>, Summary<LETTER, STATE>> m_AcceptingSummaries = new HashRelation<StateContainer<LETTER, STATE>, Summary<LETTER, STATE>>();

		public AcceptingSummariesComputation() {
			init();
			while (!m_FinAncWorklist.isEmpty()) {
				StateContainer<LETTER, STATE> cont = m_FinAncWorklist.removeFirst();
				propagateNewDownStates(cont);
			}
		}

		public HashRelation<StateContainer<LETTER, STATE>, Summary<LETTER, STATE>> getAcceptingSummaries() {
			return m_AcceptingSummaries;
		}

		private void init() {
			for (STATE fin : m_finalStates) {
				StateContainer<LETTER, STATE> cont = m_States.get(fin);
				addNewDownStates(null, cont, cont.getDownStates().keySet());
			}
		}

		private void addNewDownStates(StateContainer<LETTER, STATE> cont, StateContainer<LETTER, STATE> succCont,
				Set<STATE> potentiallyNewDownStates) {
			if (cont == succCont) {
				return;
			} else {
				boolean newDownStateWasPropagated = false;
				for (STATE down : potentiallyNewDownStates) {
					boolean modified = succCont.setDownProp(down, DownStateProp.REACHABLE_FROM_FINAL_WITHOUT_CALL);
					if (modified) {
						newDownStateWasPropagated = true;
					}
				}
				if (newDownStateWasPropagated) {
					m_FinAncWorklist.add(succCont);
				}
			}
		}

		private void propagateNewDownStates(StateContainer<LETTER, STATE> cont) {
			Set<STATE> unpropagatedDownStates = cont.getUnpropagatedDownStates();
			if (unpropagatedDownStates == null) {
				return;
			}
			for (OutgoingInternalTransition<LETTER, STATE> trans : cont.internalSuccessors()) {
				StateContainer<LETTER, STATE> succCont = m_States.get(trans.getSucc());
				addNewDownStates(cont, succCont, unpropagatedDownStates);
			}
			for (SummaryReturnTransition<LETTER, STATE> trans : returnSummarySuccessor(cont.getState())) {
				StateContainer<LETTER, STATE> succCont = m_States.get(trans.getSucc());
				addNewDownStates(cont, succCont, unpropagatedDownStates);
			}
			cont.eraseUnpropagatedDownStates();
			for (OutgoingReturnTransition<LETTER, STATE> trans : cont.returnSuccessors()) {
				StateContainer<LETTER, STATE> hierCont = m_States.get(trans.getHierPred());
				StateContainer<LETTER, STATE> succCont = m_States.get(trans.getSucc());
				STATE hierPred = trans.getHierPred();
				if (cont.hasDownProp(hierPred, DownStateProp.REACHABLE_FROM_FINAL_WITHOUT_CALL)) {
					addNewDownStates(null, succCont, hierCont.getDownStates().keySet());
					addAcceptingSummary(hierCont, cont, trans.getLetter(), succCont);
				}
			}
		}

		private void addAcceptingSummary(StateContainer<LETTER, STATE> callPred,
				StateContainer<LETTER, STATE> returnPred, LETTER letter, StateContainer<LETTER, STATE> returnSucc) {
			Summary<LETTER, STATE> summary = new Summary<LETTER, STATE>(callPred, returnPred, letter, returnSucc);
			m_AcceptingSummaries.addPair(callPred, summary);
		}

	}

	

	// //////////////////////////////////////////////////////////////////////////
	// Methods to check correctness

	public boolean containsInternalTransition(STATE state, LETTER letter, STATE succ) {
		return m_States.get(state).containsInternalTransition(letter, succ);
	}

	public boolean containsCallTransition(STATE state, LETTER letter, STATE succ) {
		return m_States.get(state).containsCallTransition(letter, succ);
	}

	public boolean containsReturnTransition(STATE state, STATE hier, LETTER letter, STATE succ) {
		return m_States.get(state).containsReturnTransition(hier, letter, succ);
	}

	protected boolean containsSummaryReturnTransition(STATE lin, STATE hier, LETTER letter, STATE succ) {
		for (SummaryReturnTransition<LETTER, STATE> trans : returnSummarySuccessor(letter, hier)) {
			if (succ.equals(trans.getSucc()) && lin.equals(trans.getLinPred())) {
				return true;
			}
		}
		return false;
	}

	private boolean checkTransitionsReturnedConsistent() {
		boolean result = true;
		for (STATE state : getStates()) {
			for (IncomingInternalTransition<LETTER, STATE> inTrans : internalPredecessors(state)) {
				result &= containsInternalTransition(inTrans.getPred(), inTrans.getLetter(), state);
				assert result;
				StateContainer<LETTER, STATE> cont = m_States.get(state);
				result &= cont.lettersInternalIncoming().contains(inTrans.getLetter());
				assert result;
				result &= cont.predInternal(inTrans.getLetter()).contains(inTrans.getPred());
				assert result;
			}
			for (OutgoingInternalTransition<LETTER, STATE> outTrans : internalSuccessors(state)) {
				result &= containsInternalTransition(state, outTrans.getLetter(), outTrans.getSucc());
				assert result;
				StateContainer<LETTER, STATE> cont = m_States.get(state);
				result &= cont.lettersInternal().contains(outTrans.getLetter());
				assert result;
				result &= cont.succInternal(outTrans.getLetter()).contains(outTrans.getSucc());
				assert result;
			}
			for (IncomingCallTransition<LETTER, STATE> inTrans : callPredecessors(state)) {
				result &= containsCallTransition(inTrans.getPred(), inTrans.getLetter(), state);
				assert result;
				StateContainer<LETTER, STATE> cont = m_States.get(state);
				result &= cont.lettersCallIncoming().contains(inTrans.getLetter());
				assert result;
				result &= cont.predCall(inTrans.getLetter()).contains(inTrans.getPred());
				assert result;
			}
			for (OutgoingCallTransition<LETTER, STATE> outTrans : callSuccessors(state)) {
				result &= containsCallTransition(state, outTrans.getLetter(), outTrans.getSucc());
				assert result;
				StateContainer<LETTER, STATE> cont = m_States.get(state);
				result &= cont.lettersCall().contains(outTrans.getLetter());
				assert result;
				result &= cont.succCall(outTrans.getLetter()).contains(outTrans.getSucc());
				assert result;
			}
			for (IncomingReturnTransition<LETTER, STATE> inTrans : returnPredecessors(state)) {
				result &= containsReturnTransition(inTrans.getLinPred(), inTrans.getHierPred(), inTrans.getLetter(),
						state);
				assert result;
				result &= containsSummaryReturnTransition(inTrans.getLinPred(), inTrans.getHierPred(),
						inTrans.getLetter(), state);
				assert result;
				StateContainer<LETTER, STATE> cont = m_States.get(state);
				result &= cont.lettersReturnIncoming().contains(inTrans.getLetter());
				assert result;
				result &= cont.predReturnHier(inTrans.getLetter()).contains(inTrans.getHierPred());
				assert result;
				result &= cont.predReturnLin(inTrans.getLetter(), inTrans.getHierPred()).contains(inTrans.getLinPred());
				assert result;
			}
			for (OutgoingReturnTransition<LETTER, STATE> outTrans : returnSuccessors(state)) {
				result &= containsReturnTransition(state, outTrans.getHierPred(), outTrans.getLetter(),
						outTrans.getSucc());
				assert result;
				result &= containsSummaryReturnTransition(state, outTrans.getHierPred(), outTrans.getLetter(),
						outTrans.getSucc());
				assert result;
				StateContainer<LETTER, STATE> cont = m_States.get(state);
				result &= cont.lettersReturn().contains(outTrans.getLetter());
				assert result;
				result &= cont.hierPred(outTrans.getLetter()).contains(outTrans.getHierPred());
				assert result;
				result &= cont.succReturn(outTrans.getHierPred(), outTrans.getLetter()).contains(outTrans.getSucc());
				assert result;
			}
			// for (LETTER letter : lettersReturnSummary(state)) {
			// for (SummaryReturnTransition<LETTER, STATE> sumTrans :
			// returnSummarySuccessor(letter, state)) {
			// result &= containsReturnTransition(state, sumTrans.getHierPred(),
			// outTrans.getLetter(), outTrans.getSucc());
			// assert result;
			// StateContainer<LETTER, STATE> cont = m_States.get(state);
			// result &= cont.lettersReturn().contains(outTrans.getLetter());
			// assert result;
			// result &=
			// cont.hierPred(outTrans.getLetter()).contains(outTrans.getHierPred());
			// assert result;
			// result &=
			// cont.succReturn(outTrans.getHierPred(),outTrans.getLetter()).contains(outTrans.getSucc());
			// assert result;
			// }
			// }

			for (LETTER letter : lettersInternal(state)) {
				for (STATE succ : succInternal(state, letter)) {
					result &= containsInternalTransition(state, letter, succ);
					assert result;
				}
			}
			for (LETTER letter : lettersCall(state)) {
				for (STATE succ : succCall(state, letter)) {
					result &= containsCallTransition(state, letter, succ);
					assert result;
				}
			}
			for (LETTER letter : lettersReturn(state)) {
				for (STATE hier : hierPred(state, letter)) {
					for (STATE succ : succReturn(state, hier, letter)) {
						result &= containsReturnTransition(state, hier, letter, succ);
						assert result;
					}
				}
			}
			for (LETTER letter : lettersInternalIncoming(state)) {
				for (STATE pred : predInternal(state, letter)) {
					result &= containsInternalTransition(pred, letter, state);
					assert result;
				}
			}
			for (LETTER letter : lettersCallIncoming(state)) {
				for (STATE pred : predCall(state, letter)) {
					result &= containsCallTransition(pred, letter, state);
					assert result;
				}
			}
			for (IncomingReturnTransition<LETTER, STATE> inTrans : returnPredecessors(state)) {
				result &= containsReturnTransition(inTrans.getLinPred(), inTrans.getHierPred(), inTrans.getLetter(),
						state);
				assert result;
			}
		}

		return result;
	}

	// private boolean cecSumConsistent() {
	// int sum = 0;
	// for (CommonEntriesComponent<LETTER,STATE> cec : m_AllCECs) {
	// sum += cec.m_Size;
	// }
	// int allStates = m_States.keySet().size();
	// return sum == allStates;
	// }
	//
	// private boolean allStatesAreInTheirCec() {
	// boolean result = true;
	// for (STATE state : m_States.keySet()) {
	// StateContainer<LETTER,STATE> sc = m_States.get(state);
	// CommonEntriesComponent<LETTER,STATE> cec =
	// sc.getCommonEntriesComponent();
	// if (!cec.m_BorderOut.keySet().contains(sc)) {
	// Set<StateContainer<LETTER,STATE>> empty = new
	// HashSet<StateContainer<LETTER,STATE>>();
	// result &= internalOutSummaryOutInCecOrForeigners(sc, empty, cec);
	// }
	// }
	// return result;
	// }
	//
	// private boolean
	// occuringStatesAreConsistent(CommonEntriesComponent<LETTER,STATE> cec) {
	// boolean result = true;
	// Set<STATE> downStates = cec.m_DownStates;
	// Set<Entry<LETTER,STATE>> entries = cec.m_Entries;
	// if (cec.m_Size > 0) {
	// result &= downStatesAreCallPredsOfEntries(downStates, entries);
	// }
	// assert (result);
	// result &= eachStateHasThisCec(cec.getReturnOutCandidates(), cec);
	// assert (result);
	// for (StateContainer<LETTER, STATE> resident : cec.m_BorderOut.keySet()) {
	// Set<StateContainer<LETTER,STATE>> foreignerSCs =
	// cec.m_BorderOut.get(resident);
	// result &= internalOutSummaryOutInCecOrForeigners(resident, foreignerSCs,
	// cec);
	// assert (result);
	// }
	// return result;
	// }
	//
	//
	// private boolean
	// downStatesConsistentwithEntriesDownStates(CommonEntriesComponent<LETTER,STATE>
	// cec) {
	// boolean result = true;
	// Set<STATE> downStates = cec.m_DownStates;
	// Set<Entry<LETTER,STATE>> entries = cec.m_Entries;
	// Set<STATE> downStatesofEntries = new HashSet<STATE>();
	// for (Entry<LETTER,STATE> entry : entries) {
	// downStatesofEntries.addAll(entry.getDownStates().keySet());
	// }
	// result &= isSubset(downStates, downStatesofEntries);
	// assert (result);
	// result &= isSubset(downStatesofEntries, downStates);
	// assert (result);
	// return result;
	// }
	//
	// private boolean
	// internalOutSummaryOutInCecOrForeigners(StateContainer<LETTER, STATE>
	// state, Set<StateContainer<LETTER,STATE>> foreigners,
	// CommonEntriesComponent<LETTER,STATE> cec) {
	// Set<StateContainer<LETTER,STATE>> neighbors = new
	// HashSet<StateContainer<LETTER,STATE>>();
	//
	// for (OutgoingInternalTransition<LETTER, STATE> trans :
	// state.internalSuccessors()) {
	// STATE succ = trans.getSucc();
	// StateContainer<LETTER,STATE> succSc = m_States.get(succ);
	// if (succSc.getCommonEntriesComponent() == cec) {
	// // do nothing
	// } else {
	// neighbors.add(succSc);
	// }
	// }
	// if (m_Summaries.containsKey(state)) {
	// for (StateContainer<LETTER,STATE> succSc : m_Summaries.get(state)) {
	// if (succSc.getCommonEntriesComponent() == cec) {
	// // do nothing
	// } else {
	// neighbors.add(succSc);
	// }
	// }
	// }
	// boolean allNeighborAreForeigners = isSubset(neighbors, foreigners);
	// assert allNeighborAreForeigners;
	// boolean allForeignersAreNeighbor = isSubset(foreigners, neighbors);
	// assert allForeignersAreNeighbor;
	// return allNeighborAreForeigners && allForeignersAreNeighbor;
	// }
	//
	// private boolean eachStateHasThisCec(Set<STATE> states,
	// CommonEntriesComponent<LETTER,STATE> cec) {
	// boolean result = true;
	// for (STATE state : states) {
	// StateContainer<LETTER, STATE> sc = m_States.get(state);
	// if (sc.getCommonEntriesComponent() != cec) {
	// result = false;
	// assert result;
	// }
	// }
	// return result;
	// }
	//
	// private boolean downStatesAreCallPredsOfEntries(Set<STATE> downStates,
	// Set<Entry<LETTER,STATE>> entries) {
	// Set<STATE> callPreds = new HashSet<STATE>();
	// for (Entry<LETTER,STATE> entry : entries) {
	// STATE entryState = entry.getState();
	// if (isInitial(entryState)) {
	// callPreds.add(getEmptyStackState());
	// }
	// for (IncomingCallTransition<LETTER, STATE> trans :
	// callPredecessors(entryState)) {
	// callPreds.add(trans.getPred());
	// }
	// }
	// boolean callPredsIndownStates = isSubset(callPreds, downStates);
	// assert (callPredsIndownStates);
	// boolean downStatesInCallPreds = isSubset(downStates, callPreds);
	// assert (downStatesInCallPreds);
	// return callPredsIndownStates && downStatesInCallPreds;
	// }
	//
	// private boolean isBorderOutConsistent(StateContainer<LETTER,STATE> cont)
	// {
	// CommonEntriesComponent<LETTER, STATE> cec =
	// cont.getCommonEntriesComponent();
	// ArrayList<STATE> preds = new ArrayList<STATE>();
	// for(IncomingInternalTransition<LETTER, STATE> inTrans :
	// internalPredecessors(cont.getState())) {
	// preds.add(inTrans.getPred());
	// }
	// for(IncomingReturnTransition<LETTER, STATE> inTrans :
	// returnPredecessors(cont.getState())) {
	// preds.add(inTrans.getHierPred());
	// }
	// boolean result = true;
	// for (STATE pred : preds) {
	// StateContainer<LETTER, STATE> predCont = m_States.get(pred);
	// if (predCont.getCommonEntriesComponent() != cec) {
	// if
	// (predCont.getCommonEntriesComponent().m_BorderOut.containsKey(predCont))
	// {
	// Set<StateContainer<LETTER, STATE>> foreigners =
	// predCont.getCommonEntriesComponent().m_BorderOut.get(predCont);
	// result &= foreigners.contains(cont);
	// } else {
	// result = false;
	// }
	// assert result;
	// } else {
	// if
	// (predCont.getCommonEntriesComponent().m_BorderOut.containsKey(predCont))
	// {
	// Set<StateContainer<LETTER, STATE>> foreigners =
	// predCont.getCommonEntriesComponent().m_BorderOut.get(predCont);
	// result&= !foreigners.contains(cont);
	// assert result;
	// }
	// }
	// }
	// return result;
	// }

	// //////////////////////////////////////////////////////////////////////////
	// Auxilliary Methods

	public static <E> boolean noElementIsNull(Collection<E> collection) {
		for (E elem : collection) {
			if (elem == null)
				return false;
		}
		return true;
	}

	private static <E> boolean isSubset(Set<E> lhs, Set<E> rhs) {
		for (E elem : lhs) {
			if (!rhs.contains(elem)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return (new AtsDefinitionPrinter<String, String>(m_Services, "nwa", this)).getDefinitionAsString();
	}

	@Override
	public Collection<SCComponentForNWARS<LETTER, STATE>> computeBalls(Set<STATE> stateSubset,
			Set<STATE> startStates) {
		if (!getStates().containsAll(stateSubset)) {
			throw new IllegalArgumentException("not a subset of the automaton's states: " + stateSubset);
		}
		if (!stateSubset.containsAll(startStates)) {
			throw new IllegalArgumentException("start states must be restricted to your subset");
		}

		
		if (m_AcceptingSummaries == null) {
			m_AcceptingSummaries = new AcceptingSummariesComputation();
		}
		AcceptingComponentsAnalysis<LETTER, STATE> sccComputation = 
				new AcceptingComponentsAnalysis<>(this, m_AcceptingSummaries, m_Services, stateSubset, startStates);
		return sccComputation.getSccComputation().getBalls();
	}
	
	


}
