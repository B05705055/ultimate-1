/*
 * Copyright (C) 2013-2015 Alexander Nutz (nutz@informatik.uni-freiburg.de)
 * Copyright (C) 2013-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Markus Lindenmann (lindenmm@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE CACSL2BoogieTranslator plug-in.
 * 
 * The ULTIMATE CACSL2BoogieTranslator plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE CACSL2BoogieTranslator plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE CACSL2BoogieTranslator plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE CACSL2BoogieTranslator plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE CACSL2BoogieTranslator plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.cHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionDeclarator;

import de.uni_freiburg.informatik.ultimate.boogie.ExpressionFactory;
import de.uni_freiburg.informatik.ultimate.boogie.ast.ASTType;
import de.uni_freiburg.informatik.ultimate.boogie.ast.AssignmentStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Attribute;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BinaryExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Body;
import de.uni_freiburg.informatik.ultimate.boogie.ast.CallStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Declaration;
import de.uni_freiburg.informatik.ultimate.boogie.ast.EnsuresSpecification;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.HavocStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IdentifierExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IfStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IntegerLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.LeftHandSide;
import de.uni_freiburg.informatik.ultimate.boogie.ast.ModifiesSpecification;
import de.uni_freiburg.informatik.ultimate.boogie.ast.PrimitiveType;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Procedure;
import de.uni_freiburg.informatik.ultimate.boogie.ast.ReturnStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Specification;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.UnaryExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.VarList;
import de.uni_freiburg.informatik.ultimate.boogie.ast.VariableDeclaration;
import de.uni_freiburg.informatik.ultimate.boogie.ast.VariableLHS;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BinaryExpression.Operator;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.CACSLLocation;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.LocationFactory;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.SymbolTable;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.CHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.MainDispatcher;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.PRDispatcher;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.TypeHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.ExpressionTranslation.AExpressionTranslation;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.cHandler.MemoryHandler.MemoryModelDeclarations;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.SymbolTableValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CArray;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CFunction;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPointer;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.GENERALPRIMITIVE;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.PRIMITIVE;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CType;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.exception.IncorrectSyntaxException;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.exception.UnsupportedSyntaxException;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.CDeclaration;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.CompoundStatementExpressionResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.ContractResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.ExpressionResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.HeapLValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.LocalLValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.RValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.Result;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.SkipResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.SFO;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.TarjanSCC;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.Dispatcher;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceStore;
import de.uni_freiburg.informatik.ultimate.model.acsl.ACSLNode;
import de.uni_freiburg.informatik.ultimate.models.ILocation;
import de.uni_freiburg.informatik.ultimate.models.annotation.Check;
import de.uni_freiburg.informatik.ultimate.models.annotation.Overapprox;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.TranslationMode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.preferences.CACSLPreferenceInitializer;

/**
 * Class that handles translation of functions.
 * 
 * @author Markus Lindenmann
 * @date 12.10.2012
 */
public class FunctionHandler {
	/**
	 * A map from procedure name to procedure declaration.
	 */
	private LinkedHashMap<String, Procedure> procedures;
	/**
	 * The currently handled procedure.
	 */
	private Procedure currentProcedure;
	/**
	 * Whether the modified globals is user defined or not. If it is in this
	 * set, then it is a modifies clause defined by the user.
	 */
	private LinkedHashSet<String> modifiedGlobalsIsUserDefined;
	/**
	 * A map from method name to all called methods of the specified one.
	 */
	private LinkedHashMap<String, LinkedHashSet<String>> callGraph;
	/**
	 * Whether the current procedure is declared to return void.
	 */
	private boolean currentProcedureIsVoid;
	/**
	 * Modified global variables of the current function.
	 */
	private LinkedHashMap<String, LinkedHashSet<String>> modifiedGlobals;
	/**
	 * Methods that have been called before they were declared. These methods
	 * need a special treatment, as they are assumed to be returning int!
	 */
	private LinkedHashSet<String> methodsCalledBeforeDeclared;

	private LinkedHashMap<String, CFunction> procedureToCFunctionType;

	private final boolean m_CheckMemoryLeakAtEndOfMain;

	/**
	 * Herein the function Signatures (as a CFunction) are stored for which a
	 * boogie procedure has to be created in the postProcessor that deals with
	 * the function pointer calls that can happen.
	 */
//	LinkedHashSet<CFunction> functionSignaturesThatHaveAFunctionPointer;
	LinkedHashSet<ProcedureSignature> functionSignaturesThatHaveAFunctionPointer;
	
	private final AExpressionTranslation m_ExpressionTranslation;
	private final TypeSizeAndOffsetComputer m_TypeSizeComputer;

	/**
	 * Constructor.
	 * @param expressionTranslation 
	 * @param typeSizeComputer 
	 */
	public FunctionHandler(AExpressionTranslation expressionTranslation, TypeSizeAndOffsetComputer typeSizeComputer) {
		this.m_ExpressionTranslation = expressionTranslation;
		this.m_TypeSizeComputer = typeSizeComputer;
		this.callGraph = new LinkedHashMap<String, LinkedHashSet<String>>();
		this.currentProcedureIsVoid = false;
		this.modifiedGlobals = new LinkedHashMap<String, LinkedHashSet<String>>();
		this.methodsCalledBeforeDeclared = new LinkedHashSet<String>();
		this.procedures = new LinkedHashMap<String, Procedure>();
		this.procedureToCFunctionType = new LinkedHashMap<>();
		this.modifiedGlobalsIsUserDefined = new LinkedHashSet<String>();
		m_CheckMemoryLeakAtEndOfMain = (new UltimatePreferenceStore(Activator.PLUGIN_ID))
				.getBoolean(CACSLPreferenceInitializer.LABEL_CHECK_MemoryLeakInMain);
		this.functionSignaturesThatHaveAFunctionPointer = new LinkedHashSet<>();
	}

	
	/**
	 * This is called from SimpleDeclaration and handles a C function
	 * declaration.
	 * 
	 * The effects are: - the declaration is stored to
	 * FunctionHandler.procedures (which stores all Boogie procedure
	 * declarations - procedureTo(Return/Param)CType memebers are updated
	 * 
	 * The returned result is empty (ResultSkip).
	 * 
	 * @param cDec
	 *            the CDeclaration of the function that was computed by visit
	 *            SimpleDeclaration
	 * @param loc
	 *            the location of the FunctionDeclarator
	 */
	public Result handleFunctionDeclarator(Dispatcher main, ILocation loc, List<ACSLNode> contract, CDeclaration cDec) {
		String methodName = cDec.getName();
		CFunction funcType = (CFunction) cDec.getType();

		addAProcedure(main, loc, contract, methodName, funcType);

		return new SkipResult();
	}

