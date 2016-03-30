/*
 * Copyright (C) 2013-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
/**
 * 
 */
package de.uni_freiburg.informatik.ultimate.model.location;

import de.uni_freiburg.informatik.ultimate.result.Check;

/**
 * Defines an area in a text file.
 * Used to specify where an BoogieASTNode is defined.
 * 
 * @author heizmann@informatik.uni-freiburg.de
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 */
public interface ILocation {
	
	/**
	 * @return Name of this {@code Location}s file.
	 */
	public String getFileName();
	
	/**
	 * @return Number of line where this {@code Location} begins. -1 if unknown.
	 */
	public int getStartLine();
	
	/**
	 * @return Number of line where this {@code Location} ends. -1 if unknown.
	 */
	public int getEndLine();
	
	/**
	 * @return Number of column where this {@code Location} begins. -1 if unknown.
	 */
	public int getStartColumn();
	
	/**
	 * @return Number of column where this {@code Location} ends. -1 if unknown.
	 */
	public int getEndColumn();

	/**
	 * This {@code Location} can be an auxiliary {@code Location} constructed
	 * with respect to some <i>origin</i> {@code Location}. E.g.,
	 * if this is an auxiliary {@code Location} for the else-branch the
	 * <i>origin</i> {@code Location} can be the {@code Location} of an 
	 * if-then-else statement of a program.
	 * 
	 * If this {@code Location} is no auxiliary location the <i>origin</i> is
	 * the location itself.
	 */
	@Deprecated
	public ILocation getOrigin();
	
	
	
	/**
	 * Textual description of the type of specification which is checked here.
	 * E.g., "NullPointerException", "AssertStatement" etc.
	 */
	@Deprecated
	public Check getCheck();
	
	
	
	/**
	 * 
	 * @return true iff this Location represents a loop.
	 */
	@Deprecated
	public boolean isLoop();
}
