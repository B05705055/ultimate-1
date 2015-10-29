/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.automata.Word;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lassoranker.SMTSolver;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.model.boogie.BoogieVar;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.RcfgProgramExecution;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.TraceAbstractionBenchmarks;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.benchmark.BenchmarkGeneratorWithStopwatches;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.benchmark.IBenchmarkDataProvider;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.benchmark.IBenchmarkType;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TraceAbstractionPreferenceInitializer.AssertCodeBlockOrder;
import de.uni_freiburg.informatik.ultimate.result.IProgramExecution.ProgramState;
import de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException;


/**
 * Check if a trace fulfills a specification. Provides an execution (that 
 * violates the specification) if the check was negative.
 * <p>
 * Given
 * <ul>
 * <li>a precondition stated by predicate φ_0
 * <li>a postcondition stated by predicate φ_n
 * <li>a trace (which is a word of CodeBlocks) cb_0 cb_2 ... cb_{n-1},
 * </ul>
 * check if the trace always fulfills the postcondition φ_n if the precondition
 * φ_0 holds before the execution of the trace, i.e. we check if the following
 * inclusion of predicates is valid. post(φ_0, cb_1 cb_2 ... cb_n) ⊆ φ_n
 * <p>
 * A feasibility check of a trace can be seen as the special case of this trace
 * check. A trace is feasible if and only if the trace does not fulfill the
 * specification given by the precondition <i>true</i> and the postcondition
 * <i>false</i>. See Example1.
 * <p>
 * Example1: If
 * <ul>
 * <li>the precondition is the predicate <i>true</i>,
 * <li>the postcondition is the predicate <i>false</i>,
 * <li>and the trace cb_0 cb_1 is x:=0; x!=-1;,
 * </ul>
 * <p>
 * then the trace fulfills its specification.
 * <p>
 * Example2: If
 * <ul>
 * <li>the precondition is the predicate x==0,
 * <li>the postcondition is the predicate x==1,
 * <li>and the trace cb_0 cb_1 is x++; x++;,
 * </ul>
 * <p>
 * then the trace does not fulfill its specification.
 * <p>
 * 
 * @author heizmann@informatik.uni-freiburg.de
 */
public class TraceChecker {

	protected final Logger mLogger;
	/**
	 * After constructing a new TraceChecker satisfiability of the trace was
	 * checked. However, the trace check is not yet finished, and the SmtManager
	 * is still locked by this TraceChecker to allow the computation of
	 * an interpolants or an execution.
	 * The trace check is only finished after the unlockSmtManager() method was 
	 * called.
	 * 
	 */
	protected boolean m_TraceCheckFinished;
	/**
	 * Interface for query the SMT solver.
	 */
	protected final SmtManager m_SmtManager;
	protected final SmtManager m_TcSmtManager;
	/**
	 * Maps a procedure name to the set of global variables which may be
	 * modified by the procedure. The set of variables is represented as a map
	 * where the identifier of the variable is mapped to the type of the
	 * variable.
	 */
	protected final ModifiableGlobalVariableManager m_ModifiedGlobals;
	protected final NestedWord<CodeBlock> m_Trace;
	protected final IPredicate m_Precondition;
	protected final IPredicate m_Postcondition;
	/**
	 * If the trace contains "pending returns" (returns without corresponding
	 * calls) we have to provide a predicate for each pending return that
	 * specifies what held in the calling context to which we return. (If the
	 * trace would contain the corresponding call, this predicate would be the
	 * predecessor of the call). We call these predicates "pending contexts".
	 * These pending contexts are provided via a mapping from the position of
	 * the pending return (given as Integer) to the predicate.
	 */
	protected final SortedMap<Integer, IPredicate> m_PendingContexts;
	protected AnnotateAndAsserter m_AAA;
	protected final LBool m_IsSafe;
	protected RcfgProgramExecution m_RcfgProgramExecution;
	protected final NestedFormulas<TransFormula, IPredicate> m_NestedFormulas;
	protected NestedSsaBuilder m_Nsb;
	protected final TraceCheckerBenchmarkGenerator m_TraceCheckerBenchmarkGenerator;
	protected final AssertCodeBlockOrder m_assertCodeBlocksIncrementally;
	protected final IUltimateServiceProvider mServices;
	protected ToolchainCanceledException m_ToolchainCanceledException;

