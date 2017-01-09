/*
 * Copyright (C) 2016 Christian Schilling (schillic@informatik.uni-freiburg.de)
 * Copyright (C) 2016 University of Freiburg
 * 
 * This file is part of the ULTIMATE TraceAbstraction plug-in.
 * 
 * The ULTIMATE TraceAbstraction plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE TraceAbstraction plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE TraceAbstraction plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE TraceAbstraction plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE TraceAbstraction plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.interpolantautomata.builders;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.IAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.IRun;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomatonSimple;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedWord;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.statefactory.IStateFactory;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singletracecheck.TraceCheckerUtils.InterpolantsPreconditionPostcondition;

/**
 * Interpolant automaton builder for multiple sequences of interpolants (also works for one sequence).
 * <p>
 * The contract of this class is that all sequences of interpolants in the input share the same pre- and postcondition.
 * Hence it suffices to look at only one of them.
 * <p>
 * This class is a generalization of {@link TwoTrackInterpolantAutomatonBuilder}.
 * 
 * @author Christian Schilling (schillic@informatik.uni-freiburg.de)
 */
public class MultiTrackInterpolantAutomatonBuilder<LETTER> implements IInterpolantAutomatonBuilder<LETTER, IPredicate> {
	private final NestedWordAutomaton<LETTER, IPredicate> mResult;

	/**
	 * @param services
	 *            Ultimate services.
	 * @param nestedRun
	 *            nested run
	 * @param interpolantSequences
	 *            list of interpolant sequences
	 * @param abstraction
	 *            old abstraction
	 */
	public MultiTrackInterpolantAutomatonBuilder(final IUltimateServiceProvider services,
			final IRun<LETTER, IPredicate, ?> nestedRun,
			final List<InterpolantsPreconditionPostcondition> interpolantSequences,
			final IAutomaton<LETTER, IPredicate> abstraction) {
		if (interpolantSequences.isEmpty()) {
			throw new IllegalArgumentException("Empty list of interpolant sequences is not allowed.");
		}
		assert sequencesHaveSamePrePostconditions(
				interpolantSequences) : "The interpolant sequences should have the same pre- and postconditions.";

		mResult = constructInterpolantAutomaton(services, abstraction, interpolantSequences,
				NestedWord.nestedWord(nestedRun.getWord()), abstraction.getStateFactory());
	}

	@Override
	public NestedWordAutomaton<LETTER, IPredicate> getResult() {
		return mResult;
	}

	private NestedWordAutomaton<LETTER, IPredicate> constructInterpolantAutomaton(
			final IUltimateServiceProvider services, final IAutomaton<LETTER, IPredicate> abstraction,
			final List<InterpolantsPreconditionPostcondition> interpolantSequences, final NestedWord<LETTER> nestedWord,
			final IStateFactory<IPredicate> taContentFactory) {
		final Set<LETTER> internalAlphabet = abstraction.getAlphabet();
		final Set<LETTER> callAlphabet;
		final Set<LETTER> returnAlphabet;

		if (abstraction instanceof INestedWordAutomatonSimple) {
			final INestedWordAutomatonSimple<LETTER, IPredicate> abstractionAsNwa =
					(INestedWordAutomatonSimple<LETTER, IPredicate>) abstraction;
			callAlphabet = abstractionAsNwa.getCallAlphabet();
			returnAlphabet = abstractionAsNwa.getReturnAlphabet();
		} else {
			callAlphabet = Collections.emptySet();
			returnAlphabet = Collections.emptySet();
		}

		final NestedWordAutomaton<LETTER, IPredicate> nwa =
				new NestedWordAutomaton<>(new AutomataLibraryServices(services), internalAlphabet, callAlphabet,
						returnAlphabet, taContentFactory);

		addStatesAccordingToPredicates(nwa, interpolantSequences, nestedWord);
		addBasicTransitions(nwa, interpolantSequences, nestedWord);

		return nwa;
	}

