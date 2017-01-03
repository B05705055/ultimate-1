/*
 * Copyright (C) 2016 Alexander Nutz (nutz@informatik.uni-freiburg.de)
 * Copyright (C) 2016 University of Freiburg
 *
 * This file is part of the ULTIMATE HeapSeparator plug-in.
 *
 * The ULTIMATE HeapSeparator plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE HeapSeparator plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE HeapSeparator plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE HeapSeparator plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE HeapSeparator plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.heapseparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermTransformer;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgEdge;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.UnmodifiableTransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVarOrConst;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.Substitution;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.ArrayEquality;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.managedscript.ManagedScript;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.VPDomainPreanalysis;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.VPDomain;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.VPDomainHelpers;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.VPDomainSymmetricPair;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.elements.ConstOrLiteral;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.irsdependencies.rcfg.visitors.SimpleRCFGVisitor;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.AbstractRelation;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.HashRelation;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.NestedMap2;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Pair;

/**
 * Does a preanalysis on the program before the actual heap separation is done (using the
 * abstract interpretation result from the equality domain).
 * Computes:
 *  - which arrays are equated, anywhere in the program (occur left and right each of an equality in a TransFormula)
 *  - for each array in the program the locations where it is accessed
 *     (question: does this mean that large block encoding is hurtful for heapseparation?)
 * 
 * 
 * @author Alexander Nutz
 *
 */
public class HeapSepPreAnalysisVisitor extends SimpleRCFGVisitor {

	private final HashRelation<IProgramVarOrConst, IcfgLocation> mArrayToAccessLocations;

	private final Set<VPDomainSymmetricPair<IProgramVarOrConst>> mArrayEqualities;

	private final ManagedScript mScript;

	private VPDomainPreanalysis mVpDomainPreAnalysis;

	/**
	 * The HeapSepPreAnalysisVisitor computes and provides the following information:
	 *  - which arrays (base arrays, not store terms) are equated in the program
	 *  - for each array at which locations in the CFG it is accessed
	 * @param logger
	 */
	public HeapSepPreAnalysisVisitor(ILogger logger, ManagedScript script, VPDomain domain) {
		super(logger);
		mArrayToAccessLocations = new HashRelation<>();
		mScript = script;
		mArrayEqualities = new HashSet<>();
		mVpDomainPreAnalysis = domain.getPreAnalysis();
	}

	@Override
	public void level(IcfgEdge edge) {
		
		if (edge instanceof CodeBlock) {

			UnmodifiableTransFormula tf = ((CodeBlock) edge).getTransformula();

			List<ArrayEquality> aeqs = ArrayEquality.extractArrayEqualities(tf.getFormula());
			for (ArrayEquality aeq : aeqs) {
				IProgramVarOrConst first = mVpDomainPreAnalysis.getIProgramVarOrConstOrLiteral(
						aeq.getLhs(), 
						VPDomainHelpers.computeProgramVarMappingFromTransFormula(tf));
				IProgramVarOrConst second = mVpDomainPreAnalysis.getIProgramVarOrConstOrLiteral(
						aeq.getRhs(), 
						VPDomainHelpers.computeProgramVarMappingFromTransFormula(tf));
				
				mArrayEqualities.add(new VPDomainSymmetricPair<IProgramVarOrConst>(first, second));
			}

			mArrayToAccessLocations.addAll(findArrayAccesses((CodeBlock) edge));
		}
		super.level(edge);
	}
	
	private HashRelation<IProgramVarOrConst, IcfgLocation> findArrayAccesses(CodeBlock edge) {
		HashRelation<IProgramVarOrConst, IcfgLocation> result = new HashRelation<>();
		
		for (Entry<IProgramVar, TermVariable> en : edge.getTransformula().getInVars().entrySet()) {
			IProgramVar pv = en.getKey();
			if (!pv.getTermVariable().getSort().isArraySort()) {
				continue;
			}
			if (!mVpDomainPreAnalysis.isArrayTracked(pv)) {
				continue;
			}
			// we have an array variable --> store that it occurs after the source location of the edge
			result.addPair(pv, edge.getSource());
		}
		for (Entry<IProgramVar, TermVariable> en : edge.getTransformula().getOutVars().entrySet()) {
			IProgramVar pv = en.getKey();
			if (!pv.getTermVariable().getSort().isArraySort()) {
				continue;
			}
			if (!mVpDomainPreAnalysis.isArrayTracked(pv)) {
				continue;
			}
			// we have an array variable --> store that it occurs after the source location of the edge
			result.addPair(pv, edge.getSource());
		}	
		return result;
	}
	
	Set<VPDomainSymmetricPair<IProgramVarOrConst>> getArrayEqualities() {
		return mArrayEqualities;
	}

	@Override
	public boolean performedChanges() {
		// this visitor is only for analysis, it should not change anything
		return false;
	}

	@Override
	public boolean abortCurrentBranch() {
		return false;
	}

	@Override
	public boolean abortAll() {
		return false;
	}
	
	HashRelation<IProgramVarOrConst, IcfgLocation> getArrayToAccessLocations() {
		return mArrayToAccessLocations;
	}
}
