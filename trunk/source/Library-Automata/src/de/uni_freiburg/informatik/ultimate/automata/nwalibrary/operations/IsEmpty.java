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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.DoubleDecker;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonSimple;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedRun;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingReturnTransition;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.util.Utils;


/**
 * Check emptiness and obtain an accepting run of a nested word automaton.
 * The algorithm computes a reachability graph. The nodes of the graph describe
 * a configuration of the automaton. They are represented by state pairs 
 * (state,stateK) where state is the "current state" in the reachability
 * analysis of the automaton, stateK is the last state before the last call
 * transition. If we consider the automaton as a machine with a stack, stateK
 * is the topmost element of the stack.
 * The edges of the reachability graph are labeled with runs of length two or
 * summary.
 * The reachability graph is obtained by traversing the automaton in a BFS 
 * manner. 
 * @author heizmann@informatik.uni-freiburg.de
 *
 */

public class IsEmpty<LETTER,STATE> implements IOperation<LETTER,STATE> {
	private final AutomataLibraryServices m_Services;
	private final ILogger m_Logger;
	
	@Override
	public String operationName() {
		return "isEmpty";
	}
	
	@Override
	public String startMessage() {
		return "Start " + operationName() +
				". Operand " + m_nwa.sizeInformation();
	}
	
	@Override
	public String exitMessage() {
		if (m_acceptingRun == null) {
			return "Finished " + operationName() +
				". No accepting run.";
		}
		else {
			return "Finished " + operationName() +
				". Found accepting run of length " + m_acceptingRun.getLength();
		}
	}
	
	/**
	 * Set of states in which the run we are searching has to begin.
	 */
	private final Collection<STATE> m_StartStates;

	/**
	 * Set of states in which the run we are searching has to end.
	 */
	private final Collection<STATE> m_GoalStates;
	
	/**
	 * If set, the goal states are exactly the accepting states of automaton 
	 * m_nwa, the set m_GoalStates is null, and we use m_nwa to check if a 
	 * state is a goal state.
	 */
	private final boolean m_GoalStateIsAcceptingState;
	
	/**
	 * Set of states in which the run we are searching must not visit.
	 */
	private final Collection<STATE> m_ForbiddenStates;
	
	/**
	 * INestedWordAutomaton for which we check emptiness.
	 */
	INestedWordAutomatonSimple<LETTER,STATE> m_nwa;
	
	NestedRun<LETTER,STATE> m_acceptingRun;
	
	/**
	 * Pairs of states visited, but possibly not processed, so far.
	 */
	private final Map<STATE,Set<STATE>> m_visitedPairs = 
		new HashMap<STATE,Set<STATE>>();
	
	/**
	 * Queue of states that have to be processed and have been visited while
	 * processing a internal transition, a return transition or a computed 
	 * summary
	 */
	private final LinkedList<DoubleDecker<STATE>> m_queue =
		new LinkedList<DoubleDecker<STATE>>();

		
	/**
	 * Queue of states that have to be processed and have been visited while
	 * processing a call transition.
	 */
	private final LinkedList<DoubleDecker<STATE>> m_queueCall =
			new LinkedList<DoubleDecker<STATE>>();

	/**
	 * Assigns to a pair of states (state,stateK) the run of length 2 that is
	 * labeled to the incoming edge of (state,stateK) in the reachability graph.
	 * The symbol of the run has to be an internal symbol. The predecessor of
	 * (state,stateK) in the reachability graph is (pred,predK), where pred is 
	 * the first state of the run and predK is stateK.  
	 */
	private final Map<STATE,
	                  Map<STATE,NestedRun<LETTER,STATE>>> m_internalSubRun =
		new HashMap<STATE,Map<STATE,NestedRun<LETTER,STATE>>>();
	
