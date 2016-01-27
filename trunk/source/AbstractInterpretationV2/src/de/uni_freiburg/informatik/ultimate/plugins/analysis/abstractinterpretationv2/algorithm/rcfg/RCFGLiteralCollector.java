/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Marius Greitschus (greitsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE AbstractInterpretationV2 plug-in.
 * 
 * The ULTIMATE AbstractInterpretationV2 plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE AbstractInterpretationV2 plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE AbstractInterpretationV2 plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE AbstractInterpretationV2 plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE AbstractInterpretationV2 plug-in grant you additional permission 
 * to convey the resulting work.
 */

package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.algorithm.rcfg;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.model.boogie.BoogieVisitor;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.IntegerLiteral;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.RealLiteral;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.algorithm.ILiteralCollector;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.algorithm.generic.LiteralCollection;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Call;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RCFGEdge;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RCFGNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.StatementSequence;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.util.RCFGEdgeVisitor;

/**
 * Collects literals of type int or real found in an RCFG. Some widening operators can use these.
 * 
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 */
public class RCFGLiteralCollector extends RCFGEdgeVisitor implements ILiteralCollector {

	private final Set<String> mLiterals;
	private final Set<BigDecimal> mNumberLiterals;
	private final StatementLiteralCollector mStatementLiteralCollector;
	private final LiteralCollection mLiteralCollection;

	public RCFGLiteralCollector(final RCFGNode root) {
		mLiterals = new HashSet<String>();
		mNumberLiterals = new HashSet<>();
		mStatementLiteralCollector = new StatementLiteralCollector();
		process(root.getOutgoingEdges());
		mLiteralCollection = new LiteralCollection(mNumberLiterals);
	}

	public LiteralCollection getLiteralCollection() {
		return mLiteralCollection;
	}

//	private void addBoundaryLiterals(Set<BigDecimal> numbers) {
//		final Set<BigDecimal> adds = new HashSet<BigDecimal>();
//		for (final BigDecimal number : numbers) {
//			adds.add(number.add(BigDecimal.ONE));
//			adds.add(number.subtract(BigDecimal.ONE));
//		}
//		numbers.addAll(adds);
//	}

	private <T extends RCFGEdge> void process(final Collection<T> edges) {
		final Deque<RCFGEdge> worklist = new ArrayDeque<RCFGEdge>();
		final Set<RCFGEdge> finished = new HashSet<RCFGEdge>();

		worklist.addAll(edges);
		while (!worklist.isEmpty()) {
			final RCFGEdge current = worklist.removeFirst();
			if (!finished.add(current)) {
				continue;
			}
			visit(current);
			worklist.addAll(current.getTarget().getOutgoingEdges());
		}
	}

	@Override
	protected void visit(StatementSequence c) {
		super.visit(c);
		for (final Statement s : c.getStatements()) {
			mStatementLiteralCollector.processStatement(s);
		}
	}

	@Override
	protected void visit(Call c) {
		super.visit(c);
		mStatementLiteralCollector.processStatement(c.getCallStatement());
	}

	/**
	 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
	 */
	private final class StatementLiteralCollector extends BoogieVisitor {

		@Override
		protected Statement processStatement(Statement statement) {
			// override because we need the visibility here
			return super.processStatement(statement);
		}

		@Override
		protected void visit(IntegerLiteral expr) {
			super.visit(expr);
			mLiterals.add(expr.getValue());
			mNumberLiterals.add(new BigDecimal(expr.getValue()));
		}

		@Override
		protected void visit(RealLiteral expr) {
			super.visit(expr);
			mLiterals.add(expr.getValue());
			mNumberLiterals.add(new BigDecimal(expr.getValue()));
		}
	}

	@Override
	public String toString() {
		return getLiteralCollection().toString();
	}
}