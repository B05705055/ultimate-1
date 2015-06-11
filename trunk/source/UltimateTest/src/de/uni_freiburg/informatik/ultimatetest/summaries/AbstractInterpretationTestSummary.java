/**
 * 
 */
package de.uni_freiburg.informatik.ultimatetest.summaries;

import de.uni_freiburg.informatik.ultimatetest.UltimateTestSuite;
import de.uni_freiburg.informatik.ultimatetest.reporting.OldTestSummary;

/**
 * @author Christopher Dillo
 *
 */
public class AbstractInterpretationTestSummary extends OldTestSummary {
	
	public AbstractInterpretationTestSummary(Class<? extends UltimateTestSuite> ultimateTestSuite) {
		super(ultimateTestSuite);
	}
	
	@Override
	public String getFilenameExtension() {
		return ".log";
	}
	
	@Override
	public String getDescriptiveLogName() {
		return "WermutSummary";
	}

	/* (non-Javadoc)
	 * @see de.uni_freiburg.informatik.ultimatetest.summary.ITestSummary#getSummaryLog()
	 */
	@Override
	public String getSummaryLog() {
		return super.generateCanonicalSummary().toString();
	}
}
