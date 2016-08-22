/*
 * Copyright (C) 2013-2015 Betim Musa (musab@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Carl Kuesters
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
/**
 * 
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.automatascriptinterpreter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.alternating.AlternatingAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.alternating.BooleanExpression;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StringFactory;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.Place;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.julian.PetriNetJulian;
import de.uni_freiburg.informatik.ultimate.automata.tree.TreeAutomatonBU;
import de.uni_freiburg.informatik.ultimate.core.model.models.ILocation;
import de.uni_freiburg.informatik.ultimate.core.model.results.IResultWithSeverity.Severity;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.plugins.generator.automatascriptinterpreter.TestFileInterpreter.LoggerSeverity;
import de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AtsASTNode;
import de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AST.AlternatingAutomatonAST;
import de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AST.AutomataDefinitionsAST;
import de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AST.NestedwordAutomatonAST;
import de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AST.PetriNetAutomatonAST;
import de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AST.PetriNetTransitionAST;
import de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AST.RankedAlphabetEntryAST;
import de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AST.TransitionListAST.Pair;
import de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AST.TreeAutomatonAST;
import de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AST.TreeAutomatonRankedAST;
import de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AST.TreeAutomatonTransitionAST;

/**
 * 
 * @author musab@informatik.uni-freiburg.de
 * 
 * Responsible for interpretation of automata definitions.
 *
 */
public class AutomataDefinitionInterpreter {
	
	/**
	 * A map from automaton name to automaton object, which contains for each automaton, that was defined in the automata
	 * definitions an entry. 
	 */
	private final Map<String,Object> mAutomata;
	/**
	 * Contains the location of current interpreting automaton.
	 */
	private ILocation mErrorLocation;
	private final IMessagePrinter mMessagePrinter;
	private final ILogger mLogger;
	private final IUltimateServiceProvider mServices;
	
	public AutomataDefinitionInterpreter(IMessagePrinter printer, ILogger logger, IUltimateServiceProvider services) {
		mAutomata = new HashMap<String, Object>();
		mMessagePrinter = printer;
		mLogger = logger;
		mServices = services;
	}
	
	/**
	 * 
	 * @param automata the definitions of automata
	 */
	public void interpret(AutomataDefinitionsAST automata) {
		final List<? extends AtsASTNode> children = automata.getListOfAutomataDefinitions();
		for (final AtsASTNode n : children) {
			if (n instanceof NestedwordAutomatonAST) {
				try {
					interpret((NestedwordAutomatonAST) n);
				} catch (final Exception e) {
					mMessagePrinter.printMessage(Severity.ERROR, LoggerSeverity.DEBUG, e.getMessage() 
							+ System.getProperty("line.separator") + e.getStackTrace(),
							"Exception thrown", n);
				}

			} else if (n instanceof PetriNetAutomatonAST) {
				try {
					interpret((PetriNetAutomatonAST) n);
				} catch (final Exception e) {
					mMessagePrinter.printMessage(Severity.ERROR, LoggerSeverity.DEBUG, e.getMessage() 
							+ System.getProperty("line.separator") + e.getStackTrace(), 
							"Exception thrown", n);
				}
			} else if (n instanceof AlternatingAutomatonAST){
				try {
					interpret((AlternatingAutomatonAST) n);
				} catch (final Exception e) {
					mMessagePrinter.printMessage(Severity.ERROR, LoggerSeverity.DEBUG, e.getMessage() 
							+ System.getProperty("line.separator") + e.getStackTrace(), 
							"Exception thrown", n);
				}
			} else if (n instanceof TreeAutomatonAST){
				try {
					interpret((TreeAutomatonAST) n);
				} catch (final Exception e) {
					mMessagePrinter.printMessage(Severity.ERROR, LoggerSeverity.DEBUG, e.getMessage() 
							+ System.getProperty("line.separator") + e.getStackTrace(), 
							"Exception thrown", n);
				}
			} else if (n instanceof TreeAutomatonRankedAST){
				try {
					interpret((TreeAutomatonRankedAST) n);
				} catch (final Exception e) {
					mMessagePrinter.printMessage(Severity.ERROR, LoggerSeverity.DEBUG, e.getMessage() 
							+ System.getProperty("line.separator") + e.getStackTrace(), 
							"Exception thrown", n);
				}
			}

		}
		
	}
	
