package de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.boogie.type.BoogieType;
import de.uni_freiburg.informatik.ultimate.boogie.type.PrimitiveType;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceStore;
import de.uni_freiburg.informatik.ultimate.core.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.Identifier;
import de.uni_freiburg.informatik.ultimate.logic.LoggingScript;
import de.uni_freiburg.informatik.ultimate.logic.QuotedObject;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.model.IElement;
import de.uni_freiburg.informatik.ultimate.model.annotation.IAnnotations;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.ASTType;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssertStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssignmentStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssumeStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BinaryExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Body;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BoogieASTNode;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BooleanLiteral;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.CallStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.EnsuresSpecification;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.GotoStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.HavocStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Label;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Procedure;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.RequiresSpecification;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.ReturnStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.UnaryExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Unit;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VarList;
import de.uni_freiburg.informatik.ultimate.model.location.ILocation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Boogie2SMT;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.BoogieDeclarations;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SolverBuilder;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.RCFGBacktranslator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.RCFGBuilder;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.TransFormulaBuilder;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.WeakestPrecondition;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.StatementSequence.Origin;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.preferences.RcfgPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.preferences.RcfgPreferenceInitializer.CodeBlockSize;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.preferences.RcfgPreferenceInitializer.Solver;
import de.uni_freiburg.informatik.ultimate.result.Check;
import de.uni_freiburg.informatik.ultimate.smtsolver.external.ScriptorWithGetInterpolants.ExternalInterpolator;
import de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException;

/**
 * This class generates a recursive control flow graph (in the style of POPL'10
 * - Heizmann, Hoenicke, Podelski - Nested Interpolants) from an Boogie AST
 * which contains only unstructured Code (i.e., no while (and others??)
 * statements). The input for this classs has to be unstructured Boogie Code
 * (e.g., no while loops) for example the output of the BoogiePreprocessor.
 * 
 * @author heizmann@informatik.uni-freiburg.de
 */

// TODO How to give every location the right line number
public class CfgBuilder {

	/**
	 * Logger for this plugin.
	 */
	private final Logger mLogger;

	/**
	 * Identifier of the auxiliary start procedure used e.g., by the
	 * CACSL2Boogie translation.
	 */
	public static final String START_PROCEDURE = "ULTIMATE.start";
	
	
	private final String m_LogicForExternalSolver;

	/**
	 * Root Node of this Ultimate model. I use this to store information that
	 * should be passed to the next plugin. The Successors of this node are
	 * exactly the entry nodes of procedures.
	 */
	private RootNode m_Graphroot;

	private RootAnnot m_RootAnnot;

	private Script m_Script;
	private Boogie2SMT m_Boogie2smt;
	private final BoogieDeclarations m_BoogieDeclarations;
	TransFormulaBuilder tfb;

	Collection<Summary> m_ImplementationSummarys = new ArrayList<Summary>();

	private RCFGBacktranslator m_Backtranslator;

	private CodeBlockSize m_CodeBlockSize;

	private final IUltimateServiceProvider mServices;
	
	private final boolean m_AddAssumeForEachAssert;

	private CodeBlockFactory m_Cbf;

	public CfgBuilder(Unit unit, RCFGBacktranslator backtranslator, IUltimateServiceProvider services,
			IToolchainStorage storage) throws IOException {
		mServices = services;
		mLogger = services.getLoggingService().getLogger(Activator.PLUGIN_ID);
		m_Backtranslator = backtranslator;
		m_AddAssumeForEachAssert = (new UltimatePreferenceStore(RCFGBuilder.s_PLUGIN_ID))
				.getBoolean(RcfgPreferenceInitializer.LABEL_ASSUME_FOR_ASSERT);
		Solver solver = (new UltimatePreferenceStore(RCFGBuilder.s_PLUGIN_ID))
				.getEnum(RcfgPreferenceInitializer.LABEL_Solver, Solver.class);

		m_CodeBlockSize = (new UltimatePreferenceStore(RCFGBuilder.s_PLUGIN_ID)).getEnum(
				RcfgPreferenceInitializer.LABEL_CodeBlockSize, CodeBlockSize.class);

		switch (solver) {
		case External_DefaultMode:
		{
			String command = (new UltimatePreferenceStore(RCFGBuilder.s_PLUGIN_ID))
				.getString(RcfgPreferenceInitializer.LABEL_ExtSolverCommand);
			m_Script = SolverBuilder.createExternalSolver(mServices, storage, command);
		}
		break;
		case External_PrincessInterpolationMode:
		{
			String command = (new UltimatePreferenceStore(RCFGBuilder.s_PLUGIN_ID))
				.getString(RcfgPreferenceInitializer.LABEL_ExtSolverCommand);
			m_Script = SolverBuilder.createExternalSolverWithInterpolation(mServices, storage, command, ExternalInterpolator.PRINCESS);
		}
		break;
		case External_Z3InterpolationMode:
		{
			String command = (new UltimatePreferenceStore(RCFGBuilder.s_PLUGIN_ID))
				.getString(RcfgPreferenceInitializer.LABEL_ExtSolverCommand);
			m_Script = SolverBuilder.createExternalSolverWithInterpolation(mServices, storage, command, ExternalInterpolator.IZ3);
		}
		break;
		case Internal_SMTInterpol:
		{
			m_Script = SolverBuilder.createSMTInterpol(mServices, storage);
			int timeoutMilliseconds = 30 * 1000;
			m_Script.setOption(":timeout", String.valueOf(timeoutMilliseconds));
		}
		break;
		default:
			throw new AssertionError("unknown solver");
		}
		
		m_LogicForExternalSolver = (new UltimatePreferenceStore(RCFGBuilder.s_PLUGIN_ID))
				.getString(RcfgPreferenceInitializer.LABEL_ExtSolverLogic);
		

		boolean dumpToFile = (new UltimatePreferenceStore(RCFGBuilder.s_PLUGIN_ID))
				.getBoolean(RcfgPreferenceInitializer.LABEL_DumpToFile);
		if (dumpToFile) {
			String directory = (new UltimatePreferenceStore(RCFGBuilder.s_PLUGIN_ID))
					.getString(RcfgPreferenceInitializer.LABEL_Path);
			directory += (directory.endsWith(System.getProperty("file.separator")) ? "" : System
					.getProperty("file.separator"));
			String filename;
			{
				filename = "";
				if (unit.hasPayload()) {
					if (unit.getPayload().hasLocation()) {
						String pathAndFilename = unit.getPayload().getLocation().getFileName();
						filename = (new File(pathAndFilename)).getName();
					}
				}
			}
			String fullFilename = directory + filename + ".smt2";
			try {
				m_Script = new LoggingScript(m_Script, fullFilename, true);
				mLogger.info("Dumping SMT Script to " + new File(fullFilename).getAbsolutePath());
			} catch (FileNotFoundException e) {
				throw new AssertionError(e);
			}
		}
		
		m_Script.setOption(":produce-models", true);
		switch (solver) {
		case External_DefaultMode:
			m_Script.setOption(":produce-unsat-cores", true);
			m_Script.setLogic(m_LogicForExternalSolver);
		break;
		case External_PrincessInterpolationMode:
		case External_Z3InterpolationMode:
			m_Script.setOption(":produce-interpolants", true);
			m_Script.setLogic(m_LogicForExternalSolver);
		break;
		case Internal_SMTInterpol:
			m_Script.setOption(":produce-unsat-cores", true);
			m_Script.setOption(":produce-interpolants", true);
			m_Script.setOption(":interpolant-check-mode", true);
			m_Script.setOption(":proof-transformation", "LU");
			// m_Script.setOption(":proof-transformation", "RPI");
			// m_Script.setOption(":proof-transformation", "LURPI");
			// m_Script.setOption(":proof-transformation", "RPILU");
			// m_Script.setOption(":verbosity", 0);
			m_Script.setLogic("QF_AUFLIRA");
		break;
		default:
			throw new AssertionError("unknown solver");
		}

		String advertising = System.lineSeparator() + "    SMT script generated on " + 
				(new SimpleDateFormat("yyyy/MM/dd")).format(new Date()) + 
				" by Ultimate. http://ultimate.informatik.uni-freiburg.de/" + 
				System.lineSeparator();
		m_Script.setInfo(":source",	new Identifier(advertising));
		m_Script.setInfo(":smt-lib-version", "2.0");
		m_Script.setInfo(":category", new QuotedObject("industrial"));
		
		m_BoogieDeclarations = new BoogieDeclarations(unit, mLogger);
		boolean blackHolesArrays = false;
		m_Boogie2smt = new Boogie2SMT(m_Script, m_BoogieDeclarations, blackHolesArrays, mServices);
		m_RootAnnot = new RootAnnot(mServices, m_BoogieDeclarations, m_Boogie2smt, m_Backtranslator);
		m_Cbf = m_RootAnnot.getCodeBlockFactory();
		storage.putStorable(CodeBlockFactory.s_CodeBlockFactoryKeyInToolchainStorage, m_Cbf);

	}

