/*
 * Copyright (C) 2012-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.uni_freiburg.informatik.ultimate.core.services.model.ILogger;

import de.uni_freiburg.informatik.ultimate.automata.Word;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.core.services.model.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IAction;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Call;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ParallelComposition;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Return;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.SequentialComposition;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.StatementSequence;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Summary;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.TraceChecker.TraceCheckerBenchmarkGenerator;
import de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException;


/**
 * TODO: use quick check
 * @author heizmann@informatik.uni-freiburg.de
 *
 */
public class AnnotateAndAsserter {
	
	protected final IUltimateServiceProvider m_Services;
	protected final ILogger m_Logger;

	protected final Script m_Script;
	protected final SmtManager m_SmtManager;
	protected final NestedWord<? extends IAction> m_Trace;


	protected LBool m_Satisfiable;
	protected final NestedFormulas<Term, Term> m_SSA;
	protected ModifiableNestedFormulas<Term, Term> m_AnnotSSA;

	protected final AnnotateAndAssertCodeBlocks m_AnnotateAndAssertCodeBlocks;

	protected final TraceCheckerBenchmarkGenerator m_Tcbg;

	public AnnotateAndAsserter(SmtManager smtManager,
			NestedFormulas<Term, Term> nestedSSA, 
			AnnotateAndAssertCodeBlocks aaacb, 
			TraceCheckerBenchmarkGenerator tcbg, IUltimateServiceProvider services) {
		m_Services = services;
		m_Logger = m_Services.getLoggingService().getLogger(Activator.s_PLUGIN_ID);
		m_SmtManager = smtManager;
		m_Script = smtManager.getScript();
		m_Trace = nestedSSA.getTrace();
		m_SSA = nestedSSA;
		m_AnnotateAndAssertCodeBlocks = aaacb;
		m_Tcbg = tcbg;
	}


	public void buildAnnotatedSsaAndAssertTerms() {
		if (m_AnnotSSA != null) {
			throw new AssertionError("already build");
		}
		assert m_Satisfiable == null;

		m_AnnotSSA = new ModifiableNestedFormulas<Term, Term>(m_Trace, new TreeMap<Integer, Term>());

		m_AnnotSSA.setPrecondition(m_AnnotateAndAssertCodeBlocks.annotateAndAssertPrecondition());
		m_AnnotSSA.setPostcondition(m_AnnotateAndAssertCodeBlocks.annotateAndAssertPostcondition());

		Collection<Integer> callPositions = new ArrayList<Integer>();
		Collection<Integer> pendingReturnPositions = new ArrayList<Integer>();
		for (int i=0; i<m_Trace.length(); i++) {
			if (m_Trace.isCallPosition(i)) {
				callPositions.add(i);
				m_AnnotSSA.setGlobalVarAssignmentAtPos(i, m_AnnotateAndAssertCodeBlocks.annotateAndAssertGlobalVarAssignemntCall(i));
				m_AnnotSSA.setLocalVarAssignmentAtPos(i, m_AnnotateAndAssertCodeBlocks.annotateAndAssertLocalVarAssignemntCall(i));
				m_AnnotSSA.setOldVarAssignmentAtPos(i, m_AnnotateAndAssertCodeBlocks.annotateAndAssertOldVarAssignemntCall(i));
			} else  {
				if (m_Trace.isReturnPosition(i) && m_Trace.isPendingReturn(i)) {
					pendingReturnPositions.add(i);
				}
				m_AnnotSSA.setFormulaAtNonCallPos(i, m_AnnotateAndAssertCodeBlocks.annotateAndAssertNonCall(i));
			}
		}

		assert callPositions.containsAll(m_Trace.getCallPositions());
		assert m_Trace.getCallPositions().containsAll(callPositions);


		// number that the pending context. The first pending context has
		// number -1, the second -2, ...
		int pendingContextCode = -1 - m_SSA.getTrace().getPendingReturns().size();
		for (Integer positionOfPendingReturn : m_SSA.getTrace().getPendingReturns().keySet()) {
			assert m_Trace.isPendingReturn(positionOfPendingReturn);
			{
				Term annotated = m_AnnotateAndAssertCodeBlocks.annotateAndAssertPendingContext(
						positionOfPendingReturn, pendingContextCode);
				m_AnnotSSA.setPendingContext(positionOfPendingReturn, annotated);
			}
			{
				Term annotated = m_AnnotateAndAssertCodeBlocks.annotateAndAssertLocalVarAssignemntPendingContext(
						positionOfPendingReturn, pendingContextCode);
				m_AnnotSSA.setLocalVarAssignmentAtPos(positionOfPendingReturn, annotated);
			}
			{
				Term annotated = m_AnnotateAndAssertCodeBlocks.annotateAndAssertOldVarAssignemntPendingContext(
						positionOfPendingReturn, pendingContextCode);
				m_AnnotSSA.setOldVarAssignmentAtPos(positionOfPendingReturn, annotated);
			}
			pendingContextCode++;
		}
		try {
			m_Satisfiable = m_SmtManager.getScript().checkSat();
		} catch (SMTLIBException e) {
			if (e.getMessage().contains("Received EOF on stdin. No stderr output.")
					&& !m_Services.getProgressMonitorService().continueProcessing()) {
				throw new ToolchainCanceledException(getClass(), 
						"checking feasibility of error trace whose length is " + m_Trace.length());
			} else {
				throw e;
			}
		}
		// Report benchmarks
		m_Tcbg.reportnewCheckSat();
		m_Tcbg.reportnewCodeBlocks(m_Trace.length());
		m_Tcbg.reportnewAssertedCodeBlocks(m_Trace.length());
		m_Logger.info("Conjunction of SSA is " + m_Satisfiable);
	}



