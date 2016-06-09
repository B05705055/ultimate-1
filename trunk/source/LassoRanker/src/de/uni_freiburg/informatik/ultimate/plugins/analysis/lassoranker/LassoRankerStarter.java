/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2014-2015 Jan Leike (leike@informatik.uni-freiburg.de)
 * Copyright (C) 2014-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE LassoRanker plug-in.
 * 
 * The ULTIMATE LassoRanker plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE LassoRanker plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE LassoRanker plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE LassoRanker plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE LassoRanker plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.analysis.lassoranker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.boogie.BoogieVar;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.core.lib.results.NoResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.NonTerminationArgumentResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.TerminationArgumentResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.TimeoutResultAtElement;
import de.uni_freiburg.informatik.ultimate.core.lib.results.UnsupportedSyntaxResult;
import de.uni_freiburg.informatik.ultimate.core.model.models.ILocation;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceProvider;
import de.uni_freiburg.informatik.ultimate.core.model.results.IResult;
import de.uni_freiburg.informatik.ultimate.core.model.services.IBacktranslationService;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lassoranker.AnalysisType;
import de.uni_freiburg.informatik.ultimate.lassoranker.BacktranslationUtil;
import de.uni_freiburg.informatik.ultimate.lassoranker.LassoAnalysis;
import de.uni_freiburg.informatik.ultimate.lassoranker.LassoRankerPreferences;
import de.uni_freiburg.informatik.ultimate.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationAnalysisSettings;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.SupportingInvariant;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationAnalysisSettings;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.RankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.AffineTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.ComposableTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.ComposedLexicographicTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.LexicographicTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.MultiphaseTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.NestedTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.ParallelTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.PiecewiseTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.RankingTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.RankVar;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Boogie2SMT;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Term2Expression;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer.BinaryStatePredicateManager;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ProgramPoint;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RcfgElement;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootAnnot;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.SequentialComposition;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.preferences.RcfgPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.preferences.RcfgPreferenceInitializer.CodeBlockSize;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.util.RcfgProgramExecution;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;

/**
 * Takes the root node of an RCFG, extracts a lasso and analyzes its termination.
 * 
 * @author Matthias Heizmann, Jan Leike
 */
public class LassoRankerStarter {
	private final ILogger mLogger;
	private static final String s_LassoError = "This is not a lasso program (a lasso program is a program "
			+ "consisting of a stem and a loop transition)";

	private final RootAnnot mRootAnnot;
	private final ProgramPoint mHonda;
	private final NestedWord<CodeBlock> mStem;
	private final NestedWord<CodeBlock> mLoop;
	private SmtManager mSmtManager;
	private final IUltimateServiceProvider mServices;

