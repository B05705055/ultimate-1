/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * Copyright (C) 2013-2015 Vincent Langenfeld (langenfv@informatik.uni-freiburg.de)
 *
 * This file is part of the ULTIMATE DebugPlugin plug-in.
 *
 * The ULTIMATE DebugPlugin plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE DebugPlugin plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE DebugPlugin plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE DebugPlugin plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE DebugPlugin plug-in grant you additional permission
 * to convey the resulting work.
 */
package tw.ntu.svvrl.ultimate.debugplugin;

import java.util.ArrayList;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.*;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.explorer.NeverClaimAutExplorer;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.explorer.ProgramStateExplorer;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.neverstate.NeverState;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.ProgramState;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.ProgramStateTransition;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.threadstate.ThreadStateTransition;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.ProgramState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedWordAutomataSizeBenchmark;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.boogie.annotation.LTLPropertyCheck;
import de.uni_freiburg.informatik.ultimate.core.model.models.IElement;
import de.uni_freiburg.informatik.ultimate.core.model.models.ModelType;
import de.uni_freiburg.informatik.ultimate.core.model.observers.IUnmanagedObserver;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IcfgEdge;
import de.uni_freiburg.informatik.ultimate.ltl2aut.never2nwa.NWAContainer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.BoogieIcfgContainer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.util.IcfgSizeBenchmark;

/**
 *
 * @author Hong-Yang Lin
 *
 */
public class DebugPluginObserver implements IUnmanagedObserver {
	
	private final ILogger mLogger;
	private BoogieIcfgContainer mRcfg;
	private NWAContainer mNeverClaimNWAContainer;
	private final IUltimateServiceProvider mServices;
	
	private ModelCheckerAssistant mModelCheckerAssistant;

	public DebugPluginObserver(final ILogger logger, final IUltimateServiceProvider services) {
		mLogger = logger;
		mServices = services;
		mRcfg = null;
		mNeverClaimNWAContainer = null;
		mModelCheckerAssistant = null;
	}

	@Override
	public void init(final ModelType modelType, final int currentModelIndex, final int numberOfModels) {
		// no initialization needed
	}

