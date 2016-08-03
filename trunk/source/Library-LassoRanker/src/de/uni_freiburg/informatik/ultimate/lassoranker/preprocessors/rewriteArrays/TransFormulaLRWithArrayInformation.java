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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lassoranker.Activator;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.RewriteArrays2;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.rewriteArrays.ArrayCellReplacementVarInformation.VarType;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.rewriteArrays.SingleUpdateNormalFormTransformer.FreshAuxVarGenerator;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.RankVar;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.ReplacementVarFactory;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.TransFormulaLR;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.TransFormulaUtils;
import de.uni_freiburg.informatik.ultimate.logic.QuantifiedFormula;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Util;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Boogie2SMT;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.IFreshTermVariableConstructor;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.PartialQuantifierElimination;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SafeSubstitution;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils.SimplicationTechnique;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils.XnfConversionTechnique;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.ArrayEquality;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.ArrayEquality.ArrayEqualityExtractor;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.ArrayIndex;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.ArrayUpdate;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.MultiDimensionalSelect;
import de.uni_freiburg.informatik.ultimate.util.datastructures.UnionFind;
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
public class TransFormulaLRWithArrayInformation {

	private final ILogger mLogger;
	private final IUltimateServiceProvider mServices;
	private final SimplicationTechnique mSimplificationTechnique;
	private final XnfConversionTechnique mXnfConversionTechnique;

	
	private final boolean mContainsArrays;

	static final String s_AuxArray = "auxArray";

	/**
	 * The script used to transform the formula
	 */
	private final Script mScript;
	private final IFreshTermVariableConstructor mFreshTermVariableConstructor;

	/**
	 * Mapping from the first generation of an array to all indices that
	 * occur in instances of the same array.
	 */
	private HashRelation<Term, ArrayIndex> mArrayFirstGeneration2Indices;
	private final HashRelation<TermVariable, TermVariable> mArrayFirstGeneration2Instances;
	private final Map<ArrayIndex, ArrayIndex> mIndexInstance2IndexRepresentative = new HashMap<>();
	private final List<List<ArrayUpdate>> mArrayUpdates;
	private final List<List<MultiDimensionalSelect>> mArrayReads;
	/**
	 * Array reads that are added while constructing additional in/out vars.
	 */
	private final List<MultiDimensionalSelect> mAdditionalArrayReads = new ArrayList<>();
	private final ArrayGenealogy[] mArrayGenealogy;
	private final Term[] sunnf;
	private final List<List<ArrayEquality>> mArrayEqualities;

	private final TransFormulaLR mTransFormulaLR;
	private final ReplacementVarFactory mReplacementVarFactory;
	
	private final NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation> mArrayCellInVars = 
			new NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation>();
	private final NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation> mArrayCellOutVars = 
			new NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation>();
	
	private SafeSubstitution mInVars2OutVars;
	private SafeSubstitution mOutVars2InVars;
	
	
	
