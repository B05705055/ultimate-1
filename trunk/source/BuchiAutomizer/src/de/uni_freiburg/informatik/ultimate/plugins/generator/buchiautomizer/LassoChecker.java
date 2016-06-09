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
import java.util.TreeMap;

import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedRun;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.buchiNwa.NestedLassoRun;
import de.uni_freiburg.informatik.ultimate.boogie.BoogieVar;
import de.uni_freiburg.informatik.ultimate.core.lib.results.BenchmarkResult;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceProvider;
import de.uni_freiburg.informatik.ultimate.core.model.results.IResult;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lassoranker.AnalysisType;
import de.uni_freiburg.informatik.ultimate.lassoranker.LassoAnalysis;
import de.uni_freiburg.informatik.ultimate.lassoranker.LassoAnalysis.AnalysisTechnique;
import de.uni_freiburg.informatik.ultimate.lassoranker.LassoAnalysis.PreprocessingBenchmark;
import de.uni_freiburg.informatik.ultimate.lassoranker.LassoRankerPreferences;
import de.uni_freiburg.informatik.ultimate.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationAnalysisSettings;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.NonterminationAnalysisBenchmark;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.SupportingInvariant;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationAnalysisBenchmark;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationAnalysisSettings;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.AffineTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.LexicographicTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.MultiphaseTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.NestedTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.PiecewiseTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.RankingTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.InequalityConverter.NlaHandling;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula.Infeasibility;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer.preferences.PreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.SequentialComposition;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.ISLPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TraceAbstractionPreferenceInitializer.AssertCodeBlockOrder;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TraceAbstractionPreferenceInitializer.INTERPOLATION;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TraceAbstractionPreferenceInitializer.UnsatCores;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.InterpolatingTraceChecker;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.InterpolatingTraceCheckerCraig;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.PredicateUnifier;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.TraceCheckerSpWp;
import de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException;

public class LassoChecker {

	private final ILogger mLogger;

	enum ContinueDirective {
		REFINE_FINITE, REFINE_BUCHI, REPORT_NONTERMINATION, REPORT_UNKNOWN, REFINE_BOTH
	}
	
	enum TraceCheckResult {
		FEASIBLE, INFEASIBLE, UNKNOWN, UNCHECKED
	}

	enum SynthesisResult {
		TERMINATING, NONTERMINATING, UNKNOWN, UNCHECKED
	}

	// ////////////////////////////// settings /////////////////////////////////

	private static final boolean mSimplifyStemAndLoop = true;
	/**
	 * If true we check if the loop is terminating even if the stem or
	 * the concatenation of stem and loop are already infeasible.
	 * This allows us to use refineFinite and refineBuchi in the same
	 * iteration.
	 */
	private final boolean mTryTwofoldRefinement;

	/**
	 * For debugging only. Check for termination arguments even if we found a
	 * nontermination argument. This may reveal unsoundness bugs.
	 */
	private static final boolean s_CheckTerminationEvenIfNonterminating = false;
	
	
	private static final boolean s_AvoidNonterminationCheckIfArraysAreContained = true;

	private final INTERPOLATION mInterpolation;

	/**
	 * Use an external solver. If false, we use SMTInterpol.
	 */
	private final boolean mExternalSolver_RankSynthesis;
	/**
	 * Command of external solver.
	 */
	private final String mExternalSolverCommand_RankSynthesis;
	/**
	 * Use an external solver. If false, we use SMTInterpol.
	 */
	private final boolean mExternalSolver_GntaSynthesis;
	/**
	 * Command of external solver.
	 */
	private final String mExternalSolverCommand_GntaSynthesis;
	
	private final AnalysisType mRankAnalysisType;
	private final AnalysisType mGntaAnalysisType;
	private final int mGntaDirections;
	private final boolean mTrySimplificationTerminationArgument;

	/**
	 * Try all templates but use the one that was found first. This is only
	 * useful to test all templates at once.
	 */
	private final boolean mTemplateBenchmarkMode;

	// ////////////////////////////// input /////////////////////////////////
	/**
	 * Intermediate layer to encapsulate communication with SMT solvers.
	 */
	private final SmtManager mSmtManager;

	private final ModifiableGlobalVariableManager mModifiableGlobalVariableManager;

	private final BinaryStatePredicateManager mBspm;

	/**
	 * Accepting run of the abstraction obtained in this iteration.
	 */
	private final NestedLassoRun<CodeBlock, IPredicate> mCounterexample;

	/**
	 * Identifier for this LassoChecker. Can be used to get unique filenames
	 * when dumping files.
	 */
	private final String mLassoCheckerIdentifier;

	// ////////////////////////////// auxilliary variables
	// //////////////////////

