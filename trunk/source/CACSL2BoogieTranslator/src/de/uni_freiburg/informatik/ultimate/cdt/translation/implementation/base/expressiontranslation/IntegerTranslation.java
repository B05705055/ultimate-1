/*
 * Copyright (C) 2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Thomas Lang
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
package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.expressiontranslation;

import java.math.BigInteger;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import de.uni_freiburg.informatik.ultimate.boogie.ExpressionFactory;
import de.uni_freiburg.informatik.ultimate.boogie.ast.ASTType;
import de.uni_freiburg.informatik.ultimate.boogie.ast.AssumeStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Attribute;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BinaryExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BinaryExpression.Operator;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.FunctionApplication;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IntegerLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.NamedAttribute;
import de.uni_freiburg.informatik.ultimate.boogie.ast.PrimitiveType;
import de.uni_freiburg.informatik.ultimate.boogie.ast.RealLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.StringLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.UnaryExpression;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.LocationFactory;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.FunctionDeclarations;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.cHandler.MemoryHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.cHandler.TypeSizes;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CEnum;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPointer;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.GENERALPRIMITIVE;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.PRIMITIVE;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CType;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.exception.UnsupportedSyntaxException;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.ExpressionResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.RValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.ISOIEC9899TC3;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.SFO;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.Dispatcher;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.ITypeHandler;
import de.uni_freiburg.informatik.ultimate.core.model.models.ILocation;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.preferences.CACSLPreferenceInitializer.PointerIntegerConversion;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.preferences.CACSLPreferenceInitializer.UnsignedTreatment;

/**
 * 
 * @author Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * @author Thomas Lang
 *
 */
public class IntegerTranslation extends AExpressionTranslation {

	private static final boolean OVERAPPROXIMATE_INT_POINTER_CONVERSION = true;

	private final UnsignedTreatment mUnsignedTreatment;

	/**
	 * Add assume statements that state that values of signed integer types are in range.
	 */
	private final boolean mAssumeThatSignedValuesAreInRange;

	public IntegerTranslation(final TypeSizes typeSizeConstants, final ITypeHandler typeHandler,
			final UnsignedTreatment unsignedTreatment, final boolean assumeSignedInRange,
			final PointerIntegerConversion pointerIntegerConversion) {
		super(typeSizeConstants, typeHandler, pointerIntegerConversion);
		mUnsignedTreatment = unsignedTreatment;
		mAssumeThatSignedValuesAreInRange = assumeSignedInRange;
	}

	@Override
	public ExpressionResult translateLiteral(Dispatcher main, IASTLiteralExpression node) {
		final ILocation loc = LocationFactory.createCLocation(node);

		switch (node.getKind()) {
		case IASTLiteralExpression.lk_char_constant:
			final String valChar = ISOIEC9899TC3.handleCharConstant(new String(node.getValue()), loc, main);
			return new ExpressionResult(new RValue(new IntegerLiteral(loc, valChar), new CPrimitive(PRIMITIVE.CHAR)));
		case IASTLiteralExpression.lk_integer_constant:
			final String valInt = new String(node.getValue());
			final RValue rVal = translateIntegerLiteral(loc, valInt);
			return new ExpressionResult(rVal);
		default:
			return super.translateLiteral(main, node);
		}
	}

	@Override
	public RValue translateIntegerLiteral(ILocation loc, String val) {
		final RValue rVal = ISOIEC9899TC3.handleIntegerConstant(val, loc, false, mTypeSizes);
		return rVal;
	}

	@Override
	public Expression constructLiteralForIntegerType(ILocation loc, CPrimitive type, BigInteger value) {
		return ISOIEC9899TC3.constructLiteralForCIntegerLiteral(loc, false, mTypeSizes, type, value);
	}

	@Override
	public RValue translateFloatingLiteral(ILocation loc, String val) {
		final RValue rVal = ISOIEC9899TC3.handleFloatConstant(val, loc, true, mTypeSizes, mFunctionDeclarations, null);
		return rVal;
	}

