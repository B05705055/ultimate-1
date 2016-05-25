/*
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
package de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.boogie.BoogieVar;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lassoranker.Activator;
import de.uni_freiburg.informatik.ultimate.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.rewriteArrays.ArrayCellRepVarConstructor;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.rewriteArrays.IndexSupportingInvariantAnalysis;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.rewriteArrays.TransFormulaLRWithArrayCells;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.rewriteArrays.TransFormulaLRWithArrayInformation;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.LassoUnderConstruction;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.ReplacementVarFactory;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.TransFormulaUtils;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Boogie2SMT;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.IFreshTermVariableConstructor;

/**
 * Replace term with arrays by term without arrays by introducing replacement
 * variables for all "important" array values and equalities that state the
 * constraints between array indices and array values (resp. their replacement
 * variables).
 * 
 * 
 * @author Matthias Heizmann
 */
public class RewriteArrays2 extends LassoPreprocessor {

	private final ILogger mLogger;
	private final IUltimateServiceProvider mServices;
	
	public static final boolean s_AdditionalChecksIfAssertionsEnabled = !false;
	
	public static final String s_Description = 
			"Removes arrays by introducing new variables for each relevant array cell";

	static final String s_AuxArray = "auxArray";

	/**
	 * The script used to transform the formula
	 */
	private final Script mScript;


//	private final boolean mSearchAdditionalSupportingInvariants;
	private final TransFormula mOriginalStem;
	private final TransFormula mOriginalLoop;
	private final Set<Term> mArrayIndexSupportingInvariants;
	private final Set<BoogieVar> mModifiableGlobalsAtHonda;
	
	private final ReplacementVarFactory mReplacementVarFactory;
	private final IFreshTermVariableConstructor mFreshTermVariableConstructor;
	private final Boogie2SMT mboogie2smt;


	private final boolean mOverapproximateByOmmitingDisjointIndices;

	public RewriteArrays2(boolean overapproximateByOmmitingDisjointIndices,
			TransFormula originalStem, TransFormula originalLoop, Set<BoogieVar> modifiableGlobalsAtHonda,
			IUltimateServiceProvider services, Set<Term> arrayIndexSupportingInvariants, Boogie2SMT boogie2smt, ReplacementVarFactory ReplacementVarFactory) {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(Activator.s_PLUGIN_ID);
		mOriginalStem = originalStem;
		mOriginalLoop = originalLoop;
		mModifiableGlobalsAtHonda = modifiableGlobalsAtHonda;
		mArrayIndexSupportingInvariants = arrayIndexSupportingInvariants;
		mOverapproximateByOmmitingDisjointIndices = overapproximateByOmmitingDisjointIndices;
		mboogie2smt = boogie2smt;
		mScript = boogie2smt.getScript();
		mReplacementVarFactory = ReplacementVarFactory;
		mFreshTermVariableConstructor = mboogie2smt.getVariableManager();
	}
	

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public String getDescription() {
		return s_Description;
	}

//	@Override
//	public void process(LassoBuilder lasso_builder) 
//			throws TermException {
//		mlassoBuilder = lasso_builder;
//		mScript = lasso_builder.getScript();
//		ReplacementVarFactory replacementVarFactory = lasso_builder.getReplacementVarFactory();
//		
//		Collection<TransFormulaLR> old_stemcomponents = lasso_builder.getStemComponentsTermination();
////		assert old_stemcomponents == lasso_builder.getStemComponentsNonTermination();
//		Collection<TransFormulaLR> old_loop_components = lasso_builder.getLoopComponentsTermination();
////		assert old_loop_components == lasso_builder.getLoopComponentsNonTermination();
//		List<TransFormulaLRWithArrayInformation> stemComponents1 = new ArrayList<TransFormulaLRWithArrayInformation>();
//		for (TransFormulaLR stemComponent : old_stemcomponents) {
//			TransFormulaLRWithArrayInformation test = new TransFormulaLRWithArrayInformation(
//					mServices, stemComponent, replacementVarFactory, mScript, mlassoBuilder.getBoogie2SMT().getVariableManager(), null);
//			stemComponents1.add(test);
//		}
//		List<TransFormulaLRWithArrayInformation> loopComponents1 = new ArrayList<TransFormulaLRWithArrayInformation>();
//		for (TransFormulaLR loopComponent : old_loop_components) {
//			TransFormulaLRWithArrayInformation test = new TransFormulaLRWithArrayInformation(
//					mServices, loopComponent, replacementVarFactory, mScript, mlassoBuilder.getBoogie2SMT().getVariableManager(), stemComponents1);
//			loopComponents1.add(test);
//		}
//		ArrayCellRepVarConstructor acrvc = new ArrayCellRepVarConstructor(replacementVarFactory, mScript, stemComponents1, loopComponents1);
//		IndexSupportingInvariantAnalysis isia = new IndexSupportingInvariantAnalysis(acrvc, true, lasso_builder.getBoogie2SMT(), mOriginalStem, mOriginalLoop, mModifiableGlobalsAtHonda);
//		mArrayIndexSupportingInvariants.addAll(isia.getAdditionalConjunctsEqualities());
//		mArrayIndexSupportingInvariants.addAll(isia.getAdditionalConjunctsNotEquals());
//		
//		// for termination, we overapproximate by ommiting disjoint indices
//		{
//			List<TransFormulaLRWithArrayCells> stemComponents2 = new ArrayList<TransFormulaLRWithArrayCells>();
//			List<TransFormulaLR> new_stemcomponents = new ArrayList<TransFormulaLR>(old_stemcomponents.size());
//			for (TransFormulaLRWithArrayInformation stemComponent : stemComponents1) {
//				TransFormulaLRWithArrayCells test = new TransFormulaLRWithArrayCells(mServices, replacementVarFactory, mScript, stemComponent, isia, lasso_builder.getBoogie2SMT(), null, true, true);
//				stemComponents2.add(test);
//				new_stemcomponents.add(test.getResult());
//			}
//			lasso_builder.setStemComponentsTermination(new_stemcomponents);
//		}
//		
//		// for nontermination, we do not overapproximate
//		{
//			List<TransFormulaLRWithArrayCells> stemComponents2 = new ArrayList<TransFormulaLRWithArrayCells>();
//			List<TransFormulaLR> new_stemcomponents = new ArrayList<TransFormulaLR>(old_stemcomponents.size());
//			for (TransFormulaLRWithArrayInformation stemComponent : stemComponents1) {
//				TransFormulaLRWithArrayCells test = new TransFormulaLRWithArrayCells(mServices, replacementVarFactory, mScript, stemComponent, isia, lasso_builder.getBoogie2SMT(), null, false, true);
//				stemComponents2.add(test);
//				new_stemcomponents.add(test.getResult());
//			}
//			lasso_builder.setStemComponentsNonTermination(new_stemcomponents);
//		}
//		
//		// for termination, we overapproximate by ommiting disjoint indices
//		{
//			List<TransFormulaLRWithArrayCells> loopComponents2 = new ArrayList<TransFormulaLRWithArrayCells>();
//			List<TransFormulaLR> new_loop_components = new ArrayList<TransFormulaLR>(old_loop_components.size());
//			for (TransFormulaLRWithArrayInformation loopComponent : loopComponents1) {
//				TransFormulaLRWithArrayCells test = new TransFormulaLRWithArrayCells(mServices, replacementVarFactory, mScript, loopComponent, isia, lasso_builder.getBoogie2SMT(), acrvc, true, false);
//				loopComponents2.add(test);
//				new_loop_components.add(test.getResult());
//			}
//
//			lasso_builder.setLoopComponentsTermination(new_loop_components);
//		}
//		
//		// for nontermination, we do not overapproximate
//		{
//			List<TransFormulaLRWithArrayCells> loopComponents2 = new ArrayList<TransFormulaLRWithArrayCells>();
//			List<TransFormulaLR> new_loop_components = new ArrayList<TransFormulaLR>(old_loop_components.size());
//			for (TransFormulaLRWithArrayInformation loopComponent : loopComponents1) {
//				TransFormulaLRWithArrayCells test = new TransFormulaLRWithArrayCells(mServices, replacementVarFactory, mScript, loopComponent, isia, lasso_builder.getBoogie2SMT(), acrvc, false, false);
//				loopComponents2.add(test);
//				new_loop_components.add(test.getResult());
//			}
//
//			lasso_builder.setLoopComponentsNonTermination(new_loop_components);
//		}
//		
//		
//	}

