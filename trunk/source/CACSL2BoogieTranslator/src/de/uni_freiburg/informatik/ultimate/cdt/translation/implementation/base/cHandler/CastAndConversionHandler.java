package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.cHandler;

import java.math.BigInteger;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.CACSLLocation;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.LocationFactory;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.ExpressionTranslation.AExpressionTranslation;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.ExpressionTranslation.IntegerTranslation;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CEnum;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPointer;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.GENERALPRIMITIVE;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.PRIMITIVE;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CType;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.RValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.ResultExpression;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.SFO;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.Dispatcher;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssumeStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BinaryExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.IdentifierExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.IntegerLiteral;
import de.uni_freiburg.informatik.ultimate.model.location.ILocation;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.preferences.CACSLPreferenceInitializer.UNSIGNED_TREATMENT;

public class CastAndConversionHandler {

	/**
	 * Make the usual arithmetic conversions as in the C99 standard section 6.3.1.8
	 * @param memoryHandler
	 * @param left
	 * @param right
	 */
	public static void usualArithmeticConversions(Dispatcher main, ILocation loc, MemoryHandler memoryHandler, 
			ResultExpression leftRex, ResultExpression rightRex, boolean wraparoundOverflows, AExpressionTranslation expressionTranslation) {
		
		RValue left = (RValue) leftRex.lrVal;
		RValue right = (RValue) rightRex.lrVal;
		
		CType lUlType = left.cType.getUnderlyingType();
		CType rUlType = right.cType.getUnderlyingType();

		CACSLLocation iLoc = LocationFactory.createIgnoreCLocation();
		Expression lSize = memoryHandler.calculateSizeOf(lUlType, iLoc);
		Expression rSize = memoryHandler.calculateSizeOf(rUlType, iLoc);
		
		Integer lSizeInt = null;
		if (lSize instanceof IntegerLiteral) 
			lSizeInt = Integer.parseInt(((IntegerLiteral) lSize).getValue());
		Integer rSizeInt = null;
		if (rSize instanceof IntegerLiteral) 
			rSizeInt = Integer.parseInt(((IntegerLiteral) rSize).getValue());
		
		boolean lIsBigger = false;
		boolean rIsBigger = false;
		if (lSizeInt != null && rSizeInt != null) {
			if (lSizeInt > rSizeInt)
				lIsBigger = true;
			else if (lSizeInt > rSizeInt)
				rIsBigger = true;
		}
	
		if (lUlType instanceof CPrimitive && rUlType instanceof CPrimitive) {
			CPrimitive lCPrim = (CPrimitive) lUlType;
			CPrimitive rCPrim = (CPrimitive) rUlType;

			//TODO assuming real type here.. we deal with complex types when they occur
			if (lCPrim.getGeneralType() == GENERALPRIMITIVE.FLOATTYPE
					&& rCPrim.getGeneralType() == GENERALPRIMITIVE.FLOATTYPE) {
				if (lCPrim.getType() == PRIMITIVE.LONGDOUBLE 
						|| rCPrim.getType() == PRIMITIVE.LONGDOUBLE) {
					if (lCPrim.getType() != PRIMITIVE.LONGDOUBLE)
						left.cType = new CPrimitive(PRIMITIVE.LONGDOUBLE);
					else if (rCPrim.getType() != PRIMITIVE.LONGDOUBLE)
						right.cType = new CPrimitive(PRIMITIVE.LONGDOUBLE);
				} else if (lCPrim.getType() == PRIMITIVE.DOUBLE
						|| rCPrim.getType() == PRIMITIVE.DOUBLE) {
					if (lCPrim.getType() != PRIMITIVE.DOUBLE)
						left.cType = new CPrimitive(PRIMITIVE.DOUBLE);
					else if (rCPrim.getType() != PRIMITIVE.DOUBLE)
						right.cType = new CPrimitive(PRIMITIVE.DOUBLE);
				} else if (lCPrim.getType() == PRIMITIVE.FLOAT
						|| rCPrim.getType() == PRIMITIVE.FLOAT) {
					if (lCPrim.getType() != PRIMITIVE.FLOAT)
						left.cType = new CPrimitive(PRIMITIVE.FLOAT);
					else if (rCPrim.getType() != PRIMITIVE.FLOAT)
						right.cType = new CPrimitive(PRIMITIVE.FLOAT);
				}
			} else if (lCPrim.getGeneralType() == GENERALPRIMITIVE.INTTYPE
					&& rCPrim.getGeneralType() == GENERALPRIMITIVE.INTTYPE) {
				if (expressionTranslation instanceof IntegerTranslation) {
				if (lCPrim.isUnsigned() || rCPrim.isUnsigned()) {
					//does the unsigned int fit into the signed one? 
					if (!lCPrim.isUnsigned() && rCPrim.isUnsigned()) {//l is signed r unsigned
						if (!rIsBigger) {
							//does not fit, need to convert signedInt l
							if (wraparoundOverflows)
								doIntOverflowTreatment(main, memoryHandler, iLoc, leftRex, rCPrim);
						}
						if (wraparoundOverflows)
							doIntOverflowTreatment(main, memoryHandler, iLoc, rightRex);
						//if lIsBigger, it fits, if both are false, we don't know
						// do nothing either way..
					} else if (lCPrim.isUnsigned() && !rCPrim.isUnsigned()) {//l is unsigned r signed
						if (!lIsBigger) {
							//does not fit, need to convert signedInt r
							if (wraparoundOverflows)
								doIntOverflowTreatment(main, memoryHandler, iLoc, rightRex, lCPrim);
						}
						if (wraparoundOverflows)
							doIntOverflowTreatment(main, memoryHandler, iLoc, leftRex);
						//if lIsBigger, it fits, if both are false, we don't know
						// do nothing either way..
					} else {//both are unsigned
						//convert lesser rank to higher
						//TODO (not that important?)
						if (wraparoundOverflows) {
								doIntOverflowTreatment(main, memoryHandler, iLoc, leftRex);					
								doIntOverflowTreatment(main, memoryHandler, iLoc, rightRex);
						}
					}
					
				}
				} else {
					expressionTranslation.usualArithmeticConversions(main, loc, memoryHandler, leftRex, rightRex, wraparoundOverflows);
				}
				
			}
			
		} 

		
	}
	
