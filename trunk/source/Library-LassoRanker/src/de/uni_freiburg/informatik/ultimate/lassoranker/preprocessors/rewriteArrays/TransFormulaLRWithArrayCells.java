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
package de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.rewriteArrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lassoranker.Activator;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.RankVar;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.ReplacementVarFactory;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.TransFormulaLR;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.TransFormulaUtils;
import de.uni_freiburg.informatik.ultimate.logic.QuantifiedFormula;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Util;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Boogie2SmtSymbolTable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.ApplicationTermFinder;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.PartialQuantifierElimination;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SafeSubstitution;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils.SimplicationTechnique;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils.XnfConversionTechnique;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.ArrayIndex;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.ArrayUpdate;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.MultiDimensionalSelect;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.equalityanalysis.EqualityAnalysisResult;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.managedscript.ManagedScript;
import de.uni_freiburg.informatik.ultimate.util.datastructures.Doubleton;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.HashRelation;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.NestedMap2;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Triple;

/**
 * Computes and provides for a TransformulaLR a DNF of the formula and 
 * information 
 * - which arrays occur in the formula,
 * - in which order the arrays are written,
 * - and the possible indices of each Array accesses.
 * 
 * @author Matthias Heizmann
 */
public class TransFormulaLRWithArrayCells {

	private final ILogger mLogger;
	private final IUltimateServiceProvider mServices;
	private final SimplicationTechnique mSimplificationTechnique;
	private final XnfConversionTechnique mXnfConversionTechnique;
	
	static final String s_AuxArray = "auxArray";

	/**
	 * The script used to transform the formula
	 */
	private final ManagedScript mScript;
	


	
	private final NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation> mForeignReplacementVars =
			new NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation>();
	private final HashRelation<TermVariable, ArrayIndex> mForeignIndices = new HashRelation<>();
	
	private final TransFormulaLRWithArrayInformation tflrwai;
	private final TransFormulaLR mResult;
	private final ReplacementVarFactory mReplacementVarFactory;
	private Map<TermVariable, Map<ArrayIndex, TermVariable>> mArrayInstance2Index2CellVariable;
	private EquivalentCells[] mEquivalentCells;
	private final boolean mOverapproximateByOmmitingDisjointIndices;
	private HashRelation<Term, ArrayIndex> mFirstGeneration2Indices;
	private IndexAnalysisResult mIndexAnalysisResult;
	private NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation> mArrayCellInVars;
	private NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation> mArrayCellOutVars;
	
	private final Map<List<Term>, ArrayIndex> mIndexInstance2IndexRepresentative = new HashMap<>();
	private final boolean mConsiderOnlyIndicesThatOccurInFormula = true;
	private final Set<TermVariable> mVariablesThatOccurInFormula;
	
	private Set<TermVariable> computeVarsThatOccurInFormula() {
		final Set<TermVariable> varsInFormula = new HashSet<>();
		varsInFormula.addAll(Arrays.asList(tflrwai.getTransFormulaLR().getFormula().getFreeVars()));
		varsInFormula.addAll(Arrays.asList(SmtUtils.and(mScript.getScript(), mIndexAnalysisResult.constructListOfEqualities(mScript.getScript())).getFreeVars()));
		if (!mOverapproximateByOmmitingDisjointIndices) {
			varsInFormula.addAll(Arrays.asList(SmtUtils.and(mScript.getScript(), mIndexAnalysisResult.constructListOfNotEquals(mScript.getScript())).getFreeVars()));
		}
		return varsInFormula;
	}

