/*
 * Copyright (C) 2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
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
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.interpolantautomata.transitionappender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.IHoareTripleChecker;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.IHoareTripleChecker.Validity;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.DivisibilityPredicateGenerator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.PredicateUnifier;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.PredicateUnifier.CoverageRelation;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.HashRelation;

/**
 * Deterministic interpolant automaton with on-demand construction.
 * The successor state for a given state ψ and a CodeBlock cb are is 
 * constructed as follows.
 * First we construct a set S of successors very similar to the construction
 * in {@see NondeterministicInterpolantAutomaton}. However, since this automaton
 * is deterministic, we do not have several successors. Instead, we take
 * as successor the conjunction of all IPredicates in S.
 * If the IPredicate that represents this conjunction is not yet a state of
 * this automaton, we add this state.
 * @author Matthias Heizmann
 *
 */
public class DeterministicInterpolantAutomaton extends BasicAbstractInterpolantAutomaton {
	
	private final Map<Set<IPredicate>, IPredicate> mInputPreds2ResultPreds = 
			new HashMap<Set<IPredicate>, IPredicate>();
	private final HashRelation<IPredicate, IPredicate> mResPred2InputPreds = 
			new HashRelation<IPredicate, IPredicate>();


	protected final Set<IPredicate> mNonTrivialPredicates;
	
	/**
	 * Split up predicates in their conjuncts. 
	 * First experiments on few examples showed that this is decreasing the
	 * performance.
	 */
	private final boolean mCannibalize;
	private final boolean mSplitNumericEqualities = true;
	private final boolean mDivisibilityPredicates = false;
	private final boolean mConservativeSuccessorCandidateSelection;
	

	

	public DeterministicInterpolantAutomaton(final IUltimateServiceProvider services, 
			final SmtManager smtManager, final ModifiableGlobalVariableManager modglobvarman, final IHoareTripleChecker hoareTripleChecker,
			final INestedWordAutomaton<CodeBlock, IPredicate> abstraction, 
			final NestedWordAutomaton<CodeBlock, IPredicate> interpolantAutomaton, 
			final PredicateUnifier predicateUnifier, final ILogger logger, 
			final boolean conservativeSuccessorCandidateSelection,
			final boolean cannibalize) {
		super(services, smtManager, hoareTripleChecker, true, abstraction, 
				predicateUnifier, 
				interpolantAutomaton, logger);
		mCannibalize = cannibalize;
		mConservativeSuccessorCandidateSelection = conservativeSuccessorCandidateSelection;
		if (mConservativeSuccessorCandidateSelection && mCannibalize) {
			throw new IllegalArgumentException("ConservativeSuccessorCandidateSelection and Cannibalize are incompatible");
		}
		Collection<IPredicate> allPredicates;
		if (mCannibalize ) {
			allPredicates = mPredicateUnifier.cannibalizeAll(mSplitNumericEqualities, interpolantAutomaton.getStates().toArray(new IPredicate[0]));
			for (final IPredicate pred : allPredicates) {
				if (!interpolantAutomaton.getStates().contains(pred)) {
					interpolantAutomaton.addState(false, false, pred);
				}
			}
		} else {
			allPredicates = interpolantAutomaton.getStates(); 
		}
		if (mDivisibilityPredicates) {
			allPredicates = new ArrayList<IPredicate>(allPredicates);
			final DivisibilityPredicateGenerator dpg = new DivisibilityPredicateGenerator(mSmtManager, mPredicateUnifier);
			final Collection<IPredicate> divPreds = dpg.divisibilityPredicates(allPredicates);
			allPredicates.addAll(divPreds);
			for (final IPredicate pred : divPreds) {
				if (!interpolantAutomaton.getStates().contains(pred)) {
					interpolantAutomaton.addState(false, false, pred);
				}
			}
		}
		
		assert mIaTrueState.getFormula().toString().equals("true");
		assert allPredicates.contains(mIaTrueState);
		mAlreadyConstrucedAutomaton.addState(true, false, mIaTrueState);
		mResPred2InputPreds.addPair(mIaTrueState, mIaTrueState);
		assert mIaFalseState.getFormula().toString().equals("false");
		assert allPredicates.contains(mIaFalseState);
		mAlreadyConstrucedAutomaton.addState(false, true, mIaFalseState);
		mResPred2InputPreds.addPair(mIaFalseState, mIaFalseState);

		mNonTrivialPredicates = new HashSet<IPredicate>();
		for (final IPredicate state : allPredicates) {
			if (state != mIaTrueState && state != mIaFalseState) {
				processResPredInputPredsMapping(state);
				mNonTrivialPredicates.add(state);
			}
		}

		mLogger.info(startMessage());
		mLogger.info(((CoverageRelation) mPredicateUnifier.getCoverageRelation()).getCoverageRelationStatistics());
	}

	/**
	 * Add add input states that are 
	 * 1. implied by resPred
	 * 2. contained in mInterpolantAutomaton
	 * 3. different from "true"
	 * to the  mResPred2InputPreds Map
	 */
	private void processResPredInputPredsMapping(final IPredicate resPred) {
		final Set<IPredicate> impliedPredicates = 
				mPredicateUnifier.getCoverageRelation().getCoveringPredicates(resPred);
		for (final IPredicate impliedPredicate : impliedPredicates) {
			if (impliedPredicate != mIaTrueState && mInputInterpolantAutomaton.getStates().contains(impliedPredicate)) {
				mResPred2InputPreds.addPair(resPred, impliedPredicate);
			}
		}
	}
	
