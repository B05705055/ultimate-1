/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE AbstractInterpretationV2 plug-in.
 * 
 * The ULTIMATE AbstractInterpretationV2 plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE AbstractInterpretationV2 plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE AbstractInterpretationV2 plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE AbstractInterpretationV2 plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE AbstractInterpretationV2 plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.algorithm.rcfg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.boogie.IBoogieVar;
import de.uni_freiburg.informatik.ultimate.core.services.model.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.Util;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Boogie2SMT;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.algorithm.IAbstractStateStorage;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.algorithm.ITransitionProvider;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.model.IAbstractState;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.model.IAbstractStateBinaryOperator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;

/**
 * 
 * @author dietsch@informatik.uni-freiburg.de
 *
 */
public abstract class BaseRcfgAbstractStateStorageProvider<STATE extends IAbstractState<STATE, CodeBlock, IBoogieVar>, LOCATION>
		implements IAbstractStateStorage<STATE, CodeBlock, IBoogieVar, LOCATION> {

	private final IAbstractStateBinaryOperator<STATE> mMergeOperator;
	private final IUltimateServiceProvider mServices;
	private final List<BaseRcfgAbstractStateStorageProvider<STATE, LOCATION>> mChildStores;
	private final ITransitionProvider<CodeBlock, LOCATION> mTransProvider;

	public BaseRcfgAbstractStateStorageProvider(final IAbstractStateBinaryOperator<STATE> mergeOperator,
			final IUltimateServiceProvider services, final ITransitionProvider<CodeBlock, LOCATION> transProvider) {
		assert mergeOperator != null;
		assert services != null;
		assert transProvider != null;
		mMergeOperator = mergeOperator;
		mServices = services;
		mTransProvider = transProvider;
		mChildStores = new ArrayList<>();
	}

	@Override
	public List<STATE> getAbstractPostStates(CodeBlock transition) {
		assert transition != null;
		return getPostStates(transition).stream().collect(Collectors.toList());
	}

	@Override
	public void addAbstractPostState(CodeBlock transition, STATE state) {
		assert transition != null;
		assert state != null;
		final Deque<STATE> states = getPostStates(transition);
		if (states.stream().anyMatch(a -> a == state || a.isEqualTo(state))) {
			// do not add already existing states or fixpoint states
			return;
		}
		states.addFirst(state);
	}

	public void removeAbstractPostState(CodeBlock transition, STATE state) {
		getPostStates(transition).remove(state);
	}

	@Override
	public STATE mergePostStates(CodeBlock transition) {
		assert transition != null;
		final Deque<STATE> states = getPostStates(transition);
		if (states.isEmpty()) {
			return null;
		}
		final Iterator<STATE> iterator = states.iterator();

		STATE accumulator = null;
		while (iterator.hasNext()) {
			final STATE state = iterator.next();
			if (accumulator == null) {
				accumulator = state;
			} else {
				accumulator = mMergeOperator.apply(state, accumulator);
				assert accumulator.getVariables().keySet()
						.equals(state.getVariables().keySet()) : "states have different variables";
			}
			iterator.remove();
		}

		assert accumulator != null;
		states.addFirst(accumulator);
		assert states.size() == 1;
		return accumulator;
	}

	@Override
	public List<STATE> widenPostState(final CodeBlock transition, final IAbstractStateBinaryOperator<STATE> wideningOp,
			STATE operand) {
		assert transition != null;
		final Deque<STATE> states = getPostStates(transition);
		final Iterator<STATE> iterator = states.iterator();
		final Deque<STATE> newStates = new ArrayDeque<STATE>(states.size());
		while (iterator.hasNext()) {
			final STATE newState = wideningOp.apply(operand, iterator.next());
			assert newState.getVariables().keySet()
					.equals(operand.getVariables().keySet()) : "states have different variables";
			assert newState != null;
			iterator.remove();
			newStates.addFirst(newState);
		}
		newStates.forEach(a -> addAbstractPostState(transition, a));
		return getAbstractPostStates(transition);
	}

	@Override
	public final IAbstractStateStorage<STATE, CodeBlock, IBoogieVar, LOCATION> createStorage() {
		final BaseRcfgAbstractStateStorageProvider<STATE, LOCATION> rtr = create();
		mChildStores.add(rtr);
		return rtr;
	}

	@Override
	public Map<LOCATION, Term> getLoc2Term(final CodeBlock initialTransition, final Script script,
			final Boogie2SMT bpl2smt) {
		// TODO: What shall we do about different scopes? Currently, states at the same location are merged and later
		// converted to terms. This leads to loss of precision, so that tools relying on those terms cannot prove
		// absence of errors with the terms alone, even if AI was able to prove it.
		final Map<LOCATION, StateDecorator> states = getMergedStatesOfAllChilds(initialTransition);
		return convertStates2Terms(states, script, bpl2smt);
	}

	@Override
	public Set<Term> getTerms(final CodeBlock initialTransition, final Script script, final Boogie2SMT bpl2smt) {
		final Set<Term> rtr = new LinkedHashSet<>();
		getLocalTerms(initialTransition, script, bpl2smt, rtr);
		for (final BaseRcfgAbstractStateStorageProvider<STATE, LOCATION> child : mChildStores) {
			rtr.addAll(child.getTerms(initialTransition, script, bpl2smt));
		}
		return rtr;
	}

	private Map<LOCATION, StateDecorator> getMergedStatesOfAllChilds(final CodeBlock initialTransition) {
		Map<LOCATION, StateDecorator> states = getMergedLocalStates(initialTransition);
		for (final BaseRcfgAbstractStateStorageProvider<STATE, LOCATION> child : mChildStores) {
			states = mergeMaps(states, child.getMergedStatesOfAllChilds(initialTransition));
		}
		return states;
	}

	private Map<LOCATION, StateDecorator> getMergedLocalStates(final CodeBlock initialTransition) {
		final Map<LOCATION, StateDecorator> rtr = new HashMap<>();
		final Deque<CodeBlock> worklist = new ArrayDeque<>();
		final Set<CodeBlock> closed = new HashSet<>();

		worklist.add(initialTransition);

		while (!worklist.isEmpty()) {
			final CodeBlock current = worklist.remove();
			// check if already processed
			if (!closed.add(current)) {
				continue;
			}

			final LOCATION postLoc = mTransProvider.getTarget(current);
			// add successors to worklist
			for (final CodeBlock outgoing : mTransProvider.getSuccessorActions(postLoc)) {
				if (!(outgoing instanceof CodeBlock)) {
					continue;
				}
				worklist.add(outgoing);
			}

			final Deque<STATE> states = getPostStates(current);

			StateDecorator currentState;
			if (states == null || states.isEmpty()) {
				// no states for this location
				currentState = new StateDecorator();
			} else if (states.size() == 1) {
				currentState = new StateDecorator(states.getFirst());
			} else {
				currentState = new StateDecorator(states.stream().reduce(mMergeOperator::apply).get());
			}

			final StateDecorator alreadyKnownState = rtr.get(current);
			if (alreadyKnownState != null) {
				currentState = alreadyKnownState.merge(currentState);
			}
			rtr.put(postLoc, currentState);
		}
		return rtr;
	}

	private Set<Term> getLocalTerms(final CodeBlock initialTransition, final Script script, final Boogie2SMT bpl2smt,
			final Set<Term> terms) {
		final Deque<CodeBlock> worklist = new ArrayDeque<>();
		final Set<CodeBlock> closed = new LinkedHashSet<>();

		worklist.add(initialTransition);

		final Term falseTerm = script.term("false");

		while (!worklist.isEmpty()) {
			final CodeBlock current = worklist.remove();
			// check if already processed
			if (!closed.add(current)) {
				continue;
			}

			final LOCATION postLoc = mTransProvider.getTarget(current);
			// add successors to worklist
			for (final CodeBlock outgoing : mTransProvider.getSuccessorActions(postLoc)) {
				if (!(outgoing instanceof CodeBlock)) {
					continue;
				}
				worklist.add(outgoing);
			}

			final Deque<STATE> states = getPostStates(current);

			if (states == null || states.isEmpty()) {
				// no states for this location
				terms.add(falseTerm);
			} else {
				Term localTerm = falseTerm;
				for (final STATE state : states) {
					localTerm = Util.or(script, localTerm, state.getTerm(script, bpl2smt));
				}
				terms.add(localTerm);
			}
		}
		return terms;
	}

	private Map<LOCATION, StateDecorator> mergeMaps(Map<LOCATION, StateDecorator> a, Map<LOCATION, StateDecorator> b) {
		final Map<LOCATION, StateDecorator> rtr = new HashMap<>();

		for (final Entry<LOCATION, StateDecorator> entryA : a.entrySet()) {
			final StateDecorator valueB = b.get(entryA.getKey());
			if (valueB == null) {
				rtr.put(entryA.getKey(), entryA.getValue());
			} else {
				rtr.put(entryA.getKey(), entryA.getValue().merge(valueB));
			}
		}

		for (final Entry<LOCATION, StateDecorator> entryB : b.entrySet()) {
			final StateDecorator valueA = a.get(entryB.getKey());
			if (valueA == null) {
				rtr.put(entryB.getKey(), entryB.getValue());
			} else {
				// do nothing, this was already done in first iteration
			}
		}

		return rtr;
	}

	private Map<LOCATION, Term> convertStates2Terms(final Map<LOCATION, StateDecorator> states, final Script script,
			final Boogie2SMT bpl2smt) {
		final Map<LOCATION, Term> rtr = new HashMap<>();

		for (final Entry<LOCATION, StateDecorator> entry : states.entrySet()) {
			final Term term = entry.getValue().getTerm(script, bpl2smt);
			final LOCATION loc = entry.getKey();
			rtr.put(loc, term);
		}

		return rtr;
	}

	protected abstract BaseRcfgAbstractStateStorageProvider<STATE, LOCATION> create();

	protected abstract Deque<STATE> getPostStates(CodeBlock action);

	protected IAbstractStateBinaryOperator<STATE> getMergeOperator() {
		return mMergeOperator;
	}

	protected IUltimateServiceProvider getServices() {
		return mServices;
	}

	protected ITransitionProvider<CodeBlock, LOCATION> getTransitionProvider() {
		return mTransProvider;
	}

	private final class StateDecorator {
		private final STATE mState;

		private StateDecorator() {
			mState = null;
		}

		private StateDecorator(STATE state) {
			mState = state;
		}

		private Term getTerm(final Script script, final Boogie2SMT bpl2smt) {
			if (mState == null) {
				return script.term("false");
			}
			return mState.getTerm(script, bpl2smt);
		}

		private StateDecorator merge(final StateDecorator other) {
			if (other == null || other.mState == null) {
				return this;
			}
			if (mState == null) {
				return other;
			}
			return new StateDecorator(mMergeOperator.apply(mState, other.mState));
		}

	}
}
