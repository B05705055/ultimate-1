/**
 * 
 */
package de.uni_freiburg.informatik.ultimatetest.traceabstraction;

import java.util.Collection;

import de.uni_freiburg.informatik.ultimatetest.UltimateTestCase;

/**
 * @author musab@informatik.uni-freiburg.de
 *
 */
public class AllExamplesTraceAbstractionTestSuite extends
		AbstractTraceAbstractionTestSuite {
	private static final String[] m_Directories = { "examples/programs/" };
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getTimeout() {
		return 20 * 1000;
	}

	private static final boolean m_TraceAbstractionWithForwardPredicates = true;
	private static final boolean m_TraceAbstractionWithBackwardPredicates = true;
	private static final boolean m_TraceAbstractionCWithForwardPredicates = true;
	private static final boolean m_TraceAbstractionCWithBackwardPredicates = true;
	
	@Override
	public Collection<UltimateTestCase> createTestCases() {
		if (m_TraceAbstractionWithForwardPredicates) {
			addTestCases(
					"AutomizerBpl.xml",
					"automizer/ForwardPredicates.epf",
				    m_Directories,
				    new String[] {".bpl"});
		} 
		if (m_TraceAbstractionWithBackwardPredicates) {
			addTestCases(
					"AutomizerBpl.xml",
					"automizer/BackwardPredicates.epf",
				    m_Directories,
				    new String[] {".bpl"});
		}
		if (m_TraceAbstractionCWithForwardPredicates) {
			addTestCases(
					"AutomizerC.xml",
					"automizer/ForwardPredicates.epf",
				    m_Directories,
				    new String[] {".c", ".i"});
		}
		if (m_TraceAbstractionCWithBackwardPredicates) {
			addTestCases(
					"AutomizerC.xml",
					"automizer/BackwardPredicates.epf",
				    m_Directories,
				    new String[] {".c", ".i"});
		}
		return super.createTestCases();
	}
}
