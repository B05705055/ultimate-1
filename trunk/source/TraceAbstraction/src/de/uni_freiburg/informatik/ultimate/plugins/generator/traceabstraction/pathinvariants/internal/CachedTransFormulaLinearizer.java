/*
 * Copyright (C) 2015 Dirk Steinmetz
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
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.pathinvariants.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearTransition;
import de.uni_freiburg.informatik.ultimate.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.AddAxioms;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.DNF;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.MatchInOutVars;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.RemoveNegation;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.RewriteBooleans;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.RewriteDivision;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.RewriteEquality;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.RewriteIte;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.RewriteStrictInequalities;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.RewriteTrueFalse;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.RewriteUserDefinedTypes;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.SimplifyPreprocessor;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.TransitionPreprocessor;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.InequalityConverter.NlaHandling;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.ReplacementVar;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.ReplacementVarFactory;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.TransFormulaLR;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils.SimplicationTechnique;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils.XnfConversionTechnique;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.managedscript.ManagedScript;

/**
 * Class linearizing {@link TransFormula}s. For improved performance and
 * variable management, this class keeps a cache of linearization results. Thus,
 * this class should only be used in one single context at a time, to ensure
 * proper garbage collection.
 * 
 * @author (mostly) Matthias Heizmann
 */
public class CachedTransFormulaLinearizer {

	private final IUltimateServiceProvider mServices;
	private final IToolchainStorage mStorage;
	private final SimplicationTechnique mSimplificationTechnique;
	private final XnfConversionTechnique mXnfConversionTechnique;
	private final Term[] mAxioms;
	private final ReplacementVarFactory mReplacementVarFactory;
	private final ManagedScript mPredicateScript;
	private final Map<TransFormula, LinearTransition> mCache;


	/**
	 * Constructs a cached TransFormula linearizer.
	 * 
	 * @param services
	 *            Service provider to use
	 * @param storage
	 *            Toolchain storage, e.g., needed for construction of new solver. 
	 * @param smtManager
	 *            SMT manager
	 * @author Matthias Heizmann
	 */
	public CachedTransFormulaLinearizer(final IUltimateServiceProvider services,
			final ManagedScript smtManager, final Collection<Term> axioms, final IToolchainStorage storage, 
			final SimplicationTechnique simplificationTechnique, final XnfConversionTechnique xnfConversionTechnique) {
		super();
		mServices = services;
		mStorage = storage;
		mSimplificationTechnique = simplificationTechnique;
		mXnfConversionTechnique = xnfConversionTechnique;
		mPredicateScript = smtManager;
		mReplacementVarFactory = new ReplacementVarFactory(mPredicateScript);
		mAxioms = axioms.toArray(new Term[axioms.size()]);

		mCache = new HashMap<TransFormula, LinearTransition>();
	}

	/**
	 * Performs a transformation, utilizing the cache if possible. If the given
	 * {@link TransFormula} has not yet been linearized, the result will also
	 * get added to the cache.
	 * 
	 * The input and the output of this transformation are related as follows.
	 * Let the input be a {@link TransFormula} that represents a formula φ whose
	 * free variables are primed and unprimed versions of the {@link BoogieVars}
	 * x_1,...,x_n. The output is a {@link LinearTransition} that represent a
	 * formula ψ whose free variables are primed and unprimed versions of
	 * x_1,...,x_n and additionally primed and unprimed versions of a set
	 * {@link ReplacementVar}s y_1,...,y_m. If we replace for each
	 * {@link ReplacementVar} the corresponding primed and unprimed variables by
	 * primed and unprimed versions of the {@link ReplacementVar}'s definition
	 * the the resulting formula is logically equivalent to the formula φ.
	 * 
	 * @param tf
	 *            transformula to transform
	 * @return transformed transformula
	 */
	public LinearTransition linearize(final TransFormula tf) {
		LinearTransition result = mCache.get(tf);
		if (result == null) {
			result = makeLinear(tf);
			mCache.put(tf, result);
		}
		return result;
	}

	/**
	 * Performs a transformation.
	 * 
	 * The input and the output of this transformation are related as follows.
	 * Let the input be a {@link TransFormula} that represents a formula φ whose
	 * free variables are primed and unprimed versions of the {@link BoogieVars}
	 * x_1,...,x_n. The output is a {@link LinearTransition} that represent a
	 * formula ψ whose free variables are primed and unprimed versions of
	 * x_1,...,x_n and additionally primed and unprimed versions of a set
	 * {@link ReplacementVar}s y_1,...,y_m. If we replace for each
	 * {@link ReplacementVar} the corresponding primed and unprimed variables by
	 * primed and unprimed versions of the {@link ReplacementVar}'s definition
	 * the the resulting formula is logically equivalent to the formula φ.
	 * 
	 * @author Matthias Heizmann
	 * @param tf
	 *            transformula to transform
	 * @return transformed transformula
	 */
	private LinearTransition makeLinear(final TransFormula tf) {
		TransFormulaLR tflr = TransFormulaLR.buildTransFormula(tf,
				mReplacementVarFactory, mPredicateScript);

		for (final TransitionPreprocessor tpp : getPreprocessors()) {
			try {
				tflr = tpp.process(mPredicateScript.getScript(), tflr);
			} catch (final TermException e) {
				throw new RuntimeException(e);
			}
		}
		LinearTransition lt;
		try {
			lt = LinearTransition.fromTransFormulaLR(tflr, NlaHandling.EXCEPTION);
		} catch (final TermException e) {
			throw new RuntimeException(e);
		}
		return lt;
	}

	/**
	 * (Undocumented Method, do not touch)
	 * 
	 * @author Matthias Heizmann
	 */
	private TransitionPreprocessor[] getPreprocessors() {
		return new TransitionPreprocessor[] {
				new MatchInOutVars(mPredicateScript),
				new AddAxioms(mReplacementVarFactory, mAxioms),
				new RewriteDivision(mReplacementVarFactory),
				new RewriteBooleans(mReplacementVarFactory, mPredicateScript), 
				new RewriteIte(mPredicateScript),
				new RewriteUserDefinedTypes(mReplacementVarFactory, mPredicateScript),
				new RewriteEquality(), 
				new SimplifyPreprocessor(mServices, mStorage, mPredicateScript, mSimplificationTechnique),
				new DNF(mServices, mPredicateScript, mXnfConversionTechnique), 
				new SimplifyPreprocessor(mServices, mStorage, mPredicateScript, mSimplificationTechnique),
				new RewriteTrueFalse(), 
				new RemoveNegation(),
				new RewriteStrictInequalities(), };
	}

}
