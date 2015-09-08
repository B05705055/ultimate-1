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
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.logic.Annotation;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.FormulaUnLet;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.QuotedObject;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Util;
import de.uni_freiburg.informatik.ultimate.model.boogie.BoogieNonOldVar;
import de.uni_freiburg.informatik.ultimate.model.boogie.BoogieOldVar;
import de.uni_freiburg.informatik.ultimate.model.boogie.BoogieVar;
import de.uni_freiburg.informatik.ultimate.model.boogie.GlobalBoogieVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula.Infeasibility;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Call;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Return;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.IHoareTripleChecker.Validity;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager.Status;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.IPredicateCoverageChecker;
import de.uni_freiburg.informatik.ultimate.util.ScopedHashMap;

@Deprecated
public class EdgeChecker {
	
	private final SmtManager m_SmtManager;
	private final Script m_Script;
	private final ModifiableGlobalVariableManager m_ModifiableGlobalVariableManager;
	
	private IPredicate m_PrePred;
	private IPredicate m_HierPred;
	private CodeBlock m_CodeBlock;
	private TransFormula m_TransFormula;
	private ScopedHashMap<BoogieVar, Term> m_HierConstants;
	public final static boolean m_AddDebugInformation = !false;
	public final static boolean m_UnletTerms = true;
	
	private final HoareTripleCheckerBenchmarkGenerator m_EdgeCheckerBenchmark;
	private final IPredicateCoverageChecker m_PredicateCoverageChecker;
	
	private static final String s_StartEdgeCheck = "starting to check validity of Hoare triples";
	private static final String s_EndEdgeCheck = "finished to check validity of Hoare triples";
	
	public EdgeChecker(SmtManager smtManager, 
			ModifiableGlobalVariableManager modGlobVarManager, 
			IPredicateCoverageChecker predicateCoverageChecker) {
		m_SmtManager = smtManager;
		m_ModifiableGlobalVariableManager = modGlobVarManager;
		m_Script = smtManager.getScript();
		m_EdgeCheckerBenchmark = new HoareTripleCheckerBenchmarkGenerator();
		m_PredicateCoverageChecker = predicateCoverageChecker;
	}
	
	public EdgeChecker(SmtManager smtManager, ModifiableGlobalVariableManager modGlobVarManager) {
		this(smtManager, modGlobVarManager, null);
	}
	
	public SmtManager getSmtManager() {
		return m_SmtManager;
	}
	
	public HoareTripleCheckerBenchmarkGenerator getEdgeCheckerBenchmark() {
		return m_EdgeCheckerBenchmark;
	}

	public LBool assertPrecondition(IPredicate p) {
		assert m_SmtManager.isLockOwner(this);
		assert m_PrePred == null : "PrePred already asserted";
		assert m_CodeBlock != null : "Assert CodeBlock first!";
		m_PrePred = p;
		m_EdgeCheckerBenchmark.continueEdgeCheckerTime();
		m_Script.push(1);
		if (m_CodeBlock instanceof Return) {
			m_HierConstants.beginScope();
		}
		Term predcondition = p.getClosedFormula();
		if (m_AddDebugInformation) {
			String name = "precondition";
			Annotation annot = new Annotation(":named", name);
			predcondition = m_Script.annotate(predcondition, annot);
		}
		LBool quickCheck = m_SmtManager.assertTerm(predcondition);
		String predProc = m_CodeBlock.getPreceedingProcedure();
		Set<BoogieVar> oldVarsOfModifiable = m_ModifiableGlobalVariableManager.
				getOldVarsAssignment(predProc).getAssignedVars();
		Collection<Term> oldVarEqualities = constructNonModOldVarsEquality(p.getVars(), oldVarsOfModifiable);
		if (!oldVarEqualities.isEmpty()) {
			Term nonModOldVarsEquality = Util.and(m_Script, oldVarEqualities.toArray(new Term[oldVarEqualities.size()]));
			if (m_AddDebugInformation) {
				String name = "precondNonModGlobalEquality";
				Annotation annot = new Annotation(":named", name);
				nonModOldVarsEquality = m_Script.annotate(nonModOldVarsEquality, annot);
			}
			quickCheck = m_SmtManager.assertTerm(nonModOldVarsEquality);
		}
		m_EdgeCheckerBenchmark.stopEdgeCheckerTime();
		return quickCheck;
	}
	
	
	/**
	 * Return a set of equalities such that for each oldvar old(g) that occurs
	 * in vars that is not contained in oldVarsOfModifiableGlobals there is
	 * an equality (= c_g c_old(g)) where c_g is the default constant of the 
	 * global variable g and c_old(g) is the default constant of old(g).
	 */
	private Collection<Term> constructNonModOldVarsEquality(Set<BoogieVar> vars,
			Set<BoogieVar> oldVarsOfModifiableGlobals) {
		Collection<Term> conjunction = new ArrayList<>();
		for (BoogieVar bv : vars) {
			if (bv instanceof BoogieOldVar && !oldVarsOfModifiableGlobals.contains(bv)) {
				conjunction.add(oldVarsEquality((BoogieOldVar) bv));
			}
		}
		return conjunction;
	}
	
	private Term oldVarsEquality(BoogieOldVar oldVar) {
		assert oldVar.isOldvar();
		BoogieVar nonOldVar = oldVar.getNonOldVar();
		Term equality = m_Script.term("=", oldVar.getDefaultConstant(), 
										   nonOldVar.getDefaultConstant());
		return equality;
	}
	

