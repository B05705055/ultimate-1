package tw.ntu.svvrl.ultimate.lib.modelcheckerassistant;

import java.util.List;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.BoogieIcfgContainer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Pair;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.explorer.ProgramStateExplorer;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.neverstate.NeverState;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.NilSelfLoop;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.ProgramState;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.ProgramStateTransition;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.explorer.NeverClaimAutExplorer;

/**
 * This is a helper for the SPIN-like model checker.
 * It mainly consists of two component. One is for the exploration of
 * Boogie program states, the other is for the exploration of the
 * never claim automaton which is originally represented as an LTL formula.
 * 
 * One can call the methods this class provides to imitate SPIN and
 * implement a "On the fly" automata-based model checker. 
 * 
 * @author Hong-Yang Lin 
 */

public class ModelCheckerAssistant {
	
	private final ILogger mLogger;
	private final IUltimateServiceProvider mServices;
	private final BoogieIcfgContainer mRcfgRoot;
	private final INestedWordAutomaton<CodeBlock, String> mNWA;
	
	/**
	 * The first main component, which generates program states on-the-fly and 
	 * keep exploring.
	 */
	private final ProgramStateExplorer mProgramStateExplorer;
	private final NeverClaimAutExplorer mNeverClaimAutExplorer;
	
	/**
	 * This constructor is for debugging.
	 */
	public ModelCheckerAssistant(final BoogieIcfgContainer rcfg,
			final ILogger logger, final IUltimateServiceProvider services) {
		
		// services and logger
		mServices = services;
		mLogger = logger;
		mRcfgRoot = rcfg;
		mNWA = null;
		
		mProgramStateExplorer = new ProgramStateExplorer(rcfg);
		mNeverClaimAutExplorer = null;
	}
	
	
	public ModelCheckerAssistant(final INestedWordAutomaton<CodeBlock, String> nwa, final BoogieIcfgContainer rcfg,
			final ILogger logger, final IUltimateServiceProvider services) {
		
		// services and logger
		mServices = services;
		mLogger = logger;
		mRcfgRoot = rcfg;
		mNWA = nwa;
		
		mProgramStateExplorer = new ProgramStateExplorer(rcfg);
		mNeverClaimAutExplorer = new NeverClaimAutExplorer(nwa);
	}
	
	public Set<ProgramState> getProgramInitialStates() {
		return mProgramStateExplorer.getInitialStates();
	}
	
	public Set<NeverState> getNeverInitialStates() {
		return mNeverClaimAutExplorer.getInitialStates();
	}
	
	public List<ProgramStateTransition> getProgramEnabledTransByThreadID(final ProgramState p, final long tid) {
		return mProgramStateExplorer.getEnabledTransByThreadID(p, tid);
	}
	
	public List<ProgramStateTransition> getProgramEnabledTrans(final ProgramState p) {
		return mProgramStateExplorer.getEnabledTrans(p);
	}
	
	/**
	 * @return If in ProgramState p, the {@link NilSelfLoop} is needed, 
	 * return an instance of {@link NilSelfLoop}. Otherwise, return <code>null</code>.
	 */
	public NilSelfLoop checkNeedOfSelfLoop(final ProgramState p) {
		return mProgramStateExplorer.checkNeedOfSelfLoop(p);
	}
	
	public List<Long> getProgramSafestOrder(final ProgramState p) {
		return mProgramStateExplorer.getSafestOrder(p);
	}
	
	public Pair<List<Long>, Boolean> getProgramSafestOrderDebug(final ProgramState p) {
		return mProgramStateExplorer.getSafestOrderDebug(p);
	}
	
	public List<OutgoingInternalTransition<CodeBlock, NeverState>> 
		getNeverEnabledTrans(final NeverState n, final ProgramState correspondingProgramState) {
		return mNeverClaimAutExplorer.getEnabledTrans(n, correspondingProgramState);
	}
	
	public ProgramState doProgramTransition(final ProgramState p, final ProgramStateTransition trans) {
		return mProgramStateExplorer.doTransition(p, trans);
	}
	
	public NeverState doNeverTransition(final NeverState n, final OutgoingInternalTransition<CodeBlock, NeverState> edge) {
		return mNeverClaimAutExplorer.doTransition(n, edge);
	}
	
	public String getCStatement(final ProgramStateTransition t) {
		return t.getCStatement();
	}
}
