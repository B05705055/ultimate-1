package tw.ntu.svvrl.ultimate.lib.modelchecker;

import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.*;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.neverstate.NeverState;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.transitions.IncomingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.transitions.OutgoingInternalTransition;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedWordAutomatonCache;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.optncsb.automata.IState;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.ProgramState;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.ProgramStateTransition;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.programstate.threadstate.ThreadStateTransition;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Pair;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.state.neverstate.NeverStateFactory;
import tw.ntu.svvrl.ultimate.lib.modelcheckerassistant.ModelCheckerAssistant;

import java.util.Map.Entry;
import java.util.*;
import java.util.stream.Collectors;

public class ModelCheckerDoubleDFSwithReduction{
	private final ModelCheckerAssistant assistant;
	private final ILogger mLogger;
	private boolean match = false;
	private boolean end = false;
	private List<ProgramState> levelNodes = new ArrayList<ProgramState>();
	protected Set<NeverState> initStates =new HashSet<>();
	
	Pair<ProgramState, NeverState> seed = null;

	Stack<Pair<ProgramState, NeverState>> CompoundStack = new Stack<>();
	Stack<Pair<Pair<ProgramState, NeverState>, Integer>> StateSpace = new Stack<>();
	Stack<Pair<Pair<ProgramState, NeverState>, List<Long>>> ErrorPath = new Stack<>();

	public ModelCheckerDoubleDFSwithReduction(final ILogger logger, final ModelCheckerAssistant mca)
	{	
		mLogger = logger;
		assistant = mca;
	}
	
	public void run() {
		// set of initial cfg locations
		levelNodes.addAll(assistant.getProgramInitialStates());
				
		// set of initial states of automaton
		Set<NeverState> initStates = new HashSet<>();
		initStates = assistant.getNeverInitialStates();
				
		for (int i = 0;i < levelNodes.size();i++) {
			for(int j = 0;j < initStates.size();j++)
			{
				if(match) {return;}
				Pair p = new Pair(levelNodes.get(i), (NeverState) initStates.toArray()[j]);
				CompoundStack.push(p);
				StateSpace.push(new Pair(p, 1));
				dfs(1);
			}
		} 
		if(!match) 
		{
			mLogger.info("All specifications hold");
			return;
		}
	}
	
	public boolean compare(Stack<Pair<Pair<ProgramState, NeverState>, Integer>> path, ProgramState node, NeverState state, int ab)
	{
		for(int i = 0; i < path.size();i++)
		{
//			if((node.equals(path.get(i).getFirst().getFirst())) && (state.equals(path.get(i).getFirst().getSecond())) && (path.get(i).getSecond() == ab))
//			{
//				return true;
//			}
			if((path.get(i).getFirst().getFirst().toString().equals(node.toString())) && (path.get(i).getFirst().getSecond().toString().equals(state.toString())) && (path.get(i).getSecond() == ab))
			{
				return true;
			}
//			if((path.get(i).getFirst().getFirst().equals(node)) && (path.get(i).getFirst().getSecond().equals(state)) && (path.get(i).getSecond() == ab))
//			{
//				return true;
//			}
		}
		return false;
	}
	
	private static List<Long> getIntersectOfLists1(List<Long> list1, List<Long> list2) {
		List<Long> intersectElements = list1.stream()
				.filter(list2 :: contains)
				.collect(Collectors.toList());
		
		if(!intersectElements.isEmpty()) {
			return intersectElements;
		}else {
			return Collections.emptyList();
		}
	}
	public List<List<Long>> getFairList(Stack<Pair<Pair<ProgramState, NeverState>, List<Long>>> path)
	{
		List<List<Long>> FairList = new ArrayList<>();
		for(int i = 0;i<path.size();i++)
		{
			FairList.add(path.get(i).getSecond());
		}
		return FairList;
	}
	
