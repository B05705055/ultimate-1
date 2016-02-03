/*
 * Copyright (C) 2013-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2010-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.automata.AutomatonDefinitionPrinter;
import de.uni_freiburg.informatik.ultimate.automata.AutomatonDefinitionPrinter.Format;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.IAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.IRun;
import de.uni_freiburg.informatik.ultimate.automata.OperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedRun;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.core.services.model.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.services.model.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.FormulaUnLet;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.model.IElement;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.RcfgProgramExecution;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ProgramPoint;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.ISLPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.IncrementalHoareTripleChecker;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.InductivityCheck;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TAPreferences;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TAPreferences.Artifact;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.IInterpolantGenerator;
import de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException;

/**
 * CEGAR loop of a trace abstraction. Can be used to check safety and
 * termination of sequential and concurrent programs. Defines roughly the
 * structure of the CEGAR loop, concrete algorithms are implemented in classes
 * which extend this one.
 * 
 * @author heizmann@informatik.uni-freiburg.de
 * 
 */
public abstract class AbstractCegarLoop {

	protected final Logger mLogger;

	/**
	 * Result of CEGAR loop iteration
	 * <ul>
	 * <li>SAFE: there is no feasible trace to an error location
	 * <li>UNSAFE: there is a feasible trace to an error location (the
	 * underlying program has at least one execution which violates its
	 * specification)
	 * <li>UNKNOWN: we found a trace for which we could not decide feasibility
	 * or we found an infeasible trace but were not able to exclude it in
	 * abstraction refinement.
	 * <li>TIMEOUT:
	 */
	public enum Result {
		SAFE, UNSAFE, TIMEOUT, UNKNOWN
	}

	/**
	 * Unique m_Name of this CEGAR loop to distinguish this instance from other
	 * instances in a complex verification task. Important only for debugging
	 * and debugging output written to files.
	 */
	private final String m_Name;

	/**
	 * Node of a recursive control flow graph which stores additional
	 * information about the program.
	 */
	protected final RootNode m_RootNode;

	/**
	 * Intermediate layer to encapsulate communication with SMT solvers.
	 */
	protected final SmtManager m_SmtManager;

	protected final ModifiableGlobalVariableManager m_ModGlobVarManager;

	/**
	 * Intermediate layer to encapsulate preferences.
	 */
	protected final TAPreferences m_Pref;

	/**
	 * Set of error location whose reachability is analyzed by this CEGAR loop.
	 */
	protected final Collection<ProgramPoint> m_ErrorLocs;

	/**
	 * Current Iteration of this CEGAR loop.
	 */
	protected int m_Iteration = 0;

	/**
	 * Accepting run of the abstraction obtained in this iteration.
	 */
	protected IRun<CodeBlock, IPredicate> m_Counterexample;

	/**
	 * Abstraction of this iteration. The language of m_Abstraction is a set of
	 * traces which is
	 * <ul>
	 * <li>a superset of the feasible program traces.
	 * <li>a subset of the traces which respect the control flow of the program.
	 */
	protected IAutomaton<CodeBlock, IPredicate> m_Abstraction;

	/**
	 * IInterpolantGenerator that was used in the current iteration.
	 */
	protected IInterpolantGenerator m_InterpolantGenerator;

	/**
	 * Interpolant automaton of this iteration.
	 */
	protected NestedWordAutomaton<CodeBlock, IPredicate> m_InterpolAutomaton;

	/**
	 * Program execution that leads to error. Only computed in the last
	 * iteration of the CEGAR loop if the program is incorrect.
	 */
	protected RcfgProgramExecution m_RcfgProgramExecution;

	// used for the collection of statistics
	public int m_InitialAbstractionSize = 0;
	public int m_NumberOfErrorLocations = 0;

	// used for debugging only
	protected IAutomaton<CodeBlock, IPredicate> m_ArtifactAutomaton;
	protected PrintWriter m_IterationPW;
	protected final Format m_PrintAutomataLabeling;

	protected CegarLoopBenchmarkGenerator m_CegarLoopBenchmark;

	protected final IUltimateServiceProvider m_Services;
	//protected final IToolchainStorage m_ToolchainStorage = null; TODO: this is not what we want, is it?
	protected final IToolchainStorage m_ToolchainStorage;

	
	private ToolchainCanceledException m_ToolchainCancelledException;
	
	public ToolchainCanceledException getToolchainCancelledException() {
		return m_ToolchainCancelledException;
	}
	
	public AbstractCegarLoop(IUltimateServiceProvider services, IToolchainStorage storage, String name, RootNode rootNode, SmtManager smtManager,
			TAPreferences taPrefs, Collection<ProgramPoint> errorLocs, Logger logger) {
		m_Services = services;
		mLogger = logger;
		this.m_PrintAutomataLabeling = taPrefs.getAutomataFormat();
		m_ModGlobVarManager = rootNode.getRootAnnot().getModGlobVarManager();
		this.m_Name = name;
		this.m_RootNode = rootNode;
		this.m_SmtManager = smtManager;
		this.m_Pref = taPrefs;
		this.m_ErrorLocs = errorLocs;
		this.m_ToolchainStorage = storage;
		
	}

