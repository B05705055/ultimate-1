package de.uni_freiburg.informatik.ultimatetest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.runner.RunWith;

import de.uni_freiburg.informatik.junit_helper.testfactory.FactoryTestRunner;
import de.uni_freiburg.informatik.junit_helper.testfactory.TestFactory;
import de.uni_freiburg.informatik.ultimatetest.reporting.IIncrementalLog;
import de.uni_freiburg.informatik.ultimatetest.reporting.ITestSummary;
import de.uni_freiburg.informatik.ultimatetest.util.TestUtil;

/**
 * 
 * @author dietsch@informatik.uni-freiburg.de
 * 
 */
@RunWith(FactoryTestRunner.class)
public abstract class UltimateTestSuite {

	private static List<ITestSummary> sSummaries;
	private static List<IIncrementalLog> sLogFiles;
	protected static Logger sLogger = Logger.getLogger(UltimateTestSuite.class);

	public UltimateTestSuite() {
		if (sSummaries == null) {
			ITestSummary[] summaries = constructTestSummaries();

			if (summaries != null) {
				for (ITestSummary sum : summaries) {
					assert sum != null;
				}
				sSummaries = Arrays.asList(summaries);
			} else {
				sSummaries = null;
			}

		}
		if (sLogFiles == null) {
			IIncrementalLog[] logs = constructIncrementalLog();
			if (logs != null) {
				for (IIncrementalLog log : logs) {
					assert log != null;
				}
				sLogFiles = Arrays.asList(logs);
			} else {
				sLogFiles = null;
			}
		}
	}

	@TestFactory
	public abstract Collection<UltimateTestCase> createTestCases();

	/**
	 * Returns the ITestSummaries instances that produce summaries while running
	 * the UltimateTestSuite. This method is called only once during each run of
	 * an UltimateTestSuite.
	 */
	protected abstract ITestSummary[] constructTestSummaries();

	protected abstract IIncrementalLog[] constructIncrementalLog();

	/**
	 * Provides a collection of ITestSummary instances.
	 * 
	 * @return A collection containing ITestSummary instances. They will be
	 *         accessed at the end of this test suite and their content written
	 *         in a file.
	 */
	protected List<ITestSummary> getSummaries() {
		return Collections.unmodifiableList(sSummaries);
	}

	protected List<IIncrementalLog> getIncrementalLogs() {
		return Collections.unmodifiableList(sLogFiles);
	}

	@AfterClass
	public final static void writeSummaries() {
		if (sSummaries == null || sSummaries.size() == 0) {
			System.out.println("No test summaries available");
			return;
		}

		for (ITestSummary summary : sSummaries) {
			try {
				TestUtil.writeSummary(summary);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		sSummaries = null;
	}
}