	/**
	 * Handles translation of IASTFunctionDefinition.
	 * 
	 * Note that a C function definition may have an ACSL specification while a
	 * Boogie procedure implementation does not have a specification (right?).
	 * Therefore we have to add any ACSL specs to the procedures member where
	 * the (Boogie) function declarations are stored.
	 * 
	 * The Result contains the Boogie procedure implementation.
	 * 
	 * @param main
	 *            a reference to the main dispatcher.
	 * @param node
	 *            the node to translate.
	 * @param contract
	 * @return the translation result.
	 */
	public Result handleFunctionDefinition(Dispatcher main, MemoryHandler memoryHandler, IASTFunctionDefinition node,
			CDeclaration cDec, List<ACSLNode> contract) {
		main.cHandler.beginScope();

		ILocation loc = LocationFactory.createCLocation(node);
		String methodName = cDec.getName();
		CType returnCType = ((CFunction) cDec.getType()).getResultType();
		boolean returnTypeIsVoid = returnCType instanceof CPrimitive
				&& ((CPrimitive) returnCType).getType() == PRIMITIVE.VOID;

		updateCFunction(methodName, returnCType, null, null, false);

		VarList[] in = processInParams(main, loc, (CFunction) cDec.getType(), methodName);

		VarList[] out = new VarList[1];
		ASTType type = main.typeHandler.ctype2asttype(loc, returnCType);

		if (returnTypeIsVoid) { // void, so there are no out vars
			out = new VarList[0];
		} else if (methodsCalledBeforeDeclared.contains(methodName)) {
			// TODO: defaulting to int -- but does this work on all examples?
			CPrimitive cPrimitive = new CPrimitive(PRIMITIVE.INT);
			out[0] = new VarList(loc, new String[] { SFO.RES }, main.typeHandler.ctype2asttype(loc, cPrimitive));
		} else { // "normal case"
			assert type != null;
//			out[0] = new VarList(loc, new String[] { methodName }, type); // at
			out[0] = new VarList(loc, new String[] { SFO.RES }, type); // at
																			// most
																			// one
																			// out
																			// param
																			// in
																			// C
		}

		Specification[] spec = makeBoogieSpecFromACSLContract(main, contract, methodName);

		Procedure proc = procedures.get(methodName);
		if (proc == null) {
			Attribute[] attr = new Attribute[0];
			String[] typeParams = new String[0];
			if (isInParamVoid(in)) {
				in = new VarList[0]; // in parameter is "void"
			}
			proc = new Procedure(loc, attr, methodName, typeParams, in, out, spec, null);
			if (procedures.containsKey(methodName)) {
				String msg = "Duplicated method identifier: " + methodName
						+ ". C does not support function overloading!";
				throw new IncorrectSyntaxException(loc, msg);
			}
			procedures.put(methodName, proc);
		} else { // check declaration against its implementation
			VarList[] declIn = proc.getInParams();
			boolean checkInParams = true;
			if (in.length != proc.getInParams().length || out.length != proc.getOutParams().length
					|| isInParamVoid(proc.getInParams())) {
				if (proc.getInParams().length == 0) {
					// the implementation can have 0 to n in parameters!
					// do not check, but use the in params of the implementation
					// as we will take the ones of the implementation anyway
					checkInParams = false;
					declIn = in;
				} else if (isInParamVoid(proc.getInParams()) && (in.length == 0 || isInParamVoid(in))) {
					declIn = new VarList[0];
					in = new VarList[0];
					checkInParams = false;
				} else {
					String msg = "Implementation does not match declaration!";
					throw new IncorrectSyntaxException(loc, msg);
				}
			}
			if (checkInParams) {
				for (int i = 0; i < in.length; i++) {
					if (!(in[i].getType().toString().equals(proc.getInParams()[i].getType().toString()))) {
						final String msg = "Implementation does not match declaration! " 
								+ "Type missmatch on in-parameters! "
								+ in.length + " arguments, "
								+ proc.getInParams().length + " parameters, "
								+ "first missmatch at position " + i + ", "
								+ "argument type " + in[i].getType().toString() + ", "
								+ "param type " + proc.getInParams()[i].toString();
						throw new IncorrectSyntaxException(loc, msg);
					}
				}
			}

			// combine the specification from the definition with the one from
			// the declaration
			List<Specification> specFromDec = Arrays.asList(proc.getSpecification());
			ArrayList<Specification> newSpecs = new ArrayList<Specification>(Arrays.asList(spec));
			newSpecs.addAll(specFromDec);
			spec = newSpecs.toArray(new Specification[0]);

			proc = new Procedure(proc.getLocation(), proc.getAttributes(), proc.getIdentifier(), proc.getTypeParams(),
					declIn, proc.getOutParams(), spec, null);
			procedures.put(methodName, proc);
		}
		Procedure declWithCorrectlyNamedInParams = new Procedure(proc.getLocation(), proc.getAttributes(),
				proc.getIdentifier(), proc.getTypeParams(), in, proc.getOutParams(), proc.getSpecification(), null);
		currentProcedure = declWithCorrectlyNamedInParams;
		currentProcedureIsVoid = returnTypeIsVoid;
		if (!modifiedGlobals.containsKey(currentProcedure.getIdentifier())) {
			modifiedGlobals.put(currentProcedure.getIdentifier(), new LinkedHashSet<String>());
		}
		if (!callGraph.containsKey(currentProcedure.getIdentifier())) {
			callGraph.put(currentProcedure.getIdentifier(), new LinkedHashSet<String>());
		}

		/*
		 * The structure is as follows: 1) Preprocessing of the method body: -
		 * add new variables for parameters - havoc them - etc. 2) dispatch body
		 * 3) handle mallocs 4) add statements and declarations to new body
		 */
		ArrayList<Statement> stmts = new ArrayList<Statement>();
		ArrayList<Declaration> decls = new ArrayList<Declaration>();
		// 1)
		handleFunctionsInParams(main, loc, memoryHandler, decls, stmts, node);
		// 2)
//		Body body = ((Body) main.dispatch(node.getBody()).node);
//		stmts.addAll(Arrays.asList(body.getBlock()));
//		for (VariableDeclaration declaration : body.getLocalVars()) {
//			decls.add(declaration);
//		}
		CompoundStatementExpressionResult cser = (CompoundStatementExpressionResult) main.dispatch(node.getBody());
		stmts.addAll(cser.stmt);
		decls.addAll(cser.decl);

		// 3) ,4)
		stmts = ((CHandler) main.cHandler).updateStmtsAndDeclsAtScopeEnd(main, decls, stmts);
	
		Body body = new Body(loc, decls.toArray(new VariableDeclaration[decls.size()]), stmts.toArray(new Statement[stmts
				.size()]));

		proc = currentProcedure;
		// Implementation -> Specification always null!
		Procedure impl = new Procedure(loc, proc.getAttributes(), methodName, proc.getTypeParams(), in,
				proc.getOutParams(), null, body);
		currentProcedure = null;
		currentProcedureIsVoid = false;
		main.cHandler.endScope();
		return new Result(impl);
	}

	/**
	 * Handles translation of IASTFunctionCallExpression.
	 * 
	 * @param main
	 *            a reference to the main dispatcher.
	 * @param memoryHandler
	 *            a reference to the memory Handler.
	 * @param node
	 *            the node to translate.
	 * @return the translation result.
	 */
	public Result handleFunctionCallExpression(Dispatcher main, MemoryHandler memoryHandler,
			StructHandler structHandler, ILocation loc, IASTExpression functionName, IASTInitializerClause[] arguments) {
		if (!(functionName instanceof IASTIdExpression)) {
			return handleFunctionPointerCall(loc, main, memoryHandler, 
					structHandler, functionName, arguments);
		}
		String methodName = ((IASTIdExpression) functionName).getName().toString();

		if (main.cHandler.getSymbolTable().containsCSymbol(methodName)) {
			return handleFunctionPointerCall(loc, main, memoryHandler, 
					structHandler, functionName, arguments);
		}

		return handleFunctionCallGivenNameAndArguments(main, memoryHandler, structHandler, 
				loc, methodName, arguments);
	}

	/**
	 * Handles translation of return statements.
	 * 
	 * @param main
	 *            a reference to the main dispatcher.
	 * @param node
	 *            the node to translate.
	 * @return the translation result.
	 */
	public Result handleReturnStatement(Dispatcher main, MemoryHandler memoryHandler, StructHandler structHandler,
			IASTReturnStatement node) {
		ArrayList<Statement> stmt = new ArrayList<Statement>();
		ArrayList<Declaration> decl = new ArrayList<Declaration>();
		Map<VariableDeclaration, ILocation> auxVars = new LinkedHashMap<VariableDeclaration, ILocation>();
		ArrayList<Overapprox> overApp = new ArrayList<>();
		// The ReturnValue could be empty!
		ILocation loc = LocationFactory.createCLocation(node);
		VarList[] outParams = this.currentProcedure.getOutParams();
		ExpressionResult rExp = new ExpressionResult(stmt, null, decl, auxVars, overApp);
		if (methodsCalledBeforeDeclared.contains(currentProcedure.getIdentifier()) && currentProcedureIsVoid) {
			// void method that was assumed to be returning int! -> return int
			String id = outParams[0].getIdentifiers()[0];
			VariableLHS lhs = new VariableLHS(loc, id);
			Statement havoc = new HavocStatement(loc, new VariableLHS[] { lhs });
			stmt.add(havoc);
		} else if (node.getReturnValue() != null) {
			ExpressionResult exprResult = ((ExpressionResult) main.dispatch(node.getReturnValue()))
					.switchToRValueIfNecessary(main, memoryHandler, structHandler, loc);
			exprResult.rexBoolToIntIfNecessary(loc, m_ExpressionTranslation);

			// do some implicit casts
			CType functionResultType = this.procedureToCFunctionType.get(currentProcedure.getIdentifier())
					.getResultType();
			if (!exprResult.lrVal.getCType().equals(functionResultType)) {
				if (functionResultType instanceof CPointer && exprResult.lrVal.getCType() instanceof CPrimitive
						&& exprResult.lrVal.getValue() instanceof IntegerLiteral
						&& ((IntegerLiteral) exprResult.lrVal.getValue()).getValue().equals("0")) {
					exprResult.lrVal = new RValue(m_ExpressionTranslation.constructNullPointer(loc), functionResultType);
				}
			}

			stmt.addAll(exprResult.stmt);
			decl.addAll(exprResult.decl);
			auxVars.putAll(exprResult.auxVars);
			overApp.addAll(exprResult.overappr);
			if (outParams.length == 0) {
				// void method which is returning something! We remove the
				// return value!
				String msg = "This method is declared to be void, but returning a value!";
				main.syntaxError(loc, msg);
			} else if (outParams.length != 1) {
				String msg = "We do not support several output parameters for functions";
				throw new UnsupportedSyntaxException(loc, msg);
			} else {
				String id = outParams[0].getIdentifiers()[0];
				VariableLHS[] lhs = new VariableLHS[] { new VariableLHS(loc, id) };
				rExp.lrVal = exprResult.lrVal;
				main.cHandler.convert(loc, rExp, functionResultType);
				RValue castExprResultRVal = (RValue) rExp.lrVal;
				stmt.add(new AssignmentStatement(loc, lhs, new Expression[] { castExprResultRVal.getValue() }));
				// //assuming that we need no auxvars or overappr, here
			}
		}
		stmt.addAll(CHandler.createHavocsForAuxVars(auxVars));

		// we need to insert a free for each malloc of an auxvar before each
		// return
		for (Entry<LocalLValueILocationPair, Integer> entry : memoryHandler.getVariablesToBeFreed().entrySet()) { // frees
																										// are
																										// inserted
																										// in
																										// handleReturnStm
			if (entry.getValue() >= 1) {
				stmt.add(memoryHandler.getDeallocCall(main, this, entry.getKey().llv, entry.getKey().loc));
				stmt.add(new HavocStatement(loc, new VariableLHS[] { (VariableLHS) entry.getKey().llv.getLHS() }));
			}
		}

		stmt.add(new ReturnStatement(loc));
		return rExp;
	}

