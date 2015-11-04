/*
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

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.QuotedObject;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.model.IType;
import de.uni_freiburg.informatik.ultimate.model.boogie.BoogieNonOldVar;
import de.uni_freiburg.informatik.ultimate.model.boogie.BoogieOldVar;
import de.uni_freiburg.informatik.ultimate.model.boogie.BoogieVar;
import de.uni_freiburg.informatik.ultimate.model.boogie.DeclarationInformation;
import de.uni_freiburg.informatik.ultimate.model.boogie.DeclarationInformation.StorageClass;
import de.uni_freiburg.informatik.ultimate.model.boogie.LocalBoogieVar;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Attribute;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BoogieASTNode;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.ConstDeclaration;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.FunctionDeclaration;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.IntegerLiteral;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.NamedAttribute;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Procedure;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.StringLiteral;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VarList;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VariableDeclaration;

/**
 * Stores a mapping from Boogie identifiers to BoogieVars and a mapping from
 * TermVariables that are representatives of BoogieVars to these BoogieVars.
 * @author Matthias Heizmann
 *
 */
public class Boogie2SmtSymbolTable {
	/**
	 * Identifier of attribute that we use to state that
	 * <ul>
	 * <li> no function has to be declared, function is already defined in the 
	 * logic
	 * <li> given value has to be used in the translation.
	 * </ul>
	 * 
	 */
	private static final String s_BUILTINIDENTIFIER = "builtin";
	
	private static final String s_INDICESIDENTIFIER = "indices";
	
	private final BoogieDeclarations m_BoogieDeclarations;
	private final Script m_Script; 
	private final TypeSortTranslator m_TypeSortTranslator;
	private final Map<String, BoogieNonOldVar> m_Globals = 
			new HashMap<String, BoogieNonOldVar>();
	private final Map<String, BoogieVar> m_OldGlobals = 
			new HashMap<String, BoogieVar>();
	private final Map<String, Map<String, BoogieVar>> m_SpecificationInParam = 
			new HashMap<String, Map<String, BoogieVar>>();
	private final Map<String, Map<String, BoogieVar>> m_SpecificationOutParam = 
			new HashMap<String, Map<String, BoogieVar>>();
	private final Map<String, Map<String, BoogieVar>> m_ImplementationInParam = 
			new HashMap<String, Map<String, BoogieVar>>();
	private final Map<String, Map<String, BoogieVar>> m_ImplementationOutParam = 
			new HashMap<String, Map<String, BoogieVar>>();
	private final Map<String, Map<String, BoogieVar>> m_ImplementationLocals = 
			new HashMap<String, Map<String, BoogieVar>>();
	private final Map<String, BoogieConst> m_Constants = 
			new HashMap<String, BoogieConst>();
	
	private final Map<TermVariable,BoogieVar> m_SmtVar2BoogieVar = 
			new HashMap<TermVariable,BoogieVar>();
	private final Map<BoogieVar,DeclarationInformation> m_BoogieVar2DeclarationInformation = 
			new HashMap<BoogieVar,DeclarationInformation>();
	private final Map<BoogieVar, BoogieASTNode> m_BoogieVar2AstNode = 
			new HashMap<BoogieVar, BoogieASTNode>();
	private final Map<ApplicationTerm, BoogieConst> m_SmtConst2BoogieConst = 
			new HashMap<ApplicationTerm,BoogieConst>();
	
	final Map<String,String> m_BoogieFunction2SmtFunction = 
			new HashMap<String,String>();
	final Map<String,String> m_SmtFunction2BoogieFunction = 
			new HashMap<String,String>();
	final Map<String, Map<String, Expression[]>> m_BoogieFunction2Attributes =
			new HashMap<String, Map<String,Expression[]>>();

	
	