	public void unAssertPrecondition() {
		assert m_SmtManager.isLockOwner(this);
		assert m_PrePred != null : "No PrePred asserted";
		m_PrePred = null;
		m_Script.pop(1);
		if (m_CodeBlock instanceof Return) {
			m_HierConstants.endScope();
		}
		if (m_CodeBlock == null) {
			throw new AssertionError("CodeBlock is asserted first");
		}
	}
	
	
	public LBool assertCodeBlock(CodeBlock cb) {
		m_SmtManager.lock(this);
		m_Script.echo(new QuotedObject(s_StartEdgeCheck));
		assert m_CodeBlock == null : "CodeBlock already asserted";
		m_CodeBlock = cb;
		m_EdgeCheckerBenchmark.continueEdgeCheckerTime();
		m_Script.push(1);
		m_TransFormula = cb.getTransitionFormula();
		Term cbFormula = m_TransFormula.getClosedFormula();
		
		if (m_AddDebugInformation) {
			String name = "codeBlock";
			Annotation annot = new Annotation(":named", name);
			cbFormula = m_Script.annotate(cbFormula, annot);
		}
		LBool quickCheck = m_SmtManager.assertTerm(cbFormula);
		if (cb instanceof Return) {
			m_HierConstants = new ScopedHashMap<BoogieVar,Term>();
			Return ret = (Return) cb;
			Call call = ret.getCorrespondingCall();
			String proc = call.getCallStatement().getMethodName();
			TransFormula ovaTF = m_ModifiableGlobalVariableManager.
					getOldVarsAssignment(proc);
			Term ovaFormula = ovaTF.getFormula();

			//rename modifiable globals to Hier vars
			ovaFormula = renameVarsToHierConstants(ovaTF.getInVars(), ovaFormula);
			//rename oldVars of modifiable globals to default vars
			ovaFormula = renameVarsToDefaultConstants(ovaTF.getOutVars(), ovaFormula);
			if (m_UnletTerms ) {
				ovaFormula = (new FormulaUnLet()).unlet(ovaFormula);
			}
			assert ovaFormula.getFreeVars().length == 0;
			if (m_AddDebugInformation) {
				String name = "modifiableVarEquality";
				Annotation annot = new Annotation(":named", name);
				ovaFormula = m_Script.annotate(ovaFormula, annot);
			}
			quickCheck = m_SmtManager.assertTerm(ovaFormula);
			
			Set<BoogieVar> modifiableGlobals = ovaTF.getInVars().keySet();
			TransFormula callTf = call.getTransitionFormula();
			Term locVarAssign = callTf.getFormula();
			//TODO: rename non-modifiable globals to DefaultConstants
			locVarAssign = renameNonModifiableGlobalsToDefaultConstants(
					callTf.getInVars(), modifiableGlobals, locVarAssign);
			
			//rename arguments vars to Hier vars
			locVarAssign = renameVarsToHierConstants(callTf.getInVars(), locVarAssign);
			//rename proc parameter vars to DefaultConstants
			locVarAssign = renameVarsToDefaultConstants(callTf.getOutVars(), locVarAssign);
			if (m_UnletTerms ) {
				locVarAssign = (new FormulaUnLet()).unlet(locVarAssign);
			}
			assert locVarAssign.getFreeVars().length == 0;
			if (m_AddDebugInformation) {
				String name = "localVarsAssignment";
				Annotation annot = new Annotation(":named", name);
				locVarAssign = m_Script.annotate(locVarAssign, annot);
			}
			quickCheck = m_SmtManager.assertTerm(locVarAssign);
		}
		m_EdgeCheckerBenchmark.stopEdgeCheckerTime();
		return quickCheck;
	}

	
	public void unAssertCodeBlock() {
		assert m_SmtManager.isLockOwner(this);
		assert m_CodeBlock != null : "No CodeBlock asserted";
		m_CodeBlock = null;
		m_HierConstants = null;
		m_Script.pop(1);
		if (m_PrePred == null) {
			m_SmtManager.unlock(this);
			m_Script.echo(new QuotedObject(s_EndEdgeCheck));
		}

	}
	
	
	public LBool assertHierPred(IPredicate p) {
		assert m_SmtManager.isLockOwner(this);
		assert m_CodeBlock != null : "assert Return first";
		assert m_CodeBlock instanceof Return : "assert Return first";
		assert m_HierPred == null : "HierPred already asserted";
		m_HierPred = p;
		m_EdgeCheckerBenchmark.continueEdgeCheckerTime();
		m_Script.push(1);
		m_HierConstants.beginScope();
		Term hierFormula = p.getFormula();

		
		// rename globals that are not modifiable by callee to default constants
		String callee = m_CodeBlock.getPreceedingProcedure();
		Set<BoogieVar> modifiableGlobalsCallee = m_ModifiableGlobalVariableManager.
				getModifiedBoogieVars(callee);
		hierFormula = renameNonModifiableNonOldGlobalsToDefaultConstants(
				p.getVars(), modifiableGlobalsCallee, hierFormula);
		
		// rename oldvars of globals that are not modifiable by caller to 
		// default constants of nonOldVar
		String caller = m_CodeBlock.getSucceedingProcedure();
		Set<BoogieVar> modifiableGlobalsCaller = m_ModifiableGlobalVariableManager.
				getModifiedBoogieVars(caller);
		hierFormula = renameNonModifiableOldGlobalsToDefaultConstantOfNonOldVar(
				p.getVars(), modifiableGlobalsCaller, hierFormula);
		


		// rename modifiable globals of callee to default constants
//		hierFormula = renameVarsToDefaultConstants(modifiableGlobalsCallee, hierFormula);
//		//rename non-modifiable globals to default constants
//		Return ret = (Return) m_CodeBlock;
//		Call call = ret.getCorrespondingCall();
//		String proc = call.getCallStatement().getMethodName();
//		TransFormula oldVarAssignment = m_ModifiableGlobalVariableManager.
//				getOldVarsAssignment(proc);
//		Set<BoogieVar> modifiableGlobals = oldVarAssignment.getInVars().keySet();
		

		//rename vars which are assigned on return to Hier vars
		hierFormula = renameVarsToHierConstants(p.getVars(), hierFormula);
		if (m_UnletTerms ) {
			hierFormula = (new FormulaUnLet()).unlet(hierFormula);
		}
		
		
		//TODO auxvars
		assert hierFormula.getFreeVars().length == 0;
		
		if (m_AddDebugInformation) {
			String name = "hierarchicalPrecondition";
			Annotation annot = new Annotation(":named", name);
			hierFormula = m_Script.annotate(hierFormula, annot);
		}
		LBool quickCheck = m_SmtManager.assertTerm(hierFormula);
		
		// Now, we have to take care for variables g that are modifiable in the
		// caller but not in the callee.
		// In the succPred and in the hierPred they will be treated as usual
		// (i.e., nonOldVar renamed to default or primed constant depending on
		// if assigned at return or not)
		// we additionally have to state that old(g) and g both have the value
		// of g at the position before the call.
		
//		Collection<Term> calleeNonModOldVarsEqualities = 
//				constructCalleeNonModOldVarsEquality(m_PrePred.getVars(), 
//						modifiableGlobalsCaller, modifiableGlobalsCallee);
//		if (!calleeNonModOldVarsEqualities.isEmpty()) {
//			Term nonModOldVarsEquality = Util.and(m_Script, 
//					calleeNonModOldVarsEqualities.toArray(new Term[calleeNonModOldVarsEqualities.size()]));
//			if (m_AddDebugInformation) {
//				String name = "NonModGlobalEqualityForCallerCalleeDifference";
//				Annotation annot = new Annotation(":named", name);
//				nonModOldVarsEquality = m_Script.annotate(nonModOldVarsEquality, annot);
//			}
//			quickCheck = m_SmtManager.assertTerm(nonModOldVarsEquality);
//		}
		
		m_EdgeCheckerBenchmark.stopEdgeCheckerTime();
		return quickCheck;
	}
	

