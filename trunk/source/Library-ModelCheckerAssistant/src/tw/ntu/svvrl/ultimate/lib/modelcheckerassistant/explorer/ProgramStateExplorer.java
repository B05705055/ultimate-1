package tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.explorer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.boogie.ast.EnsuresSpecification;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Specification;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.boogie.Boogie2SmtSymbolTable;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IcfgEdge;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.debugidentifiers.DebugIdentifier;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.ILocalProgramVar;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.BoogieIcfgContainer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.BoogieIcfgLocation;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ForkThreadCurrent;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.JoinThreadCurrent;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Pair;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.ForkHandler;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.FuncInitValuationInfo;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.JoinHandler;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.NilSelfLoop;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.ProgramState;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.ProgramStateFactory;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.ProgramStateTransition;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.threadstate.ThreadState;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.threadstate.ThreadStateTransition;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.transitiontoolkit.threadtransitiontoolkit.ThreadTransitionToolkit;

/**
 * This class explores the boogie program states with the help of
 * the pre-build {@link BoogieIcfgContainer}.
 * The {@link ThreadState}s is generated when a Icfg transition is going to be moved. 
 * 
 * @author Hong-Yang Lin
 */
public class ProgramStateExplorer {
	/*---------------RCFG fields---------------*/
	private final Map<String, BoogieIcfgLocation> mEntryNodes;
	private final Map<String, BoogieIcfgLocation> mExitNodes;
	private final Map<String, Map<DebugIdentifier, BoogieIcfgLocation>> mLocNodes;
	private final Set<BoogieIcfgLocation> mInitialNodes;
	/*------------End of RCFG fields-----------*/
	
	private final ProgramStateFactory mProgramStateFactory;
	
	private final FuncInitValuationInfo mFuncInitValuationInfo;
	
	private final Map<String, List<String>> mProc2InParams;
	private final Map<String, List<String>> mProc2OutParams;
	
	private final Map<String, List<EnsuresSpecification>> mProc2Ensures;
	private Map<String, Set<String>> mProc2ModifiedVars;

	public ProgramStateExplorer(final BoogieIcfgContainer rcfg) {
		/*---------------RCFG fields---------------*/
		mEntryNodes = rcfg.getProcedureEntryNodes();
		mExitNodes = rcfg.getProcedureExitNodes();
		mLocNodes = rcfg.getProgramPoints();
		mInitialNodes = rcfg.getInitialNodes();
		/*------------End of RCFG fields-----------*/
		
		final Boogie2SmtSymbolTable boogie2SmtSymbolTable = rcfg.getBoogie2SMT().getBoogie2SmtSymbolTable();
		mFuncInitValuationInfo = new FuncInitValuationInfo(
				boogie2SmtSymbolTable.getBoogieDeclarations().getFunctionDeclarations());
		mProc2InParams = createProc2Prams(boogie2SmtSymbolTable, "in");
		mProc2OutParams = createProc2Prams(boogie2SmtSymbolTable, "out");
		mProc2Ensures = boogie2SmtSymbolTable.getBoogieDeclarations().getEnsuresNonFree();
		mProc2ModifiedVars = boogie2SmtSymbolTable.getBoogieDeclarations().getModifiedVars();
		
		mProgramStateFactory = new ProgramStateFactory(boogie2SmtSymbolTable
				, rcfg.getCfgSmtToolkit(), mEntryNodes, mExitNodes, this);
		
		/**
		 * static reduction.
		 * @see Holzmann G.J., Peled D. (1995) An Improvement in Formal Verification.
		 * 	In: Hogrefe D., Leue S. (eds) Formal Description Techniques VII.
		 * IFIP Advances in Information and Communication Technology. Springer, Boston, MA.
		 * https://doi.org/10.1007/978-0-387-34878-0_13
		 */
		markEdges(rcfg);
	}


