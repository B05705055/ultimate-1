/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2014-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.QuantifiedFormula;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Util;
import de.uni_freiburg.informatik.ultimate.model.boogie.BoogieVar;
import de.uni_freiburg.informatik.ultimate.model.boogie.DeclarationInformation;
import de.uni_freiburg.informatik.ultimate.model.boogie.DeclarationInformation.StorageClass;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssertStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssignmentStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssumeStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BoogieASTNode;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.CallStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.EnsuresSpecification;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.HavocStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.LeftHandSide;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.ModifiesSpecification;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Procedure;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.RequiresSpecification;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Specification;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VarList;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VariableLHS;
import de.uni_freiburg.informatik.ultimate.model.location.ILocation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Expression2Term.IdentifierTranslator;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Expression2Term.MultiTermResult;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Expression2Term.SingleTermResult;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula.Infeasibility;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.partialQuantifierElimination.XnfDer;

/**
 * Translates statements into TransFormulas. The resulting TransFormula encodes
 * the transition relation of the statements as SMT formula.
 * 
 * Idea of underlying algorithm: Starts at the end of the statement sequence
 * take current variables as outVars and then computes the inVars by traversing
 * the sequence of statements backwards and computing some kind of weakest
 * precondition.
 * 
 * @author Matthias Heizmann
 * 
 */
public class Statements2TransFormula {

	/**
	 * Compute Formulas that encode violation of one of the added assert
	 * statements. This feature was used in Evrens old CFG.
	 */
	private final static boolean s_ComputeAsserts = false;
	private final static String s_ComputeAssertsNotAvailable = "computation of asserts not available";

	private final Script m_Script;
	private final BoogieDeclarations m_BoogieDeclarations;
	private final Boogie2SMT m_Boogie2SMT;
	private final VariableManager m_VariableManager;
	private final Boogie2SmtSymbolTable m_Boogie2SmtSymbolTable;
	private final Expression2Term m_Expression2Term;

	private String m_CurrentProcedure;

	private HashMap<BoogieVar, TermVariable> m_OutVars;
	private HashMap<BoogieVar, TermVariable> m_InVars;

	/**
	 * Auxiliary variables. TermVariables that occur neither as inVar nor as
	 * outVar. If you use the assumes or asserts to encode a transition the
	 * auxiliary variables are existentially quantified.
	 */
	private HashSet<TermVariable> m_AuxVars;

	private Term m_Assumes;
	private Term m_Asserts;
	private final IUltimateServiceProvider mServices;
	private Map<String, ILocation> m_Overapproximations = null;

	public Statements2TransFormula(Boogie2SMT boogie2smt, IUltimateServiceProvider services, Expression2Term expression2Term) {
		super();
		mServices = services;
		m_Boogie2SMT = boogie2smt;
		m_Script = boogie2smt.getScript();
		m_Expression2Term = expression2Term;
		m_Boogie2SmtSymbolTable = m_Boogie2SMT.getBoogie2SmtSymbolTable();
		m_VariableManager = m_Boogie2SMT.getVariableManager();
		m_BoogieDeclarations = m_Boogie2SMT.getBoogieDeclarations();
	}

	/**
	 * Initialize fields to allow construction of a new TransFormula
	 * 
	 * @param procId
	 */
	private void initialize(String procId) {
		assert m_CurrentProcedure == null;
		assert m_OutVars == null;
		assert m_InVars == null;
		assert m_AuxVars == null;
		assert m_Assumes == null;

		m_Overapproximations = new HashMap<>();
		m_CurrentProcedure = procId;
		m_OutVars = new HashMap<BoogieVar, TermVariable>();
		m_InVars = new HashMap<BoogieVar, TermVariable>();
		m_AuxVars = new HashSet<TermVariable>();
		m_Assumes = m_Script.term("true");
		if (s_ComputeAsserts) {
			m_Asserts = m_Script.term("true");
		}
	}