	public TransFormulaLRWithArrayInformation(
			final IUltimateServiceProvider services, 
			final TransFormulaLR transFormulaLR, 
			final ReplacementVarFactory replacementVarFactory, final Script script, 
			final Boogie2SMT boogie2smt, 
			final TransFormulaLRWithArrayInformation stem, 
			final SimplicationTechnique simplificationTechnique, 
			final XnfConversionTechnique xnfConversionTechnique) {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(Activator.s_PLUGIN_ID);
		mSimplificationTechnique = simplificationTechnique;
		mXnfConversionTechnique = xnfConversionTechnique;
 		mTransFormulaLR = transFormulaLR;
 		mScript = script;
		mFreshTermVariableConstructor = boogie2smt.getVariableManager();
		mReplacementVarFactory = replacementVarFactory;
		if (!SmtUtils.containsArrayVariables(mTransFormulaLR.getFormula())) {
			mContainsArrays = false;
			sunnf = null;
			mArrayUpdates = null;
			mArrayReads = null;
			mArrayEqualities = null;
			mArrayGenealogy = null;
			mArrayFirstGeneration2Instances = null;
		} else {
			mContainsArrays = true;
			final Term term = SmtUtils.simplify(mScript, mTransFormulaLR.getFormula(), mServices, simplificationTechnique, mFreshTermVariableConstructor);
			Term dnf = SmtUtils.toDnf(mServices, mScript, mFreshTermVariableConstructor, term, xnfConversionTechnique);
			dnf = SmtUtils.simplify(mScript, dnf, mServices, simplificationTechnique, mFreshTermVariableConstructor);
			final Term[] disjuncts = SmtUtils.getDisjuncts(dnf);
			sunnf = new Term[disjuncts.length];
			mArrayUpdates = new ArrayList<List<ArrayUpdate>>(disjuncts.length);
			mArrayReads = new ArrayList<List<MultiDimensionalSelect>>(disjuncts.length);
			mArrayEqualities = new ArrayList<List<ArrayEquality>>(disjuncts.length);
			mArrayGenealogy = new ArrayGenealogy[disjuncts.length];
			final FreshAuxVarGenerator favg = new FreshAuxVarGenerator(mReplacementVarFactory);
			final SingleUpdateNormalFormTransformer[] sunfts = new SingleUpdateNormalFormTransformer[disjuncts.length];
			for (int i = 0; i < disjuncts.length; i++) {
				final Term[] conjuncts = SmtUtils.getConjuncts(disjuncts[i]);
				final ArrayEqualityExtractor aee = new ArrayEqualityExtractor(conjuncts);
				mArrayEqualities.add(aee.getArrayEqualities());
				sunfts[i] = new SingleUpdateNormalFormTransformer(Util.and(mScript, aee
						.getRemainingTerms().toArray(new Term[0])), mScript, favg);
				mArrayUpdates.add(sunfts[i].getArrayUpdates());
				sunnf[i] = sunfts[i].getRemainderTerm();
				mArrayReads.add(extractArrayReads(sunfts[i].getArrayUpdates(), sunfts[i].getRemainderTerm()));
				mArrayGenealogy[i] = new ArrayGenealogy(mTransFormulaLR, mArrayEqualities.get(i), mArrayUpdates.get(i), mArrayReads.get(i));
			}
			assert !RewriteArrays2.ADDITIONAL_CHECKS_IF_ASSERTIONS_ENABLED || checkSunftranformation(
					services, mLogger, mFreshTermVariableConstructor, boogie2smt, mArrayEqualities, sunfts) 
					: "error in sunftransformation";
			constructSubstitutions();
			final HashRelation<TermVariable, ArrayIndex> foreignIndices;
			if (stem == null) {
				foreignIndices = null;
			} else {
				foreignIndices = computeForeignIndices(stem);
			}
			new IndexCollector(mTransFormulaLR, foreignIndices);
			mArrayFirstGeneration2Instances = computeArrayFirstGeneration2Instances();
			computeInVarAndOutVarArrayCells();
		}
	}
	
	
	
	private boolean checkSunftranformation(final IUltimateServiceProvider services, 
			final ILogger logger, final IFreshTermVariableConstructor ftvc, 
			final Boogie2SMT boogie2smt, final List<List<ArrayEquality>> arrayEqualities, final SingleUpdateNormalFormTransformer[] sunfts) {
		final TransFormulaLR afterSunft = constructTransFormulaLRWInSunf(services, mXnfConversionTechnique, mSimplificationTechnique, logger, ftvc, mReplacementVarFactory, mScript, mTransFormulaLR, arrayEqualities, sunfts);
		final LBool notStronger = TransFormulaUtils.implies(mServices, mLogger, mTransFormulaLR, afterSunft, mScript, boogie2smt.getBoogie2SmtSymbolTable());
		if (notStronger != LBool.SAT && notStronger != LBool.UNSAT) {
			logger.warn("result of sunf transformation notStronger check is " + notStronger);
		}
		assert (notStronger != LBool.SAT) : "result of sunf transformation too strong";
		final LBool notWeaker = TransFormulaUtils.implies(mServices, mLogger, afterSunft, mTransFormulaLR, mScript, boogie2smt.getBoogie2SmtSymbolTable());
		if (notWeaker != LBool.SAT && notWeaker != LBool.UNSAT) {
			logger.warn("result of sunf transformation notWeaker check is " + notWeaker);
		}
		assert (notWeaker != LBool.SAT) : "result of sunf transformation too weak";
		return (notStronger != LBool.SAT && notWeaker != LBool.SAT);
	}