	private void markEdges(final BoogieIcfgContainer rcfg) {
		Map<String, Map<DebugIdentifier, BoogieIcfgLocation>> locNodes = rcfg.getProgramPoints();
		for(final String procName : locNodes.keySet()) {
			for(final BoogieIcfgLocation procLoc : locNodes.get(procName).values()) {
				for(final IcfgEdge edge : procLoc.getOutgoingEdges()) {
					assert edge instanceof CodeBlock;
					final ThreadStateTransition trans = new ThreadStateTransition(edge, -1);
					final ThreadState dummyState = new ThreadState(null, procLoc, 0, this);
					final ThreadTransitionToolkit transToolkit = new ThreadTransitionToolkit(trans, dummyState, this);
					if(transToolkit.checkAccessOnlyLocalVar()) {
						((CodeBlock) edge).setAccessOnlyLocalVar(true);
					}
				}
			}
		}
	}


	/**
	 * Based on the given rcfg, extend the RCFG location "ULTIMATE.StartENTRY" with
	 * value tables so that they become start states.
	 * A start state is an entry state that all global variables have not been initialized.
	 * A initial state is a state in which all global variables are just initialized.
	 * We generate initial states from start states via doing several transitions that
	 * are responsible for the initialization of global variables.
	 * @return	A start state.
	 */
	private Set<ProgramState> getStartStates() {
		final Set<ProgramState> startProgramStates = new HashSet<>();
		
		for(BoogieIcfgLocation startLoc: mInitialNodes) {
			startProgramStates.add(mProgramStateFactory.createStartState(startLoc));
		}
		
		return startProgramStates;
	}
	
	/**
	 * @return A set of states in which all global variables are just initialized.
	 */
	public Set<ProgramState> getInitialStates() {
		final Set<ProgramState> pInitials = new HashSet<>();

		for(ProgramState p : getStartStates()) {
			while(!globalVarsInitialized(p)) {
				final List<ProgramStateTransition> trans = getEnabledTrans(p);
				assert trans.size() <= 1;
				for(ProgramStateTransition t : trans) {
					p = doTransition(p, t);
				}
			}
			pInitials.add(p);
		}
		
		return pInitials;
	}
	
	private boolean globalVarsInitialized(final ProgramState p) {
		/**
		 * All NonOld global variables must be initialized, or some errors
		 * will occur during expression evaluation.
		 * @return If some global variables are not initialized yet, return false.
		 */
		if(p.allNonOldNonAuxGlobalInitialized()) {
			return true;
		}
		return false;
	}
	

	public List<ProgramStateTransition> getEnabledTransByThreadID(final ProgramState p, long tid) {
		final List<ProgramStateTransition> enabledTrans = new ArrayList<>();
		final ThreadState threadState = p.getThreadStateByID(tid);
		enabledTrans.addAll(threadState.getEnabledTrans());
		
		/**
		 * If there is a join in <code>enableTrans</code> and
		 * it is blocked, remove it from <code>enableTrans</code>.
		 */
		final List<ProgramStateTransition> blockedTrans = new ArrayList<>();
		for(final ProgramStateTransition trans : enabledTrans) {
			assert trans instanceof ThreadStateTransition;
			if(((ThreadStateTransition) trans).getIcfgEdge() instanceof JoinThreadCurrent) {
				final JoinHandler joinHandler = new JoinHandler(p, (ThreadStateTransition) trans, this);
				if(joinHandler.isJoinBlocked()) {
					blockedTrans.add(trans);
				}
			}
		}
		enabledTrans.removeAll(blockedTrans);
		
		return enabledTrans;
	}
	
	public List<ProgramStateTransition> getEnabledTrans(final ProgramState p) {
		final List<ProgramStateTransition> enabledTrans = new ArrayList<>();
		for(final long tid : p.getThreadIDs()) {
			enabledTrans.addAll(getEnabledTransByThreadID(p, tid));
		}
		
		if(enabledTrans.isEmpty()) {
			if(checkNeedOfSelfLoop(p) != null) {
				enabledTrans.add(new NilSelfLoop());
			}
		}
		return enabledTrans;
	}
	
	/**
	 * @return If in ProgramState p, the {@link NilSelfLoop} is needed, 
	 * return an instance of {@link NilSelfLoop}. Otherwise, return null.
	 */
	public NilSelfLoop checkNeedOfSelfLoop(final ProgramState p) {
		/**
		 * If every thread state has no successor, attach a nil self-loop.
		 * @see the definition of synchronous product.
		 */
		boolean hasNoSucc = true;
		for(final ThreadState threadState : p.getThreadStates()) {
			if(!threadState.getCorrespondingIcfgLoc().getOutgoingEdges().isEmpty()) {
				hasNoSucc = false;
			}
		}
		if(hasNoSucc) {
			return new NilSelfLoop();
		}
		return null;
	}
	
