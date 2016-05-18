/*
 * Copyright (C) 2014-2015 Markus Pomrehn
 * Copyright (C) 2014-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2009-2015 University of Freiburg
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
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.visualization;

import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.alternating.AlternatingAutomaton;
import de.uni_freiburg.informatik.ultimate.core.model.models.IElement;


public class AAToUltimateModel<LETTER,STATE> {
//	private static ILogger m_Logger = 
//		NestedWordAutomata.getLogger();
//	
	public IElement getUltimateModelOfAA(AlternatingAutomaton<LETTER,STATE> aaSimple) throws AutomataOperationCanceledException {
//		final AlternatingAutomaton<LETTER,STATE> aa;
//		aa = (AlternatingAutomaton<LETTER, STATE>) aaSimple;
		System.out.println("Foo");
		AutomatonState graphroot = new AutomatonState("Sucessors of this node are the" +
					" initial states",false);	
//		Map<STATE,AutomatonState> constructed =	new HashMap<STATE,AutomatonState>();
//		LinkedList<STATE> queue = new LinkedList<STATE>();
//	
//		// add all initial states to model - all are successors of the graphroot
//		for (STATE state : aa.getInitialStates()) {
//			queue.add(state);
//			AutomatonState vsn = new AutomatonState(state,
//									nwa.isFinal(state));
//			constructed.put(state,vsn);
//			new AutomatonTransition((AutomatonState) graphroot,
//											Transition.INTERNAL,"", null, vsn);
//		}
//		
//		while (!queue.isEmpty()) {
//			STATE state = queue.removeFirst();
//			AutomatonState vsn = constructed.get(state);
//			
//			for (OutgoingInternalTransition<LETTER, STATE> trans : 
//												nwa.internalSuccessors(state)) {
//				LETTER symbol = trans.getLetter();
//				STATE succState = trans.getSucc();
//				AutomatonState succVSN;
//				if (constructed.containsKey(succState)) {
//					succVSN = constructed.get(succState);
//				}
//				else {
//					succVSN = new AutomatonState(succState,
//							nwa.isFinal(succState));
//					m_Logger.debug("Creating Node: " + succVSN.toString());
//					constructed.put(succState,succVSN);
//					queue.add(succState);
//				}
//				new AutomatonTransition(vsn,Transition.INTERNAL,symbol,null,succVSN);
//			}
//			
//			for (OutgoingCallTransition<LETTER, STATE> trans : 
//													nwa.callSuccessors(state)) {
//				LETTER symbol = trans.getLetter();
//				STATE succState = trans.getSucc();
//				AutomatonState succVSN;
//				if (constructed.containsKey(succState)) {
//					succVSN = constructed.get(succState);
//				} else {
//					succVSN = new AutomatonState(succState,
//							nwa.isFinal(succState));
//					m_Logger.debug("Creating Node: " + succVSN.toString());
//					constructed.put(succState, succVSN);
//					queue.add(succState);
//				}
//				new AutomatonTransition(vsn, Transition.CALL, symbol, null,	succVSN);
//			}
//			for(STATE hierPredState : nwa.getStates()) {
//				for (OutgoingReturnTransition<LETTER, STATE> trans : 
//							nwa.returnSuccessorsGivenHier(state, hierPredState)) {
//					LETTER symbol = trans.getLetter();
//					STATE succState = trans.getSucc();
//					AutomatonState succVSN;
//					if (constructed.containsKey(succState)) {
//						succVSN = constructed.get(succState);
//					} else {
//						succVSN = new AutomatonState(succState,nwa.isFinal(succState));
//						m_Logger.debug("Creating Node: " + succVSN.toString());
//						constructed.put(succState, succVSN);
//						queue.add(succState);
//					}
//					new AutomatonTransition(vsn, Transition.RETURN, symbol,
//							hierPredState.toString(), succVSN);
//				}
//			}
//		}
		return graphroot;
	}
	

}
