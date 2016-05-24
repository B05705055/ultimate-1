/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.DoubleDecker;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonOldApi;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.IOpWithDelayedDeadEndRemoval.UpDownEntry;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingReturnTransition;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;

/**
 * TODO Documentation
 * 
 * TODO: Optimization: For most operations the internal and call successors of
 * (<i>down</i>,<i>up</i>) are the same for all down states. So a lot of
 * successors are computed several times, but you could see the already in
 * mTraversedNwa. Suggestion: Extension that implements
 * visitAndGetInternalSuccessors(DoubleDecker) and has abstract
 * constructInternalSuccessors(IState) method.
 * 
 * @param <LETTER>
 * @param <STATE>
 */
public abstract class DoubleDeckerVisitor<LETTER, STATE>  {

	protected final AutomataLibraryServices mServices;
	protected final ILogger mLogger;
	public enum ReachFinal {
		UNKNOWN, AT_LEAST_ONCE
	}

	protected INestedWordAutomatonOldApi<LETTER, STATE> mTraversedNwa;

	/**
	 * We call a DoubleDecker marked if it has been visited or is contained in
	 * the worklist. The DoubleDecker (<i>down</i>,<i>up</i>) is marked iff
	 * <i>down</i> is contained in the range of <i>up</i>.
	 */
	private final Map<STATE, Map<STATE, ReachFinal>> mMarked_Up2Down = new HashMap<STATE, Map<STATE, ReachFinal>>();

	/**
	 * DoubleDecker that are already known but have not yet been visited.
	 */
	private final List<DoubleDecker<STATE>> mWorklist = new LinkedList<DoubleDecker<STATE>>();

	/**
	 * Pairs of states (q,q') of the automaton such that q' is reachable from q
	 * via a well-matched nested word in which the first position is a call
	 * position and the last position is a return position.
	 */
	private final Map<STATE, Map<STATE, STATE>> mCallReturnSummary = new HashMap<STATE, Map<STATE, STATE>>();

	/**
	 * We remove afterwards all dead ends iff set to true.
	 */
	protected boolean mRemoveDeadEnds = false;

	/**
	 * We remove afterwards all non-live states iff set to true.
	 */
	protected boolean mRemoveNonLiveStates = false;

	/**
	 * Compute the predecessors of all DoubleDeckers. Neccessary for removal of
	 * dead ends. TODO: Optimization make this optional for cases where we don't
	 * want to minimize.
	 */
	protected boolean mComputePredecessorDoubleDeckers = true;

	// /**
	// * Predecessor DoubleDeckers under internal transitions.
	// * Used only for removal of dead ends and non-live states.
	// */
	// protected Map<DoubleDecker<STATE>,Set<DoubleDecker<STATE>>>
	// mInternalPredecessors =
	// new HashMap<DoubleDecker<STATE>,Set<DoubleDecker<STATE>>>();
	// /**
	// * Predecessor DoubleDeckers under summary transitions.
	// * Used only for removal of dead ends and non-live states.
	// */
	// protected Map<DoubleDecker<STATE>,Set<DoubleDecker<STATE>>>
	// mSummaryPredecessors =
	// new HashMap<DoubleDecker<STATE>,Set<DoubleDecker<STATE>>>();
	// /**
	// * Predecessor DoubleDeckers under call transitions.
	// * Used only for removal of dead ends and non-live states.
	// */
	// protected Map<DoubleDecker<STATE>,Set<DoubleDecker<STATE>>>
	// mCallPredecessors =
	// new HashMap<DoubleDecker<STATE>,Set<DoubleDecker<STATE>>>();
	// /**
	// * Predecessor DoubleDeckers under call transitions.
	// * Used only for removal of dead ends and non-live states.
	// */
	// protected Map<DoubleDecker<STATE>,Set<DoubleDecker<STATE>>>
	// mReturnPredecessors =
	// new HashMap<DoubleDecker<STATE>,Set<DoubleDecker<STATE>>>();

	// /**
	// * DoubleDeckers that have been constructed but do not occur in any
	// * accepting run of the automaton.
	// */
	// private Map<STATE,Set<STATE>> mRemovedDoubleDeckers = new
	// HashMap<STATE,Set<STATE>>();

	// /**
	// * DoubleDeckers which occur on an accepting run.
	// */
	// // protected Set<DoubleDecker<STATE>> doubleDeckersThatCanReachFinal;
	// protected Map<STATE,STATE> mCallSuccOfRemovedDown;

	/**
	 * 
	 */
	// protected DoubleDecker<STATE> auxilliaryEmptyStackDoubleDecker;

	private long mDeadEndRemovalTime;

	private Set<STATE> mDeadEnds;
	
	public DoubleDeckerVisitor(AutomataLibraryServices services) {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
	}

	public Object getResult() throws AutomataOperationCanceledException {
		return mTraversedNwa;
	}

	/**
	 * True iff the DoubleDecker doubleDecker has been marked. A DoubleDecker is
	 * marked iff it has been visited or is in the mWorklist.
	 */
	private final boolean wasMarked(DoubleDecker<STATE> doubleDecker) {
		Map<STATE, ReachFinal> downState = mMarked_Up2Down.get(doubleDecker.getUp());
		if (downState == null) {
			return false;
		} else {
			return downState.containsKey(doubleDecker.getDown());
		}
	}

