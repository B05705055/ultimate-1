package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.congruence;

import java.math.BigInteger;

import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;

/**
 * Representation of a congruence value in the congruence domain
 * 
 * @author Frank Schüssele (schuessf@informatik.uni-freiburg.de)
 *
 */

public class CongruenceDomainValue implements Comparable<CongruenceDomainValue>{

	private BigInteger mValue = null;
	private boolean mIsBottom = true;
	private boolean mIsConstant = false;
	private boolean mNonZero = false;
	
	private CongruenceDomainValue() {}
	
	protected static CongruenceDomainValue createTop() {
		return createNonConstant(BigInteger.ONE);
	}
	
	protected static CongruenceDomainValue createBottom() {
		return new CongruenceDomainValue();
	}
	
	protected static CongruenceDomainValue createNonConstant(BigInteger value, boolean nonZero) {
		if (value.signum() == 0) {
			return nonZero ? createBottom() : createConstant(BigInteger.ZERO);
		}
		CongruenceDomainValue res = new CongruenceDomainValue();
		res.mValue = value.abs();
		res.mNonZero = nonZero;
		res.mIsBottom = false;
		return res;
	}
	
	protected static CongruenceDomainValue createNonConstant(BigInteger value) {
		return createNonConstant(value, false);
	}
	
	protected static CongruenceDomainValue createConstant(BigInteger value) {
		CongruenceDomainValue res = new CongruenceDomainValue();
		res.mValue = value;
		res.mNonZero = value.signum() != 0;
		res.mIsBottom = false;
		res.mIsConstant = true;
		return res;
	}
	
	protected boolean isBottom() {
		return mIsBottom;
	}
	
	protected BigInteger value() {
		return mValue;
	}
	
	protected boolean isConstant() {
		return mIsConstant;
	}
	
	@Override
	public int compareTo(CongruenceDomainValue other) {
		throw new UnsupportedOperationException(
		        "The compareTo operation is not defined on congruence clases and can therefore not be used.");
	}
	
	protected CongruenceDomainValue merge(CongruenceDomainValue other) {
		if (other == null) {
			return createBottom();
		}
		if (mIsBottom) {
			return other.copy();
		}
		if (other.mIsBottom) {
			return copy();
		}
		// If both are constant and have the same value, the result is also constant (otherwise not)
		if (mValue.equals(other.mValue) && mIsConstant && other.mIsConstant) {
			return copy();
		}
		return createNonConstant(mValue.gcd(other.mValue), mNonZero && other.mNonZero);
	}
	
	protected CongruenceDomainValue intersect(CongruenceDomainValue other) {
		if (other == null || mIsBottom || other.mIsBottom) {
			return createBottom();
		}
		// If both are constant, return the value if it's the same, bottom otherwise
		if (mIsConstant && other.mIsConstant) {
			if (mValue.equals(other.mValue)) {
				return copy();
			} else {
				return createBottom();
			}
		}
		// If one is constant, return the value if it's inside the other, bottom otherwise
		if (mIsConstant) {
			if (other.mNonZero && mValue.signum() == 0) {
				return createBottom();
			}
			if (mValue.mod(other.mValue.abs()).signum() == 0) {
				return copy();
			} else {
				return createBottom();
			}
		}
		if (other.mIsConstant) {
			if (mNonZero && other.mValue.signum() == 0) {
				return createBottom();
			}
			if (other.mValue.mod(mValue.abs()).signum() == 0) {
				return other.copy();
			} else {
				return createBottom();
			}
		}
		// Return the LCM as new value
		// LCM(a, b) = abs(a * b) / GCD(a, b)
		return createNonConstant(mValue.multiply(other.mValue).divide(mValue.gcd(other.mValue)), mNonZero || other.mNonZero);
	}

