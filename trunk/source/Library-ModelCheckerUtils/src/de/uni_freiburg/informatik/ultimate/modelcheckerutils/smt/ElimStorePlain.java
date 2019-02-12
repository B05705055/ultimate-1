/*
 * Copyright (C) 2017 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2017 University of Freiburg
 *
 * This file is part of the ULTIMATE ModelCheckerUtils Library.
 *
 * The ULTIMATE ModelCheckerUtils Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE ModelCheckerUtils Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE ModelCheckerUtils Library. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE ModelCheckerUtils Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE ModelCheckerUtils Library grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.QuantifiedFormula;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.ModelCheckerUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils.SimplificationTechnique;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils.XnfConversionTechnique;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.MultiDimensionalSort;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearterms.PrenexNormalForm;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearterms.QuantifierPusher;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearterms.QuantifierPusher.PqeTechniques;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearterms.QuantifierSequence;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearterms.QuantifierSequence.QuantifiedVariables;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.managedscript.ManagedScript;
import de.uni_freiburg.informatik.ultimate.smtinterpol.util.DAGSize;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Pair;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.TreeRelation;

/**
 * TODO 2017-10-17 Matthias: The following documentation is outdated.
 * Let aElim be the array variable that we want to eliminate. We presume that
 * there is only one term of the form (store aElim storeIndex newValue), for
 * some index element storeIndex and some value element newValue.
 *
 * The basic idea is the following. Let Idx be the set of all indices of select
 * terms that have aElim as (first) argument. We introduce
 * <ul>
 * <li>a new array variable aNew that represents the store term
 * <li>a new value variable oldCell_i for each i∈Idx that represents the value
 * of the array cell before the update.
 * </ul>
 * We replace
 * <ul>
 * <li>(store aElim storeIndex newValue) by aNew, and
 * <li>(select aElim i) by oldCell_i for each i∈Idx.
 * </ul>
 * Furthermore, we add the following conjuncts for each i∈Idx.
 * <ul>
 * <li> (i == storeIndex)==> (aNew[i] == newValue && ∀k∈Idx. i == k ==> oldCell_i == oldCell_k)
 * <li> (i != storeIndex) ==> (aNew[i] == oldCell_i)
 * </ul>
 *
 * Optimizations:
 * <ul>
 * <li> Optim1: We check equality and disequality for each pair of
 * indices and evaluate (dis)equalities in the formula above directly. Each
 * equality/disequality that is not valid (i.e. only true in this context) has
 * to be added as an additional conjunct.
 * <li> Optim2: We do not work with all
 * indices but build equivalence classes and work only with the representatives.
 * (We introduce only one oldCell variable for each equivalence class)
 * <li> Optim3: For each index i that is disjoint for the store index we do not introduce the
 * variable oldCell_i, but use aNew[i] instead.
 * <li> Optim4: For each i∈Idx we check
 * the context if we find some term tEq that is equivalent to oldCell_i. In case
 * we found some we use tEq instead of oldCell_i.
 * <li> Optim5: (Only sound in
 * combination with Optim3. For each pair i,k∈Idx that are both disjoint from
 * storeIndex, we can drop the "i == k ==> oldCell_i == oldCell_k" term.
 * Rationale: we use aNew[i] and aNew[k] instead of oldCell_i and oldCell_k,
 * hence the congruence information is already given implicitly.
 * </ul>
 *
 * @author Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 *
 */
public class ElimStorePlain {

	private final ManagedScript mMgdScript;
	private final IUltimateServiceProvider mServices;
	private final ILogger mLogger;
	private final SimplificationTechnique mSimplificationTechnique;
	private int mRecursiveCallCounter = -1;

	public ElimStorePlain(final ManagedScript mgdScript, final IUltimateServiceProvider services,
			final SimplificationTechnique simplificationTechnique) {
		super();
		mMgdScript = mgdScript;
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(ModelCheckerUtils.PLUGIN_ID);
		mSimplificationTechnique = simplificationTechnique;
	}

