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
package de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.rewriteArrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lassoranker.Activator;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.TransFormulaUtils;
import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.rewriteArrays.ArrayCellReplacementVarInformation.VarType;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.RankVar;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.ReplacementVarFactory;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.TransFormulaLR;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Util;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.IFreshTermVariableConstructor;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SafeSubstitution;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.ArrayEquality;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.ArrayEquality.ArrayEqualityExtractor;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.ArrayIndex;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.ArrayUpdate;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.MultiDimensionalSelect;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.normalForms.Dnf;
import de.uni_freiburg.informatik.ultimate.util.HashRelation;
import de.uni_freiburg.informatik.ultimate.util.UnionFind;
import de.uni_freiburg.informatik.ultimate.util.relation.NestedMap2;
import de.uni_freiburg.informatik.ultimate.util.relation.Triple;

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

	private final Logger mLogger;
	private final IUltimateServiceProvider mServices;
	
	private final boolean m_ContainsArrays;

	static final String s_AuxArray = "auxArray";

	/**
	 * The script used to transform the formula
	 */
	private final Script m_Script;
	private final IFreshTermVariableConstructor m_FreshTermVariableConstructor;

	/**
	 * Mapping from the first generation of an array to all indices that
	 * occur in instances of the same array.
	 */
	private HashRelation<TermVariable, ArrayIndex> m_ArrayFirstGeneration2Indices;
	private final HashRelation<TermVariable, TermVariable> m_ArrayFirstGeneration2Instances;
	private final Map<ArrayIndex, ArrayIndex> m_IndexInstance2IndexRepresentative = new HashMap<>();
	private final List<List<ArrayUpdate>> m_ArrayUpdates;
	private final List<List<MultiDimensionalSelect>> m_ArrayReads;
	/**
	 * Array reads that are added while constructing additional in/out vars.
	 */
	private final List<MultiDimensionalSelect> m_AdditionalArrayReads = new ArrayList<>();
	private final ArrayGenealogy[] m_ArrayGenealogy;
	private final Term[] sunnf;
	private final List<List<ArrayEquality>> m_ArrayEqualities;

	private final TransFormulaLR m_TransFormulaLR;
	private final ReplacementVarFactory m_ReplacementVarFactory;
	
	private final NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation> m_ArrayCellInVars = 
			new NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation>();
	private final NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation> m_ArrayCellOutVars = 
			new NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation>();
	
	private SafeSubstitution m_InVars2OutVars;
	private SafeSubstitution m_OutVars2InVars;
	
	
	
	public TransFormulaLRWithArrayInformation(
			IUltimateServiceProvider services, 
			TransFormulaLR transFormulaLR, 
			ReplacementVarFactory replacementVarFactory, Script script, 
			IFreshTermVariableConstructor freshTermVariableConstructor, 
			TransFormulaLRWithArrayInformation stem) {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(Activator.s_PLUGIN_ID);
 		m_TransFormulaLR = transFormulaLR;
 		m_Script = script;
		m_FreshTermVariableConstructor = freshTermVariableConstructor;
		m_ReplacementVarFactory = replacementVarFactory;
		if (!SmtUtils.containsArrayVariables(m_TransFormulaLR.getFormula())) {
			m_ContainsArrays = false;
			sunnf = null;
			m_ArrayUpdates = null;
			m_ArrayReads = null;
			m_ArrayEqualities = null;
			m_ArrayGenealogy = null;
			m_ArrayFirstGeneration2Instances = null;
		} else {
			m_ContainsArrays = true;
			Term term = SmtUtils.simplify(m_Script, m_TransFormulaLR.getFormula(), mServices);
			Term dnf = (new Dnf(m_Script, mServices, m_FreshTermVariableConstructor)).transform(term);
			dnf = SmtUtils.simplify(m_Script, dnf, mServices);
			Term[] disjuncts = SmtUtils.getDisjuncts(dnf);
			sunnf = new Term[disjuncts.length];
			m_ArrayUpdates = new ArrayList<List<ArrayUpdate>>(disjuncts.length);
			m_ArrayReads = new ArrayList<List<MultiDimensionalSelect>>(disjuncts.length);
			m_ArrayEqualities = new ArrayList<List<ArrayEquality>>(disjuncts.length);
			m_ArrayGenealogy = new ArrayGenealogy[disjuncts.length];
			for (int i = 0; i < disjuncts.length; i++) {
				Term[] conjuncts = SmtUtils.getConjuncts(disjuncts[i]);
				ArrayEqualityExtractor aee = new ArrayEqualityExtractor(conjuncts);
				m_ArrayEqualities.add(aee.getArrayEqualities());
				SingleUpdateNormalFormTransformer sunft = new SingleUpdateNormalFormTransformer(Util.and(m_Script, aee
						.getRemainingTerms().toArray(new Term[0])), m_Script, m_ReplacementVarFactory);
				m_ArrayUpdates.add(sunft.getArrayUpdates());
				sunnf[i] = sunft.getRemainderTerm();
				m_ArrayReads.add(extractArrayReads(sunft.getArrayUpdates(), sunft.getRemainderTerm()));
				m_ArrayGenealogy[i] = new ArrayGenealogy(m_TransFormulaLR, m_ArrayEqualities.get(i), m_ArrayUpdates.get(i), m_ArrayReads.get(i));
			}
			constructSubstitutions();
			final HashRelation<TermVariable, ArrayIndex> foreignIndices;
			if (stem == null) {
				foreignIndices = null;
			} else {
				foreignIndices = computeForeignIndices(stem);
			}
			new IndexCollector(m_TransFormulaLR, foreignIndices);
			m_ArrayFirstGeneration2Instances = computeArrayFirstGeneration2Instances();
			computeInVarAndOutVarArrayCells();
		}
	}
	
	
	
	private HashRelation<TermVariable, ArrayIndex> computeForeignIndices(TransFormulaLRWithArrayInformation stem) {
		HashRelation<TermVariable, ArrayIndex> arrayInVar2ForeignIndices = new HashRelation<>();
		for (Triple<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation> triple : stem.getArrayCellOutVars().entrySet()) {
			ArrayCellReplacementVarInformation acrvi = triple.getThird();
			RankVar arrayRv = acrvi.getArrayRankVar();
			TermVariable arrayInVar = (TermVariable) m_TransFormulaLR.getInVars().get(arrayRv);
			if (arrayInVar != null) {
				// array also occurs in loop, we have to add the index
				// of this ArrayCellReplacement
				ArrayIndex foreignIndex = computeForeignIndex(arrayRv, acrvi.getIndex(), acrvi.termVariableToRankVarMappingForIndex());
				assert (TransFormulaUtils.allVariablesAreInVars(foreignIndex, m_TransFormulaLR));
				if (mLogger.isDebugEnabled()) {
					mLogger.debug("Adding foreign index " + foreignIndex + " for array " + arrayInVar);
				}
				arrayInVar2ForeignIndices.addPair(arrayInVar, foreignIndex);
			}
		}
		return arrayInVar2ForeignIndices;
	}



	private ArrayIndex computeForeignIndex(RankVar arrayRv, ArrayIndex index,
			Map<TermVariable, RankVar> termVariableToRankVarMappingForIndex) {
		Map<Term, Term> substitutionMapping = new HashMap<Term, Term>();
		for (Entry<TermVariable, RankVar> foreigntv2rv : termVariableToRankVarMappingForIndex.entrySet()) {
			if (!m_TransFormulaLR.getInVars().containsKey(foreigntv2rv.getValue())) {
				addForeignInVarAndOutVar(foreigntv2rv.getValue());
			}
			TermVariable ourInVar = (TermVariable) m_TransFormulaLR.getInVars().get(foreigntv2rv.getValue());
			substitutionMapping.put(foreigntv2rv.getKey(), ourInVar);
		}
		List<Term> translatedIndex = (new SafeSubstitution(m_Script, substitutionMapping)).transform(index);
		ArrayIndex foreignIndex = new ArrayIndex(translatedIndex);
		return foreignIndex;
	}



	private void addForeignInVarAndOutVar(RankVar value) {
		String name = value.getGloballyUniqueId() + "_ForeignInOutVar";
		Sort sort = value.getDefinition().getSort();
		TermVariable inOutVar = m_FreshTermVariableConstructor.constructFreshTermVariable(name, sort);
		assert !m_TransFormulaLR.getInVars().containsKey(value);
		m_TransFormulaLR.addInVar(value, inOutVar);
		assert !m_TransFormulaLR.getOutVars().containsKey(value);
		m_TransFormulaLR.addOutVar(value, inOutVar);
	}



	public boolean containsArrays() {
		return m_ContainsArrays;
	}



	private HashRelation<TermVariable, TermVariable> computeArrayFirstGeneration2Instances() {
		HashRelation<TermVariable, TermVariable> result = new HashRelation<TermVariable, TermVariable>();
		for (int i = 0; i < this.numberOfDisjuncts(); i++) {
			for (TermVariable instance : m_ArrayGenealogy[i].getInstances()) {
				TermVariable firstGeneration = m_ArrayGenealogy[i].getProgenitor(instance);
				result.addPair(firstGeneration, instance);
			}
		}
		return result;
	}

	public HashRelation<TermVariable, ArrayIndex> getArrayFirstGeneration2Indices() {
		return m_ArrayFirstGeneration2Indices;
	}
	
	public HashRelation<TermVariable, TermVariable> getArrayFirstGeneration2Instances() {
		return m_ArrayFirstGeneration2Instances;
	}

	public List<List<ArrayUpdate>> getArrayUpdates() {
		return m_ArrayUpdates;
	}
	
	public List<List<MultiDimensionalSelect>> getArrayReads() {
		return m_ArrayReads;
	}

	public List<MultiDimensionalSelect> getAdditionalArrayReads() {
		return m_AdditionalArrayReads;
	}
	
	public List<List<ArrayEquality>> getArrayEqualities() {
		return m_ArrayEqualities;
	}

	public int numberOfDisjuncts() {
		return sunnf.length;
	}

	public Term[] getSunnf() {
		return sunnf;
	}
	
	public TransFormulaLR getTransFormulaLR() {
		return m_TransFormulaLR;
	}
	
	public ArrayIndex getOrConstructIndexRepresentative(ArrayIndex indexInstance) {
		ArrayIndex indexRepresentative = m_IndexInstance2IndexRepresentative.get(indexInstance);
		if (indexRepresentative == null) {
			indexRepresentative = new ArrayIndex(TransFormulaUtils.translateTermVariablesToDefinitions(m_Script, m_TransFormulaLR, indexInstance));
			m_IndexInstance2IndexRepresentative.put(indexInstance, indexRepresentative);
		}
		return indexRepresentative;
	}



	private List<MultiDimensionalSelect> extractArrayReads(List<ArrayUpdate> arrayUpdates, Term remainderTerm) {
		ArrayList<MultiDimensionalSelect> result = new ArrayList<>();
		for (ArrayUpdate au : arrayUpdates) {
			for (Term indexEntry : au.getIndex()) {
				result.addAll(MultiDimensionalSelect.extractSelectDeep(indexEntry, true));
			}
			result.addAll(MultiDimensionalSelect.extractSelectDeep(au.getValue(), true));
		}
		result.addAll(MultiDimensionalSelect.extractSelectDeep(remainderTerm, true));
		return result;
	}


	private class ArrayGenealogy {
		Map<ArrayGeneration, ArrayGeneration> m_Generation2OriginalGeneration = new HashMap<ArrayGeneration, ArrayGeneration>();

		Map<TermVariable, TermVariable> m_Instance2Representative = new HashMap<TermVariable, TermVariable>();

		/**
		 * If array a2 is defined as a2 = ("store", a1, index, value) we call a1
		 * the parent generation of a2.
		 */
		Map<ArrayGeneration, ArrayGeneration> m_ParentGeneration = new HashMap<ArrayGeneration, ArrayGeneration>();

		Map<TermVariable, ArrayGeneration> m_Array2Generation = new HashMap<TermVariable, ArrayGeneration>();

		List<ArrayGeneration> m_ArrayGenerations = new ArrayList<>();

		private final TransFormulaLR m_TransFormula;
		
		private ArrayGeneration getOrConstructArrayGeneration(TermVariable array) {
			ArrayGeneration ag = m_Array2Generation.get(array);
			if (ag == null) {
				ag = new ArrayGeneration(m_TransFormula, array);
				m_ArrayGenerations.add(ag);
			}
			return ag;
		}

		ArrayGenealogy(TransFormulaLR tf, List<ArrayEquality> arrayEqualities, List<ArrayUpdate> arrayUpdates,
				List<MultiDimensionalSelect> arrayReads) {
			m_TransFormula = tf;
			UnionFind<TermVariable> uf = new UnionFind<>();
			for (ArrayEquality ae : arrayEqualities) {
				TermVariable lhs = ae.getLhs();
				TermVariable rhs = ae.getRhs();
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
			for (TermVariable representative : uf.getAllRepresentatives()) {
				ArrayGeneration ag = getOrConstructArrayGeneration(representative);
				for (TermVariable array : uf.getEquivalenceClassMembers(representative)) {
					if (array != representative) {
						ag.add(array);
					}
				}
			}

			for (ArrayUpdate au : arrayUpdates) {
				ArrayGeneration oldGeneration = getOrConstructArrayGeneration((TermVariable) au.getOldArray());
				ArrayGeneration newGeneration = getOrConstructArrayGeneration(au.getNewArray());
				if (oldGeneration == newGeneration) {
					mLogger.warn("self update, this is not tested very well ");
				} else {
					putParentGeneration(newGeneration, oldGeneration);
				}
			}
			for (ArrayGeneration ag : m_ArrayGenerations) {
				ArrayGeneration fg = getFirstGeneration(ag);
				putInstance2FirstGeneration(ag, fg);
			}
			for (MultiDimensionalSelect ar : arrayReads) {
				determineRepresentative((TermVariable) ar.getArray());
			}
			for (ArrayEquality ae : arrayEqualities) {
				determineRepresentative(ae.getLhs());
				determineRepresentative(ae.getRhs());
			}
			for (ArrayUpdate au : arrayUpdates) {
				determineRepresentative(au.getNewArray());
				determineRepresentative((TermVariable) au.getOldArray());
			}
		}

		private void determineRepresentative(TermVariable array) {
			if (m_Instance2Representative.containsKey(array)) {
				// already has a representative
				return;
			}
			ArrayGeneration ag = m_Array2Generation.get(array);
			if (ag == null) {
				// occurs only in select, is its own representative
				m_Instance2Representative.put(array, array);
			} else {
				ArrayGeneration fg = m_Generation2OriginalGeneration.get(ag);
				assert fg != null : "no original generation!";
				TermVariable representative = fg.getRepresentative();
				if (TransFormulaUtils.isInvar(representative, m_TransFormula)) {
					m_Instance2Representative.put(array, representative);
				} else {
					throw new AssertionError("no invar");
				}
			}
		}

		private void putParentGeneration(ArrayGeneration child, ArrayGeneration parent) {
			assert child != null;
			assert parent != null;
			assert child != parent;
			assert child.toString() != null;
			assert parent.toString() != null;
			m_ParentGeneration.put(child, parent);
		}

		private void putInstance2FirstGeneration(ArrayGeneration child, ArrayGeneration progenitor) {
			assert child != null;
			assert progenitor != null;
			assert child.toString() != null;
			assert progenitor.toString() != null;
			m_Generation2OriginalGeneration.put(child, progenitor);
		}

		private ArrayGeneration getFirstGeneration(ArrayGeneration ag) {
			ArrayGeneration parent = m_ParentGeneration.get(ag);
			if (parent == null) {
				return ag;
			} else {
				return getFirstGeneration(parent);
			}
		}

		public TermVariable getProgenitor(TermVariable tv) {
			return m_Instance2Representative.get(tv);
		}

		public Set<TermVariable> getInstances() {
			return m_Instance2Representative.keySet();
		}

		/**
		 * An array generation is a set of arrays whose equality is implied by
		 * the disjunct.
		 * 
		 */
		private class ArrayGeneration {
			private final Set<TermVariable> m_Arrays = new HashSet<>();
			private TermVariable m_Representative;
			private final TransFormulaLR m_TransFormula;

			public ArrayGeneration(TransFormulaLR tf, TermVariable array) {
				m_TransFormula = tf;
				this.add(array);
			}

			public TermVariable getRepresentative() {
				if (m_Representative == null) {
					determineRepresentative();
				}
				return m_Representative;
			}

			private void determineRepresentative() {
				for (TermVariable array : m_Arrays) {
					if (TransFormulaUtils.isInvar(array, m_TransFormula)) {
						m_Representative = array;
						return;
					}
				}
				// no inVar, take some element
				m_Representative = m_Arrays.iterator().next();
			}

			public void add(TermVariable array) {
				m_Array2Generation.put(array, this);
				if (m_Representative != null) {
					throw new AssertionError("has already representative, cannot modify");
				}
				m_Arrays.add(array);
			}

			@Override
			public String toString() {
				return "ArrayGeneration [Arrays=" + m_Arrays + ", Representative=" + m_Representative + "]";
			}

		}
	}
	
	private void constructSubstitutions() {
		Map<Term, Term> in2outMapping = new HashMap<Term, Term>();
		Map<Term, Term> out2inMapping = new HashMap<Term, Term>();
		for (RankVar rv : m_TransFormulaLR.getInVars().keySet()) {
			Term inVar = m_TransFormulaLR.getInVars().get(rv);
			assert inVar != null;
			Term outVar = m_TransFormulaLR.getOutVars().get(rv);
			assert outVar != null;
			in2outMapping.put(inVar, outVar);
			out2inMapping.put(outVar, inVar);
		}
		m_InVars2OutVars = new SafeSubstitution(m_Script, in2outMapping);
		m_OutVars2InVars = new SafeSubstitution(m_Script, out2inMapping);
	}

	private class IndexCollector {
		private final TransFormulaLR m_TransFormula;


		public IndexCollector(TransFormulaLR tf, HashRelation<TermVariable, ArrayIndex> foreignIndices) {
			m_TransFormula = tf;
			m_ArrayFirstGeneration2Indices = new HashRelation<TermVariable, ArrayIndex>();
			for (int i = 0; i < sunnf.length; i++) {
				for (ArrayUpdate au : m_ArrayUpdates.get(i)) {
					TermVariable firstGeneration = m_ArrayGenealogy[i].getProgenitor((TermVariable) au.getOldArray());
					ArrayIndex index = au.getIndex();
					addFirstGenerationIndexPair(firstGeneration, index);
				}
				for (MultiDimensionalSelect ar : m_ArrayReads.get(i)) {
					TermVariable firstGeneration = m_ArrayGenealogy[i].getProgenitor((TermVariable) ar.getArray());
					ArrayIndex index = ar.getIndex();
					addFirstGenerationIndexPair(firstGeneration, index);
				}
			}
			if (foreignIndices != null) {
				for (TermVariable arrayInVar : foreignIndices.getDomain()) {
					TermVariable firstGenerationArray = null;
					for (ArrayGenealogy ag : m_ArrayGenealogy) {
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
					Set<ArrayIndex> foreignIndicesForInVar = foreignIndices.getImage(arrayInVar);
					for (ArrayIndex foreignIndex : foreignIndicesForInVar) {
						addFirstGenerationIndexPair(firstGenerationArray, foreignIndex);
					}
				}
			}
		}

		/**
		 * Returns true iff arrayInstance occurs in some array equality.
		 */
		private boolean occursInArrayEqualities(TermVariable arrayInstance) {
			for (List<ArrayEquality> equalitiesOfDisjunct : m_ArrayEqualities) {
				for (ArrayEquality ae : equalitiesOfDisjunct) {
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

		private void addFirstGenerationIndexPair(TermVariable firstGeneration, ArrayIndex index) {
			m_ArrayFirstGeneration2Indices.addPair(firstGeneration, index);
			if (m_TransFormulaLR.getInVarsReverseMapping().containsKey(firstGeneration)) {
				if (TransFormulaUtils.allVariablesAreInVars(index, getTransFormulaLR())) {
					ArrayIndex inReplacedByOut = new ArrayIndex(SmtUtils.substitutionElementwise(index, m_InVars2OutVars));
					m_ArrayFirstGeneration2Indices.addPair(firstGeneration, inReplacedByOut);
					m_AdditionalArrayReads.addAll(extractArrayReads(inReplacedByOut));
				}
				if (TransFormulaUtils.allVariablesAreOutVars(index, getTransFormulaLR())) {
					ArrayIndex outReplacedByIn = new ArrayIndex(SmtUtils.substitutionElementwise(index, m_OutVars2InVars));
					m_ArrayFirstGeneration2Indices.addPair(firstGeneration, outReplacedByIn);
					m_AdditionalArrayReads.addAll(extractArrayReads(outReplacedByIn));
				}

				
			}
		}

		/**
		 * Returns true iff all TermVariables that occur in index also occur
		 * in the Term of TransFormulaLR.
		 */
		private boolean allVariablesOccurInFormula(ArrayIndex index,
				TransFormulaLR transFormulaLR) {
			HashSet<TermVariable> varsInTransFormula = new HashSet<TermVariable>(
					Arrays.asList(transFormulaLR.getFormula().getFreeVars()));
			for (Term term : index) {
				for (TermVariable tv : term.getFreeVars()) {
					if (!varsInTransFormula.contains(tv)) {
						return false;
					}
				}
			}
			return true;
		}

		private List<MultiDimensionalSelect> extractArrayReads(List<Term> terms) {
			ArrayList<MultiDimensionalSelect> result = new ArrayList<>();
			for (Term term : terms) {
				result.addAll(MultiDimensionalSelect.extractSelectDeep(term, true));
			}
			return result;
		}

	}


	
	
	public void computeInVarAndOutVarArrayCells() {
//		HashRelation<TermVariable, ArrayIndex> cellVarRepresentatives = new HashRelation<>();
		for (TermVariable firstGeneration : this.getArrayFirstGeneration2Instances().getDomain()) {
			for (TermVariable instance : this.getArrayFirstGeneration2Instances().getImage(firstGeneration)) {
				Set<ArrayIndex> indicesOfAllGenerations = this.getArrayFirstGeneration2Indices().getImage(firstGeneration);
				if (indicesOfAllGenerations == null) {
					mLogger.info("Array " + firstGeneration + " is never accessed");
					continue;
				}
				for (ArrayIndex index : indicesOfAllGenerations) {
					boolean requiresRepVar = requiresRepVar(instance, index);
					if (requiresRepVar) {
						TermVariable arrayRepresentative = (TermVariable) TransFormulaUtils.getDefinition(m_TransFormulaLR, instance);
						ArrayIndex indexRepresentative = this.getOrConstructIndexRepresentative(index);
						{
							TermVariable inVarInstance = computeInVarInstance(instance);
							assert getTransFormulaLR().getInVarsReverseMapping().containsKey(inVarInstance);
							ArrayIndex inVarIndex = computeInVarIndex(index);
							assert TransFormulaUtils.allVariablesAreInVars(inVarIndex, getTransFormulaLR());
							ArrayCellReplacementVarInformation acrvi = 
									new ArrayCellReplacementVarInformation(
											inVarInstance, arrayRepresentative, 
											inVarIndex, indexRepresentative, 
											VarType.InVar, this.getTransFormulaLR());
							m_ArrayCellInVars.put(arrayRepresentative, indexRepresentative, acrvi);

						}
						{
							TermVariable outVarInstance = computeOutVarInstance(instance);
							assert getTransFormulaLR().getOutVarsReverseMapping().containsKey(outVarInstance);
							ArrayIndex outVarIndex = computeOutVarIndex(index);
							assert TransFormulaUtils.allVariablesAreOutVars(outVarIndex, getTransFormulaLR());
							ArrayCellReplacementVarInformation acrvi = 
									new ArrayCellReplacementVarInformation(
											outVarInstance, arrayRepresentative, 
											outVarIndex, indexRepresentative, 
											VarType.OutVar, this.getTransFormulaLR());
							m_ArrayCellOutVars.put(arrayRepresentative, indexRepresentative, acrvi);
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
//							m_ArrayCellInVars.put(arrayRepresentative, indexRepresentative, acrvi);
//						}
//						if (isOutVarCell) {
//							ArrayCellReplacementVarInformation acrvi = 
//									new ArrayCellReplacementVarInformation(
//											instance, arrayRepresentative, 
//											index, indexRepresentative, 
//											VarType.OutVar, this.getTransFormulaLR());
//							m_ArrayCellOutVars.put(arrayRepresentative, indexRepresentative, acrvi);
//						}
//					} 
				}

			}
		}


	}
	
	private ArrayIndex computeInVarIndex(ArrayIndex index) {
		List<Term> inVarIndex = m_OutVars2InVars.transform(index);
		return new ArrayIndex(inVarIndex);
	}
	
	private ArrayIndex computeOutVarIndex(ArrayIndex index) {
		List<Term> inVarIndex = m_InVars2OutVars.transform(index);
		return new ArrayIndex(inVarIndex);
	}
	
	private TermVariable computeInVarInstance(TermVariable arrayInstance) {
		TermVariable result = (TermVariable) m_OutVars2InVars.transform(arrayInstance);
		return result;
	}
	
	private TermVariable computeOutVarInstance(TermVariable arrayInstance) {
		TermVariable result = (TermVariable) m_InVars2OutVars.transform(arrayInstance);
		return result;
	}




	public NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation> getArrayCellInVars() {
		return m_ArrayCellInVars;
	}



	public NestedMap2<TermVariable, ArrayIndex, ArrayCellReplacementVarInformation> getArrayCellOutVars() {
		return m_ArrayCellOutVars;
	}
	
	
	public boolean requiresRepVar(TermVariable arrayInstance, ArrayIndex index) {
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
	public boolean isInVarCell(TermVariable arrayInstance, ArrayIndex index) {
		if (TransFormulaUtils.isInvar(arrayInstance, getTransFormulaLR())) {
			return TransFormulaUtils.allVariablesAreInVars(index, getTransFormulaLR());
		} else {
			return false;
		}
	}

	public boolean isOutVarCell(TermVariable arrayInstance, ArrayIndex index) {
		if (TransFormulaUtils.isOutvar(arrayInstance, getTransFormulaLR())) {
			return TransFormulaUtils.allVariablesAreOutVars(index, getTransFormulaLR());
		} else {
			return false;
		}
	}




}