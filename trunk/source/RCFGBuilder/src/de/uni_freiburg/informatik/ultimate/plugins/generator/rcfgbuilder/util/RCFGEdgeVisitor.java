/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE RCFGBuilder plug-in.
 * 
 * The ULTIMATE RCFGBuilder plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE RCFGBuilder plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE RCFGBuilder plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE RCFGBuilder plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE RCFGBuilder plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.util;

import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Call;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.GotoEdge;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ParallelComposition;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RCFGEdge;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Return;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootEdge;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.SequentialComposition;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.StatementSequence;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Summary;

public class RCFGEdgeVisitor {

	protected void visit(RCFGEdge e) {
		if (e instanceof CodeBlock) {
			visit((CodeBlock) e);
		} else if (e instanceof RootEdge) {
			visit((RootEdge) e);
		} else {
			throw new UnsupportedOperationException("Extend the new type");
		}
	}

	protected void visit(RootEdge e) {

	}

	protected void visit(CodeBlock c) {
		if (c instanceof Call) {
			visit((Call) c);
		} else if (c instanceof GotoEdge) {
			visit((GotoEdge) c);
		} else if (c instanceof ParallelComposition) {
			visit((ParallelComposition) c);
		} else if (c instanceof Return) {
			visit((Return) c);
		} else if (c instanceof SequentialComposition) {
			visit((SequentialComposition) c);
		} else if (c instanceof Summary) {
			visit((Summary) c);
		} else if (c instanceof StatementSequence) {
			visit((StatementSequence) c);
		} else {
			throw new UnsupportedOperationException("Extend the new type");
		}
	}

	protected void visit(Call c) {
	}

	protected void visit(GotoEdge c) {
	}

	protected void visit(ParallelComposition c) {
		for(final CodeBlock b : c.getCodeBlocks()){
			visit(b);
		}
	}

	protected void visit(Return c) {

	}

	protected void visit(SequentialComposition c) {
		for(final CodeBlock b : c.getCodeBlocks()){
			visit(b);
		}
	}

	protected void visit(Summary c) {

	}

	protected void visit(StatementSequence c) {

	}

}