	public LassoRankerStarter(RootNode rootNode, IUltimateServiceProvider services, IToolchainStorage storage)
			throws IOException {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(Activator.PLUGIN_ID);

		mRootAnnot = rootNode.getRootAnnot();
		// Omit check to enable Stefans BlockEncoding
		// checkRCFGBuilderSettings();
		final LassoRankerPreferences preferences = PreferencesInitializer.getLassoRankerPreferences(mServices);
		mSmtManager = new SmtManager(mRootAnnot.getScript(), mRootAnnot.getBoogie2SMT(),
				mRootAnnot.getModGlobVarManager(), mServices, false, mRootAnnot.getManagedScript());

		AbstractLassoExtractor lassoExtractor;
		final boolean useNewExtraction = true;
		if (useNewExtraction) {
			try {
				lassoExtractor = new LassoExtractorBuchi(mServices, rootNode, mSmtManager, mLogger);
			} catch (final AutomataOperationCanceledException oce) {
				throw new AssertionError("timeout while searching lasso");
				// throw new ToolchainCanceledException(this.getClass());
			} catch (final AutomataLibraryException e) {
				throw new AssertionError(e.toString());
			}
		} else {
			lassoExtractor = new LassoExtractorNaive(rootNode);
		}
		if (!lassoExtractor.wasLassoFound()) {
			reportUnuspportedSyntax(lassoExtractor.getSomeNoneForErrorReport(), s_LassoError);
			mStem = null;
			mLoop = null;
			mHonda = null;
			return;
		}
		mStem = lassoExtractor.getStem();
		mLoop = lassoExtractor.getLoop();
		mHonda = lassoExtractor.getHonda();

		final Script script = mRootAnnot.getScript();

		final TermVariableRenamer tvr = new TermVariableRenamer(script);
		TransFormula stemTF;
		if (mStem == null) {
			stemTF = null;
		} else {
			stemTF = constructTransformula(mStem);
			stemTF = tvr.renameVars(stemTF, "Stem");
		}
		TransFormula loopTf = constructTransformula(mLoop);
		loopTf = tvr.renameVars(loopTf, "Loop");

		final Term[] axioms = mRootAnnot.getBoogie2SMT().getAxioms().toArray(new Term[0]);
		final Set<BoogieVar> modifiableGlobalsAtHonda = mRootAnnot.getModGlobVarManager()
				.getModifiedBoogieVars(mHonda.getProcedure());

		// Construct LassoAnalysis for nontermination
		LassoAnalysis laNT = null;
		try {
			laNT = new LassoAnalysis(script, mRootAnnot.getBoogie2SMT(), stemTF, loopTf, modifiableGlobalsAtHonda,
					axioms, preferences, mServices, storage);
		} catch (final TermException e) {
			reportUnuspportedSyntax(mHonda, e.getMessage());
			return;
		}

		// Try to prove non-termination
		final NonTerminationAnalysisSettings nontermination_settings = PreferencesInitializer
				.getNonTerminationAnalysisSettings(mServices);
		if (nontermination_settings.analysis != AnalysisType.Disabled) {
			try {
				final NonTerminationArgument nta = laNT.checkNonTermination(nontermination_settings);
				if (nta != null) {
					if (!lassoWasOverapproximated().isEmpty()) {
						reportFailBecauseOfOverapproximationResult();
						return;
					}
					reportNonTerminationResult(nta);
					return;
				}
			} catch (final SMTLIBException e) {
				mLogger.error(e);
			} catch (final TermException e) {
				mLogger.error(e);
			}
		}

		final TerminationAnalysisSettings termination_settings = PreferencesInitializer
				.getTerminationAnalysisSettings(mServices);

		// Construct LassoAnalysis for nontermination
		LassoAnalysis laT = null;
		try {
			laT = new LassoAnalysis(script, mRootAnnot.getBoogie2SMT(), stemTF, loopTf, modifiableGlobalsAtHonda,
					axioms, preferences, mServices, storage);
		} catch (final TermException e) {
			reportUnuspportedSyntax(mHonda, e.getMessage());
			return;
		}

		// Get all templates
		RankingTemplate[] templates;
		if (termination_settings.analysis == AnalysisType.Disabled) {
			templates = new RankingTemplate[0];
		} else {
			templates = getTemplates();
		}
		// Do the termination analysis
		for (final RankingTemplate template : templates) {
			if (!mServices.getProgressMonitorService().continueProcessing()) {
				reportTimeoutResult(templates, template);
				// Timeout or abort
				return;
			}
			try {
				final TerminationArgument arg = laT.tryTemplate(template, termination_settings);
				if (arg != null) {
					// try {
					assert isTerminationArgumentCorrect(arg, stemTF, loopTf) : "Incorrect termination argument from"
							+ template.getClass().getSimpleName();
					// } catch (NoClassDefFoundError e) {
					// s_Logger.warn("Could not check validity of " +
					// "termination argument because of " +
					// "missing dependencies.");
					// // Requires: BuchiAutomizer, TraceAbstraction,
					// // NestedWordAutomata
					// }
					reportTerminationResult(arg);
					return;
				}
			} catch (final TermException e) {
				mLogger.error(e);
				throw new AssertionError(e);
			} catch (final SMTLIBException e) {
				mLogger.error(e);
				throw new AssertionError(e);
			}
		}
		reportNoResult(templates);
	}

	private Map<String, ILocation> lassoWasOverapproximated() {
		final Map<String, ILocation> overapproximations = new HashMap<>();
		overapproximations.putAll(RcfgProgramExecution.getOverapproximations(mStem.asList()));
		overapproximations.putAll(RcfgProgramExecution.getOverapproximations(mLoop.asList()));
		return overapproximations;
	}

