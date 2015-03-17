package de.uni_freiburg.informatik.ultimatetest.evals;

import de.uni_freiburg.informatik.ultimatetest.evals.ColumnDefinition.Aggregate;

public abstract class TACAS2015 extends AbstractEvaluationTestSuite {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected long getTimeout() {
		return 300 * 1000;
	}

	protected String[] getDirectories() {
		// @formatter:off
		String[] directories = {
			"examples/svcomp/ntdrivers-simplified/",
	   		"examples/svcomp/ssh-simplified/", 
			"examples/svcomp/locks/",
			"examples/svcomp/recursive/", 
			"examples/svcomp/systemc/",
		};
		return directories;
		// @formatter:on
	}
	
	@Override
	protected int getFilesPerCategory() {
		return -1;
	}	

	@Override
	protected ColumnDefinition[] getColumnDefinitions() {
		// @formatter:off
		return new ColumnDefinition[]{
				new ColumnDefinition(
						"Runtime (ns)", "Avg. runtime",
						ConversionContext.Divide(1000000000, 2, " s"), Aggregate.Sum, Aggregate.Average),	
				new ColumnDefinition(
						"Peak memory consumption (bytes)", "Mem{-}ory",
						ConversionContext.Divide(1048576, 2, " MB"), Aggregate.Max, Aggregate.Average),						
				new ColumnDefinition(
						"Overall iterations", "Iter{-}ations",
						ConversionContext.BestFitNumber(), Aggregate.Ignore, Aggregate.Average),
				new ColumnDefinition(
						"NumberOfCodeBlocks", null,
						ConversionContext.BestFitNumber(), Aggregate.Ignore, Aggregate.Average),
				new ColumnDefinition(
						"SizeOfPredicatesFP", null,
						ConversionContext.BestFitNumber(), Aggregate.Ignore, Aggregate.Average),	
				new ColumnDefinition(
						"SizeOfPredicatesBP", null,
						ConversionContext.BestFitNumber(), Aggregate.Ignore, Aggregate.Average),	
				new ColumnDefinition(
						"Conjuncts in SSA", null,
						ConversionContext.BestFitNumber(), Aggregate.Ignore, Aggregate.Average),	
				new ColumnDefinition(
						"Conjuncts in UnsatCore", null,
						ConversionContext.BestFitNumber(), Aggregate.Ignore, Aggregate.Average),
				new ColumnDefinition(
						"ICC %", "ICC",
						ConversionContext.Percent(true,2), Aggregate.Ignore, Aggregate.Average),					
			};
		// @formatter:on
	}

	@Override
	protected String[] getFileEndings() {
		return new String[] { ".c" };
	}
}
