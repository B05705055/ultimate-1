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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingReturnTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingReturnTransition;

/**
 * Contains STATES and information of transitions.
 *
 * @param <LETTER> letter type
 * @param <STATE> state type
 */
class StateContainerFieldAndMap<LETTER,STATE> extends StateContainer<LETTER, STATE> {

	private final Set<LETTER> mEmptySetOfLetters = new HashSet<LETTER>(0);
	private final Collection<STATE> mEmptySetOfStates = new HashSet<STATE>(0);
	
	private Object mOut1;
	private Object mOut2;
	private Object mOut3;
	private Object mIn1;
	private Object mIn2;
	private Object mIn3;

	StateContainerFieldAndMap(final STATE state, final int serialNumber, 
			final HashMap<STATE,Integer> downStates, final boolean canHaveOutgoingReturn) {
		super(state, serialNumber, downStates,canHaveOutgoingReturn);
	}


	boolean mapModeOutgoing() {
		return (mOut1 instanceof Map) ||(mOut2 instanceof Map) || (mOut3 instanceof Map); 
	}
	
	boolean mapModeIncoming() {
		return (mIn1 instanceof Map) ||(mIn2 instanceof Map) || (mIn3 instanceof Map); 
	}
	
	
	private void switchOutgoingToMapMode() {
		assert(!mapModeOutgoing());
		final List<Object> transitions = new ArrayList<Object>(3);
		if (mOut1 != null) {
			transitions.add(mOut1);
			mOut1 = null;
		}
		if (mOut2 != null) {
			transitions.add(mOut2);
			mOut2 = null;
		}
		if (mOut3 != null) {
			transitions.add(mOut3);
			mOut3 = null;
		}
		for (final Object trans : transitions) {
			if (trans instanceof OutgoingInternalTransition) {
				addInternalOutgoingMap((OutgoingInternalTransition<LETTER, STATE>) trans);
			} else if (trans instanceof OutgoingCallTransition) {
				addCallOutgoingMap((OutgoingCallTransition<LETTER, STATE>) trans);
			} else if (trans instanceof OutgoingReturnTransition) {
				addReturnOutgoingMap((OutgoingReturnTransition<LETTER, STATE>) trans);
			} else {
				throw new AssertionError();
			}
		}
		assert (mapModeOutgoing());
	}
	
	private void switchIncomingToMapMode() {
		assert(!mapModeIncoming());
		final List<Object> transitions = new ArrayList<Object>(3);
		if (mIn1 != null) {
			transitions.add(mIn1);
			mIn1 = null;
		}
		if (mIn2 != null) {
			transitions.add(mIn2);
			mIn2 = null;
		}
		if (mIn3 != null) {
			transitions.add(mIn3);
			mIn3 = null;
		}
		for (final Object trans : transitions) {
			if (trans instanceof IncomingInternalTransition) {
				addInternalIncomingMap((IncomingInternalTransition<LETTER, STATE>) trans);
			} else if (trans instanceof IncomingCallTransition) {
				addCallIncomingMap((IncomingCallTransition<LETTER, STATE>) trans);
			} else if (trans instanceof IncomingReturnTransition) {
				addReturnIncomingMap((IncomingReturnTransition<LETTER, STATE>) trans);
			} else {
				throw new AssertionError();
			}
		}
		assert (mapModeIncoming());
	}
	
	
	
	@Override
	void addInternalOutgoing(final OutgoingInternalTransition<LETTER, STATE> trans) {
		if (mapModeOutgoing()) {
			addInternalOutgoingMap(trans);
		} else {
			if (mOut1 == null) {
				mOut1 = trans;
			} else if (mOut2 == null) {
				mOut2 = trans;
			} else if (mOut3 == null && (mOut2 instanceof OutgoingInternalTransition)) {
				mOut3 = trans;
			} else {
				switchOutgoingToMapMode();
				addInternalOutgoingMap(trans);
			}
		}
	}


	@Override
	void addInternalIncoming(final IncomingInternalTransition<LETTER, STATE> trans) {
		if (mapModeIncoming()) {
			addInternalIncomingMap(trans);
		} else {
			if (mIn1 == null) {
				mIn1 = trans;
			} else if (mIn2 == null) {
				mIn2 = trans;
			} else if (mIn3 == null && (mIn2 instanceof IncomingInternalTransition)) {
				mIn3 = trans;
			} else {
				switchIncomingToMapMode();
				addInternalIncomingMap(trans);
			}
		}
	}


	@Override
	void addCallOutgoing(final OutgoingCallTransition<LETTER, STATE> trans) {
		if (mapModeOutgoing()) {
			addCallOutgoingMap(trans);
		} else {
			if (mOut2 == null) {
				mOut2 = trans;
			} else {
				switchOutgoingToMapMode();
				addCallOutgoingMap(trans);
			}
		}
	}


	@Override
	void addCallIncoming(final IncomingCallTransition<LETTER, STATE> trans) {
		if (mapModeIncoming()) {
			addCallIncomingMap(trans);
		} else {
			if (mIn2 == null) {
				mIn2 = trans;
			} else {
				switchIncomingToMapMode();
				addCallIncomingMap(trans);
			}
		}
	}


	@Override
	void addReturnOutgoing(final OutgoingReturnTransition<LETTER, STATE> trans) {
		if (mapModeOutgoing()) {
			addReturnOutgoingMap(trans);
		} else {
			if (mOut3 == null) {
				mOut3 = trans;
			} else {
				switchOutgoingToMapMode();
				addReturnOutgoingMap(trans);
			}
		}
	}


	@Override
	void addReturnIncoming(final IncomingReturnTransition<LETTER, STATE> trans) {
		if (mapModeIncoming()) {
			addReturnIncomingMap(trans);
		} else {
			if (mIn3 == null) {
				mIn3 = trans;
			} else {
				switchIncomingToMapMode();
				addReturnIncomingMap(trans);
			}
		}
	}


	
	void addInternalOutgoingMap(final OutgoingInternalTransition<LETTER, STATE> internalOutgoing) {
		final LETTER letter = internalOutgoing.getLetter();
		final STATE succ = internalOutgoing.getSucc();
		if (mOut1 == null) {
			mOut1 = new HashMap<LETTER, Set<STATE>>();
		}
		Set<STATE> succs = ((Map<LETTER, Set<STATE>>) mOut1).get(letter);
		if (succs == null) {
			succs = new HashSet<STATE>();
			((Map<LETTER, Set<STATE>>) mOut1).put(letter, succs);
		}
		succs.add(succ);
	}

	void addInternalIncomingMap(final IncomingInternalTransition<LETTER, STATE> internalIncoming) {
		final LETTER letter = internalIncoming.getLetter();
		final STATE pred = internalIncoming.getPred();
		if (mIn1 == null) {
			mIn1 = new HashMap<LETTER, Set<STATE>>();
		}
		Set<STATE> preds = ((Map<LETTER, Set<STATE>>) mIn1).get(letter);
		if (preds == null) {
			preds = new HashSet<STATE>();
			((Map<LETTER, Set<STATE>>) mIn1).put(letter,preds);
		}
		preds.add(pred);
	}

