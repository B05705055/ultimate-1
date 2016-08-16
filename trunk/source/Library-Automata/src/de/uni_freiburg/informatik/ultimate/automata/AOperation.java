/*
 * Copyright (C) 2016 Christian Schilling (schillic@informatik.uni-freiburg.de)
 * Copyright (C) 2016 University of Freiburg
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

import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;

/**
 * Abstract operation with the most common fields and default methods.
 * 
 * @author Christian Schilling (schillic@informatik.uni-freiburg.de)
 * @param <LETTER> letter type
 * @param <STATE> state type
 */
public abstract class AOperation<LETTER, STATE>
		implements IOperation<LETTER, STATE> {
	/**
	 * Ultimate services.
	 */
	protected final AutomataLibraryServices mServices;
	
	/**
	 * logger.
	 */
	protected final ILogger mLogger;
	
	/**
	 * Constructor.
	 * 
	 * @param services Ultimate services
	 */
	public AOperation(final AutomataLibraryServices services) {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(
				LibraryIdentifiers.PLUGIN_ID);
	}
	
	@Override
	public String operationName() {
		// use runtime class name by default
		return getClass().getSimpleName();
	}
	
	@Override
	public String startMessage() {
		return "Start " + operationName() + '.';
	}
	
	@Override
	public String exitMessage() {
		return "Finished " + operationName() + '.';
	}
	
	@Override
	public boolean checkResult(final StateFactory<STATE> stateFactory)
			throws AutomataLibraryException {
		mLogger.warn("No result check for " + operationName()
			+ " available yet.");
		return true;
	}
}