	/**
	 * One of thread state do the transition.
	 * (According to the threadID on the {@link ThreadStateTransition}).
	 */
	public ProgramState doTransition(final ProgramState p, final ProgramStateTransition trans) {
		ProgramState newProgramState = new ProgramState(p);
		
		if(trans instanceof ThreadStateTransition) {
			final ThreadStateTransition threadTrans = (ThreadStateTransition) trans;
			/**
			 * For Fork and Join, we need to pass the whole program state which
			 * consists of all thread states.
			 */
			if(threadTrans.getIcfgEdge() instanceof ForkThreadCurrent) {
				final ForkHandler forkHandler = new ForkHandler(p, threadTrans, this);
				newProgramState = forkHandler.doFork();
			} else if(threadTrans.getIcfgEdge() instanceof JoinThreadCurrent) {
				final JoinHandler joinHandler = new JoinHandler(p, threadTrans, this);
				newProgramState = joinHandler.doJoin(this);
			} else {
				/**
				 * For others(not Fork and Join), Only one thread state is considered.
				 * which thread state to be executed is according to the threadID
				 * in {@link ThreadStateTransition}.
				 */
				final ThreadState newState 
				= newProgramState.getThreadStateByID(threadTrans.getThreadID()).doTransition(threadTrans);
				/**
				 * update the thread state who did the transition.
				 */
				newProgramState.updateThreadState(newState.getThreadID(), newState);
			}
			return newProgramState;
		} else if(trans instanceof NilSelfLoop) {
			/**
			 * Do nothing.
			 */
			return p;
		} else {
			throw new UnsupportedOperationException("Unkown ProgramStateTransition type: "
					+ trans.getClass().getSimpleName());
		}
	}
	
	/**
	 * Implementation of line 8a in figure 1e in
	 * Holzmann G.J., Peled D. (1995) An Improvement in Formal Verification.
	 * In: Hogrefe D., Leue S. (eds) Formal Description Techniques VII.
	 * IFIP Advances in Information and Communication Technology. Springer, Boston, MA.
	 * https://doi.org/10.1007/978-0-387-34878-0_13
	 *
	 * @return a sorted list contains threadIDs. (Safest thread first)
	 */
	public List<Long> getSafestOrder(final ProgramState p) {
		final Set<Long> threadIDs = p.getThreadIDs();
		final Map<Long, Float> ID2SafeProp = new HashMap<>();
		for(final long tid : threadIDs) {
			final List<ProgramStateTransition> threadEnabledTrans = getEnabledTransByThreadID(p, tid);
			int safeCount = 0;
			for(final ProgramStateTransition pt : threadEnabledTrans) {
				if(pt instanceof NilSelfLoop) {
					safeCount++;
				} else if(pt instanceof ThreadStateTransition) {
					if(((ThreadStateTransition) pt).accessOnlyLocalVar()) {
						safeCount++;
					}
				} else {
					throw new UnsupportedOperationException("Unkown ThreadStateTransition type: "
							+ pt.getClass().getSimpleName());
				}
			}
			if(threadEnabledTrans.size() == 0) {
				ID2SafeProp.put(tid, (float) -1);
			} else {
				ID2SafeProp.put(tid, (float)safeCount / threadEnabledTrans.size());
			}
		}
		
		/**
		 * Sort ID2SafeProp by prop.
		 */
		 List<Map.Entry<Long, Float>> l = new ArrayList<Map.Entry<Long, Float>>(ID2SafeProp.entrySet());
		 l.sort(new Comparator<Map.Entry<Long, Float>>() {
	          @Override
	          public int compare(Map.Entry<Long, Float> o1, Map.Entry<Long, Float> o2) {
	              return o2.getValue().compareTo(o1.getValue());
	          }
	      });
		 
		 /**
		  * Add sorted threadIDs to result.
		  */
		 List<Long> result = new ArrayList<>();
		 for(final Map.Entry<Long, Float> e : l) {
			 result.add(e.getKey());
		 }
		 
		 return result;
	}
	
