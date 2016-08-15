/*
 * Copyright (C) 2016 Christian Schilling (schillic@informatik.uni-freiburg.de)
 * Copyright (C) 2016 University of Freiburg
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
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingReturnTransition;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;

/**
 * Checks whether an NWA is total.
 * 
 * An NWA is total if for each state and symbol there is an outgoing transition.
 * For return transitions, we require that for each hierarchical predecessor
 * there is a transition with each return symbol.
 * 
 * @author Christian Schilling (schillic@informatik.uni-freiburg.de)
 * @param <LETTER> letter type
 * @param <STATE> state type
 */
public class IsTotal<LETTER, STATE> implements IOperation<LETTER, STATE> {
	private final AutomataLibraryServices mServices;
	private final ILogger mLogger;
	private final INestedWordAutomaton<LETTER, STATE> mOperand;
	private final boolean mResult;
	
	/**
	 * @param operand input NWA
	 */
	public IsTotal(final AutomataLibraryServices services,
			final INestedWordAutomaton<LETTER, STATE> operand) {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		mOperand = operand;
		mResult = isTotal();
		mLogger.info("automaton is " + (mResult ? "" : "not ") + "total");
	}
	
	/**
	 * @return true iff automaton is total according to contract
	 */
	private boolean isTotal() {
		for (final STATE state : mOperand.getStates()) {
			for (final LETTER symbol : mOperand.getInternalAlphabet()) {
				final Iterable<OutgoingInternalTransition<LETTER, STATE>> it =
					mOperand.internalSuccessors(state, symbol);
				if (!it.iterator().hasNext()) {
					return false;
				}
			}
			
			for (final LETTER symbol : mOperand.getCallAlphabet()) {
				final Iterable<OutgoingCallTransition<LETTER, STATE>> it =
					mOperand.callSuccessors(state, symbol);
				if (!it.iterator().hasNext()) {
					return false;
				}
			}
			
			for (final LETTER symbol : mOperand.getReturnAlphabet()) {
				for (final STATE hier : mOperand.getStates()) {
					final Iterable<OutgoingReturnTransition<LETTER, STATE>> it =
						mOperand.returnSuccessors(state, hier, symbol);
					if (!it.iterator().hasNext()) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	@Override
	public String operationName() {
		return "IsTotal";
	}
	
	@Override
	public final String startMessage() {
		return "Start " + operationName();
	}
	
	@Override
	public final String exitMessage() {
		return "Finished " + operationName();
	}
	
	@Override
	public Boolean getResult() {
		return mResult;
	}
	
	@Override
	public boolean checkResult(final StateFactory<STATE> stateFactory)
			throws AutomataLibraryException {
		// TODO Auto-generated method stub
		return true;
	}
}