	private HashRelation<TermVariable, ArrayIndex> computeForeignIndices(final TransFormulaLRWithArrayInformation stem) {
		final HashRelation<TermVariable, ArrayIndex> arrayInVar2ForeignIndices = new HashRelation<>();
		for (final Triple<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation> triple : stem.getArrayCellOutVars().entrySet()) {
			final ArrayCellReplacementVarInformation acrvi = triple.getThird();
			final RankVar arrayRv = acrvi.getArrayRankVar();
			final TermVariable arrayInVar = (TermVariable) mTransFormulaLR.getInVars().get(arrayRv);
			if (arrayInVar != null) {
				// array also occurs in loop, we have to add the index
				// of this ArrayCellReplacement
				final ArrayIndex foreignIndex = computeForeignIndex(arrayRv, acrvi.getIndex(), acrvi.termVariableToRankVarMappingForIndex());
				assert (TransFormulaUtils.allVariablesAreInVars(foreignIndex, mTransFormulaLR));
				if (mLogger.isDebugEnabled()) {
					mLogger.debug("Adding foreign index " + foreignIndex + " for array " + arrayInVar);
				}
				arrayInVar2ForeignIndices.addPair(arrayInVar, foreignIndex);
			}
		}
		return arrayInVar2ForeignIndices;
	}



	private ArrayIndex computeForeignIndex(final RankVar arrayRv, final ArrayIndex index,
			final Map<TermVariable, RankVar> termVariableToRankVarMappingForIndex) {
		final Map<Term, Term> substitutionMapping = new HashMap<Term, Term>();
		for (final Entry<TermVariable, RankVar> foreigntv2rv : termVariableToRankVarMappingForIndex.entrySet()) {
			if (!mTransFormulaLR.getInVars().containsKey(foreigntv2rv.getValue())) {
				addForeignInVarAndOutVar(foreigntv2rv.getValue());
			}
			final TermVariable ourInVar = (TermVariable) mTransFormulaLR.getInVars().get(foreigntv2rv.getValue());
			substitutionMapping.put(foreigntv2rv.getKey(), ourInVar);
		}
		final List<Term> translatedIndex = (new SafeSubstitution(mScript, substitutionMapping)).transform(index);
		final ArrayIndex foreignIndex = new ArrayIndex(translatedIndex);
		return foreignIndex;
	}



	private void addForeignInVarAndOutVar(final RankVar value) {
		final String name = value.getGloballyUniqueId() + "_ForeignInOutVar";
		final Sort sort = value.getDefinition().getSort();
		final TermVariable inOutVar = mFreshTermVariableConstructor.constructFreshTermVariable(name, sort);
		assert !mTransFormulaLR.getInVars().containsKey(value);
		mTransFormulaLR.addInVar(value, inOutVar);
		assert !mTransFormulaLR.getOutVars().containsKey(value);
		mTransFormulaLR.addOutVar(value, inOutVar);
	}



	public boolean containsArrays() {
		return mContainsArrays;
	}



	private HashRelation<TermVariable, TermVariable> computeArrayFirstGeneration2Instances() {
		final HashRelation<TermVariable, TermVariable> result = new HashRelation<TermVariable, TermVariable>();
		for (int i = 0; i < numberOfDisjuncts(); i++) {
			for (final TermVariable instance : mArrayGenealogy[i].getInstances()) {
				final TermVariable firstGeneration = mArrayGenealogy[i].getProgenitor(instance);
				result.addPair(firstGeneration, instance);
			}
		}
		return result;
	}

	public HashRelation<Term, ArrayIndex> getArrayFirstGeneration2Indices() {
		return mArrayFirstGeneration2Indices;
	}
	
	public HashRelation<TermVariable, TermVariable> getArrayFirstGeneration2Instances() {
		return mArrayFirstGeneration2Instances;
	}

	public List<List<ArrayUpdate>> getArrayUpdates() {
		return mArrayUpdates;
	}
	
	public List<List<MultiDimensionalSelect>> getArrayReads() {
		return mArrayReads;
	}

	public List<MultiDimensionalSelect> getAdditionalArrayReads() {
		return mAdditionalArrayReads;
	}
	
	public List<List<ArrayEquality>> getArrayEqualities() {
		return mArrayEqualities;
	}