	/**
	 * Calculates transitive modifies clauses for all procedure declarations
	 * linear in time to (|procedures| + |procedure calls|).
	 * 
	 * addition (alex, may 2014): for every modifies clause: if one memory-array
	 * is included, all active memory arrays have to be included (f.i. we have
	 * procedure modifies memory_int, and memoryHandler.isFloatMMArray == true,
	 * and memoryHandler.isIntMMArray == true, memoryHandler.isPointerMMArray ==
	 * false, then we have to add memory_real to the modifies clause of
	 * procedure
	 * 
	 * @param main
	 *            a reference to the main dispatcher.
	 * @return procedure declarations
	 */
	public ArrayList<Declaration> calculateTransitiveModifiesClause(Dispatcher main, MemoryHandler memoryHandler) {
		assert isEveryCalledProcedureDeclared() == null;
		// calculate SCCs and a mapping for each methodId to its SCC
		// O(|edges| + |calls|)
		LinkedHashSet<LinkedHashSet<String>> sccs = new TarjanSCC().getSCCs(callGraph);
		LinkedHashMap<String, LinkedHashSet<String>> functionNameToScc = new LinkedHashMap<String, LinkedHashSet<String>>();
		for (LinkedHashSet<String> scc : sccs) { // O(|proc|)
			for (String s : scc) {
				functionNameToScc.put(s, scc);
			}
		}
		// counts how many incoming edges an scc has in the updateGraph
		LinkedHashMap<LinkedHashSet<String>, Integer> incomingEdges = new LinkedHashMap<LinkedHashSet<String>, Integer>();
		for (LinkedHashSet<String> scc : sccs) {
			incomingEdges.put(scc, 0);
		}
		// calculate the SCC update graph without loops and dead ends
		Queue<LinkedHashSet<String>> deadEnds = new LinkedList<LinkedHashSet<String>>();
		deadEnds.addAll(sccs);

		// updateGraph maps a calleeSCC to many callerSCCs
		// This graph might not be complete! It is i.e. missing all procedures,
		// that do not have incoming or outgoing edges!
		// But: They don't need an update anyway!
		LinkedHashMap<LinkedHashSet<String>, LinkedHashSet<LinkedHashSet<String>>> updateGraph = new LinkedHashMap<LinkedHashSet<String>, LinkedHashSet<LinkedHashSet<String>>>();
		for (String caller : callGraph.keySet()) { // O(|calls|)
			for (String callee : callGraph.get(caller)) { // foreach s : succ(p)
				LinkedHashSet<String> sccCaller = functionNameToScc.get(caller);
				LinkedHashSet<String> sccCallee = functionNameToScc.get(callee);
				if (sccCaller == sccCallee)
					continue; // skip self loops
				if (updateGraph.containsKey(sccCallee)) {
					updateGraph.get(sccCallee).add(sccCaller);
				} else {
					LinkedHashSet<LinkedHashSet<String>> predSCCs = new LinkedHashSet<LinkedHashSet<String>>();
					predSCCs.add(sccCaller);
					updateGraph.put(sccCallee, predSCCs);
				}
				deadEnds.remove(sccCaller);
			}
		}
		
		// incoming edges must be computed on a graph that has Sccs as nodes (updategraph), not just the functions (callgraph)
		for (LinkedHashSet<String> calleeScc : updateGraph.keySet()) {
			for (LinkedHashSet<String> callerScc : updateGraph.get(calleeScc)) {
				incomingEdges.put(callerScc, incomingEdges.get(callerScc) + 1);
			}
		}
		
		// calculate transitive modifies clause
		LinkedHashMap<LinkedHashSet<String>, LinkedHashSet<String>> sccToModifiedGlobals = new LinkedHashMap<LinkedHashSet<String>, LinkedHashSet<String>>();
		while (!deadEnds.isEmpty()) {
			// O (|proc| + |edges in updateGraph|), where
			// |edges in updateGraph| <= |calls|
			LinkedHashSet<String> deadEnd = deadEnds.poll();

			// the modified globals of the scc is the union of the modified globals of all its functions
			for (String func : deadEnd) {
				if (!sccToModifiedGlobals.containsKey(deadEnd)) {
					sccToModifiedGlobals.put(deadEnd, new LinkedHashSet<String>(modifiedGlobals.get(func)));
				} else {
					sccToModifiedGlobals.get(deadEnd).addAll(modifiedGlobals.get(func));
				}
			}

			//if the scc has no callers, do nothing with it
			if (updateGraph.get(deadEnd) == null)
				continue;

			// for all callers of the scc, add the modified globals to them, make them deadends, if all their input has been processed
			for (LinkedHashSet<String> caller : updateGraph.get(deadEnd)) {
				if (!sccToModifiedGlobals.containsKey(caller)) {
					LinkedHashSet<String> n = new LinkedHashSet<String>();
					n.addAll(sccToModifiedGlobals.get(deadEnd));
					sccToModifiedGlobals.put(caller, n);
				} else {
					sccToModifiedGlobals.get(caller).addAll(sccToModifiedGlobals.get(deadEnd));
				}
				int remainingUpdates = incomingEdges.get(caller) - 1;
				if (remainingUpdates == 0) {
					deadEnds.add(caller);
				}
				incomingEdges.put(caller, remainingUpdates);
			}
		}
		// update the modifies clauses!
		ArrayList<Declaration> declarations = new ArrayList<Declaration>();
		for (Procedure procDecl : procedures.values()) { // O(|proc|)
			String mId = procDecl.getIdentifier();
			Specification[] spec = procDecl.getSpecification();
			CACSLLocation loc = (CACSLLocation) procDecl.getLocation();
			if (!modifiedGlobalsIsUserDefined.contains(mId)) {
				assert functionNameToScc.get(mId) != null;
				LinkedHashSet<String> currModClause = sccToModifiedGlobals.get(functionNameToScc.get(mId));
				assert currModClause != null : "No modifies clause proc " + mId;

				modifiedGlobals.get(mId).addAll(currModClause);
				int nrSpec = spec.length;
				spec = Arrays.copyOf(spec, nrSpec + 1);
				LinkedHashSet<String> modifySet = new LinkedHashSet<>();

				for (String var : modifiedGlobals.get(mId)) {
					modifySet.add(var);
				}

				
				{
					/* add missing heap arrays
					 * If the procedure modifies one heap array, we add all
					 * heap arrays. This is a workaround. We cannot add all
					 * procedures immediately, because we do not know all heap
					 * arrays in advance since they are added lazily on demand.
					 * 
					 */
					Collection<HeapDataArray> heapDataArrays = memoryHandler.getMemoryModel().
							getDataHeapArrays(memoryHandler.getRequiredMemoryModelFeatures());
					if (containsOneHeapDataArray(modifySet, heapDataArrays)) {
						for (HeapDataArray hda : heapDataArrays) {
							modifySet.add(hda.getVariableName());
						}
					}
				}

				VariableLHS[] modifyList = new VariableLHS[modifySet.size()];
				{
					int i = 0;
					for (String modifyEntry : modifySet)
						modifyList[i++] = new VariableLHS(loc, modifyEntry);
				}
				spec[nrSpec] = new ModifiesSpecification(loc, false, modifyList);
			}
			if (main.isMMRequired() && (main.getCheckedMethod() == SFO.EMPTY || main.getCheckedMethod().equals(mId))) {
				if (m_CheckMemoryLeakAtEndOfMain) {
					// add a specification to check for memory leaks
					Expression vIe = new IdentifierExpression(loc, SFO.VALID);
					int nrSpec = spec.length;
					Check check = new Check(Check.Spec.MEMORY_LEAK);
					ILocation ensLoc = LocationFactory.createLocation(loc, check);
					spec = Arrays.copyOf(spec, nrSpec + 1);
					spec[nrSpec] = new EnsuresSpecification(ensLoc, false, ExpressionFactory.newBinaryExpression(loc, Operator.COMPEQ,
							vIe, ExpressionFactory.newUnaryExpression(loc, UnaryExpression.Operator.OLD, vIe)));
					check.addToNodeAnnot(spec[nrSpec]);
				}
			}
			declarations.add(new Procedure(loc, procDecl.getAttributes(), mId, procDecl.getTypeParams(), procDecl
					.getInParams(), procDecl.getOutParams(), spec, null));
		}
		return declarations;
	}

