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

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import de.uni_freiburg.informatik.ultimate.automata.AtsDefinitionPrinter.Labeling;
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
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.PowersetDeterminizer;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.RemoveUnreachable;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.minimization.MinimizeDfa;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.DifferenceDD;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.PetriNet2FiniteAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.julian.PetriNetJulian;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceStore;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;

@Deprecated
public class ResultChecker<LETTER,STATE> {
	
//	private static Logger logger;
	
	private static int resultCheckStackHeight = 0;
	public static final int maxResultCheckStackHeight = 1;
	
	public final static boolean m_InvariantCheck_DetComplementBuchi = false;
	
	public static boolean doingInvariantCheck() {
		return resultCheckStackHeight > 0;
	}


	
	
	
	public static boolean reduceBuchi(IUltimateServiceProvider services, INestedWordAutomatonOldApi operand,
			INestedWordAutomatonOldApi result) throws AutomataLibraryException {
		Logger logger = services.getLoggingService().getLogger(LibraryIdentifiers.s_LibraryID);

		StateFactory stateFactory = operand.getStateFactory();
		if (resultCheckStackHeight >= maxResultCheckStackHeight)
			return true;
		resultCheckStackHeight++;
		logger.debug("Testing correctness of reduceBuchi");
		
		INestedWordAutomatonOldApi minimizedOperand = (new MinimizeDfa(services, operand)).getResult();

		boolean correct = true;
		NestedLassoRun inOperandButNotInResultBuchi = nwaBuchiLanguageInclusion(services, stateFactory, minimizedOperand,result);
		if (inOperandButNotInResultBuchi != null) {
			logger.error("Lasso word accepted by operand, but not by result: " + 
					inOperandButNotInResultBuchi.getNestedLassoWord());
			correct = false;
		}
		NestedLassoRun inResultButNotInOperatndBuchi = nwaBuchiLanguageInclusion(services, stateFactory, result,minimizedOperand);
		if (inResultButNotInOperatndBuchi != null) {
			logger.error("Lasso word accepted by result, but not by operand: " + 
					inResultButNotInOperatndBuchi.getNestedLassoWord());
			correct = false;
		}

		logger.debug("Finished testing correctness of reduceBuchi");
		resultCheckStackHeight--;
		return correct;
	}
	
//	public static boolean buchiEmptiness(INestedWordAutomatonOldApi operand,
//										 NestedLassoRun result) {
//		if (resultCheckStackHeight >= maxResultCheckStackHeight) return true;
//		resultCheckStackHeight++;
//		logger.info("Testing correctness of buchiEmptiness");
//
//		boolean correct = true;
//		if (result == null) {
//			logger.warn("No check for positive buchiEmptiness");
//		} else {
//			correct = (new BuchiAccepts(operand, result.getNestedLassoWord())).getResult();
//		}
//
//		logger.info("Finished testing correctness of buchiEmptiness");
//		resultCheckStackHeight--;
//		return correct;
//	}
	
	
	public static boolean buchiIntersect(IUltimateServiceProvider services,
			INestedWordAutomatonOldApi operand1,
			INestedWordAutomatonOldApi operand2,
			INestedWordAutomatonOldApi result) {
		Logger logger = services.getLoggingService().getLogger(LibraryIdentifiers.s_LibraryID);

		if (resultCheckStackHeight >= maxResultCheckStackHeight) return true;
		resultCheckStackHeight++;
		logger.info("Testing correctness of buchiIntersect");

		boolean correct = true;
		logger.warn("No test for buchiIntersection available yet");

		logger.info("Finished testing correctness of buchiIntersect");
		resultCheckStackHeight--;
		return correct;
	}
	

	
	public static boolean buchiComplement(IUltimateServiceProvider services, INestedWordAutomatonOldApi operand,
										  INestedWordAutomatonOldApi result) throws AutomataLibraryException {
		Logger logger = services.getLoggingService().getLogger(LibraryIdentifiers.s_LibraryID);
		if (resultCheckStackHeight >= maxResultCheckStackHeight) return true;
		resultCheckStackHeight++;
		logger.info("Testing correctness of complementBuchi");
		
		int maxNumberOfStates = Math.max(operand.size(), result.size());
		boolean correct = true;
		for (int i=0; i<10; i++) {
			NestedWord stem = (new GetRandomNestedWord(operand, maxNumberOfStates)).getResult();
			NestedWord loop = (new GetRandomNestedWord(operand, maxNumberOfStates)).getResult();
			NestedLassoWord lasso = new NestedLassoWord(stem, loop);
			boolean operandAccepts = (new BuchiAccepts(services, operand, lasso)).getResult();
			boolean resultAccepts = (new BuchiAccepts(services, result, lasso)).getResult();
			if (operandAccepts ^ resultAccepts) {
				// check passed
			} else {
				correct = false;
				String message = "// Problem with lasso " + lasso.toString();
				writeToFileIfPreferred(services, "FailedBuchiComplementCheck", message, operand);
				break;
			}
		}
		
//		INestedWordAutomaton intersection = (new Intersect(true, false, operand, result)).getResult();
//		NestedLassoRun ctx = new EmptinessCheck().getAcceptingNestedLassoRun(intersection);
//		boolean correct = (ctx == null);
//		assert (correct);
		
		logger.info("Finished testing correctness of complementBuchi");
		resultCheckStackHeight--;
		return correct;
	}
	
	
	public static boolean buchiComplementSVW(IUltimateServiceProvider services, 
			INestedWordAutomatonOldApi operand,
			INestedWordAutomatonOldApi result) throws AutomataLibraryException {
		Logger logger = services.getLoggingService().getLogger(LibraryIdentifiers.s_LibraryID);

		if (resultCheckStackHeight >= maxResultCheckStackHeight) return true;
		resultCheckStackHeight++;
		logger.info("Testing correctness of complementBuchiSVW");
		
		int maxNumberOfStates = Math.max(operand.size(), result.size());
		boolean correct = true;
		for (int i=0; i<10; i++) {
			NestedWord stem = (new GetRandomNestedWord(operand, maxNumberOfStates)).getResult();
			NestedWord loop = (new GetRandomNestedWord(operand, maxNumberOfStates)).getResult();
			NestedLassoWord lasso = new NestedLassoWord(stem, loop);

			boolean operandAccepts = (new BuchiAccepts(services, operand, lasso)).getResult();
			boolean resultAccepts = (new BuchiAccepts(services, operand, lasso)).getResult();
			if (operandAccepts ^ resultAccepts) {
				// ok
			} else {
				correct = false;
				break;
			}
		}


//		{
//			INestedWordAutomaton intersection = (new Intersect(true, false, operand, result)).getResult();
//			NestedLassoRun ctx = new EmptinessCheck().getAcceptingNestedLassoRun(intersection);
//			correct = (ctx == null);
//			assert (correct);
//		}
//		
//		{
//			INestedWordAutomaton operandComplement = (new BuchiComplementFKV(operand)).getResult();
//			INestedWordAutomaton resultComplement = (new BuchiComplementFKV(result)).getResult();
//			INestedWordAutomaton intersection = (new Intersect(true, false, operandComplement, resultComplement)).getResult();
//			NestedLassoRun ctx = new EmptinessCheck().getAcceptingNestedLassoRun(intersection);
//			correct = (ctx == null);
//			assert (correct);
//		}

		logger.info("Finished testing correctness of complementBuchiSVW");
		resultCheckStackHeight--;
		return correct;
	}
	
	
	
