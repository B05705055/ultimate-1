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

import de.uni_freiburg.informatik.ultimate.boogie.DeclarationInformation;
import de.uni_freiburg.informatik.ultimate.boogie.DeclarationInformation.StorageClass;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Attribute;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BoogieASTNode;
import de.uni_freiburg.informatik.ultimate.boogie.ast.ConstDeclaration;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Declaration;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.FunctionDeclaration;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IntegerLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.NamedAttribute;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Procedure;
import de.uni_freiburg.informatik.ultimate.boogie.ast.StringLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.VarList;
import de.uni_freiburg.informatik.ultimate.boogie.ast.VariableDeclaration;
import de.uni_freiburg.informatik.ultimate.core.model.models.IType;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.QuotedObject;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramNonOldVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;

/**
 * Stores a mapping from Boogie identifiers to BoogieVars and a mapping from TermVariables that are representatives of
 * BoogieVars to these BoogieVars.
 * 
 * @author Matthias Heizmann
 *
 */
public class Boogie2SmtSymbolTable {
	/**
	 * Identifier of attribute that we use to state that
	 * <ul>
	 * <li>no function has to be declared, function is already defined in the logic
	 * <li>given value has to be used in the translation.
	 * </ul>
	 * 
	 */
	static final String s_BUILTINIDENTIFIER = "builtin";

	private static final String s_INDICESIDENTIFIER = "indices";

	private final BoogieDeclarations mBoogieDeclarations;
	private final Script mScript;
	private final TypeSortTranslator mTypeSortTranslator;
	private final Map<String, BoogieNonOldVar> mGlobals = new HashMap<String, BoogieNonOldVar>();
	private final Map<String, BoogieOldVar> mOldGlobals = new HashMap<String, BoogieOldVar>();
	private final Map<String, Map<String, BoogieVar>> mSpecificationInParam =
			new HashMap<String, Map<String, BoogieVar>>();
	private final Map<String, Map<String, BoogieVar>> mSpecificationOutParam =
			new HashMap<String, Map<String, BoogieVar>>();
	private final Map<String, Map<String, BoogieVar>> mImplementationInParam =
			new HashMap<String, Map<String, BoogieVar>>();
	private final Map<String, Map<String, BoogieVar>> mImplementationOutParam =
			new HashMap<String, Map<String, BoogieVar>>();
	private final Map<String, Map<String, LocalBoogieVar>> mImplementationLocals =
			new HashMap<String, Map<String, LocalBoogieVar>>();
	private final Map<String, BoogieConst> mConstants = new HashMap<String, BoogieConst>();

	private final Map<TermVariable, IProgramVar> mSmtVar2BoogieVar = new HashMap<TermVariable, IProgramVar>();
	private final Map<IProgramVar, DeclarationInformation> mBoogieVar2DeclarationInformation =
			new HashMap<IProgramVar, DeclarationInformation>();
	private final Map<IProgramVar, BoogieASTNode> mBoogieVar2AstNode = new HashMap<IProgramVar, BoogieASTNode>();
	private final Map<ApplicationTerm, BoogieConst> mSmtConst2BoogieConst = new HashMap<ApplicationTerm, BoogieConst>();

	final Map<String, String> mBoogieFunction2SmtFunction = new HashMap<String, String>();
	final Map<String, String> mSmtFunction2BoogieFunction = new HashMap<String, String>();
	final Map<String, Map<String, Expression[]>> mBoogieFunction2Attributes =
			new HashMap<String, Map<String, Expression[]>>();