	public Boogie2SmtSymbolTable(BoogieDeclarations boogieDeclarations,
			Script script,
			TypeSortTranslator typeSortTranslator) {
		super();
		m_Script = script;
		m_TypeSortTranslator = typeSortTranslator;
		m_BoogieDeclarations = boogieDeclarations;
		
		m_Script.echo(new QuotedObject("Start declaration of constants"));
		for (ConstDeclaration decl : m_BoogieDeclarations.getConstDeclarations()) {
			declareConstants(decl);
		}
		m_Script.echo(new QuotedObject("Finished declaration of constants"));
		
		m_Script.echo(new QuotedObject("Start declaration of functions"));
		for (FunctionDeclaration decl : m_BoogieDeclarations.getFunctionDeclarations()) {
			declareFunction(decl);
		}
		m_Script.echo(new QuotedObject("Finished declaration of functions"));
		
		m_Script.echo(new QuotedObject("Start declaration of global variables"));
		for (VariableDeclaration decl : m_BoogieDeclarations.getGlobalVarDeclarations()) {
			declareGlobalVariables(decl);
		}
		m_Script.echo(new QuotedObject("Finished declaration global variables"));
		
		m_Script.echo(new QuotedObject("Start declaration of local variables"));
		for (String procId : m_BoogieDeclarations.getProcSpecification().keySet()) {
			Procedure procSpec = m_BoogieDeclarations.getProcSpecification().get(procId);
			Procedure procImpl = m_BoogieDeclarations.getProcImplementation().get(procId);
			if (procImpl == null) {
				declareSpec(procSpec);
			} else {
				declareSpecImpl(procSpec, procImpl);
			}
		}
		m_Script.echo(new QuotedObject("Finished declaration local variables"));
	}

	private void putNew(String procId, String varId, BoogieVar bv, Map<String, Map<String, BoogieVar>> map) {
		Map<String, BoogieVar> varId2BoogieVar = map.get(procId);
		if (varId2BoogieVar == null) {
			varId2BoogieVar = new HashMap<String, BoogieVar>();
			map.put(procId, varId2BoogieVar);
		}
		BoogieVar previousValue = varId2BoogieVar.put(varId, bv);
		assert previousValue == null : "variable already contained";
	}
	
	private <VALUE> void  putNew(String varId, VALUE value, Map<String, VALUE> map) {
		VALUE previousValue = map.put(varId, value);
		assert previousValue == null : "variable already contained";
	}
	
	private BoogieVar get(String varId, String procId, Map<String, Map<String, BoogieVar>> map) {
		Map<String, BoogieVar> varId2BoogieVar = map.get(procId);
		if (varId2BoogieVar == null) {
			return null;
		} else {
			return varId2BoogieVar.get(varId);
		}
	}
	
	public static boolean isSpecification(Procedure spec) {
		return spec.getSpecification() != null;
	}
	
	public static boolean isImplementation(Procedure impl) {
		return impl.getBody() != null;
	}
	
	public Script getScript() {
		return m_Script;
	}

	public BoogieVar getBoogieVar(String varId, DeclarationInformation declarationInformation, boolean inOldContext) {
		final BoogieVar result;
		StorageClass storageClass = declarationInformation.getStorageClass();
		String procedure = declarationInformation.getProcedure();
		switch (storageClass) {
		case GLOBAL:
			if (inOldContext) {
				result = m_OldGlobals.get(varId);
			} else {
				result = m_Globals.get(varId);
			}
			break;
		case PROC_FUNC_INPARAM:
//			result = get(varId, procedure, m_SpecificationInParam);
//			break;
		case IMPLEMENTATION_INPARAM:
			result = get(varId, procedure, m_ImplementationInParam);
			break;
		case PROC_FUNC_OUTPARAM:
//			result = get(varId, procedure, m_SpecificationOutParam);
//			break;
		case IMPLEMENTATION_OUTPARAM:
			result = get(varId, procedure, m_ImplementationOutParam);
			break;
		case LOCAL:
			result = get(varId, procedure, m_ImplementationLocals);
			break;
		case IMPLEMENTATION:
		case PROC_FUNC:
		case QUANTIFIED:
		default:
			throw new AssertionError("inappropriate decl info");
		}
		return result;
	}
	
	public BoogieVar getBoogieVar(TermVariable tv) {
		return m_SmtVar2BoogieVar.get(tv);
	}
	
	public DeclarationInformation getDeclarationInformation(BoogieVar bv) {
		return m_BoogieVar2DeclarationInformation.get(bv);
	}
	
