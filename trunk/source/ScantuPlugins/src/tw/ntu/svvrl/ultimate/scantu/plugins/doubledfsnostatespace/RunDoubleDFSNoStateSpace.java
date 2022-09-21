package tw.ntu.svvrl.ultimate.scantu.plugins.doubledfsnostatespace;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import de.uni_freiburg.informatik.ultimate.automata.nestedword.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Pair;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.ModelCheckerAssistant;
//import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.Valuation;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.neverstate.NeverState;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.NilSelfLoop;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.ProgramState;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.ProgramStateTransition;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.threadstate.ThreadState;

public class RunDoubleDFSNoStateSpace {
	private final ModelCheckerAssistant mAssistant;
	private final ILogger mLogger;
	
	Pair<ProgramState, NeverState> mSeed;
	final Stack<Pair<ProgramState, NeverState>> mTrace = new Stack<>();
	final Set<Pair<Pair<ProgramState, NeverState>, Integer>> mStateSpace = new HashSet<>();
	boolean mFound = false;
	boolean mError = false;
	boolean mFisrtMove = false;
	
	private boolean DfsShutdown = false;
	
	public RunDoubleDFSNoStateSpace(final ILogger logger, final ModelCheckerAssistant mca) {
		mAssistant = mca;
		mLogger = logger;
		mSeed = null;
	}
	
	public void run() {
		final Set<ProgramState> pInitials = mAssistant.getProgramInitialStates();
		final Set<NeverState> nInitials = mAssistant.getNeverInitialStates();
		
		for(final ProgramState p : pInitials) {
			for(final NeverState n : nInitials)
			{
				final Pair<ProgramState, NeverState> s0 = new Pair<>(p, n);
				mStateSpace.add(new Pair<Pair<ProgramState, NeverState>, Integer>(s0, 1));
				mTrace.push(s0);
				mFisrtMove = true;
				Dfs(1);
			}
		}
		
		if(!mFound && !mError) {
			mLogger.info("All specifications hold");
		}
	}
	
	/*
	 * Büchi Automaton move
	 */
	private void Dfs(final int N) {
		if (DfsShutdown) {
			return;
		}
		
		final Pair<ProgramState, NeverState> s = mTrace.peek();
		//mLogger.info("Now do the DFS, the state is: " + s + ", and the N is: " + mStateSpace.size());
		//System.out.println("Now do the DFS, the state is: " + s + ", and the N is: " + mStateSpace.size());

		final List<OutgoingInternalTransition<CodeBlock, NeverState>> nxt 
			= mAssistant.getNeverEnabledTrans(getNeverState(s), getProgramState(s));
		
		for(final OutgoingInternalTransition<CodeBlock, NeverState> t : nxt) {
			final NeverState nxtNs = mAssistant.doNeverTransition(getNeverState(s), t);
			final Pair<ProgramState, NeverState> succ = new Pair<>(getProgramState(s), nxtNs);
			
			mTrace.push(succ);
			dfs(N);
		}
		mTrace.pop();
	}
	