	public Boogie2SmtSymbolTable(final BoogieDeclarations boogieDeclarations, final Script script,
			final TypeSortTranslator typeSortTranslator) {
		super();
		mScript = script;
		mTypeSortTranslator = typeSortTranslator;
		mBoogieDeclarations = boogieDeclarations;

		mScript.echo(new QuotedObject("Start declaration of constants"));
		for (final ConstDeclaration decl : mBoogieDeclarations.getConstDeclarations()) {
			declareConstants(decl);
		}
		mScript.echo(new QuotedObject("Finished declaration of constants"));

		mScript.echo(new QuotedObject("Start declaration of functions"));
		for (final FunctionDeclaration decl : mBoogieDeclarations.getFunctionDeclarations()) {
			declareFunction(decl);
		}
		mScript.echo(new QuotedObject("Finished declaration of functions"));

		mScript.echo(new QuotedObject("Start declaration of global variables"));
		for (final VariableDeclaration decl : mBoogieDeclarations.getGlobalVarDeclarations()) {
			declareGlobalVariables(decl);
		}
		mScript.echo(new QuotedObject("Finished declaration global variables"));

		mScript.echo(new QuotedObject("Start declaration of local variables"));
		for (final String procId : mBoogieDeclarations.getProcSpecification().keySet()) {
			final Procedure procSpec = mBoogieDeclarations.getProcSpecification().get(procId);
			final Procedure procImpl = mBoogieDeclarations.getProcImplementation().get(procId);
			if (procImpl == null) {
				declareSpec(procSpec);
			} else {
				declareSpecImpl(procSpec, procImpl);
			}
		}
		mScript.echo(new QuotedObject("Finished declaration local variables"));
	}

	private <T extends BoogieVar> void putNew(final String procId, final String varId, final T bv, final Map<String, Map<String, T>> map) {
		Map<String, T> varId2BoogieVar = map.get(procId);
		if (varId2BoogieVar == null) {
			varId2BoogieVar = new HashMap<String, T>();
			map.put(procId, varId2BoogieVar);
		}
		final BoogieVar previousValue = varId2BoogieVar.put(varId, bv);
		assert previousValue == null : "variable already contained";
	}

	private <VALUE> void putNew(final String varId, final VALUE value, final Map<String, VALUE> map) {
		final VALUE previousValue = map.put(varId, value);
		assert previousValue == null : "variable already contained";
	}

	private <T extends BoogieVar> T get(final String varId, final String procId, final Map<String, Map<String, T>> map) {
		final Map<String, T> varId2BoogieVar = map.get(procId);
		if (varId2BoogieVar == null) {
			return null;
		} else {
			return varId2BoogieVar.get(varId);
		}
	}

	public static boolean isSpecification(final Procedure spec) {
		return spec.getSpecification() != null;
	}

	public static boolean isImplementation(final Procedure impl) {
		return impl.getBody() != null;
	}

	public Script getScript() {
		return mScript;
	}

	public BoogieVar getBoogieVar(final String varId, final DeclarationInformation declarationInformation,
			final boolean inOldContext) {
		final BoogieVar result;
		final StorageClass storageClass = declarationInformation.getStorageClass();
		final String procedure = declarationInformation.getProcedure();
		switch (storageClass) {
		case GLOBAL:
			if (inOldContext) {
				result = mOldGlobals.get(varId);
			} else {
				result = mGlobals.get(varId);
			}
			break;
		case PROC_FUNC_INPARAM:
		case IMPLEMENTATION_INPARAM:
			result = get(varId, procedure, mImplementationInParam);
			break;
		case PROC_FUNC_OUTPARAM:
		case IMPLEMENTATION_OUTPARAM:
			result = get(varId, procedure, mImplementationOutParam);
			break;
		case LOCAL:
			result = get(varId, procedure, mImplementationLocals);
			break;
		case IMPLEMENTATION:
		case PROC_FUNC:
		case QUANTIFIED:
		default:
			throw new AssertionError("inappropriate decl info");
		}
		return result;
	}

	/**
	 * Get BoogieVar for in our outparams.
	 * 
	 * @param varId
	 *            The id of the param.
	 * @param procedure
	 *            The procedure.
	 * @param isInParam
	 *            true iff its an inparam, false if its an outparam.
	 * @return The BoogieVar.
	 */
	public BoogieVar getBoogieVar(final String varId, final String procedure, final boolean isInParam) {
		if (isInParam) {
			return get(varId, procedure, mImplementationInParam);
		} else {
			return get(varId, procedure, mImplementationOutParam);
		}
	}

	public IProgramVar getBoogieVar(final TermVariable tv) {
		return mSmtVar2BoogieVar.get(tv);
	}

