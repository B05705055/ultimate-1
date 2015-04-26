/*
 * Copyright (C) 2009-2014 University of Freiburg
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
 * along with the ULTIMATE Automata Library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Automata Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE Automata Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonOldApi;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedRun;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;

/**
 * Operation that checks if the language of the first operand is included in the
 * language of the second automaton.
 * @author heizmann@informatik.uni-freiburg.de
 *
 * @param <LETTER>
 * @param <STATE>
 */
public class IsIncluded<LETTER, STATE> implements IOperation<LETTER,STATE> {
	
	private final IUltimateServiceProvider m_Services;
	private final Logger m_Logger;
	
	private final INestedWordAutomatonOldApi<LETTER, STATE> m_Operand1;
	private final INestedWordAutomatonOldApi<LETTER, STATE> m_Operand2;
	
	private final Boolean m_Result;
	private final NestedRun<LETTER, STATE> m_Counterexample;
	
	
	public IsIncluded(IUltimateServiceProvider services,
			StateFactory<STATE> stateFactory,
			INestedWordAutomatonOldApi<LETTER, STATE> nwa1, 
			INestedWordAutomatonOldApi<LETTER, STATE> nwa2) throws AutomataLibraryException {
		m_Services = services;
		m_Logger = m_Services.getLoggingService().getLogger(LibraryIdentifiers.s_LibraryID);
		m_Operand1 = nwa1;
		m_Operand2 = nwa2;
		m_Logger.info(startMessage());
		IsEmpty<LETTER, STATE> emptinessCheck = new IsEmpty<LETTER, STATE>(
				services, (new Difference<LETTER, STATE>(m_Services, stateFactory, nwa1, nwa2)).getResult());
		m_Result = emptinessCheck.getResult();
		m_Counterexample = emptinessCheck.getNestedRun();
		m_Logger.info(exitMessage());
	}

	@Override
	public String operationName() {
		return "isIncluded";
	}

	@Override
	public String startMessage() {
			return "Start " + operationName() + ". Operand1 " + 
					m_Operand1.sizeInformation() + ". Operand2 " + 
					m_Operand2.sizeInformation();	
	}

	@Override
	public String exitMessage() {
		return "Finished " + operationName() + ". Language is " 
				+ (m_Result ? "" : "not ") + "included";
	}

	@Override
	public Boolean getResult() throws AutomataLibraryException {
		return m_Result;
	}

	public NestedRun<LETTER, STATE> getCounterexample() {
		return m_Counterexample;
	}

	@Override
	public boolean checkResult(StateFactory<STATE> stateFactory)
			throws AutomataLibraryException {
		return true;
	}
	


}
