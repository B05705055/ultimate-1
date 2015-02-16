/**
 * 
 */
package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.ta;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.IncomingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.OutgoingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.OutgoingReturnTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.Transitionlet;
import de.uni_freiburg.informatik.ultimate.boogie.symboltable.BoogieSymbolTable;
import de.uni_freiburg.informatik.ultimate.boogie.type.PreprocessorAnnotation;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceStore;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.model.IElement;
import de.uni_freiburg.informatik.ultimate.model.boogie.BoogieVar;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.CallStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Boogie2SmtSymbolTable;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.AbstractInterpretationAnnotations;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.IAbstractStateChangeListener;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.LiteralCollector;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.StateChangeLogger;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.AbstractInterpretationBoogieVisitor;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.AbstractState;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.AbstractState.LoopStackElement;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.AbstractState.Pair;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.IAbstractDomainFactory;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.IAbstractValue;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.booldomain.BoolDomainFactory;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.booldomain.BoolValue;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.booldomain.BoolValue.Bool;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.intervaldomain.IntervalDomainFactory;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.intervaldomain.IntervalValue;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.signdomain.SignDomainFactory;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.signdomain.SignValue;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.signdomain.SignValue.Sign;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.topbottomdomain.TopBottomDomainFactory;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.topbottomdomain.TopBottomValue;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.abstractdomain.topbottomdomain.TopBottomValue.TopBottom;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretation.preferences.AbstractInterpretationPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.irsdependencies.loopdetector.LoopDetectorNWA;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.RcfgProgramExecution;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Call;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.GotoEdge;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ParallelComposition;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ProgramPoint;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RCFGEdge;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RcfgElement;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Return;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootEdge;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.SequentialComposition;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.StatementSequence;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Summary;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.util.RCFGEdgeVisitor;
import de.uni_freiburg.informatik.ultimate.result.AllSpecificationsHoldResult;
import de.uni_freiburg.informatik.ultimate.result.IProgramExecution.ProgramState;
import de.uni_freiburg.informatik.ultimate.result.TimeoutResult;
import de.uni_freiburg.informatik.ultimate.result.UnprovableResult;
import de.uni_freiburg.informatik.ultimate.result.UnsupportedSyntaxResult;

/**
 * @author Christopher Dillo modified for usage on NWA by Fabian Schillinger
 * 
 */
public class AbstractInterpreterTA extends RCFGEdgeVisitor {

	private final IUltimateServiceProvider m_services;

	private Logger mLogger;

	private IAbstractDomainFactory<?> m_intDomainFactory;
	private IAbstractDomainFactory<?> m_realDomainFactory;
	private IAbstractDomainFactory<?> m_boolDomainFactory;
	private IAbstractDomainFactory<?> m_bitVectorDomainFactory;
	private IAbstractDomainFactory<?> m_stringDomainFactory;

	// object(node) <list<predecessors> list<successors>>
	private final LinkedList<Object> m_nodesToVisit = new LinkedList<Object>();
	private final Deque<Object> m_nodesToVisitDFS = new ArrayDeque<Object>();
	private final LinkedList<Object> mCallSources = new LinkedList<>();
	Map<Object, ProgramPoint> m_programPointMap = new HashMap<Object, ProgramPoint>();

	private final Map<Object, List<AbstractState>> m_states = new HashMap<Object, List<AbstractState>>();
	private final Map<Object, List<AbstractState>> mAnnotations = new HashMap<Object, List<AbstractState>>();
	// private final Map<RCFGNode, List<AbstractState>> m_states = new
	// HashMap<RCFGNode, List<AbstractState>>();

	private AbstractState m_currentState, mResultingState;

	private Object m_currentNode;

	private AbstractInterpretationBoogieVisitor m_boogieVisitor;

	private final Set<IAbstractStateChangeListener> m_stateChangeListeners = new HashSet<IAbstractStateChangeListener>();

	private final Set<Object> m_errorLocs = new HashSet<Object>();
	private final Set<Object> m_reachedErrorLocs = new HashSet<Object>();

	private HashMap<ProgramPoint, HashMap<Object, Object>> m_loopEntryNodes;

	private final Set<ProgramPoint> m_recursionEntryNodes = new HashSet<ProgramPoint>();

	private Set<String> m_fixedNumbersForWidening = new HashSet<String>();
	private Set<String> m_numbersForWidening = new HashSet<String>();

	private BoogieSymbolTable m_symbolTable;

	private Map<Object, Term> m_termMap = new HashMap<Object, Term>();

	private boolean m_continueProcessing;

	// Lists of call statements to check if there is any node with a summary
	// that has no corresponding regular call
	private List<CallStatement> m_callStatementsAtCalls = new LinkedList<CallStatement>();
	private List<CallStatement> m_callStatementsAtSummaries = new LinkedList<CallStatement>();

	// for preferences
	private String m_mainMethodName;
	private int m_iterationsUntilWidening;
	private int m_parallelStatesUntilMerge;
	private String m_widening_fixedNumbers;
	private boolean m_widening_autoNumbers;

	private boolean m_generateStateAnnotations;
	private boolean m_stateChangeLogConsole;
	private boolean m_stateChangeLogFile;
	private boolean m_stateChangeLogUseSourcePath;
	private String m_stateChangeLogPath;

	private boolean m_stopAfterAnyError;
	private boolean m_stopAfterAllErrors;

	private String m_intDomainID;
	private String m_intWideningOpName;
	private String m_intMergeOpName;

	private String m_realDomainID;
	private String m_realWideningOpName;
	private String m_realMergeOpName;

