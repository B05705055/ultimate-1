/**
 * Methods, helping to interpret C constants.
 */
package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util;

import java.math.BigInteger;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.cHandler.TypeSizeConstants;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.PRIMITIVE;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CType;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.exception.IncorrectSyntaxException;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.RValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.Dispatcher;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BitvecLiteral;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.IntegerLiteral;
import de.uni_freiburg.informatik.ultimate.model.location.ILocation;

/**
 * This class holds methods, that help translating constants.
 * 
 * @author Markus Lindenmann
 * @date 12.07.2012
 */
public final class ISOIEC9899TC3 {
	/**
	 * Message: "Ignored suffix".
	 */
	private static final String IGNORED_SUFFIX = "Ignored suffix";
	/**
	 * Octal prefix.
	 */
	private static final String OCT_0 = SFO.NR0;
	/**
	 * HEX lower case prefix.
	 */
	private static final String HEX_U0X = "0X";
	/**
	 * HEX upper case prefix.
	 */
	private static final String HEX_L0X = "0x";
	/**
	 * Float suffixes.
	 */
	private static final String[] SUFFIXES_FLOAT = new String[] { "f", "F", "l", "L" };
	/**
	 * Integer suffixes.
	 */
	private static final String[] SUFFIXES_INT = new String[] { "ULL", "Ull", "ull", "uLL", "llu", "llU", "LLu", "LLU",
			"ul", "uL", "Ul", "UL", "lu", "lU", "Lu", "LU", "ll", "LL", "u", "U", "l", "L" };

	/**
	 * Parses Integer constants according to <a
	 * href="www.open-std.org/jtc1/sc22/WG14/www/docs/n1256.pdf">ISO/IEC
	 * 9899:TC3</a>, chapter 6.4.4.4.
	 * 
	 * @param val
	 *            the value to parse
	 * @param loc
	 *            the location
	 * @return the parsed value
	 */
	public static final String handleCharConstant(String val, ILocation loc, Dispatcher dispatch) {
		int value;
		if (val.startsWith("L")) {
			// ignore wide character prefix
			val = val.substring(1, val.length());
			String msg = IGNORED_SUFFIX + "Char-Sequence wide character suffix L dropped";
			dispatch.warn(loc, msg);
		}
		if (!val.startsWith("'") || !val.endsWith("'")) {
			throw new UnsupportedOperationException();
		}

		if (val.charAt(1) == '\\') {
			switch (val.charAt(2)) {
			case '\'':
			case '\"':
			case '?':
			case '\\':
				value = (int) val.charAt(2);
				break;
			case 'a':
				value = 7;
				break;
			case 'b':
				value = 8;
				break;
			case 'f':
				value = 12;
				break;
			case 'n':
				value = 10;
				break;
			case 'r':
				value = 13;
				break;
			case 't':
				value = 9;
				break;
			case 'v':
				value = 11;
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
				value = Integer.valueOf(val.substring(2, val.length() - 1), 8);
				break;
			case 'x':
				value = Integer.valueOf(val.substring(3, val.length() - 1), 16);
				break;
			default:
				throw new UnsupportedOperationException();
			}
		} else if (val.length() == 3)
			value = (int) val.charAt(1);
		else
			throw new UnsupportedOperationException();
		return String.valueOf(value);
	}

	/**
	 * Parses Integer constants according to <a
	 * href="www.open-std.org/jtc1/sc22/WG14/www/docs/n1256.pdf">ISO/IEC
	 * 9899:TC3</a>, chapter 6.4.4.2.
	 * 
	 * @param val
	 *            the value to parse
	 * @param loc
	 *            the location
	 * @return the parsed value
	 */
	public static final String handleFloatConstant(String val, ILocation loc, Dispatcher dispatch) {
		String value = val;
		// if there is a float-suffix: throw it away
		for (String s : SUFFIXES_FLOAT) {
			if (val.endsWith(s)) {
				value = val.substring(0, val.length() - s.length());
				String msg = IGNORED_SUFFIX + " " + "Float suffix ignored: " + s;
				dispatch.warn(loc, msg);
				break;
			}
		}
		try {
			// check for integer-prefix.
			if (value.startsWith(HEX_L0X) || value.startsWith(HEX_U0X)) {
				// val is a hexadecimal-constant!
				//FIXME: --> is removing the + in front of the exponent enough??
				return Double.valueOf(value.replaceAll("\\+", "")).toString();
			}
			Double.valueOf(value); //using double for good measure..
			return value;
		} catch (NumberFormatException nfe) {
			String msg = "Unable to translate float!";
			throw new IncorrectSyntaxException(loc, msg);
		}
	}