	public static void doPrimitiveVsPointerConversions(Dispatcher main, ILocation loc, MemoryHandler memoryHandler, 
			ResultExpression leftRex, ResultExpression rightRex) {
		RValue left = (RValue) leftRex.lrVal;
		RValue right = (RValue) rightRex.lrVal;
		
		CType lUlType = left.cType.getUnderlyingType();
		CType rUlType = right.cType.getUnderlyingType();

		//save some time if the types are equal..
		if (lUlType.equals(rUlType))
			return; 

		if (lUlType instanceof CPrimitive && rUlType instanceof CPointer
				|| rUlType instanceof CPrimitive && lUlType instanceof CPointer) {
			if (lUlType instanceof CPrimitive) {
				if (left.getValue() instanceof IntegerLiteral
						&& ((IntegerLiteral) left.getValue()).getValue().equals("0")) {
					leftRex.lrVal = new RValue(new IdentifierExpression(loc, SFO.NULL), 
							new CPointer(new CPrimitive(PRIMITIVE.VOID)));
				}
			}
			if (rUlType instanceof CPrimitive) {
				if (right.getValue() instanceof IntegerLiteral
						&& ((IntegerLiteral) right.getValue()).getValue().equals("0")) {
					rightRex.lrVal = new RValue(new IdentifierExpression(loc, SFO.NULL), 
							new CPointer(new CPrimitive(PRIMITIVE.VOID)));
				}

			}
		}
	}
	
