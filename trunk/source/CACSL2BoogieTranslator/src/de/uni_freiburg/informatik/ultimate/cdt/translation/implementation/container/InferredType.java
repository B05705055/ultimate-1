/*
 * Copyright (C) 2013-2015 Alexander Nutz (nutz@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 Markus Lindenmann (lindenmm@informatik.uni-freiburg.de)
 * Copyright (C) 2013-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE CACSL2BoogieTranslator plug-in.
 * 
 * The ULTIMATE CACSL2BoogieTranslator plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE CACSL2BoogieTranslator plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE CACSL2BoogieTranslator plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE CACSL2BoogieTranslator plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE CACSL2BoogieTranslator plug-in grant you additional permission
 * to convey the resulting work.
 */
/**
 * Container object to store inferred types.
 * TODO : add a reference to the corresponding CType. That would make type
 * checking during translation easier!
 */
package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container;

import de.uni_freiburg.informatik.ultimate.boogie.ast.ASTType;
import de.uni_freiburg.informatik.ultimate.boogie.ast.ArrayType;
import de.uni_freiburg.informatik.ultimate.boogie.ast.NamedType;
import de.uni_freiburg.informatik.ultimate.boogie.ast.StructType;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.SFO;
import de.uni_freiburg.informatik.ultimate.core.model.models.IBoogieType;

/**
 * @author Markus Lindenmann
 * @date 16.06.2012
 */
public class InferredType implements IBoogieType {
    /**
     * The serial version UID.
     */
    private static final long serialVersionUID = 4825293857474339818L;
    /**
     * The held type.
     */
    private Type type;

    /**
     * @author Markus Lindenmann
     * @date 16.06.2012
     */
    public enum Type {
        /**
         * Integer.
         */
        Integer,
        /**
         * Boolean.
         */
        Boolean,
        /**
         * Reals.
         */
        Real,
        /**
         * Strings.
         */
        String,
        /**
         * Cannot be inferred or not yet inferred.
         */
        Unknown,
        /**
         * Void type!
         */
        Void,
        /**
         * Struct type.
         */
        Struct,
        /**
         * Pointer type.
         */
        Pointer
    }

    /**
     * Constructor.
     * 
     * @param type
     *            the type.
     */
    public InferredType(final Type type) {
        this.type = type;
    }

    /**
     * Constructor.
     * 
     * @param at
     *            the primitive type to convert.
     */
    public InferredType(final ASTType at) {
        if (isPointerType(at)) {
            type = Type.Pointer;
        } else if (at instanceof ArrayType) {
            final InferredType it = new InferredType(((ArrayType) at).getValueType());
            type = it.getType();
        } else if (at instanceof StructType) {
            type = Type.Struct;
        } else { // Primitive Type
            if (at == null) {
                type = Type.Unknown;
            } else {
                String s = at.toString();
                s = s.replaceFirst(at.getClass().getSimpleName(), SFO.EMPTY);
                s = s.replace("[", SFO.EMPTY);
                s = s.replace("]", SFO.EMPTY);
                if (s.equals(SFO.BOOL)) {
                    type = Type.Boolean;
                } else if (s.equals(SFO.INT)) {
                    type = Type.Integer;
                } else if (s.equals(SFO.REAL)) {
                    type = Type.Real;
                } else {
                    type = Type.Unknown;
                }
            }
        }
    }
    
    public static boolean isPointerType(final ASTType at) {
    	if (at instanceof NamedType) {
    		return ((NamedType) at).getName().equals(SFO.POINTER);
    	} else {
    		return false;
    	}
    }
    
//    public InferredType(CType cType) {
//    	CType underlyingType = cType;
//    	if (underlyingType instanceof CNamed)
//    		underlyingType = ((CNamed) cType).getUnderlyingType();
//
//		if (underlyingType instanceof CPrimitive) {
//			CPrimitive cp = (CPrimitive) underlyingType;
//			switch (cp.getType()) {
//			case INT:
//				type = Type.Integer;
//				break;
//			case FLOAT:
//			case DOUBLE:
//				type = Type.Real;
//				break;
//			case CHAR:
//				type = Type.String;
//				break;
//			default:
//				throw new UnsupportedSyntaxException(null , "..");
//			}
//		} else if (underlyingType instanceof CPointer) {
//			type = Type.Pointer;
//		} else if (underlyingType instanceof CArray) {
//			type = Type.Pointer;
//		} else if (underlyingType instanceof CEnum) {
//			type = Type.Integer;
//		} else if (underlyingType instanceof CStruct) {
//			type = Type.Struct;
//		} else if (underlyingType instanceof CNamed) {
//			assert false : "This should not be the case as we took the underlying type.";
//		} else {
//			throw new UnsupportedSyntaxException(null, "..");
//		}
//    }

    /**
     * Returns the type.
     * 
     * @return the type.
     */
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        switch (getType()) {
            case Boolean:
                return SFO.BOOL;
            case Integer:
                return SFO.INT;
            case Real:
                return SFO.REAL;
            case String:
                return SFO.STRING;
            case Pointer:
                return SFO.POINTER;
            case Struct:
            	return Type.Struct.toString();
            case Unknown:
            default:
                break;
        }
        return SFO.UNKNOWN;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final InferredType other = (InferredType) obj;
		return type == other.type;
	}
    
    
}