	private boolean containsOneHeapDataArray(LinkedHashSet<String> modifySet, 
											Collection<HeapDataArray> heapDataArrays) {
		for (HeapDataArray hda : heapDataArrays) {
			if (modifySet.contains(hda.getVariableName())) {
				return true;
			}
		}
		return false;
	}


	private Result handleFunctionCallGivenNameAndArguments(Dispatcher main, MemoryHandler memoryHandler,
			StructHandler structHandler, ILocation loc, String methodName, IASTInitializerClause[] arguments) {

		ArrayList<Statement> stmt = new ArrayList<Statement>();
		ArrayList<Declaration> decl = new ArrayList<Declaration>();
		Map<VariableDeclaration, ILocation> auxVars = new LinkedHashMap<VariableDeclaration, ILocation>();
		ArrayList<Overapprox> overappr = new ArrayList<Overapprox>();

		callGraph.get(currentProcedure.getIdentifier()).add(methodName);

		boolean procedureDeclaredWithOutInparamsButCalledWithInParams = procedures.get(methodName) != null
				&& (procedures.get(methodName).getBody() == null)
				&& procedures.get(methodName).getInParams().length == 0;

		// if the function has varArgs, we throw away all parameters that belong
		// to the varArgs part and only keep the normal ones
		IASTInitializerClause[] inParams = arguments;
		if (procedureToCFunctionType.containsKey(methodName) && procedureToCFunctionType.get(methodName).takesVarArgs()) {
			int noParameterWOVarArgs = procedureToCFunctionType.get(methodName).getParameterTypes().length;
			inParams = new IASTInitializerClause[noParameterWOVarArgs];
			for (int i = 0; i < noParameterWOVarArgs; i++) {
				inParams[i] = arguments[i];
			}
			// .. and if it is really called with more that its normal parameter
			// number, we throw an exception, because we may be unsound
			// (the code before this does not make that much sense, but maybe some
			// day we want that solution again..
			if (!(main.mPreferences.getEnum(CACSLPreferenceInitializer.LABEL_MODE, TranslationMode.class)
					.equals(TranslationMode.SV_COMP14)) 
					&& inParams.length < arguments.length)

				throw new UnsupportedSyntaxException(loc, "we cannot deal with varargs right now");
		}

		if (procedures.containsKey(methodName) && inParams.length != procedures.get(methodName).getInParams().length) {
			if (!(procedures.get(methodName).getInParams().length == 1
					&& procedures.get(methodName).getInParams()[0].getType() == null && inParams.length == 0)
					// ok, if the procedure is declared (and not implemented) as
					// having no parameters --> then we may call it with
					// parameters later
					&& !procedureDeclaredWithOutInparamsButCalledWithInParams) {
				String msg = "Function call has incorrect number of in-params!";
				throw new IncorrectSyntaxException(loc, msg);
			} // else: this means param of declaration is void and parameter
				// list of call is empty! --> OK
		}

		// dispatch the inparams
		ArrayList<Expression> args = new ArrayList<Expression>();
		for (int i = 0; i < inParams.length; i++) {
			IASTInitializerClause inParam = inParams[i];
			ExpressionResult in = ((ExpressionResult) main.dispatch(inParam));
					
			if (in.lrVal.getCType().getUnderlyingType() instanceof CArray) {
				//arrays are passed as pointers --> switch to RValue would make a boogie array..
				CType valueType = ((CArray) in.lrVal.getCType().getUnderlyingType()).getValueType().getUnderlyingType();
				if (in.lrVal instanceof HeapLValue)
					in.lrVal = new RValue(((HeapLValue)in.lrVal).getAddress(), new CPointer(valueType));
				else
					in.lrVal = new RValue(in.lrVal.getValue(), new CPointer(valueType));	
			} else {
				in = in.switchToRValueIfNecessary(main,
					memoryHandler, structHandler, loc);
			}

			
			if (in.lrVal.getValue() == null) {
				String msg = "Incorrect or invalid in-parameter! " + loc.toString();
				throw new IncorrectSyntaxException(loc, msg);
			}

			// if the procedure is declared (and not implemented) as having no
			// parameters --> then we may call it with parameters later
			// --> but from then on we know its parameters
			if (procedureDeclaredWithOutInparamsButCalledWithInParams) {
				// add the current parameter to the procedure's signature
				updateCFunction(methodName, null, null, new CDeclaration(in.lrVal.getCType(), SFO.IN_PARAM + i), false);
			} else if (procedureToCFunctionType.containsKey(methodName)) { // we
																			// already
																			// know
																			// the
																			// parameters
				// do implicit casts and bool/int conversion
				CType expectedParamType = procedureToCFunctionType.get(methodName).getParameterTypes()[i].getType().getUnderlyingType();
				// bool/int conversion
				if (expectedParamType instanceof CPrimitive
						&& ((CPrimitive) expectedParamType).getGeneralType() == GENERALPRIMITIVE.INTTYPE) {
					in.rexBoolToIntIfNecessary(loc, m_ExpressionTranslation);
				}
				if (expectedParamType instanceof CFunction) {
					// workaround - better: make this conversion already in declaration
					expectedParamType = new CPointer(expectedParamType);
				}
				if (expectedParamType instanceof CArray) {
					// workaround - better: make this conversion already in declaration
					expectedParamType = new CPointer(((CArray) expectedParamType).getValueType());
				}
				// implicit casts
				main.cHandler.convert(loc, in, expectedParamType);
			}
			args.add(in.lrVal.getValue());
			stmt.addAll(in.stmt);
			decl.addAll(in.decl);
			auxVars.putAll(in.auxVars);
			overappr.addAll(in.overappr);
		}

		if (procedureDeclaredWithOutInparamsButCalledWithInParams) {
			VarList[] procParams = new VarList[procedureToCFunctionType.get(methodName).getParameterTypes().length];
			for (int i = 0; i < procParams.length; i++) {
				procParams[i] = new VarList(loc, new String[] { procedureToCFunctionType.get(methodName)
						.getParameterTypes()[i].getName() }, ((TypeHandler) main.typeHandler).ctype2asttype(loc,
						procedureToCFunctionType.get(methodName).getParameterTypes()[i].getType()));
			}
			Procedure currentProc = procedures.get(methodName);
			Procedure newProc = new Procedure(currentProc.getLocation(), currentProc.getAttributes(),
					currentProc.getIdentifier(), currentProc.getTypeParams(), procParams, currentProc.getOutParams(),
					currentProc.getSpecification(), currentProc.getBody());
			procedures.put(methodName, newProc);
		}

		return makeTheFunctionCallItself(main, loc, methodName, stmt, decl, auxVars, overappr, args);
	}

