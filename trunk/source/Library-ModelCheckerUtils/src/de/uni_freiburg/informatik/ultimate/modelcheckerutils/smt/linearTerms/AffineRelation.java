/*
 * Copyright (C) 2012-2015 University of Freiburg
 *
 * This file is part of the ULTIMATE Model Checker Utils Library.
 *
 * The ULTIMATE Model Checker Utils Library is free software: you can 
 * redistribute it and/or modify it under the terms of the GNU Lesser General 
 * Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 *
 * The ULTIMATE Model Checker Utils Library is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Model Checker Utils Library. If not,
 * see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Model Checker Utils Library, or any covered work, 
 * by linking or combining it with Eclipse RCP (or a modified version of
 * Eclipse RCP), containing parts covered by the terms of the Eclipse Public
 * License, the licensors of the ULTIMATE Model Checker Utils Library grant you
 * additional permission to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.Util;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.BinaryRelation.NoRelationOfThisKindException;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.BinaryRelation.RelationSymbol;

/**
 * Represents an term of the form ψ ▷ φ, where ψ and φ are affine terms and ▷ is
 * a binary relation symbol. Allows to return this relation as an SMT term in
 * the following two forms: - positive normal form - the form where a specific
 * variable is on the left hand side and all other summand are moved to the
 * right hand side.
 * 
 * @author Matthias Heizmann
 */
public class AffineRelation {
	private final Term m_OriginalTerm;
	private RelationSymbol m_RelationSymbol;
	/**
	 * Affine term ψ such that the relation ψ ▷ 0 is equivalent to the
	 * m_OriginalTerm.
	 * 
	 */
	private AffineTerm m_AffineTerm;
	
	public enum TransformInequality { NO_TRANFORMATION, STRICT2NONSTRICT, NONSTRICT2STRICT }
	
	public AffineRelation(Term term) throws NotAffineException {
		this(term, TransformInequality.NO_TRANFORMATION);
	}