	private final IPredicate mTruePredicate;
	private final IPredicate mFalsePredicate;

	// ////////////////////////////// output /////////////////////////////////

	// private final BuchiModGlobalVarManager mBuchiModGlobalVarManager;

	private final PredicateUnifier mPredicateUnifier;

	private InterpolatingTraceChecker mStemCheck;
	private InterpolatingTraceChecker mLoopCheck;
	private InterpolatingTraceChecker mConcatCheck;

	private NestedRun<CodeBlock, IPredicate> mConcatenatedCounterexample;



	private NonTerminationArgument mNonterminationArgument;

	Collection<Term> mAxioms;
	private final IUltimateServiceProvider mServices;
	private final IToolchainStorage mStorage;
	private final boolean mRemoveSuperfluousSupportingInvariants = true;
	
	private final LassoCheckResult mLassoCheckResult;
	
	private final List<PreprocessingBenchmark> mpreprocessingBenchmarks = 
			new ArrayList<PreprocessingBenchmark>();
	
	private final List<TerminationAnalysisBenchmark> mTerminationAnalysisBenchmarks =
			new ArrayList<TerminationAnalysisBenchmark>();
	private final List<NonterminationAnalysisBenchmark> mNonterminationAnalysisBenchmarks =
			new ArrayList<NonterminationAnalysisBenchmark>();
	
	public LassoCheckResult getLassoCheckResult() {
		return mLassoCheckResult;
	}

	public InterpolatingTraceChecker getStemCheck() {
		return mStemCheck;
	}

	public InterpolatingTraceChecker getLoopCheck() {
		return mLoopCheck;
	}

	public InterpolatingTraceChecker getConcatCheck() {
		return mConcatCheck;
	}

	public NestedRun<CodeBlock, IPredicate> getConcatenatedCounterexample() {
		assert mConcatenatedCounterexample != null;
		return mConcatenatedCounterexample;
	}

	public BinaryStatePredicateManager getBinaryStatePredicateManager() {
		return mBspm;
	}

	public NonTerminationArgument getNonTerminationArgument() {
		return mNonterminationArgument;
	}
	
	public List<PreprocessingBenchmark> getPreprocessingBenchmarks() {
		return mpreprocessingBenchmarks;
	}
	
	public List<TerminationAnalysisBenchmark> getTerminationAnalysisBenchmarks() {
		return mTerminationAnalysisBenchmarks;
	}
	
	public List<NonterminationAnalysisBenchmark> getNonterminationAnalysisBenchmarks() {
		return mNonterminationAnalysisBenchmarks;
	}

	public LassoChecker(INTERPOLATION interpolation, SmtManager smtManager,
			ModifiableGlobalVariableManager modifiableGlobalVariableManager, Collection<Term> axioms,
			BinaryStatePredicateManager bspm, NestedLassoRun<CodeBlock, IPredicate> counterexample,
			String lassoCheckerIdentifier, IUltimateServiceProvider services, 
			IToolchainStorage storage) throws IOException {
		mServices = services;
		mStorage = storage;
		mLogger = mServices.getLoggingService().getLogger(Activator.PLUGIN_ID);
		final IPreferenceProvider baPref = mServices.getPreferenceProvider(Activator.PLUGIN_ID);
		mExternalSolver_RankSynthesis = baPref.getBoolean(PreferenceInitializer.LABEL_ExtSolverRank);
		mExternalSolverCommand_RankSynthesis = baPref.getString(PreferenceInitializer.LABEL_ExtSolverCommandRank);
		mExternalSolver_GntaSynthesis = baPref.getBoolean(PreferenceInitializer.LABEL_ExtSolverGNTA);
		mExternalSolverCommand_GntaSynthesis = baPref.getString(PreferenceInitializer.LABEL_ExtSolverCommandGNTA);
		mRankAnalysisType = baPref.getEnum(PreferenceInitializer.LABEL_AnalysisTypeRank, AnalysisType.class);
		mGntaAnalysisType = baPref.getEnum(PreferenceInitializer.LABEL_AnalysisTypeGNTA, AnalysisType.class);
		mGntaDirections = baPref.getInt(PreferenceInitializer.LABEL_GntaDirections);
		
		mTemplateBenchmarkMode = baPref.getBoolean(PreferenceInitializer.LABEL_TemplateBenchmarkMode);
		mTrySimplificationTerminationArgument = baPref.getBoolean(PreferenceInitializer.LABEL_Simplify);
		mTryTwofoldRefinement = baPref.getBoolean(PreferenceInitializer.LABEL_TryTwofoldRefinement);
		mInterpolation = interpolation;
		mSmtManager = smtManager;
		mModifiableGlobalVariableManager = modifiableGlobalVariableManager;
		mBspm = bspm;
		mCounterexample = counterexample;
		mLassoCheckerIdentifier = lassoCheckerIdentifier;
		mPredicateUnifier = new PredicateUnifier(mServices, mSmtManager);
		mTruePredicate = mPredicateUnifier.getTruePredicate();
		mFalsePredicate = mPredicateUnifier.getFalsePredicate();
		mAxioms = axioms;
		mLassoCheckResult = new LassoCheckResult();
		assert mLassoCheckResult.getStemFeasibility() != TraceCheckResult.UNCHECKED;
		assert (mLassoCheckResult.getLoopFeasibility() != TraceCheckResult.UNCHECKED)
				|| (mLassoCheckResult.getLoopFeasibility() != TraceCheckResult.INFEASIBLE && !mTryTwofoldRefinement);
		if (mLassoCheckResult.getStemFeasibility() == TraceCheckResult.INFEASIBLE) {
			assert mLassoCheckResult.getContinueDirective() == ContinueDirective.REFINE_FINITE
					|| mLassoCheckResult.getContinueDirective() == ContinueDirective.REFINE_BOTH;
		} else {
			if (mLassoCheckResult.getLoopFeasibility() == TraceCheckResult.INFEASIBLE) {
				assert mLassoCheckResult.getContinueDirective() == ContinueDirective.REFINE_FINITE;
			} else {
				// loop not infeasible
				if (mLassoCheckResult.getLoopTermination() == SynthesisResult.TERMINATING) {
					assert mBspm.providesPredicates();
				} else {
					assert mConcatCheck != null;
					if (mLassoCheckResult.getConcatFeasibility() == TraceCheckResult.INFEASIBLE) {
						assert mLassoCheckResult.getContinueDirective() == ContinueDirective.REFINE_FINITE
								|| mLassoCheckResult.getContinueDirective() == ContinueDirective.REFINE_BOTH;
						assert mConcatenatedCounterexample != null;
					} else {
						assert mLassoCheckResult.getContinueDirective() != ContinueDirective.REFINE_FINITE;
					}
				}
			}
		}
	}
	
