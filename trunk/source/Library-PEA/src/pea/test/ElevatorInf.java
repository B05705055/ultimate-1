package pea.test;
import pea.*;
import pea.modelchecking.PEA2ARMCConverter;
import pea.modelchecking.SimplifyPEAs;
import java.util.*;
import org.apache.log4j.PropertyConfigurator;

/**
 * Class to create an automaton from a counter example trace.
 *
 */
@SuppressWarnings("deprecation")
public class ElevatorInf {

    PhaseEventAutomata csppart, zpart, dcpart;

    public ElevatorInf() {
    }

    public static void main(String[] param) {
	CDD cgless = 
	    BooleanDecision.create("current <= goal - 1");
	CDD cgleq = 
	    BooleanDecision.create("current <= goal");
	CDD cggeq = 
	    BooleanDecision.create("goal <= current");
	CDD cggreater = 
	    BooleanDecision.create("goal <= current - 1");

	PropertyConfigurator.configure
	    (ClassLoader.getSystemResource("pea/test/TestLog.config"));

	CDD stateInv = 
	    cgless.and(cgleq).and(cggeq.negate()).and(cggreater.negate())
	    .or(cgless.negate().and(cgleq).and(cggeq).and(cggreater.negate()))
	    .or(cgless.negate().and(cgleq.negate()).and(cggeq).and(cggreater));
	stateInv = stateInv.and(BooleanDecision.create("Min <= Max"));
	CDD transInv = BooleanDecision.create("Min = Min'")
	    .and(BooleanDecision.create("Max = Max'"));
	    
	Phase invP = new Phase("inv", stateInv);
	invP.addTransition(invP, transInv, new String[0]);
	ElevatorInf elev = new ElevatorInf();

	elev.buildZPart();
	elev.buildCSPPart();
	elev.buildDCPart();

	//elev.csppart.dump();
	//elev.zpart.dump();
	//elev.dcpart.dump();
	PhaseEventAutomata pea = 
	    elev.csppart.parallel(elev.zpart); //.parallel(elev.dcpart);

	Phase good = new Phase("ok", CDD.TRUE);
	Phase bad = new Phase("FINAL", CDD.TRUE);
	good.addTransition(good, CDD.TRUE, new String[0]);
	good.addTransition(bad, /*BooleanDecision.create("Min <= current")*/
			   CDD.TRUE
			   .and(BooleanDecision.create("current <= Max"))
			   .negate(), new String[0]);
	bad.addTransition(bad, CDD.TRUE, new String[0]);
	PEATestAutomaton tester = 
	    new PEATestAutomaton("tester",
				   new Phase[] { good, bad },
				   new Phase[] { good },new ArrayList<String>(),
                   new Phase[] { bad });
	PEATestAutomaton all = tester.parallel(pea);
    all = all.parallel(new PhaseEventAutomata("inv", 
						  new Phase[] {invP}, 
						  new Phase[] {invP}));
	//all = all.parallel(tester);
	SimplifyPEAs simplifier = new SimplifyPEAs();
	simplifier.removeAllEvents(all);
	all = simplifier.mergeFinalLocations(all, "FINAL");
	simplifier.mergeTransitions(all);

// 	elev.csppart.parallel(elev.zpart).dump();
	all.dump();

	PEA2ARMCConverter pea2armcFast = new PEA2ARMCConverter();
	ArrayList<String> addVars = new ArrayList<String>();
	ArrayList<String> addTypes = new ArrayList<String>();
	addVars.add("current");
	addVars.add("goal");
	addVars.add("dir");
	addVars.add("Min");
	addVars.add("Max");
	addTypes.add("integer");
	addTypes.add("integer");
	addTypes.add("integer");
	addTypes.add("integer");
	addTypes.add("integer");

	
	pea2armcFast.convert(all, "./elevator.armc", 
			     addVars, addTypes, false);

	System.err.println(""+all.getPhases().length+" total states.");

// 	System.out.println("/* Complete System */");
// 	System.out.println("#locs "+all.phases.length);
// 	int trans = 0;
// 	for (i = 0; i < all.phases.length; i++) {
// 	    trans += all.phases[i].getTransitions().size();
// 	}
// 	System.out.println("#trans "+trans);
// 	//System.out.println("#clocks "+clocks);
// 	for (i = 0; i < all.phases.length; i++)
// 	    dumpKronos(all.phases[i]);
    }