	//public List<Long> getSafestOrderDebug(final ProgramState p) {
	public Pair<List<Long>, Boolean> getSafestOrderDebug(final ProgramState p) {
		final Set<Long> threadIDs = p.getThreadIDs();
		final Map<Long, Float> ID2SafeProp = new HashMap<>();
		boolean whetherAllUnsafe = true;
		for(final long tid : threadIDs) {
			final List<ProgramStateTransition> threadEnabledTrans = getEnabledTransByThreadID(p, tid);
			int safeCount = 0;
			boolean accessGlobal = false;
			for(final ProgramStateTransition pt : threadEnabledTrans) {
				if(pt instanceof NilSelfLoop) {
					safeCount++;
				} else if(pt instanceof ThreadStateTransition) {
					if(((ThreadStateTransition) pt).accessOnlyLocalVar()) {
						safeCount++;
					}
					else {
						accessGlobal = true;
					}
				} else {
					throw new UnsupportedOperationException("Unkown ThreadStateTransition type: "
							+ pt.getClass().getSimpleName());
				}
			}
			if(!accessGlobal) {
				whetherAllUnsafe = false;
			}
			if(threadEnabledTrans.size() == 0) {
				ID2SafeProp.put(tid, (float) -1);
			} else {
				ID2SafeProp.put(tid, (float)safeCount / threadEnabledTrans.size());
			}
		}
		
		/**
		 * Sort ID2SafeProp by prop.
		 */
		 List<Map.Entry<Long, Float>> l = new ArrayList<Map.Entry<Long, Float>>(ID2SafeProp.entrySet());
		 l.sort(new Comparator<Map.Entry<Long, Float>>() {
	          @Override
	          public int compare(Map.Entry<Long, Float> o1, Map.Entry<Long, Float> o2) {
	              return o2.getValue().compareTo(o1.getValue());
	          }
	      });
		 
		 /**
		  * Add sorted threadIDs to result.
		  */
		 List<Long> result = new ArrayList<>();
		 for(final Map.Entry<Long, Float> e : l) {
			 result.add(e.getKey());
		 }
		 
		 Pair<List<Long>, Boolean> resultWithSafeMark = new Pair<>(result, whetherAllUnsafe);
		 return resultWithSafeMark;
		 //return result;
	}
	
	/**
	 * Only for debugging
	 */
	/*
	public ProgramState getLocStateById(String id) {
		for(Map<DebugIdentifier, BoogieIcfgLocation> m : mLocNodes.values()) {
			for(BoogieIcfgLocation l : m.values()) {
				if(l.toString().equals(id)) {
					return mProgramStateFactory.createStartState(l);
				}
			}
		}
		return null;
	}
	*/
	
	private Map<String, List<String>> createProc2Prams(Boogie2SmtSymbolTable boogie2SmtSymbolTable
			, String inOrOut) {
		final Map<String, List<String>> result = new HashMap<>();
		Map<String, List<ILocalProgramVar>> p2p = new HashMap<>();
		if(inOrOut.equals("in")) {
			p2p = boogie2SmtSymbolTable.getProc2InParams();
		} else if(inOrOut.equals("out")) {
			p2p = boogie2SmtSymbolTable.getProc2OutParams();
		} else {
			throw new UnsupportedOperationException("Invalid argument: "
					+ inOrOut);
		}
				
		for(final String procName : p2p.keySet()) {
			List<String> paramNames = new ArrayList<>();
			for(final ILocalProgramVar param : p2p.get(procName)) {
				paramNames.add(param.getIdentifier());
			}
			result.put(procName, paramNames);
		}
		return result;
	}
	
	
	public BoogieIcfgLocation getEntryNode(final String procName) {
		return mEntryNodes.get(procName);
	}
	
	public BoogieIcfgLocation getExitNode(final String procName) {
		return mExitNodes.get(procName);
	}
	
	public FuncInitValuationInfo getFuncInitValuationInfo() {
		return mFuncInitValuationInfo;
	}
	
	public Map<String, List<String>> getProc2InParams() {
		return mProc2InParams;
	}
	
	public Map<String, List<String>> getProc2OutParams() {
		return mProc2OutParams;
	}
	
	public Map<String, List<EnsuresSpecification>> getProc2Ensures() {
		return mProc2Ensures;
	}
	
	public Map<String, Set<String>> getProc2ModifiedVars() {
		return mProc2ModifiedVars;
	}
}