	/**
	 * Build a recursive control flow graph for an unstructured boogie program.
	 * 
	 * @param Unit
	 *            that encodes a program.
	 * @return RootNode of a recursive control flow graph.
	 */
	public RootNode getRootNode(Unit unit) {

		tfb = new TransFormulaBuilder(m_Boogie2smt, mServices);

		// Initialize the root node.
		m_Graphroot = new RootNode(unit.getLocation(), m_RootAnnot);

		// Build entry, final and exit node for all procedures that have an
		// implementation
		for (String procName : m_BoogieDeclarations.getProcImplementation().keySet()) {
			Body body = m_BoogieDeclarations.getProcImplementation().get(procName).getBody();
			Statement firstStatement = body.getBlock()[0];
			ProgramPoint entryNode = new ProgramPoint(procName + "ENTRY", procName, false, firstStatement);
			// We have to use some ASTNode for final and exit node. Let's take
			// the procedure implementation.
			Procedure impl = m_BoogieDeclarations.getProcImplementation().get(procName);
			m_RootAnnot.m_entryNode.put(procName, entryNode);
			ProgramPoint finalNode = new ProgramPoint(procName + "FINAL", procName, false, impl);
			m_RootAnnot.m_finalNode.put(procName, finalNode);
			ProgramPoint exitNode = new ProgramPoint(procName + "EXIT", procName, false, impl);
			m_RootAnnot.m_exitNode.put(procName, exitNode);

			new RootEdge(m_Graphroot, m_RootAnnot.m_entryNode.get(procName));
		}

		// Build a control flow graph for each procedure
		ProcedureCfgBuilder procCfgBuilder = new ProcedureCfgBuilder();
		for (String procName : m_BoogieDeclarations.getProcSpecification().keySet()) {
			if (m_BoogieDeclarations.getProcImplementation().containsKey(procName)) {
				procCfgBuilder.buildProcedureCfgFromImplementation(procName);
			} else {
				// procCfgBuilder.buildProcedureCfgWithoutImplementation(procName);
			}
		}

		// Transform CFGs to a recursive CFG
		for (Summary se : m_ImplementationSummarys) {
			addCallTransitionAndReturnTransition(se);
		}
//		m_RootAnnot.m_ModifiableGlobalVariableManager = new ModifiableGlobalVariableManager(
//				m_BoogieDeclarations.getModifiedVars(), m_Boogie2smt);
		m_CodeBlockSize = (new UltimatePreferenceStore(RCFGBuilder.s_PLUGIN_ID)).getEnum(
				RcfgPreferenceInitializer.LABEL_CodeBlockSize, CodeBlockSize.class);
		if (m_CodeBlockSize == CodeBlockSize.LoopFreeBlock) {
			new LargeBlockEncoding();
		}

		return m_Graphroot;
	}

	/**
	 * Flatten varLists. Given an array of varList where each varList can define
	 * several variables, return an array of varLists where each varList defines
	 * only a single variable. Furthermore each variable defined in the result
	 * has gets a prefix.
	 */
	private static VarList[] varLists2singletonVarListsWithPrefix(VarList[] varLists, String Prefix) {
		ArrayList<VarList> result = new ArrayList<VarList>();
		for (VarList varList : varLists) {
			String[] identifiers = varList.getIdentifiers();
			ASTType type = varList.getType();
			for (String identifier : identifiers) {
				String[] newId = { Prefix + identifier };
				result.add(new VarList(null, newId, type));
			}
		}
		return result.toArray(new VarList[0]);
	}

	private Expression getConjunction(List<Expression> expressions) {
		if (expressions == null || expressions.isEmpty()) {
			return null;
		} else {
			Expression conj = expressions.remove(0);
			for (Expression expr : expressions) {
				conj = new BinaryExpression(null, PrimitiveType.boolType, BinaryExpression.Operator.LOGICAND, conj,
						expr);
			}
			return conj;
		}
	}

	private Expression getNegation(Expression expr) {
		if (expr == null) {
			return null;
		} else {
			return new UnaryExpression(expr.getLocation(), PrimitiveType.boolType, UnaryExpression.Operator.LOGICNEG,
					expr);
		}
	}

	/**
	 * Add CallEdge from SummaryEdge source to the entry location of the called
	 * procedure. Add ReturnEdge from the called procedures exit node to the
	 * summary edges target.
	 * 
	 * @param SummaryEdge
	 *            that summarizes execution of an implemented procedure.
	 */
	private void addCallTransitionAndReturnTransition(Summary edge) {
		CallStatement st = edge.getCallStatement();
		String callee = st.getMethodName();
		assert (m_RootAnnot.m_entryNode.containsKey(callee)) : "Source code contains" + " call of " + callee
				+ " but no such procedure.";

		// Add call transition from callerNode to procedures entry node
		ProgramPoint callerNode = (ProgramPoint) edge.getSource();
		ProgramPoint calleeEntryLoc = m_RootAnnot.m_entryNode.get(callee);

		String caller = callerNode.getProcedure();

		TransFormula arguments2InParams = m_RootAnnot.getBoogie2SMT().getStatements2TransFormula()
				.inParamAssignment(st);
		TransFormula outParams2CallerVars = m_RootAnnot.getBoogie2SMT().getStatements2TransFormula()
				.resultAssignment(st, caller);

		Call call = m_Cbf.constructCall(callerNode, calleeEntryLoc, st);
		call.setTransitionFormula(arguments2InParams);

		ProgramPoint returnNode = (ProgramPoint) edge.getTarget();
		ProgramPoint calleeExitLoc = m_RootAnnot.m_exitNode.get(callee);
		Return returnAnnot = m_Cbf.constructReturn(calleeExitLoc, returnNode, call);
		returnAnnot.setTransitionFormula(outParams2CallerVars);
	}