	/**
	 * Return a set of equalities such that for each oldvar old(g) that occurs
	 * in vars and for which the corresponding nonOldVar occurs in 
	 * modifiableGlobalsCaller but not in modifiableGlobalsCallee we add the
	 * equality (= c_old(g) c_g_hier) and
	 * for each nonOldVar that occurs in 
	 * modifiableGlobalsCaller but not in modifiableGlobalsCallee we add the
	 * equality (= c_g c_g_hier),
	 * where c_g is the default constant of the 
	 * global variable g and c_old(g) is the default constant of old(g) and
	 * c_g_hier is the constant for the nonOldVar g at the position of the
	 * hierarchical predecessor.
	 */
	private Collection<Term> constructCalleeNonModOldVarsEquality(Set<BoogieVar> vars,
			Set<BoogieVar> modifiableGlobalsCaller,
			Set<BoogieVar> modifiableGlobalsCallee) {
		if (!modifiableGlobalsCallee.containsAll(modifiableGlobalsCaller)) {
			boolean test = true;
		}
		Collection<Term> conjunction = new ArrayList<>();
		for (BoogieVar bv : vars) {
			if (bv instanceof GlobalBoogieVar) {
				BoogieNonOldVar bnov;
				if (bv instanceof BoogieOldVar) {
					bnov = ((BoogieOldVar) bv).getNonOldVar();
				} else {
					bnov = (BoogieNonOldVar) bv;
				}
				if (modifiableGlobalsCaller.contains(bnov) && 
						!modifiableGlobalsCallee.contains(bnov)) {
					Term hierConst = getOrConstructHierConstant(bnov);
					Term conjunct = SmtUtils.binaryEquality(m_Script, bv.getDefaultConstant(), hierConst);
					conjunction.add(conjunct);
				}
			}
		}
		return conjunction;
	}

	public void unAssertHierPred() {
		assert m_SmtManager.isLockOwner(this);
		assert m_HierPred != null : "No HierPred asserted";
		m_HierPred = null;
		m_Script.pop(1);
		m_HierConstants.endScope();
	}
	
	
	
	
	
	public LBool postInternalImplies(IPredicate p) {
		assert m_PrePred != null;
		assert m_CodeBlock != null;
		m_EdgeCheckerBenchmark.continueEdgeCheckerTime();
		m_Script.push(1);
		
		//OldVars renamed (depending on modifiability)
		//All variables get index 0 
		//assigned vars (locals and globals) get index 1
		//other vars get index 0
		Set<BoogieVar> assignedVars = m_TransFormula.getAssignedVars();
		Term renamedFormula = renameVarsToPrimedConstants(assignedVars, p.getFormula());
		String succProc = m_CodeBlock.getSucceedingProcedure();
		Set<BoogieVar> modifiableGlobals = m_ModifiableGlobalVariableManager.getModifiedBoogieVars(succProc);
		renamedFormula = renameNonModifiableOldGlobalsToDefaultConstantOfNonOldVar(p.getVars(), modifiableGlobals, renamedFormula);
		renamedFormula = renameVarsToDefaultConstants(p.getVars(), renamedFormula);
		if (m_UnletTerms ) {
			renamedFormula = (new FormulaUnLet()).unlet(renamedFormula);
		}
		assert renamedFormula.getFreeVars().length == 0;
		Term negation = m_Script.term("not", renamedFormula);
		if (m_AddDebugInformation) {
			String name = "negatedPostcondition";
			Annotation annot = new Annotation(":named", name);
			negation = m_Script.annotate(negation, annot);
		}
		LBool isSat = m_SmtManager.assertTerm(negation);
		
		if (isSat == LBool.UNKNOWN) {
			// quickcheck failed
			isSat = m_Script.checkSat();
		}
		switch (isSat) {
		case SAT:
			m_EdgeCheckerBenchmark.getSolverCounterSat().incIn();
			break;
		case UNKNOWN:
			m_EdgeCheckerBenchmark.getSolverCounterUnknown().incIn();
			break;
		case UNSAT:
			m_EdgeCheckerBenchmark.getSolverCounterUnsat().incIn();
			break;
		default:
			throw new AssertionError("unknown case");
		}
		m_Script.pop(1);
		m_EdgeCheckerBenchmark.stopEdgeCheckerTime();
		return isSat;
	}
	
	

