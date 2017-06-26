/*
 * Copyright (C) 2017 Jonas Werner (jonaswerner95@gmail.com)
 *
 * This file is part of the ULTIMATE IcfgTransformer library.
 *
 * The ULTIMATE IcfgTransformer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE IcfgTransformer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE IcfgTransformer library. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE IcfgTransformer library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE IcfgTransformer grant you additional permission
 * to convey the resulting work.
 */

package de.uni_freiburg.informatik.ultimate.icfgtransformer.loopacceleration.werner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.ConstantTerm;
import de.uni_freiburg.informatik.ultimate.logic.QuantifiedFormula;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IIcfgSymbolTable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.Substitution;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.managedscript.ManagedScript;

/**
 * Iterated symbolic memory of a loop.
 *
 * @author Jonas Werner (jonaswerner95@gmail.com)
 *
 */
public class IteratedSymbolicMemory extends SymbolicMemory {

	private final Map<IProgramVar, Term> mIteratedMemory;
	private final TransFormula mFormula;
	private final Loop mLoop;
	private final List<TermVariable> mPathCounters;
	private final Map<TermVariable, TermVariable> mNewPathCounters;
	private Term mAbstractPathCondition;

	private enum mCaseType {
		NOT_CHANGED, ADDITION, SUBTRACTION, CONSTANT_ASSIGNMENT, CONSTANT_ASSIGNMENT_PATHCOUNTER
	}

	/**
	 * Construct new Iterated symbolic memory of a loop
	 *
	 * @param script
	 * @param logger
	 * @param tf
	 * @param oldSymbolTable
	 * @param loop
	 *            {@link Loop} whose iterated memory is computed
	 * @param pathCounters
	 *            list of pathcounters of the loops backbones
	 * @param newPathCounter
	 *            mapping of {@link TermVariable} to new Path Counter Tau
	 */
	public IteratedSymbolicMemory(final ManagedScript script, final ILogger logger, final TransFormula tf,
			final IIcfgSymbolTable oldSymbolTable, final Loop loop, final List<TermVariable> pathCounters,
			final Map<TermVariable, TermVariable> newPathCounter) {

		super(script, logger, tf, oldSymbolTable);
		mIteratedMemory = new HashMap<>();
		mPathCounters = pathCounters;
		mNewPathCounters = newPathCounter;
		mAbstractPathCondition = mScript.getScript().term("true");
		mFormula = tf;

		for (final Entry<IProgramVar, Term> entry : mMemoryMapping.entrySet()) {
			mIteratedMemory.put(entry.getKey(), null);
		}
		mLoop = loop;
		mLogger.debug("Iterated Memory: " + mIteratedMemory);
	}