	private String m_boolDomainID;
	private String m_boolWideningOpName;
	private String m_boolMergeOpName;

	private String m_bitVectorDomainID;
	private String m_bitVectorWideningOpName;
	private String m_bitVectorMergeOpName;

	private String m_stringDomainID;
	private String m_stringWideningOpName;
	private String m_stringMergeOpName;

	// ULTIMATE.start
	private final String m_ultimateStartProcName = "ULTIMATE.start";

	private List<UnprovableResult<RcfgElement, CodeBlock, Expression>> m_result;
	private INestedWordAutomaton<CodeBlock, Object> m_nwa;

	public AbstractInterpreterTA(IUltimateServiceProvider services) {
		m_services = services;
		mLogger = m_services.getLoggingService().getLogger(
				Activator.s_PLUGIN_ID);

		fetchPreferences();

		if (!m_widening_fixedNumbers.isEmpty()) {
			String[] nums = m_widening_fixedNumbers.split(",");
			for (int i = 0; i < nums.length; i++)
				m_fixedNumbersForWidening.add(nums[i].trim());
		}
	}

	/**
	 * Adds an AbstractInterpretationAnnotation with states at the given
	 * location IElement
	 * 
	 * @param element
	 *            The location of the given states
	 * @param states
	 *            The states at the given location
	 */
	private void annotateElement(Object element, List<AbstractState> states) {
		// DD: I changed replace to put, because replace is Java8, we are on
		// Java 7; this should be the behavior that replace exhibits. Please
		// check if this was intended
		if (mAnnotations.containsKey(element)) {
			mAnnotations.put(element, states);
		}

		if (element instanceof IElement) {
			AbstractInterpretationAnnotations anno = new AbstractInterpretationAnnotations(
					states);
			anno.annotate((IElement) element);
		}
	}

	/**
	 * Adds a program state to a node's list of states
	 * 
	 * @param node
	 *            The node that state belongs to
	 * @param state
	 *            The program state to put on the node
	 * @return True if the state was added, false if not (when a superstate is
	 *         at the node)
	 */

	// private boolean putStateToNode(AbstractState state, RCFGNode node,
	// RCFGEdge fromEdge) {
	private boolean putStateToNode(AbstractState state, Object nwaState,
			Object nwaLetter) {
		List<AbstractState> statesAtNode = m_states.get(nwaState);
		if (statesAtNode == null) {
			statesAtNode = new LinkedList<AbstractState>();
			m_states.put(nwaState, statesAtNode);
		}

		AbstractState newState = state;
		List<AbstractState> statesAtNodeBackup = new ArrayList<AbstractState>(
				statesAtNode);

		// check for loop entry / widening
		boolean applyLoopWidening = false;
		if ((nwaState != null)
				&& m_loopEntryNodes
						.containsKey(m_programPointMap.get(nwaState))) {
			LoopStackElement le = newState.popLoopEntry();
			while ((le != null) && (le.getLoopNode() != null)
					&& (le.getLoopNode() != nwaState)
					&& (le.getExitEdge() == nwaLetter))
				le = newState.popLoopEntry();
			if ((le != null)
					&& (newState.peekLoopEntry().getIterationCount(nwaState) >= m_iterationsUntilWidening))
				applyLoopWidening = true;
		}
		// check for recursive method exit / widening
		boolean applyRecursionWidening = false;
		if (m_recursionEntryNodes.contains(m_programPointMap.get(nwaState)))
			applyRecursionWidening = newState.getRecursionCount() >= m_iterationsUntilWidening;

		// check existing states: super/sub/widening/merging
		Set<AbstractState> unprocessedStates = new HashSet<AbstractState>();
		Set<AbstractState> statesToRemove = new HashSet<AbstractState>();
		for (AbstractState s : statesAtNode) {
			if (!(nwaLetter instanceof Return) && s.isSuper(newState)) {
				mLogger.debug("Superstate exists");
				return false; // abort if a superstate exists
			}
			if (s.isProcessed()) {
				if (newState.isSuccessor(s)) {
					// widen after loop if possible
					if (applyLoopWidening
							&& (s.peekLoopEntry().getIterationCount(nwaState) > 0)) {
						newState = s.widen(newState);
						mLogger.debug(String.format("Widening at %s", nwaState));
					}
					// widen at new recursive call if possible
					if (applyRecursionWidening) {
						newState = s.widen(newState);
						mLogger.debug(String.format(
								"Widening after recursion at %s", nwaState));
					}

					statesToRemove.add(s); // remove old obsolete nodes
				}
			} else {
				if (newState.isSuper(s))
					statesToRemove.add(s); // remove unprocessed substates of
											// the new state
				else
					unprocessedStates.add(s); // collect unprocessed states
			}
		}
		for (AbstractState s : statesToRemove)
			statesAtNode.remove(s);
		if (unprocessedStates.size() >= m_parallelStatesUntilMerge) {
			// merge states
			mLogger.debug(String.format("Merging at %s", nwaState.toString()));
			for (AbstractState s : unprocessedStates) {
				AbstractState mergedState = s.merge(newState);
				if (mergedState != null) {
					statesAtNode.remove(s);
					newState = mergedState;
				}
			}
		}
		statesAtNode.add(newState);
		if (nwaLetter != null) {
			Transitionlet<CodeBlock, Object> it = (Transitionlet<CodeBlock, Object>) nwaLetter;
			notifyStateChangeListeners((RCFGEdge) it.getLetter(),
					statesAtNodeBackup, state, newState);
		} else {
			notifyStateChangeListeners((RCFGEdge) nwaLetter,
					statesAtNodeBackup, state, newState);
		}
		if (m_generateStateAnnotations)
			annotateElement(nwaState, statesAtNode);
		return true;
	}

