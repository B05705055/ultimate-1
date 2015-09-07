/*
 * Copyright (C) 2013-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Markus Lindenmann
 * Copyright (C) 2012-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Oleksii Saukh
 * Copyright (C) 2015 Stefan Wissert
 * Copyright (C) 2015 University of Freiburg
 * Copyright (C) 2012-2015 lindenmm
 * 
 * This file is part of the ULTIMATE Core.
 * 
 * The ULTIMATE Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Core. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Core, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE Core grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.result;

import java.util.List;

import de.uni_freiburg.informatik.ultimate.core.services.IBacktranslationService;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.core.util.CoreUtil;
import de.uni_freiburg.informatik.ultimate.model.IElement;
import de.uni_freiburg.informatik.ultimate.model.location.ILocation;

/**
 * Result to store that the specification given at some location does not always
 * holds. We also store a computerexample to the correctness of the
 * specification. This counterexample is given as list of locations. (Locations
 * of Statements which lead to a state that violates the specification.
 * 
 * @author Markus Lindenmann
 * @author Stefan Wissert
 * @author Oleksii Saukh
 * @date 02.01.2012
 * 
 * @param <ELEM>
 *            Type of position
 * @param <TE>
 *            Type of trace element
 * @param <E>
 *            Type of expression
 */
public class CounterExampleResult<ELEM extends IElement, TE extends IElement, E> extends AbstractResultAtElement<ELEM>
		implements IResultWithFiniteTrace<TE, E> {
	private final Check mCheckedSpecification;
	private String mProgramExecutionAsString;
	private final List<ILocation> mFailurePath;
	private final IProgramExecution<TE, E> mProgramExecution;

	/**
	 * Constructs a {@link CounterExampleResult}.
	 * 
	 * @param position
	 *            At which location did the error occur?
	 * @param plugin
	 *            Which plugin (PluginId) found the error location=
	 * @param translatorSequence
	 *            The current backtranslator service (obtained from
	 *            {@link IUltimateServiceProvider}).
	 * @param pe
	 *            A program execution leading to this error.
	 */
	public CounterExampleResult(ELEM position, String plugin, IBacktranslationService translatorSequence,
			IProgramExecution<TE, E> pe) {
		super(position, plugin, translatorSequence);
		mCheckedSpecification = ResultUtil.getCheckedSpecification(position);
		mProgramExecution = pe;
		mFailurePath = ResultUtil.getLocationSequence(pe);
	}

	@Override
	public String getShortDescription() {
		if (mCheckedSpecification == null) {
			return "some specification holds - ERROR (information lost during translation process)";
		} else {
			return mCheckedSpecification.getNegativeMessage();
		}
	}

	public Check getCheckedSpecification() {
		return mCheckedSpecification;
	}

	@Override
	public String getLongDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append(getShortDescription());
		sb.append(CoreUtil.getPlatformLineSeparator());
		sb.append("We found a FailurePath: ");
		sb.append(CoreUtil.getPlatformLineSeparator());
		sb.append(getProgramExecutionAsString());
		return sb.toString();
	}

	/**
	 * Getter for the failure path.
	 * 
	 * @return the failurePath
	 */
	public List<ILocation> getFailurePath() {
		return mFailurePath;
	}

	public IProgramExecution<TE, E> getProgramExecution() {
		return mProgramExecution;
	}

	public String getProgramExecutionAsString() {
		if (mProgramExecutionAsString == null) {
			mProgramExecutionAsString = mTranslatorSequence.translateProgramExecution(mProgramExecution).toString();
		}
		return mProgramExecutionAsString;
	}
}