	protected CongruenceDomainValue add(CongruenceDomainValue other) {
		if (other == null || mIsBottom || other.mIsBottom) {
			return createBottom();
		}
		if (mValue.signum() == 0) {
			return other.copy();
		}
		if (other.mValue.signum() == 0) {
			return copy();
		}
		if (mIsConstant && other.mIsConstant) {
			return createConstant(mValue.add(other.mValue));
		}
		boolean nonZero = false;
		if (mIsConstant) {
			nonZero = mValue.mod(other.mValue).signum() != 0;
		}
		if (other.mIsConstant) {
			nonZero = other.mValue.mod(mValue).signum() != 0;
		}
		return createNonConstant(mValue.gcd(other.mValue), nonZero);
	}
	
	protected CongruenceDomainValue subtract(CongruenceDomainValue other) {
		if (other == null || mIsBottom || other.mIsBottom) {
			return createBottom();
		}
		if (mValue.signum() == 0) {
			return other.negate();
		}
		if (other.mValue.signum() == 0) {
			return copy();
		}
		if (mIsConstant && other.mIsConstant) {
			return createConstant(mValue.subtract(other.mValue));
		}
		boolean nonZero = false;
		if (mIsConstant) {
			nonZero = mValue.mod(other.mValue).signum() != 0;
		}
		if (other.mIsConstant) {
			nonZero = other.mValue.mod(mValue).signum() != 0;
		}
		return createNonConstant(mValue.gcd(other.mValue), nonZero);
	}
	

	protected CongruenceDomainValue mod(CongruenceDomainValue other) {
		if (other == null || mIsBottom || other.mIsBottom) {
			return createBottom();
		}
		if (!other.mNonZero) {
			return createTop();
		}
		// If both are constant, simply calculate the result
		if (mIsConstant && other.mIsConstant) {
			return createConstant(mValue.mod(other.mValue.abs()));
		}
		// a % bZ = a if a >= 0 and a < b
		if (mIsConstant && mValue.signum() >= 0 && mValue.compareTo(other.mValue) < 0) {
			return createConstant(mValue);
		}
		// aZ % b = 0 if a % b = 0
		if (other.mIsConstant && mValue.mod(other.mValue.abs()).signum() == 0) {
			return createConstant(BigInteger.ZERO);
		}
		boolean nonZero = false;
		if (mIsConstant) {
			nonZero = mValue.mod(other.mValue).signum() != 0;
		}
		if (other.mIsConstant) {
			nonZero = other.mValue.mod(mValue).signum() != 0;
		}
		return createNonConstant(mValue.gcd(other.mValue), nonZero);
	}
	
	protected CongruenceDomainValue multiply(CongruenceDomainValue other) {
		if (other == null || mIsBottom || other.mIsBottom) {
			return createBottom();
		}
		if (mIsConstant && other.mIsConstant) {
			return createConstant(mValue.multiply(other.mValue));
		}
		return createNonConstant(mValue.multiply(other.mValue), mNonZero && other.mNonZero);
	}
	
	protected CongruenceDomainValue divide (CongruenceDomainValue other) {
		if (other == null || mIsBottom || other.mIsBottom) {
			return createBottom();
		}
		if (!other.mNonZero) {
			return createTop();
		}
		if (other.mIsConstant) {
			// If "real" result of the division is an integer, just calculate the result
			if (mValue.mod(other.mValue.abs()).signum() == 0) {
				if (mIsConstant) {
					return createConstant(mValue.divide(other.mValue));
				}
				return createNonConstant(mValue.divide(other.mValue), mNonZero);
			}
			if (mIsConstant) {
				BigInteger val = mValue.divide(other.mValue);
				// If a < 0, a / b doesn't give the expected result (euclidian divsion)
				if (mValue.signum() < 0) {
					if (other.mValue.signum() > 0) {
						val = val.subtract(BigInteger.ONE);
					} else {
						val = val.add(BigInteger.ONE);
					}
				}
				return createConstant(val);
			}
		}
		// If 0 < a < b: a / bZ = 0 
		if (mIsConstant && mValue.signum() > 0 && mValue.compareTo(other.mValue) < 0) {
			return createConstant(BigInteger.ZERO);
		}
		return createTop();
	}
	