	public List<UnprovableResult<RcfgElement, CodeBlock, Expression>> processNWA(
			INestedWordAutomaton<CodeBlock, Object> nwa, RootNode rn,
			Map<Object, ProgramPoint> programPointMap) {
		m_programPointMap = programPointMap;
		m_nwa = nwa;
		m_result = new ArrayList<UnprovableResult<RcfgElement, CodeBlock, Expression>>();

		// RootNode root = rn;
		m_errorLocs.clear();
		m_reachedErrorLocs.clear();
		m_recursionEntryNodes.clear();

		m_continueProcessing = true;

		// state change logger
		if (m_stateChangeLogConsole || m_stateChangeLogFile) {
			String fileDir = "";
			String fileName = "";
			if (m_stateChangeLogFile) {
				File sourceFile = new File(rn.getFilename());
				DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
				fileName = sourceFile.getName() + "_AI_"
						+ dfm.format(new Date()) + ".txt";
				if (m_stateChangeLogUseSourcePath) {
					fileDir = sourceFile.getParent();
				} else {
					fileDir = null;
				}
				if (fileDir == null) {
					fileDir = new File(m_stateChangeLogPath).getAbsolutePath();
				}
			}
			StateChangeLogger scl = new StateChangeLogger(mLogger,
					m_stateChangeLogConsole, m_stateChangeLogFile, fileDir
							+ File.separatorChar + fileName);
			this.registerStateChangeListener(scl);
		}

		// numbers for widening
		m_numbersForWidening.clear();
		m_numbersForWidening.addAll(m_fixedNumbersForWidening);
		// collect literals if preferences say so
		if (m_widening_autoNumbers) {
			LiteralCollector literalCollector = new LiteralCollector(nwa,
					mLogger);
			m_numbersForWidening.addAll(literalCollector.getResult());
		}

		// domain factories chosen in preferences
		m_intDomainFactory = makeDomainFactory(m_intDomainID,
				m_intWideningOpName, m_intMergeOpName);
		m_realDomainFactory = makeDomainFactory(m_realDomainID,
				m_realWideningOpName, m_realMergeOpName);
		m_boolDomainFactory = makeDomainFactory(m_boolDomainID,
				m_boolWideningOpName, m_boolMergeOpName);
		m_bitVectorDomainFactory = makeDomainFactory(m_bitVectorDomainID,
				m_bitVectorWideningOpName, m_bitVectorMergeOpName);
		m_stringDomainFactory = makeDomainFactory(m_stringDomainID,
				m_stringWideningOpName, m_stringMergeOpName);

		// fetch loop nodes with their entry/exit edges
		LoopDetectorNWA loopDetector = new LoopDetectorNWA(m_services);
		try {
			loopDetector.process(nwa, m_programPointMap);
		} catch (Throwable e1) {
			e1.printStackTrace();
		}
		m_loopEntryNodes = loopDetector.getResult();

		PreprocessorAnnotation pa = PreprocessorAnnotation.getAnnotation(rn);
		if (pa == null) {
			mLogger.error("No symbol table found on given RootNode.");
			return null;
		}
		m_symbolTable = pa.getSymbolTable();

		m_boogieVisitor = new AbstractInterpretationBoogieVisitor(mLogger,
				m_symbolTable, m_intDomainFactory, m_realDomainFactory,
				m_boolDomainFactory, m_bitVectorDomainFactory,
				m_stringDomainFactory);

		// TODO: entryNodes?
		Map<String, ProgramPoint> entryNodes = rn.getRootAnnot()
				.getEntryNodes();
		ProgramPoint mainEntry = entryNodes.get(m_mainMethodName);

		// check for ULTIMATE.start and recursive methods
		for (String s : entryNodes.keySet()) {
			ProgramPoint entryNode = entryNodes.get(s);
			// check for ULTIMATE.start
			if (entryNode.getProcedure().startsWith(m_ultimateStartProcName))
				mainEntry = entryNode;
			// check for recursive methods
			Collection<ProgramPoint> methodNodes = rn.getRootAnnot()
					.getProgramPoints().get(s).values();
			for (RCFGEdge e : entryNode.getIncomingEdges()) {
				if (methodNodes.contains(e.getSource())) {
					// entryNode belongs to recursive method
					m_recursionEntryNodes.add(entryNode);
				}
			}
		}

		m_errorLocs.addAll(nwa.getFinalStates());

		for (Object nwaState : nwa.getInitialStates()) {
			AbstractState state = new AbstractState(mLogger,
					m_intDomainFactory, m_realDomainFactory,
					m_boolDomainFactory, m_bitVectorDomainFactory,
					m_stringDomainFactory);

			m_nodesToVisit.add(nwaState);

			putStateToNode(state, nwaState, null);
		}

		visitNodes();
		//m_nodesToVisitDFS.addAll(m_nodesToVisit);
		//visitNodesDFS();

		Boogie2SmtSymbolTable b2smtst = rn.getRootAnnot().getBoogie2SMT()
				.getBoogie2SmtSymbolTable();
		Script script = rn.getRootAnnot().getScript();
		extractTerms(nwa, b2smtst, script);
		getTermMap();

		// don't report anything here
		return m_result;
	}

