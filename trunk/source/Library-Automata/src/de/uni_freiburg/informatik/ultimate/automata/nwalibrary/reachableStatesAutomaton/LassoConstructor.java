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
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.reachableStatesAutomaton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedRun;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.buchiNwa.NestedLassoRun;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.ITransitionlet;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingReturnTransition;
import de.uni_freiburg.informatik.ultimate.util.scc.StronglyConnectedComponent;

class LassoConstructor<LETTER, STATE> {
	private final AutomataLibraryServices mServices;
	private final NestedWordAutomatonReachableStates<LETTER, STATE> mNwars;
    private final StateContainer<LETTER,STATE> mGoal;
    private final Set<StateContainer<LETTER,STATE>> mVisited =
                    new HashSet<StateContainer<LETTER,STATE>>();
    private final ArrayList<Map<StateContainer<LETTER,STATE>,SuccessorInfo>> mSuccInfos = 
    		new ArrayList<Map<StateContainer<LETTER,STATE>,SuccessorInfo>>();
    private final StronglyConnectedComponent<StateContainer<LETTER, STATE>> mScc;
    private final boolean mFindAcceptingSummary;
    private int mIteration;
    private boolean mGoalFound = false;
    private NestedRun<LETTER, STATE> mLoop;
    private final NestedRun<LETTER, STATE> mStem;
    private final NestedLassoRun<LETTER, STATE> mLasso;
    
	public LassoConstructor(final AutomataLibraryServices services, 
			final NestedWordAutomatonReachableStates<LETTER, STATE> nwars, 
			final StateContainer<LETTER, STATE> goal,
			final StronglyConnectedComponent<StateContainer<LETTER, STATE>> scc)
					throws AutomataOperationCanceledException {
		mServices = services;
		mNwars = nwars;
		mGoal = goal;
		mScc = scc;
		mFindAcceptingSummary = false;
		//first, find a run, while doing a backward breadth first search
		{
			mIteration = 0;
			final Map<StateContainer<LETTER, STATE>, SuccessorInfo> map = 
					new HashMap<StateContainer<LETTER,STATE>,SuccessorInfo>();
			mSuccInfos.add(map);
			addPredecessors(mGoal, map);
		}
		findRunBackwards();
		constructRunOfLoop();
		mStem = (new RunConstructor<LETTER, STATE>(mServices, mNwars, mGoal)).constructRun();
		mLasso = new NestedLassoRun<LETTER, STATE>(mStem, mLoop);
	}
	
	public LassoConstructor(final AutomataLibraryServices services,
			final NestedWordAutomatonReachableStates<LETTER, STATE> nwars, 
			final Summary<LETTER, STATE> summary,
			final StronglyConnectedComponent<StateContainer<LETTER, STATE>> scc)
					throws AutomataOperationCanceledException {
		mServices = services;
		mNwars = nwars;
		mGoal = summary.getSucc();
		mScc = scc;
		mFindAcceptingSummary = true;
		//first, find a run, while doing a backward breadth first search
		{
			mIteration = 0;
			final Map<StateContainer<LETTER, STATE>, SuccessorInfo> map = 
					new HashMap<StateContainer<LETTER,STATE>,SuccessorInfo>();
			mSuccInfos.add(map);
			final SuccessorInfo succInfo = new SuccessorInfo(
					summary.obtainIncomingReturnTransition(mNwars), mGoal);
			map.put(summary.getHierPred(), succInfo);
//			addPredecessors(mGoal, map);
		}
		findRunBackwards();
		constructRunOfLoop();
		mStem = (new RunConstructor<LETTER, STATE>(mServices, mNwars, mGoal)).constructRun();
		mLasso = new NestedLassoRun<LETTER, STATE>(mStem, mLoop);
	}

	
	/**
	 * Check iteratively precedessors and add SuccInfos to mSuccInfos
	 */
	private void findRunBackwards() {
		while (!mGoalFound) {
			if (mIteration > mScc.getNumberOfStates()) {
				throw new AssertionError("unable to find state in SCC");
			}
			assert mSuccInfos.size() == mIteration + 1;
			mIteration++;
			final Map<StateContainer<LETTER, STATE>, SuccessorInfo> map = 
					new HashMap<StateContainer<LETTER,STATE>,SuccessorInfo>();
			mSuccInfos.add(map);
			for (final StateContainer<LETTER, STATE> sc  : mSuccInfos.get(mIteration - 1).keySet()) {
				addPredecessors(sc, map);
			}
		}
	}

