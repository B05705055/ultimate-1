package de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.model.boogie.ast.CallStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.output.BoogiePrettyPrinter;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.util.IRCFGVisitor;

/**
 * Edge in a recursive control flow graph that represents the return from a
 * called procedure. This represents the execution starting from the position
 * directly before the return statement (resp. the last position of a procedure
 * if there is no return statement) to the position after the corresponding call
 * statement. The update of the variables of the calling procedure is defined in
 * the TransFormula.
 * 
 * @author heizmann@informatik.uni-freiburg.de
 * 
 */
public class Return extends CodeBlock {

	private static final long serialVersionUID = 3561826943033450950L;

	private final Call m_CorrespondingCall;

	Return(int serialNumber, ProgramPoint source, ProgramPoint target, Call correspondingCall, Logger logger) {
		super(serialNumber, source, target, logger);
		m_CorrespondingCall = correspondingCall;
		updatePayloadName();
	}

	@Override
	public void updatePayloadName() {
		super.getPayload().setName("return " + BoogiePrettyPrinter.print(getCallStatement()));
	}

	public Call getCorrespondingCall() {
		return m_CorrespondingCall;
	}

	public ProgramPoint getCallerProgramPoint() {
		return (ProgramPoint) getCorrespondingCall().getSource();
	}

	/**
	 * The published attributes. Update this and getFieldValue() if you add new
	 * attributes.
	 */
	private final static String[] s_AttribFields = { "CallStatement", "PrettyPrintedStatements", "TransitionFormula",
			"OccurenceInCounterexamples" };

	@Override
	protected String[] getFieldNames() {
		return s_AttribFields;
	}

	@Override
	protected Object getFieldValue(String field) {
		if (field == "CallStatement") {
			return m_CorrespondingCall.getCallStatement();
		} else if (field == "PrettyPrintedStatements") {
			return m_CorrespondingCall.getPrettyPrintedStatements();
		} else {
			return super.getFieldValue(field);
		}
	}

	public String getPrettyPrintedStatements() {
		return "Return - Corresponding call: " + m_CorrespondingCall.getPrettyPrintedStatements();
	}

	public CallStatement getCallStatement() {
		return m_CorrespondingCall.getCallStatement();
	}

	@Override
	public String toString() {
		return "return;";
	}

	/**
     * Implementing the visitor pattern
     */
	@Override
	public void accept(IRCFGVisitor visitor) {			
		visitor.visitEdge(this);
		visitor.visitCodeBlock(this);
		visitor.visit(this);
		visitor.visitedCodeBlock(this);
		visitor.visitedEdge(this);
	}
}