	public BoogieASTNode getAstNode(BoogieVar bv) {
		return m_BoogieVar2AstNode.get(bv);
	}
	
	private void declareConstants(ConstDeclaration constdecl) {
		VarList varlist = constdecl.getVarList();
		Sort[] paramTypes = new Sort[0];
		IType iType = varlist.getType().getBoogieType();
		Sort sort = m_TypeSortTranslator.getSort(iType, varlist);
		for (String constId : varlist.getIdentifiers()) {
			m_Script.declareFun(constId, paramTypes, sort);
			ApplicationTerm constant = (ApplicationTerm) m_Script.term(constId);
			BoogieConst boogieConst = new BoogieConst(constId, iType, constant);
			BoogieConst previousValue = m_Constants.put(constId, boogieConst);
			assert previousValue == null : "constant already contained";
			m_SmtConst2BoogieConst.put(constant, boogieConst);
		}
	}
	
	public BoogieConst getBoogieConst(String constId) {
		return m_Constants.get(constId);
	}
	
	public BoogieConst getBoogieConst(ApplicationTerm smtConstant) {
		return m_SmtConst2BoogieConst.get(smtConstant);
	}
	
	public Map<String, Expression[]> getAttributes(String boogieFunctionId) {
		return Collections.unmodifiableMap(m_BoogieFunction2Attributes.get(boogieFunctionId));
	}
	
	private void declareFunction(FunctionDeclaration funcdecl) {
		Map<String, Expression[]> attributes = extractAttributes(funcdecl);
		String id = funcdecl.getIdentifier();
		m_BoogieFunction2Attributes.put(id, attributes);
		String attributeDefinedIdentifier = checkForAttributeDefinedIdentifier(attributes, s_BUILTINIDENTIFIER);
		String smtID;
		if (attributeDefinedIdentifier == null) {
			 smtID = Boogie2SMT.quoteId(id);
		} else {
			smtID = attributeDefinedIdentifier;
		}
		int numParams = 0;
		for (VarList vl : funcdecl.getInParams()) {
			int ids = vl.getIdentifiers().length;
			numParams += ids == 0 ? 1 : ids;
		}

		Sort[] paramSorts = new Sort[numParams];
		int paramNr = 0;
		for (VarList vl : funcdecl.getInParams()) {
			int ids = vl.getIdentifiers().length;
			if (ids == 0) {
				ids = 1;
			}
			IType paramType = vl.getType().getBoogieType();
			Sort paramSort = m_TypeSortTranslator.getSort(paramType, funcdecl);
			for (int i = 0; i < ids; i++) {
				paramSorts[paramNr++] = paramSort;
			}
		}
		IType resultType = funcdecl.getOutParam().getType().getBoogieType();
		Sort resultSort = m_TypeSortTranslator.getSort(resultType, funcdecl);
		if (attributeDefinedIdentifier == null) {
			// no builtin function, we have to declare it
			m_Script.declareFun(smtID, paramSorts, resultSort);
		}
		m_BoogieFunction2SmtFunction.put(id, smtID);
		m_SmtFunction2BoogieFunction.put(smtID, id);
	}

	
	/**
	 * Returns the single StringLiteral value of the NamedAttribute with name n.
	 * Throws an IllegalArgumentException if there is a NamedAttribute with
	 * name whose value is not a single StringLiteral.
	 * Returns null if there is no NamedAttribute with name n.
	 */
	public static String checkForAttributeDefinedIdentifier(
			Map<String, Expression[]> attributes, String n) {
		Expression[] values = attributes.get(n);
		if (values == null) {
			// no such name
			return null;
		} else {
			if (values.length == 1 && values[0] instanceof StringLiteral) {
				StringLiteral sl = (StringLiteral) values[0];
				return sl.getValue();
			} else {
				throw new IllegalArgumentException("no single value attribute");
			}
		}
	}
	
