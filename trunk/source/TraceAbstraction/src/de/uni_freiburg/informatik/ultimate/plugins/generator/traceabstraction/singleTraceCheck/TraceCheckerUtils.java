/*
 * Copyright (C) 2014-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE TraceAbstraction plug-in.
 * 
 * The ULTIMATE TraceAbstraction plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE TraceAbstraction plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE TraceAbstraction plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE TraceAbstraction plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE TraceAbstraction plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.core.services.model.ILogger;
import de.uni_freiburg.informatik.ultimate.core.services.model.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.ICallAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IInternalAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IReturnAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.IHoareTripleChecker;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.IHoareTripleChecker.Validity;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ProgramPoint;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.CoverageAnalysis;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.CoverageAnalysis.BackwardCoveringInformation;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.MonolithicHoareTripleChecker;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.util.DebugMessage;

/**
 * Class that contains static methods that are related to the TraceChecker 
 * @author Matthias Heizmann
 *
 */
public class TraceCheckerUtils {
	
	/**
	 * Given a trace cb_0,...,cb_n returns the sequence of ProgramPoints 
	 * that corresponds to this trace. This is the sequence
	 * pp_0,...,pp_{n+1} such that
	 * <ul>
	 * <li> pp_i is the ProgramPoint before CodeBlock cb_i, and
	 * <li> pp_{i+1} is the ProgramPoint after CodeBlock cb_i.
	 * </ul>  
	 */
	public static List<ProgramPoint> getSequenceOfProgramPoints(
											NestedWord<CodeBlock> trace) {
		List<ProgramPoint> result = new ArrayList<ProgramPoint>();
		for (CodeBlock cb : trace) {
			ProgramPoint pp = (ProgramPoint) cb.getSource();
			result.add(pp);
		}
		CodeBlock cb = trace.getSymbol(trace.length()-1);
		ProgramPoint pp = (ProgramPoint) cb.getTarget();
		result.add(pp);
		return result;
	}
	
	/**
	 * Variant of computeCoverageCapability where the sequence of ProgramPoints
	 * is not a parameter but computed from the trace.
	 * @param logger 
	 */
	public static BackwardCoveringInformation computeCoverageCapability(
			IUltimateServiceProvider services, 
			IInterpolantGenerator traceChecker, ILogger logger) {
		NestedWord<CodeBlock> trace = (NestedWord<CodeBlock>) NestedWord.nestedWord(traceChecker.getTrace());
		List<ProgramPoint> programPoints = getSequenceOfProgramPoints(trace);
		return computeCoverageCapability(services, traceChecker, programPoints, logger);
	}
	
	public static BackwardCoveringInformation computeCoverageCapability(
			IUltimateServiceProvider services, 
			IInterpolantGenerator interpolantGenerator, List<ProgramPoint> programPoints, ILogger logger) {
		if (interpolantGenerator.getInterpolants() == null) {
			throw new AssertionError("We can only build an interpolant "
					+ "automaton for which interpolants were computed");
		}
		CoverageAnalysis ca = new CoverageAnalysis(services, interpolantGenerator, programPoints, logger);
		ca.analyze();
		return ca.getBackwardCoveringInformation();
	}
	
	
	/**
	 * The sequence of interpolants returned by a TraceChecker contains neither
	 * the precondition nor the postcondition of the trace check.
	 * This auxiliary class allows one to access the precondition via the
	 * index 0 and to access the postcondition via the index 
	 * interpolants.lenth+1 (first index after the interpolants array).
	 * All other indices are shifted by one.
	 * 
	 * In the future we might also use negative indices to access pending
	 * contexts (therefore you should not catch the Error throw by the 
	 * getInterpolant method).
	 */
	public static class InterpolantsPreconditionPostcondition {
		private final IPredicate m_Precondition;
		private final IPredicate m_Postcondition;
		private final List<IPredicate> m_Interpolants;
		
		public InterpolantsPreconditionPostcondition(IInterpolantGenerator interpolantGenerator) {
			if (interpolantGenerator.getInterpolants() == null) {
				throw new AssertionError("We can only build an interpolant "
						+ "automaton for which interpolants were computed");
			}
			m_Precondition = interpolantGenerator.getPrecondition();
			m_Postcondition = interpolantGenerator.getPostcondition();
			m_Interpolants = Arrays.asList(interpolantGenerator.getInterpolants());
		}
		
		public InterpolantsPreconditionPostcondition(IPredicate precondition,
				IPredicate postcondition, List<IPredicate> interpolants) {
			super();
			m_Precondition = precondition;
			m_Postcondition = postcondition;
			m_Interpolants = interpolants;
		}