	public void interpret(AlternatingAutomatonAST astNode) throws IllegalArgumentException{
		mErrorLocation = astNode.getLocation();
		final HashSet<String> alphabet = new HashSet<String>(astNode.getAlphabet());
		final AlternatingAutomaton<String, String> alternatingAutomaton = new AlternatingAutomaton<String, String>(alphabet, new StringFactory());
		//States
		final List<String> states = astNode.getStates();
		final List<String> finalStates = astNode.getFinalStates();
		for(final String state : states){
			alternatingAutomaton.addState(state);
			if(finalStates.contains(state)){
				alternatingAutomaton.setStateFinal(state);
			}
		}
		//Transitions
		for(final Entry<Pair<String, String>, Set<String>> entry : astNode.getTransitions().entrySet()){
			final String expression = entry.getValue().iterator().next();
			final LinkedList<BooleanExpression> booleanExpressions = parseBooleanExpressions(alternatingAutomaton, expression);
			for(final BooleanExpression booleanExpression : booleanExpressions){
				alternatingAutomaton.addTransition(entry.getKey().right, entry.getKey().left, booleanExpression);
			}
		}
		//Accepting Function
		final LinkedList<BooleanExpression> acceptingBooleanExpressions = parseBooleanExpressions(alternatingAutomaton, astNode.getAcceptingFunction());
		for(final BooleanExpression booleanExpression : acceptingBooleanExpressions){
			alternatingAutomaton.addAcceptingConjunction(booleanExpression);
		}
		alternatingAutomaton.setReversed(astNode.isReversed());
		mAutomata.put(astNode.getName(), alternatingAutomaton);
	}

	public void interpret(TreeAutomatonAST astNode) throws IllegalArgumentException{
		mErrorLocation = astNode.getLocation();
		
		TreeAutomatonBU<String, String> treeAutomaton = new TreeAutomatonBU<>();

		for (String ltr : astNode.getAlphabet())
			treeAutomaton.addFinalState(ltr);
		
		for (String s : astNode.getStates())
			treeAutomaton.addState(s);

		for (String is : astNode.getInitialStates())
			treeAutomaton.addInitialState(is);

		for (String fs : astNode.getFinalStates())
			treeAutomaton.addFinalState(fs);
		
		for (TreeAutomatonTransitionAST trans : astNode.getTransitions()) {
			if (trans.getSourceStates().size() == 0) {
				throw new UnsupportedOperationException("The TreeAutomaton format with initial states "
						+ "(and implicit symbol ranks) does not allow nullary rules, i.e.,"
						+ "rules where the source state list is empty");
			} else {
				treeAutomaton.addRule(trans.getSymbol(), trans.getSourceStates(), trans.getTargetState());
			}
		}
		mAutomata.put(astNode.getName(), treeAutomaton);
	}

	public void interpret(TreeAutomatonRankedAST astNode) throws IllegalArgumentException{
		mErrorLocation = astNode.getLocation();
		
		TreeAutomatonBU<String, String> treeAutomaton = new TreeAutomatonBU<>();
		String nullaryString = "elim0arySymbol_";

		List<RankedAlphabetEntryAST> ra = astNode.getRankedAlphabet();
		for (RankedAlphabetEntryAST rae : ra) {
			for (String ltr : rae.getAlphabet()) {
				treeAutomaton.addLetter(ltr);
				if (Integer.parseInt(rae.getRank()) == 0) {
					// our tree automata don't have 0-ary symbols right now 
					// (they use 1-ary, initial states, and adapted rules instead)
					// this converts 0-ary symbols accordingly
					String inState = nullaryString + ltr;
					treeAutomaton.addState(inState); 
					treeAutomaton.addInitialState(inState);
				}
			}
		}
		
		for (String s : astNode.getStates()) {
			treeAutomaton.addState(s);
		}
		
		for (String fs : astNode.getFinalStates()) {
			treeAutomaton.addFinalState(fs);
		}
		
		for (TreeAutomatonTransitionAST trans : astNode.getTransitions()) {
			if (trans.getSourceStates().size() == 0) {
				treeAutomaton.addRule(trans.getSymbol(), 
						Collections.singletonList(nullaryString + trans.getSymbol()), 
						trans.getTargetState());
			} else {
				treeAutomaton.addRule(trans.getSymbol(), trans.getSourceStates(), trans.getTargetState());
			}
		}
		mAutomata.put(astNode.getName(), treeAutomaton);
	}

	private static LinkedList<BooleanExpression> parseBooleanExpressions(AlternatingAutomaton<String, String> alternatingAutomaton, String expression){
		final LinkedList<BooleanExpression> booleanExpressions = new LinkedList<BooleanExpression>();
		if(expression.equals("true")){
			booleanExpressions.add(new BooleanExpression(new BitSet(), new BitSet()));
		}
		else if(expression.equals("false")){
			//Not supported yet
		}
		else{
			final String[] disjunctiveExpressions = expression.split("\\|");
			for(final String disjunctiveExpression : disjunctiveExpressions){
				final String[] stateExpressions = disjunctiveExpression.split("&");
				final LinkedList<String> resultStates = new LinkedList<String>();
				final LinkedList<String> negatedResultStates = new LinkedList<String>();
				for(final String stateExpression : stateExpressions){
					if(stateExpression.startsWith("~")){
						negatedResultStates.add(stateExpression.substring(1));
					}
					else{
						resultStates.add(stateExpression);
					}
				}
				final BooleanExpression booleanExpression = alternatingAutomaton.generateCube(
						resultStates.toArray(new String[resultStates.size()]), 
						negatedResultStates.toArray(new String[negatedResultStates.size()]));
				booleanExpressions.add(booleanExpression);
			}
		}
		return booleanExpressions;
	}
	
