/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2013-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE BuchiAutomizer plug-in.
 * 
 * The ULTIMATE BuchiAutomizer plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE BuchiAutomizer plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE BuchiAutomizer plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE BuchiAutomizer plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE BuchiAutomizer plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.AutomatonDefinitionPrinter;
import de.uni_freiburg.informatik.ultimate.automata.AutomatonDefinitionPrinter.Format;
import de.uni_freiburg.informatik.ultimate.automata.HistogramOfIterable;
import de.uni_freiburg.informatik.ultimate.automata.IAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonOldApi;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.InCaReAlphabet;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedRun;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.buchiNwa.BuchiClosureNwa;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.buchiNwa.BuchiIsEmpty;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.buchiNwa.NestedLassoRun;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.Accepts;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.Difference;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.PowersetDeterminizer;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.RemoveNonLiveStates;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.RemoveUnreachable;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.minimization.MinimizeSevpa;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.minimization.ShrinkNwa;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.simulation.delayed.BuchiReduce;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.simulation.fair.ReduceBuchiFairDirectSimulation;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.simulation.fair.ReduceBuchiFairSimulation;
import de.uni_freiburg.informatik.ultimate.boogie.annotation.LTLPropertyCheck;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.core.lib.results.TerminationArgumentResult;
import de.uni_freiburg.informatik.ultimate.core.model.models.ILocation;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.core.preferences.RcpPreferenceProvider;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.SupportingInvariant;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.RankingFunction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.IHoareTripleChecker;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.IncrementalHoareTripleChecker;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer.LassoChecker.ContinueDirective;
import de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer.LassoChecker.LassoCheckResult;
import de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer.LassoChecker.TraceCheckResult;
import de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer.RefineBuchi.RefinementSetting;
import de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer.annot.BuchiProgramAcceptingStateAnnotation;
import de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer.preferences.PreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer.preferences.PreferenceInitializer.AutomataMinimization;
import de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer.preferences.PreferenceInitializer.BComplementationConstruction;
import de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer.preferences.PreferenceInitializer.BInterpolantAutomaton;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.RcfgProgramExecution;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ProgramPoint;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RcfgElement;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.AbstractCegarLoop.CegarLoopStatisticsDefinitions;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.CFG2NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.CoverageAnalysis;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.CoverageAnalysis.BackwardCoveringInformation;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.HoareAnnotationFragments;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.PredicateFactoryForInterpolantAutomata;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.PredicateFactoryRefinement;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.PredicateFactoryResultChecking;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.InterpolantAutomataTransitionAppender.DeterministicInterpolantAutomaton;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.interpolantAutomataBuilders.CanonicalInterpolantAutomatonBuilder;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.EfficientHoareTripleChecker;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.ISLPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.InductivityCheck;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.InterpolationPreferenceChecker;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TAPreferences;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TAPreferences.Artifact;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TraceAbstractionPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TraceAbstractionPreferenceInitializer.INTERPOLATION;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.InterpolatingTraceChecker;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.TraceCheckerUtils;
import de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException;

public class BuchiCegarLoop {
	protected final ILogger mLogger;

	/**
	 * Result of CEGAR loop iteration
	 * <ul>
	 * <li>SAFE: there is no feasible trace to an error location
	 * <li>UNSAFE: there is a feasible trace to an error location (the underlying program has at least one execution
	 * which violates its specification)
	 * <li>UNKNOWN: we found a trace for which we could not decide feasibility or we found an infeasible trace but were
	 * not able to exclude it in abstraction refinement.
	 * <li>TIMEOUT:
	 */
	public enum Result {
		TERMINATING, TIMEOUT, UNKNOWN, NONTERMINATING
	}

	/**
	 * true iff we are run in an LTL toolchain and should report appropriate results
	 */
	private boolean mLTLMode;

	private final String m_Name;

	/**
	 * Node of a recursive control flow graph which stores additional information about the
	 */
	protected final RootNode m_RootNode;

	/**
	 * Intermediate layer to encapsulate communication with SMT solvers.
	 */
	protected final SmtManager m_SmtManager;

	protected final BinaryStatePredicateManager m_BinaryStatePredicateManager;

	/**
	 * Intermediate layer to encapsulate preferences.
	 */
	protected final TAPreferences m_Pref;

	/**
	 * Current Iteration of this CEGAR loop.
	 */
	protected int m_Iteration = 0;

	/**
	 * Accepting run of the abstraction obtained in this iteration.
	 */
	protected NestedLassoRun<CodeBlock, IPredicate> m_Counterexample;

	/**
	 * Abstraction of this iteration. The language of m_Abstraction is a set of traces which is
	 * <ul>
	 * <li>a superset of the feasible program traces.
	 * <li>a subset of the traces which respect the control flow of of the program.
	 */
	protected INestedWordAutomatonOldApi<CodeBlock, IPredicate> m_Abstraction;

	/**
	 * Interpolant automaton of this iteration.
	 */
	protected NestedWordAutomaton<CodeBlock, IPredicate> m_InterpolAutomaton;

	protected IAutomaton<CodeBlock, IPredicate> m_ArtifactAutomaton;
	protected final static Format m_PrintAutomataLabeling = Format.ATS;

	// used for the collection of statistics
	int m_Infeasible = 0;
	int m_RankWithoutSi = 0;
	int m_RankWithSi = 0;

	private final PredicateFactoryForInterpolantAutomata m_DefaultStateFactory;
	private final PredicateFactoryResultChecking m_PredicateFactoryResultChecking;

	private final HoareAnnotationFragments m_Haf;

	private final PredicateFactoryRefinement m_StateFactoryForRefinement;

	private final BuchiAutomizerModuleDecompositionBenchmark m_MDBenchmark;

