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
import de.uni_freiburg.informatik.ultimate.core.model.results.IResult;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.core.preferences.RcpPreferenceProvider;
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

	private static final boolean m_SimplifyStemAndLoop = true;
	/**
	 * If true we check if the loop is terminating even if the stem or
	 * the concatenation of stem and loop are already infeasible.
	 * This allows us to use refineFinite and refineBuchi in the same
	 * iteration.
	 */
	private final boolean m_TryTwofoldRefinement;

	/**
	 * For debugging only. Check for termination arguments even if we found a
	 * nontermination argument. This may reveal unsoundness bugs.
	 */
	private static final boolean s_CheckTerminationEvenIfNonterminating = false;
	
	
	private static final boolean s_AvoidNonterminationCheckIfArraysAreContained = true;

	private final INTERPOLATION m_Interpolation;

	/**
	 * Use an external solver. If false, we use SMTInterpol.
	 */
	private boolean m_ExternalSolver_RankSynthesis;
	/**
	 * Command of external solver.
	 */
	private final String m_ExternalSolverCommand_RankSynthesis;
	/**
	 * Use an external solver. If false, we use SMTInterpol.
	 */
	private boolean m_ExternalSolver_GntaSynthesis;
	/**
	 * Command of external solver.
	 */
	private final String m_ExternalSolverCommand_GntaSynthesis;
	
	private final AnalysisType m_RankAnalysisType;
	private final AnalysisType m_GntaAnalysisType;
	private final int m_GntaDirections;
	private final boolean m_TrySimplificationTerminationArgument;

	/**
	 * Try all templates but use the one that was found first. This is only
	 * useful to test all templates at once.
	 */
	private final boolean m_TemplateBenchmarkMode;

	// ////////////////////////////// input /////////////////////////////////
	/**
	 * Intermediate layer to encapsulate communication with SMT solvers.
	 */
	private final SmtManager m_SmtManager;

	private final ModifiableGlobalVariableManager m_ModifiableGlobalVariableManager;

	private final BinaryStatePredicateManager m_Bspm;

	/**
	 * Accepting run of the abstraction obtained in this iteration.
	 */
	private final NestedLassoRun<CodeBlock, IPredicate> m_Counterexample;

	/**
	 * Identifier for this LassoChecker. Can be used to get unique filenames
	 * when dumping files.
	 */
	private final String m_LassoCheckerIdentifier;

	// ////////////////////////////// auxilliary variables
	// //////////////////////

	private final IPredicate m_TruePredicate;
	private final IPredicate m_FalsePredicate;

	// ////////////////////////////// output /////////////////////////////////

	// private final BuchiModGlobalVarManager m_BuchiModGlobalVarManager;

	private final PredicateUnifier m_PredicateUnifier;

	private InterpolatingTraceChecker m_StemCheck;
	private InterpolatingTraceChecker m_LoopCheck;
	private InterpolatingTraceChecker m_ConcatCheck;

	private NestedRun<CodeBlock, IPredicate> m_ConcatenatedCounterexample;



	private NonTerminationArgument m_NonterminationArgument;

	Collection<Term> m_Axioms;
	private final IUltimateServiceProvider mServices;
	private final IToolchainStorage mStorage;
	private final boolean m_RemoveSuperfluousSupportingInvariants = true;
	
	private final LassoCheckResult m_LassoCheckResult;
	
	private final List<PreprocessingBenchmark> m_preprocessingBenchmarks = 
			new ArrayList<PreprocessingBenchmark>();
	
	private final List<TerminationAnalysisBenchmark> m_TerminationAnalysisBenchmarks =
			new ArrayList<TerminationAnalysisBenchmark>();
	private final List<NonterminationAnalysisBenchmark> m_NonterminationAnalysisBenchmarks =
			new ArrayList<NonterminationAnalysisBenchmark>();
	
	public LassoCheckResult getLassoCheckResult() {
		return m_LassoCheckResult;
	}

	public InterpolatingTraceChecker getStemCheck() {
		return m_StemCheck;
	}

	public InterpolatingTraceChecker getLoopCheck() {
		return m_LoopCheck;
	}

	public InterpolatingTraceChecker getConcatCheck() {
		return m_ConcatCheck;
	}

	public NestedRun<CodeBlock, IPredicate> getConcatenatedCounterexample() {
		assert m_ConcatenatedCounterexample != null;
		return m_ConcatenatedCounterexample;
	}

	public BinaryStatePredicateManager getBinaryStatePredicateManager() {
		return m_Bspm;
	}

	public NonTerminationArgument getNonTerminationArgument() {
		return m_NonterminationArgument;
	}
	
	public List<PreprocessingBenchmark> getPreprocessingBenchmarks() {
		return m_preprocessingBenchmarks;
	}
	
	public List<TerminationAnalysisBenchmark> getTerminationAnalysisBenchmarks() {
		return m_TerminationAnalysisBenchmarks;
	}
	
	public List<NonterminationAnalysisBenchmark> getNonterminationAnalysisBenchmarks() {
		return m_NonterminationAnalysisBenchmarks;
	}

	public LassoChecker(INTERPOLATION interpolation, SmtManager smtManager,
			ModifiableGlobalVariableManager modifiableGlobalVariableManager, Collection<Term> axioms,
			BinaryStatePredicateManager bspm, NestedLassoRun<CodeBlock, IPredicate> counterexample,
			String lassoCheckerIdentifier, IUltimateServiceProvider services, 
			IToolchainStorage storage) throws IOException {
		mServices = services;
		mStorage = storage;
		mLogger = mServices.getLoggingService().getLogger(Activator.PLUGIN_ID);
		RcpPreferenceProvider baPref = new RcpPreferenceProvider(Activator.PLUGIN_ID);
		m_ExternalSolver_RankSynthesis = baPref.getBoolean(PreferenceInitializer.LABEL_ExtSolverRank);
		m_ExternalSolverCommand_RankSynthesis = baPref.getString(PreferenceInitializer.LABEL_ExtSolverCommandRank);
		m_ExternalSolver_GntaSynthesis = baPref.getBoolean(PreferenceInitializer.LABEL_ExtSolverGNTA);
		m_ExternalSolverCommand_GntaSynthesis = baPref.getString(PreferenceInitializer.LABEL_ExtSolverCommandGNTA);
		m_RankAnalysisType = baPref.getEnum(PreferenceInitializer.LABEL_AnalysisTypeRank, AnalysisType.class);
		m_GntaAnalysisType = baPref.getEnum(PreferenceInitializer.LABEL_AnalysisTypeGNTA, AnalysisType.class);
		m_GntaDirections = baPref.getInt(PreferenceInitializer.LABEL_GntaDirections);
		
		m_TemplateBenchmarkMode = baPref.getBoolean(PreferenceInitializer.LABEL_TemplateBenchmarkMode);
		m_TrySimplificationTerminationArgument = baPref.getBoolean(PreferenceInitializer.LABEL_Simplify);
		m_TryTwofoldRefinement = baPref.getBoolean(PreferenceInitializer.LABEL_TryTwofoldRefinement);
		m_Interpolation = interpolation;
		m_SmtManager = smtManager;
		m_ModifiableGlobalVariableManager = modifiableGlobalVariableManager;
		m_Bspm = bspm;
		m_Counterexample = counterexample;
		m_LassoCheckerIdentifier = lassoCheckerIdentifier;
		m_PredicateUnifier = new PredicateUnifier(mServices, m_SmtManager);
		m_TruePredicate = m_PredicateUnifier.getTruePredicate();
		m_FalsePredicate = m_PredicateUnifier.getFalsePredicate();
		m_Axioms = axioms;
		m_LassoCheckResult = new LassoCheckResult();
		assert m_LassoCheckResult.getStemFeasibility() != TraceCheckResult.UNCHECKED;
		assert (m_LassoCheckResult.getLoopFeasibility() != TraceCheckResult.UNCHECKED)
				|| (m_LassoCheckResult.getLoopFeasibility() != TraceCheckResult.INFEASIBLE && !m_TryTwofoldRefinement);
		if (m_LassoCheckResult.getStemFeasibility() == TraceCheckResult.INFEASIBLE) {
			assert m_LassoCheckResult.getContinueDirective() == ContinueDirective.REFINE_FINITE
					|| m_LassoCheckResult.getContinueDirective() == ContinueDirective.REFINE_BOTH;
		} else {
			if (m_LassoCheckResult.getLoopFeasibility() == TraceCheckResult.INFEASIBLE) {
				assert m_LassoCheckResult.getContinueDirective() == ContinueDirective.REFINE_FINITE;
			} else {
				// loop not infeasible
				if (m_LassoCheckResult.getLoopTermination() == SynthesisResult.TERMINATING) {
					assert m_Bspm.providesPredicates();
				} else {
					assert m_ConcatCheck != null;
					if (m_LassoCheckResult.getConcatFeasibility() == TraceCheckResult.INFEASIBLE) {
						assert m_LassoCheckResult.getContinueDirective() == ContinueDirective.REFINE_FINITE
								|| m_LassoCheckResult.getContinueDirective() == ContinueDirective.REFINE_BOTH;
						assert m_ConcatenatedCounterexample != null;
					} else {
						assert m_LassoCheckResult.getContinueDirective() != ContinueDirective.REFINE_FINITE;
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
		
		private final TraceCheckResult m_StemFeasibility;
		private final TraceCheckResult m_LoopFeasibility;
		private final TraceCheckResult m_ConcatFeasibility;

		private final SynthesisResult m_LoopTermination;
		private final SynthesisResult m_LassoTermination;
		
		private final ContinueDirective m_ContinueDirective;


		public LassoCheckResult() throws IOException {
			NestedRun<CodeBlock, IPredicate> stem = m_Counterexample.getStem();
			mLogger.info("Stem: " + stem);
			NestedRun<CodeBlock, IPredicate> loop = m_Counterexample.getLoop();
			mLogger.info("Loop: " + loop);
			m_StemFeasibility = checkStemFeasibility();
			if (m_StemFeasibility == TraceCheckResult.INFEASIBLE) {
				mLogger.info("stem already infeasible");
				if (!m_TryTwofoldRefinement) {
					m_LoopFeasibility = TraceCheckResult.UNCHECKED;
					m_ConcatFeasibility = TraceCheckResult.UNCHECKED;
					m_LoopTermination = SynthesisResult.UNCHECKED;
					m_LassoTermination = SynthesisResult.UNCHECKED;
					m_ContinueDirective = ContinueDirective.REFINE_FINITE;
					return;
				}
			}
			m_LoopFeasibility = checkLoopFeasibility();
			if (m_LoopFeasibility == TraceCheckResult.INFEASIBLE) {
				mLogger.info("loop already infeasible");
				m_ConcatFeasibility = TraceCheckResult.UNCHECKED;
				m_LoopTermination = SynthesisResult.UNCHECKED;
				m_LassoTermination = SynthesisResult.UNCHECKED;
				m_ContinueDirective = ContinueDirective.REFINE_FINITE;
				return;
			} else {
				if (m_StemFeasibility == TraceCheckResult.INFEASIBLE) {
					assert (m_TryTwofoldRefinement);
					TransFormula loopTF = computeLoopTF();
					m_LoopTermination = checkLoopTermination(loopTF);
					m_ConcatFeasibility = TraceCheckResult.UNCHECKED;
					m_LassoTermination = SynthesisResult.UNCHECKED;
					if (m_LoopTermination == SynthesisResult.TERMINATING) {
						m_ContinueDirective = ContinueDirective.REFINE_BOTH;
						return;
					} else {
						m_ContinueDirective = ContinueDirective.REFINE_FINITE;
						return;
					}
				} else {
					// stem feasible
					m_ConcatFeasibility = checkConcatFeasibility();
					if (m_ConcatFeasibility == TraceCheckResult.INFEASIBLE) {
						m_LassoTermination = SynthesisResult.UNCHECKED;
						if (m_TryTwofoldRefinement) {
							TransFormula loopTF = computeLoopTF();
							m_LoopTermination = checkLoopTermination(loopTF);
							if (m_LoopTermination == SynthesisResult.TERMINATING) {
								m_ContinueDirective = ContinueDirective.REFINE_BOTH;
								return;
							} else {
								m_ContinueDirective = ContinueDirective.REFINE_FINITE;
								return;
							}
						} else {
							m_LoopTermination = SynthesisResult.UNCHECKED;
							m_ContinueDirective = ContinueDirective.REFINE_FINITE;
							return;
						}
					} else {
						// concat feasible
						TransFormula loopTF = computeLoopTF();
						// checking loop termination before we check lasso 
						// termination is a workaround.
						// We want to avoid supporting invariants in possible
						// yet the termination argument simplification of the
						// LassoChecker is not optimal. Hence we first check
						// only the loop, which guarantees that there are no
						// supporting invariants.
						m_LoopTermination = checkLoopTermination(loopTF);
						if (m_LoopTermination == SynthesisResult.TERMINATING) {
							m_LassoTermination = SynthesisResult.UNCHECKED;
							m_ContinueDirective = ContinueDirective.REFINE_BUCHI;
							return;
						} else {
							TransFormula stemTF = computeStemTF();
							m_LassoTermination = checkLassoTermination(stemTF, loopTF);
							if (m_LassoTermination == SynthesisResult.TERMINATING) {
								m_ContinueDirective = ContinueDirective.REFINE_BUCHI;
								return;
							} else if (m_LassoTermination == SynthesisResult.NONTERMINATING) {
								m_ContinueDirective = ContinueDirective.REPORT_NONTERMINATION;
								return;
							} else {
								m_ContinueDirective = ContinueDirective.REPORT_UNKNOWN;
								return;
							}
						}
					}
				}
			}
		}

		private TraceCheckResult checkStemFeasibility() {
			NestedRun<CodeBlock, IPredicate> stem = m_Counterexample.getStem();
			if (BuchiCegarLoop.emptyStem(m_Counterexample)) {
				return TraceCheckResult.FEASIBLE;
			} else {
				m_StemCheck = checkFeasibilityAndComputeInterpolants(stem);
				return translateSatisfiabilityToFeasibility(m_StemCheck.isCorrect());
			}
		}

		private TraceCheckResult checkLoopFeasibility() {
			NestedRun<CodeBlock, IPredicate> loop = m_Counterexample.getLoop();
			m_LoopCheck = checkFeasibilityAndComputeInterpolants(loop);
			return translateSatisfiabilityToFeasibility(m_LoopCheck.isCorrect());
		}

		private TraceCheckResult checkConcatFeasibility() {
			NestedRun<CodeBlock, IPredicate> stem = m_Counterexample.getStem();
			NestedRun<CodeBlock, IPredicate> loop = m_Counterexample.getLoop();
			NestedRun<CodeBlock, IPredicate> concat = stem.concatenate(loop);
			m_ConcatCheck = checkFeasibilityAndComputeInterpolants(concat);
			if (m_ConcatCheck.isCorrect() == LBool.UNSAT) {
				m_ConcatenatedCounterexample = concat;
			}
			return translateSatisfiabilityToFeasibility(m_ConcatCheck.isCorrect());
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
			switch (m_Interpolation) {
			case Craig_NestedInterpolation:
			case Craig_TreeInterpolation:
				result = new InterpolatingTraceCheckerCraig(m_TruePredicate, m_FalsePredicate, new TreeMap<Integer, IPredicate>(),
						run.getWord(), m_SmtManager, m_ModifiableGlobalVariableManager,
						/*
						 * TODO: When Matthias
						 * introduced this parameter he
						 * set the argument to AssertCodeBlockOrder.NOT_INCREMENTALLY.
						 * Check if you want to set this
						 * to a different value.
						 */AssertCodeBlockOrder.NOT_INCREMENTALLY, mServices, false, m_PredicateUnifier, m_Interpolation, true);
				break;
			case ForwardPredicates:
			case BackwardPredicates:
			case FPandBP:
				result = new TraceCheckerSpWp(m_TruePredicate, m_FalsePredicate, new TreeMap<Integer, IPredicate>(),
						run.getWord(), m_SmtManager, m_ModifiableGlobalVariableManager,
						/*
						 * TODO: When Matthias
						 * introduced this parameter he
						 * set the argument to AssertCodeBlockOrder.NOT_INCREMENTALLY.
						 * Check if you want to set this
						 * to a different value.
						 */AssertCodeBlockOrder.NOT_INCREMENTALLY, 
						 UnsatCores.CONJUNCT_LEVEL, true, mServices, false, m_PredicateUnifier, m_Interpolation, m_SmtManager);
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
			assert !m_Bspm.providesPredicates() : "termination already checked";
			boolean containsArrays = SmtUtils.containsArrayVariables(loopTF.getFormula());
			if (containsArrays) {
				// if there are array variables we will probably run in a huge
				// DNF, so as a precaution we do not check and say unknown
				return SynthesisResult.UNKNOWN;
			} else {
				return synthesize(false, null, loopTF, containsArrays);
			}
		}

		private SynthesisResult checkLassoTermination(TransFormula stemTF, TransFormula loopTF) throws IOException {
			assert !m_Bspm.providesPredicates() : "termination already checked";
			assert loopTF != null;
			boolean containsArrays = SmtUtils.containsArrayVariables(stemTF.getFormula())
					|| SmtUtils.containsArrayVariables(loopTF.getFormula());
			return synthesize(true, stemTF, loopTF, containsArrays);
		}

		public TraceCheckResult getStemFeasibility() {
			return m_StemFeasibility;
		}

		public TraceCheckResult getLoopFeasibility() {
			return m_LoopFeasibility;
		}

		public TraceCheckResult getConcatFeasibility() {
			return m_ConcatFeasibility;
		}

		public SynthesisResult getLoopTermination() {
			return m_LoopTermination;
		}

		public SynthesisResult getLassoTermination() {
			return m_LassoTermination;
		}

		public ContinueDirective getContinueDirective() {
			return m_ContinueDirective;
		}
	
	}

	/**
	 * Compute TransFormula that represents the stem.
	 */
	protected TransFormula computeStemTF() {
		NestedWord<CodeBlock> stem = m_Counterexample.getStem().getWord();
		try {
			TransFormula stemTF = computeTF(stem, m_SimplifyStemAndLoop, true, false);
			return stemTF;
		} catch (ToolchainCanceledException tce) {
			throw new ToolchainCanceledException(getClass(), 
					tce.getRunningTaskInfo() + " while constructing stem TransFormula");
		}
	}

	/**
	 * Compute TransFormula that represents the loop.
	 */
	protected TransFormula computeLoopTF() {
		NestedWord<CodeBlock> loop = m_Counterexample.getLoop().getWord();
		try {
			TransFormula loopTF = computeTF(loop, m_SimplifyStemAndLoop, true, false);
			return loopTF;
		} catch (ToolchainCanceledException tce) {
			throw new ToolchainCanceledException(getClass(), 
					tce.getRunningTaskInfo() + " while constructing loop TransFormula");
		}
	}

	/**
	 * Compute TransFormula that represents the NestedWord word.
	 */
	private TransFormula computeTF(NestedWord<CodeBlock> word, boolean simplify,
			boolean extendedPartialQuantifierElimination, boolean withBranchEncoders) {
		boolean toCNF = false;
		TransFormula tf = SequentialComposition.getInterproceduralTransFormula(m_SmtManager.getBoogie2Smt(),
				m_ModifiableGlobalVariableManager, simplify, extendedPartialQuantifierElimination, toCNF,
				withBranchEncoders, mLogger, mServices, word.asList());
		return tf;
	}

	private boolean areSupportingInvariantsCorrect() {
		NestedWord<CodeBlock> stem = m_Counterexample.getStem().getWord();
		mLogger.info("Stem: " + stem);
		NestedWord<CodeBlock> loop = m_Counterexample.getLoop().getWord();
		mLogger.info("Loop: " + loop);
		boolean siCorrect = true;
		if (stem.length() == 0) {
			// do nothing
			// TODO: check that si is equivalent to true
		} else {
			for (SupportingInvariant si : m_Bspm.getTerminationArgument().getSupportingInvariants()) {
				IPredicate siPred = m_Bspm.supportingInvariant2Predicate(si);
				siCorrect &= m_Bspm.checkSupportingInvariant(siPred, stem, loop, m_ModifiableGlobalVariableManager);
			}
			// check array index supporting invariants
			for (Term aisi : m_Bspm.getTerminationArgument().getArrayIndexSupportingInvariants()) {
				IPredicate siPred = m_Bspm.term2Predicate(aisi);
				siCorrect &= m_Bspm.checkSupportingInvariant(siPred, stem, loop, m_ModifiableGlobalVariableManager);
			}
		}
		return siCorrect;
	}

	private boolean isRankingFunctionCorrect() {
		NestedWord<CodeBlock> loop = m_Counterexample.getLoop().getWord();
		mLogger.info("Loop: " + loop);
		boolean rfCorrect = m_Bspm.checkRankDecrease(loop, m_ModifiableGlobalVariableManager);
		return rfCorrect;
	}

	private String generateFileBasenamePrefix(boolean withStem) {
		return m_LassoCheckerIdentifier + "_" + (withStem ? "Lasso" : "Loop");
	}

	private LassoRankerPreferences constructLassoRankerPreferences(boolean withStem,
			boolean overapproximateArrayIndexConnection, NlaHandling nlaHandling, 
			AnalysisTechnique analysis) {
		LassoRankerPreferences pref = new LassoRankerPreferences();
		switch (analysis) {
		case GEOMETRIC_NONTERMINATION_ARGUMENTS: {
			pref.externalSolver = m_ExternalSolver_GntaSynthesis;
			pref.smt_solver_command = m_ExternalSolverCommand_GntaSynthesis;
			break;
		}
		case RANKING_FUNCTIONS_SUPPORTING_INVARIANTS: {
			pref.externalSolver = m_ExternalSolver_RankSynthesis;
			pref.smt_solver_command = m_ExternalSolverCommand_RankSynthesis;
			break;
		}
		default:
			throw new AssertionError();
		}
		RcpPreferenceProvider baPref = new RcpPreferenceProvider(Activator.PLUGIN_ID);
		pref.dumpSmtSolverScript = baPref.getBoolean(PreferenceInitializer.LABEL_DumpToFile);
		pref.path_of_dumped_script = baPref.getString(PreferenceInitializer.LABEL_DumpPath);
		pref.baseNameOfDumpedScript = generateFileBasenamePrefix(withStem);
		pref.overapproximateArrayIndexConnection = overapproximateArrayIndexConnection;
		pref.nlaHandling = nlaHandling;
		return pref;
	}

	private TerminationAnalysisSettings constructTASettings() {
		TerminationAnalysisSettings settings = new TerminationAnalysisSettings();
		settings.analysis = m_RankAnalysisType;
		settings.num_non_strict_invariants = 1;
		settings.num_strict_invariants = 0;
		settings.nondecreasing_invariants = true;
		settings.simplify_termination_argument = m_TrySimplificationTerminationArgument;
		settings.simplify_supporting_invariants = m_TrySimplificationTerminationArgument;
		return settings;
	}

	private NonTerminationAnalysisSettings constructNTASettings() {
		NonTerminationAnalysisSettings settings = new NonTerminationAnalysisSettings();
		settings.analysis = m_GntaAnalysisType;
		settings.number_of_gevs = m_GntaDirections;
		return settings;
	}

	private SynthesisResult synthesize(final boolean withStem, TransFormula stemTF, final TransFormula loopTF,
			boolean containsArrays) throws IOException {
		if (m_SmtManager.isLocked()) {
			throw new AssertionError("SMTManager must not be locked at the beginning of synthesis");
		}
		
		Set<BoogieVar> modifiableGlobalsAtHonda = m_ModifiableGlobalVariableManager.getModifiedBoogieVars(
				((ISLPredicate) m_Counterexample.getLoop().getStateAtPosition(0)).getProgramPoint().getProcedure());

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
		
		boolean doNonterminationAnalysis = !(s_AvoidNonterminationCheckIfArraysAreContained && containsArrays);
		
		NonTerminationArgument nonTermArgument = null;
		if (doNonterminationAnalysis) {
			LassoAnalysis laNT = null;
			try {
				boolean overapproximateArrayIndexConnection = false;
				laNT = new LassoAnalysis(m_SmtManager.getScript(), m_SmtManager.getBoogie2Smt(), stemTF, loopTF,
						modifiableGlobalsAtHonda, m_Axioms.toArray(new Term[m_Axioms.size()]), 
						constructLassoRankerPreferences(withStem, overapproximateArrayIndexConnection, 
						NlaHandling.UNDERAPPROXIMATE, AnalysisTechnique.GEOMETRIC_NONTERMINATION_ARGUMENTS), mServices, mStorage);
				m_preprocessingBenchmarks.add(laNT.getPreprocessingBenchmark());
			} catch (TermException e) {
				e.printStackTrace();
				throw new AssertionError("TermException " + e);
			}
			try {
				NonTerminationAnalysisSettings settings = constructNTASettings();
				nonTermArgument = laNT.checkNonTermination(settings);
				List<NonterminationAnalysisBenchmark> benchs = laNT.getNonterminationAnalysisBenchmarks();
				m_NonterminationAnalysisBenchmarks.addAll(benchs);
			} catch (SMTLIBException e) {
				e.printStackTrace();
				throw new AssertionError("SMTLIBException " + e);
			} catch (TermException e) {
				e.printStackTrace();
				throw new AssertionError("TermException " + e);
			}
			if (withStem) {
				m_NonterminationArgument = nonTermArgument;
			}
			if (!s_CheckTerminationEvenIfNonterminating && nonTermArgument != null) {
				return SynthesisResult.NONTERMINATING;
			}
		}
		
		LassoAnalysis laT = null;
		try {
			boolean overapproximateArrayIndexConnection = true;
			laT = new LassoAnalysis(m_SmtManager.getScript(), m_SmtManager.getBoogie2Smt(), stemTF, loopTF,
					modifiableGlobalsAtHonda, m_Axioms.toArray(new Term[m_Axioms.size()]), 
					constructLassoRankerPreferences(withStem, overapproximateArrayIndexConnection, 
					NlaHandling.OVERAPPROXIMATE, AnalysisTechnique.RANKING_FUNCTIONS_SUPPORTING_INVARIANTS), mServices, mStorage);
			m_preprocessingBenchmarks.add(laT.getPreprocessingBenchmark());
		} catch (TermException e) {
			e.printStackTrace();
			throw new AssertionError("TermException " + e);
		}

		List<RankingTemplate> rankingFunctionTemplates = new ArrayList<RankingTemplate>();
		rankingFunctionTemplates.add(new AffineTemplate());

		// if (m_AllowNonLinearConstraints) {
		// rankingFunctionTemplates.add(new NestedTemplate(1));
		rankingFunctionTemplates.add(new NestedTemplate(2));
		rankingFunctionTemplates.add(new NestedTemplate(3));
		rankingFunctionTemplates.add(new NestedTemplate(4));
		if (m_TemplateBenchmarkMode) {
			rankingFunctionTemplates.add(new NestedTemplate(5));
			rankingFunctionTemplates.add(new NestedTemplate(6));
			rankingFunctionTemplates.add(new NestedTemplate(7));
		}

		// rankingFunctionTemplates.add(new MultiphaseTemplate(1));
		rankingFunctionTemplates.add(new MultiphaseTemplate(2));
		rankingFunctionTemplates.add(new MultiphaseTemplate(3));
		rankingFunctionTemplates.add(new MultiphaseTemplate(4));
		if (m_TemplateBenchmarkMode) {
			rankingFunctionTemplates.add(new MultiphaseTemplate(5));
			rankingFunctionTemplates.add(new MultiphaseTemplate(6));
			rankingFunctionTemplates.add(new MultiphaseTemplate(7));
		}

		// rankingFunctionTemplates.add(new LexicographicTemplate(1));
		rankingFunctionTemplates.add(new LexicographicTemplate(2));
		rankingFunctionTemplates.add(new LexicographicTemplate(3));
		if (m_TemplateBenchmarkMode) {
			rankingFunctionTemplates.add(new LexicographicTemplate(4));
		}

		if (m_TemplateBenchmarkMode) {
			rankingFunctionTemplates.add(new PiecewiseTemplate(2));
			rankingFunctionTemplates.add(new PiecewiseTemplate(3));
			rankingFunctionTemplates.add(new PiecewiseTemplate(4));
		}
		// }

		TerminationArgument termArg = tryTemplatesAndComputePredicates(withStem, laT, rankingFunctionTemplates, stemTF, loopTF);
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
		String hondaProcedure = ((ISLPredicate) m_Counterexample.getLoop().getStateAtPosition(0)).getProgramPoint().getProcedure();
		Set<BoogieVar> modifiableGlobals = m_ModifiableGlobalVariableManager.getModifiedBoogieVars(hondaProcedure);
		
		TerminationArgument firstTerminationArgument = null;
		for (RankingTemplate rft : rankingFunctionTemplates) {
			if (!mServices.getProgressMonitorService().continueProcessing()) {
				throw new ToolchainCanceledException(this.getClass());
			}
			TerminationArgument termArg;
			try {
				TerminationAnalysisSettings settings = constructTASettings();
				termArg = la.tryTemplate(rft, settings);
				List<TerminationAnalysisBenchmark> benchs = la.getTerminationAnalysisBenchmarks();
				m_TerminationAnalysisBenchmarks.addAll(benchs);
				if (m_TemplateBenchmarkMode) {
					for (TerminationAnalysisBenchmark bench : benchs) {
						IResult benchmarkResult = new BenchmarkResult<>(Activator.PLUGIN_ID, "LassoTerminationAnalysisBenchmarks", bench);
						mServices.getResultService().reportResult(Activator.PLUGIN_ID, benchmarkResult);
					}
				}
			} catch (SMTLIBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new AssertionError("SMTLIBException " + e);
			} catch (TermException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new AssertionError("TermException " + e);
			}
			if (termArg != null) {
				assert termArg.getRankingFunction() != null;
				assert termArg.getSupportingInvariants() != null;
				m_Bspm.computePredicates(!withStem, termArg, m_RemoveSuperfluousSupportingInvariants, stemTF, loopTF, modifiableGlobals);
				assert m_Bspm.providesPredicates();
//				assert areSupportingInvariantsCorrect() : "incorrect supporting invariant with"
//						+ rft.getClass().getSimpleName();
				assert isRankingFunctionCorrect() : "incorrect ranking function with" + rft.getClass().getSimpleName();
				if (!m_TemplateBenchmarkMode) {
					return termArg;
				} else {
					if (firstTerminationArgument == null) {
						firstTerminationArgument = termArg;
					}
				}
				m_Bspm.clearPredicates();
			}
		}
		if (firstTerminationArgument != null) {
			assert firstTerminationArgument.getRankingFunction() != null;
			assert firstTerminationArgument.getSupportingInvariants() != null;
			m_Bspm.computePredicates(!withStem, firstTerminationArgument, m_RemoveSuperfluousSupportingInvariants, stemTF, loopTF, modifiableGlobals);
			assert m_Bspm.providesPredicates();
			return firstTerminationArgument;
		} else {
			return null;
		}
	}

	// private List<LassoRankerParam> getLassoRankerParameters() {
	// List<LassoRankerParam> lassoRankerParams = new
	// ArrayList<LassoRankerParam>();
	// Preferences pref = new Preferences();
	// pref.num_non_strict_invariants = 2;
	// pref.num_strict_invariants = 0;
	// pref.only_nondecreasing_invariants = false;
	// lassoRankerParams.add(new LassoRankerParam(new AffineTemplate(), pref));
	// return lassoRankerParams;
	// }

	private TransFormula getDummyTF() {
		Term term = m_SmtManager.getScript().term("true");
		Map<BoogieVar, TermVariable> inVars = new HashMap<BoogieVar, TermVariable>();
		Map<BoogieVar, TermVariable> outVars = new HashMap<BoogieVar, TermVariable>();
		Set<TermVariable> auxVars = new HashSet<TermVariable>();
		Set<TermVariable> branchEncoders = new HashSet<TermVariable>();
		Infeasibility infeasibility = Infeasibility.UNPROVEABLE;
		Term closedFormula = term;
		return new TransFormula(term, inVars, outVars, auxVars, branchEncoders, infeasibility, closedFormula);
	}

	// private class LassoRankerParam {
	// private final RankingFunctionTemplate m_RankingFunctionTemplate;
	// private final Preferences m_Preferences;
	// public LassoRankerParam(RankingFunctionTemplate rankingFunctionTemplate,
	// Preferences preferences) {
	// super();
	// this.m_RankingFunctionTemplate = rankingFunctionTemplate;
	// this.m_Preferences = preferences;
	// }
	// public RankingFunctionTemplate getRankingFunctionTemplate() {
	// return m_RankingFunctionTemplate;
	// }
	// public Preferences getPreferences() {
	// return m_Preferences;
	// }
	// }

}
