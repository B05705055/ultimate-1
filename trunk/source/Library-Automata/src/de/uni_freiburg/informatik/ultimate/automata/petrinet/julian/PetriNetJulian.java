/*
 * Copyright (C) 2011-2015 Julian Jarecki (jareckij@informatik.uni-freiburg.de)
 * Copyright (C) 2011-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.automata.petrinet.julian;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.LibraryIdentifiers;
import de.uni_freiburg.informatik.ultimate.automata.ResultChecker;
import de.uni_freiburg.informatik.ultimate.automata.Word;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedRun;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.IPetriNet;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.ITransition;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.Marking;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.PetriNetRun;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.Place;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.julian.petruchio.EmptinessPetruchio;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;

public class PetriNetJulian<S, C> implements IPetriNet<S, C> {
	
	private final AutomataLibraryServices mServices;

	@SuppressWarnings("unused")
	private final ILogger mLogger;

	private final Set<S> mAlphabet;
	private final StateFactory<C> mStateFactory;

	private final Collection<Place<S, C>> mPlaces = new HashSet<Place<S, C>>();
	private final Set<Place<S, C>> mInitialPlaces = new HashSet<Place<S, C>>();
	private final Collection<Place<S, C>> mAcceptingPlaces = new HashSet<Place<S, C>>();
	private final Collection<ITransition<S, C>> mTransitions = new HashSet<ITransition<S, C>>();

	/**
	 * If true the number of tokens in this petri net is constant. Formally:
	 * There is a natural number n such that every reachable marking consists of
	 * n places.
	 */
	private final boolean mConstantTokenAmount;

	public PetriNetJulian(final AutomataLibraryServices services, final Set<S> alphabet,
			final StateFactory<C> stateFactory, final boolean constantTokenAmount) {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		this.mAlphabet = alphabet;
		this.mStateFactory = stateFactory;
		this.mConstantTokenAmount = constantTokenAmount;
		assert (!constantTokenAmount() || transitionsPreserveTokenAmount());
	}

	public PetriNetJulian(final AutomataLibraryServices services, 
			final INestedWordAutomaton<S, C> nwa)
			throws AutomataLibraryException {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(LibraryIdentifiers.PLUGIN_ID);
		mAlphabet = nwa.getInternalAlphabet();
		mStateFactory = nwa.getStateFactory();
		this.mConstantTokenAmount = true;
		final Map<C, Place<S, C>> state2place = new HashMap<C, Place<S, C>>();
		for (final C content : nwa.getStates()) {
			// C content = state.getContent();
			final boolean isInitial = nwa.isInitial(content);
			final boolean isAccepting = nwa.isFinal(content);
			final Place<S, C> place = this.addPlace(content, isInitial, isAccepting);
			state2place.put(content, place);
		}
		Collection<Place<S, C>> succPlace;
		Collection<Place<S, C>> predPlace;
		for (final C content : nwa.getStates()) {
			predPlace = new ArrayList<Place<S, C>>(1);
			predPlace.add(state2place.get(content));
			for (final OutgoingInternalTransition<S, C> trans :
					nwa.internalSuccessors(content)) {
				succPlace = new ArrayList<Place<S, C>>(1);
				succPlace.add(state2place.get(trans.getSucc()));
				this.addTransition(trans.getLetter(), predPlace, succPlace);
			}
		}

		// for (NestedWordAutomaton<S, C>.InternalTransition iTrans : nwa
		// .getInternalTransitions()) {
		// predPlace = new ArrayList<Place<S, C>>(1);
		// predPlace
		// .add(state2place.get(iTrans.getPredecessor().getContent()));
		// S symbol = iTrans.getSymbol();
		// succPlace = new ArrayList<Place<S, C>>(1);
		// succPlace.add(state2place.get(iTrans.getSuccessor().getContent()));
		// this.addTransition(symbol, predPlace, succPlace);
		// }

		assert (!constantTokenAmount() || transitionsPreserveTokenAmount());
		assert ResultChecker.petriNetJulian(mServices, nwa, this);
	}

	public Place<S, C> addPlace(final C content, final boolean isInitial, final boolean isFinal) {
		final Place<S, C> place = new Place<S, C>(content);
		mPlaces.add(place);
		if (isInitial) {
			mInitialPlaces.add(place);
		}
		if (isFinal) {
			mAcceptingPlaces.add(place);
		}
		return place;
	}

	public Transition<S, C> addTransition(final S symbol,
			final Collection<Place<S, C>> preds, final Collection<Place<S, C>> succs) {
		if (!mAlphabet.contains(symbol)) {
			throw new IllegalArgumentException("unknown letter: " + symbol);
		}
		final Transition<S, C> transition = new Transition<S, C>(symbol, preds,
				succs, mTransitions.size());
		for (final Place<S, C> pred : preds) {
			if (!mPlaces.contains(pred)) {
				throw new IllegalArgumentException("unknown place");
			}
			pred.addSuccessor(transition);
		}
		for (final Place<S, C> succ : succs) {
			if (!mPlaces.contains(succ)) {
				throw new IllegalArgumentException("unknown place");
			}
			succ.addPredecessor(transition);
		}
		mTransitions.add(transition);
		return transition;
	}

	/**
	 * Hack to satisfy requirements from IPetriNet. Used by visualization
	 */
	@Override
	public Collection<Collection<Place<S, C>>> getAcceptingMarkings() {
		final ArrayList<Collection<Place<S, C>>> list = new ArrayList<Collection<Place<S, C>>>();
		list.add(mAcceptingPlaces);
		return list;
	}

	// public Collection<ITransition<S, C>> getEnabledTransitions(
	// Collection<Place<S, C>> marking) {
	// return CollectionExtension.from(transitions).filter(
	// new IPredicate<ITransition<S, C>>() {
	// @Override
	// public boolean test(ITransition<S, C> t) {
	// return false;
	// }
	// });
	// }

	public boolean isTransitionEnabled(final ITransition<S, C> transition,
			final Collection<Place<S, C>> marking) {
		return marking.containsAll(transition.getPredecessors());
	}

	public Collection<Place<S, C>> fireTransition(final ITransition<S, C> transition,
			final Collection<Place<S, C>> marking) {

		marking.removeAll(transition.getPredecessors());
		marking.addAll(transition.getSuccessors());

		return marking;
	}

	@Override
	public Set<S> getAlphabet() {
		return mAlphabet;
	}

	@Override
	public StateFactory<C> getStateFactory() {
		return mStateFactory;
	}

	@Override
	public Collection<Place<S, C>> getPlaces() {
		return mPlaces;
	}

	@Override
	public Marking<S, C> getInitialMarking() {
		return new Marking<S, C>(mInitialPlaces);
	}

	public Collection<Place<S, C>> getAcceptingPlaces() {
		return mAcceptingPlaces;
	}

	@Override
	public Collection<ITransition<S, C>> getTransitions() {
		return mTransitions;
	}

	/**
	 * if true, then the number of tokens in the net is constant (= size of
	 * initial marking) during every run of the net
	 */
	public boolean constantTokenAmount() {
		return mConstantTokenAmount;
	}

	@Override
	public boolean isAccepting(final Marking<S, C> marking) {
		for (final Place<S, C> place : marking) {
			if (getAcceptingPlaces().contains(place)) {
				return true;
			}
		}
		return false;
	}



	public PetriNetRun<S, C> acceptingRun() throws AutomataLibraryException {
		// NestedRun<S, C> test = getAcceptingNestedRun();
		// System.out.print(test);
		return (new PetriNetUnfolder<S, C>(mServices, this, PetriNetUnfolder.order.ERV,
				false, true)).getAcceptingRun();
	}

	public NestedRun<S, C> getAcceptingNestedRun() throws AutomataLibraryException {
		final EmptinessPetruchio<S, C> ep = new EmptinessPetruchio<S, C>(mServices, this);
		final NestedRun<S, C> result = ep.getResult();

		// NestedRun<S,C> result = (new
		// PetriNet2FiniteAutomaton<S,C>(this)).getResult().
		// getAcceptingNestedRun();
		return result;
	}

	boolean transitionsPreserveTokenAmount() {
		for (final ITransition<S, C> t : this.getTransitions()) {
			if (t.getPredecessors().size() != t.getSuccessors().size()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int size() {
		return mPlaces.size();
	}

	@Override
	public String sizeInformation() {
		return "has " + mPlaces.size() + "places, " + mTransitions.size()
				+ " transitions";
	}

	@Override
	public boolean accepts(final Word<S> word) {
		throw new UnsupportedOperationException();
	}
}