	void addCallOutgoingMap(final OutgoingCallTransition<LETTER, STATE> callOutgoing) {
		final LETTER letter = callOutgoing.getLetter();
		final STATE succ = callOutgoing.getSucc();
		if (mOut2 == null) {
			mOut2 = new HashMap<LETTER, Set<STATE>>();
		}
		Set<STATE> succs = ((Map<LETTER, Set<STATE>>) mOut2).get(letter);
		if (succs == null) {
			succs = new HashSet<STATE>();
			((Map<LETTER, Set<STATE>>) mOut2).put(letter,succs);
		}
		succs.add(succ);
	}
	void addCallIncomingMap(final IncomingCallTransition<LETTER, STATE> callIncoming) {
		final LETTER letter = callIncoming.getLetter();
		final STATE pred = callIncoming.getPred();
		if (mIn2 == null) {
			mIn2 = new HashMap<LETTER, Set<STATE>>();
		}
		Set<STATE> preds = ((Map<LETTER, Set<STATE>>) mIn2).get(letter);
		if (preds == null) {
			preds = new HashSet<STATE>();
			((Map<LETTER, Set<STATE>>) mIn2).put(letter,preds);
		}
		preds.add(pred);
	}
	void addReturnOutgoingMap(final OutgoingReturnTransition<LETTER, STATE> returnOutgoing) {
		final LETTER letter = returnOutgoing.getLetter();
		final STATE hier = returnOutgoing.getHierPred();
		final STATE succ = returnOutgoing.getSucc();
		if (mOut3 == null) {
			mOut3 = new HashMap<LETTER, Map<STATE, Set<STATE>>>();
		}
		Map<STATE, Set<STATE>> hier2succs = ((Map<LETTER, Map<STATE, Set<STATE>>>) mOut3).get(letter);
		if (hier2succs == null) {
			hier2succs = new HashMap<STATE, Set<STATE>>();
			((Map<LETTER, Map<STATE, Set<STATE>>>) mOut3).put(letter, hier2succs);
		}
		Set<STATE> succs = hier2succs.get(hier);
		if (succs == null) {
			succs = new HashSet<STATE>();
			hier2succs.put(hier, succs);
		}
		succs.add(succ);
	}
	void addReturnIncomingMap(final IncomingReturnTransition<LETTER, STATE> returnIncoming) {
		final LETTER letter = returnIncoming.getLetter();
		final STATE hier = returnIncoming.getHierPred();
		final STATE pred = returnIncoming.getLinPred();
		if (mIn3 == null) {
			mIn3 = new HashMap<LETTER, Map<STATE, Set<STATE>>>();
		}
		Map<STATE, Set<STATE>> hier2preds = ((Map<LETTER, Map<STATE, Set<STATE>>>) mIn3).get(letter);
		if (hier2preds == null) {
			hier2preds = new HashMap<STATE, Set<STATE>>();
			((Map<LETTER, Map<STATE, Set<STATE>>>) mIn3).put(letter, hier2preds);
		}
		Set<STATE> preds = hier2preds.get(hier);
		if (preds == null) {
			preds = new HashSet<STATE>();
			hier2preds.put(hier, preds);
		}
		preds.add(pred);
	}


	@Override
	public Set<LETTER> lettersInternal() {
		if (mapModeOutgoing()) {
			final Map<LETTER, Set<STATE>> map = (Map<LETTER, Set<STATE>>) mOut1;
			return map == null ? mEmptySetOfLetters : map.keySet();
		} else {
			final Set<LETTER> result = new HashSet<LETTER>(3);
			if (mOut1 instanceof OutgoingInternalTransition) {
				LETTER letter = ((OutgoingInternalTransition<LETTER, STATE>) mOut1).getLetter();
				result.add(letter);
				if (mOut2 instanceof OutgoingInternalTransition) {
					letter = ((OutgoingInternalTransition<LETTER, STATE>) mOut2).getLetter();
					if (!result.contains(letter)) {
						result.add(letter);
					}
					if (mOut3 instanceof OutgoingInternalTransition) {
						letter = ((OutgoingInternalTransition<LETTER, STATE>) mOut3).getLetter();
						if (!result.contains(letter)) {
							result.add(letter);
							
						}
					}
				}
			}
			return result;
		}
	}


	@Override
	public Set<LETTER> lettersInternalIncoming() {
		if (mapModeIncoming()) {
			final Map<LETTER, Set<STATE>> map = (Map<LETTER, Set<STATE>>) mIn1;
			return map == null ? mEmptySetOfLetters : map.keySet();
		} else {
			final Set<LETTER> result = new HashSet<LETTER>(3);
			if (mIn1 instanceof IncomingInternalTransition) {
				LETTER letter = ((IncomingInternalTransition<LETTER, STATE>) mIn1).getLetter();
				result.add(letter);
				if (mIn2 instanceof IncomingInternalTransition) {
					letter = ((IncomingInternalTransition<LETTER, STATE>) mIn2).getLetter();
					if (!result.contains(letter)) {
						result.add(letter);
					}
					if (mIn3 instanceof IncomingInternalTransition) {
						letter = ((IncomingInternalTransition<LETTER, STATE>) mIn3).getLetter();
						if (!result.contains(letter)) {
							result.add(letter);
							
						}
					}
				}
			}
			return result;
		}
	}

	@Override
	public Set<LETTER> lettersCall() {
		if (mapModeOutgoing()) {
			final Map<LETTER, Set<STATE>> map = (Map<LETTER, Set<STATE>>) mOut2;
			return map == null ? mEmptySetOfLetters : map.keySet();
		} else {
			final Set<LETTER> result = new HashSet<LETTER>(1);
			if (mOut2 instanceof OutgoingCallTransition) {
				final LETTER letter = ((OutgoingCallTransition<LETTER, STATE>) mOut2).getLetter();
				result.add(letter);
			}
			return result;
		}
	}

	@Override
	public Set<LETTER> lettersCallIncoming() {
		if (mapModeIncoming()) {
			final Map<LETTER, Set<STATE>> map = (Map<LETTER, Set<STATE>>) mIn2;
			return map == null ? mEmptySetOfLetters : map.keySet();
		} else {
			final Set<LETTER> result = new HashSet<LETTER>(1);
			if (mIn2 instanceof IncomingCallTransition) {
				final LETTER letter = ((IncomingCallTransition<LETTER, STATE>) mIn2).getLetter();
				result.add(letter);
			}
			return result;
		}
	}

	@Override
	public Set<LETTER> lettersReturn() {
		if (mapModeOutgoing()) {
			final Map<LETTER, Map<STATE, Set<STATE>>> map = (Map<LETTER, Map<STATE, Set<STATE>>>) mOut3;
			return map == null ? mEmptySetOfLetters : map.keySet();
		} else {
			final Set<LETTER> result = new HashSet<LETTER>(1);
			if (mOut3 instanceof OutgoingReturnTransition) {
				final LETTER letter = ((OutgoingReturnTransition<LETTER, STATE>) mOut3).getLetter();
				result.add(letter);
			}
			return result;
		}
	}

	@Override
	public Set<LETTER> lettersReturnIncoming() {
		if (mapModeIncoming()) {
			final Map<LETTER, Map<STATE, Set<STATE>>> map = (Map<LETTER, Map<STATE, Set<STATE>>>) mIn3;
			return map == null ? mEmptySetOfLetters : map.keySet();
		} else {
			final Set<LETTER> result = new HashSet<LETTER>(1);
			if (mIn3 instanceof IncomingReturnTransition) {
				final LETTER letter = ((IncomingReturnTransition<LETTER, STATE>) mIn3).getLetter();
				result.add(letter);
			}
			return result;
		}
	}


