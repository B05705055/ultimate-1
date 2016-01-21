/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE AbstractInterpretationV2 plug-in.
 * 
 * The ULTIMATE AbstractInterpretationV2 plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE AbstractInterpretationV2 plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE AbstractInterpretationV2 plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE AbstractInterpretationV2 plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE AbstractInterpretationV2 plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.empty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.model.IAbstractPostOperator;

/**
 * 
 * @author dietsch@informatik.uni-freiburg.de
 *
 * @param <ACTION>
 * @param <VARDECL>
 */
public final class EmptyPostOperator<ACTION, VARDECL>
        implements IAbstractPostOperator<EmptyDomainState<ACTION, VARDECL>, ACTION, VARDECL> {

	@Override
	public List<EmptyDomainState<ACTION, VARDECL>> apply(EmptyDomainState<ACTION, VARDECL> oldstate, ACTION concrete) {
		final List<EmptyDomainState<ACTION, VARDECL>> returnList = new ArrayList<>();
		returnList.add(new EmptyDomainState<>(new HashMap<>(oldstate.getVariables())));
		return returnList;
	}

	@Override
	public EmptyDomainState<ACTION, VARDECL> apply(EmptyDomainState<ACTION, VARDECL> oldstate,
	        EmptyDomainState<ACTION, VARDECL> oldstateWithFreshVariables, ACTION transition) {
		return new EmptyDomainState<>(new HashMap<>(oldstateWithFreshVariables.getVariables()));
	}
}