	/**
	 * Object for that does computation of lasso check and stores the
	 * result.
	 * Note that the methods used for the computation also modify member
	 * variables of the superclass.
	 */
	class LassoCheckResult {
		
		private final TraceCheckResult mStemFeasibility;
		private final TraceCheckResult mLoopFeasibility;
		private final TraceCheckResult mConcatFeasibility;

		private final SynthesisResult mLoopTermination;
		private final SynthesisResult mLassoTermination;
		
		private final ContinueDirective mContinueDirective;


		public LassoCheckResult() throws IOException {
			final NestedRun<CodeBlock, IPredicate> stem = mCounterexample.getStem();
			mLogger.info("Stem: " + stem);
			final NestedRun<CodeBlock, IPredicate> loop = mCounterexample.getLoop();
			mLogger.info("Loop: " + loop);
			mStemFeasibility = checkStemFeasibility();
			if (mStemFeasibility == TraceCheckResult.INFEASIBLE) {
				mLogger.info("stem already infeasible");
				if (!mTryTwofoldRefinement) {
					mLoopFeasibility = TraceCheckResult.UNCHECKED;
					mConcatFeasibility = TraceCheckResult.UNCHECKED;
					mLoopTermination = SynthesisResult.UNCHECKED;
					mLassoTermination = SynthesisResult.UNCHECKED;
					mContinueDirective = ContinueDirective.REFINE_FINITE;
					return;
				}
			}
			mLoopFeasibility = checkLoopFeasibility();
			if (mLoopFeasibility == TraceCheckResult.INFEASIBLE) {
				mLogger.info("loop already infeasible");
				mConcatFeasibility = TraceCheckResult.UNCHECKED;
				mLoopTermination = SynthesisResult.UNCHECKED;
				mLassoTermination = SynthesisResult.UNCHECKED;
				mContinueDirective = ContinueDirective.REFINE_FINITE;
				return;
			} else {
				if (mStemFeasibility == TraceCheckResult.INFEASIBLE) {
					assert (mTryTwofoldRefinement);
					final TransFormula loopTF = computeLoopTF();
					mLoopTermination = checkLoopTermination(loopTF);
					mConcatFeasibility = TraceCheckResult.UNCHECKED;
					mLassoTermination = SynthesisResult.UNCHECKED;
					if (mLoopTermination == SynthesisResult.TERMINATING) {
						mContinueDirective = ContinueDirective.REFINE_BOTH;
						return;
					} else {
						mContinueDirective = ContinueDirective.REFINE_FINITE;
						return;
					}
				} else {
					// stem feasible
					mConcatFeasibility = checkConcatFeasibility();
					if (mConcatFeasibility == TraceCheckResult.INFEASIBLE) {
						mLassoTermination = SynthesisResult.UNCHECKED;
						if (mTryTwofoldRefinement) {
							final TransFormula loopTF = computeLoopTF();
							mLoopTermination = checkLoopTermination(loopTF);
							if (mLoopTermination == SynthesisResult.TERMINATING) {
								mContinueDirective = ContinueDirective.REFINE_BOTH;
								return;
							} else {
								mContinueDirective = ContinueDirective.REFINE_FINITE;
								return;
							}
						} else {
							mLoopTermination = SynthesisResult.UNCHECKED;
							mContinueDirective = ContinueDirective.REFINE_FINITE;
							return;
						}
					} else {
						// concat feasible
						final TransFormula loopTF = computeLoopTF();
						// checking loop termination before we check lasso 
						// termination is a workaround.
						// We want to avoid supporting invariants in possible
						// yet the termination argument simplification of the
						// LassoChecker is not optimal. Hence we first check
						// only the loop, which guarantees that there are no
						// supporting invariants.
						mLoopTermination = checkLoopTermination(loopTF);
						if (mLoopTermination == SynthesisResult.TERMINATING) {
							mLassoTermination = SynthesisResult.UNCHECKED;
							mContinueDirective = ContinueDirective.REFINE_BUCHI;
							return;
						} else {
							final TransFormula stemTF = computeStemTF();
							mLassoTermination = checkLassoTermination(stemTF, loopTF);
							if (mLassoTermination == SynthesisResult.TERMINATING) {
								mContinueDirective = ContinueDirective.REFINE_BUCHI;
								return;
							} else if (mLassoTermination == SynthesisResult.NONTERMINATING) {
								mContinueDirective = ContinueDirective.REPORT_NONTERMINATION;
								return;
							} else {
								mContinueDirective = ContinueDirective.REPORT_UNKNOWN;
								return;
							}
						}
					}
				}
			}
		}

