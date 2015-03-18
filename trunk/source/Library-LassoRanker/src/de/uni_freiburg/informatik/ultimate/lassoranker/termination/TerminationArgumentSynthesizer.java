/*
 * Copyright (C) 2012-2014 University of Freiburg
 *
 * This file is part of the ULTIMATE LassoRanker Library.
 *
 * The ULTIMATE LassoRanker Library is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * The ULTIMATE LassoRanker Library is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE LassoRanker Library. If not,
 * see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE LassoRanker Library, or any covered work, by
 * linking or combining it with Eclipse RCP (or a modified version of
 * Eclipse RCP), containing parts covered by the terms of the Eclipse Public
 * License, the licensors of the ULTIMATE LassoRanker Library grant you
 * additional permission to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.lassoranker.termination;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lassoranker.AnalysisType;
import de.uni_freiburg.informatik.ultimate.lassoranker.ArgumentSynthesizer;
import de.uni_freiburg.informatik.ultimate.lassoranker.Lasso;
import de.uni_freiburg.informatik.ultimate.lassoranker.LassoRankerPreferences;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearInequality;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearInequality.PossibleMotzkinCoefficients;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearTransition;
import de.uni_freiburg.informatik.ultimate.lassoranker.SMTSolver;
import de.uni_freiburg.informatik.ultimate.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.RankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.RankingTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.RankVar;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.QuotedObject;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;

/**
 * This is the synthesizer that generates ranking functions.
 * 
 * @author Jan Leike
 */
public class TerminationArgumentSynthesizer extends ArgumentSynthesizer {
	/**
	 * The (local) settings for termination analysis
	 */
	private final TerminationAnalysisSettings m_settings;

	/**
	 * The template to be used
	 */
	private final RankingTemplate m_template;

	/**
	 * List of supporting invariant generators used by the last synthesize()
	 * call
	 */
	private final Collection<SupportingInvariantGenerator> m_si_generators;

	/**
	 * Number of Motzkin's Theorem applications used by the last synthesize()
	 * call
	 */
	private int m_num_motzkin = 0;

	// Objects resulting from the synthesis process
	private RankingFunction m_ranking_function = null;
	private Collection<SupportingInvariant> m_supporting_invariants = null;

	/**
	 * Set of terms in which RewriteArrays has put additional supporting
	 * invariants
	 */
	private final Set<Term> m_ArrayIndexSupportingInvariants;

	/**
	 * Constructor for the termination argument function synthesizer.
	 * 
	 * @param lasso
	 *            the lasso program
	 * @param template
	 *            the ranking function template to be used in the analysis
	 * @param preferences
	 *            arguments to the synthesis process
	 * @param settings
	 *            (local) settings for termination analysis
	 * @param arrayIndexSupportingInvariants
	 *            supporting invariants that were discovered during
	 *            preprocessing
	 * @param services
	 * @param storage
	 * @throws IOException 
	 */
	public TerminationArgumentSynthesizer(Lasso lasso, RankingTemplate template,
			LassoRankerPreferences preferences, TerminationAnalysisSettings settings,
			Set<Term> arrayIndexSupportingInvariants, IUltimateServiceProvider services, IToolchainStorage storage) throws IOException {
		super(lasso, preferences, template.getName() + "Template", services, storage);

		// Check the settings
		m_settings = new TerminationAnalysisSettings(settings); // defensive
																// copy
		m_settings.checkSanity();
		mLogger.info("Termination Analysis Settings:\n" + settings.toString());
		assert !m_settings.analysis.isDisabled();
		if (m_settings.num_strict_invariants == 0 && m_settings.num_non_strict_invariants == 0) {
			mLogger.info("Generation of supporting invariants is disabled.");
		}

		// Set logic
		if (m_settings.analysis.isLinear()) {
			m_script.setLogic(Logics.QF_LRA);
		} else {
			m_script.setLogic(Logics.QF_NRA);
		}
		if (m_settings.analysis == AnalysisType.Linear && !settings.nondecreasing_invariants) {
			mLogger.warn("Termination analysis type is 'Linear', " + "hence invariants must be non-decreasing!");
		}

		// Set other fields
		m_template = template;
		m_si_generators = new ArrayList<SupportingInvariantGenerator>();
		m_supporting_invariants = new ArrayList<SupportingInvariant>();
		m_ArrayIndexSupportingInvariants = arrayIndexSupportingInvariants;
	}

