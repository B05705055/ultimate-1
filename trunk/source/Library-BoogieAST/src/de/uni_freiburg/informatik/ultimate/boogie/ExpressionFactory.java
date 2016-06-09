/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2014-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE Core.
 * 
 * The ULTIMATE Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Core. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Core, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE Core grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.boogie;

import java.math.BigDecimal;
import java.math.BigInteger;

import de.uni_freiburg.informatik.ultimate.boogie.ast.BinaryExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BinaryExpression.Operator;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BitvecLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BooleanLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IfThenElseExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IntegerLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.RealLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.UnaryExpression;
import de.uni_freiburg.informatik.ultimate.core.model.models.ILocation;

/**
 * Construct Boogie Expressions and do minor simplification if all operands
 * are literals.
 * 
 * @author Matthias Heizmann
 * 
 */
public class ExpressionFactory extends BoogieTransformer {
	
	public static Expression newUnaryExpression(ILocation loc, UnaryExpression.Operator operator, Expression expr) {
		final Expression exprLiteral = filterLiteral(expr);
		Expression result;
		if (exprLiteral != null) {
			switch (operator) {
			case ARITHNEGATIVE:
				if (exprLiteral instanceof IntegerLiteral) {
					final BigInteger value = new BigInteger(((IntegerLiteral) exprLiteral).getValue());
					result = new IntegerLiteral(loc, (value.negate()).toString());
				} else if (exprLiteral instanceof RealLiteral) {
					final BigDecimal value = new BigDecimal(((RealLiteral) exprLiteral).getValue());
					result = new RealLiteral(loc, (value.negate()).toString());
				} else {
					throw new IllegalArgumentException("type error: unable to apply " + operator);
				}
				break;
			case LOGICNEG:
				if (exprLiteral instanceof BooleanLiteral) {
					final boolean value = ((BooleanLiteral) exprLiteral).getValue();
					result = new BooleanLiteral(loc, !value);
				} else {
					throw new IllegalArgumentException("type error: unable to apply " + operator);
				}
				break;
			case OLD:
				result = expr;
			default:
				throw new AssertionError("unknown operator " + operator);
			}
		} else {
			result = new UnaryExpression(loc, operator, expr);
		}
		return result;
	}

