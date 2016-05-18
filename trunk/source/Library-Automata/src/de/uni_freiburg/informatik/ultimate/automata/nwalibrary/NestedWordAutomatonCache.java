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
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.AutomatonDefinitionPrinter;
import de.uni_freiburg.informatik.ultimate.automata.AutomatonDefinitionPrinter.Format;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingReturnTransition;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;



/**
 * 
 * 
 * @author heizmann@informatik.uni-freiburg.de
 *
 * @param <LETTER>
 * @param <STATE>
 */
public class NestedWordAutomatonCache<LETTER,STATE> implements INestedWordAutomatonSimple<LETTER,STATE> {
	
	private final AutomataLibraryServices m_Services;
	private final ILogger m_Logger;
	
	
	private Set<LETTER> m_InternalAlphabet;
	private Set<LETTER> m_CallAlphabet;
	private Set<LETTER> m_ReturnAlphabet;
	
	protected final StateFactory<STATE> m_StateFactory;
	
	/**
	 * Set of internal transitions PREs x LETTERs x SUCCs stored as map
	 * PREs -> LETTERs -> SUCCs
	 * The keySet of this map is used to store the set of states of this
	 * automaton.
	 */
	private Map<STATE,Map<LETTER,Set<STATE>>> m_InternalOut =
			new HashMap<STATE,Map<LETTER,Set<STATE>>>();
	
	/**
	 * Set of call transitions PREs x LETTERs x SUCCs stored as map
	 * PREs -> LETTERs -> SUCCs
	 */
	private Map<STATE,Map<LETTER,Set<STATE>>> m_CallOut =
			new HashMap<STATE,Map<LETTER,Set<STATE>>>();
	
	/**
	 * Set of return transitions LinPREs x HierPREs x LETTERs x SUCCs stored as 
	 * map LinPREs -> LETTERs -> HierPREs -> SUCCs
	 * 
	 */
	private Map<STATE,Map<LETTER,Map<STATE,Set<STATE>>>> m_ReturnOut =
			new HashMap<STATE,Map<LETTER,Map<STATE,Set<STATE>>>>();
	
	private Set<STATE> m_InitialStates = new HashSet<STATE>();
	private Set<STATE> m_FinalStates = new HashSet<STATE>();
	
	
	protected final STATE emptyStackState;
	

	
	
	
	
	
	
	
	@Override
	public Set<LETTER> getInternalAlphabet() {
		return m_InternalAlphabet;
	}	
	
	@Override
	public Set<LETTER> getCallAlphabet() {
		return m_CallAlphabet == null ? new HashSet<LETTER>(0) : m_CallAlphabet;
	}
	
	@Override
	public Set<LETTER> getReturnAlphabet() {
		return m_ReturnAlphabet == null ? new HashSet<LETTER>(0) : m_ReturnAlphabet;
	}
	
	private Collection<STATE> getStates() {
		return this.m_InternalOut.keySet();
	}
	
	@Override
	public STATE getEmptyStackState() {
		return this.emptyStackState;
	}

	@Override
	public StateFactory<STATE> getStateFactory() {
		return this.m_StateFactory;
	}
	
	public boolean contains(STATE state) {
		return m_InternalOut.containsKey(state);
	}
	
	
	@Override
	public int size() {
		return m_InternalOut.size();
	}


	@Override
	public Set<LETTER> getAlphabet() {
		return getInternalAlphabet();
	}

	@Override
	public Collection<STATE> getInitialStates() {
		return m_InitialStates;
	}


	@Override
	public boolean isInitial(STATE state) {
		assert contains(state);
		return m_InitialStates.contains(state);
	}

	@Override
	public boolean isFinal(STATE state) {
		assert contains(state);
		return m_FinalStates.contains(state);
	}

	public void addState(boolean isInitial, boolean isFinal, STATE state) {
		assert (state != null);
		if (m_InternalOut.containsKey(state)) {
			throw new IllegalArgumentException("State already exists");
		}
		m_InternalOut.put(state, null);
		
		if (isInitial) {
			m_InitialStates.add(state);
		}
		if (isFinal) {
			m_FinalStates.add(state);
		}
	}
	