	/**
	 * Checks if the methodname is a function where we have our own specification or implementation and returns
	 * that in case.
	 * (This is typically the case for functions defined in the C standard.)
	 * @param main
	 * @param memoryHandler
	 * @param structHandler
	 * @param loc
	 * @param methodName
	 * @param arguments
	 * @return
	 */
	public Result handleStandardFunctions(Dispatcher main,
			MemoryHandler memoryHandler, StructHandler structHandler,
			ILocation loc, String methodName, IASTInitializerClause[] arguments) {
		if (methodName.equals("malloc") || methodName.equals("alloca") || methodName.equals("__builtin_alloca")) {
			assert arguments.length == 1;
			ExpressionResult exprRes = (ExpressionResult) main.dispatch(arguments[0]);
			exprRes = exprRes.switchToRValueIfNecessary(main, memoryHandler, structHandler, loc);
			main.cHandler.convert(loc, exprRes, m_TypeSizeComputer.getSize_T());
			
	    	CPointer resultType = new CPointer(new CPrimitive(PRIMITIVE.VOID));
	    	String tmpId = main.nameHandler.getTempVarUID(SFO.AUXVAR.MALLOC, resultType);
	        VariableDeclaration tmpVarDecl = SFO.getTempVarVariableDeclaration(tmpId, 
	        		main.typeHandler.constructPointerType(loc), loc);
	        exprRes.decl.add(tmpVarDecl);
	        
	        exprRes.stmt.add(memoryHandler.getMallocCall(exprRes.lrVal.getValue(), tmpId, loc));
	        exprRes.lrVal = new RValue(new IdentifierExpression(loc, tmpId), resultType);
	        
	        
			// for alloc a we have to free the variable ourselves when the
			// stackframe is closed, i.e. at a return
			if (methodName.equals("alloca") || methodName.equals("__builtin_alloca")) {
				LocalLValue llVal = new LocalLValue(new VariableLHS(loc, tmpId), resultType);
				memoryHandler.addVariableToBeFreed(main, 
						new LocalLValueILocationPair(llVal, 
								LocationFactory.createIgnoreLocation(loc)));
				//we need to clear auxVars because otherwise the malloc auxvar is havocced after 
				//this, and free (triggered by the statement before) would fail.
				exprRes.auxVars.clear();
			}
			return exprRes;
		} else if (methodName.equals("free")) {
			assert arguments.length == 1;
			Result pRes = main.dispatch(arguments[0]);
			assert pRes instanceof ExpressionResult;
			ExpressionResult pRex = ((ExpressionResult) pRes).switchToRValueIfNecessary(main, memoryHandler,
					structHandler, loc);
			pRex.stmt.add(memoryHandler.getFreeCall(main, this, pRex.lrVal, loc));
			return pRex;
		} else if (methodName.equals("calloc")) {
			/*
			 * C11 says in 7.22.3.2
			 * void *calloc(size_t nmemb, size_t size);
			 * The calloc function allocates space for an array of nmemb 
			 * objects, each of whose size is size. The space is initialized 
			 * to all bits zero.
			 */
			assert arguments.length == 2;
			ExpressionResult nmemb = ((ExpressionResult) main.dispatch(arguments[0])).switchToRValueIfNecessary(main, memoryHandler,structHandler, loc);
			main.cHandler.convert(loc, nmemb, m_TypeSizeComputer.getSize_T());
			ExpressionResult size = ((ExpressionResult) main.dispatch(arguments[1])).switchToRValueIfNecessary(main, memoryHandler,structHandler, loc);
			main.cHandler.convert(loc, size, m_TypeSizeComputer.getSize_T());
			
			Expression product = m_ExpressionTranslation.constructArithmeticExpression(
					loc, IASTBinaryExpression.op_multiply,
					nmemb.lrVal.getValue(), m_TypeSizeComputer.getSize_T(), 
					size.lrVal.getValue(), m_TypeSizeComputer.getSize_T());
			final ExpressionResult result = ExpressionResult.copyStmtDeclAuxvarOverapprox(nmemb, size);
			
	    	CPointer resultType = new CPointer(new CPrimitive(PRIMITIVE.VOID));
	    	String tmpId = main.nameHandler.getTempVarUID(SFO.AUXVAR.MALLOC, resultType);
	        VariableDeclaration tmpVarDecl = SFO.getTempVarVariableDeclaration(tmpId, 
	        		main.typeHandler.constructPointerType(loc), loc);
	        result.decl.add(tmpVarDecl);
	        
	        result.stmt.add(memoryHandler.getMallocCall(product, tmpId, loc));
	        result.lrVal = new RValue(new IdentifierExpression(loc, tmpId), resultType);
			
			result.stmt.add(memoryHandler.constructUltimateMeminitCall(loc, nmemb.lrVal.getValue(), 
					size.lrVal.getValue(), product, new IdentifierExpression(loc, tmpId)));
			
			if (this.callGraph.get(this.currentProcedure.getIdentifier()) == null)
				this.callGraph.put(this.currentProcedure.getIdentifier(), new LinkedHashSet<String>());
			this.callGraph.get(this.currentProcedure.getIdentifier()).add(MemoryModelDeclarations.Ultimate_MemInit.getName());
			this.callGraph.get(this.currentProcedure.getIdentifier()).add(MemoryModelDeclarations.Ultimate_Alloc.getName());

			return result;
		} else if (methodName.equals("memset")) {
			/*
			 * C11 says in 7.24.6.1
			 * void *memset(void *s, int c, size_t n);
			 * The memset function copies the value of c (converted to an 
			 * unsigned char) into each of the first n characters of the 
			 * object pointed to by s.
			 */
			assert arguments.length == 3 : "wrong number of arguments";
			ExpressionResult arg_s = ((ExpressionResult) main.dispatch(arguments[0])).switchToRValueIfNecessary(main, memoryHandler,structHandler, loc);
			ExpressionResult arg_c = ((ExpressionResult) main.dispatch(arguments[1])).switchToRValueIfNecessary(main, memoryHandler,structHandler, loc);
			m_ExpressionTranslation.convertIntToInt(loc, arg_c, new CPrimitive(PRIMITIVE.INT));
			ExpressionResult arg_n = ((ExpressionResult) main.dispatch(arguments[2])).switchToRValueIfNecessary(main, memoryHandler,structHandler, loc);
			m_ExpressionTranslation.convertIntToInt(loc, arg_n, m_TypeSizeComputer.getSize_T());
			
			final ExpressionResult result = new ExpressionResult(arg_s.lrVal);
			result.addAll(arg_s);
			result.addAll(arg_c);
			result.addAll(arg_n);
			
			String tId = main.nameHandler.getTempVarUID(SFO.AUXVAR.MEMSETRES, new CPointer(new CPrimitive(PRIMITIVE.VOID)));
			VariableDeclaration tVarDecl = new VariableDeclaration(loc, new Attribute[0], new VarList[] { new VarList(
					loc, new String[] { tId }, main.typeHandler.constructPointerType(loc)) });
			result.decl.add(tVarDecl);
			result.auxVars.put(tVarDecl, loc);		
			
			result.stmt.add(memoryHandler.constructUltimateMemsetCall(loc, arg_s.lrVal.getValue(), 
					arg_c.lrVal.getValue(), arg_n.lrVal.getValue(), tId));
			
			if (this.callGraph.get(this.currentProcedure.getIdentifier()) == null)
				this.callGraph.put(this.currentProcedure.getIdentifier(), new LinkedHashSet<String>());
			this.callGraph.get(this.currentProcedure.getIdentifier()).add(MemoryModelDeclarations.C_Memset.getName());
			
			return result;
			
		} else {
			return null;
		}
	}



	/**
	 * 
	 * The plan for function pointers:
	 *  - every function f, that is used as a pointer in the C code gets a number #f
	 *  - a pointer variable that points to a function then has the value {base: -1, offset: #f}
	 *  - for every function f, that is used as a pointer, and that has a signature s, we introduce a "dispatch-procedure" in Boogie for s
	 *  - the dispatch function for s = t1 x t2 x ... x tn -> t has the signature t1 x t2 x ... x tn x fp -> t, i.e., it takes the normal arguments, and a function address. 
	 *    When called, it calls the procedure that corresponds to the function address with the corresponding arguments and returns the returned value
	 *  - a call to a function pointer is then translated to a call to the dispatch-procedure with fitting signature where the function pointer is given as additional argument
	 *  - nb: when thinking about the function signatures, one has to keep in mind, the differences between C and Boogie, here. 
	 *    For instance, different C-function-signatures may correspond to on Boogie procedure signature, because a Boogie pointer does not know what it points to.
	 *    Also, void types need special treatment as any pointer can be used as a void-pointer 
	 *    The special method CType.isCompatibleWith() is used for this.
	 *      --> the  names of the different dispatch function have to match exactly the classification done by isCompatibleWith.
	 *    
	 * @param loc
	 * @param main
	 * @param memoryHandler
	 * @param structHandler
	 * @param functionName
	 * @param arguments
	 * @return
	 */
	private Result handleFunctionPointerCall(ILocation loc, Dispatcher main, MemoryHandler memoryHandler,
			StructHandler structHandler, IASTExpression functionName, IASTInitializerClause[] arguments) {

		assert (main instanceof PRDispatcher) || ((MainDispatcher) main).getFunctionToIndex().size() > 0;
		ExpressionResult funcNameRex = (ExpressionResult) main.dispatch(functionName);
//		RValue calledFuncRVal = (RValue) funcNameRex.switchToRValueIfNecessary(main, memoryHandler, structHandler, loc).lrVal;
		CType calledFuncType = funcNameRex.lrVal.getCType().getUnderlyingType();
		if (!(calledFuncType instanceof CFunction)) {
			// .. because function pointers don't need to be dereferenced in
			// order to be called
			if (calledFuncType instanceof CPointer) {
				calledFuncType = ((CPointer) calledFuncType).pointsToType.getUnderlyingType();
			}
		}
		assert calledFuncType instanceof CFunction : "We need to unpack it further, right?";
		CFunction calledFuncCFunction = (CFunction) calledFuncType;

		// check if the function is declared without parameters -- then the
		// signature is determined by the (first) call
		if (calledFuncCFunction.getParameterTypes().length == 0 && arguments.length > 0) {
			CDeclaration[] paramDecsFromCall = new CDeclaration[arguments.length];
			for (int i = 0; i < arguments.length; i++) {
				ExpressionResult rex = (ExpressionResult) main.dispatch(arguments[i]);
				paramDecsFromCall[i] = new CDeclaration(rex.lrVal.getCType(), "#param" + i); // TODO:
																						// SFO?
			}
			calledFuncCFunction = new CFunction(calledFuncCFunction.getResultType(), paramDecsFromCall,
					calledFuncCFunction.takesVarArgs());
		}

		//new Procedure()
		//functionSignaturesThatHaveAFunctionPointer = null;
//		TODO: use is compatible with instead of equals/set, make the name of the inserted procedure compatible to isCompatibleWith
		ProcedureSignature procSig = new ProcedureSignature(main, calledFuncCFunction);
		functionSignaturesThatHaveAFunctionPointer.add(procSig); 

//		String procName = calledFuncCFunction.functionSignatureAsProcedureName();
		String procName = procSig.toString();

		CFunction cFuncWithFP = addFPParamToCFunction(calledFuncCFunction);

		if (!procedures.containsKey(procName)) {
			addAProcedure(main, loc, null, procName, cFuncWithFP);
		}

		IASTInitializerClause[] newArgs = new IASTInitializerClause[arguments.length + 1];
		for (int i = 0; i < newArgs.length - 1; i++)
			newArgs[i] = arguments[i];
		newArgs[newArgs.length - 1] = functionName;

		return handleFunctionCallGivenNameAndArguments(main, memoryHandler, structHandler, loc, procName, newArgs);
	}