	public boolean compareErrorPath(List<List<Long>> FairList)
	{
		while(FairList.size()>1)
		{
			List<Long> temp = getIntersectOfLists1(FairList.get(0), FairList.get(1));
			FairList.remove(0);
			FairList.remove(0);
			FairList.add(temp);
		}
		if(FairList.get(0).size()<3)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	

	/* system move */
	public void dfs(int a)
	{
		ProgramState node = CompoundStack.peek().getFirst();
		NeverState state = CompoundStack.peek().getSecond();
		
		List<Long> OrderofProcesses = assistant.getProgramSafestOrder(node);
		 
		/* Without Partial Order Reduction */
//		Set<Long> Processes = node.getThreadIDs();
//		mLogger.info("getThreadIDs()"+Processes);
//		List<Long> OrderofProcesses = new ArrayList<>(Processes);
		
		List<ProgramStateTransition> programEdges = new ArrayList<ProgramStateTransition>();
		
		for(int k = 0;k < OrderofProcesses.size();k++)
		{
			// programEdges.addAll(assistant.getProgramEnabledTransByThreadID(node, k));
			programEdges = assistant.getProgramEnabledTransByThreadID(node, OrderofProcesses.get(k));
			
			if(programEdges.isEmpty())
			{
				programEdges.add(assistant.checkNeedOfSelfLoop(node));
			}
			
//			boolean NotInStack = true;
//			boolean AtLeaseOneSuccesor = false;
			
			if(programEdges.contains(null))
			{
				continue;
			}

			List<ProgramState> nProgramNodes = new ArrayList<ProgramState>();
			for(int j = 0;j < programEdges.size();j++)
			{
				nProgramNodes.add(assistant.doProgramTransition(node, programEdges.get(j)));
			}
			levelNodes = nProgramNodes;
					
			for (int i = 0;i < levelNodes.size();i++) {
				if(match) {return;}
				ProgramState nextNode = levelNodes.get(i);
				
				
				Pair p = new Pair(nextNode, state);
				Pair s = new Pair(p, a);
				
				// if(!StateSpace.contains(s) || (node.equals(nextNode)&&state.equals(state)))
//				if(!compare(StateSpace, nextNode, state, a) || (node.equals(nextNode)&&(a == 2)))
//				if(!compare(StateSpace, nextNode, state, a) || (a == 2))
				if(!compare(StateSpace, nextNode, state, a))
				{
					if(a==2 && seed!=null)
					{
						List<Long> f = OrderofProcesses;
						Pair l = new Pair(p, f);
						ErrorPath.push(l);
						//mLogger.info("ErrorPath: "+ErrorPath.peek().getFirst().getFirst().toString()+ErrorPath.peek().getFirst().getSecond().getName()+ErrorPath.peek().getSecond());
					}
					
					StateSpace.push(s);
					CompoundStack.push(p);
					Dfs(a);
				}	
//				else if(compare2(CompoundStack, nextNode, state))
//				{
//					NotInStack = false;
//				}
//				AtLeaseOneSuccesor = true;
			}
//			if(AtLeaseOneSuccesor && NotInStack)
//			{
//				break;
//			}
		}

	}
	/* specification move */
	public void Dfs(int b)
	{
		NeverState state = CompoundStack.peek().getSecond();
		ProgramState node = CompoundStack.peek().getFirst();
		
		List<OutgoingInternalTransition<CodeBlock, NeverState>> neverEdges = assistant.getNeverEnabledTrans(state, node);
		
		if(neverEdges.contains(null))
		{
			return;
		}
		
		for (int j = 0;j < neverEdges.size();j++) {
			
			if(match) {return;}
			OutgoingInternalTransition<CodeBlock, NeverState> neverEdge = neverEdges.get(j);
			NeverState nextState = assistant.doNeverTransition(state, neverEdge);
			
			Pair p = new Pair(node, nextState);
			Pair s = new Pair(p, b);
			
//			if(node.getThreadStates().toString().contains("ULTIMATE.startEXIT"))
//			{
//				end = true;
//			}
			/*debug for equal func*/
//			if(StateSpace.size()>200)
//			{
//				mLogger.info("StateSpace.size()>50");
//				match = true;
//				for(int a = 0; a < StateSpace.size();a++)
//				{
//					mLogger.info(StateSpace.get(a).getFirst().getFirst().toString()+StateSpace.get(a).getFirst().getSecond().getName()+"("+StateSpace.get(a).getSecond()+")"+StateSpace.size());
//				}
//				return;
//			}
			
			if(j == 0)
			{
				StateSpace.pop();
				CompoundStack.pop();
			}
			
			//if(b==2 && compare(StateSpace, node, nextState, b))
			//if(b==2 && compare2(CompoundStack, node, nextState))
			//if(b==2 && node.equals(seed.getFirst()) && nextState.equals(seed.getSecond()))
			if(b==2 && node.toString().equals(seed.getFirst().toString()) && nextState.toString().equals(seed.getSecond().toString()))
			{
				List<List<Long>> fList = getFairList(ErrorPath);
				for(int a = 0; a < fList.size();a++)
				{
//					mLogger.info(fList.get(a).toString());
				}
				boolean x = (fList.size()<5);
				boolean y = compareErrorPath(fList);
				
				//if((compareErrorPath(fList)) && (fList.size()<5))
				if(y)
				{
					if(x)
					{
						match = true;
						for(int a = 0; a < CompoundStack.size();a++)
						{
							mLogger.info(CompoundStack.get(a).getFirst().getThreadStates().toString() + CompoundStack.get(a).getSecond().getName());
						}
						mLogger.info("Violation of LTL property");
						return;
					}
					return;
				}
				else
				{
//					if(!CompoundStack.peek().getFirst().equals(seed.getFirst()))
//					{
//						CompoundStack.pop();
//					}
					StateSpace.push(new Pair(p, 2));
					seed = null;
					continue;
					//b = 1;  
				}
			}
			
			
			
			if(!compare(StateSpace, node, nextState, b))
			{
				StateSpace.push(s);			
				CompoundStack.push(p);
//				mLogger.info(StateSpace.peek().getFirst().getFirst().toString()+StateSpace.peek().getFirst().getSecond().getName()+"("+StateSpace.peek().getSecond()+")"+StateSpace.size());
				mLogger.info(StateSpace.peek().getFirst().getFirst().toString()+StateSpace.peek().getFirst().getSecond().getName()+"("+StateSpace.peek().getSecond()+")");
				//mLogger.info(CompoundStack.size());
				dfs(b);
			
			}
			
			if(!CompoundStack.empty())
			{
				if(match) {return;}
				if(b == 1 && CompoundStack.peek().getSecond().isFinal())
				//if(CompoundStack.peek().getSecond().isFinal())
				{
					ErrorPath.clear();
					seed = CompoundStack.peek();
//					mLogger.info("seed:"+seed.getFirst().getThreadStates().toString() +seed.getSecond().getName());
					dfs(2);
				}
			}
		}
		
		Pair<ProgramState, NeverState> remove2 = CompoundStack.pop();
//		if(!CompoundStack.empty())
//		{
//			if(match) {return;}
//			if(b == 1 && CompoundStack.peek().getSecond().isFinal())
//			{
//				seed = CompoundStack.peek();
//				dfs(2);
//			}
//		}
		
		//Pair<ProgramState, NeverState> remove2 = CompoundStack.pop();
		
	}

}
