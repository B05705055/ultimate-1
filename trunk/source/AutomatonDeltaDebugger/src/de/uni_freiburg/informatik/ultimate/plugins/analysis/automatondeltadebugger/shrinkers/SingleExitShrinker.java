/*
 * Copyright (C) 2015 Christian Schilling <schillic@informatik.uni-freiburg.de>
 * Copyright (C) 2009-2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE Automaton Delta Debugger.
 * 
 * The ULTIMATE Automaton Delta Debugger is free software: you can redistribute
 * it and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * The ULTIMATE Automaton Delta Debugger is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Automaton Delta Debugger. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7: If you modify the
 * ULTIMATE Automaton Delta Debugger, or any covered work, by linking or
 * combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE Automaton Delta Debugger grant you additional
 * permission to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.analysis.automatondeltadebugger.shrinkers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.IncomingReturnTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingCallTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.transitions.OutgoingReturnTransition;
import de.uni_freiburg.informatik.ultimate.util.relation.Pair;

/**
 * Removes states which only have one outgoing transition and bends over all
 * incoming transitions to the respective target state.
 * 
 * Example: Two simple chains of four states connected by internal transitions
 * "q1 -a-> q2 -b-> q4" and call-return transitions "q1 -c-> q3 -r/q1-> q4" can
 * be simplified by removing the states in the middle, i.e., q2 and q3 to
 * "q1 -a-> q4" and "q1 -c-> q4". There is an important difference, namely that
 * the internal b-transition and the return transition have been removed, which
 * may not always be reasonable. But often all that matters is that the target
 * state is still reachable.
 * 
 * This shrinker is best used together with a general transition shrinker to
 * raise the number of states with only one outgoing transition.
 * 
 * This class could also be generalized to states with more than one outgoing
 * transition, but then it is not clear where the transitions should be bent to,
 * and the data structures become more complicated.
 * 
 * @author Christian Schilling <schillic@informatik.uni-freiburg.de>
 */
public class SingleExitShrinker<LETTER, STATE>
		extends AShrinker<Pair<STATE, STATE>, LETTER, STATE> {
	@Override
	public INestedWordAutomaton<LETTER, STATE>
			createAutomaton(final List<Pair<STATE, STATE>> list) {
		// create fresh automaton
		final INestedWordAutomaton<LETTER, STATE> automaton = mFactory.create();
		
		// data structures to contain all transitive chains (forward & backward)
		final HashMap<STATE, STATE> left2right = new HashMap<STATE, STATE>();
		HashMap<STATE, STATE> right2left = new HashMap<STATE, STATE>();
		
		/*
		 * add states which are not a left-hand side in the list; also set up
		 * data structures which contain all transitive chains
		 */
		final HashSet<STATE> states =
				new HashSet<STATE>(mAutomaton.getStates());
		for (final Pair<STATE, STATE> pair : list) {
			final STATE source = pair.getFirst();
			final STATE target = pair.getSecond();
			
			// left-hand side state will be removed
			final boolean wasPresent = states.remove(source);
			assert wasPresent : "Pairs in the list should be left-unique.";
			
			// update transitive chains
			STATE lhs = right2left.remove(source);
			STATE rhs = left2right.remove(target);
			if (lhs == null) {
				// the removed state was (not yet) a unique target
				lhs = source;
			}
			if (rhs == null) {
				// the target state was (not yet) removed
				rhs = target;
			}
			left2right.put(lhs, rhs);
			right2left.put(rhs, lhs);
		}
		right2left = null; // not needed anymore
		mFactory.addStates(automaton, states);
		
		// add transitions which are still unconcerned by removing the states
		mFactory.addFilteredTransitions(automaton, mAutomaton);
		
		// add transitions which close a (transitive) chain of removed states
		for (final Entry<STATE, STATE> entry : left2right.entrySet()) {
			final STATE source = entry.getKey();
			final STATE transitiveTarget = entry.getValue();
			if ((transitiveTarget == null) ||
					(!states.contains(transitiveTarget))) {
				// source state is no entry of a transitive chain, ignore it
				continue;
			}
			// add missing transitions and bend them to transitive chain target
			for (final IncomingInternalTransition<LETTER, STATE> trans : mAutomaton
					.internalPredecessors(source)) {
				final STATE pred = trans.getPred();
				if (states.contains(pred)) {
					mFactory.addInternalTransition(automaton, pred,
							trans.getLetter(), transitiveTarget);
				}
			}
			for (final IncomingCallTransition<LETTER, STATE> trans : mAutomaton
					.callPredecessors(source)) {
				final STATE pred = trans.getPred();
				if (states.contains(pred)) {
					mFactory.addCallTransition(automaton, pred,
							trans.getLetter(), transitiveTarget);
				}
			}
			for (final IncomingReturnTransition<LETTER, STATE> trans : mAutomaton
					.returnPredecessors(source)) {
				final STATE linPred = trans.getLinPred();
				final STATE hierPred = trans.getHierPred();
				if (states.contains(linPred) && states.contains(hierPred)) {
					mFactory.addReturnTransition(automaton, trans.getLinPred(),
							hierPred, trans.getLetter(), transitiveTarget);
				}
			}
		}
		
		return automaton;
	}
	
	@Override
	public List<Pair<STATE, STATE>> extractList() {
		final ArrayList<Pair<STATE, STATE>> list =
				new ArrayList<Pair<STATE, STATE>>();
				
		/*
		 * check that there is exactly one internal/call/return successor which
		 * is not a self-loop
		 */
		chooseState: for (final STATE state : mAutomaton.getStates()) {
			STATE target = null;
			for (final OutgoingInternalTransition<LETTER, STATE> trans : mAutomaton
					.internalSuccessors(state)) {
				if ((target != null) && (!target.equals(state))) {
					continue chooseState;
				}
				target = trans.getSucc();
			}
			for (final OutgoingCallTransition<LETTER, STATE> trans : mAutomaton
					.callSuccessors(state)) {
				if ((target != null) && (!target.equals(state))) {
					continue chooseState;
				}
				target = trans.getSucc();
			}
			for (final OutgoingReturnTransition<LETTER, STATE> trans : mAutomaton
					.returnSuccessors(state)) {
				if ((target != null) && (!target.equals(state))) {
					continue chooseState;
				}
				target = trans.getSucc();
			}
			
			if (target != null) {
				// state has exactly one successor, add the pair
				list.add(new Pair<STATE, STATE>(state, target));
			}
		}
		return list;
	}
}