	/**
	 * takes the contract (we got from CHandler) and translates it into an array
	 * of Boogie specifications (this needs to be called after the procedure
	 * parameters have been added to the symboltable)
	 * 
	 * @param main
	 * @param contract
	 * @param methodName
	 * @return
	 */
	private Specification[] makeBoogieSpecFromACSLContract(Dispatcher main, List<ACSLNode> contract, String methodName) {
		Specification[] spec;
		if (contract == null) {
			spec = new Specification[0];
		} else {
			List<Specification> specList = new ArrayList<Specification>();
			for (int i = 0; i < contract.size(); i++) {
				// retranslate ACSL specification needed e.g., in cases
				// where ids of function parameters differ from is in ACSL
				// expression
				Result retranslateRes = main.dispatch(contract.get(i));
				assert (retranslateRes instanceof ContractResult);
				ContractResult resContr = (ContractResult) retranslateRes;
				specList.addAll(Arrays.asList(resContr.specs));
			}
			spec = specList.toArray(new Specification[0]);
			for (int i = 0; i < spec.length; i++) {
				if (spec[i] instanceof ModifiesSpecification) {
					modifiedGlobalsIsUserDefined.add(methodName);
					ModifiesSpecification ms = (ModifiesSpecification) spec[i];
					LinkedHashSet<String> modifiedSet = new LinkedHashSet<String>();
					for (VariableLHS var : ms.getIdentifiers())
						modifiedSet.add(var.getIdentifier());
					modifiedGlobals.put(methodName, modifiedSet);
				}
			}

			main.cHandler.clearContract(); // take care for behavior and
											// completeness
		}
		return spec;
	}

	/**
	 * Take the parameter information from the CDeclaration. Make a Varlist from
	 * it. Add the parameters to the symboltable. Also update
	 * procedureToParamCType member.
	 * 
	 * @return
	 */
	private VarList[] processInParams(Dispatcher main, ILocation loc, CFunction cFun, String methodName) {
		CDeclaration[] paramDecs = cFun.getParameterTypes();
		VarList[] in = new VarList[paramDecs.length];
		for (int i = 0; i < paramDecs.length; ++i) {
			CDeclaration paramDec = paramDecs[i];

			ASTType type = null;
			if (paramDec.getType() instanceof CArray) {// arrays are passed as
														// pointers in C -- so
														// we pass a Pointer in
														// Boogie
				type = main.typeHandler.constructPointerType(loc);
			} else {
				type = main.typeHandler.ctype2asttype(loc, paramDec.getType());
			}

			String paramId = main.nameHandler.getInParamIdentifier(paramDec.getName(), paramDec.getType());
			in[i] = new VarList(loc, new String[] { paramId }, type);
			main.cHandler.getSymbolTable().put(paramDec.getName(),
					new SymbolTableValue(paramId, null, paramDec, false, null));
		}
		updateCFunction(methodName, null, paramDecs, null, false);
		return in;
	}

	/**
	 * Creates local variables for in parameters.
	 * 
	 * @param main
	 *            a reference to the main dispatcher.
	 * @param loc
	 *            the location
	 * @param decl
	 *            the declaration list to append to.
	 * @param stmt
	 *            the statement list to append to.
	 * @param parent
	 */
	private void handleFunctionsInParams(Dispatcher main, ILocation loc, MemoryHandler memoryHandler,
			ArrayList<Declaration> decl, ArrayList<Statement> stmt, IASTFunctionDefinition parent) {
		VarList[] varListArray = currentProcedure.getInParams();
		IASTParameterDeclaration[] paramDecs;
		if (varListArray.length == 0) {
			/*
			 * In C it is possible to write func(void) { ... } This results in
			 * the empty name. (alex: what is an empty name??)
			 */
			assert ((CASTFunctionDeclarator) parent.getDeclarator()).getParameters().length == 0
					|| (((CASTFunctionDeclarator) parent.getDeclarator()).getParameters().length == 1 && ((CASTFunctionDeclarator) parent
							.getDeclarator()).getParameters()[0].getDeclarator().getName().toString().equals(""));
			paramDecs = new IASTParameterDeclaration[0];
		} else {
			paramDecs = ((CASTFunctionDeclarator) parent.getDeclarator()).getParameters();
		}
		assert varListArray.length == paramDecs.length;
		for (int i = 0; i < paramDecs.length; ++i) {
			VarList varList = varListArray[i];
			IASTParameterDeclaration paramDec = paramDecs[i];
			for (final String bId : varList.getIdentifiers()) {
				final String cId = main.cHandler.getSymbolTable().getCID4BoogieID(bId, loc);

				ASTType type = varList.getType();
				CType cvar = main.cHandler.getSymbolTable().get(cId, loc).getCVariable();

				// onHeap case for a function parameter means the parameter is
				// addressoffed in the function body
				boolean isOnHeap = false;
				if (main instanceof MainDispatcher) //otherwise we are in PreRun mode
						isOnHeap = ((MainDispatcher) main).getVariablesForHeap().contains(paramDec);

				// Copy of inparam that is writeable
				String auxInvar = main.nameHandler.getUniqueIdentifier(parent, cId, 0, isOnHeap, cvar);

				if (isOnHeap || cvar instanceof CArray) {
					type = main.typeHandler.constructPointerType(loc);
					((CHandler) main.cHandler).addBoogieIdsOfHeapVars(auxInvar);
				}
				VarList var = new VarList(loc, new String[] { auxInvar }, type);
				VariableDeclaration inVarDecl = new VariableDeclaration(loc, new Attribute[0], new VarList[] { var });

				VariableLHS tempLHS = new VariableLHS(loc, auxInvar);
				IdentifierExpression rhsId = new IdentifierExpression(loc, bId);

				ILocation igLoc = LocationFactory.createIgnoreLocation(loc);
				if (isOnHeap && !(cvar instanceof CArray)) {//we treat an array argument as a pointer -- thus no onHeap treatment here
					LocalLValue llv = new LocalLValue(tempLHS, cvar);
					// malloc
					memoryHandler.addVariableToBeFreed(main, 
							new LocalLValueILocationPair(llv, igLoc));
					// dereference
					HeapLValue hlv = new HeapLValue(llv.getValue(), cvar);

					ExpressionResult assign = ((CHandler) main.cHandler).makeAssignment(igLoc, stmt, hlv, // convention: if a variable is put on heap or not,
					// its ctype stays the same
					new RValue(rhsId, cvar),
							new ArrayList<Declaration>(), new LinkedHashMap<VariableDeclaration, ILocation>(), new ArrayList<Overapprox>());
					stmt.add(
							memoryHandler.getMallocCall(main, this, llv, igLoc));						
					stmt.addAll(assign.stmt);
				} else {
					stmt.add(new AssignmentStatement(igLoc, new LeftHandSide[] { tempLHS }, new Expression[] { rhsId }));
				}
				assert main.cHandler.getSymbolTable().containsCSymbol(cId);
				// Overwrite the information in the symbolTable for cId, s.t. it
				// points to the locally declared variable.
				main.cHandler.getSymbolTable().put(
						cId,
						new SymbolTableValue(auxInvar, inVarDecl, new CDeclaration(cvar, cId), false, paramDec));
			}
		}
	}

	/**
	 * Update the map procedureToCFunctionType according to the given arguments
	 * If a parameter is null, the corresponding value will not be changed. (for
	 * takesVarArgs, use "false" to change nothing).
	 */
	private void updateCFunction(String methodName, CType returnType, CDeclaration[] allParamDecs,
			CDeclaration oneParamDec, boolean takesVarArgs) {
		CFunction oldCFunction = procedureToCFunctionType.get(methodName);

		CType oldRetType = oldCFunction == null ? null : oldCFunction.getResultType();
		CDeclaration[] oldInParams = oldCFunction == null ? new CDeclaration[0] : oldCFunction.getParameterTypes();
		boolean oldTakesVarArgs = oldCFunction == null ? false : oldCFunction.takesVarArgs();

		CType newRetType = oldRetType;
		CDeclaration[] newInParams = oldInParams;
		boolean newTakesVarArgs = oldTakesVarArgs || takesVarArgs;

		if (allParamDecs != null) { // set a new parameter list
			assert oneParamDec == null;
			newInParams = allParamDecs;
		} else if (oneParamDec != null) { // add a parameter to the list
			assert allParamDecs == null;

			ArrayList<CDeclaration> ips = new ArrayList<>(Arrays.asList(oldInParams));
			ips.add(oneParamDec);
			newInParams = ips.toArray(new CDeclaration[ips.size()]);
		}
		if (returnType != null) {
			newRetType = returnType;
		}

		procedureToCFunctionType.put(methodName, new CFunction(newRetType, newInParams, newTakesVarArgs));
	}

