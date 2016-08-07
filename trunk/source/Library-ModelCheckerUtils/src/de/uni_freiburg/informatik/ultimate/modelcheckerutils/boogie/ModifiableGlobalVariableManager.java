/*
 * Copyright (C) 2013-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE ModelCheckerUtils Library.
 * 
 * The ULTIMATE ModelCheckerUtils Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE ModelCheckerUtils Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE ModelCheckerUtils Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE ModelCheckerUtils Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE ModelCheckerUtils Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Util;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormula.Infeasibility;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormulaBuilder;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramNonOldVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramOldVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;

/**
 * Contains information about modifiable global variables and provides 
 * auxiliary TransFormulas useful for verification.
 * @author Matthias Heizmann
 */
public class ModifiableGlobalVariableManager {
	
	
	/**
	 * Maps a procedure name to the set of global variables which may be
	 * modified by the procedure. The set of variables is represented as a map
	 * where the identifier of the variable is mapped to the type of the
	 * variable. 
	 */
	private final Map<String,Set<String>> mModifiedVars;
	private final Boogie2SMT mBoogie2smt;
	
	private final Map<String, TransFormula> mProc2OldVarsAssignment;
	private final Map<String, TransFormula> mProc2GlobalVarsAssignment;
	
	
	
	
	public ModifiableGlobalVariableManager(
			final Map<String, Set<String>> modifiedVars,
			final Boogie2SMT boogie2smt) {
		mModifiedVars = modifiedVars;
		mBoogie2smt = boogie2smt;
		mProc2OldVarsAssignment = new HashMap<String, TransFormula>();
		mProc2GlobalVarsAssignment = new HashMap<String, TransFormula>();
	}
	
	protected ModifiableGlobalVariableManager(final ModifiableGlobalVariableManager modifiableGlobalVariableManager) {
		mModifiedVars = modifiableGlobalVariableManager.mModifiedVars;
		mBoogie2smt = modifiableGlobalVariableManager.mBoogie2smt;
		mProc2OldVarsAssignment = modifiableGlobalVariableManager.mProc2OldVarsAssignment;
		mProc2GlobalVarsAssignment = modifiableGlobalVariableManager.mProc2GlobalVarsAssignment;
	}
	
	/**
	 * Return the set of all global BoogieVars that may be modified by procedure
	 * proc.
	 */
	public Set<IProgramVar> getModifiedBoogieVars(final String proc) {
		return getOldVarsAssignment(proc).getInVars().keySet();
	}

	/**
	 * Returns true iff the corresponding non-oldVar of bv is modifiable by
	 * procedure proc.
	 */
	public boolean isModifiable(final IProgramOldVar bv, final String proc) {
		final IProgramNonOldVar bnov = bv.getNonOldVar();
		return getModifiedBoogieVars(proc).contains(bnov);
	}

	/**
	 * Returns true iff the variable bv is modifiable by procedure proc.
	 */
	public boolean isModifiable(final IProgramNonOldVar bnov, final String proc) {
		return getModifiedBoogieVars(proc).contains(bnov);
	}

	/**
	 * Returns a TransFormula that represents an assignment 
	 * gOld_1,...,gOld_n :=g_1,...,g_n
	 * where g_1,...,g_n are the global variables that can be modified by
	 * procedure proc and gOld_1,...,gOld_n are the corresponding oldvars.
	 */
	public TransFormula getOldVarsAssignment(final String proc) {
		TransFormula oldVarsAssignment = mProc2OldVarsAssignment.get(proc);
		if (oldVarsAssignment == null) {
			oldVarsAssignment = constructOldVarsAssignment(proc);
			mProc2OldVarsAssignment.put(proc, oldVarsAssignment);
		}
		return mProc2OldVarsAssignment.get(proc);
	}
	
	
	/**
	 * Returns a TransFormula that represents an assignment 
	 * g_1,...,g_n :=gOld_1,...,gOld_n
	 * where g_1,...,g_n are the global variables that can be modified by
	 * procedure proc and gOld_1,...,gOld_n are the corresponding oldvars.
	 */
	public TransFormula getGlobalVarsAssignment(final String proc) {
		TransFormula globalVarsAssignment = mProc2GlobalVarsAssignment.get(proc);
		if (globalVarsAssignment == null) {
			globalVarsAssignment = constructGlobalVarsAssignment(proc);
			mProc2GlobalVarsAssignment.put(proc, globalVarsAssignment);
		}
		return mProc2GlobalVarsAssignment.get(proc);
	}
	
	