	@Override
	public Expression constructBinaryComparisonIntegerExpression(final ILocation loc, final int nodeOperator,
			final Expression exp1, final CPrimitive type1, final Expression exp2, final CPrimitive type2) {
		if (!type1.equals(type2)) {
			throw new IllegalArgumentException("incompatible types " + type1 + " and " + type2);
		}
		Expression leftExpr = exp1;
		Expression rightExpr = exp2;
		if (mUnsignedTreatment == UnsignedTreatment.WRAPAROUND && type1.isUnsigned()) {
			assert type2.isUnsigned();
			leftExpr = applyWraparound(loc, mTypeSizes, type1, leftExpr);
			rightExpr = applyWraparound(loc, mTypeSizes, type2, rightExpr);
		}
		BinaryExpression.Operator op;
		switch (nodeOperator) {
		case IASTBinaryExpression.op_equals:
			op = BinaryExpression.Operator.COMPEQ;
			break;
		case IASTBinaryExpression.op_greaterEqual:
			op = BinaryExpression.Operator.COMPGEQ;
			break;
		case IASTBinaryExpression.op_greaterThan:
			op = BinaryExpression.Operator.COMPGT;
			break;
		case IASTBinaryExpression.op_lessEqual:
			op = BinaryExpression.Operator.COMPLEQ;
			break;
		case IASTBinaryExpression.op_lessThan:
			op = BinaryExpression.Operator.COMPLT;
			break;
		case IASTBinaryExpression.op_notequals:
			op = BinaryExpression.Operator.COMPNEQ;
			break;
		default:
			throw new AssertionError("Unknown BinaryExpression operator " + nodeOperator);
		}

		return ExpressionFactory.newBinaryExpression(loc, op, leftExpr, rightExpr);
	}