	private final BuchiCegarLoopBenchmarkGenerator m_BenchmarkGenerator;

	private static final boolean s_ReduceAbstractionSize = true;

	private final boolean m_Difference;
	private final boolean m_UseDoubleDeckers;
	private final BComplementationConstruction m_ComplementationConstruction;
	private final BInterpolantAutomaton m_InterpolantAutomaton;
	private final boolean m_BouncerStem;
	private final boolean m_BouncerLoop;
	private final boolean m_ScroogeNondeterminismStem;
	private final boolean m_ScroogeNondeterminismLoop;
	private final boolean m_CannibalizeLoop;
	private final boolean m_ConstructTermcompProof;
	private final TermcompProofBenchmark m_TermcompProofBenchmark;

	private final INTERPOLATION m_Interpolation;

	private final RefineBuchi m_RefineBuchi;
	private final List<RefineBuchi.RefinementSetting> m_BuchiRefinementSettingSequence;

	private final AutomataMinimization m_AutomataMinimization;

	private NonTerminationArgument m_NonterminationArgument;

	private final IUltimateServiceProvider m_Services;

	private final IToolchainStorage mStorage;

	private ToolchainCanceledException m_ToolchainCancelledException;

	public ToolchainCanceledException getToolchainCancelledException() {
		return m_ToolchainCancelledException;
	}

	public NonTerminationArgument getNonTerminationArgument() {
		return m_NonterminationArgument;
	}

	public BuchiCegarLoop(RootNode rootNode, SmtManager smtManager, TAPreferences taPrefs,
			IUltimateServiceProvider services, IToolchainStorage storage) {
		assert services != null;
		mLTLMode = false;
		m_Services = services;
		mStorage = storage;
		mLogger = m_Services.getLoggingService().getLogger(Activator.s_PLUGIN_ID);
		m_MDBenchmark = new BuchiAutomizerModuleDecompositionBenchmark(m_Services.getBacktranslationService());
		m_Name = "BuchiCegarLoop";
		m_RootNode = rootNode;
		m_SmtManager = smtManager;
		m_BinaryStatePredicateManager = new BinaryStatePredicateManager(m_SmtManager, m_Services);
		m_BenchmarkGenerator = new BuchiCegarLoopBenchmarkGenerator();
		m_BenchmarkGenerator.start(CegarLoopStatisticsDefinitions.OverallTime.toString());
		// this.buchiModGlobalVarManager = new BuchiModGlobalVarManager(
		// m_Bspm.getUnseededVariable(), m_Bspm.getOldRankVariable(),
		// m_RootNode.getRootAnnot().getModGlobVarManager(),
		// m_RootNode.getRootAnnot().getBoogie2SMT());

		m_Pref = taPrefs;
		m_DefaultStateFactory = new PredicateFactoryForInterpolantAutomata(m_SmtManager, m_Pref);
		m_PredicateFactoryResultChecking = new PredicateFactoryResultChecking(m_SmtManager);

		m_Haf = new HoareAnnotationFragments(mLogger, null, null);
		m_StateFactoryForRefinement = new PredicateFactoryRefinement(m_RootNode.getRootAnnot().getProgramPoints(),
				m_SmtManager, m_Pref, m_Pref.computeHoareAnnotation(), m_Haf, null);

		RcpPreferenceProvider baPref = new RcpPreferenceProvider(Activator.s_PLUGIN_ID);

		m_UseDoubleDeckers = !baPref.getBoolean(PreferenceInitializer.LABEL_IgnoreDownStates);
		m_Difference = baPref.getBoolean(PreferenceInitializer.LABEL_DeterminizationOnDemand);
		m_InterpolantAutomaton = baPref.getEnum(PreferenceInitializer.LABEL_BuchiInterpolantAutomaton,
				BInterpolantAutomaton.class);
		m_ComplementationConstruction = baPref.getEnum(PreferenceInitializer.LABEL_BuchiComplementationConstruction,
				BComplementationConstruction.class);
		m_BouncerStem = baPref.getBoolean(PreferenceInitializer.LABEL_BouncerStem);
		m_BouncerLoop = baPref.getBoolean(PreferenceInitializer.LABEL_BouncerLoop);
		m_ScroogeNondeterminismStem = baPref.getBoolean(PreferenceInitializer.LABEL_ScroogeNondeterminismStem);
		m_ScroogeNondeterminismLoop = baPref.getBoolean(PreferenceInitializer.LABEL_ScroogeNondeterminismLoop);
		if ((m_ScroogeNondeterminismStem || m_ScroogeNondeterminismLoop)
				&& m_InterpolantAutomaton != BInterpolantAutomaton.ScroogeNondeterminism) {
			throw new IllegalArgumentException("illegal combination of settings");
		}
		if ((!m_ScroogeNondeterminismStem && !m_ScroogeNondeterminismLoop)
				&& m_InterpolantAutomaton == BInterpolantAutomaton.ScroogeNondeterminism) {
			throw new IllegalArgumentException("illegal combination of settings");
		}
		m_AutomataMinimization = baPref.getEnum(PreferenceInitializer.LABEL_AutomataMinimization,
				AutomataMinimization.class);
		m_CannibalizeLoop = baPref.getBoolean(PreferenceInitializer.LABEL_CannibalizeLoop);
		m_Interpolation = baPref.getEnum(TraceAbstractionPreferenceInitializer.LABEL_INTERPOLATED_LOCS,
				INTERPOLATION.class);
		InterpolationPreferenceChecker.check(Activator.s_PLUGIN_NAME, m_Interpolation);
		m_ConstructTermcompProof = baPref.getBoolean(PreferenceInitializer.LABEL_TermcompProof);
		if (m_ConstructTermcompProof) {
			m_TermcompProofBenchmark = new TermcompProofBenchmark(m_Services);
		} else {
			m_TermcompProofBenchmark = null;
		}

		m_RefineBuchi = new RefineBuchi(m_RootNode, m_SmtManager, m_Pref.dumpAutomata(), m_Difference,
				m_DefaultStateFactory, m_StateFactoryForRefinement, m_UseDoubleDeckers, m_Pref.dumpPath(),
				m_Pref.getAutomataFormat(), m_Interpolation, m_Services, mLogger);
		m_BuchiRefinementSettingSequence = new ArrayList<RefineBuchi.RefinementSetting>();
		switch (m_InterpolantAutomaton) {
		case TwoStage:
			m_BuchiRefinementSettingSequence.add(m_RefineBuchi.new RefinementSetting(
					BInterpolantAutomaton.ScroogeNondeterminism, false, false, true, false, false));
			m_BuchiRefinementSettingSequence.add(m_RefineBuchi.new RefinementSetting(
					BInterpolantAutomaton.ScroogeNondeterminism, false, false, true, true, false));
			break;
		case Staged:
			m_BuchiRefinementSettingSequence.add(m_RefineBuchi.new RefinementSetting(
					BInterpolantAutomaton.Deterministic, true, false, false, false, false));
			m_BuchiRefinementSettingSequence.add(m_RefineBuchi.new RefinementSetting(
					BInterpolantAutomaton.Deterministic, true, true, false, false, false));
			m_BuchiRefinementSettingSequence.add(m_RefineBuchi.new RefinementSetting(
					BInterpolantAutomaton.ScroogeNondeterminism, true, false, true, false, false));
			m_BuchiRefinementSettingSequence.add(m_RefineBuchi.new RefinementSetting(
					BInterpolantAutomaton.ScroogeNondeterminism, true, true, true, false, false));
			m_BuchiRefinementSettingSequence.add(m_RefineBuchi.new RefinementSetting(
					BInterpolantAutomaton.ScroogeNondeterminism, false, false, true, true, false));
			break;
		case LassoAutomaton:
		case EagerNondeterminism:
		case ScroogeNondeterminism:
		case Deterministic:
			m_BuchiRefinementSettingSequence
					.add(m_RefineBuchi.new RefinementSetting(m_InterpolantAutomaton, m_BouncerStem, m_BouncerLoop,
							m_ScroogeNondeterminismStem, m_ScroogeNondeterminismLoop, m_CannibalizeLoop));
			break;
		default:
			throw new UnsupportedOperationException("unknown automaton");
		}
	}

