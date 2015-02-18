package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import de.uni_freiburg.informatik.ultimate.automata.Word;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.model.boogie.BoogieVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Call;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.InterproceduralSequentialComposition;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ParallelComposition;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Return;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.SequentialComposition;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.StatementSequence;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Summary;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.benchmark.IBenchmarkDataProvider;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.benchmark.IBenchmarkType;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.BasicPredicateExplicitQuantifier;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.EdgeChecker;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.PredicateTransformer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TraceAbstractionPreferenceInitializer.AssertCodeBlockOrder;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TraceAbstractionPreferenceInitializer.INTERPOLATION;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TraceAbstractionPreferenceInitializer.UnsatCores;
import de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException;

public class TraceCheckerSpWp extends InterpolatingTraceChecker {
	/*
	 * Settings for SVComp: m_useUnsatCore = true;
	 * m_useUnsatCoreOfFineGranularity = true; m_useLiveVariables = true;
	 * m_LogInformation = false; m_CollectInformationAboutQuantifiedPredicates =
	 * false; m_ComputeInterpolantsSp = true; m_ComputeInterpolantsFp = true;
	 * m_ComputeInterpolantsBp = false; m_ComputeInterpolantsWp = false;
	 */

	protected IPredicate[] m_InterpolantsSp;
	protected IPredicate[] m_InterpolantsWp;

	// Forward relevant predicates
	protected IPredicate[] m_InterpolantsFp;
	// Backward relevant predicates
	protected IPredicate[] m_InterpolantsBp;

	private final UnsatCores m_UnsatCores;
	private final boolean m_LiveVariables;
	private final static boolean m_useLiveVariablesInsteadOfRelevantVariables = false;
	private final static boolean m_LogInformation = true;
	private final static boolean m_CollectInformationAboutQuantifiedPredicates = true;
	private final static boolean m_CollectInformationAboutSizeOfPredicates = true;

	private boolean m_ComputeInterpolantsSp;
	private boolean m_ComputeInterpolantsFp;
	private boolean m_ComputeInterpolantsBp;
	private boolean m_ComputeInterpolantsWp;

	private static final boolean s_TransformToCNF = true;

	private AnnotateAndAssertConjunctsOfCodeBlocks m_AnnotateAndAsserterConjuncts;
	private final PredicateTransformer m_PredicateTransformer;

	public TraceCheckerSpWp(IPredicate precondition, IPredicate postcondition,
			SortedMap<Integer, IPredicate> pendingContexts, NestedWord<CodeBlock> trace, SmtManager smtManager,
			ModifiableGlobalVariableManager modifiedGlobals, AssertCodeBlockOrder assertCodeBlocksIncrementally,
			UnsatCores unsatCores, boolean useLiveVariables, 
			IUltimateServiceProvider services, boolean computeRcfgProgramExecution, 
			PredicateUnifier predicateUnifier, INTERPOLATION interpolation) {
		super(precondition, postcondition, pendingContexts, trace, smtManager, modifiedGlobals,
				assertCodeBlocksIncrementally, services, computeRcfgProgramExecution, predicateUnifier);
		m_UnsatCores = unsatCores;
		m_LiveVariables = useLiveVariables;
		m_PredicateTransformer = new PredicateTransformer(m_SmtManager, m_ModifiedGlobals, mServices);
		if (isCorrect() == LBool.UNSAT) {
			computeInterpolants(new AllIntegers(), interpolation);
		}
	}

	@Override
	protected TraceCheckerBenchmarkGenerator getBenchmarkGenerator() {
		return new TraceCheckerBenchmarkSpWpGenerator();
	}

	@Override
	public void computeInterpolants(Set<Integer> interpolatedPositions,
			INTERPOLATION interpolation) {
		m_TraceCheckerBenchmarkGenerator.start(TraceCheckerBenchmarkType.s_InterpolantComputation);
		switch (interpolation) {
		case ForwardPredicates:
			m_ComputeInterpolantsSp = true;
			m_ComputeInterpolantsFp = true;
			m_ComputeInterpolantsWp = false;
			m_ComputeInterpolantsBp = false;
			break;
		case BackwardPredicates:
			m_ComputeInterpolantsSp = false;
			m_ComputeInterpolantsFp = false;
			m_ComputeInterpolantsWp = true;
			m_ComputeInterpolantsBp = true;
			break;
		case FPandBP:
			m_ComputeInterpolantsSp = true;
			m_ComputeInterpolantsFp = true;
			m_ComputeInterpolantsWp = true;
			m_ComputeInterpolantsBp = true;
			break;
		default:
			throw new UnsupportedOperationException("unsupportedInterpolation");
		}
		try {
			computeInterpolantsUsingUnsatCore(interpolatedPositions);
		} catch (ToolchainCanceledException e) {
			mLogger.info("Timeout while computing interpolants");
			throw e;
		}
		m_TraceCheckerBenchmarkGenerator.stop(TraceCheckerBenchmarkType.s_InterpolantComputation);
		m_TraceCheckFinished = true;
	}

	public boolean forwardsPredicatesComputed() {
		return m_ComputeInterpolantsFp;
	}

	public boolean backwardsPredicatesComputed() {
		return m_ComputeInterpolantsBp;
	}

	public IPredicate getForwardPredicateAtPosition(int i) {
		assert i >= -1 && i <= m_InterpolantsFp.length : ("The given position " + i
				+ " is not a correct position. #Interpolants = " + m_InterpolantsFp.length);
		if (i == -1) {
			return super.getPrecondition();
		} else if (i == m_InterpolantsFp.length) {
			return super.getPostcondition();
		} else {
			return m_InterpolantsFp[i];
		}
	}

	public IPredicate getBackwardPredicateAtPosition(int i) {
		assert i >= -1 && i <= m_InterpolantsBp.length : ("The given position " + i
				+ " is not a correct position! #Interpolants = " + m_InterpolantsBp.length);
		if (i == -1) {
			return super.getPrecondition();
		} else if (i == m_InterpolantsBp.length) {
			return super.getPostcondition();
		} else {
			return m_InterpolantsBp[i];
		}
	}