	private final void mark(DoubleDecker<STATE> doubleDecker) {
		Map<STATE, ReachFinal> downStates = mMarked_Up2Down.get(doubleDecker.getUp());
		if (downStates == null) {
			downStates = new HashMap<STATE, ReachFinal>();
			mMarked_Up2Down.put(doubleDecker.getUp(), downStates);
		}
		downStates.put(doubleDecker.getDown(), ReachFinal.UNKNOWN);

		// Set<STATE> upStates = mMarked_Down2Up.get(doubleDecker.getDown());
		// if (upStates == null) {
		// upStates = new HashSet<STATE>();
		// mMarked_Down2Up.put(doubleDecker.getDown(), upStates);
		// }
		// upStates.add(doubleDecker.getUp());
	}

	/**
	 * Add doubleDecker to worklist if it has not yet been marked.
	 */
	private final void enqueueAndMark(DoubleDecker<STATE> doubleDecker) {
		if (!wasMarked(doubleDecker)) {
			mark(doubleDecker);
			mWorklist.add(doubleDecker);
		}
	}

	// /**
	// * Record in predecessorMapping that preDoubleDecker is a predecessor of
	// * doubleDecker.
	// * predecessorMapping should be the mapping for either, call predecessors,
	// * internal predecessors, summary predecessors or return predecesssors.
	// */
	// private final void memorizePredecessor(
	// DoubleDecker<STATE> doubleDecker,
	// DoubleDecker<STATE> preDoubleDecker,
	// Map<DoubleDecker<STATE>,Set<DoubleDecker<STATE>>> predecessorMapping) {
	// if (!mComputePredecessorDoubleDeckers) {
	// return;
	// }
	// assert ( predecessorMapping == mCallPredecessors
	// || predecessorMapping == mReturnPredecessors
	// || predecessorMapping == mInternalPredecessors
	// || predecessorMapping == mSummaryPredecessors);
	// if (preDoubleDecker != null) {
	// Set<DoubleDecker<STATE>> predSet = predecessorMapping.get(doubleDecker);
	// if (predSet == null) {
	// predSet = new HashSet<DoubleDecker<STATE>>();
	// predecessorMapping.put(doubleDecker, predSet);
	// }
	// predSet.add(preDoubleDecker);
	// }
	// }

	/**
	 * Record that summarySucc is reachable from summaryPred via a run over a
	 * well-matched NestedWord.
	 */
	private final void addSummary(STATE summaryPred, STATE summarySucc, STATE returnPred) {
		Map<STATE, STATE> summarySuccessors = mCallReturnSummary.get(summaryPred);
		if (summarySuccessors == null) {
			summarySuccessors = new HashMap<STATE, STATE>();
			mCallReturnSummary.put(summaryPred, summarySuccessors);
		}
		summarySuccessors.put(summarySucc, returnPred);
		enqueueSummarySuccs(summaryPred, summarySucc, returnPred);
	}

	/**
	 * For all DoubleDeckers (<i>down</i>,summaryPred) that have been marked
	 * enqueue and mark the DoubleDecker (<i>down</i>,summarySucc) and record
	 * that the DoubleDecker (summaryPred,returnPred) is a predecessor of
	 * (<i>down</i>,summarySucc).
	 */
	private final void enqueueSummarySuccs(STATE summaryPred, STATE summarySucc, STATE returnPred) {
		for (STATE summaryPreDown : mMarked_Up2Down.get(summaryPred).keySet()) {
			DoubleDecker<STATE> doubleDecker = new DoubleDecker<STATE>(summaryPreDown, summaryPred);
			DoubleDecker<STATE> summarySuccDoubleDecker = new DoubleDecker<STATE>(summaryPreDown, summarySucc);
			DoubleDecker<STATE> summaryReturnPred = new DoubleDecker<STATE>(summaryPred, returnPred);
			// memorizePredecessor(summarySuccDoubleDecker, summaryReturnPred,
			// mReturnPredecessors);
			// memorizePredecessor(summarySuccDoubleDecker, doubleDecker,
			// mSummaryPredecessors);
			enqueueAndMark(summarySuccDoubleDecker);
		}
	}

	/**
	 * Get all states <i>down</i> such that the DoubleDecker
	 * (<i>down</i>,<i>up</i>) has been visited so far.
	 */
	private final Set<STATE> getKnownDownStates(STATE up) {
		Set<STATE> downStates = mMarked_Up2Down.get(up).keySet();
		if (downStates == null) {
			return new HashSet<STATE>(0);
		} else {
			return downStates;
		}
	}

	public Map<STATE, Map<STATE, ReachFinal>> getUp2DownMapping() {
		return Collections.unmodifiableMap(mMarked_Up2Down);
	}

