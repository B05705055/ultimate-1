package de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.uni_freiburg.informatik.ultimate.access.IUnmanagedObserver;
import de.uni_freiburg.informatik.ultimate.access.WalkerOptions;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.buchiNwa.NestedLassoRun;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceStore;
import de.uni_freiburg.informatik.ultimate.core.services.IBacktranslationService;
import de.uni_freiburg.informatik.ultimate.core.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.RankVar;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.model.GraphType;
import de.uni_freiburg.informatik.ultimate.model.IElement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Term2Expression;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer.BuchiCegarLoop.Result;
import de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer.preferences.PreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.RcfgProgramExecution;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ProgramPoint;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RcfgElement;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootAnnot;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.ISLPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TAPreferences;
import de.uni_freiburg.informatik.ultimate.result.AllSpecificationsHoldResult;
import de.uni_freiburg.informatik.ultimate.result.BenchmarkResult;
import de.uni_freiburg.informatik.ultimate.result.IProgramExecution.ProgramState;
import de.uni_freiburg.informatik.ultimate.result.IResult;
import de.uni_freiburg.informatik.ultimate.result.LTLFiniteCounterExampleResult;
import de.uni_freiburg.informatik.ultimate.result.LTLInfiniteCounterExampleResult;
import de.uni_freiburg.informatik.ultimate.result.LTLPropertyCheck;
import de.uni_freiburg.informatik.ultimate.result.NonTerminationArgumentResult;
import de.uni_freiburg.informatik.ultimate.result.NonterminatingLassoResult;
import de.uni_freiburg.informatik.ultimate.result.TerminationAnalysisResult;
import de.uni_freiburg.informatik.ultimate.result.TerminationAnalysisResult.TERMINATION;
import de.uni_freiburg.informatik.ultimate.result.TimeoutResultAtElement;
import de.uni_freiburg.informatik.ultimate.util.csv.ICsvProvider;
import de.uni_freiburg.informatik.ultimate.util.csv.ICsvProviderProvider;
import de.uni_freiburg.informatik.ultimate.util.csv.SimpleCsvProvider;

/**
 * Auto-Generated Stub for the plug-in's Observer
 */
public class BuchiAutomizerObserver implements IUnmanagedObserver {

	private IElement mGraphRoot;
	private SmtManager mSmtManager;
	private TAPreferences mPref;

	private RootAnnot mRootAnnot;
	private final IUltimateServiceProvider mServices;
	private final IToolchainStorage mStorage;

	public BuchiAutomizerObserver(IUltimateServiceProvider services, IToolchainStorage storage) {
		mServices = services;
		mStorage = storage;
	}

	@Override
	public boolean process(IElement root) throws IOException {
		if (!(root instanceof RootNode)) {
			return false;
		}
		mRootAnnot = ((RootNode) root).getRootAnnot();
		TAPreferences taPrefs = new TAPreferences();
		mGraphRoot = root;

		mSmtManager = new SmtManager(mRootAnnot.getBoogie2SMT(), mRootAnnot.getModGlobVarManager(), mServices);

		mPref = taPrefs;

		BuchiCegarLoop bcl = new BuchiCegarLoop((RootNode) root, mSmtManager, mPref, mServices, mStorage);
		Result result = bcl.iterate();
		BuchiCegarLoopBenchmarkGenerator benchGen = bcl.getBenchmarkGenerator();
		benchGen.stop(BuchiCegarLoopBenchmark.s_OverallTime);

		IResult benchDecomp = new BenchmarkResult<String>(Activator.s_PLUGIN_ID,
				"Constructed decomposition of program", bcl.getMDBenchmark());
		reportResult(benchDecomp);

		boolean constructTermcompProof = (new UltimatePreferenceStore(Activator.s_PLUGIN_ID))
				.getBoolean(PreferenceInitializer.LABEL_TermcompProof);
		if (constructTermcompProof) {
			IResult termcompProof = new BenchmarkResult<Double>(Activator.s_PLUGIN_ID,
					"Constructed termination proof in form of nested word automata", bcl.getTermcompProofBenchmark());
			reportResult(termcompProof);
		}

		BuchiAutomizerTimingBenchmark timingBenchmark = new BuchiAutomizerTimingBenchmark(benchGen);
		IResult benchTiming = new BenchmarkResult<>(Activator.s_PLUGIN_ID, "Timing statistics", timingBenchmark);
		reportResult(benchTiming);

		interpretAndReportResult(bcl, result);
		return false;
	}

