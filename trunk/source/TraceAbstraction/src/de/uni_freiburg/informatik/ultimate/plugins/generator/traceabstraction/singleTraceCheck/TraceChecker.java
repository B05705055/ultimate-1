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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.core.model.translation.IProgramExecution.ProgramState;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.ContainsQuantifier;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.TermTransferrer;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.AffineTerm;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.AffineTermTransformer;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.managedscript.ManagedScript;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RCFGEdge;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.util.RcfgProgramExecution;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.TraceAbstractionBenchmarks;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TraceAbstractionPreferenceInitializer.AssertCodeBlockOrder;
import de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException;
import de.uni_freiburg.informatik.ultimate.util.statistics.IStatisticsDataProvider;
import de.uni_freiburg.informatik.ultimate.util.statistics.IStatisticsType;
import de.uni_freiburg.informatik.ultimate.util.statistics.StatisticsGeneratorWithStopwatches;

/**
 * Check if a trace fulfills a specification. Provides an execution (that violates the specification) if the check was
 * negative.
 * <p>
 * Given
 * <ul>
 * <li>a precondition stated by predicate φ_0
 * <li>a postcondition stated by predicate φ_n
 * <li>a trace (which is a word of CodeBlocks) cb_0 cb_2 ... cb_{n-1},
 * </ul>
 * check if the trace always fulfills the postcondition φ_n if the precondition φ_0 holds before the execution of the
 * trace, i.e. we check if the following inclusion of predicates is valid. post(φ_0, cb_1 cb_2 ... cb_n) ⊆ φ_n
 * <p>
 * A feasibility check of a trace can be seen as the special case of this trace check. A trace is feasible if and only
 * if the trace does not fulfill the specification given by the precondition <i>true</i> and the postcondition
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

	protected final ILogger mLogger;
	protected final IUltimateServiceProvider mServices;
	/**
	 * After constructing a new TraceChecker satisfiability of the trace was checked. However, the trace check is not
	 * yet finished, and the SmtManager is still locked by this TraceChecker to allow the computation of an interpolants
	 * or an execution. The trace check is only finished after the unlockSmtManager() method was called.
	 * 
	 */
	protected boolean mTraceCheckFinished;
	/**
	 * Interface for query the SMT solver.
	 */
	protected final SmtManager mSmtManager;
	protected final ManagedScript mManagedScript;
	protected final Script mScript;
	protected final SmtManager mTcSmtManager;
	/**
	 * Maps a procedure name to the set of global variables which may be modified by the procedure. The set of variables
	 * is represented as a map where the identifier of the variable is mapped to the type of the variable.
	 */
	protected final ModifiableGlobalVariableManager mModifiedGlobals;
	protected final NestedWord<? extends IAction> mTrace;
	protected final IPredicate mPrecondition;
	protected final IPredicate mPostcondition;
	/**
	 * If the trace contains "pending returns" (returns without corresponding calls) we have to provide a predicate for
	 * each pending return that specifies what held in the calling context to which we return. (If the trace would
	 * contain the corresponding call, this predicate would be the predecessor of the call). We call these predicates
	 * "pending contexts". These pending contexts are provided via a mapping from the position of the pending return
	 * (given as Integer) to the predicate.
	 */
	protected final SortedMap<Integer, IPredicate> mPendingContexts;
	protected AnnotateAndAsserter mAAA;
	protected final LBool mIsSafe;
	protected RcfgProgramExecution mRcfgProgramExecution;
	protected final NestedFormulas<TransFormula, IPredicate> mNestedFormulas;
	protected NestedSsaBuilder mNsb;
	protected final TraceCheckerBenchmarkGenerator mTraceCheckerBenchmarkGenerator;
	protected final AssertCodeBlockOrder massertCodeBlocksIncrementally;
	protected ToolchainCanceledException mToolchainCanceledException;

	/**
	 * Defines benchmark for measuring data about the usage of TraceCheckers. E.g., number and size of predicates
	 * obtained via interpolation.
	 * 
	 * @author Matthias Heizmann
	 * 
	 */
	public static class TraceCheckerBenchmarkType implements IStatisticsType {

		private static TraceCheckerBenchmarkType s_Instance = new TraceCheckerBenchmarkType();

		protected final static String s_SsaConstruction = "SsaConstructionTime";
		protected final static String s_SatisfiabilityAnalysis = "SatisfiabilityAnalysisTime";
		protected final static String s_InterpolantComputation = "InterpolantComputationTime";

		protected final static String s_NumberOfCodeBlocks = "NumberOfCodeBlocks";
		protected final static String s_NumberOfCodeBlocksAsserted = "NumberOfCodeBlocksAsserted";
		protected final static String s_NumberOfCheckSat = "NumberOfCheckSat";
		protected final static String s_ConstructedInterpolants = "ConstructedInterpolants";
		protected final static String s_QuantifiedInterpolants = "QuantifiedInterpolants";

		public static TraceCheckerBenchmarkType getInstance() {
			return s_Instance;
		}

		@Override
		public Collection<String> getKeys() {
			return Arrays.asList(new String[] { s_SsaConstruction, s_SatisfiabilityAnalysis, s_InterpolantComputation,
					s_NumberOfCodeBlocks, s_NumberOfCodeBlocksAsserted, s_NumberOfCheckSat, s_ConstructedInterpolants,
					s_QuantifiedInterpolants});
		}

		@Override
		public Object aggregate(final String key, final Object value1, final Object value2) {
			switch (key) {
			case s_SsaConstruction:
			case s_SatisfiabilityAnalysis:
			case s_InterpolantComputation:
				final Long time1 = (Long) value1;
				final Long time2 = (Long) value2;
				return time1 + time2;
			case s_NumberOfCodeBlocks:
			case s_NumberOfCodeBlocksAsserted:
			case s_NumberOfCheckSat:
			case s_ConstructedInterpolants:
			case s_QuantifiedInterpolants:
				final Integer number1 = (Integer) value1;
				final Integer number2 = (Integer) value2;
				return number1 + number2;
			default:
				throw new AssertionError("unknown key");
			}
		}

		@Override
		public String prettyprintBenchmarkData(final IStatisticsDataProvider benchmarkData) {
			final StringBuilder sb = new StringBuilder();
			addTimedStatistic(benchmarkData, sb,s_SsaConstruction);
			addTimedStatistic(benchmarkData, sb,s_SatisfiabilityAnalysis);
			addTimedStatistic(benchmarkData, sb,s_InterpolantComputation);
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
			sb.append(" ");
			final Integer quantifiedInterpolants = (Integer) benchmarkData.getValue(s_QuantifiedInterpolants);
			final Integer constructedInterpolants = (Integer) benchmarkData.getValue(s_ConstructedInterpolants);
			sb.append(s_QuantifiedInterpolants);
			sb.append(": ");
			sb.append(benchmarkData.getValue(s_QuantifiedInterpolants));
			sb.append("/");
			sb.append(benchmarkData.getValue(s_ConstructedInterpolants));
			sb.append("=");
			final double percent;
			if (constructedInterpolants == 0) {
				percent = 0;
			} else {
				percent = (((double) quantifiedInterpolants) / ((double) constructedInterpolants)) * 100;
			}
			sb.append(percent);
			sb.append("%");
			return sb.toString();
		}

		private StringBuilder addTimedStatistic(final IStatisticsDataProvider benchmarkData, final StringBuilder sb, final String key) {
			sb.append(key);
			sb.append(": ");
			final Long time = (Long) benchmarkData.getValue(key);
			sb.append(TraceAbstractionBenchmarks.prettyprintNanoseconds(time));
			sb.append(" ");
			return sb;
		}
	}

	/**
	 * Stores benchmark data about the usage of TraceCheckers. E.g., number and size of predicates obtained via
	 * interpolation.
	 * 
	 * @author Matthias Heizmann
	 */
	public class TraceCheckerBenchmarkGenerator extends StatisticsGeneratorWithStopwatches
			implements IStatisticsDataProvider {

		int mNumberOfCodeBlocks = 0;
		int mNumberOfCodeBlocksAsserted = 0;
		int mNumberOfCheckSat = 0;
		int mConstructedInterpolants = 0;
		int mQuantifiedInterpolants = 0;

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
		public Object getValue(final String key) {
			switch (key) {
			case TraceCheckerBenchmarkType.s_SsaConstruction:
			case TraceCheckerBenchmarkType.s_SatisfiabilityAnalysis:
			case TraceCheckerBenchmarkType.s_InterpolantComputation:
				try {
					return getElapsedTime(key);
				} catch (final StopwatchStillRunningException e) {
					throw new AssertionError("clock still running: " + key);
				}
			case TraceCheckerBenchmarkType.s_NumberOfCodeBlocks:
				return mNumberOfCodeBlocks;
			case TraceCheckerBenchmarkType.s_NumberOfCodeBlocksAsserted:
				return mNumberOfCodeBlocksAsserted;
			case TraceCheckerBenchmarkType.s_NumberOfCheckSat:
				return mNumberOfCheckSat;
			case TraceCheckerBenchmarkType.s_ConstructedInterpolants:
				return mConstructedInterpolants;
			case TraceCheckerBenchmarkType.s_QuantifiedInterpolants:
				return mQuantifiedInterpolants;
			default:
				throw new AssertionError("unknown data");
			}
		}

		@Override
		public IStatisticsType getBenchmarkType() {
			return TraceCheckerBenchmarkType.getInstance();
		}

		/**
		 * Tell the Benchmark that the checked trace has n CodeBlocks
		 */
		public void reportnewCodeBlocks(final int n) {
			mNumberOfCodeBlocks = mNumberOfCodeBlocks + n;
		}

		/**
		 * Tell the Benchmark that n CodeBlocks have been asserted additionally
		 */
		public void reportnewAssertedCodeBlocks(final int n) {
			mNumberOfCodeBlocksAsserted = mNumberOfCodeBlocksAsserted + n;
		}

		/**
		 * Tell the Benchmark we did another check sat
		 */
		public void reportnewCheckSat() {
			mNumberOfCheckSat++;
		}

		public void reportNewInterpolant(final boolean isQuantified) {
			mConstructedInterpolants++;
			if (isQuantified) {
				mQuantifiedInterpolants++;
			}
		}

		public void reportSequenceOfInterpolants(final List<IPredicate> interpolants) {
			for (final IPredicate pred : interpolants) {
				final boolean isQuantified = new ContainsQuantifier().containsQuantifier(pred.getFormula());
				mTraceCheckerBenchmarkGenerator.reportNewInterpolant(isQuantified);
			}
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
	 * <li>UNKNOWN if it was not possible to determine if the trace fulfills its specification.
	 * </ul>
	 */
	public LBool isCorrect() {
		return mIsSafe;
	}

	/**
	 * Check if trace fulfills specification given by precondition, postcondition and pending contexts. The
	 * pendingContext maps the positions of pending returns to predicates which define possible variable valuations in
	 * the context to which the return leads the trace.
	 * 
	 * @param assertCodeBlocksIncrementally
	 *            If set to false, check-sat is called after all CodeBlocks are asserted. If set to true we use Betims
	 *            heuristic an incrementally assert CodeBlocks and do check-sat until all CodeBlocks are asserted or the
	 *            result to a check-sat is UNSAT.
	 * @param logger
	 * @param services
	 */
	public TraceChecker(final IPredicate precondition, final IPredicate postcondition,
			final SortedMap<Integer, IPredicate> pendingContexts, final NestedWord<? extends IAction> trace, final SmtManager smtManager,
			final ModifiableGlobalVariableManager modifiedGlobals, final AssertCodeBlockOrder assertCodeBlocksIncrementally,
			final IUltimateServiceProvider services, final boolean computeRcfgProgramExecution) {
		this(precondition, postcondition, pendingContexts, trace, smtManager, modifiedGlobals,
				new DefaultTransFormulas(trace, precondition, postcondition, pendingContexts, modifiedGlobals, false),
				assertCodeBlocksIncrementally, services, computeRcfgProgramExecution, true);
	}

	protected TraceChecker(final IPredicate precondition, final IPredicate postcondition,
			final SortedMap<Integer, IPredicate> pendingContexts, final NestedWord<? extends IAction> trace, final SmtManager smtManager,
			final ModifiableGlobalVariableManager modifiedGlobals, final NestedFormulas<TransFormula, IPredicate> rv,
			final AssertCodeBlockOrder assertCodeBlocksIncrementally, final IUltimateServiceProvider services,
			final boolean computeRcfgProgramExecution, final boolean unlockSmtSolverAlsoIfUnsat) {
		this(precondition, postcondition, pendingContexts, trace, smtManager, modifiedGlobals, rv,
				assertCodeBlocksIncrementally, services, computeRcfgProgramExecution, unlockSmtSolverAlsoIfUnsat,
				smtManager);
	}

	/**
	 * Commit additionally the DefaultTransFormulas
	 * 
	 * @param services
	 * 
	 */
	protected TraceChecker(final IPredicate precondition, final IPredicate postcondition,
			final SortedMap<Integer, IPredicate> pendingContexts, final NestedWord<? extends IAction> trace, final SmtManager smtManager,
			final ModifiableGlobalVariableManager modifiedGlobals, final NestedFormulas<TransFormula, IPredicate> rv,
			final AssertCodeBlockOrder assertCodeBlocksIncrementally, final IUltimateServiceProvider services,
			final boolean computeRcfgProgramExecution, final boolean unlockSmtSolverAlsoIfUnsat, final SmtManager tcSmtManager) {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(Activator.PLUGIN_ID);
		mSmtManager = smtManager;
		mManagedScript = smtManager.getManagedScript();
		mScript = smtManager.getScript();
		mTcSmtManager = tcSmtManager;
		mModifiedGlobals = modifiedGlobals;
		mTrace = trace;
		mPrecondition = precondition;
		mPostcondition = postcondition;
		if (pendingContexts == null) {
			throw new NullPointerException(
					"pendingContexts must not be " + "null, if there are no pending contexts, use an empty map");
		}
		mPendingContexts = pendingContexts;
		mNestedFormulas = rv;
		mTraceCheckerBenchmarkGenerator = getBenchmarkGenerator();
		massertCodeBlocksIncrementally = assertCodeBlocksIncrementally;
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
					mTraceCheckFinished = true;
					unlockSmtManager();
				}
			}
		} catch (final ToolchainCanceledException tce) {
			mToolchainCanceledException = tce;
		} finally {
			mIsSafe = isSafe;
		}
	}

	/**
	 * Like three-argument-checkTrace-Method above but for traces which contain pending returns. The pendingContext maps
	 * the positions of pending returns to predicates which define possible variable valuations in the context to which
	 * the return leads the trace.
	 * 
	 */
	protected LBool checkTrace() {
		LBool isSafe;
		mTcSmtManager.startTraceCheck(this);
		final boolean transferToDifferentScript = (mTcSmtManager != mSmtManager);
		mTraceCheckerBenchmarkGenerator.start(TraceCheckerBenchmarkType.s_SsaConstruction);
		mNsb = new NestedSsaBuilder(mTrace, mTcSmtManager, mNestedFormulas, mModifiedGlobals, mLogger,
				transferToDifferentScript);
		final NestedFormulas<Term, Term> ssa = mNsb.getSsa();
		mTraceCheckerBenchmarkGenerator.stop(TraceCheckerBenchmarkType.s_SsaConstruction);

		mTraceCheckerBenchmarkGenerator.start(TraceCheckerBenchmarkType.s_SatisfiabilityAnalysis);
		if (massertCodeBlocksIncrementally != AssertCodeBlockOrder.NOT_INCREMENTALLY) {
			mAAA = new AnnotateAndAsserterWithStmtOrderPrioritization(mTcSmtManager, ssa,
					getAnnotateAndAsserterCodeBlocks(ssa), mTraceCheckerBenchmarkGenerator,
					massertCodeBlocksIncrementally, mServices);
		} else {
			mAAA = new AnnotateAndAsserter(mTcSmtManager, ssa, getAnnotateAndAsserterCodeBlocks(ssa),
					mTraceCheckerBenchmarkGenerator, mServices);
			// Report the asserted code blocks
			// mTraceCheckerBenchmarkGenerator.reportnewAssertedCodeBlocks(mTrace.length());
		}
		try {
			mAAA.buildAnnotatedSsaAndAssertTerms();
			isSafe = mAAA.isInputSatisfiable();
		} catch (final SMTLIBException e) {
			if (e.getMessage().equals("Unsupported non-linear arithmetic")) {
				isSafe = LBool.UNKNOWN;
			} else {
				throw e;
			}
		} finally {
			mTraceCheckerBenchmarkGenerator.stop(TraceCheckerBenchmarkType.s_SatisfiabilityAnalysis);
		}
		return isSafe;
	}

	/**
	 * Compute a program execution for the checked trace.
	 * <ul>
	 * <li>If the checked trace violates its specification (result of trace check is SAT), we compute a program
	 * execution that contains program states that witness the violation of the specification (however, this can still
	 * be partial program states e.g., no values assigned to arrays) and that contains information which branch of a
	 * parallel composed CodeBlock violates the specification.
	 * <li>If we can not determine if the trace violates its specification (result of trace check is UNKNOWN) we compute
	 * a program execution trace that contains neither states nor information about which branch of a parallel composed
	 * CodeBlock violates the specification.
	 * <li>If we have proven that the trace satisfies its specification (result of trace check is UNSAT) we throw an
	 * Error.
	 * 
	 * @param isSafe
	 */
	private void computeRcfgProgramExecution(final LBool isSafe) {
		if (!(mNestedFormulas instanceof DefaultTransFormulas)) {
			throw new AssertionError(
					"program execution only computable if " + "mNestedFormulas instanceof DefaultTransFormulas");
		}
		if (isSafe == LBool.SAT) {
			if (!((DefaultTransFormulas) mNestedFormulas).hasBranchEncoders()) {
				unlockSmtManager();
				final DefaultTransFormulas withBE = new DefaultTransFormulas(mNestedFormulas.getTrace(),
						mNestedFormulas.getPrecondition(), mNestedFormulas.getPostcondition(), mPendingContexts,
						mModifiedGlobals, true);
				final TraceChecker tc = new TraceChecker(mNestedFormulas.getPrecondition(),
						mNestedFormulas.getPostcondition(), mPendingContexts, mNestedFormulas.getTrace(),
						mSmtManager, mModifiedGlobals, withBE, AssertCodeBlockOrder.NOT_INCREMENTALLY, mServices,
						true, true, mTcSmtManager);
				if (tc.getToolchainCancelledExpection() != null) {
					throw tc.getToolchainCancelledExpection();
				}
				assert tc.isCorrect() == LBool.SAT : "result of second trace check is different";
				mRcfgProgramExecution = tc.getRcfgProgramExecution();
			} else {
				mRcfgProgramExecution = computeRcfgProgramExecutionCaseSAT(mNsb);
			}
		} else if (isSafe == LBool.UNKNOWN) {
			mRcfgProgramExecution = computeRcfgProgramExecutionCaseUNKNOWN();
		} else if (isSafe == LBool.UNSAT) {
			throw new AssertionError("specification satisfied - " + "cannot compute counterexample");
		} else {
			throw new AssertionError("unexpected result of correctness check");
		}
		mTraceCheckFinished = true;
	}

	/**
	 * Compute program execution in the case that we do not know if the checked specification is violated (result of
	 * trace check is UNKNOWN).
	 */
	private RcfgProgramExecution computeRcfgProgramExecutionCaseUNKNOWN() {
		final Map<Integer, ProgramState<Expression>> emptyMap = Collections.emptyMap();
		@SuppressWarnings("unchecked")
		final
		Map<TermVariable, Boolean>[] branchEncoders = new Map[0];
		unlockSmtManager();
		mTraceCheckFinished = true;
		return new RcfgProgramExecution((List<? extends RCFGEdge>) mNestedFormulas.getTrace().lettersAsList(), emptyMap, branchEncoders);
	}

	/**
	 * Compute program execution in the case that the checked specification is violated (result of trace check is SAT).
	 */
	private RcfgProgramExecution computeRcfgProgramExecutionCaseSAT(final NestedSsaBuilder nsb) {
		final RelevantVariables relVars = new RelevantVariables(mNestedFormulas, mModifiedGlobals);
		final RcfgProgramExecutionBuilder rpeb = new RcfgProgramExecutionBuilder(mModifiedGlobals,
				(NestedWord<CodeBlock>) mTrace, relVars, mSmtManager.getBoogie2Smt().getBoogie2SmtSymbolTable());
		for (int i = 0; i < mTrace.length(); i++) {
			final CodeBlock cb = (CodeBlock) mTrace.getSymbolAt(i);
			final TransFormula tf = cb.getTransitionFormulaWithBranchEncoders();
			if (tf.getBranchEncoders().size() > 0) {
				final Map<TermVariable, Boolean> beMapping = new HashMap<TermVariable, Boolean>();
				for (final TermVariable tv : tf.getBranchEncoders()) {
					final String nameOfConstant = NestedSsaBuilder.branchEncoderConstantName(tv, i);
					final Term indexedBe = mTcSmtManager.getScript().term(nameOfConstant);
					final Term value = getValue(indexedBe);
					final Boolean booleanValue = getBooleanValue(value);
					beMapping.put(tv, booleanValue);
				}
				rpeb.setBranchEncoders(i, beMapping);
			}
		}
		for (final IProgramVar bv : nsb.getIndexedVarRepresentative().keySet()) {
			if (TraceCheckerUtils.isSortForWhichWeCanGetValues(bv.getTermVariable().getSort())) {
				for (final Integer index : nsb.getIndexedVarRepresentative().get(bv).keySet()) {
					final Term indexedVar = nsb.getIndexedVarRepresentative().get(bv).get(index);
					Term valueT = getValue(indexedVar);
					if (mSmtManager != mTcSmtManager) {
						valueT = new TermTransferrer(mSmtManager.getScript()).transform(valueT);
					}
					final Expression valueE = mSmtManager.getBoogie2Smt().getTerm2Expression().translate(valueT);
					rpeb.addValueAtVarAssignmentPosition(bv, index, valueE);
				}
			}
		}
		unlockSmtManager();
		return rpeb.getRcfgProgramExecution();
	}

	protected AnnotateAndAssertCodeBlocks getAnnotateAndAsserterCodeBlocks(final NestedFormulas<Term, Term> ssa) {
		return new AnnotateAndAssertCodeBlocks(mTcSmtManager, ssa, mLogger);

		// AnnotateAndAssertCodeBlocks aaacb =
		// return new AnnotateAndAsserter(mSmtManager, ssa, aaacb);
	}

	private Term getValue(final Term term) {
		final Term[] arr = { term };
		final Map<Term, Term> map = mTcSmtManager.getScript().getValue(arr);
		final Term value = map.get(term);
		/*
		 * Some solvers, e.g., Z3 return -1 not as a literal but as a unary minus of a positive literal. We use our
		 * affine term to obtain the negative literal.
		 */
		final AffineTerm affineTerm = (AffineTerm) (new AffineTermTransformer(mTcSmtManager.getScript()))
				.transform(value);
		if (affineTerm.isErrorTerm()) {
			return value;
		} else {
			return affineTerm.toTerm(mTcSmtManager.getScript());
		}

	}

	private Boolean getBooleanValue(final Term term) {
		Boolean result;
		if (SmtUtils.isTrue(term)) {
			result = true;
		} else {
			if (SmtUtils.isFalse(term)) {
				result = false;
			} else {
				throw new AssertionError();
			}
		}
		return result;
	}

	public NestedWord<? extends IAction> getTrace() {
		return mTrace;
	}

	public IPredicate getPrecondition() {
		return mPrecondition;
	}

	public IPredicate getPostcondition() {
		return mPostcondition;
	}

	public Map<Integer, IPredicate> getPendingContexts() {
		return mPendingContexts;
	}

	/**
	 * Return the RcfgProgramExecution that has been computed by computeRcfgProgramExecution().
	 */
	public RcfgProgramExecution getRcfgProgramExecution() {
		if (mRcfgProgramExecution == null) {
			throw new AssertionError("program execution has not yet been computed");
		}
		return mRcfgProgramExecution;
	}

	protected void unlockSmtManager() {
		mTcSmtManager.endTraceCheck(this);
	}

	public TraceCheckerBenchmarkGenerator getTraceCheckerBenchmark() {
		if (mTraceCheckFinished || mToolchainCanceledException != null) {
			return mTraceCheckerBenchmarkGenerator;
		} else {
			throw new AssertionError("Benchmark is only available after the trace check is finished.");
		}
	}

	/**
	 * Returns the {@link ToolchainCanceledException} that was thrown if the computation was cancelled. If the
	 * computation was not cancelled, we return null.
	 */
	public ToolchainCanceledException getToolchainCancelledExpection() {
		return mToolchainCanceledException;
	}

}