	/**
	 * Parses Integer constants according to <a
	 * href="www.open-std.org/jtc1/sc22/WG14/www/docs/n1256.pdf">ISO/IEC
	 * 9899:TC3</a>, chapter 6.4.4.1.
	 * 
	 * @param val
	 *            the value to parse
	 * @param loc
	 *            the location
	 * @param bitvectorTranslation
	 *            if true the Expression of the resulting RValue is a bitvecor
	 *            if false the Expression is an int.
	 * @param typeSizeConstants
	 * 			  object that contains information about the size of
	 *  		  primitive types.
	 * @return the parsed value
	 */
	public static final RValue handleIntegerConstant(String val, ILocation loc, 
			boolean bitvectorTranslation, 
			TypeSizeConstants typeSizeConstants) {
		String value = val;
		String suffix = "";
		final CPrimitive cType;
		String valAsString;
		// if there is a integer-suffix: throw it away
		for (String s : SUFFIXES_INT) {
			if (val.endsWith(s)) {
				value = val.substring(0, val.length() - s.length());
				suffix = s;
				break;
			}
		}
		switch (suffix) {
		case "ULL": 
		case "Ull": 
		case "ull": 
		case "uLL":
		case "llu":
		case "llU":
		case "LLu":
		case "LLU":
			cType = new CPrimitive(PRIMITIVE.ULONGLONG);
			break;
		case "ul":
		case "uL":
		case "Ul":
		case "UL":
		case "lu":
		case "lU":
		case "Lu":
		case "LU":
			cType = new CPrimitive(PRIMITIVE.ULONG);
			break;
		case "ll":
		case "LL":
			cType = new CPrimitive(PRIMITIVE.LONGLONG);
			break;
		case "u":
		case "U":
			cType = new CPrimitive(PRIMITIVE.UINT);
			break;
		case "l": 
		case "L":
			cType = new CPrimitive(PRIMITIVE.LONG);
			break;
		default:
			cType = new CPrimitive(PRIMITIVE.INT);
			break;
		}
		try {
			// check for integer-prefix.
			if (value.startsWith(HEX_L0X) || value.startsWith(HEX_U0X)) {
				// val is a hexadecimal-constant
//				return new BigInteger(value.substring(2), 16).toString();
				valAsString = new BigInteger(value.substring(2), 16).toString();
			} else if (value.startsWith(OCT_0)) {
				// val is a octal-constant.
//				return new BigInteger(value, 8).toString();
				valAsString =new BigInteger(value, 8).toString();
			} else {
//				return new BigInteger(value).toString(); // check if correct!
				valAsString = new BigInteger(value).toString(); // check if correct!
			}
		} catch (NumberFormatException nfe) {
			String msg = "Unable to translate int!";
			throw new IncorrectSyntaxException(loc, msg);
		}
		final Expression resultLiteral = constructLiteralForCIntegerLiteral(
				loc, bitvectorTranslation, typeSizeConstants, cType,
				valAsString);
		return new RValue(resultLiteral, cType);
	}

	public static Expression constructLiteralForCIntegerLiteral(
			ILocation loc, boolean bitvectorTranslation,
			TypeSizeConstants typeSizeConstants, final CPrimitive cType,
			String valAsString) {
		final Expression resultLiteral;
		if (bitvectorTranslation) {
			int bitlength = 8 * typeSizeConstants. getCPrimitiveToTypeSizeConstant().get(cType.getType());
			BigInteger value = new BigInteger(valAsString);
			if (value.signum() == -1) {
				long maxValue = (long) Math.pow(2, bitlength);
				value.add(BigInteger.valueOf(maxValue));
			}
			resultLiteral = new BitvecLiteral(loc, value.toString(), bitlength);
		} else {
			resultLiteral = new IntegerLiteral(loc, valAsString);
		}
		return resultLiteral;
	}
}
