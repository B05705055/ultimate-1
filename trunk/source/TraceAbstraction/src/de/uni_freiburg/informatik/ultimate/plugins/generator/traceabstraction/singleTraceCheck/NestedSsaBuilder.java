/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2010-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import de.uni_freiburg.informatik.ultimate.core.services.model.ILogger;

import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.boogie.BoogieNonOldVar;
import de.uni_freiburg.informatik.ultimate.boogie.BoogieOldVar;
import de.uni_freiburg.informatik.ultimate.boogie.BoogieVar;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.Substitution;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.TermTransferrer;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.PredicateUtils;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Call;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Return;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;

/**
 * A trace has single static assignment form (SSA) is each variable is assigned
 * exactly once ( http://en.wikipedia.org/wiki/Static_single_assignment_form).
 * 
 * This class transforms a trace to an SSA representation by renaming variables.
 * 
 * Roughly variable x is renamed to x_j, where j is the position where j is the
 * last position where x obtained a new value.
 * 
 * We use the SSA for checking satisfiability with an SMT solver, therefore we
 * represent the indexed variables by constants. Furthermore we replace all
 * auxiliary variables and branch encoders in the TransFormulas by fresh
 * constants.
 * 
 * We rename inVars of a variable x at trace position i+1 according to the
 * following scheme.
 * <ul>
 * <li>if x is local: we rename the inVar to x_j, where j is the largest
 * position <= i in the same calling context, where x is assigned. If x was not
 * assigned in this calling context up to position i, j is the start of the
 * calling context.
 * <li>if x is global and not oldvar: we rename the inVar to x_j, where j is the
 * largest position <=i where x is assigned. If x was not assigned up to
 * position i, j is the start of the lowest calling context (which is -1 if
 * there are no pending returns and numberOfPendingReturns-1 otherwise).
 * <li>if x is global and oldvar: if x is modifiable in the current calling
 * context we rename the inVar to x_j, where j is the start of the current
 * calling context, if x is not modifiable in the current calling context we
 * threat this variable as a non-oldVar
 * </ul>
 * If x is assigned at position i+1, we rename the outVar to x_{x+1}. If x in
 * not assigned at position i+1, the outVar does not exist or coincides with the
 * inVar and was already renamed above.
 * 
 * @author Matthias Heizmann
 * 
 */
public class NestedSsaBuilder {

	private final ILogger mLogger;

	private final static String s_GotosUnsupportedMessage = "TraceChecker is only applicable to RCFGs whose auxilliary goto edges have been removed";

	private final Script m_Script;
	private final SmtManager m_SmtManager;

	/**
	 * Map global BoogieVar bv to the constant bv_j that represents bv at the
	 * moment.
	 */
	final private Map<BoogieVar, Term> currentGlobalVarVersion = new HashMap<BoogieVar, Term>();
	/**
	 * Map local or oldVar BoogieVar bv to the constant bv_j that represents bv
	 * at the moment.
	 */
	protected Map<BoogieVar, Term> currentLocalAndOldVarVersion;

	/**
	 * Stores current versions for local or oldVar that are not visible at the
	 * moment.
	 */
	protected final Stack<Map<BoogieVar, Term>> currentVersionStack = new Stack<Map<BoogieVar, Term>>();

	private Integer startOfCallingContext;
	private final Stack<Integer> startOfCallingContextStack = new Stack<Integer>();

	private final Map<BoogieVar, TreeMap<Integer, Term>> m_IndexedVarRepresentative = new HashMap<BoogieVar, TreeMap<Integer, Term>>();

	public Map<BoogieVar, TreeMap<Integer, Term>> getIndexedVarRepresentative() {
		return m_IndexedVarRepresentative;
	}

	protected final Map<Term, BoogieVar> m_Constants2BoogieVar = new HashMap<Term, BoogieVar>();

	public Map<Term, BoogieVar> getConstants2BoogieVar() {
		return m_Constants2BoogieVar;
	}

	protected final NestedFormulas<TransFormula, IPredicate> m_Formulas;

