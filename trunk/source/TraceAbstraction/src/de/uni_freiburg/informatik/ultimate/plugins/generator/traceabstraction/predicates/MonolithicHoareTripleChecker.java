package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates;

import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Call;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Return;

public class MonolithicHoareTripleChecker implements IHoareTripleChecker {
	
	private final SmtManager m_SmtManager;
	private HoareTripleCheckerBenchmarkGenerator m_EdgeCheckerBenchmark;
	
	

	public MonolithicHoareTripleChecker(SmtManager smtManager) {
		super();
		m_SmtManager = smtManager;
		m_EdgeCheckerBenchmark = new HoareTripleCheckerBenchmarkGenerator();
	}

	@Override
	public Validity checkInternal(IPredicate pre, CodeBlock cb, IPredicate succ) {
		m_EdgeCheckerBenchmark.continueEdgeCheckerTime();
		Validity result = SmtManager.lbool2validity(m_SmtManager.isInductive(pre, cb, succ));
		m_EdgeCheckerBenchmark.stopEdgeCheckerTime();
		switch (result) {
		case INVALID:
			m_EdgeCheckerBenchmark.getSolverCounterSat().incIn();
			break;
		case UNKNOWN:
			m_EdgeCheckerBenchmark.getSolverCounterUnknown().incIn();
			break;
		case VALID:
			m_EdgeCheckerBenchmark.getSolverCounterUnsat().incIn();
			break;
		default:
			throw new AssertionError("unknown case");
		}
		return result;
	}

	@Override
	public Validity checkCall(IPredicate pre, CodeBlock cb, IPredicate succ) {
		m_EdgeCheckerBenchmark.continueEdgeCheckerTime();
		Validity result =  SmtManager.lbool2validity(m_SmtManager.isInductiveCall(pre, (Call) cb, succ));
		m_EdgeCheckerBenchmark.stopEdgeCheckerTime();
		switch (result) {
		case INVALID:
			m_EdgeCheckerBenchmark.getSolverCounterSat().incCa();
			break;
		case UNKNOWN:
			m_EdgeCheckerBenchmark.getSolverCounterUnknown().incCa();
			break;
		case VALID:
			m_EdgeCheckerBenchmark.getSolverCounterUnsat().incCa();
			break;
		default:
			throw new AssertionError("unknown case");
		}
		return result;
	}

	@Override
	public Validity checkReturn(IPredicate preLin, IPredicate preHier,
			CodeBlock cb, IPredicate succ) {
		m_EdgeCheckerBenchmark.continueEdgeCheckerTime();
		Validity result =  SmtManager.lbool2validity(m_SmtManager.isInductiveReturn(preLin, preHier, (Return) cb, succ));
		m_EdgeCheckerBenchmark.stopEdgeCheckerTime();
		switch (result) {
		case INVALID:
			m_EdgeCheckerBenchmark.getSolverCounterSat().incRe();
			break;
		case UNKNOWN:
			m_EdgeCheckerBenchmark.getSolverCounterUnknown().incRe();
			break;
		case VALID:
			m_EdgeCheckerBenchmark.getSolverCounterUnsat().incRe();
			break;
		default:
			throw new AssertionError("unknown case");
		}
		return result;
	}

	public HoareTripleCheckerBenchmarkGenerator getEdgeCheckerBenchmark() {
		return m_EdgeCheckerBenchmark;
	}

	@Override
	public void releaseLock() {
		// do nothing, since objects of this class do not lock the solver
	}
	
	

}