	public static Expression applyWraparound(ILocation loc, TypeSizes typeSizes, CPrimitive cPrimitive,
			Expression operand) {
		if (cPrimitive.getGeneralType() == GENERALPRIMITIVE.INTTYPE) {
			if (cPrimitive.isUnsigned()) {
				final BigInteger maxValuePlusOne = typeSizes.getMaxValueOfPrimitiveType(cPrimitive).add(BigInteger.ONE);
				return ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.ARITHMOD, operand,
						new IntegerLiteral(loc, maxValuePlusOne.toString()));
			} else {
				throw new AssertionError("wraparound only for unsigned types");
			}
		} else {
			throw new AssertionError("wraparound only for integer types");
		}
	}

	@Override
	public Expression constructBinaryBitwiseIntegerExpression(ILocation loc, int op, Expression left,
			CPrimitive typeLeft, Expression right, CPrimitive typeRight) {
		final String funcname;
		switch (op) {
		case IASTBinaryExpression.op_binaryAnd:
		case IASTBinaryExpression.op_binaryAndAssign:
			funcname = "bitwiseAnd";
			break;
		case IASTBinaryExpression.op_binaryOr:
		case IASTBinaryExpression.op_binaryOrAssign:
			funcname = "bitwiseOr";
			break;
		case IASTBinaryExpression.op_binaryXor:
		case IASTBinaryExpression.op_binaryXorAssign:
			funcname = "bitwiseXor";
			break;
		case IASTBinaryExpression.op_shiftLeft:
		case IASTBinaryExpression.op_shiftLeftAssign:
			funcname = "shiftLeft";
			break;
		case IASTBinaryExpression.op_shiftRight:
		case IASTBinaryExpression.op_shiftRightAssign:
			funcname = "shiftRight";
			break;
		default:
			final String msg = "Unknown or unsupported bitwise expression";
			throw new UnsupportedSyntaxException(loc, msg);
		}
		declareBitvectorFunction(loc, SFO.AUXILIARY_FUNCTION_PREFIX + funcname, false, typeLeft, typeLeft, typeRight);
		final Expression func = new FunctionApplication(loc, SFO.AUXILIARY_FUNCTION_PREFIX + funcname,
				new Expression[] { left, right });
		return func;
	}

	@Override
	public Expression constructUnaryIntegerExpression(ILocation loc, int op, Expression expr, CPrimitive type) {
		switch (op) {
		case IASTUnaryExpression.op_tilde:
			return constructUnaryIntExprTilde(loc, expr, type);
		case IASTUnaryExpression.op_minus:
			return constructUnaryIntExprMinus(loc, expr, type);
		default:
			final String msg = "Unknown or unsupported bitwise expression";
			throw new UnsupportedSyntaxException(loc, msg);
		}
	}

	private Expression constructUnaryIntExprTilde(ILocation loc, Expression expr, CPrimitive type) {
		final String funcname = "bitwiseComplement";
		declareBitvectorFunction(loc, SFO.AUXILIARY_FUNCTION_PREFIX + funcname, false, type, type);
		return new FunctionApplication(loc, SFO.AUXILIARY_FUNCTION_PREFIX + funcname, new Expression[] { expr });
	}

	private Expression constructUnaryIntExprMinus(ILocation loc, Expression expr, CPrimitive type) {
		if (type.getGeneralType() == GENERALPRIMITIVE.INTTYPE) {
			return ExpressionFactory.newUnaryExpression(loc, UnaryExpression.Operator.ARITHNEGATIVE, expr);
		} else if (type.getGeneralType() == GENERALPRIMITIVE.FLOATTYPE) {
			// TODO: having boogie deal with negative real literals would be the nice solution..
			return ExpressionFactory.newBinaryExpression(loc, Operator.ARITHMINUS, new RealLiteral(loc, "0.0"),
					expr);
		} else {
			throw new IllegalArgumentException("unsupported " + type);
		}
	}

	private void declareBitvectorFunction(ILocation loc, String prefixedFunctionName, boolean boogieResultTypeBool,
			CPrimitive resultCType, CPrimitive... paramCType) {
		final String functionName = prefixedFunctionName.substring(1, prefixedFunctionName.length());
		final Attribute attribute = new NamedAttribute(loc, FunctionDeclarations.s_OVERAPPROX_IDENTIFIER,
				new Expression[] { new StringLiteral(loc, functionName) });
		final Attribute[] attributes = new Attribute[] { attribute };
		mFunctionDeclarations.declareFunction(loc, SFO.AUXILIARY_FUNCTION_PREFIX + functionName, attributes,
				boogieResultTypeBool, resultCType, paramCType);
	}

	@Override
	public Expression constructArithmeticIntegerExpression(final ILocation loc, final int nodeOperator,
			final Expression leftExp, final CPrimitive leftType, final Expression rightExp,
			final CPrimitive rightType) {
		assert leftType.getGeneralType() == GENERALPRIMITIVE.INTTYPE;
		assert rightType.getGeneralType() == GENERALPRIMITIVE.INTTYPE;

		Expression leftExpr = leftExp;
		Expression rightExpr = rightExp;
		if (leftType.isIntegerType() && leftType.isUnsigned()) {
			assert rightType.isIntegerType() && rightType.isUnsigned() : "incompatible types";
			if (nodeOperator == IASTBinaryExpression.op_divide || nodeOperator == IASTBinaryExpression.op_divideAssign
					|| nodeOperator == IASTBinaryExpression.op_modulo
					|| nodeOperator == IASTBinaryExpression.op_moduloAssign) {
				// apply wraparound to ensure that Nutz transformation is sound
				// (see examples/programs/regression/c/NutzTransformation02.c)
				leftExpr = applyWraparound(loc, mTypeSizes, leftType, leftExpr);
				rightExpr = applyWraparound(loc, mTypeSizes, rightType, rightExpr);
			}
		}
		final boolean bothAreIntegerLiterals =
				leftExpr instanceof IntegerLiteral && rightExpr instanceof IntegerLiteral;
		BigInteger leftValue = null;
		BigInteger rightValue = null;
		// TODO: add checks for UnaryExpression (otherwise we don't catch negative constants, here) --> or remove all
		// the cases
		// (if-then-else conditions are checked for being constant in RCFGBuilder anyway, so this is merely a decision
		// of readability of Boogie code..)
		if (leftExpr instanceof IntegerLiteral) {
			leftValue = new BigInteger(((IntegerLiteral) leftExpr).getValue());
		}
		if (rightExpr instanceof IntegerLiteral) {
			rightValue = new BigInteger(((IntegerLiteral) rightExpr).getValue());
		}
		// TODO: make this more general, (a + 4) + 4 may still occur this way..

		switch (nodeOperator) {
		case IASTBinaryExpression.op_minusAssign:
		case IASTBinaryExpression.op_minus:
			return constructArIntExprMinus(loc, leftExpr, rightExpr, bothAreIntegerLiterals, leftValue, rightValue);
		case IASTBinaryExpression.op_multiplyAssign:
		case IASTBinaryExpression.op_multiply:
			return constructArIntExprMul(loc, leftExpr, rightExpr, bothAreIntegerLiterals, leftValue, rightValue);
		case IASTBinaryExpression.op_divideAssign:
		case IASTBinaryExpression.op_divide:
			return constructArIntExprDiv(loc, leftExpr, rightExpr, bothAreIntegerLiterals, leftValue, rightValue);
		case IASTBinaryExpression.op_moduloAssign:
		case IASTBinaryExpression.op_modulo:
			return constructArIntExprMod(loc, leftExpr, rightExpr, bothAreIntegerLiterals, leftValue, rightValue);
		case IASTBinaryExpression.op_plusAssign:
		case IASTBinaryExpression.op_plus:
			return constructArIntExprPlus(loc, leftExpr, rightExpr, bothAreIntegerLiterals, leftValue, rightValue);
		default:
			final String msg = "Unknown or unsupported arithmetic expression";
			throw new UnsupportedSyntaxException(loc, msg);
		}
	}

	private Expression constructArIntExprDiv(ILocation loc, Expression exp1, Expression exp2,
			final boolean bothAreIntegerLiterals, BigInteger leftValue, BigInteger rightValue) {
		final BinaryExpression.Operator operator;
		operator = Operator.ARITHDIV;
		/*
		 * In C the semantics of integer division is "rounding towards zero". In Boogie euclidian division is used. We
		 * translate a / b into (a < 0 && a%b != 0) ? ( (b < 0) ? (a/b)+1 : (a/b)-1) : a/b
		 */
		if (bothAreIntegerLiterals) {
			final String constantResult = leftValue.divide(rightValue).toString();
			return new IntegerLiteral(loc, constantResult);
		} else {
			final Expression leftSmallerZeroAndThereIsRemainder =
					getLeftSmallerZeroAndThereIsRemainder(loc, exp1, exp2);
			final Expression rightSmallerZero = ExpressionFactory.newBinaryExpression(loc,
					BinaryExpression.Operator.COMPLT, exp2, new IntegerLiteral(loc, SFO.NR0));
			final Expression normalDivision = ExpressionFactory.newBinaryExpression(loc, operator, exp1, exp2);
			if (exp1 instanceof IntegerLiteral) {
				if (leftValue.signum() == 1) {
					return normalDivision;
				} else if (leftValue.signum() == -1) {
					return ExpressionFactory.newIfThenElseExpression(loc, rightSmallerZero,
							ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.ARITHMINUS,
									normalDivision, new IntegerLiteral(loc, SFO.NR1)),
							ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.ARITHPLUS,
									normalDivision, new IntegerLiteral(loc, SFO.NR1)));
				} else {
					return new IntegerLiteral(loc, SFO.NR0);
				}
			} else if (exp2 instanceof IntegerLiteral) {
				if (rightValue.signum() == 1 || rightValue.signum() == 0) {
					return ExpressionFactory.newIfThenElseExpression(loc, leftSmallerZeroAndThereIsRemainder,
							ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.ARITHPLUS,
									normalDivision, new IntegerLiteral(loc, SFO.NR1)),
							normalDivision);
				} else if (rightValue.signum() == -1) {
					return ExpressionFactory.newIfThenElseExpression(loc, leftSmallerZeroAndThereIsRemainder,
							ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.ARITHMINUS,
									normalDivision, new IntegerLiteral(loc, SFO.NR1)),
							normalDivision);
				}
				throw new UnsupportedOperationException("Is it expected that this is a fall-through switch?");
			} else {
				return ExpressionFactory.newIfThenElseExpression(loc, leftSmallerZeroAndThereIsRemainder,
						ExpressionFactory.newIfThenElseExpression(loc, rightSmallerZero,
								ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.ARITHMINUS,
										normalDivision, new IntegerLiteral(loc, SFO.NR1)),
								ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.ARITHPLUS,
										normalDivision, new IntegerLiteral(loc, SFO.NR1))),
						normalDivision);
			}
		}
	}

	private Expression constructArIntExprMinus(ILocation loc, Expression exp1, Expression exp2,
			final boolean bothAreIntegerLiterals, BigInteger leftValue, BigInteger rightValue) {
		final BinaryExpression.Operator operator;
		String constantResult;
		operator = Operator.ARITHMINUS;
		if (bothAreIntegerLiterals) {
			constantResult = leftValue.subtract(rightValue).toString();
			return new IntegerLiteral(loc, constantResult);
		} else {
			return ExpressionFactory.newBinaryExpression(loc, operator, exp1, exp2);
		}
	}

	private Expression constructArIntExprMul(ILocation loc, Expression exp1, Expression exp2,
			final boolean bothAreIntegerLiterals, BigInteger leftValue, BigInteger rightValue) {
		final BinaryExpression.Operator operator;
		String constantResult;
		operator = Operator.ARITHMUL;
		if (bothAreIntegerLiterals) {
			constantResult = leftValue.multiply(rightValue).toString();
			return new IntegerLiteral(loc, constantResult);
		} else {
			return ExpressionFactory.newBinaryExpression(loc, operator, exp1, exp2);
		}
	}

	private Expression constructArIntExprPlus(ILocation loc, Expression exp1, Expression exp2,
			final boolean bothAreIntegerLiterals, BigInteger leftValue, BigInteger rightValue) {
		final BinaryExpression.Operator operator;
		String constantResult;
		operator = Operator.ARITHPLUS;
		if (bothAreIntegerLiterals) {
			constantResult = leftValue.add(rightValue).toString();
			return new IntegerLiteral(loc, constantResult);
		} else {
			return ExpressionFactory.newBinaryExpression(loc, operator, exp1, exp2);
		}
	}

	private Expression constructArIntExprMod(ILocation loc, Expression exp1, Expression exp2,
			final boolean bothAreIntegerLiterals, BigInteger leftValue, BigInteger rightValue) {
		final BinaryExpression.Operator operator;
		operator = Operator.ARITHMOD;
		/*
		 * In C the semantics of integer division is "rounding towards zero". In Boogie euclidian division is used. We
		 * translate a % b into (a < 0 && a%b != 0) ? ( (b < 0) ? (a%b)-b : (a%b)+b) : a%b
		 */
		// modulo on bigInteger does not seem to follow the "multiply, add, and get the result back"-rule, together
		// with its division..
		if (bothAreIntegerLiterals) {
			final String constantResult;
			if (leftValue.signum() == 1 || leftValue.signum() == 0) {
				if (rightValue.signum() == 1) {
					constantResult = leftValue.mod(rightValue).toString();
				} else if (rightValue.signum() == -1) {
					constantResult = leftValue.mod(rightValue.negate()).toString();
				} else {
					constantResult = "0";
				}
			} else if (leftValue.signum() == -1) {
				if (rightValue.signum() == 1) {
					constantResult = (leftValue.negate().mod(rightValue)).negate().toString();
				} else if (rightValue.signum() == -1) {
					constantResult = (leftValue.negate().mod(rightValue.negate())).negate().toString();
				} else {
					constantResult = "0";
				}
			} else {
				throw new UnsupportedOperationException("constant is not assigned");
			}
			return new IntegerLiteral(loc, constantResult);
		} else {
			Expression leftSmallerZeroAndThereIsRemainder = getLeftSmallerZeroAndThereIsRemainder(loc, exp1, exp2);
			final Expression rightSmallerZero = ExpressionFactory.newBinaryExpression(loc,
					BinaryExpression.Operator.COMPLT, exp2, new IntegerLiteral(loc, SFO.NR0));
			final Expression normalModulo = ExpressionFactory.newBinaryExpression(loc, operator, exp1, exp2);
			if (exp1 instanceof IntegerLiteral) {
				if (leftValue.signum() == 1) {
					return normalModulo;
				} else if (leftValue.signum() == -1) {
					return ExpressionFactory.newIfThenElseExpression(loc, rightSmallerZero,
							ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.ARITHPLUS,
									normalModulo, exp2),
							ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.ARITHMINUS,
									normalModulo, exp2));
				} else {
					return new IntegerLiteral(loc, SFO.NR0);
				}
			} else if (exp2 instanceof IntegerLiteral) {
				if (rightValue.signum() == 1 || rightValue.signum() == 0) {
					return ExpressionFactory.newIfThenElseExpression(loc, leftSmallerZeroAndThereIsRemainder,
							ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.ARITHMINUS,
									normalModulo, exp2),
							normalModulo);
				} else if (rightValue.signum() == -1) {
					return ExpressionFactory.newIfThenElseExpression(loc, leftSmallerZeroAndThereIsRemainder,
							ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.ARITHPLUS,
									normalModulo, exp2),
							normalModulo);
				}
				throw new UnsupportedOperationException("Is it expected that this is a fall-through switch?");
			} else {
				return ExpressionFactory.newIfThenElseExpression(loc, leftSmallerZeroAndThereIsRemainder,
						ExpressionFactory.newIfThenElseExpression(loc, rightSmallerZero,
								ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.ARITHPLUS,
										normalModulo, exp2),
								ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.ARITHMINUS,
										normalModulo, exp2)),
						normalModulo);
			}
		}
	}
	
	private Expression getLeftSmallerZeroAndThereIsRemainder(ILocation loc, Expression exp1, Expression exp2) {
		final Expression leftModRight = ExpressionFactory.newBinaryExpression(loc, Operator.ARITHMOD, exp1, exp2);
		final Expression thereIsRemainder = ExpressionFactory.newBinaryExpression(loc, Operator.COMPNEQ, leftModRight,
				new IntegerLiteral(loc, SFO.NR0));
		final Expression leftSmallerZero = ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.COMPLT,
				exp1, new IntegerLiteral(loc, SFO.NR0));
		return ExpressionFactory.newBinaryExpression(loc, Operator.LOGICAND, leftSmallerZero, thereIsRemainder);
	}

	@Override
	public void convertIntToInt_NonBool(ILocation loc, ExpressionResult operand, CPrimitive resultType) {
		if (resultType.isIntegerType()) {
			convertToIntegerType(loc, operand, resultType);
		} else {
			throw new UnsupportedOperationException("not yet supported: conversion to " + resultType);
		}
	}

	private void convertToIntegerType(ILocation loc, ExpressionResult operand, CPrimitive resultType) {
		assert resultType.isIntegerType();
		final CPrimitive oldType = (CPrimitive) operand.lrVal.getCType();
		if (oldType.isIntegerType()) {
			final Expression newExpression;
			if (resultType.isUnsigned()) {
				final Expression oldWrappedIfNeeded;
				if (oldType.isUnsigned()
						&& mTypeSizes.getSize(resultType.getType()) > mTypeSizes.getSize(oldType.getType())) {
					// required for sound Nutz transformation
					// (see examples/programs/regression/c/NutzTransformation03.c)
					oldWrappedIfNeeded = applyWraparound(loc, mTypeSizes, oldType, operand.lrVal.getValue());
				} else {
					oldWrappedIfNeeded = operand.lrVal.getValue();
				}
				if (mUnsignedTreatment == UnsignedTreatment.ASSUME_ALL) {
					final BigInteger maxValuePlusOne =
							mTypeSizes.getMaxValueOfPrimitiveType(resultType).add(BigInteger.ONE);
					final AssumeStatement assumeGeq0 = new AssumeStatement(loc,
							ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.COMPGEQ,
									oldWrappedIfNeeded, new IntegerLiteral(loc, SFO.NR0)));
					operand.stmt.add(assumeGeq0);

					final AssumeStatement assumeLtMax = new AssumeStatement(loc,
							ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.COMPLT,
									oldWrappedIfNeeded, new IntegerLiteral(loc, maxValuePlusOne.toString())));
					operand.stmt.add(assumeLtMax);
				} else {
					// do nothing
				}
				newExpression = oldWrappedIfNeeded;
			} else {
				assert !resultType.isUnsigned();
				final Expression oldWrappedIfUnsigned;
				if (oldType.isUnsigned()) {
					// required for sound Nutz transformation
					// (see examples/programs/regression/c/NutzTransformation01.c)
					oldWrappedIfUnsigned = applyWraparound(loc, mTypeSizes, oldType, operand.lrVal.getValue());
				} else {
					oldWrappedIfUnsigned = operand.lrVal.getValue();
				}
				if (mTypeSizes.getSize(resultType.getType()) > mTypeSizes.getSize(oldType.getType())
						|| (mTypeSizes.getSize(resultType.getType()).equals(mTypeSizes.getSize(oldType.getType()))
								&& !oldType.isUnsigned())) {
					newExpression = oldWrappedIfUnsigned;
				} else {
					// According to C11 6.3.1.3.3 the result is implementation-defined
					// it the value cannot be represented by the new type
					// We have chosen an implementation that is similar to
					// taking the lowest bits in a two's complement representation:
					// First we take the value modulo the cardinality of the
					// data range (which is 2*(MAX_VALUE+1) for signed )
					// If the number is strictly larger than MAX_VALUE we
					// subtract the cardinality of the data range.
					final CPrimitive correspondingUnsignedType = resultType.getCorrespondingUnsignedType();
					final Expression wrapped =
							applyWraparound(loc, mTypeSizes, correspondingUnsignedType, oldWrappedIfUnsigned);
					final Expression maxValue = constructLiteralForIntegerType(loc, oldType,
							mTypeSizes.getMaxValueOfPrimitiveType(resultType));
					final Expression condition =
							ExpressionFactory.newBinaryExpression(loc, Operator.COMPLEQ, wrapped, maxValue);
					final Expression range = constructLiteralForIntegerType(loc, oldType,
							mTypeSizes.getMaxValueOfPrimitiveType(correspondingUnsignedType).add(BigInteger.ONE));
					newExpression = ExpressionFactory.newIfThenElseExpression(loc, condition, wrapped,
							ExpressionFactory.newBinaryExpression(loc, Operator.ARITHMINUS, wrapped, range));
				}

			}
			final RValue newRValue = new RValue(newExpression, resultType, false, false);
			operand.lrVal = newRValue;
		} else {
			throw new UnsupportedOperationException("not yet supported: conversion from " + oldType);
		}
	}

	public void oldConvertPointerToInt(ILocation loc, ExpressionResult rexp, CPrimitive newType) {
		assert newType.isIntegerType();
		assert rexp.lrVal.getCType() instanceof CPointer;
		if (OVERAPPROXIMATE_INT_POINTER_CONVERSION) {
			super.convertPointerToInt(loc, rexp, newType);
		} else {
			final Expression pointerExpression = rexp.lrVal.getValue();
			final Expression intExpression;
			if (mTypeSizes.useFixedTypeSizes()) {
				final BigInteger maxPtrValuePlusOne = mTypeSizes.getMaxValueOfPointer().add(BigInteger.ONE);
				final IntegerLiteral maxPointer = new IntegerLiteral(loc, maxPtrValuePlusOne.toString());
				intExpression = constructArithmeticExpression(loc, IASTBinaryExpression.op_plus,
						constructArithmeticExpression(loc, IASTBinaryExpression.op_multiply,
								MemoryHandler.getPointerBaseAddress(pointerExpression, loc), newType, maxPointer,
								newType),
						newType, MemoryHandler.getPointerOffset(pointerExpression, loc), newType);
			} else {
				intExpression = MemoryHandler.getPointerOffset(pointerExpression, loc);
			}
			final RValue rValue = new RValue(intExpression, newType, false, true);
			rexp.lrVal = rValue;
		}
	}

	public void oldConvertIntToPointer(ILocation loc, ExpressionResult rexp, CPointer newType) {
		if (OVERAPPROXIMATE_INT_POINTER_CONVERSION) {
			super.convertIntToPointer(loc, rexp, newType);
		} else {
			final Expression intExpression = rexp.lrVal.getValue();
			final Expression baseAdress;
			final Expression offsetAdress;
			if (mTypeSizes.useFixedTypeSizes()) {
				final BigInteger maxPtrValuePlusOne = mTypeSizes.getMaxValueOfPointer().add(BigInteger.ONE);
				final IntegerLiteral maxPointer = new IntegerLiteral(loc, maxPtrValuePlusOne.toString());
				baseAdress = constructArithmeticExpression(loc, IASTBinaryExpression.op_divide, intExpression,
						getCTypeOfPointerComponents(), maxPointer, getCTypeOfPointerComponents());
				offsetAdress = constructArithmeticExpression(loc, IASTBinaryExpression.op_modulo, intExpression,
						getCTypeOfPointerComponents(), maxPointer, getCTypeOfPointerComponents());
			} else {
				baseAdress = constructLiteralForIntegerType(loc, getCTypeOfPointerComponents(), BigInteger.ZERO);
				offsetAdress = intExpression;
			}
			final Expression pointerExpression =
					MemoryHandler.constructPointerFromBaseAndOffset(baseAdress, offsetAdress, loc);
			final RValue rValue = new RValue(pointerExpression, newType, false, false);
			rexp.lrVal = rValue;
		}
	}

	@Override
	public BigInteger extractIntegerValue(Expression expr, CType cType) {
		if (cType.isIntegerType()) {
			if (expr instanceof IntegerLiteral) {
				final BigInteger value = new BigInteger(((IntegerLiteral) expr).getValue());
				if (((CPrimitive) cType).isUnsigned()) {
					final BigInteger maxValue = mTypeSizes.getMaxValueOfPrimitiveType((CPrimitive) cType);
					final BigInteger maxValuePlusOne = maxValue.add(BigInteger.ONE);
					return value.mod(maxValuePlusOne);
				} else {
					return value;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public CPrimitive getCTypeOfPointerComponents() {
		return new CPrimitive(PRIMITIVE.LONG);
	}

	@Override
	public void addAssumeValueInRangeStatements(ILocation loc, Expression expr, CType cType, List<Statement> stmt) {
		if (mAssumeThatSignedValuesAreInRange && cType.getUnderlyingType().isIntegerType()) {
			final CPrimitive cPrimitive = (CPrimitive) CEnum.replaceEnumWithInt(cType);
			if (!cPrimitive.isUnsigned()) {
				stmt.add(constructAssumeInRangeStatement(mTypeSizes, loc, expr, cPrimitive));
			}
		}
	}

	/**
	 * Returns "assume (minValue <= lrValue && lrValue <= maxValue)"
	 */
	private AssumeStatement constructAssumeInRangeStatement(TypeSizes typeSizes, ILocation loc, Expression expr,
			CPrimitive type) {
		final Expression minValue =
				constructLiteralForIntegerType(loc, type, typeSizes.getMinValueOfPrimitiveType(type));
		final Expression maxValue =
				constructLiteralForIntegerType(loc, type, typeSizes.getMaxValueOfPrimitiveType(type));

		final Expression biggerMinInt =
				constructBinaryComparisonExpression(loc, IASTBinaryExpression.op_lessEqual, minValue, type, expr, type);
		final Expression smallerMaxValue =
				constructBinaryComparisonExpression(loc, IASTBinaryExpression.op_lessEqual, expr, type, maxValue, type);
		final AssumeStatement inRange = new AssumeStatement(loc, ExpressionFactory.newBinaryExpression(loc,
				BinaryExpression.Operator.LOGICAND, biggerMinInt, smallerMaxValue));
		return inRange;
	}

	@Override
	public Expression extractBits(ILocation loc, Expression operand, int high, int low) {
		// we probably also have to provide information if input is signed/unsigned
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Expression concatBits(ILocation loc, List<Expression> dataChunks, int size) {
		// we probably also have to provide information if input is signed/unsigned
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Expression signExtend(ILocation loc, Expression operand, int bitsBefore, int bitsAfter) {
		// we probably also have to provide information if input is signed/unsigned
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Expression constructBinaryComparisonFloatingPointExpression(ILocation loc, int nodeOperator, Expression exp1,
			CPrimitive type1, Expression exp2, CPrimitive type2) {
		final String functionName = "someBinary" + type1.toString() + "ComparisonOperation";
		final String prefixedFunctionName = "~" + functionName;
		if (!mFunctionDeclarations.getDeclaredFunctions().containsKey(prefixedFunctionName)) {
			final Attribute attribute = new NamedAttribute(loc, FunctionDeclarations.s_OVERAPPROX_IDENTIFIER,
					new Expression[] { new StringLiteral(loc, functionName) });
			final Attribute[] attributes = new Attribute[] { attribute };
			final ASTType paramAstType = mTypeHandler.ctype2asttype(loc, type1);
			final ASTType resultAstType = new PrimitiveType(loc, SFO.BOOL);
			mFunctionDeclarations.declareFunction(loc, prefixedFunctionName, attributes, resultAstType, paramAstType,
					paramAstType);
		}
		return new FunctionApplication(loc, prefixedFunctionName, new Expression[] { exp1, exp2 });
	}

	@Override
	public Expression constructUnaryFloatingPointExpression(ILocation loc, int nodeOperator, Expression exp,
			CPrimitive type) {
		final String functionName = "someUnary" + type.toString() + "operation";
		final String prefixedFunctionName = "~" + functionName;
		if (!mFunctionDeclarations.getDeclaredFunctions().containsKey(prefixedFunctionName)) {
			final Attribute attribute = new NamedAttribute(loc, FunctionDeclarations.s_OVERAPPROX_IDENTIFIER,
					new Expression[] { new StringLiteral(loc, functionName) });
			final Attribute[] attributes = new Attribute[] { attribute };
			final ASTType astType = mTypeHandler.ctype2asttype(loc, type);
			mFunctionDeclarations.declareFunction(loc, prefixedFunctionName, attributes, astType, astType);
		}
		return new FunctionApplication(loc, prefixedFunctionName, new Expression[] { exp });
	}

	@Override
	public Expression constructArithmeticFloatingPointExpression(ILocation loc, int nodeOperator, Expression exp1,
			CPrimitive type1, Expression exp2, CPrimitive type2) {
		final String functionName = "someBinaryArithmetic" + type1.toString() + "operation";
		final String prefixedFunctionName = "~" + functionName;
		if (!mFunctionDeclarations.getDeclaredFunctions().containsKey(prefixedFunctionName)) {
			final Attribute attribute = new NamedAttribute(loc, FunctionDeclarations.s_OVERAPPROX_IDENTIFIER,
					new Expression[] { new StringLiteral(loc, functionName) });
			final Attribute[] attributes = new Attribute[] { attribute };
			final ASTType astType = mTypeHandler.ctype2asttype(loc, type1);
			mFunctionDeclarations.declareFunction(loc, prefixedFunctionName, attributes, astType, astType, astType);
		}
		return new FunctionApplication(loc, prefixedFunctionName, new Expression[] { exp1, exp2 });
	}

	@Override
	public Expression constructBinaryEqualityExpression_Floating(ILocation loc, int nodeOperator, Expression exp1,
			CType type1, Expression exp2, CType type2) {
		final String prefixedFunctionName = declareBinaryFloatComparisonOperation(loc, (CPrimitive) type1);
		return new FunctionApplication(loc, prefixedFunctionName, new Expression[] { exp1, exp2 });
	}

	@Override
	public Expression constructBinaryEqualityExpression_Integer(final ILocation loc, final int nodeOperator,
			final Expression exp1, final CType type1, final Expression exp2, final CType type2) {
		Expression leftExpr = exp1;
		Expression rightExpr = exp2;
		if ((type1 instanceof CPrimitive) && (type2 instanceof CPrimitive)) {
			final CPrimitive primitive1 = (CPrimitive) type1;
			final CPrimitive primitive2 = (CPrimitive) type2;
			if (mUnsignedTreatment == UnsignedTreatment.WRAPAROUND && primitive1.isUnsigned()) {
				assert primitive2.isUnsigned();
				leftExpr = applyWraparound(loc, mTypeSizes, primitive1, leftExpr);
				rightExpr = applyWraparound(loc, mTypeSizes, primitive2, rightExpr);
			}
		}

		if (nodeOperator == IASTBinaryExpression.op_equals) {
			return ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.COMPEQ, leftExpr, rightExpr);
		} else if (nodeOperator == IASTBinaryExpression.op_notequals) {
			return ExpressionFactory.newBinaryExpression(loc, BinaryExpression.Operator.COMPNEQ, leftExpr, rightExpr);
		} else {
			throw new IllegalArgumentException("operator is neither equals nor not equals");
		}
	}

	@Override
	protected String declareConversionFunction(ILocation loc, CPrimitive oldType, CPrimitive newType) {
		return declareConversionFunctionOverApprox(loc, oldType, newType);
	}

	@Override
	public ExpressionResult createNanOrInfinity(ILocation loc, String name) {
		throw new UnsupportedOperationException("createNanOrInfinity is unsupported");
	}

	@Override
	public Expression getRoundingMode() {
		throw new UnsupportedOperationException("getRoundingMode is unsupported");
	}

	@Override
	public Expression createFloatingPointClassificationFunction(ILocation loc, String name) {
		throw new UnsupportedOperationException("createFloatingPointClassificationFunction is unsupported");
	}
}
