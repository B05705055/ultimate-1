/*
 * Copyright (C) 2014-2015 Daniel Tischner
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

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StringFactory;

/**
 * Utility class that provides a method
 * {@link #generatePackedRandomDFA(int, int, int, boolean, boolean)
 * generatePackedRandomDFA(...)} to generate uniform or non-uniform distributed
 * random connected or not connected total or not-total DFAs (Deterministic
 * finite automaton) in a specific packed int[] array format. This format can be
 * unpacked by {@link #extractPackedDFA(int[], int, int, int, Random)
 * extractPackedDFA(...)}<br/>
 * <br/>
 * Runtime is in:<br/>
 * <b>O(n^3 * k) * O(random)</b> if result should be uniform and caching is not
 * enabled<br/>
 * <b>O(n * k) * O(random)</b> if result should not be uniform<br/>
 * <b>O(n^2 * k) * O(random)</b> if result should be uniform, caching is enabled
 * and there is a valid cache (n must be equals)<br/>
 * where 'n' is the amount of nodes, 'k' the size of the alphabet and 'random'
 * methods of {@link java.util.Random}.
 * 
 * @author Daniel Tischner
 *
 */
public final class GetRandomDfa implements IOperation<String, String> {
	
	/**
	 * Constant for no valid state. Valid states are 0, 1, ...
	 */
	public static final int NO_STATE = -1;
	/**
	 * Constant for full percentage of 100.
	 */
	public static final int PERC_FULL = 100;
	/**
	 * Lower bound for percentage of totality.
	 */
	public static final int PERC_TOTALITY_BOUND_LOWER = 0;
	/**
	 * Upper bound for percentage of totality.
	 */
	public static final int PERC_TOTALITY_BOUND_UPPER = PERC_FULL;
	/**
	 * Table that contains the amount of all permutations of different DFA
	 * classes. Dimensions are [size][size * alphabetSize]. Also used for
	 * caching purpose.
	 */
	private static BigInteger[][] permutationsTable;
	/**
	 * Prefix for nodes. A node then is called 'prefix + index' where index is
	 * the index to a node in the node list.
	 */
	public static final String PREFIX_NODE = "q";
	/**
	 * Prefix for transitions. A transition then is called 'prefix + index'
	 * where index is the index to a transition in the transition list.
	 */
	public static final String PREFIX_TRANSITION = "a";

	/**
	 * Generates a uniform distributed random {@link BigInteger} between 0
	 * (inclusive) and 'upperBound' (exclusive) using a given random generator.<br/><br/>
	 * Runtime is in:<br/>
	 * <b>O(2 * random)</b><br/>
	 * where 'random' are methods of {@link java.util.Random}.
	 * 
	 * @param upperBound
	 *            Upper bound for the generated number (exclusive)
	 * @param rnd
	 *            Random generator
	 * @return Uniform distributed random {@link BigInteger} between 0
	 *         (inclusive) and 'upperBound' (exclusive)
	 */
	private static BigInteger nextRandomBigInteger(final BigInteger upperBound,
			final Random rnd) {
		BigInteger result = new BigInteger(upperBound.bitLength(), rnd);

		// Converges to one iteration because chance decreases by 0.5 every
		// step.
		while (result.compareTo(upperBound) >= 0) {
			result = new BigInteger(upperBound.bitLength(), rnd);
		}
		return result;
	}
	/**
	 * Size of the alphabet.
	 */
	private final int malphabetSize;
	/**
	 * If true enables caching of pre-calculated results for future similar
	 * requests. If false caching will not be done. Best results can be achieved
	 * by executing requests with same 'size' and similar 'alphabetSize' behind
	 * one another.
	 */
	private final boolean menableCaching;
	/**
	 * If true it is ensured that the DFA
	 * is connected meaning all states are reached.
	 * If false and if {@link mpercOfTotality} is small
	 * it may happen that the automata is not connected.
	 */
	private final boolean mensureIsConnected;
	/**
	 * If true ensures a uniform distribution of the connected DFAs at high cost
	 * of performance for big 'size'. If false random classes of DFAs get
	 * favored over other random classes but the generation is very fast.
	 */
	private final boolean mensureIsUniform;
	/**
	 * If true ensures that all states reach a final state at cost of
	 * performance by creating extra final states. If false just
	 * {@link mnumOfAccStates} final states will be created.
	 */
	private final boolean mensureStatesReachFinal;
	/**
	 * Flags of the DFA specified by {@link #generateFlag(int, int) generateFlag(...)}.
	 */
	private final Set<Integer> mflags;
	/**
	 * Number of the accepting states.
	 */
	private final int mnumOfAccStates;
	/**
	 * Percentage of DFAs totality. If 100 the resulting DFA will be total.
	 * If 50 about half of the transitions will miss.
	 * If 0 all transitions that can be deleted,
	 * by ensuring all states get reached, are missing.
	 */
	private final int mpercOfTotality;
	/**
	 * Random generator.
	 */
	private final Random mrandom;
	/**
	 * Resulting automaton of generator.
	 */
	private final INestedWordAutomaton<String, String> mresult;
	/**
	 * Service provider.
	 */
	private final AutomataLibraryServices mServices;