	/**
	 * Construct the automaton m_Abstraction such that the language recognized
	 * by m_Abstation is a superset of the language of the program. The initial
	 * abstraction in our implementations will usually be an automaton that has
	 * the same graph as the program.
	 * 
	 * @throws AutomataLibraryException
	 */
	protected abstract void getInitialAbstraction() throws OperationCanceledException, AutomataLibraryException;

	/**
	 * Return true iff the m_Abstraction does not accept any trace.
	 * 
	 * @throws OperationCanceledException
	 */
	protected abstract boolean isAbstractionCorrect() throws OperationCanceledException;

	/**
	 * Determine if the trace of m_Counterexample is a feasible sequence of
	 * CodeBlocks. Return
	 * <ul>
	 * <li>SAT if the trace is feasible
	 * <li>UNSAT if the trace is infeasible
	 * <li>UNKNOWN if the algorithm was not able to determine the feasibility.
	 * </ul>
	 */
	protected abstract LBool isCounterexampleFeasible();

	/**
	 * Construct an automaton m_InterpolantAutomaton which
	 * <ul>
	 * <li>accepts the trace of m_Counterexample,
	 * <li>accepts only infeasible traces.
	 * </ul>
	 * 
	 * @throws OperationCanceledException
	 */
	protected abstract void constructInterpolantAutomaton() throws OperationCanceledException;

	/**
	 * Construct a new automaton m_Abstraction such that
	 * <ul>
	 * <li>the language of the new m_Abstraction is (not necessary strictly)
	 * smaller than the language of the old m_Abstraction
	 * <li>the new m_Abstraction accepts all feasible traces of the old
	 * m_Abstraction (only infeasible traces are removed)
	 * <ul>
	 * 
	 * @return true iff the trace of m_Counterexample (which was accepted by the
	 *         old m_Abstraction) is not accepted by the m_Abstraction.
	 * @throws AutomataLibraryException
	 */
	protected abstract boolean refineAbstraction() throws OperationCanceledException, AutomataLibraryException;

	/**
	 * Add Hoare annotation to the control flow graph. Use the information
	 * computed so far annotate the ProgramPoints of the control flow graph with
	 * invariants.
	 */
	protected abstract void computeCFGHoareAnnotation();

	/**
	 * Return the Artifact whose computation was requested. This artifact can be
	 * either the control flow graph, an abstraction, an interpolant automaton,
	 * or a negated interpolant automaton. The artifact is only used for
	 * debugging.
	 * 
	 * @return The root node of the artifact after it was transformed to an
	 *         ULTIMATE model.
	 */
	public abstract IElement getArtifact();

	public int getIteration() {
		return m_Iteration;
	}

	public RcfgProgramExecution getRcfgProgramExecution() {
		return m_RcfgProgramExecution;
	}

	public SmtManager getSmtManager() {
		return m_SmtManager;
	}

	public String errorLocs() {
		return m_ErrorLocs.toString();
	}