	/**
	 * Defines benchmark for measuring data about the usage of TraceCheckers.
	 * E.g., number and size of predicates obtained via interpolation.
	 * 
	 * @author Matthias Heizmann
	 * 
	 */
	public static class TraceCheckerBenchmarkType implements IBenchmarkType {
	
		private static TraceCheckerBenchmarkType s_Instance = new TraceCheckerBenchmarkType();
	
		protected final static String s_SsaConstruction = "SsaConstructionTime";
		protected final static String s_SatisfiabilityAnalysis = "SatisfiabilityAnalysisTime";
		protected final static String s_InterpolantComputation = "InterpolantComputationTime";
	
		protected final static String s_NumberOfCodeBlocks = "NumberOfCodeBlocks";
		protected final static String s_NumberOfCodeBlocksAsserted = "NumberOfCodeBlocksAsserted";
		protected final static String s_NumberOfCheckSat = "NumberOfCheckSat";
	
		public static TraceCheckerBenchmarkType getInstance() {
			return s_Instance;
		}
	
		@Override
		public Collection<String> getKeys() {
			return Arrays.asList(new String[] { s_SsaConstruction, s_SatisfiabilityAnalysis, s_InterpolantComputation,
					s_NumberOfCodeBlocks, s_NumberOfCodeBlocksAsserted, s_NumberOfCheckSat });
		}
	
		@Override
		public Object aggregate(String key, Object value1, Object value2) {
			switch (key) {
			case s_SsaConstruction:
			case s_SatisfiabilityAnalysis:
			case s_InterpolantComputation:
				Long time1 = (Long) value1;
				Long time2 = (Long) value2;
				return time1 + time2;
			case s_NumberOfCodeBlocks:
			case s_NumberOfCodeBlocksAsserted:
			case s_NumberOfCheckSat:
				Integer number1 = (Integer) value1;
				Integer number2 = (Integer) value2;
				return number1 + number2;
			default:
				throw new AssertionError("unknown key");
			}
		}
	
		@Override
		public String prettyprintBenchmarkData(IBenchmarkDataProvider benchmarkData) {
			StringBuilder sb = new StringBuilder();
			sb.append(s_SsaConstruction);
			sb.append(": ");
			Long ssaConstructionTime = (Long) benchmarkData.getValue(s_SsaConstruction);
			sb.append(TraceAbstractionBenchmarks.prettyprintNanoseconds(ssaConstructionTime));
			sb.append(" ");
			sb.append(s_SatisfiabilityAnalysis);
			sb.append(": ");
			Long satisfiabilityAnalysisTime = (Long) benchmarkData.getValue(s_SatisfiabilityAnalysis);
			sb.append(TraceAbstractionBenchmarks.prettyprintNanoseconds(satisfiabilityAnalysisTime));
			sb.append(" ");
			sb.append(s_InterpolantComputation);
			sb.append(": ");
			Long interpolantComputationTime = (Long) benchmarkData.getValue(s_InterpolantComputation);
			sb.append(TraceAbstractionBenchmarks.prettyprintNanoseconds(interpolantComputationTime));
			sb.append(" ");
			sb.append(s_NumberOfCodeBlocks);
			sb.append(": ");
			sb.append(benchmarkData.getValue(s_NumberOfCodeBlocks));
			sb.append(" ");
			sb.append(s_NumberOfCodeBlocksAsserted);
			sb.append(": ");
			sb.append(benchmarkData.getValue(s_NumberOfCodeBlocksAsserted));
			sb.append(" ");
			sb.append(s_NumberOfCheckSat);
			sb.append(": ");
			sb.append(benchmarkData.getValue(s_NumberOfCheckSat));
			return sb.toString();
		}
	}
	
