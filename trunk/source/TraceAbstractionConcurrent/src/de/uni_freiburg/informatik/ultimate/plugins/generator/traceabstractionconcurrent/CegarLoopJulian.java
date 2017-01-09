/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2011-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 *
 * This file is part of the ULTIMATE TraceAbstractionConcurrent plug-in.
 *
 * The ULTIMATE TraceAbstractionConcurrent plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE TraceAbstractionConcurrent plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE TraceAbstractionConcurrent plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE TraceAbstractionConcurrent plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE TraceAbstractionConcurrent plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstractionconcurrent;

import java.util.Collection;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.IAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.Word;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomatonSimple;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedWord;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.PowersetDeterminizer;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.oldapi.ComplementDD;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.oldapi.DeterminizeDD;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.IPetriNet;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.PetriNet2FiniteAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.julian.BranchingProcess;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.julian.DifferenceBlackAndWhite;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.julian.FinitePrefix2PetriNet;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.julian.PetriNetJulian;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.julian.PetriNetUnfolder;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.julian.PetriNetUnfolder.UnfoldingOrder;
import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.CfgSmtToolkit;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.IncrementalHoareTripleChecker;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.BoogieIcfgContainer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.BoogieIcfgLocation;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.BasicCegarLoop;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.TraceAbstractionBenchmarks;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.InductivityCheck;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.PredicateFactory;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TAPreferences;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TAPreferences.Artifact;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TraceAbstractionPreferenceInitializer.InterpolationTechnique;

public class CegarLoopJulian<LETTER extends IIcfgTransition<?>> extends BasicCegarLoop<LETTER> {

	private BranchingProcess<LETTER, IPredicate> mUnfolding;
	public int mCoRelationQueries = 0;
	public int mBiggestAbstractionTransitions;

	public CegarLoopJulian(final String name, final BoogieIcfgContainer rootNode, final CfgSmtToolkit csToolkit,
			final PredicateFactory predicateFactory, final TraceAbstractionBenchmarks timingStatistics,
			final TAPreferences taPrefs, final Collection<BoogieIcfgLocation> errorLocs,
			final IUltimateServiceProvider services, final IToolchainStorage storage) {
		super(name, rootNode, csToolkit, predicateFactory, taPrefs, errorLocs,
				InterpolationTechnique.Craig_TreeInterpolation, false, services, storage);
	}

	@Override
	protected void getInitialAbstraction() throws AutomataLibraryException {
		final TaConcurContentFactory contentFactory = new TaConcurContentFactory(this, super.mCsToolkit,
				mPredicateFactory, super.mPref.computeHoareAnnotation(), mPref.computeHoareAnnotation(), false);
		final Cfg2NetJulian<LETTER> cFG2Automaton = new Cfg2NetJulian<>(mIcfgContainer, contentFactory, mCsToolkit,
				mPredicateFactory, mServices, mXnfConversionTechnique, mSimplificationTechnique);
		mAbstraction = cFG2Automaton.getResult();

		if (mIteration <= mPref.watchIteration()
				&& (mPref.artifact() == Artifact.ABSTRACTION || mPref.artifact() == Artifact.RCFG)) {
			mArtifactAutomaton = mAbstraction;
		}
		if (mPref.dumpAutomata()) {
			final String filename = "Abstraction" + mIteration;
			writeAutomatonToFile(mAbstraction, filename);
		}
	}

	@Override
	protected boolean isAbstractionCorrect() throws AutomataOperationCanceledException {
		final PetriNetJulian<LETTER, IPredicate> abstraction = (PetriNetJulian<LETTER, IPredicate>) mAbstraction;
		final String orderString = mPref.order();
		final boolean cutOffSameTrans = mPref.cutOffRequiresSameTransition();
		UnfoldingOrder ord;
		if (orderString.equals(UnfoldingOrder.KMM.getDescription())) {
			ord = UnfoldingOrder.KMM;
		} else if (orderString.equals(UnfoldingOrder.ERV.getDescription())) {
			ord = UnfoldingOrder.ERV;
		} else if (orderString.equals(UnfoldingOrder.ERV_MARK.getDescription())) {
			ord = UnfoldingOrder.ERV_MARK;
		} else {
			throw new IllegalArgumentException();
		}

		final PetriNetUnfolder<LETTER, IPredicate> unf = new PetriNetUnfolder<>(new AutomataLibraryServices(mServices),
				abstraction, ord, cutOffSameTrans, !mPref.unfoldingToNet());
		mUnfolding = unf.getFinitePrefix();
		mCoRelationQueries += mUnfolding.getCoRelationQueries();

		mCounterexample = unf.getAcceptingRun();
		if (mCounterexample == null) {
			return true;
		}
		if (mPref.dumpAutomata()) {
			mDumper.dumpNestedRun(mCounterexample);
		}
		return false;
	}