	public int numberOfDisjuncts() {
		return sunnf.length;
	}

	public Term[] getSunnf() {
		return sunnf;
	}
	
	public TransFormulaLR getTransFormulaLR() {
		return mTransFormulaLR;
	}
	
	public ArrayIndex getOrConstructIndexRepresentative(final ArrayIndex indexInstance) {
		ArrayIndex indexRepresentative = mIndexInstance2IndexRepresentative.get(indexInstance);
		if (indexRepresentative == null) {
			indexRepresentative = new ArrayIndex(TransFormulaUtils.translateTermVariablesToDefinitions(mScript, mTransFormulaLR, indexInstance));
			mIndexInstance2IndexRepresentative.put(indexInstance, indexRepresentative);
		}
		return indexRepresentative;
	}



	private List<MultiDimensionalSelect> extractArrayReads(final List<ArrayUpdate> arrayUpdates, final Term remainderTerm) {
		final ArrayList<MultiDimensionalSelect> result = new ArrayList<>();
		for (final ArrayUpdate au : arrayUpdates) {
			for (final Term indexEntry : au.getIndex()) {
				result.addAll(MultiDimensionalSelect.extractSelectDeep(indexEntry, true));
			}
			result.addAll(MultiDimensionalSelect.extractSelectDeep(au.getValue(), true));
		}
		result.addAll(MultiDimensionalSelect.extractSelectDeep(remainderTerm, true));
		return result;
	}


	private class ArrayGenealogy {
		Map<ArrayGeneration, ArrayGeneration> mGeneration2OriginalGeneration = new HashMap<ArrayGeneration, ArrayGeneration>();

		Map<TermVariable, TermVariable> mInstance2Representative = new HashMap<TermVariable, TermVariable>();

		/**
		 * If array a2 is defined as a2 = ("store", a1, index, value) we call a1
		 * the parent generation of a2.
		 */
		Map<ArrayGeneration, ArrayGeneration> mParentGeneration = new HashMap<ArrayGeneration, ArrayGeneration>();

		Map<TermVariable, ArrayGeneration> mArray2Generation = new HashMap<TermVariable, ArrayGeneration>();

		List<ArrayGeneration> mArrayGenerations = new ArrayList<>();

		private final TransFormulaLR mTransFormula;
		
		private ArrayGeneration getOrConstructArrayGeneration(final TermVariable array) {
			ArrayGeneration ag = mArray2Generation.get(array);
			if (ag == null) {
				ag = new ArrayGeneration(mTransFormula, array);
				mArrayGenerations.add(ag);
			}
			return ag;
		}

		ArrayGenealogy(final TransFormulaLR tf, final List<ArrayEquality> arrayEqualities, final List<ArrayUpdate> arrayUpdates,
				final List<MultiDimensionalSelect> arrayReads) {
			mTransFormula = tf;
			final UnionFind<TermVariable> uf = new UnionFind<>();
			for (final ArrayEquality ae : arrayEqualities) {
				final TermVariable lhs = ae.getLhs();
				final TermVariable rhs = ae.getRhs();
				TermVariable lhsRepresentative = uf.find(lhs);
				if (lhsRepresentative == null) {
					uf.makeEquivalenceClass(lhs);
					lhsRepresentative = lhs;
				}
				TermVariable rhsRepresentative = uf.find(rhs);
				if (rhsRepresentative == null) {
					uf.makeEquivalenceClass(rhs);
					rhsRepresentative = rhs;
				}
				uf.union(lhsRepresentative, rhsRepresentative);
				// putInstance2FirstGeneration(ae.getOutVar(), ae.getInVar());
				// putInstance2FirstGeneration(ae.getInVar(), ae.getInVar());
			}
			for (final TermVariable representative : uf.getAllRepresentatives()) {
				final ArrayGeneration ag = getOrConstructArrayGeneration(representative);
				for (final TermVariable array : uf.getEquivalenceClassMembers(representative)) {
					if (array != representative) {
						ag.add(array);
					}
				}
			}

			for (final ArrayUpdate au : arrayUpdates) {
				final ArrayGeneration oldGeneration = getOrConstructArrayGeneration((TermVariable) au.getOldArray());
				final ArrayGeneration newGeneration = getOrConstructArrayGeneration(au.getNewArray());
				if (oldGeneration == newGeneration) {
					mLogger.warn("self update, this is not tested very well ");
				} else {
					putParentGeneration(newGeneration, oldGeneration);
				}
			}
			for (final ArrayGeneration ag : mArrayGenerations) {
				final ArrayGeneration fg = getFirstGeneration(ag);
				putInstance2FirstGeneration(ag, fg);
			}
			for (final MultiDimensionalSelect ar : arrayReads) {
				determineRepresentative((TermVariable) ar.getArray());
			}
			for (final ArrayEquality ae : arrayEqualities) {
				determineRepresentative(ae.getLhs());
				determineRepresentative(ae.getRhs());
			}
			for (final ArrayUpdate au : arrayUpdates) {
				determineRepresentative(au.getNewArray());
				determineRepresentative((TermVariable) au.getOldArray());
			}
		}