	protected final ModifiableNestedFormulas<Term, Term> m_Ssa;
	protected final ModifiableNestedFormulas<Map<TermVariable, Term>, Map<TermVariable, Term>> m_Variable2Constant;

	private final ModifiableGlobalVariableManager m_ModGlobVarManager;

	private final Map<String, Term> m_IndexedConstants = new HashMap<String, Term>();

	public NestedFormulas<Term, Term> getSsa() {
		return m_Ssa;
	}

	public ModifiableNestedFormulas<Map<TermVariable, Term>, Map<TermVariable, Term>> getVariable2Constant() {
		return m_Variable2Constant;
	}

	protected String m_currentProcedure;

	/**
	 * maps position of pending context to position of pending return the
	 * positions of pending contexts are -2,-3,-4,...
	 */
	protected final Map<Integer, Integer> m_PendingContext2PendingReturn = new HashMap<Integer, Integer>();
	
	/**
	 * True iff the NestedSsaBuilder has to use a different Script than the
	 * Script that was used to construct the Term that occur in the 
	 * NestedFormulas that are the input for the SSA construction.
	 */
	private final boolean m_TransferToScriptNeeded;
	private final TermTransferrer m_TermTransferrer;
	

	public NestedSsaBuilder(NestedWord<? extends IAction> trace, SmtManager smtManager,
			NestedFormulas<TransFormula, IPredicate> nestedTransFormulas, 
			ModifiableGlobalVariableManager globModVarManager, ILogger logger,
			boolean transferToScriptNeeded) {
		mLogger = logger;
		m_Script = smtManager.getScript();
		m_SmtManager = smtManager;
		m_Formulas = nestedTransFormulas;
		m_ModGlobVarManager = globModVarManager;
		m_Ssa = new ModifiableNestedFormulas<Term, Term>(trace, new TreeMap<Integer, Term>());
		m_Variable2Constant = new ModifiableNestedFormulas<Map<TermVariable, Term>, Map<TermVariable, Term>>(trace,
				new TreeMap<Integer, Map<TermVariable, Term>>());
		m_TransferToScriptNeeded = transferToScriptNeeded;
		if (m_TransferToScriptNeeded) {
			m_TermTransferrer = new TermTransferrer(m_Script);
		} else {
			m_TermTransferrer = null;
		}
		buildSSA();
	}

