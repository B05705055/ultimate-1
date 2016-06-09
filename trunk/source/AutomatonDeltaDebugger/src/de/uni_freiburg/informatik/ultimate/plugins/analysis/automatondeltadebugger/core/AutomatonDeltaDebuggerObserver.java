/*
 * Copyright (C) 2015-2016 Christian Schilling (schillic@informatik.uni-freiburg.de)
 * Copyright (C) 2015-2016 University of Freiburg
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
package de.uni_freiburg.informatik.ultimate.plugins.analysis.automatondeltadebugger.core;

import java.util.List;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.core.lib.observers.BaseObserver;
import de.uni_freiburg.informatik.ultimate.core.model.models.IElement;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.automatondeltadebugger.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.automatondeltadebugger.factories.AAutomatonFactory;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.automatondeltadebugger.factories.NestedWordAutomatonFactory;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.automatondeltadebugger.shrinkers.AShrinker;
import de.uni_freiburg.informatik.ultimate.plugins.generator.automatascriptinterpreter.AutomataDefinitionInterpreter;
import de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AST.AutomataDefinitionsAST;
import de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AST.AutomataTestFileAST;

/**
 * Obeserver which initializes the delta debugging process.
 * 
 * @see AutomatonDebugger
 * @author Christian Schilling <schillic@informatik.uni-freiburg.de>
 */
public class AutomatonDeltaDebuggerObserver<LETTER, STATE>
		extends BaseObserver {
	private final IUltimateServiceProvider mServices;
	private final ATester<LETTER, STATE> mTester;
	private final List<AShrinker<?, LETTER, STATE>> mShrinkersLoop;
	private final List<AShrinker<?, LETTER, STATE>> mShrinkersEnd;
	private final ILogger mLogger;
	
	/**
	 * @param services Ultimate services
	 * @param tester tester
	 * @param shrinkersLoop rules to be appplied iteratively
	 * @param shrinkersEnd rules to be applied once in the end
	 */
	public AutomatonDeltaDebuggerObserver(
			final IUltimateServiceProvider services,
			final ATester<LETTER, STATE> tester,
			final List<AShrinker<?, LETTER, STATE>> shrinkersLoop,
			final List<AShrinker<?, LETTER, STATE>> shrinkersEnd) {
		mServices = services;
		mTester = tester;
		mLogger = services.getLoggingService().getLogger(Activator.PLUGIN_ID);
		mShrinkersLoop = shrinkersLoop;
		mShrinkersEnd = shrinkersEnd;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean process(final IElement root) throws Throwable {
		if (!(root instanceof AutomataTestFileAST)) {
			return true;
		}
		final AutomataDefinitionsAST automataDefinition =
				((AutomataTestFileAST) root).getAutomataDefinitions();
		final AutomataDefinitionInterpreter adi =
				new AutomataDefinitionInterpreter(null, mLogger, mServices);
		adi.interpret(automataDefinition);
		final Map<String, Object> automata = adi.getAutomata();
		// design decision: take the first automaton from the iterator
		INestedWordAutomaton<LETTER, STATE> automaton = null;
		for (final Object object : automata.values()) {
			if (object instanceof INestedWordAutomaton<?, ?>) {
				automaton = (INestedWordAutomaton<LETTER, STATE>) object;
				break;
			}
		}
		if (automaton == null) {
			mLogger.info("The input file did not contain any nested word " +
					"automaton (type INestedWordAutomaton).");
			return true;
		}
		deltaDebug(automaton);
		return false;
	}
	
	/**
	 * initializes and runs the delta debugging process
	 * 
	 * NOTE: A user may want to change the type of automaton factory here.
	 * 
	 * @param automaton input automaton
	 */
	private void deltaDebug(INestedWordAutomaton<LETTER, STATE> automaton) {
		// automaton factory
		final AAutomatonFactory<LETTER, STATE> automatonFactory =
				new NestedWordAutomatonFactory<LETTER, STATE>(automaton,
						mServices);
		
		// construct delta debugger
		final AutomatonDebugger<LETTER, STATE> debugger =
				new AutomatonDebugger<LETTER, STATE>(automaton,
						automatonFactory, mTester);
		
		// execute delta debugger (binary search)
		final INestedWordAutomaton<LETTER, STATE> result =
				debugger.shrink(mShrinkersLoop, mShrinkersEnd);
		
		// print result
		mLogger.info(
				"The automaton debugger terminated, resulting in the following automaton:");
		mLogger.info(result);
	}
}