		private TraceCheckResult checkStemFeasibility() {
			final NestedRun<CodeBlock, IPredicate> stem = mCounterexample.getStem();
			if (BuchiCegarLoop.emptyStem(mCounterexample)) {
				return TraceCheckResult.FEASIBLE;
			} else {
				mStemCheck = checkFeasibilityAndComputeInterpolants(stem);
				return translateSatisfiabilityToFeasibility(mStemCheck.isCorrect());
			}
		}

		private TraceCheckResult checkLoopFeasibility() {
			final NestedRun<CodeBlock, IPredicate> loop = mCounterexample.getLoop();
			mLoopCheck = checkFeasibilityAndComputeInterpolants(loop);
			return translateSatisfiabilityToFeasibility(mLoopCheck.isCorrect());
		}

		private TraceCheckResult checkConcatFeasibility() {
			final NestedRun<CodeBlock, IPredicate> stem = mCounterexample.getStem();
			final NestedRun<CodeBlock, IPredicate> loop = mCounterexample.getLoop();
			final NestedRun<CodeBlock, IPredicate> concat = stem.concatenate(loop);
			mConcatCheck = checkFeasibilityAndComputeInterpolants(concat);
			if (mConcatCheck.isCorrect() == LBool.UNSAT) {
				mConcatenatedCounterexample = concat;
			}
			return translateSatisfiabilityToFeasibility(mConcatCheck.isCorrect());
		}

		private TraceCheckResult translateSatisfiabilityToFeasibility(LBool lBool) {
			switch (lBool) {
			case SAT:
				return TraceCheckResult.FEASIBLE;
			case UNKNOWN:
				return TraceCheckResult.UNKNOWN;
			case UNSAT:
				return TraceCheckResult.INFEASIBLE;
			default:
				throw new AssertionError("unknown case");
			}
		}

		private InterpolatingTraceChecker checkFeasibilityAndComputeInterpolants(NestedRun<CodeBlock, IPredicate> run) {
			InterpolatingTraceChecker result;
			switch (mInterpolation) {
			case Craig_NestedInterpolation:
			case Craig_TreeInterpolation:
				result = new InterpolatingTraceCheckerCraig(mTruePredicate, mFalsePredicate, new TreeMap<Integer, IPredicate>(),
						run.getWord(), mSmtManager, mModifiableGlobalVariableManager,
						/*
						 * TODO: When Matthias
						 * introduced this parameter he
						 * set the argument to AssertCodeBlockOrder.NOT_INCREMENTALLY.
						 * Check if you want to set this
						 * to a different value.
						 */AssertCodeBlockOrder.NOT_INCREMENTALLY, mServices, false, mPredicateUnifier, mInterpolation, true);
				break;
			case ForwardPredicates:
			case BackwardPredicates:
			case FPandBP:
				result = new TraceCheckerSpWp(mTruePredicate, mFalsePredicate, new TreeMap<Integer, IPredicate>(),
						run.getWord(), mSmtManager, mModifiableGlobalVariableManager,
						/*
						 * TODO: When Matthias
						 * introduced this parameter he
						 * set the argument to AssertCodeBlockOrder.NOT_INCREMENTALLY.
						 * Check if you want to set this
						 * to a different value.
						 */AssertCodeBlockOrder.NOT_INCREMENTALLY, 
						 UnsatCores.CONJUNCT_LEVEL, true, mServices, false, mPredicateUnifier, mInterpolation, mSmtManager);
				break;
			default:
				throw new UnsupportedOperationException("unsupported interpolation");
			}
			if (result.getToolchainCancelledExpection() != null) {
				throw result.getToolchainCancelledExpection();
			}
			return result;
		}