	protected final void traverseDoubleDeckerGraph() throws AutomataOperationCanceledException {
		Collection<STATE> initialStates = getInitialStates();
		for (STATE state : initialStates) {
			DoubleDecker<STATE> initialDoubleDecker = new DoubleDecker<STATE>(mTraversedNwa.getEmptyStackState(),
					state);
			enqueueAndMark(initialDoubleDecker);
		}

		while (!mWorklist.isEmpty()) {
			DoubleDecker<STATE> doubleDecker = mWorklist.remove(0);

			Iterable<STATE> internalSuccs = visitAndGetInternalSuccessors(doubleDecker);
			for (STATE succ : internalSuccs) {
				DoubleDecker<STATE> succDoubleDecker = new DoubleDecker<STATE>(doubleDecker.getDown(), succ);
				// memorizePredecessor(succDoubleDecker, doubleDecker,
				// mInternalPredecessors);
				enqueueAndMark(succDoubleDecker);
			}
			Iterable<STATE> callSuccs = visitAndGetCallSuccessors(doubleDecker);
			for (STATE succ : callSuccs) {
				DoubleDecker<STATE> succDoubleDecker = new DoubleDecker<STATE>(doubleDecker.getUp(), succ);
				// memorizePredecessor(succDoubleDecker, doubleDecker,
				// mCallPredecessors);
				enqueueAndMark(succDoubleDecker);
			}
			Iterable<STATE> returnSuccs = visitAndGetReturnSuccessors(doubleDecker);
			for (STATE succ : returnSuccs) {
				addSummary(doubleDecker.getDown(), succ, doubleDecker.getUp());
				for (STATE resLinPredCallerState : getKnownDownStates(doubleDecker.getDown())) {
					DoubleDecker<STATE> succDoubleDecker = new DoubleDecker<STATE>(resLinPredCallerState, succ);
					// memorizePredecessor(succDoubleDecker, doubleDecker,
					// mReturnPredecessors);
					enqueueAndMark(succDoubleDecker);
				}
			}

			if (mCallReturnSummary.containsKey(doubleDecker.getUp())) {
				Map<STATE, STATE> summarySucc2returnPred = mCallReturnSummary.get(doubleDecker.getUp());
				for (STATE summarySucc : summarySucc2returnPred.keySet()) {
					STATE returnPred = summarySucc2returnPred.get(summarySucc);
					DoubleDecker<STATE> summarySuccDoubleDecker = new DoubleDecker<STATE>(doubleDecker.getDown(),
							summarySucc);
					DoubleDecker<STATE> shortcutReturnPred = new DoubleDecker<STATE>(doubleDecker.getUp(), returnPred);
					// memorizePredecessor(summarySuccDoubleDecker,
					// shortcutReturnPred, mReturnPredecessors);
					// memorizePredecessor(summarySuccDoubleDecker,
					// doubleDecker, mSummaryPredecessors);
					enqueueAndMark(summarySuccDoubleDecker);
				}
			}
			if (mServices.getProgressMonitorService() != null
					&& !mServices.getProgressMonitorService().continueProcessing()) {
				throw new AutomataOperationCanceledException(this.getClass());
			}

		}
		mLogger.info("Before removal of dead ends " + mTraversedNwa.sizeInformation());
		if (mRemoveDeadEnds && mRemoveNonLiveStates) {
			throw new IllegalArgumentException("RemoveDeadEnds and RemoveNonLiveStates is set");
		}

		mDeadEnds = computeDeadEnds();

		// Set<DoubleDecker<STATE>> oldMethod = removedDoubleDeckersOldMethod();
		// Set<DoubleDecker<STATE>> newMethod =
		// removedDoubleDeckersViaIterator();
		// assert oldMethod.containsAll(newMethod);
		// assert newMethod.containsAll(oldMethod);

		if (mRemoveDeadEnds) {
			// new TestFileWriter(mTraversedNwa, "TheAutomaotn",
			// TestFileWriter.Labeling.TOSTRING);
			removeDeadEnds();
			if (mTraversedNwa.getInitialStates().isEmpty()) {
				assert mTraversedNwa.getStates().isEmpty();
			}
			mLogger.info("After removal of dead ends " + mTraversedNwa.sizeInformation());

		}
		if (mRemoveNonLiveStates) {
			// mLogger.warn("Minimize before non-live removal: " +
			// ((NestedWordAutomaton<LETTER,STATE>) (new MinimizeDfa<LETTER,
			// STATE>(mTraversedNwa)).getResult()).sizeInformation());
			removeNonLiveStates();
			// mLogger.warn("Minimize after non-live removal: " +
			// ((NestedWordAutomaton<LETTER,STATE>) (new MinimizeDfa<LETTER,
			// STATE>(mTraversedNwa)).getResult()).sizeInformation());
			if (mTraversedNwa.getInitialStates().isEmpty()) {
				assert mTraversedNwa.getStates().isEmpty();
				// mTraversedNwa = getTotalizedEmptyAutomaton();
			}
			mLogger.info("After removal of nonLiveStates " + mTraversedNwa.sizeInformation());
		}
	}

	// protected NestedWordAutomaton<LETTER,STATE> getTotalizedEmptyAutomaton()
	// {
	// NestedWordAutomaton<LETTER,STATE> emptyAutomaton = new
	// NestedWordAutomaton<LETTER,STATE>(
	// mTraversedNwa.getInternalAlphabet(),
	// mTraversedNwa.getCallAlphabet(),
	// mTraversedNwa.getReturnAlphabet(),
	// mTraversedNwa.getStateFactory());
	// STATE sinkState =
	// emptyAutomaton.getStateFactory().createSinkStateContent();
	// emptyAutomaton.addState(true, false, sinkState);
	//
	// for (STATE state : emptyAutomaton.getStates()) {
	// for (LETTER letter : emptyAutomaton.getInternalAlphabet()) {
	// if (emptyAutomaton.succInternal(state,letter).isEmpty()) {
	// emptyAutomaton.addInternalTransition(state, letter, sinkState);
	// }
	// }
	// for (LETTER letter : emptyAutomaton.getCallAlphabet()) {
	// if (emptyAutomaton.succCall(state,letter).isEmpty()) {
	// emptyAutomaton.addCallTransition(state, letter, sinkState);
	// }
	// }
	// for (LETTER symbol : emptyAutomaton.getReturnAlphabet()) {
	// for (STATE hier : emptyAutomaton.getStates()) {
	// if (emptyAutomaton.succReturn(state,hier,symbol).isEmpty()) {
	// emptyAutomaton.addReturnTransition(state, hier, symbol, sinkState);
	// }
	// }
	// }
	// }
	//
	// return emptyAutomaton;
	// }

