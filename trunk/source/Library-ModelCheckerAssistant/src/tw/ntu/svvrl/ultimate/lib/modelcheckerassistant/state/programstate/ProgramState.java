package tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.BoogieIcfgLocation;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ForkThreadCurrent;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.JoinThreadCurrent;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.Valuation;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.ValuationState;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.threadstate.ThreadState;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.threadstate.ThreadStateTransition;

/**
 * ProgramState Consists of one or many {@link ThreadState}s.
 * The <code> mValuation </code> only keeps global valuation.
 * All {@link ThreadState}s' (in <code> mThreadStates </code>) global valuations 
 * are references to this valuation. 
 * So if one thread modifies the global variable, the valuation here also
 * changes.
 */
public class ProgramState extends ValuationState<ProgramState> {
	/**
	 * Thread ID to ThreadState
	 * One thread must contain only one thread state. 
	 */
	final private Map<Long, ThreadState> mThreadStates = new HashMap<>();
	
	public ProgramState(ThreadState threadState, Valuation globalValuation) {
		mValuation = globalValuation;
		addThreadState(threadState);
	}
	
	
	/**
	 * Deep copy a program state and let all the threadStates' global valuation 
	 * refer to <code> mValuation </code>.
	 */
	public ProgramState(final ProgramState state) {
		mValuation = state.getValuationFullCopy();
		for(final ThreadState s : state.mThreadStates.values()) {
			final ThreadState t = new ThreadState(s, ThreadState.ConstructStrategy.localCopy);
			t.getValuation().linkGlobals(mValuation);
			this.mThreadStates.put(s.getThreadID(), t);
		}
	}
	
	public boolean isErrorState() {
		for(final ThreadState threadState : mThreadStates.values()) {
			if(threadState.getCorrespondingIcfgLoc().isErrorLocation()) {
				return true;
			}
		}
		return false;
	}

	public int getThreadNumber() {
		return mThreadStates.size();
	}
	
	public Set<Long> getThreadIDs() {
		return mThreadStates.keySet();
	}
	
	public Collection<ThreadState> getThreadStates() {
		return mThreadStates.values();
	}
	
	public ThreadState getThreadStateByID(final long threadID) {
		return mThreadStates.get(threadID);
	}
	
	public void updateThreadState(final long threadID, final ThreadState newState) {
		mThreadStates.put(threadID, newState);
	}
	
	public void removeThreadState(final long threadID) {
		mThreadStates.remove(threadID);
	}
	
	public boolean allNonOldNonAuxGlobalInitialized() {
		return mValuation.allNonOldNonAuxGlobalInitialized();
	}
	
	private void addThreadState(ThreadState s) {
		if(mThreadStates.containsKey(s.getThreadID())) {
			throw new UnsupportedOperationException("Thread "
					+ String.valueOf(s.getThreadID()) + " already exists.");
		}
		mThreadStates.put(s.getThreadID(), s);
	}

	
	/**
	 * Check whether two program automaton states are equivalent.
	 * This method is needed in the nested DFS procedure. 
	 * @param anotherProgramState
	 * 		the state which is going to compared with.
	 * @return
	 * 		true if two states are equivalent, false if not.
	 */
	@Override
	public boolean equals(final ProgramState anotherProgramState) {
		if(this.getThreadNumber() != anotherProgramState.getThreadNumber()) {
			return false;
		}
		final int threadNumber = this.getThreadNumber();
		
		/*List<Boolean> match = new ArrayList<>();
		for(int i = 0; i < threadNumber; i++) {
			match.add(false);
		}*/
		
		/**
		 * If two program state are equivalent, thread states must have
		 * one-to-one mapping.
		 */
		ThreadState[] thisThreadStates = this.mThreadStates.values().toArray(new ThreadState[threadNumber]);
		ThreadState[] anotherThreadStates = anotherProgramState.mThreadStates.values().toArray(new ThreadState[threadNumber]);
		for(int i = 0; i < threadNumber; i++) {
			boolean whetherAlldif = true;
			for(int j = 0; j < threadNumber; j++) {
				if(thisThreadStates[i].equals(anotherThreadStates[j])) {
					whetherAlldif = false;
					break;
				}
			}
			if(whetherAlldif == true) {
				return false;
			}
		}
		return true;
		/*for(int i = 0; i < threadNumber; i++) {
			for(int j = 0; j < threadNumber; j++) {
				if(!match.get(j) && thisThreadStates[i].equals(anotherThreadStates[j])) {
					match.set(j, true);
					break;
				}
			}
		}*/
		
		/**
		 * If there's any thread state that doesn't match, the two program states
		 * are not equivalent.
		 */
		//return !match.contains(false);
	}
	
	@Override
	public String toString() {
		if(getThreadStates().size() == 0) {
			return "[]";
		}
		String s = "[";
		for(ThreadState t : getThreadStates()) {
			s = s + t.toString() + ", ";
		}
		/**
		 * Remove the last ", "
		 */
		if(s.length() > 1) {
			s = s.substring(0, s.length() - 2);
		}
		return s + "]";
	}
}