	/**
	 * Use mSuccInfos to construct a run for a loop that has been found.
	 * @throws AutomataOperationCanceledException if timeout exceeds
	 */
	private void constructRunOfLoop() throws AutomataOperationCanceledException {
		//then we reconstruct the run
		mLoop = new NestedRun<LETTER, STATE>(mGoal.getState());
		StateContainer<LETTER, STATE> current = mGoal;
		for (int i = mIteration; i >= 0; i--) {
			NestedRun<LETTER, STATE> newSuffix;
			final SuccessorInfo info = mSuccInfos.get(i).get(current);
			if (info.getTransition() instanceof IncomingInternalTransition) {
				final IncomingInternalTransition<LETTER, STATE> inTrans =
						(IncomingInternalTransition<LETTER, STATE>) info.getTransition();
				newSuffix = new NestedRun<LETTER, STATE>(current.getState(),
						inTrans.getLetter(), NestedWord.INTERNAL_POSITION,
						info.getSuccessor().getState());
			} else if (info.getTransition() instanceof IncomingCallTransition) {
				final IncomingCallTransition<LETTER, STATE> inTrans =
						(IncomingCallTransition<LETTER, STATE>) info.getTransition();
				newSuffix = new NestedRun<LETTER, STATE>(current.getState(),
						inTrans.getLetter(), NestedWord.PLUS_INFINITY,
						info.getSuccessor().getState());
			} else if (info.getTransition() instanceof IncomingReturnTransition) {
				final IncomingReturnTransition<LETTER, STATE> inTrans =
						(IncomingReturnTransition<LETTER, STATE>) info.getTransition();
				final StateContainer<LETTER, STATE> returnPredSc =
						mNwars.obtainSC(inTrans.getLinPred());
				assert current.getState().equals(inTrans.getHierPred());
				final Summary<LETTER,STATE> summary = new Summary<LETTER, STATE>(
						current,
						returnPredSc,
						inTrans.getLetter(),
						info.getSuccessor());
				final boolean summaryMustContainAccepting =
						mFindAcceptingSummary && i == 0;
				newSuffix = (new RunConstructor<LETTER, STATE>(mServices,
						mNwars, summary, summaryMustContainAccepting)).constructRun();
				assert newSuffix != null : "no run for summary found";
			} else {
				throw new AssertionError("unsupported transition");
			}
			mLoop = mLoop.concatenate(newSuffix);
			current = info.getSuccessor();
		}
	}
    
    public NestedLassoRun<LETTER, STATE> getNestedLassoRun() {
		return mLasso;
	}

	/**
	 * Add for all predecessors of sc that have not yet been visited the
	 * successor information to map.
	 */
	private void addPredecessors(final StateContainer<LETTER,STATE> sc,
			final Map<StateContainer<LETTER,STATE>,SuccessorInfo> succInfo) {
		for (final IncomingReturnTransition<LETTER, STATE> inTrans : mNwars.returnPredecessors(sc.getState())) {
			final StateContainer<LETTER, STATE> predSc = mNwars.obtainSC(inTrans.getHierPred());
			checkAndAddPredecessor(sc, succInfo, inTrans, predSc);
		}
		for (final IncomingCallTransition<LETTER, STATE> inTrans : mNwars.callPredecessors(sc.getState())) {
			final StateContainer<LETTER, STATE> predSc = mNwars.obtainSC(inTrans.getPred());
			checkAndAddPredecessor(sc, succInfo, inTrans, predSc);
		}
		for (final IncomingInternalTransition<LETTER, STATE> inTrans : mNwars.internalPredecessors(sc.getState())) {
			final StateContainer<LETTER, STATE> predSc = mNwars.obtainSC(inTrans.getPred());
			checkAndAddPredecessor(sc, succInfo, inTrans, predSc);
		}
    }

	/**
	 * Add successor information for predSc and inTrans, if predSc is in
	 * SCC and has not been visited before.
	 */
	private void checkAndAddPredecessor(final StateContainer<LETTER, STATE> sc,
			final Map<StateContainer<LETTER, STATE>, SuccessorInfo> succInfo,
			final ITransitionlet<LETTER, STATE> inTrans,
			final StateContainer<LETTER, STATE> predSc) {
		if (mScc.getNodes().contains(predSc) && !mVisited.contains(predSc)) {
			mVisited.add(predSc);
			final SuccessorInfo info = new SuccessorInfo(inTrans, sc);
			succInfo.put(predSc, info);
			if (mGoal.equals(predSc)) {
				mGoalFound = true;
			}
		}
	}
	
	
	
	private class SuccessorInfo {
		private final ITransitionlet<LETTER, STATE> mTransition;
		private final StateContainer<LETTER,STATE> mSuccessor;
		
		public SuccessorInfo(final ITransitionlet<LETTER, STATE> transition, final StateContainer<LETTER, STATE> successor) {
			super();
			mTransition = transition;
			mSuccessor = successor;
		}
		
		public ITransitionlet<LETTER, STATE> getTransition() {
			return mTransition;
		}
		
		public StateContainer<LETTER, STATE> getSuccessor() {
			return mSuccessor;
		}
		
	}
}
