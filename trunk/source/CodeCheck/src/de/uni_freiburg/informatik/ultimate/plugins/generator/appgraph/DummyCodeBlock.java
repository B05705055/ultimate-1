package de.uni_freiburg.informatik.ultimate.plugins.generator.appgraph;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.util.IRCFGVisitor;

public class DummyCodeBlock extends CodeBlock {

	private static final long serialVersionUID = 1L;

	public DummyCodeBlock(Logger logger) {
		super(null, null, logger);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected String[] getFieldNames() {
		return new String[] {};
	}

	@Override
	public String getPrettyPrintedStatements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		return "DUMMYCODEBLOCK";
	}

	@Override
	public void accept(IRCFGVisitor visitor) {
		visitor.visitCodeBlock(this);
		visitor.visitedCodeBlock(this);
	}

}