	/**
	 * Report a nontermination argument back to Ultimate's toolchain
	 * 
	 * @param arg
	 */
	private void reportNonTerminationResult(ProgramPoint honda, NonTerminationArgument nta) {
		// TODO: translate also the rational coefficients to Expressions?
		// m_RootAnnot.getBoogie2Smt().translate(term)
		Term2Expression term2expression = mRootAnnot.getBoogie2SMT().getTerm2Expression();
		
		List<Map<RankVar, Rational>> states =
				new ArrayList<Map<RankVar, Rational>>();
		states.add(nta.getStateInit());
		states.add(nta.getStateHonda());
		states.addAll(nta.getRays());
		List<Map<Expression, Rational>> initHondaRays =
				NonTerminationArgument.rank2Boogie(term2expression, states);
		
		NonTerminationArgumentResult<RcfgElement> result =
				new NonTerminationArgumentResult<RcfgElement>(honda,
				Activator.s_PLUGIN_NAME, initHondaRays.get(0),
				initHondaRays.get(1),
				initHondaRays.subList(2, initHondaRays.size() - 2),
				nta.getLambdas(),
				getBacktranslationService());
		reportResult(result);
	}
	
	private void interpretAndReportResult(BuchiCegarLoop bcl, Result result) throws AssertionError {
		String whatToProve = "termination";

		if (bcl.isInLTLMode()) {
			LTLPropertyCheck ltlAnnot = LTLPropertyCheck.getAnnotation(mGraphRoot);
			switch (result) {
			case NONTERMINATING:
				// there is a violation of the LTL property
				reportLTLPropertyIsViolated(bcl, ltlAnnot);
				return;
			case TERMINATING:
				// the LTL property holds
				reportLTLPropertyHolds(bcl, ltlAnnot);
				return;
			case TIMEOUT:
			case UNKNOWN:
				// use the standard report, but say its the LTL property
				whatToProve = ltlAnnot.getLTLProperty();
				break;
			default:
				throw new AssertionError("Extend the switch with the new enum member " + result);
			}
		}

		if (result == Result.TERMINATING) {
			String longDescr = "Buchi Automizer proved that your program is terminating";
			IResult reportRes = new TerminationAnalysisResult(Activator.s_PLUGIN_ID, TERMINATION.TERMINATING, longDescr);
			reportResult(reportRes);
		} else if (result == Result.UNKNOWN) {
			NestedLassoRun<CodeBlock, IPredicate> counterexample = bcl.getCounterexample();
			StringBuilder longDescr = new StringBuilder();
			longDescr.append("Buchi Automizer is unable to decide " + whatToProve + " for the following lasso. ");
			longDescr.append(System.getProperty("line.separator"));
			longDescr.append("Stem: ");
			longDescr.append(counterexample.getStem().getWord());
			longDescr.append(System.getProperty("line.separator"));
			longDescr.append("Loop: ");
			longDescr.append(counterexample.getLoop().getWord());
			IResult reportRes = new TerminationAnalysisResult(Activator.s_PLUGIN_ID, TERMINATION.UNKNOWN,
					longDescr.toString());
			reportResult(reportRes);
		} else if (result == Result.TIMEOUT) {
			ProgramPoint position = mRootAnnot.getEntryNodes().values().iterator().next();
			String longDescr = "Timeout while trying to prove " + whatToProve 
					+ ". (Timeout occurred in class " + bcl.getToolchainCancelledException().getClassOfThrower().getSimpleName() +
					(bcl.getToolchainCancelledException().getRunningTaskInfo() != null ? " during the following task: " 
					+ bcl.getToolchainCancelledException().getRunningTaskInfo() : "") + ")";
			IResult reportRes = new TimeoutResultAtElement<RcfgElement>(position, Activator.s_PLUGIN_ID,
					mServices.getBacktranslationService(), longDescr);
			reportResult(reportRes);
		} else if (result == Result.NONTERMINATING) {
			String longDescr = "Buchi Automizer proved that your program is nonterminating for some inputs";
			IResult reportRes = new TerminationAnalysisResult(Activator.s_PLUGIN_ID, TERMINATION.NONTERMINATING,
					longDescr);
			reportResult(reportRes);

			NestedLassoRun<CodeBlock, IPredicate> counterexample = bcl.getCounterexample();
			IPredicate hondaPredicate = counterexample.getLoop().getStateAtPosition(0);
			ProgramPoint honda = ((ISLPredicate) hondaPredicate).getProgramPoint();
			NonTerminationArgument nta = bcl.getNonTerminationArgument();
			reportNonTerminationResult(honda, nta);
			reportResult(new BenchmarkResult<String>(Activator.s_PLUGIN_NAME, "NonterminationBenchmark", new NonterminationBenchmark(nta)));

			Map<Integer, ProgramState<Expression>> partialProgramStateMapping = Collections.emptyMap();
			@SuppressWarnings("unchecked")
			RcfgProgramExecution stemPE = new RcfgProgramExecution(counterexample.getStem().getWord().lettersAsList(),
					partialProgramStateMapping, new Map[counterexample.getStem().getLength()]);
			@SuppressWarnings("unchecked")
			RcfgProgramExecution loopPE = new RcfgProgramExecution(counterexample.getLoop().getWord().lettersAsList(),
					partialProgramStateMapping, new Map[counterexample.getLoop().getLength()]);
			IResult ntreportRes = new NonterminatingLassoResult<RcfgElement, CodeBlock, Expression>(honda,
					Activator.s_PLUGIN_ID, mServices.getBacktranslationService(), stemPE, loopPE, honda.getPayload()
							.getLocation());
			reportResult(ntreportRes);
		} else {
			throw new AssertionError();
		}
	}