	NestedLassoRun<CodeBlock, IPredicate> getCounterexample() {
		return m_Counterexample;
	}

	static boolean emptyStem(NestedLassoRun<CodeBlock, IPredicate> nlr) {
		assert nlr.getStem().getLength() > 0;
		return nlr.getStem().getLength() == 1;
	}

	public final Result iterate() throws IOException {
		mLogger.info("Interprodecural is " + m_Pref.interprocedural());
		mLogger.info("Hoare is " + m_Pref.computeHoareAnnotation());
		mLogger.info("Compute interpolants for " + m_Interpolation);
		mLogger.info("Backedges is " + m_Pref.interpolantAutomaton());
		mLogger.info("Determinization is " + m_Pref.interpolantAutomatonEnhancement());
		mLogger.info("Difference is " + m_Pref.differenceSenwa());
		mLogger.info("Minimize is " + m_Pref.minimize());

		m_Iteration = 0;
		mLogger.info("======== Iteration " + m_Iteration + "==of CEGAR loop == " + m_Name + "========");

		// try {
		getInitialAbstraction();
		// } catch (OperationCanceledException e1) {
		// s_Logger.warn("Verification cancelled");
		// return Result.TIMEOUT;
		// }

		if (m_Iteration <= m_Pref.watchIteration()
				&& (m_Pref.artifact() == Artifact.ABSTRACTION || m_Pref.artifact() == Artifact.RCFG)) {
			m_ArtifactAutomaton = m_Abstraction;
		}
		if (m_Pref.dumpAutomata()) {
			String filename = m_RootNode.getFilename() + "_" + m_Name + "Abstraction" + m_Iteration;
			writeAutomatonToFile(m_Services, m_Abstraction, m_Pref.dumpPath(), filename, m_Pref.getAutomataFormat(),
					"");
		}

		boolean initalAbstractionCorrect;
		try {
			initalAbstractionCorrect = isAbstractionCorrect();
		} catch (AutomataLibraryException e1) {
			mLogger.warn("Verification cancelled");
			m_MDBenchmark.reportRemainderModule(m_Abstraction.size(), false);
			m_BenchmarkGenerator.setResult(Result.TIMEOUT);
			m_ToolchainCancelledException = new ToolchainCanceledException(e1.getClassOfThrower());
			return Result.TIMEOUT;
		}
		if (initalAbstractionCorrect) {
			m_MDBenchmark.reportNoRemainderModule();
			m_BenchmarkGenerator.setResult(Result.TERMINATING);
			return Result.TERMINATING;
		}

		for (m_Iteration = 1; m_Iteration <= m_Pref.maxIterations(); m_Iteration++) {
			mLogger.info("======== Iteration " + m_Iteration + "============");
			m_SmtManager.setIteration(m_Iteration);
			m_BenchmarkGenerator.announceNextIteration();

			boolean abstractionCorrect;
			try {
				abstractionCorrect = isAbstractionCorrect();
			} catch (AutomataLibraryException e1) {
				mLogger.warn("Verification cancelled");
				m_MDBenchmark.reportRemainderModule(m_Abstraction.size(), false);
				if (m_ConstructTermcompProof) {
					m_TermcompProofBenchmark.reportRemainderModule(false);
				}
				m_BenchmarkGenerator.setResult(Result.TIMEOUT);
				m_ToolchainCancelledException = new ToolchainCanceledException(e1.getClassOfThrower());
				return Result.TIMEOUT;
			}
			if (abstractionCorrect) {
				m_MDBenchmark.reportNoRemainderModule();
				if (m_ConstructTermcompProof) {
					m_TermcompProofBenchmark.reportNoRemainderModule();
				}
				m_BenchmarkGenerator.setResult(Result.TERMINATING);
				return Result.TERMINATING;
			}

			LassoChecker lassoChecker;
			try {
				m_BenchmarkGenerator.start(BuchiCegarLoopBenchmark.s_LassoAnalysisTime);
				lassoChecker = new LassoChecker(m_Interpolation, m_SmtManager,
						m_RootNode.getRootAnnot().getModGlobVarManager(),
						m_RootNode.getRootAnnot().getBoogie2SMT().getAxioms(), m_BinaryStatePredicateManager,
						m_Counterexample, generateLassoCheckerIdentifier(), m_Services, mStorage);
				if (lassoChecker.getLassoCheckResult().getContinueDirective() == ContinueDirective.REPORT_UNKNOWN) {
					// if result was unknown, then try again but this time add one
					// iteration of the loop to the stem.
					// This allows us to verify Vincent's coolant examples
					mLogger.info("Result of lasso check was UNKNOWN. I will concatenate loop to stem and try again.");
					NestedRun<CodeBlock, IPredicate> newStem = m_Counterexample.getStem()
							.concatenate(m_Counterexample.getLoop());
					m_Counterexample = new NestedLassoRun<>(newStem, m_Counterexample.getLoop());
					lassoChecker = new LassoChecker(m_Interpolation, m_SmtManager,
							m_RootNode.getRootAnnot().getModGlobVarManager(),
							m_RootNode.getRootAnnot().getBoogie2SMT().getAxioms(), m_BinaryStatePredicateManager,
							m_Counterexample, generateLassoCheckerIdentifier(), m_Services, mStorage);
				}
			} catch (ToolchainCanceledException e) {
				m_ToolchainCancelledException = e;
				m_BenchmarkGenerator.setResult(Result.TIMEOUT);
				return Result.TIMEOUT;
			} finally {
				m_BenchmarkGenerator.stop(BuchiCegarLoopBenchmark.s_LassoAnalysisTime);
			}

			ContinueDirective cd = lassoChecker.getLassoCheckResult().getContinueDirective();
			m_BenchmarkGenerator.reportLassoAnalysis(lassoChecker);
			try {
				switch (cd) {
				case REFINE_BOTH: {
					BinaryStatePredicateManager bspm = lassoChecker.getBinaryStatePredicateManager();
					if (bspm.isLoopWithoutStemTerminating()) {
						m_RankWithoutSi++;
					} else {
						m_RankWithSi++;
					}
					ISLPredicate hondaISLP = (ISLPredicate) m_Counterexample.getLoop().getStateAtPosition(0);
					ProgramPoint hondaPP = hondaISLP.getProgramPoint();
					TerminationArgumentResult<RcfgElement, Expression> tar = constructTAResult(
							bspm.getTerminationArgument(), hondaPP, m_Counterexample.getStem().getWord(),
							m_Counterexample.getLoop().getWord());
					m_MDBenchmark.reportRankingFunction(m_Iteration, tar);

					INestedWordAutomatonOldApi<CodeBlock, IPredicate> newAbstraction = refineBuchi(lassoChecker);
					m_Abstraction = newAbstraction;
					m_BinaryStatePredicateManager.clearPredicates();

					if (s_ReduceAbstractionSize) {
						reduceAbstractionSize();
					}

					refineFinite(lassoChecker);
					m_Infeasible++;
				}
					break;
				case REFINE_FINITE:
					refineFinite(lassoChecker);
					m_Infeasible++;
					break;

				case REFINE_BUCHI:
					BinaryStatePredicateManager bspm = lassoChecker.getBinaryStatePredicateManager();
					if (bspm.isLoopWithoutStemTerminating()) {
						m_RankWithoutSi++;
					} else {
						m_RankWithSi++;
					}
					ISLPredicate hondaISLP = (ISLPredicate) m_Counterexample.getLoop().getStateAtPosition(0);
					ProgramPoint hondaPP = hondaISLP.getProgramPoint();
					TerminationArgumentResult<RcfgElement, Expression> tar = constructTAResult(
							bspm.getTerminationArgument(), hondaPP, m_Counterexample.getStem().getWord(),
							m_Counterexample.getLoop().getWord());
					m_MDBenchmark.reportRankingFunction(m_Iteration, tar);

					INestedWordAutomatonOldApi<CodeBlock, IPredicate> newAbstraction = refineBuchi(lassoChecker);
					m_Abstraction = newAbstraction;
					m_BinaryStatePredicateManager.clearPredicates();
					break;
				case REPORT_UNKNOWN:
					m_MDBenchmark.reportRemainderModule(m_Abstraction.size(), false);
					if (m_ConstructTermcompProof) {
						m_TermcompProofBenchmark.reportRemainderModule(false);
					}
					m_BenchmarkGenerator.setResult(Result.UNKNOWN);
					return Result.UNKNOWN;
				case REPORT_NONTERMINATION:
					if (!lassoWasOverapproximated().isEmpty()) {
						m_MDBenchmark.reportRemainderModule(m_Abstraction.size(), false);
						if (m_ConstructTermcompProof) {
							m_TermcompProofBenchmark.reportRemainderModule(false);
						}
						m_BenchmarkGenerator.setResult(Result.UNKNOWN);
						return Result.UNKNOWN;
					}
					m_NonterminationArgument = lassoChecker.getNonTerminationArgument();
					m_MDBenchmark.reportRemainderModule(m_Abstraction.size(), true);
					if (m_ConstructTermcompProof) {
						m_TermcompProofBenchmark.reportRemainderModule(true);
					}
					m_BenchmarkGenerator.setResult(Result.NONTERMINATING);
					return Result.NONTERMINATING;
				default:
					throw new AssertionError("impossible case");
				}
				mLogger.info("Abstraction has " + m_Abstraction.sizeInformation());
				// s_Logger.info("Interpolant automaton has " +
				// m_RefineBuchi.getInterpolAutomatonUsedInRefinement().sizeInformation());

				if (s_ReduceAbstractionSize) {
					reduceAbstractionSize();
				}

				if (m_Iteration <= m_Pref.watchIteration() && m_Pref.artifact() == Artifact.ABSTRACTION) {
					m_ArtifactAutomaton = m_Abstraction;
				}

				if (m_Pref.dumpAutomata()) {
					String filename = m_RootNode.getFilename() + "_" + "Abstraction" + m_Iteration;
					writeAutomatonToFile(m_Services, m_Abstraction, m_Pref.dumpPath(), filename,
							m_Pref.getAutomataFormat(), "");
				}
				m_BenchmarkGenerator.reportAbstractionSize(m_Abstraction.size(), m_Iteration);

			} catch (AutomataOperationCanceledException e) {
				m_ToolchainCancelledException = new ToolchainCanceledException(e.getClassOfThrower());
				m_BenchmarkGenerator.setResult(Result.TIMEOUT);
				return Result.TIMEOUT;
			} catch (ToolchainCanceledException e) {
				m_ToolchainCancelledException = e;
				m_BenchmarkGenerator.setResult(Result.TIMEOUT);
				return Result.TIMEOUT;
			}
			m_InterpolAutomaton = null;
		}
		m_BenchmarkGenerator.setResult(Result.TIMEOUT);
		return Result.TIMEOUT;
	}