	private TransFormula constructOldVarsAssignment(final String proc) {
		Set<String> vars = mModifiedVars.get(proc);
		if (vars == null) {
			//no global var modified
			vars = Collections.emptySet();
		}
		final TransFormulaBuilder tfb = new TransFormulaBuilder(null, null, true, null, true);
		Term glob2oldFormula = mBoogie2smt.getScript().term("true");
		final Map<String, IProgramNonOldVar> globals = mBoogie2smt.getBoogie2SmtSymbolTable().getGlobals();
		for (final String modVar : vars) {
			final IProgramNonOldVar boogieVar = globals.get(modVar);
			final IProgramVar boogieOldVar = boogieVar.getOldVar();
			final Sort sort = boogieVar.getDefaultConstant().getSort();
			
			final String nameIn = modVar + "_In";
			final TermVariable tvIn = mBoogie2smt.getScript().variable(nameIn, sort);
			final String nameOut = "old(" + modVar + ")" + "_Out";
			final TermVariable tvOut = mBoogie2smt.getScript().variable(nameOut, sort);
			tfb.addInVar(boogieVar, tvIn);
			tfb.addOutVar(boogieVar, tvIn);
			tfb.addOutVar(boogieOldVar, tvOut);
			final Term assignment = mBoogie2smt.getScript().term("=", tvOut, tvIn);
			glob2oldFormula = Util.and(mBoogie2smt.getScript(), glob2oldFormula, assignment);
		}
		tfb.setFormula(glob2oldFormula);
		tfb.setInfeasibility(Infeasibility.UNPROVEABLE);
		return tfb.finishConstruction(mBoogie2smt.getScript());
	}


	
	private TransFormula constructGlobalVarsAssignment(final String proc) {
		Set<String> vars = mModifiedVars.get(proc);
		if (vars == null) {
			//no global var modified
			vars = Collections.emptySet();
		}
		final TransFormulaBuilder tfb = new TransFormulaBuilder(null, null, true, null, true);
		Term old2globFormula = mBoogie2smt.getScript().term("true");
		final Map<String, IProgramNonOldVar> globals = mBoogie2smt.getBoogie2SmtSymbolTable().getGlobals();
		for (final String modVar : vars) {
			final IProgramNonOldVar boogieVar = globals.get(modVar);
			final IProgramVar boogieOldVar = boogieVar.getOldVar();
			final Sort sort = boogieVar.getDefaultConstant().getSort();
			{
				final String nameIn = "old(" + modVar + ")" + "_In";
				final TermVariable tvIn = mBoogie2smt.getScript().variable(nameIn, sort);
				final String nameOut = modVar + "_Out";
				final TermVariable tvOut = mBoogie2smt.getScript().variable(nameOut, sort);
				tfb.addInVar(boogieOldVar, tvIn);
				tfb.addOutVar(boogieOldVar, tvIn);
				tfb.addOutVar(boogieVar, tvOut);
				final Term assignment = mBoogie2smt.getScript().term("=", tvOut, tvIn);
				old2globFormula = Util.and(mBoogie2smt.getScript(), old2globFormula, assignment);
			}			
		}
		tfb.setFormula(old2globFormula);
		tfb.setInfeasibility(Infeasibility.UNPROVEABLE);
		return tfb.finishConstruction(mBoogie2smt.getScript());
	}
	
	/**
	 * Return global variables;
	 */
	public Map<String, IProgramNonOldVar> getGlobals() {
		return mBoogie2smt.getBoogie2SmtSymbolTable().getGlobals();
	}
	
	
	/**
	 * @return true iff pred contains an oldvar that is not modifiable by
	 * procedure proc.
	 */
	public boolean containsNonModifiableOldVars(final IPredicate pred, final String proc) {
		final Set<String> modiableGlobals = mModifiedVars.get(proc);
		for (final IProgramVar bv : pred.getVars()) {
			if (bv.isOldvar()) {
				if (!modiableGlobals.contains(bv.getIdentifier())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return equality (= g oldg) where g is the default constant of the 
	 * BoogieNonOldVar bv and oldg is the default constant of the corresponding
	 * oldVar. If primed is true, we return the primed constant instead of
	 * the default constant.
	 */
	public static Term constructConstantOldVarEquality(final IProgramNonOldVar bv, final boolean primed, final Script script) {
		final IProgramOldVar oldVar = bv.getOldVar();
		final Term nonOldConstant = (primed ? bv.getPrimedConstant() : bv.getDefaultConstant());
		final Term oldConstant = (primed ? oldVar.getPrimedConstant() : oldVar.getDefaultConstant());
		return script.term("=", oldConstant, nonOldConstant);
	}

}
