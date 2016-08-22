/*
 * Copyright (C) 2011-2016 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2009-2016 University of Freiburg
 * 
 * This file is part of the ULTIMATE Automata Library.
 * 
 * The ULTIMATE Automata Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE Automata Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Automata Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Automata Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE Automata Library grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.automata;

import de.uni_freiburg.informatik.ultimate.automata.nestedword.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.StringFactory;

/**
 * Interface for automata operations.<br>
 * If possible,
 * <ul>
 * <li>each operation is defined in its own class
 * <li>for every application of the operation a new instance of this class is
 * constructed
 * <li>the result is returned via the {@link #getResult()} method
 * <li>start and end of the operation are reported to the logger using log
 * level <tt>INFO</tt>
 * <li>correctness checks for this operation are implemented in the
 * checkResult method. Whoever executes this operation should add an
 * <blockquote>
 * {@code assert} {@link #checkResult()}
 * </blockquote>
 * in the code.
 * </ul>
 * By convention the constructor of an IOperation has the following parameters.
 * <ul>
 * <li>The fist parameter are the {@link AutomataLibraryServices}. If the operation
 * is executed by the automata script interpreter, the interpreter will use
 * the {@link AutomataLibraryServices} of the current toolchain as an argument.
 * <li>If the IOperation requires a {@link StateFactory}, the {@link StateFactory} should
 * be the second parameter. If the second parameter is a {@link StateFactory}, the
 * automata script interpreter uses a {@link StringFactory} as argument.
 * <li>The remaining parameters of the constructor are the parameters of the
 * operation (i.e., the parameters for which you provide arguments in an
 * .ats file). It is good practice to have a default constructor with a minimal number of arguments, and optionally
 * other constructors with more arguments. The minimal constructor should be considered the default for .ats files.
 * </ul>
 * 
 * @author Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * @param <LETTER>
 *            Type of objects that are contained in the alphabet.
 * @param <STATE>
 *            Type of objects that are used to label states (resp. places
 *            for PetriNet)
 */
public interface IOperation<LETTER, STATE> {
	
	/**
	 * @return Name of the operation.<br>
	 *         This name should also be used in the test grammar.
	 */
	String operationName();
	
	/**
	 * @return Message that should be logged when the operation is stated.<br>
	 *         Use some information like: "Started operation intersection. First
	 *         operand has 2394 states, second operand has 9374 states."
	 */
	String startMessage();
	
	/**
	 * @return Message that should be logged when the operation is finished.<br>
	 *         Use some information like: "Finished operation intersection. Result has
	 *         345 states."
	 */
	String exitMessage();
	
	/**
	 * @return The result of the operation.
	 * @throws AutomataLibraryException
	 *             if operation fails
	 */
	Object getResult() throws AutomataLibraryException;
	
	/**
	 * Run some checks to test correctness of the result.
	 * 
	 * @param stateFactory
	 *            If new automata have to be built, use this state factory.
	 * @return true iff all checks succeeded.
	 * @throws AutomataLibraryException
	 *             when checks fail
	 */
	boolean checkResult(StateFactory<STATE> stateFactory) throws AutomataLibraryException;
	
	/**
	 * Get information about the runtime and resource consumption of the
	 * operation.
	 * <p>
	 * Delivering this information is optional.
	 * 
	 * @return statistics object
	 */
	default AutomataOperationStatistics getAutomataOperationStatistics() {
		return null;
	}
}