	/**
	 * Checks if there is an annotation with the name {@link #s_INDICESIDENTIFIER}
	 * According to our convention this attribute defines the indices for the 
	 * corresponding SMT function. Returns the array of indices if there is an
	 * attribute with this name and null otherwise.
	 */
	public static BigInteger[] checkForIndices(Map<String, Expression[]> attributes) {
		Expression[] values = attributes.get(s_INDICESIDENTIFIER);
		if (values == null) {
			// no such name
			return null;
		} else {
			BigInteger[] result = new BigInteger[values.length];
			for (int i=0; i<values.length; i++) {
				if (values[i] instanceof IntegerLiteral) {
					result[i] = new BigInteger(((IntegerLiteral) values[i]).getValue());
				} else {
					throw new IllegalArgumentException("no single value attribute");
				}
			}
			return result;
		}
	}
	
	private Map<String, Expression[]> extractAttributes(FunctionDeclaration funcdecl) {
		Map<String, Expression[]> result = new HashMap<String, Expression[]>();
		for (Attribute attr : funcdecl.getAttributes()) {
			if (attr instanceof NamedAttribute) {
				NamedAttribute nattr = (NamedAttribute) attr;
				result.put(nattr.getName(), ((NamedAttribute) attr).getValues());
			}
		}
		return result;
	}
	
	public Map<String, String> getSmtFunction2BoogieFunction() {
		return Collections.unmodifiableMap(m_SmtFunction2BoogieFunction);
	}
	
	public Map<String, String> getBoogieFunction2SmtFunction() {
		return Collections.unmodifiableMap(m_BoogieFunction2SmtFunction);
	}
	
	
	private void declareGlobalVariables(VariableDeclaration vardecl) {
		for (VarList vl : vardecl.getVariables()) {
			for (String id : vl.getIdentifiers()) {
				IType type = vl.getType().getBoogieType();
				BoogieNonOldVar global = constructGlobalBoogieVar(
						id, type, vl);
				putNew(id, global, m_Globals);
				BoogieVar oldGlobal = global.getOldVar();
				putNew(id, oldGlobal, m_OldGlobals);
			}
		}
	}
	
	/**
	 * Return global variables;
	 */
	public Map<String, BoogieNonOldVar> getGlobals() {
		return Collections.unmodifiableMap(m_Globals);
	}
	
	/**
	 * Return global constants;
	 */
	public Map<String, BoogieConst> getConsts() {
		return Collections.unmodifiableMap(m_Constants);
	}
	
	private void declareSpecImpl(Procedure spec, Procedure impl) {
		String procId = spec.getIdentifier();
		assert procId.equals(impl.getIdentifier());
		DeclarationInformation declInfoInParam;
		DeclarationInformation declInfoOutParam;
		if (spec == impl) {
			// implementation is given in procedure declaration, in this case
			// we consider all in/out-params as procedure variables
			declInfoInParam = new DeclarationInformation(StorageClass.PROC_FUNC_INPARAM, procId);
			declInfoOutParam = new DeclarationInformation(StorageClass.PROC_FUNC_OUTPARAM, procId);
		} else {
			assert (isSpecAndImpl(spec, impl));
			// implementation is given in a separate declaration, in this case
			// we consider all in/out-params as implementation variables
			declInfoInParam = new DeclarationInformation(StorageClass.IMPLEMENTATION_INPARAM, procId);
			declInfoOutParam = new DeclarationInformation(StorageClass.IMPLEMENTATION_OUTPARAM, procId);
			
		}
		declareParams(procId, spec.getInParams(), impl.getInParams(), 
				m_SpecificationInParam, m_ImplementationInParam, 
				declInfoInParam);
		declareParams(procId, spec.getOutParams(), impl.getOutParams(), 
				m_SpecificationOutParam, m_ImplementationOutParam, 
				declInfoOutParam);
		declareLocals(impl);
	}
	
	/**
	 * Returns true if spec contains only a specification or impl contains only
	 * an implementation.
	 */
	private boolean isSpecAndImpl(Procedure spec, Procedure impl) {
		return isSpecification(spec) && !isImplementation(spec) && 
				isImplementation(impl) && !isSpecification(impl);
		
	}
	