	private IPredicate getBackwardPredicateAtPosition(int i, IPredicate tracePostcondition, boolean backwardPredicate) {
		if (i >= m_InterpolantsWp.length) {
			return tracePostcondition;
		} else {
			assert i >= 0;
			if (!backwardPredicate)
				return m_InterpolantsWp[i];
			else
				return m_InterpolantsBp[i];
		}
	}

	private IPredicate getForwardPredicateAtPosition(int i, IPredicate tracePrecondition, boolean forwardPredicate) {
		if (i < 0) {
			return tracePrecondition;
		} else {
			assert i < m_InterpolantsSp.length;
			if (!forwardPredicate)
				return m_InterpolantsSp[i];
			else
				return m_InterpolantsFp[i];
		}
	}

	/**
	 * Computes a summary of the procedure. The procedure consists (or is
	 * represented) by the Call statement, the Return statement and the inner
	 * statements.
	 * 
	 * @param trace
	 *            - the inner statements of the procedure
	 * @param Call
	 * @param Return
	 * @param oldVarsAssignment
	 * @param rv
	 * @param call_pos
	 * @return
	 */
	private ProcedureSummary computeProcedureSummary(NestedWord<CodeBlock> trace, TransFormula Call,
			TransFormula Return, TransFormula oldVarsAssignment, NestedFormulas<TransFormula, IPredicate> rv,
			int call_pos, int return_pos) {
		TransFormula summaryOfInnerStatements = computeSummaryForInterproceduralTrace(trace, rv, call_pos + 1,
				return_pos);
		TransFormula summaryWithCallAndReturn = TransFormula.sequentialCompositionWithCallAndReturn(
				m_SmtManager.getBoogie2Smt(), true, false, s_TransformToCNF, Call, oldVarsAssignment,
				summaryOfInnerStatements, Return, mLogger, mServices);
		return new ProcedureSummary(summaryWithCallAndReturn, summaryOfInnerStatements);
	}

	/**
	 * Computes a summary for the given trace, but only for the statements from
	 * position "start" to position "end".
	 * 
	 * @return - a summary for the statements from the given trace from position
	 *         "start" to position "end"
	 */
	private TransFormula computeSummaryForInterproceduralTrace(NestedWord<CodeBlock> trace,
			NestedFormulas<TransFormula, IPredicate> rv, int start, int end) {
		LinkedList<TransFormula> transformulasToComputeSummaryFor = new LinkedList<TransFormula>();
		for (int i = start; i < end; i++) {
			if (trace.getSymbol(i) instanceof Call) {
				if (!trace.isPendingCall(i)) {
					// Case: non-pending call
					// Compute a summary for Call and corresponding Return, but
					// only if the position of the corresponding
					// Return is smaller than the position "end"
					int returnPosition = trace.getReturnPosition(i);
					if (returnPosition < end) {
						// 1. Compute a summary for the statements between this
						// non-pending Call
						// and the corresponding Return recursively
						TransFormula summaryBetweenCallAndReturn = computeSummaryForInterproceduralTrace(trace, rv,
								i + 1, returnPosition);
						/**
						 * FIXME: Here, we have to assign the original
						 * statements for Call and Return as parameters, because
						 * the method
						 * {@link TransFormula.sequentialCompositionWithCallAndReturn}
						 * expects CodeBlocks and not TransFormulas.
						 */
						transformulasToComputeSummaryFor.addLast(TransFormula.sequentialCompositionWithCallAndReturn(
								m_SmtManager.getBoogie2Smt(), true, false, s_TransformToCNF, trace.getSymbol(i)
										.getTransitionFormula(), rv.getOldVarAssignment(i),
								summaryBetweenCallAndReturn, trace.getSymbol(returnPosition).getTransitionFormula(),
								mLogger, mServices));
						i = returnPosition;
					} else {
						// If the position of the corresponding Return is >=
						// "end",
						// then we handle this case as a pending-call
						TransFormula summaryAfterPendingCall = computeSummaryForInterproceduralTrace(trace, rv, i + 1,
								end);
						return TransFormula.sequentialCompositionWithPendingCall(m_SmtManager.getBoogie2Smt(), true,
								false, s_TransformToCNF, transformulasToComputeSummaryFor.toArray(new TransFormula[0]),
								rv.getLocalVarAssignment(i), rv.getOldVarAssignment(i), summaryAfterPendingCall,
								mLogger, mServices);
					}
				} else {
					TransFormula summaryAfterPendingCall = computeSummaryForInterproceduralTrace(trace, rv, i + 1, end);
					return TransFormula.sequentialCompositionWithPendingCall(m_SmtManager.getBoogie2Smt(), true, false,
							s_TransformToCNF, transformulasToComputeSummaryFor.toArray(new TransFormula[0]),
							rv.getLocalVarAssignment(i), rv.getOldVarAssignment(i), summaryAfterPendingCall, mLogger,
							mServices);
				}
			} else if (trace.getSymbol(i) instanceof Return) {
				// Nothing to do
			} else {
				transformulasToComputeSummaryFor.addLast(rv.getFormulaFromNonCallPos(i));
			}
		}
		return TransFormula.sequentialComposition(mLogger, mServices, m_SmtManager.getBoogie2Smt(), true, false,
				s_TransformToCNF, transformulasToComputeSummaryFor.toArray(new TransFormula[0]));

	}