	/**
	 * Build control flow graph of single procedures.
	 * 
	 * @author heizmann@informatik.uni-freiburg.de
	 */
	private class ProcedureCfgBuilder {

		/**
		 * Maps a position identifier to the LocNode that represents this
		 * position in the CFG.
		 */
		private Map<String, ProgramPoint> m_procLocNodes;

		/**
		 * Maps a Label identifier to the LocNode that represents this Label in
		 * the CFG.
		 */
		private HashMap<String, ProgramPoint> m_label2LocNodes;

		/**
		 * Set of all labels that occurred in the procedure. If an element is
		 * inserted twice this is an error.
		 */
		private Set<String> m_Labels;

		/**
		 * Name of that last Label for which we constructed a LocNode
		 */
		String m_lastLabelName;

		/**
		 * Distance to the last LocNode that was constructed as representative
		 * of a label.
		 */
		// int m_locSuffix;

		/**
		 * Element at which we continue building the CFG. This should be a -
		 * LocNode if the last processed Statement was a Label or a
		 * CallStatement - TransEdge if the last processed Statement was Assume,
		 * Assignment, Havoc or Assert. - null if the last processed Statement
		 * was Goto or Return.
		 */
		IElement m_current;

		/**
		 * True only if the current code is deadcode. E.g., if there was a goto
		 * or return but not yet a label.
		 */
		boolean m_deadcode;

		/**
		 * List of auxiliary edges, which represent Gotos and get removed later.
		 */
		List<GotoEdge> m_GotoEdges = new LinkedList<GotoEdge>();

		/**
		 * Name of the procedure for which the CFG is build (at the moment)
		 */
		String m_currentProcedureName;

		/**
		 * The last processed Statement. This is only used in assertions to
		 */
		Statement m_LastSt = new Label(null, null);

		/**
		 * The non goto edges of this procedure.
		 */
		Set<CodeBlock> m_Edges;

		/**
		 * Builds the control flow graph of a single procedure according to a
		 * given implementation.
		 * 
		 * @param Identifier
		 *            of the procedure for which the CFG will be build.
		 */
		private void buildProcedureCfgFromImplementation(String procName) {
			m_currentProcedureName = procName;
			m_Edges = new HashSet<CodeBlock>();
			m_Labels = new HashSet<String>();

			Procedure proc = m_BoogieDeclarations.getProcImplementation().get(m_currentProcedureName);
			// m_Boogie2smt.declareLocals(proc);

			Statement[] statements = m_BoogieDeclarations.getProcImplementation().get(procName).getBody().getBlock();
			if (statements.length == 0) {
				throw new UnsupportedOperationException("Procedure contains no statement");
			}

			m_label2LocNodes = new HashMap<String, ProgramPoint>();

			// initialize the Map from labels to LocNodes for this procedure
			m_procLocNodes = new HashMap<String, ProgramPoint>();
			m_RootAnnot.m_LocNodes.put(procName, m_procLocNodes);

			mLogger.debug("Start construction of the CFG for" + procName);

			{
				// first LocNode is the entry node of the procedure
				ProgramPoint locNode = m_RootAnnot.m_entryNode.get(procName);
				m_lastLabelName = locNode.getLocationName();
				// m_locSuffix = 0;
				m_procLocNodes.put(m_lastLabelName, locNode);
				m_current = locNode;
			}
			assumeRequires(false);

			for (Statement st : statements) {

				if (!mServices.getProgressMonitorService().continueProcessing()) {
					mLogger.warn("Timeout while constructing control flow graph");
					throw new ToolchainCanceledException(this.getClass());
				}

				ILocation loc = st.getLocation();
				assert loc != null : "location of the following statement is null " + st;
				if (loc.isLoop()) {
					mLogger.debug("Found loop entry: " + st);
				}

				if (st instanceof Label) {
					if (m_current instanceof ProgramPoint) {
						assert (m_current == m_RootAnnot.m_entryNode.get(procName) || m_LastSt instanceof Label) : "If st is Label"
								+ " and m_current is LocNode lastSt is Label";
						mLogger.debug("Two Labels in a row: " + m_current + " and " + ((Label) st).getName() + "."
								+ " I am expecting that at least one was" + " introduced by the user (or vcc). In the"
								+ " CFG only the first label of those two (or" + " more) will be used");
					}
					if (m_current instanceof CodeBlock) {
						assert (m_LastSt instanceof AssumeStatement || m_LastSt instanceof AssignmentStatement
								|| m_LastSt instanceof HavocStatement || m_LastSt instanceof AssertStatement || m_LastSt instanceof CallStatement) : "If st"
								+ " is a Label and the last constructed node"
								+ " was a TransEdge, then the last"
								+ " Statement must not be a Label, Return or" + " Goto";
						mLogger.warn("Label in the middle of a codeblock.");
					}

					processLabel((Label) st);
				}

				else if (st instanceof AssumeStatement || st instanceof AssignmentStatement
						|| st instanceof HavocStatement) {
					if (m_current instanceof CodeBlock) {
						assert (m_LastSt instanceof AssumeStatement || m_LastSt instanceof AssignmentStatement
								|| m_LastSt instanceof HavocStatement || m_LastSt instanceof AssertStatement || m_LastSt instanceof CallStatement) : "If the"
								+ " last constructed node is a TransEdge, then"
								+ " the last Statement must not be a Label,"
								+ " Return or Goto. (i.e. this is not the first" + " Statemnt of the block)";
					}
					processAssuAssiHavoStatement(st, Origin.IMPLEMENTATION);
				}

				else if (st instanceof AssertStatement) {
					if (m_current instanceof CodeBlock) {
						assert (m_LastSt instanceof AssumeStatement || m_LastSt instanceof AssignmentStatement
								|| m_LastSt instanceof HavocStatement || m_LastSt instanceof AssertStatement || m_LastSt instanceof CallStatement) : "If the"
								+ " last constructed node is a TransEdge, then"
								+ " the last Statement must not be a Label,"
								+ " Return or Goto. (i.e. this is not the first" + " Statement of the block)";
					}
					processAssertStatement((AssertStatement) st);
				}

				else if (st instanceof GotoStatement) {
					// assert (! (m_LastSt instanceof GotoStatement)) :
					// "Two Gotos in a row";
					if (m_LastSt instanceof GotoStatement) {
						mLogger.warn("Two Gotos in a row! There was dead code");
					} else {
						processGotoStatement((GotoStatement) st);
					}
				}

				else if (st instanceof CallStatement) {
					if (m_current instanceof CodeBlock) {
						assert (m_LastSt instanceof AssumeStatement || m_LastSt instanceof AssignmentStatement
								|| m_LastSt instanceof HavocStatement || m_LastSt instanceof AssertStatement || m_LastSt instanceof CallStatement) : "If m_current is a TransEdge, then lastSt"
								+ " must not be a Label, Return or Goto."
								+ " (i.e. this is not the first Statemnt"
								+ " of the block)";
					}
					if (m_current instanceof ProgramPoint) {
						assert (m_LastSt instanceof Label || m_LastSt instanceof CallStatement) : "If m_current is LocNode, then st is"
								+ " first statement of a block or fist" + " statement after a call";
					}
					processCallStatement((CallStatement) st);
				}

				else if (st instanceof ReturnStatement) {
					processReturnStatement();
				}

				else {
					throw new UnsupportedOperationException("At the moment"
							+ " only Labels, Assert, Assume, Assignment, Havoc" + " and Goto statements are supported");
				}
				m_LastSt = st;
			}

			// If there is no ReturnStatement at the end of the procedure act
			// like there would have been one.
			if (!(m_LastSt instanceof ReturnStatement)) {
				processReturnStatement();
			}

			// Assume that the procedures final node may be reachable
			m_deadcode = false;

			assertAndAssumeEnsures();

			// Remove auxiliary GotoTransitions
			boolean removeGotoEdges = (new UltimatePreferenceStore(RCFGBuilder.s_PLUGIN_ID))
					.getBoolean(RcfgPreferenceInitializer.LABEL_RemoveGotoEdges);
			if (removeGotoEdges) {
				mLogger.debug("Starting removal of auxiliaryGotoTransitions");
				while (!(m_GotoEdges.isEmpty())) {
					GotoEdge gotoEdge = m_GotoEdges.remove(0);
					removeAuxiliaryGoto(gotoEdge);
				}
			}

			for (CodeBlock transEdge : m_Edges) {
				tfb.addTransitionFormulas(transEdge, procName);
			}
			// m_Boogie2smt.removeLocals(proc);
		}

