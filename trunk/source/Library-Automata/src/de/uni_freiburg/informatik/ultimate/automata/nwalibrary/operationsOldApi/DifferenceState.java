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
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi;

import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.IStateDeterminizer;



/**
 * State of an NWA that accepts the language difference of two NWAs.
 * A DifferenceState is a pair whose first entry is a state of the minuend, the
 * second entry is a DeterminizedState of the subtrahend. A DifferenceState is
 * final iff the minuend state is final and the subtrahend state is not final. 
 * 
 * @author heizmann@informatik.uni-freiburg.de
 *
 * @param <LETTER> Symbol
 * @param <STATE> Content
 */
	public class DifferenceState<LETTER,STATE> {
		private final STATE mMinuendState;
		private final DeterminizedState<LETTER,STATE> subtrahendDeterminizedState;
		private final boolean isFinal;
		private final int mHashCode;
		private STATE mState;
		
		
		public DifferenceState(	
				STATE minuendState, 
				DeterminizedState<LETTER,STATE> subtrahendDeterminizedState,
				boolean isFinal) {
			
			this.mMinuendState = minuendState;
			this.subtrahendDeterminizedState = subtrahendDeterminizedState;
			this.isFinal = isFinal; 
		//			minuend.isFinal(minuendState) &&
		//								!subtrahendDeterminizedState.containsFinal();
			this.mHashCode = computehashCode();
		}
		
		public STATE getMinuendState() {
			return mMinuendState;
		}

		public DeterminizedState<LETTER,STATE> getSubtrahendDeterminizedState() {
			return subtrahendDeterminizedState;
		}

		public boolean isFinal() {
			return this.isFinal;
		}
		
		public STATE getState(StateFactory<STATE> stateFactory, 
				IStateDeterminizer<LETTER, STATE> stateDeterminizer) {
			if (mState == null) {
				mState = stateFactory.intersection(
						this.getMinuendState(),
						stateDeterminizer.getState(getSubtrahendDeterminizedState()));
			} 
			return mState;
		}


		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DifferenceState other = (DifferenceState) obj;
			if (isFinal != other.isFinal)
				return false;
			if (mMinuendState == null) {
				if (other.mMinuendState != null)
					return false;
			} else if (!mMinuendState.equals(other.mMinuendState))
				return false;
			if (subtrahendDeterminizedState == null) {
				if (other.subtrahendDeterminizedState != null)
					return false;
			} else if (!subtrahendDeterminizedState
					.equals(other.subtrahendDeterminizedState))
				return false;
			return true;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return mHashCode;
		}
		
		public int computehashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (isFinal ? 1231 : 1237);
			result = prime
					* result
					+ ((mMinuendState == null) ? 0 : mMinuendState.hashCode());
			result = prime
					* result
					+ ((subtrahendDeterminizedState == null) ? 0
							: subtrahendDeterminizedState.hashCode());
			return result;
		}
		
		@Override
		public String toString() {
			return "<[< " + mMinuendState.toString() + " , "
					+ subtrahendDeterminizedState.toString() + ">]>";
		}
	}