		private SynthesisResult checkLoopTermination(TransFormula loopTF) throws IOException {
			assert !mBspm.providesPredicates() : "termination already checked";
			final boolean containsArrays = SmtUtils.containsArrayVariables(loopTF.getFormula());
			if (containsArrays) {
				// if there are array variables we will probably run in a huge
				// DNF, so as a precaution we do not check and say unknown
				return SynthesisResult.UNKNOWN;
			} else {
				return synthesize(false, null, loopTF, containsArrays);
			}
		}

		private SynthesisResult checkLassoTermination(TransFormula stemTF, TransFormula loopTF) throws IOException {
			assert !mBspm.providesPredicates() : "termination already checked";
			assert loopTF != null;
			final boolean containsArrays = SmtUtils.containsArrayVariables(stemTF.getFormula())
					|| SmtUtils.containsArrayVariables(loopTF.getFormula());
			return synthesize(true, stemTF, loopTF, containsArrays);
		}

		public TraceCheckResult getStemFeasibility() {
			return mStemFeasibility;
		}

		public TraceCheckResult getLoopFeasibility() {
			return mLoopFeasibility;
		}

		public TraceCheckResult getConcatFeasibility() {
			return mConcatFeasibility;
		}

		public SynthesisResult getLoopTermination() {
			return mLoopTermination;
		}

		public SynthesisResult getLassoTermination() {
			return mLassoTermination;
		}

		public ContinueDirective getContinueDirective() {
			return mContinueDirective;
		}
	
	}

	/**
	 * Compute TransFormula that represents the stem.
	 */
	protected TransFormula computeStemTF() {
		final NestedWord<CodeBlock> stem = mCounterexample.getStem().getWord();
		try {
			final TransFormula stemTF = computeTF(stem, mSimplifyStemAndLoop, true, false);
			return stemTF;
		} catch (final ToolchainCanceledException tce) {
			throw new ToolchainCanceledException(getClass(), 
					tce.getRunningTaskInfo() + " while constructing stem TransFormula");
		}
	}

	/**
	 * Compute TransFormula that represents the loop.
	 */
	protected TransFormula computeLoopTF() {
		final NestedWord<CodeBlock> loop = mCounterexample.getLoop().getWord();
		try {
			final TransFormula loopTF = computeTF(loop, mSimplifyStemAndLoop, true, false);
			return loopTF;
		} catch (final ToolchainCanceledException tce) {
			throw new ToolchainCanceledException(getClass(), 
					tce.getRunningTaskInfo() + " while constructing loop TransFormula");
		}
	}

	/**
	 * Compute TransFormula that represents the NestedWord word.
	 */
	private TransFormula computeTF(NestedWord<CodeBlock> word, boolean simplify,
			boolean extendedPartialQuantifierElimination, boolean withBranchEncoders) {
		final boolean toCNF = false;
		final TransFormula tf = SequentialComposition.getInterproceduralTransFormula(mSmtManager.getBoogie2Smt(),
				mModifiableGlobalVariableManager, simplify, extendedPartialQuantifierElimination, toCNF,
				withBranchEncoders, mLogger, mServices, word.asList());
		return tf;
	}

	private boolean areSupportingInvariantsCorrect() {
		final NestedWord<CodeBlock> stem = mCounterexample.getStem().getWord();
		mLogger.info("Stem: " + stem);
		final NestedWord<CodeBlock> loop = mCounterexample.getLoop().getWord();
		mLogger.info("Loop: " + loop);
		boolean siCorrect = true;
		if (stem.length() == 0) {
			// do nothing
			// TODO: check that si is equivalent to true
		} else {
			for (final SupportingInvariant si : mBspm.getTerminationArgument().getSupportingInvariants()) {
				final IPredicate siPred = mBspm.supportingInvariant2Predicate(si);
				siCorrect &= mBspm.checkSupportingInvariant(siPred, stem, loop, mModifiableGlobalVariableManager);
			}
			// check array index supporting invariants
			for (final Term aisi : mBspm.getTerminationArgument().getArrayIndexSupportingInvariants()) {
				final IPredicate siPred = mBspm.term2Predicate(aisi);
				siCorrect &= mBspm.checkSupportingInvariant(siPred, stem, loop, mModifiableGlobalVariableManager);
			}
		}
		return siCorrect;
	}