	/**
	 * @return initial states of automaton
	 */
	protected abstract Collection<STATE> getInitialStates();

	/**
	 * @return internal successors of doubleDeckers up state
	 */
	protected abstract Collection<STATE> visitAndGetInternalSuccessors(DoubleDecker<STATE> doubleDecker);

	/**
	 * @return call successors of doubleDeckers up state
	 */
	protected abstract Collection<STATE> visitAndGetCallSuccessors(DoubleDecker<STATE> doubleDecker);

	/**
	 * @return return successors of doubleDeckers up state
	 */
	protected abstract Collection<STATE> visitAndGetReturnSuccessors(DoubleDecker<STATE> doubleDecker);

	private void enqueueInternalPred(STATE up, Collection<STATE> downStates, DoubleDeckerWorkList worklist) {
		for (IncomingInternalTransition<LETTER, STATE> inTrans : mTraversedNwa.internalPredecessors(up)) {
			STATE predUp = inTrans.getPred();
			for (STATE down : downStates) {
				ReachFinal doubleDeckerReach = mMarked_Up2Down.get(predUp).get(down);
				if (doubleDeckerReach == ReachFinal.UNKNOWN) {
					// assert (doubleDeckersThatCanReachFinal.contains(new
					// DoubleDecker<STATE>(down, predUp))) :
					// "deadEndRemovalFailed";
					worklist.add(predUp, down);
				} else {
					assert doubleDeckerReach == null || doubleDeckerReach == ReachFinal.AT_LEAST_ONCE;
				}
			}
		}
	}

	private void enqueueCallPred(STATE up, Collection<STATE> downStates, DoubleDeckerWorkList worklist) {
		// we for call transitions we may use all of predecessors
		// down states (use only when considering only non ret ancestors!)
		for (IncomingCallTransition<LETTER, STATE> inTrans : mTraversedNwa.callPredecessors(up)) {
			STATE predUp = inTrans.getPred();
			for (STATE predDown : mMarked_Up2Down.get(predUp).keySet()) {
				ReachFinal doubleDeckerReach = mMarked_Up2Down.get(predUp).get(predDown);
				if (doubleDeckerReach == ReachFinal.UNKNOWN) {
					// assert (doubleDeckersThatCanReachFinal.contains(new
					// DoubleDecker<STATE>(predDown, predUp))) :
					// "deadEndRemovalFailed";
					worklist.add(predUp, predDown);
				} else {
					assert doubleDeckerReach == null || doubleDeckerReach == ReachFinal.AT_LEAST_ONCE;
				}
			}
		}
	}

	private void enqueueReturnPred(STATE up, Collection<STATE> downStates, DoubleDeckerWorkList summaryWorklist,
			DoubleDeckerWorkList linPredworklist) {
		for (IncomingReturnTransition<LETTER, STATE> inTrans : mTraversedNwa.returnPredecessors(up)) {
			STATE hier = inTrans.getHierPred();
			// We have to check if there is some double decker (hier,down) with
			// down∈downStates. Only in that case we may add (lin,hier) to the
			// worklist (done after this while loop)
			boolean hierIsUpOfSomePredDoubleDecker = false;
			for (STATE down : downStates) {
				ReachFinal doubleDeckerReach = mMarked_Up2Down.get(hier).get(down);
				if (doubleDeckerReach != null) {
					hierIsUpOfSomePredDoubleDecker = true;
					if (doubleDeckerReach == ReachFinal.UNKNOWN) {
						// assert (doubleDeckersThatCanReachFinal.contains(new
						// DoubleDecker<STATE>(down, hier))) :
						// "deadEndRemovalFailed";
						summaryWorklist.add(hier, down);
					} else {
						assert doubleDeckerReach == ReachFinal.AT_LEAST_ONCE;
					}
				}
			}
			STATE linPred = inTrans.getLinPred();
			if (hierIsUpOfSomePredDoubleDecker) {
				ReachFinal doubleDeckerReach = mMarked_Up2Down.get(linPred).get(hier);
				if (doubleDeckerReach == ReachFinal.UNKNOWN) {
					// assert (doubleDeckersThatCanReachFinal.contains(new
					// DoubleDecker<STATE>(hier, linPred))) :
					// "deadEndRemovalFailed";
					linPredworklist.add(linPred, hier);
				} else {
					assert doubleDeckerReach == ReachFinal.AT_LEAST_ONCE;
				}
			}
		}
	}