    public void buildZPart() {
	CDD nnewgoal = EventDecision.create('/', "newgoal");
	CDD nstart   = EventDecision.create('/', "start");
	CDD npassed  = EventDecision.create('/', "passed");
	CDD nstop    = EventDecision.create('/', "stop");
	CDD xicurrent = BooleanDecision.create("current' = current");
	CDD xigoal    = BooleanDecision.create("goal' = goal");
	CDD xidir     = BooleanDecision.create("dir' = dir");
	CDD stutter = nnewgoal.and(nstart).and(npassed).and(nstop)
	    .and(xicurrent).and(xigoal).and(xidir);
	Phase zstate = new Phase("z", CDD.TRUE);
	Phase istate = new Phase("zi", 
				 BooleanDecision.create("Min = current"));
	String[] noresets = new String[0];
	istate.addTransition(zstate, stutter, noresets);
	zstate.addTransition(zstate, stutter, noresets);
	zstate.addTransition(zstate, nnewgoal.negate()
			     .and(nstart).and(npassed).and(nstop)
			     .and(xicurrent).and(xidir)
			     .and(BooleanDecision.create("Min <= goal'"))
			     .and(BooleanDecision.create("goal' <= Max"))
			     .and(BooleanDecision.create("current <= goal'").negate()
				  .or(BooleanDecision.create("goal' <= current").negate())),
			     noresets);
	zstate.addTransition(zstate, nstart.negate()
			     .and(nnewgoal).and(npassed).and(nstop)
			     .and(xicurrent).and(xigoal)
			     .and(BooleanDecision.create("current <= goal")
				  .or(BooleanDecision.create("dir' = -1")))
			     .and(BooleanDecision.create("goal <= current")
				  .or(BooleanDecision.create("dir' = 1"))),
			     noresets);
	zstate.addTransition(zstate, npassed.negate()
			     .and(nnewgoal).and(nstart).and(nstop)
			     .and(xigoal).and(xidir)
			     .and(BooleanDecision.
				  create("current' = current + dir")
				  .and(BooleanDecision.create("current < goal")
				       .or(BooleanDecision.create("goal < current")))),
			     noresets);
	zstate.addTransition(zstate, nstop.negate()
			     .and(nnewgoal).and(nstart).and(npassed)
			     .and(xicurrent).and(xigoal).and(xidir)
			     .and(BooleanDecision.create("current = goal")),
			     noresets);
	zpart = new PhaseEventAutomata("ZPart", new Phase[] {istate, zstate},
				       new Phase[] {istate});
	zpart.dump();
    }

    public void buildCSPPart() {
	String[] noresets = new String[0];
	Phase[] p = new Phase[] { 
	    new Phase("c0", CDD.TRUE, CDD.TRUE),
	    new Phase("c1", CDD.TRUE, CDD.TRUE),
	    new Phase("c2", CDD.TRUE, CDD.TRUE),
	};
	CDD ev;
	for (int i = 0; i < 3; i++) {
	    ev = EventDecision.create('/', "newgoal")
		.and(EventDecision.create('/', "start"))
		.and(EventDecision.create('/', "passed"))
		.and(EventDecision.create('/', "stop"));
	    
	    p[i].addTransition(p[i], ev, noresets);
	}
	
	ev = EventDecision.create("newgoal")
	    .and(EventDecision.create('/', "start"))
	    .and(EventDecision.create('/', "passed"))
	    .and(EventDecision.create('/', "stop"));
	
	p[0].addTransition(p[1], ev, noresets);
	
	ev = EventDecision.create('/', "newgoal")
	.and(EventDecision.create("start"))
	    .and(EventDecision.create('/', "passed"))
	    .and(EventDecision.create('/', "stop"));
	p[1].addTransition(p[2], ev, noresets);

	ev = EventDecision.create('/', "newgoal")
	    .and(EventDecision.create('/', "start"))
	    .and(EventDecision.create("passed"))
	    .and(EventDecision.create('/', "stop"));
	p[2].addTransition(p[2], ev, noresets);
	ev = EventDecision.create('/', "newgoal")
	    .and(EventDecision.create('/', "start"))
	    .and(EventDecision.create('/', "passed"))
	    .and(EventDecision.create("stop"));
	p[2].addTransition(p[0], ev, noresets);
	csppart = new PhaseEventAutomata("CSPPart", p, new Phase[] { p[0] });
    }

    public void buildDCPart() {
	CDD passed = EventDecision.create("passed");
	CDD cgeq = BooleanDecision.create("current <= goal")
	    .and(BooleanDecision.create("goal <= current"));
	Trace2PEACompiler compiler = new Trace2PEACompiler();
	PhaseEventAutomata dc1, dc2;
	dc1 = compiler.compile("passed_not_too_fast", 
			     new CounterTrace(new CounterTrace.DCPhase[] {
	    new CounterTrace.DCPhase(CDD.TRUE),
	    new CounterTrace.DCPhase(passed, CDD.TRUE, 
				     CounterTrace.BOUND_LESSEQUAL, 3),
	    new CounterTrace.DCPhase(passed, CDD.TRUE)
	}));

	dc2 = compiler.compile("stop_on_floor", 
			     new CounterTrace(new CounterTrace.DCPhase[] {
	    new CounterTrace.DCPhase(CDD.TRUE),
	    new CounterTrace.DCPhase(cgeq.negate()),
	    new CounterTrace.DCPhase(cgeq,
				     CounterTrace.BOUND_GREATEREQUAL, 2,
				     Collections.singleton("stop")),
	    new CounterTrace.DCPhase(CDD.TRUE)
	}));
	dc1.dump();
	dc2.dump();
	dcpart = dc1.parallel(dc2);
    }

}