	/**
	 * Old, iterative elimination. Is sound but if we cannot eliminate all
	 * quantifiers it sometimes produces a large number of similar
	 * disjuncts/conjuncts that is too large for simplification.
	 *
	 */
	public EliminationTask elimAll(final EliminationTask eTask) {

		final Stack<EliminationTask> taskStack = new Stack<>();
		final ArrayList<Term> resultDisjuncts = new ArrayList<>();
		final Set<TermVariable> resultEliminatees = new LinkedHashSet<>();
		{
			final EliminationTask eliminationTask = new EliminationTask(eTask.getQuantifier(), eTask.getEliminatees(),
					eTask.getTerm());
			pushTaskOnStack(eliminationTask, taskStack);
		}
		int numberOfRounds = 0;
		while (!taskStack.isEmpty()) {
			final EliminationTask currentETask = taskStack.pop();
			final TreeRelation<Integer, TermVariable> tr = classifyEliminatees(currentETask.getEliminatees());

			final LinkedHashSet<TermVariable> arrayEliminatees = getArrayTvSmallDimensionsFirst(tr);

			if (arrayEliminatees.isEmpty()) {
				// no array eliminatees left
				resultDisjuncts.add(currentETask.getTerm());
				resultEliminatees.addAll(currentETask.getEliminatees());
			} else {
				TermVariable thisIterationEliminatee;
				{
					final Iterator<TermVariable> it = arrayEliminatees.iterator();
					thisIterationEliminatee = it.next();
					it.remove();
				}

				final EliminationTask ssdElimRes = new Elim1Store(mMgdScript, mServices, mSimplificationTechnique,
						eTask.getQuantifier()).elim1(currentETask.getQuantifier(), thisIterationEliminatee,
								currentETask.getTerm(), QuantifierUtils.getAbsorbingElement(mMgdScript.getScript(), eTask.getQuantifier()));
				arrayEliminatees.addAll(ssdElimRes.getEliminatees());
				// also add non-array eliminatees
				arrayEliminatees.addAll(tr.getImage(0));
				final EliminationTask eliminationTask1 = new EliminationTask(ssdElimRes.getQuantifier(),
						arrayEliminatees, ssdElimRes.getTerm());
				final EliminationTask eliminationTask2 = applyNonSddEliminations(mServices, mMgdScript,
						eliminationTask1, PqeTechniques.ALL_LOCAL);
				if (mLogger.isInfoEnabled()) {
					mLogger.info("Start of round: " + printVarInfo(tr) + " End of round: "
							+ printVarInfo(classifyEliminatees(eliminationTask2.getEliminatees())) + " and "
							+ QuantifierUtils.getXjunctsOuter(eTask.getQuantifier(), eliminationTask2.getTerm()).length
							+ " xjuncts.");
				}
//				assert (!maxSizeIncrease(tr, classifyEliminatees(eliminationTask2.getEliminatees()))) : "number of max-dim elements increased!";

				pushTaskOnStack(eliminationTask2, taskStack);
			}
			numberOfRounds++;
		}
		mLogger.info("Needed " + numberOfRounds + " rounds to eliminate " + eTask.getEliminatees().size()
				+ " variables, produced " + resultDisjuncts.size() + " xjuncts");
		// return term and variables that we could not eliminate
		return new EliminationTask(eTask.getQuantifier(), resultEliminatees, QuantifierUtils
				.applyCorrespondingFiniteConnective(mMgdScript.getScript(), eTask.getQuantifier(), resultDisjuncts));
	}

	/**
	 * New recursive elimination. Not yet finished but should be sound.
	 */
	public EliminationTask elimAllRec(final EliminationTask eTask) {
		final TreeRelation<Integer, TermVariable> tr = classifyEliminatees(eTask.getEliminatees());
		if (tr.isEmpty() || (tr.getDomain().size() == 1 && tr.getDomain().contains(0))) {
			return eTask;
		}
		mRecursiveCallCounter = 0;
		final long inputSize = new DAGSize().treesize(eTask.getTerm());
		final EliminationTask result = doElimAllRec(
				QuantifierUtils.getAbsorbingElement(mMgdScript.getScript(), eTask.getQuantifier()), eTask);
		final long outputSize = new DAGSize().treesize(result.getTerm());
		mLogger.info(String.format(
				"Needed %s recursive calls to eliminate %s variables, input treesize:%s, output treesize:%s",
				mRecursiveCallCounter, eTask.getEliminatees().size(), inputSize, outputSize));
		return result;
	}


	private EliminationTask doElimOneRec(final Term context, final EliminationTask eTask) {
		// input one ?
		// split in disjunction
		// elim1store, output many
		// apply non-sdd on new only
		// classify, recurse, result array free, put results together
		// classify, recurse, result array free, put results together
		// ...
		assert eTask.getEliminatees().size() == 1;
		final TermVariable eliminatee = eTask.getEliminatees().iterator().next();
		assert SmtSortUtils.isArraySort(eliminatee.getSort());
		final EliminationTask ssdElimRes = new Elim1Store(mMgdScript, mServices, mSimplificationTechnique,
				eTask.getQuantifier()).elim1(eTask.getQuantifier(), eliminatee,
						eTask.getTerm(), context);
		final EliminationTask eliminationTask2 = applyNonSddEliminations(mServices, mMgdScript,
				ssdElimRes, PqeTechniques.ALL_LOCAL);
		return doElimAllRec(context, eliminationTask2);

	}