	public TransFormula constructTransformula(NestedWord<CodeBlock> nw) {
		final Boogie2SMT boogie2smt = mRootAnnot.getBoogie2SMT();
		final ModifiableGlobalVariableManager modGlobVarManager = mRootAnnot.getModGlobVarManager();
		final boolean simplify = true;
		final boolean extPqe = true;
		final boolean tranformToCNF = false;
		final boolean withBranchEncoders = false;
		final List<CodeBlock> codeBlocks = Collections.unmodifiableList(nw.asList());
		return SequentialComposition.getInterproceduralTransFormula(boogie2smt, modGlobVarManager, simplify, extPqe,
				tranformToCNF, withBranchEncoders, mLogger, mServices, codeBlocks);
	}

	/**
	 * Build a list of templates. Add all templates from size 2 up to the given size.
	 * 
	 * @param preferences
	 * @return the templates specified in the preferences
	 */
	private RankingTemplate[] getTemplates() {
		final IPreferenceProvider store = mServices.getPreferenceProvider(Activator.PLUGIN_ID);
		final List<RankingTemplate> templates = new ArrayList<RankingTemplate>();

		if (store.getBoolean(PreferencesInitializer.LABEL_enable_affine_template)) {
			templates.add(new AffineTemplate());
		}
		if (store.getBoolean(PreferencesInitializer.LABEL_enable_nested_template)) {
			final int maxSize = store.getInt(PreferencesInitializer.LABEL_nested_template_size);
			for (int i = 2; i <= maxSize; i++) {
				templates.add(new NestedTemplate(i));
			}
		}
		if (store.getBoolean(PreferencesInitializer.LABEL_enable_multiphase_template)) {
			final int maxSize = store.getInt(PreferencesInitializer.LABEL_multiphase_template_size);
			for (int i = 2; i <= maxSize; i++) {
				templates.add(new MultiphaseTemplate(i));
			}
		}
		if (store.getBoolean(PreferencesInitializer.LABEL_enable_lex_template)) {
			final int maxSize = store.getInt(PreferencesInitializer.LABEL_lex_template_size);
			for (int i = 2; i <= maxSize; i++) {
				final AffineTemplate[] parts = new AffineTemplate[i];
				for (int j = 0; j < i; ++j) {
					parts[j] = new AffineTemplate();
				}
				templates.add(new ComposedLexicographicTemplate(parts));
			}
		}
		if (store.getBoolean(PreferencesInitializer.LABEL_enable_piecewise_template)) {
			final int maxSize = store.getInt(PreferencesInitializer.LABEL_piecewise_template_size);
			for (int i = 2; i <= maxSize; i++) {
				templates.add(new PiecewiseTemplate(i));
			}
		}
		if (store.getBoolean(PreferencesInitializer.LABEL_enable_parallel_template)) {
			final int maxSize = store.getInt(PreferencesInitializer.LABEL_parallel_template_size);
			for (int i = 2; i <= maxSize; i++) {
				templates.add(new ParallelTemplate(i));
			}
		}
		if (store.getBoolean(PreferencesInitializer.LABEL_enable_multilex_template)) {
			final int maxSize = store.getInt(PreferencesInitializer.LABEL_multilex_template_size);
			for (int i = 2; i <= maxSize; i++) {
				final ComposableTemplate[] parts = new ComposableTemplate[i];
				for (int j = 0; j < i; ++j) {
					parts[j] = new MultiphaseTemplate(i);
				}
				templates.add(new ComposedLexicographicTemplate(parts));
			}
		}
		return templates.toArray(new RankingTemplate[0]);
	}

	/**
	 * Build a list of templates. Add all templates with exactly the given size.
	 * 
	 * @param preferences
	 * @return the templates specified in the preferences
	 */
	private RankingTemplate[] getTemplatesExactly() {
		final IPreferenceProvider store = mServices.getPreferenceProvider(Activator.PLUGIN_ID);
		final List<RankingTemplate> templates = new ArrayList<RankingTemplate>();

		if (store.getBoolean(PreferencesInitializer.LABEL_enable_affine_template)) {
			templates.add(new AffineTemplate());
		}
		if (store.getBoolean(PreferencesInitializer.LABEL_enable_nested_template)) {
			templates.add(new NestedTemplate(store.getInt(PreferencesInitializer.LABEL_nested_template_size)));
		}
		if (store.getBoolean(PreferencesInitializer.LABEL_enable_multiphase_template)) {
			templates.add(new MultiphaseTemplate(store.getInt(PreferencesInitializer.LABEL_multiphase_template_size)));
		}
		if (store.getBoolean(PreferencesInitializer.LABEL_enable_lex_template)) {
			templates.add(new LexicographicTemplate(store.getInt(PreferencesInitializer.LABEL_lex_template_size)));
		}
		if (store.getBoolean(PreferencesInitializer.LABEL_enable_piecewise_template)) {
			templates.add(new PiecewiseTemplate(store.getInt(PreferencesInitializer.LABEL_piecewise_template_size)));
		}
		if (store.getBoolean(PreferencesInitializer.LABEL_enable_parallel_template)) {
			templates.add(new ParallelTemplate(store.getInt(PreferencesInitializer.LABEL_parallel_template_size)));
		}
		return templates.toArray(new RankingTemplate[0]);
	}

