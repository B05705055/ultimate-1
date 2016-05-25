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
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.AutomatonDefinitionPrinter;
import de.uni_freiburg.informatik.ultimate.automata.AutomatonDefinitionPrinter.Format;
import de.uni_freiburg.informatik.ultimate.automata.HistogramOfIterable;
import de.uni_freiburg.informatik.ultimate.automata.IAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.IDoubleDeckerAutomaton;
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
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.minimization.MinimizeNwaMaxSat2;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.minimization.MinimizeSevpa;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.minimization.ShrinkNwa;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.minimization.maxsat.MinimizeNwaMaxSAT;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.simulation.delayed.BuchiReduce;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.simulation.fair.ReduceBuchiFairDirectSimulation;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.simulation.fair.ReduceBuchiFairSimulation;
import de.uni_freiburg.informatik.ultimate.boogie.annotation.LTLPropertyCheck;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.core.lib.results.TerminationArgumentResult;
import de.uni_freiburg.informatik.ultimate.core.model.models.ILocation;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceProvider;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.SupportingInvariant;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.RankingFunction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.IHoareTripleChecker;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.IncrementalHoareTripleChecker;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SolverBuilder.SolverMode;
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
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ProgramPoint;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RcfgElement;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.preferences.RcfgPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.util.RcfgProgramExecution;
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

	private final String mName;

	/**
	 * Node of a recursive control flow graph which stores additional information about the
	 */
	protected final RootNode mRootNode;

	/**
	 * Intermediate layer to encapsulate communication with SMT solvers.
	 */
	protected final SmtManager mSmtManager;

	protected final BinaryStatePredicateManager mBinaryStatePredicateManager;

	/**
	 * Intermediate layer to encapsulate preferences.
	 */
	protected final TAPreferences mPref;

	/**
	 * Current Iteration of this CEGAR loop.
	 */
	protected int mIteration = 0;

	/**
	 * Accepting run of the abstraction obtained in this iteration.
	 */
	protected NestedLassoRun<CodeBlock, IPredicate> mCounterexample;

	/**
	 * Abstraction of this iteration. The language of mAbstraction is a set of traces which is
	 * <ul>
	 * <li>a superset of the feasible program traces.
	 * <li>a subset of the traces which respect the control flow of of the program.
	 */
	protected INestedWordAutomatonOldApi<CodeBlock, IPredicate> mAbstraction;

	/**
	 * Interpolant automaton of this iteration.
	 */
	protected NestedWordAutomaton<CodeBlock, IPredicate> mInterpolAutomaton;

	protected IAutomaton<CodeBlock, IPredicate> mArtifactAutomaton;
	protected final static Format mPrintAutomataLabeling = Format.ATS;

	// used for the collection of statistics
	int mInfeasible = 0;
	int mRankWithoutSi = 0;
	int mRankWithSi = 0;

	private final PredicateFactoryForInterpolantAutomata mDefaultStateFactory;
	private final PredicateFactoryResultChecking mPredicateFactoryResultChecking;

	private final HoareAnnotationFragments mHaf;

	private final PredicateFactoryRefinement mStateFactoryForRefinement;

	private final BuchiAutomizerModuleDecompositionBenchmark mMDBenchmark;

	private final BuchiCegarLoopBenchmarkGenerator mBenchmarkGenerator;

	private static final boolean s_ReduceAbstractionSize = true;

	private final boolean mDifference;
	private final boolean mUseDoubleDeckers;
	private final BComplementationConstruction mComplementationConstruction;
	private final BInterpolantAutomaton mInterpolantAutomaton;
	private final boolean mBouncerStem;
	private final boolean mBouncerLoop;
	private final boolean mScroogeNondeterminismStem;
	private final boolean mScroogeNondeterminismLoop;
	private final boolean mCannibalizeLoop;
	private final boolean mConstructTermcompProof;
	private final TermcompProofBenchmark mTermcompProofBenchmark;

	private final INTERPOLATION mInterpolation;

	private final RefineBuchi mRefineBuchi;
	private final List<RefineBuchi.RefinementSetting> mBuchiRefinementSettingSequence;

	private final AutomataMinimization mAutomataMinimization;

	private NonTerminationArgument mNonterminationArgument;

	private final IUltimateServiceProvider mServices;

	private final IToolchainStorage mStorage;

	private ToolchainCanceledException mToolchainCancelledException;

	public ToolchainCanceledException getToolchainCancelledException() {
		return mToolchainCancelledException;
	}

	public NonTerminationArgument getNonTerminationArgument() {
		return mNonterminationArgument;
	}

	public BuchiCegarLoop(RootNode rootNode, SmtManager smtManager, TAPreferences taPrefs,
			IUltimateServiceProvider services, IToolchainStorage storage) {
		assert services != null;
		mLTLMode = false;
		mServices = services;
		mStorage = storage;
		mLogger = mServices.getLoggingService().getLogger(Activator.PLUGIN_ID);
		mMDBenchmark = new BuchiAutomizerModuleDecompositionBenchmark(mServices.getBacktranslationService());
		mName = "BuchiCegarLoop";
		mRootNode = rootNode;
		mSmtManager = smtManager;
		mBinaryStatePredicateManager = new BinaryStatePredicateManager(mSmtManager, mServices);
		mBenchmarkGenerator = new BuchiCegarLoopBenchmarkGenerator();
		mBenchmarkGenerator.start(CegarLoopStatisticsDefinitions.OverallTime.toString());
		// this.buchiModGlobalVarManager = new BuchiModGlobalVarManager(
		// mBspm.getUnseededVariable(), mBspm.getOldRankVariable(),
		// mRootNode.getRootAnnot().getModGlobVarManager(),
		// mRootNode.getRootAnnot().getBoogie2SMT());

		mPref = taPrefs;
		mDefaultStateFactory = new PredicateFactoryForInterpolantAutomata(mSmtManager, mPref);
		mPredicateFactoryResultChecking = new PredicateFactoryResultChecking(mSmtManager);

		mHaf = new HoareAnnotationFragments(mLogger, null, null);
		mStateFactoryForRefinement = new PredicateFactoryRefinement(mRootNode.getRootAnnot().getProgramPoints(),
				mSmtManager, mPref, mPref.computeHoareAnnotation(), mHaf, null);

		final IPreferenceProvider baPref = mServices.getPreferenceProvider(Activator.PLUGIN_ID);

		mUseDoubleDeckers = !baPref.getBoolean(PreferenceInitializer.LABEL_IgnoreDownStates);
		mDifference = baPref.getBoolean(PreferenceInitializer.LABEL_DeterminizationOnDemand);
		mInterpolantAutomaton = baPref.getEnum(PreferenceInitializer.LABEL_BuchiInterpolantAutomaton,
				BInterpolantAutomaton.class);
		mComplementationConstruction = baPref.getEnum(PreferenceInitializer.LABEL_BuchiComplementationConstruction,
				BComplementationConstruction.class);
		mBouncerStem = baPref.getBoolean(PreferenceInitializer.LABEL_BouncerStem);
		mBouncerLoop = baPref.getBoolean(PreferenceInitializer.LABEL_BouncerLoop);
		mScroogeNondeterminismStem = baPref.getBoolean(PreferenceInitializer.LABEL_ScroogeNondeterminismStem);
		mScroogeNondeterminismLoop = baPref.getBoolean(PreferenceInitializer.LABEL_ScroogeNondeterminismLoop);
		if ((mScroogeNondeterminismStem || mScroogeNondeterminismLoop)
				&& mInterpolantAutomaton != BInterpolantAutomaton.ScroogeNondeterminism) {
			throw new IllegalArgumentException("illegal combination of settings");
		}
		if ((!mScroogeNondeterminismStem && !mScroogeNondeterminismLoop)
				&& mInterpolantAutomaton == BInterpolantAutomaton.ScroogeNondeterminism) {
			throw new IllegalArgumentException("illegal combination of settings");
		}
		mAutomataMinimization = baPref.getEnum(PreferenceInitializer.LABEL_AutomataMinimization,
				AutomataMinimization.class);
		mCannibalizeLoop = baPref.getBoolean(PreferenceInitializer.LABEL_CannibalizeLoop);
		mInterpolation = baPref.getEnum(TraceAbstractionPreferenceInitializer.LABEL_INTERPOLATED_LOCS,
				INTERPOLATION.class);

		InterpolationPreferenceChecker.check(Activator.PLUGIN_NAME, mInterpolation,
				baPref.getEnum(RcfgPreferenceInitializer.LABEL_Solver, SolverMode.class));
		mConstructTermcompProof = baPref.getBoolean(PreferenceInitializer.LABEL_TermcompProof);
		if (mConstructTermcompProof) {
			mTermcompProofBenchmark = new TermcompProofBenchmark(mServices);
		} else {
			mTermcompProofBenchmark = null;
		}

		mRefineBuchi = new RefineBuchi(mRootNode, mSmtManager, mPref.dumpAutomata(), mDifference, mDefaultStateFactory,
				mStateFactoryForRefinement, mUseDoubleDeckers, mPref.dumpPath(), mPref.getAutomataFormat(),
				mInterpolation, mServices, mLogger);
		mBuchiRefinementSettingSequence = new ArrayList<RefineBuchi.RefinementSetting>();
		switch (mInterpolantAutomaton) {
		case TwoStage:
			mBuchiRefinementSettingSequence.add(mRefineBuchi.new RefinementSetting(
					BInterpolantAutomaton.ScroogeNondeterminism, false, false, true, false, false));
			mBuchiRefinementSettingSequence.add(mRefineBuchi.new RefinementSetting(
					BInterpolantAutomaton.ScroogeNondeterminism, false, false, true, true, false));
			break;
		case Staged:
			mBuchiRefinementSettingSequence.add(mRefineBuchi.new RefinementSetting(BInterpolantAutomaton.Deterministic,
					true, false, false, false, false));
			mBuchiRefinementSettingSequence.add(mRefineBuchi.new RefinementSetting(BInterpolantAutomaton.Deterministic,
					true, true, false, false, false));
			mBuchiRefinementSettingSequence.add(mRefineBuchi.new RefinementSetting(
					BInterpolantAutomaton.ScroogeNondeterminism, true, false, true, false, false));
			mBuchiRefinementSettingSequence.add(mRefineBuchi.new RefinementSetting(
					BInterpolantAutomaton.ScroogeNondeterminism, true, true, true, false, false));
			mBuchiRefinementSettingSequence.add(mRefineBuchi.new RefinementSetting(
					BInterpolantAutomaton.ScroogeNondeterminism, false, false, true, true, false));
			break;
		case LassoAutomaton:
		case EagerNondeterminism:
		case ScroogeNondeterminism:
		case Deterministic:
			mBuchiRefinementSettingSequence.add(mRefineBuchi.new RefinementSetting(mInterpolantAutomaton, mBouncerStem,
					mBouncerLoop, mScroogeNondeterminismStem, mScroogeNondeterminismLoop, mCannibalizeLoop));
			break;
		default:
			throw new UnsupportedOperationException("unknown automaton");
		}
	}

	NestedLassoRun<CodeBlock, IPredicate> getCounterexample() {
		return mCounterexample;
	}

	static boolean emptyStem(NestedLassoRun<CodeBlock, IPredicate> nlr) {
		assert nlr.getStem().getLength() > 0;
		return nlr.getStem().getLength() == 1;
	}

	public final Result iterate() throws IOException {
		mLogger.info("Interprodecural is " + mPref.interprocedural());
		mLogger.info("Hoare is " + mPref.computeHoareAnnotation());
		mLogger.info("Compute interpolants for " + mInterpolation);
		mLogger.info("Backedges is " + mPref.interpolantAutomaton());
		mLogger.info("Determinization is " + mPref.interpolantAutomatonEnhancement());
		mLogger.info("Difference is " + mPref.differenceSenwa());
		mLogger.info("Minimize is " + mPref.minimize());

		mIteration = 0;
		mLogger.info("======== Iteration " + mIteration + "==of CEGAR loop == " + mName + "========");

		getInitialAbstraction();

		if (mIteration <= mPref.watchIteration()
				&& (mPref.artifact() == Artifact.ABSTRACTION || mPref.artifact() == Artifact.RCFG)) {
			mArtifactAutomaton = mAbstraction;
		}
		if (mPref.dumpAutomata()) {
			final String filename = mRootNode.getFilename() + "_" + mName + "Abstraction" + mIteration;
			writeAutomatonToFile(mServices, mAbstraction, mPref.dumpPath(), filename, mPref.getAutomataFormat(), "");
		}

		boolean initalAbstractionCorrect;
		try {
			initalAbstractionCorrect = isAbstractionCorrect();
		} catch (final AutomataLibraryException e1) {
			mLogger.warn("Verification cancelled");
			mMDBenchmark.reportRemainderModule(mAbstraction.size(), false);
			mBenchmarkGenerator.setResult(Result.TIMEOUT);
			mToolchainCancelledException = new ToolchainCanceledException(e1.getClassOfThrower());
			return Result.TIMEOUT;
		}
		if (initalAbstractionCorrect) {
			mMDBenchmark.reportNoRemainderModule();
			mBenchmarkGenerator.setResult(Result.TERMINATING);
			return Result.TERMINATING;
		}

		for (mIteration = 1; mIteration <= mPref.maxIterations(); mIteration++) {
			mLogger.info("======== Iteration " + mIteration + "============");
			mSmtManager.setIteration(mIteration);
			mBenchmarkGenerator.announceNextIteration();

			boolean abstractionCorrect;
			try {
				abstractionCorrect = isAbstractionCorrect();
			} catch (final AutomataLibraryException e1) {
				mLogger.warn("Verification cancelled");
				mMDBenchmark.reportRemainderModule(mAbstraction.size(), false);
				if (mConstructTermcompProof) {
					mTermcompProofBenchmark.reportRemainderModule(false);
				}
				mBenchmarkGenerator.setResult(Result.TIMEOUT);
				mToolchainCancelledException = new ToolchainCanceledException(e1.getClassOfThrower());
				return Result.TIMEOUT;
			}
			if (abstractionCorrect) {
				mMDBenchmark.reportNoRemainderModule();
				if (mConstructTermcompProof) {
					mTermcompProofBenchmark.reportNoRemainderModule();
				}
				mBenchmarkGenerator.setResult(Result.TERMINATING);
				return Result.TERMINATING;
			}

			LassoChecker lassoChecker;
			try {
				mBenchmarkGenerator.start(BuchiCegarLoopBenchmark.s_LassoAnalysisTime);
				lassoChecker = new LassoChecker(mInterpolation, mSmtManager,
						mRootNode.getRootAnnot().getModGlobVarManager(),
						mRootNode.getRootAnnot().getBoogie2SMT().getAxioms(), mBinaryStatePredicateManager,
						mCounterexample, generateLassoCheckerIdentifier(), mServices, mStorage);
				if (lassoChecker.getLassoCheckResult().getContinueDirective() == ContinueDirective.REPORT_UNKNOWN) {
					// if result was unknown, then try again but this time add one
					// iteration of the loop to the stem.
					// This allows us to verify Vincent's coolant examples
					mLogger.info("Result of lasso check was UNKNOWN. I will concatenate loop to stem and try again.");
					final NestedRun<CodeBlock, IPredicate> newStem = mCounterexample.getStem()
							.concatenate(mCounterexample.getLoop());
					mCounterexample = new NestedLassoRun<>(newStem, mCounterexample.getLoop());
					lassoChecker = new LassoChecker(mInterpolation, mSmtManager,
							mRootNode.getRootAnnot().getModGlobVarManager(),
							mRootNode.getRootAnnot().getBoogie2SMT().getAxioms(), mBinaryStatePredicateManager,
							mCounterexample, generateLassoCheckerIdentifier(), mServices, mStorage);
				}
			} catch (final ToolchainCanceledException e) {
				mToolchainCancelledException = e;
				mBenchmarkGenerator.setResult(Result.TIMEOUT);
				return Result.TIMEOUT;
			} finally {
				mBenchmarkGenerator.stop(BuchiCegarLoopBenchmark.s_LassoAnalysisTime);
			}

			final ContinueDirective cd = lassoChecker.getLassoCheckResult().getContinueDirective();
			mBenchmarkGenerator.reportLassoAnalysis(lassoChecker);
			try {
				switch (cd) {
				case REFINE_BOTH: {
					final BinaryStatePredicateManager bspm = lassoChecker.getBinaryStatePredicateManager();
					if (bspm.isLoopWithoutStemTerminating()) {
						mRankWithoutSi++;
					} else {
						mRankWithSi++;
					}
					final ISLPredicate hondaISLP = (ISLPredicate) mCounterexample.getLoop().getStateAtPosition(0);
					final ProgramPoint hondaPP = hondaISLP.getProgramPoint();
					final TerminationArgumentResult<RcfgElement, Expression> tar = constructTAResult(
							bspm.getTerminationArgument(), hondaPP, mCounterexample.getStem().getWord(),
							mCounterexample.getLoop().getWord());
					mMDBenchmark.reportRankingFunction(mIteration, tar);

					final INestedWordAutomatonOldApi<CodeBlock, IPredicate> newAbstraction = refineBuchi(lassoChecker);
					mAbstraction = newAbstraction;
					mBinaryStatePredicateManager.clearPredicates();

					if (s_ReduceAbstractionSize) {
						reduceAbstractionSize();
					}

					refineFinite(lassoChecker);
					mInfeasible++;
				}
					break;
				case REFINE_FINITE:
					refineFinite(lassoChecker);
					mInfeasible++;
					break;

				case REFINE_BUCHI:
					final BinaryStatePredicateManager bspm = lassoChecker.getBinaryStatePredicateManager();
					if (bspm.isLoopWithoutStemTerminating()) {
						mRankWithoutSi++;
					} else {
						mRankWithSi++;
					}
					final ISLPredicate hondaISLP = (ISLPredicate) mCounterexample.getLoop().getStateAtPosition(0);
					final ProgramPoint hondaPP = hondaISLP.getProgramPoint();
					final TerminationArgumentResult<RcfgElement, Expression> tar = constructTAResult(
							bspm.getTerminationArgument(), hondaPP, mCounterexample.getStem().getWord(),
							mCounterexample.getLoop().getWord());
					mMDBenchmark.reportRankingFunction(mIteration, tar);

					final INestedWordAutomatonOldApi<CodeBlock, IPredicate> newAbstraction = refineBuchi(lassoChecker);
					mAbstraction = newAbstraction;
					mBinaryStatePredicateManager.clearPredicates();
					break;
				case REPORT_UNKNOWN:
					mMDBenchmark.reportRemainderModule(mAbstraction.size(), false);
					if (mConstructTermcompProof) {
						mTermcompProofBenchmark.reportRemainderModule(false);
					}
					mBenchmarkGenerator.setResult(Result.UNKNOWN);
					return Result.UNKNOWN;
				case REPORT_NONTERMINATION:
					if (!lassoWasOverapproximated().isEmpty()) {
						mMDBenchmark.reportRemainderModule(mAbstraction.size(), false);
						if (mConstructTermcompProof) {
							mTermcompProofBenchmark.reportRemainderModule(false);
						}
						mBenchmarkGenerator.setResult(Result.UNKNOWN);
						return Result.UNKNOWN;
					}
					mNonterminationArgument = lassoChecker.getNonTerminationArgument();
					mMDBenchmark.reportRemainderModule(mAbstraction.size(), true);
					if (mConstructTermcompProof) {
						mTermcompProofBenchmark.reportRemainderModule(true);
					}
					mBenchmarkGenerator.setResult(Result.NONTERMINATING);
					return Result.NONTERMINATING;
				default:
					throw new AssertionError("impossible case");
				}
				mLogger.info("Abstraction has " + mAbstraction.sizeInformation());
				// s_Logger.info("Interpolant automaton has " +
				// mRefineBuchi.getInterpolAutomatonUsedInRefinement().sizeInformation());

				if (s_ReduceAbstractionSize) {
					reduceAbstractionSize();
				}

				if (mIteration <= mPref.watchIteration() && mPref.artifact() == Artifact.ABSTRACTION) {
					mArtifactAutomaton = mAbstraction;
				}

				if (mPref.dumpAutomata()) {
					final String filename = mRootNode.getFilename() + "_" + "Abstraction" + mIteration;
					writeAutomatonToFile(mServices, mAbstraction, mPref.dumpPath(), filename, mPref.getAutomataFormat(),
							"");
				}
				mBenchmarkGenerator.reportAbstractionSize(mAbstraction.size(), mIteration);

			} catch (final AutomataOperationCanceledException e) {
				mToolchainCancelledException = new ToolchainCanceledException(e.getClassOfThrower());
				mBenchmarkGenerator.setResult(Result.TIMEOUT);
				return Result.TIMEOUT;
			} catch (final ToolchainCanceledException e) {
				mToolchainCancelledException = e;
				mBenchmarkGenerator.setResult(Result.TIMEOUT);
				return Result.TIMEOUT;
			}
			mInterpolAutomaton = null;
		}
		mBenchmarkGenerator.setResult(Result.TIMEOUT);
		return Result.TIMEOUT;
	}

	private Map<String, ILocation> lassoWasOverapproximated() {
		final NestedWord<CodeBlock> stem = mCounterexample.getStem().getWord();
		final NestedWord<CodeBlock> loop = mCounterexample.getLoop().getWord();
		final Map<String, ILocation> overapproximations = new HashMap<>();
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
		mBenchmarkGenerator.start(BuchiCegarLoopBenchmark.s_NonLiveStateRemoval);
		try {
			mAbstraction = (new RemoveNonLiveStates<CodeBlock, IPredicate>(new AutomataLibraryServices(mServices),
					mAbstraction)).getResult();
		} finally {
			mBenchmarkGenerator.stop(BuchiCegarLoopBenchmark.s_NonLiveStateRemoval);
		}
		mBenchmarkGenerator.start(BuchiCegarLoopBenchmark.s_BuchiClosure);
		try {
			mAbstraction = (new BuchiClosureNwa<>(new AutomataLibraryServices(mServices), mAbstraction));
			// mAbstraction = (new RemoveDeadEnds<CodeBlock, IPredicate>(mServices, mAbstraction)).getResult();
		} finally {
			mBenchmarkGenerator.stop(BuchiCegarLoopBenchmark.s_BuchiClosure);
		}
		mBenchmarkGenerator.start(CegarLoopStatisticsDefinitions.AutomataMinimizationTime.toString());
		final int statesBeforeMinimization = mAbstraction.size();
		mLogger.info("Abstraction has " + mAbstraction.sizeInformation());
		final Collection<Set<IPredicate>> partition = computePartition(mAbstraction);
		try {
			if (mAbstraction.size() > 0) {
				final INestedWordAutomatonOldApi<CodeBlock, IPredicate> minimized = minimize(partition);
				mAbstraction = minimized;
			}
		} catch (final AutomataOperationCanceledException e) {
			throw new ToolchainCanceledException(getClass(),
					"minimizing automaton with " + mAbstraction.size() + " states");
		} catch (final AutomataLibraryException e) {
			throw new AssertionError(e.getMessage());
		} finally {
			mBenchmarkGenerator.stop(CegarLoopStatisticsDefinitions.AutomataMinimizationTime.toString());
		}
		final int statesAfterMinimization = mAbstraction.size();
		mBenchmarkGenerator.announceStatesRemovedByMinimization(statesBeforeMinimization - statesAfterMinimization);
		mLogger.info("Abstraction has " + mAbstraction.sizeInformation());
	}

	private INestedWordAutomatonOldApi<CodeBlock, IPredicate> minimize(Collection<Set<IPredicate>> partition)
			throws AutomataOperationCanceledException, AutomataLibraryException {
		final INestedWordAutomatonOldApi<CodeBlock, IPredicate> result;
		switch (mAutomataMinimization) {
		case DelayedSimulation: {
			final BuchiReduce<CodeBlock, IPredicate> minimizeOp = new BuchiReduce<>(new AutomataLibraryServices(mServices),
					mStateFactoryForRefinement, mAbstraction);
			result = minimizeOp.getResult();
			break;
		}
		case FairSimulation_WithoutSCC: {
			final ReduceBuchiFairSimulation<CodeBlock, IPredicate> minimizeOp = new ReduceBuchiFairSimulation<>(
					new AutomataLibraryServices(mServices), mStateFactoryForRefinement, mAbstraction, false);
			result = minimizeOp.getResult();
			break;
		}
		case FairSimulation_WithSCC: {
			final ReduceBuchiFairSimulation<CodeBlock, IPredicate> minimizeOp = new ReduceBuchiFairSimulation<>(
					new AutomataLibraryServices(mServices), mStateFactoryForRefinement, mAbstraction, true);
			result = minimizeOp.getResult();
			break;
		}
		case FairDirectSimulation: {
			final ReduceBuchiFairDirectSimulation<CodeBlock, IPredicate> minimizeOp = new ReduceBuchiFairDirectSimulation<>(
					new AutomataLibraryServices(mServices), mStateFactoryForRefinement, mAbstraction, true);
			result = minimizeOp.getResult();
			break;
		}
		case MinimizeSevpa: {
			final MinimizeSevpa<CodeBlock, IPredicate> minimizeOp = new MinimizeSevpa<CodeBlock, IPredicate>(
					new AutomataLibraryServices(mServices), mAbstraction, partition, mStateFactoryForRefinement);
			assert (minimizeOp.checkResult(mPredicateFactoryResultChecking));
			result = minimizeOp.getResult();
			break;
		}
		case None: {
			result = mAbstraction;
			break;
		}
		case ShrinkNwa: {
			final ShrinkNwa<CodeBlock, IPredicate> minimizeOp = new ShrinkNwa<CodeBlock, IPredicate>(
					new AutomataLibraryServices(mServices), mStateFactoryForRefinement, mAbstraction, partition, true,
					false, false, 200, false, 0, false, false);
			assert minimizeOp.checkResult(mStateFactoryForRefinement);
			result = (new RemoveUnreachable<CodeBlock, IPredicate>(new AutomataLibraryServices(mServices),
					minimizeOp.getResult())).getResult();
			break;
		}
		case MinimizeNwaMaxSat2: {
			final MinimizeNwaMaxSat2<CodeBlock, IPredicate> minimizeOp = new MinimizeNwaMaxSat2<CodeBlock, IPredicate>(
					new AutomataLibraryServices(mServices), mStateFactoryForRefinement, (IDoubleDeckerAutomaton<CodeBlock, IPredicate>) mAbstraction);
			assert minimizeOp.checkResult(mStateFactoryForRefinement);
			result = (INestedWordAutomatonOldApi<CodeBlock, IPredicate>) minimizeOp.getResult();
			break;
		}
		case MinimizeNwaMaxSat: {
			final MinimizeNwaMaxSAT<CodeBlock, IPredicate> minimizeOp = new MinimizeNwaMaxSAT<CodeBlock, IPredicate>(
					new AutomataLibraryServices(mServices), mStateFactoryForRefinement, mAbstraction);
			assert minimizeOp.checkResult(mStateFactoryForRefinement);
			result = minimizeOp.getResult();
			break;
		}
		default:
			throw new AssertionError();
		}
		return result;
	}

	private INestedWordAutomatonOldApi<CodeBlock, IPredicate> refineBuchi(LassoChecker lassoChecker)
			throws AutomataOperationCanceledException {
		mBenchmarkGenerator.start(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
		int stage = 0;
		final BuchiModGlobalVarManager bmgvm = new BuchiModGlobalVarManager(
				lassoChecker.getBinaryStatePredicateManager().getUnseededVariable(),
				lassoChecker.getBinaryStatePredicateManager().getOldRankVariables(),
				mRootNode.getRootAnnot().getModGlobVarManager(), mRootNode.getRootAnnot().getBoogie2SMT());
		for (final RefinementSetting rs : mBuchiRefinementSettingSequence) {
			INestedWordAutomatonOldApi<CodeBlock, IPredicate> newAbstraction = null;
			try {
				newAbstraction = mRefineBuchi.refineBuchi(mAbstraction, mCounterexample, mIteration, rs,
						lassoChecker.getBinaryStatePredicateManager(), bmgvm, mInterpolation, mBenchmarkGenerator,
						mComplementationConstruction);
			} catch (final AutomataOperationCanceledException e) {
				mBenchmarkGenerator.stop(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
				final String runningTaskInfo = "applying " + e.getClassOfThrower().getSimpleName() + " in stage " + stage;
				throw new ToolchainCanceledException(getClass(), runningTaskInfo);
			} catch (final ToolchainCanceledException e) {
				mBenchmarkGenerator.stop(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
				throw e;
			} catch (final AutomataLibraryException e) {
				throw new AssertionError(e.getMessage());
			}
			if (newAbstraction != null) {
				if (mConstructTermcompProof) {
					mTermcompProofBenchmark.reportBuchiModule(mIteration,
							mRefineBuchi.getInterpolAutomatonUsedInRefinement());
				}
				mBenchmarkGenerator.announceSuccessfullRefinementStage(stage);
				switch (rs.getInterpolantAutomaton()) {
				case Deterministic:
				case LassoAutomaton:
					mMDBenchmark.reportDeterminsticModule(mIteration,
							mRefineBuchi.getInterpolAutomatonUsedInRefinement().size());
					break;
				case ScroogeNondeterminism:
				case EagerNondeterminism:
					mMDBenchmark.reportNonDeterminsticModule(mIteration,
							mRefineBuchi.getInterpolAutomatonUsedInRefinement().size());
					break;
				default:
					throw new AssertionError("unsupported");
				}
				mBenchmarkGenerator.stop(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
				mBenchmarkGenerator.addBackwardCoveringInformationBuchi(mRefineBuchi.getBci());
				return newAbstraction;
			}
			stage++;
		}
		throw new AssertionError("no settings was sufficient");
	}

	private boolean isAbstractionCorrect() throws AutomataLibraryException {
		final BuchiIsEmpty<CodeBlock, IPredicate> ec = new BuchiIsEmpty<CodeBlock, IPredicate>(
				new AutomataLibraryServices(mServices), mAbstraction);
		if (ec.getResult()) {
			return true;
		} else {
			mCounterexample = ec.getAcceptingNestedLassoRun();
			if (mLogger.isInfoEnabled()) {
				mLogger.info("Counterexample stem histogram "
						+ (new HistogramOfIterable<CodeBlock>(mCounterexample.getStem().getWord())));
				mLogger.info("Counterexample loop histogram "
						+ (new HistogramOfIterable<CodeBlock>(mCounterexample.getLoop().getWord())));
			}
			assert mCounterexample.getLoop().getLength() > 1;
			return false;
		}
	}

	private void getInitialAbstraction() {
		final CFG2NestedWordAutomaton cFG2NestedWordAutomaton = new CFG2NestedWordAutomaton(mServices,
				mPref.interprocedural(), mSmtManager, mLogger);
		Collection<ProgramPoint> acceptingNodes;
		final Collection<ProgramPoint> allNodes = new HashSet<ProgramPoint>();
		for (final Map<String, ProgramPoint> prog2pp : mRootNode.getRootAnnot().getProgramPoints().values()) {
			allNodes.addAll(prog2pp.values());
		}

		// check if we run in LTL mode and set accepting states accordingly
		if (LTLPropertyCheck.getAnnotation(mRootNode) != null) {
			mLTLMode = true;
			acceptingNodes = new HashSet<ProgramPoint>();
			for (final ProgramPoint pp : allNodes) {
				if (BuchiProgramAcceptingStateAnnotation.getAnnotation(pp) != null) {
					acceptingNodes.add(pp);
				}
			}
		} else {
			mLTLMode = false;
			acceptingNodes = allNodes;
		}
		mAbstraction = cFG2NestedWordAutomaton.getNestedWordAutomaton(mRootNode, mDefaultStateFactory, acceptingNodes);
	}

	private void refineFinite(LassoChecker lassoChecker) throws AutomataOperationCanceledException {
		mBenchmarkGenerator.start(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
		final InterpolatingTraceChecker traceChecker;
		final NestedRun<CodeBlock, IPredicate> run;
		final LassoCheckResult lcr = lassoChecker.getLassoCheckResult();
		if (lassoChecker.getLassoCheckResult().getStemFeasibility() == TraceCheckResult.INFEASIBLE) {
			// if both (stem and loop) are infeasible we take the smaller
			// one.
			final int stemSize = mCounterexample.getStem().getLength();
			final int loopSize = mCounterexample.getLoop().getLength();
			if (lcr.getLoopFeasibility() == TraceCheckResult.INFEASIBLE && loopSize <= stemSize) {
				traceChecker = lassoChecker.getLoopCheck();
				run = mCounterexample.getLoop();
			} else {
				traceChecker = lassoChecker.getStemCheck();
				run = mCounterexample.getStem();
			}
		} else if (lcr.getLoopFeasibility() == TraceCheckResult.INFEASIBLE) {
			traceChecker = lassoChecker.getLoopCheck();
			run = mCounterexample.getLoop();
		} else {
			assert lcr.getConcatFeasibility() == TraceCheckResult.INFEASIBLE;
			traceChecker = lassoChecker.getConcatCheck();
			run = lassoChecker.getConcatenatedCounterexample();
		}
		final BackwardCoveringInformation bci = TraceCheckerUtils.computeCoverageCapability(mServices, traceChecker, mLogger);
		mBenchmarkGenerator.addBackwardCoveringInformationFinite(bci);
		constructInterpolantAutomaton(traceChecker, run);

		final ModifiableGlobalVariableManager modGlobVarManager = mRootNode.getRootAnnot().getModGlobVarManager();
		final IHoareTripleChecker solverHtc = new IncrementalHoareTripleChecker(
				mRootNode.getRootAnnot().getManagedScript(), modGlobVarManager, mSmtManager.getBoogie2Smt());
		final IHoareTripleChecker htc = new EfficientHoareTripleChecker(solverHtc, modGlobVarManager,
				traceChecker.getPredicateUnifier(), mSmtManager);

		final DeterministicInterpolantAutomaton determinized = new DeterministicInterpolantAutomaton(mServices, mSmtManager,
				modGlobVarManager, htc, mAbstraction, mInterpolAutomaton, traceChecker.getPredicateUnifier(), mLogger,
				false, false);
		final PowersetDeterminizer<CodeBlock, IPredicate> psd = new PowersetDeterminizer<CodeBlock, IPredicate>(determinized,
				true, mDefaultStateFactory);
		Difference<CodeBlock, IPredicate> diff = null;
		try {
			diff = new Difference<CodeBlock, IPredicate>(new AutomataLibraryServices(mServices), mAbstraction,
					determinized, psd, mStateFactoryForRefinement, true);
		} catch (final AutomataLibraryException e) {
			if (e instanceof AutomataOperationCanceledException) {
				mBenchmarkGenerator.stop(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
				throw (AutomataOperationCanceledException) e;
			} else {
				throw new AssertionError();
			}
		} catch (final ToolchainCanceledException e) {
			mBenchmarkGenerator.stop(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
			throw e;
		}
		determinized.switchToReadonlyMode();
		if (mPref.dumpAutomata()) {
			final String filename = mRootNode.getFilename() + "_" + "interpolAutomatonUsedInRefinement" + mIteration
					+ "after";
			writeAutomatonToFile(mServices, mInterpolAutomaton, mPref.dumpPath(), filename, mPref.getAutomataFormat(),
					"");
		}
		if (mConstructTermcompProof) {
			mTermcompProofBenchmark.reportFiniteModule(mIteration, mRefineBuchi.getInterpolAutomatonUsedInRefinement());
		}
		mMDBenchmark.reportTrivialModule(mIteration, mInterpolAutomaton.size());
		assert (new InductivityCheck(mServices,
				mInterpolAutomaton, false, true, new IncrementalHoareTripleChecker(
						mRootNode.getRootAnnot().getManagedScript(), modGlobVarManager, mSmtManager.getBoogie2Smt())))
								.getResult();
		mAbstraction = diff.getResult();
		mBenchmarkGenerator.addEdgeCheckerData(htc.getEdgeCheckerBenchmark());
		mBenchmarkGenerator.stop(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
	}

	protected void constructInterpolantAutomaton(InterpolatingTraceChecker traceChecker,
			NestedRun<CodeBlock, IPredicate> run) throws AutomataOperationCanceledException {
		final CanonicalInterpolantAutomatonBuilder iab = new CanonicalInterpolantAutomatonBuilder(mServices, traceChecker,
				CoverageAnalysis.extractProgramPoints(run), new InCaReAlphabet<CodeBlock>(mAbstraction), mSmtManager,
				mAbstraction.getStateFactory(), mLogger);
		iab.analyze();
		mInterpolAutomaton = iab.getInterpolantAutomaton();

		try {
			assert ((new Accepts<CodeBlock, IPredicate>(new AutomataLibraryServices(mServices), mInterpolAutomaton,
					run.getWord())).getResult()) : "Interpolant automaton broken!";
		} catch (final AutomataLibraryException e) {
			throw new AssertionError(e);
		}
		// assert((new BuchiAccepts<CodeBlock, IPredicate>(mInterpolAutomaton,
		// mCounterexample.getNestedLassoWord())).getResult()) :
		// "Interpolant automaton broken!";
		assert (new InductivityCheck(mServices, mInterpolAutomaton, false, true,
				new IncrementalHoareTripleChecker(mRootNode.getRootAnnot().getManagedScript(),
						mRootNode.getRootAnnot().getModGlobVarManager(), mSmtManager.getBoogie2Smt()))).getResult();
	}

	private TerminationArgumentResult<RcfgElement, Expression> constructTAResult(
			TerminationArgument terminationArgument, ProgramPoint honda, NestedWord<CodeBlock> stem,
			NestedWord<CodeBlock> loop) {
		final RankingFunction rf = terminationArgument.getRankingFunction();
		final Collection<SupportingInvariant> si_list = terminationArgument.getSupportingInvariants();
		final Expression[] supporting_invariants = new Expression[si_list.size()];
		int i = 0;
		for (final SupportingInvariant si : terminationArgument.getSupportingInvariants()) {
			supporting_invariants[i] = si.asExpression(mSmtManager.getScript(),
					mRootNode.getRootAnnot().getBoogie2SMT().getTerm2Expression());
			++i;
		}
		final TerminationArgumentResult<RcfgElement, Expression> result = new TerminationArgumentResult<RcfgElement, Expression>(
				honda, Activator.PLUGIN_NAME,
				rf.asLexExpression(mSmtManager.getScript(),
						mRootNode.getRootAnnot().getBoogie2SMT().getTerm2Expression()),
				rf.getName(), supporting_invariants, mServices.getBacktranslationService(), Expression.class);
		return result;
	}

	public Collection<Set<IPredicate>> computePartition(INestedWordAutomatonOldApi<CodeBlock, IPredicate> automaton) {
		mLogger.info("Start computation of initial partition.");
		final Collection<IPredicate> states = automaton.getStates();
		final Map<ProgramPoint, Set<IPredicate>> accepting = new HashMap<ProgramPoint, Set<IPredicate>>();
		final Map<ProgramPoint, Set<IPredicate>> nonAccepting = new HashMap<ProgramPoint, Set<IPredicate>>();
		for (final IPredicate p : states) {
			final ISLPredicate sp = (ISLPredicate) p;
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
		final Collection<Set<IPredicate>> partition = new ArrayList<Set<IPredicate>>();
		for (final ProgramPoint pp : accepting.keySet()) {
			final Set<IPredicate> statesWithSamePP = accepting.get(pp);
			partition.add(statesWithSamePP);
		}
		for (final ProgramPoint pp : nonAccepting.keySet()) {
			final Set<IPredicate> statesWithSamePP = nonAccepting.get(pp);
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
		return mMDBenchmark;
	}

	public TermcompProofBenchmark getTermcompProofBenchmark() {
		return mTermcompProofBenchmark;
	}

	/**
	 * Returns an Identifier that describes a lasso analysis. Right now, this is the Filename (without path prefix) of
	 * analyzed file together with the number of the current iteration.
	 * 
	 */
	public String generateLassoCheckerIdentifier() {
		final String pureFilename = mRootNode.getFilename();
		return pureFilename + "_Iteration" + mIteration;
	}

	public BuchiCegarLoopBenchmarkGenerator getBenchmarkGenerator() {
		return mBenchmarkGenerator;
	}

	/**
	 * @return true iff run in LTL mode and results should be interpreted accordingly.
	 */
	public boolean isInLTLMode() {
		return mLTLMode;
	}

}
