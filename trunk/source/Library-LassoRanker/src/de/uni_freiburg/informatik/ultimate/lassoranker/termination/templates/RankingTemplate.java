/*
 * Copyright (C) 2014-2015 Jan Leike (leike@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE LassoRanker Library.
 * 
 * The ULTIMATE LassoRanker Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE LassoRanker Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE LassoRanker Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE LassoRanker Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE LassoRanker Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.lassoranker.InstanceCounting;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearInequality;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgumentSynthesizer;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.RankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.RankVar;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;


/**
 * This is the superclass for all linear ranking templates.
 * 
 * @author Jan Leike
 *
 */
public abstract class RankingTemplate extends InstanceCounting {
	/**
	 * Fix Motzkin coefficients of the red atoms?
	 */
	public static final boolean sRedAtoms = true;
	
	/**
	 * Fix Motzkin coefficients of the blue atoms?
	 */
	public static final boolean sBlueAtoms = true;
	
	protected Script mscript = null;
	protected TerminationArgumentSynthesizer mtas = null;
	protected Collection<RankVar> mvariables = null;
	
	private boolean minitialized = false;
	
	/**
	 * Initialize the template and affiliate this template with a particular
	 * TerminationArgumentSynthesizer.
	 * 
	 * Call this before getConstaints()
	 * 
	 * @param tas the parent TerminationArgumentSynthesizer
	 */
	public final void init(TerminationArgumentSynthesizer tas) {
		mtas = tas;
		mscript = tas.getScript();
		mvariables = tas.getRankVars();
		_init();
		minitialized = true;
	}
	
	/**
	 * Init method to be overwritten by the children
	 */
	protected abstract void _init();
	
	/**
	 * Check if the template was properly initialized using init()
	 */
	protected void checkInitialized() {
		assert(minitialized);
		assert(mtas != null);
		assert(mvariables != null);
	}
	
	
	/**
	 * Returns the name of the template (e.g., affine, 2-phase, or 3-nested)
	 * 
	 */
	public abstract String getName();
	
	/**
	 * Generate the constraints in form of linear inequalities in CNF
	 * 
	 * Must be initialized before calling this!
	 * 
	 * @param inVars  Input variables for the loop transition
	 * @param outVars Output variables for the loop transition
	 * @return FarkasApplications in CNF; one clause for every conjunct in this
	 *          template's formula. These Applictions will be augmented by
	 *          the loop transition in form of affine terms and the supporting
	 *          invariants.
	 */
	public abstract List<List<LinearInequality>> getConstraints(
			Map<RankVar, Term> inVars, Map<RankVar, Term> outVars);
	
	/**
	 * Returns a string for every constraint conjunct for annotating
	 * MotzkinTransformation instances.
	 * 
	 * The returned list should have exactly as many elements as the list
	 * returned by constraints()
	 * 
	 * @return a list of annotations
	 */
	public abstract List<String> getAnnotations();
	
	/**
	 * Return all SMT variables used by this template
	 */
	public abstract Collection<Term> getVariables();
	
	/**
	 * Returns the degree of the template, i.e, the number of Motzkin
	 * coefficients occurring in non-linear operation in the generated
	 * constraints
	 * @return degree of the template
	 */
	public abstract int getDegree();
	
	/**
	 * Extract the ranking function from a model
	 * @return ranking function
	 * @throws SMTLIBException
	 */
	public abstract RankingFunction extractRankingFunction(Map<Term,
			Rational> val) throws SMTLIBException;
	
	/**
	 * Create a new positive variable (as a nullary function symbol)
	 * @param name the new variable's name
	 * @return the new variable as a term
	 */
	protected Term newDelta(String name) {
		Term delta = mtas.newConstant(name, "Real");
		Term t = mscript.term(">", delta, mscript.decimal("0"));
		mscript.assertTerm(t);
		return delta;
	}
}
