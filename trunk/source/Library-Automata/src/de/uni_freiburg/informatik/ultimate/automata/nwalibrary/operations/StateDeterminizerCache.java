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
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations;

import java.util.HashMap;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.DeterminizedState;

public class StateDeterminizerCache<LETTER, STATE> implements
		IStateDeterminizer<LETTER, STATE> {
	
	
	private final IStateDeterminizer<LETTER, STATE> mStateDeterminizer;
	
	Map<DeterminizedState<LETTER,STATE>,Map<LETTER,DeterminizedState<LETTER,STATE>>>	mInternalSuccessorCache =
	new HashMap<DeterminizedState<LETTER,STATE>,Map<LETTER,DeterminizedState<LETTER,STATE>>>();
	
	Map<DeterminizedState<LETTER,STATE>,Map<LETTER,DeterminizedState<LETTER,STATE>>>	mCallSuccessorCache =
		new HashMap<DeterminizedState<LETTER,STATE>,Map<LETTER,DeterminizedState<LETTER,STATE>>>();
	
	Map<DeterminizedState<LETTER,STATE>,Map<DeterminizedState<LETTER,STATE>,Map<LETTER,DeterminizedState<LETTER,STATE>>>> 
	 mReturnSuccessorCache = new HashMap<DeterminizedState<LETTER,STATE>,
		Map<DeterminizedState<LETTER,STATE>,Map<LETTER,DeterminizedState<LETTER,STATE>>>>();

	int mInternalSuccs = 0;
	int mInternalSuccsCache = 0;
	int mCallSuccs = 0;
	int mCallSuccsCache = 0;
	int mReturnSuccs = 0;
	int mReturnSuccsCache = 0;
	int mUnnecessary = 0;
	
	
	public StateDeterminizerCache(
			IStateDeterminizer<LETTER, STATE> stateDeterminizer) {
		mStateDeterminizer = stateDeterminizer;
	}

	@Override
	public DeterminizedState<LETTER, STATE> initialState() {
		return mStateDeterminizer.initialState();
	}

	
	@Override
	public DeterminizedState<LETTER, STATE> internalSuccessor(
			DeterminizedState<LETTER, STATE> detState, LETTER symbol) {
		DeterminizedState<LETTER, STATE> detSucc;
		detSucc = internalSuccessorCache(detState, symbol);
		if (detSucc == null) {
			detSucc = mStateDeterminizer.internalSuccessor(detState, symbol);
			putInternalSuccessorCache(detState, symbol, detSucc);
			mInternalSuccs++;
		} else {
			mInternalSuccsCache++;
		}
		return detSucc;
	}


	@Override
	public DeterminizedState<LETTER, STATE> callSuccessor(
			DeterminizedState<LETTER, STATE> detState, LETTER symbol) {
		DeterminizedState<LETTER, STATE> detSucc;
		detSucc = callSuccessorCache(detState, symbol);
		if (detSucc == null) {
			detSucc = mStateDeterminizer.callSuccessor(detState, symbol);
			putCallSuccessorCache(detState, symbol, detSucc);
			mCallSuccs++;
		} else {
			mCallSuccsCache++;
		}
		return detSucc;
	}


	@Override
	public DeterminizedState<LETTER, STATE> returnSuccessor(
			DeterminizedState<LETTER, STATE> detState,
			DeterminizedState<LETTER, STATE> hierPred, LETTER symbol) {
		DeterminizedState<LETTER, STATE> detSucc;
		detSucc = returnSuccessorCache(detState, hierPred, symbol);
		if (detSucc == null) {
			detSucc = mStateDeterminizer.returnSuccessor(detState, hierPred, symbol);
			putReturnSuccessorCache(detState, hierPred, symbol, detSucc);
			mReturnSuccs++;
		} else {
			mReturnSuccsCache++;
		}
		return detSucc;
	}

	@Override
	public int getMaxDegreeOfNondeterminism() {
		return mStateDeterminizer.getMaxDegreeOfNondeterminism();
	}
	
	

	private DeterminizedState<LETTER,STATE> internalSuccessorCache(
			DeterminizedState<LETTER,STATE>  state,
			LETTER symbol) {
		Map<LETTER, DeterminizedState<LETTER,STATE>> symbol2succ = 
			mInternalSuccessorCache.get(state);
		if (symbol2succ == null) {
			return null;
		}
		return symbol2succ.get(symbol);
	}
	
	private void putInternalSuccessorCache(
			DeterminizedState<LETTER,STATE>  state,
			LETTER symbol,
			DeterminizedState<LETTER,STATE>  succ) {
		Map<LETTER, DeterminizedState<LETTER,STATE>> symbol2succ = 
			mInternalSuccessorCache.get(state);
		if (symbol2succ == null) {
			symbol2succ = new HashMap<LETTER,DeterminizedState<LETTER,STATE>>();
			mInternalSuccessorCache.put(state, symbol2succ);
		}
		symbol2succ.put(symbol, succ);	
	}
	
	
	
	private DeterminizedState<LETTER,STATE> callSuccessorCache(
			DeterminizedState<LETTER,STATE>  state,
			LETTER symbol) {
		Map<LETTER, DeterminizedState<LETTER,STATE>> symbol2succ = 
			mCallSuccessorCache.get(state);
		if (symbol2succ == null) {
			return null;
		}
		return symbol2succ.get(symbol);
	}
	
	private void putCallSuccessorCache(
			DeterminizedState<LETTER,STATE>  state,
			LETTER symbol,
			DeterminizedState<LETTER,STATE>  succ) {
		Map<LETTER, DeterminizedState<LETTER,STATE>> symbol2succ = 
			mCallSuccessorCache.get(state);
		if (symbol2succ == null) {
			symbol2succ = new HashMap<LETTER,DeterminizedState<LETTER,STATE>>();
			mCallSuccessorCache.put(state, symbol2succ);
		}
		symbol2succ.put(symbol, succ);	
	}
	
	private DeterminizedState<LETTER,STATE> returnSuccessorCache(
			DeterminizedState<LETTER,STATE>  state,
			DeterminizedState<LETTER,STATE> linPred,
			LETTER symbol) {
		Map<DeterminizedState<LETTER,STATE>,Map<LETTER, DeterminizedState<LETTER,STATE>>> linPred2symbol2succ =
			mReturnSuccessorCache.get(linPred);
		if (linPred2symbol2succ == null) {
			return null;
		}
		Map<LETTER, DeterminizedState<LETTER,STATE>> symbol2succ = 
			linPred2symbol2succ.get(state);
		if (symbol2succ == null) {
			return null;
		}
		return symbol2succ.get(symbol);
	}
	
	private void putReturnSuccessorCache(
			DeterminizedState<LETTER,STATE>  state,
			DeterminizedState<LETTER,STATE> linPred,
			LETTER symbol,
			DeterminizedState<LETTER,STATE>  succ) {
		Map<DeterminizedState<LETTER,STATE>,Map<LETTER, DeterminizedState<LETTER,STATE>>> linPred2symbol2succ =
			mReturnSuccessorCache.get(linPred);
		if (linPred2symbol2succ == null) {
			linPred2symbol2succ = 
				new HashMap<DeterminizedState<LETTER,STATE>,Map<LETTER, DeterminizedState<LETTER,STATE>>>();
			mReturnSuccessorCache.put(linPred, linPred2symbol2succ);
		}
		Map<LETTER, DeterminizedState<LETTER,STATE>> symbol2succ = 
			linPred2symbol2succ.get(state);
		if (symbol2succ == null) {
			symbol2succ = new HashMap<LETTER,DeterminizedState<LETTER,STATE>>();
			linPred2symbol2succ.put(state, symbol2succ);
		}
		symbol2succ.put(symbol, succ);	
	}

	@Override
	public boolean useDoubleDeckers() {
		return mStateDeterminizer.useDoubleDeckers();
	}

	@Override
	public STATE getState(DeterminizedState<LETTER, STATE> determinizedState) {
		return mStateDeterminizer.getState(determinizedState);
	}

}