	private boolean isRankingFunctionCorrect() {
		final NestedWord<CodeBlock> loop = mCounterexample.getLoop().getWord();
		mLogger.info("Loop: " + loop);
		final boolean rfCorrect = mBspm.checkRankDecrease(loop, mModifiableGlobalVariableManager);
		return rfCorrect;
	}

	private String generateFileBasenamePrefix(boolean withStem) {
		return mLassoCheckerIdentifier + "_" + (withStem ? "Lasso" : "Loop");
	}

	private LassoRankerPreferences constructLassoRankerPreferences(boolean withStem,
			boolean overapproximateArrayIndexConnection, NlaHandling nlaHandling, 
			AnalysisTechnique analysis) {
		final LassoRankerPreferences pref = new LassoRankerPreferences();
		switch (analysis) {
		case GEOMETRIC_NONTERMINATION_ARGUMENTS: {
			pref.externalSolver = mExternalSolver_GntaSynthesis;
			pref.smt_solver_command = mExternalSolverCommand_GntaSynthesis;
			break;
		}
		case RANKING_FUNCTIONS_SUPPORTING_INVARIANTS: {
			pref.externalSolver = mExternalSolver_RankSynthesis;
			pref.smt_solver_command = mExternalSolverCommand_RankSynthesis;
			break;
		}
		default:
			throw new AssertionError();
		}
		final IPreferenceProvider baPref = mServices.getPreferenceProvider(Activator.PLUGIN_ID);
		pref.dumpSmtSolverScript = baPref.getBoolean(PreferenceInitializer.LABEL_DumpToFile);
		pref.path_of_dumped_script = baPref.getString(PreferenceInitializer.LABEL_DumpPath);
		pref.baseNameOfDumpedScript = generateFileBasenamePrefix(withStem);
		pref.overapproximateArrayIndexConnection = overapproximateArrayIndexConnection;
		pref.nlaHandling = nlaHandling;
		return pref;
	}

	private TerminationAnalysisSettings constructTASettings() {
		final TerminationAnalysisSettings settings = new TerminationAnalysisSettings();
		settings.analysis = mRankAnalysisType;
		settings.numnon_strict_invariants = 1;
		settings.numstrict_invariants = 0;
		settings.nondecreasing_invariants = true;
		settings.simplify_termination_argument = mTrySimplificationTerminationArgument;
		settings.simplify_supporting_invariants = mTrySimplificationTerminationArgument;
		return settings;
	}

	private NonTerminationAnalysisSettings constructNTASettings() {
		final NonTerminationAnalysisSettings settings = new NonTerminationAnalysisSettings();
		settings.analysis = mGntaAnalysisType;
		settings.number_of_gevs = mGntaDirections;
		return settings;
	}