	@Override
	public void finish() throws Throwable {
		if (mNeverClaimNWAContainer == null || mRcfg == null) {
			return;
		}

		// measure size of nwa and rcfg
		reportSizeBenchmark("Initial property automaton", mNeverClaimNWAContainer.getValue());
		reportSizeBenchmark("Initial RCFG", mRcfg);

		mLogger.info("Do something with these two models...");
		// new crawler here. 
		mModelCheckerAssistant = new ModelCheckerAssistant(mNeverClaimNWAContainer.getValue(), mRcfg, mLogger, mServices);
		//mModelCheckerAssistant = new ModelCheckerAssistant(mRcfg, mLogger, mServices);
		
		TestVerifierDebug v = new TestVerifierDebug(mLogger, mModelCheckerAssistant);
		//TestVerifier v = new TestVerifier(mLogger, mModelCheckerAssistant);
		v.run();
		
		/*-----------debugging-----------*/
//		Set<ProgramState> pInitialStates = new HashSet<>();
//		pInitialStates = mModelCheckerAssistant.getProgramInitialStates();
//		
////		Set<NeverState> nInitialStates = new HashSet<>();
////		nInitialStates = mModelCheckerAssistant.getNeverInitialStates();
////		NeverState n = ((NeverState) nInitialStates.toArray()[0]);
//		
//		
//		ProgramState a = ((ProgramState) pInitialStates.toArray()[0]);
//		List<ProgramStateTransition> edges = mModelCheckerAssistant.getProgramEnabledTrans(a);
//		ProgramStateTransition edge = edges.get(0);
//		ProgramState b = mModelCheckerAssistant.doProgramTransition(a, edge);
		
//		if(mModelCheckerAssistant.globalVarsInitialized(b)) {
//			List<OutgoingInternalTransition<CodeBlock, NeverState>> nedges = mModelCheckerAssistant.getNeverEnabledTrans(n, b);
//			if(nedges.size() > 0) {
//				OutgoingInternalTransition<CodeBlock, NeverState> nedge = nedges.get(0);
//				NeverState m = mModelCheckerAssistant.doNeverTransition(n, nedge, b);
//				n = m;
//			}
//		}
		
//		for(int i = 0; i < 7000; i++) {
//			edges = mModelCheckerAssistant.getProgramEnabledTrans(b);
//			edge = edges.get(0);
//			b = mModelCheckerAssistant.doProgramTransition(b, edge);
//		}
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(b);
//		edge = edges.get(0);
//		ProgramState c = mModelCheckerAssistant.doProgramTransition(b, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(c);
//		edge = edges.get(0);
//		ProgramState d = mModelCheckerAssistant.doProgramTransition(c, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(d);
//		edge = edges.get(0);
//		ProgramState e = mModelCheckerAssistant.doProgramTransition(d, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(e);
//		edge = edges.get(0);
//		ProgramState f = mModelCheckerAssistant.doProgramTransition(e, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(f);
//		edge = edges.get(0);
//		ProgramState g = mModelCheckerAssistant.doProgramTransition(f, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(g);
//		edge = edges.get(0);
//		ProgramState h = mModelCheckerAssistant.doProgramTransition(g, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(h);
//		edge = edges.get(0);
//		ProgramState i = mModelCheckerAssistant.doProgramTransition(h, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(i);
//		edge = edges.get(0);
//		ProgramState j = mModelCheckerAssistant.doProgramTransition(i, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(j);
//		edge = edges.get(0);
//		ProgramState k = mModelCheckerAssistant.doProgramTransition(j, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(k);
//		edge = edges.get(0);
//		ProgramState l = mModelCheckerAssistant.doProgramTransition(k, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(l);
//		edge = edges.get(0);
//		ProgramState o = mModelCheckerAssistant.doProgramTransition(l, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(o);
//		edge = edges.get(0);
//		ProgramState p = mModelCheckerAssistant.doProgramTransition(o, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(p);
//		edge = edges.get(0);
//		ProgramState q = mModelCheckerAssistant.doProgramTransition(p, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(q);
//		edge = edges.get(0);
//		ProgramState r = mModelCheckerAssistant.doProgramTransition(q, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(r);
//		edge = edges.get(0);
//		ProgramState s = mModelCheckerAssistant.doProgramTransition(r, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(s);
//		edge = edges.get(0);
//		ProgramState t = mModelCheckerAssistant.doProgramTransition(s, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(t);
//		edge = edges.get(0);
//		ProgramState u = mModelCheckerAssistant.doProgramTransition(t, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(u);
//		edge = edges.get(0);
//		ProgramState v = mModelCheckerAssistant.doProgramTransition(u, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(v);
//		edge = edges.get(0);
//		ProgramState w = mModelCheckerAssistant.doProgramTransition(v, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(w);
//		edge = edges.get(0);
//		ProgramState x = mModelCheckerAssistant.doProgramTransition(w, edge);
//				
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(x);
//		edge = edges.get(0);
//		ProgramState y = mModelCheckerAssistant.doProgramTransition(x, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(y);
//		edge = edges.get(0);
//		ProgramState z = mModelCheckerAssistant.doProgramTransition(y, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(z);
//		edge = edges.get(0);
//		ProgramState A = mModelCheckerAssistant.doProgramTransition(z, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(A);
//		edge = edges.get(0);
//		ProgramState B = mModelCheckerAssistant.doProgramTransition(A, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(B);
//		edge = edges.get(0);
//		ProgramState C = mModelCheckerAssistant.doProgramTransition(B, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(C);
//		edge = edges.get(0);
//		ProgramState D = mModelCheckerAssistant.doProgramTransition(C, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(D);
//		edge = edges.get(0);
//		ProgramState E = mModelCheckerAssistant.doProgramTransition(D, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(E);
//		edge = edges.get(0);
//		ProgramState F = mModelCheckerAssistant.doProgramTransition(E, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(F);
//		edge = edges.get(0);
//		ProgramState G = mModelCheckerAssistant.doProgramTransition(F, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(G);
//		edge = edges.get(0);
//		ProgramState H = mModelCheckerAssistant.doProgramTransition(G, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(H);
//		edge = edges.get(0);
//		ProgramState I = mModelCheckerAssistant.doProgramTransition(H, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(I);
//		edge = edges.get(0);
//		ProgramState J = mModelCheckerAssistant.doProgramTransition(I, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(J);
//		edge = edges.get(0);
//		ProgramState K = mModelCheckerAssistant.doProgramTransition(J, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(K);
//		edge = edges.get(0);
//		ProgramState L = mModelCheckerAssistant.doProgramTransition(K, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(L);
//		edge = edges.get(0);
//		ProgramState M = mModelCheckerAssistant.doProgramTransition(L, edge);
//		
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(M);
//		edge = edges.get(0);
//		ProgramState N = mModelCheckerAssistant.doProgramTransition(M, edge);
//
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(N);
//		edge = edges.get(0);
//		ProgramState O = mModelCheckerAssistant.doProgramTransition(N, edge);
//
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(O);
//		edge = edges.get(0);
//		ProgramState P = mModelCheckerAssistant.doProgramTransition(O, edge);
//
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(P);
//		edge = edges.get(0);
//		ProgramState Q = mModelCheckerAssistant.doProgramTransition(P, edge);
//
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(Q);
//		edge = edges.get(0);
//		ProgramState R = mModelCheckerAssistant.doProgramTransition(Q, edge);
//
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(R);
//		edge = edges.get(0);
//		ProgramState S = mModelCheckerAssistant.doProgramTransition(R, edge);
//
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(S);
//		edge = edges.get(0);
//		ProgramState T = mModelCheckerAssistant.doProgramTransition(S, edge);
//
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(T);
//		edge = edges.get(0);
//		ProgramState U = mModelCheckerAssistant.doProgramTransition(T, edge);
//
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(U);
//		edge = edges.get(0);
//		ProgramState V = mModelCheckerAssistant.doProgramTransition(U, edge);
//
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(V);
//		edge = edges.get(0);
//		ProgramState W = mModelCheckerAssistant.doProgramTransition(V, edge);
//
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(W);
//		edge = edges.get(0);
//		ProgramState X = mModelCheckerAssistant.doProgramTransition(W, edge);
//
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(X);
//		edge = edges.get(0);
//		ProgramState Y = mModelCheckerAssistant.doProgramTransition(X, edge);
//
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(Y);
//		edge = edges.get(0);
//		ProgramState Z = mModelCheckerAssistant.doProgramTransition(Y, edge);
//
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(Z);
//		edge = edges.get(0);
//		ProgramState aa = mModelCheckerAssistant.doProgramTransition(Z, edge);
//
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(aa);
//		edge = edges.get(0);
//		ProgramState ab = mModelCheckerAssistant.doProgramTransition(aa, edge);
//
//		edges = mModelCheckerAssistant.getProgramEnabledTrans(ab);
//		edge = edges.get(0);
//		ProgramState ac = mModelCheckerAssistant.doProgramTransition(ab, edge);
		
		// Do something...
	}