	public TransFormulaLRWithArrayCells(
			final IUltimateServiceProvider services, 
			final ReplacementVarFactory replacementVarFactory, final ManagedScript script,
			final TransFormulaLRWithArrayInformation tflrwai, 
			final EqualityAnalysisResult equalityAnalysisAtHonda, 
			final Boogie2SmtSymbolTable boogie2smt, final ArrayCellRepVarConstructor acrvc, final boolean moverapproximateByOmmitingDisjointIndices, final boolean isStem, final SimplicationTechnique simplificationTechnique, final XnfConversionTechnique xnfConversionTechnique) {
			mServices = services;
			mLogger = mServices.getLoggingService().getLogger(Activator.s_PLUGIN_ID);
			mSimplificationTechnique = simplificationTechnique;
			mXnfConversionTechnique = xnfConversionTechnique;
			mOverapproximateByOmmitingDisjointIndices = moverapproximateByOmmitingDisjointIndices;
			this.tflrwai = tflrwai;
			mScript = script;
			mReplacementVarFactory = replacementVarFactory;
			if (!this.tflrwai.containsArrays()) {
				mResult = this.tflrwai.getTransFormulaLR();
				mVariablesThatOccurInFormula = null;
				return;
			}
			mResult = new TransFormulaLR(tflrwai.getTransFormulaLR());
			mFirstGeneration2Indices = new HashRelation<>();
			mFirstGeneration2Indices.addAll(tflrwai.getArrayFirstGeneration2Indices());
			if (acrvc != null) {
//				addForeignReplacementVars(acrvc);
			}

			mArrayCellInVars = new NestedMap2<>();
			mArrayCellInVars.addAll(tflrwai.getArrayCellInVars());
			mArrayCellInVars.addAll(mForeignReplacementVars);
			mArrayCellOutVars = new NestedMap2<>();
			mArrayCellOutVars.addAll(tflrwai.getArrayCellOutVars());
			mArrayCellOutVars.addAll(mForeignReplacementVars);

			doSomething();
			
			final EqualityAnalysisResult invariantEqualitiesBefore;
			if (isStem) {
				final Set<Doubleton<Term>> emptySet = Collections.emptySet();
				invariantEqualitiesBefore = new EqualityAnalysisResult(emptySet, emptySet, emptySet);
			} else {
				invariantEqualitiesBefore = equalityAnalysisAtHonda;
			}
			final EqualityAnalysisResult invariantEqualitiesAfter = equalityAnalysisAtHonda;
			
			final IndexAnalyzer ia = new IndexAnalyzer(mResult.getFormula(), mFirstGeneration2Indices, 
					boogie2smt, mResult, invariantEqualitiesBefore, 
					invariantEqualitiesAfter, mLogger, mReplacementVarFactory);
			mIndexAnalysisResult = ia.getResult();
			final CellVariableBuilder cvb = new CellVariableBuilder(mResult, this, replacementVarFactory, mLogger, mFirstGeneration2Indices, mArrayCellInVars, mArrayCellOutVars);
			mVariablesThatOccurInFormula = computeVarsThatOccurInFormula();			
			mArrayInstance2Index2CellVariable = cvb.getArrayInstance2Index2CellVariable();
			mEquivalentCells = new EquivalentCells[tflrwai.numberOfDisjuncts()];
			for (int i = 0; i < tflrwai.numberOfDisjuncts(); i++) {
				mEquivalentCells[i] = new EquivalentCells(mScript.getScript(), mResult, tflrwai.getArrayEqualities().get(i), tflrwai.getArrayUpdates().get(i), mIndexAnalysisResult, mArrayInstance2Index2CellVariable); 
//						computeEquivalentCells(mResult, tflrwai.getArrayEqualities().get(i), tflrwai.getArrayUpdates().get(i));
			}

			final SafeSubstitution[] mSelect2CellVariable = new SafeSubstitution[tflrwai.numberOfDisjuncts()];
			for (int i = 0; i < tflrwai.numberOfDisjuncts(); i++) {
				mSelect2CellVariable[i] = constructIndex2CellVariableSubstitution(mEquivalentCells[i], i);
			}

			final Term[] arrayEqualityConstraints = new Term[tflrwai.numberOfDisjuncts()];
			for (int i = 0; i < tflrwai.numberOfDisjuncts(); i++) {
				arrayEqualityConstraints[i] = mEquivalentCells[i].getInOutEqauality();
			}

			final Term[] indexValueConstraints = new Term[tflrwai.numberOfDisjuncts()];
			for (int i = 0; i < tflrwai.numberOfDisjuncts(); i++) {
				indexValueConstraints[i] = buildIndexValueConstraints(mSelect2CellVariable[i], mEquivalentCells[i]);
			}

			final Term[] arrayUpdateConstraints = new Term[tflrwai.numberOfDisjuncts()];
			for (int i = 0; i < tflrwai.numberOfDisjuncts(); i++) {
				arrayUpdateConstraints[i] = buildArrayUpdateConstraints(tflrwai.getArrayUpdates().get(i), mSelect2CellVariable[i],
						mEquivalentCells[i]);
			}
			final Term[] disjunctsWithUpdateConstraints = new Term[tflrwai.numberOfDisjuncts()];
			for (int i = 0; i < disjunctsWithUpdateConstraints.length; i++) {
				final Term removedSelect = mSelect2CellVariable[i].transform(tflrwai.getSunnf()[i]);
				Term[] conjuncts;
				if (mOverapproximateByOmmitingDisjointIndices) {
					conjuncts = new Term[5];
				} else {
					conjuncts = new Term[6];
					conjuncts[5] = SmtUtils.and(mScript.getScript(), mIndexAnalysisResult.constructListOfNotEquals(mScript.getScript()));
				}
				conjuncts[0] = removedSelect;
				conjuncts[1] = indexValueConstraints[i];
				conjuncts[2] = arrayUpdateConstraints[i];
				conjuncts[3] = arrayEqualityConstraints[i];
				conjuncts[4] = mSelect2CellVariable[i].transform(SmtUtils.and(mScript.getScript(), mIndexAnalysisResult.constructListOfEqualities(mScript.getScript())));
				disjunctsWithUpdateConstraints[i] = mSelect2CellVariable[i].transform(Util.and(mScript.getScript(), conjuncts));
			}
			final Term resultDisjuntion = Util.or(mScript.getScript(), disjunctsWithUpdateConstraints);
			final HashSet<TermVariable> auxVars = new HashSet<TermVariable>(cvb.getAuxVars());

			Term result = //resultDisjuntion;
					PartialQuantifierElimination.elim(mScript, QuantifiedFormula.EXISTS, auxVars, resultDisjuntion, mServices, mLogger, mSimplificationTechnique, mXnfConversionTechnique);
			
			assert SmtUtils.isArrayFree(result) : "Result contains still arrays!";
			result = SmtUtils.simplify(mScript, result, mServices, mSimplificationTechnique);
			
			removeArrayInOutVars();
			
			mResult.setFormula(result);
			mResult.addAuxVars(auxVars);
	}
	
