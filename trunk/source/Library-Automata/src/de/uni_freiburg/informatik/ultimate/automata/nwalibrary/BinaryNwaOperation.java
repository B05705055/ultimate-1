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
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.GeneralOperation;

/**
 * Abstract operation taking two automata as input.
 * The most common methods are provided but can also be overwritten.
 * 
 * @author Christian Schilling (schillic@informatik.uni-freiburg.de)
 * @param <LETTER>
 *            letter type
 * @param <STATE>
 *            state type
 */
public abstract class BinaryNwaOperation<LETTER, STATE>
		extends GeneralOperation<LETTER, STATE> {
	/**
	 * Input nested word automaton.
	 */
	protected final INestedWordAutomatonSimple<LETTER, STATE> mFstOperand;
	
	/**
	 * Input nested word automaton.
	 */
	protected final INestedWordAutomatonSimple<LETTER, STATE> mSndOperand;
	
	/**
	 * Constructor.
	 * 
	 * @param services
	 *            Ultimate services
	 * @param fstOperand
	 *            first operand
	 * @param sndOperand
	 *            second operand
	 */
	public BinaryNwaOperation(final AutomataLibraryServices services,
			final INestedWordAutomatonSimple<LETTER, STATE> fstOperand,
			final INestedWordAutomatonSimple<LETTER, STATE> sndOperand) {
		super(services);
		mFstOperand = fstOperand;
		mSndOperand = sndOperand;
	}
	
	@Override
	public String startMessage() {
		return "Start " + operationName() + ". First operand "
				+ mFstOperand.sizeInformation() + " Second operand "
				+ mSndOperand.sizeInformation();
	}
	
	/**
	 * @return true iff the alphabets of the two operands differ.
	 */
	protected boolean alphabetsDiffer() {
		if (!mFstOperand.getInternalAlphabet().equals(mSndOperand.getInternalAlphabet())) {
			return true;
		}
		if (!mFstOperand.getCallAlphabet().equals(mSndOperand.getCallAlphabet())) {
			return true;
		}
		if (!mFstOperand.getReturnAlphabet().equals(mSndOperand.getReturnAlphabet())) {
			return true;
		}
		return false;
	}
}
