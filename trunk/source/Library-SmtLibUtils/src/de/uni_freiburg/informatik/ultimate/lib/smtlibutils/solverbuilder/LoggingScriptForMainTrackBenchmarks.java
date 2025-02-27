/*
 * Copyright (C) 2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 University of Freiburg
 *
 * This file is part of the ULTIMATE ModelCheckerUtils Library.
 *
 * The ULTIMATE ModelCheckerUtils Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE ModelCheckerUtils Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE ModelCheckerUtils Library. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE ModelCheckerUtils Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE ModelCheckerUtils Library grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.lib.smtlibutils.solverbuilder;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.SmtSortUtils;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.TermClassifier;
import de.uni_freiburg.informatik.ultimate.logic.AnnotatedTerm;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.QuotedObject;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.smtsolver.external.SmtCommandUtils.AssertCommand;
import de.uni_freiburg.informatik.ultimate.smtsolver.external.SmtCommandUtils.CheckSatCommand;
import de.uni_freiburg.informatik.ultimate.smtsolver.external.SmtCommandUtils.DeclareFunCommand;
import de.uni_freiburg.informatik.ultimate.smtsolver.external.SmtCommandUtils.ExitCommand;
import de.uni_freiburg.informatik.ultimate.smtsolver.external.SmtCommandUtils.ISmtCommand;
import de.uni_freiburg.informatik.ultimate.smtsolver.external.SmtCommandUtils.SetInfoCommand;
import de.uni_freiburg.informatik.ultimate.smtsolver.external.SmtCommandUtils.SetLogicCommand;

public class LoggingScriptForMainTrackBenchmarks extends LoggingScriptForNonIncrementalBenchmarks {
	public static final String SOURCE_INVSYNTH = "(set-info :source |" + System.lineSeparator()
			+ "SMT script generated by Ultimate Automizer [1,2]." + System.lineSeparator()
			+ "Ultimate Automizer is a software verifier for C programs that implements an" + System.lineSeparator()
			+ "automata-based approach [3]." + System.lineSeparator()
			+ "The commands in this SMT scripts are used for a constraint-based synthesis" + System.lineSeparator()
			+ "of invariants [4]." + System.lineSeparator() + "" + System.lineSeparator()
			+ "2016-04-30, Matthias Heizmann (heizmann@informatik.uni-freiburg.de)" + System.lineSeparator() + ""
			+ System.lineSeparator() + "" + System.lineSeparator()
			+ "[1] http://http://ultimate.informatik.uni-freiburg.de/automizer/" + System.lineSeparator()
			+ "[2] Matthias Heizmann, Daniel Dietsch, Marius Greitschus, Jan Leike," + System.lineSeparator()
			+ "Betim Musa, Claus Schätzle, Andreas Podelski: Ultimate Automizer with" + System.lineSeparator()
			+ "Two-track Proofs - (Competition Contribution). TACAS 2016: 950-953" + System.lineSeparator()
			+ "[3] Matthias Heizmann, Jochen Hoenicke, Andreas Podelski: Software Model" + System.lineSeparator()
			+ "Checking for People Who Love Automata. CAV 2013:36-52" + System.lineSeparator()
			+ "[4] Michael Colon, Sriram Sankaranarayanan, Henny Sipma: Linear Invariant" + System.lineSeparator()
			+ "Generation Using Non-linear Constraint Solving. CAV 2003: 420-432" + System.lineSeparator() + ""
			+ System.lineSeparator() + "|)" + System.lineSeparator();

	public static final String SOURCE_GNTA = "(set-info :source |" + System.lineSeparator() + ""
			+ System.lineSeparator() + "SMT script generated by Ultimate LassoRanker [1]." + System.lineSeparator()
			+ "Ultimate LassoRanker is a tool that analyzes termination and nontermination of" + System.lineSeparator()
			+ "lasso-shaped programs. This script contains the SMT commands that Ultimate " + System.lineSeparator()
			+ "LassoRanker used while checking if a lasso-shaped program has a geometric " + System.lineSeparator()
			+ "nontermination argument. (See [2] for a preliminary definition of" + System.lineSeparator()
			+ "geometric nontermination argument.)" + System.lineSeparator() + "" + System.lineSeparator()
			+ "This SMT script belongs to a set of SMT scripts that was generated by applying" + System.lineSeparator()
			+ "Ultimate Buchi Automizer [3,4] to benchmarks from the SV-COMP 2016 [5,6] " + System.lineSeparator()
			+ "which are available at [7]. Ultimate Buchi Automizer takes omega-traces" + System.lineSeparator()
			+ "(lasso-shaped programs) and uses LassoRanker in order to check if the " + System.lineSeparator()
			+ "lasso-shaped program is terminating." + System.lineSeparator() + "" + System.lineSeparator()
			+ "2016-04-30, Matthias Heizmann (heizmann@informatik.uni-freiburg.de)" + System.lineSeparator() + ""
			+ System.lineSeparator() + "" + System.lineSeparator()
			+ "[1] https://ultimate.informatik.uni-freiburg.de/LassoRanker/" + System.lineSeparator()
			+ "[2] Jan Leike, Matthias Heizmann: Geometric Series as Nontermination" + System.lineSeparator()
			+ "Arguments for Linear Lasso Programs. CoRR abs/1405.4413 (2014)" + System.lineSeparator()
			+ "http://arxiv.org/abs/1405.4413" + System.lineSeparator()
			+ "[3] http://ultimate.informatik.uni-freiburg.de/BuchiAutomizer/" + System.lineSeparator()
			+ "[4] Matthias Heizmann, Jochen Hoenicke, Andreas Podelski: Software Model" + System.lineSeparator()
			+ "Checking for People Who Love Automata. CAV 2013:36-52" + System.lineSeparator()
			+ "[5] http://sv-comp.sosy-lab.org/2016/" + System.lineSeparator()
			+ "[6] Dirk Beyer: Reliable and Reproducible Competition Results with BenchExec" + System.lineSeparator()
			+ "and Witnesses (Report on SV-COMP 2016). TACAS 2016: 887-904" + System.lineSeparator()
			+ "[7] https://github.com/dbeyer/sv-benchmarks" + System.lineSeparator() + "" + System.lineSeparator()
			+ "|)" + System.lineSeparator();

	// public static final String SOURCE_AUTOMIZER =
	// "(set-info :source |" + System.lineSeparator() +
	// "" + System.lineSeparator() +
	// "SMT script generated by Ultimate Automizer [1,2]." + System.lineSeparator() +
	// "Ultimate Automizer is a software verifier for C programs that implements an" + System.lineSeparator() +
	// "automata-based approach [3]." + System.lineSeparator() +
	// "This SMT script belongs to a set of SMT scripts that was generated by applying" + System.lineSeparator() +
	// "Ultimate Automizer to benchmarks from the SV-COMP 2016 [4,5] which are" + System.lineSeparator() +
	// "available at [6]. " + System.lineSeparator() +
	// "" + System.lineSeparator() +
	// "May 2016, Matthias Heizmann (heizmann@informatik.uni-freiburg.de)" + System.lineSeparator() +
	// "" + System.lineSeparator() +
	// "" + System.lineSeparator() +
	// "[1] http://http://ultimate.informatik.uni-freiburg.de/automizer/" + System.lineSeparator() +
	// "[2] Matthias Heizmann, Daniel Dietsch, Marius Greitschus, Jan Leike, " + System.lineSeparator() +
	// "Betim Musa, Claus Schätzle, Andreas Podelski: Ultimate Automizer with" + System.lineSeparator() +
	// "Two-track Proofs - (Competition Contribution). TACAS 2016: 950-953" + System.lineSeparator() +
	// "[3] Matthias Heizmann, Jochen Hoenicke, Andreas Podelski: Software Model" + System.lineSeparator() +
	// "Checking for People Who Love Automata. CAV 2013:36-52" + System.lineSeparator() +
	// "[4] http://sv-comp.sosy-lab.org/2016/" + System.lineSeparator() +
	// "[5] Dirk Beyer: Reliable and Reproducible Competition Results with BenchExec" + System.lineSeparator() +
	// "and Witnesses (Report on SV-COMP 2016). TACAS 2016: 887-904" + System.lineSeparator() +
	// "[6] https://github.com/dbeyer/sv-benchmarks" + System.lineSeparator() +
	// "" + System.lineSeparator() +
	// "|)" + System.lineSeparator();

	public static final String SOURCE_AUTOMIZER = "|" + System.lineSeparator()
			+ "Generated by the tool Ultimate Automizer [1,2] which implements" + System.lineSeparator()
			+ "an automata theoretic approach [3] to software verification." + System.lineSeparator() + ""
			+ System.lineSeparator() + "This SMT script belongs to a set of SMT scripts that was generated by"
			+ System.lineSeparator() + "applying Ultimate Automizer to benchmarks [4] from the SV-COMP 2019 [5,6]."
			+ System.lineSeparator() +

			"This script might _not_ contain all SMT commands that are used by" + System.lineSeparator()
			+ "Ultimate Automizer. In order to satisfy the restrictions of" + System.lineSeparator()
			+ "the SMT-COMP we have to drop e.g., the commands for getting" + System.lineSeparator()
			+ "values (resp. models), unsatisfiable cores and interpolants." + System.lineSeparator() + ""
			+ System.lineSeparator() + "2019-04-27, Matthias Heizmann (heizmann@informatik.uni-freiburg.de)"
			+ System.lineSeparator() + "" + System.lineSeparator()
			+ "[1] https://ultimate.informatik.uni-freiburg.de/automizer/" + System.lineSeparator()
			+ "[2] Matthias Heizmann, Yu-Fang Chen, Daniel Dietsch, Marius Greitschus," + System.lineSeparator()
			+ "     Jochen Hoenicke, Yong Li, Alexander Nutz, Betim Musa, Christian" + System.lineSeparator()
			+ "     Schilling, Tanja Schindler, Andreas Podelski: Ultimate Automizer" + System.lineSeparator()
			+ "     and the Search for Perfect Interpolants - (Competition Contribution)." + System.lineSeparator()
			+ "     TACAS (2) 2018: 447-451" + System.lineSeparator()
			+ "[3] Matthias Heizmann, Jochen Hoenicke, Andreas Podelski: Software Model" + System.lineSeparator()
			+ "     Checking for People Who Love Automata. CAV 2013:36-52" + System.lineSeparator()
			+ "[4] https://github.com/sosy-lab/sv-benchmarks" + System.lineSeparator()
			+ "[5] Dirk Beyer: Automatic Verification of C and Java Programs: SV-COMP 2019." + System.lineSeparator()
			+ "     TACAS (3) 2019: 133-155" + System.lineSeparator() + "[6] https://sv-comp.sosy-lab.org/2019/"
			+ System.lineSeparator() + "|";

	public static final String SOURCE_POLYNOMIAL_RELATION_TEST = "|" + System.lineSeparator()
			+ "Generated by a testsuite for polynomials of the Ultimate framework [1]." + System.lineSeparator()
			+ "This testsuite runs transformations on polynomials and uses an SMT solver" + System.lineSeparator()
			+ "to check that the input and the output are logically equivalent." + System.lineSeparator()
			+ "These transformations are mainly used by the quantifier elimination " + System.lineSeparator()
			+ "implemented in Ultimate Eliminator [2] which itself is used by " + System.lineSeparator()
			+ "the software verifier Ultimate Automizer[3,4,5] to generate state " + System.lineSeparator()
			+ "assertions from unsatisfiable cores [6]." + System.lineSeparator() + "" + System.lineSeparator()
			+ "2020-06-14, Matthias Heizmann (heizmann@informatik.uni-freiburg.de)" + System.lineSeparator() + ""
			+ System.lineSeparator() + "[1] https://ultimate.informatik.uni-freiburg.de/" + System.lineSeparator()
			+ "[2] https://ultimate.informatik.uni-freiburg.de/eliminator/" + System.lineSeparator()
			+ "[3] https://ultimate.informatik.uni-freiburg.de/automizer/" + System.lineSeparator()
			+ "[4] Matthias Heizmann, Yu-Fang Chen, Daniel Dietsch, Marius Greitschus," + System.lineSeparator()
			+ "     Jochen Hoenicke, Yong Li, Alexander Nutz, Betim Musa, Christian" + System.lineSeparator()
			+ "     Schilling, Tanja Schindler, Andreas Podelski: Ultimate Automizer" + System.lineSeparator()
			+ "     and the Search for Perfect Interpolants - (Competition Contribution)." + System.lineSeparator()
			+ "     TACAS (2) 2018: 447-451" + System.lineSeparator()
			+ "[5] Matthias Heizmann, Jochen Hoenicke, Andreas Podelski: Software Model" + System.lineSeparator()
			+ "     Checking for People Who Love Automata. CAV 2013:36-52" + System.lineSeparator()
			+ "[6] Daniel Dietsch, Matthias Heizmann, Betim Musa, Alexander Nutz, Andreas Podelski"
			+ System.lineSeparator() + "    Craig vs. Newton in software model checking. ESEC/SIGSOFT FSE 2017: 487-497"
			+ System.lineSeparator() + "|";

	private int mWrittenScriptCounter = 0;

	private final int mBenchmarkTooSimpleThreshold = 10 * 1000;
	private final boolean mWriteUnsolvedBenchmarks = true;

	public LoggingScriptForMainTrackBenchmarks(final Script script, final String baseFilename, final String directory) {
		super(script, baseFilename, directory);
	}

	@Override
	public LBool checkSat() throws SMTLIBException {
		final long timeBefore = System.nanoTime();
		final LBool sat = super.mScript.checkSat();
		final long durationInMilliseconds = (System.nanoTime() - timeBefore) / 1000 / 1000;
		final boolean solved = sat == LBool.SAT || sat == LBool.UNSAT;
		if (solved && durationInMilliseconds >= mBenchmarkTooSimpleThreshold || !solved && mWriteUnsolvedBenchmarks) {
			// final File file = constructFile('_' + String.valueOf(mWrittenScriptCounter));
			// final List<ArrayList<ISmtCommand<?>>> processedCommandStack = process(mCommandStack, sat);
			// writeCommandStackToFile(file, processedCommandStack);
			final File file = constructFile('_' + String.valueOf(mWrittenScriptCounter));
			final List<ArrayList<ISmtCommand<?>>> processedCommandStack = postprocessCommandStack(mCommandStack, sat);
			writeCommandStackToFile(file, processedCommandStack);
			mWrittenScriptCounter++;
		}
		return sat;
	}

	@Override
	public LBool assertTerm(final Term term) throws SMTLIBException {
		final Term nonNamedTerm;
		if (term instanceof AnnotatedTerm) {
			nonNamedTerm = ((AnnotatedTerm) term).getSubterm();
		} else {
			nonNamedTerm = term;
		}
		if (nonNamedTerm != mScript.term("true")) {
			addToCurrentAssertionStack(new AssertCommand(nonNamedTerm));
		}
		return mScript.assertTerm(term);
	}

	@Override
	public Map<Term, Term> getValue(final Term[] terms) throws SMTLIBException, UnsupportedOperationException {
		return mScript.getValue(terms);
	}

	private List<ArrayList<ISmtCommand<?>>> process(final LinkedList<ArrayList<ISmtCommand<?>>> commandStack,
			final LBool status) {
		final ArrayList<ISmtCommand<?>> flattenedStack = new ArrayList<>();
		addInvarSynthCommands(flattenedStack, status);
		boolean toKeepCommandsReached = false;
		for (final ArrayList<ISmtCommand<?>> list : commandStack) {
			for (final ISmtCommand<?> command : list) {
				if (!toKeepCommandsReached) {
					if (command.toString().contains("declare-fun")) {
						toKeepCommandsReached = true;
					}
				}
				if (toKeepCommandsReached) {
					flattenedStack.add(command);
				}
			}
		}
		flattenedStack.add(new CheckSatCommand());
		flattenedStack.add(new ExitCommand());
		return Collections.singletonList(flattenedStack);
	}

	private void addInvarSynthCommands(final ArrayList<ISmtCommand<?>> flattenedStack, final LBool status) {
		// final String logic = "(set-logic " + getLogic() + ")" + System.lineSeparator();
		// flattenedStack.add(new SmtCommandInStringRepresentation(logic));
		// flattenedStack.add(new SmtCommandInStringRepresentation(getSourceInfo()));
		// final String version = "(set-info :smt-lib-version 2.5)" + System.lineSeparator();
		// flattenedStack.add(new SmtCommandInStringRepresentation(version));
		// final String category = "(set-info :category \"industrial\")" + System.lineSeparator();
		// flattenedStack.add(new SmtCommandInStringRepresentation(category));
		// final String statusInfo = "(set-info :status " + status + ")" + System.lineSeparator();
		// flattenedStack.add(new SmtCommandInStringRepresentation(statusInfo));
	}

	public static String getSourceInfo() {
		// return SOURCE_INVSYNTH;
		// return SOURCE_GNTA;
		// return SOURCE_AUTOMIZER;
		return SOURCE_POLYNOMIAL_RELATION_TEST;
	}

	public static String getLogic() {
		// return "QF_NRA";
		// return "QF_NIA";
		// return "QF_AUFNIRA";
		// return "QF_ABV";
		return "BV";
	}

	private List<ISmtCommand<?>> buildPreamble(final Logics logic, final LBool sat, final String info) {
		final List<ISmtCommand<?>> result = new ArrayList<>();
		result.add(new SetInfoCommand(":smt-lib-version", new BigDecimal("2.6")));
		result.add(new SetLogicCommand(logic.name()));
		result.add(new SetInfoCommand(":source", new QuotedObject(info)));
		result.add(new SetInfoCommand(":license", new QuotedObject("https://creativecommons.org/licenses/by/4.0/")));
		result.add(new SetInfoCommand(":category", new QuotedObject("industrial")));
		result.add(new SetInfoCommand(":status", new QuotedObject(sat.toString())));
		return result;
	}

	private List<ArrayList<ISmtCommand<?>>>
			postprocessCommandStack(final LinkedList<ArrayList<ISmtCommand<?>>> commandStack, final LBool sat) {
		final ArrayDeque<ISmtCommand<?>> tmp = new ArrayDeque<>();
		final TermClassifier tc = new TermClassifier();
		final Iterator<ArrayList<ISmtCommand<?>>> it = commandStack.descendingIterator();
		while (it.hasNext()) {
			final ArrayList<ISmtCommand<?>> commands = it.next();
			for (int i = commands.size() - 1; i >= 0; i--) {
				final ISmtCommand<?> command = commands.get(i);
				if (command instanceof AssertCommand) {
					final AssertCommand ac = (AssertCommand) command;
					tc.checkTerm(ac.getTerm());
					tmp.addFirst(command);
				} else if (command instanceof DeclareFunCommand) {
					final DeclareFunCommand dfc = (DeclareFunCommand) command;
					if (tc.getOccuringFunctionNames().contains(dfc.getFun())) {
						tmp.addFirst(command);
					}
				} else {
					// do nothing, command not supported or not relevant for Main Track script
				}
			}
		}
		tmp.add(new CheckSatCommand());
		tmp.add(new ExitCommand());
		final Logics logic = determineLogic(tc);
		final ArrayList<ISmtCommand<?>> result = new ArrayList<>();
		result.addAll(buildPreamble(logic, sat, getSourceInfo()));
		result.addAll(tmp);
		return Collections.singletonList(result);
	}

	private Logics determineLogic(final TermClassifier tc) {
		final ArrayList<Logics> remainingCandidates = new ArrayList<>();
		for (final Logics logic : Logics.values()) {
			if (logic == Logics.ALL) {
				continue;
			}
			if (logic.isDifferenceLogic()) {
				continue;
			}
			if (logic.isUF()) {
				continue;
			}
			if (logic.isNonLinearArithmetic() != tc.hasNonlinearArithmetic()) {
				continue;
			}
			if (tc.getOccuringQuantifiers().isEmpty() == logic.isQuantified()) {
				continue;
			}
			if (tc.hasArrays() != logic.isArray()) {
				continue;
			}
			if (tc.getOccuringSortNames().contains(SmtSortUtils.INT_SORT) != logic.hasIntegers()) {
				continue;
			}
			if (tc.getOccuringSortNames().contains(SmtSortUtils.REAL_SORT) != logic.hasReals()) {
				if (tc.getOccuringSortNames().contains(SmtSortUtils.FLOATINGPOINT_SORT)) {
					// if we have FloatingPoint then Reals are ok
				} else {
					continue;
				}
			}
			if (tc.getOccuringSortNames().contains(SmtSortUtils.BITVECTOR_SORT) != logic.isBitVector()) {
				continue;
			}
			if ((tc.getOccuringSortNames().contains(SmtSortUtils.FLOATINGPOINT_SORT)
					|| tc.getOccuringSortNames().contains(SmtSortUtils.ROUNDINGMODE_SORT)) != logic.isFloatingPoint()) {
				continue;
			}
			remainingCandidates.add(logic);
		}
		if (remainingCandidates.isEmpty()) {
			throw new AssertionError("no applicable logic");
		} else if (remainingCandidates.size() == 1) {
			return remainingCandidates.iterator().next();
		} else {
			throw new AssertionError("too many candiate logics " + remainingCandidates);
		}
	}
}