	public static boolean petriNetJulian(IUltimateServiceProvider services, INestedWordAutomatonOldApi op,
										 PetriNetJulian result) throws AutomataLibraryException {
		Logger logger = services.getLoggingService().getLogger(LibraryIdentifiers.s_LibraryID);

		if (resultCheckStackHeight >= maxResultCheckStackHeight) return true;
		resultCheckStackHeight++;
		logger.info("Testing correctness of PetriNetJulian constructor");

		INestedWordAutomatonOldApi resultAutomata = 
							(new PetriNet2FiniteAutomaton(services, result)).getResult();
		boolean correct = true;
		correct &= (nwaLanguageInclusion(services, resultAutomata,op,op.getStateFactory()) == null);
		correct &= (nwaLanguageInclusion(services, op,resultAutomata,op.getStateFactory()) == null);

		logger.info("Finished testing correctness of PetriNetJulian constructor");
		resultCheckStackHeight--;
		return correct;
	}
	
	

	

	public static boolean petriNetLanguageEquivalence(IUltimateServiceProvider services, PetriNetJulian net1, PetriNetJulian net2) throws AutomataLibraryException {
		Logger logger = services.getLoggingService().getLogger(LibraryIdentifiers.s_LibraryID);

		if (resultCheckStackHeight >= maxResultCheckStackHeight) return true;
		resultCheckStackHeight++;
		logger.info("Testing Petri net language equivalence");
		INestedWordAutomatonOldApi finAuto1 = (new PetriNet2FiniteAutomaton(services, net1)).getResult();
		INestedWordAutomatonOldApi finAuto2 = (new PetriNet2FiniteAutomaton(services, net2)).getResult();
		NestedRun subsetCounterex = nwaLanguageInclusion(services, finAuto1, finAuto2, net1.getStateFactory());
		boolean subset = subsetCounterex == null;
		if (!subset) {
			logger.error("Only accepted by first: " + subsetCounterex.getWord());
		}
		NestedRun supersetCounterex = nwaLanguageInclusion(services, finAuto2, finAuto1, net1.getStateFactory());
		boolean superset = supersetCounterex == null;
		if (!superset) {
			logger.error("Only accepted by second: " + supersetCounterex.getWord());
		}
		boolean result = subset && superset;
		logger.info("Finished Petri net language equivalence");
		resultCheckStackHeight--;
		return result;
	}
	
	
	public static <E> boolean isSubset(Collection<E> lhs, Collection<E> rhs) {
		for (E elem : lhs) {
			if (!rhs.contains(elem)) {
				return false;
			}
		}
		return true;
	}


