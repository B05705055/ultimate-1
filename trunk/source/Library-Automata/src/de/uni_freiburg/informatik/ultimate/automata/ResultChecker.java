/*
 * Copyright (C) 2011-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2009-2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE Automata Library.
 * 
 * The ULTIMATE Automata Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE Automata Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Automata Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Automata Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE Automata Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.automata;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import de.uni_freiburg.informatik.ultimate.automata.AutomatonDefinitionPrinter.Format;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonOldApi;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonSimple;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedRun;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.buchiNwa.BuchiAccepts;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.buchiNwa.BuchiIsIncluded;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.buchiNwa.NestedLassoRun;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.buchiNwa.NestedLassoWord;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.GetRandomNestedWord;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.IStateDeterminizer;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.IsEmpty;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.IsIncluded;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.PowersetDeterminizer;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.RemoveUnreachable;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.DifferenceDD;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.PetriNet2FiniteAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.julian.PetriNetJulian;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;

@Deprecated
public final class ResultChecker {
	private static int sResultCheckStackHeight = 0;
	public static final int MAX_RESULT_CHECK_STACK_HEIGHT = 1;
	public static final boolean INVARIANT_CHECK_DET_COMPLEMENT_BUCHI = false;
	
	private ResultChecker() {
		// empty private constructor
	}

	public static boolean doingInvariantCheck() {
		return sResultCheckStackHeight > 0;
	}

	public static <LETTER, STATE> boolean reduceBuchi(
			final AutomataLibraryServices services,
			final INestedWordAutomaton<LETTER, STATE> operand,
			final INestedWordAutomaton<LETTER, STATE> result)
					throws AutomataLibraryException {
		final ILogger logger =
				services.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);

		if (sResultCheckStackHeight >= MAX_RESULT_CHECK_STACK_HEIGHT) {
			return true;
		}
		sResultCheckStackHeight++;
		logger.debug("Testing correctness of reduceBuchi");

		/*
		 * TODO Christian 2016-08-01: I removed this minimization call for new
		 *      API inclusion; also, it is not reasonable to use this old
		 *      minimization here.
		 */
		final INestedWordAutomaton<LETTER, STATE> minimizedOperand = 