	protected void buildSSA() {
		/*
		 * Step 1: We rename the formulas in each pending context. The index
		 * starts from (-1 - numberPendingContexts) and ends at -2. Furthermore
		 * we need the oldVarAssignment and the globalVarAssignment they will
		 * link the pending context with the pending return.
		 */
		final Integer[] pendingReturns = m_Formulas.getTrace().getPendingReturns().keySet().toArray(new Integer[0]);
		final int numberPendingContexts = pendingReturns.length;

		startOfCallingContext = -1 - numberPendingContexts;
		currentLocalAndOldVarVersion = new HashMap<BoogieVar, Term>();

		for (int i = numberPendingContexts - 1; i >= 0; i--) {
			final int pendingReturnPosition = pendingReturns[i];
			m_PendingContext2PendingReturn.put(startOfCallingContext, pendingReturnPosition);
			Return ret = (Return) m_Formulas.getTrace().getSymbol(pendingReturnPosition);
			Call correspondingCall = ret.getCorrespondingCall();
			m_currentProcedure = correspondingCall.getPreceedingProcedure();

			reVersionModifiableGlobals();
			if (i == numberPendingContexts - 1) {
				reVersionModifiableOldVars();
			} else {
				// have already been reversioned at the last oldVarAssignment
			}

			IPredicate pendingContext = m_Formulas.getPendingContext(pendingReturnPosition);
			VariableVersioneer pendingContextVV = new VariableVersioneer(pendingContext);
			pendingContextVV.versionPredicate();
			m_Ssa.setPendingContext(pendingReturnPosition, pendingContextVV.getVersioneeredTerm());
			m_Variable2Constant.setPendingContext(pendingReturnPosition, pendingContextVV.getSubstitutionMapping());

			TransFormula localVarAssignment = correspondingCall.getTransitionFormula();
			VariableVersioneer initLocalVarsVV = new VariableVersioneer(localVarAssignment);
			initLocalVarsVV.versionInVars();

			String calledProcedure = correspondingCall.getCallStatement().getMethodName();
			TransFormula oldVarAssignment = m_Formulas.getOldVarAssignment(pendingReturnPosition);
			VariableVersioneer initOldVarsVV = new VariableVersioneer(oldVarAssignment);
			initOldVarsVV.versionInVars();

			startOfCallingContextStack.push(startOfCallingContext);
			startOfCallingContext++;
			m_currentProcedure = calledProcedure;
			currentVersionStack.push(currentLocalAndOldVarVersion);
			currentLocalAndOldVarVersion = new HashMap<BoogieVar, Term>();

			/*
			 * Parameters and oldVars of procedure form that the pending return
			 * returns get the index of the next pending context.
			 */
			initOldVarsVV.versionAssignedVars(startOfCallingContext);
			initLocalVarsVV.versionAssignedVars(startOfCallingContext);

			m_Ssa.setOldVarAssignmentAtPos(pendingReturnPosition, initOldVarsVV.getVersioneeredTerm());
			m_Variable2Constant.setOldVarAssignmentAtPos(pendingReturnPosition, initOldVarsVV.getSubstitutionMapping());
			m_Ssa.setLocalVarAssignmentAtPos(pendingReturnPosition, initLocalVarsVV.getVersioneeredTerm());
			m_Variable2Constant.setLocalVarAssignmentAtPos(pendingReturnPosition,
					initLocalVarsVV.getSubstitutionMapping());
		}

		assert (startOfCallingContext == -1);

		/*
		 * Step 2: We rename the formula of the precondition. We use as index
		 * -1.
		 */
		if (m_currentProcedure == null) {
			assert numberPendingContexts == 0;
			IAction firstCodeBlock = m_Formulas.getTrace().getSymbolAt(0);
			m_currentProcedure = firstCodeBlock.getPreceedingProcedure();
		}
		reVersionModifiableGlobals();
		if (pendingReturns.length == 0) {
			reVersionModifiableOldVars();
		} else {
			// have already been reversioned at the last oldVarAssignment
		}
		VariableVersioneer precondVV = new VariableVersioneer(m_Formulas.getPrecondition());
		precondVV.versionPredicate();
		m_Ssa.setPrecondition(precondVV.getVersioneeredTerm());
		m_Variable2Constant.setPrecondition(precondVV.getSubstitutionMapping());

		/*
		 * Step 3: We rename the TransFormulas of the traces CodeBlocks
		 */
		int numberOfPendingCalls = 0;
		for (int i = 0; i < m_Formulas.getTrace().length(); i++) {
			IAction symbol = m_Formulas.getTrace().getSymbolAt(i);
//			if (symbol instanceof GotoEdge) {
//				throw new IllegalArgumentException(s_GotosUnsupportedMessage);
//			}

			TransFormula tf;
			if (m_Formulas.getTrace().isCallPosition(i)) {
				tf = m_Formulas.getLocalVarAssignment(i);
			} else {
				tf = m_Formulas.getFormulaFromNonCallPos(i);
			}
			assert tf != null : "CodeBlock " + symbol + " has no TransFormula";
			VariableVersioneer tfVV = new VariableVersioneer(tf);
			tfVV.versionInVars();

			if (m_Formulas.getTrace().isCallPosition(i)) {
				assert (symbol instanceof Call) : "current implementation supports only Call";
				if (m_Formulas.getTrace().isPendingCall(i)) {
					numberOfPendingCalls++;
				}
				Call call = (Call) symbol;
				String calledProcedure = call.getCallStatement().getMethodName();
				m_currentProcedure = calledProcedure;
				TransFormula oldVarAssignment = m_Formulas.getOldVarAssignment(i);
				VariableVersioneer initOldVarsVV = new VariableVersioneer(oldVarAssignment);
				initOldVarsVV.versionInVars();
				startOfCallingContextStack.push(startOfCallingContext);
				startOfCallingContext = i;

				currentVersionStack.push(currentLocalAndOldVarVersion);
				currentLocalAndOldVarVersion = new HashMap<BoogieVar, Term>();

				initOldVarsVV.versionAssignedVars(i);
				m_Ssa.setOldVarAssignmentAtPos(i, initOldVarsVV.getVersioneeredTerm());
				m_Variable2Constant.setOldVarAssignmentAtPos(i, initOldVarsVV.getSubstitutionMapping());

				TransFormula globalVarAssignment = m_Formulas.getGlobalVarAssignment(i);
				VariableVersioneer initGlobalVarsVV = new VariableVersioneer(globalVarAssignment);
				initGlobalVarsVV.versionInVars();
				initGlobalVarsVV.versionAssignedVars(i);
				m_Ssa.setGlobalVarAssignmentAtPos(i, initGlobalVarsVV.getVersioneeredTerm());
				m_Variable2Constant.setGlobalVarAssignmentAtPos(i, initGlobalVarsVV.getSubstitutionMapping());

			}
			if (m_Formulas.getTrace().isReturnPosition(i)) {
				Return ret = (Return) symbol;
				m_currentProcedure = ret.getCallerProgramPoint().getProcedure();
				currentLocalAndOldVarVersion = currentVersionStack.pop();
				startOfCallingContext = startOfCallingContextStack.pop();
			}
			tfVV.versionAssignedVars(i);
			tfVV.versionBranchEncoders(i);
			tfVV.replaceAuxVars();
			if (m_Formulas.getTrace().isCallPosition(i)) {
				m_Ssa.setLocalVarAssignmentAtPos(i, tfVV.getVersioneeredTerm());
				m_Variable2Constant.setLocalVarAssignmentAtPos(i, tfVV.getSubstitutionMapping());
			} else {
				m_Ssa.setFormulaAtNonCallPos(i, tfVV.getVersioneeredTerm());
				m_Variable2Constant.setFormulaAtNonCallPos(i, tfVV.getSubstitutionMapping());
			}
		}

		/*
		 * Step 4: We rename the postcondition.
		 */
		assert currentVersionStack.size() == numberOfPendingCalls;
		assert numberOfPendingCalls > 0 || startOfCallingContext == -1 - numberPendingContexts;
		assert numberOfPendingCalls == 0 || numberPendingContexts == 0;

		VariableVersioneer postCondVV = new VariableVersioneer(m_Formulas.getPostcondition());
		postCondVV.versionPredicate();
		m_Ssa.setPostcondition(postCondVV.getVersioneeredTerm());
		m_Variable2Constant.setPostcondition(postCondVV.getSubstitutionMapping());

	}