	/**
	 * Size of the automaton also amount of nodes.
	 */
	private final int msize;

	/**
	 * Generates a uniform distributed random connected total DFA with a
	 * given amount of nodes, size of alphabet and
	 * number of accepting states. It is not ensured that all states reach
	 * accepting states.<br />
	 * <br />
	 * Additionally with following flags:<br />
	 * int <b>percOfTotality</b> : <b>{@link PERC_TOTALITY_BOUND_UPPER}</b>
	 * Ensures that the DFA is total.<br />
	 * boolean <b>ensureIsConnected</b> : <b>true</b> Ensures that all states
	 * are reached.<br />
	 * boolean <b>ensureStatesReachFinal</b> : <b>false</b> It is not ensured
	 * that all states reach a final state.<br />
	 * boolean <b>ensureIsUniform</b> : <b>true</b> Ensures a uniform
	 * distribution of the DFAs at high cost of performance for big 'size'.<br/>
	 * boolean <b>enableCaching</b> : <b>true</b> Enables caching of
	 * pre-calculated results for future similar requests.<br/>
	 * Best results can be achieved by executing requests with same 'size' and
	 * similar 'alphabetSize' behind one another.<br/>
	 * <br/>
	 * Runtime is in:<br/>
	 * <b>O(n^2 * k) * O(random)</b> if there is a valid cache (n must be
	 * equals)<br/>
	 * <b>O(n^3 * k) * O(random)</b> if there is no valid cache<br/>
	 * where 'n' is the amount of nodes, 'k' the size of the alphabet and
	 * 'random' methods of {@link java.util.Random}.
	 * 
	 * @param services
	 *            Service provider
	 * @param size
	 *            Amount of nodes
	 * @param alphabetSize
	 *            Size of the alphabet
	 * @param numOfAccStates
	 *            Number of accepting states
	 * @return Uniform distributed random total DFA
	 */
	public GetRandomDfa(final AutomataLibraryServices services,
			final int size, final int alphabetSize,
			final int numOfAccStates) {
		this(services, size, alphabetSize, numOfAccStates,
				PERC_TOTALITY_BOUND_UPPER, true, false, true, true);
	}

	/**
	 * Generates a uniform distributed random connected or not-connected total
	 * or not-total DFA with a given amount of nodes, size of alphabet and
	 * number of accepting states. It is not ensured that all states reach
	 * accepting states.<br />
	 * <br />
	 * Additionally with following flags:<br />
	 * boolean <b>ensureStatesReachFinal</b> : <b>false</b> It is not ensured
	 * that all states reach a final state.<br />
	 * boolean <b>ensureIsUniform</b> : <b>true</b> Ensures a uniform
	 * distribution of the DFAs at high cost of performance for big 'size'.<br/>
	 * boolean <b>enableCaching</b> : <b>true</b> Enables caching of
	 * pre-calculated results for future similar requests.<br/>
	 * Best results can be achieved by executing requests with same 'size' and
	 * similar 'alphabetSize' behind one another.<br/>
	 * <br/>
	 * Runtime is in:<br/>
	 * <b>O(n^2 * k) * O(random)</b> if there is a valid cache (n must be
	 * equals)<br/>
	 * <b>O(n^3 * k) * O(random)</b> if there is no valid cache<br/>
	 * where 'n' is the amount of nodes, 'k' the size of the alphabet and
	 * 'random' methods of {@link java.util.Random}.
	 * 
	 * @param services
	 *            Service provider
	 * @param size
	 *            Amount of nodes
	 * @param alphabetSize
	 *            Size of the alphabet
	 * @param numOfAccStates
	 *            Number of accepting states
	 * @param percOfTotality
	 *            Percentage of DFAs totality. If 1.0 the resulting DFA will be total.
	 *            If 0.5 about half of the transitions will miss.
	 *            If 0.0 all transitions that can be deleted,
	 *            by ensuring all states get reached, are missing.
	 * @param ensureIsConnected
	 *            If true it is ensured that the DFA
	 *            is connected meaning all states are reached.
	 *            If false and if {@link mpercOfTotality} is small
	 *            it may happen that the automata is not connected.
	 * @return Uniform distributed random total DFA
	 */
	public GetRandomDfa(final AutomataLibraryServices services,
			final int size, final int alphabetSize, final int numOfAccStates,
			final int percOfTotality, final boolean ensureIsConnected) {
		this(services, size, alphabetSize, numOfAccStates, percOfTotality,
				ensureIsConnected, false, true, true);
	}