		/**
		 * construct error location BoogieASTNode in procedure procName add
		 * constructed location to m_procLocNodes and m_ErrorNodes.
		 * 
		 * @return
		 */
		private ProgramPoint addErrorNode(String procName, BoogieASTNode BoogieASTNode) {
			Collection<ProgramPoint> errorNodes = m_RootAnnot.m_ErrorNodes.get(procName);
			if (errorNodes == null) {
				errorNodes = new ArrayList<ProgramPoint>();
				m_RootAnnot.m_ErrorNodes.put(procName, errorNodes);
			}
			int locNodeNumber = m_RootAnnot.m_ErrorNodes.get(procName).size();
			String errorLocLabel;
			if (BoogieASTNode instanceof AssertStatement) {
				errorLocLabel = procName + "Err" + locNodeNumber + "AssertViolation";
			} else if (BoogieASTNode instanceof EnsuresSpecification) {
				errorLocLabel = procName + "Err" + locNodeNumber + "EnsuresViolation";
			} else if (BoogieASTNode instanceof CallStatement) {
				errorLocLabel = procName + "Err" + locNodeNumber + "RequiresViolation";
			} else {
				throw new IllegalArgumentException();
			}
			ProgramPoint errorLocNode = new ProgramPoint(errorLocLabel, procName, true, BoogieASTNode);
			Object checkCand = BoogieASTNode.getPayload().getAnnotations().get(Check.getIdentifier());
			if (checkCand != null) {
				Check check = (Check) checkCand;
				errorLocNode.getPayload().getAnnotations().put(Check.getIdentifier(), check);
			}
			m_procLocNodes.put(errorLocLabel, errorLocNode);
			errorNodes.add(errorLocNode);
			return errorLocNode;
		}

		/**
		 * @return List of {@code EnsuresSpecification}s that contains only one
		 *         {@code EnsuresSpecification} which is true.
		 */
		private List<EnsuresSpecification> getDummyEnsuresSpecifications(ILocation loc) {
			Expression dummyExpr = new BooleanLiteral(loc, BoogieType.boolType, true);
			EnsuresSpecification dummySpec = new EnsuresSpecification(loc, false, dummyExpr);
			ArrayList<EnsuresSpecification> dummySpecs = new ArrayList<EnsuresSpecification>(1);
			dummySpecs.add(dummySpec);
			return dummySpecs;
		}

		/**
		 * @return List of {@code RequiresSpecification}s that contains only one
		 *         {@code RequiresSpecification} which is true.
		 */
		private List<RequiresSpecification> getDummyRequiresSpecifications() {
			Expression dummyExpr = new BooleanLiteral(null, BoogieType.boolType, true);
			RequiresSpecification dummySpec = new RequiresSpecification(null, false, dummyExpr);
			ArrayList<RequiresSpecification> dummySpecs = new ArrayList<RequiresSpecification>(1);
			dummySpecs.add(dummySpec);
			return dummySpecs;
		}

		/**
		 * Remove GotoEdge from a CFG in a way that the program behaviour is not
		 * changed.
		 */
		private void removeAuxiliaryGoto(GotoEdge gotoEdge) {
			ProgramPoint mother = (ProgramPoint) gotoEdge.getSource();
			ProgramPoint child = (ProgramPoint) gotoEdge.getTarget();

			// Target of a goto should never be an error location.
			// If this assertion will fail some day. A fix might be that
			// mother has to become an error location.
			assert (!child.isErrorLocation());

			for (RCFGEdge grandchild : child.getOutgoingEdges()) {
				if (grandchild instanceof Call) {
					mLogger.info("Will not remove gotoEdge" + gotoEdge
							+ "since this would involve adding/removing call"
							+ "and return edges and bring my naive goto" + " replacing algorithm into terrible trouble");
					return;
				}
			}

			mother.removeOutgoing(gotoEdge);
			gotoEdge.setSource(null);
			gotoEdge.setTarget(null);
			child.removeIncoming(gotoEdge);
			mLogger.debug("Removed GotoEdge from" + mother + " to " + child);
			if (mother == child) {
				mLogger.debug("GotoEdge was selfloop");
			} else {
				if (child.getIncomingEdges().isEmpty() || mother.getOutgoingEdges().isEmpty()) {
					mLogger.debug(mother + " has no sucessors any more or " + child + "has no predecessors any more.");
					mLogger.debug(child + " gets absorbed by " + mother);
					mergeLocNodes(child, mother);
				} else {
					// Not allowed to merge mother and child in this case
					mLogger.debug(child + " has " + child.getIncomingEdges().size() + " predecessors," + " namely "
							+ child.getIncomingNodes());
					mLogger.debug(mother + " has " + mother.getIncomingEdges().size() + " successors" + ", namely "
							+ mother.getOutgoingNodes());
					mLogger.debug("Adding for every successor" + " transition of " + child + " a copy of that"
							+ " transition as successor of " + mother);
					for (RCFGEdge grandchild : child.getOutgoingEdges()) {
						ProgramPoint target = (ProgramPoint) grandchild.getTarget();
						CodeBlock edge = m_Cbf.copyCodeBlock((CodeBlock) grandchild, mother, target);
						if (edge instanceof GotoEdge) {
							m_GotoEdges.add((GotoEdge) edge);
						} else {
							m_Edges.add(edge);
						}
					}
				}
			}
			
		}