	@Override
	public Collection<STATE> succInternal(final LETTER letter) {
		if (mapModeOutgoing()) {
			final Map<LETTER, Set<STATE>> map = (Map<LETTER, Set<STATE>>) mOut1;
			if (map == null) {
				return mEmptySetOfStates;
			}
			final Set<STATE> result = map.get(letter);
			return result == null ? mEmptySetOfStates : result;
		} else {
			final Collection<STATE> result = new ArrayList<STATE>(3);
			if (properOutgoingInternalTransitionAtPosition1(letter)) {
				final STATE state = ((OutgoingInternalTransition<LETTER, STATE>) mOut1).getSucc();
				result.add(state);
			}
			if (properOutgoingInternalTransitionAtPosition2(letter)) {
				final STATE state = ((OutgoingInternalTransition<LETTER, STATE>) mOut2).getSucc();
				if (!result.contains(state)) {
					result.add(state);
				}
			}
			if (properOutgoingInternalTransitionAtPosition3(letter)) {
				final STATE state = ((OutgoingInternalTransition<LETTER, STATE>) mOut3).getSucc();
				if (!result.contains(state)) {
					result.add(state);
				}
			}
			return result;
		}
	}

	@Override
	public Collection<STATE> predInternal(final LETTER letter) {
		if (mapModeIncoming()) {
			final Map<LETTER, Set<STATE>> map = (Map<LETTER, Set<STATE>>) mIn1;
			if (map == null) {
				return mEmptySetOfStates;
			}
			final Set<STATE> result = map.get(letter);
			return result == null ? mEmptySetOfStates : result;
		} else {
			final Collection<STATE> result = new ArrayList<STATE>(3);
			if (properIncomingInternalTransitionAtPosition1(letter)) {
				final STATE state = ((IncomingInternalTransition<LETTER, STATE>) mIn1).getPred();
				result.add(state);
			}
			if (properIncomingInternalTransitionAtPosition2(letter)) {
				final STATE state = ((IncomingInternalTransition<LETTER, STATE>) mIn2).getPred();
				if (!result.contains(state)) {
					result.add(state);
				}
			}
			if (properIncomingInternalTransitionAtPosition3(letter)) {
				final STATE state = ((IncomingInternalTransition<LETTER, STATE>) mIn3).getPred();
				if (!result.contains(state)) {
					result.add(state);
				}
			}
			return result;
		}
	}

	@Override
	public Collection<STATE> succCall(final LETTER letter) {
		if (mapModeOutgoing()) {
			final Map<LETTER, Set<STATE>> map = (Map<LETTER, Set<STATE>>) mOut2;
			if (map == null) {
				return mEmptySetOfStates;
			}
			final Set<STATE> result = map.get(letter);
			return result == null ? mEmptySetOfStates : result;
		} else {
			final Collection<STATE> result = new ArrayList<STATE>(1);
			if (properOutgoingCallTransitionAtPosition2(letter)) {
				final STATE state = ((OutgoingCallTransition<LETTER, STATE>) mOut2).getSucc();
				result.add(state);
			}
			return result;
		}
	}

	@Override
	public Collection<STATE> predCall(final LETTER letter) {
		if (mapModeIncoming()) {
			final Map<LETTER, Set<STATE>> map = (Map<LETTER, Set<STATE>>) mIn2;
			if (map == null) {
				return mEmptySetOfStates;
			}
			final Set<STATE> result = map.get(letter);
			return result == null ? mEmptySetOfStates : result;
		} else {
			final Collection<STATE> result = new ArrayList<STATE>(1);
			if (properIncomingCallTransitionAtPosition2(letter)) {
				final STATE state = ((IncomingCallTransition<LETTER, STATE>) mIn2).getPred();
				result.add(state);
			}
			return result;
		}
	}

	@Override
	public Collection<STATE> hierPred(final LETTER letter) {
		if (mapModeOutgoing()) {
			final Map<LETTER, Map<STATE, Set<STATE>>> map = (Map<LETTER, Map<STATE, Set<STATE>>>) mOut3;
			if (map == null) {
				return mEmptySetOfStates;
			}
			final Map<STATE, Set<STATE>> hier2succs = map.get(letter);
			return hier2succs == null ? mEmptySetOfStates : hier2succs.keySet();
		} else {
			final Collection<STATE> result = new ArrayList<STATE>(1);
			if (properOutgoingReturnTransitionAtPosition3(null, letter)) {
				final STATE state = ((OutgoingReturnTransition<LETTER, STATE>) mOut3).getHierPred();
				result.add(state);
			}
			return result;
		}
	}

	@Override
	public Collection<STATE> succReturn(final STATE hier, final LETTER letter) {
		if (mapModeOutgoing()) {
			final Map<LETTER, Map<STATE, Set<STATE>>> map = (Map<LETTER, Map<STATE, Set<STATE>>>) mOut3;
			if (map == null) {
				return mEmptySetOfStates;
			}
			final Map<STATE, Set<STATE>> hier2succs = map.get(letter);
			if (hier2succs == null) {
				return mEmptySetOfStates;
			}
			final Set<STATE> result = hier2succs.get(hier);
			return result == null ? mEmptySetOfStates : result;
		} else {
			final Collection<STATE> result = new ArrayList<STATE>(1);
			if (properOutgoingReturnTransitionAtPosition3(hier, letter)) {
				final STATE state = ((OutgoingReturnTransition<LETTER, STATE>) mOut3).getSucc();
				result.add(state);
			}
			return result;
		}
	}

	@Override
	public Collection<STATE> predReturnLin(final LETTER letter, final STATE hier) {
		if (mapModeIncoming()) {
			final Map<LETTER, Map<STATE, Set<STATE>>> letter2hier2preds  = (Map<LETTER, Map<STATE, Set<STATE>>>) mIn3;
			if (letter2hier2preds == null) {
				return mEmptySetOfStates;
			}
			final Map<STATE, Set<STATE>> hier2preds = letter2hier2preds.get(letter);
			if (hier2preds == null) {
				return mEmptySetOfStates;
			}
			final Set<STATE> result = hier2preds.get(hier);
			return result == null ? mEmptySetOfStates : result;
		} else {
			final Collection<STATE> result = new ArrayList<STATE>(1);
			if (properIncomingReturnTransitionAtPosition3(hier, letter)) {
				final STATE state = ((IncomingReturnTransition<LETTER, STATE>) mIn3).getLinPred();
				result.add(state);
			}
			return result;
		}
	}