	public LBool postCallImplies(IPredicate p) {
		assert m_PrePred != null;
		assert (m_CodeBlock instanceof Call);
		m_EdgeCheckerBenchmark.continueEdgeCheckerTime();
		m_Script.push(1);
		
		Set<BoogieVar> boogieVars = p.getVars();
		// rename oldVars to default constants of non-oldvars
		Term renamedFormula = renameGlobalsAndOldVarsToNonOldDefaultConstants(
												boogieVars, p.getFormula());
		// rename remaining variables
		renamedFormula = renameVarsToPrimedConstants(boogieVars, renamedFormula);
		renamedFormula = renameVarsToDefaultConstants(p.getVars(), renamedFormula);
		if (m_UnletTerms ) {
			renamedFormula = (new FormulaUnLet()).unlet(renamedFormula);
		}
		assert renamedFormula.getFreeVars().length == 0;
		Term negation = m_Script.term("not", renamedFormula);
		if (m_AddDebugInformation) {
			String name = "negatedPostcondition";
			Annotation annot = new Annotation(":named", name);
			negation = m_Script.annotate(negation, annot);
		}
		LBool isSat = m_SmtManager.assertTerm(negation);
		
		if (isSat == LBool.UNKNOWN) {
			// quickcheck failed
			isSat = m_Script.checkSat();
		}
		switch (isSat) {
		case SAT:
			m_EdgeCheckerBenchmark.getSolverCounterSat().incCa();
			break;
		case UNKNOWN:
			m_EdgeCheckerBenchmark.getSolverCounterUnknown().incCa();
			break;
		case UNSAT:
			m_EdgeCheckerBenchmark.getSolverCounterUnsat().incCa();
			break;
		default:
			throw new AssertionError("unknown case");
		}
		m_Script.pop(1);
		m_EdgeCheckerBenchmark.stopEdgeCheckerTime();
		return isSat;
	}



	public LBool postReturnImplies(IPredicate p) {
		assert m_PrePred != null;
		assert (m_CodeBlock instanceof Return);
		assert m_HierPred != null;
		m_EdgeCheckerBenchmark.continueEdgeCheckerTime();
		m_Script.push(1);
		m_HierConstants.beginScope();
		
		
		//rename assignedVars to primed vars
		Set<BoogieVar> assignedVars = m_TransFormula.getAssignedVars();
		Term renamedFormula = renameVarsToPrimedConstants(assignedVars, p.getFormula());
		
		String callee = m_CodeBlock.getPreceedingProcedure();
		Set<BoogieVar> modifiableGlobalsCallee = m_ModifiableGlobalVariableManager.
				getModifiedBoogieVars(callee);
		
		//rename modifiable globals to default constants
		renamedFormula = renameVarsToDefaultConstants(modifiableGlobalsCallee, renamedFormula);
		
		// rename globals that are not modifiable by callee to default constants
		renamedFormula = renameNonModifiableNonOldGlobalsToDefaultConstants(
				p.getVars(), modifiableGlobalsCallee, renamedFormula);
		
		// rename oldvars of globals that are not modifiable by caller to 
		// default constants of nonOldVar
		String caller = m_CodeBlock.getSucceedingProcedure();
		Set<BoogieVar> modifiableGlobalsCaller = m_ModifiableGlobalVariableManager.
				getModifiedBoogieVars(caller);
		renamedFormula = renameNonModifiableOldGlobalsToDefaultConstantOfNonOldVar(
				p.getVars(), modifiableGlobalsCaller, renamedFormula);
		
		// rename remaining non-old Globals to default constants
//		renamedFormula = renameNonOldGlobalsToDefaultConstants(p.getVars(), renamedFormula);
		
		//rename remaining vars to Hier vars
		renamedFormula = renameVarsToHierConstants(p.getVars(), renamedFormula);
		
		if (m_UnletTerms ) {
			renamedFormula = (new FormulaUnLet()).unlet(renamedFormula);
		}
		assert renamedFormula.getFreeVars().length == 0;
		Term negation = m_Script.term("not", renamedFormula);

		if (m_AddDebugInformation) {
			String name = "negatedPostcondition";
			Annotation annot = new Annotation(":named", name);
			negation = m_Script.annotate(negation, annot);
		}
		LBool isSat = m_SmtManager.assertTerm(negation);
		
		if (isSat == LBool.UNKNOWN) {
			// quickcheck failed
			isSat = m_Script.checkSat();
		}
		switch (isSat) {
		case SAT:
			m_EdgeCheckerBenchmark.getSolverCounterSat().incRe();
			break;
		case UNKNOWN:
			m_EdgeCheckerBenchmark.getSolverCounterUnknown().incRe();
			break;
		case UNSAT:
			m_EdgeCheckerBenchmark.getSolverCounterUnsat().incRe();
			break;
		default:
			throw new AssertionError("unknown case");
		}
		m_Script.pop(1);
		m_HierConstants.endScope();
		m_EdgeCheckerBenchmark.stopEdgeCheckerTime();
		return isSat;
	}
	

	
	
	
	

	
	
//	private Term renameGlobalsToDefaultOldGlobalConstants(
//								Set<BoogieVar> boogieVars, Term formula) {
//		ArrayList<TermVariable> replacees = new ArrayList<TermVariable>();
//		ArrayList<Term> replacers = new ArrayList<Term>();
//		for (BoogieVar bv : boogieVars) {
//			assert bv.isGlobal();
//			assert !bv.isOldvar();
//			replacees.add(bv.getTermVariable());
//			BoogieVar oldVar = m_Smt2Boogie.getOldGlobals().get(bv.getIdentifier());
//			replacers.add(oldVar.getDefaultConstant());
//		}
//		TermVariable[] vars = replacees.toArray(new TermVariable[replacees.size()]);
//		Term[] values = replacers.toArray(new Term[replacers.size()]);
//		return m_Script.let( vars , values, formula);
//	}
	
	
	private Term renameVarsToDefaultConstants(Set<BoogieVar> boogieVars, Term formula) {
		ArrayList<TermVariable> replacees = new ArrayList<TermVariable>();
		ArrayList<Term> replacers = new ArrayList<Term>();
		for (BoogieVar bv : boogieVars) {
			replacees.add(bv.getTermVariable());
			replacers.add(bv.getDefaultConstant());
		}
		TermVariable[] vars = replacees.toArray(new TermVariable[replacees.size()]);
		Term[] values = replacers.toArray(new Term[replacers.size()]);
		return m_Script.let( vars , values, formula);
	}
	
	
	private Term renameVarsToDefaultConstants(Map<BoogieVar, TermVariable> bv2tv, Term formula) {
		ArrayList<TermVariable> replacees = new ArrayList<TermVariable>();
		ArrayList<Term> replacers = new ArrayList<Term>();
		for (BoogieVar bv : bv2tv.keySet()) {
			replacees.add(bv2tv.get(bv));
			replacers.add(bv.getDefaultConstant());
		}
		TermVariable[] vars = replacees.toArray(new TermVariable[replacees.size()]);
		Term[] values = replacers.toArray(new Term[replacers.size()]);
		return m_Script.let( vars , values, formula);
	}
	
	
	private Term renameVarsToPrimedConstants(Set<BoogieVar> boogieVars, Term formula) {
		ArrayList<TermVariable> replacees = new ArrayList<TermVariable>();
		ArrayList<Term> replacers = new ArrayList<Term>();
		for (BoogieVar bv : boogieVars) {
			replacees.add(bv.getTermVariable());
			replacers.add(bv.getPrimedConstant());
		}
		TermVariable[] vars = replacees.toArray(new TermVariable[replacees.size()]);
		Term[] values = replacers.toArray(new Term[replacers.size()]);
		return m_Script.let( vars , values, formula);
	}