	private final Set<STATE> computeDeadEnds() {
		// Set used to compute the states that can never reach the final state
		// initialized with all states and narrowed by the algorithm
		Set<STATE> statesNeverReachFinal = new HashSet<STATE>(mTraversedNwa.getStates());

		{
			DoubleDeckerWorkList nonRetAncest = new DoubleDeckerWorkList();

			DoubleDeckerWorkList allAncest = new DoubleDeckerWorkList();

			// compute return ancestors in two steps

			// step one: consider only hierarchical predecessors of return
			// transitions. Reason: We want to compute all reachable double
			// deckers. Neglecting linPreds of return transitions all double
			// deckers (up,down) of an up state are reachable.
			for (STATE state : mTraversedNwa.getFinalStates()) {
				Map<STATE, ReachFinal> down2reachFinal = mMarked_Up2Down.get(state);
				if (down2reachFinal == null) {
					mLogger.debug("Unreachable final state: " + state);
				} else {
					for (STATE down : mMarked_Up2Down.get(state).keySet()) {
						nonRetAncest.add(state, down);
					}
				}
			}
			while (!nonRetAncest.isEmpty()) {
				Map<STATE, Set<STATE>> up2downs = nonRetAncest.removeUpAndItsDowns();
				assert up2downs.size() == 1;
				STATE up = up2downs.keySet().iterator().next();
				statesNeverReachFinal.remove(up);
				Set<STATE> downs = up2downs.get(up);
				// down states such that final reachability of (up,down) was new
				// information (and has to be propagated to predecessors.
				List<STATE> updatedDowns = new ArrayList<STATE>();
				for (STATE down : downs) {
					if (mMarked_Up2Down.get(up).get(down) == ReachFinal.UNKNOWN) {
						updatedDowns.add(down);
					} else {
						assert mMarked_Up2Down.get(up).get(down) == ReachFinal.AT_LEAST_ONCE;
					}
					// assert (doubleDeckersThatCanReachFinal.contains(new
					// DoubleDecker<STATE>(down, up))) : "deadEndRemovalFailed";
					mMarked_Up2Down.get(up).put(down, ReachFinal.AT_LEAST_ONCE);
				}

				if (!updatedDowns.isEmpty()) {
					enqueueInternalPred(up, updatedDowns, nonRetAncest);

					enqueueCallPred(up, updatedDowns, nonRetAncest);
					enqueueReturnPred(up, updatedDowns, nonRetAncest, allAncest);
				}
			}

			// step two
			while (!allAncest.isEmpty()) {
				Map<STATE, Set<STATE>> up2downs = allAncest.removeUpAndItsDowns();
				assert up2downs.size() == 1;
				STATE up = up2downs.keySet().iterator().next();
				statesNeverReachFinal.remove(up);
				Set<STATE> downs = up2downs.get(up);
				// down states such that final reachability of (up,down) was new
				// information (and has to be propagated to predecessors.
				List<STATE> updatedDowns = new ArrayList<STATE>();
				for (STATE down : downs) {
					if (mMarked_Up2Down.get(up).get(down) == ReachFinal.UNKNOWN) {
						updatedDowns.add(down);
					}
					// assert (doubleDeckersThatCanReachFinal.contains(new
					// DoubleDecker<STATE>(down, up))) : "deadEndRemovalFailed";
					mMarked_Up2Down.get(up).put(down, ReachFinal.AT_LEAST_ONCE);
				}
				if (!updatedDowns.isEmpty()) {
					enqueueInternalPred(up, updatedDowns, allAncest);
					// we may omit call transitions - state are already
					// considered as hier preds of returns.
					enqueueReturnPred(up, updatedDowns, allAncest, allAncest);
				}
			}
		}
		return statesNeverReachFinal;
	}