	/**
	 * Set new var version for all globals that are modifiable by the current
	 * procedure.
	 */
	protected void reVersionModifiableGlobals() {
		Set<BoogieVar> modifiable = m_ModGlobVarManager.getGlobalVarsAssignment(m_currentProcedure).getAssignedVars();
		for (BoogieVar bv : modifiable) {
			setCurrentVarVersion(bv, startOfCallingContext);
		}
	}

	/**
	 * Set new var version for all oldVars that are modifiable by the current
	 * procedure.
	 */
	protected void reVersionModifiableOldVars() {
		Set<BoogieVar> modifiable = m_ModGlobVarManager.getOldVarsAssignment(m_currentProcedure).getAssignedVars();
		for (BoogieVar bv : modifiable) {
			setCurrentVarVersion(bv, startOfCallingContext);
		}
	}

	/**
	 * Compute identifier of the Constant that represents the branch encoder tv
	 * at position pos.
	 */
	public static String branchEncoderConstantName(TermVariable tv, int pos) {
		String name = tv.getName() + "_" + pos;
		return name;
	}

	class VariableVersioneer {
		private final TransFormula m_TF;
		private final IPredicate m_Pred;
		private final Map<TermVariable, Term> m_SubstitutionMapping = new HashMap<TermVariable, Term>();
		private Term m_formula;

