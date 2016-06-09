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
package de.uni_freiburg.informatik.ultimate.automata.petrinet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class Place<S,C> implements Serializable {
	private static final long serialVersionUID = -4577818193149596161L;

	private final int mHashCode;
	
	static int s_SerialNumberCounter = 0;
	
	private final C mContent;
	private final ArrayList<ITransition<S,C>> mPredecessors;
	private final ArrayList<ITransition<S,C>> mSuccessors;
	
	private final int mSerialNumber = s_SerialNumberCounter++;
	
	
	
	public Place(C content) {
		this.mContent = content;
		this.mPredecessors = new ArrayList<ITransition<S,C>>();
		this.mSuccessors = new ArrayList<ITransition<S,C>>();
		mHashCode = computeHashCode();
	}
	
	public C getContent() {
		return mContent;
	}
	
	public Collection<ITransition<S, C>> getPredecessors() {
		return mPredecessors;
	}
	
	public Collection<ITransition<S, C>> getSuccessors() {
		return mSuccessors;
	}
	
	public void addPredecessor(ITransition<S,C> transition) {
		mPredecessors.add(transition);
	}
	
	public void addSuccessor(ITransition<S,C> transition) {
		mSuccessors.add(transition);
	}
	
	@Override
	public String toString() {
		return String.valueOf(mContent);
	}
	
	public String toStringWithSerial() {
		return "#"+ mSerialNumber + "#" + String.valueOf(mContent);
	}

	@Override
	public int hashCode() {
		return mHashCode;
	}
	
	public int computeHashCode() {
		return mSerialNumber;
	}
	
}