	public LBool isInputSatisfiable() {
		return m_Satisfiable;
	}




	/**
	 * Return a ParallelComposition-free trace of a trace.
	 * While using large block encoding this sequence is not unique.
	 * @param smtManager <ul>
	 * <li> If smtManager is null some branch of a ParallelComposition is taken.
	 * <li> If smtManager is not null, the smtManger has to be a state where a
	 * valuation of this traces branch indicators is available. Then some branch
	 * for which the branch indicator evaluates to true is taken.
	 */
	public static List<CodeBlock> constructFailureTrace(
			Word<CodeBlock> word, SmtManager smtManager) {
		List<CodeBlock> failurePath = new ArrayList<CodeBlock>();
		for (int i=0; i<word.length(); i++) {
			CodeBlock codeBlock = word.getSymbol(i);
			addToFailureTrace(codeBlock, i , failurePath, smtManager);
		}
		return failurePath;
	}

	/**
	 * Recursive method used by getFailurePath
	 */
	private static void addToFailureTrace(CodeBlock codeBlock, int pos, 
			List<CodeBlock> failureTrace, SmtManager smtManager) {
		if (codeBlock instanceof Call) {
			failureTrace.add(codeBlock);
		} else if (codeBlock instanceof Return) {
			failureTrace.add(codeBlock);
		} else if (codeBlock instanceof Summary) {
			failureTrace.add(codeBlock);
		} else if (codeBlock instanceof StatementSequence) {
			failureTrace.add(codeBlock);
		} else if (codeBlock instanceof SequentialComposition) {
			SequentialComposition seqComp = (SequentialComposition) codeBlock;
			for (CodeBlock elem : seqComp.getCodeBlocks()) {
				addToFailureTrace(elem, pos, failureTrace, smtManager);
			}
		} else if (codeBlock instanceof ParallelComposition) {
			ParallelComposition parComp = (ParallelComposition) codeBlock;

			Set<TermVariable> branchIndicators = parComp.getBranchIndicator2CodeBlock().keySet();

			TermVariable taken = null;

			if (smtManager == null) {
				// take some branch
				taken = branchIndicators.iterator().next();
			}
			else {
				// take some branch for which the branch indicator evaluates to
				// true
				for (TermVariable tv : branchIndicators) {
					String constantName = tv.getName()+"_"+pos;
					Term constant = smtManager.getScript().term(constantName);
					Term[] terms = { constant };
					Map<Term, Term> valuation = smtManager.getScript().getValue(terms);
					Term value = valuation.get(constant);
					if (value == smtManager.getScript().term("true")) {
						taken = tv;
					}
				}
			}
			assert (taken != null);
			CodeBlock cb = parComp.getBranchIndicator2CodeBlock().get(taken); 
			addToFailureTrace(cb, pos, failureTrace, smtManager);
		} else {
			throw new IllegalArgumentException("unkown code block");
		}
	}


	public NestedFormulas<Term, Term> getAnnotatedSsa() {
		return m_AnnotSSA;
	}



}
