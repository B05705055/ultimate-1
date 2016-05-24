/*
 * Copyright (C) 2013-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.core.lib.util.MonitoredProcess;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.Assignments;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException;
import java_cup.runtime.Symbol;

/**
 * This class runs an external SMT solver. The main methods are <code>input</code>, which gives an input to the SMT
 * solver, and the <code>parse...</code> methods, which parse the output from the SMT solver.
 * 
 * @author Oday Jubran
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * @author Matthias Heizmann
 * 
 */
class Executor {

	private MonitoredProcess mProcess;
	private Lexer mLexer;
	private BufferedWriter mWriter;
	private InputStream mStdErr;

	private final Script mScript;
	private final String mSolverCmd;
	private final ILogger mLogger;
	private final IUltimateServiceProvider mServices;
	private final IToolchainStorage mStorage;
	private final String mName;

	private static final String sEofErrorMessage = "Received EOF on stdin.";

	Executor(final String solverCommand, final Script script, final ILogger logger,
			final IUltimateServiceProvider services, final IToolchainStorage storage, final String solverName)
					throws IOException {
		mServices = services;
		mStorage = storage;
		mSolverCmd = solverCommand;
		mScript = script;
		mLogger = logger;
		mName = solverName;
		createProcess();
	}

	private void createProcess() throws IOException {
		// mLogger = ILogger.getRootLogger();
		mProcess = MonitoredProcess.exec(mSolverCmd, "(exit)", mServices, mStorage);
		// TODO:
		// Let all processes terminate when the toolchain terminates
		mProcess.setTerminationAfterToolchainTimeout(20 * 1000);

		if (mProcess == null) {
			final String errorMsg = getLogStringPrefix() + " Could not create process, terminating... ";
			mLogger.fatal(errorMsg);
			throw new IllegalStateException(errorMsg);
		}

		OutputStream stdin = mProcess.getOutputStream();
		InputStream stdout = mProcess.getInputStream();

		mStdErr = mProcess.getErrorStream();

		MySymbolFactory symfactory = new MySymbolFactory();
		mLexer = new Lexer(new InputStreamReader(stdout));
		mLexer.setSymbolFactory(symfactory);

		mWriter = new BufferedWriter(new OutputStreamWriter(stdin));

		input("(set-option :print-success true)");
		parseSuccess();
	}

	public void input(String in) {
		if (mLogger.isDebugEnabled()) {
			mLogger.debug(getLogStringPrefix() + " " + in);
		}
		try {
			mWriter.write(in + "\n");
			mWriter.flush();
		} catch (IOException e) {
			if (mServices.getProgressMonitorService().continueProcessing()) {
				throw new SMTLIBException(getLogStringPrefix() + " Connection to SMT solver broken", e);
			} else {
				throw new ToolchainCanceledException(getClass());
			}
		}
	}

	public void exit() {
		input("(exit)");
		// 2015-11-12 Matthias: Do not parse "success" after exit.
		// Some solvers do return success (Barcelogic, CVC4, Z3) some solvers
		// don't do it (Princess, SMTInterpol).
		// parseSuccess();
		mProcess.forceShutdown();
		mProcess = null;

	}

	private List<Symbol> parseSexpr(Lexer lexer) throws IOException {
		ArrayList<Symbol> result = new ArrayList<Symbol>();
		int parenLevel = 0;
		do {
			Symbol sym = lexer.next_token();
			if (sym.sym == LexerSymbols.LPAR)
				parenLevel++;
			if (sym.sym == LexerSymbols.RPAR)
				parenLevel--;
			result.add(sym);
		} while (parenLevel > 0);
		return result;
	}

	private List<Symbol> readAnswer() {
		try {
			final List<Symbol> result = parseSexpr(mLexer);
			if (mLogger.isDebugEnabled()) {
				for (Symbol s : result) {
					mLogger.debug(s.toString() + "\n");
				}
			}
			return result;
		} catch (IOException e) {
			throw new SMTLIBException(getLogStringPrefix() + " Connection to SMT solver broken", e);
		}
	}

	public void reset() throws IOException {
		try {
			mWriter.write("(exit)\n");
			mWriter.flush();
		} catch (IOException e) {
			/* ignore */
		}
		mProcess.forceShutdown();
		createProcess();
	}

	public Symbol parse(int what) {
		final List<Symbol> answer = readAnswer();
		String stderr = "";

		// clear the std error buffer as it blocks when it runs full
		try {
			if (mStdErr.available() > 0) {
				final StringBuilder sb = new StringBuilder();
				while (mStdErr.available() > 0) {
					int i = mStdErr.read();
					char c = (char) i;
					sb.append(c);
				}
				stderr = sb.toString();
				mLogger.warn(getLogStringPrefix() + " " + generateStderrMessage(stderr));
			}
		} catch (IOException e) {
			// we don't care what happens on stdErr
		}

		final Parser parser = new Parser();
		parser.setScript(mScript);
		answer.add(0, new Symbol(what));
		parser.setAnswer(answer);
		try {
			return parser.parse();
		} catch (SMTLIBException ex) {
			if (ex.getMessage().equals(Parser.s_EOF)) {
				throw new SMTLIBException(getLogStringPrefix() + sEofErrorMessage + " " + generateStderrMessage(stderr),
						ex);
			} else {
				throw ex;
			}
		} catch (UnsupportedOperationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new SMTLIBException(
					getLogStringPrefix() + "Unexpected Exception while parsing. " + generateStderrMessage(stderr), ex);
		}
	}

	public void parseSuccess() {
		parse(LexerSymbols.SUCCESS);
	}

	public LBool parseCheckSatResult() {
		return (LBool) parse(LexerSymbols.CHECKSAT).value;
	}

	public Term[] parseGetAssertionsResult() {
		return (Term[]) parse(LexerSymbols.GETASSERTIONS).value;
	}

	public Term[] parseGetUnsatCoreResult() {
		return (Term[]) parse(LexerSymbols.GETUNSATCORE).value;
	}

	@SuppressWarnings("unchecked")
	public Map<Term, Term> parseGetValueResult() {
		return (Map<Term, Term>) parse(LexerSymbols.GETVALUE).value;
	}

	public Assignments parseGetAssignmentResult() {
		return (Assignments) parse(LexerSymbols.GETASSIGNMENT).value;
	}

	public Object[] parseGetInfoResult() {
		return (Object[]) parse(LexerSymbols.GETINFO).value;
	}

	public Object parseGetOptionResult() {
		return (Object) parse(LexerSymbols.GETOPTION).value;
	}

	public Term parseTerm() {
		return (Term) parse(LexerSymbols.GETTERM).value;
	}

	private String getLogStringPrefix() {
		return mName + " (" + mSolverCmd + ")";
	}

	private static String generateStderrMessage(String stderr) {
		if (stderr.isEmpty()) {
			return "No stderr output.";
		} else {
			return "stderr output: " + stderr;
		}
	}

}
