/*
 * Copyright (C) 2013-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE AutomataScriptInterpreter plug-in.
 * 
 * The ULTIMATE AutomataScriptInterpreter plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE AutomataScriptInterpreter plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE AutomataScriptInterpreter plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE AutomataScriptInterpreter plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE AutomataScriptInterpreter plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.automatascriptinterpreter;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Create a list of all available operations.
 * @author heizmann@informatik.uni-freiburg.de
 *
 */
public class ListExistingOperations {
	
	private Map<String, Set<Class<?>>> mExistingOperations;
	private List<String> mOperationList = new ArrayList<String>();

	public ListExistingOperations(
			Map<String, Set<Class<?>>> existingOperations) {
		mExistingOperations = existingOperations;
		for (String operation : mExistingOperations.keySet()) {
			for (Class<?> clazz : mExistingOperations.get(operation)) {
				for (Constructor<?> constructor : clazz.getConstructors()) {
					mOperationList.add(constructorStringRepresentation(constructor));
				}
			}
		}
	}
	
	
	private String constructorStringRepresentation(Constructor<?> constructor) {
		StringBuilder result = new StringBuilder();
		result.append(constructor.getDeclaringClass().getSimpleName());
		result.append("(");
		for (int i=0; i<constructor.getParameterTypes().length; i++) {
			Class<?> clazz = constructor.getParameterTypes()[i];
			if (i!=0) {
				result.append(",");
			}
			result.append(clazz.getSimpleName());
		}
		result.append(")");
		return result.toString();
	}
	
	/**
	 * Representation of available operations. One line for each operation.
	 */
	public String prettyPrint() {
		StringBuilder result = new StringBuilder();
		String[] sorted = mOperationList.toArray(new String[0]);
		Arrays.sort(sorted);
		for(String op : sorted) {
			result.append(op);
			result.append(System.getProperty("line.separator"));
		}
		return result.toString();
	}

}
