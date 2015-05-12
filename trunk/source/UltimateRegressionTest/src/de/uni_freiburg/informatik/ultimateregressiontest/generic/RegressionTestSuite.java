package de.uni_freiburg.informatik.ultimateregressiontest.generic;

import de.uni_freiburg.informatik.ultimateregressiontest.AbstractRegressionTestSuite;
import de.uni_freiburg.informatik.ultimatetest.UltimateRunDefinition;
import de.uni_freiburg.informatik.ultimatetest.decider.ITestResultDecider;
import de.uni_freiburg.informatik.ultimatetest.decider.SafetyCheckTestResultDecider;
import de.uni_freiburg.informatik.ultimatetest.util.TestUtil;

/**
 * 
 * @author dietsch@informatik.uni-freiburg.de
 * 
 */
public class RegressionTestSuite extends AbstractRegressionTestSuite {

	public RegressionTestSuite() {
		super();
		mTimeout = 20 * 1000;
		mRootFolder = TestUtil.getPathFromTrunk("examples/");

		// match every path not containing CToBoogieTranslation or Backtranslation
		mFilterRegex = "((?!CToBoogieTranslation|Backtranslation)[\\s\\S])*";
	}

	@Override
	protected ITestResultDecider getTestResultDecider(UltimateRunDefinition runDefinition) {
		return new SafetyCheckTestResultDecider(runDefinition, false);
	}


}