	public void interpret(NestedwordAutomatonAST nwa) throws IllegalArgumentException {
		mErrorLocation = nwa.getLocation();
		final Set<String> internalAlphabet = new HashSet<String>(nwa.getInternalAlphabet());
		final Set<String> callAlphabet = new HashSet<String>(nwa.getCallAlphabet());
		final Set<String> returnAlphabet = new HashSet<String>(nwa.getReturnAlphabet());
		
		final NestedWordAutomaton<String, String> nw = new NestedWordAutomaton<String, String>(
				new AutomataLibraryServices(mServices),
				Collections.unmodifiableSet(internalAlphabet), 
				Collections.unmodifiableSet(callAlphabet), 
				Collections.unmodifiableSet(returnAlphabet), 
				new StringFactory());
		
		/*
		 * Now add the states to the NestedWordAutomaton 
		 */
		final List<String> initStates = nwa.getInitialStates();
		final List<String> finalStates = nwa.getFinalStates();
		
		final Set<String> allStates = new HashSet<>(nwa.getStates());
		for (final String init : initStates) {
			if (!allStates.contains(init)) {
				throw new IllegalArgumentException("Initial state " + init + " not in set of states");
			}
		}
		for (final String fin : finalStates) {
			if (!allStates.contains(fin)) {
				throw new IllegalArgumentException("Final state " + fin + " not in set of states");
			}
		}

		
		for (final String state : nwa.getStates()) {
			if (initStates.contains(state)) {
				if (finalStates.contains(state)) {
					nw.addState(true, true, state);
				} else {
					nw.addState(true, false, state);
				}
			} else if (finalStates.contains(state)) {
				nw.addState(false, true, state);
			} else {
				nw.addState(false, false, state);
			}
		}
		
		
		/*
		 * Now add the transitions to the NestedWordAutomaton
		 */
		for (final Entry<Pair<String, String>, Set<String>> entry : nwa.getInternalTransitions().entrySet()) {
			for (final String succ : entry.getValue()) {
				nw.addInternalTransition(entry.getKey().left, entry.getKey().right, succ);
			}
			
		}
		
		for (final Entry<Pair<String, String>, Set<String>> entry : nwa.getCallTransitions().entrySet()) {
			for (final String succ : entry.getValue()) { 
				nw.addCallTransition(entry.getKey().left, entry.getKey().right, succ);
			}
		}
		
		for ( final String linPred  : nwa.getReturnTransitions().keySet()) {
			for (final String hierPred : nwa.getReturnTransitions().get(linPred).keySet()) {
				for (final String letter : nwa.getReturnTransitions().get(linPred).get(hierPred).keySet()) {
					for (final String succ : nwa.getReturnTransitions().get(linPred).get(hierPred).get(letter)) {
						nw.addReturnTransition(linPred, hierPred, letter, succ);
					}
				}
			}
		}
		mAutomata.put(nwa.getName(), nw);
		
	}
	
	public void interpret(PetriNetAutomatonAST pna) throws IllegalArgumentException {
		mErrorLocation = pna.getLocation();
		final PetriNetJulian<String, String> net = new PetriNetJulian<String, String>(
				new AutomataLibraryServices(mServices),
				new HashSet<String>(pna.getAlphabet()), 
				new StringFactory(), false);
		final Map<String, Place<String, String>> name2places = new HashMap<String, Place<String, String>>();
		// Add the places
		for (final String p : pna.getPlaces()) {
			final Place<String, String> place = net.addPlace(p, 
					pna.getInitialMarkings().containsPlace(p), 
					pna.getAcceptingPlaces().contains(p));
			name2places.put(p, place);
		}


		// Add the transitions
		for (final PetriNetTransitionAST ptrans : pna.getTransitions()) {
			final Collection<Place<String,String>> preds = new ArrayList<Place<String,String>>();
			final Collection<Place<String,String>> succs = new ArrayList<Place<String,String>>();
			for (final String pred : ptrans.getPreds()) {
				if (!name2places.containsKey(pred)) {
					throw new IllegalArgumentException("undefined place:" + pred);
				} else {
					preds.add(name2places.get(pred));
				}
			}
			for (final String succ : ptrans.getSuccs()) {
				if (!name2places.containsKey(succ)) {
					throw new IllegalArgumentException("undefined place:" + succ);
				} else {
					succs.add(name2places.get(succ));
				}
			}
			net.addTransition(ptrans.getSymbol(), preds, succs);
		}

		mAutomata.put(pna.getName(), net);
	}
	
	public Map<String, Object> getAutomata() {
		return mAutomata;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("AutomataDefinitionInterpreter [");
		if (mAutomata != null) {
			builder.append("#AutomataDefinitions: ");
			builder.append(mAutomata.size());
		}
		builder.append("]");
		return builder.toString();
	}

	
	public ILocation getErrorLocation() {
		return mErrorLocation;
	}
	
}