	private Map<String, ILocation> lassoWasOverapproximated() {
		NestedWord<CodeBlock> stem = m_Counterexample.getStem().getWord();
		NestedWord<CodeBlock> loop = m_Counterexample.getLoop().getWord();
		Map<String, ILocation> overapproximations = new HashMap<>();
		overapproximations.putAll(RcfgProgramExecution.getOverapproximations(stem.asList()));
		overapproximations.putAll(RcfgProgramExecution.getOverapproximations(loop.asList()));
		return overapproximations;
	}

	/**
	 * @throws AutomataOperationCanceledException
	 * @throws AutomataLibraryException
	 * @throws AssertionError
	 */
	private void reduceAbstractionSize() throws AutomataOperationCanceledException, AssertionError {
		m_BenchmarkGenerator.start(BuchiCegarLoopBenchmark.s_NonLiveStateRemoval);
		try {
			m_Abstraction = (new RemoveNonLiveStates<CodeBlock, IPredicate>(new AutomataLibraryServices(m_Services),
					m_Abstraction)).getResult();
		} finally {
			m_BenchmarkGenerator.stop(BuchiCegarLoopBenchmark.s_NonLiveStateRemoval);
		}
		m_BenchmarkGenerator.start(BuchiCegarLoopBenchmark.s_BuchiClosure);
		try {
			m_Abstraction = (new BuchiClosureNwa<>(
					new AutomataLibraryServices(m_Services), m_Abstraction));
			// m_Abstraction = (new RemoveDeadEnds<CodeBlock, IPredicate>(m_Services, m_Abstraction)).getResult();
		} finally {
			m_BenchmarkGenerator.stop(BuchiCegarLoopBenchmark.s_BuchiClosure);
		}
		m_BenchmarkGenerator.start(CegarLoopStatisticsDefinitions.AutomataMinimizationTime.toString());
		int statesBeforeMinimization = m_Abstraction.size();
		mLogger.info("Abstraction has " + m_Abstraction.sizeInformation());
		Collection<Set<IPredicate>> partition = computePartition(m_Abstraction);
		try {
			if (m_Abstraction.size() > 0) {
				INestedWordAutomatonOldApi<CodeBlock, IPredicate> minimized = minimize(partition);
				m_Abstraction = minimized;
			}
		} catch (AutomataOperationCanceledException e) {
			throw new ToolchainCanceledException(getClass(),
					"minimizing automaton with " + m_Abstraction.size() + " states");
		} catch (AutomataLibraryException e) {
			throw new AssertionError(e.getMessage());
		} finally {
			m_BenchmarkGenerator.stop(CegarLoopStatisticsDefinitions.AutomataMinimizationTime.toString());
		}
		int statesAfterMinimization = m_Abstraction.size();
		m_BenchmarkGenerator.announceStatesRemovedByMinimization(statesBeforeMinimization - statesAfterMinimization);
		mLogger.info("Abstraction has " + m_Abstraction.sizeInformation());
	}