	public static Expression newBinaryExpression(ILocation loc, Operator operator, Expression left, Expression right) {
		final Expression leftLiteral = filterLiteral(left);
		final Expression rightLiteral = filterLiteral(right);
		Expression result;
		if ((leftLiteral != null) && (rightLiteral != null)) {
			assert leftLiteral.getClass().equals(rightLiteral.getClass()) : "incompatible literals: " + leftLiteral.getClass() + " and " + rightLiteral.getClass();
			if (leftLiteral instanceof BooleanLiteral) {
				result = constructBinaryExpression_Bool(loc, operator, (BooleanLiteral) leftLiteral, (BooleanLiteral) rightLiteral);
			} else if (leftLiteral instanceof IntegerLiteral) {
				result = constructBinaryExpression_Integer(loc, operator, (IntegerLiteral) leftLiteral, (IntegerLiteral) rightLiteral);
			} else if (leftLiteral instanceof RealLiteral) {
				result = constructBinaryExpression_Real(loc, operator, (RealLiteral) leftLiteral, (RealLiteral) rightLiteral);
			} else if (leftLiteral instanceof BitvecLiteral) {
				result = constructBinaryExpression_Bitvector(loc, operator, (BitvecLiteral) leftLiteral, (BitvecLiteral) rightLiteral);
			} else {
				throw new AssertionError("impossible");
			}
		} else {
			result = new BinaryExpression(loc, operator, left, right);
		}
		return result;
	}
	
	
	private static BooleanLiteral constructBinaryExpression_Bool(ILocation loc, Operator operator, BooleanLiteral leftLiteral,
			BooleanLiteral rightLiteral) {
		final boolean leftValue = leftLiteral.getValue();
		final boolean rightValue = rightLiteral.getValue();
		final boolean result;
		switch (operator) {
		case ARITHDIV:
		case ARITHMINUS:
		case ARITHMOD:
		case ARITHMUL:
		case ARITHPLUS:
		case BITVECCONCAT:
		case COMPGEQ:
		case COMPGT:
		case COMPLEQ:
		case COMPLT:
		case COMPPO:
			throw new IllegalArgumentException("type error: unable to apply " + operator + " to bool");
		case COMPEQ:
			result = (leftValue == rightValue);
			break;
		case COMPNEQ:
			result = (leftValue != rightValue);
			break;
		case LOGICAND:
			result = leftValue && rightValue;
			break;
		case LOGICIFF:
			result = (leftValue == rightValue);
			break;
		case LOGICIMPLIES:
			result = (!leftValue || rightValue);
			break;
		case LOGICOR:
			result = leftValue || rightValue;
			break;
		default:
			throw new AssertionError("unknown operator " + operator);
		}
		return new BooleanLiteral(loc, result);
	}
	
	
	private static Expression constructBinaryExpression_Integer(ILocation loc, Operator operator, IntegerLiteral leftLiteral,
			IntegerLiteral rightLiteral) {
		final BigInteger leftValue = new BigInteger(leftLiteral.getValue());
		final BigInteger rightValue = new BigInteger(rightLiteral.getValue());
		switch (operator) {
		case ARITHDIV: {
			final BigInteger result = BoogieUtils.euclideanDiv(leftValue, rightValue);
			return new IntegerLiteral(loc, result.toString());
		}
		case ARITHMINUS: {
			final BigInteger result = leftValue.subtract(rightValue);
			return new IntegerLiteral(loc, result.toString());
		}
		case ARITHMOD: {
			final BigInteger result = BoogieUtils.euclideanMod(leftValue, rightValue);
			return new IntegerLiteral(loc, result.toString());
		}
		case ARITHMUL: {
			final BigInteger result = leftValue.multiply(rightValue);
			return new IntegerLiteral(loc, result.toString());
		}
		case ARITHPLUS: {
			final BigInteger result = leftValue.add(rightValue);
			return new IntegerLiteral(loc, result.toString());
		}
		case COMPEQ: {
			final boolean result = (leftValue.compareTo(rightValue) == 0);
			return new BooleanLiteral(loc, result);
		}
		case COMPGEQ: {
			final boolean result = (leftValue.compareTo(rightValue) >= 0);
			return new BooleanLiteral(loc, result);
		}
		case COMPGT: {
			final boolean result = (leftValue.compareTo(rightValue) > 0);
			return new BooleanLiteral(loc, result);
		}
		case COMPLEQ: {
			final boolean result = (leftValue.compareTo(rightValue) <= 0);
			return new BooleanLiteral(loc, result);
		}
		case COMPLT: {
			final boolean result = (leftValue.compareTo(rightValue) < 0);
			return new BooleanLiteral(loc, result);
		}
		case COMPNEQ: {
			final boolean result = (leftValue.compareTo(rightValue) != 0);
			return new BooleanLiteral(loc, result);
		}
		case BITVECCONCAT:
		case COMPPO:
		case LOGICAND:
		case LOGICIFF:
		case LOGICIMPLIES:
		case LOGICOR:
			throw new IllegalArgumentException("type error: unable to apply " + operator + " to bool");
		default:
			throw new AssertionError("unknown operator " + operator);
		}
	}
	