	private void removeArrayInOutVars() {
		{
			final List<RankVar> toRemove = new ArrayList<RankVar>();
			toRemove.addAll(filterArrays(mResult.getInVars().keySet()));
			for (final RankVar rv : toRemove) {
				mResult.removeInVar(rv);
			}
		}
		{
			final List<RankVar> toRemove = new ArrayList<RankVar>();
			toRemove.addAll(filterArrays(mResult.getOutVars().keySet()));
			for (final RankVar rv : toRemove) {
				mResult.removeOutVar(rv);
			}
		}
	}

	private Collection<RankVar> filterArrays(final Set<RankVar> keySet) {
		final List<RankVar> result = new ArrayList<RankVar>();
		for (final RankVar rv : keySet) {
			if (rv.getDefinition().getSort().isArraySort()) {
				result.add(rv);
			}
		}
		return result;
	}

	public ArrayIndex getOrConstructIndexRepresentative(final ArrayIndex indexInstance) {
		ArrayIndex indexRepresentative = mIndexInstance2IndexRepresentative.get(indexInstance);
		if (indexRepresentative == null) {
			indexRepresentative = new ArrayIndex(TransFormulaUtils.translateTermVariablesToDefinitions(mScript.getScript(), mResult, indexInstance));
			mIndexInstance2IndexRepresentative.put(indexInstance, indexRepresentative);
		}
		return indexRepresentative;
	}
	
	
	