	private TranslationResult getTransFormula(boolean simplify, boolean feasibilityKnown) {
		Set<TermVariable> auxVars = m_AuxVars;
		Term formula = m_Assumes;
		formula = eliminateAuxVars(m_Assumes, auxVars);

		Infeasibility infeasibility = null;
		if (simplify) {
			formula = SmtUtils.simplify(m_Script, formula, mServices);
			if (formula == m_Script.term("false")) {
				infeasibility = Infeasibility.INFEASIBLE;
			}
		}

		if (feasibilityKnown) {
			infeasibility = Infeasibility.UNPROVEABLE;
		}

		if (infeasibility == null) {
			if (simplify) {
				infeasibility = Infeasibility.UNPROVEABLE;
			} else {
				LBool isSat = Util.checkSat(m_Script, formula);
				if (isSat == LBool.UNSAT) {
					formula = m_Script.term("false");
					infeasibility = Infeasibility.INFEASIBLE;
				} else {
					infeasibility = Infeasibility.UNPROVEABLE;
				}

			}
		}
		TransFormula.removeSuperfluousVars(formula, m_InVars, m_OutVars, auxVars);
		HashSet<TermVariable> branchEncoders = new HashSet<TermVariable>(0);
		Term closedFormula = TransFormula.computeClosedFormula(formula, m_InVars, m_OutVars, auxVars, false,
				m_Boogie2SMT);
		TransFormula tf = new TransFormula(formula, m_InVars, m_OutVars, auxVars, branchEncoders, infeasibility,
				closedFormula);
		m_CurrentProcedure = null;
		m_OutVars = null;
		m_InVars = null;
		m_AuxVars = null;
		m_Assumes = null;
		return new TranslationResult(tf, m_Overapproximations);
	}

	private BoogieVar getModifiableBoogieVar(String id, DeclarationInformation declInfo) {
		StorageClass storageClass = declInfo.getStorageClass();
		// assert (declInfo.getProcedure() == null ||
		// declInfo.getProcedure().equals(m_CurrentProcedure));
		BoogieVar result;
		switch (storageClass) {
		case GLOBAL:
		case LOCAL:
		case IMPLEMENTATION_OUTPARAM:
		case PROC_FUNC_OUTPARAM:
			result = m_Boogie2SmtSymbolTable.getBoogieVar(id, declInfo, false);
			break;
		case IMPLEMENTATION_INPARAM:
		case PROC_FUNC_INPARAM:
			throw new AssertionError("not modifiable");
		case IMPLEMENTATION:
		case PROC_FUNC:
		case QUANTIFIED:
		default:
			throw new AssertionError("no appropriate variable ");
		}
		return result;
	}

	private IdentifierTranslator[] getIdentifierTranslatorsIntraprocedural() {
		return new IdentifierTranslator[] { new LocalVarTranslatorWithInOutVarManagement(),
				new GlobalVarTranslatorWithInOutVarManagement(m_CurrentProcedure, false),
				m_Boogie2SMT.getConstOnlyIdentifierTranslator() };
	}

	/**
	 * Let assign be a statement of the form v_i:=expr_i Remove v_i from the
	 * inVars (if contained). If neccessary v_i is put to outVars (possibly by
	 * getSmtIdentifier).
	 */
	private void addAssignment(AssignmentStatement assign) {
		LeftHandSide[] lhs = assign.getLhs();
		Expression[] rhs = assign.getRhs();
		Map<TermVariable, Expression> addedEqualities = new HashMap<TermVariable, Expression>();
		for (int i = 0; i < lhs.length; i++) {
			/* ArrayLHS are removed by preprocessor */
			VariableLHS vlhs = (VariableLHS) lhs[i];
			assert vlhs.getDeclarationInformation() != null : " no declaration information";
			String name = vlhs.getIdentifier();
			DeclarationInformation declInfo = vlhs.getDeclarationInformation();
			BoogieVar boogieVar = getModifiableBoogieVar(name, declInfo);
			assert (boogieVar != null);
			getOrConstuctCurrentRepresentative(boogieVar);
			if (m_InVars.containsKey(boogieVar)) {
				TermVariable tv = m_InVars.get(boogieVar);
				addedEqualities.put(tv, rhs[i]);
				removeInVar(boogieVar);
			}
		}
		IdentifierTranslator[] its = getIdentifierTranslatorsIntraprocedural();

		for (TermVariable tv : addedEqualities.keySet()) {

			SingleTermResult tlres = m_Expression2Term.translateToTerm(its, addedEqualities.get(tv));
			m_AuxVars.addAll(tlres.getAuxiliaryVars());
			m_Overapproximations.putAll(tlres.getOverappoximations()); 
			Term rhsTerm = tlres.getTerm();
			Term eq = m_Script.term("=", tv, rhsTerm);

			m_Assumes = Util.and(m_Script, eq, m_Assumes);
			if (s_ComputeAsserts) {
				m_Asserts = Util.implies(m_Script, eq, m_Asserts);
			}
		}
	}