	/***
	 * Computes predicates (interpolants) for the statements of an infeasible
	 * trace specified by the given set. Depending on settings, there are only
	 * forward predicates, or only backward predicates or both of them computed
	 * {@see computeForwardRelevantPredicates,
	 * computeBackwardRelevantPredicates} <br>
	 * The predicates are computed using an unsatisfiable core of the
	 * corresponding infeasibility proof of the trace in the following way: <br>
	 * 1. Replace statements, which don't occur in the unsatisfiable core by the
	 * statement <code> assume(true) </code> <br>
	 * 2. Compute live variables. <br>
	 * 3. Compute predicates for the trace, where the non-relevant statements
	 * has been substituted by <code> assume(true) </code>.
	 * 
	 * @see LiveVariables
	 * @see PredicateTransformer
	 */
	private void computeInterpolantsUsingUnsatCore(Set<Integer> interpolatedPositions) {
		int[] numberOfQuantifiedPredicates = new int[4];
		int[] sizeOfPredicatesFP = null;
		int[] sizeOfPredicatesBP = null;

		Term[] unsat_core = m_SmtManager.getScript().getUnsatCore();

		Set<Term> unsat_coresAsSet = new HashSet<Term>(Arrays.asList(unsat_core));

		if (!(interpolatedPositions instanceof AllIntegers)) {
			throw new UnsupportedOperationException();
		}
		IPredicate tracePrecondition = m_Precondition;
		IPredicate tracePostcondition = m_Postcondition;
		NestedWord<CodeBlock> trace = m_Trace;
		unlockSmtManager();
		NestedFormulas<TransFormula, IPredicate> rtf = null;

		if (m_LogInformation) {
			int totalNumberOfConjunctsInTrace = m_AnnotateAndAsserterConjuncts.getAnnotated2Original().keySet().size();
			mLogger.debug("Total number of conjuncts in trace: " + totalNumberOfConjunctsInTrace);
			mLogger.debug("Number of conjuncts in unsatisfiable core: " + unsat_coresAsSet.size());
			((TraceCheckerBenchmarkSpWpGenerator) m_TraceCheckerBenchmarkGenerator).setConjunctsInSSA(
					totalNumberOfConjunctsInTrace, unsat_coresAsSet.size());
		}

		if (m_UnsatCores == UnsatCores.IGNORE) {
			rtf = new DefaultTransFormulas(m_Trace, m_Precondition, m_Postcondition, m_PendingContexts,
					m_ModifiedGlobals, false);
		} else if (m_UnsatCores == UnsatCores.STATEMENT_LEVEL) {
			boolean[] localVarAssignmentAtCallInUnsatCore = new boolean[trace.length()];
			boolean[] oldVarAssignmentAtCallInUnsatCore = new boolean[trace.length()];
			// Filter out the statements, which doesn't occur in the unsat core.
			Set<CodeBlock> codeBlocksInUnsatCore = filterOutIrrelevantStatements(trace, unsat_coresAsSet,
					localVarAssignmentAtCallInUnsatCore, oldVarAssignmentAtCallInUnsatCore);
			rtf = new RelevantTransFormulas(trace, m_Precondition, m_Postcondition, m_PendingContexts,
					codeBlocksInUnsatCore, m_ModifiedGlobals, localVarAssignmentAtCallInUnsatCore,
					oldVarAssignmentAtCallInUnsatCore, m_SmtManager);
		} else if (m_UnsatCores == UnsatCores.CONJUNCT_LEVEL) {
			rtf = new RelevantTransFormulas(trace, m_Precondition, m_Postcondition, m_PendingContexts,
					unsat_coresAsSet, m_ModifiedGlobals, m_SmtManager, m_AAA, m_AnnotateAndAsserterConjuncts);
		} else {
			throw new AssertionError("unknown case:" + m_UnsatCores);
		}
		assert stillInfeasible(rtf) : "incorrect Unsatisfiable Core";

		Set<BoogieVar>[] relevantVarsToUseForFPBP = null;

		if (m_useLiveVariablesInsteadOfRelevantVariables) {
			LiveVariables lvar = new LiveVariables(m_Nsb.getVariable2Constant(), m_Nsb.getConstants2BoogieVar(),
					m_Nsb.getIndexedVarRepresentative(), m_SmtManager, m_ModifiedGlobals);
			relevantVarsToUseForFPBP = lvar.getLiveVariables();
			RelevantVariables rvar = new RelevantVariables(rtf, m_ModifiedGlobals);
			rvar.toString();
		} else {
			RelevantVariables rvar = new RelevantVariables(rtf, m_ModifiedGlobals);
			relevantVarsToUseForFPBP = rvar.getRelevantVariables();
		}
		


		if (m_ComputeInterpolantsFp) {
			mLogger.debug("Computing forward relevant predicates...");
			computeForwardRelevantPredicates(relevantVarsToUseForFPBP, rtf, trace, tracePrecondition, m_LiveVariables,
					numberOfQuantifiedPredicates);
			mLogger.debug("Checking inductivity of forward relevant predicates...");
//			if (!TraceCheckerUtils.checkInterpolantsInductivityForward(m_InterpolantsFp, 
//					trace, tracePrecondition, tracePostcondition, m_PendingContexts, "FP", m_SmtManager, m_ModifiedGlobals, mLogger)) {
//				throw new AssertionError("invalid Hoare triple in FP");
//			}
			assert TraceCheckerUtils.checkInterpolantsInductivityForward(m_InterpolantsFp, 
					trace, tracePrecondition, tracePostcondition, m_PendingContexts, "FP", 
					m_SmtManager, m_ModifiedGlobals, mLogger) : "invalid Hoare triple in FP";
			if (m_CollectInformationAboutSizeOfPredicates) {
				sizeOfPredicatesFP = m_SmtManager.computeDagSizeOfPredicates(m_InterpolantsFp);
			}
		} else if (m_ComputeInterpolantsSp && !m_ComputeInterpolantsFp) {
			mLogger.debug("Computing forward relevant predicates...");
			computeForwardRelevantPredicates(relevantVarsToUseForFPBP, rtf, trace, tracePrecondition, false,
					numberOfQuantifiedPredicates);
			mLogger.debug("Checking inductivity of forward relevant predicates...");
			assert TraceCheckerUtils.checkInterpolantsInductivityForward(m_InterpolantsFp, 
					trace, tracePrecondition, tracePostcondition, m_PendingContexts, "FP", 
					m_SmtManager, m_ModifiedGlobals, mLogger) : "invalid Hoare triple in FP";
		}

		if (m_LogInformation) {
			mLogger.debug("Length of trace:" + trace.length());
			mLogger.debug("#quantifiedPredicates in SP: " + numberOfQuantifiedPredicates[0]);
			mLogger.debug("#quantifiedPredicates in FP: " + numberOfQuantifiedPredicates[1]);
		}

		if (m_ComputeInterpolantsBp) {
			mLogger.debug("Computing backward relevant predicates...");
			computeBackwardRelevantPredicates(relevantVarsToUseForFPBP, rtf, trace, tracePostcondition,
					m_LiveVariables, numberOfQuantifiedPredicates);
			mLogger.debug("Checking inductivity of backward relevant predicates...");
			assert TraceCheckerUtils.checkInterpolantsInductivityBackward(m_InterpolantsBp, 
					trace, tracePrecondition, tracePostcondition, m_PendingContexts, "BP", 
					m_SmtManager, m_ModifiedGlobals, mLogger) : "invalid Hoare triple in BP";
			if (m_CollectInformationAboutSizeOfPredicates) {
				sizeOfPredicatesBP = m_SmtManager.computeDagSizeOfPredicates(m_InterpolantsBp);
			}
		} else if (m_ComputeInterpolantsWp && !m_ComputeInterpolantsBp) {
			mLogger.debug("Computing backward relevant predicates...");
			computeBackwardRelevantPredicates(relevantVarsToUseForFPBP, rtf, trace, tracePostcondition, false,
					numberOfQuantifiedPredicates);
			mLogger.debug("Checking inductivity of backward relevant predicates...");
			assert TraceCheckerUtils.checkInterpolantsInductivityBackward(m_InterpolantsBp, 
					trace, tracePrecondition, tracePostcondition, m_PendingContexts, "BP", 
					m_SmtManager, m_ModifiedGlobals, mLogger) : "invalid Hoare triple in BP";
		}

		((TraceCheckerBenchmarkSpWpGenerator) super.m_TraceCheckerBenchmarkGenerator).setPredicateData(
				numberOfQuantifiedPredicates, sizeOfPredicatesFP, sizeOfPredicatesBP);

		if (m_LogInformation) {
			mLogger.debug("Length of trace:" + trace.length());
			mLogger.debug("#quantifiedPredicates in WP: " + numberOfQuantifiedPredicates[2]);
			mLogger.debug("#quantifiedPredicates in BP: " + numberOfQuantifiedPredicates[3]);
		}

		// Check the validity of the computed interpolants.
		if (m_ComputeInterpolantsSp && m_ComputeInterpolantsWp) {
			checkSPImpliesWP(m_InterpolantsSp, m_InterpolantsWp);
		}
		if (m_ComputeInterpolantsFp && m_ComputeInterpolantsBp) {
			selectListOFPredicatesFromBothTypes();
		} else if (m_ComputeInterpolantsFp) {
			m_Interpolants = m_InterpolantsFp;
		} else if (m_ComputeInterpolantsSp) {
			m_Interpolants = m_InterpolantsSp;
		} else if (m_ComputeInterpolantsBp) {
			m_Interpolants = m_InterpolantsBp;
		} else {
			assert m_InterpolantsWp != null;
			m_Interpolants = m_InterpolantsWp;
		}
	}