	public TransFormulaLRWithArrayInformation getTransFormulaLRWithArrayInformation() {
		return tflrwai;
	}



//	private void addForeignReplacementVars(ArrayCellRepVarConstructor arrayCellRepVarConstructor) {
//		NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation> array2Index2RepVar = 
//				arrayCellRepVarConstructor.getArrayRepresentative2IndexRepresentative2ReplacementVar();
//		for (TermVariable array : array2Index2RepVar.keySet()) {
//			if (arrayOccursInThisTransFormulaAsInvar(array)) {
//				for (Entry<ArrayIndex, ArrayCellReplacementVarInformation> entry : array2Index2RepVar.get(array).entrySet()) {
//					ArrayIndex index = entry.getKey();
//					ArrayCellReplacementVarInformation acrvi = entry.getValue();
//					allVarsOfIndexOccurInThisTransFormulaAsInvar(acrvi);
//						if (!arrayCellOccursInThisTransFormula(array, index)) {
//							mForeignReplacementVars.put(array, index, entry.getValue());
//						}
//				}
//			}
//		}
//	}
//	
//	private void allVarsOfIndexOccurInThisTransFormulaAsInvar(
//			ArrayCellReplacementVarInformation acrvi) {
//		Collection<RankVar> rankVarsOccurringInIndex = acrvi.termVariableToRankVarMappingForIndex().values();
//		for (RankVar rv : rankVarsOccurringInIndex) {
//			if (!this.tflrwai.getTransFormulaLR().getInVars().containsKey(rv)) {
//				return false;
//			}
//		}
//		return true;
//	}

	public void doSomething() {
		for (final Triple<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation> triple  : mForeignReplacementVars.entrySet()) {
			final ArrayCellReplacementVarInformation acrvi = triple.getThird();
			assert acrvi.getArrayRepresentative().equals(triple.getFirst());
			assert acrvi.getIndexRepresentative().equals(triple.getSecond());
			final Collection<RankVar> rankVarsOccurringInIndex = acrvi.termVariableToRankVarMappingForIndex().values();
			for (final RankVar rv : rankVarsOccurringInIndex) {
				if (!rankVarOccursInThisTransformula(rv, mResult)) {
					addRankVar(rv);
					throw new AssertionError("case may not occur any more");
				}
			}
			final TermVariable translatedArray = (TermVariable) tflrwai.getTransFormulaLR().getInVars().get(acrvi.getArrayRankVar());
			final ArrayIndex translatedIndex = translateIndex(acrvi.getIndex(), acrvi.termVariableToRankVarMappingForIndex());
			mFirstGeneration2Indices.addPair(translatedArray, translatedIndex);
		}
	}
	
	
	
	private ArrayIndex translateIndex(final ArrayIndex index,
			final Map<TermVariable, RankVar> termVariableToRankVarMappingForIndex) {
		final List<Term> translatedIndex = new ArrayList<Term>();
		for (final Term entry : index) {
			final Term translatedEntry = translateIndexEntry(entry, termVariableToRankVarMappingForIndex);
			translatedIndex.add(translatedEntry);
		}
		return new ArrayIndex(translatedIndex);
	}

	private Term translateIndexEntry(final Term entry,
			final Map<TermVariable, RankVar> termVariableToRankVarMappingForIndex) {
		final Map<Term, Term> substitutionMapping = new HashMap<Term, Term>();
		for (final TermVariable originalTv : entry.getFreeVars()) {
			final RankVar rv = termVariableToRankVarMappingForIndex.get(originalTv);
			final TermVariable newTv = (TermVariable) mResult.getInVars().get(rv);
			substitutionMapping.put(originalTv, newTv);
		}
		final Term renamedEntry = (new SafeSubstitution(mScript.getScript(), substitutionMapping)).transform(entry);
		return renamedEntry;
	}