	/**
	 * Generates a uniform or non-uniform distributed random connected or
	 * not-connected total or not-total DFA with a given amount of nodes, size
	 * of alphabet and number of accepting states.<br />
	 * <br />
	 * Additionally with following flags:<br />
	 * boolean <b>enableCaching</b> : <b>true</b> Enables caching of
	 * pre-calculated results for future similar requests.<br/>
	 * Best results can be achieved by executing requests with same 'size' and
	 * similar 'alphabetSize' behind one another.<br/>
	 * <br/>
	 * Runtime is in:<br/>
	 * <b>O(n^2 * k) * O(random)</b> if result should be uniform and there is a
	 * valid cache (n must be equals)<br/>
	 * <b>O(n^3 * k) * O(random)</b> if result should be uniform and there is no
	 * valid cache<br/>
	 * <b>O(n * k) * O(random)</b> if result should not be uniform<br/>
	 * where 'n' is the amount of nodes, 'k' the size of the alphabet and
	 * 'random' methods of {@link java.util.Random}.
	 * 
	 * @param services
	 *            Service provider
	 * @param size
	 *            Amount of nodes
	 * @param alphabetSize
	 *            Size of the alphabet
	 * @param numOfAccStates
	 *            Number of accepting states which may be just a bottom bound if
	 *            'ensureStatesReachFinal' is true
	 * @param percOfTotality
	 *            Percentage of DFAs totality. If 1.0 the resulting DFA will be total.
	 *            If 0.5 about half of the transitions will miss.
	 *            If 0.0 all transitions that can be deleted,
	 *            by ensuring all states get reached, are missing.
	 * @param ensureIsConnected
	 *            If true it is ensured that the DFA
	 *            is connected meaning all states are reached.
	 *            If false and if {@link mpercOfTotality} is small
	 *            it may happen that the automata is not connected.
	 * @param ensureStatesReachFinal
	 *            If true ensures that all states reach a final state at cost of
	 *            performance by creating extra final states. If false just
	 *            {@link numOfAccStates} final states will be created.
	 * @param ensureIsUniform
	 *            If true ensures a uniform distribution of the connected DFAs
	 *            at high cost of performance for big 'size'. If false random
	 *            classes of DFAs get favored over other random classes but the
	 *            generation is very fast.
	 * @return Uniform or non-uniform distributed random DFA
	 */
	public GetRandomDfa(final AutomataLibraryServices services,
			final int size, final int alphabetSize, final int numOfAccStates,
			final int percOfTotality, final boolean ensureIsConnected,
			final boolean ensureStatesReachFinal,
			final boolean ensureIsUniform) {
		this(services, size, alphabetSize, numOfAccStates, percOfTotality,
				ensureIsConnected, ensureStatesReachFinal,
				ensureIsUniform, true);
	}
	
