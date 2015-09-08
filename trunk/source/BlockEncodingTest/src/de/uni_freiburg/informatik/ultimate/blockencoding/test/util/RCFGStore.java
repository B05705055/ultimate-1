/*
 * Copyright (C) 2013-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Stefan Wissert
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE BlockEncoding plug-in.
 * 
 * The ULTIMATE BlockEncoding plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE BlockEncoding plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE BlockEncoding plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE BlockEncoding plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE BlockEncoding plug-in grant you additional permission 
 * to convey the resulting work.
 */
/**
 * 
 */
package de.uni_freiburg.informatik.ultimate.blockencoding.test.util;

import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RCFGNode;

/**
 * This is a static store, to share the generated RCFG with all test classes.
 * 
 * @author Stefan Wissert
 * 
 */
public class RCFGStore {

	private static RCFGNode rcfgNode;

	/**
	 * @param node
	 */
	public static void setRCFG(RCFGNode node) {
		rcfgNode = node;
	}

	/**
	 * @return
	 */
	public static RCFGNode getRCFG() {
		if (rcfgNode == null) {
			throw new IllegalArgumentException(
					"There is no RCFG-Node present (which is set by the Observer)"
							+ " , you cannot run the unit tests, without running"
							+ " Ultimate. To run Unit-Tests, you have to set the"
							+ " special observer in the settings.");
		}
		return rcfgNode;
	}

}
