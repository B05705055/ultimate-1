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
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;

public class IntersectDD<LETTER, STATE> extends AbstractIntersect<LETTER, STATE>
		implements IOperation<LETTER, STATE> {

	public IntersectDD(final AutomataLibraryServices services,
			final INestedWordAutomaton<LETTER, STATE> fstNwa,
			final INestedWordAutomaton<LETTER, STATE> sndNwa)
			throws AutomataLibraryException {
		super(services, false, false, fstNwa, sndNwa);
	}

	public IntersectDD(
			final AutomataLibraryServices services,
			final boolean minimizeResult,
			final INestedWordAutomaton<LETTER, STATE> fstNwa,
			final INestedWordAutomaton<LETTER, STATE> sndNwa)
			throws AutomataLibraryException {
		super(services, false, minimizeResult, fstNwa, sndNwa);
	}

	@Override
	public String operationName() {
		return "intersectDD";
	}

	@Override
	public boolean checkResult(final StateFactory<STATE> stateFactory)
			throws AutomataLibraryException {
		mLogger.warn("Correctness of result was not tested");
		return true;
	}

}