		public VariableVersioneer(TransFormula tf) {
			m_TF = tf;
			m_Pred = null;
			m_formula = transferToCurrentScriptIfNecessary(tf.getFormula());
		}

		public VariableVersioneer(IPredicate pred) {
			m_TF = null;
			m_Pred = pred;
			m_formula = transferToCurrentScriptIfNecessary(pred.getFormula());
		}

		public void versionInVars() {
			for (BoogieVar bv : m_TF.getInVars().keySet()) {
				TermVariable tv = transferToCurrentScriptIfNecessary(m_TF.getInVars().get(bv));
				Term versioneered = getCurrentVarVersion(bv);
				m_Constants2BoogieVar.put(versioneered, bv);
				m_SubstitutionMapping.put(tv, versioneered);
			}
		}

		public void versionAssignedVars(int currentPos) {
			for (BoogieVar bv : m_TF.getAssignedVars()) {
				TermVariable tv = transferToCurrentScriptIfNecessary(m_TF.getOutVars().get(bv));
				Term versioneered = setCurrentVarVersion(bv, currentPos);
				m_Constants2BoogieVar.put(versioneered, bv);
				m_SubstitutionMapping.put(tv, versioneered);
			}
		}

		public void versionBranchEncoders(int currentPos) {
			for (TermVariable tv : m_TF.getBranchEncoders()) {
				tv = transferToCurrentScriptIfNecessary(tv);
				String name = branchEncoderConstantName(tv, currentPos);
				m_Script.declareFun(name, new Sort[0], tv.getSort());
				m_SubstitutionMapping.put(tv, m_Script.term(name));
			}
		}

		public void replaceAuxVars() {
			for (TermVariable tv : m_TF.getAuxVars()) {
				// we deliberately construct the fresh variable in the 
				// old script first and translate it afterwards
				Term freshConst = m_SmtManager.getVariableManager().constructFreshConstant(tv);
				tv = transferToCurrentScriptIfNecessary(tv);
				freshConst = transferToCurrentScriptIfNecessary(freshConst);
				m_SubstitutionMapping.put(tv, freshConst);
			}
		}

		public void versionPredicate() {
			for (BoogieVar bv : m_Pred.getVars()) {
				TermVariable tv = transferToCurrentScriptIfNecessary(bv.getTermVariable());
				Term versioneered = getCurrentVarVersion(bv);
				m_Constants2BoogieVar.put(versioneered, bv);
				m_SubstitutionMapping.put(tv, versioneered);
			}
		}

		public Term getVersioneeredTerm() {
			Substitution subst = new Substitution(m_SubstitutionMapping, m_Script);
			Term result = subst.transform(m_formula);
			assert result.getFreeVars().length == 0 : "free vars in versioneered term: " + String.valueOf(result.getFreeVars());
			return result;
		}

		public Map<TermVariable, Term> getSubstitutionMapping() {
			return m_SubstitutionMapping;
		}

	}

	/**
	 * Get the current version of BoogieVariable bv. Construct this version if
	 * it does not exist yet.
	 */
	private Term getCurrentVarVersion(BoogieVar bv) {
		Term result;
		if (bv.isGlobal()) {
			if (bv instanceof BoogieOldVar) {
				assert bv.isOldvar();
				BoogieOldVar oldVar = (BoogieOldVar) bv;
				if (m_ModGlobVarManager.isModifiable((BoogieOldVar) oldVar, m_currentProcedure)) {
					result = currentLocalAndOldVarVersion.get(oldVar);
				} else {
					// not modifiable in current procedure
					// according to semantics value of oldvar is value of
					// non-oldvar at beginning of procedure
					// we use current var version of non-oldvar
					result = getOrSetCurrentGlobalVarVersion(oldVar.getNonOldVar());
				}
			} else {
				BoogieNonOldVar bnov = (BoogieNonOldVar) bv;
				result = getOrSetCurrentGlobalVarVersion(bnov);
			}
		} else {
			result = currentLocalAndOldVarVersion.get(bv);
			if (result == null) {
				// variable was not yet assigned in the calling context
				result = setCurrentVarVersion(bv, startOfCallingContext);
			}
		}
		return result;
	}