	private void addHavoc(HavocStatement havoc) {
		for (VariableLHS lhs : havoc.getIdentifiers()) {
			assert lhs.getDeclarationInformation() != null : " no declaration information";
			String name = lhs.getIdentifier();
			DeclarationInformation declInfo = lhs.getDeclarationInformation();
			BoogieVar boogieVar = getModifiableBoogieVar(name, declInfo);
			assert (boogieVar != null);
			getOrConstuctCurrentRepresentative(boogieVar);
			if (m_InVars.containsKey(boogieVar)) {
				removeInVar(boogieVar);
			}
		}
	}

	private void addAssume(AssumeStatement assume) {
		IdentifierTranslator[] its = getIdentifierTranslatorsIntraprocedural();

		SingleTermResult tlres = m_Expression2Term.translateToTerm(its, assume.getFormula());
		m_AuxVars.addAll(tlres.getAuxiliaryVars());
		m_Overapproximations.putAll(tlres.getOverappoximations()); 
		Term f = tlres.getTerm();
		
		m_Assumes = Util.and(m_Script, f, m_Assumes);
		if (s_ComputeAsserts) {
			m_Asserts = Util.implies(m_Script, f, m_Asserts);
		}
	}

	private void addAssert(AssertStatement assertstmt) {
		if (s_ComputeAsserts) {
			IdentifierTranslator[] its = getIdentifierTranslatorsIntraprocedural();
			SingleTermResult tlres = m_Expression2Term.translateToTerm(its, assertstmt.getFormula());
			m_AuxVars.addAll(tlres.getAuxiliaryVars());
			m_Overapproximations.putAll(tlres.getOverappoximations()); 
			Term f = tlres.getTerm();
			
			m_Assumes = Util.and(m_Script, f, m_Assumes);
			m_Asserts = Util.and(m_Script, f, m_Asserts);
			assert (m_Assumes.toString() instanceof Object);
		} else {
			throw new AssertionError(s_ComputeAssertsNotAvailable);
		}
	}

