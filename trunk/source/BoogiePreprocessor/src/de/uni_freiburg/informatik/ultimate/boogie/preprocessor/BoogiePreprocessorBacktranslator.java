/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE BoogiePreprocessor plug-in.
 * 
 * The ULTIMATE BoogiePreprocessor plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE BoogiePreprocessor plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE BoogiePreprocessor plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE BoogiePreprocessor plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE BoogiePreprocessor plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.boogie.preprocessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.boogie.BoogieProgramExecution;
import de.uni_freiburg.informatik.ultimate.boogie.BoogieTransformer;
import de.uni_freiburg.informatik.ultimate.boogie.DeclarationInformation.StorageClass;
import de.uni_freiburg.informatik.ultimate.boogie.ast.AssumeStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BoogieASTNode;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BooleanLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.CallStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Declaration;
import de.uni_freiburg.informatik.ultimate.boogie.ast.EnsuresSpecification;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IdentifierExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IfStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.LoopInvariantSpecification;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Procedure;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.UnaryExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.UnaryExpression.Operator;
import de.uni_freiburg.informatik.ultimate.boogie.ast.VarList;
import de.uni_freiburg.informatik.ultimate.boogie.ast.VariableDeclaration;
import de.uni_freiburg.informatik.ultimate.boogie.ast.WhileStatement;
import de.uni_freiburg.informatik.ultimate.boogie.output.BoogiePrettyPrinter;
import de.uni_freiburg.informatik.ultimate.boogie.symboltable.BoogieSymbolTable;
import de.uni_freiburg.informatik.ultimate.boogie.type.BoogieType;
import de.uni_freiburg.informatik.ultimate.boogie.type.ConstructedType;
import de.uni_freiburg.informatik.ultimate.boogie.type.PrimitiveType;
import de.uni_freiburg.informatik.ultimate.boogie.type.StructType;
import de.uni_freiburg.informatik.ultimate.core.lib.models.Multigraph;
import de.uni_freiburg.informatik.ultimate.core.lib.models.MultigraphEdge;
import de.uni_freiburg.informatik.ultimate.core.lib.models.annotation.ConditionAnnotation;
import de.uni_freiburg.informatik.ultimate.core.lib.results.GenericResult;
import de.uni_freiburg.informatik.ultimate.core.lib.translation.DefaultTranslator;
import de.uni_freiburg.informatik.ultimate.core.model.models.IExplicitEdgesMultigraph;
import de.uni_freiburg.informatik.ultimate.core.model.models.ILocation;
import de.uni_freiburg.informatik.ultimate.core.model.models.IMultigraphEdge;
import de.uni_freiburg.informatik.ultimate.core.model.models.IType;
import de.uni_freiburg.informatik.ultimate.core.model.results.IResultWithSeverity.Severity;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.core.model.translation.AtomicTraceElement;
import de.uni_freiburg.informatik.ultimate.core.model.translation.AtomicTraceElement.StepInfo;
import de.uni_freiburg.informatik.ultimate.core.model.translation.IBacktranslatedCFG;
import de.uni_freiburg.informatik.ultimate.core.model.translation.IProgramExecution;
import de.uni_freiburg.informatik.ultimate.core.model.translation.IProgramExecution.ProgramState;
import de.uni_freiburg.informatik.ultimate.core.model.translation.IToString;

/**
 * 
 * @author dietsch@informatik.uni-freiburg.de
 * 
 */