	/***
	 * Selects a list of predicates from the predicates computed via strongest
	 * post-condition and the weakest precondition, such that the number of
	 * predicates containing quantifiers is minimized and a mix-up of the two
	 * types is avoided. (If the predicates are mixed-up, they may get
	 * non-inductive.) Predicates are selected in the following way: (TODO:)
	 * 
	 */
	private void selectListOFPredicatesFromBothTypes() {
		assert m_InterpolantsFp.length == m_InterpolantsBp.length;
		m_Interpolants = new IPredicate[m_InterpolantsBp.length];
		int i = 0; // position of predicate computed by strongest post-condition
		int j = m_InterpolantsBp.length; // position of predicate computed by
											// weakest precondition
		while (i != j) {
			if (!(m_InterpolantsBp[j - 1] instanceof BasicPredicateExplicitQuantifier)) {
				m_Interpolants[j - 1] = m_InterpolantsBp[j - 1];
				j--;
			} else if (!(m_InterpolantsFp[i] instanceof BasicPredicateExplicitQuantifier)) {
				m_Interpolants[i] = m_InterpolantsFp[i];
				i++;
			} else {
				int numOfQuantifiedVarsInFp = ((BasicPredicateExplicitQuantifier) m_InterpolantsFp[i])
						.getQuantifiedVariables().size();
				int numOfQuantifiedVarsInBp = ((BasicPredicateExplicitQuantifier) m_InterpolantsBp[j - 1])
						.getQuantifiedVariables().size();
				if (numOfQuantifiedVarsInFp < numOfQuantifiedVarsInBp) {
					m_Interpolants[i] = m_InterpolantsFp[i];
					i++;
				} else {
					m_Interpolants[j - 1] = m_InterpolantsBp[j - 1];
					j--;
				}
			}
		}
	}

	/**
	 * Checks whether the trace consisting of only relevant statements is still
	 * infeasible. This check is desired, when we use unsatisfiable cores of
	 * finer granularity.
	 */
	private boolean stillInfeasible(NestedFormulas<TransFormula, IPredicate> rv) {
		TraceChecker tc = new TraceChecker(rv.getPrecondition(), rv.getPostcondition(),
				new TreeMap<Integer, IPredicate>(), rv.getTrace(), m_SmtManager, m_ModifiedGlobals, rv,
				AssertCodeBlockOrder.NOT_INCREMENTALLY, mServices, false, true);
		boolean result = (tc.isCorrect() == LBool.UNSAT);
		return result;
	}

