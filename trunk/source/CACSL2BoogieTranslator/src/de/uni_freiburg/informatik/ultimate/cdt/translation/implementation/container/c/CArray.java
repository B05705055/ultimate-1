/*
 * Copyright (C) 2013-2015 Alexander Nutz (nutz@informatik.uni-freiburg.de)
 * Copyright (C) 2013-2015 Christian Schilling (schillic@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 Markus Lindenmann (lindenmm@informatik.uni-freiburg.de)
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
 * Describes an array given in C.
 */
package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.CACSLLocation;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.LocationFactory;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.InferredType;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.PRIMITIVE;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.exception.IncorrectSyntaxException;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.exception.UnsupportedSyntaxException;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.RValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.SFO;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssertStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BinaryExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BinaryExpression.Operator;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BooleanLiteral;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.IdentifierExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.IntegerLiteral;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.UnaryExpression;
import de.uni_freiburg.informatik.ultimate.result.Check;
import de.uni_freiburg.informatik.ultimate.util.HashUtils;

/**
 * @author Markus Lindenmann
 * @date 18.09.2012
 */
public class CArray extends CType {
    /**
     * Array dimensions.
     */
    private final RValue[] dimensions;
    /**
     * Array type.
     */
    private final CType valueType;

    private boolean isOnHeap = false;;
    
    /**
     * Constructor.
     * 
     * @param dimensions
     *            the dimensions of this array.
     * @param valueType
     *            the type of the array.
     * @param cDeclSpec
     *            the C declaration used.
     */
    public CArray(RValue[] dimensions,
            CType valueType) {
        super(false, false, false, false); //FIXME: integrate those flags
        this.dimensions = dimensions;
        this.valueType = valueType;
//        this.variableLength = false;
    }
    
    /**
     * @return the dimensions
     */
    public RValue[] getDimensions() {
        return dimensions.clone();
    }

    /**
     * @return the valueType
     */
    public CType getValueType() {
        return valueType;
    }

//    /**
//     * Generates and returns assert statements for an array access, checking the
//     * indices to be smaller then the size of the declared array.
//     * 
//     * @param loc
//     *            the location of the access, annotated with Check.
//     * @param accessedIndices
//     *            the indices that are being accessed
//     * @return an assert statement.
//     */
//    public AssertStatement getAccessAsserts(CACSLLocation loc,
//            Expression[] accessedIndices) {
//        if (dimensions.length <= 0
//                || accessedIndices.length != dimensions.length) {
//            String msg = "Invalid array access! Too many or too few dimensions!";
//            throw new IncorrectSyntaxException(loc, msg);
//        }
//        Expression int0 = new IntegerLiteral(loc, new InferredType(
//                InferredType.Type.Integer), SFO.NR0);
//        Expression conjunction = null;
//        for (int i = 0; i < dimensions.length; i++) {
//            Expression inner;
//            // idx < dimSize
//            inner = new BinaryExpression(loc, BinaryExpression.Operator.COMPLT,
//                    accessedIndices[i], dimensions[i]);
//            // dimSize > 0
//            inner = new BinaryExpression(loc, Operator.LOGICAND, inner,
//                    new BinaryExpression(loc, BinaryExpression.Operator.COMPGT,
//                            dimensions[i], int0));
//            // idx >= 0
//            inner = new BinaryExpression(loc, Operator.LOGICAND, inner,
//                    new BinaryExpression(loc,
//                            BinaryExpression.Operator.COMPGEQ,
//                            accessedIndices[i], int0));
//            if (conjunction == null) {
//                conjunction = inner;
//            } else {
//                conjunction = new BinaryExpression(loc, Operator.LOGICAND,
//                        conjunction, inner);
//            }
//        }
//        if (conjunction == null) {
//            conjunction = new BooleanLiteral(loc, true);
//        }
//        Check check = new Check(Check.Spec.ARRAY_INDEX);
//        AssertStatement assertStmt = new AssertStatement(
//                LocationFactory.createLocation(loc, check), conjunction);
//        check.addToNodeAnnot(assertStmt);
//        return assertStmt;
//    }