	/**
	 * @return RankVar's that are relevant for supporting invariants
	 */
	public Collection<RankVar> getSIVars() {
		/*
		 * Variables that occur as outVars of the stem but are not read by the
		 * loop (i.e., do not occur as inVar of the loop) are not relevant for
		 * supporting invariants.
		 */
		Collection<RankVar> result = new LinkedHashSet<RankVar>(m_lasso.getStem().getOutVars().keySet());
		result.retainAll(m_lasso.getLoop().getInVars().keySet());
		return result;
	}

	/**
	 * @return RankVar's that are relevant for ranking functions
	 */
	public Collection<RankVar> getRankVars() {
		Collection<RankVar> result = new LinkedHashSet<RankVar>(m_lasso.getLoop().getOutVars().keySet());
		result.retainAll(m_lasso.getLoop().getInVars().keySet());
		return result;
	}

	/**
	 * Use the ranking template to build the constraints whose solution gives
	 * the termination argument
	 * 
	 * @param template
	 *            the ranking function template
	 * @param si_generators
	 *            Output container for the used SI generators
	 * @return List of all conjuncts of the constraints
	 */
	private Collection<Term> buildConstraints(LinearTransition stem,
			LinearTransition loop, RankingTemplate template,
			Collection<SupportingInvariantGenerator> si_generators) {
		
		List<Term> conj = new ArrayList<Term>(); // List of constraints

		Collection<RankVar> siVars = getSIVars();
		List<List<LinearInequality>> templateConstraints =
				template.getConstraints(loop.getInVars(), loop.getOutVars());
		List<String> annotations = template.getAnnotations();
		assert annotations.size() == templateConstraints.size();

		mLogger.info(stem.getNumPolyhedra() + " stem disjuncts");
		mLogger.info(loop.getNumPolyhedra() + " loop disjuncts");
		mLogger.info(templateConstraints.size() + " template conjuncts.");

		// Negate template inequalities
		{
			Set<LinearInequality> negated = new HashSet<LinearInequality>();
			/*
			 * The same linear inequality may occur multiple times in the
			 * constraints if we use a composed template. That's why we have
			 * to make sure that we are not negating the same linear inequality
			 * twice.
			 * 
			 * Guess how much debugging it took me to find this error... :/
			 */
			for (List<LinearInequality> templateDisj : templateConstraints) {
				for (LinearInequality li : templateDisj) {
					if (!negated.contains(li)) {
						li.negate();
						negated.add(li);
					}
				}
			}
		}

		// Get guesses for loop eigenvalues as possible Motzkin coefficients
		Rational[] eigenvalue_guesses;
		if (m_settings.analysis.wantsGuesses()) {
			eigenvalue_guesses = m_lasso.guessEigenvalues(false);
			assert eigenvalue_guesses.length >= 2;
		} else {
			eigenvalue_guesses = new Rational[0];
		}

		// loop(x, x') /\ si(x) -> template(x, x')
		// Iterate over the loop conjunctions and template disjunctions
		int j = 0;
		for (List<LinearInequality> loopConj : loop.getPolyhedra()) {
			++j;
			for (int m = 0; m < templateConstraints.size(); ++m) {
				MotzkinTransformation motzkin = new MotzkinTransformation(m_script, m_settings.analysis,
						m_preferences.annotate_terms);
				motzkin.annotation = annotations.get(m) + " " + j;
				motzkin.add_inequalities(loopConj);
				motzkin.add_inequalities(templateConstraints.get(m));

				// Add supporting invariants
				assert (m_settings.num_strict_invariants >= 0);
				for (int i = 0; i < m_settings.num_strict_invariants; ++i) {
					SupportingInvariantGenerator sig = new SupportingInvariantGenerator(m_script, siVars, true);
					si_generators.add(sig);
					motzkin.add_inequality(sig.generate(loop.getInVars()));
				}
				assert (m_settings.num_non_strict_invariants >= 0);
				for (int i = 0; i < m_settings.num_non_strict_invariants; ++i) {
					SupportingInvariantGenerator sig = new SupportingInvariantGenerator(m_script, siVars, false);
					si_generators.add(sig);
					LinearInequality li = sig.generate(loop.getInVars());
					li.motzkin_coefficient = PossibleMotzkinCoefficients.ONE;
					motzkin.add_inequality(li);
				}
				mLogger.debug(motzkin);
				conj.add(motzkin.transform(eigenvalue_guesses));
			}
		}

		// Add constraints for the supporting invariants
		mLogger.debug("Adding the constraints for " + si_generators.size() + " supporting invariants.");
		int i = 0;
		for (SupportingInvariantGenerator sig : si_generators) {
			++i;

			// stem(x0) -> si(x0)
			j = 0;
			for (List<LinearInequality> stemConj : stem.getPolyhedra()) {
				++j;
				MotzkinTransformation motzkin = new MotzkinTransformation(m_script, m_settings.analysis,
						m_preferences.annotate_terms);
				motzkin.annotation = "invariant " + i + " initiation " + j;
				motzkin.add_inequalities(stemConj);
				LinearInequality li = sig.generate(stem.getOutVars());
				li.negate();
				li.motzkin_coefficient = PossibleMotzkinCoefficients.ONE;
				motzkin.add_inequality(li);
				conj.add(motzkin.transform(eigenvalue_guesses));
			}

			// si(x) /\ loop(x, x') -> si(x')
			j = 0;
			for (List<LinearInequality> loopConj : loop.getPolyhedra()) {
				++j;
				MotzkinTransformation motzkin = new MotzkinTransformation(m_script, m_settings.analysis,
						m_preferences.annotate_terms);
				motzkin.annotation = "invariant " + i + " consecution " + j;
				motzkin.add_inequalities(loopConj);
				motzkin.add_inequality(sig.generate(loop.getInVars())); // si(x)
				LinearInequality li = sig.generate(loop.getOutVars()); // ~si(x')
				li.motzkin_coefficient = m_settings.nondecreasing_invariants
						|| m_settings.analysis == AnalysisType.Linear ? PossibleMotzkinCoefficients.ZERO_AND_ONE
						: PossibleMotzkinCoefficients.ANYTHING;
				li.negate();
				motzkin.add_inequality(li);
				conj.add(motzkin.transform(eigenvalue_guesses));
			}
		}
		return conj;
	}

