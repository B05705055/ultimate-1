/*
 * Copyright (C) 2011-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE TraceAbstraction plug-in.
 * 
 * The ULTIMATE TraceAbstraction plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE TraceAbstraction plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE TraceAbstraction plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE TraceAbstraction plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE TraceAbstraction plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates;

import java.util.Set;

import de.uni_freiburg.informatik.ultimate.boogie.BoogieVar;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ProgramPoint;

public class UnknownState implements ISLPredicate {

//	private static final long serialVersionUID = 9190582215913478152L;
	private final ProgramPoint mProgramPoint;
	private final int mSerialNumber;
	private final Term mTerm;
	
	protected UnknownState(ProgramPoint programPoint, int serialNumber, Term term) {
		mProgramPoint = programPoint;
		mSerialNumber = serialNumber;
		mTerm = term;
		
//		super(programPoint, serialNumber, new String[0], term, null, null);
	}
	
	/**
	 * The published attributes.  Update this and getFieldValue()
	 * if you add new attributes.
	 */
	private final static String[] s_AttribFields = {
		"ProgramPoint", "isUnknown"
	};
	
//	@Override
//	protected String[] getFieldNames() {
//		return s_AttribFields;
//	}

//	@Override
//	protected Object getFieldValue(String field) {
//		if (field == "ProgramPoint")
//			return mProgramPoint;
//		else if (field == "isUnknown")
//			return true;
//		else
//			throw new UnsupportedOperationException("Unknown field "+field);
//	}
	
	@Override
	public String toString() {
		String result = mSerialNumber + "#";
		if (mProgramPoint != null) {
			result += mProgramPoint.getPosition();
		}
		else {
			result += "unknown";
		}
		return result;
	}
	
	@Override
	public int hashCode() {
		return mSerialNumber;
	}
	
	
	public ProgramPoint getProgramPoint() {
		return mProgramPoint;
	}
	
	
	@Override
	public Term getFormula() {
		return mTerm;
	}

	@Override
	public Set<BoogieVar> getVars() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String[] getProcedures() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Term getClosedFormula() {
		throw new UnsupportedOperationException();
	}





}