	@Override
	protected boolean refineAbstraction() throws AutomataLibraryException {
		PetriNetJulian<LETTER, IPredicate> abstraction = (PetriNetJulian<LETTER, IPredicate>) mAbstraction;
		if (mPref.unfoldingToNet()) {
			abstraction = new FinitePrefix2PetriNet<>(new AutomataLibraryServices(mServices), mUnfolding).getResult();
		}

		// Determinize the interpolant automaton
		final INestedWordAutomatonSimple<LETTER, IPredicate> dia = determinizeInterpolantAutomaton(mInterpolAutomaton);

		// Complement the interpolant automaton
		final INestedWordAutomatonSimple<LETTER, IPredicate> nia =
				new ComplementDD<>(new AutomataLibraryServices(mServices), mPredicateFactoryInterpolantAutomata, dia)
						.getResult();
		assert !accepts(mServices, nia, mCounterexample.getWord()) : "Complementation broken!";
		mLogger.info("Complemented interpolant automaton has " + nia.size() + " states");

		if (mIteration <= mPref.watchIteration() && mPref.artifact() == Artifact.NEG_INTERPOLANT_AUTOMATON) {
			mArtifactAutomaton = nia;
		}
		mAbstraction = new DifferenceBlackAndWhite<>(new AutomataLibraryServices(mServices), abstraction,
				(NestedWordAutomaton<LETTER, IPredicate>) dia).getResult();

		mCegarLoopBenchmark.reportAbstractionSize(mAbstraction.size(), mIteration);
		// if (mBiggestAbstractionSize < mAbstraction.size()){
		// mBiggestAbstractionSize = mAbstraction.size();
		// mBiggestAbstractionTransitions =
		// abstraction.getTransitions().size();
		// mBiggestAbstractionIteration = mIteration;
		// }

		assert !acceptsPetriViaFA(mServices, mAbstraction, mCounterexample.getWord()) : "Intersection broken!";

		// int[] statistic = mAbstraction.transitions();
		// s_Logger.debug("Abstraction has " +
		// mAbstraction.getAllStates().size() + "states, " +
		// statistic[0] + " internal transitions " + statistic[1] +
		// "call transitions " + statistic[2]+ " return transitions ");

		if (mIteration <= mPref.watchIteration()
				&& (mPref.artifact() == Artifact.ABSTRACTION || mPref.artifact() == Artifact.RCFG)) {
			mArtifactAutomaton = mAbstraction;
		}
		if (mPref.dumpAutomata()) {
			final String filename = "Abstraction" + mIteration;
			writeAutomatonToFile(mAbstraction, filename);
		}
		return true;
	}

	protected INestedWordAutomaton<LETTER, IPredicate>
			determinizeInterpolantAutomaton(final INestedWordAutomaton<LETTER, IPredicate> interpolAutomaton)
					throws AutomataOperationCanceledException {
		mLogger.debug("Start determinization");
		INestedWordAutomaton<LETTER, IPredicate> dia;
		switch (mPref.interpolantAutomatonEnhancement()) {
		case NONE:
			final PowersetDeterminizer<LETTER, IPredicate> psd =
					new PowersetDeterminizer<>(interpolAutomaton, true, mPredicateFactoryInterpolantAutomata);
			final DeterminizeDD<LETTER, IPredicate> dabps =
					new DeterminizeDD<>(new AutomataLibraryServices(mServices), interpolAutomaton, psd);
			dia = dabps.getResult();
			break;
		default:
			throw new UnsupportedOperationException();
		}

		if (mComputeHoareAnnotation) {
			assert new InductivityCheck<>(mServices, dia, false, true,
					new IncrementalHoareTripleChecker(super.mCsToolkit)).getResult() : "Not inductive";
		}
		if (mPref.dumpAutomata()) {
			final String filename = "InterpolantAutomatonDeterminized_Iteration" + mIteration;
			writeAutomatonToFile(dia, filename);
		}
		assert accepts(mServices, dia, mCounterexample.getWord());
		mLogger.debug("Sucessfully determinized");
		return dia;
	}

	@Override
	protected void computeCFGHoareAnnotation() {
		throw new UnsupportedOperationException();
	}

	private boolean acceptsPetriViaFA(final IUltimateServiceProvider services,
			final IAutomaton<LETTER, IPredicate> automaton, final Word<LETTER> word)
			throws AutomataOperationCanceledException {
		final NestedWord<LETTER> nw = NestedWord.nestedWord(word);
		final INestedWordAutomatonSimple<LETTER, IPredicate> petriNetAsFA =
				new PetriNet2FiniteAutomaton<>(new AutomataLibraryServices(services),
						(IPetriNet<LETTER, IPredicate>) automaton).getResult();
		return super.accepts(services, petriNetAsFA, nw);

	}

}
