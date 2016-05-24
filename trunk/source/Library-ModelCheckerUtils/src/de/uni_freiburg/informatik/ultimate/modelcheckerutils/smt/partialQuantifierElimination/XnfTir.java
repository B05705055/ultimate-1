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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.QuantifiedFormula;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.IFreshTermVariableConstructor;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.PartialQuantifierElimination;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.MultiDimensionalSelect;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.AffineRelation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.AffineRelation.TransformInequality;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.BinaryNumericRelation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.BinaryRelation.NoRelationOfThisKindException;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.NotAffineException;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.normalForms.Cnf;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.normalForms.Dnf;
import de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException;

/**
 * Transitive inequality resolution (TIR) for terms in XNF.
 * @author Matthias Heizmann
 */
public class XnfTir extends XjunctPartialQuantifierElimination {
	
	private final IFreshTermVariableConstructor mFreshTermVariableConstructor;

	public XnfTir(Script script, IUltimateServiceProvider services, 
			IFreshTermVariableConstructor freshTermVariableConstructor) {
		super(script, services);
		mFreshTermVariableConstructor = freshTermVariableConstructor;
	}

	@Override
	public String getName() {
		return "transitive inequality resolution";
	}

	@Override
	public String getAcronym() {
		return "TIR";
	}
	
	@Override
	public boolean resultIsXjunction() {
		return false;
	};

	
	public enum BoundType { UPPER, LOWER }

	@Override
	public Term[] tryToEliminate(int quantifier, Term[] inputConjuncts,
			Set<TermVariable> eliminatees) {
		final Term inputConjunction = PartialQuantifierElimination.composeXjunctsInner(mScript, quantifier, inputConjuncts);
		List<Term> currentDisjuncts = new ArrayList<Term>(Arrays.asList(inputConjunction));
		final Iterator<TermVariable> it = eliminatees.iterator();
		while (it.hasNext()) {
			List<Term> nextDisjuncts = new ArrayList<Term>();
			TermVariable eliminatee = it.next();
			if (!eliminatee.getSort().isNumericSort()) {
				// this technique is not applicable
				nextDisjuncts = currentDisjuncts;
				continue;
			}
			boolean unableToRemoveEliminatee = false;
			for (Term oldDisjunct : currentDisjuncts) {
				List<Term> elimResultDisjuncts = tryToEliminate_singleDisjuct(quantifier, oldDisjunct, eliminatee);
				if (elimResultDisjuncts == null) {
					// unable to eliminate
					unableToRemoveEliminatee = true;
					nextDisjuncts.add(oldDisjunct);
				} else {
					nextDisjuncts.addAll(elimResultDisjuncts);
				}
			}
			if (unableToRemoveEliminatee) {
				// not eliminated :-(
			} else {
				it.remove();
			}
			currentDisjuncts = nextDisjuncts;
		}
		final Term[] resultDisjuncts = currentDisjuncts.toArray(new Term[currentDisjuncts.size()]);
		final Term resultDisjunction =  PartialQuantifierElimination.composeXjunctsOuter(mScript, quantifier, resultDisjuncts);
		return new Term[] { resultDisjunction };
	}

	private List<Term> tryToEliminate_singleDisjuct(int quantifier, Term disjunct,
			TermVariable eliminatee) {
		Term[] conjuncts = PartialQuantifierElimination.getXjunctsInner(quantifier, disjunct);
		List<Term> result = tryToEliminate_Conjuncts(quantifier, conjuncts, eliminatee);
//		Following lines used for debugging - remove them
//		Term term = SmtUtils.or(mScript, (Collection<Term>) result);
//		term = SmtUtils.simplify(mScript, term, mServices);
//		result = Arrays.asList(PartialQuantifierElimination.getXjunctsOuter(quantifier, term));
//		
		return result;
	}
	