		public IPredicate getInterpolant(int i) {
			if (i < 0) {
				throw new AssertionError("index beyond precondition");
			} else if (i == 0) {
				return m_Precondition;
			} else if (i <= m_Interpolants.size()) {
				return m_Interpolants.get(i-1);
			} else if (i == m_Interpolants.size()+1) {
				return m_Postcondition;
			} else {
				throw new AssertionError("index beyond postcondition");
			}
		}
		
		public List<IPredicate> getInterpolants() {
			return Collections.unmodifiableList(m_Interpolants);
		}
	}
	
	
	/***
	 * Checks whether the given sequence of predicates is inductive.
	 * For each i we check if  {predicates[i-1]} st_i {predicates[i]} is a 
	 * valid Hoare triple. If all triples are valid, we return true.
	 * Otherwise an exception is thrown.
	 */
	public static boolean checkInterpolantsInductivityForward(List<IPredicate> interpolants, NestedWord<? extends IAction> trace, 
			IPredicate precondition, IPredicate postcondition, 
			SortedMap<Integer, IPredicate> pendingContexts, String computation, 
			SmtManager smtManager, ModifiableGlobalVariableManager mgvManager,
			ILogger logger) {
		InterpolantsPreconditionPostcondition ipp = 
				new InterpolantsPreconditionPostcondition(precondition, postcondition, interpolants);
		Validity result;
		for (int i = 0; i <= interpolants.size(); i++) {
			result = checkInductivityAtPosition(i, ipp, trace, pendingContexts, smtManager, mgvManager, logger);
			if (result != Validity.VALID && result != Validity.UNKNOWN) {
				throw new AssertionError("invalid Hoare triple in " + computation);
			}
		}
		return true;
	}
	
	/***
	 * Similar to the method checkInterpolantsInductivityForward.
	 * But here we start from the end. This ensures that we get the last
	 * Hoare triple that is invalid.
	 * 
	 * @see checkInterpolantsInductivityForward
	 */
	public static boolean checkInterpolantsInductivityBackward(List<IPredicate> interpolants, NestedWord<? extends IAction> trace, 
			IPredicate precondition, IPredicate postcondition, 
			SortedMap<Integer, IPredicate> pendingContexts, String computation, 
			SmtManager smtManager, ModifiableGlobalVariableManager mgvManager,
			ILogger logger) {
		InterpolantsPreconditionPostcondition ipp = 
				new InterpolantsPreconditionPostcondition(precondition, postcondition, interpolants);
		Validity result;
		for (int i = interpolants.size(); i >= 0; i--) {
			result = checkInductivityAtPosition(i, ipp, trace, pendingContexts, smtManager, mgvManager, logger);
			if (result != Validity.VALID && result != Validity.UNKNOWN) {
				throw new AssertionError("invalid Hoare triple in " + computation);
			}
		}
		return true;
	}
	
	
	private static Validity checkInductivityAtPosition(int i,
			InterpolantsPreconditionPostcondition ipp,
			NestedWord<? extends IAction> trace,
			SortedMap<Integer, IPredicate> pendingContexts,
			SmtManager smtManager, ModifiableGlobalVariableManager mgvManager,
			ILogger logger) {
		IHoareTripleChecker htc = new MonolithicHoareTripleChecker(smtManager);
		IPredicate predecessor = ipp.getInterpolant(i);
		IPredicate successor = ipp.getInterpolant(i+1);
		IAction cb = trace.getSymbol(i);
		final Validity result;
		if (trace.isCallPosition(i)) {
			assert (cb instanceof ICallAction) : "not Call at call position";
			result = htc.checkCall(predecessor, (ICallAction) cb, successor);
			logger.info(new DebugMessage("{0}: Hoare triple '{'{1}'}' {2} '{'{3}'}' is {4}", 
					i, predecessor, cb, successor, result));
		} else if (trace.isReturnPosition(i)) {
			assert (cb instanceof IReturnAction) : "not Call at call position";
			IPredicate hierarchicalPredecessor;
			if (trace.isPendingReturn(i)) {
				hierarchicalPredecessor = pendingContexts.get(i);
			} else {
				int callPosition = trace.getCallPosition(i);
				hierarchicalPredecessor = ipp.getInterpolant(callPosition);
			}
			result = htc.checkReturn(predecessor, hierarchicalPredecessor, (IReturnAction) cb, successor);
			logger.info(new DebugMessage("{0}: Hoare quadruple '{'{1}'}' '{'{5}'}' {2} '{'{3}'}' is {4}", 
					i, predecessor, cb, successor, result, hierarchicalPredecessor));
		} else if (trace.isInternalPosition(i)) {
			assert (cb instanceof IInternalAction);
			result = htc.checkInternal(predecessor, (IInternalAction) cb, successor);
			logger.info(new DebugMessage("{0}: Hoare triple '{'{1}'}' {2} '{'{3}'}' is {4}", 
					i, predecessor, cb, successor, result));
		} else {
			throw new AssertionError("unsupported position");
		}
		return result;
	}


	
}