		/**
		 * Builds the control flow graph of a single procedure according to a
		 * given specification. Use this if no implementation is available.
		 * 
		 * @param Name
		 *            of the procedure for which the CFG will be build.
		 */
		private void buildProcedureCfgWithoutImplementation(String procName) {
			m_currentProcedureName = procName;

			Procedure proc = m_BoogieDeclarations.getProcSpecification().get(m_currentProcedureName);

			// initialize the Map from labels to LocNodes for this procedure
			m_procLocNodes = new HashMap<String, ProgramPoint>();
			m_RootAnnot.m_LocNodes.put(procName, m_procLocNodes);

			mLogger.debug("Start construction of the CFG for" + procName);

			// first LocNode is the entry node of the procedure
			ProgramPoint locNode = m_RootAnnot.m_entryNode.get(procName);
			m_lastLabelName = locNode.getLocationName();
			// m_locSuffix = 0;
			m_procLocNodes.put(m_lastLabelName, locNode);
			m_current = locNode;

			assumeRequires(true);

			ProgramPoint finalNode = m_RootAnnot.m_finalNode.get(m_currentProcedureName);
			assert (m_current != locNode);
			((CodeBlock) m_current).connectTarget(finalNode);

			assertAndAssumeEnsures();
		}

		/**
		 * Assert the ensures clause. For each ensures clause expr
		 * <ul>
		 * <li>append {@code assume (expr)} between the finalNode and the
		 * exitNode of the procedure</li> add an edge labeled with
		 * {@code assume (not expr)} from the final Node to the errorNode
		 */
		private void assertAndAssumeEnsures() {
			// Assume the ensures specification at the end of the procedure.
			List<EnsuresSpecification> ensures = m_BoogieDeclarations.getEnsures().get(m_currentProcedureName);
			if (ensures == null || ensures.isEmpty()) {
				Procedure proc = m_BoogieDeclarations.getProcSpecification().get(m_currentProcedureName);
				ensures = getDummyEnsuresSpecifications(proc.getLocation());
			}
			ProgramPoint finalNode = m_RootAnnot.m_finalNode.get(m_currentProcedureName);
			m_lastLabelName = finalNode.getLocationName();
			m_procLocNodes.put(m_lastLabelName, finalNode);
			// m_locSuffix = 0;
			m_current = finalNode;

			for (EnsuresSpecification spec : ensures) {
				AssumeStatement st = new AssumeStatement(spec.getLocation(), spec.getFormula());
				passAllAnnotations(spec, st);
				m_Backtranslator.putAux(st, new BoogieASTNode[] { spec });
				processAssuAssiHavoStatement(st, Origin.ENSURES);
				m_LastSt = st;
			}
			ProgramPoint exitNode = m_RootAnnot.m_exitNode.get(m_currentProcedureName);
			m_lastLabelName = exitNode.getLocationName();
			m_procLocNodes.put(m_lastLabelName, exitNode);
			((CodeBlock) m_current).connectTarget(exitNode);

			// Violations against the ensures part of the procedure
			// specification
			List<EnsuresSpecification> ensuresNonFree = m_BoogieDeclarations.getEnsuresNonFree().get(
					m_currentProcedureName);
			if (ensuresNonFree != null && !ensuresNonFree.isEmpty()) {
				for (EnsuresSpecification spec : ensuresNonFree) {
					Expression specExpr = spec.getFormula();
					AssumeStatement assumeSt;
					assumeSt = new AssumeStatement(spec.getLocation(), getNegation(specExpr));
					passAllAnnotations(spec, assumeSt);
					m_Backtranslator.putAux(assumeSt, new BoogieASTNode[] { spec });
					ProgramPoint errorLocNode = addErrorNode(m_currentProcedureName, spec);
					CodeBlock assumeEdge = m_Cbf.constructStatementSequence(finalNode, errorLocNode, assumeSt, Origin.ENSURES);
					passAllAnnotations(spec, assumeEdge);
					passAllAnnotations(spec, errorLocNode);
					m_Edges.add(assumeEdge);
				}
			}
		}

		/**
		 * Assume the requires clause. If the requires clause is empty and
		 * dummyRequiresIfEmpty is true add an dummy requires specification.
		 */
		private void assumeRequires(boolean dummyRequiresIfEmpty) {
			// Assume everything mentioned in the requires specification
			List<RequiresSpecification> requires = m_BoogieDeclarations.getRequires().get(m_currentProcedureName);
			if ((requires == null || requires.isEmpty()) && dummyRequiresIfEmpty) {
				requires = getDummyRequiresSpecifications();
			}
			if (requires != null && !requires.isEmpty()) {
				for (RequiresSpecification spec : requires) {
					AssumeStatement st = new AssumeStatement(spec.getLocation(), spec.getFormula());
					passAllAnnotations(spec, st);
					m_Backtranslator.putAux(st, new BoogieASTNode[] { spec });
					processAssuAssiHavoStatement(st, Origin.REQUIRES);
					m_LastSt = st;
				}
			}
		}

		// /////////////////////////////////////////////////////////
		// private void assignModifiableGlobals() {
		//
		// }
		// //////////////////////////////////////////////////////////
		//
		// /**
		// * Build AssignmentStatement such that to a variable the own value is
		// * assigned.
		// * This AssignmentStatement seems to be pretty useless, but will be
		// used to
		// * build an auxiliary TransitionFormula used for the computation of
		// nested
		// * interpolants.
		// * @param vars Representation for a set of variables. A variable name
		// is
		// * mapped to its type.
		// * @return Assignment where we assign to each variable in vars its own
		// value
		// */
		// private AssignmentStatement oldVar2VarAssignment(Map<String,ASTType>
		// vars) {
		// Collection<String> identifiers;
		// if (vars==null) {
		// identifiers = new HashSet<String>(0);
		// }
		// else {
		// identifiers = vars.keySet();
		// }
		// VariableLHS[] lhs = new VariableLHS[identifiers.size()];
		// Expression[] rhs = new Expression[identifiers.size()];
		//
		// int i=0;
		// for (String identifier : identifiers) {
		// IType type = vars.get(identifier).getBoogieType();
		// lhs[i] = new VariableLHS(null,type,identifier);
		// rhs[i] = new IdentifierExpression(null,type,identifier);
		// rhs[i] = new UnaryExpression(null,UnaryExpression.Operator.OLD,
		// rhs[i]);
		// i++;
		// }
		// return new AssignmentStatement(null,lhs,rhs);
		// }

		private String getLocName(ILocation location) {
			int startLine = location.getStartLine();
			String unprimedName = "L" + startLine;
			if (location.isLoop()) {
				unprimedName += "loopEntry";
			}
			String result = getUniqueName(unprimedName);
			return result;
		}