	private List<Term> tryToEliminate_Conjuncts(int quantifier, Term[] inputAtoms,
			TermVariable eliminatee) {
		List<Term> termsWithoutEliminatee = new ArrayList<Term>();
		List<Bound> upperBounds = new ArrayList<Bound>();
		List<Bound> lowerBounds = new ArrayList<Bound>();
		List<Term> antiDer = new ArrayList<Term>();

		
		for (Term term : inputAtoms) {
			if (!Arrays.asList(term.getFreeVars()).contains(eliminatee)) {
				termsWithoutEliminatee.add(term);
			} else {
				ApplicationTerm eliminateeOnLhs;
				AffineRelation rel;
				try {
					TransformInequality transform;
					if (quantifier == QuantifiedFormula.EXISTS) {
						transform = TransformInequality.STRICT2NONSTRICT;
					} else if (quantifier == QuantifiedFormula.FORALL) {
						transform = TransformInequality.NONSTRICT2STRICT;
					} else {
						throw new AssertionError("unknown quantifier");
					}
					 rel = new AffineRelation(mScript, term, transform);
				} catch (NotAffineException e) {
					// no chance to eliminate the variable
					return null;
				}
				if (!rel.isVariable(eliminatee)) {
					// eliminatee occurs probably only in select
					return null;
				}
				try {
					eliminateeOnLhs = rel.onLeftHandSideOnly(mScript, eliminatee);
				} catch (NotAffineException e) {
					// no chance to eliminate the variable
					return null;
				}
				if (!SmtUtils.occursAtMostAsLhs(eliminatee, eliminateeOnLhs)) {
					// eliminatee occurs additionally in rhs e.g., inside a
					// select or modulo term.
					return null;
				}
				try {
					BinaryNumericRelation bnr = new BinaryNumericRelation(eliminateeOnLhs);
					switch (bnr.getRelationSymbol()) {
					case DISTINCT:
						if (quantifier == QuantifiedFormula.EXISTS) {
							 antiDer.add(bnr.getRhs());
						} else {
							assert occursInsideSelectTerm(term, eliminatee) : "should have been removed by DER";
							// no chance to eliminate the variable
						}
						break;
					case EQ:
						if (quantifier == QuantifiedFormula.FORALL) {
							 antiDer.add(bnr.getRhs());
						} else {
							assert occursInsideSelectTerm(term, eliminatee) : "should have been removed by DER";
							// no chance to eliminate the variable
						}
						break;
					case GEQ:
						lowerBounds.add(new Bound(false, bnr.getRhs()));
						break;
					case GREATER:
						lowerBounds.add(new Bound(true, bnr.getRhs()));
						break;
					case LEQ:
						upperBounds.add(new Bound(false, bnr.getRhs()));
						break;
					case LESS:
						upperBounds.add(new Bound(true, bnr.getRhs()));
						break;
					default:
						throw new AssertionError();
					}
				} catch (NoRelationOfThisKindException e) {
					throw new AssertionError();
				}
			}
		}
		BuildingInstructions bi = new BuildingInstructions(quantifier,
				eliminatee.getSort(),
				termsWithoutEliminatee, 
				upperBounds, 
				lowerBounds, 
				antiDer);
		List<Term> resultAtoms = new ArrayList<Term>();
		for (Bound lowerBound : lowerBounds) {
			for (Bound upperBound : upperBounds) {
				resultAtoms.add(buildInequality(quantifier, lowerBound, upperBound));
			}
		}
		resultAtoms.addAll(termsWithoutEliminatee);
		final List<Term> resultDisjunctions;
		if (antiDer.isEmpty()) {
			resultDisjunctions = new ArrayList<Term>();
			Term tmp = PartialQuantifierElimination.composeXjunctsInner(mScript, quantifier, resultAtoms.toArray(new Term[resultAtoms.size()]));
			assert !Arrays.asList(tmp.getFreeVars()).contains(eliminatee) : "not eliminated";
			resultDisjunctions.add(tmp);
		} else {
			resultAtoms.add(bi.computeAdDisjunction());
			Term tmp = PartialQuantifierElimination.composeXjunctsInner(mScript, quantifier, resultAtoms.toArray(new Term[resultAtoms.size()]));
			Term disjunction;
			if (quantifier == QuantifiedFormula.EXISTS) {
				disjunction = (new Dnf(mScript, mServices, mFreshTermVariableConstructor)).transform(tmp);
			} else if (quantifier == QuantifiedFormula.FORALL) {
				disjunction = (new Cnf(mScript, mServices, mFreshTermVariableConstructor)).transform(tmp);
			} else {
				throw new AssertionError("unknown quantifier");
			}
			assert !Arrays.asList(disjunction.getFreeVars()).contains(eliminatee) : "not eliminated";
			resultDisjunctions = Arrays.asList(PartialQuantifierElimination.getXjunctsOuter(quantifier, disjunction));
		}
		return resultDisjunctions;
	}

