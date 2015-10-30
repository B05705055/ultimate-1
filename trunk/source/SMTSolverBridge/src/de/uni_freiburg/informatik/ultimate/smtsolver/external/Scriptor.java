/*
 * Copyright (C) 2013-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 Oday Jubran
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE SMTSolverBridge.
 * 
 * The ULTIMATE SMTSolverBridge is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE SMTSolverBridge is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE SMTSolverBridge. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE SMTSolverBridge, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE SMTSolverBridge grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.smtsolver.external;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.core.services.model.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.services.model.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.Assignments;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.Model;
import de.uni_freiburg.informatik.ultimate.logic.NoopScript;
import de.uni_freiburg.informatik.ultimate.logic.PrintTerm;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;

/**
 * Create a script that connects to an external SMT solver. The solver must be
 * SMTLIB-2 compliant and expect commands on standard input. It must return its
 * output on standard output.
 * 
 * Some commands are only partially supported. For example getProof does not
 * return a useful proof object. Also commands, for which the output format is
 * not fully specified, e.g. (get-model), may not return useful return values.
 * 
 * @author Oday Jubran
 */
public class Scriptor extends NoopScript {

	protected Executor m_Executor;
	private LBool m_Status = LBool.UNKNOWN;

	/**
	 * Create a script connecting to an external SMT solver.
	 * 
	 * @param command
	 *            the command that starts the external SMT solver. The solver is
	 *            expected to read smtlib 2 commands on stdin.
	 * @param services
	 * @param storage
	 * @throws IOExceptionO
	 *             If the solver is not installed
	 */
	public Scriptor(String command, Logger logger, IUltimateServiceProvider services, IToolchainStorage storage)
			throws IOException {
		m_Executor = new Executor(command, this, logger, services, storage);
		super.setOption(":print-success", true);
	}

	@Override
	public void setLogic(Logics logic) throws UnsupportedOperationException, SMTLIBException {
		super.setLogic(logic);
		m_Executor.input("(set-logic " + logic + ")");
		m_Executor.parseSuccess();
	}

	@Override
	public void setOption(String opt, Object value) throws UnsupportedOperationException, SMTLIBException {
		if (!opt.equals(":print-success")) {
			StringBuilder sb = new StringBuilder();
			sb.append("(set-option ").append(opt);
			if (value != null) {
				sb.append(" ");
				if (value instanceof String) {
					// symbol
					sb.append(PrintTerm.quoteIdentifier((String) value));
				} else if (value instanceof Object[]) {
					// s-expr
					new PrintTerm().append(sb, (Object[]) value);
				} else {
					sb.append(value.toString());
				}
			}
			sb.append(")");
			m_Executor.input(sb.toString());
			m_Executor.parseSuccess();
		}
	}

	@Override
	public void setInfo(String info, Object value) {
		StringBuilder sb = new StringBuilder();
		sb.append("(set-info ");
		sb.append(info);
		sb.append(' ');
		sb.append(value);
		sb.append(")");
		sb.append(System.lineSeparator());
		m_Executor.input(sb.toString());
		m_Executor.parseSuccess();
	}

	@Override
	public void declareSort(String sort, int arity) throws SMTLIBException {
		super.declareSort(sort, arity);
		StringBuilder sb = new StringBuilder("(declare-sort ").append(PrintTerm.quoteIdentifier(sort));
		sb.append(" ").append(arity).append(")");
		m_Executor.input(sb.toString());
		m_Executor.parseSuccess();
	}

	@Override
	public void defineSort(String sort, Sort[] sortParams, Sort definition) throws SMTLIBException {
		super.defineSort(sort, sortParams, definition);
		PrintTerm pt = new PrintTerm();
		StringBuilder sb = new StringBuilder();
		sb.append("(define-sort ");
		sb.append(PrintTerm.quoteIdentifier(sort));
		sb.append(" (");
		String delim = "";
		for (Sort s : sortParams) {
			sb.append(delim);
			pt.append(sb, s);
			delim = " ";
		}
		sb.append(") ");
		pt.append(sb, definition);
		sb.append(")");
		m_Executor.input(sb.toString());
		m_Executor.parseSuccess();
	}

	@Override
	public void declareFun(String fun, Sort[] paramSorts, Sort resultSort) throws SMTLIBException {
		super.declareFun(fun, paramSorts, resultSort);
		PrintTerm pt = new PrintTerm();
		StringBuilder sb = new StringBuilder();
		sb.append("(declare-fun ");
		sb.append(PrintTerm.quoteIdentifier(fun));
		sb.append(" (");
		String delim = "";
		for (Sort s : paramSorts) {
			sb.append(delim);
			pt.append(sb, s);
			delim = " ";
		}
		sb.append(") ");
		pt.append(sb, resultSort);
		sb.append(")");
		m_Executor.input(sb.toString());
		m_Executor.parseSuccess();
	}