	private EliminationTask doElimAllRec(final Term inputContext, final EliminationTask eTask) {
		mRecursiveCallCounter++;
		final int thisRecursiveCallNumber = mRecursiveCallCounter;
		final EliminationTask eTaskWithoutSos = ArrayQuantifierEliminationUtils.elimAllSos(eTask, mMgdScript, mServices, mLogger);
		final TreeRelation<Integer, TermVariable> tr = classifyEliminatees(eTaskWithoutSos.getEliminatees());
		Term currentTerm = eTaskWithoutSos.getTerm();
		final Set<TermVariable> newElimnatees = new LinkedHashSet<>();
		for (final Entry<Integer, TermVariable> entry : tr.entrySet()) {
			if (entry.getKey() != 0) {
				// get all disjuncts for exists
				final Pair<Term[], Term> split = split(eTaskWithoutSos.getQuantifier(), entry.getValue(), currentTerm);
				final Term[] sameJuncts = split.getFirst();
				final Term additionalContext = split.getSecond();
				final Term totalContext = QuantifierUtils.applyDualFiniteConnective(mMgdScript.getScript(),
						eTask.getQuantifier(), inputContext, additionalContext);
				final List<Term> resSameJuncts = new ArrayList<>();
				for (final Term sameJunct : sameJuncts) {
					if (Arrays.asList(sameJunct.getFreeVars()).contains(entry.getValue())) {
						final EliminationTask res = doElimOneRec(totalContext, new EliminationTask(
								eTaskWithoutSos.getQuantifier(), Collections.singleton(entry.getValue()), sameJunct));
						newElimnatees.addAll(res.getEliminatees());
						resSameJuncts.add(res.getTerm());
					} else {
						resSameJuncts.add(sameJunct);
					}
				}
				currentTerm = compose(additionalContext, eTaskWithoutSos.getQuantifier(), resSameJuncts);
				final boolean contextIsDisjunctive = (eTask.getQuantifier() == QuantifiedFormula.FORALL);
				currentTerm = new SimplifyDDAWithTimeout(mMgdScript.getScript(), false, mServices, inputContext, contextIsDisjunctive)
						.getSimplifiedTerm(currentTerm);
			}
		}
		final Set<TermVariable> resultEliminatees = new HashSet<>(newElimnatees);
		resultEliminatees.addAll(eTaskWithoutSos.getEliminatees());
		final EliminationTask resultEliminationTask = new EliminationTask(eTaskWithoutSos.getQuantifier(), resultEliminatees, currentTerm);
		final EliminationTask finalResult = applyNonSddEliminations(mServices, mMgdScript,
				resultEliminationTask, PqeTechniques.ALL_LOCAL);
		if (mLogger.isInfoEnabled()) {
			mLogger.info("Start of recursive call " + thisRecursiveCallNumber + ": " + printVarInfo(tr) + " End of recursive call: "
					+ printVarInfo(classifyEliminatees(finalResult.getEliminatees())) + " and "
					+ QuantifierUtils.getXjunctsOuter(finalResult.getQuantifier(), finalResult.getTerm()).length
					+ " xjuncts.");
		}
		return finalResult;
	}



	private Term compose(final Term additionalContext, final int quantifier, final List<Term> resSameJuncts) {
		final Term resSameJunction = QuantifierUtils.applyCorrespondingFiniteConnective(mMgdScript.getScript(), quantifier,
				resSameJuncts);
		final Term result = QuantifierUtils.applyDualFiniteConnective(mMgdScript.getScript(), quantifier, additionalContext,
				resSameJunction);
		return result;
	}

	private Pair<Term[], Term> split(final int quantifier, final TermVariable eliminatee, final Term term) {
		final List<Term> dualJunctsWithEliminatee = new ArrayList<>();
		final List<Term> dualJunctsWithoutEliminatee = new ArrayList<>();
		final Term[] dualJuncts = QuantifierUtils.getXjunctsInner(quantifier, term);
		for (final Term xjunct : dualJuncts) {
			if (Arrays.asList(xjunct.getFreeVars()).contains(eliminatee)) {
				dualJunctsWithEliminatee.add(xjunct);
			} else {
				dualJunctsWithoutEliminatee.add(xjunct);
			}
		}
		final Term dualJunctionWithElimantee = QuantifierUtils.applyDualFiniteConnective(mMgdScript.getScript(), quantifier, dualJunctsWithEliminatee);
		final Term dualJunctionWithoutElimantee = QuantifierUtils.applyDualFiniteConnective(mMgdScript.getScript(), quantifier, dualJunctsWithoutEliminatee);
		final Term xnf = QuantifierUtils.transformToXnf(mServices, mMgdScript.getScript(), quantifier, mMgdScript, dualJunctionWithElimantee,
				XnfConversionTechnique.BOTTOM_UP_WITH_LOCAL_SIMPLIFICATION);
		final Term[] result = QuantifierUtils.getXjunctsOuter(quantifier, xnf);
		return new Pair<Term[], Term >(result, dualJunctionWithoutElimantee);
	}