	public static void doIntOverflowTreatment(Dispatcher main, MemoryHandler memoryHandler, ILocation loc,
			ResultExpression rex) {
		doIntOverflowTreatment(main, memoryHandler, loc, rex, rex.lrVal.cType.getUnderlyingType());
	}
	
	public static void doIntOverflowTreatment(Dispatcher main, MemoryHandler memoryHandler, ILocation loc,
			ResultExpression rex, CType targetType) {
		BigInteger maxValuePlusOne = main.getTypeSizes().getMaxValueOfPrimitiveType((CPrimitive) targetType).add(BigInteger.ONE);

		if (main.cHandler.getUnsignedTreatment() == UNSIGNED_TREATMENT.ASSUME_ALL) {
			AssumeStatement assumeGeq0 = new AssumeStatement(loc, new BinaryExpression(loc, BinaryExpression.Operator.COMPGEQ,
					rex.lrVal.getValue(), new IntegerLiteral(loc, SFO.NR0)));
			rex.stmt.add(assumeGeq0);

			AssumeStatement assumeLtMax = new AssumeStatement(loc, new BinaryExpression(loc, BinaryExpression.Operator.COMPLT,
					rex.lrVal.getValue(), new IntegerLiteral(loc, maxValuePlusOne.toString())));
			rex.stmt.add(assumeLtMax);
		} else if (main.cHandler.getUnsignedTreatment() == UNSIGNED_TREATMENT.WRAPAROUND) {
			rex.lrVal = new RValue(applyWrapAround(loc, main.getTypeSizes(), (CPrimitive) rex.lrVal.cType, rex.lrVal.getValue()),
					rex.lrVal.cType, 
					rex.lrVal.isBoogieBool,
					false);
		}
	}
	
	public static Expression applyWrapAround(ILocation loc, TypeSizes typeSizes, CPrimitive cPrimitive, Expression operand) {
		if (cPrimitive.getGeneralType() == GENERALPRIMITIVE.INTTYPE) {
			if (cPrimitive.isUnsigned()) {
				BigInteger maxValuePlusOne = typeSizes.getMaxValueOfPrimitiveType(cPrimitive).add(BigInteger.ONE);
				return new BinaryExpression(loc, BinaryExpression.Operator.ARITHMOD, 
						operand, 
						new IntegerLiteral(loc, maxValuePlusOne.toString()));
			} else {
				throw new AssertionError("wraparound only for unsigned types");
			}
		} else {
			throw new AssertionError("wraparound only for integer types");
		}
	}



//	private static int determineByteSize(TypeSizes typeSizes,
//			CType ulType) {
//		int byteSize; 
//		if (ulType instanceof CEnum) {
//			byteSize = typeSizes.sizeOfEnumType;
//		} else {
//			//should be primitive
//			byteSize = typeSizes.getSize(((CPrimitive) ulType).getType());
//		}
//		return byteSize;
//	}

	public static void doIntOverflowTreatmentInComparisonIfApplicable(Dispatcher main, MemoryHandler memoryHandler,
			ILocation loc, ResultExpression left, ResultExpression right) {
		if (main.cHandler.getUnsignedTreatment() == UNSIGNED_TREATMENT.IGNORE)
			return;
		
		boolean isLeftUnsigned = left.lrVal.cType instanceof CPrimitive
				&& ((CPrimitive) left.lrVal.cType).isUnsigned()
				&& !left.lrVal.isIntFromPointer;
		boolean isRightUnsigned = right.lrVal.cType instanceof CPrimitive
				&& ((CPrimitive) right.lrVal.cType).isUnsigned()
				&& !right.lrVal.isIntFromPointer;

		//if one is unsigned, we convert the other to unsigned
		// --> C99: "usual conversions"
		if (isLeftUnsigned || isRightUnsigned) {
			CastAndConversionHandler.doIntOverflowTreatment(main, memoryHandler, loc, left);
			CastAndConversionHandler.doIntOverflowTreatment(main, memoryHandler, loc, right);
		}
		
	}
}