	@Override
	protected String startMessage() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Constructing interpolant automaton ");
		sb.append("starting with ");
		sb.append(mNonTrivialPredicates.size() + 2);
		sb.append(" interpolants.");
		return sb.toString();
	}
	
	@Override
	protected String switchToReadonlyMessage() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Switched to read-only mode: deterministic interpolant automaton has ");
		sb.append(mAlreadyConstrucedAutomaton.size()).append(" states. ");
		return sb.toString();
	}
	
	@Override
	protected String switchToOnTheFlyConstructionMessage() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Switched to On-DemandConstruction mode: deterministic interpolant automaton has ");
		sb.append(mAlreadyConstrucedAutomaton.size()).append(" states. ");
		return sb.toString();
	}


	@Override
	protected void addOtherSuccessors(final IPredicate resPred, final IPredicate resHier,
			final CodeBlock letter, final SuccessorComputationHelper sch,
			final Set<IPredicate> inputSuccs) {
		for (final IPredicate succCand : selectSuccessorCandidates(resPred, resHier)) {
			if (!inputSuccs.contains(succCand)) {
				final Validity sat = sch.computeSuccWithSolver(resPred, resHier, letter, succCand);
				if (sat == Validity.VALID) {
					inputSuccs.add(succCand);
				}
			}
		}
	}
	
	private Set<IPredicate> selectSuccessorCandidates(final IPredicate resPred, final IPredicate resHier) {
		if (mConservativeSuccessorCandidateSelection ) {
			return selectSuccessorCandidates_TryConservative(resPred, resHier);
		} else {
			return selectSuccessorCandidates_TryAll();
		}
	}
	
	private Set<IPredicate> selectSuccessorCandidates_TryConservative(final IPredicate resPred, final IPredicate resHier) {
		Set<IPredicate> succCands;
		if (resHier != null) {
			succCands = new HashSet<IPredicate>();
			succCands.addAll(mResPred2InputPreds.getImage(resPred));
			succCands.addAll(mResPred2InputPreds.getImage(resHier));
		} else {
			succCands = mResPred2InputPreds.getImage(resPred);
		}
		return succCands;
	}
	
	private Set<IPredicate> selectSuccessorCandidates_TryAll() {
		return mNonTrivialPredicates;		
	}


	/**
	 * Add all successors of resPred, resHier, and letter in automaton.
	 * If resPred and resHier were constructed as a conjunction of 
	 * inputPredicates, we also take the conjuncts.
	 * @param inputSuccs 
	 */
	@Override
	protected void addInputAutomatonSuccs(
			final IPredicate resPred, final IPredicate resHier, final CodeBlock letter,
			final SuccessorComputationHelper sch, final Set<IPredicate> inputSuccs) {
		Set<IPredicate> resPredConjuncts = mResPred2InputPreds.getImage(resPred);
		assert mCannibalize || resPredConjuncts != null;
		if (resPredConjuncts == null) {
			resPredConjuncts = Collections.emptySet();
		}
		final IterableWithAdditionalElement<IPredicate> resPredConjunctsWithTrue = 
				new IterableWithAdditionalElement<IPredicate>(resPredConjuncts, mIaTrueState);
		final IterableWithAdditionalElement<IPredicate> resHierConjunctsWithTrue;
		if (resHier == null) {
			resHierConjunctsWithTrue = null;
		} else {
			Set<IPredicate> resHierConjuncts;
			resHierConjuncts = mResPred2InputPreds.getImage(resHier);
			resHierConjunctsWithTrue = new IterableWithAdditionalElement<IPredicate>(resHierConjuncts, mIaTrueState);
		}
		for (final IPredicate inputPred : resPredConjunctsWithTrue) {
			if (resHier == null) {
				inputSuccs.addAll(sch.getSuccsInterpolantAutomaton(inputPred, null, letter));
			} else {
				for (final IPredicate inputHier : resHierConjunctsWithTrue) {
					inputSuccs.addAll(sch.getSuccsInterpolantAutomaton(inputPred, inputHier, letter));
				}
			}
		}
	}

	




	private IPredicate getOrConstructPredicate(final Set<IPredicate> succs) {
//		assert mInterpolantAutomaton.getStates().containsAll(succs);
		final IPredicate result;
		if (succs.isEmpty()) {
			result = mIaTrueState;
		} else if (succs.size() == 1) {
			result = succs.iterator().next();
			if (!mAlreadyConstrucedAutomaton.contains(result)) {
				mAlreadyConstrucedAutomaton.addState(false, false, result);
			}
		} else {
			IPredicate resSucc = mInputPreds2ResultPreds.get(succs);
			if (resSucc == null) {
				resSucc = mPredicateUnifier.getOrConstructPredicateForConjunction(succs);
				mInputPreds2ResultPreds.put(succs, resSucc);
				for (final IPredicate succ : succs) {
					assert mAlreadyConstrucedAutomaton.contains(succ) || 
						mInputInterpolantAutomaton.getStates().contains(succ) : "unknown state " + succ; 
					if (mNonTrivialPredicates.contains(succ)) {
						mResPred2InputPreds.addPair(resSucc, succ);
					}
				}
				if (!mAlreadyConstrucedAutomaton.contains(resSucc)) {
					processResPredInputPredsMapping(resSucc);
					mAlreadyConstrucedAutomaton.addState(false, false, resSucc);
				}
			} 
			result = resSucc;
		}
		return result;
	}

	@Override
	protected void constructSuccessorsAndTransitions(final IPredicate resPred,
			final IPredicate resHier, final CodeBlock letter, 
			final SuccessorComputationHelper sch, final Set<IPredicate> inputSuccs) {
		final IPredicate resSucc = getOrConstructPredicate(inputSuccs);
		sch.addTransition(resPred, resHier, letter, resSucc);
		sch.reportSuccsComputed(resPred, resHier, letter);
	}
	
	
}
