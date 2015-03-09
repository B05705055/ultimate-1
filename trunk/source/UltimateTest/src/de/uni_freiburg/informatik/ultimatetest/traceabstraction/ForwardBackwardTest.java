/**
 * 
 */
package de.uni_freiburg.informatik.ultimatetest.traceabstraction;

import java.util.Collection;

import de.uni_freiburg.informatik.ultimatetest.UltimateTestCase;

/**
 * Test for the two interpolation techniques "ForwardPredicates" and 
 * "BackwardPredicates".
 * @author musab@informatik.uni-freiburg.de, heizmanninformatik.uni-freiburg.de
 *
 */

public class ForwardBackwardTest extends
		AbstractTraceAbstractionTestSuite {
	private static final String[] m_Directories = {
		"/examples/programs/regression/.*\\.c|/examples/programs/regression/.*\\.bpl",
//		"examples/programs/quantifier",
//		"examples/programs/recursivePrograms",
//		"examples/programs/toy"
	};
	
	private static final boolean m_TraceAbstractionBoogieWithBackwardPredicates = !true;
	private static final boolean m_TraceAbstractionBoogieWithForwardPredicates = !true;
	private static final boolean m_TraceAbstractionBoogieWithFPandBP = !true;
	private static final boolean m_TraceAbstractionCWithBackwardPredicates = !true;
	private static final boolean m_TraceAbstractionCWithForwardPredicates = !true;		
	private static final boolean m_TraceAbstractionCWithFPandBP = true;
	// Time out for each test case in milliseconds
	private final static int m_Timeout = 10000;
	
	@Override
	public Collection<UltimateTestCase> createTestCases() {
		if (m_TraceAbstractionBoogieWithForwardPredicates) {
			addTestCases(
					"AutomizerBpl.xml",
					"automizer/ForwardPredicates.epf",
				    m_Directories,
				    new String[] {".bpl"},
				    m_Timeout);
		} 
		if (m_TraceAbstractionBoogieWithBackwardPredicates) {
			addTestCases(
					"AutomizerBpl.xml",
					"automizer/BackwardPredicates.epf",
				    m_Directories,
				    new String[] {".bpl"},
				    m_Timeout);
		}
		if (m_TraceAbstractionBoogieWithFPandBP) {
			addTestCases(
					"AutomizerBpl.xml",
					"automizer/ForwardPredicatesAndBackwardPredicates.epf",
				    m_Directories,
				    new String[] {".bpl"},
				    m_Timeout);
		}
		if (m_TraceAbstractionCWithForwardPredicates) {
			addTestCases(
					"AutomizerC.xml",
					"automizer/ForwardPredicates.epf",
				    m_Directories,
				    new String[] {".c", ".i"},
				    m_Timeout);
		}
		if (m_TraceAbstractionCWithBackwardPredicates) {
			addTestCases(
					"AutomizerC.xml",
					"automizer/BackwardPredicates.epf",
				    m_Directories,
				    new String[] {".c", ".i"},
				    m_Timeout);
		}
		if (m_TraceAbstractionCWithFPandBP) {
			addTestCasesRegExp(
					"AutomizerC.xml",
					"automizer/ForwardPredicatesAndBackwardPredicates.epf",
				    m_Directories,
				    m_Timeout);
		}
		return super.createTestCases();
	}

	
}