	public DeclarationInformation getDeclarationInformation(final IProgramVar bv) {
		return mBoogieVar2DeclarationInformation.get(bv);
	}

	public BoogieASTNode getAstNode(final IProgramVar bv) {
		return mBoogieVar2AstNode.get(bv);
	}

	private void declareConstants(final ConstDeclaration constdecl) {
		final VarList varlist = constdecl.getVarList();
		final Sort[] paramTypes = new Sort[0];
		final IType iType = varlist.getType().getBoogieType();
		final Sort sort = mTypeSortTranslator.getSort(iType, varlist);

		final Map<String, Expression[]> attributes = extractAttributes(constdecl);
		if (attributes != null) {
			final String attributeDefinedIdentifier =
					checkForAttributeDefinedIdentifier(attributes, s_BUILTINIDENTIFIER);
			if (attributeDefinedIdentifier != null) {
				final BigInteger[] indices = Boogie2SmtSymbolTable.checkForIndices(attributes);
				if (varlist.getIdentifiers().length > 1) {
					throw new IllegalArgumentException(
							"if builtin identifier is " + "used we support only one constant per const declaration");
				}
				final String constId = varlist.getIdentifiers()[0];
				final ApplicationTerm constant =
						(ApplicationTerm) mScript.term(attributeDefinedIdentifier, indices, null);
				final BoogieConst boogieConst = new BoogieConst(constId, iType, constant);
				final BoogieConst previousValue = mConstants.put(constId, boogieConst);
				assert previousValue == null : "constant already contained";
				mSmtConst2BoogieConst.put(constant, boogieConst);
				return;
			}
		}
		for (final String constId : varlist.getIdentifiers()) {
			mScript.declareFun(constId, paramTypes, sort);
			final ApplicationTerm constant = (ApplicationTerm) mScript.term(constId);
			final BoogieConst boogieConst = new BoogieConst(constId, iType, constant);
			final BoogieConst previousValue = mConstants.put(constId, boogieConst);
			assert previousValue == null : "constant already contained";
			mSmtConst2BoogieConst.put(constant, boogieConst);
		}
	}

	public BoogieConst getBoogieConst(final String constId) {
		return mConstants.get(constId);
	}

	public BoogieConst getBoogieConst(final ApplicationTerm smtConstant) {
		return mSmtConst2BoogieConst.get(smtConstant);
	}

	public Map<String, Expression[]> getAttributes(final String boogieFunctionId) {
		return Collections.unmodifiableMap(mBoogieFunction2Attributes.get(boogieFunctionId));
	}

	private void declareFunction(final FunctionDeclaration funcdecl) {
		final Map<String, Expression[]> attributes = extractAttributes(funcdecl);
		final String id = funcdecl.getIdentifier();
		mBoogieFunction2Attributes.put(id, attributes);
		final String attributeDefinedIdentifier = checkForAttributeDefinedIdentifier(attributes, s_BUILTINIDENTIFIER);
		String smtID;
		if (attributeDefinedIdentifier == null) {
			smtID = Boogie2SMT.quoteId(id);
		} else {
			smtID = attributeDefinedIdentifier;
		}
		int numParams = 0;
		for (final VarList vl : funcdecl.getInParams()) {
			final int ids = vl.getIdentifiers().length;
			numParams += ids == 0 ? 1 : ids;
		}

		final Sort[] paramSorts = new Sort[numParams];
		int paramNr = 0;
		for (final VarList vl : funcdecl.getInParams()) {
			int ids = vl.getIdentifiers().length;
			if (ids == 0) {
				ids = 1;
			}
			final IType paramType = vl.getType().getBoogieType();
			final Sort paramSort = mTypeSortTranslator.getSort(paramType, funcdecl);
			for (int i = 0; i < ids; i++) {
				paramSorts[paramNr++] = paramSort;
			}
		}
		final IType resultType = funcdecl.getOutParam().getType().getBoogieType();
		final Sort resultSort = mTypeSortTranslator.getSort(resultType, funcdecl);
		if (attributeDefinedIdentifier == null) {
			// no builtin function, we have to declare it
			mScript.declareFun(smtID, paramSorts, resultSort);
		}
		mBoogieFunction2SmtFunction.put(id, smtID);
		mSmtFunction2BoogieFunction.put(smtID, id);
	}

