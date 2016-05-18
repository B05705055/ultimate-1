/*
 * Copyright (C) 2013-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.ResultChecker;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonOldApi;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonSimple;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.DeterminizeDD;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.reachableStatesAutomaton.NestedWordAutomatonReachableStates;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;


public class Determinize<LETTER,STATE> implements IOperation<LETTER,STATE> {

	private final AutomataLibraryServices m_Services;
	private final ILogger m_Logger;
	
	private final INestedWordAutomatonSimple<LETTER,STATE> m_Operand;
	private final DeterminizeNwa<LETTER, STATE> m_Determinized;
	private final NestedWordAutomatonReachableStates<LETTER,STATE> m_Result;
	private final IStateDeterminizer<LETTER,STATE> stateDeterminizer;
	private final StateFactory<STATE> m_StateFactory;
	
	
	@Override
	public String operationName() {
		return "determinize";
	}
	
	
	@Override
	public String startMessage() {
		return "Start " + operationName() + " Operand " + 
			m_Operand.sizeInformation();
	}
	
	
	@Override
	public String exitMessage() {
		return "Finished " + operationName() + " Result " + 
				m_Result.sizeInformation();
	}
	
	
	public Determinize(AutomataLibraryServices services,
			StateFactory<STATE> stateFactory, 
			INestedWordAutomatonSimple<LETTER,STATE> input) throws AutomataLibraryException {
		m_Services = services;
		m_Logger = m_Services.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		this.stateDeterminizer = new PowersetDeterminizer<LETTER, STATE>(input, true, stateFactory);
		this.m_StateFactory = stateFactory;
		this.m_Operand = input;
		m_Logger.info(startMessage());
		m_Determinized = new DeterminizeNwa<LETTER, STATE>(m_Services, input, stateDeterminizer, m_StateFactory);
		m_Result = new NestedWordAutomatonReachableStates<LETTER, STATE>(m_Services, m_Determinized);
		m_Logger.info(exitMessage());
	}
	


	@Override
	public INestedWordAutomatonOldApi<LETTER, STATE> getResult() {
		return m_Result;
	}


	@Override
	public boolean checkResult(StateFactory<STATE> sf) throws AutomataLibraryException {
		boolean correct = true;
		if (stateDeterminizer instanceof PowersetDeterminizer) {
			m_Logger.info("Start testing correctness of " + operationName());
			INestedWordAutomatonOldApi<LETTER, STATE> operandOldApi = ResultChecker.getOldApiNwa(m_Services, m_Operand);

			// should have same number of states as old determinization
			INestedWordAutomatonOldApi<LETTER, STATE> resultDD = 
					(new DeterminizeDD<LETTER, STATE>(m_Services, sf, operandOldApi)).getResult();
			correct &= (resultDD.size() == m_Result.size());
			// should recognize same language as old computation
			correct &= (ResultChecker.nwaLanguageInclusion(m_Services, resultDD, m_Result, sf) == null);
			correct &= (ResultChecker.nwaLanguageInclusion(m_Services, m_Result, resultDD, sf) == null);
			if (!correct) {
				ResultChecker.writeToFileIfPreferred(m_Services, operationName() + "Failed", "", m_Operand);
			}
		m_Logger.info("Finished testing correctness of " + operationName());
		} else {
			m_Logger.warn("result was not tested");
		}
		return correct;
	}
	
	
}