	/**
	 * update the iterated memory by using the symbolic memories of the
	 * backbones.
	 */
	public void updateMemory() {

		for (final Entry<IProgramVar, Term> entry : mIteratedMemory.entrySet()) {

			final Term symbol = mMemoryMapping.get(entry.getKey());

			Term update = symbol;
			mCaseType caseType = mCaseType.NOT_CHANGED;
			mCaseType prevCase = mCaseType.NOT_CHANGED;

			for (final Backbone backbone : mLoop.getBackbones()) {

				// A new value can only be computed, iff the cases do not
				// change.
				if (!caseType.equals(prevCase) && !prevCase.equals(mCaseType.NOT_CHANGED)) {
					update = null;
					mLogger.debug("None of the cases applicable " + entry.getKey() + " value = unknown");
					break;
				}

				final Term memory = backbone.getSymbolicMemory().getValue(entry.getKey());

				// Case 1: variable not changed in backbone
				if (memory == null || memory.equals(symbol)) {
					continue;
				}

				mLogger.debug("SYMBOLIC MEMORY: " + mMemoryMapping);
				mLogger.debug("Symbol: " + mMemoryMapping.get(entry.getKey()));
				mLogger.debug("FORMULA: " + mFormula);
				mLogger.debug("ITERATED MEMORY: " + backbone.getSymbolicMemory().getMemory());
				mLogger.debug("BackMem: " + memory);

				// @Todo:
				if (memory instanceof TermVariable || memory instanceof ConstantTerm) {
					update = memory;
					continue;
				}

				// Case 2.1: if the variable is changed from its symbol by
				// adding
				// a constant for each backbone.
				if ("+".equals(((ApplicationTerm) memory).getFunction().getName())
						&& Arrays.asList(((ApplicationTerm) memory).getParameters()).contains(symbol)) {

					mLogger.debug("Addition");

					update = mScript.getScript().term("+", update, mScript.getScript().term("*",
							((ApplicationTerm) memory).getParameters()[1], backbone.getPathCounter()));

					prevCase = caseType;
					caseType = mCaseType.ADDITION;

				}

				// Case 2.2: if the variable is changed from its symbol by
				// subtracting
				// a constant for each backbone.
				if ("-".equals(((ApplicationTerm) memory).getFunction().getName())
						&& Arrays.asList(((ApplicationTerm) memory).getParameters()).contains(symbol)) {

					mLogger.debug("Subtraction");

					update = mScript.getScript().term("-", update, mScript.getScript().term("*",
							((ApplicationTerm) memory).getParameters()[1], backbone.getPathCounter()));

					prevCase = caseType;
					caseType = mCaseType.SUBTRACTION;

				}

				// Case 3:
				// in each backbone the variable is either not changed or set to
				// an expression,
				if (!Arrays.asList(((ApplicationTerm) memory).getParameters()).contains(symbol)) {

					update = memory;
					prevCase = caseType;
					caseType = mCaseType.CONSTANT_ASSIGNMENT;

					mLogger.debug("Assignment");
				}

				// TODO only allowed in one backbone
				if (!Arrays.asList(((ApplicationTerm) memory).getParameters()).contains(symbol) && Arrays
						.asList(((ApplicationTerm) memory).getParameters()).contains(backbone.getPathCounter())) {

					final Map<Term, Term> mapping = new HashMap<>();

					final Term newMapping = mScript.getScript().term("-", backbone.getPathCounter(),
							mScript.getScript().numeral("1"));
					mapping.put(backbone.getPathCounter(), newMapping);

					final Substitution sub = new Substitution(mScript, mapping);
					update = sub.transform(memory);
				}
			}
			mIteratedMemory.replace(entry.getKey(), update);
		}
		mLogger.debug("Iterated Memory: " + mIteratedMemory);
	}