	private static Expression constructBinaryExpression_Real(ILocation loc, Operator operator, RealLiteral leftLiteral,
			RealLiteral rightLiteral) {
		final BigDecimal leftValue = new BigDecimal(leftLiteral.getValue());
		final BigDecimal rightValue = new BigDecimal(rightLiteral.getValue());
		switch (operator) {
		case ARITHDIV: {
			final BigDecimal result = leftValue.divide(rightValue);
			return new RealLiteral(loc, result.toString());
		}
		case ARITHMINUS: {
			final BigDecimal result = leftValue.subtract(rightValue);
			return new RealLiteral(loc, result.toString());
		}
		case ARITHMUL: {
			final BigDecimal result = leftValue.multiply(rightValue);
			return new RealLiteral(loc, result.toString());
		}
		case ARITHPLUS: {
			final BigDecimal result = leftValue.add(rightValue);
			return new RealLiteral(loc, result.toString());
		}
		case COMPEQ: {
			final boolean result = (leftValue.compareTo(rightValue) >= 0);
			return new BooleanLiteral(loc, result);
		}
		case COMPGEQ: {
			final boolean result = (leftValue.compareTo(rightValue) >= 0);
			return new BooleanLiteral(loc, result);
		}
		case COMPGT: {
			final boolean result = (leftValue.compareTo(rightValue) >= 0);
			return new BooleanLiteral(loc, result);
		}
		case COMPLEQ: {
			final boolean result = (leftValue.compareTo(rightValue) >= 0);
			return new BooleanLiteral(loc, result);
		}
		case COMPLT: {
			final boolean result = (leftValue.compareTo(rightValue) >= 0);
			return new BooleanLiteral(loc, result);
		}
		case COMPNEQ: {
			final boolean result = (leftValue.compareTo(rightValue) >= 0);
			return new BooleanLiteral(loc, result);
		}
		case ARITHMOD:
		case BITVECCONCAT:
		case COMPPO:
		case LOGICAND:
		case LOGICIFF:
		case LOGICIMPLIES:
		case LOGICOR:
			throw new IllegalArgumentException("type error: unable to apply " + operator + " to bool");
		default:
			throw new AssertionError("unknown operator " + operator);
		}
	}


	private static Expression constructBinaryExpression_Bitvector(ILocation loc, Operator operator, BitvecLiteral leftLiteral,
			BitvecLiteral rightLiteral) {
		final BigInteger leftValue = new BigInteger(leftLiteral.getValue());
		final BigInteger rightValue = new BigInteger(rightLiteral.getValue());
		if (leftValue.compareTo(BigInteger.ZERO) < 0) {
			throw new IllegalArgumentException("assumed non-negative number here!");
		}
		if (rightValue.compareTo(BigInteger.ZERO) < 0) {
			throw new IllegalArgumentException("assumed non-negativenumber here!");
		}
		final int leftLength = leftLiteral.getLength();
		final int rightLength = rightLiteral.getLength();
		
		switch (operator) {
		case COMPEQ: {
			if (leftLength != rightLength) {
				throw new IllegalArgumentException("type error: cannot compare bitvectors of differnt lengths");
			}
			final boolean result = (leftValue.equals(rightValue));
			return new BooleanLiteral(loc, result);
		}
		case COMPNEQ: {
			if (leftLength != rightLength) {
				throw new IllegalArgumentException("type error: cannot compare bitvectors of differnt lengths");
			}
			final boolean result = !(leftValue.equals(rightValue));
			return new BooleanLiteral(loc, result);
		}
		case BITVECCONCAT: {
			throw new UnsupportedOperationException("not yet implemented " + operator);
		}
		case ARITHDIV:
		case ARITHMINUS:
		case ARITHMOD:
		case ARITHMUL:
		case ARITHPLUS:
		case COMPGEQ: 
		case COMPGT:
		case COMPLEQ:
		case COMPLT:
		case COMPPO:
		case LOGICAND:
		case LOGICIFF:
		case LOGICIMPLIES:
		case LOGICOR:
			throw new IllegalArgumentException("type error: unable to apply " + operator + " to bool");
		default:
			throw new AssertionError("unknown operator " + operator);
		}
	}
	
	public static Expression newIfThenElseExpression(ILocation loc, Expression condition, Expression thenPart, Expression elsePart) {
		final Expression condLiteral = filterLiteral(condition);
		if (condLiteral instanceof BooleanLiteral) {
			final boolean value = ((BooleanLiteral) condLiteral).getValue();
			if (value) {
				return thenPart;
			} else {
				return elsePart;
			}
		} else {
			return new IfThenElseExpression(loc, condition, thenPart, elsePart);
		}
		
	}



	private static Expression filterLiteral(Expression expr) {
		if ((expr instanceof IntegerLiteral) || 
				(expr instanceof BooleanLiteral) || 
				(expr instanceof BitvecLiteral) || 
				(expr instanceof RealLiteral)) {
			return expr;
		} else {
			return null;
		}
	}
}