	/**
	 * 	@return true iff tv is subterm of some select term in term. 
	 */
	private boolean occursInsideSelectTerm(Term term, TermVariable tv) {
		List<MultiDimensionalSelect> selectTerms = MultiDimensionalSelect.extractSelectShallow(term, true);
		for (MultiDimensionalSelect mds : selectTerms) {
			for (Term index : mds.getIndex()) {
				if (Arrays.asList(index.getFreeVars()).contains(tv)) {
					return true;
				}
			}
			if (Arrays.asList(mds.getSelectTerm().getFreeVars()).contains(tv)) {
				return true;
			}
			if (Arrays.asList(mds.getArray().getFreeVars()).contains(tv)) {
				return true;
			}
		}
		return false;
	}

	private Term buildInequality(String symbol, Term lhs, Term rhs) {
		Term term = mScript.term(symbol, lhs, rhs);
		AffineRelation rel;
		try {
			rel = new AffineRelation(mScript, term);
		} catch (NotAffineException e) {
			throw new AssertionError("should be affine");
		}
		return rel.positiveNormalForm(mScript);
	}
	
	private Term buildInequality(int quantifier, Bound lowerBound, Bound upperBound) {
		final boolean isStrict;
		if (quantifier == QuantifiedFormula.EXISTS) {
			isStrict = lowerBound.isIsStrict() || upperBound.isIsStrict();
			assert !(lowerBound.isIsStrict() && upperBound.isIsStrict()) || 
			!lowerBound.getTerm().getSort().getName().equals("Int") : "unsound if int and both are strict";
		} else if (quantifier == QuantifiedFormula.FORALL) {
			isStrict = lowerBound.isIsStrict() && upperBound.isIsStrict();
			assert !(!lowerBound.isIsStrict() && !upperBound.isIsStrict()) || 
			!lowerBound.getTerm().getSort().getName().equals("Int") : "unsound if int and both are non-strict";
		} else {
			throw new AssertionError("unknown quantifier");
		}
		String symbol = (isStrict ? "<" : "<=");
		Term term = mScript.term(symbol, lowerBound.getTerm(), upperBound.getTerm());
		AffineRelation rel;
		try {
			rel = new AffineRelation(mScript, term);
		} catch (NotAffineException e) {
			throw new AssertionError("should be affine");
		}
		return rel.positiveNormalForm(mScript);
	}


	private class BuildingInstructions {
		private final int mquantifier;
		private final Sort mSort;
		private final List<Term> mtermsWithoutEliminatee;
		private final List<Bound> mUpperBounds;
		private final List<Bound> mLowerBounds;
		private final List<Term> mantiDer;
		public BuildingInstructions(int quantifier, Sort sort,
				List<Term> termsWithoutEliminatee, List<Bound> upperBounds,
				List<Bound> lowerBounds, List<Term> antiDer) {
			super();
			mquantifier = quantifier;
			mSort = sort;
			mtermsWithoutEliminatee = termsWithoutEliminatee;
			mUpperBounds = upperBounds;
			mLowerBounds = lowerBounds;
			mantiDer = antiDer;
		}
		

