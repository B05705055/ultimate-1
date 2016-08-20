/*
 * Copyright (C) 2010-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2009-2015 University of Freiburg
 * Copyright (C) 2010-2015 wuxio
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

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonSimple;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.reachableStatesAutomaton.AcceptingComponentsAnalysis;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.reachableStatesAutomaton.NestedWordAutomatonReachableStates;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;

/**
 * Class that provides the Buchi emptiness check for nested word automata.
 * 
 * @param <LETTER>
 *            Symbol. Type of the symbols used as alphabet.
 * @param <STATE>
 *            Content. Type of the labels (the content) of the automata states.
 * @version 2010-12-18
 */
public class BuchiIsEmpty<LETTER, STATE> implements IOperation<LETTER, STATE> {
	
	private final AutomataLibraryServices mServices;
	INestedWordAutomatonSimple<LETTER, STATE> mNwa;
	NestedWordAutomatonReachableStates<LETTER, STATE> mReach;
	AcceptingComponentsAnalysis<LETTER, STATE> mSccs;
	final Boolean mResult;
	
	public BuchiIsEmpty(final AutomataLibraryServices services,
			final INestedWordAutomatonSimple<LETTER, STATE> nwa) throws AutomataLibraryException {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		mNwa = nwa;
		mLogger.info(startMessage());
		try {
			if (mNwa instanceof NestedWordAutomatonReachableStates) {
				mReach = (NestedWordAutomatonReachableStates<LETTER, STATE>) mNwa;
			} else {
				mReach = new NestedWordAutomatonReachableStates<LETTER, STATE>(mServices, mNwa);
			}
			mSccs = mReach.getOrComputeAcceptingComponents();
			mResult = mSccs.buchiIsEmpty();
		} catch (final AutomataOperationCanceledException oce) {
			throw new AutomataOperationCanceledException(getClass());
		}
		mLogger.info(exitMessage());
	}
	
	@Override
	public String operationName() {
		return "buchiIsEmpty";
	}
	
	@Override
	public String startMessage() {
		return "Start " + operationName() + ". Operand " + mNwa.sizeInformation();
	}
	
	@Override
	public String exitMessage() {
		return "Finished " + operationName() + " Result is " + mResult;
	}
	
	@Override
	public Boolean getResult() throws AutomataLibraryException {
		return mResult;
	}
	
	private final ILogger mLogger;
	
	public NestedLassoRun<LETTER, STATE> getAcceptingNestedLassoRun() throws AutomataLibraryException {
		if (mResult) {
			mLogger.info("There is no accepting nested lasso run");
			return null;
		} else {
			mLogger.info("Starting construction of run");
			return mReach.getOrComputeAcceptingComponents().getNestedLassoRun();
		}
	}
	
	@Override
	public boolean checkResult(final StateFactory<STATE> stateFactory)
			throws AutomataLibraryException {
		return true;
	}
	
}