	public final Result iterate() {
		mLogger.info("Interprodecural is " + m_Pref.interprocedural());
		mLogger.info("Hoare is " + m_Pref.computeHoareAnnotation());
		mLogger.info("Compute interpolants for " + m_Pref.interpolation());
		mLogger.info("Backedges is " + m_Pref.interpolantAutomaton());
		mLogger.info("Determinization is " + m_Pref.interpolantAutomatonEnhancement());
		mLogger.info("Difference is " + m_Pref.differenceSenwa());
		mLogger.info("Minimize is " + m_Pref.minimize());

		m_Iteration = 0;
		mLogger.info("======== Iteration " + m_Iteration + "==of CEGAR loop == " + m_Name + "========");

		// intialize dump of debugging output to files if necessary
		if (m_Pref.dumpAutomata()) {
			dumpInitinalize();
		}
		try {
			getInitialAbstraction();
		} catch (OperationCanceledException e1) {
			mLogger.warn("Verification cancelled");
			m_CegarLoopBenchmark.setResult(Result.TIMEOUT);
			return Result.TIMEOUT;
		} catch (AutomataLibraryException e) {
			throw new AssertionError(e.getMessage());
		}

		if (m_Iteration <= m_Pref.watchIteration()
				&& (m_Pref.artifact() == Artifact.ABSTRACTION || m_Pref.artifact() == Artifact.RCFG)) {
			m_ArtifactAutomaton = m_Abstraction;
		}
		if (m_Pref.dumpAutomata()) {
			String filename = m_Name + "Abstraction" + m_Iteration;
			writeAutomatonToFile(m_Abstraction, filename);
		}
		m_InitialAbstractionSize = m_Abstraction.size();
		m_CegarLoopBenchmark.reportAbstractionSize(m_Abstraction.size(), m_Iteration);
		m_NumberOfErrorLocations = m_ErrorLocs.size();

		boolean initalAbstractionCorrect;
		try {
			initalAbstractionCorrect = isAbstractionCorrect();
		} catch (OperationCanceledException e1) {
			mLogger.warn("Verification cancelled");
			m_CegarLoopBenchmark.setResult(Result.TIMEOUT);
			return Result.TIMEOUT;
		}
		if (initalAbstractionCorrect) {
			m_CegarLoopBenchmark.setResult(Result.SAFE);
			return Result.SAFE;
		}

		for (m_Iteration = 1; m_Iteration <= m_Pref.maxIterations(); m_Iteration++) {
			mLogger.info("=== Iteration " + m_Iteration + " === "+ errorLocs() + "===");
			m_SmtManager.setIteration(m_Iteration);
			m_CegarLoopBenchmark.announceNextIteration();
			if (m_Pref.dumpAutomata()) {
				dumpInitinalize();
			}
			try {
				LBool isCounterexampleFeasible = isCounterexampleFeasible();
				if (isCounterexampleFeasible == Script.LBool.SAT) {
					m_CegarLoopBenchmark.setResult(Result.UNSAFE);
					return Result.UNSAFE;
				}
				if (isCounterexampleFeasible == Script.LBool.UNKNOWN) {
					m_CegarLoopBenchmark.setResult(Result.UNKNOWN);
					return Result.UNKNOWN;
				}
			} catch (ToolchainCanceledException e) {
				m_ToolchainCancelledException = e;
				mLogger.warn("Verification cancelled");
				m_CegarLoopBenchmark.setResult(Result.TIMEOUT);
				return Result.TIMEOUT;
			}

			try {
				constructInterpolantAutomaton();
			} catch (OperationCanceledException e1) {
				mLogger.warn("Verification cancelled");
				m_CegarLoopBenchmark.setResult(Result.TIMEOUT);
				return Result.TIMEOUT;
			} catch (ToolchainCanceledException e) {
				m_ToolchainCancelledException = e;
				mLogger.warn("Verification cancelled");
				m_CegarLoopBenchmark.setResult(Result.TIMEOUT);
				return Result.TIMEOUT;
			}

			mLogger.info("Interpolant Automaton has " + m_InterpolAutomaton.getStates().size() + " states");

			if (m_Iteration <= m_Pref.watchIteration() && m_Pref.artifact() == Artifact.INTERPOLANT_AUTOMATON) {
				m_ArtifactAutomaton = m_InterpolAutomaton;
			}
			if (m_Pref.dumpAutomata()) {
				writeAutomatonToFile(m_InterpolAutomaton, "InterpolantAutomaton_Iteration" + m_Iteration);
			}

			try {
				boolean progress = refineAbstraction();
				if (!progress) {
					mLogger.warn("No progress! Counterexample is still accepted by refined abstraction.");
					throw new AssertionError("No progress! Counterexample is still accepted by refined abstraction.");
					// return Result.UNKNOWN;
				}
			} catch (OperationCanceledException e) {
				mLogger.warn("Verification cancelled");
				m_CegarLoopBenchmark.setResult(Result.TIMEOUT);
				return Result.TIMEOUT;
			} catch (ToolchainCanceledException e) {
				m_ToolchainCancelledException = e;
				mLogger.warn("Verification cancelled");
				m_CegarLoopBenchmark.setResult(Result.TIMEOUT);
				return Result.TIMEOUT;
			} catch (AutomataLibraryException e) {
				throw new AssertionError("Automata Operation failed" + e.getMessage());
			}

			mLogger.info("Abstraction has " + m_Abstraction.sizeInformation());
			mLogger.info("Interpolant automaton has " + m_InterpolAutomaton.sizeInformation());

			if (m_Pref.computeHoareAnnotation()) {
				assert (new InductivityCheck(m_Services, (INestedWordAutomaton) m_Abstraction,
						false, true, new IncrementalHoareTripleChecker(m_SmtManager, m_ModGlobVarManager))).getResult() : "Not inductive";
			}

			if (m_Iteration <= m_Pref.watchIteration() && m_Pref.artifact() == Artifact.ABSTRACTION) {
				m_ArtifactAutomaton = m_Abstraction;
			}

			if (m_Pref.dumpAutomata()) {
				String filename = "Abstraction" + m_Iteration;
				writeAutomatonToFile(m_Abstraction, filename);
			}

			m_CegarLoopBenchmark.reportAbstractionSize(m_Abstraction.size(), m_Iteration);

			boolean isAbstractionCorrect;
			try {
				isAbstractionCorrect = isAbstractionCorrect();
			} catch (OperationCanceledException e) {
				mLogger.warn("Verification cancelled");
				m_CegarLoopBenchmark.setResult(Result.TIMEOUT);
				return Result.TIMEOUT;
			}
			if (isAbstractionCorrect) {
				m_CegarLoopBenchmark.setResult(Result.SAFE);
				return Result.SAFE;
			}
		}
		m_CegarLoopBenchmark.setResult(Result.TIMEOUT);
		return Result.TIMEOUT;
	}

