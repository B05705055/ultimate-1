/**
 * 
 */
package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.valuedomain.booldomain;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.abstractdomain.valuedomain.booldomain.BoolValue.Bool;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.valuedomain.IAbstractValue;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.valuedomain.IValueMergeOperator;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.valuedomain.IValueWideningOperator;

/**
 * It does merge and widening (it does not matter which of those you do)
 * 
 * @author Christopher Dillo
 *
 */
public class BoolMergeWideningOperator implements IValueMergeOperator<Bool>,
		IValueWideningOperator<BoolValue.Bool> {

	private BoolValueFactory m_factory;

	private Logger m_logger;

	public BoolMergeWideningOperator(BoolValueFactory factory, Logger logger) {
		m_factory = factory;
		m_logger = logger;
	}

	public static String getName() {
		return "BOOL Merge & Widening";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uni_freiburg.informatik.ultimate.plugins.analysis.
	 * abstractinterpretationMk2
	 * .abstractdomain.IMergeOperator#apply(de.uni_freiburg
	 * .informatik.ultimate.plugins
	 * .analysis.abstractinterpretationMk2.abstractdomain.IAbstractValue,
	 * de.uni_freiburg
	 * .informatik.ultimate.plugins.analysis.abstractinterpretationMk2
	 * .abstractdomain.IAbstractValue)
	 */
	@Override
	public BoolValue apply(IAbstractValue<?> valueA, IAbstractValue<?> valueB) {
		BoolValue bvalA = (BoolValue) valueA;
		BoolValue bvalB = (BoolValue) valueB;

		// invalid state objects
		if ((bvalA == null) || (bvalB == null)) {
			return m_factory.makeTopValue();
		}

		Bool boolA = bvalA.getValue();
		Bool boolB = bvalB.getValue();

		if (boolA == boolB)
			return m_factory.makeValue(boolA);

		if (boolA == Bool.EMPTY) {
			if (boolB == Bool.TRUE)
				return m_factory.makeValue(Bool.TRUE);
			if (boolB == Bool.FALSE)
				return m_factory.makeValue(Bool.FALSE);
		}
		if (boolB == Bool.EMPTY) {
			if (boolA == Bool.TRUE)
				return m_factory.makeValue(Bool.TRUE);
			if (boolA == Bool.FALSE)
				return m_factory.makeValue(Bool.FALSE);
		}

		return m_factory.makeValue(Bool.UNKNOWN);
	}

	@Override
	public BoolMergeWideningOperator copy() {
		return new BoolMergeWideningOperator(m_factory, m_logger);
	}

}
