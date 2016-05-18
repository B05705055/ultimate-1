/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE UnitTest Library.
 * 
 * The ULTIMATE UnitTest Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE UnitTest Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE UnitTest Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE UnitTest Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE UnitTest Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.test.reporting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.core.model.services.IResultService;
import de.uni_freiburg.informatik.ultimate.test.UltimateRunDefinition;
import de.uni_freiburg.informatik.ultimate.test.UltimateTestSuite;
import de.uni_freiburg.informatik.ultimate.test.decider.ITestResultDecider.TestResult;
import de.uni_freiburg.informatik.ultimate.test.util.TestUtil;
import de.uni_freiburg.informatik.ultimate.util.csv.CsvUtils;
import de.uni_freiburg.informatik.ultimate.util.csv.ICsvProvider;
import de.uni_freiburg.informatik.ultimate.util.csv.ICsvProviderProvider;
import de.uni_freiburg.informatik.ultimate.util.csv.SimpleCsvProvider;

/**
 * Summarizes all benchmarks of a certain class to a CSV. Searches through all
 * IResults and takes only the BenchmarkResults whose benchmarks is an
 * ICsvProvider<Object>> of a specified type. Each row is extends by an entry
 * for the following.
 * <ul>
 * <li>File
 * <li>Setting
 * <li>Toolchain
 * </ul>
 * Furthermore the rows of each Benchmark and each test case are concatenated to
 * a single CSV.
 * 
 * @author heizmann@informatik.uni-freiburg.de
 * 
 */
public class CsvConcatenator implements ITestSummary {

	private final Class<? extends UltimateTestSuite> mUltimateTestSuite;
	private final Class<? extends ICsvProviderProvider<? extends Object>> mBenchmark;
	private ICsvProvider<Object> mCsvProvider;

	public CsvConcatenator(Class<? extends UltimateTestSuite> ultimateTestSuite,
			Class<? extends ICsvProviderProvider<? extends Object>> benchmark) {
		super();
		mUltimateTestSuite = ultimateTestSuite;
		mBenchmark = benchmark;
		List<String> emtpyList = Collections.emptyList();
		mCsvProvider = new SimpleCsvProvider<Object>(emtpyList);
	}

	@Override
	public String getSummaryLog() {
		return mCsvProvider.toCsv(null, null).toString();
	}

	@Override
	public Class<? extends UltimateTestSuite> getUltimateTestSuiteClass() {
		return mUltimateTestSuite;
	}

	@Override
	public String getDescriptiveLogName() {
		return "Summarized " + mBenchmark.getSimpleName();
	}

	@Override
	public String getFilenameExtension() {
		return ".csv";
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addResult(UltimateRunDefinition ultimateRunDefinition, TestResult threeValuedResult, String category,
			String message, String testname, IResultService resultService) {
		if (resultService == null) {
			return;
		}
		for (ICsvProviderProvider<?> benchmarkResultWildcard : TestUtil.getCsvProviderProviderFromUltimateResults(resultService.getResults(),
				mBenchmark)) {
			ICsvProviderProvider<Object> benchmarkResult = (ICsvProviderProvider<Object>) benchmarkResultWildcard;
			ICsvProvider<Object> benchmarkCsv = benchmarkResult.createCvsProvider();
			ICsvProvider<Object> benchmarkCsvWithRunDefinition = addUltimateRunDefinition(ultimateRunDefinition,
					benchmarkCsv, category, message);
			add(benchmarkCsvWithRunDefinition);
		}
	}

	private void add(ICsvProvider<Object> benchmarkCsvWithRunDefinition) {
		mCsvProvider = CsvUtils.concatenateRows(mCsvProvider, benchmarkCsvWithRunDefinition);
	}

	private ICsvProvider<Object> addUltimateRunDefinition(UltimateRunDefinition ultimateRunDefinition,
			ICsvProvider<Object> benchmark, String category, String message) {
		List<String> resultColumns = new ArrayList<>();
		resultColumns.add("File");
		resultColumns.add("Settings");
		resultColumns.add("Toolchain");
		resultColumns.addAll(benchmark.getColumnTitles());
		ICsvProvider<Object> result = new SimpleCsvProvider<>(resultColumns);
		int rows = benchmark.getRowHeaders().size();
		for (int i = 0; i < rows; i++) {
			List<Object> resultRow = new ArrayList<>();
			resultRow.add(ultimateRunDefinition.getInputFileNames().replace(",", ";"));
			resultRow.add(ultimateRunDefinition.getSettings().getAbsolutePath());
			resultRow.add(ultimateRunDefinition.getToolchain().getAbsolutePath());
			resultRow.addAll(benchmark.getRow(i));
			result.addRow(resultRow);
		}
		return result;
	}

}