	public void declareSpec(Procedure spec) {
		assert isSpecification(spec) : "no specification";
		assert !isImplementation(spec) : "is implementation";
		String procId = spec.getIdentifier();
		declareParams(procId, spec.getInParams(), m_SpecificationInParam,
				new DeclarationInformation(StorageClass.PROC_FUNC_INPARAM, procId));
		declareParams(procId, spec.getOutParams(), m_SpecificationOutParam,
				new DeclarationInformation(StorageClass.PROC_FUNC_OUTPARAM, procId));
	}
	
	
	private void declareParams(String procId, VarList[] specVl, VarList[] implVl, 
			Map<String, Map<String, BoogieVar>> specMap, 
			Map<String, Map<String, BoogieVar>> implMap,
			DeclarationInformation declarationInformation) {
		if (specVl.length != implVl.length) {
			throw new IllegalArgumentException(
					"specification and implementation have different param length");
		}
		for (int i=0; i<specVl.length; i++) {
			IType specType = specVl[i].getType().getBoogieType();
			IType implType = implVl[i].getType().getBoogieType();
			if (!specType.equals(implType)) {
				throw new IllegalArgumentException(
						"specification and implementation have different types");
			}
			String[] specIds = specVl[i].getIdentifiers();
			String[] implIds = implVl[i].getIdentifiers();
			if (specIds.length != implIds.length) {
				throw new IllegalArgumentException(
						"specification and implementation have different param length");
			}
			for (int j=0; j<specIds.length; j++) {
				BoogieVar bv = constructLocalBoogieVar(implIds[j], procId, 
						implType, implVl[i], declarationInformation);
				putNew(procId, implIds[j], bv, implMap);
				putNew(procId, specIds[j], bv, specMap);
			}
		}
	}
	
	
	/**
	 * Declare in or our parameters of a specification. 
	 * @param procId name of procedure
	 * @param vl Varlist defining the parameters
	 * @param specMap map for the specification
	 * @param declarationInformation StorageClass of the constructed BoogieVar
	 */
	private void declareParams(String procId, VarList[] vl, 
			Map<String, Map<String, BoogieVar>> specMap,
			DeclarationInformation declarationInformation) {
		for (int i=0; i<vl.length; i++) {
			IType type = vl[i].getType().getBoogieType();
			String[] ids = vl[i].getIdentifiers();
			for (int j=0; j<ids.length; j++) {
				BoogieVar bv = constructLocalBoogieVar(ids[j], procId,
						type, vl[i], declarationInformation);
				putNew(procId, ids[j], bv, specMap);
			}
		}
	}
			
			

	public void declareLocals(Procedure proc) {
		if (proc.getBody() != null) {
			DeclarationInformation declarationInformation = 
					new DeclarationInformation(StorageClass.LOCAL, proc.getIdentifier());
			for (VariableDeclaration vdecl : proc.getBody().getLocalVars()) {
				for (VarList vl : vdecl.getVariables()) {
					for (String id : vl.getIdentifiers()) {
						IType type = vl.getType().getBoogieType();
						BoogieVar bv = constructLocalBoogieVar(id, proc.getIdentifier(),
								type, vl, declarationInformation);
						putNew(proc.getIdentifier(), id, bv, m_ImplementationLocals);
					}
				}
			}
		}
	}
	
	
	/**
	 * Construct BoogieVar and store it. Expects that no BoogieVar with the same
	 * identifier has already been constructed.
	 * 
	 * @param identifier
	 * @param procedure
	 * @param iType
	 * @param isOldvar
	 * @param boogieASTNode
	 *            BoogieASTNode for which errors (e.g., unsupported syntax) are
	 *            reported
	 * @param declarationInformation 
	 */
	private LocalBoogieVar constructLocalBoogieVar(String identifier, 
			String procedure, IType iType, VarList varList, 
			DeclarationInformation declarationInformation) {
		Sort sort = m_TypeSortTranslator.getSort(iType, varList);

		String name = constructBoogieVarName(identifier, procedure,
				false, false);

		TermVariable termVariable = m_Script.variable(name, sort);

		ApplicationTerm defaultConstant = constructDefaultConstant(sort, name);
		ApplicationTerm primedConstant = constructPrimedConstant(sort, name);

		LocalBoogieVar bv = new LocalBoogieVar(identifier, procedure, iType,
				termVariable, defaultConstant, primedConstant);
		
		m_SmtVar2BoogieVar.put(termVariable, bv);
		m_BoogieVar2DeclarationInformation.put(bv, declarationInformation);
		m_BoogieVar2AstNode.put(bv, varList);
		return bv;
	}
	