		private void determineRepresentative(final TermVariable array) {
			if (mInstance2Representative.containsKey(array)) {
				// already has a representative
				return;
			}
			final ArrayGeneration ag = mArray2Generation.get(array);
			if (ag == null) {
				// occurs only in select, is its own representative
				mInstance2Representative.put(array, array);
			} else {
				final ArrayGeneration fg = mGeneration2OriginalGeneration.get(ag);
				assert fg != null : "no original generation!";
				final TermVariable representative = fg.getRepresentative();
				if (TransFormulaUtils.isInvar(representative, mTransFormula)) {
					mInstance2Representative.put(array, representative);
				} else {
					throw new AssertionError("no invar");
				}
			}
		}

		private void putParentGeneration(final ArrayGeneration child, final ArrayGeneration parent) {
			assert child != null;
			assert parent != null;
			assert child != parent;
			assert child.toString() != null;
			assert parent.toString() != null;
			mParentGeneration.put(child, parent);
		}

		private void putInstance2FirstGeneration(final ArrayGeneration child, final ArrayGeneration progenitor) {
			assert child != null;
			assert progenitor != null;
			assert child.toString() != null;
			assert progenitor.toString() != null;
			mGeneration2OriginalGeneration.put(child, progenitor);
		}

		private ArrayGeneration getFirstGeneration(final ArrayGeneration ag) {
			final ArrayGeneration parent = mParentGeneration.get(ag);
			if (parent == null) {
				return ag;
			} else {
				return getFirstGeneration(parent);
			}
		}

		public TermVariable getProgenitor(final TermVariable tv) {
			return mInstance2Representative.get(tv);
		}

		public Set<TermVariable> getInstances() {
			return mInstance2Representative.keySet();
		}

		/**
		 * An array generation is a set of arrays whose equality is implied by
		 * the disjunct.
		 * 
		 */
		private class ArrayGeneration {
			private final Set<TermVariable> mArrays = new HashSet<>();
			private TermVariable mRepresentative;
			private final TransFormulaLR mTransFormula;

			public ArrayGeneration(final TransFormulaLR tf, final TermVariable array) {
				mTransFormula = tf;
				add(array);
			}

			public TermVariable getRepresentative() {
				if (mRepresentative == null) {
					determineRepresentative();
				}
				return mRepresentative;
			}

			private void determineRepresentative() {
				for (final TermVariable array : mArrays) {
					if (TransFormulaUtils.isInvar(array, mTransFormula)) {
						mRepresentative = array;
						return;
					}
				}
				// no inVar, take some element
				mRepresentative = mArrays.iterator().next();
			}

			public void add(final TermVariable array) {
				mArray2Generation.put(array, this);
				if (mRepresentative != null) {
					throw new AssertionError("has already representative, cannot modify");
				}
				mArrays.add(array);
			}

			@Override
			public String toString() {
				return "ArrayGeneration [Arrays=" + mArrays + ", Representative=" + mRepresentative + "]";
			}

		}
	}
	
	private void constructSubstitutions() {
		final Map<Term, Term> in2outMapping = new HashMap<Term, Term>();
		final Map<Term, Term> out2inMapping = new HashMap<Term, Term>();
		for (final RankVar rv : mTransFormulaLR.getInVars().keySet()) {
			final Term inVar = mTransFormulaLR.getInVars().get(rv);
			assert inVar != null;
			final Term outVar = mTransFormulaLR.getOutVars().get(rv);
			assert outVar != null;
			in2outMapping.put(inVar, outVar);
			out2inMapping.put(outVar, inVar);
		}
		mInVars2OutVars = new SafeSubstitution(mScript, in2outMapping);
		mOutVars2InVars = new SafeSubstitution(mScript, out2inMapping);
	}