	/**
	 * Add a procedure to procedures according to a given CFunction. I.e. do a
	 * procedure declaration.
	 */
	private void addAProcedure(Dispatcher main, ILocation loc, List<ACSLNode> contract, String methodName,
			CFunction funcType) {
		// begin new scope for retranslation of ACSL specification
		main.cHandler.beginScope();

		VarList[] in = processInParams(main, loc, funcType, methodName);

		// OUT VARLIST : only one out param in C
		VarList[] out = new VarList[1];

		Attribute[] attr = new Attribute[0];
		String[] typeParams = new String[0];
		Specification[] spec = makeBoogieSpecFromACSLContract(main, contract, methodName);

		if (funcType.getResultType() instanceof CPrimitive
				&& ((CPrimitive) funcType.getResultType()).getType() == PRIMITIVE.VOID
				&& !(funcType.getResultType() instanceof CPointer)) {
			if (methodsCalledBeforeDeclared.contains(methodName)) {
				// this method was assumed to return int -> return int
				out[0] = new VarList(loc, new String[] { SFO.RES }, new PrimitiveType(loc, SFO.INT));
			} else {
				// void, so there are no out vars
				out = new VarList[0];
			}
		} else {
			// we found a type, so node is type ASTType
			ASTType type = main.typeHandler.ctype2asttype(loc, funcType.getResultType());
			out[0] = new VarList(loc, new String[] { SFO.RES }, type);
		}
		if (!modifiedGlobals.containsKey(methodName)) {
			modifiedGlobals.put(methodName, new LinkedHashSet<String>());
		}
		if (!callGraph.containsKey(methodName)) {
			callGraph.put(methodName, new LinkedHashSet<String>());
		}

		Procedure proc = procedures.get(methodName);
		if (proc != null) {
			// combine the specification from the definition with the one from
			// the declaration
			List<Specification> specFromDef = Arrays.asList(proc.getSpecification());
			ArrayList<Specification> newSpecs = new ArrayList<Specification>(Arrays.asList(spec));
			newSpecs.addAll(specFromDef);
			spec = newSpecs.toArray(new Specification[0]);
			// TODO something else to take over for a declaration after the
			// definition?
		}
		proc = new Procedure(loc, attr, methodName, typeParams, in, out, spec, null);

		procedures.put(methodName, proc);
		updateCFunction(methodName, funcType.getResultType(), null, null, funcType.takesVarArgs());
		// end scope for retranslation of ACSL specification
		main.cHandler.endScope();
	}

	/**
	 * Adds searchString to modifiedGlobals iff searchString is a global
	 * variable and the user has not defined a modifies clause.
	 * 
	 * @param main
	 *            a reference to the main dispatcher.
	 * 
	 * @param searchString
	 *            = boogieVarName!
	 * @param errLoc
	 *            the location for possible errors!
	 */
	public void checkIfModifiedGlobal(SymbolTable symbTab, String searchString, ILocation errLoc) {
		String cName;
		if (!symbTab.containsBoogieSymbol(searchString)) {
			return; // temp variable!
		}
		cName = symbTab.getCID4BoogieID(searchString, errLoc);
		String cId = currentProcedure.getIdentifier();
		SymbolTableValue stValue = symbTab.get(cName, errLoc);
		CType cvar = stValue.getCVariable();
		if (cvar != null && stValue.getCDecl().isStatic()) {
			modifiedGlobals.get(cId).add(searchString);
			return;
		}
		if (modifiedGlobalsIsUserDefined.contains(cId))
			return;
		boolean isLocal = false;
		if (searchString.equals(SFO.RES)) {
			// this variable is reserved for the return variable and
			// therefore local!
			isLocal = true;
		} else {
			isLocal = !symbTab.get(cName, errLoc).isBoogieGlobalVar();
		}
		if (!isLocal) {
			// the variable is not local but could be a formal parameter
			if (!searchString.startsWith(SFO.IN_PARAM)) { // variable is global!
				modifiedGlobals.get(cId).add(searchString);
			} else {
				assert false;
			}
		}
	}

	/**
	 * Checks, whether all procedures that are being called in C, were
	 * eventually declared within the C program.
	 * 
	 * @return null if all called procedures were declared, otherwise the
	 *         identifier of one procedure that was called but not declared.
	 */
	public String isEveryCalledProcedureDeclared() {
		for (String s : methodsCalledBeforeDeclared) {
			if (!procedures.containsKey(s)) {
				return s;
			}
		}
		return null;
	}

	void beginUltimateInit(Dispatcher main, ILocation loc, String startOrInit) {
		main.cHandler.beginScope();
		callGraph.put(startOrInit, new LinkedHashSet<String>());
		currentProcedure = new Procedure(loc, new Attribute[0], startOrInit, new String[0], new VarList[0],
				new VarList[0], new Specification[0], null);
		procedures.put(startOrInit, currentProcedure);
		modifiedGlobals.put(currentProcedure.getIdentifier(), new LinkedHashSet<String>());
	}

	void endUltimateInit(Dispatcher main, Procedure initDecl, String startOrInit) {
		procedures.put(startOrInit, initDecl);
		main.cHandler.endScope();
	}

	public CFunction addFPParamToCFunction(CFunction calledFuncCFunction) {
		CDeclaration[] newCDecs = new CDeclaration[calledFuncCFunction.getParameterTypes().length + 1];
		for (int i = 0; i < newCDecs.length - 1; i++)
			newCDecs[i] = calledFuncCFunction.getParameterTypes()[i];
		newCDecs[newCDecs.length - 1] = new CDeclaration(new CPointer(new CPrimitive(PRIMITIVE.VOID)), "#fp"); // FIXME
																												// string
																												// to
																												// SFO..?
		CFunction cFuncWithFP = new CFunction(calledFuncCFunction.getResultType(), newCDecs, false);
		return cFuncWithFP;
	}