	/**
	 * @param nwa
	 * @param b2smtst
	 * @param script
	 */
	private void extractTerms(INestedWordAutomaton<CodeBlock, Object> nwa,
			Boogie2SmtSymbolTable b2smtst, Script script) {
		mLogger.debug("Extracting terms...");
		for (Object currentState : nwa.getStates()) {
			List<AbstractState> abstractStatesAtNode = mAnnotations
					.get(currentState);
			Term[] termsAtActualNode = new Term[0];
			if (abstractStatesAtNode != null) {
				for (int j = 0; j < abstractStatesAtNode.size(); j++) {
					AbstractState s = abstractStatesAtNode.get(j);
					termsAtActualNode = new Term[s.getCurrentScope()
							.getValues().entrySet().size()];
					int k = 0;
					for (Entry<Pair, IAbstractValue<?>> entry : s
							.getCurrentScope().getValues().entrySet()) {
						Pair p = entry.getKey();
						IAbstractValue<?> abstractValue = s.getCurrentScope()
								.getValues().get(p);
						BoogieVar boogieVar = b2smtst.getBoogieVar(
								p.getString(), p.getDeclaration(), false);
						TermVariable termVariable = boogieVar.getTermVariable();
						termsAtActualNode[k] = abstractValueToTerm(script,
								abstractValue, termVariable);
						k++;
					}
				}
			}

			Term combinedTermAtActualNode = null;
			if (termsAtActualNode.length == 0) {
				// TODO: wenn kein term an NWA-Knoten ist term dann true? Oder
				// false wegen unreachable?
				// TODO: Mit Matthias klären
				combinedTermAtActualNode = script.term("false");
			} else if (termsAtActualNode.length == 1) {
				combinedTermAtActualNode = termsAtActualNode[0];
			} else {
				combinedTermAtActualNode = script
						.term("and", termsAtActualNode);
			}
			m_termMap.put(currentState, combinedTermAtActualNode);
		}
	}

	/**
	 * @param script
	 * @param abstractValue
	 * @param termVariable
	 * @return
	 */
	private Term abstractValueToTerm(Script script,
			IAbstractValue<?> abstractValue, TermVariable termVariable) {
		if (abstractValue.getClass().equals(IntervalValue.class)) {
			IntervalValue intervalValue = (IntervalValue) abstractValue;
			Term lowerBound = null;
			Term upperBound = null;
			Rational lower = intervalValue.getValue().getLowerBound();
			Rational upper = intervalValue.getValue().getUpperBound();
			if (lower.isRational() && upper.isRational()) {
				if (!lower.equals(upper)) {
					lowerBound = lower.toTerm(termVariable.getSort());
					lowerBound = script.term("<=", lowerBound, termVariable);
					upperBound = upper.toTerm(termVariable.getSort());
					upperBound = script.term(">=", upperBound, termVariable);
					return script.term("and", lowerBound, upperBound);
				} else {
					lowerBound = lower.toTerm(termVariable.getSort());
					return script.term("=", lowerBound, termVariable);
				}
			} else if ((lower == Rational.NEGATIVE_INFINITY)
					&& (upper == Rational.POSITIVE_INFINITY)) {
				return script.term("true");
			} else if ((lower == Rational.NEGATIVE_INFINITY)
					&& (upper.isRational())) {
				upperBound = upper.toTerm(termVariable.getSort());
				return script.term(">=", upperBound, termVariable);
			} else if ((lower.isRational())
					&& (upper == Rational.POSITIVE_INFINITY)) {
				lowerBound = lower.toTerm(termVariable.getSort());
				return script.term("<=", lowerBound, termVariable);
			} else
				return script.term("false");
		} else if (abstractValue.getClass().equals(BoolValue.class)) {
			BoolValue boolValue = (BoolValue) abstractValue;
			Term boolTerm = null;
			if (boolValue.getValue() == Bool.TRUE) {
				boolTerm = script.term("true");
			} else if (boolValue.getValue() == Bool.FALSE) {
				boolTerm = script.term("false");
			} else
				return script.term("=", boolTerm, termVariable);
		} else if (abstractValue.getClass().equals(SignValue.class)) {
			SignValue signValue = (SignValue) abstractValue;
			Term signTerm = null;
			if (signValue.getValue() == Sign.EMPTY) {
				signTerm = script.term("false");
				return script.term("=", signTerm, termVariable);
			} else if (signValue.getValue() == Sign.MINUS) {
				signTerm = Rational.valueOf(0, 0)
						.toTerm(termVariable.getSort());
				return script.term("<", signTerm, termVariable);
			} else if (signValue.getValue() == Sign.PLUS) {
				signTerm = Rational.valueOf(0, 0)
						.toTerm(termVariable.getSort());
				return script.term(">", signTerm, termVariable);
			} else if (signValue.getValue() == Sign.PLUSMINUS) {
				signTerm = script.term("true");
				return script.term("=", signTerm, termVariable);
			} else {
				signTerm = Rational.valueOf(0, 0)
						.toTerm(termVariable.getSort());
				return script.term("=", signTerm, termVariable);
			}
		} else if (abstractValue.getClass().equals(TopBottomValue.class)) {
			TopBottomValue topBottomValue = (TopBottomValue) abstractValue;
			if (topBottomValue.getValue() == TopBottom.BOTTOM) {
				return script.term("false");
			} else
				return script.term("true");
		}
		return null;
	}

	public Map<Object, Term> getTermMap() {
		return m_termMap;
	}

