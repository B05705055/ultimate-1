/*
 * Copyright (C) 2009-2014 University of Freiburg
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
 * along with the ULTIMATE Automata Library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Automata Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE Automata Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.automata.petrinet.julian.petruchio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import petruchio.cov.Backward;
import petruchio.cov.SimpleList;
import petruchio.interfaces.petrinet.Place;
import petruchio.interfaces.petrinet.Transition;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.NestedWordAutomata;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedRun;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.IsEmpty;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.PetriNet2FiniteAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.julian.PetriNetJulian;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;

/**
 * Check if a PetriNetJulian has an accepting run.
 * The emptiness check uses Tim Straznys Petruchio.
 * A marking of a PetriNetJulian is accepting if the marking contains an 
 * accepting place.
 * EmptinessPetruchio checks if any (singleton) marking {p} such that p is an
 * accepting place can be covered.
 * 
 * @author heizmann@informatik.uni-freiburg.de
 *
 * @param <S> Type of alphabet symbols
 * @param <C> Type of place labeling
 */

public class EmptinessPetruchio<S,C> implements IOperation<S,C> {
	
	private final IUltimateServiceProvider m_Services;
	private static Logger s_Logger = 
		NestedWordAutomata.getLogger();
	
	@Override
	public String operationName() {
		return "emptinessPetruchio";
	}
	
	@Override
	public String startMessage() {
		return "Start emptinessPetruchio. " +
				"Operand " + m_NetJulian.sizeInformation();
	}
	
	@Override
	public String exitMessage() {
		return "Finished emptinessPetruchio";
	}



	final PetruchioWrapper<S, C> m_Petruchio;

	final PetriNetJulian<S,C> m_NetJulian;

	NestedRun<S,C> m_AcceptedRun = null;
	
	public EmptinessPetruchio(IUltimateServiceProvider services, 
			PetriNetJulian<S,C> net) {
		m_Services = services;
		m_NetJulian = net;
		s_Logger.info(startMessage());
		m_Petruchio = new PetruchioWrapper<S,C>(net);
		m_AcceptedRun = constructAcceptingRun();
		s_Logger.info(exitMessage());
	}
	
	
	
	/**
	 * 
	 * @return Some accepting run if the Petri net has an accepting run, null
	 * otherwise. 
	 * Note: A run should be an interleaved sequence of markings and symbols.
	 * Here each marking will be null instead. 
	 */
	private NestedRun<S,C> constructAcceptingRun() {
		s_Logger.debug("Net has "+m_NetJulian.getPlaces().size()+ " Places");
		s_Logger.debug("Net has "+m_NetJulian.getTransitions().size()+ " Transitions");
		
		// construct single invariant p_1 + ... + p_n where p_i \in places
		Collection<Map<Place, Integer>> invariants = new ArrayList<Map<Place, Integer>>(1);
		Map<Place, Integer> theInvariant = 
			new IdentityHashMap<Place, Integer>(m_Petruchio.getNet().getPlaces().size());
		for(Place pPetruchio : m_Petruchio.getNet().getPlaces()) {
		   theInvariant.put(pPetruchio, Integer.valueOf(1));
		}
		invariants.add(theInvariant);
		
		// construct the following target:
		// at least one of { {p_j} | p_j \in Paccepting } is coverable
		Collection<Map<Place, Integer>> targets = new ArrayList<Map<Place, Integer>>();
		for(de.uni_freiburg.informatik.ultimate.automata.petrinet.Place<S,C> pAcceptingJulian : m_NetJulian.getAcceptingPlaces()) {
			// construct single target pAccepting >= 1
			Place pAcceptingPetruchio = m_Petruchio.getpJulian2pPetruchio().get(pAcceptingJulian);
			Map<Place, Integer> placeWithOneToken = new IdentityHashMap<Place, Integer>();
			placeWithOneToken.put(pAcceptingPetruchio,1);
			targets.add(placeWithOneToken);
		}
		
		s_Logger.debug("Check coverability of " + m_NetJulian.getAcceptingPlaces().toString());
		s_Logger.warn(targets);
		SimpleList<Transition> tracePetruchio = 
			Backward.checkCoverability(m_Petruchio.getNet(), targets);//, invariants);
		s_Logger.debug("done");
		
		if (tracePetruchio == null) {
			return null;
			
		}
		else {
			return tracePetruchio2run(tracePetruchio);
		}
		
	}
		


	/**
	 * Translate sequence of Petruchio transitions to run of PetriNet
	 * @param tracePetruchio
	 * @return
	 */
	private NestedRun<S,C> tracePetruchio2run(SimpleList<Transition> tracePetruchio) {
		
		NestedRun<S,C> result = new NestedRun<S,C>(null);
		// workaround if initial marking can be covered petruchio deliveres a
		// list with one element that is null
		if (tracePetruchio.getLength() == 1 &&
				tracePetruchio.iterator().next() == null) {
			return result;
		}
		for(Transition tPetruchio : tracePetruchio) {
			S symbol = m_Petruchio.gettPetruchio2tJulian().get(tPetruchio).getSymbol();
			NestedRun<S,C> oneStepSubrun = 
				new NestedRun<S,C>(null, symbol,NestedWord.INTERNAL_POSITION, null);
			result = result.concatenate(oneStepSubrun);
		}
		return result;
	}
	
	
	public NestedRun<S,C> getResult() throws AutomataLibraryException {
		return m_AcceptedRun;
	}

	@Override
	public boolean checkResult(StateFactory<C> stateFactory)
			throws AutomataLibraryException {
		s_Logger.info("Testing correctness of emptinessCheck");

		boolean correct = true;
		if (m_AcceptedRun == null) {
			NestedRun automataRun = (new IsEmpty((new PetriNet2FiniteAutomaton(m_Services, m_NetJulian)).getResult())).getNestedRun();
			correct = (automataRun == null);
		} else {
			correct =  m_NetJulian.accepts(m_AcceptedRun.getWord());
		}
		s_Logger.info("Finished testing correctness of emptinessCheck");
		return correct;
	}



}