	private boolean isTerminationArgumentCorrect(TerminationArgument arg, TransFormula stemTF, TransFormula loopTf) {

		final BinaryStatePredicateManager bspm = new BinaryStatePredicateManager(mSmtManager, mServices);
		final Set<BoogieVar> modifiableGlobals = mRootAnnot.getModGlobVarManager()
				.getModifiedBoogieVars(mHonda.getProcedure());
		bspm.computePredicates(false, arg, false, stemTF, loopTf, modifiableGlobals);

		// check supporting invariants
		boolean siCorrect = true;
		for (final SupportingInvariant si : bspm.getTerminationArgument().getSupportingInvariants()) {
			final IPredicate siPred = bspm.supportingInvariant2Predicate(si);
			siCorrect &= bspm.checkSupportingInvariant(siPred, mStem, mLoop, mRootAnnot.getModGlobVarManager());
		}

		// check array index supporting invariants
		for (final Term aisi : bspm.getTerminationArgument().getArrayIndexSupportingInvariants()) {
			final IPredicate siPred = bspm.term2Predicate(aisi);
			siCorrect &= bspm.checkSupportingInvariant(siPred, mStem, mLoop, mRootAnnot.getModGlobVarManager());
		}

		// check ranking function
		final boolean rfCorrect = bspm.checkRankDecrease(mLoop, mRootAnnot.getModGlobVarManager());
		if (siCorrect && rfCorrect) {
			mLogger.info("Termination argument has been successfully verified.");
		}
		return siCorrect && rfCorrect;
	}

	/**
	 * @return the current translator sequence for building results
	 */
	private IBacktranslationService getTranslatorSequence() {
		return mServices.getBacktranslationService();
	}

	/**
	 * Report a termination argument back to Ultimate's toolchain.
	 * 
	 * @param arg
	 *            the termination argument
	 */
	private void reportTerminationResult(TerminationArgument arg) {
		final RankingFunction rf = arg.getRankingFunction();
		final Collection<SupportingInvariant> si_list = arg.getSupportingInvariants();

		final Script script = mRootAnnot.getScript();
		final Term2Expression term2expression = mRootAnnot.getBoogie2SMT().getTerm2Expression();

		final Expression[] supporting_invariants = new Expression[si_list.size()];
		int i = 0;
		for (final SupportingInvariant si : si_list) {
			supporting_invariants[i] = si.asExpression(script, term2expression);
			++i;
		}

		final TerminationArgumentResult<RcfgElement, Expression> result = new TerminationArgumentResult<RcfgElement, Expression>(
				mHonda, Activator.PLUGIN_NAME, rf.asLexExpression(script, term2expression), rf.getName(),
				supporting_invariants, getTranslatorSequence(), Expression.class);
		reportResult(result);
	}

	/**
	 * Report a nontermination argument back to Ultimate's toolchain
	 * 
	 * @param arg
	 */
	private void reportNonTerminationResult(NonTerminationArgument nta) {
		// TODO: translate also the rational coefficients to Expressions?
		// mRootAnnot.getBoogie2Smt().translate(term)
		final Term2Expression term2expression = mRootAnnot.getBoogie2SMT().getTerm2Expression();

		final List<Map<RankVar, Rational>> states = new ArrayList<Map<RankVar, Rational>>();
		states.add(nta.getStateInit());
		states.add(nta.getStateHonda());
		states.addAll(nta.getGEVs());
		final List<Map<Expression, Rational>> initHondaRays = BacktranslationUtil.rank2Boogie(term2expression, states);

		final NonTerminationArgumentResult<RcfgElement, Expression> result = new NonTerminationArgumentResult<RcfgElement, Expression>(
				mHonda, Activator.PLUGIN_NAME, initHondaRays.get(0), initHondaRays.get(1),
				initHondaRays.subList(2, initHondaRays.size()), nta.getLambdas(), nta.getNus(), getTranslatorSequence(),
				Expression.class);
		reportResult(result);
	}

