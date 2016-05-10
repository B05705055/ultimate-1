/*
 * Copyright (C) 2008-2015 Jochen Hoenicke (hoenicke@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE BoogiePreprocessor plug-in.
 * 
 * The ULTIMATE BoogiePreprocessor plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE BoogiePreprocessor plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE BoogiePreprocessor plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE BoogiePreprocessor plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE BoogiePreprocessor plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.boogie.type;

import java.util.ArrayList;

import de.uni_freiburg.informatik.ultimate.boogie.ast.ASTType;
import de.uni_freiburg.informatik.ultimate.models.ILocation;

public class PrimitiveType extends BoogieType {
	/**
	 * long serialVersionUID
	 */
	private static final long serialVersionUID = -1842658349197318623L;
	public static final int BOOL  = -1;
	public static final int INT   = -2;
	public static final int REAL  = -3;
	public static final int ERROR = -42;
	
	/**
	 * The type code.  If this is >= 0, this is the length and the class
	 * represents a bit vector type of this length.  Otherwise, this is 
	 * one of BOOL, INT, REAL, or ERROR. 
	 */
	private final int type;
	
	PrimitiveType(int type) {
		this.type = type;
	}

	//@Override
	public BoogieType getUnderlyingType() {
		return this;
	}

	//@Override
	protected boolean hasPlaceholder(int minDepth, int maxDepth) {
		return false;
	}

	//@Override
	protected BoogieType incrementPlaceholders(int depth, int incDepth) {
		return this;
	}

	//@Override
	protected boolean isUnifiableTo(int depth, BoogieType other,
			ArrayList<BoogieType> subst) {
		if (other instanceof PlaceholderType)
			return other.isUnifiableTo(depth, this, subst);
		return this == errorType || other == errorType || this == other;
	}

	//@Override
	protected BoogieType substitutePlaceholders(int depth,
			BoogieType[] substType) {
		return this;
	}

	//@Override
	protected String toString(int depth, boolean needParentheses) {
		switch (type) {
		case INT:
			return "int";
		case BOOL:
			return "bool";
		case REAL:
		    return "real";
		case ERROR:
			return "*type-error*";
		default:
			return "bv"+type;
					
		}
	}
	
	@Override
	protected ASTType toASTType(ILocation loc, int depth) {
		return new de.uni_freiburg.informatik.ultimate.boogie.ast.
			PrimitiveType(loc, this, toString(depth, false));
	}
	
	//@Override
	protected boolean unify(int depth, BoogieType other,
			BoogieType[] substitution) {
		return this == errorType || other == errorType || this == other;
	}

	public int getTypeCode() {
		return type;
	}

	public boolean isFinite() {
		/* Everything except INT may be finite */
		return type != INT;
	}
}