	private void addRankVar(final RankVar rv) {
		final String name = SmtUtils.removeSmtQuoteCharacters(rv.getGloballyUniqueId() + "_InOut");
		final Sort sort = rv.getDefinition().getSort();
		final TermVariable tv = mReplacementVarFactory.getOrConstructAuxVar(name, sort);
		mResult.addInVar(rv, tv);
		mResult.addOutVar(rv, tv);
	}

	private boolean rankVarOccursInThisTransformula(final RankVar rv,
			final TransFormulaLR transFormulaLR) {
		final Term inVar = transFormulaLR.getInVars().get(rv);
		final Term outVar = transFormulaLR.getOutVars().get(rv);
		if (inVar == null && outVar == null) {
			return false;
		}
		if (inVar != null && outVar != null) {
			return true;
		}
		throw new AssertionError(rv + " occurs only as inVar or only as outVar");
	}

	private boolean arrayOccursInThisTransFormulaAsInvar(final TermVariable array) {
		return tflrwai.getArrayCellInVars().keySet().contains(array);
	}
	
	private boolean arrayCellOccursInThisTransFormula(final TermVariable array, final List<Term> index) {
		return tflrwai.getArrayCellInVars().get(array).containsKey(index);
//		||	this.tflrwai.getArrayCellOutVars().get(array).containsKey(index);
	}

	
	






	

	private Term buildArrayEqualityConstraints(final TermVariable oldArray, final TermVariable newArray) {
		final Map<ArrayIndex, TermVariable> newInstance2Index2CellVariable = mArrayInstance2Index2CellVariable.get(newArray);
		final Map<ArrayIndex, TermVariable> oldInstance2Index2CellVariable = mArrayInstance2Index2CellVariable.get(oldArray);
		if (newInstance2Index2CellVariable == null && oldInstance2Index2CellVariable == null) {
			return mScript.getScript().term("true");
		}
		final Term[] conjuncts = new Term[newInstance2Index2CellVariable.keySet().size()];
		int offset = 0;
		for (final List<Term> index : newInstance2Index2CellVariable.keySet()) {
			final Term newCellVariable = newInstance2Index2CellVariable.get(index);
			final Term oldCellVariable = oldInstance2Index2CellVariable.get(index);
			conjuncts[offset] = SmtUtils.binaryEquality(mScript.getScript(), oldCellVariable, newCellVariable);
			offset++;
		}
		return Util.and(mScript.getScript(), conjuncts);
	}

	private Term buildArrayUpdateConstraints(final List<ArrayUpdate> arrayUpdates, final SafeSubstitution select2CellVariable,
			final EquivalentCells equivalentCells) {
		final Term[] conjuncts = new Term[arrayUpdates.size()];
		int offset = 0;
		for (final ArrayUpdate au : arrayUpdates) {
			conjuncts[offset] = buildArrayUpdateConstraints(au.getNewArray(), (TermVariable) au.getOldArray(), au.getIndex(),
					au.getValue(), select2CellVariable, equivalentCells);
			offset++;
		}
		final Term result = Util.and(mScript.getScript(), conjuncts);
		assert (new ApplicationTermFinder("select", true)).findMatchingSubterms(result).isEmpty() : "contains select terms";
		return result;
	}