	@Override
	public Collection<STATE> predReturnHier(final LETTER letter) {
		if (mapModeIncoming()) {
			final Map<LETTER, Map<STATE, Set<STATE>>> letter2hier2preds  = (Map<LETTER, Map<STATE, Set<STATE>>>) mIn3;
			if (letter2hier2preds == null) {
				return mEmptySetOfStates ;
			}
			final Map<STATE, Set<STATE>> hier2preds = letter2hier2preds.get(letter);
			if (hier2preds == null) {
				return mEmptySetOfStates;
			}
			return hier2preds.keySet();
		} else {
			final Collection<STATE> result = new ArrayList<STATE>(1);
			if (properIncomingReturnTransitionAtPosition3(null, letter)) {
				final STATE state = ((IncomingReturnTransition<LETTER, STATE>) mIn3).getHierPred();
				result.add(state);
			}
			return result;
		}
	}



	
	private Iterable<IncomingInternalTransition<LETTER, STATE>> internalPredecessorsMap(
			final LETTER letter) {
		assert (mapModeIncoming());
		return new Iterable<IncomingInternalTransition<LETTER, STATE>>() {
			@Override
			public Iterator<IncomingInternalTransition<LETTER, STATE>> iterator() {
				final Iterator<IncomingInternalTransition<LETTER, STATE>> iterator = 
						new Iterator<IncomingInternalTransition<LETTER, STATE>>() {
					private Iterator<STATE> mIterator;
					{
						final Map<LETTER, Set<STATE>> letter2pred = (Map<LETTER, Set<STATE>>) mIn1;
						if (letter2pred != null) {
							if (letter2pred.get(letter) != null) {
								mIterator = letter2pred.get(letter).iterator();
							} else {
								mIterator = null;
							}
						} else {
							mIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return mIterator != null && mIterator.hasNext();
					}

					@Override
					public IncomingInternalTransition<LETTER, STATE> next() {
						if (mIterator == null) {
							throw new NoSuchElementException();
						} else {
							final STATE pred = mIterator.next(); 
							return new IncomingInternalTransition<LETTER, STATE>(pred, letter);
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



	private Iterable<IncomingInternalTransition<LETTER, STATE>> internalPredecessorsMap() {
		assert (mapModeIncoming());
		return new Iterable<IncomingInternalTransition<LETTER, STATE>>() {
			/**
			 * Iterates over all IncomingInternalTransition of succ.
			 * Iterates over all incoming internal letters and uses the 
			 * iterators returned by internalPredecessorsMap(letter, succ)
			 */
			@Override
			public Iterator<IncomingInternalTransition<LETTER, STATE>> iterator() {
				final Iterator<IncomingInternalTransition<LETTER, STATE>> iterator = 
						new Iterator<IncomingInternalTransition<LETTER, STATE>>() {
					private Iterator<LETTER> mLetterIterator;
					private LETTER mCurrentLetter;
					private Iterator<IncomingInternalTransition<LETTER, STATE>> mCurrentIterator;
					{
						mLetterIterator = lettersInternalIncoming().iterator();
						nextLetter();
					}

					private void nextLetter() {
						if (mLetterIterator.hasNext()) {
							do {
								mCurrentLetter = mLetterIterator.next();
								mCurrentIterator = internalPredecessorsMap(
										mCurrentLetter).iterator();
							} while (!mCurrentIterator.hasNext()
									&& mLetterIterator.hasNext());
							if (!mCurrentIterator.hasNext()) {
								mCurrentLetter = null;
								mCurrentIterator = null;
							}
						} else {
							mCurrentLetter = null;
							mCurrentIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return mCurrentLetter != null;
					}

					@Override
					public IncomingInternalTransition<LETTER, STATE> next() {
						if (mCurrentLetter == null) {
							throw new NoSuchElementException();
						} else {
							final IncomingInternalTransition<LETTER, STATE> result = 
									mCurrentIterator.next();
							if (!mCurrentIterator.hasNext()) {
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





	private Iterable<IncomingCallTransition<LETTER, STATE>> callPredecessorsMap(
			final LETTER letter) {
		assert (mapModeIncoming());
		return new Iterable<IncomingCallTransition<LETTER, STATE>>() {
			@Override
			public Iterator<IncomingCallTransition<LETTER, STATE>> iterator() {
				final Iterator<IncomingCallTransition<LETTER, STATE>> iterator = 
						new Iterator<IncomingCallTransition<LETTER, STATE>>() {
					private Iterator<STATE> mIterator;
					{
						final Map<LETTER, Set<STATE>> letter2pred = (Map<LETTER, Set<STATE>>) mIn2;
						if (letter2pred != null) {
							if (letter2pred.get(letter) != null) {
								mIterator = letter2pred.get(letter).iterator();
							} else {
								mIterator = null;
							}
						} else {
							mIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return mIterator != null && mIterator.hasNext();
					}

					@Override
					public IncomingCallTransition<LETTER, STATE> next() {
						if (mIterator == null) {
							throw new NoSuchElementException();
						} else {
							final STATE pred = mIterator.next(); 
							return new IncomingCallTransition<LETTER, STATE>(pred, letter);
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



	private Iterable<IncomingCallTransition<LETTER, STATE>> callPredecessorsMap() {
		assert (mapModeIncoming());
		return new Iterable<IncomingCallTransition<LETTER, STATE>>() {
			/**
			 * Iterates over all IncomingCallTransition of succ.
			 * Iterates over all incoming call letters and uses the 
			 * iterators returned by callPredecessorsMap(letter, succ)
			 */
			@Override
			public Iterator<IncomingCallTransition<LETTER, STATE>> iterator() {
				final Iterator<IncomingCallTransition<LETTER, STATE>> iterator = 
						new Iterator<IncomingCallTransition<LETTER, STATE>>() {
					private Iterator<LETTER> mLetterIterator;
					private LETTER mCurrentLetter;
					private Iterator<IncomingCallTransition<LETTER, STATE>> mCurrentIterator;
					{
						mLetterIterator = lettersCallIncoming().iterator();
						nextLetter();
					}

					private void nextLetter() {
						if (mLetterIterator.hasNext()) {
							do {
								mCurrentLetter = mLetterIterator.next();
								mCurrentIterator = callPredecessorsMap(
										mCurrentLetter).iterator();
							} while (!mCurrentIterator.hasNext()
									&& mLetterIterator.hasNext());
							if (!mCurrentIterator.hasNext()) {
								mCurrentLetter = null;
								mCurrentIterator = null;
							}
						} else {
							mCurrentLetter = null;
							mCurrentIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return mCurrentLetter != null;
					}

					@Override
					public IncomingCallTransition<LETTER, STATE> next() {
						if (mCurrentLetter == null) {
							throw new NoSuchElementException();
						} else {
							final IncomingCallTransition<LETTER, STATE> result = 
									mCurrentIterator.next();
							if (!mCurrentIterator.hasNext()) {
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



	private Iterable<IncomingReturnTransition<LETTER, STATE>> returnPredecessorsMap(
			final STATE hier, final LETTER letter) {
		assert (mapModeIncoming());
		return new Iterable<IncomingReturnTransition<LETTER, STATE>>() {
			@Override
			public Iterator<IncomingReturnTransition<LETTER, STATE>> iterator() {
				final Iterator<IncomingReturnTransition<LETTER, STATE>> iterator = 
						new Iterator<IncomingReturnTransition<LETTER, STATE>>() {
					private Iterator<STATE> mIterator;
					{
						final Map<LETTER, Map<STATE, Set<STATE>>> letter2hier2pred = (Map<LETTER, Map<STATE, Set<STATE>>>) mIn3;
						if (letter2hier2pred != null) {
							final Map<STATE, Set<STATE>> hier2pred = letter2hier2pred.get(letter);
							if (hier2pred != null) {
								if (hier2pred.get(hier) != null) {
									mIterator = hier2pred.get(hier).iterator();
								} else {
									mIterator = null;
								}
							} else {
								mIterator = null;
							}
						} else {
							mIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return mIterator != null && mIterator.hasNext();
					}

					@Override
					public IncomingReturnTransition<LETTER, STATE> next() {
						if (mIterator == null) {
							throw new NoSuchElementException();
						} else {
							final STATE pred = mIterator.next(); 
							return new IncomingReturnTransition<LETTER, STATE>(pred, hier, letter);
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


	private Iterable<IncomingReturnTransition<LETTER, STATE>> returnPredecessorsMap(
			final LETTER letter) {
		assert (mapModeIncoming());
		return new Iterable<IncomingReturnTransition<LETTER, STATE>>() {
			/**
			 * Iterates over all IncomingReturnTransition of succ.
			 * Iterates over all incoming return letters and uses the 
			 * iterators returned by returnPredecessorsMap(hier, letter, succ)
			 */
			@Override
			public Iterator<IncomingReturnTransition<LETTER, STATE>> iterator() {
				final Iterator<IncomingReturnTransition<LETTER, STATE>> iterator = 
						new Iterator<IncomingReturnTransition<LETTER, STATE>>() {
					private Iterator<STATE> mHierIterator;
					private STATE mCurrentHier;
					private Iterator<IncomingReturnTransition<LETTER, STATE>> mCurrentIterator;
					{
						mHierIterator = predReturnHier(letter).iterator();
						nextHier();
					}

					private void nextHier() {
						if (mHierIterator.hasNext()) {
							do {
								mCurrentHier = mHierIterator.next();
								mCurrentIterator = returnPredecessorsMap(
										mCurrentHier, letter).iterator();
							} while (!mCurrentIterator.hasNext()
									&& mHierIterator.hasNext());
							if (!mCurrentIterator.hasNext()) {
								mCurrentHier = null;
								mCurrentIterator = null;
							}
						} else {
							mCurrentHier = null;
							mCurrentIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return mCurrentHier != null;
					}

					@Override
					public IncomingReturnTransition<LETTER, STATE> next() {
						if (mCurrentHier == null) {
							throw new NoSuchElementException();
						} else {
							final IncomingReturnTransition<LETTER, STATE> result = 
									mCurrentIterator.next();
							if (!mCurrentIterator.hasNext()) {
								nextHier();
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


	private Iterable<IncomingReturnTransition<LETTER, STATE>> returnPredecessorsMap() {
		assert (mapModeIncoming());
		return new Iterable<IncomingReturnTransition<LETTER, STATE>>() {
			/**
			 * Iterates over all IncomingReturnTransition of succ.
			 * Iterates over all incoming return letters and uses the 
			 * iterators returned by returnPredecessorsMap(letter, succ)
			 */
			@Override
			public Iterator<IncomingReturnTransition<LETTER, STATE>> iterator() {
				final Iterator<IncomingReturnTransition<LETTER, STATE>> iterator = 
						new Iterator<IncomingReturnTransition<LETTER, STATE>>() {
					private Iterator<LETTER> mLetterIterator;
					private LETTER mCurrentLetter;
					private Iterator<IncomingReturnTransition<LETTER, STATE>> mCurrentIterator;
					{
						mLetterIterator = lettersReturnIncoming().iterator();
						nextLetter();
					}

					private void nextLetter() {
						if (mLetterIterator.hasNext()) {
							do {
								mCurrentLetter = mLetterIterator.next();
								mCurrentIterator = returnPredecessorsMap(
										mCurrentLetter).iterator();
							} while (!mCurrentIterator.hasNext()
									&& mLetterIterator.hasNext());
							if (!mCurrentIterator.hasNext()) {
								mCurrentLetter = null;
								mCurrentIterator = null;
							}
						} else {
							mCurrentLetter = null;
							mCurrentIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return mCurrentLetter != null;
					}

					@Override
					public IncomingReturnTransition<LETTER, STATE> next() {
						if (mCurrentLetter == null) {
							throw new NoSuchElementException();
						} else {
							final IncomingReturnTransition<LETTER, STATE> result = 
									mCurrentIterator.next();
							if (!mCurrentIterator.hasNext()) {
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



	private Iterable<OutgoingInternalTransition<LETTER, STATE>> internalSuccessorsMap(
			final LETTER letter) {
		assert (mapModeOutgoing());
		return new Iterable<OutgoingInternalTransition<LETTER, STATE>>() {
			@Override
			public Iterator<OutgoingInternalTransition<LETTER, STATE>> iterator() {
				final Iterator<OutgoingInternalTransition<LETTER, STATE>> iterator = 
						new Iterator<OutgoingInternalTransition<LETTER, STATE>>() {
					private Iterator<STATE> mIterator;
					{
						final Map<LETTER, Set<STATE>> letter2succ = (Map<LETTER, Set<STATE>>) mOut1;
						if (letter2succ != null) {
							if (letter2succ.get(letter) != null) {
								mIterator = letter2succ.get(letter).iterator();
							} else {
								mIterator = null;
							}
						} else {
							mIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return mIterator != null && mIterator.hasNext();
					}

					@Override
					public OutgoingInternalTransition<LETTER, STATE> next() {
						if (mIterator == null) {
							throw new NoSuchElementException();
						} else {
							final STATE succ = mIterator.next(); 
							return new OutgoingInternalTransition<LETTER, STATE>(letter, succ);
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

	private Iterable<OutgoingInternalTransition<LETTER, STATE>> internalSuccessorsMap() {
		assert (mapModeOutgoing());
		return new Iterable<OutgoingInternalTransition<LETTER, STATE>>() {
			/**
			 * Iterates over all OutgoingInternalTransition of state.
			 * Iterates over all outgoing internal letters and uses the 
			 * iterators returned by internalSuccessorsMap(state, letter)
			 */
			@Override
			public Iterator<OutgoingInternalTransition<LETTER, STATE>> iterator() {
				final Iterator<OutgoingInternalTransition<LETTER, STATE>> iterator = 
						new Iterator<OutgoingInternalTransition<LETTER, STATE>>() {
					private Iterator<LETTER> mLetterIterator;
					private LETTER mCurrentLetter;
					private Iterator<OutgoingInternalTransition<LETTER, STATE>> mCurrentIterator;
					{
						mLetterIterator = lettersInternal().iterator();
						nextLetter();
					}

					private void nextLetter() {
						if (mLetterIterator.hasNext()) {
							do {
								mCurrentLetter = mLetterIterator.next();
								mCurrentIterator = internalSuccessorsMap(
										mCurrentLetter).iterator();
							} while (!mCurrentIterator.hasNext()
									&& mLetterIterator.hasNext());
							if (!mCurrentIterator.hasNext()) {
								mCurrentLetter = null;
								mCurrentIterator = null;
							}
						} else {
							mCurrentLetter = null;
							mCurrentIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return mCurrentLetter != null;
					}

					@Override
					public OutgoingInternalTransition<LETTER, STATE> next() {
						if (mCurrentLetter == null) {
							throw new NoSuchElementException();
						} else {
							final OutgoingInternalTransition<LETTER, STATE> result = 
									mCurrentIterator.next();
							if (!mCurrentIterator.hasNext()) {
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





	private Iterable<OutgoingCallTransition<LETTER, STATE>> callSuccessorsMap(
			final LETTER letter) {
		assert (mapModeOutgoing());
		return new Iterable<OutgoingCallTransition<LETTER, STATE>>() {
			@Override
			public Iterator<OutgoingCallTransition<LETTER, STATE>> iterator() {
				final Iterator<OutgoingCallTransition<LETTER, STATE>> iterator = 
						new Iterator<OutgoingCallTransition<LETTER, STATE>>() {
					private Iterator<STATE> mIterator;
					{
						final Map<LETTER, Set<STATE>> letter2succ = (Map<LETTER, Set<STATE>>) mOut2;
						if (letter2succ != null) {
							if (letter2succ.get(letter) != null) {
								mIterator = letter2succ.get(letter).iterator();
							} else {
								mIterator = null;
							}
						} else {
							mIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return mIterator != null && mIterator.hasNext();
					}

					@Override
					public OutgoingCallTransition<LETTER, STATE> next() {
						if (mIterator == null) {
							throw new NoSuchElementException();
						} else {
							final STATE succ = mIterator.next(); 
							return new OutgoingCallTransition<LETTER, STATE>(letter, succ);
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

	private Iterable<OutgoingCallTransition<LETTER, STATE>> callSuccessorsMap() {
		assert (mapModeOutgoing());
		return new Iterable<OutgoingCallTransition<LETTER, STATE>>() {
			/**
			 * Iterates over all OutgoingCallTransition of state.
			 * Iterates over all outgoing call letters and uses the 
			 * iterators returned by callSuccessorsMap(state, letter)
			 */
			@Override
			public Iterator<OutgoingCallTransition<LETTER, STATE>> iterator() {
				final Iterator<OutgoingCallTransition<LETTER, STATE>> iterator = 
						new Iterator<OutgoingCallTransition<LETTER, STATE>>() {
					private Iterator<LETTER> mLetterIterator;
					private LETTER mCurrentLetter;
					private Iterator<OutgoingCallTransition<LETTER, STATE>> mCurrentIterator;
					{
						mLetterIterator = lettersCall().iterator();
						nextLetter();
					}

					private void nextLetter() {
						if (mLetterIterator.hasNext()) {
							do {
								mCurrentLetter = mLetterIterator.next();
								mCurrentIterator = callSuccessorsMap(mCurrentLetter).iterator();
							} while (!mCurrentIterator.hasNext()
									&& mLetterIterator.hasNext());
							if (!mCurrentIterator.hasNext()) {
								mCurrentLetter = null;
								mCurrentIterator = null;
							}
						} else {
							mCurrentLetter = null;
							mCurrentIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return mCurrentLetter != null;
					}

					@Override
					public OutgoingCallTransition<LETTER, STATE> next() {
						if (mCurrentLetter == null) {
							throw new NoSuchElementException();
						} else {
							final OutgoingCallTransition<LETTER, STATE> result = 
									mCurrentIterator.next();
							if (!mCurrentIterator.hasNext()) {
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








	private Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessorsMap(
			final STATE hier, final LETTER letter) {
		assert (mapModeOutgoing());
		return new Iterable<OutgoingReturnTransition<LETTER, STATE>>() {
			@Override
			public Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator() {
				final Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator = 
						new Iterator<OutgoingReturnTransition<LETTER, STATE>>() {
					private Iterator<STATE> mIterator;
					{
						final Map<LETTER, Map<STATE, Set<STATE>>> letter2hier2succ = (Map<LETTER, Map<STATE, Set<STATE>>>) mOut3;
						if (letter2hier2succ != null) {
							final Map<STATE, Set<STATE>> hier2succ = letter2hier2succ.get(letter);
							if (hier2succ != null) {
								if (hier2succ.get(hier) != null) {
									mIterator = hier2succ.get(hier).iterator();
								} else {
									mIterator = null;
								}
							} else {
								mIterator = null;
							}
						} else {
							mIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return mIterator != null && mIterator.hasNext();
					}

					@Override
					public OutgoingReturnTransition<LETTER, STATE> next() {
						if (mIterator == null) {
							throw new NoSuchElementException();
						} else {
							final STATE succ = mIterator.next(); 
							return new OutgoingReturnTransition<LETTER, STATE>(hier, letter, succ);
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


	private Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessorsMap(
			final LETTER letter) {
		assert (mapModeOutgoing());
		return new Iterable<OutgoingReturnTransition<LETTER, STATE>>() {
			/**
			 * Iterates over all OutgoingReturnTransition of state.
			 * Iterates over all outgoing return letters and uses the 
			 * iterators returned by returnSuccecessorsMap(state, letter)
			 */
			@Override
			public Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator() {
				final Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator = 
						new Iterator<OutgoingReturnTransition<LETTER, STATE>>() {
					private Iterator<STATE> mHierIterator;
					private STATE mCurrentHier;
					private Iterator<OutgoingReturnTransition<LETTER, STATE>> mCurrentIterator;
					{
						mHierIterator = hierPred(letter).iterator();
						nextHier();
					}

					private void nextHier() {
						if (mHierIterator.hasNext()) {
							do {
								mCurrentHier = mHierIterator.next();
								mCurrentIterator = returnSuccessorsMap(
										mCurrentHier, letter).iterator();
							} while (!mCurrentIterator.hasNext()
									&& mHierIterator.hasNext());
							if (!mCurrentIterator.hasNext()) {
								mCurrentHier = null;
								mCurrentIterator = null;
							}
						} else {
							mCurrentHier = null;
							mCurrentIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return mCurrentHier != null;
					}

					@Override
					public OutgoingReturnTransition<LETTER, STATE> next() {
						if (mCurrentHier == null) {
							throw new NoSuchElementException();
						} else {
							final OutgoingReturnTransition<LETTER, STATE> result = 
									mCurrentIterator.next();
							if (!mCurrentIterator.hasNext()) {
								nextHier();
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


	private Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessorsMap(
			) {
		assert (mapModeOutgoing());
		return new Iterable<OutgoingReturnTransition<LETTER, STATE>>() {
			/**
			 * Iterates over all OutgoingReturnTransition of state.
			 * Iterates over all outgoing return letters and uses the 
			 * iterators returned by returnSuccessorsMap(state, letter)
			 */
			@Override
			public Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator() {
				final Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator = 
						new Iterator<OutgoingReturnTransition<LETTER, STATE>>() {
					private Iterator<LETTER> mLetterIterator;
					private LETTER mCurrentLetter;
					private Iterator<OutgoingReturnTransition<LETTER, STATE>> mCurrentIterator;
					{
						mLetterIterator = lettersReturn().iterator();
						nextLetter();
					}

					private void nextLetter() {
						if (mLetterIterator.hasNext()) {
							do {
								mCurrentLetter = mLetterIterator.next();
								mCurrentIterator = returnSuccessorsMap(mCurrentLetter).iterator();
							} while (!mCurrentIterator.hasNext()
									&& mLetterIterator.hasNext());
							if (!mCurrentIterator.hasNext()) {
								mCurrentLetter = null;
								mCurrentIterator = null;
							}
						} else {
							mCurrentLetter = null;
							mCurrentIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return mCurrentLetter != null;
					}

					@Override
					public OutgoingReturnTransition<LETTER, STATE> next() {
						if (mCurrentLetter == null) {
							throw new NoSuchElementException();
						} else {
							final OutgoingReturnTransition<LETTER, STATE> result = 
									mCurrentIterator.next();
							if (!mCurrentIterator.hasNext()) {
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



	private Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessorsGivenHierMap(
			final STATE hier) {
		assert (mapModeOutgoing());
		return new Iterable<OutgoingReturnTransition<LETTER, STATE>>() {
			/**
			 * Iterates over all OutgoingReturnTransition of state with 
			 * hierarchical successor hier. 
			 * Iterates over all outgoing return letters and uses the 
			 * iterators returned by returnSuccecessorsMap(state, hier, letter)
			 */
			@Override
			public Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator() {
				final Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator = 
						new Iterator<OutgoingReturnTransition<LETTER, STATE>>() {
					private Iterator<LETTER> mLetterIterator;
					private LETTER mCurrentLetter;
					private Iterator<OutgoingReturnTransition<LETTER, STATE>> mCurrentIterator;
					{
						mLetterIterator = lettersReturn().iterator();
						nextLetter();
					}

					private void nextLetter() {
						if (mLetterIterator.hasNext()) {
							do {
								mCurrentLetter = mLetterIterator.next();
								mCurrentIterator = returnSuccessorsMap(
										hier, mCurrentLetter).iterator();
							} while (!mCurrentIterator.hasNext()
									&& mLetterIterator.hasNext());
							if (!mCurrentIterator.hasNext()) {
								mCurrentLetter = null;
								mCurrentIterator = null;
							}
						} else {
							mCurrentLetter = null;
							mCurrentIterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return mCurrentLetter != null;
					}

					@Override
					public OutgoingReturnTransition<LETTER, STATE> next() {
						if (mCurrentLetter == null) {
							throw new NoSuchElementException();
						} else {
							final OutgoingReturnTransition<LETTER, STATE> result = 
									mCurrentIterator.next();
							if (!mCurrentIterator.hasNext()) {
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
	
	
	private boolean properOutgoingInternalTransitionAtPosition1(final LETTER letter) {
		return (mOut1 instanceof OutgoingInternalTransition) &&
				(letter == null || letter.equals(((OutgoingInternalTransition<LETTER, STATE>) mOut1).getLetter()));
	}
	
	private boolean properOutgoingInternalTransitionAtPosition2(final LETTER letter) {
		return (mOut2 instanceof OutgoingInternalTransition) &&
				(letter == null || letter.equals(((OutgoingInternalTransition<LETTER, STATE>) mOut2).getLetter()));
	}
	
	private boolean properOutgoingInternalTransitionAtPosition3(final LETTER letter) {
		return (mOut3 instanceof OutgoingInternalTransition) &&
				(letter == null || letter.equals(((OutgoingInternalTransition<LETTER, STATE>) mOut3).getLetter()));
	}

	private Iterable<OutgoingInternalTransition<LETTER, STATE>> internalSuccessorsField(final LETTER letter) {
		return new Iterable<OutgoingInternalTransition<LETTER, STATE>>() {
			@Override
			public Iterator<OutgoingInternalTransition<LETTER, STATE>> iterator() {
				final Iterator<OutgoingInternalTransition<LETTER, STATE>> iterator = 
						new Iterator<OutgoingInternalTransition<LETTER, STATE>>() {
					/**
					 * Points to next field that has OutgoingInternalTransition.
					 */
					private short mPosition;
					{
						mPosition = 0;
						updatePosition();
					}
					
					private void updatePosition() {
						mPosition++;
						while (mPosition < 4) {
							if (mPosition == 1 && properOutgoingInternalTransitionAtPosition1(letter)) {
								return;
							} else if (mPosition == 2 && properOutgoingInternalTransitionAtPosition2(letter)) {
								return;
							} else if (mPosition == 3 && properOutgoingInternalTransitionAtPosition3(letter)) {
								return;
							} else {
								mPosition++;
							}
						}
					}

					@Override
					public boolean hasNext() {
						return mPosition < 4;
					}

					@Override
					public OutgoingInternalTransition<LETTER, STATE> next() {
						Object result;
						if (mPosition == 1) {
							result = mOut1;
						} else if (mPosition == 2) {
							result = mOut2;
						} else if (mPosition == 3) {
							result = mOut3;
						} else {
							throw new NoSuchElementException();
						}
						updatePosition();
						return (OutgoingInternalTransition<LETTER, STATE>) result;
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



	private boolean properIncomingInternalTransitionAtPosition1(final LETTER letter) {
		return (mIn1 instanceof IncomingInternalTransition) &&
				(letter == null || letter.equals(((IncomingInternalTransition<LETTER, STATE>) mIn1).getLetter()));
	}
	
	private boolean properIncomingInternalTransitionAtPosition2(final LETTER letter) {
		return (mIn2 instanceof IncomingInternalTransition) &&
				(letter == null || letter.equals(((IncomingInternalTransition<LETTER, STATE>) mIn2).getLetter()));
	}
	
	private boolean properIncomingInternalTransitionAtPosition3(final LETTER letter) {
		return (mIn3 instanceof IncomingInternalTransition) &&
				(letter == null || letter.equals(((IncomingInternalTransition<LETTER, STATE>) mIn3).getLetter()));
	}

	private Iterable<IncomingInternalTransition<LETTER, STATE>> internalPredecessorsField(final LETTER letter) {
		return new Iterable<IncomingInternalTransition<LETTER, STATE>>() {
			@Override
			public Iterator<IncomingInternalTransition<LETTER, STATE>> iterator() {
				final Iterator<IncomingInternalTransition<LETTER, STATE>> iterator = 
						new Iterator<IncomingInternalTransition<LETTER, STATE>>() {
					/**
					 * Points to next field that has IncomingInternalTransition.
					 */
					private short mPosition;
					{
						mPosition = 0;
						updatePosition();
					}
					
					private void updatePosition() {
						mPosition++;
						while (mPosition < 4) {
							if (mPosition == 1 && properIncomingInternalTransitionAtPosition1(letter)) {
								return;
							} else if (mPosition == 2 && properIncomingInternalTransitionAtPosition2(letter)) {
								return;
							} else if (mPosition == 3 && properIncomingInternalTransitionAtPosition3(letter)) {
								return;
							} else {
								mPosition++;
							}
						}
					}

					@Override
					public boolean hasNext() {
						return mPosition < 4;
					}

					@Override
					public IncomingInternalTransition<LETTER, STATE> next() {
						Object result;
						if (mPosition == 1) {
							result = mIn1;
						} else if (mPosition == 2) {
							result = mIn2;
						} else if (mPosition == 3) {
							result = mIn3;
						} else {
							throw new NoSuchElementException();
						}
						updatePosition();
						return (IncomingInternalTransition<LETTER, STATE>) result;
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
	
	private boolean properOutgoingCallTransitionAtPosition2(final LETTER letter) {
		return (mOut2 instanceof OutgoingCallTransition) &&
				(letter == null || letter.equals(((OutgoingCallTransition<LETTER, STATE>) mOut2).getLetter()));
	}
	
	private boolean properIncomingCallTransitionAtPosition2(final LETTER letter) {
		return (mIn2 instanceof IncomingCallTransition) &&
				(letter == null || letter.equals(((IncomingCallTransition<LETTER, STATE>) mIn2).getLetter()));
	}
	
	private boolean properOutgoingReturnTransitionAtPosition3(final STATE hier, final LETTER letter) {
		return (mOut3 instanceof OutgoingReturnTransition) &&
				(hier == null || hier.equals(((OutgoingReturnTransition<LETTER, STATE>) mOut3).getHierPred())) &&
				(letter == null || letter.equals(((OutgoingReturnTransition<LETTER, STATE>) mOut3).getLetter()));
	}
	
	private boolean properIncomingReturnTransitionAtPosition3(final STATE hier, final LETTER letter) {
		return (mIn3 instanceof IncomingReturnTransition) &&
				(hier == null || hier.equals(((IncomingReturnTransition<LETTER, STATE>) mIn3).getHierPred())) &&
				(letter == null || letter.equals(((IncomingReturnTransition<LETTER, STATE>) mIn3).getLetter()));
	}
	
	@Override
	public Iterable<OutgoingCallTransition<LETTER, STATE>> callSuccessors(
			final LETTER letter) {
		if (mapModeOutgoing()) {
			return callSuccessorsMap(letter);
		} else {
			final ArrayList<OutgoingCallTransition<LETTER, STATE>> result = 
					new ArrayList<OutgoingCallTransition<LETTER, STATE>>(1);
			if (properOutgoingCallTransitionAtPosition2(letter)) {
				result.add((OutgoingCallTransition<LETTER, STATE>) mOut2);
			}
			return result;
		}
	}


	@Override
	public Iterable<OutgoingCallTransition<LETTER, STATE>> callSuccessors() {
		if (mapModeOutgoing()) {
			return callSuccessorsMap();
		} else {
			final ArrayList<OutgoingCallTransition<LETTER, STATE>> result = 
					new ArrayList<OutgoingCallTransition<LETTER, STATE>>(1);
			if (properOutgoingCallTransitionAtPosition2(null)) {
				result.add((OutgoingCallTransition<LETTER, STATE>) mOut2);
			}
			return result;
		}
	}


	@Override
	public Iterable<IncomingCallTransition<LETTER, STATE>> callPredecessors(
			final LETTER letter) {
		if (mapModeIncoming()) {
			return callPredecessorsMap(letter);
		} else {
			final ArrayList<IncomingCallTransition<LETTER, STATE>> result = 
					new ArrayList<IncomingCallTransition<LETTER, STATE>>(1);
			if (properIncomingCallTransitionAtPosition2(letter)) {
				result.add((IncomingCallTransition<LETTER, STATE>) mIn2);
			}
			return result;
		}
	}


	@Override
	public Iterable<IncomingCallTransition<LETTER, STATE>> callPredecessors() {
		if (mapModeIncoming()) {
			return callPredecessorsMap();
		} else {
			final ArrayList<IncomingCallTransition<LETTER, STATE>> result = 
					new ArrayList<IncomingCallTransition<LETTER, STATE>>(1);
			if (properIncomingCallTransitionAtPosition2(null)) {
				result.add((IncomingCallTransition<LETTER, STATE>) mIn2);
			}
			return result;
		}
	}


	@Override
	public Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessors(
			final STATE hier, final LETTER letter) {
		if (mapModeOutgoing()) {
			return returnSuccessorsMap(hier,letter);
		} else {
			final ArrayList<OutgoingReturnTransition<LETTER, STATE>> result = 
					new ArrayList<OutgoingReturnTransition<LETTER, STATE>>(1);
			if (properOutgoingReturnTransitionAtPosition3(hier,letter)) {
				result.add((OutgoingReturnTransition<LETTER, STATE>) mOut3);
			}
			return result;
		}
	}


	@Override
	public Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessors(
			final LETTER letter) {
		if (mapModeOutgoing()) {
			return returnSuccessorsMap(letter);
		} else {
			final ArrayList<OutgoingReturnTransition<LETTER, STATE>> result = 
					new ArrayList<OutgoingReturnTransition<LETTER, STATE>>(1);
			if (properOutgoingReturnTransitionAtPosition3(null,letter)) {
				result.add((OutgoingReturnTransition<LETTER, STATE>) mOut3);
			}
			return result;
		}
	}


	@Override
	public Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessors() {
		if (mapModeOutgoing()) {
			return returnSuccessorsMap();
		} else {
			final ArrayList<OutgoingReturnTransition<LETTER, STATE>> result = 
					new ArrayList<OutgoingReturnTransition<LETTER, STATE>>(1);
			if (properOutgoingReturnTransitionAtPosition3(null,null)) {
				result.add((OutgoingReturnTransition<LETTER, STATE>) mOut3);
			}
			return result;
		}
	}


	@Override
	public Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessorsGivenHier(
			final STATE hier) {
		if (mapModeOutgoing()) {
			return returnSuccessorsGivenHierMap(hier);
		} else {
			final ArrayList<OutgoingReturnTransition<LETTER, STATE>> result = 
					new ArrayList<OutgoingReturnTransition<LETTER, STATE>>(1);
			if (properOutgoingReturnTransitionAtPosition3(hier,null)) {
				result.add((OutgoingReturnTransition<LETTER, STATE>) mOut3);
			}
			return result;
		}
	}


	@Override
	public Iterable<IncomingReturnTransition<LETTER, STATE>> returnPredecessors(
			final STATE hier, final LETTER letter) {
		if (mapModeIncoming()) {
			return returnPredecessorsMap(hier,letter);
		} else {
			final ArrayList<IncomingReturnTransition<LETTER, STATE>> result = 
					new ArrayList<IncomingReturnTransition<LETTER, STATE>>(1);
			if (properIncomingReturnTransitionAtPosition3(hier,letter)) {
				result.add((IncomingReturnTransition<LETTER, STATE>) mIn3);
			}
			return result;
		}
	}


	@Override
	public Iterable<IncomingReturnTransition<LETTER, STATE>> returnPredecessors(
			final LETTER letter) {
		if (mapModeIncoming()) {
			return returnPredecessorsMap(letter);
		} else {
			final ArrayList<IncomingReturnTransition<LETTER, STATE>> result = 
					new ArrayList<IncomingReturnTransition<LETTER, STATE>>(1);
			if (properIncomingReturnTransitionAtPosition3(null,letter)) {
				result.add((IncomingReturnTransition<LETTER, STATE>) mIn3);
			}
			return result;
		}
	}


	@Override
	public Iterable<IncomingReturnTransition<LETTER, STATE>> returnPredecessors() {
		if (mapModeIncoming()) {
			return returnPredecessorsMap();
		} else {
			final ArrayList<IncomingReturnTransition<LETTER, STATE>> result = 
					new ArrayList<IncomingReturnTransition<LETTER, STATE>>(1);
			if (properIncomingReturnTransitionAtPosition3(null,null)) {
				result.add((IncomingReturnTransition<LETTER, STATE>) mIn3);
			}
			return result;
		}
	}





	@Override
	public Iterable<OutgoingInternalTransition<LETTER, STATE>> internalSuccessors(
			final LETTER letter) {
		if(mapModeOutgoing()) {
			return internalSuccessorsMap(letter);
		} else {
			return internalSuccessorsField(letter);
		}
	}


	@Override
	public Iterable<OutgoingInternalTransition<LETTER, STATE>> internalSuccessors() {
		if(mapModeOutgoing()) {
			return internalSuccessorsMap();
		} else {
			return internalSuccessorsField(null);
		}
	}


	@Override
	public Iterable<IncomingInternalTransition<LETTER, STATE>> internalPredecessors(
			final LETTER letter) {
		if(mapModeIncoming()) {
			return internalPredecessorsMap(letter);
		} else {
			return internalPredecessorsField(letter);
		}
	}


	@Override
	public Iterable<IncomingInternalTransition<LETTER, STATE>> internalPredecessors() {
		if(mapModeIncoming()) {
			return internalPredecessorsMap();
		} else {
			return internalPredecessorsField(null);
		}
	}
}