    @Override
    public String toString() {
        StringBuilder id = new StringBuilder("ARRAY#");
        StringBuilder dimString = new StringBuilder("_");
        for (RValue rvalueDim : getDimensions()) {
        	Expression dim = rvalueDim.getValue(); 
            if (dim instanceof BinaryExpression ||
                    dim instanceof UnaryExpression) {
            	// 2015-11-08 Matthias: Use C representation or introduce a factory
            	// for types.
            	dimString.append(dim.toString());
//                dim = getArithmeticResultAsIntegerLiteral(dim);
            }
            if (dim instanceof IntegerLiteral) {
                dimString.append(((IntegerLiteral) dim).getValue());
                dimString.append("_");
            } else if (dim instanceof IdentifierExpression) {
            	dimString.append(((IdentifierExpression) dim).getIdentifier());
                dimString.append("_");
            } else {
//                dimString = new StringBuilder("variableLength");
                dimString.append("variableLength");
                dimString.append("_");
                break;
            }
        }
        
//        if (variableLength) {
//        if (this.isVariableLength()) {
//        	id.append("_VARLENGTH");
//        }
//        id.append(getDimensions().length);
        id.append(dimString.toString());
        id.append("~");
        id.append(valueType.toString());
        id.append("#");
        return id.toString();
    }
    
//    public boolean isVariableLength() {
//    	boolean varL = false;
//    	for (Expression sizeEx : this.dimensions) {
//    		varL |= !(sizeEx instanceof IntegerLiteral);
//    	}
//    	return varL;
////    	return this.variableLength;
//    }
    
    /**
     * Computes the result of an integer arithmetic expression and
     * returns an the IntegerLiteral.
     * 
     * @param loc location
     * @param e arithmetic expression in the integers
     * @return expression of the resulting integer
     */
    private IntegerLiteral getArithmeticResultAsIntegerLiteral(Expression e) {
        assert (e instanceof UnaryExpression || e instanceof BinaryExpression);
        return new IntegerLiteral(e.getLocation(),
                Integer.toString(getArithmeticResultAsInteger(e)));
    }
    
    /**
     * Helper method for the computation of an arithmetic result from
     * expressions.
     * 
     * @param e expression (unary or binary)
     * @return the result as an int
     */
    private int getArithmeticResultAsInteger(Expression e) {
        if (e instanceof IntegerLiteral) {
            return Integer.parseInt(((IntegerLiteral)e).getValue());
        }
        assert (e instanceof UnaryExpression || e instanceof BinaryExpression);
        if (e instanceof BinaryExpression) {
            BinaryExpression be = (BinaryExpression)e;
            BinaryExpression.Operator operator = be.getOperator();
            int left = getArithmeticResultAsInteger(be.getLeft());
            int right = getArithmeticResultAsInteger(be.getRight());
            if (operator.equals(Operator.ARITHPLUS)) {
                return left + right;
            }
            else if (operator.equals(Operator.ARITHMINUS)) {
                return left - right;
            }
            else if (operator.equals(Operator.ARITHMUL)) {
                return left * right;
            }
            else if (operator.equals(Operator.ARITHDIV)) {
                return left / right;
            }
            else if (operator.equals(Operator.ARITHMOD)) {
                return left % right;
            }
            else {
                throw new UnsupportedSyntaxException(e.getLocation(),
                        "arithmetic expression with operator " + operator);
            }
        } else {
            UnaryExpression ue = (UnaryExpression)e;
            UnaryExpression.Operator operator = ue.getOperator();
            if (! operator.equals(UnaryExpression.Operator.ARITHNEGATIVE)) {
                throw new UnsupportedSyntaxException(e.getLocation(),
                        "arithmetic expression with operator " + operator);
            }
            return 0 - getArithmeticResultAsInteger(ue.getExpr());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CType)) {
            return false;
        }
        CType oType = ((CType)o).getUnderlyingType();
        if (!(oType instanceof CArray)) {
            return false;
        }
        
        CArray oArr = (CArray)oType;
        if (!(valueType.equals(oArr.valueType))) {
            return false;
        }
        if (dimensions.length != oArr.dimensions.length) {
            return false;
        }
        for (int i = dimensions.length - 1; i >= 0; --i) {
            if (!(dimensions[i].equals(oArr.dimensions[i]))) {
                return false;
            }
        }
        return true;
    }

	@Override
	public boolean isCompatibleWith(CType o) {
		if (o instanceof CPrimitive &&
				((CPrimitive) o).getType() == PRIMITIVE.VOID)
			return true;
		
        CType oType = ((CType)o).getUnderlyingType();
        if (!(oType instanceof CArray))
            return false;
        
        CArray oArr = (CArray) oType;
        if (!(valueType.isCompatibleWith(oArr.valueType))) {
            return false;
        }
        if (dimensions.length != oArr.dimensions.length) {
            return false;
        }
        for (int i = dimensions.length - 1; i >= 0; --i) {
            if (!(dimensions[i].equals(oArr.dimensions[i]))) {
                return false;
            }
        }
        return true;
	}
	
	@Override
	public int hashCode() {
//		return HashUtils.hashJenkins(31, dimensions, valueType, variableLength);
		return HashUtils.hashJenkins(31, dimensions, valueType);
	}
}
