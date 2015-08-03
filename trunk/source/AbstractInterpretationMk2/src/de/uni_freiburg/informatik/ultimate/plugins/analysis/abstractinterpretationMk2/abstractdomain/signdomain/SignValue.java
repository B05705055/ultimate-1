/**
 * 
 */
package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.signdomain;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.valuedomain.IAbstractValue;

/**
 * @author Christopher Dillo
 *
 */
public class SignValue implements IAbstractValue<SignValue.Sign> {
	
	/**
	 * Possible values for the sign domain.
	 * EMPTY < ZERO, PLUS, MINUS < PLUSMINUS
	 * ZERO, PLUS, MINUS : no relation
	 * ZERO : 0
	 * PLUS : > 0
	 * MINUS : < 0
	 */
	public enum Sign {
		EMPTY, // Bottom
		ZERO,
		PLUS,
		MINUS, 
		PLUSMINUS // Top
	}
	
	private Sign m_value;
	
	private SignValueFactory m_factory;
	
	private Logger m_logger;
	
	/**
	 * Generate a new SignValue with the given value
	 * @param value ZERO? PLUSMINUS?
	 */
	protected SignValue(Sign value, SignValueFactory factory, Logger logger) {
		m_value = value;
		m_factory = factory;
		m_logger = logger;
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#getValue()
	 */
	@Override
	public Sign getValue() {
		return m_value;
	}

	@Override
	public boolean isTrue()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isFalse()
	{
		// TODO Auto-generated method stub
		return m_value == Sign.MINUS || m_value == Sign.ZERO;
	}
	
	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#isTop()
	 */
	@Override
	public boolean isTop() {
		return (m_value == Sign.PLUSMINUS);
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#isBottom()
	 */
	@Override
	public boolean isBottom() {
		return (m_value == Sign.EMPTY);
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#representsSingleConcreteValue()
	 */
	@Override
	public boolean representsSingleConcreteValue() {
		return (m_value == Sign.PLUS) || (m_value == Sign.MINUS) || (m_value == Sign.ZERO);
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#isEqual(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public boolean isEqual(IAbstractValue<?> value) {
		SignValue signVal = (SignValue) value;
		if (signVal == null) return false;
		
		return (m_value == signVal.getValue());
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#isGreater(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public boolean isSuper(IAbstractValue<?> value) {
		SignValue signVal = (SignValue) value;
		if (signVal == null) return false;
		
		if (m_value == Sign.PLUSMINUS)
			return true;
		
		if (m_value == signVal.getValue())
			return true;
		
		if (signVal.getValue() == Sign.EMPTY)
			return true;
		
		return false;
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#isLesser(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public boolean isSub(IAbstractValue<?> value) {
		SignValue signVal = (SignValue) value;
		if (signVal == null) return false;
		
		if (m_value == Sign.EMPTY)
			return true;
		
		if (m_value == signVal.getValue())
			return true;
		
		if (signVal.getValue() == Sign.PLUSMINUS)
			return true;
		
		return false;
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#copy()
	 */
	@Override
	public SignValue copy() {
		return m_factory.makeValue(m_value);
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#add(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue add(IAbstractValue<?> value) {
		SignValue signVal = (SignValue) value;
		if (signVal == null) return m_factory.makeBottomValue();
		Sign otherSign = signVal.getValue();
		
		switch (m_value) {
		case ZERO :
			return m_factory.makeValue(otherSign);
		case PLUS :
			switch (otherSign) {
			case ZERO :
			case PLUS :
				return m_factory.makeValue(Sign.PLUS);
			case MINUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.PLUSMINUS);
			default :
				return m_factory.makeBottomValue();
			}
		case MINUS :
			switch (otherSign) {
			case ZERO :
			case MINUS :
				return m_factory.makeValue(Sign.MINUS);
			case PLUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.PLUSMINUS);
			default :
				return m_factory.makeBottomValue();
			}
		case PLUSMINUS :
			return m_factory.makeValue(Sign.PLUSMINUS);
		default :
			return m_factory.makeBottomValue();
		}
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#subtract(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue subtract(IAbstractValue<?> value) {
		SignValue signVal = (SignValue) value;
		if (signVal == null) return m_factory.makeBottomValue();
		Sign otherSign = signVal.getValue();
		
		switch (m_value) {
		case ZERO :
			switch (otherSign) {
			case ZERO :
			case PLUSMINUS :
				return m_factory.makeValue(otherSign);
			case PLUS :
				return m_factory.makeValue(Sign.MINUS);
			case MINUS :
				return m_factory.makeValue(Sign.PLUS);
			default :
				return m_factory.makeBottomValue();
			}
		case PLUS :
			switch (otherSign) {
			case ZERO :
			case MINUS :
				return m_factory.makeValue(Sign.PLUS);
			case PLUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.PLUSMINUS);
			default :
				return m_factory.makeBottomValue();
			}
		case MINUS :
			switch (otherSign) {
			case ZERO :
			case PLUS :
				return m_factory.makeValue(Sign.MINUS);
			case MINUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.PLUSMINUS);
			default :
				return m_factory.makeBottomValue();
			}
		case PLUSMINUS :
			return m_factory.makeValue(Sign.PLUSMINUS);
		default :
			return m_factory.makeBottomValue();
		}
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#multiply(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue multiply(IAbstractValue<?> value) {
		SignValue signVal = (SignValue) value;
		if (signVal == null) return m_factory.makeBottomValue();
		Sign otherSign = signVal.getValue();
		
		switch (m_value) {
		case ZERO :
			return m_factory.makeValue(Sign.ZERO);
		case PLUS :
			switch (otherSign) {
			case ZERO :
				return m_factory.makeValue(Sign.ZERO);
			case PLUS :
				return m_factory.makeValue(Sign.PLUS);
			case MINUS :
				return m_factory.makeValue(Sign.MINUS);
			case PLUSMINUS :
				return m_factory.makeValue(Sign.PLUSMINUS);
			default :
				return m_factory.makeBottomValue();
			}
		case MINUS :
			switch (otherSign) {
			case ZERO :
				return m_factory.makeValue(Sign.ZERO);
			case PLUS :
				return m_factory.makeValue(Sign.MINUS);
			case MINUS :
				return m_factory.makeValue(Sign.PLUS);
			case PLUSMINUS :
				return m_factory.makeValue(Sign.PLUSMINUS);
			default :
				return m_factory.makeBottomValue();
			}
		case PLUSMINUS :
			return m_factory.makeValue(Sign.PLUSMINUS);
		default :
			return m_factory.makeBottomValue();
		}
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#divide(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue divide(IAbstractValue<?> value) {
		if (value == null) return m_factory.makeBottomValue();
		
		if ((value.getValue() == Sign.ZERO) || (value.getValue() == Sign.PLUSMINUS)) {
			m_logger.warn(String.format("Potential division by zero: %s / %s", this, value));
			return m_factory.makeBottomValue();
		}
		
		return this.multiply(value);
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#modulo(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue modulo(IAbstractValue<?> value) {
		if (value == null) return m_factory.makeBottomValue();

		if ((value.getValue() == Sign.ZERO) || (value.getValue() == Sign.PLUSMINUS)) {
			m_logger.warn(String.format("Potential modulo division by zero: %s %% %s", this, value));
			return m_factory.makeBottomValue();
		}
		
		return m_factory.makeValue(Sign.PLUSMINUS); // remainder is always >= 0, which is only covered by PLUSMINUS
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#negation()
	 */
	@Override
	public SignValue negative() {
		switch (m_value) {
		case PLUS :
			return m_factory.makeValue(Sign.MINUS);
		case MINUS :
			return m_factory.makeValue(Sign.PLUS);
		default :
			return m_factory.makeValue(m_value);
		}
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#compareIsEqual(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue compareIsEqual(IAbstractValue<?> value) {
		SignValue signVal = (SignValue) value;
		if (signVal == null) return m_factory.makeBottomValue();
		Sign otherSign = signVal.getValue();
		
		if (m_value == otherSign)
			return m_factory.makeValue(m_value);
		
		switch (m_value) {
		case ZERO :
			switch (otherSign) {
			case ZERO :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.ZERO);
			case MINUS :
			case PLUS :
			default :
				return m_factory.makeBottomValue();
			}
		case PLUS :
			switch (otherSign) {
			case PLUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.PLUS);
			case ZERO :
			case MINUS :
			default :
				return m_factory.makeBottomValue();
			}
		case MINUS :
			switch (otherSign) {
			case MINUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.MINUS);
			case ZERO :
			case PLUS :
			default :
				return m_factory.makeBottomValue();
			}
		case PLUSMINUS :
			return m_factory.makeValue(otherSign);
		default :
			return m_factory.makeBottomValue();
		}
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#compareIsNotEqual(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue compareIsNotEqual(IAbstractValue<?> value) {
		SignValue signVal = (SignValue) value;
		if (signVal == null) return m_factory.makeBottomValue();
		Sign otherSign = signVal.getValue();
		
		switch (m_value) {
		case ZERO :
			switch (otherSign) {
			case ZERO :
				return m_factory.makeBottomValue();
			default :
				return m_factory.makeValue(otherSign);
			}
		case PLUS :
			switch (otherSign) {
			case PLUS :
				return m_factory.makeValue(Sign.PLUS);
			case ZERO :
			case MINUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.PLUSMINUS);
			default :
				return m_factory.makeBottomValue();
			}
		case MINUS :
			switch (otherSign) {
			case MINUS :
				return m_factory.makeValue(Sign.MINUS);
			case PLUS :
			case ZERO :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.PLUSMINUS);
			default :
				return m_factory.makeBottomValue();
			}
		case PLUSMINUS :
			return m_factory.makeValue(Sign.PLUSMINUS);
		default :
			return m_factory.makeBottomValue();
		}
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#compareIsLess(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue compareIsLess(IAbstractValue<?> value) {
		SignValue signVal = (SignValue) value;
		if (signVal == null) return m_factory.makeBottomValue();
		Sign otherSign = signVal.getValue();
		
		switch (m_value) {
		case ZERO :
			switch (otherSign) {
			case PLUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.ZERO);
			case ZERO :
			case MINUS :
			default :
				return m_factory.makeBottomValue();
			}
		case PLUS :
			switch (otherSign) {
			case PLUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.PLUS);
			case ZERO :
			case MINUS :
			default :
				return m_factory.makeBottomValue();
			}
		case MINUS :
			return m_factory.makeValue((otherSign == Sign.EMPTY) ? Sign.EMPTY : Sign.MINUS);
		case PLUSMINUS :
			switch (otherSign) {
			case ZERO :
			case MINUS :
				return m_factory.makeValue(Sign.MINUS);
			case PLUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.PLUSMINUS);
			default :
				return m_factory.makeBottomValue();
			}
		default :
			return m_factory.makeBottomValue();
		}
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#compareIsGreater(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue compareIsGreater(IAbstractValue<?> value) {
		SignValue signVal = (SignValue) value;
		if (signVal == null) return m_factory.makeBottomValue();
		Sign otherSign = signVal.getValue();
		
		switch (m_value) {
		case ZERO :
			switch (otherSign) {
			case MINUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.ZERO);
			case ZERO :
			case PLUS :
			default :
				return m_factory.makeBottomValue();
			}
		case PLUS :
			return m_factory.makeValue((otherSign == Sign.EMPTY) ? Sign.EMPTY : Sign.PLUS);
		case MINUS :
			switch (otherSign) {
			case MINUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.MINUS);
			case ZERO :
			case PLUS :
			default :
				return m_factory.makeBottomValue();
			}
		case PLUSMINUS :
			switch (otherSign) {
			case ZERO :
			case PLUS :
				return m_factory.makeValue(Sign.PLUS);
			case MINUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.PLUSMINUS);
			default :
				return m_factory.makeBottomValue();
			}
		default :
			return m_factory.makeBottomValue();
		}
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#compareIsLessEqual(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue compareIsLessEqual(IAbstractValue<?> value) {
		SignValue signVal = (SignValue) value;
		if (signVal == null) return m_factory.makeBottomValue();
		Sign otherSign = signVal.getValue();
		
		switch (m_value) {
		case ZERO :
			switch (otherSign) {
			case ZERO :
			case PLUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.ZERO);
			case MINUS :
			default :
				return m_factory.makeBottomValue();
			}
		case PLUS :
			switch (otherSign) {
			case PLUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.PLUS);
			case ZERO :
			case MINUS :
			default :
				return m_factory.makeBottomValue();
			}
		case MINUS :
			return m_factory.makeValue((otherSign == Sign.EMPTY) ? Sign.EMPTY : Sign.MINUS);
		case PLUSMINUS :
			switch (otherSign) {
			case ZERO :
			case MINUS :
				return m_factory.makeValue(Sign.MINUS);
			case PLUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.PLUSMINUS);
			default :
				return m_factory.makeBottomValue();
			}
		default :
			return m_factory.makeBottomValue();
		}
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#compareIsGreaterEqual(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue compareIsGreaterEqual(IAbstractValue<?> value) {
		SignValue signVal = (SignValue) value;
		if (signVal == null) return m_factory.makeBottomValue();
		Sign otherSign = signVal.getValue();
		
		switch (m_value) {
		case ZERO :
			switch (otherSign) {
			case ZERO :
			case MINUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.ZERO);
			case PLUS :
			default :
				return m_factory.makeBottomValue();
			}
		case PLUS :
			return m_factory.makeValue((otherSign == Sign.EMPTY) ? Sign.EMPTY : Sign.PLUS);
		case MINUS :
			switch (otherSign) {
			case MINUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.MINUS);
			case ZERO :
			case PLUS :
			default :
				return m_factory.makeBottomValue();
			}
		case PLUSMINUS :
			switch (otherSign) {
			case ZERO :
			case PLUS :
				return m_factory.makeValue(Sign.PLUS);
			case MINUS :
			case PLUSMINUS :
				return m_factory.makeValue(Sign.PLUSMINUS);
			default :
				return m_factory.makeBottomValue();
			}
		default :
			return m_factory.makeBottomValue();
		}
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#logicIff(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue logicIff(IAbstractValue<?> value) {
		return m_factory.makeBottomValue();
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#logicImplies(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue logicImplies(IAbstractValue<?> value) {
		return m_factory.makeBottomValue();
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#logicAnd(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue logicAnd(IAbstractValue<?> value) {
		return m_factory.makeBottomValue();
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#logicOr(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue logicOr(IAbstractValue<?> value) {
		return m_factory.makeBottomValue();
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#logicNot()
	 */
	@Override
	public SignValue logicNot() {
		return m_factory.makeBottomValue();
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#bitVectorConcat(de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue)
	 */
	@Override
	public SignValue bitVectorConcat(IAbstractValue<?> value) {
		return m_factory.makeBottomValue();
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue#bitVectorAccess(int, int)
	 */
	@Override
	public SignValue bitVectorAccess(int start, int end) {
		return m_factory.makeBottomValue();
	}

	@Override
	public String toString() {
		switch (m_value) {
		case ZERO :
			return "Sign: 0";
		case PLUS :
			return "Sign: +";
		case MINUS :
			return "Sign: -";
		case PLUSMINUS :
			return "Sign: +-";
		default :
			return "Sign: empty";
		}
	}	
}