	/**
	 * Compute the abstract condition using the {@link IteratedSymbolicMemory}
	 */
	public void updateCondition() {
		for (final Backbone backbone : mLoop.getBackbones()) {

			final List<TermVariable> freeVars = new ArrayList<>();
			List<Term> terms;

			for (final TermVariable var : backbone.getCondition().getFreeVars()) {
				if (mPathCounters.contains(var)) {
					freeVars.add(var);
				}
			}

			final Map<Term, Term> mapping = new HashMap<>();
			final ApplicationTerm appTerm = (ApplicationTerm) backbone.getCondition();

			mLogger.debug("APPTERM: " + appTerm);

			for (Term term : appTerm.getParameters()) {
				mapping.putAll(termUnravel(term));
			}

			mLogger.debug("MAPPING: " + mapping);

			Substitution sub = new Substitution(mScript, mapping);
			Term newCondition = sub.transform(appTerm);
			mapping.clear();

			// TODO more than one pathcounter in condition
			mapping.put(backbone.getPathCounter(), mNewPathCounters.get(backbone.getPathCounter()));

			sub = new Substitution(mScript, mapping);
			newCondition = sub.transform(newCondition);

			final List<TermVariable> tempPathCounters = mPathCounters;
			tempPathCounters.remove(backbone.getPathCounter());

			final List<TermVariable> tempNewPathCounters = new ArrayList<>(mNewPathCounters.values());
			tempNewPathCounters.remove(mNewPathCounters.get(backbone.getPathCounter()));

			final Term t1 = mScript.getScript().term("<", mNewPathCounters.get(backbone.getPathCounter()),
					backbone.getPathCounter());
			final Term t2 = mScript.getScript().term("<=", mScript.getScript().numeral("0"),
					mNewPathCounters.get(backbone.getPathCounter()));

			Term tFirstPart = mScript.getScript().term("and", t1, t2);

			Term tBackPart = mScript.getScript().term("true");

			for (TermVariable var : tempPathCounters) {
				tBackPart = mScript.getScript().term("and", tBackPart, mScript.getScript().term("<=",
						mScript.getScript().numeral("0"), mNewPathCounters.get(var), var));
			}

			Term tBackPartAddition = mScript.getScript().term("true");
			for (TermVariable var : freeVars) {
				tBackPartAddition = mScript.getScript().term("and", tBackPartAddition,
						mScript.getScript().term("<=", mScript.getScript().numeral("0"), var));
			}

			terms = Arrays.asList(tBackPartAddition, newCondition);
			tBackPart = SmtUtils.and(mScript.getScript(), terms);

			if (!freeVars.isEmpty()) {
				tBackPartAddition = mScript.getScript().quantifier(QuantifiedFormula.EXISTS,
						freeVars.toArray(new TermVariable[freeVars.size()]), tBackPartAddition);
			}

			terms = Arrays.asList(tBackPart, tBackPartAddition);
			tBackPart = SmtUtils.and(mScript.getScript(), terms);

			if (!tempNewPathCounters.isEmpty()) {
				tBackPart = mScript.getScript().quantifier(QuantifiedFormula.EXISTS,
						tempNewPathCounters.toArray(new TermVariable[tempNewPathCounters.size()]), tBackPart);
			}

			tFirstPart = mScript.getScript().term("=>", tFirstPart, tBackPart);

			mLogger.debug("TEST: t1:" + t1 + " t2: " + t2 + " tFirstpart: " + tFirstPart + " tBackPart: " + tBackPart
					+ " tBackPartAddtion: " + tBackPartAddition);

			final TermVariable[] vars = { mNewPathCounters.get(backbone.getPathCounter()) };
			final Term necessaryCondition = mScript.getScript().quantifier(QuantifiedFormula.FORALL, vars, tFirstPart);

			mLogger.debug("FINAL CONDITION: " + necessaryCondition);
			terms = Arrays.asList(mAbstractPathCondition, necessaryCondition);

			mAbstractPathCondition = SmtUtils.and(mScript.getScript(), terms);
		}
	}

	@Override
	protected Map<Term, Term> termUnravel(final Term term) {

		final Map<Term, Term> result = new HashMap<>();
		if (term instanceof TermVariable) {
			final TermVariable tv = (TermVariable) term;
			for (Entry<IProgramVar, Term> entry : mMemoryMapping.entrySet()) {
				if (tv.equals(entry.getValue())) {
					result.put(term, mIteratedMemory.get(entry.getKey()));
					break;
				}
			}

			return result;
		}

		if (term instanceof ConstantTerm) {
			return result;
		}

		final ApplicationTerm appTerm = (ApplicationTerm) term;
		for (Term subTerm : appTerm.getParameters()) {
			result.putAll(termUnravel(subTerm));
		}
		return result;
	}

	/**
	 * Get the iterated memory mapping
	 * 
	 * @return
	 */
	public Map<IProgramVar, Term> getIteratedMemory() {
		return mIteratedMemory;
	}

	/**
	 * Get the pathcounters of the {@link Backbone}s
	 * 
	 * @return
	 */
	public List<TermVariable> getPathCounters() {
		return mPathCounters;
	}

	public Term getAbstractCondition() {
		return mAbstractPathCondition;
	}
}
