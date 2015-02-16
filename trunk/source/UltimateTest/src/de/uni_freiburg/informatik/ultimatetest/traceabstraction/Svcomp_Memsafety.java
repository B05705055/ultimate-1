/**
 * 
 */
package de.uni_freiburg.informatik.ultimatetest.traceabstraction;

import java.util.Collection;

import de.uni_freiburg.informatik.ultimatetest.UltimateTestCase;

/**
 * @author heizmann@informatik.uni-freiburg.de
 *
 */
public class Svcomp_Memsafety extends
		AbstractTraceAbstractionTestSuite {
	private static final String[] m_Directories = { 
		"examples/svcomp/memsafety",
		"examples/svcomp/memsafety-ext",
		"examples/svcomp/list-ext-properties",
		"examples/svcomp/memory-alloca/"
		};
	
	// Time out for each test case in milliseconds
	private static int m_Timeout = 60 * 1000;

	private static final boolean m_AutomizerWithForwardPredicates = true;
	private static final boolean m_AutomizerWithBackwardPredicates = !true;
	
	@Override
	public Collection<UltimateTestCase> createTestCases() {
		if (m_AutomizerWithForwardPredicates) {
			addTestCases(
					"AutomizerC.xml",
					"automizer/ForwardPredicates_SvcompMemsafety.epf",
				    m_Directories,
				    new String[] {".i"},
				    m_Timeout);
		}
		if (m_AutomizerWithForwardPredicates) {
			addTestCases(
					"AutomizerC.xml",
					"automizer/ForwardPredicates_SvcompMemsafetyConservative.epf",
				    m_Directories,
				    new String[] {".i"},
				    m_Timeout);
		}
//		if (m_AutomizerWithForwardPredicates) {
//			addTestCases(
//					"AutomizerC.xml",
//					"automizer/ForwardPredicates_SvcompMemsafetyAdditionalAssume.epf",
//				    m_Directories,
//				    new String[] {".i"},
//				    m_Timeout);
//		}
		if (m_AutomizerWithBackwardPredicates) {
			addTestCases(
					"AutomizerC.xml",
					"automizer/BackwardPredicates_SvcompMemsafety.epf",
				    m_Directories,
				    new String[] {".c", ".i"},
				    m_Timeout);
		}
		return super.createTestCases();
	}
}