	protected void visitNodes() {
		while (!m_nodesToVisit.isEmpty()) {
			Object currentNWAState = m_nodesToVisit.poll();

			if (currentNWAState != null) {
				m_callStatementsAtCalls.clear();
				m_callStatementsAtSummaries.clear();

				List<AbstractState> statesAtNode = m_states
						.get(currentNWAState);
				mLogger.debug(String.format("---- PROCESSING NODE %S %S----",
						currentNWAState, currentNWAState.getClass()
								.getSimpleName()));
				// process all unprocessed states at the node
				boolean hasUnprocessed = true;
				while (hasUnprocessed && m_continueProcessing) {
					AbstractState unprocessedState = null;
					for (AbstractState s : statesAtNode) {
						if (!s.isProcessed()) {
							unprocessedState = s;
							break;
						}
					}
					if (unprocessedState != null) {
						m_currentState = unprocessedState;
						m_currentState.setProcessed(true);

						m_currentNode = currentNWAState;

						for (OutgoingInternalTransition<CodeBlock, Object> e : m_nwa
								.internalSuccessors(currentNWAState)) {
							mResultingState = null;
							visit(e);
						}
						for (OutgoingCallTransition<CodeBlock, Object> e : m_nwa
								.callSuccessors(currentNWAState)) {
							mResultingState = null;
							// mCallSources.add(currentNWAState);
							visit(e);
						}
						for (OutgoingReturnTransition<CodeBlock, Object> e : m_nwa
								.returnSuccessors(currentNWAState)) {
							// if (!mCallSources.isEmpty() && e.getHierPred() ==
							// mCallSources.getLast()) {
							// mCallSources.removeLast();
							mResultingState = null;
							visit(e);
							// }
						}
					} else {
						hasUnprocessed = false;
					}
					// abort if asked to cancel
					m_continueProcessing = m_continueProcessing
							&& m_services.getProgressMonitorService()
									.continueProcessing();
				} // hasUnprocessed

				// remove states if they aren't needed for possible widening
				// anymore
				int incomingSize = m_nwa.lettersInternalIncoming(
						currentNWAState).size()
						+ m_nwa.lettersReturnIncoming(currentNWAState).size()
						+ m_nwa.lettersCallIncoming(currentNWAState).size();
				if (incomingSize <= 1)
					m_states.remove(currentNWAState);

				if (!m_callStatementsAtCalls
						.containsAll(m_callStatementsAtSummaries))
					reportUnsupportedSyntaxResult(
							(IElement) currentNWAState,
							"(NWA) Abstract interpretation plug-in can't verify "
									+ "programs which contain procedures without implementations.");

				if (!m_continueProcessing)
					break;
			}
		}
		return;
	}

	protected void visitNodesDFS() {
		while (!m_nodesToVisitDFS.isEmpty()) {
			Object currentObject = m_nodesToVisitDFS.pop();
			if (currentObject != null) {
				if ((currentObject instanceof OutgoingInternalTransition)
						|| (currentObject instanceof OutgoingCallTransition)
						|| (currentObject instanceof OutgoingReturnTransition)) {
					visitDFS(currentObject);
				} else {

					m_callStatementsAtCalls.clear();
					m_callStatementsAtSummaries.clear();

					List<AbstractState> statesAtNode = m_states
							.get(currentObject);
					mLogger.debug(String.format(
							"---- PROCESSING NODE %S %S----", currentObject,
							currentObject.getClass().getSimpleName()));
					// process all unprocessed states at the node
					boolean hasUnprocessed = true;
					while (hasUnprocessed && m_continueProcessing) {
						AbstractState unprocessedState = null;
						for (AbstractState s : statesAtNode) {
							if (!s.isProcessed()) {
								unprocessedState = s;
								break;
							}
						}
						if (unprocessedState != null) {
							m_currentState = unprocessedState;
							m_currentState.setProcessed(true);

							m_currentNode = currentObject;

							for (OutgoingInternalTransition<CodeBlock, Object> e : m_nwa
									.internalSuccessors(currentObject)) {
								m_nodesToVisitDFS.push(e);
							}
							for (OutgoingCallTransition<CodeBlock, Object> e : m_nwa
									.callSuccessors(currentObject)) {
								m_nodesToVisitDFS.push(e);
							}
							for (OutgoingReturnTransition<CodeBlock, Object> e : m_nwa
									.returnSuccessors(currentObject)) {
								m_nodesToVisitDFS.push(e);
							}
						} else {
							hasUnprocessed = false;
						}
						// abort if asked to cancel
						m_continueProcessing = m_continueProcessing
								&& m_services.getProgressMonitorService()
										.continueProcessing();
					} // hasUnprocessed

					// remove states if they aren't needed for possible widening
					// anymore
					int incomingSize = m_nwa.lettersInternalIncoming(
							currentObject).size()
							+ m_nwa.lettersReturnIncoming(currentObject).size()
							+ m_nwa.lettersCallIncoming(currentObject).size();
					if (incomingSize <= 1)
						m_states.remove(currentObject);

					if (!m_callStatementsAtCalls
							.containsAll(m_callStatementsAtSummaries))
						reportUnsupportedSyntaxResult(
								(IElement) currentObject,
								"(NWA) Abstract interpretation plug-in can't verify "
										+ "programs which contain procedures without implementations.");

					if (!m_continueProcessing)
						break;
				}
			}
		}
		return;
	}