	/**
	 * Get current version for global variable. Set current var version if it
	 * has not yet been set.
	 */
	private Term getOrSetCurrentGlobalVarVersion(BoogieNonOldVar bv) {
		Term result;
		result = currentGlobalVarVersion.get(bv);
		if (result == null) {
			// variable was not yet assigned in trace
			// FIXME: in oder to be compliant with the documentation
			// we should use an initial calling context
			// -1-numberOfCallingContexts. But this should not have
			// an impact on correctness.
			result = setCurrentVarVersion(bv, -1);
		}
		return result;
	}

	/**
	 * Set the current version of BoogieVariable bv to the constant b_index and
	 * return b_index.
	 */
	private Term setCurrentVarVersion(BoogieVar bv, int index) {
		Term var = buildVersion(bv, index);
		if (bv.isGlobal()) {
			if (bv.isOldvar()) {
				assert (index == startOfCallingContext) : "oldVars may only be assigned at entry of procedure";
				currentLocalAndOldVarVersion.put(bv, var);
			} else {
				currentGlobalVarVersion.put(bv, var);
			}
		} else {
			currentLocalAndOldVarVersion.put(bv, var);
		}
		return var;
	}

	/**
	 * Build constant bv_index that represents BoogieVar bv that obtains a new
	 * value at position index.
	 */
	private Term buildVersion(BoogieVar bv, int index) {
		TreeMap<Integer, Term> index2constant = m_IndexedVarRepresentative.get(bv);
		if (index2constant == null) {
			index2constant = new TreeMap<Integer, Term>();
			m_IndexedVarRepresentative.put(bv, index2constant);
		}
		assert !index2constant.containsKey(index) : "version was already constructed";
		Sort sort = transferToCurrentScriptIfNecessary(bv.getTermVariable()).getSort();
		Term constant = PredicateUtils.getIndexedConstant(bv.getGloballyUniqueId(), 
				sort, index, m_IndexedConstants, m_Script);
		index2constant.put(index, constant);
		return constant;
	}

	/**
	 * May the corresponding global var of the oldvar bv be modified in in the
	 * current calling context (according to modifies clauses?)
	 */
	private boolean modifiedInCurrentCallingContext(BoogieVar bv) {
		if (!bv.isGlobal()) {
			throw new IllegalArgumentException(bv + " no global var");
		}
		TransFormula oldVarAssignment;
		if (startOfCallingContext >= 0) {
			oldVarAssignment = m_Formulas.getOldVarAssignment(startOfCallingContext);
		} else if (startOfCallingContext == -1) {
			// from some point of view each variable is modified in the
			// initial calling context, because variables get their
			// initial values here
			return true;
		} else {
			assert startOfCallingContext < -1;
			int pendingReturnPosition = m_PendingContext2PendingReturn.get(startOfCallingContext);
			oldVarAssignment = m_Formulas.getOldVarAssignment(pendingReturnPosition);
		}
		boolean isModified;
		if (bv.isOldvar()) {
			isModified = oldVarAssignment.getAssignedVars().contains(bv);
		} else {
			isModified = oldVarAssignment.getInVars().keySet().contains(bv);
		}
		return isModified;
	}
	
	private Term transferToCurrentScriptIfNecessary(Term term) {
		if (m_TransferToScriptNeeded) {
			return m_TermTransferrer.transform(term);
		} else {
			return term;
		}
	}
	
	private TermVariable transferToCurrentScriptIfNecessary(TermVariable tv) {
		if (m_TransferToScriptNeeded) {
			return (TermVariable) m_TermTransferrer.transform(tv);
		} else {
			return tv;
		}
	}
}