	/**
	 * Returns the single StringLiteral value of the NamedAttribute with name n. Throws an IllegalArgumentException if
	 * there is a NamedAttribute with name whose value is not a single StringLiteral. Returns null if there is no
	 * NamedAttribute with name n.
	 */
	public static String checkForAttributeDefinedIdentifier(final Map<String, Expression[]> attributes, final String n) {
		final Expression[] values = attributes.get(n);
		if (values == null) {
			// no such name
			return null;
		} else {
			if (values.length == 1 && values[0] instanceof StringLiteral) {
				final StringLiteral sl = (StringLiteral) values[0];
				return sl.getValue();
			} else {
				throw new IllegalArgumentException("no single value attribute");
			}
		}
	}

	/**
	 * Checks if there is an annotation with the name {@link #s_INDICESIDENTIFIER} According to our convention this
	 * attribute defines the indices for the corresponding SMT function. Returns the array of indices if there is an
	 * attribute with this name and null otherwise.
	 */
	public static BigInteger[] checkForIndices(final Map<String, Expression[]> attributes) {
		final Expression[] values = attributes.get(s_INDICESIDENTIFIER);
		if (values == null) {
			// no such name
			return null;
		} else {
			final BigInteger[] result = new BigInteger[values.length];
			for (int i = 0; i < values.length; i++) {
				if (values[i] instanceof IntegerLiteral) {
					result[i] = new BigInteger(((IntegerLiteral) values[i]).getValue());
				} else {
					throw new IllegalArgumentException("no single value attribute");
				}
			}
			return result;
		}
	}

	public static Map<String, Expression[]> extractAttributes(final Declaration decl) {
		final Map<String, Expression[]> result = new HashMap<String, Expression[]>();
		for (final Attribute attr : decl.getAttributes()) {
			if (attr instanceof NamedAttribute) {
				final NamedAttribute nattr = (NamedAttribute) attr;
				result.put(nattr.getName(), ((NamedAttribute) attr).getValues());
			}
		}
		return result;
	}

	public Map<String, String> getSmtFunction2BoogieFunction() {
		return Collections.unmodifiableMap(mSmtFunction2BoogieFunction);
	}

	public Map<String, String> getBoogieFunction2SmtFunction() {
		return Collections.unmodifiableMap(mBoogieFunction2SmtFunction);
	}

	private void declareGlobalVariables(final VariableDeclaration vardecl) {
		for (final VarList vl : vardecl.getVariables()) {
			for (final String id : vl.getIdentifiers()) {
				final IType type = vl.getType().getBoogieType();
				final BoogieNonOldVar global = constructGlobalBoogieVar(id, type, vl);
				putNew(id, global, mGlobals);
				final BoogieOldVar oldGlobal = global.getOldVar();
				putNew(id, oldGlobal, mOldGlobals);
			}
		}
	}

	/**
	 * Return global variables;
	 * @return Map that assigns to each variable identifier the 
	 * non-old global variable
	 */
	public Map<String, IProgramNonOldVar> getGlobals() {
		return Collections.unmodifiableMap(mGlobals);
	}
	
	/**
	 * Return all local variables, input parameters and output parameters 
	 * for a given procedure.
	 */
	public Map<String, LocalBoogieVar> getLocals(final String proc) {
		return null;
	}

	/**
	 * Return global constants;
	 */
	public Map<String, BoogieConst> getConsts() {
		return Collections.unmodifiableMap(mConstants);
	}

	private void declareSpecImpl(final Procedure spec, final Procedure impl) {
		final String procId = spec.getIdentifier();
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
		declareParams(procId, spec.getInParams(), impl.getInParams(), mSpecificationInParam, mImplementationInParam,
				declInfoInParam);
		declareParams(procId, spec.getOutParams(), impl.getOutParams(), mSpecificationOutParam, mImplementationOutParam,
				declInfoOutParam);
		declareLocals(impl);
	}