	/**
	 * Generates a uniform or non-uniform distributed random connected or
	 * not-connected total or not-total DFA with a given amount of nodes and
	 * size of alphabet.<br/>
	 * <br/>
	 * Runtime is in:<br/>
	 * <b>O(n^3 * k) * O(random)</b> if result should be uniform and caching is
	 * not enabled<br/>
	 * <b>O(n * k) * O(random)</b> if result should not be uniform<br/>
	 * <b>O(n^2 * k) * O(random)</b> if result should be uniform, caching is
	 * enabled and there is a valid cache (n must be equals)<br/>
	 * where 'n' is the amount of nodes, 'k' the size of the alphabet and
	 * 'random' methods of {@link java.util.Random}.
	 * 
	 * @param services
	 *            Service provider
	 * @param size
	 *            Amount of nodes
	 * @param alphabetSize
	 *            Size of the alphabet
	 * @param numOfAccStates
	 *            Number of accepting states which may be just a bottom bound if
	 *            'ensureStatesReachFinal' is true
	 * @param percOfTotality
	 *            Percentage of DFAs totality. If 1.0 the resulting DFA will be total.
	 *            If 0.5 about half of the transitions will miss.
	 *            If 0.0 all transitions that can be deleted,
	 *            by ensuring all states get reached, are missing.
	 * @param ensureIsConnected
	 *            If true it is ensured that the DFA
	 *            is connected meaning all states are reached.
	 *            If false and if {@link mpercOfTotality} is small
	 *            it may happen that the automata is not connected.
	 * @param ensureStatesReachFinal
	 *            If true ensures that all states reach a final state at cost of
	 *            performance by creating extra final states. If false just
	 *            {@link numOfAccStates} final states will be created.
	 * @param ensureIsUniform
	 *            If true ensures a uniform distribution of the connected DFAs
	 *            at high cost of performance for big 'size'. If false random
	 *            classes of DFAs get favored over other random classes but the
	 *            generation is very fast.
	 * @param enableCaching
	 *            If true enables caching of pre-calculated results for future
	 *            similar requests. If false caching will not be done. Best
	 *            results can be achieved by executing requests with same 'size'
	 *            and similar 'alphabetSize' behind one another.
	 * @return Uniform or non-uniform distributed random DFA
	 */
	public GetRandomDfa(final AutomataLibraryServices services,
			final int size, final int alphabetSize, final int numOfAccStates,
			final int percOfTotality, final boolean ensureIsConnected,
			final boolean ensureStatesReachFinal,
			final boolean ensureIsUniform, final boolean enableCaching) {
		mServices = services;
		msize = size;
		malphabetSize = alphabetSize;
		mnumOfAccStates = numOfAccStates;
		mpercOfTotality = percOfTotality;
		mensureIsConnected = ensureIsConnected;
		mensureStatesReachFinal = ensureStatesReachFinal;
		mensureIsUniform = ensureIsUniform;
		menableCaching = enableCaching;
		mflags = new HashSet<Integer>(msize - 1);

		mrandom = new Random();
		final int[] dfa = generatePackedRandomDFA();
		final Set<Integer> transToDelete = calcTransitionsToDelete(dfa);
		final Set<Integer> accStates = calcAccStates(dfa, transToDelete);
		mresult = extractPackedDFA(dfa, accStates, transToDelete);
	}

	/**
	 * Calculates which states of a DFA should be accepting using internal
	 * properties of the object. Can also ensure that all states reach a
	 * accepting state by creating extra accepting states which costs
	 * performance.<br />
	 * Runtime is in:<br />
	 * <b>O(numOfAccStates)</b> if not every state needs to reach a accepting<br />
	 * <b>O(size * alphabetSize)</b> if it needs to be ensured that every state
	 * reaches a accepting
	 * 
	 * @param dfa
	 *            The DFA to calculate accepting states for in the int[] array
	 *            format specified by
	 *            {@link #generatePackedRandomDFA(int, int, int, boolean, boolean)
	 *            generatePackedRandomDFA(...)}
	 * @param transToDelete
	 *            Set that contains indexes in the DFA sequence of all transitions
	 *            that should be deleted
	 * @return Set of states that should be accepting
	 */
	private Set<Integer> calcAccStates(final int[] dfa, final Set<Integer> transToDelete) {
		final LinkedHashSet<Integer> finalStates = new LinkedHashSet<Integer>(
				mnumOfAccStates);
		// Initialize list
		final List<Integer> shuffledStateList = new ArrayList<Integer>(msize);
		for (int i = 0; i < msize; i++) {
			shuffledStateList.add(i);
		}
		// Add basic final states
		Collections.shuffle(shuffledStateList, mrandom);
		for (int i = 0; i < mnumOfAccStates; i++) {
			finalStates.add(shuffledStateList.get(i));
		}

		if (!mensureStatesReachFinal) {
			return finalStates;
		}
		// If it should be ensured that all states reach a final state ensure
		// that
		// Create a representation of the DFA where every state knows by which
		// it is reached.
		final List<Set<Integer>> statesReachedBy = new ArrayList<Set<Integer>>(msize);
		for (int i = 0; i < msize; i++) {
			statesReachedBy.add(new HashSet<Integer>(malphabetSize));
		}
		for (int i = 0; i < msize; i++) {
			final int offset = i * malphabetSize;
			// Resulting states are reached by state i
			for (int j = 0; j < malphabetSize; j++) {
				//Skip transition if it should not be contained in the final automata
				if (transToDelete.contains(offset + j)) {
					continue;
				}
				final int resultingState = dfa[offset + j];
				statesReachedBy.get(resultingState).add(i);
			}
		}

		// Initialize set that will contain remaining states that do not reach a
		// final state
		final LinkedHashSet<Integer> remainingStates = new LinkedHashSet<Integer>(msize);
		/*
		 * Christian: Detected a bug: This value is not necessarily the size of
		 * the data structure and caused problems (could become negative).
		 * Let us hope this fixed it - need to double-check with Daniel.
		 */
//		int remainingStatesAmount = msize;
		for (int i = 0; i < msize; i++) {
			remainingStates.add(i);
		}
		// Search all states that do not reach final states
		// Make one of them final and repeat until all states reach final states
		do {
			// Search all states that reach final states and remove them from
			// 'remainingStates'
			for (final int finalState : finalStates) {
				if (!remainingStates.contains(finalState)) {
					continue;
				}
				final Queue<Integer> statesToProcess = new LinkedList<Integer>();
				statesToProcess.add(finalState);
				while (!statesToProcess.isEmpty()) {
					final int currState = statesToProcess.poll();
					remainingStates.remove(currState);
//					remainingStatesAmount--;
					final Set<Integer> currStateReachedBy = statesReachedBy
							.get(currState);
					for (final int state : currStateReachedBy) {
						if (remainingStates.contains(state)) {
							statesToProcess.add(state);
						}
					}
				}
			}
			// Make one of the remaining states final and repeat until all
			// states reach final states
			final Iterator<Integer> iterator = remainingStates.iterator();
			if (remainingStates.size() > 0) {
//			if (remainingStatesAmount > 0) {
				int remainingState = NO_STATE;
				int counter = mrandom.nextInt(remainingStates.size());
//				int counter = mrandom.nextInt(remainingStatesAmount);
				while (counter >= 0) {
					remainingState = iterator.next();
					counter--;
				}
				if (remainingState != NO_STATE) {
					finalStates.add(remainingState);
				}
			}
		} while (!remainingStates.isEmpty());

		return finalStates;
	}

