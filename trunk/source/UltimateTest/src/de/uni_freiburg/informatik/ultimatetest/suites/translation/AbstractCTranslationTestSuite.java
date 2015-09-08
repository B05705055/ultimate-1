/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE Test Library.
 * 
 * The ULTIMATE Test Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE Test Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Test Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Test Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE Test Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimatetest.suites.translation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import de.uni_freiburg.informatik.ultimatetest.UltimateRunDefinition;
import de.uni_freiburg.informatik.ultimatetest.UltimateStarter;
import de.uni_freiburg.informatik.ultimatetest.UltimateTestCase;
import de.uni_freiburg.informatik.ultimatetest.UltimateTestSuite;
import de.uni_freiburg.informatik.ultimatetest.decider.TranslationTestResultDecider;
import de.uni_freiburg.informatik.ultimatetest.reporting.IIncrementalLog;
import de.uni_freiburg.informatik.ultimatetest.reporting.ITestSummary;
import de.uni_freiburg.informatik.ultimatetest.summaries.TranslationTestSummary;
import de.uni_freiburg.informatik.ultimatetest.util.TestUtil;

public abstract class AbstractCTranslationTestSuite extends UltimateTestSuite {

	@Override
	public Collection<UltimateTestCase> createTestCases() {
		ArrayList<UltimateTestCase> rtr = new ArrayList<UltimateTestCase>();

		// get a set of input files

		Collection<File> inputFiles = getInputFiles();
		File settingsFile = getSettings();

		File toolchainFile = new File(TestUtil.getPathFromTrunk("examples/toolchains/CTranslationTest.xml"));
		long deadline = 10000;

		for (File inputFile : inputFiles) {
			UltimateRunDefinition urd = new UltimateRunDefinition(inputFile, settingsFile, toolchainFile);
			UltimateStarter starter = new UltimateStarter(urd, deadline, null, null);
			rtr.add(new UltimateTestCase(inputFile.getAbsolutePath(), new TranslationTestResultDecider(inputFile
					.getAbsolutePath()), starter, urd, super.getSummaries(), null));
		}

		return rtr;
	}

	public abstract Collection<File> getInputFiles();

	public File getSettings() {
		return null;
	}
	
	@Override
	protected ITestSummary[] constructTestSummaries() {
		return new ITestSummary[] {
				new TranslationTestSummary(this.getClass())
		};
	}
	
	@Override
	protected IIncrementalLog[] constructIncrementalLog() {
		return new IIncrementalLog[0];
	}
}