	private Term buildArrayUpdateConstraints(final TermVariable newArray, final TermVariable oldArray, final ArrayIndex updateIndex,
			Term data, final SafeSubstitution select2CellVariable, final EquivalentCells equivalentCells) {
		data = select2CellVariable.transform(data);
		Map<ArrayIndex, TermVariable> newInstance2Index2CellVariable = mArrayInstance2Index2CellVariable.get(newArray);
		Map<ArrayIndex, TermVariable> oldInstance2Index2CellVariable = mArrayInstance2Index2CellVariable.get(oldArray);
		if (mConsiderOnlyIndicesThatOccurInFormula ) {
			newInstance2Index2CellVariable = filterNonOccurring(newInstance2Index2CellVariable);
			oldInstance2Index2CellVariable = filterNonOccurring(oldInstance2Index2CellVariable);
		}
		
		final Term[] conjuncts = new Term[newInstance2Index2CellVariable.keySet().size()];
		int offset = 0;
		for (final ArrayIndex index : newInstance2Index2CellVariable.keySet()) {
			TermVariable newCellVariable = newInstance2Index2CellVariable.get(index);
			newCellVariable = equivalentCells.getInOutRepresentative(newCellVariable);
			TermVariable oldCellVariable = oldInstance2Index2CellVariable.get(index);
			oldCellVariable = equivalentCells.getInOutRepresentative(oldCellVariable);
			final Term indexIsUpdateIndex = pairwiseEqualityExploitDoubletons(index, updateIndex,
					select2CellVariable);
			final Term newDataIsUpdateData = SmtUtils.binaryEquality(mScript.getScript(), newCellVariable, data);
			final Term newDateIsOldData = SmtUtils.binaryEquality(mScript.getScript(), newCellVariable, oldCellVariable);
			final Term indexIsNotUpdateIndex = SmtUtils.not(mScript.getScript(), indexIsUpdateIndex);
			final Term indexIsUpdateIndexImpliesUpdateData = Util.or(mScript.getScript(), indexIsNotUpdateIndex, newDataIsUpdateData);
			final Term indexIsNotUpdateIndexImpliesOldData = Util.or(mScript.getScript(), indexIsUpdateIndex, newDateIsOldData);
			conjuncts[offset] = Util.and(mScript.getScript(), indexIsUpdateIndexImpliesUpdateData,
					indexIsNotUpdateIndexImpliesOldData);
			offset++;
		}
		return Util.and(mScript.getScript(), conjuncts);
	}