	/**
	 * Calculates the transitions of the DFA that should be deleted.
	 * Therefore using {@link mpercOfTotality}.
	 * Transitions that first reach a state, stated by
	 * {@link mflags} do not get deleted hence the DFA keeps connected.<br />
	 * <br />
	 * 
	 * Runtime is in <b>O(size * alphabetSize)</b>
	 * @param dfa
	 *            The DFA to calculate accepting states for in the int[] array
	 *            format specified by
	 *            {@link #generatePackedRandomDFA(int, int, int, boolean, boolean)
	 *            generatePackedRandomDFA(...)}
	 * @return List of transitions that should get deleted as indexes in the DFA sequence
	 */
	private Set<Integer> calcTransitionsToDelete(final int[] dfa) {
		if (mpercOfTotality < PERC_TOTALITY_BOUND_LOWER || mpercOfTotality > PERC_TOTALITY_BOUND_UPPER) {
			throw new IllegalArgumentException(
					"'percOfTotality' must not exceed '" + PERC_TOTALITY_BOUND_UPPER + "' or be less than "
					+ PERC_TOTALITY_BOUND_LOWER + ".");
		}
		
		//Skip calculation in default case where no transition should be deleted
		if (mpercOfTotality == PERC_TOTALITY_BOUND_UPPER) {
			return new HashSet<Integer>(0);
		}
		
		final int amountOfTrans = dfa.length;
		int maxAllowedToDelete;
		if (mensureIsConnected) {
			//Ensure flag edges are not deleted
			final float percOfFlags = (mflags.size() + 0.0f) / amountOfTrans;
			maxAllowedToDelete = Math.round((((PERC_FULL + 0.0f) / PERC_FULL) - percOfFlags)
					* amountOfTrans);
		} else {
			//All edges are allowed to delete
			maxAllowedToDelete = amountOfTrans;
		}
		
		final int desiredToDelete = Math.round(((PERC_FULL - mpercOfTotality + 0.0f) / PERC_FULL)
				* amountOfTrans);
		final int amountToDelete = Math.min(maxAllowedToDelete, desiredToDelete);
		
		Set<Integer> transToDelete = new HashSet<Integer>(amountToDelete);
		
		final int variantThreshold = dfa.length / 2;
		final int generationVariantMaxTries = (int) (dfa.length * 2f);
		boolean useShuffleVariant = amountToDelete > variantThreshold;
		
		//Variant 1: Generate random indexes until we have enough unique
		if (!useShuffleVariant) {
			int counter = 0;
			while(!useShuffleVariant && transToDelete.size() < amountToDelete) {
				final int transition = mrandom.nextInt(dfa.length);
				if (mensureIsConnected) {
					//Don't add flag edges for deletion
					if (!mflags.contains(transition)) {
						transToDelete.add(transition);
					}
				} else {
					transToDelete.add(transition);
				}
				//Break variant and use other if it takes too long
				counter++;
				if (counter > generationVariantMaxTries) {
					useShuffleVariant = true;
					transToDelete = new HashSet<Integer>(amountToDelete);
				}
			}
		}
		//Variant 2: Permute a list of all indexes and select the first valid ones
		if (useShuffleVariant) {
			final List<Integer> transitions = new ArrayList<Integer>(dfa.length - mflags.size());
			for (int i = 0; i < dfa.length; i++) {
				if (mensureIsConnected) {
					//Don't add flag edges for deletion
					if (!mflags.contains(i)) {
						transitions.add(i);
					}
				} else {
					transitions.add(i);
				}
			}
			Collections.shuffle(transitions, mrandom);
			for (int i = 0; i < amountToDelete; i++) {
				transToDelete.add(transitions.get(i));
			}
		}
		return transToDelete;
	}
	
