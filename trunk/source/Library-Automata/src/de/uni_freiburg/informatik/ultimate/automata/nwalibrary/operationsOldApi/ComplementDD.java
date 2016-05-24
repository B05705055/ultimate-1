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
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonOldApi;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.IsEmpty;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;

public class ComplementDD<LETTER, STATE> implements IOperation<LETTER, STATE> {

	private final AutomataLibraryServices mServices;
	private final ILogger mLogger;

	protected INestedWordAutomatonOldApi<LETTER, STATE> mOperand;
	protected INestedWordAutomatonOldApi<LETTER, STATE> mDeterminizedOperand;
	protected INestedWordAutomatonOldApi<LETTER, STATE> mResult;

	@Override
	public String operationName() {
		return "complementDD";
	}

	@Override
	public String startMessage() {
		return "Start " + operationName() + " Operand "
				+ mOperand.sizeInformation();
	}

	@Override
	public String exitMessage() {
		return "Finished " + operationName() + " Result "
				+ mResult.sizeInformation();
	}

	public INestedWordAutomatonOldApi<LETTER, STATE> getResult()
			throws AutomataLibraryException {
		return mResult;
	}

	public ComplementDD(AutomataLibraryServices services,
			StateFactory<STATE> stateFactory,
			INestedWordAutomatonOldApi<LETTER, STATE> operand)
			throws AutomataLibraryException {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		mOperand = operand;

		mLogger.info(startMessage());
		if (!mOperand.isDeterministic()) {
			mDeterminizedOperand = 
				   (new DeterminizeDD<LETTER, STATE>(mServices, stateFactory, mOperand)).getResult();
		} else {
			mDeterminizedOperand = mOperand;
			mLogger.debug("Operand is already deterministic");
		}
		mResult = new ReachableStatesCopy<LETTER, STATE>(
				mServices, mDeterminizedOperand, true, true, false, false).getResult();
		mLogger.info(exitMessage());
	}

	@Override
	public boolean checkResult(StateFactory<STATE> stateFactory)
			throws AutomataLibraryException {
		mLogger.debug("Testing correctness of complement");
		boolean correct = true;
		INestedWordAutomatonOldApi intersectionOperandResult = (new IntersectDD(mServices, false, mOperand, mResult)).getResult();
		correct &=  ((new IsEmpty(mServices, intersectionOperandResult)).getResult() == true);
		mLogger.debug("Finished testing correctness of complement");
		return correct;
	}

}
