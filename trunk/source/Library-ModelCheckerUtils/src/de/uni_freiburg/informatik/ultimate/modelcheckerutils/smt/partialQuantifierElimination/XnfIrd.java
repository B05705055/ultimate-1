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
package de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.partialQuantifierElimination;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.QuantifiedFormula;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.AffineRelation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.NotAffineException;
import de.uni_freiburg.informatik.ultimate.util.DebugMessage;

public class XnfIrd extends XjunctPartialQuantifierElimination {

	public XnfIrd(Script script, IUltimateServiceProvider services) {
		super(script, services);
	}

	@Override
	public String getName() {
		return "infinity restrictor drop";
	}

	@Override
	public String getAcronym() {
		return "IRD";
	}
	
	@Override
	public boolean resultIsXjunction() {
		return true;
	};


	@Override
	public Term[] tryToEliminate(int quantifier, Term[] oldParams,
			Set<TermVariable> eliminatees) {
		Iterator<TermVariable> it = eliminatees.iterator();
		Term[] result = oldParams;
		while (it.hasNext()) {
			TermVariable tv = it.next();
			if (!SmtUtils.getFreeVars(Arrays.asList(result)).contains(tv)) {
				// case where var does not occur
				it.remove();
				continue;
			} else {
				if (tv.getSort().isNumericSort()) {
					Term[] withoutTv = irdSimple(m_Script, quantifier, result, tv, m_Logger);
					if (withoutTv != null) {
						m_Logger.debug(new DebugMessage("eliminated quantifier via IRD for {0}", tv));
						result = withoutTv;
						it.remove();
					} else {
						m_Logger.debug(new DebugMessage("not eliminated quantifier via IRD for {0}", tv));
					}
				} else {
					// ird is only applicable to variables of numeric sort
					m_Logger.debug(new DebugMessage("not eliminated quantifier via IRD for {0}", tv));
				}
			}
		}
		return result;
	}
	
	/**
	 * If the application term contains only parameters param such that for each
	 * param one of the following holds and the third case applies at most once,
	 * we return all params that do not contain tv. 1. param does not contain tv
	 * 2. param is an AffineRelation such that tv is a variable of the
	 * AffineRelation and the function symbol is "distinct" and quantifier is ∃
	 * or the function symbol is "=" and the quantifier is ∀ 3. param is an
	 * inequality
	 * 
	 * @param logger
	 */
	public static Term[] irdSimple(Script script, int quantifier, Term[] oldParams, TermVariable tv, ILogger logger) {
		assert tv.getSort().isNumericSort() : "only applicable for numeric sorts";

		ArrayList<Term> paramsWithoutTv = new ArrayList<Term>();
		short inequalitiesWithTv = 0;
		for (Term oldParam : oldParams) {
			if (!Arrays.asList(oldParam.getFreeVars()).contains(tv)) {
				paramsWithoutTv.add(oldParam);
			} else {
				AffineRelation affineRelation;
				try {
					affineRelation = new AffineRelation(script, oldParam);
				} catch (NotAffineException e) {
					// unable to eliminate quantifier
					return null;
				}
				if (!affineRelation.isVariable(tv)) {
					// unable to eliminate quantifier
					// tv occurs in affine relation but not as affine variable
					// it might occur inside a function or array.
					return null;
				}
				try {
					ApplicationTerm lhsonly = affineRelation.onLeftHandSideOnly(script, tv);
					if (!SmtUtils.occursAtMostAsLhs(tv, lhsonly)) {
						// eliminatee occurs additionally in rhs e.g., inside a
						// select or modulo term.
						return null;
					}

				} catch (NotAffineException e) {
					// unable to eliminate quantifier
					return null;
				}
				String functionSymbol = affineRelation.getFunctionSymbolName();
				switch (functionSymbol) {
				case "=":
					if (quantifier == QuantifiedFormula.EXISTS) {
						// unable to eliminate quantifier
						return null;
					} else if (quantifier == QuantifiedFormula.FORALL) {
						// we may drop this parameter
					} else {
						throw new AssertionError("unknown quantifier");
					}
					break;
				case "distinct":
					if (quantifier == QuantifiedFormula.EXISTS) {
						// we may drop this parameter
					} else if (quantifier == QuantifiedFormula.FORALL) {
						// unable to eliminate quantifier
						return null;
					} else {
						throw new AssertionError("unknown quantifier");
					}
					break;
				case ">":
				case ">=":
				case "<":
				case "<=":
					if (inequalitiesWithTv > 0) {
						// unable to eliminate quantifier, we may drop at most
						// one inequality
						return null;
					} else {
						inequalitiesWithTv++;
						// we may drop this parameter (but it has to be the
						// only dropped inequality
					}
					break;
				default:
					throw new AssertionError("unknown functionSymbol");
				}
			}
		}
		return paramsWithoutTv.toArray(new Term[paramsWithoutTv.size()]);
	}

}