	private void addSummary(CallStatement call) {
		Procedure procedure = m_BoogieDeclarations.getProcSpecification().get(call.getMethodName());

		HashMap<String, Term> substitution = new HashMap<String, Term>();
		Expression[] arguments = call.getArguments();
		int offset;
		VariableLHS[] callLhs = call.getLhs();
		offset = 0;
		ArrayList<BoogieVar> callLhsBvs = new ArrayList<BoogieVar>();
		for (VarList outParamVl : procedure.getOutParams()) {
			for (String outParamId : outParamVl.getIdentifiers()) {
				String callLhsId = callLhs[offset].getIdentifier();
				DeclarationInformation callLhsDeclInfo = callLhs[offset].getDeclarationInformation();
				BoogieVar callLhsBv = getModifiableBoogieVar(callLhsId, callLhsDeclInfo);
				assert (callLhsBv != null);
				TermVariable callLhsTv = getOrConstuctCurrentRepresentative(callLhsBv);

				substitution.put(outParamId, callLhsTv);
				callLhsBvs.add(callLhsBv);
				offset++;
			}
		}

		for (BoogieVar bv : callLhsBvs) {
			removeInVar(bv);
		}

		Map<BoogieVar, Term> requiresSubstitution = new HashMap<BoogieVar, Term>();
		Map<BoogieVar, Term> ensuresSubstitution = new HashMap<BoogieVar, Term>();

		for (Specification spec : procedure.getSpecification()) {
			if (spec instanceof ModifiesSpecification) {
				for (VariableLHS var : ((ModifiesSpecification) spec).getIdentifiers()) {
					String id = var.getIdentifier();
					BoogieVar boogieVar = m_Boogie2SmtSymbolTable.getBoogieVar(id, var.getDeclarationInformation(),
							false);
					BoogieVar boogieOldVar = m_Boogie2SmtSymbolTable.getBoogieVar(id, var.getDeclarationInformation(),
							true);
					assert boogieVar != null;
					assert boogieOldVar != null;
					TermVariable tvAfter = getOrConstuctCurrentRepresentative(boogieVar);
					removeInVar(boogieVar);

					TermVariable tvBefore = m_VariableManager.constructFreshTermVariable(boogieVar);
					m_InVars.put(boogieVar, tvBefore);
					ensuresSubstitution.put(boogieVar, tvAfter);
					ensuresSubstitution.put(boogieOldVar, tvBefore);
					requiresSubstitution.put(boogieVar, tvBefore);
					requiresSubstitution.put(boogieOldVar, tvBefore);

				}
			}
		}

		Term[] argumentTerms;
		{
			IdentifierTranslator[] its = getIdentifierTranslatorsIntraprocedural();
			MultiTermResult tlres = m_Expression2Term.translateToTerms(its, arguments); 
			m_AuxVars.addAll(tlres.getAuxiliaryVars());
			m_Overapproximations.putAll(tlres.getOverappoximations()); 
			argumentTerms = tlres.getTerms();
		}

		offset = 0;
		for (VarList vl : procedure.getInParams()) {
			for (String id : vl.getIdentifiers()) {
				substitution.put(id, argumentTerms[offset++]);
			}
		}

		String calledProcedure = call.getMethodName();

		IdentifierTranslator[] ensIts = new IdentifierTranslator[] { new SubstitutionTranslatorId(substitution),
				new SubstitutionTranslatorBoogieVar(ensuresSubstitution),
				new GlobalVarTranslatorWithInOutVarManagement(calledProcedure, false),
				m_Boogie2SMT.getConstOnlyIdentifierTranslator() };

		for (Specification spec : procedure.getSpecification()) {
			if (spec instanceof EnsuresSpecification) {
				Expression post = ((EnsuresSpecification) spec).getFormula();
				SingleTermResult tlres = m_Expression2Term.translateToTerm(ensIts, post);
				m_AuxVars.addAll(tlres.getAuxiliaryVars());
				m_Overapproximations.putAll(tlres.getOverappoximations()); 
				Term f = tlres.getTerm();
				m_Assumes = Util.and(m_Script, f, m_Assumes);
				if (s_ComputeAsserts) {
					if (spec.isFree()) {
						m_Asserts = Util.implies(m_Script, f, m_Asserts);
					} else {
						m_Asserts = Util.and(m_Script, f, m_Asserts);
					}
				}
			}
		}

		IdentifierTranslator[] reqIts = new IdentifierTranslator[] { new SubstitutionTranslatorId(substitution),
				new SubstitutionTranslatorBoogieVar(requiresSubstitution),
				new GlobalVarTranslatorWithInOutVarManagement(calledProcedure, false),
				m_Boogie2SMT.getConstOnlyIdentifierTranslator() };

		for (Specification spec : procedure.getSpecification()) {
			if (spec instanceof RequiresSpecification) {
				Expression pre = ((RequiresSpecification) spec).getFormula();
				SingleTermResult tlres = m_Expression2Term.translateToTerm(reqIts, pre);
				m_AuxVars.addAll(tlres.getAuxiliaryVars());
				m_Overapproximations.putAll(tlres.getOverappoximations()); 
				Term f = tlres.getTerm();
				m_Assumes = Util.and(m_Script, f, m_Assumes);
				if (s_ComputeAsserts) {
					if (spec.isFree()) {
						m_Asserts = Util.implies(m_Script, f, m_Asserts);
					} else {
						m_Asserts = Util.and(m_Script, f, m_Asserts);
					}
				}
			}
		}
	}