	/**
	 * Stores benchmark data about the usage of TraceCheckers. E.g., number and
	 * size of predicates obtained via interpolation.
	 * 
	 * @author Matthias Heizmann
	 */
	public class TraceCheckerBenchmarkGenerator extends BenchmarkGeneratorWithStopwatches implements
			IBenchmarkDataProvider {

		int m_NumberOfCodeBlocks = 0;
		int m_NumberOfCodeBlocksAsserted = 0;
		int m_NumberOfCheckSat = 0;

		@Override
		public String[] getStopwatches() {
			return new String[] { TraceCheckerBenchmarkType.s_SsaConstruction,
					TraceCheckerBenchmarkType.s_SatisfiabilityAnalysis,
					TraceCheckerBenchmarkType.s_InterpolantComputation };
		}

		@Override
		public Collection<String> getKeys() {
			return TraceCheckerBenchmarkType.getInstance().getKeys();
		}

		@Override
		public Object getValue(String key) {
			switch (key) {
			case TraceCheckerBenchmarkType.s_SsaConstruction:
			case TraceCheckerBenchmarkType.s_SatisfiabilityAnalysis:
			case TraceCheckerBenchmarkType.s_InterpolantComputation:
				try {
					return getElapsedTime(key);
				} catch (StopwatchStillRunningException e) {
					throw new AssertionError("clock still running: " + key);
				}
			case TraceCheckerBenchmarkType.s_NumberOfCodeBlocks:
				return m_NumberOfCodeBlocks;
			case TraceCheckerBenchmarkType.s_NumberOfCodeBlocksAsserted:
				return m_NumberOfCodeBlocksAsserted;
			case TraceCheckerBenchmarkType.s_NumberOfCheckSat:
				return m_NumberOfCheckSat;
			default:
				throw new AssertionError("unknown data");
			}
		}

		@Override
		public IBenchmarkType getBenchmarkType() {
			return TraceCheckerBenchmarkType.getInstance();
		}

		/**
		 * Tell the Benchmark that the checked trace has n CodeBlocks
		 */
		public void reportnewCodeBlocks(int n) {
			m_NumberOfCodeBlocks = m_NumberOfCodeBlocks + n;
		}

		/**
		 * Tell the Benchmark that n CodeBlocks have been asserted additionally
		 */
		public void reportnewAssertedCodeBlocks(int n) {
			m_NumberOfCodeBlocksAsserted = m_NumberOfCodeBlocksAsserted + n;
		}

		/**
		 * Tell the Benchmark we did another check sat
		 */
		public void reportnewCheckSat() {
			m_NumberOfCheckSat++;
		}

	}

	protected TraceCheckerBenchmarkGenerator getBenchmarkGenerator() {
		return new TraceCheckerBenchmarkGenerator();
	}

	/**
	 * Returns
	 * <ul>
	 * <li>SAT if the trace does not fulfill its specification,
	 * <li>UNSAT if the trace does fulfill its specification,
	 * <li>UNKNOWN if it was not possible to determine if the trace fulfills its
	 * specification.
	 * </ul>
	 */
	public LBool isCorrect() {
		return m_IsSafe;
	}
	
	
	/**
	 * Check if trace fulfills specification given by precondition,
	 * postcondition and pending contexts. The pendingContext maps the positions
	 * of pending returns to predicates which define possible variable
	 * valuations in the context to which the return leads the trace.
	 * 
	 * @param assertCodeBlocksIncrementally
	 *            If set to false, check-sat is called after all CodeBlocks are
	 *            asserted. If set to true we use Betims heuristic an
	 *            incrementally assert CodeBlocks and do check-sat until all
	 *            CodeBlocks are asserted or the result to a check-sat is UNSAT.
	 * @param logger
	 * @param services
	 */
	public TraceChecker(IPredicate precondition, IPredicate postcondition,
			SortedMap<Integer, IPredicate> pendingContexts, NestedWord<CodeBlock> trace, SmtManager smtManager,
			ModifiableGlobalVariableManager modifiedGlobals, AssertCodeBlockOrder assertCodeBlocksIncrementally,
			IUltimateServiceProvider services, boolean computeRcfgProgramExecution) {
		this(precondition, postcondition, pendingContexts, trace, smtManager, modifiedGlobals,
				new DefaultTransFormulas(trace, precondition, postcondition, pendingContexts, modifiedGlobals, false),
				assertCodeBlocksIncrementally, services, computeRcfgProgramExecution, true);
	}
	