	private SynthesisResult synthesize(final boolean withStem, TransFormula stemTF, final TransFormula loopTF,
			boolean containsArrays) throws IOException {
		if (mSmtManager.isLocked()) {
			throw new AssertionError("SMTManager must not be locked at the beginning of synthesis");
		}
		
		final Set<BoogieVar> modifiableGlobalsAtHonda = mModifiableGlobalVariableManager.getModifiedBoogieVars(
				((ISLPredicate) mCounterexample.getLoop().getStateAtPosition(0)).getProgramPoint().getProcedure());

		if (!withStem) {
			stemTF = getDummyTF();
		}
		// TODO: present this somewhere else
		// int loopVars = loopTF.getFormula().getFreeVars().length;
		// if (stemTF == null) {
		// s_Logger.info("Statistics: no stem, loopVars: " + loopVars);
		// } else {
		// int stemVars = stemTF.getFormula().getFreeVars().length;
		// s_Logger.info("Statistics: stemVars: " + stemVars + "loopVars: " +
		// loopVars);
		// }
		
		final boolean doNonterminationAnalysis = !(s_AvoidNonterminationCheckIfArraysAreContained && containsArrays);
		
		NonTerminationArgument nonTermArgument = null;
		if (doNonterminationAnalysis) {
			LassoAnalysis laNT = null;
			try {
				final boolean overapproximateArrayIndexConnection = false;
				laNT = new LassoAnalysis(mSmtManager.getScript(), mSmtManager.getBoogie2Smt(), stemTF, loopTF,
						modifiableGlobalsAtHonda, mAxioms.toArray(new Term[mAxioms.size()]), 
						constructLassoRankerPreferences(withStem, overapproximateArrayIndexConnection, 
						NlaHandling.UNDERAPPROXIMATE, AnalysisTechnique.GEOMETRIC_NONTERMINATION_ARGUMENTS), mServices, mStorage);
				mpreprocessingBenchmarks.add(laNT.getPreprocessingBenchmark());
			} catch (final TermException e) {
				e.printStackTrace();
				throw new AssertionError("TermException " + e);
			}
			try {
				final NonTerminationAnalysisSettings settings = constructNTASettings();
				nonTermArgument = laNT.checkNonTermination(settings);
				final List<NonterminationAnalysisBenchmark> benchs = laNT.getNonterminationAnalysisBenchmarks();
				mNonterminationAnalysisBenchmarks.addAll(benchs);
			} catch (final SMTLIBException e) {
				e.printStackTrace();
				throw new AssertionError("SMTLIBException " + e);
			} catch (final TermException e) {
				e.printStackTrace();
				throw new AssertionError("TermException " + e);
			}
			if (withStem) {
				mNonterminationArgument = nonTermArgument;
			}
			if (!s_CheckTerminationEvenIfNonterminating && nonTermArgument != null) {
				return SynthesisResult.NONTERMINATING;
			}
		}
		
		LassoAnalysis laT = null;
		try {
			final boolean overapproximateArrayIndexConnection = true;
			laT = new LassoAnalysis(mSmtManager.getScript(), mSmtManager.getBoogie2Smt(), stemTF, loopTF,
					modifiableGlobalsAtHonda, mAxioms.toArray(new Term[mAxioms.size()]), 
					constructLassoRankerPreferences(withStem, overapproximateArrayIndexConnection, 
					NlaHandling.OVERAPPROXIMATE, AnalysisTechnique.RANKING_FUNCTIONS_SUPPORTING_INVARIANTS), mServices, mStorage);
			mpreprocessingBenchmarks.add(laT.getPreprocessingBenchmark());
		} catch (final TermException e) {
			e.printStackTrace();
			throw new AssertionError("TermException " + e);
		}

		final List<RankingTemplate> rankingFunctionTemplates = new ArrayList<RankingTemplate>();
		rankingFunctionTemplates.add(new AffineTemplate());

		// if (mAllowNonLinearConstraints) {
		// rankingFunctionTemplates.add(new NestedTemplate(1));
		rankingFunctionTemplates.add(new NestedTemplate(2));
		rankingFunctionTemplates.add(new NestedTemplate(3));
		rankingFunctionTemplates.add(new NestedTemplate(4));
		if (mTemplateBenchmarkMode) {
			rankingFunctionTemplates.add(new NestedTemplate(5));
			rankingFunctionTemplates.add(new NestedTemplate(6));
			rankingFunctionTemplates.add(new NestedTemplate(7));
		}

		// rankingFunctionTemplates.add(new MultiphaseTemplate(1));
		rankingFunctionTemplates.add(new MultiphaseTemplate(2));
		rankingFunctionTemplates.add(new MultiphaseTemplate(3));
		rankingFunctionTemplates.add(new MultiphaseTemplate(4));
		if (mTemplateBenchmarkMode) {
			rankingFunctionTemplates.add(new MultiphaseTemplate(5));
			rankingFunctionTemplates.add(new MultiphaseTemplate(6));
			rankingFunctionTemplates.add(new MultiphaseTemplate(7));
		}

		// rankingFunctionTemplates.add(new LexicographicTemplate(1));
		rankingFunctionTemplates.add(new LexicographicTemplate(2));
		rankingFunctionTemplates.add(new LexicographicTemplate(3));
		if (mTemplateBenchmarkMode) {
			rankingFunctionTemplates.add(new LexicographicTemplate(4));
		}

		if (mTemplateBenchmarkMode) {
			rankingFunctionTemplates.add(new PiecewiseTemplate(2));
			rankingFunctionTemplates.add(new PiecewiseTemplate(3));
			rankingFunctionTemplates.add(new PiecewiseTemplate(4));
		}
		// }

		final TerminationArgument termArg = tryTemplatesAndComputePredicates(withStem, laT, rankingFunctionTemplates, stemTF, loopTF);
		assert (nonTermArgument == null || termArg == null) : " terminating and nonterminating";
		if (termArg != null) {
			return SynthesisResult.TERMINATING;
		} else if (nonTermArgument != null) {
			return SynthesisResult.NONTERMINATING;
		} else {
			return SynthesisResult.UNKNOWN;
		}
	}