		Term computeAdDisjunction() {
			ArrayList<Term> resultXJuncts = new ArrayList<Term>();
			for (int i=0; i<Math.pow(2,mantiDer.size()); i++) {
				ArrayList<Term> resultAtoms = new ArrayList<Term>();
				ArrayList<Bound> adLowerBounds = new ArrayList<Bound>();
				ArrayList<Bound> adUpperBounds = new ArrayList<Bound>();
				for (int k=0; k<mantiDer.size(); k++) {
					// zero means lower -  one means upper
					if (BigInteger.valueOf(i).testBit(k)) {
						Bound upperBound = computeBound(mantiDer.get(k), 
								mquantifier, BoundType.UPPER);
						adUpperBounds.add(upperBound);
					} else {
						Bound lowerBound = computeBound(mantiDer.get(k), 
								mquantifier, BoundType.LOWER);
						adLowerBounds.add(lowerBound);

					}
				}
				
				for (Bound adLower : adLowerBounds) {
					for (Bound adUpper : adUpperBounds) {
						resultAtoms.add(buildInequality(mquantifier, adLower, adUpper));
					}
					for (Bound upperBound : mUpperBounds) {
						resultAtoms.add(buildInequality(mquantifier, adLower, upperBound));
					}
				}
				for (Bound adUpper : adUpperBounds) {
					for (Bound lowerBound : mLowerBounds) {
						resultAtoms.add(buildInequality(mquantifier, lowerBound, adUpper));
					}
				}
				resultXJuncts.add(PartialQuantifierElimination.composeXjunctsInner(mScript, mquantifier, resultAtoms.toArray(new Term[resultAtoms.size()])));
				if (!mServices.getProgressMonitorService().continueProcessing()) {
					throw new ToolchainCanceledException(this.getClass(),
							"TIR is building " + Math.pow(2,mantiDer.size()) + " xjuncts");
				}
			}
			return PartialQuantifierElimination.composeXjunctsOuter(mScript, mquantifier, resultXJuncts.toArray(new Term[resultXJuncts.size()]));
		}

		private Bound computeBound(Term term,
				int quantifier, BoundType boundType) {
			final Bound result;
			if (term.getSort().getName().equals("Real")) {
				if (quantifier == QuantifiedFormula.EXISTS) {
					return new Bound(true, term); 
				} else if (quantifier == QuantifiedFormula.FORALL) {
					return new Bound(false, term);
				} else {
					throw new AssertionError("unknown quantifier");
				}
			} else if (term.getSort().getName().equals("Int")) {
				Term one = mScript.numeral(BigInteger.ONE);
				if (quantifier == QuantifiedFormula.EXISTS) {
					// transform terms such that
					//     lower < x /\ x < upper
					// becomes
					//     lower+1 <= x /\ x <= upper-1
					if (boundType == BoundType.LOWER) {
						result = new Bound(false, mScript.term("+", term, one));
					} else if (boundType == BoundType.UPPER) {
						result = new Bound(false, mScript.term("-", term, one));
					} else {
						throw new AssertionError("unknown BoundType" + boundType);
					}
				} else if (quantifier == QuantifiedFormula.FORALL) {
					// transform terms such that
					// lower <= x \/ x <= upper becomes
					// lower-1 < x \/ x < upper+1
					if (boundType == BoundType.LOWER) {
						result = new Bound(true, mScript.term("-", term, one));
					} else if (boundType == BoundType.UPPER) {
						result = new Bound(true, mScript.term("+", term, one));
					} else {
						throw new AssertionError("unknown BoundType" + boundType);
					}
				} else {
					throw new AssertionError("unknown quantifier");
				}
			} else {
				throw new AssertionError("unknown sort " + term.getSort());
			}
			return result;
		}


		private String computeRelationSymbol(int quantifier, Sort sort) {
			if (quantifier == QuantifiedFormula.FORALL) {
				return "<=";
			} else {
				switch (mSort.getName()) {
				case "Int":
					return "<=";
				case "Real":
					return "<";
				default:
					throw new UnsupportedOperationException("unknown Sort");
				}
			}
		}
		


		/**
		 * Add Term summand2 
		 * @param adUpperBounds
		 * @param term
		 * @return
		 */
		private ArrayList<Term> add(ArrayList<Term> terms, Term summand) {
			assert summand.getSort().getName().equals("Int");
			ArrayList<Term> result = new ArrayList<Term>();
			for (Term term : terms) {
				assert term.getSort().getName().equals("Int");
				result.add(mScript.term("+", term, summand));
			}
			return result;
		}

		
		
	}
	
	
	private static class Bound {
		private final boolean mIsStrict;
		private final Term mTerm;
		public Bound(boolean isStrict, Term term) {
			super();
			mIsStrict = isStrict;
			mTerm = term;
		}
		public boolean isIsStrict() {
			return mIsStrict;
		}
		public Term getTerm() {
			return mTerm;
		}
		@Override
		public String toString() {
			return "Bound [mIsStrict=" + mIsStrict + ", mTerm=" + mTerm
					+ "]";
		}
	}
	

	


}
