/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
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
package de.uni_freiburg.informatik.ultimate.core.services.model;

import java.util.List;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.result.IResult;

/**
 * {@link IResultService} allows tools to report results.
 * 
 * @author dietsch@informatik.uni-freiburg.de
 *
 */
public interface IResultService {

	/**
	 * @return A map containing all results of a toolchain up to now.
	 */
	Map<String, List<IResult>> getResults();

	/**
	 * Report a result to the Ultimate result service. The result may not be
	 * null and may not contain null values (i.e. at least
	 * {@link IResult#getShortDescription()} and
	 * {@link IResult#getLongDescription()} must not be null).
	 * 
	 * @param pluginId
	 *            The plugin ID of the tool which generated the result.
	 * @param result
	 *            The result itself.
	 */
	void reportResult(String pluginId, IResult result);
}