	private void reportLTLPropertyHolds(BuchiCegarLoop bcl, LTLPropertyCheck ltlAnnot) {
		IResult result = new AllSpecificationsHoldResult(Activator.s_PLUGIN_ID,
				"Buchi Automizer proved that the LTL property " + ltlAnnot.getLTLProperty() + " holds");
		reportResult(result);
	}

	private void reportLTLPropertyIsViolated(BuchiCegarLoop bcl, LTLPropertyCheck ltlAnnot) {
		NestedLassoRun<CodeBlock, IPredicate> counterexample = bcl.getCounterexample();
		ProgramPoint position = ((ISLPredicate) counterexample.getLoop().getStateAtPosition(0)).getProgramPoint();
		// first, check if the counter example is really infinite or not

		List<CodeBlock> stem = counterexample.getStem().getWord().lettersAsList();
		List<CodeBlock> loop = counterexample.getLoop().getWord().lettersAsList();

		boolean isFinite = isLTLCounterExampleFinite(loop);

		if (isFinite) {
			// TODO: Make some attempt at getting the values
			Map<Integer, ProgramState<Expression>> partialProgramStateMapping = Collections.emptyMap();
			List<CodeBlock> combined = new ArrayList<CodeBlock>();
			combined.addAll(stem);

			// TODO: It seems that the loop is not necessary if the trace is
			// finite, but only contains things that have been inserted by
			// BuchiProgramProduct
			// combined.addAll(loop);

			@SuppressWarnings("unchecked")
			RcfgProgramExecution cex = new RcfgProgramExecution(combined, partialProgramStateMapping,
					new Map[combined.size()]);
			reportResult(new LTLFiniteCounterExampleResult<>(position, Activator.s_PLUGIN_ID,
					mServices.getBacktranslationService(), cex, ltlAnnot));
		} else {
			// TODO: Make some attempt at getting the values
			Map<Integer, ProgramState<Expression>> partialProgramStateMapping = Collections.emptyMap();

			@SuppressWarnings("unchecked")
			RcfgProgramExecution stemPE = new RcfgProgramExecution(stem, partialProgramStateMapping,
					new Map[stem.size()]);
			@SuppressWarnings("unchecked")
			RcfgProgramExecution loopPE = new RcfgProgramExecution(loop, partialProgramStateMapping,
					new Map[loop.size()]);
			reportResult(new LTLInfiniteCounterExampleResult<>(position, Activator.s_PLUGIN_ID,
					mServices.getBacktranslationService(), stemPE, loopPE, position.getPayload().getLocation(),
					ltlAnnot.getLTLProperty()));
		}
	}