	/**
	 * Returns true if spec contains only a specification or impl contains only an implementation.
	 */
	private boolean isSpecAndImpl(final Procedure spec, final Procedure impl) {
		return isSpecification(spec) && !isImplementation(spec) && isImplementation(impl) && !isSpecification(impl);

	}

	public void declareSpec(final Procedure spec) {
		assert isSpecification(spec) : "no specification";
		assert !isImplementation(spec) : "is implementation";
		final String procId = spec.getIdentifier();
		declareParams(procId, spec.getInParams(), mSpecificationInParam,
				new DeclarationInformation(StorageClass.PROC_FUNC_INPARAM, procId));
		declareParams(procId, spec.getOutParams(), mSpecificationOutParam,
				new DeclarationInformation(StorageClass.PROC_FUNC_OUTPARAM, procId));
	}

	private void declareParams(final String procId, final VarList[] specVl, final VarList[] implVl,
			final Map<String, Map<String, BoogieVar>> specMap, final Map<String, Map<String, BoogieVar>> implMap,
			final DeclarationInformation declarationInformation) {
		if (specVl.length != implVl.length) {
			throw new IllegalArgumentException("specification and implementation have different param length");
		}
		for (int i = 0; i < specVl.length; i++) {
			final IType specType = specVl[i].getType().getBoogieType();
			final IType implType = implVl[i].getType().getBoogieType();
			if (!specType.equals(implType)) {
				throw new IllegalArgumentException("specification and implementation have different types");
			}
			final String[] specIds = specVl[i].getIdentifiers();
			final String[] implIds = implVl[i].getIdentifiers();
			if (specIds.length != implIds.length) {
				throw new IllegalArgumentException("specification and implementation have different param length");
			}
			for (int j = 0; j < specIds.length; j++) {
				final BoogieVar bv =
						constructLocalBoogieVar(implIds[j], procId, implType, implVl[i], declarationInformation);
				putNew(procId, implIds[j], bv, implMap);
				putNew(procId, specIds[j], bv, specMap);
			}
		}
	}

	/**
	 * Declare in or our parameters of a specification.
	 * 
	 * @param procId
	 *            name of procedure
	 * @param vl
	 *            Varlist defining the parameters
	 * @param specMap
	 *            map for the specification
	 * @param declarationInformation
	 *            StorageClass of the constructed BoogieVar
	 */
	private void declareParams(final String procId, final VarList[] vl, final Map<String, Map<String, BoogieVar>> specMap,
			final DeclarationInformation declarationInformation) {
		for (int i = 0; i < vl.length; i++) {
			final IType type = vl[i].getType().getBoogieType();
			final String[] ids = vl[i].getIdentifiers();
			for (int j = 0; j < ids.length; j++) {
				final BoogieVar bv = constructLocalBoogieVar(ids[j], procId, type, vl[i], declarationInformation);
				putNew(procId, ids[j], bv, specMap);
			}
		}
	}

	public void declareLocals(final Procedure proc) {
		if (proc.getBody() != null) {
			final DeclarationInformation declarationInformation =
					new DeclarationInformation(StorageClass.LOCAL, proc.getIdentifier());
			for (final VariableDeclaration vdecl : proc.getBody().getLocalVars()) {
				for (final VarList vl : vdecl.getVariables()) {
					for (final String id : vl.getIdentifiers()) {
						final IType type = vl.getType().getBoogieType();
						final LocalBoogieVar bv =
								constructLocalBoogieVar(id, proc.getIdentifier(), type, vl, declarationInformation);
						putNew(proc.getIdentifier(), id, bv, mImplementationLocals);
					}
				}
			}
		}
	}

