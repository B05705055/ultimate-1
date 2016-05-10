/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE Util Library.
 * 
 * The ULTIMATE Util Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE Util Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Util Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Util Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE Util Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.util;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_freiburg.informatik.ultimate.util.statistics.Benchmark;

/***
 * 
 * @author Dietsch
 * 
 */
public class BenchmarkTest {

	@BeforeClass
	public static void Setup() {

	}

	@Test
	public void TimeStartStopSingle() throws InterruptedException {

		long actualTime = 100;
		double measuredTime = -1;
		double allowedEpsilon = 1;
		String title = "TimeStartStopSingle";

		Benchmark bench = new Benchmark();

		bench.start(title);
		Thread.sleep(actualTime);
		bench.stop(title);

		measuredTime = bench.getElapsedTime(title, TimeUnit.MILLISECONDS);
		System.out.println("Measured time was " + measuredTime + "ms, and should be " + actualTime + "ms");
		System.out.println("Benchmark.Report(): " + bench.getReportString(title));
		System.out.println("--");

		Assert.assertTrue(Math.abs(actualTime - measuredTime) <= allowedEpsilon);
	}

	@Test
	public void HeapStartStopSingle() throws InterruptedException {

		int numInt = 1000000;
		int intSize = (Integer.SIZE / Byte.SIZE);
		// overhead for array is 12 bytes, total number is rounded up to a
		// multiple of 8
		// see http://www.javamex.com/tutorials/memory/array_memory_usage.shtml
		int actualHeapSize = numInt * intSize + 12;
		actualHeapSize = actualHeapSize + (actualHeapSize % 8);

		long measuredHeapDelta = -1;
		double allowedEpsilon = 0;
		String title = "HeapStartStopSingle";

		Benchmark bench = new Benchmark();
		System.gc();
		Thread.sleep(100);
		bench.start(title);
		int[] array = new int[numInt];
		for (int i = 0; i < numInt; ++i) {
			array[i] = i;
		}
		Thread.sleep(100);
		bench.stop(title);

		long startSize = bench.getStartMemoryFreeSize(title);
		long stopSize = bench.getStopMemoryFreeSize(title);
		measuredHeapDelta = startSize - stopSize;

		System.out.println("sizeof(int) = " + intSize + " byte");
		System.out.println("Measured memory delta was " + measuredHeapDelta + " byte, and should be " + actualHeapSize
				+ " byte");
		System.out.println("Measured memory consumed was " + bench.getPeakMemoryConsumed(title) + " byte, and should be " + actualHeapSize
				+ " byte");
		System.out.println("Benchmark.Report(): " + bench.getReportString(title));
		System.out.println("We print a random array element to keep the array from being thrown away: "
				+ array[(int) (Math.random() * numInt)]);
		System.out.println("--");

		Assert.assertTrue(Math.abs(actualHeapSize - measuredHeapDelta) <= allowedEpsilon);
		Assert.assertTrue(measuredHeapDelta == bench.getPeakMemoryConsumed(title));
	}

	@Test
	public void TimePauseSingle() throws InterruptedException {

		long actualTime = 100;
		double measuredTime = -1;
		double allowedEpsilon = 1;
		String title = "TimePauseSingle";

		Benchmark bench = new Benchmark();

		bench.start(title);
		Thread.sleep(actualTime);

		bench.pause(title);
		Thread.sleep(actualTime);
		bench.unpause(title);

		Thread.sleep(actualTime);

		bench.pause(title);
		Thread.sleep(actualTime);
		bench.unpause(title);

		Thread.sleep(actualTime);

		bench.stop(title);

		// we expect that we measured 3 Thread.sleep() periods instead of 5.
		actualTime = 3 * actualTime;

		measuredTime = bench.getElapsedTime(title, TimeUnit.MILLISECONDS);
		System.out.println("Measured time was " + measuredTime + "ms, and should be " + actualTime + "ms");
		System.out.println("Benchmark.Report(): " + bench.getReportString(title));
		System.out.println("--");

		Assert.assertTrue(Math.abs(actualTime - measuredTime) <= allowedEpsilon);
	}