		private String getUniqueName(String name) {
			if (m_procLocNodes.containsKey(name)) {
				return getUniqueName(name + "'");
			} else {
				return name;
			}
		}

		/**
		 * Get the LocNode that represents a label. If there is already a
		 * LocNode that represents this Label return this representative.
		 * Otherwise construct a new LocNode that becomes the representative for
		 * this label.
		 * 
		 * @param labelName
		 *            Name of the Label for which you want the corresponding
		 *            LocNode.
		 * @param st
		 *            Statement whose (Ultimate) Location should be added to
		 *            this LocNode. If this method is called while processing a
		 *            GotoStatement the Statement can be set to null, since the
		 *            Location will be overwritten, when this method is called
		 *            with the correct Label as second parameter.
		 * @return LocNode that is the representative for labelName.
		 */
		private ProgramPoint getLocNodeforLabel(String labelName, Statement st) {
			if (m_label2LocNodes.containsKey(labelName)) {
				ProgramPoint locNode = m_label2LocNodes.get(labelName);
				mLogger.debug("LocNode for " + labelName + " already" + " constructed, namely: " + locNode);
				if (st instanceof Label && locNode.getLocationName() == labelName) {
					ILocation loc = st.getLocation();
					locNode.getPayload().setLocation(loc);
					if (st.getLocation().isLoop()) {
						mLogger.debug("LocNode does not have to Location of the while loop" + st.getLocation());
						m_RootAnnot.m_LoopLocations.put(locNode, st.getLocation());
					}
				}
				return locNode;
			} else {
				ProgramPoint locNode = new ProgramPoint(labelName, m_currentProcedureName, false, st);
				m_label2LocNodes.put(labelName, locNode);
				m_procLocNodes.put(labelName, locNode);
				mLogger.debug("LocNode for " + labelName + " has not" + " existed yet. Constructed it");
				if (st != null && st.getLocation().isLoop()) {
					m_RootAnnot.m_LoopLocations.put(locNode, st.getLocation());
				}
				return locNode;
			}
		}

		private void processLabel(Label st) {
			String labelName = ((Label) st).getName();
			boolean existsAlready = !m_Labels.add(labelName);
			if (existsAlready) {
				throw new AssertionError("Label " + labelName + " occurred twice");
			}
			if (m_current instanceof ProgramPoint) {
				// from now on this label is represented by m_current
				ProgramPoint oldNodeForLabel = m_label2LocNodes.get(labelName);
				if (oldNodeForLabel != null) {
					mergeLocNodes(oldNodeForLabel, (ProgramPoint) m_current);
				}
				m_label2LocNodes.put(labelName, (ProgramPoint) m_current);
			} else // (m_current instanceof TransEdge) or m_current = null
			{
				m_lastLabelName = labelName;
				// m_locSuffix = 0;

				// is there already a LocNode that represents this
				// label? (This can be the case if this label was destination
				// of a goto statement) If not construct the LocNode.
				// If yes, add the Location Object to the existing LocNode.
				ProgramPoint locNode = getLocNodeforLabel(labelName, st);

				if (m_current instanceof CodeBlock) {
					((RCFGEdge) m_current).setTarget(locNode);
					locNode.addIncoming((CodeBlock) m_current);
				}
				m_current = locNode;
			}
			m_deadcode = false;
		}

		private void processAssuAssiHavoStatement(Statement st, Origin origin) {
			if (m_deadcode) {
				return;
			}
			if (m_current instanceof ProgramPoint) {
				StatementSequence codeBlock = m_Cbf.constructStatementSequence((ProgramPoint) m_current, null, st, origin);
				passAllAnnotations(st, codeBlock);
				m_Edges.add(codeBlock);
				m_current = codeBlock;
			} else if (m_current instanceof CodeBlock) {
				if (m_CodeBlockSize == CodeBlockSize.SequenceOfStatements
						|| m_CodeBlockSize == CodeBlockSize.LoopFreeBlock) {
					StatementSequence stSeq = (StatementSequence) m_current;
					stSeq.addStatement(st);
					stSeq.updatePayloadName();
					passAllAnnotations(st, stSeq);
				} else {
					String locName = getLocName(st.getLocation());
					ProgramPoint locNode = new ProgramPoint(locName, m_currentProcedureName, false, st);
					((CodeBlock) m_current).connectTarget(locNode);
					m_procLocNodes.put(locName, locNode);
					StatementSequence codeBlock = m_Cbf.constructStatementSequence(locNode, null, st, origin);
					passAllAnnotations(st, codeBlock);
					m_Edges.add(codeBlock);
					m_current = codeBlock;
				}
			} else {
				// m_current must either be LocNode or TransEdge
				throw new IllegalArgumentException();
			}

		}

		private void processAssertStatement(AssertStatement st) {
			if (m_deadcode) {
				return;
			}
			if (m_current instanceof CodeBlock) {
				String locName = getLocName(st.getLocation());
				ProgramPoint locNode = new ProgramPoint(locName, m_currentProcedureName, false, st);
				((CodeBlock) m_current).connectTarget(locNode);
				m_procLocNodes.put(locName, locNode);
				m_current = locNode;
			}
			ProgramPoint locNode = (ProgramPoint) m_current;
			Expression assertion = ((AssertStatement) st).getFormula();
			AssumeStatement assumeError = new AssumeStatement(st.getLocation(), getNegation(assertion));
			passAllAnnotations(st, assumeError);
			m_Backtranslator.putAux(assumeError, new BoogieASTNode[] { st });
			ProgramPoint errorLocNode = addErrorNode(m_currentProcedureName, st);
			StatementSequence assumeErrorCB = m_Cbf.constructStatementSequence(locNode, errorLocNode, assumeError, Origin.ASSERT);
			passAllAnnotations(st, errorLocNode);
			passAllAnnotations(st, assumeErrorCB);
			m_Edges.add(assumeErrorCB);
			AssumeStatement assumeSafe = new AssumeStatement(st.getLocation(), assertion);
			if (m_AddAssumeForEachAssert) {
				assumeSafe = new AssumeStatement(st.getLocation(), assertion);
			} else {
				// we cannot omit this assume(true) because if the assert is
				// the last node of the procedure the final location will be
				// merged with the last location. In this case this would be
				// the predecessor of the assume(!expression).
				// Hence the error location would be erroneously reachable from
				// the final location.
				assumeSafe = new AssumeStatement(st.getLocation(), 
						new BooleanLiteral(st.getLocation(), true));
			}
			passAllAnnotations(st, assumeSafe);
			m_Backtranslator.putAux(assumeSafe, new BoogieASTNode[] { st });
			StatementSequence assumeSafeCB = m_Cbf.constructStatementSequence(locNode, null, assumeSafe, Origin.ASSERT);
			passAllAnnotations(st, assumeSafeCB);
			// add a new TransEdge labeled with st as successor of the
			// last constructed LocNode
			m_Edges.add(assumeSafeCB);
			m_current = assumeSafeCB;
		}