	private class IndexCollector {
		private final TransFormulaLR mTransFormula;


		public IndexCollector(final TransFormulaLR tf, final HashRelation<TermVariable, ArrayIndex> foreignIndices) {
			mTransFormula = tf;
			mArrayFirstGeneration2Indices = new HashRelation<Term, ArrayIndex>();
			for (int i = 0; i < sunnf.length; i++) {
				for (final ArrayUpdate au : mArrayUpdates.get(i)) {
					final TermVariable firstGeneration = mArrayGenealogy[i].getProgenitor((TermVariable) au.getOldArray());
					final ArrayIndex index = au.getIndex();
					addFirstGenerationIndexPair(firstGeneration, index);
				}
				for (final MultiDimensionalSelect ar : mArrayReads.get(i)) {
					final TermVariable firstGeneration = mArrayGenealogy[i].getProgenitor((TermVariable) ar.getArray());
					final ArrayIndex index = ar.getIndex();
					addFirstGenerationIndexPair(firstGeneration, index);
				}
			}
			if (foreignIndices != null) {
				for (final TermVariable arrayInVar : foreignIndices.getDomain()) {
					TermVariable firstGenerationArray = null;
					for (final ArrayGenealogy ag : mArrayGenealogy) {
						firstGenerationArray = ag.getProgenitor(arrayInVar);
						if (firstGenerationArray != null) {
							break;
						}
					}
					assert firstGenerationArray != null : arrayInVar + " has no progenitor";
					if (firstGenerationArray != arrayInVar) {
						assert occursInArrayEqualities(arrayInVar) : 
							"if arrayInVar of foreign index is not first generation it has to occur in array equality";
						assert occursInArrayEqualities(firstGenerationArray) : 
							"if arrayInVar of foreign index is not first generation the first generation has to occur in array equality";
					}
					final Set<ArrayIndex> foreignIndicesForInVar = foreignIndices.getImage(arrayInVar);
					for (final ArrayIndex foreignIndex : foreignIndicesForInVar) {
						addFirstGenerationIndexPair(firstGenerationArray, foreignIndex);
					}
				}
			}
		}

		/**
		 * Returns true iff arrayInstance occurs in some array equality.
		 */
		private boolean occursInArrayEqualities(final TermVariable arrayInstance) {
			for (final List<ArrayEquality> equalitiesOfDisjunct : mArrayEqualities) {
				for (final ArrayEquality ae : equalitiesOfDisjunct) {
					if (ae.getLhs() == arrayInstance) {
						return true;
					}
					if (ae.getRhs() == arrayInstance) {
						return true;
					}
				}
			}
			return false;
		}

		private void addFirstGenerationIndexPair(final TermVariable firstGeneration, final ArrayIndex index) {
			mArrayFirstGeneration2Indices.addPair(firstGeneration, index);
			if (mTransFormulaLR.getInVarsReverseMapping().containsKey(firstGeneration)) {
				if (TransFormulaUtils.allVariablesAreInVars(index, getTransFormulaLR())) {
					final ArrayIndex inReplacedByOut = new ArrayIndex(SmtUtils.substitutionElementwise(index, mInVars2OutVars));
					mArrayFirstGeneration2Indices.addPair(firstGeneration, inReplacedByOut);
					mAdditionalArrayReads.addAll(extractArrayReads(inReplacedByOut));
				}
				if (TransFormulaUtils.allVariablesAreOutVars(index, getTransFormulaLR())) {
					final ArrayIndex outReplacedByIn = new ArrayIndex(SmtUtils.substitutionElementwise(index, mOutVars2InVars));
					mArrayFirstGeneration2Indices.addPair(firstGeneration, outReplacedByIn);
					mAdditionalArrayReads.addAll(extractArrayReads(outReplacedByIn));
				}

				
			}
		}