	/**
	 * Ranking function generation for lasso programs
	 * 
	 * This procedure is complete in the sense that if there is a linear ranking
	 * function that can be derived with a linear supporting invariant of the
	 * form si(x) >= 0, then it will be found by this procedure.
	 * 
	 * Iff a ranking function is found, this method returns true and the
	 * resulting ranking function and supporting invariant can be retrieved
	 * using the methods getRankingFunction() and getSupportingInvariant().
	 * 
	 * @param template
	 *            the ranking template to be used
	 * @return SAT if a termination argument was found, UNSAT if existence of a
	 *         termination argument was refuted, UNKNOWN if the solver was not
	 *         able to decide existence of a termination argument
	 * @throws SMTLIBException
	 *             error with the SMT solver
	 * @throws TermException
	 *             if the supplied transitions contain non-affine update
	 *             statements
	 * @throws IOException 
	 */
	@Override
	protected LBool do_synthesis() throws SMTLIBException, TermException, IOException {
		m_template.init(this);
		if (m_settings.analysis.isLinear() && m_template.getDegree() > 0) {
			mLogger.warn("Using a linear SMT query and a templates of degree "
					+ "> 0, hence this method is incomplete.");
		}
		Collection<RankVar> rankVars = getRankVars();
		Collection<RankVar> siVars = getSIVars();
		mLogger.info("Template has degree " + m_template.getDegree() + ".");
		mLogger.debug("Variables for ranking functions: " + rankVars);
		mLogger.debug("Variables for supporting invariants: " + siVars);
		
		LinearTransition stem = m_lasso.getStem();
		LinearTransition loop = m_lasso.getLoop();
		
		/*
		 * // The following code makes examples like StemUnsat.bpl fail if
		 * (siVars.isEmpty()) {
		 * s_Logger.info("There is no variables for invariants; " +
		 * "disabling supporting invariant generation.");
		 * m_preferences.num_strict_invariants = 0;
		 * m_preferences.num_non_strict_invariants = 0; }
		 */
		if (m_lasso.getStem().isTrue()) {
			mLogger.info("There is no stem transition; "
					+ "disabling supporting invariant generation.");
			m_settings.num_strict_invariants = 0;
			m_settings.num_non_strict_invariants = 0;
		} else if (m_settings.overapproximate_stem) {
			mLogger.info("Overapproximating stem...");
			StemOverapproximator so = new StemOverapproximator(m_preferences,
					m_services, m_storage);
			int stem_atoms = stem.getNumInequalities();
			stem = so.overapproximate(stem);
			mLogger.info("Reduced " + stem_atoms + " stem atoms to "
					+ stem.getNumInequalities() + ".");
		}

		// Assert all conjuncts generated from the template
		Collection<Term> constraints = buildConstraints(stem, loop, m_template,
				m_si_generators);
		m_num_motzkin = constraints.size();
		mLogger.info("We have " + getNumMotzkin() + " Motzkin's Theorem applications.");
		mLogger.info("A total of " + getNumSIs() + " supporting invariants were added.");
		for (Term constraint : constraints) {
			m_script.assertTerm(constraint);
		}

		// Check for a model
		LBool sat = m_script.checkSat();
		if (sat == LBool.SAT) {
			// Get all relevant variables
			ArrayList<Term> variables = new ArrayList<Term>();
			variables.addAll(m_template.getVariables());
			for (SupportingInvariantGenerator sig : m_si_generators) {
				variables.addAll(sig.getVariables());
			}

			// Get valuation for the variables
			Map<Term, Rational> val;
			if (m_settings.simplify_termination_argument) {
				mLogger.info("Found a termination argument, trying to simplify.");
				val = getSimplifiedAssignment(variables);
			} else {
				val = getValuation(variables);
			}

			// Extract ranking function and supporting invariants
			m_ranking_function = m_template.extractRankingFunction(val);
			for (SupportingInvariantGenerator sig : m_si_generators) {
				m_supporting_invariants.add(sig.extractSupportingInvariant(val));
			}
			
			// Simplify supporting invariants
			if (m_settings.simplify_supporting_invariants) {
				SupportingInvariantSimplifier tas =
						new SupportingInvariantSimplifier(m_preferences,
								m_services, m_storage);
				mLogger.info("Simplifying supporting invariants...");
				int before = m_supporting_invariants.size();
				m_supporting_invariants = tas.simplify(m_supporting_invariants);
				mLogger.info("Removed " + (before - m_supporting_invariants.size())
						+ " redundant supporting invariants from a total of "
						+ before + ".");
			}
		} else if (sat == LBool.UNKNOWN) {
			m_script.echo(new QuotedObject(ArgumentSynthesizer.s_SolverUnknownMessage));
			// Problem: If we use the following line we can receive the
			// following response which is not SMTLIB2 compliant.
			// (:reason-unknown canceled)
			// Object reason = m_script.getInfo(":reason-unknown");
			// TODO: discuss the above claim with Jürgen
		}
		return sat;
	}

	/**
	 * @return the number of supporting invariants used
	 */
	public int getNumSIs() {
		assert m_si_generators != null;
		return m_si_generators.size();
	}

	/**
	 * @return the number of Motzkin's Theorem applications
	 */
	public int getNumMotzkin() {
		return m_num_motzkin;
	}

	/**
	 * @return the synthesized TerminationArgument
	 */
	public TerminationArgument getArgument() {
		assert synthesisSuccessful();
		return new TerminationArgument(m_ranking_function, m_supporting_invariants, m_ArrayIndexSupportingInvariants);
	}
}