	private Term renameVarsToHierConstants(Set<BoogieVar> boogieVars, Term formula) {
		ArrayList<TermVariable> replacees = new ArrayList<TermVariable>();
		ArrayList<Term> replacers = new ArrayList<Term>();
		for (BoogieVar bv : boogieVars) {
			replacees.add(bv.getTermVariable());
			replacers.add(getOrConstructHierConstant(bv));
		}
		TermVariable[] vars = replacees.toArray(new TermVariable[replacees.size()]);
		Term[] values = replacers.toArray(new Term[replacers.size()]);
		return m_Script.let( vars , values, formula);
	}
	
	private Term renameVarsToHierConstants(Map<BoogieVar, TermVariable> bv2tv, Term formula) {
		ArrayList<TermVariable> replacees = new ArrayList<TermVariable>();
		ArrayList<Term> replacers = new ArrayList<Term>();
		for (BoogieVar bv : bv2tv.keySet()) {
			replacees.add(bv2tv.get(bv));
			replacers.add(getOrConstructHierConstant(bv));
		}
		TermVariable[] vars = replacees.toArray(new TermVariable[replacees.size()]);
		Term[] values = replacers.toArray(new Term[replacers.size()]);
		return m_Script.let( vars , values, formula);
	}


	private Term getOrConstructHierConstant(BoogieVar bv) {
		Term preHierConstant = m_HierConstants.get(bv);
		if (preHierConstant == null) {
			String name = "c_" + bv.getTermVariable().getName() + "_Hier";
			Sort sort = bv.getTermVariable().getSort();
			m_Script.declareFun(name, new Sort[0], sort);
			preHierConstant = m_Script.term(name);
			m_HierConstants.put(bv, preHierConstant);
		}
		return preHierConstant;
	}
	
	