	Set<LETTER> m_EmptySetOfLetters = 
			Collections.unmodifiableSet(new HashSet<LETTER>(0));
	Set<STATE> m_EmptySetOfStates = 
			Collections.unmodifiableSet(new HashSet<STATE>(0));


	
	
	@Override
	public Set<LETTER> lettersInternal(STATE state) {
		if (!contains(state)) {
			throw new IllegalArgumentException("State " + state + " unknown");
		}
		Map<LETTER, Set<STATE>> map = m_InternalOut.get(state);
		return map == null ? m_EmptySetOfLetters : map.keySet();
	}
	
	@Override
	public Set<LETTER> lettersCall(STATE state) {
		if (!contains(state)) {
			throw new IllegalArgumentException("State " + state + " unknown");
		}
		Map<LETTER, Set<STATE>> map = m_CallOut.get(state);
		return map == null ? m_EmptySetOfLetters : map.keySet();
	}
	
	@Override
	public Set<LETTER> lettersReturn(STATE state) {
		if (!contains(state)) {
			throw new IllegalArgumentException("State " + state + " unknown");
		}
		 Map<LETTER, Map<STATE, Set<STATE>>> map = m_ReturnOut.get(state);
		return map == null ? m_EmptySetOfLetters : map.keySet();
	}
	
	public Collection<STATE> hierPred(STATE state, LETTER letter) {
		assert contains(state);
		Map<LETTER, Map<STATE, Set<STATE>>> map = m_ReturnOut.get(state);
		if (map == null) {
			return m_EmptySetOfStates;
		}
		 Map<STATE, Set<STATE>> hier2succs = map.get(letter);
		return hier2succs == null ? m_EmptySetOfStates : hier2succs.keySet();
	}
	
	
	public Collection<STATE> succInternal(STATE state, LETTER letter) {
		assert contains(state);
		Map<LETTER, Set<STATE>> map = m_InternalOut.get(state);
		if (map == null) {
			return null;
		}
		Set<STATE> result = map.get(letter);
		return result;
	}
	
	public Collection<STATE> succCall(STATE state, LETTER letter) {
		assert contains(state);
		Map<LETTER, Set<STATE>> map = m_CallOut.get(state);
		if (map == null) {
			return null;
		}
		Set<STATE> result = map.get(letter);
		return result;
	}
	
	public Collection<STATE> succReturn(STATE state, STATE hier, LETTER letter) {
		assert contains(state);
		assert contains(hier);
		Map<LETTER, Map<STATE, Set<STATE>>> map = m_ReturnOut.get(state);
		if (map == null) {
			return null;
		}
		Map<STATE, Set<STATE>> hier2succs = map.get(letter);
		if (hier2succs == null) {
			return null;
		}
		Set<STATE> result = hier2succs.get(hier);
		return result;
	}
	