	/**
	 * Assigns to a triple of states (state,stateK,predK) the run of length 2
	 * that is labeled to the incoming edge of the state pair (state,stateK)
	 * in the reachability graph. The symbol of the run has to be a call symbol.
	 * The predecessor of (state,stateK) in the reachability graph is 
	 * (pred,predK), where pred is stateK.  
	 */
	private final Map<STATE,
	                  Map<STATE,
	                      Map<STATE,NestedRun<LETTER,STATE>>>> m_callSubRun =
		new HashMap<STATE,
		            Map<STATE,Map<STATE,NestedRun<LETTER,STATE>>>>();
	
	/**
	 * Assigns to a pair of states (state,stateK) a state predK. predK is the
	 * second component of the state pair (pred,predK) for which (state,stateK)
	 * was added to the reachability graph (for the first time).  
	 */
	private final Map<STATE,Map<STATE,STATE>> m_callFirst =
		new HashMap<STATE,Map<STATE,STATE>>();


	/**
	 * Assigns to a pair of states (state,stateK) the run of length 2 that is
	 * labeled to the incoming edge of (state,stateK) in the reachability graph.
	 * The symbol of the run has to be a return symbol. The predecessor of
	 * (state,stateK) in the reachability graph is (pred,predK), where pred is 
	 * the first state of the run. predK can be obtained from m_returnPredStateK  
	 */
	private final Map<STATE,
				      Map<STATE,NestedRun<LETTER,STATE>>> m_returnSubRun = 
		new HashMap<STATE,Map<STATE,NestedRun<LETTER,STATE>>>();
	
	/**
	 * Assigns to a pair of states (state,stateK) a state predK. predK is the
	 * second component of the predecessor (pred,predK) of (state,stateK) in the
	 * reachability graph.
	 */
	private final Map<STATE,
	                  Map<STATE,STATE>> m_returnPredStateK = 
		new HashMap<STATE,Map<STATE,STATE>>();
	
	
	
	/**
	 * If a triple (state,succ,returnPred) is contained in this map, a summary
	 * from state to succ has been discovered and returnPred is the predecessor
	 * of the return transition of this summary.
	 */
	private final Map<STATE,
	                  Map<STATE,STATE>> m_summaryReturnPred =
		new HashMap<STATE,Map<STATE,STATE>>();

	/**
	 * If a triple (state,succ,symbol) is contained in this map, a summary
	 * from state to succ has been discovered and symbol is the label of the
	 * return transition of this summary.
	 */
	private final Map<STATE,
	                  Map<STATE,LETTER>> m_summaryReturnSymbol =
		new HashMap<STATE,Map<STATE,LETTER>>();


	
	/**
	 * Second Element of the initial state pair. This state indicates that
	 * nothing is on the stack of the automaton, in other words while processing
	 * we have taken the same number of calls and returns.
	 */
	private final STATE dummyEmptyStackState;
	

	/**
	 * Stack for the constructing a run if non-emptiness was detected. Contains
	 * an element for every return that was processed and to corresponding call
	 * was processed yet.
	 * Corresponds to the stack-of-returned-elements-that-have-not-been-called
	 * of the automaton but all elements are shifted by one.   
	 */
	Stack<STATE> m_reconstructionStack = new Stack<STATE>();

	private NestedRun<LETTER,STATE> m_ReconstructionOneStepRun;

	private STATE m_ReconstructionPredK;


	
	/**
	 * Default constructor. Here we search a run from the initial states
	 * of the automaton to the final states of the automaton.
	 */
	public IsEmpty(AutomataLibraryServices services,
			INestedWordAutomatonSimple<LETTER,STATE> nwa) {
		m_Services = services;
		m_Logger = m_Services.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		m_nwa = nwa;
		dummyEmptyStackState = m_nwa.getEmptyStackState();
		m_StartStates = Utils.constructHashSet(m_nwa.getInitialStates());
		m_GoalStateIsAcceptingState = true;
		m_GoalStates = null;
		m_ForbiddenStates = Collections.emptySet();
		m_Logger.info(startMessage());
		m_acceptingRun = getAcceptingRun();
		m_Logger.info(exitMessage());
	}
	