	private String printVarInfo(final TreeRelation<Integer, TermVariable> tr) {
		final StringBuilder sb = new StringBuilder();
		for (final Integer dim : tr.getDomain()) {
			sb.append(tr.getImage(dim).size() + " dim-" + dim + " vars, ");
		}
		return sb.toString();
	}

	private void pushTaskOnStack(final EliminationTask eTask, final Stack<EliminationTask> taskStack) {
		final Term term = eTask.getTerm();
		final Term[] disjuncts = QuantifierUtils.getXjunctsOuter(eTask.getQuantifier(), term);
		if (disjuncts.length == 1) {
			taskStack.push(eTask);
		} else {
			for (final Term disjunct : disjuncts) {
				taskStack.push(new EliminationTask(eTask.getQuantifier(), eTask.getEliminatees(), disjunct));
			}
		}
	}

	public static EliminationTask applyNonSddEliminations(final IUltimateServiceProvider services,
			final ManagedScript mgdScript, final EliminationTask eTask, final PqeTechniques techniques) {

		final Term xnf = QuantifierUtils.transformToXnf(services, mgdScript.getScript(), eTask.getQuantifier(),
				mgdScript, eTask.getTerm(), XnfConversionTechnique.BOTTOM_UP_WITH_LOCAL_SIMPLIFICATION);
		final Term quantified = SmtUtils.quantifier(mgdScript.getScript(), eTask.getQuantifier(),
				eTask.getEliminatees(), xnf);
		final Term pushed = new QuantifierPusher(mgdScript, services, true, techniques).transform(quantified);

		final Term pnf = new PrenexNormalForm(mgdScript).transform(pushed);
		final QuantifierSequence qs = new QuantifierSequence(mgdScript.getScript(), pnf);
		final Term matrix = qs.getInnerTerm();
		final List<QuantifiedVariables> qvs = qs.getQuantifierBlocks();

		final Set<TermVariable> eliminatees1;
		if (qvs.isEmpty()) {
			eliminatees1 = Collections.emptySet();
		} else if (qvs.size() == 1) {
			eliminatees1 = qvs.get(0).getVariables();
			if (qvs.get(0).getQuantifier() != eTask.getQuantifier()) {
				throw new UnsupportedOperationException("alternation not yet supported");
			}
		} else if (qvs.size() > 1) {
			throw new UnsupportedOperationException("alternation not yet supported");
		} else {
			throw new AssertionError();
		}
		return new EliminationTask(eTask.getQuantifier(), eliminatees1, matrix);
	}

	private LinkedHashSet<TermVariable> getArrayTvSmallDimensionsFirst(final TreeRelation<Integer, TermVariable> tr) {
		final LinkedHashSet<TermVariable> result = new LinkedHashSet<>();
		for (final Integer dim : tr.getDomain()) {
			if (dim != 0) {
				result.addAll(tr.getImage(dim));
			}
		}
		return result;
	}

	/**
	 * Given a set of {@link TermVariables} a_1,...,a_n, let dim(a_i) be the "array
	 * dimension" of variable a_i. Returns a tree relation that contains (dim(a_i),
	 * a_i) for all i\in{1,...,n}.
	 */
	private static TreeRelation<Integer, TermVariable> classifyEliminatees(final Collection<TermVariable> eliminatees) {
		final TreeRelation<Integer, TermVariable> tr = new TreeRelation<>();
		for (final TermVariable eliminatee : eliminatees) {
			final MultiDimensionalSort mds = new MultiDimensionalSort(eliminatee.getSort());
			tr.addPair(mds.getDimension(), eliminatee);
		}
		return tr;
	}


	private static boolean maxSizeIncrease(final TreeRelation<Integer, TermVariable> tr1, final TreeRelation<Integer, TermVariable> tr2) {
		if (tr2.isEmpty()) {
			return false;
		}
		final int tr1max = tr1.descendingDomain().first();
		final int tr2max = tr2.descendingDomain().first();
		final int max = Math.max(tr1max, tr2max);
		final Set<TermVariable> maxElemsTr1 = tr1.getImage(max);
		final Set<TermVariable> maxElemsTr2 = tr2.getImage(max);
		if (maxElemsTr1 == null) {
			assert maxElemsTr2 != null;
			return true;
		}
		if (maxElemsTr2 == null) {
			assert maxElemsTr1 != null;
			return false;
		}
		return maxElemsTr2.size() > maxElemsTr1.size();

	}

}