	protected void visitDFS(Object transition) {
		mResultingState = null;
		CodeBlock letter = null;
		Object succ = null;
		if (transition instanceof OutgoingInternalTransition) {
			OutgoingInternalTransition<CodeBlock, Object> tr = (OutgoingInternalTransition<CodeBlock, Object>) transition;
			letter = tr.getLetter();
			succ = tr.getSucc();
		} else if (transition instanceof OutgoingCallTransition) {
			OutgoingCallTransition<CodeBlock, Object> tr = (OutgoingCallTransition<CodeBlock, Object>) transition;
			letter = tr.getLetter();
			succ = tr.getSucc();
		} else if (transition instanceof OutgoingReturnTransition) {
			OutgoingReturnTransition<CodeBlock, Object> tr = (OutgoingReturnTransition<CodeBlock, Object>) transition;
			letter = tr.getLetter();
			succ = tr.getSucc();
		} else {
			return;
		}

		super.visit((RCFGEdge) letter);

		String evaluationError = m_boogieVisitor.getErrorMessage();
		if (!evaluationError.isEmpty()) {
			reportUnsupportedSyntaxResult((IElement) transition,
					evaluationError);

			mResultingState = null; // return, abort, stop.
		}

		if (mResultingState == null) {
			mLogger.debug("No resulting state, ignoring target node");
			return; // do not process target node!
		}

		Map<Object, Object> loopEdges = m_loopEntryNodes.get(m_programPointMap
				.get(m_currentNode));
		if (loopEdges != null) {
			Object exitEdge = loopEdges.get(letter);
			if (exitEdge != null)
				mResultingState.pushLoopEntry(
						(ProgramPoint) m_programPointMap.get(m_currentNode),
						(RCFGEdge) exitEdge);
		}

		mResultingState.addCodeBlockToTrace((CodeBlock) letter);

		if (succ != null) {
			if (putStateToNode(mResultingState, succ, transition)) {
				if (m_errorLocs.contains(succ)
						&& !m_reachedErrorLocs.contains(succ)) {
					m_reachedErrorLocs.add(succ);
					ProgramPoint pp = m_programPointMap.get(succ);
					reportErrorResult(pp, mResultingState);
				} else {
					if (!m_nodesToVisitDFS.contains(succ))
						m_nodesToVisitDFS.push(succ);
				}
			} else {
				mLogger.debug("Skipping successors");
			}
		} else {
			mLogger.debug("There is no successor");
		}
		return;
	}

	protected void visit(Object transition) {
		// protected UnprovableResult<RcfgElement, CodeBlock, Expression>
		// visit(RCFGEdge e, boolean runsOnNWA) {
		CodeBlock letter = null;
		Object succ = null;
		if (transition instanceof OutgoingInternalTransition) {
			OutgoingInternalTransition<CodeBlock, Object> tr = (OutgoingInternalTransition<CodeBlock, Object>) transition;
			letter = tr.getLetter();
			succ = tr.getSucc();
		} else if (transition instanceof OutgoingCallTransition) {
			OutgoingCallTransition<CodeBlock, Object> tr = (OutgoingCallTransition<CodeBlock, Object>) transition;
			letter = tr.getLetter();
			succ = tr.getSucc();
		} else if (transition instanceof OutgoingReturnTransition) {
			OutgoingReturnTransition<CodeBlock, Object> tr = (OutgoingReturnTransition<CodeBlock, Object>) transition;
			letter = tr.getLetter();
			succ = tr.getSucc();
		} else {
			return;
		}

		super.visit((RCFGEdge) letter);

		String evaluationError = m_boogieVisitor.getErrorMessage();
		if (!evaluationError.isEmpty()) {
			reportUnsupportedSyntaxResult((IElement) transition,
					evaluationError);

			mResultingState = null; // return, abort, stop.
		}

		if (mResultingState == null) {
			mLogger.debug("No resulting state, ignoring target node");
			return; // do not process target node!
		}

		Map<Object, Object> loopEdges = m_loopEntryNodes.get(m_programPointMap
				.get(m_currentNode));
		if (loopEdges != null) {
			Object exitEdge = loopEdges.get(letter);
			if (exitEdge != null)
				mResultingState.pushLoopEntry(
						(ProgramPoint) m_programPointMap.get(m_currentNode),
						(RCFGEdge) exitEdge);
		}

		mResultingState.addCodeBlockToTrace((CodeBlock) letter);

		if (succ != null) {
			if (putStateToNode(mResultingState, succ, transition)) {
				if (m_errorLocs.contains(succ)
						&& !m_reachedErrorLocs.contains(succ)) {
					m_reachedErrorLocs.add(succ);
					ProgramPoint pp = m_programPointMap.get(succ);
					reportErrorResult(pp, mResultingState);
				} else {
					if (!m_nodesToVisit.contains(succ))
						m_nodesToVisit.add(succ);
				}
			} else {
				mLogger.debug("Skipping successors");
			}
		} else {
			mLogger.debug("There is no successor");
		}
		return;
	}

	@Override
	protected void visit(RootEdge e) {
		mLogger.debug("> RootEdge");

		super.visit(e);

		mResultingState = m_currentState.copy();
	}

	@Override
	protected void visit(CodeBlock c) {
		mLogger.debug("> CodeBlock START");

		super.visit(c);

		mLogger.debug("< CodeBlock END");
	}

	@Override
	protected void visit(Call c) {
		mLogger.debug("> Call");

		CallStatement cs = c.getCallStatement();
		m_callStatementsAtCalls.add(cs);
		mResultingState = m_boogieVisitor.evaluateStatement(cs, m_currentState);
	}

	@Override
	protected void visit(GotoEdge c) {
		mLogger.debug("> GotoEdge");

		mResultingState = m_currentState.copy();
	}

	@Override
	protected void visit(ParallelComposition c) {
		mLogger.debug("> ParallelComposition START");

		Collection<CodeBlock> blocks = c.getBranchIndicator2CodeBlock()
				.values();

		// process all blocks wrt. the current state and merge all resulting
		// states
		AbstractState mergedState = null;
		for (CodeBlock block : blocks) {
			mLogger.debug(String.format("Parallel: %s",
					block.getPrettyPrintedStatements()));
			visit(block);
			if (mResultingState != null) {
				if (mergedState == null) {
					mergedState = mResultingState;
				} else {
					mergedState = mergedState.merge(mResultingState);
				}
			}
		}
		mResultingState = mergedState;

		mLogger.debug("< ParallelComposition END");
	}