	protected TraceChecker(IPredicate precondition, IPredicate postcondition,
			SortedMap<Integer, IPredicate> pendingContexts, NestedWord<CodeBlock> trace, SmtManager smtManager,
			ModifiableGlobalVariableManager modifiedGlobals, NestedFormulas<TransFormula, IPredicate> rv,
			AssertCodeBlockOrder assertCodeBlocksIncrementally, IUltimateServiceProvider services,
			boolean computeRcfgProgramExecution, boolean unlockSmtSolverAlsoIfUnsat) {
		this(precondition, postcondition, pendingContexts, trace, smtManager, 
				modifiedGlobals, rv, assertCodeBlocksIncrementally, services, 
				computeRcfgProgramExecution, unlockSmtSolverAlsoIfUnsat, smtManager);
	}
	
	
	
	/**
	 * Commit additionally the DefaultTransFormulas
	 * 
	 * @param services
	 * 
	 */
	protected TraceChecker(IPredicate precondition, IPredicate postcondition,
			SortedMap<Integer, IPredicate> pendingContexts, NestedWord<CodeBlock> trace, SmtManager smtManager,
			ModifiableGlobalVariableManager modifiedGlobals, NestedFormulas<TransFormula, IPredicate> rv,
			AssertCodeBlockOrder assertCodeBlocksIncrementally, IUltimateServiceProvider services,
			boolean computeRcfgProgramExecution, boolean unlockSmtSolverAlsoIfUnsat, SmtManager tcSmtManager) {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(Activator.s_PLUGIN_ID);
		m_SmtManager = smtManager;
		m_TcSmtManager = tcSmtManager;
		m_ModifiedGlobals = modifiedGlobals;
		m_Trace = trace;
		m_Precondition = precondition;
		m_Postcondition = postcondition;
		if (pendingContexts == null) {
			throw new NullPointerException("pendingContexts must not be "
					+ "null, if there are no pending contexts, use an empty map");
		}
		m_PendingContexts = pendingContexts;
		m_NestedFormulas = rv;
		m_TraceCheckerBenchmarkGenerator = getBenchmarkGenerator();
		m_assertCodeBlocksIncrementally = assertCodeBlocksIncrementally;
		LBool isSafe = null;
		try {
			isSafe = checkTrace();
			if (isSafe == LBool.UNSAT) {
				if (unlockSmtSolverAlsoIfUnsat) {
					unlockSmtManager();
				}
			} else {
				if (computeRcfgProgramExecution) {
					computeRcfgProgramExecution(isSafe);
				} else {
					m_TraceCheckFinished = true;
					unlockSmtManager();
				}
			}
		} catch (ToolchainCanceledException tce) {
			m_ToolchainCanceledException = tce;
		} finally {
			m_IsSafe = isSafe;
		}
	}
	

	/**
	 * Like three-argument-checkTrace-Method above but for traces which contain
	 * pending returns. The pendingContext maps the positions of pending returns
	 * to predicates which define possible variable valuations in the context to
	 * which the return leads the trace.
	 * 
	 */
	protected LBool checkTrace() {
		LBool isSafe;
		m_SmtManager.startTraceCheck(this);
		boolean transferToDifferentScript = (m_TcSmtManager != m_SmtManager);
		m_TraceCheckerBenchmarkGenerator.start(TraceCheckerBenchmarkType.s_SsaConstruction);
		m_Nsb = new NestedSsaBuilder(m_Trace, m_TcSmtManager, m_NestedFormulas,
				m_ModifiedGlobals, mLogger, transferToDifferentScript);
		NestedFormulas<Term, Term> ssa = m_Nsb.getSsa();
		m_TraceCheckerBenchmarkGenerator.stop(TraceCheckerBenchmarkType.s_SsaConstruction);
	
		m_TraceCheckerBenchmarkGenerator.start(TraceCheckerBenchmarkType.s_SatisfiabilityAnalysis);
		if (m_assertCodeBlocksIncrementally != AssertCodeBlockOrder.NOT_INCREMENTALLY) {
			m_AAA = new AnnotateAndAsserterWithStmtOrderPrioritization(m_TcSmtManager, ssa,
					getAnnotateAndAsserterCodeBlocks(ssa), m_TraceCheckerBenchmarkGenerator,
					m_assertCodeBlocksIncrementally, mLogger);
		} else {
			m_AAA = new AnnotateAndAsserter(m_TcSmtManager, ssa, getAnnotateAndAsserterCodeBlocks(ssa),
					m_TraceCheckerBenchmarkGenerator, mLogger);
			// Report the asserted code blocks
//			m_TraceCheckerBenchmarkGenerator.reportnewAssertedCodeBlocks(m_Trace.length());
		}
		try {
			m_AAA.buildAnnotatedSsaAndAssertTerms();
			isSafe = m_AAA.isInputSatisfiable();
		} catch (SMTLIBException e) {
			if (e.getMessage().equals("Unsupported non-linear arithmetic")) {
				isSafe = LBool.UNKNOWN;
			} else {
				throw e;
			}
		} finally {
			m_TraceCheckerBenchmarkGenerator.stop(TraceCheckerBenchmarkType.s_SatisfiabilityAnalysis);
		}
		return isSafe;
	}