	@Override
	public Collection<LassoUnderConstruction> process(LassoUnderConstruction lasso) throws TermException {
		final boolean overapproximate = true;
		final TransFormulaLRWithArrayInformation stemTfwai = new TransFormulaLRWithArrayInformation(
					mServices, lasso.getStem(), mReplacementVarFactory, mScript, mboogie2smt, null);
		final TransFormulaLRWithArrayInformation loopTfwai = new TransFormulaLRWithArrayInformation(
					mServices, lasso.getLoop(), mReplacementVarFactory, mScript, mboogie2smt, stemTfwai);
		final ArrayCellRepVarConstructor acrvc = new ArrayCellRepVarConstructor(mReplacementVarFactory, mScript, stemTfwai, loopTfwai);
		final IndexSupportingInvariantAnalysis isia = new IndexSupportingInvariantAnalysis(acrvc, true, mboogie2smt, mOriginalStem, mOriginalLoop, mModifiableGlobalsAtHonda);
		mArrayIndexSupportingInvariants.addAll(isia.getAdditionalConjunctsEqualities());
		mArrayIndexSupportingInvariants.addAll(isia.getAdditionalConjunctsNotEquals());
		final TransFormulaLRWithArrayCells stem = new TransFormulaLRWithArrayCells(mServices, mReplacementVarFactory, mScript, stemTfwai, isia, mboogie2smt, null, overapproximate, true);
		final TransFormulaLRWithArrayCells loop = new TransFormulaLRWithArrayCells(mServices, mReplacementVarFactory, mScript, loopTfwai, isia, mboogie2smt, acrvc, overapproximate, false);
		final LassoUnderConstruction newLasso = new LassoUnderConstruction(stem.getResult(), loop.getResult());
		assert !s_AdditionalChecksIfAssertionsEnabled || checkStemImplication(
				mServices, mLogger, lasso, newLasso, mboogie2smt) : "result of RewriteArrays too strong";
		return Collections.singleton(newLasso);
	}
	
	
	private boolean checkStemImplication(IUltimateServiceProvider services, 
			ILogger logger,
			LassoUnderConstruction oldLasso,
			LassoUnderConstruction newLasso,
			Boogie2SMT boogie2smt) {
		final LBool implies = TransFormulaUtils.implies(mServices, mLogger, 
				oldLasso.getStem(), newLasso.getStem(), mScript, boogie2smt.getBoogie2SmtSymbolTable());
		if (implies != LBool.SAT && implies != LBool.UNSAT) {
			logger.warn("result of RewriteArrays check is " + implies);
		}
		assert (implies != LBool.SAT) : "result of RewriteArrays too strong";
		return (implies != LBool.SAT);
	}



}