	public Body getFunctionPointerFunctionBody(ILocation loc, Dispatcher main, MemoryHandler memoryHandler,
			//StructHandler structHandler, String fpfName, CFunction funcSignature, VarList[] inParams, VarList[] outParam) {
			StructHandler structHandler, String fpfName, ProcedureSignature funcSignature, VarList[] inParams, VarList[] outParam) {
		//CFunction calledFuncType = funcSignature;

//		boolean resultTypeIsVoid = calledFuncType.getResultType() instanceof CPrimitive
//				&& ((CPrimitive) calledFuncType.getResultType()).getType() == PRIMITIVE.VOID;
		boolean resultTypeIsVoid = funcSignature.returnType == null;
				
		ArrayList<Statement> stmt = new ArrayList<>();
		ArrayList<VariableDeclaration> decl = new ArrayList<>();

		ArrayList<Expression> args = new ArrayList<>();
		for (int i = 0; i < inParams.length - 1; i++) {// the last inParam is
														// the function pointer
														// -> therefore
														// "..length - 1"
			VarList vl = inParams[i];
			assert vl.getIdentifiers().length == 1;
			String oldId = vl.getIdentifiers()[0];
			String newId = oldId.replaceFirst("in", "");
			decl.add(new VariableDeclaration(loc, new Attribute[0], new VarList[] { new VarList(loc,
					new String[] { newId }, vl.getType()) }));
			stmt.add(new AssignmentStatement(loc, new LeftHandSide[] { new VariableLHS(loc, newId) },
					new Expression[] { new IdentifierExpression(loc, oldId) }));
			args.add(new IdentifierExpression(loc, newId));
		}

		// collect all functions that are addressoffed in the program and that
		// match the signature
		ArrayList<String> fittingFunctions = new ArrayList<>();
		for (Entry<String, Integer> en : ((Dispatcher) main).getFunctionToIndex().entrySet()) {
			CFunction ptdToFuncType = procedureToCFunctionType.get(en.getKey());
//			if (ptdToFuncType.isCompatibleWith(calledFuncType)) {
			if (new ProcedureSignature(main, ptdToFuncType).equals(funcSignature)) {
				fittingFunctions.add(en.getKey());
			}
		}

		// add the functionPointerProcedure and the procedures it calls to the
		// call graph and modifiedGlobals
		// such that calculateTransitive (which is executed later, in
		// visit(TranslationUnit) after the postprocessor)
		// can compute the correct modifies clause
		modifiedGlobals.put(fpfName, new LinkedHashSet<String>());
		callGraph.get(fpfName).addAll(fittingFunctions);

		// generate the actual body
		IdentifierExpression funcCallResult = null;
		if (fittingFunctions.size() == 0) {
			return new Body(loc, decl.toArray(new VariableDeclaration[decl.size()]), stmt.toArray(new Statement[stmt
					.size()]));		
		} else if (fittingFunctions.size() == 1) {
			ExpressionResult rex = (ExpressionResult) makeTheFunctionCallItself(main, loc, fittingFunctions.get(0),
					new ArrayList<Statement>(), new ArrayList<Declaration>(),
					new LinkedHashMap<VariableDeclaration, ILocation>(), new ArrayList<Overapprox>(), args);
			funcCallResult = (IdentifierExpression) rex.lrVal.getValue();
			for (Declaration dec : rex.decl)
				decl.add((VariableDeclaration) dec);

			stmt.addAll(rex.stmt);
			if (outParam.length == 1) {
				stmt.add(new AssignmentStatement(loc, new LeftHandSide[] { new VariableLHS(loc, outParam[0]
						.getIdentifiers()[0]) }, new Expression[] { funcCallResult }));
			}
			stmt.addAll(CHandler.createHavocsForAuxVars(rex.auxVars));
			stmt.add(new ReturnStatement(loc));
			return new Body(loc, decl.toArray(new VariableDeclaration[decl.size()]), stmt.toArray(new Statement[stmt
					.size()]));
		} else {
			Map<VariableDeclaration, ILocation> auxVars = new LinkedHashMap<>();

			String tmpId = null;

			if (!resultTypeIsVoid) {
				tmpId = main.nameHandler.getTempVarUID(SFO.AUXVAR.FUNCPTRRES, null);
				VariableDeclaration tmpVarDec = new VariableDeclaration(loc, new Attribute[0],
						new VarList[] { new VarList(loc, new String[] { tmpId }, 
								//main.typeHandler.ctype2asttype(loc,
//								((CFunction) calledFuncType).getResultType())) });
								funcSignature.returnType )});
				decl.add(tmpVarDec);
				auxVars.put(tmpVarDec, loc);
				funcCallResult = new IdentifierExpression(loc, tmpId);
			}

			ExpressionResult firstElseRex = (ExpressionResult) makeTheFunctionCallItself(main, loc,
					fittingFunctions.get(0), new ArrayList<Statement>(), new ArrayList<Declaration>(),
					new LinkedHashMap<VariableDeclaration, ILocation>(), new ArrayList<Overapprox>(), args);
			for (Declaration dec : firstElseRex.decl)
				decl.add((VariableDeclaration) dec);
			auxVars.putAll(firstElseRex.auxVars);

			ArrayList<Statement> firstElseStmt = new ArrayList<>();
			firstElseStmt.addAll(firstElseRex.stmt);
			if (!resultTypeIsVoid) {
				AssignmentStatement assignment = new AssignmentStatement(loc, new VariableLHS[] { new VariableLHS(loc,
						tmpId) }, new Expression[] { firstElseRex.lrVal.getValue() });
				firstElseStmt.add(assignment);
			}
			IfStatement currentIfStmt = null;

			for (int i = 1; i < fittingFunctions.size(); i++) {
				ExpressionResult currentRex = (ExpressionResult) makeTheFunctionCallItself(main, loc,
						fittingFunctions.get(i), new ArrayList<Statement>(), new ArrayList<Declaration>(),
						new LinkedHashMap<VariableDeclaration, ILocation>(), new ArrayList<Overapprox>(), args);
				for (Declaration dec : currentRex.decl)
					decl.add((VariableDeclaration) dec);
				auxVars.putAll(currentRex.auxVars);

				ArrayList<Statement> newStmts = new ArrayList<>();
				newStmts.addAll(currentRex.stmt);
				if (!resultTypeIsVoid) {
					AssignmentStatement assignment = new AssignmentStatement(loc, new VariableLHS[] { new VariableLHS(
							loc, tmpId) }, new Expression[] { currentRex.lrVal.getValue() });
					newStmts.add(assignment);
				}

				Expression condition = ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.COMPEQ,
						new IdentifierExpression(loc, inParams[inParams.length - 1].getIdentifiers()[0]),
						new IdentifierExpression(loc, SFO.FUNCTION_ADDRESS + fittingFunctions.get(i)));

				if (i == 1) {
					currentIfStmt = new IfStatement(loc, condition, newStmts.toArray(new Statement[newStmts.size()]),
							firstElseStmt.toArray(new Statement[firstElseStmt.size()]));
				} else {
					currentIfStmt = new IfStatement(loc, condition, newStmts.toArray(new Statement[newStmts.size()]),
							new Statement[] { currentIfStmt });
				}
			}

			stmt.add(currentIfStmt);
			if (outParam.length == 1) {
				stmt.add(new AssignmentStatement(loc, new LeftHandSide[] { new VariableLHS(loc, outParam[0]
						.getIdentifiers()[0]) }, new Expression[] { funcCallResult }));
			}
			stmt.addAll(CHandler.createHavocsForAuxVars(auxVars));
			stmt.add(new ReturnStatement(loc));
			return new Body(loc, decl.toArray(new VariableDeclaration[decl.size()]), stmt.toArray(new Statement[stmt
					.size()]));
		}
	}

	public Result makeTheFunctionCallItself(Dispatcher main, ILocation loc, String methodName,
			ArrayList<Statement> stmt, ArrayList<Declaration> decl, Map<VariableDeclaration, ILocation> auxVars,
			ArrayList<Overapprox> overappr, ArrayList<Expression> args) {
		Expression expr = null;
		Statement call;
		if (procedures.containsKey(methodName)) {
			VarList[] type = procedures.get(methodName).getOutParams();
			if (type.length == 0) { // void
				// C has only one return statement -> no need for forall
				call = new CallStatement(loc, false, new VariableLHS[0], methodName, args.toArray(new Expression[0]));
			} else if (type.length == 1) { // one return value
				String tmpId = main.nameHandler.getTempVarUID(SFO.AUXVAR.RETURNED, null);
				expr = new IdentifierExpression(loc, tmpId);
				VariableDeclaration tmpVar = SFO.getTempVarVariableDeclaration(tmpId, type[0].getType(), loc);
				auxVars.put(tmpVar, loc);
				decl.add(tmpVar);
				VariableLHS tmpLhs = new VariableLHS(loc, tmpId);
				call = new CallStatement(loc, false, new VariableLHS[] { tmpLhs }, methodName,
						args.toArray(new Expression[0]));
			} else { // unsupported!
			// String msg = "Cannot handle multiple out params! "
			// + loc.toString();
				// throw new IncorrectSyntaxException(loc, msg);
				return null; // FIXME ..
			}
		} else {
			methodsCalledBeforeDeclared.add(methodName);
			String longDescription = "Return value of method '" + methodName
					+ "' unknown! Methods should be declared, before they are used! Return value assumed to be int ...";
			main.warn(loc, longDescription);
			String ident = main.nameHandler.getTempVarUID(SFO.AUXVAR.RETURNED, null);
			expr = new IdentifierExpression(loc, ident);
			
			// we don't know the CType of the returned value 
			// we we INT
			CPrimitive cPrimitive = new CPrimitive(PRIMITIVE.INT);
			VarList tempVar = new VarList(loc, new String[] { ident }, main.typeHandler.ctype2asttype(loc, cPrimitive));
			VariableDeclaration tmpVar = new VariableDeclaration(loc, new Attribute[0], new VarList[] { tempVar });
			auxVars.put(tmpVar, loc);
			decl.add(tmpVar);
			VariableLHS lhs = new VariableLHS(loc, ident);
			call = new CallStatement(loc, false, new VariableLHS[] { lhs }, methodName, args.toArray(new Expression[0]));
		}
		stmt.add(call);
		CType returnCType = methodsCalledBeforeDeclared.contains(methodName) ? new CPrimitive(PRIMITIVE.INT)
				: procedureToCFunctionType.get(methodName).getResultType().getUnderlyingType();
		m_ExpressionTranslation.addAssumeValueInRangeStatements(loc, expr, returnCType, stmt);
		assert (CHandler.isAuxVarMapcomplete(main.nameHandler, decl, auxVars));
		return new ExpressionResult(stmt, new RValue(expr, returnCType), decl, auxVars, overappr);
	}

	/**
	 * Checks a VarList for a specific pattern, that represents "void".
	 * 
	 * @param in
	 *            the methods in-parameter list.
	 * @return true iff in represents void.
	 */
	private static final boolean isInParamVoid(VarList[] in) {
		if (in.length > 0 && in[0] == null)
			throw new IllegalArgumentException("In-param cannot be null!");
		// convention (necessary probably only because of here):
		// typeHandler.ctype2boogietype yields "null" for
		// CPrimitive(PRIMITIVE.VOID)
		return (in.length == 1 && in[0].getType() == null);
	}

	/**
	 * Getter for modified globals.
	 * 
	 * @return modified globals.
	 */
	public LinkedHashMap<String, LinkedHashSet<String>> getModifiedGlobals() {
		return this.modifiedGlobals;
	}

	/**
	 * Getter for procedures.
	 * 
	 * @return procedures.
	 */
	public LinkedHashMap<String, Procedure> getProcedures() {
		return this.procedures;
	}

	/**
	 * Returns the identifier of the current procedure.
	 * 
	 * @return the identifier of the current procedure.
	 */
	public String getCurrentProcedureID() {
		if (currentProcedure == null)
			return null;
		else
			return this.currentProcedure.getIdentifier();
	}

	public boolean noCurrentProcedure() {
		return this.currentProcedure == null;
	}

	/**
	 * Getter for the call graph.
	 * 
	 * @return the call graph.
	 */
	public LinkedHashMap<String, LinkedHashSet<String>> getCallGraph() {
		return this.callGraph;
	}
	
	public CFunction getCFunctionType(String function) {
		return this.procedureToCFunctionType.get(function);
	}

}