	@Override
	public boolean checkResult(final StateFactory<String> stateFactory)
			throws AutomataLibraryException {
		return true;
	}

	@Override
	public String exitMessage() {
		return "Finished " + operationName() + " Result "
				+ mresult.sizeInformation() + ".";
	}

	/**
	 * Extracts a DFA that is packed into the int[] array format specified by
	 * {@link #generatePackedRandomDFA(int, int, int, boolean, boolean)
	 * generatePackedRandomDFA(...)} and returns it as
	 * {@link INestedWordAutomaton}.<br />
	 * Runtime is in <b>O(size * alphabetSize)</b>.
	 * 
	 * @param dfa
	 *            The DFA to extract in the int[] array format specified by
	 *            {@link #generatePackedRandomDFA(int, int, int, boolean, boolean)
	 *            generatePackedRandomDFA(...)}
	 * @param accStates
	 *            Set that contains all accepting states
	 * @param transToDelete
	 *            Set that contains indexes in the DFA sequence of all transitions
	 *            that should be deleted
	 * @return As {@link INestedWordAutomaton} extracted DFA
	 */
	private INestedWordAutomaton<String, String> extractPackedDFA(
			final int[] dfa, final Set<Integer> accStates,
			final Set<Integer> transToDelete) {
		final List<String> num2State = new ArrayList<String>(msize);
		for (int i = 0; i < msize; ++i) {
			num2State.add(PREFIX_NODE + i);
		}
		final String initialState = num2State.get(0);

		final List<String> num2Letter = new ArrayList<String>(malphabetSize);
		for (int i = 0; i < malphabetSize; ++i) {
			num2Letter.add(PREFIX_TRANSITION + i);
		}

		final StateFactory<String> stateFactory = new StringFactory();
		NestedWordAutomaton<String, String> result;
		result = new NestedWordAutomaton<String, String>(mServices,
				new HashSet<String>(num2Letter), null, null, stateFactory);

		// Create states
		for (int i = 0; i < msize; ++i) {
			final String state = num2State.get(i);
			final boolean isAccepting = accStates.contains(i);
			final boolean isInitial = state.equals(initialState);
			result.addState(isInitial, isAccepting, state);
		}

		// Create transitions
		for (int i = 0; i < dfa.length; i++) {
			//Skip transition if it should not be contained in the final automata
			if (transToDelete.contains(i)) {
				continue;
			}
			final int predStateIndex = (int) Math.floor((i + 0.0) / malphabetSize);
			final int letterIndex = i % malphabetSize;
			final int succStateIndex = dfa[i];
			// Skip transition if it points to a node out of the wished size.
			// This node is the sink node for non-total DFAs.
			final String predState = num2State.get(predStateIndex);
			final String letter = num2Letter.get(letterIndex);
			final String succState = num2State.get(succStateIndex);

			result.addInternalTransition(predState, letter, succState);
		}

		return result;
	}