	// private final Set<STATE> computeStatesThatCanNotReachFinal() {
	//
	// // // Set used to compute the states that can never reach the final state
	// // // initialized with all states and narrowed by the algorithm
	// // Set<STATE> statesNeverReachFinal = new
	// HashSet<STATE>(mTraversedNwa.getStates());
	// //
	// // {
	// // Set<DoubleDecker<STATE>> nonReturnAncestors = new
	// HashSet<DoubleDecker<STATE>>();
	// // Set<DoubleDecker<STATE>> acceptingDoubleDeckers = new
	// HashSet<DoubleDecker<STATE>>();
	// // for (STATE finalState : mTraversedNwa.getFinalStates()) {
	// // Set<STATE> finalsDownStates =
	// mMarked_Up2Down.get(finalState).keySet();
	// // for (STATE downStatesOfFinal : finalsDownStates) {
	// // DoubleDecker<STATE> summary = new
	// DoubleDecker<STATE>(downStatesOfFinal, finalState);
	// // acceptingDoubleDeckers.add(summary);
	// // }
	// // }
	// //
	// // LinkedList<DoubleDecker<STATE>> ancestorSearchWorklist;
	// //
	// // //Computation of nonReturnAncestors
	// // ancestorSearchWorklist = new LinkedList<DoubleDecker<STATE>>();
	// // ancestorSearchWorklist.addAll(acceptingDoubleDeckers);
	// // nonReturnAncestors.addAll(acceptingDoubleDeckers);
	// // while (!ancestorSearchWorklist.isEmpty()) {
	// // DoubleDecker<STATE> doubleDecker=
	// ancestorSearchWorklist.removeFirst();
	// // statesNeverReachFinal.remove(doubleDecker.getUp());
	// // ArrayList<Set<DoubleDecker<STATE>>> predSets = new
	// ArrayList<Set<DoubleDecker<STATE>>>(3);
	// // predSets.add(mInternalPredecessors.get(doubleDecker));
	// // predSets.add(mSummaryPredecessors.get(doubleDecker));
	// // predSets.add(mCallPredecessors.get(doubleDecker));
	// // for (Set<DoubleDecker<STATE>> preds : predSets) {
	// // if (preds == null) {
	// // //assert mTraversedNwa.getInitial().contains(doubleDecker.getUp());
	// // }
	// // else {
	// // for (DoubleDecker<STATE> pred : preds) {
	// // if (!nonReturnAncestors.contains(pred)) {
	// // nonReturnAncestors.add(pred);
	// // ancestorSearchWorklist.add(pred);
	// // }
	// // }
	// // }
	// // }
	// // }
	// //
	// // //add Return Ancestors
	// // ancestorSearchWorklist = new LinkedList<DoubleDecker<STATE>>();
	// // ancestorSearchWorklist.addAll(nonReturnAncestors);
	// // while (!ancestorSearchWorklist.isEmpty()) {
	// // DoubleDecker<STATE> doubleDecker=
	// ancestorSearchWorklist.removeFirst();
	// // statesNeverReachFinal.remove(doubleDecker.getUp());
	// // ArrayList<Set<DoubleDecker<STATE>>> predSets = new
	// ArrayList<Set<DoubleDecker<STATE>>>(3);
	// // predSets.add(mInternalPredecessors.get(doubleDecker));
	// // predSets.add(mSummaryPredecessors.get(doubleDecker));
	// // predSets.add(mReturnPredecessors.get(doubleDecker));
	// // for (Set<DoubleDecker<STATE>> preds : predSets) {
	// // if (preds == null) {
	// // //assert mTraversedNwa.getInitial().contains(doubleDecker.getUp());
	// // }
	// // else {
	// // for (DoubleDecker<STATE> pred : preds) {
	// // if (!nonReturnAncestors.contains(pred)) {
	// // nonReturnAncestors.add(pred);
	// // ancestorSearchWorklist.add(pred);
	// // }
	// // }
	// // }
	// // }
	// // }
	// //
	// // // DoubleDeckers that have been visited in this search which starts
	// from
	// // // final states
	// // doubleDeckersThatCanReachFinal = nonReturnAncestors;
	// // doubleDeckersThatCanReachFinal.addAll(acceptingDoubleDeckers);
	// // }
	//
	//
	//
	//
	// Set<STATE> statesNeverReachFin =
	// computeStatesThatCanNotReachFinalNewVersion();
	// // mLogger.error("STATEs " + mTraversedNwa.getStates().size());
	// //// new TestFileWriter(mTraversedNwa, "TheAutomaotn",
	// TestFileWriter.Labeling.TOSTRING, "test");
	// // assert statesNeverReachFinal.containsAll(statesNeverReachFin) :
	// "deadEndRemovalFailed";
	// // assert statesNeverReachFin.containsAll(statesNeverReachFinal) :
	// "deadEndRemovalFailed";
	// //
	// // for (DoubleDecker<STATE> dd : doubleDeckersThatCanReachFinal) {
	// // STATE up = dd.getUp();
	// // STATE down = dd.getDown();
	// // assert mMarked_Up2Down.get(up).get(down) == ReachFinal.AT_LEAST_ONCE
	// : "deadEndRemovalFailed";
	// // }
	// // for (STATE up : mMarked_Up2Down.keySet()) {
	// // for (STATE down : mMarked_Up2Down.get(up).keySet()) {
	// // if (mMarked_Up2Down.get(up).get(down) == ReachFinal.AT_LEAST_ONCE &&
	// down != mTraversedNwa.getEmptyStackState()) {
	// // assert (doubleDeckersThatCanReachFinal.contains(new
	// DoubleDecker<STATE>(down, up))) : "deadEndRemovalFailed";
	// // }
	// // }
	// // }
	//
	// return statesNeverReachFin;
	// }

	/**
	 * Remove in the resulting automaton all states that can not reach a final
	 * state.
	 * 
	 * @param computeRemovedDoubleDeckersAndCallSuccessors
	 *            compute the set of all DoubleDeckers which occurred in the
	 *            build automaton but are not reachable after the removal
	 * @return true iff at least one state was removed.
	 */
	public final boolean removeDeadEnds() {
		long startTime = System.currentTimeMillis();

		// some states are not removed but loose inital property
		Set<STATE> statesThatShouldNotBeInitialAnyMore = new HashSet<STATE>();
		for (STATE state : mTraversedNwa.getInitialStates()) {
			if (mDeadEnds.contains(state)) {
				continue;
			}
			if (mMarked_Up2Down.get(state).get(mTraversedNwa.getEmptyStackState()) == ReachFinal.AT_LEAST_ONCE) {
				continue;
			} else {
				assert mMarked_Up2Down.get(state).get(mTraversedNwa.getEmptyStackState()) == ReachFinal.UNKNOWN;
			}
			statesThatShouldNotBeInitialAnyMore.add(state);
		}
		for (STATE state : statesThatShouldNotBeInitialAnyMore) {
			((NestedWordAutomaton<LETTER, STATE>) mTraversedNwa).makeStateNonIntial(state);
			mLogger.warn("The following state is not final any more: " + state);
		}

		for (STATE state : mDeadEnds) {
			((NestedWordAutomaton<LETTER, STATE>) mTraversedNwa).removeState(state);
		}

		boolean atLeastOneStateRemoved = !mDeadEnds.isEmpty();
		mDeadEnds = null;
		mDeadEndRemovalTime += (System.currentTimeMillis() - startTime);
		mLogger.info("After removal of dead ends " + mTraversedNwa.sizeInformation());
		return atLeastOneStateRemoved;
	}