	@Override
	public Iterable<OutgoingInternalTransition<LETTER, STATE>> internalSuccessors(
			final STATE state, final LETTER letter) {
		return new Iterable<OutgoingInternalTransition<LETTER, STATE>>() {
			@Override
			public Iterator<OutgoingInternalTransition<LETTER, STATE>> iterator() {
				Iterator<OutgoingInternalTransition<LETTER, STATE>> iterator = 
						new Iterator<OutgoingInternalTransition<LETTER, STATE>>() {
					Iterator<STATE> m_Iterator;
					{
						Map<LETTER, Set<STATE>> letter2succ = m_InternalOut.get(state);
						if (letter2succ != null) {
							if (letter2succ.get(letter) != null) {
								m_Iterator = letter2succ.get(letter).iterator();
							} else {
								m_Iterator = null;
							}
						} else {
							m_Iterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return m_Iterator != null && m_Iterator.hasNext();
					}

					@Override
					public OutgoingInternalTransition<LETTER, STATE> next() {
						if (m_Iterator == null) {
							throw new NoSuchElementException();
						} else {
							STATE succ = m_Iterator.next(); 
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
	
	@Override
	public Iterable<OutgoingInternalTransition<LETTER, STATE>> internalSuccessors(
			final STATE state) {
		return new Iterable<OutgoingInternalTransition<LETTER, STATE>>() {
			/**
			 * Iterates over all OutgoingInternalTransition of state.
			 * Iterates over all outgoing internal letters and uses the 
			 * iterators returned by internalSuccessors(state, letter)
			 */
			@Override
			public Iterator<OutgoingInternalTransition<LETTER, STATE>> iterator() {
				Iterator<OutgoingInternalTransition<LETTER, STATE>> iterator = 
						new Iterator<OutgoingInternalTransition<LETTER, STATE>>() {
					Iterator<LETTER> m_LetterIterator;
					LETTER m_CurrentLetter;
					Iterator<OutgoingInternalTransition<LETTER, STATE>> m_CurrentIterator;
					{
						m_LetterIterator = lettersInternal(state).iterator();
						nextLetter();
					}

					private void nextLetter() {
						if (m_LetterIterator.hasNext()) {
							do {
								m_CurrentLetter = m_LetterIterator.next();
								m_CurrentIterator = internalSuccessors(state,
										m_CurrentLetter).iterator();
							} while (!m_CurrentIterator.hasNext()
									&& m_LetterIterator.hasNext());
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
					public OutgoingInternalTransition<LETTER, STATE> next() {
						if (m_CurrentLetter == null) {
							throw new NoSuchElementException();
						} else {
							OutgoingInternalTransition<LETTER, STATE> result = 
									m_CurrentIterator.next();
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
	public Iterable<OutgoingCallTransition<LETTER, STATE>> callSuccessors(
			final STATE state, final LETTER letter) {
		return new Iterable<OutgoingCallTransition<LETTER, STATE>>() {
			@Override
			public Iterator<OutgoingCallTransition<LETTER, STATE>> iterator() {
				Iterator<OutgoingCallTransition<LETTER, STATE>> iterator = 
						new Iterator<OutgoingCallTransition<LETTER, STATE>>() {
					Iterator<STATE> m_Iterator;
					{
						Map<LETTER, Set<STATE>> letter2succ = m_CallOut.get(state);
						if (letter2succ != null) {
							if (letter2succ.get(letter) != null) {
								m_Iterator = letter2succ.get(letter).iterator();
							} else {
								m_Iterator = null;
							}
						} else {
							m_Iterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return m_Iterator != null && m_Iterator.hasNext();
					}

					@Override
					public OutgoingCallTransition<LETTER, STATE> next() {
						if (m_Iterator == null) {
							throw new NoSuchElementException();
						} else {
							STATE succ = m_Iterator.next(); 
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
	
	
	@Override
	public Iterable<OutgoingCallTransition<LETTER, STATE>> callSuccessors(
			final STATE state) {
		return new Iterable<OutgoingCallTransition<LETTER, STATE>>() {
			/**
			 * Iterates over all OutgoingCallTransition of state.
			 * Iterates over all outgoing call letters and uses the 
			 * iterators returned by callSuccessors(state, letter)
			 */
			@Override
			public Iterator<OutgoingCallTransition<LETTER, STATE>> iterator() {
				Iterator<OutgoingCallTransition<LETTER, STATE>> iterator = 
						new Iterator<OutgoingCallTransition<LETTER, STATE>>() {
					Iterator<LETTER> m_LetterIterator;
					LETTER m_CurrentLetter;
					Iterator<OutgoingCallTransition<LETTER, STATE>> m_CurrentIterator;
					{
						m_LetterIterator = lettersCall(state).iterator();
						nextLetter();
					}

					private void nextLetter() {
						if (m_LetterIterator.hasNext()) {
							do {
								m_CurrentLetter = m_LetterIterator.next();
								m_CurrentIterator = callSuccessors(state,
										m_CurrentLetter).iterator();
							} while (!m_CurrentIterator.hasNext()
									&& m_LetterIterator.hasNext());
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
					public OutgoingCallTransition<LETTER, STATE> next() {
						if (m_CurrentLetter == null) {
							throw new NoSuchElementException();
						} else {
							OutgoingCallTransition<LETTER, STATE> result = 
									m_CurrentIterator.next();
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
	public Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSucccessors(
			final STATE state, final STATE hier, final LETTER letter) {
		return new Iterable<OutgoingReturnTransition<LETTER, STATE>>() {
			@Override
			public Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator() {
				Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator = 
						new Iterator<OutgoingReturnTransition<LETTER, STATE>>() {
					Iterator<STATE> m_Iterator;
					{
						Map<LETTER, Map<STATE, Set<STATE>>> letter2hier2succ = m_ReturnOut.get(state);
						if (letter2hier2succ != null) {
							Map<STATE, Set<STATE>> hier2succ = letter2hier2succ.get(letter);
							if (hier2succ != null) {
								if (hier2succ.get(hier) != null) {
									m_Iterator = hier2succ.get(hier).iterator();
								} else {
									m_Iterator = null;
								}
							} else {
								m_Iterator = null;
							}
						} else {
							m_Iterator = null;
						}
					}

					@Override
					public boolean hasNext() {
						return m_Iterator != null && m_Iterator.hasNext();
					}

					@Override
					public OutgoingReturnTransition<LETTER, STATE> next() {
						if (m_Iterator == null) {
							throw new NoSuchElementException();
						} else {
							STATE succ = m_Iterator.next(); 
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
	
	
//	@Override
//	public Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessors(
//			final STATE state, final LETTER letter) {
//		return new Iterable<OutgoingReturnTransition<LETTER, STATE>>() {
//			/**
//			 * Iterates over all OutgoingReturnTransition of state.
//			 * Iterates over all outgoing return letters and uses the 
//			 * iterators returned by returnSuccecessors(state, letter)
//			 */
//			@Override
//			public Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator() {
//				Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator = 
//						new Iterator<OutgoingReturnTransition<LETTER, STATE>>() {
//					Iterator<STATE> m_HierIterator;
//					STATE m_CurrentHier;
//					Iterator<OutgoingReturnTransition<LETTER, STATE>> m_CurrentIterator;
//					{
//						m_HierIterator = hierPred(state, letter).iterator();
//						nextHier();
//					}
//
//					private void nextHier() {
//						if (m_HierIterator.hasNext()) {
//							do {
//								m_CurrentHier = m_HierIterator.next();
//								m_CurrentIterator = returnSucccessors(
//										state, m_CurrentHier, letter).iterator();
//							} while (!m_CurrentIterator.hasNext()
//									&& m_HierIterator.hasNext());
//							if (!m_CurrentIterator.hasNext()) {
//								m_CurrentHier = null;
//								m_CurrentIterator = null;
//							}
//						} else {
//							m_CurrentHier = null;
//							m_CurrentIterator = null;
//						}
//					}
//
//					@Override
//					public boolean hasNext() {
//						return m_CurrentHier != null;
//					}
//
//					@Override
//					public OutgoingReturnTransition<LETTER, STATE> next() {
//						if (m_CurrentHier == null) {
//							throw new NoSuchElementException();
//						} else {
//							OutgoingReturnTransition<LETTER, STATE> result = 
//									m_CurrentIterator.next();
//							if (!m_CurrentIterator.hasNext()) {
//								nextHier();
//							}
//							return result;
//						}
//					}
//
//					@Override
//					public void remove() {
//						throw new UnsupportedOperationException();
//					}
//				};
//				return iterator;
//			}
//		};
//	}
	
	@Override
	public Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessorsGivenHier(
			final STATE state, final STATE hier) {
		return new Iterable<OutgoingReturnTransition<LETTER, STATE>>() {
			/**
			 * Iterates over all OutgoingReturnTransition of state with 
			 * hierarchical successor hier. 
			 * Iterates over all outgoing return letters and uses the 
			 * iterators returned by returnSuccecessors(state, hier, letter)
			 */
			@Override
			public Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator() {
				Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator = 
						new Iterator<OutgoingReturnTransition<LETTER, STATE>>() {
					Iterator<LETTER> m_LetterIterator;
					LETTER m_CurrentLetter;
					Iterator<OutgoingReturnTransition<LETTER, STATE>> m_CurrentIterator;
					{
						m_LetterIterator = lettersReturn(state).iterator();
						nextLetter();
					}

					private void nextLetter() {
						if (m_LetterIterator.hasNext()) {
							do {
								m_CurrentLetter = m_LetterIterator.next();
								m_CurrentIterator = returnSucccessors(
										state, hier, m_CurrentLetter).iterator();
							} while (!m_CurrentIterator.hasNext()
									&& m_LetterIterator.hasNext());
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
					public OutgoingReturnTransition<LETTER, STATE> next() {
						if (m_CurrentLetter == null) {
							throw new NoSuchElementException();
						} else {
							OutgoingReturnTransition<LETTER, STATE> result = 
									m_CurrentIterator.next();
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
	
	
//	@Override
//	public Iterable<OutgoingReturnTransition<LETTER, STATE>> returnSuccessors(
//			final STATE state) {
//		return new Iterable<OutgoingReturnTransition<LETTER, STATE>>() {
//			/**
//			 * Iterates over all OutgoingReturnTransition of state.
//			 * Iterates over all outgoing return letters and uses the 
//			 * iterators returned by returnSuccessors(state, letter)
//			 */
//			@Override
//			public Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator() {
//				Iterator<OutgoingReturnTransition<LETTER, STATE>> iterator = 
//						new Iterator<OutgoingReturnTransition<LETTER, STATE>>() {
//					Iterator<LETTER> m_LetterIterator;
//					LETTER m_CurrentLetter;
//					Iterator<OutgoingReturnTransition<LETTER, STATE>> m_CurrentIterator;
//					{
//						m_LetterIterator = lettersReturn(state).iterator();
//						nextLetter();
//					}
//
//					private void nextLetter() {
//						if (m_LetterIterator.hasNext()) {
//							do {
//								m_CurrentLetter = m_LetterIterator.next();
//								m_CurrentIterator = returnSuccessors(state,
//										m_CurrentLetter).iterator();
//							} while (!m_CurrentIterator.hasNext()
//									&& m_LetterIterator.hasNext());
//							if (!m_CurrentIterator.hasNext()) {
//								m_CurrentLetter = null;
//								m_CurrentIterator = null;
//							}
//						} else {
//							m_CurrentLetter = null;
//							m_CurrentIterator = null;
//						}
//					}
//
//					@Override
//					public boolean hasNext() {
//						return m_CurrentLetter != null;
//					}
//
//					@Override
//					public OutgoingReturnTransition<LETTER, STATE> next() {
//						if (m_CurrentLetter == null) {
//							throw new NoSuchElementException();
//						} else {
//							OutgoingReturnTransition<LETTER, STATE> result = 
//									m_CurrentIterator.next();
//							if (!m_CurrentIterator.hasNext()) {
//								nextLetter();
//							}
//							return result;
//						}
//					}
//
//					@Override
//					public void remove() {
//						throw new UnsupportedOperationException();
//					}
//				};
//				return iterator;
//			}
//		};
//	}
	
	
	
	
	
	
	public boolean containsInternalTransition(STATE state, LETTER letter, STATE succ) {
		assert contains(state);
		Map<LETTER, Set<STATE>> map = m_InternalOut.get(state);
		if (map == null) {
			return false;
		}
		Set<STATE> result = map.get(letter);
		return result == null ? false : result.contains(succ);
	}
	
	public boolean containsCallTransition(STATE state, LETTER letter, STATE succ) {
		assert contains(state);
		Map<LETTER, Set<STATE>> map = m_CallOut.get(state);
		if (map == null) {
			return false;
		}
		Set<STATE> result = map.get(letter);
		return result == null ? false : result.contains(succ);
	}
	
	public boolean containsReturnTransition(STATE state, STATE hier, LETTER letter, STATE succ) {
		assert contains(state);
		assert contains(hier);
		Map<LETTER, Map<STATE, Set<STATE>>> map = m_ReturnOut.get(state);
		if (map == null) {
			return false;
		}
		Map<STATE, Set<STATE>> hier2succs = map.get(letter);
		if (hier2succs == null) {
			return false;
		}
		Set<STATE> result = hier2succs.get(hier);
		return result == null ? false : result.contains(succ);
	}
	
	


	@Override
	public String sizeInformation() {
		boolean verbose = false;
		if (!verbose) {
			int states = m_InternalOut.size();
			return states + " states.";
		}
		int statesWithInternalSuccessors = 0;
		int internalSuccessors = 0;
		for (STATE pred : m_InternalOut.keySet()) {
			Map<LETTER, Set<STATE>> letter2succs = m_InternalOut.get(pred);
			if (letter2succs == null) {
				// may be null because the keySet is used to store the set of
				// all states, but some state my not have an outgoing internal
				// transition
				continue;
			}
			statesWithInternalSuccessors++;
			for (LETTER letter : letter2succs.keySet()) {
				Set<STATE> succs = letter2succs.get(letter);
				internalSuccessors += succs.size();
			}
		}
		int statesWithCallSuccessors = 0;
		int callSuccessors = 0;		
		for (STATE pred : m_CallOut.keySet()) {
			statesWithCallSuccessors++;
			Map<LETTER, Set<STATE>> letter2succs = m_CallOut.get(pred);
			for (LETTER letter : letter2succs.keySet()) {
				Set<STATE> succs = letter2succs.get(letter);
				callSuccessors += succs.size();
			}
		}
		int statesWithReturnSuccessor = 0;
		int returnSuccessors = 0;
		for (STATE pred : m_ReturnOut.keySet()) {
			statesWithReturnSuccessor++;
			Map<LETTER, Map<STATE, Set<STATE>>> letter2hier2succs = m_ReturnOut.get(pred);
			for (LETTER letter : letter2hier2succs.keySet()) {
				Map<STATE, Set<STATE>> hier2succs = letter2hier2succs.get(letter);
				for (STATE hier : hier2succs.keySet()) {
					Set<STATE> succs = hier2succs.get(hier);
					returnSuccessors += succs.size();
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append(" has ").append(m_InternalOut.size()).append(" states, " +
				statesWithInternalSuccessors).append( " states have internal successors, (").append(internalSuccessors).append("), ").append(
				
				statesWithCallSuccessors).append(" states have call successors, (").append(callSuccessors).append("), ").append(
				
				statesWithReturnSuccessor).append(" states have return successors, (").append(returnSuccessors).append("), ");
		return sb.toString();
		
	}

	
	public void addInternalTransition(STATE pred, LETTER letter, STATE succ) {
		if (!contains(pred)) {
			throw new IllegalArgumentException("State " + pred + " not in automaton");
		}
		assert contains(pred) : "State " + pred + " not in automaton";
		assert contains(succ) : "State " + succ + " not in automaton";
		assert getInternalAlphabet().contains(letter);
		Map<LETTER, Set<STATE>> letter2succs = m_InternalOut.get(pred);
		if (letter2succs == null) {
			letter2succs = new HashMap<LETTER, Set<STATE>>();
			m_InternalOut.put(pred, letter2succs);
		}
		Set<STATE> succs = letter2succs.get(letter);
		if (succs == null) {
			succs = new HashSet<STATE>();
			letter2succs.put(letter,succs);
		}
		succs.add(succ);
	}
	
	public void addInternalTransitions(STATE pred, LETTER letter, Collection<STATE> succs) {
		if (!contains(pred)) {
			throw new IllegalArgumentException("State " + pred + " not in automaton");
		}
		assert contains(pred) : "State " + pred + " not in automaton";
//		assert contains(succ) : "State " + succ + " not in automaton";
		assert getInternalAlphabet().contains(letter);
		Map<LETTER, Set<STATE>> letter2succs = m_InternalOut.get(pred);
		if (letter2succs == null) {
			letter2succs = new HashMap<LETTER, Set<STATE>>();
			m_InternalOut.put(pred, letter2succs);
		}
		Set<STATE> oldSuccs = letter2succs.get(letter);
		if (oldSuccs == null) {
			oldSuccs = new HashSet<STATE>();
			letter2succs.put(letter,oldSuccs);
		}
		oldSuccs.addAll(succs);
	}
	

	public void addCallTransition(STATE pred, LETTER letter, STATE succ) {
		assert contains(pred) : "State " + pred + " not in automaton";
		assert contains(succ) : "State " + succ + " not in automaton";
		assert getCallAlphabet().contains(letter);
		Map<LETTER, Set<STATE>> letter2succs = m_CallOut.get(pred);
		if (letter2succs == null) {
			letter2succs = new HashMap<LETTER, Set<STATE>>();
			m_CallOut.put(pred, letter2succs);
		}
		Set<STATE> succs = letter2succs.get(letter);
		if (succs == null) {
			succs = new HashSet<STATE>();
			letter2succs.put(letter,succs);
		}
		succs.add(succ);
	}
	
	public void addCallTransitions(STATE pred, LETTER letter, Collection<STATE> succs) {
		assert contains(pred) : "State " + pred + " not in automaton";
//		assert contains(succ) : "State " + succ + " not in automaton";
		assert getCallAlphabet().contains(letter);
		Map<LETTER, Set<STATE>> letter2succs = m_CallOut.get(pred);
		if (letter2succs == null) {
			letter2succs = new HashMap<LETTER, Set<STATE>>();
			m_CallOut.put(pred, letter2succs);
		}
		Set<STATE> oldSuccs = letter2succs.get(letter);
		if (oldSuccs == null) {
			oldSuccs = new HashSet<STATE>();
			letter2succs.put(letter,oldSuccs);
		}
		oldSuccs.addAll(succs);
	}
	

	public void addReturnTransition(STATE pred, STATE hier, LETTER letter, STATE succ) {
		assert contains(pred) : "State " + pred + " not in automaton";
		assert contains(succ) : "State " + succ + " not in automaton";
		assert contains(hier) : "State " + hier + " not in automaton";
		assert getReturnAlphabet().contains(letter);
		Map<LETTER, Map<STATE, Set<STATE>>> letter2hier2succs = m_ReturnOut.get(pred);
		if (letter2hier2succs == null) {
			letter2hier2succs = new HashMap<LETTER, Map<STATE, Set<STATE>>>();
			m_ReturnOut.put(pred, letter2hier2succs);
		}
		Map<STATE, Set<STATE>> hier2succs = letter2hier2succs.get(letter);
		if (hier2succs == null) {
			hier2succs = new HashMap<STATE, Set<STATE>>();
			letter2hier2succs.put(letter, hier2succs);
		}
		Set<STATE> succs = hier2succs.get(hier);
		if (succs == null) {
			succs = new HashSet<STATE>();
			hier2succs.put(hier, succs);
		}
		succs.add(succ);
	}
	
	public void addReturnTransitions(STATE pred, STATE hier, LETTER letter, Collection<STATE> succs) {
		assert contains(pred) : "State " + pred + " not in automaton";
//		assert contains(succ) : "State " + succ + " not in automaton";
		assert contains(hier) : "State " + hier + " not in automaton";
		assert getReturnAlphabet().contains(letter);
		Map<LETTER, Map<STATE, Set<STATE>>> letter2hier2succs = m_ReturnOut.get(pred);
		if (letter2hier2succs == null) {
			letter2hier2succs = new HashMap<LETTER, Map<STATE, Set<STATE>>>();
			m_ReturnOut.put(pred, letter2hier2succs);
		}
		Map<STATE, Set<STATE>> hier2succs = letter2hier2succs.get(letter);
		if (hier2succs == null) {
			hier2succs = new HashMap<STATE, Set<STATE>>();
			letter2hier2succs.put(letter, hier2succs);
		}
		Set<STATE> oldSuccs = hier2succs.get(hier);
		if (oldSuccs == null) {
			oldSuccs = new HashSet<STATE>();
			hier2succs.put(hier, oldSuccs);
		}
		oldSuccs.addAll(succs);
	}
	
	
	
	
	
	

	

	
	public NestedWordAutomatonCache(AutomataLibraryServices services,
			Set<LETTER> internalAlphabet,
				Set<LETTER> callAlphabet,
				Set<LETTER> returnAlphabet,
			   StateFactory<STATE> stateFactory) {
		m_Services = services;
		m_Logger = m_Services.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		if (internalAlphabet == null) {
			throw new IllegalArgumentException("nwa must have internal alphabet");
		}
		if (stateFactory == null) {
			throw new IllegalArgumentException("nwa must have stateFactory");
		}
		this.m_InternalAlphabet = internalAlphabet;
		this.m_CallAlphabet = callAlphabet;
		this.m_ReturnAlphabet = returnAlphabet;
		this.m_StateFactory = stateFactory;
		this.emptyStackState = m_StateFactory.createEmptyStackState();
	}
	
	
	/**
	 * Return true iff this automaton is deterministic.
	 */
	public boolean isDeterministic() {
		if(getInitialStates().size() > 1) {
			return false;
		}
		for (STATE state : this.getStates()) {
			for (LETTER symbol : lettersInternal(state)) {
				if (succInternal(state, symbol).size() > 1) {
					return false;
				}
			}
			for (LETTER symbol : lettersCall(state)) {
				if (succCall(state, symbol).size() > 1) {
					return false;
				}
			}
			for (LETTER symbol : lettersReturn(state)) {
				for (STATE hier : hierPred(state, symbol)) {
					if (succReturn(state, hier, symbol).size() > 1) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Return true iff this automaton is total.
	 */
	public boolean isTotal() {
		if(getInitialStates().size() < 1) {
			return false;
		}
		for (STATE state : this.getStates()) {
			for (LETTER symbol : getInternalAlphabet()) {
				if (succInternal(state, symbol).size() < 1) {
					return false;
				}
			}
			for (LETTER symbol : getCallAlphabet()) {
				if (succCall(state, symbol).size() < 1) {
					return false;
				}
			}
			for (LETTER symbol : getReturnAlphabet()) {
				for (STATE hier : getStates()) {
					if (succReturn(state, hier, symbol).size() < 1) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	
	
	
	
	
	public int numberOfOutgoingInternalTransitions(STATE state) {
		int result = 0;
		for (LETTER letter : lettersInternal(state)) {
			for (STATE succ : succInternal(state, letter)) {
				result++;
			}
		}
		return result;
	}
	
	public static <LETTER, STATE> boolean sameAlphabet(
			INestedWordAutomatonSimple<LETTER, STATE> nwa1, 
			INestedWordAutomatonSimple<LETTER, STATE> nwa2) {
		boolean result = true;
		Collection<LETTER> in1 = nwa1.getInternalAlphabet();
		Collection<LETTER> in2 = nwa2.getInternalAlphabet();
		result &= in1.equals(in2);
		result &= nwa1.getInternalAlphabet().equals(nwa2.getInternalAlphabet());
		result &= nwa1.getCallAlphabet().equals(nwa2.getCallAlphabet());
		result &= nwa1.getReturnAlphabet().equals(nwa2.getReturnAlphabet());
		return result;
	}
	
	
	@Override
	public String toString() {
		return (new AutomatonDefinitionPrinter<String,String>(m_Services, "nwa", Format.ATS, this)).getDefinitionAsString();
	}





	
}
