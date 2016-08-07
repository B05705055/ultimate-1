/*
 * Copyright (C) 2014-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 University of Freiburg
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
package de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramNonOldVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramOldVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SafeSubstitution;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.smtinterpol.util.DAGSize;

public class PredicateUtils {
	
	/**
	 * Returns the DAG size of the predicate's formula. 
	 * (DAG size means that similar sub-formulas are counted only once.)
	 */
	public static int computeDagSizeOfPredicate(final IPredicate p) {
		return (new DAGSize()).size(p.getFormula());
	}
	
	/**
	 * Computes DAG size for an array of predicates.
	 */
	public static int[] computeDagSizeOfPredicates(final List<IPredicate> predicates) {
		final int[] sizeOfPredicates = new int[predicates.size()];
		for (int i = 0; i < predicates.size(); i++) {
			sizeOfPredicates[i] = computeDagSizeOfPredicate(predicates.get(i));
		}
		return sizeOfPredicates;
	}
	

	public static Term computeClosedFormula(final Term formula, final Set<IProgramVar> boogieVars, final Script script) {
		final Map<Term, Term> substitutionMapping = new HashMap<Term, Term>();
		for (final IProgramVar bv : boogieVars) {
			substitutionMapping.put(bv.getTermVariable(), bv.getDefaultConstant());
		}
		final Term closedTerm = (new SafeSubstitution(script, substitutionMapping)).transform(formula);
		assert closedTerm.getFreeVars().length == 0;
		return closedTerm;
	}

	// public static LBool isInductiveHelper(Boogie2SMT boogie2smt,
	// IPredicate ps1, IPredicate ps2, TransFormula tf,
	// Set<BoogieVar> modifiableGlobalsPs1, Set<BoogieVar> modifiableGlobalsPs2) {
	// boogie2smt.getScript().push(1);
	// Map<String, Term> indexedConstants = new HashMap<String, Term>();
	// //OldVars not renamed if modifiable
	// //All variables get index 0
	// Term ps1renamed = formulaWithIndexedVars(ps1,new HashSet<BoogieVar>(0),
	// 4, 0, Integer.MIN_VALUE,null,-5,0, indexedConstants,
	// boogie2smt.getScript(), modifiableGlobalsPs1);
	//
	// Set<BoogieVar> assignedVars = new HashSet<BoogieVar>();
	// Term fTrans = formulaWithIndexedVars(tf, 0, 1, assignedVars, indexedConstants,
	// boogie2smt.getScript(), boogie2smt.getVariableManager());
	//
	// //OldVars not renamed if modifiable
	// //All variables get index 0
	// //assigned vars (locals and globals) get index 1
	// //other vars get index 0
	// Term ps2renamed = formulaWithIndexedVars(ps2, assignedVars,
	// 1, 0, Integer.MIN_VALUE,assignedVars,1,0, indexedConstants,
	// boogie2smt.getScript(), modifiableGlobalsPs2);
	//
	//
	// //We want to return true if (fState1 && fTrans)-> fState2 is valid
	// //This is the case if (fState1 && fTrans && !fState2) is unsatisfiable
	// Term f = SmtUtils.not(boogie2smt.getScript(),ps2renamed);
	// f = Util.and(boogie2smt.getScript(),fTrans,f);
	// f = Util.and(boogie2smt.getScript(),ps1renamed, f);
	//
	//// f = new FormulaUnLet().unlet(f);
	// boogie2smt.getScript().assertTerm(f);
	//
	// LBool result = boogie2smt.getScript().checkSat();
	//// LBool result = boogie2smt.getScript().checkSatisfiable(f);
	//
	// boogie2smt.getScript().pop(1);
	// return result;
	// }

	/**
	 * Return formula of a program state where all variables are renamed (substituted by a constant that has the new
	 * name) according to the following scheme:
	 * <ul>
	 * <li>Each variable v that is contained in varsWithSpecialIdx is renamed to v_specialIdx
	 * <li>Each variable v that is not contained in varsWithSpecialIdx is renamed to v_defaultIdx
	 * <li>If oldVarIdx is Integer.MIN_VALUE, each oldVar v is renamed to v_OLD
	 * <li>For oldvars we check if the the corresponding non-old var is in the set of modifiableGlobals. If NO, the
	 * oldvar becomes the same constant as the corresponding non-old var. if YES the following applies. If oldVarIdx is
	 * not Integer.MIN_VALUE, each oldVar v is renamed to v_oldVarIdx. This means v can get the same name as a
	 * non-oldVar! If oldVar is Integer.MIN_VALUE, we check
	 * </ul>
	 */
	public static Term formulaWithIndexedVars(final IPredicate ps, final Set<IProgramVar> varsWithSpecialIdx, final int specialIdx,
			final int defaultIdx, final int oldVarIdx, final Set<IProgramVar> globalsWithSpecialIdx, final int globSpecialIdx, final int globDefaultIdx,
			final Map<String, Term> indexedConstants, final Script script, final Set<IProgramVar> modifiableGlobals) {
		final Term psTerm = ps.getFormula();
		if (ps.getVars() == null) {
			return psTerm;
		}

		final Map<TermVariable, Term> substitution = new HashMap<TermVariable, Term>();

		for (final IProgramVar var : ps.getVars()) {
			Term cIndex;
			if (varsWithSpecialIdx.contains(var)) {
				cIndex = getIndexedConstant(var, specialIdx, indexedConstants, script);
			} else if (var.isOldvar()) {
				final IProgramNonOldVar bnov = ((IProgramOldVar) var).getNonOldVar();
				if (modifiableGlobals.contains(bnov)) {
					if (oldVarIdx == Integer.MIN_VALUE) {
						cIndex = var.getDefaultConstant();
					} else {
						cIndex = getIndexedConstant(((IProgramOldVar) var).getNonOldVar(), oldVarIdx, indexedConstants,
								script);
					}
				} else {
					cIndex = getIndexedConstant(bnov, globDefaultIdx, indexedConstants, script);
				}
			} else if (var.isGlobal()) {
				if (globalsWithSpecialIdx != null && globalsWithSpecialIdx.contains(var)) {
					cIndex = getIndexedConstant(var, globSpecialIdx, indexedConstants, script);
				} else {
					cIndex = getIndexedConstant(var, globDefaultIdx, indexedConstants, script);
				}
			} else {
				// var is local and not contained in specialIdx
				cIndex = getIndexedConstant(var, defaultIdx, indexedConstants, script);
			}
			final TermVariable c = var.getTermVariable();
			substitution.put(c, cIndex);
		}

		final TermVariable[] vars = new TermVariable[substitution.size()];
		final Term[] values = new Term[substitution.size()];
		int i = 0;
		for (final TermVariable var : substitution.keySet()) {
			vars[i] = var;
			values[i] = substitution.get(var);
			i++;
		}
		final Term result = script.let(vars, values, psTerm);
		return result;
	}

	/**
	 * Return formula of a transition where all variables are renamed (substituted by a constant that has the new name)
	 * according to the following scheme:
	 * <ul>
	 * <li>Each inVar v is renamed to v_idxInVar.
	 * <li>Each outVar v that does not occur as an inVar (no update/assignment) is renamed to v_idxOutVar.
	 * <li>Each variable v that occurs neither as inVar nor outVar is renamed to v_23.
	 * <li>Each oldVar v is renamed to v_OLD.
	 * </ul>
	 */
	public static Term formulaWithIndexedVars(final TransFormula tf, final int idxInVar, final int idxOutVar, final Set<IProgramVar> assignedVars,
			final Map<String, Term> indexedConstants, final Script script) {
		assert (assignedVars != null && assignedVars.isEmpty());
		final Set<TermVariable> notYetSubst = new HashSet<TermVariable>();
		notYetSubst.addAll(Arrays.asList(tf.getFormula().getFreeVars()));
		Term fTrans = tf.getFormula();
		String t = fTrans.toString();
		final Map<TermVariable, IProgramVar> reverseMapping = new HashMap<TermVariable, IProgramVar>();
		for (final IProgramVar inVar : tf.getInVars().keySet()) {
			final TermVariable tv = tf.getInVars().get(inVar);
			reverseMapping.put(tv, inVar);
			Term cIndex;
			if (inVar.isOldvar()) {
				cIndex = inVar.getDefaultConstant();
			} else {
				cIndex = getIndexedConstant(inVar, idxInVar, indexedConstants, script);
			}
			final TermVariable[] vars = { tv };
			final Term[] values = { cIndex };
			final Term undamagedFTrans = fTrans;
			fTrans = script.let(vars, values, fTrans);
			t = fTrans.toString();
			notYetSubst.remove(tv);
		}
		for (final IProgramVar outVar : tf.getOutVars().keySet()) {
			final TermVariable tv = tf.getOutVars().get(outVar);
			reverseMapping.put(tv, outVar);
			if (tf.getInVars().get(outVar) != tv) {
				assignedVars.add(outVar);
				Term cIndex;
				if (outVar.isOldvar() && !tf.getAssignedVars().contains(outVar)) {
					cIndex = outVar.getDefaultConstant();
				} else {
					cIndex = getIndexedConstant(outVar, idxOutVar, indexedConstants, script);
				}
				final TermVariable[] vars = { tv };
				final Term[] values = { cIndex };
				fTrans = script.let(vars, values, fTrans);
				t = fTrans.toString();
				notYetSubst.remove(tv);
			}
		}
		for (final TermVariable tv : notYetSubst) {
			final Term cIndex = reverseMapping.get(tv).getDefaultConstant();
			final TermVariable[] vars = { tv };
			final Term[] values = { cIndex };
			fTrans = script.let(vars, values, fTrans);
			t = fTrans.toString();
		}
		return fTrans;
	}

	public static Term getIndexedConstant(final IProgramVar bv, final int index, final Map<String, Term> indexedConstants, final Script script) {
		return getIndexedConstant(bv.getGloballyUniqueId(), bv.getTermVariable().getSort(), index, indexedConstants,
				script);
	}

	public static Term getIndexedConstant(final String id, final Sort sort, final int index, final Map<String, Term> indexedConstants,
			final Script script) {
		final String indexString = String.valueOf(index);
		final String name = id + "_" + indexString;
		Term constant = indexedConstants.get(name);
		if (constant == null) {
			final Sort[] emptySorts = {};
			script.declareFun(name, emptySorts, sort);
			final Term[] emptyTerms = {};
			constant = script.term(name, emptyTerms);
			indexedConstants.put(name, constant);
		}
		return constant;
	}

	public static LBool isInductiveHelper(final Script script, final IPredicate precond, final IPredicate postcond,
			final TransFormula tf, final Set<IProgramVar> modifiableGlobalsBefore, final Set<IProgramVar> modifiableGlobalsAfter) {
		script.push(1);

		final Set<IProgramVar> empty = Collections.emptySet();
		{
			final Set<IProgramNonOldVar> unprimedOldVarEqualities = new HashSet<>();
			final Set<IProgramNonOldVar> primedOldVarEqualities = new HashSet<>();

			findNonModifiablesGlobals(precond.getVars(), modifiableGlobalsBefore, empty, unprimedOldVarEqualities,
					primedOldVarEqualities);
			findNonModifiablesGlobals(tf.getInVars().keySet(), modifiableGlobalsBefore, empty, unprimedOldVarEqualities,
					primedOldVarEqualities);
			findNonModifiablesGlobals(tf.getOutVars().keySet(), modifiableGlobalsAfter, tf.getAssignedVars(),
					unprimedOldVarEqualities, primedOldVarEqualities);

			final List<Term> positiveConjuncts = new ArrayList<Term>();
			for (final IProgramNonOldVar bv : unprimedOldVarEqualities) {
				positiveConjuncts.add(ModifiableGlobalVariableManager.constructConstantOldVarEquality(bv, false,
						script));
			}
			for (final IProgramNonOldVar bv : primedOldVarEqualities) {
				positiveConjuncts.add(ModifiableGlobalVariableManager.constructConstantOldVarEquality(bv, true,
						script));
			}
			final Term tfRenamed = tf.getClosedFormula();
			assert tfRenamed != null;
			final Term precondRenamed = precond.getClosedFormula();
			assert precondRenamed != null;
			positiveConjuncts.add(precondRenamed);
			positiveConjuncts.add(tfRenamed);
			final Term positive = SmtUtils.and(script, positiveConjuncts);
			script.assertTerm(positive);
		}
		{
			final Set<IProgramNonOldVar> unprimedOldVarEqualities = new HashSet<>();
			final Set<IProgramNonOldVar> primedOldVarEqualities = new HashSet<>();
			final List<Term> negativeConjuncts = new ArrayList<Term>();
			findNonModifiablesGlobals(postcond.getVars(), modifiableGlobalsAfter, tf.getAssignedVars(),
					unprimedOldVarEqualities, primedOldVarEqualities);
			for (final IProgramNonOldVar bv : unprimedOldVarEqualities) {
				negativeConjuncts.add(ModifiableGlobalVariableManager.constructConstantOldVarEquality(bv, false,
						script));
			}
			for (final IProgramNonOldVar bv : primedOldVarEqualities) {
				negativeConjuncts.add(ModifiableGlobalVariableManager.constructConstantOldVarEquality(bv, true,
						script));
			}
			final Term postcondRenamed = rename(script, postcond, tf.getAssignedVars());
			negativeConjuncts.add(postcondRenamed);
			final Term negative = SmtUtils.and(script, negativeConjuncts);
			script.assertTerm(SmtUtils.not(script, negative));
		}
		final LBool result = script.checkSat();

		script.pop(1);
		return result;
	}

	/**
	 * Find all nonOldVars such that they are modifiable, their oldVar is in vars. Put the nonOldVar in
	 * nonModifiableGlobalsPrimed if the corresponding oldVar is in primedRequired.
	 * 
	 * @param vars
	 * @param modifiables
	 * @param primedRequired
	 * @param nonModifiableGlobalsUnprimed
	 * @param nonModifiableGlobalsPrimed
	 */
	private static void findNonModifiablesGlobals(final Set<IProgramVar> vars, final Set<IProgramVar> modifiables,
			final Set<IProgramVar> primedRequired, final Set<IProgramNonOldVar> nonModifiableGlobalsUnprimed,
			final Set<IProgramNonOldVar> nonModifiableGlobalsPrimed) {
		for (final IProgramVar bv : vars) {
			if (bv instanceof IProgramOldVar) {
				final IProgramNonOldVar nonOldVar = ((IProgramOldVar) bv).getNonOldVar();
				if (modifiables.contains(nonOldVar)) {
					// var modifiable, do nothing
				} else {
					if (primedRequired.contains(bv)) {
						nonModifiableGlobalsPrimed.add(nonOldVar);
					} else {
						nonModifiableGlobalsUnprimed.add(nonOldVar);
					}
				}
			}
		}
	}

	private static Term rename(final Script script, final IPredicate postcond, final Set<IProgramVar> assignedVars) {
		final Map<Term, Term> substitutionMapping = new HashMap<>();
		for (final IProgramVar bv : postcond.getVars()) {
			Term constant;
			if (assignedVars.contains(bv)) {
				constant = bv.getPrimedConstant();
			} else {
				constant = bv.getDefaultConstant();
			}
			substitutionMapping.put(bv.getTermVariable(), constant);
		}
		final Term result = (new SafeSubstitution(script, substitutionMapping)).transform(postcond.getFormula());
		assert result.getFreeVars().length == 0 : "there are free vars";
		return result;
	}
}