	// /**
	// * Announce removal of all DoubleDeckers (<i>down</i>,<i>up</i>) such that
	// * <i>down</i> or <i>up</i> is contained in statesGoingToBeRemoved.
	// */
	// // _before_ because on removal we want to be able to access all states
	// // of the automaton
	// private void announceRemovalOfDoubleDeckers(Set<STATE>
	// statesGoingToBeRemoved) {
	// mCallSuccOfRemovedDown = new HashMap<STATE,STATE>();
	//
	// /**
	// * DoubleDeckers that have been constructed but do not occur in any
	// * accepting run of the automaton.
	// */
	// for (STATE up : mMarked_Up2Down.keySet()) {
	// for (STATE down : mMarked_Up2Down.get(up).keySet()) {
	// if (mMarked_Up2Down.get(up).get(down) == ReachFinal.UNKNOWN) {
	//
	// Set<STATE> downStates = mRemovedDoubleDeckers.get(up);
	// if (downStates == null) {
	// downStates = new HashSet<STATE>();
	// mRemovedDoubleDeckers.put(up, downStates);
	// }
	// downStates.add(down);
	//
	// Set<STATE> downCallSuccs = computeState2CallSuccs(down);
	// if (downCallSuccs.size() > 1) {
	// throw new UnsupportedOperationException("If state has" +
	// " several outgoing call transitions Hoare annotation might be incorrect.");
	// } else if (downCallSuccs.size() == 1){
	// STATE callSucc = downCallSuccs.iterator().next();
	// mCallSuccOfRemovedDown.put(down, callSucc);
	// } else {
	// assert downCallSuccs.isEmpty();
	// }
	// }
	// }
	// }
	// }

	/**
	 * Compute call successors for a given set of states.
	 */
	private Set<STATE> computeState2CallSuccs(STATE state) {
		Set<STATE> callSuccs = new HashSet<STATE>();
		if (state != mTraversedNwa.getEmptyStackState()) {
			for (LETTER letter : mTraversedNwa.lettersCall(state)) {
				for (STATE succ : mTraversedNwa.succCall(state, letter)) {
					callSuccs.add(succ);
				}
			}
		}
		return callSuccs;
	}