	@Override
	protected void visit(Return c) {
		mLogger.debug("> Return");

		mResultingState = m_boogieVisitor.evaluateReturnStatement(
				c.getCallStatement(), m_currentState);
	}

	@Override
	protected void visit(SequentialComposition c) {
		mLogger.debug("> SequentialComposition START");

		AbstractState currentState = m_currentState;
		// backup, as the member variable is manipulated during iterating the
		// CodeBlocks

		List<CodeBlock> blocks = c.getCodeBlocks();

		for (int i = 0; i < blocks.size(); i++) {
			visit(blocks.get(i));
			m_currentState = mResultingState;
			// so the next CodeBlocks current state is this states' result state

			if (mResultingState == null)
				break;
		}

		m_currentState = currentState;

		mLogger.debug("< SequentialComposition END");
	}

	@Override
	protected void visit(Summary c) {
		mLogger.debug("> Summary");

		m_callStatementsAtSummaries.add(c.getCallStatement());
		mResultingState = null; // do not process target node (ignore summary
								// edges)
	}

	@Override
	protected void visit(StatementSequence c) {
		mLogger.debug("Processing edge [" + c.hashCode() + "]: " + c);

		List<Statement> statements = c.getStatements();

		if (statements.size() > 0) {
			mResultingState = m_currentState;
			for (int i = 0; i < statements.size(); i++) {
				Statement s = statements.get(i);
				if (s != null) {
					mResultingState = m_boogieVisitor.evaluateStatement(s,
							mResultingState);
					if (mResultingState == null)
						break;
				}
			}
		}
	}

	/**
	 * Add a state change listener to be notified on state changes
	 * 
	 * @param listener
	 *            The listener to notify
	 * @return True if it has been added, false if not, i.e. if it was already
	 *         registered
	 */
	public boolean registerStateChangeListener(
			IAbstractStateChangeListener listener) {
		return m_stateChangeListeners.add(listener);
	}

	/**
	 * Remove a state change listener
	 * 
	 * @param listener
	 *            The listener to remove
	 * @return True if it was in the list and has been removed, false if it
	 *         wasn't there to begin with
	 */
	public boolean removeStateChangeListener(
			IAbstractStateChangeListener listener) {
		return m_stateChangeListeners.remove(listener);
	}

	/**
	 * Notifies all state change listeners of a state change
	 * 
	 * @param viaEdge
	 * @param oldStates
	 * @param newState
	 * @param mergedState
	 */
	private void notifyStateChangeListeners(RCFGEdge viaEdge,
			List<AbstractState> oldStates, AbstractState newState,
			AbstractState mergedState) {
		for (IAbstractStateChangeListener l : m_stateChangeListeners) {
			List<AbstractState> statesCopy = new ArrayList<AbstractState>(
					oldStates.size());
			statesCopy.addAll(oldStates);
			l.onStateChange(viaEdge, statesCopy, newState, mergedState);
		}
	}

	/**
	 * Reports a possible error as the plugin's result
	 * 
	 * @param location
	 *            The error location
	 * @param state
	 *            The abstract state at the error location
	 */
	private UnprovableResult<RcfgElement, CodeBlock, Expression> reportErrorResult(
			ProgramPoint location, AbstractState state) {
		RcfgProgramExecution programExecution = new RcfgProgramExecution(
				state.getTrace(),
				new HashMap<Integer, ProgramState<Expression>>(), null);

		UnprovableResult<RcfgElement, CodeBlock, Expression> result = new UnprovableResult<RcfgElement, CodeBlock, Expression>(
				Activator.s_PLUGIN_NAME, location,
				m_services.getBacktranslationService(), programExecution);

		if (m_stopAfterAnyError) {
			m_continueProcessing = false;
			mLogger.info(String
					.format("Abstract interpretation finished after reaching error location %s",
							location.toString()));
		} else if (m_stopAfterAllErrors) {
			if (m_reachedErrorLocs.containsAll(m_errorLocs))
				m_continueProcessing = false;
			mLogger.info("Abstract interpretation finished after reaching all error locations");
		}
		m_result.add(result);
		return result;
	}

	private void reportUnsupportedSyntaxResult(IElement location, String message) {
		UnsupportedSyntaxResult<IElement> result = new UnsupportedSyntaxResult<IElement>(
				location, Activator.s_PLUGIN_NAME,
				m_services.getBacktranslationService(), message);

		m_services.getResultService().reportResult(Activator.s_PLUGIN_ID,
				result);

		m_continueProcessing = false;
	}

	/**
	 * Reports safety of the program as the plugin's result
	 */
	private void reportSafeResult() {
		m_services.getResultService().reportResult(
				Activator.s_PLUGIN_ID,
				new AllSpecificationsHoldResult(Activator.s_PLUGIN_NAME,
						"No error locations were reached."));
	}

	/**
	 * Reports a timeout of the analysis
	 */
	private void reportTimeoutResult() {
		m_services.getResultService()
				.reportResult(
						Activator.s_PLUGIN_ID,
						new TimeoutResult(Activator.s_PLUGIN_NAME,
								"Analysis aborted."));
	}

