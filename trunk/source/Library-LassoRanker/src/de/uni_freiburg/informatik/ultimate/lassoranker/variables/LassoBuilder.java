/*
 * Copyright (C) 2014-2015 Jan Leike (leike@informatik.uni-freiburg.de)
 * Copyright (C) 2014-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.lassoranker.variables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.lassoranker.Lasso;
import de.uni_freiburg.informatik.ultimate.lassoranker.LassoAnalysis.PreprocessingBenchmark;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearTransition;
import de.uni_freiburg.informatik.ultimate.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.LassoPreprocessor;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.InequalityConverter.NlaHandling;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Boogie2SMT;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula;
import de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException;


/**
 * 
 * The LassoBuilder class holds the lasso components during preprocessing.
 * 
 * This object is *not* immutable.
 * 
 * @author Jan Leike 
 * @author Matthias Heizmann
 */
public class LassoBuilder {
	/**
	 * The Boogie2SMT object
	 */
	private final Boogie2SMT mboogie2smt;
	
	/**
	 * Collection of all generated replacement TermVariables
	 */
	private final Collection<TermVariable> mtermVariables;

	/**
	 * Conjunctive representation of the lassos during the preprocessing.
	 */
	private List<LassoUnderConstruction> mLassosUC;
	
	private Collection<Lasso> mLassos;
	
	
	/**
	 * The script used to create terms in the transition formulas
	 */
	private final Script mScript;
	
	/**
	 * Object that has to be used for getting and constructing ReplacementVars
	 * that occur in this LassoBuilder.
	 */
	private final ReplacementVarFactory mReplacementVarFactory;

	private PreprocessingBenchmark mPreprocessingBenchmark;

	private final ILogger mLogger;

	private final NlaHandling mNlaHandling;
	
	/**
	 * Create a new LassoBuilder object from components
	 * 
	 * @param script the script that created the transition formulae
	 * @param boogie2smt the boogie smt translator
	 * @param stem the stem transition
	 * @param loop the loop transition
	 */
	public LassoBuilder(ILogger logger, Script script, Boogie2SMT boogie2smt, TransFormula stem,
			TransFormula loop, NlaHandling nlaHandling) {
		assert script != null;
		assert boogie2smt != null;
		mLogger = logger;
		mScript = script;
		mboogie2smt = boogie2smt;
		mNlaHandling = nlaHandling;
		mtermVariables = new ArrayList<TermVariable>();
		
		mReplacementVarFactory =
				new ReplacementVarFactory(mboogie2smt.getVariableManager());
		
		mLassosUC = new ArrayList<>();
		mLassosUC.add(new LassoUnderConstruction(
				TransFormulaLR.buildTransFormula(stem, mReplacementVarFactory),
				TransFormulaLR.buildTransFormula(loop, mReplacementVarFactory)
			));
	}
	
	/**
	 * @return the script used to generate the transition formulas
	 */
	public Script getScript() {
		return mScript;
	}
	
	/**
	 * @return the associated Boogie2SMT object
	 */
	public Boogie2SMT getBoogie2SMT() {
		return mboogie2smt;
	}
	
	public ReplacementVarFactory getReplacementVarFactory() {
		return mReplacementVarFactory;
	}

	/**
	 * @return a collection of all new TermVariable's created with this object
	 */
	public Collection<TermVariable> getGeneratedTermVariables() {
		return Collections.unmodifiableCollection(mtermVariables);
	}
	
//	/**
//	 * Is the stem the same for termination analysis and nontermination analysis?
//	 * @return whether getStemComponentsTermination() == getStemComponentsNonTermination()
//	 */
//	public boolean isStemApproximated() {
//		return mstem_components_t != mstem_components_nt;
//	}
//	
//	/**
//	 * Is the loop the same for termination analysis and nontermination analysis?
//	 * @return whether getLoopComponentsTermination() == getLoopComponentsNonTermination()
//	 */
//	public boolean isLoopApproximated() {
//		return mloop_components_t != mloop_components_nt;
//	}
	