	protected CongruenceDomainValue negate() {
		if (mIsBottom) {
			return createBottom();
		}
		if (mIsConstant) {
			return createConstant(mValue.negate());
		}
		return copy();
	}
	
	@Override
	public String toString() {
		if (mIsBottom) {
			return "{}";
		}
		if (mIsConstant) {
			return mValue.toString();
		}
		if (mNonZero) {
			return mValue.toString() + "Z \\ {0}";
		} else {
			return mValue.toString() + "Z";
		}
		
	}

	protected Term getTerm(final Script script, final Sort sort, final Term var) {
		assert sort.isNumericSort();
		if (mIsBottom) {
			return script.term("false");
		} 
		if (mIsConstant) {
			return script.term("=", var, script.numeral(mValue));
		}
		Term nonZeroTerm = script.term("not", script.term("=", var, script.numeral(BigInteger.ZERO)));
		if (mValue.equals(BigInteger.ONE)) {
			if (mNonZero) {
				return nonZeroTerm;
			}
			return script.term("true");
		}
		Term modTerm = script.term("=", script.term("mod", var, script.numeral(mValue)), script.numeral(BigInteger.ZERO));
		if (mNonZero) {
			return script.term("and", modTerm, nonZeroTerm);
		}
		return modTerm;
		
	}
	
	/**
	 * Returns <code>true</code> if and only if <code>this</code> is equal to <code>other</code>.
	 */
	protected boolean isEqualTo(CongruenceDomainValue other) {
		if (other == null) {
			return false;
		}
		if (mIsBottom && other.mIsBottom) {
			return true;
		}
		return mValue.equals(other.mValue) && mIsConstant == other.mIsConstant && mNonZero == other.mNonZero;
	}
	
	/**
	 * Return a copy of the value
	 */
	protected CongruenceDomainValue copy() {
		if (mIsBottom) {
			return createBottom();
		}
		if (mIsConstant) {
			return createConstant(mValue);
		}
		return createNonConstant(mValue, mNonZero);
	}

	/**
	 * Return the the new value for x for a "x % this == rest" - expression (soft-merge)
	 */
	protected CongruenceDomainValue modEquals(CongruenceDomainValue rest) {
		if (mIsBottom ||  rest == null || rest.mIsBottom) {
			return createBottom();
		}
		if (!mNonZero) {
			return createTop();
		}
		// If the rest is < 0, return bottom
		if (rest.mValue.signum() < 0) {
			return createBottom();
		}
		// If the rest is >= |this|, return bottom if rest is constant, otherwise the non-constant value of this
		// (because rest has to be 0 then, since all other values are too big)
		if (mIsConstant && rest.mValue.compareTo(mValue.abs()) >= 0) {
			if (rest.mIsConstant) {
				return createBottom();
			} else {
				return createNonConstant(mValue);
			}			
		}
		// Otherwise return the GCD (=non-constant merge)
		return createNonConstant(mValue.gcd(rest.mValue), rest.mNonZero);
	}
	
	/**
	 * Returns <code>true</code> if this is contained in other.
	 * 
	 * @param other
	 *            The other value to check against.
	 * @return <code>true</code> if and only if the value of this is contained in the value of other, <code>false</code>
	 */
	public boolean isContainedIn(CongruenceDomainValue other) {
		if (mIsBottom) {
			return true;
		}
		if (other.mIsBottom) {
			return false;
		}
		if (other.mIsConstant) {
			return mIsConstant && mValue.equals(other.mValue);
		}
		if (!mNonZero && other.mNonZero) {
			return false;
		}
		return mValue.mod(other.mValue).signum() == 0;
	}
	
	protected CongruenceDomainValue getNonZeroValue() {
		if (mIsConstant) {
			return mValue.signum() == 0 ? createBottom() : copy();
		}
		CongruenceDomainValue res = copy();
		res.mNonZero = true;
		return res;
	}
}