	/**
	 * Remove boogieVars from inVars mapping, if the inVar is not an outVar, add
	 * it to he auxilliary variables auxVar.
	 */
	private void removeInVar(BoogieVar boogieVar) {
		TermVariable tv = m_InVars.remove(boogieVar);
		if (m_OutVars.get(boogieVar) != tv) {
			m_AuxVars.add(tv);
		}
	}

	/**
	 * Obtain TermVariable that represents BoogieVar bv at the current position.
	 * This is the current inVar. If this inVar does not yet exist, we create
	 * it. In this case we have to add (bv,tv) to the outVars if bv is not
	 * already an outvar.
	 */
	private TermVariable getOrConstuctCurrentRepresentative(BoogieVar bv) {
		TermVariable tv = m_InVars.get(bv);
		if (tv == null) {
			tv = createInVar(bv);
			if (!m_OutVars.containsKey(bv)) {
				m_OutVars.put(bv, tv);
			}
		}
		return tv;
	}

	/**
	 * Construct fresh TermVariable for BoogieVar bv and add it to inVars.
	 * Special case: If BoogieVar bv is an oldVar we do not take a fresh
	 * TermVariable but the default TermVariable for this BoogieVar.
	 */
	private TermVariable createInVar(BoogieVar bv) {
		TermVariable tv;
		if (bv.isOldvar()) {
			tv = bv.getTermVariable();
		} else {
			tv = m_VariableManager.constructFreshTermVariable(bv);
		}
		m_InVars.put(bv, tv);
		return tv;
	}

	public abstract class IdentifierTranslatorWithInOutVarManagement implements IdentifierTranslator {

		@Override
		public Term getSmtIdentifier(String id, DeclarationInformation declInfo, boolean isOldContext,
				BoogieASTNode boogieASTNode) {
			BoogieVar bv = getBoogieVar(id, declInfo, isOldContext, boogieASTNode);
			if (bv == null) {
				return null;
			} else {
				TermVariable tv = getOrConstuctCurrentRepresentative(bv);
				return tv;
			}
		}

		abstract protected BoogieVar getBoogieVar(String id, DeclarationInformation declInfo, boolean isOldContext,
				BoogieASTNode boogieASTNode);

	}

	public class LocalVarTranslatorWithInOutVarManagement extends IdentifierTranslatorWithInOutVarManagement {

		@Override
		protected BoogieVar getBoogieVar(String id, DeclarationInformation declInfo, boolean isOldContext,
				BoogieASTNode boogieASTNode) {
			StorageClass storageClass = declInfo.getStorageClass();
			switch (storageClass) {
			case IMPLEMENTATION_INPARAM:
			case IMPLEMENTATION_OUTPARAM:
			case PROC_FUNC_INPARAM:
			case PROC_FUNC_OUTPARAM:
			case LOCAL:
				return m_Boogie2SmtSymbolTable.getBoogieVar(id, declInfo, isOldContext);
			case GLOBAL:
				return null;
			case IMPLEMENTATION:
			case PROC_FUNC:
			case QUANTIFIED:
			default:
				throw new AssertionError();
			}
		}
	}

	public class GlobalVarTranslatorWithInOutVarManagement extends IdentifierTranslatorWithInOutVarManagement {
		private final String m_CurrentProcedure;
		/**
		 * Translate all variables to the non old global variable, independent
		 * of the context. This feature is not used at the moment. Maybe we can
		 * drop it.
		 */
		private final boolean m_AllNonOld;
		private Set<String> m_ModifiableByCurrentProcedure;

		public GlobalVarTranslatorWithInOutVarManagement(String currentProcedure, boolean allNonOld) {
			m_CurrentProcedure = currentProcedure;
			m_AllNonOld = allNonOld;
			m_ModifiableByCurrentProcedure = m_BoogieDeclarations.getModifiedVars().get(m_CurrentProcedure);

		}