	/**
	 * Generates a flag for a given node using the first possible position at
	 * where it is allowed to appear.<br />
	 * The flag represents the first edge in the DFA that points to the given
	 * node, as index in the string format sequence (specified by
	 * {@link #generatePackedRandomDFA(int, int, int, boolean, boolean)
	 * generatePackedRandomDFA(...)}).<br />
	 * It takes the correct probability of each flag, by calculation of the
	 * number of all possible DFAs with this setting, into account.<br/>
	 * <br/>
	 * This method is used by random DFA generation to ensure that some rules
	 * like <i>'every node must get accessed before it is reached in the
	 * sequence'</i> are satisfied.<br/>
	 * <br/>
	 * Runtime is in:<br/>
	 * <b>O(n * k) * O(random)</b><br/>
	 * where 'n' is the amount of nodes, 'k' the size of the alphabet and
	 * 'random' methods of {@link java.util.Random}.
	 * 
	 * @param node
	 *            Node to calculate flag for
	 * @param firstPossiblePos
	 *            First possible position in the sequence at where the flag is
	 *            allowed to appear
	 * @return Flag for the given node
	 */
	private int generateFlag(final int node, final int firstPossiblePos) {
		/*
		 * The length of the sequence before 'node's edges are reached. Flag
		 * must appear before this to satisfy all rules.
		 */
		final int PRE_SEQUENCE_LENGTH = node * malphabetSize;

		// If a uniform distribution must not be ensured randomly select a
		// possible position for the flag.
		if (!mensureIsUniform) {
			return mrandom.nextInt(PRE_SEQUENCE_LENGTH - firstPossiblePos)
					+ firstPossiblePos;
		}

		BigInteger permutations = BigInteger.ZERO;

		// Contains the numbers of DFAs including a probability of it where the
		// first occurrence of 'node' is at position 'index'.
		final BigInteger[] permutationsPerStep = new BigInteger[PRE_SEQUENCE_LENGTH
				- firstPossiblePos];

		int counter = 0;
		// Calculate the number of DFAs including a probability of it where the
		// first occurrence of 'node' is between 'firstPossiblePos' and
		// 'PRE_SEQUENCE_LENGTH - 1'.
		for (int i = firstPossiblePos; i <= PRE_SEQUENCE_LENGTH - 1; i++) {
			permutationsPerStep[counter] = permutationsTable[node][i]
					.multiply(BigInteger.valueOf((int) Math.pow(node, i
							- firstPossiblePos)));
			permutations = permutations.add(permutationsPerStep[counter]);
			counter++;
		}
		// Randomly select one of all possible permutations including its
		// probability
		BigInteger permutation = nextRandomBigInteger(
				permutations.add(BigInteger.ONE), mrandom);

		counter = 0;
		// Calculate the flag using the probability of each DFA setting and the
		// selected permutation
		for (int i = firstPossiblePos; i <= PRE_SEQUENCE_LENGTH - 1; i++) {
			if (permutation.compareTo(permutationsPerStep[counter]) < 0) {
				return i;
			} else {
				permutation = permutation
						.subtract(permutationsPerStep[counter]);
			}
			counter++;
		}
		return PRE_SEQUENCE_LENGTH - 1;
	}

	/**
	 * Generates a uniform or non-uniform distributed random connected total
	 * DFA with a given amount of nodes and size of alphabet.<br />
	 * Finally returns the DFA in a specific int[] array format.<br /><br />
	 * The int[] array format is a sequence of numbers that represents a breadth-first
	 * search where each state and edge is ordered by '<'.<br /><br />
	 * Example:<br />
	 * [0,1,0,0,1,2] with 3 nodes and an alphabet of size 2.<br />
	 * [0,1|0,0|1,2] each of the 3 nodes has 2 outgoing edges where the number denotes
	 * the destination.<br />
	 * e.g. 2nd edge of first node points to the second node.
	 * @return Uniform or non-uniform distributed random connected total
	 * DFA in a specific int[] array format
	 */
	public int[] generatePackedRandomDFA() throws IllegalArgumentException {
		if (msize < 1 || malphabetSize < 1) {
			throw new IllegalArgumentException(
					"Neither 'size' nor 'alphabetSize' must be less than one.");
		}
		if (mnumOfAccStates < 0 || mnumOfAccStates > msize) {
			throw new IllegalArgumentException(
					"'numOfAccStates' must not exceed 'size' or be less than zero.");
		}
		final int SEQUENCE_LENGTH = msize * malphabetSize;

		final int[] sequence = new int[SEQUENCE_LENGTH];
		int curSequenceIndex = 0;

		// Special case where size == 1
		if (msize == 1) {
			for (int i = 0; i < malphabetSize; i++) {
				sequence[curSequenceIndex] = 0;
				curSequenceIndex++;
			}
			return sequence;
		}

		// Case where size >= 2
		final Random rnd = new Random();

		if (mensureIsUniform) {
			preCalcPermutationsTable(SEQUENCE_LENGTH);
		}

		int lastFlag = -1;
		// Calculate the flags for each node and generate the sequence from left
		// to right until all nodes are reached by an edge.
		for (int i = 1; i <= msize - 1; i++) {
			final int curFlag = generateFlag(i, lastFlag + 1);
			mflags.add(curFlag);
			for (int j = lastFlag + 1; j <= curFlag - 1; j++) {
				// Only use nodes that were already reached
				sequence[curSequenceIndex] = rnd.nextInt(i);
				curSequenceIndex++;
			}
			sequence[curSequenceIndex] = i;
			curSequenceIndex++;
			lastFlag = curFlag;
		}
		// Now all nodes are reached by an edge and the rest of the sequence can
		// be filled up by using all nodes as edge destinations.
		for (int i = lastFlag + 1; i <= SEQUENCE_LENGTH - 1; i++) {
			sequence[curSequenceIndex] = rnd.nextInt(msize);
			curSequenceIndex++;
		}

		if (!menableCaching) {
			permutationsTable = null;
		}

		return sequence;
	}