	/**
	 * Compute a program execution for the checked trace.
	 * <ul>
	 * <li>If the checked trace violates its specification (result of trace
	 * check is SAT), we compute a program execution that contains program
	 * states that witness the violation of the specification (however, this can
	 * still be partial program states e.g., no values assigned to arrays) and
	 * that contains information which branch of a parallel composed CodeBlock
	 * violates the specification.
	 * <li>If we can not determine if the trace violates its specification
	 * (result of trace check is UNKNOWN) we compute a program execution trace
	 * that contains neither states nor information about which branch of a
	 * parallel composed CodeBlock violates the specification.
	 * <li>If we have proven that the trace satisfies its specification (result
	 * of trace check is UNSAT) we throw an Error.
	 * @param isSafe 
	 */
	private void computeRcfgProgramExecution(LBool isSafe) {
		if (!(m_NestedFormulas instanceof DefaultTransFormulas)) {
			throw new AssertionError("program execution only computable if "
					+ "m_NestedFormulas instanceof DefaultTransFormulas");
		}
		if (isSafe == LBool.SAT) {
			if (!((DefaultTransFormulas) m_NestedFormulas).hasBranchEncoders()) {
				unlockSmtManager();
				DefaultTransFormulas withBE = new DefaultTransFormulas(m_NestedFormulas.getTrace(),
						m_NestedFormulas.getPrecondition(), m_NestedFormulas.getPostcondition(),
						m_PendingContexts, m_ModifiedGlobals, true);
				TraceChecker tc = new TraceChecker(m_NestedFormulas.getPrecondition(),
						m_NestedFormulas.getPostcondition(), m_PendingContexts,
						m_NestedFormulas.getTrace(), m_SmtManager, m_ModifiedGlobals, withBE,
						AssertCodeBlockOrder.NOT_INCREMENTALLY, mServices, true, true);
				if (tc.getToolchainCancelledExpection() != null) {
					throw tc.getToolchainCancelledExpection();
				}
				assert tc.isCorrect() == LBool.SAT;
				m_RcfgProgramExecution = tc.getRcfgProgramExecution();
			} else {
				m_RcfgProgramExecution = computeRcfgProgramExecutionCaseSAT(m_Nsb);
			}
		} else if (isSafe == LBool.UNKNOWN) {
			m_RcfgProgramExecution = computeRcfgProgramExecutionCaseUNKNOWN();
		} else if (isSafe == LBool.UNSAT) {
			throw new AssertionError("specification satisfied - " + "cannot compute counterexample");
		} else {
			throw new AssertionError("unexpected result of correctness check");
		}
		m_TraceCheckFinished = true;
	}

	/**
	 * Compute program execution in the case that we do not know if the checked
	 * specification is violated (result of trace check is UNKNOWN).
	 */
	private RcfgProgramExecution computeRcfgProgramExecutionCaseUNKNOWN() {
		Map<Integer, ProgramState<Expression>> emptyMap = Collections.emptyMap();
		Map<TermVariable, Boolean>[] branchEncoders = new Map[0];
		unlockSmtManager();
		m_TraceCheckFinished = true;
		return new RcfgProgramExecution(m_NestedFormulas.getTrace().lettersAsList(), emptyMap, branchEncoders);
	}