	public static <LETTER,STATE> NestedRun nwaLanguageInclusion(IUltimateServiceProvider services, INestedWordAutomatonOldApi nwa1, INestedWordAutomatonOldApi nwa2, StateFactory stateFactory) throws AutomataLibraryException {
		IStateDeterminizer stateDeterminizer = new PowersetDeterminizer<LETTER,STATE>(nwa2, true, stateFactory);
		INestedWordAutomatonOldApi nwa1MinusNwa2 = (new DifferenceDD(services, nwa1, nwa2, stateDeterminizer, stateFactory, false, false)).getResult();
		NestedRun inNwa1ButNotInNwa2 = (new IsEmpty(services, nwa1MinusNwa2)).getNestedRun();
		return inNwa1ButNotInNwa2;
//		if (inNwa1ButNotInNwa2 != null) {
//			logger.error("Word accepted by nwa1, but not by nwa2: " + 
//					inNwa1ButNotInNwa2.getWord());
//			correct = false;
//		}
	}
	
	public static <LETTER, STATE> INestedWordAutomatonOldApi<LETTER, STATE> getOldApiNwa(
			IUltimateServiceProvider services,
			INestedWordAutomatonSimple<LETTER, STATE> nwa)
			throws AutomataLibraryException {
		if (nwa instanceof INestedWordAutomatonOldApi) {
			return (INestedWordAutomatonOldApi<LETTER, STATE>) nwa;
		} else {
			return (new RemoveUnreachable<LETTER, STATE>(services, nwa)).getResult();
		}
	}
	
	private static NestedLassoRun nwaBuchiLanguageInclusion(IUltimateServiceProvider services, StateFactory stateFactory, 
			INestedWordAutomatonOldApi nwa1, INestedWordAutomatonOldApi nwa2) throws AutomataLibraryException {
		return (new BuchiIsIncluded(services, stateFactory, nwa1, nwa2)).getCounterexample();
	}
	
	
    private static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date();
        return dateFormat.format(date);
    }
    
    public static void writeToFileIfPreferred(IUltimateServiceProvider services, String filenamePrefix, String message, IAutomaton... automata) {
    	String workingDirectory = System.getProperty("user.dir");
    	
		IScopeContext scope = InstanceScope.INSTANCE;
		UltimatePreferenceStore prefs = new UltimatePreferenceStore(LibraryIdentifiers.s_LibraryID);
//		boolean writeToFile = prefs.getBoolean(PreferenceInitializer.Name_Write);
//		if (writeToFile) {
			String filename = workingDirectory + File.separator+filenamePrefix + getDateTime() + ".ats";
			new AtsDefinitionPrinter(services, filenamePrefix, filename, Labeling.NUMERATE, message, automata);
//		}
    }
    
	public static <LETTER,STATE> NestedLassoWord<LETTER> getRandomNestedLassoWord(INestedWordAutomatonSimple<LETTER, STATE> automaton, int size) throws AutomataLibraryException {
		NestedWord<LETTER> stem = (new GetRandomNestedWord<LETTER, STATE>(automaton, size)).getResult();
		NestedWord<LETTER> loop = (new GetRandomNestedWord<LETTER, STATE>(automaton, size)).getResult();
		return new NestedLassoWord<LETTER>(stem, loop);
	}
    
	
}