	private Map<ArrayIndex, TermVariable> filterNonOccurring(final Map<ArrayIndex, TermVariable> newInstance2Index2CellVariable) {
		final Map<ArrayIndex, TermVariable> result = new HashMap<>();
		for ( final Entry<ArrayIndex, TermVariable> entry : newInstance2Index2CellVariable.entrySet()) {
			if (entry.getKey().freeVarsAreSubset(mVariablesThatOccurInFormula)) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}

	private Term buildIndexValueConstraints(final SafeSubstitution select2CellVariable, final EquivalentCells equivalentCells) {
		final Term[] conjuncts = new Term[mArrayInstance2Index2CellVariable.size()];
		int offset = 0;
		for (final Entry<TermVariable, Map<ArrayIndex, TermVariable>> entry : mArrayInstance2Index2CellVariable.entrySet()) {
			Map<ArrayIndex, TermVariable> indices2values = entry.getValue();
			if (mConsiderOnlyIndicesThatOccurInFormula) {
				indices2values = filterNonOccurring(indices2values);
			}
			conjuncts[offset] = buildIndexValueConstraints(indices2values, select2CellVariable, equivalentCells);
			offset++;
		}
		return Util.and(mScript.getScript(), conjuncts);
	}

	private Term buildIndexValueConstraints(final Map<ArrayIndex, TermVariable> indices2values,
			final SafeSubstitution select2CellVariable, final EquivalentCells equivalentCells) {
		final ArrayIndex[] indices = new ArrayIndex[indices2values.size()];
		final TermVariable[] values = new TermVariable[indices2values.size()];
		int offset = 0;
		for (final Entry<ArrayIndex, TermVariable> index2value : indices2values.entrySet()) {
			indices[offset] = index2value.getKey();
			values[offset] = index2value.getValue();
			offset++;
		}
		final int numberOfPairs = indices2values.size() * (indices2values.size() - 1) / 2;
		final Term[] conjuncts = new Term[numberOfPairs];
		int k = 0;
		for (int i = 0; i < indices2values.size(); i++) {
			for (int j = 0; j < i; j++) {
				final ArrayIndex index1 = indices[i];
				final ArrayIndex index2 = indices[j];
				final TermVariable value1 = values[i];
				final TermVariable value2 = values[j];
				final TermVariable value1Representative = equivalentCells.getInOutRepresentative(value1);
				final TermVariable value2Representative = equivalentCells.getInOutRepresentative(value2);
				conjuncts[k] = indexEqualityImpliesValueEquality(index1, index2, value1Representative,
						value2Representative, select2CellVariable);
				k++;
			}
		}
		final Term result = Util.and(mScript.getScript(), conjuncts);
		return result;
	}

	private Term indexEqualityImpliesValueEquality(final ArrayIndex index1, final ArrayIndex index2, final Term value1, final Term value2,
			final SafeSubstitution select2CellVariable) {
		final Term indexEquality = pairwiseEqualityExploitDoubletons(index1, index2, select2CellVariable);
		final Term valueEquality = SmtUtils.binaryEquality(mScript.getScript(), value1, value2);
		return Util.or(mScript.getScript(), SmtUtils.not(mScript.getScript(), indexEquality), valueEquality);
	}

	Term pairwiseEqualityExploitDoubletons(final ArrayIndex index1, final ArrayIndex index2, final SafeSubstitution select2CellVariable) {
		assert index1.size() == index2.size();
		final Term[] conjuncts = new Term[index1.size()];
		for (int i = 0; i < index1.size(); i++) {
			final Term fst = index1.get(i);
			final Term snd = index2.get(i);
			if (fst == snd || mIndexAnalysisResult.isEqualDoubleton(fst, snd)) {
				conjuncts[i] = mScript.getScript().term("true");
			} else if (mIndexAnalysisResult.isDistinctDoubleton(fst, snd)) {
				conjuncts[i] = mScript.getScript().term("false");
			} else if (mIndexAnalysisResult.isUnknownDoubleton(fst, snd) || mIndexAnalysisResult.isIgnoredDoubleton(fst, snd)) {
				final Term fstSubst = select2CellVariable.transform(fst);
				final Term sndSubst = select2CellVariable.transform(snd);
				conjuncts[i] = SmtUtils.binaryEquality(mScript.getScript(), fstSubst, sndSubst);
			} else {
				throw new AssertionError("unknown doubleton");
			}
		}
		return Util.and(mScript.getScript(), conjuncts);
	}

	/**
	 * Replace all select terms by the corresponding cell variables.
	 */
	private SafeSubstitution constructIndex2CellVariableSubstitution(final EquivalentCells ec, final int i) {
		final Map<Term, Term> substitutionMapping = new HashMap<Term, Term>();
		for (final MultiDimensionalSelect ar : tflrwai.getArrayReads().get(i)) {
			final TermVariable cellVariable = mArrayInstance2Index2CellVariable.get(ar.getArray()).get(ar.getIndex());
			final Term inOutRepresentative = ec.getInOutRepresentative(cellVariable);
			assert inOutRepresentative != null;
			substitutionMapping.put(ar.getSelectTerm(), inOutRepresentative);
		}

		for (final MultiDimensionalSelect ar : tflrwai.getAdditionalArrayReads()) {
			final TermVariable cellVariable = mArrayInstance2Index2CellVariable.get(ar.getArray()).get(ar.getIndex());
			final Term inOutRepresentative = ec.getInOutRepresentative(cellVariable);
			assert inOutRepresentative != null;
			substitutionMapping.put(ar.getSelectTerm(), inOutRepresentative);
		}
		return new SafeSubstitution(mScript.getScript(), substitutionMapping);
	}



	public TransFormulaLR getResult() {
		return mResult;
	}




}
