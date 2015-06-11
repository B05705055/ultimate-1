/**
 * 
 */
package de.uni_freiburg.informatik.ultimatetest.suites.traceabstraction;

import java.util.Collection;

import de.uni_freiburg.informatik.ultimatetest.UltimateRunDefinition;
import de.uni_freiburg.informatik.ultimatetest.UltimateTestCase;
import de.uni_freiburg.informatik.ultimatetest.decider.ITestResultDecider;
import de.uni_freiburg.informatik.ultimatetest.decider.SafetyCheckTestResultDecider_Overflow;

/**
 * @author heizmann@informatik.uni-freiburg.de
 * 
 */
public class Svcomp_Overflow_PreciseMemoryModel extends AbstractTraceAbstractionTestSuite {
	
	@Override
	public ITestResultDecider constructITestResultDecider(UltimateRunDefinition ultimateRunDefinition) {
		return new SafetyCheckTestResultDecider_Overflow(ultimateRunDefinition, true);
	}
	
	private static final DirectoryFileEndingsPair[] m_DirectoryFileEndingsPairs = {
		/*** Category 1. Arrays ***/
		new DirectoryFileEndingsPair("examples/svcomp/array-examples/", new String[]{ ".i" }) ,
		
		/*** Category 2. Bit Vectors ***/
		new DirectoryFileEndingsPair("examples/svcomp/bitvector/", new String[]{ ".i", ".c" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/bitvector-regression/", new String[]{ ".i", ".c" }) ,
		
		/*** Category 4. Control Flow and Integer Variables ***/
		new DirectoryFileEndingsPair("examples/svcomp/ntdrivers-simplified/", new String[]{".c" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/ssh-simplified/", new String[]{".c" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/locks/", new String[]{".c" }) ,
		
		new DirectoryFileEndingsPair("examples/svcomp/loops/", new String[]{".i"}) ,
		new DirectoryFileEndingsPair("examples/svcomp/loop-acceleration/", new String[]{".c" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/loop-invgen/", new String[]{".i"}) ,
		new DirectoryFileEndingsPair("examples/svcomp/loop-lit/", new String[]{ ".i", ".c" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/loop-new/", new String[]{".i"}) ,
		
		new DirectoryFileEndingsPair("examples/svcomp/eca-rers2012/", new String[]{".c" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/product-lines/", new String[]{".c" }) ,
		
		/*** Category 6. Heap Manipulation / Dynamic Data Structures ***/
		new DirectoryFileEndingsPair("examples/svcomp/heap-manipulation/", new String[]{ ".i" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/list-properties/", new String[]{ ".i" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/ldv-regression/", new String[]{ ".i" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/ddv-machzwd/", new String[]{ ".i" }) ,
		
		/*** Category 7. Memory Safety ***/
		new DirectoryFileEndingsPair("examples/svcomp/memsafety/", new String[]{ ".i" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/list-ext-properties/", new String[]{ ".i" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/memory-alloca/", new String[]{ ".i" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/memory-unsafe/", new String[]{ ".i" }) ,

		/*** Category 8. Recursive ***/
		new DirectoryFileEndingsPair("examples/svcomp/recursive/", new String[]{ ".c" }) ,
		
		/*** Category 9. Sequentialized Concurrent Programs ***/
		new DirectoryFileEndingsPair("examples/svcomp/systemc/", new String[]{ ".c" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/seq-mthreaded/", new String[]{ ".c" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/seq-pthread/", new String[]{ ".i" }) ,
		
		/*** Category 12. Termination ***/
		new DirectoryFileEndingsPair("examples/svcomp/termination-crafted/", new String[]{ ".c" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/termination-crafted-lit/", new String[]{ ".c" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/termination-memory-alloca/", new String[]{ ".i" }) ,
		new DirectoryFileEndingsPair("examples/svcomp/termination-numeric/", new String[]{ ".c" }) ,
		
//		new DirectoryFileEndingsPair("examples/svcomp/ssh/", new String[]{ ".c" }) ,
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getTimeout() {
		return 60 * 1000;
	}

	private static final boolean m_AutomizerWithForwardPredicates = true;
	private static final boolean m_AutomizerWithBackwardPredicates = false;
	
	@Override
	public Collection<UltimateTestCase> createTestCases() {
		if (m_AutomizerWithForwardPredicates) {
			addTestCases("AutomizerC.xml", 
					"automizer/ForwardPredicates_SvcompOverflowPreciseMM.epf", 
					m_DirectoryFileEndingsPairs);
		}
		if (m_AutomizerWithBackwardPredicates) {
			addTestCases("AutomizerC.xml", 
					"automizer/BackwardPredicates_SvcompOverflowPreciseMM.epf", 
					m_DirectoryFileEndingsPairs);
		}
		// return Util.firstN(super.createTestCases(), 3);
		return super.createTestCases();
	}

	
}