		private void processGotoStatement(GotoStatement st) {
			if (m_deadcode) {
				return;
			}
			String[] targets = ((GotoStatement) st).getLabels();
			assert (targets.length != 0) : "Goto must have at least one target";
			mLogger.debug("Goto statement with " + targets.length + " targets.");
			ProgramPoint locNode;
			if (m_current instanceof CodeBlock) {
				String locName = getLocName(st.getLocation());
				locNode = new ProgramPoint(locName, m_currentProcedureName, false, st);
				((CodeBlock) m_current).connectTarget(locNode);
				m_procLocNodes.put(locName, locNode);
			} else if (m_current instanceof ProgramPoint) {
				locNode = (ProgramPoint) m_current;
			} else {
				// m_current must either LocNode or TransEdge
				throw new IllegalArgumentException();

			}
			for (String label : targets) {
				// Add an auxiliary GotoEdge and a LocNode
				// for each target of the GotoStatement.
				ProgramPoint targetLocNode = getLocNodeforLabel(label, st);
				m_GotoEdges.add(m_Cbf.constructGotoEdge(locNode, targetLocNode));
			}
			// We have not constructed a new node that should be used in the
			// next iteration step, therefore setting m_current to null.
			m_current = null;
			m_deadcode = true;
		}

		private void processCallStatement(CallStatement st) {
			if (m_deadcode) {
				return;
			}
			ProgramPoint locNode;
			if (m_current instanceof CodeBlock) {
				String locName = getLocName(st.getLocation());
				locNode = new ProgramPoint(locName, m_currentProcedureName, false, st);
				((CodeBlock) m_current).connectTarget(locNode);
				m_procLocNodes.put(locName, locNode);
			} else if (m_current instanceof ProgramPoint) {
				locNode = (ProgramPoint) m_current;
			} else {
				// m_current must be either LocNode or TransEdge
				throw new IllegalArgumentException();
			}
			String locName = getLocName(st.getLocation());
			ProgramPoint returnNode = new ProgramPoint(locName, m_currentProcedureName, false, st);
			m_procLocNodes.put(locName, returnNode);
			// add summary edge
			String callee = st.getMethodName();
			Summary summaryEdge;
			if (m_BoogieDeclarations.getProcImplementation().containsKey(callee)) {
				summaryEdge = m_Cbf.constructSummary(locNode, returnNode, st, true);
				passAllAnnotations(st, summaryEdge);
				m_ImplementationSummarys.add(summaryEdge);
			} else {
				summaryEdge = m_Cbf.constructSummary(locNode, returnNode, st, false);
				passAllAnnotations(st, summaryEdge);
			}
			m_Edges.add(summaryEdge);
			m_current = returnNode;

			// Violations against the requires part of the procedure
			// specification. Omit intruduction of these additional auxiliary
			// assert statements if current procedure is START_PROCEDURE.
			//
			List<RequiresSpecification> requiresNonFree = m_BoogieDeclarations.getRequiresNonFree().get(callee);
			if (requiresNonFree != null && !requiresNonFree.isEmpty()
					&& !m_currentProcedureName.equals(START_PROCEDURE)) {
				for (RequiresSpecification spec : requiresNonFree) {
					// use implementation if available and specification
					// otherwise. To use the implementation is important in
					// cases where signature of procedure and implementation are
					// different.
					Procedure proc;
					if (m_BoogieDeclarations.getProcImplementation().containsKey(callee)) {
						proc = m_BoogieDeclarations.getProcImplementation().get(callee);
					} else {
						proc = m_BoogieDeclarations.getProcSpecification().get(callee);
					}
					Expression violatedRequires = getNegation(new WeakestPrecondition(spec.getFormula(), st, proc)
							.getResult());
					AssumeStatement assumeSt;
					assumeSt = new AssumeStatement(st.getLocation(), violatedRequires);
					passAllAnnotations(st, assumeSt);
					m_Backtranslator.putAux(assumeSt, new BoogieASTNode[] { st, spec });
					ProgramPoint errorLocNode = addErrorNode(m_currentProcedureName, st);
					StatementSequence errorCB = m_Cbf.constructStatementSequence(locNode, errorLocNode, assumeSt, Origin.REQUIRES);
					passAllAnnotations(spec, errorCB);
					passAllAnnotations(spec, errorLocNode);
					m_Edges.add(errorCB);
				}
			}
		}

		// FIXME problem if last statement is goto
		// fixed on 16.05.2011 - still needs to be tested
		private void processReturnStatement() {
			if (m_deadcode) {
				return;
			}
			// If m_current is a transition add as successor the final Node
			// of this procedure.
			// If m_current is a location replace it with the final Node of
			// this procedure.
			ProgramPoint finalNode = m_RootAnnot.m_finalNode.get(m_currentProcedureName);
			if (m_current instanceof CodeBlock) {
				CodeBlock transEdge = (CodeBlock) m_current;
				transEdge.connectTarget(finalNode);
				mLogger.debug("Constructed TransEdge " + transEdge + "as predecessr of " + m_RootAnnot.m_finalNode);
			} else if (m_current instanceof ProgramPoint) {
				mergeLocNodes((ProgramPoint) m_current, finalNode);
				mLogger.debug("Replacing " + m_current + " by " + finalNode);
			} else {
				// m_current must be either LocNode or TransEdge
				// s_Logger.warn("Last location of " + m_currentProcedureName +
				// "not reachable");
				throw new IllegalArgumentException();
			}
			// No new nodes created, set m_current to null
			m_current = null;
			m_deadcode = true;

		}

		/**
		 * Merge one LocNode into another. The oldLocNode will be merged into
		 * the newLocNode. The newLocNode gets connected to all
		 * incoming/outgoing transitions of the oldLocNode. The oldLocNode
		 * looses connections to all incoming/outgoing transitions. If the
		 * oldLocNode was representative for a Label the new location will from
		 * now on be the representative of this Label.
		 * 
		 * @param oldLocNode
		 *            LocNode that gets merged into the newLocNode. Must not
		 *            represent an error location.
		 * @param newLocNode
		 *            LocNode that absorbes the oldLocNode.
		 */
		private void mergeLocNodes(ProgramPoint oldLocNode, ProgramPoint newLocNode) {
			// oldLocNode must not represent an error location
			assert (!oldLocNode.isErrorLocation());
			if (oldLocNode == newLocNode) {
				return;
			}

			for (RCFGEdge transEdge : oldLocNode.getIncomingEdges()) {
				transEdge.setTarget(newLocNode);
				newLocNode.addIncoming(transEdge);
			}
			oldLocNode.clearIncoming();
			for (RCFGEdge transEdge : oldLocNode.getOutgoingEdges()) {
				transEdge.setSource(newLocNode);
				newLocNode.addOutgoing(transEdge);
			}
			oldLocNode.clearOutgoing();
			if (oldLocNode.getBoogieASTNode() != null && oldLocNode.getBoogieASTNode().getLocation().isLoop()) {
				mLogger.debug("LocNode does not have to Location of the while loop"
						+ oldLocNode.getBoogieASTNode().getLocation());
			}

			m_procLocNodes.remove(oldLocNode.getPosition());

			// If the LocNode that should be replaced was constructed for a
			// label it is in m_locNodeOf and the representative for this label
			// should be updated accordingly.
			if (m_label2LocNodes.containsKey(oldLocNode.getLocationName())) {
				m_label2LocNodes.put(oldLocNode.getLocationName(), newLocNode);
			}
			if (m_RootAnnot.m_LoopLocations.containsKey(oldLocNode)) {
				ILocation loopLoc = m_RootAnnot.m_LoopLocations.get(oldLocNode);
				m_RootAnnot.m_LoopLocations.remove(oldLocNode);
				m_RootAnnot.m_LoopLocations.put(newLocNode, loopLoc);
			}
			assert oldLocNode != m_RootAnnot.m_exitNode.get(m_currentProcedureName);
			if (oldLocNode == m_RootAnnot.m_entryNode.get(m_currentProcedureName)) {
				m_RootAnnot.m_entryNode.put(m_currentProcedureName, newLocNode);
			}
		}

	}