	/**
	 * Rename each g in boogieVars that is not contained in modifiableGlobals
	 * to c_g, where c_g is the default constant for g.
	 */
	private Term renameNonModifiableNonOldGlobalsToDefaultConstants(
			Set<BoogieVar> boogieVars, 
			Set<BoogieVar> modifiableGlobals,
			Term formula) {
		ArrayList<TermVariable> replacees = new ArrayList<TermVariable>();
		ArrayList<Term> replacers = new ArrayList<Term>();
		for (BoogieVar bv : boogieVars) {
			if (bv.isGlobal()) {
				if (bv instanceof BoogieNonOldVar) {
					if (modifiableGlobals.contains(bv)) {
						//do nothing
					} else {
						replacees.add(bv.getTermVariable());
						replacers.add(bv.getDefaultConstant());
					}
				}
			}
		}
		TermVariable[] vars = replacees.toArray(new TermVariable[replacees.size()]);
		Term[] values = replacers.toArray(new Term[replacers.size()]);
		return m_Script.let( vars , values, formula);
	}
	
	
	/**
	 * Rename oldVars old(g) of non-modifiable globals to the
	 * default constants of g. 
	 */
	private Term renameNonModifiableOldGlobalsToDefaultConstantOfNonOldVar(
			Set<BoogieVar> boogieVars, 
			Set<BoogieVar> modifiableGlobals,
			Term formula) {
		ArrayList<TermVariable> replacees = new ArrayList<TermVariable>();
		ArrayList<Term> replacers = new ArrayList<Term>();
		for (BoogieVar bv : boogieVars) {
			if (bv instanceof BoogieOldVar) {
				BoogieNonOldVar nonOldVar = ((BoogieOldVar) bv).getNonOldVar();
				if (modifiableGlobals.contains(nonOldVar)) {
					//do nothing
				} else {
					replacees.add(bv.getTermVariable());
					replacers.add(nonOldVar.getDefaultConstant());
				}
				
			}
		}
		TermVariable[] vars = replacees.toArray(new TermVariable[replacees.size()]);
		Term[] values = replacers.toArray(new Term[replacers.size()]);
		return m_Script.let( vars , values, formula);
	}

	
	
	
	private Term renameNonModifiableGlobalsToDefaultConstants(
			Map<BoogieVar,TermVariable> boogieVars, 
			Set<BoogieVar> modifiableGlobals,
			Term formula) {
		ArrayList<TermVariable> replacees = new ArrayList<TermVariable>();
		ArrayList<Term> replacers = new ArrayList<Term>();
		for (BoogieVar bv : boogieVars.keySet()) {
			if (bv.isGlobal()) {
				if (bv.isOldvar()) {
					assert !modifiableGlobals.contains(bv);
					// do nothing
				} else {
					if (modifiableGlobals.contains(bv)) {
						//do noting
					} else {
						//oldVar of global which is not modifiable by called proc
						replacees.add(boogieVars.get(bv));
						replacers.add(bv.getDefaultConstant());
					}
				}
			} else {
				assert !modifiableGlobals.contains(bv);
			}
		}
		TermVariable[] vars = replacees.toArray(new TermVariable[replacees.size()]);
		Term[] values = replacers.toArray(new Term[replacers.size()]);
		return m_Script.let( vars , values, formula);
	}
	
	
	
	
	private Term renameGlobalsAndOldVarsToNonOldDefaultConstants(
			Set<BoogieVar> boogieVars, 
			Term formula) {
		ArrayList<TermVariable> replacees = new ArrayList<TermVariable>();
		ArrayList<Term> replacers = new ArrayList<Term>();
		for (BoogieVar bv : boogieVars) {
			if (bv.isGlobal()) {
				if (bv.isOldvar()) {
					replacees.add(bv.getTermVariable());
					BoogieVar nonOldbv = ((BoogieOldVar) bv).getNonOldVar();
					replacers.add(nonOldbv.getDefaultConstant());
				} else {
					replacees.add(bv.getTermVariable());
					replacers.add(bv.getDefaultConstant());
				}
			}
		}
		TermVariable[] vars = replacees.toArray(new TermVariable[replacees.size()]);
		Term[] values = replacers.toArray(new Term[replacers.size()]);
		return m_Script.let( vars , values, formula);
	}
	
//	private Term renameNonOldGlobalsToDefaultConstants(
//			Set<BoogieVar> boogieVars, 
//			Term formula) {
//		ArrayList<TermVariable> replacees = new ArrayList<TermVariable>();
//		ArrayList<Term> replacers = new ArrayList<Term>();
//		for (BoogieVar bv : boogieVars) {
//			if (bv.isGlobal()) {
//				if (bv.isOldvar()) {
//					// do nothing
//				} else {
//					replacees.add(bv.getTermVariable());
//					replacers.add(bv.getDefaultConstant());
//				}
//			}
//		}
//		TermVariable[] vars = replacees.toArray(new TermVariable[replacees.size()]);
//		Term[] values = replacers.toArray(new Term[replacers.size()]);
//		return m_Script.let( vars , values, formula);
//	}
	
	
	
	
	private static boolean varSetDisjoint(Set<BoogieVar> set1, Set<BoogieVar> set2) {
		if (set1.size() < set2.size()) {
			for (BoogieVar bv : set1) {
				if (set2.contains(bv)) {
					return false;
				}
			}
		} else {
			for (BoogieVar bv : set2) {
				if (set1.contains(bv)) {
					return false;
				}
			}
		}
		return true;
	}
	
	
	
	
	/**
	 * Idea: If the formula of the code block is satisfiable,
	 * the predecessor is satisfiable and the vars of predecessor are disjoint
	 * from the inVars of the code block, then a transition to false is not
	 * inductive.
	 * Idea with UNKNOWN: if the solver was unable to decide feasibility of cb, 
	 * the predecessor is satisfiable and the vars of predecessor are disjoint
	 * from the inVars of the code block, then the solver will be unable to
	 * show that a transition to false is inductive. 
	 *
	 * FIXME: Check for precondition false, not for precondition true.
	 */
	public LBool sdecInternalToFalse(IPredicate pre, CodeBlock cb) {
		Infeasibility infeasiblity = cb.getTransitionFormula().isInfeasible();
		if (infeasiblity == Infeasibility.UNPROVEABLE) {
			if (pre.getFormula() == m_Script.term("true")) {
				m_EdgeCheckerBenchmark.getSDtfsCounter().incIn();
				return LBool.UNKNOWN;
			} else {
				if (varsDisjoinedFormInVars(pre, cb)) {
					m_EdgeCheckerBenchmark.getSDtfsCounter().incIn();
					return LBool.SAT;
				} else  {
					return null;
				}
			}
						
		} else if (infeasiblity == Infeasibility.INFEASIBLE) {
			m_EdgeCheckerBenchmark.getSDtfsCounter().incIn();
			return LBool.UNSAT;
		} else if (infeasiblity == Infeasibility.NOT_DETERMINED) {
			return null;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	
	
	
	/**
	 * Returns true iff the variables occurring in state are disjoint from the
	 * inVars of CodeBlock letter.
	 * @param state
	 * @param symbol
	 * @return
	 */
	private boolean varsDisjoinedFormInVars(IPredicate state, CodeBlock letter) {
		for (BoogieVar bv : state.getVars()) {
			if (letter.getTransitionFormula().getInVars().containsKey(bv)) {
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * FIXME: Mention assumptions.
	 * Idea: If 
	 * <ul>
	 * <li> the formula of the code block is satisfiable, 
	 * <li> the predecessor is satisfiable,
	 * <li> the successor is not unsatisfiable,
	 * <li> the variables of the predecessor are disjoint from the invars
	 * of the code block, and
	 * <li> the variables of the successor are disjoint from the outvars of the
	 * code block, from the invars of the code block and from the vars of the
	 * predecessor,
	 * </ul>
	 * then a transition (pre, cb, post) is not inductive. 
	 *
	 * FIXME: Check for preconditions, postcondition? Check at least for
	 * infeasibility flag of TransFormula.
	 */
	public LBool sdecInteral(IPredicate pre, CodeBlock cb, IPredicate post) {
		if (m_PredicateCoverageChecker != null) {
			Validity sat = m_PredicateCoverageChecker.isCovered(pre, post);
			if (sat == Validity.VALID) {
				if (Collections.disjoint(pre.getVars(), cb.getTransitionFormula().getAssignedVars())) {
					return LBool.UNSAT;
				}
			}
		}
		for (BoogieVar bv : pre.getVars()) {
			if (cb.getTransitionFormula().getInVars().containsKey(bv)) {
				return null;
			}
		}
		for (BoogieVar bv : post.getVars()) {
//			if (pre.getVars().contains(bv)) {
//				return null;
//			} else 
			if (cb.getTransitionFormula().getInVars().containsKey(bv)) {
				return null;
			} else if (cb.getTransitionFormula().getOutVars().containsKey(bv)) {
				return null;
			}
		}
		// now, we know that vars of pre and post are both disjoint from the
		/// vars of cb. Edge is inductive iff pre implies post
		if (m_PredicateCoverageChecker != null) {
			Validity sat = m_PredicateCoverageChecker.isCovered(pre, post);
			if (sat == Validity.VALID) {
				return LBool.UNSAT;
			}
		} else {
			if (!Collections.disjoint(pre.getVars(), post.getVars())) {
				return null;
			}
		}
		m_EdgeCheckerBenchmark.getSDsCounter().incIn();
		return LBool.SAT;
	}
	
	
//	public boolean sdecSatAssured(Predicate pre, CodeBlock cb) {
//		Set<BoogieVar> inVars = cb.getTransitionFormula().getInVars().keySet();
//		 if (varSetDisjoint(pre.getVars(), inVars)) {
//			 return true;
//		 }
//		 else return false;
//	}
	
	
	public LBool sdLazyEcInteral(IPredicate pre, CodeBlock cb, IPredicate post) {
		if (isOrIteFormula(post)) {
			return sdecInteral(pre, cb, post);
		}
		for (BoogieVar bv : post.getVars()) {
			if (pre.getVars().contains(bv)) {
				continue;
			} else if (cb.getTransitionFormula().getInVars().containsKey(bv)) {
				continue;
			} else if (cb.getTransitionFormula().getOutVars().containsKey(bv)) {
				continue;
			}
			// occurs neither in pre not in codeBlock, probably unsat
			m_EdgeCheckerBenchmark.getSdLazyCounter().incIn();
			return LBool.SAT;
		}
		return null;
	}
	
	public LBool sdecCallToFalse(IPredicate pre, CodeBlock cb) {
		// TODO:
		// there could be a contradiction if the Call is not a simple call
		// but interprocedural sequential composition 			
		if (cb instanceof Call) {
			m_EdgeCheckerBenchmark.getSDtfsCounter().incCa();
			return LBool.SAT;
		} else {
			return null;
		}
	}
	
	public LBool sdecCall(IPredicate pre, CodeBlock cb, IPredicate post) {
		for (BoogieVar bv : post.getVars()) {
			if (bv.isOldvar()) {
				//if oldVar occurs this edge might be inductive since 
				// old(g)=g is true
				return null;
			} else if (bv.isGlobal()) {
				assert !bv.isOldvar();
				if (pre.getVars().contains(bv)) {
					return null;
				}
			}
		}
		//workaround see preHierIndependent()
		TransFormula locVarAssignTf = cb.getTransitionFormula();
		if (!varSetDisjoint(locVarAssignTf.getAssignedVars(), pre.getVars())) {
			return null;
		}
		if (preHierIndependent(post, pre, (Call) cb)) {
			m_EdgeCheckerBenchmark.getSDsCounter().incCa();
			return LBool.SAT;
		}
		return null;
	}
	
	public LBool sdLazyEcCall(IPredicate pre, Call cb, IPredicate post) {
		if (isOrIteFormula(post)) {
			return sdecCall(pre, cb, post);
		}
		TransFormula locVarAssignTf = cb.getTransitionFormula();
		boolean argumentsRestrictedByPre = 
				!varSetDisjoint(locVarAssignTf.getInVars().keySet(), pre.getVars());
		for (BoogieVar bv : post.getVars()) {
			if (bv.isGlobal()) {
				continue;
			} else {
				if (locVarAssignTf.getAssignedVars().contains(bv)) {
					if (argumentsRestrictedByPre) {
						continue;
					}
				}
			}
			m_EdgeCheckerBenchmark.getSdLazyCounter().incCa();
			return LBool.SAT;
		}
		return null;
	}
	
	
	public LBool sdecReturn(IPredicate pre, IPredicate hier, CodeBlock cb, IPredicate post) {
		Return ret = (Return) cb;
		Call call = ret.getCorrespondingCall();
		if (hierPostIndependent(hier, ret, post) 
				&& preHierIndependent(pre, hier, call)
				&& prePostIndependent(pre, ret, post)) {
			m_EdgeCheckerBenchmark.getSDsCounter().incRe();
			return LBool.SAT;

		}
		return null;
	}
	
	
	public LBool sdLazyEcReturn(IPredicate pre, IPredicate hier, Return cb, IPredicate post) {
		if (isOrIteFormula(post)) {
			return sdecReturn(pre, hier, cb, post);
		}
		Call call = cb.getCorrespondingCall();
		Set<BoogieVar> assignedVars = cb.getTransitionFormula().getAssignedVars();
		
		/*
		 * Old Version. Does not work if param set to constant.
		 * 
		// If there is an argument in post which is not assigned by return
		// and an parameter is in pre we have to return UNKNOWN
		Map<BoogieVar, TermVariable> arguments = call.getTransitionFormula().getInVars();
		Set<BoogieVar> parameters = call.getTransitionFormula().getAssignedVars();
		if (!varSetDisjoint(parameters, pre.getVars())) {
			for (BoogieVar bv : arguments.keySet()) {
				if (post.getVars().contains(bv) && !assignedVars.contains(bv)) {
					m_LazyEdgeCheckQueries++;
					return null;
				}
			}
		}
		 * 
		 */
		Set<BoogieVar> parameters = call.getTransitionFormula().getAssignedVars();
		if (!varSetDisjoint(parameters, pre.getVars())) {
			return null;
		}

		String proc = call.getCallStatement().getMethodName();
		Set<BoogieVar> modifiableGlobals = 
				m_ModifiableGlobalVariableManager.getModifiedBoogieVars(proc);
		boolean assignedVarsRestrictedByPre = 
				!varSetDisjoint(cb.getTransitionFormula().getInVars().keySet(), pre.getVars());
		for (BoogieVar bv : post.getVars()) {
			if (bv.isGlobal()) {
				if (bv.isOldvar()) {
					if (hier.getVars().contains(bv)) {
						continue;
					}
				} else {
					if (modifiableGlobals.contains(bv)) {
						if (pre.getVars().contains(bv)) {
							continue;
						}
					} else {
						if (hier.getVars().contains(bv)) {
							continue;
						}
						if (pre.getVars().contains(bv)) {
							continue;
						}

						
					}
					if (assignedVars.contains(bv)) {
						if (assignedVarsRestrictedByPre) {
							continue;
						}
					}
				}
				
			} else {
				if (assignedVars.contains(bv)) {
					if (assignedVarsRestrictedByPre) {
						continue;
					}
				} else {
					if (hier.getVars().contains(bv)) {
						continue;
					}
				}
			}
			m_EdgeCheckerBenchmark.getSdLazyCounter().incRe();
			return LBool.SAT;
		}
		return null;
	}
	

	private boolean preHierIndependent(IPredicate pre, IPredicate hier, Call call) {
		TransFormula locVarAssignTf = call.getTransitionFormula();
		//TODO: Matthias 7.10.2012 I hoped following would be sufficient.
		// But this is not sufficient when constant assigned to invar
		// e.g. pre is x!=0 and call is x_Out=1. Might be solved with
		// dataflow map.
		// 8.10.2012 consider also case where inVar is non-modifiable global
		// which does not occur in hier, but in pre
//		if (!varSetDisjoint(hier.getVars(), locVarAssignTf.getInVars().keySet())
//				&& !varSetDisjoint(locVarAssignTf.getAssignedVars(), pre.getVars())) {
//			return false;
//		}
		//workaround for preceding problem
		if (!varSetDisjoint(locVarAssignTf.getAssignedVars(), pre.getVars())) {
			return false;
		}
		
		// cases where pre and hier share non-modifiable var g, or
		// g occurs in hier, and old(g) occurs in pre.
		String proc = call.getCallStatement().getMethodName();
		Set<BoogieVar> modifiableGlobals = 
				m_ModifiableGlobalVariableManager.getModifiedBoogieVars(proc);

		
		for (BoogieVar bv : pre.getVars()) {
			if (bv.isGlobal()) {
				if (bv.isOldvar()) {
					if (hier.getVars().contains(((BoogieOldVar) bv).getNonOldVar())) {
						return false;
					}
				} else {
					if (!modifiableGlobals.contains(bv) 
							&& hier.getVars().contains(bv)) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	
	private boolean prePostIndependent(IPredicate pre, Return ret, IPredicate post) {
		TransFormula returnAssignTf = ret.getTransitionFormula();
		if (!varSetDisjoint(pre.getVars(), returnAssignTf.getInVars().keySet())
				&& !varSetDisjoint(returnAssignTf.getAssignedVars(), post.getVars())) {
			return false;
		}
		Call call = ret.getCorrespondingCall();
		TransFormula locVarAssignTf = call.getTransitionFormula();
		if (!varSetDisjoint(post.getVars(), locVarAssignTf.getInVars().keySet())
				&& !varSetDisjoint(locVarAssignTf.getAssignedVars(), pre.getVars())) {
			return false;
		}
		for (BoogieVar bv : pre.getVars()) {
			if (bv.isGlobal() && !bv.isOldvar()) {
				if (pre.getVars().contains(bv)) {
					return false;
				}
			}
		}
		return true;
	}
	
	
	private boolean hierPostIndependent(IPredicate hier, Return ret, IPredicate post) {
		Call call = ret.getCorrespondingCall();
		Set<BoogieVar> assignedVars = ret.getTransitionFormula().getAssignedVars();
		
		String proc = call.getCallStatement().getMethodName();
		Set<BoogieVar> modifiableGlobals = 
				m_ModifiableGlobalVariableManager.getModifiedBoogieVars(proc);
		
		for (BoogieVar bv : post.getVars()) {
			if (modifiableGlobals.contains(bv)) {
				//do nothing
				continue;
			}
			if (assignedVars.contains(bv)) {
				//do nothing
				continue;
			}
			if (hier.getVars().contains(bv)) {
				return false;
			}
		}
		return true;
	}
	
	
	
	
	/**
	 * If the assigned vars of cb are disjoint from the variables in p the
	 * selfloop (p,cb,p) is trivially inductive.
	 * Returns LBool.UNSAT if selfloop is inductive. Returns null if we are
	 * not able to determinie inductivity selfloop. 
	 */
	public LBool sdecInternalSelfloop(IPredicate p, CodeBlock cb) {
		Set<BoogieVar> assignedVars = cb.getTransitionFormula().getAssignedVars();
		Set<BoogieVar> occVars = p.getVars();
		for (BoogieVar occVar : occVars) {
			if (assignedVars.contains(occVar)) {
				return null;
			}
		}
		return LBool.UNSAT;
	}
	
	
	/**
	 * Returns UNSAT if p contains only non-old globals.
	 */
	public LBool sdecCallSelfloop(IPredicate p, CodeBlock cb) {
		for (BoogieVar bv : p.getVars()) {
			if (bv.isGlobal()) {
				if (bv.isOldvar()) {
					return null;
				} 
			} else {
				return null;
			}
		}
		return LBool.UNSAT;
	}
	
	
	
	public LBool sdecReturnSelfloopPre(IPredicate p, Return ret) {
		Set<BoogieVar> assignedVars = ret.getTransitionFormula().getAssignedVars();
		for (BoogieVar bv : p.getVars()) {
			if (bv.isGlobal()) {
				if (bv.isOldvar()) {
					return null;
				} else {
					if (assignedVars.contains(bv)) {
						return null;
					}
				}
			} else {
				return null;
			}
		}
		return LBool.UNSAT;
	}
	
	
	public LBool sdecReturnSelfloopHier(IPredicate p, Return ret) {
		Set<BoogieVar> assignedVars = ret.getTransitionFormula().getAssignedVars();
		String proc = ret.getCorrespondingCall().getCallStatement().getMethodName();
		Set<BoogieVar> modifiableGlobals = 
				m_ModifiableGlobalVariableManager.getModifiedBoogieVars(proc);

		for (BoogieVar bv : p.getVars()) {
			if (modifiableGlobals.contains(bv)) {
				return null;
			}
			if (assignedVars.contains(bv)) {
				return null;
			}
		}
		return LBool.UNSAT;
	}
	

	/**
	 * Returns true if the formula of this predicate is an or-term or an
	 * ite-term.
	 */
	private boolean isOrIteFormula(IPredicate p) {
		Term formula = p.getFormula();
		if (formula instanceof ApplicationTerm) {
			ApplicationTerm appTerm = (ApplicationTerm) formula;
			FunctionSymbol symbol = appTerm.getFunction();
			boolean result = symbol.getName().equals("or") || 
					symbol.getName().equals("ite"); 
			return result;
		} else {
			return false;
		}
	}

	
	public boolean isAssertionStackEmpty() {
		if (m_CodeBlock == null) {
			assert m_PrePred == null;
			assert m_HierPred == null;
			return true;
		} else {
			return false;
		}
	}
	
}