	/**
	 * Select the statements from the given trace, which are contained in the
	 * unsatisfiable core. These statements contribute to the unsatisfiability
	 * of the trace, and therefore only these are important for the computations
	 * done afterwards.
	 */
	private Set<CodeBlock> filterOutIrrelevantStatements(NestedWord<CodeBlock> trace, Set<Term> unsat_coresAsSet,
			boolean[] localVarAssignmentAtCallInUnsatCore, boolean[] oldVarAssignmentAtCallInUnsatCore) {
		Set<CodeBlock> codeBlocksInUnsatCore = new HashSet<CodeBlock>();
		for (int i = 0; i < trace.length(); i++) {
			if (!trace.isCallPosition(i)
					&& unsat_coresAsSet.contains(m_AAA.getAnnotatedSsa().getFormulaFromNonCallPos(i))) {
				codeBlocksInUnsatCore.add(trace.getSymbol(i));
			} else if (trace.isCallPosition(i)
					&& (unsat_coresAsSet.contains(m_AAA.getAnnotatedSsa().getGlobalVarAssignment(i)) || unsat_coresAsSet
							.contains(m_AAA.getAnnotatedSsa().getOldVarAssignment(i)))) {
				// The upper condition checks, whether the globalVarAssignments
				// is in unsat core, now check whether the local variable
				// assignments
				// is in unsat core, if it is Call statement
				if (unsat_coresAsSet.contains(m_AAA.getAnnotatedSsa().getLocalVarAssignment(i))) {
					localVarAssignmentAtCallInUnsatCore[i] = true;
				}
				if (unsat_coresAsSet.contains(m_AAA.getAnnotatedSsa().getOldVarAssignment(i))) {
					oldVarAssignmentAtCallInUnsatCore[i] = true;
				}
				// Add the globalVarAssignments to the unsat_core, if it is a
				// Call statement, otherwise it adds
				// the statement
				codeBlocksInUnsatCore.add(trace.getSymbol(i));
			} else {
				if (trace.getSymbol(i) instanceof Call) {
					if (unsat_coresAsSet.contains(m_AAA.getAnnotatedSsa().getLocalVarAssignment(i))) {
						localVarAssignmentAtCallInUnsatCore[i] = true;
					}
				}
			}
		}
		return codeBlocksInUnsatCore;
	}

	/**
	 * Computes two types of predicates (interpolants) for the given trace.
	 * First type of predicates is devised by computing the strongest
	 * post-condition of a statement and the previous predicate. The second type
	 * of predicates (so-called forward predicates) is acquired by computing the
	 * strongest post-condition and additionally existentially quantifying
	 * irrelevant variables.
	 * 
	 * @param numberOfQuantifiedPredicates
	 * 
	 */
	private void computeForwardRelevantPredicates(Set<BoogieVar>[] relevantVars,
			NestedFormulas<TransFormula, IPredicate> rv, NestedWord<CodeBlock> trace, IPredicate tracePrecondition,
			boolean quantifyIrrelevantVariables, int[] numberOfQuantifiedPredicates) {
		m_InterpolantsSp = new IPredicate[trace.length() - 1];
		m_InterpolantsFp = new IPredicate[m_InterpolantsSp.length];

		if (trace.length() > 1) {
			for (int i = 0; i < m_InterpolantsSp.length; i++) {
				IPredicate sp = null;
				if (trace.getSymbol(i) instanceof Call) {
					sp = m_PredicateTransformer.strongestPostcondition(
							getForwardPredicateAtPosition(i - 1, tracePrecondition, true), rv.getLocalVarAssignment(i),
							rv.getGlobalVarAssignment(i), rv.getOldVarAssignment(i),
							((NestedWord<CodeBlock>) trace).isPendingCall(i));
					m_InterpolantsSp[i] = m_PredicateUnifier.getOrConstructPredicate(sp.getFormula(), sp.getVars(),
							sp.getProcedures());
				} else if (trace.getSymbol(i) instanceof Return) {
					IPredicate callerPred = tracePrecondition;
					TransFormula globalVarsAssignment = null;
					TransFormula oldVarsAssignment = null;
					TransFormula callTF = null;
					if (trace.isPendingReturn(i)) {
						callerPred = m_PendingContexts.get(new Integer(i));
						callTF = rv.getLocalVarAssignment(i);
						oldVarsAssignment = rv.getOldVarAssignment(i);
					} else {
						int call_pos = ((NestedWord<CodeBlock>) trace).getCallPosition(i);
						assert call_pos >= 0 && call_pos <= i : "Bad call position!";
						if (call_pos > 0) {
							callerPred = m_InterpolantsSp[call_pos - 1];
						}
						globalVarsAssignment = rv.getGlobalVarAssignment(call_pos);
						callTF = rv.getLocalVarAssignment(call_pos);
					}
					IPredicate p = m_PredicateTransformer.strongestPostcondition(
							getForwardPredicateAtPosition(i - 1, tracePrecondition, true), callerPred,
							rv.getFormulaFromNonCallPos(i), callTF, globalVarsAssignment, oldVarsAssignment);
					m_InterpolantsSp[i] = m_PredicateUnifier.getOrConstructPredicate(p.getFormula(), p.getVars(),
							p.getProcedures());

				} else {
					sp = m_PredicateTransformer.strongestPostcondition(
							getForwardPredicateAtPosition(i - 1, tracePrecondition, true),
							rv.getFormulaFromNonCallPos(i));
					m_InterpolantsSp[i] = m_PredicateUnifier.getOrConstructPredicate(sp.getFormula(), sp.getVars(),
							sp.getProcedures());
				}
				if (quantifyIrrelevantVariables) {
					IPredicate fp = m_PredicateTransformer.computeForwardRelevantPredicate(
							getForwardPredicateAtPosition(i, tracePrecondition, false), relevantVars[i + 1]);
					m_InterpolantsFp[i] = m_PredicateUnifier.getOrConstructPredicate(fp.getFormula(), fp.getVars(),
							fp.getProcedures());

				} else {
					m_InterpolantsFp[i] = m_InterpolantsSp[i];
				}
				if (m_CollectInformationAboutQuantifiedPredicates) {
					IPredicate p = getForwardPredicateAtPosition(i, tracePrecondition, false);
					if (p != null && p instanceof BasicPredicateExplicitQuantifier) {
						numberOfQuantifiedPredicates[0]++;
					}
					p = getForwardPredicateAtPosition(i, tracePrecondition, true);
					if (p != null && p instanceof BasicPredicateExplicitQuantifier) {
						numberOfQuantifiedPredicates[1]++;
					}
				}

			}
		}

	}