		/**
		 * Returns true iff all TermVariables that occur in index also occur
		 * in the Term of TransFormulaLR.
		 */
		private boolean allVariablesOccurInFormula(final ArrayIndex index,
				final TransFormulaLR transFormulaLR) {
			final HashSet<TermVariable> varsInTransFormula = new HashSet<TermVariable>(
					Arrays.asList(transFormulaLR.getFormula().getFreeVars()));
			for (final Term term : index) {
				for (final TermVariable tv : term.getFreeVars()) {
					if (!varsInTransFormula.contains(tv)) {
						return false;
					}
				}
			}
			return true;
		}

		private List<MultiDimensionalSelect> extractArrayReads(final List<Term> terms) {
			final ArrayList<MultiDimensionalSelect> result = new ArrayList<>();
			for (final Term term : terms) {
				result.addAll(MultiDimensionalSelect.extractSelectDeep(term, true));
			}
			return result;
		}

	}


	
	
	public void computeInVarAndOutVarArrayCells() {
//		HashRelation<TermVariable, ArrayIndex> cellVarRepresentatives = new HashRelation<>();
		for (final TermVariable firstGeneration : getArrayFirstGeneration2Instances().getDomain()) {
			for (final TermVariable instance : getArrayFirstGeneration2Instances().getImage(firstGeneration)) {
				final Set<ArrayIndex> indicesOfAllGenerations = getArrayFirstGeneration2Indices().getImage(firstGeneration);
				if (indicesOfAllGenerations == null) {
					mLogger.info("Array " + firstGeneration + " is never accessed");
					continue;
				}
				for (final ArrayIndex index : indicesOfAllGenerations) {
					final boolean requiresRepVar = requiresRepVar(instance, index);
					if (requiresRepVar) {
						final TermVariable arrayRepresentative = (TermVariable) TransFormulaUtils.getDefinition(mTransFormulaLR, instance);
						final ArrayIndex indexRepresentative = getOrConstructIndexRepresentative(index);
						{
							final TermVariable inVarInstance = computeInVarInstance(instance);
							assert getTransFormulaLR().getInVarsReverseMapping().containsKey(inVarInstance);
							final ArrayIndex inVarIndex = computeInVarIndex(index);
							assert TransFormulaUtils.allVariablesAreInVars(inVarIndex, getTransFormulaLR());
							final ArrayCellReplacementVarInformation acrvi = 
									new ArrayCellReplacementVarInformation(
											inVarInstance, arrayRepresentative, 
											inVarIndex, indexRepresentative, 
											VarType.InVar, getTransFormulaLR());
							mArrayCellInVars.put(arrayRepresentative, indexRepresentative, acrvi);

						}
						{
							final TermVariable outVarInstance = computeOutVarInstance(instance);
							assert getTransFormulaLR().getOutVarsReverseMapping().containsKey(outVarInstance);
							final ArrayIndex outVarIndex = computeOutVarIndex(index);
							assert TransFormulaUtils.allVariablesAreOutVars(outVarIndex, getTransFormulaLR());
							final ArrayCellReplacementVarInformation acrvi = 
									new ArrayCellReplacementVarInformation(
											outVarInstance, arrayRepresentative, 
											outVarIndex, indexRepresentative, 
											VarType.OutVar, getTransFormulaLR());
							mArrayCellOutVars.put(arrayRepresentative, indexRepresentative, acrvi);
						}


					}
					
//					boolean isInVarCell = this.isInVarCell(instance, index);
//					boolean isOutVarCell = this.isOutVarCell(instance, index);
//					if (isInVarCell || isOutVarCell) {
//						if (isInVarCell) {
//							ArrayCellReplacementVarInformation acrvi = 
//									new ArrayCellReplacementVarInformation(
//											instance, arrayRepresentative, 
//											index, indexRepresentative, 
//											VarType.InVar, this.getTransFormulaLR());
//							mArrayCellInVars.put(arrayRepresentative, indexRepresentative, acrvi);
//						}
//						if (isOutVarCell) {
//							ArrayCellReplacementVarInformation acrvi = 
//									new ArrayCellReplacementVarInformation(
//											instance, arrayRepresentative, 
//											index, indexRepresentative, 
//											VarType.OutVar, this.getTransFormulaLR());
//							mArrayCellOutVars.put(arrayRepresentative, indexRepresentative, acrvi);
//						}
//					} 
				}

			}
		}


	}
	
