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
import de.uni_freiburg.informatik.ultimate.automata.AutomatonDefinitionPrinter;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.BinaryNwaOperation;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonSimple;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.IntersectDD;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.reachableStatesAutomaton.NestedWordAutomatonReachableStates;


public class Intersect<LETTER,STATE>
		extends BinaryNwaOperation<LETTER, STATE>
		implements IOperation<LETTER,STATE> {

	private final IntersectNwa<LETTER, STATE> mIntersect;
	private final NestedWordAutomatonReachableStates<LETTER,STATE> mResult;
	private final StateFactory<STATE> mStateFactory;
	
	/**
	 * @param services Ultimate services
	 * @param fstOperand first operand
	 * @param sndOperand second operand
	 * @throws AutomataLibraryException if construction fails
	 */
	public Intersect(final AutomataLibraryServices services,
			final INestedWordAutomatonSimple<LETTER,STATE> fstOperand,
			final INestedWordAutomatonSimple<LETTER,STATE> sndOperand)
					throws AutomataLibraryException {
		super(services, fstOperand, sndOperand);
		mStateFactory = mFstOperand.getStateFactory();
		mLogger.info(startMessage());
		mIntersect = new IntersectNwa<LETTER, STATE>(mFstOperand, mSndOperand, mStateFactory, false);
		mResult = new NestedWordAutomatonReachableStates<LETTER, STATE>(mServices, mIntersect);
		mLogger.info(exitMessage());
	}
	
	
	@Override
	public String operationName() {
		return "intersect";
	}
	
	
	@Override
	public String exitMessage() {
		return "Finished " + operationName() + " Result "
				+ mResult.sizeInformation();
	}
	

	@Override
	public INestedWordAutomaton<LETTER, STATE> getResult()
			throws AutomataLibraryException {
		return mResult;
	}

	
	@Override
	public boolean checkResult(final StateFactory<STATE> sf) throws AutomataLibraryException {
		mLogger.info("Start testing correctness of " + operationName());
		final INestedWordAutomaton<LETTER, STATE> resultDD =
				(new IntersectDD<LETTER, STATE>(mServices, mFstOperand, mSndOperand)).getResult();
		boolean correct = true;
		correct &= (resultDD.size() == mResult.size());
		assert correct;
		correct &= new IsIncluded<>(mServices, sf, resultDD, mResult).getResult();
		assert correct;
		correct &= new IsIncluded<>(mServices, sf, mResult, resultDD).getResult();
		assert correct;
		if (!correct) {
			AutomatonDefinitionPrinter.writeToFileIfPreferred(mServices,
					operationName() + "Failed", "language is different",
					mFstOperand, mSndOperand);
		}
		mLogger.info("Finished testing correctness of " + operationName());
		return correct;
	}
}
