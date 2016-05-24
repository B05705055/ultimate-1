/*
 * Copyright (C) 2013-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck;

import java.util.SortedMap;

import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.ICallAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IInternalAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IReturnAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;

public class DefaultTransFormulas extends NestedFormulas<TransFormula, IPredicate> {
	
	private final ModifiableGlobalVariableManager mModifiableGlobalVariableManager;
	private final boolean mWithBranchEncoders;
	
	
	
	public ModifiableGlobalVariableManager getModifiableGlobalVariableManager() {
		return mModifiableGlobalVariableManager;
	}

	public DefaultTransFormulas(NestedWord<? extends IAction> nestedWord, 
			IPredicate precondition, IPredicate postcondition,
			SortedMap<Integer, IPredicate> pendingContexts,
			ModifiableGlobalVariableManager modifiableGlobalVariableManager,
			boolean withBranchEncoders) {
		super(nestedWord, pendingContexts);
		super.setPrecondition(precondition);
		super.setPostcondition(postcondition);
		mModifiableGlobalVariableManager = modifiableGlobalVariableManager;
		mWithBranchEncoders = withBranchEncoders;
	}
	
	public boolean hasBranchEncoders() {
		return mWithBranchEncoders;
	}
	
	@Override
	protected TransFormula getFormulaFromValidNonCallPos(int i) {
		if (super.getTrace().isReturnPosition(i)) {
			IReturnAction ret = (IReturnAction) super.getTrace().getSymbolAt(i);
			return ret.getAssignmentOfReturn();
		} else {
			IInternalAction cb = (IInternalAction) super.getTrace().getSymbolAt(i);
			if (mWithBranchEncoders) {
				return ((CodeBlock) cb).getTransitionFormulaWithBranchEncoders();
			} else {
				return cb.getTransformula();
			}
		}
	}

	@Override
	protected TransFormula getLocalVarAssignmentFromValidPos(int i) {
		ICallAction cb = (ICallAction) super.getTrace().getSymbolAt(i);
		return cb.getLocalVarsAssignment();
	}

	@Override
	protected TransFormula getGlobalVarAssignmentFromValidPos(int i) {
		String calledProcedure = getCalledProcedure(i);
		return mModifiableGlobalVariableManager.getGlobalVarsAssignment(calledProcedure);

	}

	@Override
	protected TransFormula getOldVarAssignmentFromValidPos(int i) {		
		String calledProcedure = getCalledProcedure(i);
		return mModifiableGlobalVariableManager.getOldVarsAssignment(calledProcedure);
	}
	
	/**
	 * TODO: return set of all pending calls in case of InterproceduralSequentialComposition
	 */
	private String getCalledProcedure(int i) {
		if (super.getTrace().isCallPosition(i)) {
			ICallAction call = (ICallAction) super.getTrace().getSymbolAt(i);
			return call.getSucceedingProcedure();
		} else if (super.getTrace().isPendingReturn(i)) {
			IReturnAction ret = (IReturnAction) super.getTrace().getSymbolAt(i);
			return ret.getPreceedingProcedure();
		} else {
			throw new UnsupportedOperationException("only available for call and pending return");
		}
	}


}