		@Override
		protected BoogieVar getBoogieVar(String id, DeclarationInformation declInfo, boolean isOldContext,
				BoogieASTNode boogieASTNode) {
			StorageClass storageClass = declInfo.getStorageClass();
			switch (storageClass) {
			case IMPLEMENTATION_INPARAM:
			case IMPLEMENTATION_OUTPARAM:
			case PROC_FUNC_INPARAM:
			case PROC_FUNC_OUTPARAM:
			case LOCAL:
				return null;
			case GLOBAL:
				BoogieVar bv;
				if (isOldContext) {
					if (m_AllNonOld || !modifiableByCurrentProcedure(id)) {
						bv = m_Boogie2SmtSymbolTable.getBoogieVar(id, declInfo, false);
					} else {
						bv = m_Boogie2SmtSymbolTable.getBoogieVar(id, declInfo, true);
					}
				} else {
					bv = m_Boogie2SmtSymbolTable.getBoogieVar(id, declInfo, false);
				}
				return bv;
			case IMPLEMENTATION:
			case PROC_FUNC:
			case QUANTIFIED:
			default:
				throw new AssertionError();
			}
		}

		private boolean modifiableByCurrentProcedure(String id) {
			return m_ModifiableByCurrentProcedure.contains(id);
		}

	}

	private class SubstitutionTranslatorId implements IdentifierTranslator {
		private final Map<String, Term> m_Substitution;

		public SubstitutionTranslatorId(Map<String, Term> substitution) {
			super();
			m_Substitution = substitution;
		}

		@Override
		public Term getSmtIdentifier(String id, DeclarationInformation declInfo, boolean isOldContext,
				BoogieASTNode boogieASTNode) {
			return m_Substitution.get(id);
		}
	}

	public class SubstitutionTranslatorBoogieVar implements IdentifierTranslator {
		private final Map<BoogieVar, Term> m_Substitution;

		public SubstitutionTranslatorBoogieVar(Map<BoogieVar, Term> substitution) {
			super();
			m_Substitution = substitution;
		}

		@Override
		public Term getSmtIdentifier(String id, DeclarationInformation declInfo, boolean isOldContext,
				BoogieASTNode boogieASTNode) {
			BoogieVar bv = m_Boogie2SmtSymbolTable.getBoogieVar(id, declInfo, isOldContext);
			if (bv == null) {
				return null;
			} else
				return m_Substitution.get(bv);
		}
	}

	/**
	 * Eliminate auxVars from input if possible. Let {x_1,...,x_n} be a subset
	 * of auxVars. Returns a term that is equivalent to ∃x_1,...,∃x_n input and
	 * remove {x_1,...,x_n} from auxVars. The set {x_1,...,x_n} is determined by
	 * NaiveDestructiveEqualityResolution.
	 * 
	 * Returns term that is equisatisfiable to input. If a x is free variable
	 * 
	 * @param input
	 * @param auxVars
	 *            set of free variables occurring in input
	 * @return
	 */
	private Term eliminateAuxVars(Term input, Set<TermVariable> auxVars) {
		XnfDer xnfDer = new XnfDer(m_Script, mServices);
		Term result = Util.and(m_Script, xnfDer.tryToEliminate(QuantifiedFormula.EXISTS, SmtUtils.getConjuncts(input), auxVars));
		return result;
	}

	public TranslationResult statementSequence(boolean simplify, String procId, Statement... statements) {
		initialize(procId);
		for (int i = statements.length - 1; i >= 0; i--) {
			Statement st = statements[i];
			if (st instanceof AssumeStatement) {
				addAssume((AssumeStatement) st);
			} else if (st instanceof AssignmentStatement) {
				addAssignment((AssignmentStatement) st);
			} else if (st instanceof HavocStatement) {
				addHavoc((HavocStatement) st);
			} else if (st instanceof CallStatement) {
				addSummary((CallStatement) st);
			} else {
				throw new IllegalArgumentException("Intenal Edge only contains"
						+ " Assume, Assignment or Havoc Statement");
			}

		}
		return getTransFormula(simplify, false);
	}