	/**
	 * Computes two types of predicates (interpolants) for the given trace.
	 * First type of predicates is devised by computing the weakest precondition
	 * of a statement and the previous predicate. The second type of predicates
	 * (so-called backward predicates) is acquired by computing the weakest
	 * precondition and additionally universally quantifying irrelevant
	 * variables.
	 * 
	 * @param numberOfQuantifiedPredicates
	 * 
	 */
	private void computeBackwardRelevantPredicates(Set<BoogieVar>[] relevantVars,
			NestedFormulas<TransFormula, IPredicate> rv, NestedWord<CodeBlock> trace, IPredicate tracePostcondition,
			boolean quantifyIrrelevantVariables, int[] numberOfQuantifiedPredicates) {
		m_InterpolantsWp = new IPredicate[trace.length() - 1];
		m_InterpolantsBp = new IPredicate[m_InterpolantsWp.length];

		if (trace.length() > 1) {
			// Contains the predicates, which are computed during a Return with
			// the second method, where the callerPred
			// is computed as wp(returnerPred, summaryOfCalledProcedure).
			Map<Integer, IPredicate> callerPredicatesComputed = new HashMap<Integer, IPredicate>();
			for (int i = m_InterpolantsWp.length - 1; i >= 0; i--) {
				IPredicate wp = null;
				if (trace.getSymbol(i + 1) instanceof Call) {
					if (callerPredicatesComputed.containsKey(i + 1)) {
						wp = m_SmtManager.renameVarsOfOtherProcsToFreshVars(callerPredicatesComputed.get(i + 1),
								((Call) trace.getSymbol(i + 1)).getPreceedingProcedure());
						m_InterpolantsWp[i] = m_PredicateUnifier.getOrConstructPredicate(wp.getFormula(), wp.getVars(),
								wp.getProcedures());
					} else {
						wp = m_PredicateTransformer.weakestPrecondition(
								getBackwardPredicateAtPosition(i + 1, tracePostcondition, true),
								rv.getLocalVarAssignment(i + 1), rv.getGlobalVarAssignment(i + 1),
								rv.getOldVarAssignment(i + 1), true);
						m_InterpolantsWp[i] = m_PredicateUnifier.getOrConstructPredicate(wp.getFormula(), wp.getVars(),
								wp.getProcedures());

					}
				} else if (trace.getSymbol(i + 1) instanceof Return) {
					TransFormula globalVarsAssignments = null;
					TransFormula oldVarAssignments = null;
					TransFormula callTF = null;
					IPredicate callerPred = null;
					Set<BoogieVar> varsOccurringBetweenCallAndReturn = null;
					if (trace.isPendingReturn(i)) {
						callerPred = m_PendingContexts.get(new Integer(i));
						// we may get the local variable assignment (pending
						// context)
						// by requesting it at the position of the
						// pending-return.
						callTF = rv.getLocalVarAssignment(i);
						oldVarAssignments = rv.getOldVarAssignment(i);
					} else {
						int call_pos = ((NestedWord<CodeBlock>) trace).getCallPosition(i + 1);
						assert call_pos >= 0 && call_pos <= i : "Bad call position!";
						callTF = rv.getLocalVarAssignment(call_pos);
						globalVarsAssignments = rv.getGlobalVarAssignment(call_pos);
						oldVarAssignments = rv.getOldVarAssignment(call_pos);
						// TODO: Documentation!
						ProcedureSummary summary = computeProcedureSummary(trace, callTF,
								rv.getFormulaFromNonCallPos(i + 1), oldVarAssignments, rv, call_pos, i + 1);
						varsOccurringBetweenCallAndReturn = summary.computeVariableInInnerSummary();
						callerPred = m_PredicateTransformer.weakestPrecondition(
								getBackwardPredicateAtPosition(i + 1, tracePostcondition, true),
								summary.getWithCallAndReturn());
						callerPredicatesComputed.put(
								call_pos,
								m_PredicateUnifier.getOrConstructPredicate(callerPred.getFormula(),
										callerPred.getVars(), callerPred.getProcedures()));
					}
					wp = m_PredicateTransformer.weakestPrecondition(
							getBackwardPredicateAtPosition(i + 1, tracePostcondition, true),
							callerPred,
							rv.getFormulaFromNonCallPos(i + 1),
							callTF,
							globalVarsAssignments,
							oldVarAssignments,
							m_ModifiedGlobals.getModifiedBoogieVars(((Return) trace.getSymbol(i + 1))
									.getCorrespondingCall().getCallStatement().getMethodName()),
							varsOccurringBetweenCallAndReturn);
					m_InterpolantsWp[i] = m_PredicateUnifier.getOrConstructPredicate(wp.getFormula(), wp.getVars(),
							wp.getProcedures());

				} else {
					wp = m_PredicateTransformer.weakestPrecondition(
							getBackwardPredicateAtPosition(i + 1, tracePostcondition, true),
							rv.getFormulaFromNonCallPos(i + 1));
					m_InterpolantsWp[i] = m_PredicateUnifier.getOrConstructPredicate(wp.getFormula(), wp.getVars(),
							wp.getProcedures());
				}
				if (quantifyIrrelevantVariables) {
					IPredicate bp = m_PredicateTransformer.computeBackwardRelevantPredicate(
							getBackwardPredicateAtPosition(i, tracePostcondition, false), relevantVars[i + 1]);
					m_InterpolantsBp[i] = m_PredicateUnifier.getOrConstructPredicate(bp.getFormula(), bp.getVars(),
							bp.getProcedures());
					if (m_CollectInformationAboutQuantifiedPredicates) {
						if (wp != null && wp instanceof BasicPredicateExplicitQuantifier) {
							numberOfQuantifiedPredicates[2]++;
						}
						if (bp instanceof BasicPredicateExplicitQuantifier) {
							numberOfQuantifiedPredicates[3]++;
						}
					}
				}
			}
		}

	}