public class BoogiePreprocessorBacktranslator
		extends DefaultTranslator<BoogieASTNode, BoogieASTNode, Expression, Expression> {

	private final ILogger mLogger;
	/**
	 * Mapping from target nodes to source nodes (i.e. output to input)
	 */
	private final HashMap<BoogieASTNode, BoogieASTNode> mMapping;
	private final IUltimateServiceProvider mServices;
	private BoogieSymbolTable mSymbolTable;

	BoogiePreprocessorBacktranslator(IUltimateServiceProvider services) {
		super(BoogieASTNode.class, BoogieASTNode.class, Expression.class, Expression.class);
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(Activator.PLUGIN_ID);
		mMapping = new HashMap<>();
	}

	public BoogieSymbolTable getSymbolTable() {
		return mSymbolTable;
	}

	public void setSymbolTable(BoogieSymbolTable symbolTable) {
		mSymbolTable = symbolTable;
	}

	void addMapping(BoogieASTNode inputNode, BoogieASTNode outputNode) {
		BoogieASTNode realInputNode = mMapping.get(inputNode);
		if (realInputNode == null) {
			realInputNode = inputNode;
		}
		mMapping.put(outputNode, realInputNode);
		if (mLogger.isDebugEnabled()) {
			mLogger.debug("Create mapping between");
			mLogger.debug("\tOutput " + printDebug(outputNode));
			mLogger.debug("\tInput  " + printDebug(realInputNode));
		}
	}

	@Override
	public IProgramExecution<BoogieASTNode, Expression>
			translateProgramExecution(final IProgramExecution<BoogieASTNode, Expression> programExecution) {

		final List<BoogieASTNode> newTrace = new ArrayList<>();
		final List<ProgramState<Expression>> newProgramStates = new ArrayList<>();

		final ProgramState<Expression> newInitialState =
				backtranslateProgramState(programExecution.getInitialProgramState());
		final int length = programExecution.getLength();
		for (int i = 0; i < length; ++i) {
			final BoogieASTNode elem = programExecution.getTraceElement(i).getTraceElement();
			// the call to backtranslateTraceElements may produce null values,
			// but we keep them anyways s.t. the indices between newTrace and
			// programExecution match
			newTrace.add(backtranslateTraceElement(elem));
			newProgramStates.add(backtranslateProgramState(programExecution.getProgramState(i)));
		}
		return createProgramExecutionFromTrace(newTrace, newInitialState, newProgramStates, programExecution);
	}

	private ProgramState<Expression> backtranslateProgramState(ProgramState<Expression> state) {
		if (state == null) {
			return null;
		} else {
			final Map<Expression, Collection<Expression>> newVariable2Values = new HashMap<>();
			for (final Expression var : state.getVariables()) {
				final Expression newVar = translateExpression(var);
				final Collection<Expression> newValues = new ArrayList<>();
				for (final Expression value : state.getValues(var)) {
					newValues.add(translateExpression(value));
				}
				newVariable2Values.put(newVar, newValues);
			}
			return new ProgramState<>(newVariable2Values);
		}
	}

	private BoogieASTNode backtranslateTraceElement(BoogieASTNode elem) {
		final BoogieASTNode newElem = mMapping.get(elem);

		if (newElem == null) {
			if (elem instanceof EnsuresSpecification) {
				final EnsuresSpecification spec = (EnsuresSpecification) elem;
				final Expression formula = spec.getFormula();
				if (formula instanceof BooleanLiteral && ((BooleanLiteral) formula).getValue()) {
					// this EnuresSpecification was inserted by RCFG Builder and
					// does not provide any additional information. We exclude it from the error path.
					return null;
				}
				reportUnfinishedBacktranslation(
						"Generated EnsuresSpecification " + BoogiePrettyPrinter.print(spec) + " is not ensure(true)");
				return null;
			}
			// if there is no mapping, we return the identity (we do not change
			// everything, so this may be right)
			return elem;
		} else if (newElem instanceof Statement || newElem instanceof LoopInvariantSpecification) {
			return newElem;
		} else {
			reportUnfinishedBacktranslation(
					"Unfinished backtranslation: Ignored translation of " + newElem.getClass().getSimpleName());
			return null;
		}

	}

	private IProgramExecution<BoogieASTNode, Expression> createProgramExecutionFromTrace(
			List<BoogieASTNode> translatedTrace, ProgramState<Expression> newInitialState,
			List<ProgramState<Expression>> newProgramStates,
			IProgramExecution<BoogieASTNode, Expression> programExecution) {

		final List<AtomicTraceElement<BoogieASTNode>> atomicTrace = new ArrayList<>();
		final IToString<BoogieASTNode> stringProvider = BoogiePrettyPrinter.getBoogieToStringprovider();

		for (int i = 0; i < translatedTrace.size(); ++i) {
			final BoogieASTNode elem = translatedTrace.get(i);

			if (elem == null) {
				// we kept the null values so that indices match between trace
				// and inputProgramExecution
				atomicTrace.add(null);
				continue;
			}

			final AtomicTraceElement<BoogieASTNode> ate = programExecution.getTraceElement(i);

			if (elem instanceof WhileStatement) {
				final AssumeStatement assumeStmt = (AssumeStatement) ate.getTraceElement();
				final WhileStatement stmt = (WhileStatement) elem;
				final StepInfo info = getStepInfoFromCondition(assumeStmt.getFormula(), stmt.getCondition());
				atomicTrace.add(new AtomicTraceElement<BoogieASTNode>(stmt, stmt.getCondition(), info, stringProvider,
						ate.getRelevanceInformation()));

			} else if (elem instanceof IfStatement) {
				final AssumeStatement assumeStmt = (AssumeStatement) ate.getTraceElement();
				final IfStatement stmt = (IfStatement) elem;
				final StepInfo info = getStepInfoFromCondition(assumeStmt.getFormula(), stmt.getCondition());
				atomicTrace.add(new AtomicTraceElement<BoogieASTNode>(stmt, stmt.getCondition(), info, stringProvider,
						ate.getRelevanceInformation()));

			} else if (elem instanceof CallStatement) {
				// for call statements, we simply rely on the stepinfo of our
				// input: if its none, its a function call (so there will be no
				// return), else its a procedure call with corresponding return

				if (ate.hasStepInfo(StepInfo.NONE)) {
					atomicTrace.add(new AtomicTraceElement<BoogieASTNode>(elem, elem, StepInfo.FUNC_CALL,
							stringProvider, ate.getRelevanceInformation()));
				} else {
					atomicTrace.add(new AtomicTraceElement<BoogieASTNode>(elem, elem, ate.getStepInfo(), stringProvider,
							ate.getRelevanceInformation()));
				}

			} else {
				// it could be that we missed some cases... revisit this if you
				// suspect errors in the backtranslation
				atomicTrace.add(
						new AtomicTraceElement<BoogieASTNode>(elem, stringProvider, ate.getRelevanceInformation()));
			}
		}

		// we need to clear the null values before creating the final
		// BoogieProgramExecution
		final List<AtomicTraceElement<BoogieASTNode>> actualAtomicTrace = new ArrayList<>();
		final Map<Integer, ProgramState<Expression>> partialProgramStateMapping = new HashMap<>();
		partialProgramStateMapping.put(-1, newInitialState);
		int i = 0;
		int j = 0;
		for (final AtomicTraceElement<BoogieASTNode> possibleNullElem : atomicTrace) {
			if (possibleNullElem != null) {
				actualAtomicTrace.add(possibleNullElem);
				partialProgramStateMapping.put(j, newProgramStates.get(i));
				j++;
			}
			i++;
		}
		return new BoogieProgramExecution(partialProgramStateMapping, actualAtomicTrace);
	}

	private StepInfo getStepInfoFromCondition(Expression input, Expression output) {
		// compare the depth of UnaryExpression in the condition of the assume
		// and the condition of the mapped conditional to determine if the
		// condition
		// evaluated to true or to false
		if (!(input instanceof UnaryExpression)) {
			// it is not even an unary expression, it surely evaluates to true
			return StepInfo.CONDITION_EVAL_TRUE;
		} else {
			final UnaryExpression inputCond = (UnaryExpression) input;
			if (inputCond.getOperator() != Operator.LOGICNEG) {
				// it is an unaryCond, but its no negation, so it must be true
				return StepInfo.CONDITION_EVAL_TRUE;
			}
			// now it gets interesting: it is a negation, but is the real
			// condition also a negation?

			if (!(output instanceof UnaryExpression)) {
				// nope, so that means it is false
				return StepInfo.CONDITION_EVAL_FALSE;
			} else {
				final UnaryExpression outputCond = (UnaryExpression) output;
				if (inputCond.getOperator() != Operator.LOGICNEG) {
					// it is an unaryCond, but its no negation, so it must be
					// false
					return StepInfo.CONDITION_EVAL_FALSE;
				} else {
					// both have outer unary expressions that are logicneg.
					// now we recurse, because we already stripped the outer
					// negations
					return getStepInfoFromCondition(inputCond.getExpr(), outputCond.getExpr());
				}
			}
		}
	}

	@Override
	public List<BoogieASTNode> translateTrace(List<BoogieASTNode> trace) {
		return super.translateTrace(trace);
	}

	@Override
	public Expression translateExpression(Expression expression) {
		return new ExpressionTranslator().processExpression(expression);
	}

	@Override
	public String targetExpressionToString(Expression expression) {
		return BoogiePrettyPrinter.print(expression);
	}

	@Override
	public List<String> targetTraceToString(List<BoogieASTNode> trace) {
		final List<String> rtr = new ArrayList<>();
		for (final BoogieASTNode node : trace) {
			if (node instanceof Statement) {
				rtr.add(BoogiePrettyPrinter.print((Statement) node));
			} else {
				return super.targetTraceToString(trace);
			}
		}
		return rtr;
	}

	@Override
	public IBacktranslatedCFG<?, BoogieASTNode> translateCFG(final IBacktranslatedCFG<?, BoogieASTNode> cfg) {
		return translateCFG(cfg, (a, b, c) -> translateCFGEdge(a, b, c));
	}

	private <VL> Multigraph<VL, BoogieASTNode> translateCFGEdge(
			final Map<IExplicitEdgesMultigraph<?, ?, VL, BoogieASTNode, ?>, Multigraph<VL, BoogieASTNode>> cache,
			final IMultigraphEdge<?, ?, VL, BoogieASTNode, ?> oldEdge,
			final Multigraph<VL, BoogieASTNode> newSourceNode) {
		final BoogieASTNode newLabel = backtranslateTraceElement(oldEdge.getLabel());
		final IExplicitEdgesMultigraph<?, ?, VL, BoogieASTNode, ?> oldTarget = oldEdge.getTarget();
		Multigraph<VL, BoogieASTNode> newTarget = cache.get(oldTarget);
		if (newTarget == null) {
			newTarget = createWitnessNode(oldTarget);
			cache.put(oldTarget, newTarget);
		}
		final MultigraphEdge<VL, BoogieASTNode> newEdge =
				new MultigraphEdge<VL, BoogieASTNode>(newSourceNode, newLabel, newTarget);
		final ConditionAnnotation coan = ConditionAnnotation.getAnnotation(oldEdge.getLabel());
		if (coan != null) {
			coan.annotate(newEdge);
		}
		return newTarget;
	}

	private void reportUnfinishedBacktranslation(String message) {
		mLogger.warn(message);
		mServices.getResultService().reportResult(Activator.PLUGIN_ID,
				new GenericResult(Activator.PLUGIN_ID, "Unfinished Backtranslation", message, Severity.WARNING));
	}

	private String printDebug(BoogieASTNode node) {
		if (node instanceof Statement) {
			return BoogiePrettyPrinter.print((Statement) node);
		}

		if (node instanceof Expression) {
			return BoogiePrettyPrinter.print((Expression) node);
		}

		if (node instanceof Procedure) {
			return BoogiePrettyPrinter.printSignature((Procedure) node);
		}

		if (node instanceof VarList) {
			return BoogiePrettyPrinter.print((VarList) node);
		}

		final StringBuilder output = new StringBuilder();
		output.append(node.getClass().getSimpleName());
		final ILocation loc = node.getLocation();

		if (loc != null) {
			final int start = loc.getStartLine();
			final int end = loc.getEndLine();

			output.append(" L");
			output.append(start);
			if (start != end) {
				output.append(":");
				output.append(end);
			}

			final int startC = loc.getStartColumn();
			final int endC = loc.getEndColumn();
			output.append(" C");
			output.append(startC);

			if (startC != endC) {
				output.append(":");
				output.append(endC);
			}
		}
		return output.toString();
	}

	/**
	 * Backtranslate arbitrary Boogie expressions
	 * 
	 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
	 */
	private class ExpressionTranslator extends BoogieTransformer {

		@Override
		protected Expression processExpression(Expression expr) {
			if (mSymbolTable == null) {
				reportUnfinishedBacktranslation(
						"No symboltable available, using identity as back-translation of " + expr);
				return expr;
			}

			if (!(expr instanceof IdentifierExpression)) {
				// we only have to translate identifier expressions; so we just descend (using BoogieTransformer)
				return super.processExpression(expr);
			}

			final IdentifierExpression ident = (IdentifierExpression) expr;
			if (ident.getDeclarationInformation() == null) {
				reportUnfinishedBacktranslation("Identifier has no declaration information, "
						+ "using identity as back-translation of " + expr);
				return expr;
			}

			if (ident.getDeclarationInformation().getStorageClass() == StorageClass.QUANTIFIED) {
				reportUnfinishedBacktranslation(
						"Identifier is quantified, " + "using identity as back-translation of " + expr);
				return expr;
			}

			final Declaration decl = mSymbolTable.getDeclaration(ident);
			if (decl == null) {
				reportUnfinishedBacktranslation(
						"No declaration in symboltable, using identity as " + "back-translation of " + expr);
				return expr;
			}

			final BoogieASTNode newDecl = getMapping(decl);
			if (newDecl instanceof Declaration) {
				return extractIdentifier((Declaration) newDecl, ident);
			} else if (newDecl instanceof VarList) {
				return extractIdentifier(newDecl.getLocation(), (VarList) newDecl, ident);
			} else {
				// this is ok, the expression was not changed during preprocessing
				return expr;
			}

		}

		private BoogieASTNode getMapping(final BoogieASTNode decl) {
			BoogieASTNode newDecl = mMapping.get(decl);
			if (newDecl != null) {
				return newDecl;
			} else if (decl instanceof VariableDeclaration) {
				// it could be some kind of pointer type
				final VariableDeclaration varDecl = (VariableDeclaration) decl;
				for (final VarList vl : varDecl.getVariables()) {
					newDecl = getMapping(vl);
					if (newDecl != null) {
						return newDecl;
					}
				}
			}
			return null;
		}

		private IdentifierExpression extractIdentifier(final Declaration mappedDecl,
				final IdentifierExpression inputExp) {
			IdentifierExpression rtr = inputExp;
			if (mappedDecl instanceof VariableDeclaration) {
				final VariableDeclaration mappedVarDecl = (VariableDeclaration) mappedDecl;
				rtr = extractIdentifier(mappedVarDecl.getLocation(), mappedVarDecl.getVariables(), inputExp);
				if (rtr != inputExp) {
					return rtr;
				}
				reportUnfinishedBacktranslation("Unfinished backtranslation: Name guessing unsuccessful for VarDecl "
						+ BoogiePrettyPrinter.print(mappedVarDecl) + " and expression "
						+ BoogiePrettyPrinter.print(inputExp));

			} else if (mappedDecl instanceof Procedure) {
				final Procedure proc = (Procedure) mappedDecl;
				rtr = extractIdentifier(proc.getLocation(), proc.getInParams(), inputExp);
				if (rtr != inputExp) {
					return rtr;
				}
				rtr = extractIdentifier(proc.getLocation(), proc.getOutParams(), inputExp);
				if (rtr != inputExp) {
					return rtr;
				}
				reportUnfinishedBacktranslation("Unfinished backtranslation: Name guessing unsuccessful for Procedure "
						+ BoogiePrettyPrinter.printSignature(proc) + " and expression "
						+ BoogiePrettyPrinter.print(inputExp));
			} else {
				reportUnfinishedBacktranslation(
						"Unfinished backtranslation: Declaration " + mappedDecl.getClass().getSimpleName()
								+ " not handled for expression " + BoogiePrettyPrinter.print(inputExp));
			}

			return rtr;
		}

		private IdentifierExpression extractIdentifier(ILocation mappedLoc, VarList[] list,
				IdentifierExpression inputExp) {
			if (list == null || list.length == 0) {
				return inputExp;
			}
			IdentifierExpression rtr = inputExp;
			for (final VarList lil : list) {
				rtr = extractIdentifier(mappedLoc, lil, inputExp);
				if (rtr != inputExp) {
					return rtr;
				}
			}
			return rtr;
		}

		private IdentifierExpression extractIdentifier(ILocation mappedLoc, VarList list,
				IdentifierExpression inputExp) {
			if (list == null) {
				return inputExp;
			}
			final IType bplType = list.getType().getBoogieType();
			if (!(bplType instanceof BoogieType)) {
				throw new UnsupportedOperationException("The BoogiePreprocessorBacktranslator cannot handle "
						+ bplType.getClass().getSimpleName() + " as type of VarList");
			}
			final BoogieType type = (BoogieType) bplType;
			return extractIdentifier(mappedLoc, list, inputExp, type);

		}

		private IdentifierExpression extractIdentifier(final ILocation mappedLoc, final VarList list,
				final IdentifierExpression inputExp, final BoogieType type) {
			if (type instanceof StructType) {
				return extractStructIdentifier(mappedLoc, list, inputExp, type, (StructType) type);
			} else if (type instanceof ConstructedType) {
				final ConstructedType ct = (ConstructedType) type;
				if (ct.equals(ct.getUnderlyingType())) {
					// this constructed type is a named type
					return matchIdentifier(mappedLoc, list, inputExp);
				} else {
					return extractIdentifier(mappedLoc, list, inputExp, ct.getUnderlyingType());
				}
			} else if (type instanceof PrimitiveType) {
				return matchIdentifier(mappedLoc, list, inputExp);
			} else {
				reportUnfinishedBacktranslation("Unfinished Backtranslation: Type" + type + " of VarList "
						+ BoogiePrettyPrinter.print(list) + " not handled");
				return inputExp;
			}

		}

		private IdentifierExpression matchIdentifier(final ILocation mappedLoc, final VarList list,
				final IdentifierExpression inputExp) {
			final String inputName = inputExp.getIdentifier();
			for (final String name : list.getIdentifiers()) {
				if (inputName.contains(name)) {
					return new IdentifierExpression(mappedLoc, list.getType().getBoogieType(), name,
							inputExp.getDeclarationInformation());
				}
			}
			return inputExp;
		}

		private IdentifierExpression extractStructIdentifier(final ILocation mappedLoc, final VarList list,
				final IdentifierExpression inputExp, final BoogieType type, final StructType st) {
			final String[] inputNames = inputExp.getIdentifier().split("\\.");
			if (inputNames.length == 1) {
				// its the struct itself
				final String inputName = inputExp.getIdentifier();
				for (final String name : list.getIdentifiers()) {
					if (inputName.contains(name)) {
						return new IdentifierExpression(mappedLoc, type, name, inputExp.getDeclarationInformation());
					}
				}
			} else if (inputNames.length == 2) {
				// its a struct field access
				// first, find the name of the struct
				String structName = null;
				final String inputStructName = inputNames[0];
				for (final String name : list.getIdentifiers()) {
					if (inputStructName.contains(name)) {
						structName = name;
						break;
					}
				}
				if (structName != null) {
					// if this worked, lets get the field name
					for (final String fieldName : st.getFieldIds()) {
						if (inputNames[1].contains(fieldName)) {
							return new IdentifierExpression(mappedLoc, type, structName + "!" + fieldName,
									inputExp.getDeclarationInformation());
						}
					}
				}
			} else {
				// its a nested struct field access (this sucks)
				reportUnfinishedBacktranslation("Unfinished Backtranslation: Nested struct field access of VarList "
						+ BoogiePrettyPrinter.print(list) + " not handled");
			}
			return inputExp;
		}
	}
}