	@Test
	public void AllSingle() throws InterruptedException {
		long sleepTime = 100;
		double measuredTime = -1;
		double allowedEpsilon = 1;
		long actualTime = 2 * sleepTime;
		String title = "AllSingle";

		Benchmark bench = new Benchmark();

		bench.start(title);
		Thread.sleep(sleepTime);

		bench.pause(title);
		Thread.sleep(sleepTime);
		bench.unpause(title);

		Thread.sleep(sleepTime);

		bench.stop(title);

		// we expect that we measured 2 Thread.sleep() periods.
		measuredTime = bench.getElapsedTime(title, TimeUnit.MILLISECONDS);
		System.out.println("Measured time was " + measuredTime + "ms, and should be " + actualTime + "ms");
		Assert.assertTrue(Math.abs(actualTime - measuredTime) <= allowedEpsilon);

		bench.start(title);
		Thread.sleep(sleepTime);
		bench.stop(title);

		// we expect that we measured 1 Thread.sleep() periods.
		actualTime = sleepTime;
		measuredTime = bench.getElapsedTime(title, TimeUnit.MILLISECONDS);
		System.out.println("Measured time was " + measuredTime + "ms, and should be " + actualTime + "ms");
		Assert.assertTrue(Math.abs(actualTime - measuredTime) <= allowedEpsilon);

		bench.reset();

		// we expect that we measured nothing.
		actualTime = -1;
		measuredTime = bench.getElapsedTime(title, TimeUnit.MILLISECONDS);
		System.out.println("Measured time was " + measuredTime + "ms, and should be " + actualTime + "ms");
		Assert.assertTrue(Math.abs(actualTime - measuredTime) <= allowedEpsilon);

		bench.start(title);
		Thread.sleep(sleepTime);
		bench.stop(title);

		// we expect that we measured 1 Thread.sleep() periods.
		actualTime = sleepTime;
		measuredTime = bench.getElapsedTime(title, TimeUnit.MILLISECONDS);
		System.out.println("Measured time was " + measuredTime + "ms, and should be " + actualTime + "ms");
		Assert.assertTrue(Math.abs(actualTime - measuredTime) <= allowedEpsilon);

		System.out.println("--");
	}

	@Test
	public void AllMultiple() throws InterruptedException {
		long sleepTime = 100;
		double measuredTime = -1;
		double allowedEpsilon = 1;
		long actualTime = 3 * sleepTime;
		int watches = 10;

		String[] titles = new String[watches];
		for (int i = watches - 1; i >= 0; i--) {
			titles[i] = "AllMultiple-" + i;
		}

		Benchmark bench = new Benchmark();

		for (int i = watches - 1; i >= 0; i--) {
			bench.register(titles[i]);
		}

		bench.startAll();
		Thread.sleep(sleepTime);

		bench.pause(titles[0]);
		Thread.sleep(sleepTime);
		bench.unpause(titles[0]);

		Thread.sleep(sleepTime);

		bench.stopAll();

		// we expect that we measured 3 Thread.sleep() periods with all watches
		// except watch 0, which measured 2 periods.
		for (int i = watches - 1; i > 0; i--) {
			measuredTime = bench.getElapsedTime(titles[i], TimeUnit.MILLISECONDS);
			System.out.println(titles[i] + ": Measured time was " + measuredTime + "ms, and should be " + actualTime
					+ "ms");
			Assert.assertTrue(Math.abs(actualTime - measuredTime) <= allowedEpsilon);
		}
		actualTime = 2 * sleepTime;
		measuredTime = bench.getElapsedTime(titles[0], TimeUnit.MILLISECONDS);
		System.out
				.println(titles[0] + ": Measured time was " + measuredTime + "ms, and should be " + actualTime + "ms");
		Assert.assertTrue(Math.abs(actualTime - measuredTime) <= allowedEpsilon);
		System.out.println("--");

	}

}