	/**
	 * Transform Term into AffineRelation.
	 * @param term Term to which the resulting AffineRelation is equivalent.
	 * @param transformInequality transform strict inequalities to non-strict
	 * inequalities and vice versa
	 * @throws NotAffineException Thrown if Term is not affine.
	 */
	public AffineRelation(Term term, TransformInequality transformInequality) throws NotAffineException {
		m_OriginalTerm = term;
		BinaryNumericRelation bnr = null;
		try {
			bnr = new BinaryNumericRelation(term);
		} catch (NoRelationOfThisKindException e) {
			throw new NotAffineException("Relation is not affine");
		}
		
		Term lhs = bnr.getLhs();
		Term rhs = bnr.getRhs();
		AffineTerm affineLhs = (AffineTerm) (new AffineTermTransformer()).transform(lhs);
		AffineTerm affineRhs = (AffineTerm) (new AffineTermTransformer()).transform(rhs);
		AffineTerm difference;
		if (affineLhs.isErrorTerm() || affineRhs.isErrorTerm()) {
			throw new NotAffineException("Relation is not affine");
		} else {
			difference = new AffineTerm(affineLhs, new AffineTerm(affineRhs, Rational.MONE));
		}
		if (transformInequality != TransformInequality.NO_TRANFORMATION && 
				difference.getSort().getName().equals("Int")) {
			if (transformInequality == TransformInequality.STRICT2NONSTRICT ) {
				switch (bnr.getRelationSymbol()) {
				case DISTINCT:
				case EQ:
				case GEQ:
				case LEQ:
					// relation symbol is not strict anyway
					m_AffineTerm = difference; 
					m_RelationSymbol = bnr.getRelationSymbol();
					break;
				case LESS:
					// decrement affine term by one
					m_RelationSymbol = RelationSymbol.LEQ;
					m_AffineTerm = new AffineTerm(difference, 
							new AffineTerm(difference.getSort(), Rational.ONE));
					break;
				case GREATER:
					// increment affine term by one
					m_RelationSymbol = RelationSymbol.GEQ;
					m_AffineTerm = new AffineTerm(difference, 
							new AffineTerm(difference.getSort(), Rational.MONE));
					break;
				default:
					throw new AssertionError("unknown symbol");
				}
			} else if (transformInequality == TransformInequality.NONSTRICT2STRICT) {
				switch (bnr.getRelationSymbol()) {
				case DISTINCT:
				case EQ:
				case LESS:
				case GREATER:
					// relation symbol is strict anyway
					m_AffineTerm = difference; 
					m_RelationSymbol = bnr.getRelationSymbol();
					break;
				case GEQ:
					// decrement affine term by one
					m_RelationSymbol = RelationSymbol.GREATER;
					m_AffineTerm = new AffineTerm(difference, 
							new AffineTerm(difference.getSort(), Rational.ONE));
					break;
				case LEQ:
					// increment affine term by one
					m_RelationSymbol = RelationSymbol.LESS;
					m_AffineTerm = new AffineTerm(difference, 
							new AffineTerm(difference.getSort(), Rational.MONE));
					break;
				default:
					throw new AssertionError("unknown symbol");
				}
			} else {
				throw new AssertionError("unknown case");
			}
		} else {
			m_AffineTerm = difference; 
			m_RelationSymbol = bnr.getRelationSymbol();

		}
	}
	
	
	private void makeNonStrict() {
		if (!m_AffineTerm.getSort().getName().equals("Int")) {
			throw new UnsupportedOperationException("can only make Int terms non strict");
		}
		switch (m_RelationSymbol) {
		case DISTINCT:
		case EQ:
		case GEQ:
		case LEQ:
			throw new UnsupportedOperationException("can only make strict symbols non-strict");
		case LESS:
			// dencrement affine term by one
			m_RelationSymbol = RelationSymbol.LEQ;
			m_AffineTerm = new AffineTerm(m_AffineTerm, 
					new AffineTerm(m_AffineTerm.getSort(), Rational.MONE));
			break;
		case GREATER:
			// increment affine term by one
			m_RelationSymbol = RelationSymbol.GEQ;
			m_AffineTerm = new AffineTerm(m_AffineTerm, 
					new AffineTerm(m_AffineTerm.getSort(), Rational.ONE));
			break;
		default:
			throw new AssertionError("unknown symbol");
		}
	}

	/**
	 * Returns the name of the function symbol which is one of the following {=,
	 * <=, >=, <, >, distinct }.
	 * 
	 * @return
	 */
	public String getFunctionSymbolName() {
		return m_RelationSymbol.toString();
	}

	/**
	 * Return if term is variable (possibly with coefficient 0) in this affine
	 * relation.
	 */
	public boolean isVariable(Term term) {
		return m_AffineTerm.getVariable2Coefficient().containsKey(term);
	}

	/**
	 * Returns a term representation of this AffineRelation where each summand
	 * occurs only positive and the greater-than relation symbols are replaced
	 * by less-than relation symbols.
	 */
	public Term positiveNormalForm(Script script) {
		List<Term> lhsSummands = new ArrayList<Term>();
		List<Term> rhsSummands = new ArrayList<Term>();
		for (Entry<Term, Rational> entry : m_AffineTerm.getVariable2Coefficient().entrySet()) {
			if (entry.getValue().isNegative()) {
				rhsSummands.add(product(script, entry.getValue().abs(), entry.getKey()));
			} else {
				lhsSummands.add(product(script, entry.getValue(), entry.getKey()));
			}
		}
		if (m_AffineTerm.getConstant() != Rational.ZERO) {
			if (m_AffineTerm.getConstant().isNegative()) {
				rhsSummands.add(m_AffineTerm.getConstant().abs().toTerm(m_AffineTerm.getSort()));
			} else {
				lhsSummands.add(m_AffineTerm.getConstant().toTerm(m_AffineTerm.getSort()));
			}
		}
		Term lhsTerm = SmtUtils.sum(script, m_AffineTerm.getSort(), lhsSummands.toArray(new Term[lhsSummands.size()]));
		Term rhsTerm = SmtUtils.sum(script, m_AffineTerm.getSort(), rhsSummands.toArray(new Term[rhsSummands.size()]));
		Term result = BinaryRelation.constructLessNormalForm(script, m_RelationSymbol, lhsTerm, rhsTerm);
		result = BinaryRelation.constructLessNormalForm(script, m_RelationSymbol, lhsTerm, rhsTerm);
		assert isEquivalent(script, m_OriginalTerm, result) != LBool.SAT : "transformation to positive normal form unsound";
		return result;
	}