	/**
	 * Construct global BoogieVar and the corresponding oldVar and store both. 
	 * Expects that no local BoogieVarwith the same identifier has already been
	 * constructed.
	 * @param boogieASTNode
	 *            BoogieASTNode for which errors (e.g., unsupported syntax) are
	 *            reported
	 */
	private BoogieNonOldVar constructGlobalBoogieVar(String identifier,
			IType iType, VarList varlist) {
		Sort sort = m_TypeSortTranslator.getSort(iType, varlist);
		String procedure = null;
		DeclarationInformation declarationInformation = 
				new DeclarationInformation(StorageClass.GLOBAL, null);
		
		BoogieOldVar oldVar;
		{
			boolean isOldVar = true;
			String name = constructBoogieVarName(identifier, procedure,
					true, isOldVar);
			TermVariable termVariable = m_Script.variable(name, sort);
			ApplicationTerm defaultConstant = constructDefaultConstant(sort, name);
			ApplicationTerm primedConstant = constructPrimedConstant(sort, name);

			oldVar = new BoogieOldVar(identifier, iType,
					isOldVar, termVariable, defaultConstant, primedConstant);
			m_SmtVar2BoogieVar.put(termVariable, oldVar);
			m_BoogieVar2DeclarationInformation.put(oldVar, declarationInformation);
			m_BoogieVar2AstNode.put(oldVar, varlist);
		}
		BoogieNonOldVar nonOldVar;
		{
			boolean isOldVar = false;
			String name = constructBoogieVarName(identifier, procedure,
					true, isOldVar);
			TermVariable termVariable = m_Script.variable(name, sort);
			ApplicationTerm defaultConstant = constructDefaultConstant(sort, name);
			ApplicationTerm primedConstant = constructPrimedConstant(sort, name);

			nonOldVar = new BoogieNonOldVar(identifier, iType,
					termVariable, defaultConstant, primedConstant, oldVar);
			m_SmtVar2BoogieVar.put(termVariable, nonOldVar);
			m_BoogieVar2DeclarationInformation.put(nonOldVar, declarationInformation);
			m_BoogieVar2AstNode.put(nonOldVar, varlist);
		}
		oldVar.setNonOldVar(nonOldVar);
		return nonOldVar;
	}
	

	private ApplicationTerm constructPrimedConstant(Sort sort, String name) {
		ApplicationTerm primedConstant;
		{
			String primedConstantName = "c_" + name + "_primed";
			m_Script.declareFun(primedConstantName, new Sort[0], sort);
			primedConstant = (ApplicationTerm) m_Script.term(primedConstantName);
		}
		return primedConstant;
	}

	private ApplicationTerm constructDefaultConstant(Sort sort, String name) {
		ApplicationTerm defaultConstant;
		{
			String defaultConstantName = "c_" + name;
			m_Script.declareFun(defaultConstantName, new Sort[0], sort);
			defaultConstant = (ApplicationTerm) m_Script.term(defaultConstantName);
		}
		return defaultConstant;
	}

	private String constructBoogieVarName(String identifier, String procedure,
			boolean isGlobal, boolean isOldvar) {
		String name;
		if (isGlobal) {
			assert procedure == null;
			if (isOldvar) {
				name = "old(" + identifier + ")";
			} else {
				name = identifier;
			}
		} else {
			assert (!isOldvar) : "only global vars can be oldvars";
			name = procedure + "_" + identifier;
		}
		return name;
	}
	
	BoogieNonOldVar constructAuxiliaryGlobalBoogieVar(String identifier, String procedure,
			IType iType, VarList varList) {
		BoogieNonOldVar bv = constructGlobalBoogieVar(identifier, iType, varList);
		m_Globals.put(identifier, bv);
		m_OldGlobals.put(identifier, bv.getOldVar());
		return bv;
	}
	

}