//				(new MinimizeDfa(services, operand)).getResult();
				operand;

		boolean correct = true;
		final StateFactory<STATE> stateFactory = operand.getStateFactory();
		final NestedLassoRun<LETTER, STATE> inOperandButNotInResultBuchi =
				nwaBuchiLanguageInclusion(services, stateFactory,
						minimizedOperand, result);
		if (inOperandButNotInResultBuchi != null) {
			logger.error("Lasso word accepted by operand, but not by result: "
					+ inOperandButNotInResultBuchi.getNestedLassoWord());
			correct = false;
		}
		final NestedLassoRun<LETTER, STATE> inResultButNotInOperatndBuchi =
				nwaBuchiLanguageInclusion(services, stateFactory, result,
						minimizedOperand);
		if (inResultButNotInOperatndBuchi != null) {
			logger.error("Lasso word accepted by result, but not by operand: "
					+ inResultButNotInOperatndBuchi.getNestedLassoWord());
			correct = false;
		}

		logger.debug("Finished testing correctness of reduceBuchi");
		sResultCheckStackHeight--;
		return correct;
	}

	// public static boolean buchiEmptiness(INestedWordAutomatonOldApi operand,
	// NestedLassoRun result) {
	// if (resultCheckStackHeight >= maxResultCheckStackHeight) return true;
	// resultCheckStackHeight++;
	// logger.info("Testing correctness of buchiEmptiness");
	//
	// boolean correct = true;
	// if (result == null) {
	// logger.warn("No check for positive buchiEmptiness");
	// } else {
	// correct = (new BuchiAccepts(operand, result.getNestedLassoWord())).getResult();
	// }
	//
	// logger.info("Finished testing correctness of buchiEmptiness");
	// resultCheckStackHeight--;
	// return correct;
	// }

	/**
	 * TODO not correctly implemented yet
	 */
	public static <LETTER, STATE> boolean buchiIntersect(
			final AutomataLibraryServices services,
			final INestedWordAutomaton<LETTER, STATE> operand1,
			final INestedWordAutomaton<LETTER, STATE> operand2,
			final INestedWordAutomaton<LETTER, STATE> result) {
		final ILogger logger =
				services.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);

		if (sResultCheckStackHeight >= MAX_RESULT_CHECK_STACK_HEIGHT) {
			return true;
		}
		sResultCheckStackHeight++;
		logger.info("Testing correctness of buchiIntersect");

		final boolean correct = true;
		logger.warn("No test for buchiIntersection available yet");

		logger.info("Finished testing correctness of buchiIntersect");
		sResultCheckStackHeight--;
		return correct;
	}

	public static <LETTER, STATE> boolean buchiComplement(
			final AutomataLibraryServices services,
			final INestedWordAutomaton<LETTER, STATE> operand,
			final INestedWordAutomaton<LETTER, STATE> result)
					throws AutomataLibraryException {
		final ILogger logger =
				services.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		if (sResultCheckStackHeight >= MAX_RESULT_CHECK_STACK_HEIGHT) {
			return true;
		}
		sResultCheckStackHeight++;
		logger.info("Testing correctness of buchiComplement");

		final int maxLength = Math.max(operand.size(), result.size());
		final int numberOfSamples = 10;
		boolean correct = true;
		for (int i = 0; i < numberOfSamples; i++) {
			final NestedWord<LETTER> stem =
					(new GetRandomNestedWord<LETTER, STATE>(operand, maxLength)).getResult();
			final NestedWord<LETTER> loop =
					(new GetRandomNestedWord<LETTER, STATE>(operand, maxLength)).getResult();
			final NestedLassoWord<LETTER> lasso =
					new NestedLassoWord<LETTER>(stem, loop);
			final boolean operandAccepts = 
					(new BuchiAccepts<LETTER, STATE>(services, operand, lasso)).getResult();
			final boolean resultAccepts =
					(new BuchiAccepts<LETTER, STATE>(services, result, lasso)).getResult();
			if (operandAccepts ^ resultAccepts) {
				// check passed
			} else {
				correct = false;
				final String message = "// Problem with lasso " + lasso.toString();
				writeToFileIfPreferred(services, "FailedBuchiComplementCheck", message, operand);
				break;
			}
		}

		// INestedWordAutomaton intersection = (new Intersect(true, false, operand, result)).getResult();
		// NestedLassoRun ctx = new EmptinessCheck().getAcceptingNestedLassoRun(intersection);
		// boolean correct = (ctx == null);
		// assert (correct);

		logger.info("Finished testing correctness of complementBuchi");
		sResultCheckStackHeight--;
		return correct;
	}

	public static <LETTER, STATE> boolean buchiComplementSVW(
			final AutomataLibraryServices services,
			final INestedWordAutomaton<LETTER, STATE> operand,
			final INestedWordAutomaton<LETTER, STATE> result)
					throws AutomataLibraryException {
		final ILogger logger =
				services.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);

		if (sResultCheckStackHeight >= MAX_RESULT_CHECK_STACK_HEIGHT) {
			return true;
		}
		sResultCheckStackHeight++;
		logger.info("Testing correctness of complementBuchiSVW");

		final int maxNumberOfStates = Math.max(operand.size(), result.size());
		boolean correct = true;
		for (int i = 0; i < 10; i++) {
			final NestedWord<LETTER> stem =
					(new GetRandomNestedWord<LETTER, STATE>(operand, maxNumberOfStates)).getResult();
			final NestedWord<LETTER> loop =
					(new GetRandomNestedWord<LETTER, STATE>(operand, maxNumberOfStates)).getResult();
			final NestedLassoWord<LETTER> lasso =
					new NestedLassoWord<LETTER>(stem, loop);

			final boolean operandAccepts =
					(new BuchiAccepts<LETTER, STATE>(services, operand, lasso)).getResult();
			final boolean resultAccepts =
					(new BuchiAccepts<LETTER, STATE>(services, operand, lasso)).getResult();
			if (operandAccepts ^ resultAccepts) {
				// ok
			} else {
				correct = false;
				break;
			}
		}

		// {
		// INestedWordAutomaton intersection = (new Intersect(true, false, operand, result)).getResult();
		// NestedLassoRun ctx = new EmptinessCheck().getAcceptingNestedLassoRun(intersection);
		// correct = (ctx == null);
		// assert (correct);
		// }
		//
		// {
		// INestedWordAutomaton operandComplement = (new BuchiComplementFKV(operand)).getResult();
		// INestedWordAutomaton resultComplement = (new BuchiComplementFKV(result)).getResult();
		// INestedWordAutomaton intersection = (new Intersect(true, false, operandComplement,
		// resultComplement)).getResult();
		// NestedLassoRun ctx = new EmptinessCheck().getAcceptingNestedLassoRun(intersection);
		// correct = (ctx == null);
		// assert (correct);
		// }

		logger.info("Finished testing correctness of complementBuchiSVW");
		sResultCheckStackHeight--;
		return correct;
	}

	public static <LETTER, STATE> boolean petriNetJulian(
			final AutomataLibraryServices services,
			final INestedWordAutomaton<LETTER, STATE> op,
			final PetriNetJulian<LETTER, STATE> result)
					throws AutomataLibraryException {
		final ILogger logger =
				services.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);

		if (sResultCheckStackHeight >= MAX_RESULT_CHECK_STACK_HEIGHT) {
			return true;
		}
		sResultCheckStackHeight++;
		logger.info("Testing correctness of PetriNetJulian constructor");

		final INestedWordAutomaton<LETTER, STATE> resultAutomata =
				(new PetriNet2FiniteAutomaton<LETTER, STATE>(services, result)).getResult();
		boolean correct = true;
		correct &= (nwaLanguageInclusionNew(services, resultAutomata, op, op.getStateFactory()) == null);
		correct &= (nwaLanguageInclusionNew(services, op, resultAutomata, op.getStateFactory()) == null);

		logger.info("Finished testing correctness of PetriNetJulian constructor");
		sResultCheckStackHeight--;
		return correct;
	}

	public static <LETTER, STATE> boolean petriNetLanguageEquivalence(
			final AutomataLibraryServices services,
			final PetriNetJulian<LETTER, STATE> net1,
			final PetriNetJulian<LETTER, STATE> net2)
					throws AutomataLibraryException {
		final ILogger logger =
				services.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);

		if (sResultCheckStackHeight >= MAX_RESULT_CHECK_STACK_HEIGHT) {
			return true;
		}
		sResultCheckStackHeight++;
		logger.info("Testing Petri net language equivalence");
		final INestedWordAutomaton<LETTER, STATE> finAuto1 =
				(new PetriNet2FiniteAutomaton<LETTER, STATE>(services, net1)).getResult();
		final INestedWordAutomaton<LETTER, STATE> finAuto2 =
				(new PetriNet2FiniteAutomaton<LETTER, STATE>(services, net2)).getResult();
		final NestedRun<LETTER, STATE> subsetCounterex =
				nwaLanguageInclusionNew(services, finAuto1, finAuto2, net1.getStateFactory());
		final boolean subset = subsetCounterex == null;
		if (!subset) {
			logger.error("Only accepted by first: " + subsetCounterex.getWord());
		}
		final NestedRun<LETTER, STATE> supersetCounterex =
				nwaLanguageInclusionNew(services, finAuto2, finAuto1, net1.getStateFactory());
		final boolean superset = supersetCounterex == null;
		if (!superset) {
			logger.error("Only accepted by second: " + supersetCounterex.getWord());
		}
		final boolean result = subset && superset;
		logger.info("Finished Petri net language equivalence");
		sResultCheckStackHeight--;
		return result;
	}

	public static <E> boolean isSubset(final Collection<E> lhs, final Collection<E> rhs) {
		for (final E elem : lhs) {
			if (!rhs.contains(elem)) {
				return false;
			}
		}
		return true;
	}

	public static <LETTER, STATE> NestedRun<LETTER, STATE> nwaLanguageInclusion(
			final AutomataLibraryServices services,
			final INestedWordAutomatonOldApi<LETTER, STATE> nwa1,
			final INestedWordAutomatonOldApi<LETTER, STATE> nwa2,
			final StateFactory<STATE> stateFactory)
			throws AutomataLibraryException {
		final IStateDeterminizer<LETTER, STATE> stateDeterminizer =
				new PowersetDeterminizer<LETTER, STATE>(nwa2, true, stateFactory);
		final INestedWordAutomaton<LETTER, STATE> nwa1MinusNwa2 =
				(new DifferenceDD<LETTER, STATE>(services, nwa1, nwa2, stateDeterminizer,
						stateFactory, false, false)).getResult();
		final NestedRun<LETTER, STATE> inNwa1ButNotInNwa2 =
				(new IsEmpty<LETTER, STATE>(services, nwa1MinusNwa2)).getNestedRun();
		return inNwa1ButNotInNwa2;
		// if (inNwa1ButNotInNwa2 != null) {
		// logger.error("Word accepted by nwa1, but not by nwa2: " +
		// inNwa1ButNotInNwa2.getWord());
		// correct = false;
		// }
	}
	
	public static <LETTER, STATE> NestedRun<LETTER, STATE> nwaLanguageInclusionNew(
			final AutomataLibraryServices services,
			final INestedWordAutomaton<LETTER, STATE> nwa1,
			final INestedWordAutomaton<LETTER, STATE> nwa2,
			final StateFactory<STATE> stateFactory)
					throws AutomataLibraryException {
		return new IsIncluded<>(services, stateFactory, nwa1, nwa2).getCounterexample();
	}

	public static <LETTER, STATE> INestedWordAutomatonOldApi<LETTER, STATE> getOldApiNwa(
			final AutomataLibraryServices services,
			final INestedWordAutomatonSimple<LETTER, STATE> nwa)
			throws AutomataLibraryException {
		if (nwa instanceof INestedWordAutomatonOldApi) {
			return (INestedWordAutomatonOldApi<LETTER, STATE>) nwa;
		} else {
			return (new RemoveUnreachable<LETTER, STATE>(services, nwa)).getResult();
		}
	}

	private static <LETTER, STATE> NestedLassoRun<LETTER, STATE> nwaBuchiLanguageInclusion(
			final AutomataLibraryServices services,
			final StateFactory<STATE> stateFactory,
			final INestedWordAutomaton<LETTER, STATE> nwa1,
			final INestedWordAutomaton<LETTER, STATE> nwa2)
					throws AutomataLibraryException {
		return (new BuchiIsIncluded<LETTER, STATE>(services, stateFactory, nwa1, nwa2)).getCounterexample();
	}

	private static String getDateTime() {
		final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		final Date date = new Date();
		return dateFormat.format(date);
	}

	public static <LETTER, STATE> void writeToFileIfPreferred(
			final AutomataLibraryServices services,
			final String filenamePrefix,
			final String message,
			final IAutomaton<LETTER, STATE>... automata) {
		final String workingDirectory = System.getProperty("user.dir");
		final String filename = workingDirectory + File.separator
				+ filenamePrefix + getDateTime() + ".ats";
		new AutomatonDefinitionPrinter<LETTER, STATE>(services, filenamePrefix,
				filename, Format.ATS_NUMERATE, message, automata);
	}

	public static <LETTER, STATE> NestedLassoWord<LETTER> getRandomNestedLassoWord(
			final INestedWordAutomatonSimple<LETTER, STATE> automaton,
			final int size)
					throws AutomataLibraryException {
		final NestedWord<LETTER> stem =
				(new GetRandomNestedWord<LETTER, STATE>(automaton, size)).getResult();
		final NestedWord<LETTER> loop =
				(new GetRandomNestedWord<LETTER, STATE>(automaton, size)).getResult();
		return new NestedLassoWord<LETTER>(stem, loop);
	}
}