	/**
	 * Constructor that is not restricted to emptiness checks. 
	 * The set of startStates defines where the run that we search has to start. 
	 * The set of forbiddenStates defines states that the run must not visit.
	 * The set of goalStates defines where the run that we search has to end.
	 */
	public IsEmpty(AutomataLibraryServices services,
			INestedWordAutomaton<LETTER,STATE> nwa, 
			Set<STATE> startStates, Set<STATE> forbiddenStates, 
			Set<STATE> goalStates) {
		m_Services = services;
		m_Logger = m_Services.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		m_nwa = nwa;
		assert nwa.getStates().containsAll(startStates) : "unknown states";
		assert nwa.getStates().containsAll(goalStates) : "unknown states";
		dummyEmptyStackState = m_nwa.getEmptyStackState();
		m_StartStates = startStates;
		m_GoalStateIsAcceptingState = false;
		m_GoalStates = goalStates;
		m_ForbiddenStates = forbiddenStates;
		m_Logger.info(startMessage());
		m_acceptingRun = getAcceptingRun();
		m_Logger.info(exitMessage());
	}
	
	/**
	 * If we use the accepting states of m_nwa as goal states (in this case 
	 * m_GoalStateIsAcceptingState is set and m_GoalStates is null) then we
	 * return true iff state is an accepting state.
	 * Otherwise we return true iff m_GoalStates.contains(state).
	 */
	private boolean isGoalState(STATE state) {
		if (m_GoalStateIsAcceptingState) {
			assert m_GoalStates == null : 
				"if we search accepting states, m_GoalStates is null";
			return m_nwa.isFinal(state);
		} else {
			return m_GoalStates.contains(state);
		}
	}
	
	
	/**
	 * Enqueue a state pair that has been discovered by taking an internal
	 * transition, a return transition or a summary. Mark the state pair as
	 * visited afterwards. 
	 */
	private void enqueueAndMarkVisited(STATE state, STATE stateK) {
		DoubleDecker<STATE> pair = new DoubleDecker<STATE>(stateK, state);
		m_queue.addLast(pair);
		markVisited(state, stateK);
	}
	
	
	/**
	 * Enqueue a state pair that has been discovered by taking a call
	 * transition. Mark the state pair as visited afterwards. 
	 */
	private void enqueueAndMarkVisitedCall(STATE state, STATE callPred) {
		DoubleDecker<STATE> pair = new DoubleDecker<STATE>(callPred, state);
		m_queueCall.addLast(pair);
		markVisited(state, callPred);
	}
	
	
//  The following implementation of dequeue is faster but leads to unsound
//	results. See BugBfsEmptinessLowPriorityCallQueue.fat for details.
//  Alternative workaround (where we may still use the low priority call queue):
//  When final state is reached, process the whole call queue before
//  computation of accepting run.
//	/**
//	 * Dequeue a state pair. If available take a state pair that has been
//	 * discovered by taking an internal transition, a return transition or a
//	 * summary. If not take a state pair that has been discovered by taking a
//	 * call transition.
//	 */
//	private IState<LETTER,STATE>[] dequeue() {
//		if (!m_queue.isEmpty()) {
//			return m_queue.removeFirst();
//		}
//		else {
//			return m_queueCall.removeFirst();
//		}
//	}
	/**
	 * Dequeue a state pair. If available take a state pair that has been
	 * discovered by taking a
	 * call transition. If not take a state pair that has been discovered by 
	 * taking an internal transition, a return transition or a
	 * summary.
	 */
	private DoubleDecker<STATE> dequeue() {
		if (!m_queueCall.isEmpty()) {
			return m_queueCall.removeFirst();
		}
		else {
			return m_queue.removeFirst();
		}
	}
	