	/**
	 * Report that no result has been found to Ultimate's toolchain
	 * 
	 * @param preferences
	 *            the current preferences
	 */
	private void reportNoResult(RankingTemplate[] templates) {
		final NoResult<RcfgElement> result = new NoResult<RcfgElement>(mHonda, Activator.PLUGIN_NAME,
				getTranslatorSequence());
		result.setShortDescription("LassoRanker could not prove termination");
		final StringBuilder sb = new StringBuilder();
		sb.append(
				"LassoRanker could not prove termination " + "or nontermination of the given linear lasso program.\n");
		sb.append("Templates:");
		for (final RankingTemplate template : templates) {
			sb.append(" ");
			sb.append(template.getClass().getSimpleName());
		}
		result.setLongDescription(sb.toString());
		mLogger.info(sb.toString());
		reportResult(result);
	}

	/**
	 * Report that no result has been found and that the reason might be that the lasso was overapproximated.
	 * 
	 * @param preferences
	 *            the current preferences
	 */
	private void reportFailBecauseOfOverapproximationResult() {
		final NoResult<RcfgElement> result = new NoResult<RcfgElement>(mHonda, Activator.PLUGIN_NAME,
				getTranslatorSequence());
		result.setShortDescription("LassoRanker could not prove termination");
		final StringBuilder sb = new StringBuilder();
		sb.append(
				"LassoRanker could not prove termination " + "or nontermination of the given linear lasso program.\n");
		sb.append("The reason might be that LassoRanker had to use an overapproximation of the original lasso.");
		result.setLongDescription(sb.toString());
		mLogger.info(sb.toString());
		reportResult(result);
	}

	/**
	 * Report that there was a timeout. TODO: which templates already failed, where did the timeout occur.
	 */
	private void reportTimeoutResult(RankingTemplate[] templates, RankingTemplate templateWhereTimeoutOccurred) {
		final StringBuilder sb = new StringBuilder();
		sb.append(
				"LassoRanker could not prove termination " + "or nontermination of the given linear lasso program.\n");
		sb.append("Templates:");
		for (final RankingTemplate template : templates) {
			sb.append(" ");
			sb.append(template.getClass().getSimpleName());
		}
		final TimeoutResultAtElement<RcfgElement> result = new TimeoutResultAtElement<RcfgElement>(mHonda,
				Activator.PLUGIN_NAME, getTranslatorSequence(), sb.toString());
		mLogger.info(sb.toString());
		reportResult(result);
	}

	/**
	 * Report that unsupported syntax was discovered
	 * 
	 * @param position
	 *            the program point
	 * @param message
	 *            an error message explaining the problem
	 */
	private void reportUnuspportedSyntax(RcfgElement position, String message) {
		mLogger.error(message);
		final UnsupportedSyntaxResult<RcfgElement> result = new UnsupportedSyntaxResult<RcfgElement>(position,
				Activator.PLUGIN_NAME, getTranslatorSequence(), message);
		reportResult(result);
	}

	/**
	 * Report a result back to the Ultimate toolchain
	 * 
	 * @param result
	 *            the result
	 */
	private void reportResult(IResult result) {
		mServices.getResultService().reportResult(Activator.PLUGIN_ID, result);
	}

	// FIXME: allow also Stefans BlockEncoding
	private void checkRCFGBuilderSettings() {
		final IPreferenceProvider store = RcfgPreferenceInitializer.getPreferences(mServices);
		final boolean removeGoto = store.getBoolean(RcfgPreferenceInitializer.LABEL_RemoveGotoEdges);
		final CodeBlockSize codeBlockSize = store.getEnum(RcfgPreferenceInitializer.LABEL_CodeBlockSize,
				RcfgPreferenceInitializer.CodeBlockSize.class);
		if (codeBlockSize != CodeBlockSize.LoopFreeBlock) {
			throw new UnsupportedOperationException(
					"Unsupported input: Use the large block encoding of the RCFGBuilder");
		}
		if (!removeGoto) {
			throw new UnsupportedOperationException("Unsupported input: Let RCFGBuilder remove goto edges");
		}
	}

}