	/**
	 * Returns a term representation of this AffineRelation where the variable
	 * var (note that in our AffineTerms the variables may be SMT terms like
	 * e.g., a select term) is on the left hand side with coeffcient one. Throw
	 * a NotAffineException if no such representation is possible (e.g, if the
	 * variable does not occur in the term, or the variable is x, its sort is
	 * Int and the term is 2x=1.)
	 */
	public ApplicationTerm onLeftHandSideOnly(Script script, Term var) throws NotAffineException {
		assert m_AffineTerm.getVariable2Coefficient().containsKey(var);
		final Rational termsCoeff = m_AffineTerm.getVariable2Coefficient().get(var);
		if (termsCoeff.equals(Rational.ZERO)) {
			throw new NotAffineException("No affine representation " + "where desired variable is on left hand side");
		}
		List<Term> rhsSummands = new ArrayList<Term>(m_AffineTerm.getVariable2Coefficient().size());
		for (Entry<Term, Rational> entry : m_AffineTerm.getVariable2Coefficient().entrySet()) {
			if (var == entry.getKey()) {
				// do nothing
			} else {
				Rational newCoeff = entry.getValue().div(termsCoeff);
				if (newCoeff.isIntegral() || m_AffineTerm.getSort().getName().equals("Real")) {
					Rational negated = newCoeff.negate();
					rhsSummands.add(product(script, negated, entry.getKey()));
				} else {
					throw new NotAffineException("No affine representation "
							+ "where desired variable is on left hand side");
				}
			}
		}
		{
			Rational newConstant = m_AffineTerm.getConstant().div(termsCoeff);
			if (newConstant.isIntegral() || m_AffineTerm.getSort().getName().equals("Real")) {
				Rational negated = newConstant.negate();
				rhsSummands.add(negated.toTerm(m_AffineTerm.getSort()));
			} else {
				throw new NotAffineException("No affine representation "
						+ "where desired variable is on left hand side");
			}
		}
		Term rhsTerm = SmtUtils.sum(script, m_AffineTerm.getSort(), rhsSummands.toArray(new Term[rhsSummands.size()]));

		// if coefficient is negative we have to use the "swapped"
		// RelationSymbol
		boolean useRelationSymbolForSwappedTerms = termsCoeff.isNegative();
		RelationSymbol relSymb = useRelationSymbolForSwappedTerms ? BinaryRelation.swapParameters(m_RelationSymbol)
				: m_RelationSymbol;
		ApplicationTerm result = (ApplicationTerm) script.term(relSymb.toString(), var, rhsTerm);
		assert isEquivalent(script, m_OriginalTerm, result) == LBool.UNSAT : "transformation to AffineRelation unsound";
		return result;
	}

	private static LBool isEquivalent(Script script, Term term1, Term term2) {
		Term comp = script.term("=", term1, term2);
		comp = script.term("not", comp);
		LBool sat = Util.checkSat(script, comp);
		return sat;
	}

	private static Term product(Script script, Rational rational, Term term) {
		if (rational.equals(Rational.ONE)) {
			return term;
		} else if (rational.equals(Rational.MONE)) {
			return script.term("-", term);
		} else {
			return script.term("*", rational.toTerm(term.getSort()), term);
		}
	}
	


}
