/*
 * Copyright (C) 2015 Alexander Nutz (nutz@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Carl Kuesters
 * Copyright (C) 2015 University of Freiburg
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
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.alternating;

import java.util.HashMap;
import java.util.Map.Entry;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;

public class AA_MergedUnion<LETTER, STATE> implements IOperation<LETTER, STATE>{

	public AA_MergedUnion(AlternatingAutomaton<LETTER, STATE> automaton1, AlternatingAutomaton<LETTER, STATE> automaton2){
		assert automaton1.getAlphabet().equals(automaton2.getAlphabet());
		assert (automaton1.isReversed() == automaton2.isReversed());
		resultAutomaton = new AlternatingAutomaton<>(automaton1.getAlphabet(), automaton1.getStateFactory());
		final HashMap<Integer, Integer> shiftMap1 = new HashMap<>();
		final HashMap<Integer, Integer> shiftMap2 = new HashMap<>();
		for(final STATE state : automaton1.getStates()){
			resultAutomaton.addState(state);
			shiftMap1.put(automaton1.getStateIndex(state), resultAutomaton.getStateIndex(state));
			if(automaton1.isStateFinal(state)){
				resultAutomaton.setStateFinal(state);
			}
		}
		for(final STATE state : automaton2.getStates()){
			resultAutomaton.addState(state);
			shiftMap2.put(automaton2.getStateIndex(state), resultAutomaton.getStateIndex(state));
			if(automaton2.isStateFinal(state)){
				resultAutomaton.setStateFinal(state);
			}
		}
		final int newSize = resultAutomaton.getStates().size();
		for(final Entry<LETTER, BooleanExpression[]> entry : automaton1.getTransitionFunction().entrySet()){
			for(int i=0;i<automaton1.getStates().size();i++){
				if(entry.getValue()[i] != null){
					resultAutomaton.addTransition(
							entry.getKey(), 
							automaton1.getStates().get(i), 
							entry.getValue()[i].cloneShifted(shiftMap1, newSize));
				}
			}
		}
		for(final Entry<LETTER, BooleanExpression[]> entry : automaton2.getTransitionFunction().entrySet()){
			for(int i=0;i<automaton2.getStates().size();i++){
				if(entry.getValue()[i] != null){
					resultAutomaton.addTransition(
							entry.getKey(), 
							automaton2.getStates().get(i), 
							entry.getValue()[i].cloneShifted(shiftMap2, newSize));
				}
			}
		}
		resultAutomaton.addAcceptingConjunction(automaton1.getAcceptingFunction().cloneShifted(shiftMap1, newSize));
		resultAutomaton.addAcceptingConjunction(automaton2.getAcceptingFunction().cloneShifted(shiftMap2, newSize));
		resultAutomaton.setReversed(automaton1.isReversed());
	}
	private final AlternatingAutomaton<LETTER, STATE> resultAutomaton;

	@Override
	public String operationName(){
		return "AA_MergedUnion";
	}

	@Override
	public String startMessage(){
		return "Start: " + operationName();
	}

	@Override
	public String exitMessage(){
		return "Exit: " + operationName();
	}

	@Override
	public AlternatingAutomaton<LETTER, STATE> getResult() throws AutomataOperationCanceledException{
		return resultAutomaton;
	}

	@Override
	public boolean checkResult(StateFactory<STATE> stateFactory) throws AutomataLibraryException{
		return true;
	}
}