	/**
	 * @param withStem
	 * @param lrta
	 * @param nonTermArgument
	 * @param rankingFunctionTemplates
	 * @param loopTF 
	 * @return
	 * @throws AssertionError
	 * @throws IOException 
	 */
	private TerminationArgument tryTemplatesAndComputePredicates(final boolean withStem, LassoAnalysis la,
			List<RankingTemplate> rankingFunctionTemplates, TransFormula stemTF, TransFormula loopTF) throws AssertionError, IOException {
		final String hondaProcedure = ((ISLPredicate) mCounterexample.getLoop().getStateAtPosition(0)).getProgramPoint().getProcedure();
		final Set<BoogieVar> modifiableGlobals = mModifiableGlobalVariableManager.getModifiedBoogieVars(hondaProcedure);
		
		TerminationArgument firstTerminationArgument = null;
		for (final RankingTemplate rft : rankingFunctionTemplates) {
			if (!mServices.getProgressMonitorService().continueProcessing()) {
				throw new ToolchainCanceledException(this.getClass());
			}
			TerminationArgument termArg;
			try {
				final TerminationAnalysisSettings settings = constructTASettings();
				termArg = la.tryTemplate(rft, settings);
				final List<TerminationAnalysisBenchmark> benchs = la.getTerminationAnalysisBenchmarks();
				mTerminationAnalysisBenchmarks.addAll(benchs);
				if (mTemplateBenchmarkMode) {
					for (final TerminationAnalysisBenchmark bench : benchs) {
						final IResult benchmarkResult = new BenchmarkResult<>(Activator.PLUGIN_ID, "LassoTerminationAnalysisBenchmarks", bench);
						mServices.getResultService().reportResult(Activator.PLUGIN_ID, benchmarkResult);
					}
				}
			} catch (final SMTLIBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new AssertionError("SMTLIBException " + e);
			} catch (final TermException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new AssertionError("TermException " + e);
			}
			if (termArg != null) {
				assert termArg.getRankingFunction() != null;
				assert termArg.getSupportingInvariants() != null;
				mBspm.computePredicates(!withStem, termArg, mRemoveSuperfluousSupportingInvariants, stemTF, loopTF, modifiableGlobals);
				assert mBspm.providesPredicates();
//				assert areSupportingInvariantsCorrect() : "incorrect supporting invariant with"
//						+ rft.getClass().getSimpleName();
				assert isRankingFunctionCorrect() : "incorrect ranking function with" + rft.getClass().getSimpleName();
				if (!mTemplateBenchmarkMode) {
					return termArg;
				} else {
					if (firstTerminationArgument == null) {
						firstTerminationArgument = termArg;
					}
				}
				mBspm.clearPredicates();
			}
		}
		if (firstTerminationArgument != null) {
			assert firstTerminationArgument.getRankingFunction() != null;
			assert firstTerminationArgument.getSupportingInvariants() != null;
			mBspm.computePredicates(!withStem, firstTerminationArgument, mRemoveSuperfluousSupportingInvariants, stemTF, loopTF, modifiableGlobals);
			assert mBspm.providesPredicates();
			return firstTerminationArgument;
		} else {
			return null;
		}
	}

	// private List<LassoRankerParam> getLassoRankerParameters() {
	// List<LassoRankerParam> lassoRankerParams = new
	// ArrayList<LassoRankerParam>();
	// Preferences pref = new Preferences();
	// pref.numnon_strict_invariants = 2;
	// pref.numstrict_invariants = 0;
	// pref.only_nondecreasing_invariants = false;
	// lassoRankerParams.add(new LassoRankerParam(new AffineTemplate(), pref));
	// return lassoRankerParams;
	// }

	private TransFormula getDummyTF() {
		final Term term = mSmtManager.getScript().term("true");
		final Map<BoogieVar, TermVariable> inVars = new HashMap<BoogieVar, TermVariable>();
		final Map<BoogieVar, TermVariable> outVars = new HashMap<BoogieVar, TermVariable>();
		final Set<TermVariable> auxVars = new HashSet<TermVariable>();
		final Set<TermVariable> branchEncoders = new HashSet<TermVariable>();
		final Infeasibility infeasibility = Infeasibility.UNPROVEABLE;
		final Term closedFormula = term;
		return new TransFormula(term, inVars, outVars, auxVars, branchEncoders, infeasibility, closedFormula);
	}

	// private class LassoRankerParam {
	// private final RankingFunctionTemplate mRankingFunctionTemplate;
	// private final Preferences mPreferences;
	// public LassoRankerParam(RankingFunctionTemplate rankingFunctionTemplate,
	// Preferences preferences) {
	// super();
	// this.mRankingFunctionTemplate = rankingFunctionTemplate;
	// this.mPreferences = preferences;
	// }
	// public RankingFunctionTemplate getRankingFunctionTemplate() {
	// return mRankingFunctionTemplate;
	// }
	// public Preferences getPreferences() {
	// return mPreferences;
	// }
	// }

}
