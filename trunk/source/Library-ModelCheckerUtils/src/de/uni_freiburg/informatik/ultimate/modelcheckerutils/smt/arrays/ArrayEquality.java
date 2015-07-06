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
package de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays;

import java.util.ArrayList;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;

/**
 * Wrapper for an equality of the form a1=a2, where a1 and a2 are TermVariables.
 * In the future we may extend in a way that a1 and a2 can be constants (i.e.
 * 0-ary function symbols).
 * @author Matthias Heizmann
 */
public class ArrayEquality {
	private final Term m_OriginalTerm;
	private final TermVariable lhs;
	private final TermVariable rhs;

	public ArrayEquality(Term term) throws ArrayEqualityException {
		if (!(term instanceof ApplicationTerm)) {
			throw new ArrayEqualityException("no ApplicationTerm");
		}
		ApplicationTerm eqAppTerm = (ApplicationTerm) term;
		if (!eqAppTerm.getFunction().getName().equals("=")) {
			throw new ArrayEqualityException("no equality");
		}
		if (!(eqAppTerm.getParameters().length == 2)) {
			throw new ArrayEqualityException("no binary equality");
		}
		m_OriginalTerm = term;
		Term lhsTerm = eqAppTerm.getParameters()[0];
		Term rhsTerm = eqAppTerm.getParameters()[1];
		if (!(lhsTerm.getSort().isArraySort())) {
			throw new ArrayEqualityException("no array");
		}

		if (lhsTerm instanceof TermVariable) {
			lhs = (TermVariable) lhsTerm;
		} else {
			throw new ArrayEqualityException("no tv");
		}

		if (rhsTerm instanceof TermVariable) {
			rhs = (TermVariable) rhsTerm;
		} else {
			throw new ArrayEqualityException("no tv");
		}
	}

	public Term getOriginalTerm() {
		return m_OriginalTerm;
	}

	public TermVariable getLhs() {
		return lhs;
	}

	public TermVariable getRhs() {
		return rhs;
	}

	@Override
	public String toString() {
		return m_OriginalTerm.toString();
	}
	
	private class ArrayEqualityException extends Exception {

		private static final long serialVersionUID = -5344050289008681972L;

		public ArrayEqualityException(String message) {
			super(message);
		}
	}
	
	/**
	 * Given an array of terms, partition them into terms that are array
	 * equalities and terms that are not array equalities.
	 */
	public static class ArrayEqualityExtractor {
		private final List<ArrayEquality> m_ArrayEqualities = new ArrayList<ArrayEquality>();
		private final List<Term> remainingTerms = new ArrayList<Term>();

		public ArrayEqualityExtractor(Term[] terms) {
			for (Term term : terms) {
				ArrayEquality au;
				try {
					au = new ArrayEquality(term);
				} catch (ArrayEqualityException e) {
					au = null;
				}
				if (au == null) {
					remainingTerms.add(term);
				} else {
					m_ArrayEqualities.add(au);
				}
			}
		}

		public List<ArrayEquality> getArrayEqualities() {
			return m_ArrayEqualities;
		}

		public List<Term> getRemainingTerms() {
			return remainingTerms;
		}
	}

}