	private class LargeBlockEncoding {

		Set<ProgramPoint> sequentialQueue = new HashSet<ProgramPoint>();
		Map<ProgramPoint, List<CodeBlock>> parallelQueue = new HashMap<ProgramPoint, List<CodeBlock>>();
		final boolean m_SimplifyCodeBlocks;

		public LargeBlockEncoding() {
			m_SimplifyCodeBlocks = (new UltimatePreferenceStore(RCFGBuilder.s_PLUGIN_ID))
					.getBoolean(RcfgPreferenceInitializer.LABEL_Simplify);

			for (String proc : m_RootAnnot.m_LocNodes.keySet()) {
				for (String position : m_RootAnnot.m_LocNodes.get(proc).keySet()) {
					ProgramPoint pp = m_RootAnnot.m_LocNodes.get(proc).get(position);
					if (superfluousSequential(pp)) {
						sequentialQueue.add(pp);
					} else {
						List<CodeBlock> list = superfluousParallel(pp);
						if (list != null) {
							parallelQueue.put(pp, list);
						}
					}
				}
			}
			while (!sequentialQueue.isEmpty() || !parallelQueue.isEmpty()) {
				if (!sequentialQueue.isEmpty()) {
					ProgramPoint superfluousPP = sequentialQueue.iterator().next();
					sequentialQueue.remove(superfluousPP);
					composeSequential(superfluousPP);
				} else {
					Entry<ProgramPoint, List<CodeBlock>> superfluous = parallelQueue.entrySet().iterator().next();
					ProgramPoint pp = superfluous.getKey();
					List<CodeBlock> outgoing = superfluous.getValue();
					parallelQueue.remove(pp);
					composeParallel(pp, outgoing);
				}
			}
		}

		private void composeSequential(ProgramPoint pp) {
			assert (pp.getIncomingEdges().size() == 1);
			assert (pp.getOutgoingEdges().size() == 1);
			CodeBlock incoming = (CodeBlock) pp.getIncomingEdges().get(0);
			CodeBlock outgoing = (CodeBlock) pp.getOutgoingEdges().get(0);
			ProgramPoint predecessor = (ProgramPoint) incoming.getSource();
			ProgramPoint successor = (ProgramPoint) outgoing.getTarget();
			List<CodeBlock> sequence = new ArrayList<>(2);
			sequence.add(incoming);
			sequence.add(outgoing);
			m_Cbf.constructSequentialComposition(predecessor, successor, m_SimplifyCodeBlocks, false, sequence);
			if (!sequentialQueue.contains(predecessor)) {
				List<CodeBlock> outEdges = superfluousParallel(predecessor);
				if (outEdges != null) {
					parallelQueue.put(predecessor, outEdges);
				}
			}
		}

		private void composeParallel(ProgramPoint pp, List<CodeBlock> outgoing) {
			ProgramPoint successor = (ProgramPoint) outgoing.get(0).getTarget();
			m_Cbf.constructParallelComposition(pp, successor, Collections.unmodifiableList(outgoing));
			if (superfluousSequential(pp)) {
				sequentialQueue.add(pp);
			} else {
				List<CodeBlock> list = superfluousParallel(pp);
				if (list != null) {
					parallelQueue.put(pp, list);
				}
			}
			if (superfluousSequential(successor)) {
				sequentialQueue.add(successor);
			}
		}

		private boolean superfluousSequential(ProgramPoint pp) {
			if (pp.getIncomingEdges().size() != 1) {
				return false;
			}
			if (pp.getOutgoingEdges().size() != 1) {
				return false;
			}
			RCFGEdge incoming = pp.getIncomingEdges().get(0);
			if (incoming instanceof RootEdge) {
				return false;
			}
			if (incoming instanceof Call) {
				return false;
			}
			assert (incoming instanceof StatementSequence || incoming instanceof SequentialComposition
					|| incoming instanceof ParallelComposition || incoming instanceof Summary);
			RCFGEdge outgoing = pp.getOutgoingEdges().get(0);
			if (outgoing instanceof Return) {
				return false;
			}
			assert (outgoing instanceof StatementSequence || outgoing instanceof SequentialComposition
					|| outgoing instanceof ParallelComposition || outgoing instanceof Summary);
			return true;
		}

		/**
		 * Check if ProgramPoint pp has several outgoing edges whose target is
		 * the same ProgramPoint.
		 * 
		 * @return For some successor ProgramPoint the list of all outgoing
		 *         edges whose target is this (successor) ProgramPoint, if there
		 *         can be such a list with more than one element. Otherwise
		 *         (each outgoing edge leads to a different ProgramPoint) return
		 *         null.
		 */
		private List<CodeBlock> superfluousParallel(ProgramPoint pp) {
			List<CodeBlock> result = null;
			Map<ProgramPoint, List<CodeBlock>> succ2edge = new HashMap<ProgramPoint, List<CodeBlock>>();
			for (RCFGEdge edge : pp.getOutgoingEdges()) {
				if (!(edge instanceof Return)) {
					CodeBlock cb = (CodeBlock) edge;
					ProgramPoint succ = (ProgramPoint) cb.getTarget();
					List<CodeBlock> edges = succ2edge.get(succ);
					if (edges == null) {
						edges = new ArrayList<CodeBlock>();
						succ2edge.put(succ, edges);
					}
					edges.add(cb);
					if (result == null && edges.size() > 1) {
						result = edges;
					}
				}
			}
			return result;
		}
	}


	private static void passAllAnnotations(BoogieASTNode node, RcfgElement cb) {
		if (node.getPayload().hasAnnotation()) {
			HashMap<String, IAnnotations> annots = node.getPayload().getAnnotations();
			cb.getPayload().getAnnotations().putAll(annots);
		}
	}

	private static void passAllAnnotations(BoogieASTNode node, Statement st) {
		if (node.getPayload().hasAnnotation()) {
			HashMap<String, IAnnotations> annots = node.getPayload().getAnnotations();
			st.getPayload().getAnnotations().putAll(annots);
		}
	}

}