	protected void writeAutomatonToFile(IAutomaton<CodeBlock, IPredicate> automaton, String filename) {
		new AutomatonDefinitionPrinter<String, String>(new AutomataLibraryServices(m_Services), filename, m_Pref.dumpPath() + "/" + filename, m_PrintAutomataLabeling,
				"", automaton);
	}

	private void dumpInitinalize() {
		File file = new File(m_Pref.dumpPath() + "/" + m_Name + "_iteration" + m_Iteration + ".txt");
		FileWriter fileWriter;
		try {
			fileWriter = new FileWriter(file);
			m_IterationPW = new PrintWriter(fileWriter);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * TODO unify sequential and concurrent
	 */
	protected static void dumpNestedRun(IRun<CodeBlock, IPredicate> run, PrintWriter pW,Logger logger) {
		NestedWord<CodeBlock> counterexample = NestedWord.nestedWord(run.getWord());
		ArrayList<IPredicate> stateSequence = null;
		if (run instanceof NestedRun) {
			stateSequence = ((NestedRun) run).getStateSequence();
		}
		String line;
		int indentation = 0;
		try {
			line = "===============Run of potential Counterexample==========";
			pW.println(line);
			for (int i = 0; i < counterexample.length(); i++) {

				if (run instanceof NestedRun) {
					line = addIndentation(indentation,
							"Location" + i + ": " + ((ISLPredicate) stateSequence.get(i)).getProgramPoint());
					logger.debug(line);
					pW.println(line);
				}

				if (counterexample.isCallPosition(i)) {
					indentation++;
				}
				line = addIndentation(indentation, "Statement" + i + ": "
						+ counterexample.getSymbolAt(i).getPrettyPrintedStatements());
				logger.debug(line);
				pW.println(line);
				if (counterexample.isReturnPosition(i)) {
					indentation--;
				}
			}
			pW.println("ErrorLocation");
			pW.println("");
			pW.println("");
		} finally {
			pW.flush();
		}
	}

	private void dumpSsa(Term[] ssa) {
		FormulaUnLet unflet = new FormulaUnLet();
		try {
			m_IterationPW.println("===============SSA of potential Counterexample==========");
			for (int i = 0; i < ssa.length; i++) {
				m_IterationPW.println("UnFletedTerm" + i + ": " + unflet.unlet(ssa[i]));
			}
			m_IterationPW.println("");
			m_IterationPW.println("");
		} finally {
			m_IterationPW.flush();
		}
	}

	private void dumpStateFormulas(IPredicate[] interpolants) {
		try {
			m_IterationPW.println("===============Interpolated StateFormulas==========");
			for (int i = 0; i < interpolants.length; i++) {
				m_IterationPW.println("Interpolant" + i + ": " + interpolants[i]);
			}
			m_IterationPW.println("");
			m_IterationPW.println("");
		} finally {
			m_IterationPW.flush();
		}
	}

	public static String addIndentation(int indentation, String s) {
		StringBuilder sb = new StringBuilder("");
		for (int i = 0; i < indentation; i++) {
			sb.append("    ");
		}
		sb.append(s);
		return sb.toString();
	}

	static void dumpBackedges(ProgramPoint repLocName, int position, IPredicate state,
			Collection<IPredicate> linPredStates, CodeBlock transition, IPredicate succState, IPredicate sf1,
			IPredicate sf2, LBool result, int iteration, int satProblem, PrintWriter iterationPW) {
		try {
			iterationPW.println(repLocName + " occured once again at position " + position + ". Added backedge");
			iterationPW.println("from:   " + state);
			iterationPW.println("labeled with:   " + transition.getPrettyPrintedStatements());
			iterationPW.println("to:   " + succState);
			if (linPredStates != null) {
				iterationPW.println("for each linPredStates:   " + linPredStates);
			}
			if (satProblem == -1) {
				iterationPW.println("because ");
			} else {
				assert (result == Script.LBool.UNSAT);
				iterationPW.println("because Iteration" + iteration + "_SatProblem" + satProblem + " says:");
			}
			iterationPW.println("  " + sf1);
			iterationPW.println("implies");
			iterationPW.println("  " + sf2);
			iterationPW.println("");
		} finally {
			iterationPW.flush();
		}
	}

}