	/**
	 * Construct BoogieVar and store it. Expects that no BoogieVar with the same identifier has already been
	 * constructed.
	 * 
	 * @param identifier
	 * @param procedure
	 * @param iType
	 * @param isOldvar
	 * @param boogieASTNode
	 *            BoogieASTNode for which errors (e.g., unsupported syntax) are reported
	 * @param declarationInformation
	 */
	private LocalBoogieVar constructLocalBoogieVar(final String identifier, final String procedure, final IType iType, final VarList varList,
			final DeclarationInformation declarationInformation) {
		final Sort sort = mTypeSortTranslator.getSort(iType, varList);

		final String name = constructBoogieVarName(identifier, procedure, false, false);

		final TermVariable termVariable = mScript.variable(name, sort);

		final ApplicationTerm defaultConstant = constructDefaultConstant(sort, name);
		final ApplicationTerm primedConstant = constructPrimedConstant(sort, name);

		final LocalBoogieVar bv =
				new LocalBoogieVar(identifier, procedure, iType, termVariable, defaultConstant, primedConstant);

		mSmtVar2BoogieVar.put(termVariable, bv);
		mBoogieVar2DeclarationInformation.put(bv, declarationInformation);
		mBoogieVar2AstNode.put(bv, varList);
		return bv;
	}

	/**
	 * Construct global BoogieVar and the corresponding oldVar and store both. Expects that no local BoogieVarwith the
	 * same identifier has already been constructed.
	 * 
	 * @param boogieASTNode
	 *            BoogieASTNode for which errors (e.g., unsupported syntax) are reported
	 */
	private BoogieNonOldVar constructGlobalBoogieVar(final String identifier, final IType iType, final VarList varlist) {
		final Sort sort = mTypeSortTranslator.getSort(iType, varlist);
		final String procedure = null;
		final DeclarationInformation declarationInformation = new DeclarationInformation(StorageClass.GLOBAL, null);

		BoogieOldVar oldVar;
		{
			final boolean isOldVar = true;
			final String name = constructBoogieVarName(identifier, procedure, true, isOldVar);
			final TermVariable termVariable = mScript.variable(name, sort);
			final ApplicationTerm defaultConstant = constructDefaultConstant(sort, name);
			final ApplicationTerm primedConstant = constructPrimedConstant(sort, name);

			oldVar = new BoogieOldVar(identifier, iType, isOldVar, termVariable, defaultConstant, primedConstant);
			mSmtVar2BoogieVar.put(termVariable, oldVar);
			mBoogieVar2DeclarationInformation.put(oldVar, declarationInformation);
			mBoogieVar2AstNode.put(oldVar, varlist);
		}
		BoogieNonOldVar nonOldVar;
		{
			final boolean isOldVar = false;
			final String name = constructBoogieVarName(identifier, procedure, true, isOldVar);
			final TermVariable termVariable = mScript.variable(name, sort);
			final ApplicationTerm defaultConstant = constructDefaultConstant(sort, name);
			final ApplicationTerm primedConstant = constructPrimedConstant(sort, name);

			nonOldVar = new BoogieNonOldVar(identifier, iType, termVariable, defaultConstant, primedConstant, oldVar);
			mSmtVar2BoogieVar.put(termVariable, nonOldVar);
			mBoogieVar2DeclarationInformation.put(nonOldVar, declarationInformation);
			mBoogieVar2AstNode.put(nonOldVar, varlist);
		}
		oldVar.setNonOldVar(nonOldVar);
		return nonOldVar;
	}

	private ApplicationTerm constructPrimedConstant(final Sort sort, final String name) {
		ApplicationTerm primedConstant;
		{
			final String primedConstantName = "c_" + name + "_primed";
			mScript.declareFun(primedConstantName, new Sort[0], sort);
			primedConstant = (ApplicationTerm) mScript.term(primedConstantName);
		}
		return primedConstant;
	}

	private ApplicationTerm constructDefaultConstant(final Sort sort, final String name) {
		ApplicationTerm defaultConstant;
		{
			final String defaultConstantName = "c_" + name;
			mScript.declareFun(defaultConstantName, new Sort[0], sort);
			defaultConstant = (ApplicationTerm) mScript.term(defaultConstantName);
		}
		return defaultConstant;
	}

	private String constructBoogieVarName(final String identifier, final String procedure, final boolean isGlobal, final boolean isOldvar) {
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

	IProgramNonOldVar constructAuxiliaryGlobalBoogieVar(final String identifier, final String procedure, final IType iType,
			final VarList varList) {
		final BoogieNonOldVar bv = constructGlobalBoogieVar(identifier, iType, varList);
		mGlobals.put(identifier, bv);
		mOldGlobals.put(identifier, bv.getOldVar());
		return bv;
	}

}