	private void reportSizeBenchmark(final String message, final INestedWordAutomaton<CodeBlock, String> nwa) {
		final NestedWordAutomataSizeBenchmark<CodeBlock, String> bench =
				new NestedWordAutomataSizeBenchmark<>(nwa, message);
		mLogger.info(message + " " + bench);
		bench.reportBenchmarkResult(mServices.getResultService(), Activator.PLUGIN_ID, message);
	}

	private void reportSizeBenchmark(final String message, final BoogieIcfgContainer root) {
		final IcfgSizeBenchmark bench = new IcfgSizeBenchmark(root, message);
		mLogger.info(message + " " + bench);
		bench.reportBenchmarkResult(mServices.getResultService(), Activator.PLUGIN_ID, message);
	}

	@Override
	public boolean performedChanges() {
		return false;
	}

	// No model generated so far.
	public IElement getModel() {
		return null;
	}

	/**
	 * Collect one RCFG and one LTL2Aut.AST
	 */
	@Override
	public boolean process(final IElement root) throws Exception {
		// collect root nodes of Buchi automaton
		if (root instanceof NWAContainer) {
			mLogger.debug("Collecting NWA representing NeverClaim");
			mNeverClaimNWAContainer = (NWAContainer) root;
			return false;
		}

		// collect root node of program's RCFG
		if (root instanceof BoogieIcfgContainer) {
			mLogger.debug("Collecting RCFG RootNode");
			mRcfg = (BoogieIcfgContainer) root;
			return false;
		}
		return true;
	}
}
