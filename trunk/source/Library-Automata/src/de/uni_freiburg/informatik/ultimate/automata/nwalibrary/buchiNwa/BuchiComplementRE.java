/*
 * Copyright (C) 2012-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.buchiNwa;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.ResultChecker;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonOldApi;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.PowersetDeterminizer;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.DeterminizeUnderappox;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.ReachableStatesCopy;

public class BuchiComplementRE<LETTER,STATE> implements IOperation<LETTER,STATE> {

	private final AutomataLibraryServices m_Services;
	private final Logger m_Logger;

	private INestedWordAutomatonOldApi<LETTER,STATE> m_Operand;
	private INestedWordAutomatonOldApi<LETTER,STATE> m_Result;
	
	private boolean m_buchiComplementREApplicable;
	
	@Override
	public String operationName() {
		return "buchiComplementRE";
	}

	@Override
	public String startMessage() {
		return "Start " + operationName() + " Operand " + 
			m_Operand.sizeInformation();
	}

	@Override
	public String exitMessage() {
		if (m_buchiComplementREApplicable) {
			return "Finished " + operationName() + " Result " + 
				m_Result.sizeInformation();
		} else {
			return "Unable to perform " + operationName() + "on this input";
		}
	}

	@Override
	public INestedWordAutomatonOldApi<LETTER,STATE> getResult() throws AutomataLibraryException {
		if (m_buchiComplementREApplicable) {
			return m_Result;
		} else {
			assert m_Result == null;
			throw new UnsupportedOperationException("Operation was not applicable");
		}
	}
	
	
	public BuchiComplementRE(AutomataLibraryServices services,
			StateFactory<STATE> stateFactory,
			INestedWordAutomatonOldApi<LETTER,STATE> operand) throws AutomataLibraryException {
		m_Services = services;
		m_Logger = m_Services.getLoggingService().getLogger(LibraryIdentifiers.s_LibraryID);
		m_Operand = operand;
		m_Logger.info(startMessage());
		INestedWordAutomatonOldApi<LETTER,STATE> operandWithoutNonLiveStates = 
				(new ReachableStatesCopy<LETTER,STATE>(m_Services, operand, false, false, false, true)).getResult();
		if (operandWithoutNonLiveStates.isDeterministic()) {
			m_Logger.info("Rüdigers determinization knack not necessary, already deterministic");
			m_Result = (new BuchiComplementDeterministic<LETTER,STATE>(m_Services, operandWithoutNonLiveStates)).getResult();
		}
		else {
			PowersetDeterminizer<LETTER,STATE> pd = 
					new PowersetDeterminizer<LETTER,STATE>(operandWithoutNonLiveStates, true, stateFactory);
			INestedWordAutomatonOldApi<LETTER,STATE> determinized = 
					(new DeterminizeUnderappox<LETTER,STATE>(m_Services, operandWithoutNonLiveStates,pd)).getResult();
			INestedWordAutomatonOldApi<LETTER,STATE> determinizedComplement =
					(new BuchiComplementDeterministic<LETTER,STATE>(m_Services, determinized)).getResult();
			INestedWordAutomatonOldApi<LETTER,STATE> intersectionWithOperand =
					(new BuchiIntersectDD<LETTER,STATE>(m_Services, operandWithoutNonLiveStates, determinizedComplement, true)).getResult();
			NestedLassoRun<LETTER,STATE> run = (new BuchiIsEmpty<LETTER,STATE>(m_Services, intersectionWithOperand)).getAcceptingNestedLassoRun();
			if (run == null) {
				m_Logger.info("Rüdigers determinization knack applicable");
				m_buchiComplementREApplicable = true;
				m_Result = determinizedComplement;
			}
			else {
				m_Logger.info("Rüdigers determinization knack not applicable");
				m_buchiComplementREApplicable = false;
				m_Result = null;
			}
		}


		
		m_Logger.info(exitMessage());
	}
	
	
	/**
	 * Return true if buchiComplementRE was applicable on the input.
	 */
	public boolean applicable() {
		return m_buchiComplementREApplicable;
	}

	@Override
	public boolean checkResult(StateFactory<STATE> stateFactory)
			throws AutomataLibraryException {
		return ResultChecker.buchiComplement(m_Services, m_Operand, m_Result);
	}

}