	private boolean isLTLCounterExampleFinite(List<CodeBlock> loop) {
		// TODO: does not work reliably
		// boolean isFinite = true;
		//
		// // is the loop a real loop in the program?
		// for (CodeBlock cb : loop) {
		// if (mRootAnnot.getLoopLocations().keySet().contains(cb.getSource()))
		// {
		// isFinite = false;
		// }
		// }
		//
		// // TODO: is the loop part of a recursion? Then its also not finite
		//
		// return isFinite;
		return false;
	}

	/**
	 * @return the current translator sequence for building results
	 */
	private IBacktranslationService getBacktranslationService() {
		return mServices.getBacktranslationService();
	}

	void reportResult(IResult res) {
		mServices.getResultService().reportResult(Activator.s_PLUGIN_ID, res);
	}

	@Override
	public void finish() {

	}

	@Override
	public WalkerOptions getWalkerOptions() {
		return null;
	}

	@Override
	public void init(GraphType modelType, int currentModelIndex, int numberOfModels) {

	}

	@Override
	public boolean performedChanges() {
		return false;
	}

	public IElement getModel() {
		return mGraphRoot;
	}

	// public static TransFormula sequentialComposition(int serialNumber,
	// Boogie2SMT boogie2smt, TransFormula... transFormulas) {
	// TransFormula result = transFormulas[0];
	// for (int i=1; i<transFormulas.length; i++) {
	// result = TransFormula.sequentialComposition(result, transFormulas[i],
	// boogie2smt, serialNumber++);
	// }
	// return result;
	// }
	
	private class NonterminationBenchmark implements ICsvProviderProvider<String> {
		private final String m_Ntar;
		private final boolean m_LambdaZero;
		private final boolean m_RayZero;
		
		public NonterminationBenchmark(NonTerminationArgument nta) {
			boolean lambdaZero = true;
			boolean rayZero = true;
			List<Rational> lambdas = nta.getLambdas();
			for (int i = 0; i < nta.getNumberOfRays(); ++i) {
				lambdaZero &= (nta.getLambdas().get(i).numerator().equals(BigInteger.ZERO));
				rayZero &= isZero(nta.getRays().get(i));
			}
			
			m_LambdaZero = lambdaZero;
			m_RayZero = rayZero;
			m_Ntar = (isFixpoint() ? "Fixpoint " : "Unbounded Execution ") +
					"Lambdas: " + lambdas + 
					" Ray: " + (m_RayZero ? "is zero" : "is not zero");
		}
		
		private boolean isFixpoint() {
			return m_LambdaZero || m_RayZero;
		}

		/**
		 * Return true iff all coefficients of ray are zero.
		 */
		private boolean isZero(Map<RankVar, Rational> ray) {
			for (Entry<RankVar, Rational> entry : ray.entrySet()) {
				if (!entry.getValue().numerator().equals(BigInteger.ZERO)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public ICsvProvider<String> createCvsProvider() {
			return new SimpleCsvProvider<>(Arrays.asList(new String[] {"nta"}));
		}

		@Override
		public String toString() {
			return m_Ntar;
		}
		
	}

}
