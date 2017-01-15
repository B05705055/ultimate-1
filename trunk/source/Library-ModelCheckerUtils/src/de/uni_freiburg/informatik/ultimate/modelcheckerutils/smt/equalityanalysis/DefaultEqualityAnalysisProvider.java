package de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.equalityanalysis;

import java.util.Set;

import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.util.datastructures.Doubleton;

/**
 * Default implementation of {@link IEqualityAnalysisResultProvider}, simply always returns "unknown" as result.
 *
 * @author Frank Schüssele (schuessf@informatik.uni-freiburg.de)
 */
public class DefaultEqualityAnalysisProvider<LOC> implements IEqualityAnalysisResultProvider<LOC> {

	@Override
	public EqualityAnalysisResult getAnalysisResult(final LOC location, final Set<Doubleton<Term>> doubletons) {
		return new EqualityAnalysisResult(doubletons);
	}
}