	private INestedWordAutomatonOldApi<CodeBlock, IPredicate> minimize(Collection<Set<IPredicate>> partition)
			throws AutomataOperationCanceledException, AutomataLibraryException {
		final INestedWordAutomatonOldApi<CodeBlock, IPredicate> result;
		switch (m_AutomataMinimization) {
		case DelayedSimulation: {
			BuchiReduce<CodeBlock, IPredicate> minimizeOp = new BuchiReduce<>(new AutomataLibraryServices(m_Services),
					m_StateFactoryForRefinement, m_Abstraction);
			result = minimizeOp.getResult();
			break;
		}
		case FairSimulation_WithoutSCC: {
			ReduceBuchiFairSimulation<CodeBlock, IPredicate> minimizeOp = new ReduceBuchiFairSimulation<>(
					new AutomataLibraryServices(m_Services), m_StateFactoryForRefinement, m_Abstraction, false);
			result = minimizeOp.getResult();
			break;
		}
		case FairSimulation_WithSCC: {
			ReduceBuchiFairSimulation<CodeBlock, IPredicate> minimizeOp = new ReduceBuchiFairSimulation<>(
					new AutomataLibraryServices(m_Services), m_StateFactoryForRefinement, m_Abstraction, true);
			result = minimizeOp.getResult();
			break;
		}
		case FairDirectSimulation: {
			ReduceBuchiFairDirectSimulation<CodeBlock, IPredicate> minimizeOp = new ReduceBuchiFairDirectSimulation<>(
					new AutomataLibraryServices(m_Services), m_StateFactoryForRefinement, m_Abstraction, true);
			result = minimizeOp.getResult();
			break;
		}
		case MinimizeSevpa: {
			MinimizeSevpa<CodeBlock, IPredicate> minimizeOp = new MinimizeSevpa<CodeBlock, IPredicate>(
					new AutomataLibraryServices(m_Services), m_Abstraction, partition, m_StateFactoryForRefinement);
			assert (minimizeOp.checkResult(m_PredicateFactoryResultChecking));
			result = minimizeOp.getResult();
			break;
		}
		case None: {
			result = m_Abstraction;
			break;
		}
		case ShrinkNwa: {
			ShrinkNwa<CodeBlock, IPredicate> minimizeOp = new ShrinkNwa<CodeBlock, IPredicate>(
					new AutomataLibraryServices(m_Services), m_StateFactoryForRefinement, m_Abstraction, partition,
					true, false, false, 200, false, 0, false, false);
			assert minimizeOp.checkResult(m_StateFactoryForRefinement);
			result = (new RemoveUnreachable<CodeBlock, IPredicate>(new AutomataLibraryServices(m_Services),
					minimizeOp.getResult())).getResult();
			break;
		}
		default:
			throw new AssertionError();
		}
		return result;
	}