	/**
	 * Add a state for each forward predicate and for each backward predicate.
	 * 
	 * @param nwa
	 *            the automaton to which the states are added
	 * @param interpolantSequences
	 *            sequences of interpolants
	 * @param nestedWord
	 *            trace along which the interpolants are constructed
	 */
	private void addStatesAccordingToPredicates(final NestedWordAutomaton<LETTER, IPredicate> nwa,
			final List<InterpolantsPreconditionPostcondition> interpolantSequences,
			final NestedWord<LETTER> nestedWord) {
		// add initial state with precondition predicate
		nwa.addState(true, false, interpolantSequences.get(0).getPrecondition());

		for (final InterpolantsPreconditionPostcondition interpolantSequence : interpolantSequences) {
			for (int i = 1; i < nestedWord.length() + 1; i++) {
				final IPredicate interpolant = interpolantSequence.getInterpolant(i);
				if (!nwa.getStates().contains(interpolant)) {
					nwa.addState(false, isFalsePredicate(interpolant), interpolant);
				}
			}
		}
	}

	/**
	 * @param predicate
	 *            Predicate.
	 * @return {@code true} iff the predicate is {@code false}
	 */
	private static boolean isFalsePredicate(final IPredicate predicate) {
		return SmtUtils.isFalse(predicate.getFormula());
	}

	/**
	 * Add basic transitions in 3 steps. 1. For each predicate type add a transition from the precondition to the first
	 * predicate. (i.e. add transition (preCondition, st_0, FP_0), add transition (preCondition, st_0, BP_0)) 2. For
	 * each predicate type add a transition from the previous predicate to the current predicate. (i.e. add transition
	 * (FP_i-1, st_i, FP_i), add transition (BP_i-1, st_i, BP_i)) 3. For each predicate type add a transition from the
	 * last predicate to the post-condition. (i.e. add transition (FP_n-1, st_n, postCondition), add transition (BP_n-1,
	 * st_n, postCondition))
	 * 
	 * @param nwa
	 *            - the automaton to which the basic transition are added
	 * @param interpolantSequences
	 *            sequences of interpolants
	 * @param nestedWord
	 *            trace along which the interpolants are constructed
	 */
	private void addBasicTransitions(final NestedWordAutomaton<LETTER, IPredicate> nwa,
			final List<InterpolantsPreconditionPostcondition> interpolantSequences,
			final NestedWord<LETTER> nestedWord) {
		for (final InterpolantsPreconditionPostcondition interpolantSequence : interpolantSequences) {
			for (int i = 0; i < nestedWord.length(); i++) {
				addTransition(nwa, interpolantSequence, nestedWord, i);
			}
		}
	}

	private void addTransition(final NestedWordAutomaton<LETTER, IPredicate> nwa,
			final InterpolantsPreconditionPostcondition interpolantSequence, final NestedWord<LETTER> nestedWord,
			final int symbolPos) {
		final LETTER symbol = nestedWord.getSymbol(symbolPos);
		final IPredicate succ = interpolantSequence.getInterpolant(symbolPos + 1);
		if (nestedWord.isCallPosition(symbolPos)) {
			final IPredicate pred = interpolantSequence.getInterpolant(symbolPos);
			if (!nwa.containsCallTransition(pred, symbol, succ)) {
				nwa.addCallTransition(pred, symbol, succ);
			}
		} else if (nestedWord.isReturnPosition(symbolPos)) {
			final IPredicate pred = interpolantSequence.getInterpolant(symbolPos);
			final int callPos = nestedWord.getCallPosition(symbolPos);
			final IPredicate hier = interpolantSequence.getInterpolant(callPos);
			if (!nwa.containsReturnTransition(pred, hier, symbol, succ)) {
				nwa.addReturnTransition(pred, hier, symbol, succ);
			}
		} else {
			final IPredicate pred = interpolantSequence.getInterpolant(symbolPos);
			if (!nwa.containsInternalTransition(pred, symbol, succ)) {
				nwa.addInternalTransition(pred, symbol, succ);
			}
		}
	}

	@SuppressWarnings("squid:S1698")
	private static boolean
			sequencesHaveSamePrePostconditions(final List<InterpolantsPreconditionPostcondition> interpolantSequences) {
		final Iterator<InterpolantsPreconditionPostcondition> it = interpolantSequences.iterator();
		final InterpolantsPreconditionPostcondition first = it.next();

		final IPredicate precondition = first.getPrecondition();
		final IPredicate postcondition = first.getPostcondition();

		while (it.hasNext()) {
			final InterpolantsPreconditionPostcondition sequence = it.next();
			if (precondition != sequence.getPrecondition() || postcondition != sequence.getPostcondition()) {
				return false;
			}
		}
		return true;
	}
}