	@Override
	public INestedWordAutomaton<String, String> getResult() {
		return mresult;
	}

	@Override
	public String operationName() {
		return "getRandomDfa";
	}

	/**
	 * Pre-calculates the permutations table where permutationsTable[m][j] is
	 * the number of DFAs that have the first occurrence of node 'm' at position
	 * 'j' in the sequence.<br/><br/>
	 * Runtime is in:<br/>
	 * <b>O(n * k)</b> if caching is not enabled<br/>
	 * <b>O(1)</b> if caching is enabled and n, k are equal to cached version<br/>
	 * <b>O(k)</b> if caching is enabled and n is equal to cached version<br/>
	 * where 'n' is the amount of nodes, 'k' the size of the alphabet.
	 * 
	 * @param sequenceLength
	 *            Length of sequence that must be size * alphabetSize
	 */
	private void preCalcPermutationsTable(final int sequenceLength) {
		final boolean hasUsableCache = menableCaching && permutationsTable != null
				&& permutationsTable[0] != null
				&& permutationsTable.length == msize;
		if (hasUsableCache && permutationsTable[0].length == sequenceLength) {
			return;
		}

		final BigInteger[][] nextPermutationsTable = new BigInteger[msize][sequenceLength];

		// Calculate the bottom row of the table.
		for (int i = (msize - 1) * malphabetSize - 1; i >= msize - 2; i--) {

			// If there is a usable cache, the second index is in range and
			// there is a value then copy it because this row is independent of
			// changes in alphabetSize.
			if (hasUsableCache && i < permutationsTable[0].length
					&& permutationsTable[msize - 1] != null
					&& permutationsTable[msize - 1][i] != null) {
				nextPermutationsTable[msize - 1][i] = permutationsTable[msize - 1][i];
			} else {
				nextPermutationsTable[msize - 1][i] = BigInteger.valueOf(
						msize).pow(sequenceLength - 1 - i);
			}
		}
		// Calculate the other rows from bottom to top and right to left using
		// the other entries.
		// Caching is not possible because all entries here are dependent on
		// changes in size and alphabetSize.
		for (int curNode = msize - 2; curNode >= 1; curNode--) {
			// Length of the sequence before 'curNode's edges are reached.
			final int preSequenceLength = curNode * malphabetSize;

			// Calculate the rightest entry of the current row using the
			// diagonal right entry of the bottom row.
			BigInteger permutations = BigInteger.ZERO;

			for (int i = 0; i <= malphabetSize - 1; i++) {
				permutations = permutations
						.add(nextPermutationsTable[curNode + 1][preSequenceLength
								+ i].multiply(BigInteger.valueOf((int) Math
								.pow(curNode + 1, i))));
			}
			nextPermutationsTable[curNode][preSequenceLength - 1] = permutations;

			// Calculate the other entries of the current row from right to left
			// using the righter entry of this row and the diagonal right entry
			// of the bottom row.
			for (int i = preSequenceLength - 2; i >= curNode - 1; i--) {
				nextPermutationsTable[curNode][i] = BigInteger
						.valueOf(curNode + 1)
						.multiply(nextPermutationsTable[curNode][i + 1])
						.add(nextPermutationsTable[curNode + 1][i + 1]);
			}
		}

		permutationsTable = nextPermutationsTable;
	}

	@Override
	public String startMessage() {
		return MessageFormat
				.format("Start {0}. Alphabet size {1} Number of states {2} "
						+ "Number of accepting states {3} Perc of totality {4} "
						+ "Ensure states reach final {5} Ensure is uniform {6} "
						+ "Is caching enabled {7}", operationName(),
						malphabetSize, msize, mnumOfAccStates, mpercOfTotality,
						mensureStatesReachFinal, mensureIsUniform, menableCaching);
	}
}