	/**
	 * Compute program execution in the case that the checked specification is
	 * violated (result of trace check is SAT).
	 */
	private RcfgProgramExecution computeRcfgProgramExecutionCaseSAT(NestedSsaBuilder nsb) {
		RelevantVariables relVars = new RelevantVariables(m_NestedFormulas, m_ModifiedGlobals);
		RcfgProgramExecutionBuilder rpeb = new RcfgProgramExecutionBuilder(m_ModifiedGlobals,
				(NestedWord<CodeBlock>) m_Trace, relVars, m_SmtManager.getBoogie2Smt().getBoogie2SmtSymbolTable());
		for (int i = 0; i < m_Trace.length(); i++) {
			CodeBlock cb = m_Trace.getSymbolAt(i);
			TransFormula tf = cb.getTransitionFormulaWithBranchEncoders();
			if (tf.getBranchEncoders().size() > 0) {
				Map<TermVariable, Boolean> beMapping = new HashMap<TermVariable, Boolean>();
				for (TermVariable tv : tf.getBranchEncoders()) {
					String nameOfConstant = NestedSsaBuilder.branchEncoderConstantName(tv, i);
					Term indexedBe = m_SmtManager.getScript().term(nameOfConstant);
					Term value = getValue(indexedBe);
					Boolean booleanValue = getBooleanValue(value);
					beMapping.put(tv, booleanValue);
				}
				rpeb.setBranchEncoders(i, beMapping);
			}
		}
		for (BoogieVar bv : nsb.getIndexedVarRepresentative().keySet()) {
			if (bv.getTermVariable().getSort().isNumericSort()
					|| bv.getTermVariable().getSort().getRealSort().getName().equals("Bool")
					|| bv.getTermVariable().getSort().getRealSort().getName().equals("BitVec")) {
				for (Integer index : nsb.getIndexedVarRepresentative().get(bv).keySet()) {
					Term indexedVar = nsb.getIndexedVarRepresentative().get(bv).get(index);
					Term valueT = getValue(indexedVar);
					Expression valueE = m_SmtManager.getBoogie2Smt().getTerm2Expression().translate(valueT);
					rpeb.addValueAtVarAssignmentPosition(bv, index, valueE);
				}
			}
		}
		unlockSmtManager();
		return rpeb.getRcfgProgramExecution();
	}

	protected AnnotateAndAssertCodeBlocks getAnnotateAndAsserterCodeBlocks(NestedFormulas<Term, Term> ssa) {
		return new AnnotateAndAssertCodeBlocks(m_SmtManager, ssa, mLogger);
	
		// AnnotateAndAssertCodeBlocks aaacb =
		// return new AnnotateAndAsserter(m_SmtManager, ssa, aaacb);
	}

	private Term getValue(Term term) {
		Term[] arr = { term };
		Map<Term, Term> map = m_SmtManager.getScript().getValue(arr);
		Term value = map.get(term);
		return value;
	}

	private Boolean getBooleanValue(Term term) {
		Boolean result;
		Term trueTerm = m_SmtManager.getScript().term("true");
		if (term.equals(trueTerm)) {
			result = true;
		} else {
			Term falseTerm = m_SmtManager.getScript().term("false");
			if (term.equals(falseTerm)) {
				result = false;
			} else {
				throw new AssertionError();
			}
		}
		return result;
	}

	public NestedWord<CodeBlock> getTrace() {
		return m_Trace;
	}

	public IPredicate getPrecondition() {
		return m_Precondition;
	}

	public IPredicate getPostcondition() {
		return m_Postcondition;
	}

	public Map<Integer, IPredicate> getPendingContexts() {
		return m_PendingContexts;
	}

	/**
	 * Return the RcfgProgramExecution that has been computed by
	 * computeRcfgProgramExecution().
	 */
	public RcfgProgramExecution getRcfgProgramExecution() {
		if (m_RcfgProgramExecution == null) {
			throw new AssertionError("program execution has not yet been computed");
		}
		return m_RcfgProgramExecution;
	}

	protected void unlockSmtManager() {
		m_SmtManager.endTraceCheck(this);
	}


	public TraceCheckerBenchmarkGenerator getTraceCheckerBenchmark() {
		if (m_TraceCheckFinished || m_ToolchainCanceledException != null) {
			return m_TraceCheckerBenchmarkGenerator;
		} else {
			throw new AssertionError("Benchmark is only available after the trace check is finished.");
		}
	}
	
	/**
	 * Returns the {@link ToolchainCanceledException} that was thrown if
	 * the computation was cancelled. If the computation was not cancelled,
	 * we return null.
	 */
	public ToolchainCanceledException getToolchainCancelledExpection() {
		return m_ToolchainCanceledException;
	}

}