	/*
	 * System move
	 */
	private void dfs(int N) {
		if (DfsShutdown) {
			return;
		}
		
		final Pair<ProgramState, NeverState> s = mTrace.peek();
		//mLogger.info("Now do the dfs, the state is: " + s + ", and the N is: " + mStateSpace.size());
		//System.out.println("Now do the dfs, the state is: " + s + ", and the N is: " + mStateSpace.size());
		
		if(getProgramState(s).isErrorState()) {
			mLogger.info("Reach Error State");
			mError = true;
			DfsShutdown = true;
			return;
		}
		
		//List<Long> order = mAssistant.getProgramSafestOrder(getProgramState(s));
		Pair<List<Long>, Boolean> reduction_order = mAssistant.getProgramSafestOrderDebug(getProgramState(s));
		List<Long> order = reduction_order.getFirst();
		//boolean whetherAllUnsafe = reduction_order.getSecond();
		
		for(Long i : order) {
			//boolean notInStack = true;
			//boolean atLeastOneSuccessor = false;
			final List<ProgramStateTransition> nxt 
				= mAssistant.getProgramEnabledTransByThreadID(getProgramState(s), i);
			
			NilSelfLoop nilSelfLoop = mAssistant.checkNeedOfSelfLoop(getProgramState(s));
			if(nilSelfLoop != null) {
				nxt.add(nilSelfLoop);
			}
			
			for(final ProgramStateTransition t : nxt) {
				final ProgramState nxtPs = mAssistant.doProgramTransition(getProgramState(s), t);
				final Pair<ProgramState, NeverState> succ = new Pair<>(nxtPs, getNeverState(s));
				final Pair<Pair<ProgramState, NeverState>, Integer> succN = new Pair<>(succ, N);
				
				if(N == 2 && isEqual(succ, mSeed)) {
					mFound = true;
					mLogger.info("Acceptance Cycle Found");
					printTrace(mTrace);
					DfsShutdown = true;
					return;
				}
				
				if(!inStateSpace(succN)) {
					
					if (succN.toString().equals("[[[Thread0@L60-1, Thread2@L31, Thread3@L29-2, Thread4@L29-3], NeverState@T0_S3], 1]")) {
						System.out.println(succN);
						for(ThreadState ts : succ.getFirst().getThreadStates()) {
							System.out.println("000");
							System.out.println(ts.getValuation());
						}
					}
					
					mStateSpace.add(succN);
					mTrace.push(succ);
					Dfs(N);
					if(N == 1 && getNeverState(succ).isFinal()) {
						mSeed = succ;
						mTrace.push(succ);
						Dfs(2);
					}
				}
				/*else if(inTrace(succ)) {
					notInStack = false;
				}
				atLeastOneSuccessor = true;*/
			}
			/*if(!whetherAllUnsafe && notInStack && atLeastOneSuccessor) {
				break;
			}*/
		}
		mTrace.pop();
	}

	private boolean inStateSpace(Pair<Pair<ProgramState, NeverState>, Integer> p) {
		for(final Pair<Pair<ProgramState, NeverState>, Integer> s : mStateSpace) {
			if(isEqualN(s, p)) {
				return true;
			}
		}
		return false;
	}
	/*private boolean inStateSpace(Pair<Pair<ProgramState, NeverState>, Integer> p, Set<Pair<Pair<ProgramState, NeverState>, Integer>> mStateSpace) {
		for(final Pair<Pair<ProgramState, NeverState>, Integer> s : mStateSpace) {
			if(isEqualN(s, p)) {
				return true;
			}
		}
		return false;
	}*/
	
	private boolean inTrace(final Pair<ProgramState, NeverState> succ) {
		for(int i = mTrace.size()-1; i >= 0; i--) {
			if(isEqual(mTrace.get(i), succ)) {
				return true;
			}
		}
		return false;
	}

	private void printTrace(Stack<Pair<ProgramState, NeverState>> trace) {
		assert trace.size() > 0;
		final Pair<ProgramState, NeverState> s0 = mTrace.get(0);
		mLogger.info("Initialize global variables and reach " + getProgramState(s0).getThreadStates().toString());
		boolean inCycle = false;
		for(int i = 0; i < mTrace.size() - 1; i++)
		{
			Pair<ProgramState, NeverState> s = mTrace.get(i);
			if(isEqual(s, mSeed) && !inCycle) {
				mLogger.info("-----------------Cycle start---------------");
				inCycle = true;
			}
			mLogger.info(getProgramState(s).getThreadStates().toString() + getNeverState(s).getName());
		}
		mLogger.info("-----------------Cycle end-----------------");
	}

	private ProgramState getProgramState(final Pair<ProgramState, NeverState> pair) {
		return pair.getFirst();
	}
	
	private NeverState getNeverState(final Pair<ProgramState, NeverState> pair) {
		return pair.getSecond();
	}
	
	private boolean isEqual(final Pair<ProgramState, NeverState> p1
							, final Pair<ProgramState, NeverState> p2) {
		return getProgramState(p1).equals(getProgramState(p2)) 
			&& getNeverState(p1).equals(getNeverState(p2));
	}
	
	private boolean isEqualN(final Pair<Pair<ProgramState, NeverState>, Integer> p1
							, final Pair<Pair<ProgramState, NeverState>, Integer> p2) {
		return isEqual(p1.getFirst(), p2.getFirst()) && p1.getSecond() == p2.getSecond();
	}
}