	private class ProcedureSummary {
		private final TransFormula m_WithCallAndReturn;
		private final TransFormula m_WithoutCallAndReturn;

		public ProcedureSummary(TransFormula withCallAndReturn, TransFormula withoutCallAndReturn) {
			super();
			m_WithCallAndReturn = withCallAndReturn;
			m_WithoutCallAndReturn = withoutCallAndReturn;
		}

		public TransFormula getWithCallAndReturn() {
			return m_WithCallAndReturn;
		}

		public TransFormula getWithoutCallAndReturn() {
			return m_WithoutCallAndReturn;
		}

		/**
		 * Returns a set that contains all variables that occur in the summary
		 * without call and return.
		 */
		public Set<BoogieVar> computeVariableInInnerSummary() {
			return new Set<BoogieVar>() {

				@Override
				public boolean add(BoogieVar e) {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean addAll(Collection<? extends BoogieVar> c) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void clear() {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean contains(Object o) {
					return m_WithoutCallAndReturn.getInVars().containsKey(o)
							|| m_WithoutCallAndReturn.getOutVars().containsKey(o);
				}

				@Override
				public boolean containsAll(Collection<?> c) {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean isEmpty() {
					throw new UnsupportedOperationException();
				}

				@Override
				public Iterator<BoogieVar> iterator() {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean remove(Object o) {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean removeAll(Collection<?> c) {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean retainAll(Collection<?> c) {
					throw new UnsupportedOperationException();
				}

				@Override
				public int size() {
					throw new UnsupportedOperationException();
				}

				@Override
				public Object[] toArray() {
					throw new UnsupportedOperationException();
				}

				@Override
				public <T> T[] toArray(T[] a) {
					throw new UnsupportedOperationException();
				}
			};
		}

	}

	/***
	 * Check for each predicate computed via the strongest post-condition,
	 * whether it implies the predicate computed via weakest precondition. This
	 * check is desired, when predicates are computed twice (once via strongest
	 * post, and once via weakest pre-condition). It ensures the correctness of
	 * the predicates.
	 */
	private void checkSPImpliesWP(IPredicate[] interpolantsSP, IPredicate[] interpolantsWP) {
		mLogger.debug("Checking implication of SP and WP...");
		for (int i = 0; i < interpolantsSP.length; i++) {
			LBool result = m_SmtManager.isCovered(interpolantsSP[i], interpolantsWP[i]);
			mLogger.debug("SP {" + interpolantsSP[i] + "} ==> WP {" + interpolantsWP[i] + "} is "
					+ (result == LBool.UNSAT ? "valid" : (result == LBool.SAT ? "not valid" : result)));
			assert (result == LBool.UNSAT || result == LBool.UNKNOWN) : "checkSPImpliesWP failed";
		}
	}

	@Override
	protected AnnotateAndAssertCodeBlocks getAnnotateAndAsserterCodeBlocks(NestedFormulas<Term, Term> ssa) {
		if (m_AnnotateAndAsserterConjuncts == null) {
			m_AnnotateAndAsserterConjuncts = new AnnotateAndAssertConjunctsOfCodeBlocks(m_SmtManager, ssa,
					m_NestedFormulas, mLogger);
		}
		return m_AnnotateAndAsserterConjuncts;
	}

	@Override
	public int getTotalNumberOfPredicates(INTERPOLATION interpolation) {
		switch (interpolation) {
		case ForwardPredicates:
			return m_InterpolantsFp != null ? m_InterpolantsFp.length : 0;
		case BackwardPredicates:
			return m_InterpolantsBp != null ? m_InterpolantsBp.length : 0;
		default:
			return super.getTotalNumberOfPredicates(interpolation);
		}
	}

	public static class TraceCheckerSpWpBenchmarkType extends TraceCheckerBenchmarkType implements IBenchmarkType {

		private static TraceCheckerSpWpBenchmarkType s_Instance = new TraceCheckerSpWpBenchmarkType();

		protected final static String s_SizeOfPredicatesFP = "SizeOfPredicatesFP";
		protected final static String s_SizeOfPredicatesBP = "SizeOfPredicatesBP";
		protected final static String s_NumberOfQuantifiedPredicates = "NumberOfQuantifiedPredicates";
		protected final static String s_ConjunctsInSSA = "Conjuncts in SSA";
		protected final static String s_ConjunctsInUnsatCore = "Conjuncts in UnsatCore";

		public static TraceCheckerSpWpBenchmarkType getInstance() {
			return s_Instance;
		}

		@Override
		public Collection<String> getKeys() {
			ArrayList<String> result = new ArrayList<String>();
			for (String key : super.getKeys()) {
				result.add(key);
			}
			result.add(s_SizeOfPredicatesFP);
			result.add(s_SizeOfPredicatesBP);
			result.add(s_NumberOfQuantifiedPredicates);
			result.add(s_ConjunctsInSSA);
			result.add(s_ConjunctsInUnsatCore);
			return result;
		}

		@Override
		public Object aggregate(String key, Object value1, Object value2) {
			switch (key) {
			case s_NumberOfQuantifiedPredicates: {
				int[] array1 = (int[]) value1;
				int[] array2 = (int[]) value2;
				assert array1.length == 4;
				assert array2.length == 4;
				int[] result = new int[4];
				for (int i = 0; i < 4; i++) {
					result[i] = array1[i] + array1[i];
				}
				return result;
			}
			case s_SizeOfPredicatesFP:
			case s_SizeOfPredicatesBP:
				long size1 = (long) value1;
				long size2 = (long) value2;
				long result = size1 + size2;
				return result;
			case s_ConjunctsInSSA: {
				int numberConjuncts1 = (int) value1;
				int numberConjuncts2 = (int) value2;
				return numberConjuncts1 + numberConjuncts2;
			}
			case s_ConjunctsInUnsatCore: {
				int numberConjuncts1 = (int) value1;
				int numberConjuncts2 = (int) value2;
				return numberConjuncts1 + numberConjuncts2;
			}
			default:
				return super.aggregate(key, value1, value2);
			}
		}

		@Override
		public String prettyprintBenchmarkData(IBenchmarkDataProvider benchmarkData) {
			StringBuilder sb = new StringBuilder();
			sb.append(super.prettyprintBenchmarkData(benchmarkData));
			sb.append("\t");
			sb.append(s_ConjunctsInSSA).append(": ");
			int conjunctsSSA = (int) benchmarkData.getValue(s_ConjunctsInSSA);
			sb.append(conjunctsSSA);
			sb.append(" ");
			sb.append(s_ConjunctsInUnsatCore).append(": ");
			int conjunctsUC = (int) benchmarkData.getValue(s_ConjunctsInUnsatCore);
			sb.append(conjunctsUC);
			sb.append("\t");
			int[] numberOfQuantifiedPredicates = (int[]) benchmarkData.getValue(s_NumberOfQuantifiedPredicates);
			assert numberOfQuantifiedPredicates.length == 4;
			if (numberOfQuantifiedPredicates[1] > 0) {
				sb.append("Num of quantified predicates FP: " + numberOfQuantifiedPredicates[1]);
				sb.append(" ");
			}
			if (numberOfQuantifiedPredicates[3] > 0) {
				sb.append("Num of quantified predicates BP: " + numberOfQuantifiedPredicates[3]);
				sb.append(" ");
			}
			long sizeOfPredicatesFP = (long) benchmarkData.getValue(s_SizeOfPredicatesFP);
			sb.append("Size of predicates FP: " + sizeOfPredicatesFP + " ");
			long sizeOfPredicatesBP = (long) benchmarkData.getValue(s_SizeOfPredicatesBP);
			sb.append("Size of predicates BP: " + sizeOfPredicatesBP + " ");
			return sb.toString();
		}
	}

	/**
	 * Stores benchmark data about the usage of TraceCheckers. E.g., number and
	 * size of predicates obtained via interpolation.
	 */
	public class TraceCheckerBenchmarkSpWpGenerator extends TraceCheckerBenchmarkGenerator implements
			IBenchmarkDataProvider {
		// m_NumberOfQuantifierFreePredicates[0] : #quantified predicates of SP
		// m_NumberOfQuantifierFreePredicates[1] : #quantified predicates of FP
		// m_NumberOfQuantifierFreePredicates[2] : #quantified predicates of WP
		// m_NumberOfQuantifierFreePredicates[3] : #quantified predicates of BP
		private int[] m_NumberOfQuantifiedPredicates = new int[4];
		// m_NumberOfQuantifierFreePredicates[0] : Sum of the DAG-Size of
		// predicates computed via FP
		// m_NumberOfQuantifierFreePredicates[1] : Sum of the DAG-Size of
		// predicates computed via BP
		private long[] m_SizeOfPredicates = new long[2];

		private int m_ConjunctsInSSA;
		private int m_ConjunctsInUC;

		@Override
		public String[] getStopwatches() {
			return super.getStopwatches();
		}

		public void setPredicateData(int[] numberOfQuantifiedPredicates, int[] sizeOfPredicatesFP,
				int[] sizeOfPredicatesBP) {
			assert numberOfQuantifiedPredicates != null;
			m_NumberOfQuantifiedPredicates = numberOfQuantifiedPredicates;
			m_SizeOfPredicates = new long[2];
			if (sizeOfPredicatesFP != null) {
				m_SizeOfPredicates[0] = getSumOfIntArray(sizeOfPredicatesFP);
			} else {
				m_SizeOfPredicates[0] = 0;
			}
			if (sizeOfPredicatesBP != null) {
				m_SizeOfPredicates[1] = getSumOfIntArray(sizeOfPredicatesBP);
			} else {
				m_SizeOfPredicates[1] = 0;
			}
		}

		public void setConjunctsInSSA(int conjunctsInSSA, int conjunctsInUC) {
			assert m_ConjunctsInSSA == 0 : "have already been set";
			assert m_ConjunctsInUC == 0 : "have already been set";
			m_ConjunctsInSSA = conjunctsInSSA;
			m_ConjunctsInUC = conjunctsInUC;
		}

		private long getSumOfIntArray(int[] arr) {
			long sum = 0;
			for (int i = 0; i < arr.length; i++) {
				sum += arr[i];
			}
			return sum;
		}

		@Override
		public Collection<String> getKeys() {
			return TraceCheckerSpWpBenchmarkType.getInstance().getKeys();
		}

		@Override
		public Object getValue(String key) {
			switch (key) {
			case TraceCheckerSpWpBenchmarkType.s_NumberOfQuantifiedPredicates:
				return m_NumberOfQuantifiedPredicates;
			case TraceCheckerSpWpBenchmarkType.s_SizeOfPredicatesFP:
				return m_SizeOfPredicates[0];
			case TraceCheckerSpWpBenchmarkType.s_SizeOfPredicatesBP:
				return m_SizeOfPredicates[1];
			case TraceCheckerSpWpBenchmarkType.s_ConjunctsInSSA:
				return m_ConjunctsInSSA;
			case TraceCheckerSpWpBenchmarkType.s_ConjunctsInUnsatCore:
				return m_ConjunctsInUC;
			default:
				return super.getValue(key);
			}
		}

		@Override
		public TraceCheckerSpWpBenchmarkType getBenchmarkType() {
			return TraceCheckerSpWpBenchmarkType.getInstance();
		}
	}

}
