package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.tool;

import java.util.List;

import de.uni_freiburg.informatik.ultimate.util.relation.Triple;

/**
 * 
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 * @param <STATE>
 * @param <ACTION>
 * @param <VARDECL>
 * @param <LOCATION>
 */
public final class AbstractCounterexample<STATE, ACTION, VARDECL, LOCATION> {
	private final STATE mInitialState;
	private final LOCATION mInitialLocation;
	private final List<Triple<STATE, LOCATION, ACTION>> mAbstractExecution;

	public AbstractCounterexample(final STATE initialState, final LOCATION initialLocation,
			final List<Triple<STATE, LOCATION, ACTION>> abstractExecution) {
		assert initialLocation != null;
		assert initialState != null;
		assert abstractExecution != null;

		mInitialLocation = initialLocation;
		mInitialState = initialState;
		mAbstractExecution = abstractExecution;
	}

	public STATE getInitialState() {
		return mInitialState;
	}

	public LOCATION getInitialLocation() {
		return mInitialLocation;
	}

	public List<Triple<STATE, LOCATION, ACTION>> getAbstractExecution() {
		return mAbstractExecution;
	}
}