	private INestedWordAutomatonOldApi<CodeBlock, IPredicate> refineBuchi(LassoChecker lassoChecker)
			throws AutomataOperationCanceledException {
		m_BenchmarkGenerator.start(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
		int stage = 0;
		BuchiModGlobalVarManager bmgvm = new BuchiModGlobalVarManager(
				lassoChecker.getBinaryStatePredicateManager().getUnseededVariable(),
				lassoChecker.getBinaryStatePredicateManager().getOldRankVariables(),
				m_RootNode.getRootAnnot().getModGlobVarManager(), m_RootNode.getRootAnnot().getBoogie2SMT());
		for (RefinementSetting rs : m_BuchiRefinementSettingSequence) {
			// if (stage > 0) {
			// s_Logger.info("Statistics: We needed stage " + stage);
			// }
			INestedWordAutomatonOldApi<CodeBlock, IPredicate> newAbstraction = null;
			try {
				newAbstraction = m_RefineBuchi.refineBuchi(m_Abstraction, m_Counterexample, m_Iteration, rs,
						lassoChecker.getBinaryStatePredicateManager(), bmgvm, m_Interpolation, m_BenchmarkGenerator,
						m_ComplementationConstruction);
			} catch (AutomataOperationCanceledException e) {
				m_BenchmarkGenerator.stop(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
				String runningTaskInfo = "applying " + e.getClassOfThrower().getSimpleName() + " in stage " + stage;
				throw new ToolchainCanceledException(getClass(), runningTaskInfo);
			} catch (ToolchainCanceledException e) {
				m_BenchmarkGenerator.stop(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
				throw e;
			} catch (AutomataLibraryException e) {
				throw new AssertionError(e.getMessage());
			}
			if (newAbstraction != null) {
				if (m_ConstructTermcompProof) {
					m_TermcompProofBenchmark.reportBuchiModule(m_Iteration,
							m_RefineBuchi.getInterpolAutomatonUsedInRefinement());
				}
				m_BenchmarkGenerator.announceSuccessfullRefinementStage(stage);
				switch (rs.getInterpolantAutomaton()) {
				case Deterministic:
				case LassoAutomaton:
					m_MDBenchmark.reportDeterminsticModule(m_Iteration,
							m_RefineBuchi.getInterpolAutomatonUsedInRefinement().size());
					break;
				case ScroogeNondeterminism:
				case EagerNondeterminism:
					m_MDBenchmark.reportNonDeterminsticModule(m_Iteration,
							m_RefineBuchi.getInterpolAutomatonUsedInRefinement().size());
					break;
				default:
					throw new AssertionError("unsupported");
				}
				m_BenchmarkGenerator.stop(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
				m_BenchmarkGenerator.addBackwardCoveringInformationBuchi(m_RefineBuchi.getBci());
				return newAbstraction;
			}
			stage++;
		}
		throw new AssertionError("no settings was sufficient");
	}

	private boolean isAbstractionCorrect() throws AutomataLibraryException {
		BuchiIsEmpty<CodeBlock, IPredicate> ec = new BuchiIsEmpty<CodeBlock, IPredicate>(
				new AutomataLibraryServices(m_Services), m_Abstraction);
		if (ec.getResult()) {
			return true;
		} else {
			m_Counterexample = ec.getAcceptingNestedLassoRun();
			if (mLogger.isInfoEnabled()) {
				mLogger.info("Counterexample stem histogram "
						+ (new HistogramOfIterable<CodeBlock>(m_Counterexample.getStem().getWord())));
				mLogger.info("Counterexample loop histogram "
						+ (new HistogramOfIterable<CodeBlock>(m_Counterexample.getLoop().getWord())));
			}
			assert m_Counterexample.getLoop().getLength() > 1;
			return false;
		}
	}

	private void getInitialAbstraction() {
		CFG2NestedWordAutomaton cFG2NestedWordAutomaton = new CFG2NestedWordAutomaton(m_Services,
				m_Pref.interprocedural(), m_SmtManager, mLogger);
		Collection<ProgramPoint> acceptingNodes;
		Collection<ProgramPoint> allNodes = new HashSet<ProgramPoint>();
		for (Map<String, ProgramPoint> prog2pp : m_RootNode.getRootAnnot().getProgramPoints().values()) {
			allNodes.addAll(prog2pp.values());
		}

		// check if we run in LTL mode and set accepting states accordingly
		if (LTLPropertyCheck.getAnnotation(m_RootNode) != null) {
			mLTLMode = true;
			acceptingNodes = new HashSet<ProgramPoint>();
			for (ProgramPoint pp : allNodes) {
				if (BuchiProgramAcceptingStateAnnotation.getAnnotation(pp) != null) {
					acceptingNodes.add(pp);
				}
			}
		} else {
			mLTLMode = false;
			acceptingNodes = allNodes;
		}
		m_Abstraction = cFG2NestedWordAutomaton.getNestedWordAutomaton(m_RootNode, m_DefaultStateFactory,
				acceptingNodes);
	}

	private void refineFinite(LassoChecker lassoChecker) throws AutomataOperationCanceledException {
		m_BenchmarkGenerator.start(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
		final InterpolatingTraceChecker traceChecker;
		final NestedRun<CodeBlock, IPredicate> run;
		LassoCheckResult lcr = lassoChecker.getLassoCheckResult();
		if (lassoChecker.getLassoCheckResult().getStemFeasibility() == TraceCheckResult.INFEASIBLE) {
			// if both (stem and loop) are infeasible we take the smaller
			// one.
			int stemSize = m_Counterexample.getStem().getLength();
			int loopSize = m_Counterexample.getLoop().getLength();
			if (lcr.getLoopFeasibility() == TraceCheckResult.INFEASIBLE && loopSize <= stemSize) {
				traceChecker = lassoChecker.getLoopCheck();
				run = m_Counterexample.getLoop();
			} else {
				traceChecker = lassoChecker.getStemCheck();
				run = m_Counterexample.getStem();
			}
		} else if (lcr.getLoopFeasibility() == TraceCheckResult.INFEASIBLE) {
			traceChecker = lassoChecker.getLoopCheck();
			run = m_Counterexample.getLoop();
		} else {
			assert lcr.getConcatFeasibility() == TraceCheckResult.INFEASIBLE;
			traceChecker = lassoChecker.getConcatCheck();
			run = lassoChecker.getConcatenatedCounterexample();
		}
		BackwardCoveringInformation bci = TraceCheckerUtils.computeCoverageCapability(m_Services, traceChecker,
				mLogger);
		m_BenchmarkGenerator.addBackwardCoveringInformationFinite(bci);
		constructInterpolantAutomaton(traceChecker, run);

		ModifiableGlobalVariableManager modGlobVarManager = m_RootNode.getRootAnnot().getModGlobVarManager();
		final IHoareTripleChecker solverHtc = new IncrementalHoareTripleChecker(
				m_RootNode.getRootAnnot().getManagedScript(), modGlobVarManager, m_SmtManager.getBoogie2Smt());
		IHoareTripleChecker htc = new EfficientHoareTripleChecker(solverHtc, modGlobVarManager,
				traceChecker.getPredicateUnifier(), m_SmtManager);

		DeterministicInterpolantAutomaton determinized = new DeterministicInterpolantAutomaton(m_Services, m_SmtManager,
				modGlobVarManager, htc, m_Abstraction, m_InterpolAutomaton, traceChecker.getPredicateUnifier(), mLogger,
				false, false);
		PowersetDeterminizer<CodeBlock, IPredicate> psd = new PowersetDeterminizer<CodeBlock, IPredicate>(determinized,
				true, m_DefaultStateFactory);
		Difference<CodeBlock, IPredicate> diff = null;
		try {
			diff = new Difference<CodeBlock, IPredicate>(new AutomataLibraryServices(m_Services), m_Abstraction,
					determinized, psd, m_StateFactoryForRefinement, true);
		} catch (AutomataLibraryException e) {
			if (e instanceof AutomataOperationCanceledException) {
				m_BenchmarkGenerator.stop(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
				throw (AutomataOperationCanceledException) e;
			} else {
				throw new AssertionError();
			}
		} catch (ToolchainCanceledException e) {
			m_BenchmarkGenerator.stop(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
			throw e;
		}
		determinized.switchToReadonlyMode();
		if (m_Pref.dumpAutomata()) {
			String filename = m_RootNode.getFilename() + "_" + "interpolAutomatonUsedInRefinement" + m_Iteration
					+ "after";
			writeAutomatonToFile(m_Services, m_InterpolAutomaton, m_Pref.dumpPath(), filename,
					m_Pref.getAutomataFormat(), "");
		}
		if (m_ConstructTermcompProof) {
			m_TermcompProofBenchmark.reportFiniteModule(m_Iteration,
					m_RefineBuchi.getInterpolAutomatonUsedInRefinement());
		}
		m_MDBenchmark.reportTrivialModule(m_Iteration, m_InterpolAutomaton.size());
		assert (new InductivityCheck(m_Services,
				m_InterpolAutomaton, false, true, new IncrementalHoareTripleChecker(
						m_RootNode.getRootAnnot().getManagedScript(), modGlobVarManager, m_SmtManager.getBoogie2Smt())))
								.getResult();
		m_Abstraction = diff.getResult();
		m_BenchmarkGenerator.addEdgeCheckerData(htc.getEdgeCheckerBenchmark());
		m_BenchmarkGenerator.stop(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
	}

	protected void constructInterpolantAutomaton(InterpolatingTraceChecker traceChecker,
			NestedRun<CodeBlock, IPredicate> run) throws AutomataOperationCanceledException {
		CanonicalInterpolantAutomatonBuilder iab = new CanonicalInterpolantAutomatonBuilder(m_Services, traceChecker,
				CoverageAnalysis.extractProgramPoints(run), new InCaReAlphabet<CodeBlock>(m_Abstraction), m_SmtManager,
				m_Abstraction.getStateFactory(), mLogger);
		iab.analyze();
		m_InterpolAutomaton = iab.getInterpolantAutomaton();

		try {
			assert ((new Accepts<CodeBlock, IPredicate>(new AutomataLibraryServices(m_Services), m_InterpolAutomaton,
					run.getWord())).getResult()) : "Interpolant automaton broken!";
		} catch (AutomataLibraryException e) {
			throw new AssertionError(e);
		}
		// assert((new BuchiAccepts<CodeBlock, IPredicate>(m_InterpolAutomaton,
		// m_Counterexample.getNestedLassoWord())).getResult()) :
		// "Interpolant automaton broken!";
		assert (new InductivityCheck(m_Services, m_InterpolAutomaton, false, true,
				new IncrementalHoareTripleChecker(m_RootNode.getRootAnnot().getManagedScript(),
						m_RootNode.getRootAnnot().getModGlobVarManager(), m_SmtManager.getBoogie2Smt()))).getResult();
	}

	private TerminationArgumentResult<RcfgElement, Expression> constructTAResult(
			TerminationArgument terminationArgument, ProgramPoint honda, NestedWord<CodeBlock> stem,
			NestedWord<CodeBlock> loop) {
		RankingFunction rf = terminationArgument.getRankingFunction();
		Collection<SupportingInvariant> si_list = terminationArgument.getSupportingInvariants();
		Expression[] supporting_invariants = new Expression[si_list.size()];
		int i = 0;
		for (SupportingInvariant si : terminationArgument.getSupportingInvariants()) {
			supporting_invariants[i] = si.asExpression(m_SmtManager.getScript(),
					m_RootNode.getRootAnnot().getBoogie2SMT().getTerm2Expression());
			++i;
		}
		TerminationArgumentResult<RcfgElement, Expression> result = new TerminationArgumentResult<RcfgElement, Expression>(
				honda, Activator.s_PLUGIN_NAME,
				rf.asLexExpression(m_SmtManager.getScript(),
						m_RootNode.getRootAnnot().getBoogie2SMT().getTerm2Expression()),
				rf.getName(), supporting_invariants, m_Services.getBacktranslationService(), Expression.class);
		return result;
	}

	public Collection<Set<IPredicate>> computePartition(INestedWordAutomatonOldApi<CodeBlock, IPredicate> automaton) {
		mLogger.info("Start computation of initial partition.");
		Collection<IPredicate> states = automaton.getStates();
		Map<ProgramPoint, Set<IPredicate>> accepting = new HashMap<ProgramPoint, Set<IPredicate>>();
		Map<ProgramPoint, Set<IPredicate>> nonAccepting = new HashMap<ProgramPoint, Set<IPredicate>>();
		for (IPredicate p : states) {
			ISLPredicate sp = (ISLPredicate) p;
			if (automaton.isFinal(p)) {
				Set<IPredicate> statesWithSamePP = accepting.get(sp.getProgramPoint());
				if (statesWithSamePP == null) {
					statesWithSamePP = new HashSet<IPredicate>();
					accepting.put(sp.getProgramPoint(), statesWithSamePP);
				}
				statesWithSamePP.add(p);
			} else {
				Set<IPredicate> statesWithSamePP = nonAccepting.get(sp.getProgramPoint());
				if (statesWithSamePP == null) {
					statesWithSamePP = new HashSet<IPredicate>();
					nonAccepting.put(sp.getProgramPoint(), statesWithSamePP);
				}
				statesWithSamePP.add(p);
			}
		}
		Collection<Set<IPredicate>> partition = new ArrayList<Set<IPredicate>>();
		for (ProgramPoint pp : accepting.keySet()) {
			Set<IPredicate> statesWithSamePP = accepting.get(pp);
			partition.add(statesWithSamePP);
		}
		for (ProgramPoint pp : nonAccepting.keySet()) {
			Set<IPredicate> statesWithSamePP = nonAccepting.get(pp);
			partition.add(statesWithSamePP);
		}
		mLogger.info("Finished computation of initial partition.");
		return partition;
	}

	protected static void writeAutomatonToFile(IUltimateServiceProvider services,
			IAutomaton<CodeBlock, IPredicate> automaton, String path, String filename, Format format, String message) {
		new AutomatonDefinitionPrinter<String, String>(new AutomataLibraryServices(services), "nwa",
				path + "/" + filename, format, message, automaton);
	}

	public BuchiAutomizerModuleDecompositionBenchmark getMDBenchmark() {
		return m_MDBenchmark;
	}

	public TermcompProofBenchmark getTermcompProofBenchmark() {
		return m_TermcompProofBenchmark;
	}

	/**
	 * Returns an Identifier that describes a lasso analysis. Right now, this is the Filename (without path prefix) of
	 * analyzed file together with the number of the current iteration.
	 * 
	 */
	public String generateLassoCheckerIdentifier() {
		String pureFilename = m_RootNode.getFilename();
		return pureFilename + "_Iteration" + m_Iteration;
	}

	public BuchiCegarLoopBenchmarkGenerator getBenchmarkGenerator() {
		return m_BenchmarkGenerator;
	}

	/**
	 * @return true iff run in LTL mode and results should be interpreted accordingly.
	 */
	public boolean isInLTLMode() {
		return mLTLMode;
	}

}