	/**
	 * Get preferences from the preference store
	 */
	private void fetchPreferences() {
		UltimatePreferenceStore prefs = new UltimatePreferenceStore(
				Activator.s_PLUGIN_ID);

		m_mainMethodName = prefs
				.getString(AbstractInterpretationPreferenceInitializer.LABEL_MAIN_METHOD_NAME);

		m_iterationsUntilWidening = prefs
				.getInt(AbstractInterpretationPreferenceInitializer.LABEL_ITERATIONS_UNTIL_WIDENING);
		m_parallelStatesUntilMerge = prefs
				.getInt(AbstractInterpretationPreferenceInitializer.LABEL_STATES_UNTIL_MERGE);

		m_widening_fixedNumbers = prefs
				.getString(AbstractInterpretationPreferenceInitializer.LABEL_WIDENING_FIXEDNUMBERS);
		m_widening_autoNumbers = prefs
				.getBoolean(AbstractInterpretationPreferenceInitializer.LABEL_WIDENING_AUTONUMBERS);

		m_generateStateAnnotations = prefs
				.getBoolean(AbstractInterpretationPreferenceInitializer.LABEL_STATE_ANNOTATIONS);
		m_stateChangeLogConsole = prefs
				.getBoolean(AbstractInterpretationPreferenceInitializer.LABEL_LOGSTATES_CONSOLE);
		m_stateChangeLogFile = prefs
				.getBoolean(AbstractInterpretationPreferenceInitializer.LABEL_LOGSTATES_FILE);
		m_stateChangeLogUseSourcePath = prefs
				.getBoolean(AbstractInterpretationPreferenceInitializer.LABEL_LOGSTATES_USESOURCEPATH);
		m_stateChangeLogPath = prefs
				.getString(AbstractInterpretationPreferenceInitializer.LABEL_LOGSTATES_PATH);

		String stopAfter = prefs
				.getString(AbstractInterpretationPreferenceInitializer.LABEL_STOPAFTER);
		m_stopAfterAnyError = (stopAfter
				.equals(AbstractInterpretationPreferenceInitializer.OPTION_STOPAFTER_ANYERROR));
		m_stopAfterAllErrors = (stopAfter
				.equals(AbstractInterpretationPreferenceInitializer.OPTION_STOPAFTER_ALLERRORS));

		m_intDomainID = prefs
				.getString(AbstractInterpretationPreferenceInitializer.LABEL_INTDOMAIN);
		m_intWideningOpName = prefs.getString(String.format(
				AbstractInterpretationPreferenceInitializer.LABEL_WIDENINGOP,
				m_intDomainID));
		m_intMergeOpName = prefs.getString(String.format(
				AbstractInterpretationPreferenceInitializer.LABEL_MERGEOP,
				m_intDomainID));

		m_realDomainID = prefs
				.getString(AbstractInterpretationPreferenceInitializer.LABEL_REALDOMAIN);
		m_realWideningOpName = prefs.getString(String.format(
				AbstractInterpretationPreferenceInitializer.LABEL_WIDENINGOP,
				m_realDomainID));
		m_realMergeOpName = prefs.getString(String.format(
				AbstractInterpretationPreferenceInitializer.LABEL_MERGEOP,
				m_realDomainID));

		m_boolDomainID = prefs
				.getString(AbstractInterpretationPreferenceInitializer.LABEL_BOOLDOMAIN);
		m_boolWideningOpName = prefs.getString(String.format(
				AbstractInterpretationPreferenceInitializer.LABEL_WIDENINGOP,
				m_boolDomainID));
		m_boolMergeOpName = prefs.getString(String.format(
				AbstractInterpretationPreferenceInitializer.LABEL_MERGEOP,
				m_boolDomainID));

		m_bitVectorDomainID = prefs
				.getString(AbstractInterpretationPreferenceInitializer.LABEL_BITVECTORDOMAIN);
		m_bitVectorWideningOpName = prefs.getString(String.format(
				AbstractInterpretationPreferenceInitializer.LABEL_WIDENINGOP,
				m_bitVectorDomainID));
		m_bitVectorMergeOpName = prefs.getString(String.format(
				AbstractInterpretationPreferenceInitializer.LABEL_MERGEOP,
				m_bitVectorDomainID));

		m_stringDomainID = prefs
				.getString(AbstractInterpretationPreferenceInitializer.LABEL_STRINGDOMAIN);
		m_stringWideningOpName = prefs.getString(String.format(
				AbstractInterpretationPreferenceInitializer.LABEL_WIDENINGOP,
				m_stringDomainID));
		m_stringMergeOpName = prefs.getString(String.format(
				AbstractInterpretationPreferenceInitializer.LABEL_MERGEOP,
				m_stringDomainID));
	}

	/**
	 * @param domainID
	 * @param wideningOpName
	 * @param mergeOpName
	 * @return An abstract domain factory for the abstract domain system given
	 *         by its ID
	 */
	private IAbstractDomainFactory<?> makeDomainFactory(String domainID,
			String wideningOpName, String mergeOpName) {
		if (domainID.equals(TopBottomDomainFactory.getDomainID()))
			return new TopBottomDomainFactory(mLogger);

		if (domainID.equals(BoolDomainFactory.getDomainID()))
			return new BoolDomainFactory(mLogger);

		if (domainID.equals(SignDomainFactory.getDomainID()))
			return new SignDomainFactory(mLogger, wideningOpName, mergeOpName);

		if (domainID.equals(IntervalDomainFactory.getDomainID()))
			return new IntervalDomainFactory(mLogger, new HashSet<String>(
					m_numbersForWidening), wideningOpName, mergeOpName);

		// default ADS: TOPBOTTOM
		mLogger.warn(String
				.format("Unknown abstract domain system \"%s\" chosen, using \"%s\" instead",
						domainID, TopBottomDomainFactory.getDomainID()));
		return new TopBottomDomainFactory(mLogger);
	}
}