	@Override
	public void defineFun(String fun, TermVariable[] params, Sort resultSort, Term definition) throws SMTLIBException {
		super.defineFun(fun, params, resultSort, definition);
		PrintTerm pt = new PrintTerm();
		StringBuilder sb = new StringBuilder();
		sb.append("(define-fun ");
		sb.append(PrintTerm.quoteIdentifier(fun));
		sb.append(" (");
		String delim = "";
		for (TermVariable t : params) {
			sb.append(delim);
			sb.append("(").append(t).append(" ");
			pt.append(sb, t.getSort());
			sb.append(")");
			delim = " ";
		}
		sb.append(") ");
		pt.append(sb, resultSort);
		pt.append(sb, definition);
		sb.append(")");
		m_Executor.input(sb.toString());
		m_Executor.parseSuccess();
	}

	@Override
	public void push(int levels) throws SMTLIBException {
		super.push(levels);
		m_Executor.input("(push " + levels + ")");
		m_Executor.parseSuccess();
	}

	@Override
	public void pop(int levels) throws SMTLIBException {
		super.pop(levels);
		m_Executor.input("(pop " + levels + ")");
		m_Executor.parseSuccess();
	}

	@Override
	public LBool assertTerm(Term term) throws SMTLIBException {
		// super.assertTerm(term);
		m_Executor.input("(assert " + term.toStringDirect() + ")");
		m_Executor.parseSuccess();
		return LBool.UNKNOWN;
	}

	@Override
	public LBool checkSat() throws SMTLIBException {
		m_Executor.input("(check-sat)");
		m_Status = m_Executor.parseCheckSatResult();
		return m_Status;
	}

	@Override
	public Term[] getAssertions() throws SMTLIBException {
		m_Executor.input("(get-assertions)");
		return m_Executor.parseGetAssertionsResult();
	}

	/** Proofs are not supported, since they are not standardized **/
	@Override
	public Term getProof() throws SMTLIBException, UnsupportedOperationException {
		throw new UnsupportedOperationException("Proofs are not supported");
	}

	@Override
	public Term[] getUnsatCore() throws SMTLIBException, UnsupportedOperationException {
		m_Executor.input("(get-unsat-core)");
		return m_Executor.parseGetUnsatCoreResult();
	}

	@Override
	public Map<Term, Term> getValue(Term[] terms) throws SMTLIBException, UnsupportedOperationException {
		for (Term t : terms) {
			if (!t.getSort().isNumericSort() 
					&& t.getSort() != getTheory().getBooleanSort()
				    && !t.getSort().getRealSort().getName().equals("BitVec"))
				throw new UnsupportedOperationException();
		}
		StringBuilder command = new StringBuilder();
		PrintTerm pt = new PrintTerm();
		command.append("(get-value (");
		String sep = "";
		for (Term t : terms) {
			command.append(sep);
			pt.append(command, t);
			sep = " ";
		}
		command.append("))");
		m_Executor.input(command.toString());
		return m_Executor.parseGetValueResult();
	}

	@Override
	public Assignments getAssignment() throws SMTLIBException, UnsupportedOperationException {
		m_Executor.input("(get-assignment)");
		return m_Executor.parseGetAssignmentResult();
	}

	@Override
	public Object getOption(String opt) throws UnsupportedOperationException {
		m_Executor.input("(get-option " + opt + ")");
		return m_Executor.parseGetOptionResult();
	}

	@Override
	public Object getInfo(String info) throws UnsupportedOperationException {
		m_Executor.input("(get-info " + info + ")");
		Object[] result = m_Executor.parseGetInfoResult();
		if (result.length == 1)
			return result[0];
		return result;
	}

	@Override
	public void exit() {
		m_Executor.exit();

	}

	@Override
	public Term simplify(Term term) throws SMTLIBException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void reset() {
		super.reset();
		try {
			m_Executor.reset();
		} catch (IOException e) {
			// this should only happen if the solver executable is removed
			// between creating executor and calling reset.
			e.printStackTrace();
		}
	}

	@Override
	public Model getModel() throws SMTLIBException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/** This method is used in the output parser, to support (get-info :status) **/
	public LBool getStatus() {
		return m_Status;
	}
}