	/**
	 * Return true iff state has successors
	 */
	private boolean hasSuccessors(STATE state) {
		for (LETTER symbol : mTraversedNwa.lettersInternal(state)) {
			if (mTraversedNwa.succInternal(state, symbol).iterator().hasNext()) {
				return true;
			}
		}
		for (LETTER symbol : mTraversedNwa.lettersCall(state)) {
			if (mTraversedNwa.succCall(state, symbol).iterator().hasNext()) {
				return true;
			}
		}
		for (LETTER symbol : mTraversedNwa.lettersReturn(state)) {
			for (STATE hier : mTraversedNwa.hierPred(state, symbol)) {
				if (mTraversedNwa.succReturn(state, hier, symbol).iterator().hasNext()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Remove all state that are accepting and do not have any successor.
	 * 
	 * @return true iff some state was removed
	 */
	private final boolean removeAcceptingStatesWithoutSuccessors() {

		ArrayList<STATE> finalStatesWithoutSuccessor = new ArrayList<STATE>();
		for (STATE accepting : mTraversedNwa.getFinalStates()) {
			if (!hasSuccessors(accepting)) {
				finalStatesWithoutSuccessor.add(accepting);
			}
		}
		boolean atLeastOneStateRemoved = !finalStatesWithoutSuccessor.isEmpty();
		for (STATE finalStateWithoutSuccessor : finalStatesWithoutSuccessor) {
			((NestedWordAutomaton<LETTER, STATE>) mTraversedNwa).removeState(finalStateWithoutSuccessor);
		}
		return atLeastOneStateRemoved;
	}

	/**
	 * Remove all states from which only finitely many accepting states are
	 * reachable.
	 */
	public final void removeNonLiveStates() {
		boolean stateRemovedInInteration;
		do {
			if (mDeadEnds == null) {
				mDeadEnds = computeDeadEnds();
			}
			stateRemovedInInteration = removeDeadEnds();
			resetReachabilityInformation();
			stateRemovedInInteration |= removeAcceptingStatesWithoutSuccessors();
		} while (stateRemovedInInteration);
	}

	private void resetReachabilityInformation() {
		for (STATE state : mTraversedNwa.getStates()) {
			Map<STATE, ReachFinal> down2reachProp = mMarked_Up2Down.get(state);
			for (STATE down : down2reachProp.keySet()) {
				down2reachProp.put(down, ReachFinal.UNKNOWN);
			}
		}
	}

	public long getDeadEndRemovalTime() {
		return mDeadEndRemovalTime;
	}

	private class DoubleDeckerSet {
		private Map<STATE, Set<STATE>> mup2down = new HashMap<STATE, Set<STATE>>();

		public void add(STATE up, STATE down) {
			Set<STATE> downStates = mup2down.get(up);
			if (downStates == null) {
				downStates = new HashSet<STATE>();
				mup2down.put(up, downStates);
			}
			downStates.add(down);
		}

		public void addAll(Collection<DoubleDecker<STATE>> collection) {
			for (DoubleDecker<STATE> dd : collection) {
				this.add(dd.getUp(), dd.getDown());
			}
		}

		public Map<STATE, Set<STATE>> getUp2DownMap() {
			return mup2down;
		}
	}

	private class DoubleDeckerWorkList {
		private Map<STATE, Set<STATE>> mup2down = new HashMap<STATE, Set<STATE>>();

		public void add(STATE up, STATE down) {
			Set<STATE> downStates = mup2down.get(up);
			if (downStates == null) {
				downStates = new HashSet<STATE>();
				mup2down.put(up, downStates);
			}
			downStates.add(down);
		}

		public void add(STATE up, Collection<STATE> downs) {
			Set<STATE> downStates = mup2down.get(up);
			if (downStates == null) {
				downStates = new HashSet<STATE>();
				mup2down.put(up, downStates);
			}
			downStates.addAll(downs);
		}

		public boolean isEmpty() {
			return mup2down.isEmpty();
		}

		public Map<STATE, Set<STATE>> removeUpAndItsDowns() {
			STATE up = mup2down.keySet().iterator().next();
			Map<STATE, Set<STATE>> result = Collections.singletonMap(up, mup2down.get(up));
			mup2down.remove(up);
			return result;
		}
	}

	// @Override
	// public boolean removeStatesThatCanNotReachFinal(
	// boolean computeRemovedDoubleDeckersAndCallSuccessors) {
	// // TODO Auto-generated method stub
	// return false;
	// }

	public Iterable<UpDownEntry<STATE>> getRemovedUpDownEntry() {

		return new Iterable<UpDownEntry<STATE>>() {

			@Override
			public Iterator<UpDownEntry<STATE>> iterator() {
				return new Iterator<UpDownEntry<STATE>>() {
					private Iterator<STATE> mUpIterator;
					private STATE mUp;
					private Iterator<STATE> mDownIterator;
					private STATE mDown;
					boolean mhasNext = true;

					{
						mUpIterator = getUp2DownMapping().keySet().iterator();
						if (mUpIterator.hasNext()) {
							mUp = mUpIterator.next();
							mDownIterator = getUp2DownMapping().get(mUp).keySet().iterator();
						} else {
							mhasNext = false;
						}
						computeNextElement();

					}

					private void computeNextElement() {
						mDown = null;
						while (mDown == null && mhasNext) {
							if (mDownIterator.hasNext()) {
								STATE downCandidate = mDownIterator.next();
								ReachFinal reach = getUp2DownMapping().get(mUp).get(downCandidate);
								if (reach == ReachFinal.UNKNOWN) {
									mDown = downCandidate;
								} else {
									assert reach == ReachFinal.AT_LEAST_ONCE;
								}
							} else {
								if (mUpIterator.hasNext()) {
									mUp = mUpIterator.next();
									mDownIterator = getUp2DownMapping().get(mUp).keySet().iterator();
								} else {
									mhasNext = false;
								}
							}

						}
					}

					@Override
					public boolean hasNext() {
						return mhasNext;
					}

					@Override
					public UpDownEntry<STATE> next() {
						if (!hasNext()) {
							throw new NoSuchElementException();
						}
						STATE entry;
						Set<STATE> callSuccs = computeState2CallSuccs(mDown);
						if (callSuccs.size() > 1) {
							throw new UnsupportedOperationException("State has more than one call successor");
						} else if (callSuccs.size() == 1) {
							entry = callSuccs.iterator().next();
						} else {
							entry = null;
							assert mDown == mTraversedNwa.getEmptyStackState();
						}
						UpDownEntry<STATE> result = new UpDownEntry<STATE>(mUp, mDown, entry);
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
				if (state != mTraversedNwa.getEmptyStackState()) {
					for (LETTER letter : mTraversedNwa.lettersCall(state)) {
						for (STATE succ : mTraversedNwa.succCall(state, letter)) {
							callSuccs.add(succ);
						}
					}
				}
				return callSuccs;
			}

		};

	}

	private Set<DoubleDecker<STATE>> removedDoubleDeckersOldMethod() {
		Set<DoubleDecker<STATE>> result = new HashSet<DoubleDecker<STATE>>();
		for (STATE up : mMarked_Up2Down.keySet()) {
			for (STATE down : mMarked_Up2Down.get(up).keySet()) {
				if (mMarked_Up2Down.get(up).get(down) == ReachFinal.UNKNOWN) {
					result.add(new DoubleDecker<STATE>(down, up));
				}
			}
		}
		return result;
	}

	private Set<DoubleDecker<STATE>> removedDoubleDeckersViaIterator() {
		Set<DoubleDecker<STATE>> result = new HashSet<DoubleDecker<STATE>>();
		for (UpDownEntry<STATE> upDownEntry : getRemovedUpDownEntry()) {
			result.add(new DoubleDecker<STATE>(upDownEntry.getDown(), upDownEntry.getUp()));
		}
		return result;
	}
}