	private ArrayIndex computeInVarIndex(final ArrayIndex index) {
		final List<Term> inVarIndex = mOutVars2InVars.transform(index);
		return new ArrayIndex(inVarIndex);
	}
	
	private ArrayIndex computeOutVarIndex(final ArrayIndex index) {
		final List<Term> inVarIndex = mInVars2OutVars.transform(index);
		return new ArrayIndex(inVarIndex);
	}
	
	private TermVariable computeInVarInstance(final TermVariable arrayInstance) {
		final TermVariable result = (TermVariable) mOutVars2InVars.transform(arrayInstance);
		return result;
	}
	
	private TermVariable computeOutVarInstance(final TermVariable arrayInstance) {
		final TermVariable result = (TermVariable) mInVars2OutVars.transform(arrayInstance);
		return result;
	}




	public NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation> getArrayCellInVars() {
		return mArrayCellInVars;
	}



	public NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation> getArrayCellOutVars() {
		return mArrayCellOutVars;
	}
	
	
	public boolean requiresRepVar(final TermVariable arrayInstance, final ArrayIndex index) {
		// check if arrayInstance is inVar or outVar and if all indices are inVars or outVars
		if (getTransFormulaLR().getOutVarsReverseMapping().keySet().contains(arrayInstance) || 
				getTransFormulaLR().getInVarsReverseMapping().keySet().contains(arrayInstance)) {
			return TransFormulaUtils.allVariablesAreVisible(index, getTransFormulaLR());
		} else {
			return false;
		}
	}

	
	
	/**
	 * Is the cellVariable that we construct for arrayInstance[index] is an
	 * inVar. This is the case if arrayInstance and each free variable of
	 * index is an inVar.
	 */
	public boolean isInVarCell(final TermVariable arrayInstance, final ArrayIndex index) {
		if (TransFormulaUtils.isInvar(arrayInstance, getTransFormulaLR())) {
			return TransFormulaUtils.allVariablesAreInVars(index, getTransFormulaLR());
		} else {
			return false;
		}
	}

	public boolean isOutVarCell(final TermVariable arrayInstance, final ArrayIndex index) {
		if (TransFormulaUtils.isOutvar(arrayInstance, getTransFormulaLR())) {
			return TransFormulaUtils.allVariablesAreOutVars(index, getTransFormulaLR());
		} else {
			return false;
		}
	}


	private static TransFormulaLR constructTransFormulaLRWInSunf(final IUltimateServiceProvider services, 
			final XnfConversionTechnique xnfConversionTechnique, final SimplicationTechnique simplificationTechnique, 
			final ILogger logger, final IFreshTermVariableConstructor ftvc, final ReplacementVarFactory repVarFactory, 
			final Script script, final TransFormulaLR tf, 
			final List<List<ArrayEquality>> arrayEqualities, final SingleUpdateNormalFormTransformer... sunfts) {
		final TransFormulaLR result = new TransFormulaLR(tf);
		final List<Term> disjuncts = new ArrayList<Term>();
		assert arrayEqualities.size() == sunfts.length;
		for (int i=0; i<sunfts.length; i++) {
			final List<Term> conjuncts = new ArrayList<>();
			for (final ArrayUpdate au : sunfts[i].getArrayUpdates()) {
				conjuncts.add(au.getArrayUpdateTerm());
			}
			for (final ArrayEquality ae : arrayEqualities.get(i)) {
				conjuncts.add(ae.getOriginalTerm());
			}
			conjuncts.add(sunfts[i].getRemainderTerm());
			Term disjunct = SmtUtils.and(script, conjuncts);
			final Set<TermVariable> auxVars = new HashSet<>(sunfts[i].getAuxVars());
			disjunct = PartialQuantifierElimination.elim(script, QuantifiedFormula.EXISTS, auxVars, disjunct, services, logger, ftvc, simplificationTechnique, xnfConversionTechnique); 
			disjuncts.add(disjunct);
			final Map<TermVariable, Term> auxVar2Const = repVarFactory.constructAuxVarMapping(auxVars);
			result.addAuxVars(auxVar2Const);
		}
		final Term resultTerm = SmtUtils.or(script, disjuncts);
		result.setFormula(resultTerm);
		return result;
	}

}