	/**
	 * Returns a TransFormula that describes the assignment of arguments to
	 * callees (local) input parameters. The (local) input parameters of the
	 * callee are the only outVars. For each inParameter we construct a new
	 * BoogieVar which is equivalent to the BoogieVars which were constructed
	 * while processing the callee.
	 */
	public TranslationResult inParamAssignment(CallStatement st) {
		String callee = st.getMethodName();
		initialize(callee);
		Procedure calleeImpl = m_BoogieDeclarations.getProcImplementation().get(callee);

		IdentifierTranslator[] its = getIdentifierTranslatorsIntraprocedural();
		MultiTermResult tlres = m_Expression2Term.translateToTerms(its, st.getArguments()); 
		m_AuxVars.addAll(tlres.getAuxiliaryVars());
		m_Overapproximations.putAll(tlres.getOverappoximations()); 
		Term[] argTerms = tlres.getTerms();
		
		m_OutVars.clear();

		DeclarationInformation declInfo = new DeclarationInformation(StorageClass.PROC_FUNC_INPARAM, callee);
		Term[] assignments = new Term[st.getArguments().length];
		int offset = 0;
		for (VarList varList : calleeImpl.getInParams()) {
			for (String var : varList.getIdentifiers()) {
				BoogieVar boogieVar = m_Boogie2SMT.getBoogie2SmtSymbolTable().getBoogieVar(var, declInfo, false);
				assert boogieVar != null;
				String suffix = "InParam";
				TermVariable tv = m_VariableManager.constructTermVariableWithSuffix(boogieVar, suffix);
				m_OutVars.put(boogieVar, tv);
				assignments[offset] = m_Script.term("=", tv, argTerms[offset]);
				offset++;
			}
		}
		assert (st.getArguments().length == offset);
		m_Assumes = Util.and(m_Script, assignments);
		return getTransFormula(false, true);
	}

	/**
	 * Returns a TransFormula that describes the assignment of (local) out
	 * parameters to variables that take the result. The variables on the left
	 * hand side of the call statement are the only outVars. For each
	 * outParameter and each left hand side of the call we construct a new
	 * BoogieVar which is equivalent to the BoogieVars of the corresponding
	 * procedures.
	 */
	public TranslationResult resultAssignment(CallStatement st, String caller) {
		initialize(caller);
		String callee = st.getMethodName();
		Procedure impl = m_BoogieDeclarations.getProcImplementation().get(callee);
		int offset = 0;
		DeclarationInformation declInfo = new DeclarationInformation(StorageClass.IMPLEMENTATION_OUTPARAM, callee);
		Term[] assignments = new Term[st.getLhs().length];
		for (VarList ourParamVarList : impl.getOutParams()) {
			for (String outParamId : ourParamVarList.getIdentifiers()) {
				BoogieVar outParamBv = m_Boogie2SmtSymbolTable.getBoogieVar(outParamId, declInfo, false);
				String suffix = "OutParam";
				TermVariable outParamTv = m_VariableManager.constructTermVariableWithSuffix(outParamBv, suffix);
				m_InVars.put(outParamBv, outParamTv);
				String callLhsId = st.getLhs()[offset].getIdentifier();
				DeclarationInformation callLhsDeclInfo = ((VariableLHS) st.getLhs()[offset])
						.getDeclarationInformation();
				BoogieVar callLhsBv = m_Boogie2SmtSymbolTable.getBoogieVar(callLhsId, callLhsDeclInfo, false);
				TermVariable callLhsTv = m_VariableManager.constructFreshTermVariable(callLhsBv);
				m_OutVars.put(callLhsBv, callLhsTv);
				assignments[offset] = m_Script.term("=", callLhsTv, outParamTv);
				offset++;
			}
		}
		assert (st.getLhs().length == offset);
		m_Assumes = Util.and(m_Script, assignments);
		return getTransFormula(false, true);
	}
	
	
	public class TranslationResult {
		private final TransFormula m_TransFormula;
		private final Map<String, ILocation> m_Overapproximations;
		public TranslationResult(TransFormula transFormula,
				Map<String, ILocation> overapproximations) {
			super();
			m_TransFormula = transFormula;
			m_Overapproximations = overapproximations;
		}
		public TransFormula getTransFormula() {
			return m_TransFormula;
		}
		public Map<String, ILocation> getOverapproximations() {
			return m_Overapproximations;
		}
		
	}

}