	/**
	 * @return the conjunction of lassos
	 */
	public List<LassoUnderConstruction> getLassosUC() {
		return mLassosUC;
	}
	
	
	public void applyPreprocessor(LassoPreprocessor preprocessor) throws TermException {
		final ArrayList<LassoUnderConstruction> newLassos = new ArrayList<LassoUnderConstruction>();
		for (final LassoUnderConstruction lasso : mLassosUC) {
			try {
				newLassos.addAll(preprocessor.process(lasso));
			} catch (final ToolchainCanceledException tce) {
				String taskMessage = "applying " + preprocessor.getName() + " to lasso for termination ";
				if (tce.getRunningTaskInfo() != null) {
					taskMessage += tce.getRunningTaskInfo();
				}
				throw new ToolchainCanceledException(getClass(), taskMessage);
			}
		}
		mLassosUC = newLassos;
	}
	
	
	/**
	 * Run a few sanity checks
	 * @return false if something is fishy
	 */
	public boolean isSane() {
		boolean sane = true;
		for (final LassoUnderConstruction luc : mLassosUC) {
			sane &= luc.getStem().auxVarsDisjointFromInOutVars();
			sane &= luc.getStem().allAreInOutAux(luc.getStem().getFormula().getFreeVars()) == null;

			sane &= luc.getLoop().auxVarsDisjointFromInOutVars();
			sane &= luc.getLoop().allAreInOutAux(luc.getLoop().getFormula().getFreeVars()) == null;
		}
		return sane;
	}
	
	/**
	 * Construct a polyhedron representation for each component of stem and 
	 * loop.
	 * 
	 * Only succeeds if the transition formulas are of the required form,
	 * i.e., if preprocessing has been completed.
	 * @throws TermException if the transition formulas are not of the correct
	 *                       form
	 */
	public void constructPolyhedra() throws TermException {
		final int n = mLassosUC.size();
		final List<Lasso> lassos = new ArrayList<Lasso>(n);
		for (int i = 0; i < n; ++i) {
			final TransFormulaLR stemTF = mLassosUC.get(i).getStem();
			final TransFormulaLR loopTF = mLassosUC.get(i).getLoop();
			final LinearTransition stem = LinearTransition.fromTransFormulaLR(stemTF, mNlaHandling);
			final LinearTransition loop = LinearTransition.fromTransFormulaLR(loopTF, mNlaHandling);
			lassos.add(new Lasso(stem, loop));
		}
		mLassos = lassos;
	}
	
	/**
	 * @return a colletion of lassos, one for each component
	 * @throws TermException if the transition formulas are not of the correct
	 *                       form
	 */
	public Collection<Lasso> getLassos() {
		return mLassos;
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		if (mLassos == null) {
			sb.append("Preprocessing has not been completed.\n");
			
			sb.append("Current lassos:\n");
			for (final LassoUnderConstruction luc : mLassosUC) {
				sb.append(luc);
				sb.append(System.lineSeparator());
			}
		} else {
			sb.append("Lassos:\n");
			for (final Lasso lasso : mLassos) {
				sb.append(lasso);
				sb.append("\n");
			}
		}
		return sb.toString();
	}
	
	public static int computeMaxDagSize(List<LassoUnderConstruction> lassos) {
		if (lassos.isEmpty()) {
			return 0;
		} else {
			final int[] sizes = new int[lassos.size()];
			for (int i = 0; i < lassos.size(); ++i) {
				sizes[i] = lassos.get(i).getFormulaSize(); 
			}
			Arrays.sort(sizes);
			return sizes[lassos.size() - 1];
		}
	}
	
	public int computeMaxDagSize() {
		return computeMaxDagSize(mLassosUC);
	}

	public void preprocess(LassoPreprocessor[] preProcessorsTermination, LassoPreprocessor[] preProcessorsNontermination) throws TermException {
		mPreprocessingBenchmark = new PreprocessingBenchmark(
				computeMaxDagSize());
		// Apply preprocessors
		for (final LassoPreprocessor preprocessor : preProcessorsTermination) {
			if (preprocessor == null) {
				continue;
			}
			mLogger.debug(preprocessor.getDescription());
			applyPreprocessor(preprocessor);
			mPreprocessingBenchmark.addPreprocessingData(
						preprocessor.getDescription(), 
						computeMaxDagSize());
			assert isSane() : "lasso failed sanity check";
		}
		
	}

	public PreprocessingBenchmark getPreprocessingBenchmark() {
		return mPreprocessingBenchmark;
	}
	

}