	/**
	 * Is the queue (is internally represented by two queues) empty? 
	 */
	private boolean isQueueEmpty() {
		return m_queue.isEmpty() && m_queueCall.isEmpty();
	}
	
	
	/**
	 * Mark a state pair a visited. 
	 */
	private void markVisited(STATE state, STATE stateK) {
		Set<STATE> callPreds = m_visitedPairs.get(state);
		if (callPreds == null) {
			callPreds = new HashSet<STATE>();
			m_visitedPairs.put(state, callPreds);
		}
		callPreds.add(stateK);
	}
	
	
	/**
	 * Was the state pair (state,stateK) already visited?
	 */
	private boolean wasVisited(STATE state, STATE stateK) {
		Set<STATE> callPreds = m_visitedPairs.get(state);
		if (callPreds == null) {
			return false;
		}
		else {
			return callPreds.contains(stateK);
		}
	}
	

	
	/**
	 * Get an accepting run of the automaton passed to the constructor. Return
	 * null if the automaton does not accept any nested word.
	 */
	private NestedRun<LETTER,STATE> getAcceptingRun() {
		for (STATE state : m_StartStates) {
			enqueueAndMarkVisited(state, dummyEmptyStackState);
		}
	
		while (!isQueueEmpty()) {
			DoubleDecker<STATE> pair = dequeue();
			STATE state = pair.getUp();
			STATE stateK = pair.getDown();
			
			if (isGoalState(state)) {
				return constructRun(state, stateK);
			}
			
			processSummaries(state,stateK);
			
			for (OutgoingInternalTransition<LETTER, STATE> internalTransition : 
											m_nwa.internalSuccessors(state)) {
				LETTER symbol = internalTransition.getLetter();
				STATE succ = internalTransition.getSucc();
				if (!m_ForbiddenStates.contains(succ)) {
					if(!wasVisited(succ, stateK)) {
						addRunInformationInternal(
								succ,stateK,symbol,state,stateK);
						enqueueAndMarkVisited(succ, stateK);
					}
				}
			}

			for (OutgoingCallTransition<LETTER, STATE> callTransition : 
												m_nwa.callSuccessors(state)) {
				LETTER symbol = callTransition.getLetter();
				STATE succ = callTransition.getSucc();
				if (!m_ForbiddenStates.contains(succ)) {
					//add these information even in already visited
					addRunInformationCall(succ, state, symbol, state, stateK);
					if(!wasVisited(succ, state)) {
						enqueueAndMarkVisitedCall(succ, state);
					}
				}
			}
			
			if (stateK == m_nwa.getEmptyStackState()) {
				//there is no return transition
				continue;
			}
			
			for (OutgoingReturnTransition<LETTER, STATE> returnTransition : 
							m_nwa.returnSuccessorsGivenHier(state, stateK)) {
				LETTER symbol = returnTransition.getLetter();
				STATE succ = returnTransition.getSucc();
				if (!m_ForbiddenStates.contains(succ)) {
					for (STATE stateKK : getCallStatesOfCallState(stateK) ) {
						addSummary(stateK, succ, state, symbol);
						if(!wasVisited(succ, stateKK)) {
							enqueueAndMarkVisited(succ, stateKK);
							addRunInformationReturn(succ, stateKK, symbol, state, stateK);
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Compute successor state pairs (succ,succK) of the state pair
	 * (state,stateK) under an already discovered summary in the reachability
	 * graph.
	 * succK is stateK. For adding run information for (succ,succK) information
	 * about the summary is fetched. 
	 */
	private void processSummaries(STATE state, STATE stateK) {
		if (m_summaryReturnPred.containsKey(state)) {
			assert(m_summaryReturnSymbol.containsKey(state));
			Map<STATE,STATE> succ2ReturnPred = 
						m_summaryReturnPred.get(state);
			Map<STATE,LETTER> succ2ReturnSymbol = 
						m_summaryReturnSymbol.get(state);
			for (STATE succ : succ2ReturnPred.keySet()) {
				assert(succ2ReturnSymbol.containsKey(succ));
				STATE returnPred = succ2ReturnPred.get(succ);
				LETTER symbol = succ2ReturnSymbol.get(succ);
				if(!wasVisited(succ, stateK)) {
					enqueueAndMarkVisited(succ, stateK);
					addRunInformationReturn(
									succ, stateK, symbol, returnPred, state);
				}
			}
			
		}
		
	}

	
	/**
	 * Store for a state pair (succ,succK) in the reachability graph information
	 * about the predecessor (state,stateK) under an internal transition and a
	 * run of length two from state to succ.
	 */
	private void addRunInformationInternal(STATE succ,
			STATE succK,
			LETTER symbol,
			STATE state,
			STATE stateK) {
		Map<STATE, NestedRun<LETTER,STATE>> succK2run = 
													m_internalSubRun.get(succ);
		if (succK2run == null) {
			succK2run = new HashMap<STATE, NestedRun<LETTER,STATE>>();
			m_internalSubRun.put(succ, succK2run);
		}
		assert(succK2run.get(succK) == null);
		NestedRun<LETTER,STATE> run = new NestedRun<LETTER,STATE>(
							state, symbol, NestedWord.INTERNAL_POSITION, succ);
		succK2run.put(succK, run);
	}
	
	/**
	 * Store for a state pair (succ,succK) in the reachability graph information
	 * about the predecessor (state,stateK) under a call transition and a
	 * run of length two from state to succ.
	 * If (succ,succK) was visited for the first time, store also stateK in
	 * m_callFirst.
	 */
	private void addRunInformationCall(STATE succ,
			STATE succK,
			LETTER symbol,
			STATE state,
			STATE stateK) {
//		m_Logger.debug("Call SubrunInformation: From ("+succ+","+succK+
//			") can go to ("+state+","+stateK+")");
		assert(state == succK);
		Map<STATE, Map<STATE, NestedRun<LETTER,STATE>>> succK2stateK2Run = 
				m_callSubRun.get(succ);
		Map<STATE, STATE> succK2FirstStateK = 
				m_callFirst.get(succ);
		if (succK2stateK2Run == null) {
			succK2stateK2Run = 
				new HashMap<STATE, Map<STATE, NestedRun<LETTER,STATE>>>();
			m_callSubRun.put(succ,succK2stateK2Run);
			succK2FirstStateK = new HashMap<STATE, STATE>();
			m_callFirst.put(succ, succK2FirstStateK);
		}
		if (!succK2FirstStateK.containsKey(succK)) {
			succK2FirstStateK.put(succK, stateK);
		}
		Map<STATE, NestedRun<LETTER,STATE>> stateK2Run = 
				succK2stateK2Run.get(succK);
		if (stateK2Run == null) {
			stateK2Run = new HashMap<STATE, NestedRun<LETTER,STATE>>();
			succK2stateK2Run.put(succK,stateK2Run);
		}
//		The following assertion is wrong, there can be a two different call
//		transitions from stateK to state. (But in this case we always want to
//		take the one that was first discovered.)
//		assert(!stateK2Run.containsKey(stateK));
		NestedRun<LETTER,STATE> run = 
			new NestedRun<LETTER,STATE>(state, symbol, NestedWord.PLUS_INFINITY, succ);
		stateK2Run.put(stateK, run);
	}
	
	
	/**
	 * Store for a state pair (succ,succK) in the reachability graph information
	 * about the predecessor (state,stateK) under a return transition and a
	 * run of length two from state to succ.
	 * Store also succK to m_returnPredStateK.
	 */
	private void addRunInformationReturn(STATE succ,
			STATE succK,
			LETTER symbol,
			STATE state,
			STATE stateK) {
		Map<STATE, NestedRun<LETTER,STATE>> succK2SubRun = 
									m_returnSubRun.get(succ);
		Map<STATE, STATE> succK2PredStateK = 
									m_returnPredStateK.get(succ);
		if (succK2SubRun == null) {
			assert(succK2PredStateK == null);
			succK2SubRun = new HashMap<STATE, NestedRun<LETTER,STATE>>();
			m_returnSubRun.put(succ,succK2SubRun);
			succK2PredStateK = new HashMap<STATE, STATE>();
			m_returnPredStateK.put(succ, succK2PredStateK);
		}
		assert(!succK2SubRun.containsKey(succK));
		assert(!succK2PredStateK.containsKey(succK));
		NestedRun<LETTER,STATE> run = 
			new NestedRun<LETTER,STATE>(state, symbol, NestedWord.MINUS_INFINITY, succ);
		succK2SubRun.put(succK, run);
		succK2PredStateK.put(succK, stateK);
	}
	

	/**
	 * Get all states which occur as the second component of a state pair
	 * (callState,*) in the reachability graph, where the first component is
	 * callState. 
	 */
	private Set<STATE> getCallStatesOfCallState(STATE callState) {
		Set<STATE> callStatesOfCallStates = m_visitedPairs.get(callState);
		if (callStatesOfCallStates == null) {
			return new HashSet<STATE>(0);
		}
		else {
			return callStatesOfCallStates;
		}
	}
	
//	private void addCallStatesOfCallState(IState<LETTER,STATE> callState,
//			                              IState<LETTER,STATE> callStateOfCallState) {
//		Set<IState<LETTER,STATE>> callStatesOfCallStates = 
//							m_CallStatesOfCallState.get(callState);
//		if (callStatesOfCallStates == null) {
//			callStatesOfCallStates = new HashSet<IState<LETTER,STATE>>();
//			m_CallStatesOfCallState.put(callState,callStatesOfCallStates);
//		}
//		callStatesOfCallStates.add(callStateOfCallState);
//	}

	
	/**
	 * Store information about a discovered summary.
	 */
	private void addSummary(STATE stateBeforeCall,
							STATE stateAfterReturn,
							STATE stateBeforeReturn,
							LETTER returnSymbol) {
		Map<STATE,STATE> succ2ReturnPred = 
			m_summaryReturnPred.get(stateBeforeCall);
		Map<STATE,LETTER> succ2ReturnSymbol = 
			m_summaryReturnSymbol.get(stateBeforeCall);
		if (succ2ReturnPred == null) {
			assert(succ2ReturnSymbol == null);
			succ2ReturnPred = new HashMap<STATE,STATE>();
			m_summaryReturnPred.put(stateBeforeCall, succ2ReturnPred);
			succ2ReturnSymbol = new HashMap<STATE,LETTER>();
			m_summaryReturnSymbol.put(stateBeforeCall, succ2ReturnSymbol);
			
		}
		//update only if there is not already an entry
		if (!succ2ReturnPred.containsKey(stateAfterReturn)) {
			succ2ReturnPred.put(stateAfterReturn,stateBeforeReturn);
			assert (!succ2ReturnSymbol.containsKey(stateAfterReturn));
			succ2ReturnSymbol.put(stateAfterReturn, returnSymbol);
		}
	}
	
	
	
	
	/**
	 * Construct a run for a discovered final state in the reachability
	 * analysis. The run is constructed backwards using the information of
	 * predecessors in the reachability graph and the corresponding runs of
	 * length two.
	 */
	private NestedRun<LETTER,STATE> constructRun(STATE state, STATE stateK) {
//		m_Logger.debug("Reconstruction from " + state + " " + stateK);
		NestedRun<LETTER,STATE> run = new NestedRun<LETTER,STATE>(state);
		while (!m_StartStates.contains(state) ||
				!m_reconstructionStack.isEmpty()) {
			if (computeInternalSubRun(state, stateK)) {
			}
			else if (computeCallSubRun(state, stateK)) {
			}
			else if (computeReturnSubRun(state, stateK)) {
			}
			else {
				m_Logger.warn("No Run ending in pair "+state+ "  "+ stateK + 
						" with reconstructionStack" + m_reconstructionStack);
				throw new AssertionError();
			}
			run = m_ReconstructionOneStepRun.concatenate(run);
			state = run.getStateAtPosition(0);
			stateK = m_ReconstructionPredK;
		}
		return run;
	}
	
	
	/**
	 * Return true iff the run that lead to the accepting state contains an
	 * internal transition which is succeeded by state and where stateK is the
	 * topmost stack element. 
	 */
	private boolean computeInternalSubRun(STATE state, STATE stateK) {
		Map<STATE, NestedRun<LETTER,STATE>> k2InternalMap = 
			m_internalSubRun.get(state);
		if (k2InternalMap != null) {
			NestedRun<LETTER,STATE> run = k2InternalMap.get(stateK);
			if (run != null) {
				m_ReconstructionOneStepRun = run;
				m_ReconstructionPredK = stateK;
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Return true iff the run that lead to the accepting state contains a
	 * call transition which is succeeded by state and where stateK is the
	 * topmost stack element. 
	 */
	private boolean computeCallSubRun(STATE state, STATE stateK) {
		Map<STATE, Map<STATE, NestedRun<LETTER,STATE>>> k2CallMap = 
			m_callSubRun.get(state);
		if (k2CallMap != null) {
			Map<STATE, NestedRun<LETTER,STATE>> callMap = k2CallMap.get(stateK);
			if (callMap != null) {
				if (m_reconstructionStack.isEmpty()) {
					STATE predK = m_callFirst.get(state).get(stateK);
					m_ReconstructionPredK = predK;
					m_ReconstructionOneStepRun = callMap.get(predK);
					return true;
				}
				else {
					STATE predKcandidate = m_reconstructionStack.peek();
					if (callMap.containsKey(predKcandidate)) {
						m_ReconstructionOneStepRun = callMap.get(predKcandidate);
						m_ReconstructionPredK = predKcandidate;
						m_reconstructionStack.pop();
						return true;
					}
//					throw new AssertionError();
				}
			}
		}
		return false;
	}
	
	
	/**
	 * Return true iff the run that lead to the accepting state contains a
	 * return transition which is succeeded by state and where stateK is the
	 * topmost stack element. 
	 */
	private boolean computeReturnSubRun(STATE state, STATE stateK) {
		Map<STATE, NestedRun<LETTER,STATE>> succK2SubRun = 
			m_returnSubRun.get(state);
		if (succK2SubRun != null) {
			Map<STATE, STATE> succK2PredStateK = 
				m_returnPredStateK.get(state);
			assert (succK2PredStateK != null);
			NestedRun<LETTER,STATE> run = succK2SubRun.get(stateK);
			STATE predK = succK2PredStateK.get(stateK);
			if (run != null) {
				m_reconstructionStack.push(stateK);
				m_ReconstructionOneStepRun = run;
				m_ReconstructionPredK = predK;
				return true;
			}
		}
		return false;
	}
	
	@Override
	public Boolean getResult() throws AutomataLibraryException {
		return m_acceptingRun == null;
	}


	public NestedRun<LETTER,STATE> getNestedRun() throws AutomataOperationCanceledException {
		return m_acceptingRun;
	}

	@Override
	public boolean checkResult(StateFactory<STATE> stateFactory)
			throws AutomataLibraryException {
		boolean correct = true;
		if (m_acceptingRun == null) {
			m_Logger.warn("Emptiness not double checked ");
		}
		else {
			m_Logger.info("Correctness of emptinessCheck not tested.");
			correct = (new Accepts<LETTER, STATE>(m_Services, m_nwa, m_acceptingRun.getWord())).getResult();
			m_Logger.info("Finished testing correctness of emptinessCheck");
		}
		return correct;
	}

	
}
