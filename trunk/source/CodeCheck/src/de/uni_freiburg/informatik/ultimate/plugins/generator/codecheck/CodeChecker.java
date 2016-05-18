/*
 * Copyright (C) 2013-2015 Alexander Nutz (nutz@informatik.uni-freiburg.de)
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2013-2015 Mostafa Mahmoud Amin
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE CodeCheck plug-in.
 * 
 * The ULTIMATE CodeCheck plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE CodeCheck plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE CodeCheck plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE CodeCheck plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE CodeCheck plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck;

import java.util.HashSet;

import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedRun;
import de.uni_freiburg.informatik.ultimate.core.model.models.IElement;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.IHoareTripleChecker;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.appgraph.AnnotatedProgramPoint;
import de.uni_freiburg.informatik.ultimate.plugins.generator.appgraph.ImpRootNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.PredicateUnifier;
import de.uni_freiburg.informatik.ultimate.util.relation.IsContained;
import de.uni_freiburg.informatik.ultimate.util.relation.NestedMap3;
import de.uni_freiburg.informatik.ultimate.util.relation.NestedMap4;

public abstract class CodeChecker {

	protected RootNode m_originalRoot;
	protected SmtManager m_smtManager;
	protected ImpRootNode m_graphRoot;


	protected IHoareTripleChecker _edgeChecker;
	protected PredicateUnifier m_predicateUnifier;

	/*
	 * Maps for storing edge check results. Not that in case of ImpulseChecker these really are valid, not sat, triples.
	 * TODO: either change name, make duplicates for ImpulseChecker, or modify ImpulseChecker such that those are really sat triples.
	 */
	protected NestedMap3<IPredicate, CodeBlock, IPredicate, IsContained> _satTriples;
	protected NestedMap3<IPredicate, CodeBlock, IPredicate, IsContained> _unsatTriples;
	protected NestedMap4<IPredicate, IPredicate, CodeBlock, IPredicate, IsContained> _satQuadruples;
	protected NestedMap4<IPredicate, IPredicate, CodeBlock, IPredicate, IsContained> _unsatQuadruples;

	// stats
	protected int memoizationHitsSat = 0;
	protected int memoizationHitsUnsat = 0;
	protected int memoizationReturnHitsSat = 0;
	protected int memoizationReturnHitsUnsat = 0;

	protected GraphWriter _graphWriter;

	public CodeChecker(IElement root, SmtManager smtManager, RootNode originalRoot, ImpRootNode graphRoot, GraphWriter graphWriter,
			IHoareTripleChecker edgeChecker, PredicateUnifier predicateUnifier, ILogger logger) {
		mLogger = logger;
		this.m_smtManager = smtManager;
		this.m_originalRoot = originalRoot;
		this.m_graphRoot = graphRoot;

		this._edgeChecker = edgeChecker;
		this.m_predicateUnifier = predicateUnifier;

		this._graphWriter = graphWriter;
	}

	public abstract boolean codeCheck(NestedRun<CodeBlock, AnnotatedProgramPoint> errorRun, IPredicate[] interpolants,
			AnnotatedProgramPoint procedureRoot);

	public abstract boolean codeCheck(NestedRun<CodeBlock, AnnotatedProgramPoint> errorRun, IPredicate[] interpolants,
			AnnotatedProgramPoint procedureRoot,
			NestedMap3<IPredicate, CodeBlock, IPredicate, IsContained> satTriples,
			NestedMap3<IPredicate, CodeBlock, IPredicate, IsContained> unsatTriples);

	public abstract boolean codeCheck(NestedRun<CodeBlock, AnnotatedProgramPoint> errorRun, IPredicate[] interpolants,
			AnnotatedProgramPoint procedureRoot,
			NestedMap3<IPredicate, CodeBlock, IPredicate, IsContained> satTriples,
			NestedMap3<IPredicate, CodeBlock, IPredicate, IsContained> unsatTriples,
			NestedMap4<IPredicate, IPredicate, CodeBlock, IPredicate, IsContained> satQuadruples,
			NestedMap4<IPredicate, IPredicate, CodeBlock, IPredicate, IsContained> unsatQuadruples);

	/**
	 * Given 2 predicates, return a predicate which is the conjunction of both.
	 * 
	 * @param a
	 *            : The first Predicate.
	 * @param b
	 *            : The second Predicate.
	 */
	protected IPredicate conjugatePredicates(IPredicate a, IPredicate b) {
		Term tvp = m_smtManager.getPredicateFactory().and(a, b);
		return m_predicateUnifier.getOrConstructPredicate(tvp);
	}

	/**
	 * Given a predicate, return a predicate which is the negation of it.
	 * 
	 * @param a
	 *            : The Predicate.
	 */
	protected IPredicate negatePredicate(IPredicate a) {
		Term tvp = m_smtManager.getPredicateFactory().not(a);
		return m_predicateUnifier.getOrConstructPredicate(tvp);
	}

	/**
	 * Given a predicate, return a predicate which is the negation of it, not
	 * showing it to the PredicateUnifier
	 * 
	 * @param a
	 *            : The Predicate.
	 */
	protected IPredicate negatePredicateNoPU(IPredicate a) {
		final Term negation = m_smtManager.getPredicateFactory().not(a);
		return m_smtManager.getPredicateFactory().newPredicate(negation);
	}


	public static String objectReference(Object o) {
		return Integer.toHexString(System.identityHashCode(o));
	}

	/**
	 * Debugs all the nodes in a graph.
	 */
	HashSet<AnnotatedProgramPoint> visited = new HashSet<AnnotatedProgramPoint>();
	protected final ILogger mLogger;

	public void debug() {
		visited.clear();
		dfs(m_graphRoot);
	}

	protected boolean debugNode(AnnotatedProgramPoint node) {
		return debugNode(node, "");
	}

	/**
	 * A method used for debugging, it prints all the connections of a given
	 * node. A message can be added to the printed information.
	 * 
	 * @param node
	 * @param message
	 * @return
	 */
	protected boolean debugNode(AnnotatedProgramPoint node, String message) {
		String display = "";
		/*
		 * display += String.format("connected To: %s\n",
		 * node.getOutgoingNodes()); display +=
		 * String.format("connected Fr: %s\n", node.getIncomingNodes());
		 */
		// if (node.m_outgoingReturnCallPreds != null &&
		// node.m_outgoingReturnCallPreds.size() > 0) {
		// display += String.format("outGoing: %s\n",
		// node.m_outgoingReturnCallPreds);
		// }
		// if (node.m_nodesThatThisIsReturnCallPredOf != null &&
		// node.m_nodesThatThisIsReturnCallPredOf.size() > 0) {
		// display += String.format("inHyperEdges: %s\n",
		// node.m_nodesThatThisIsReturnCallPredOf);
		// }
		if (display.length() > 0) {
			display = String.format("%s\nNode %s:\n", message, node) + display;
			mLogger.debug(display);
		}
		return false;
	}

	/**
	 * Depth First Search algorithm that debugs all the nodes in a graph.
	 * 
	 * @param node
	 *            : The current Node being explored in the DFS.
	 * @return
	 */
	private boolean dfs(AnnotatedProgramPoint node) {
		if (!visited.contains(node)) {
			visited.add(node);
			debugNode(node);
			AnnotatedProgramPoint[] adj = node.getOutgoingNodes().toArray(new AnnotatedProgramPoint[] {});
			for (AnnotatedProgramPoint child : adj)
				dfs(child);
		}
		return false;
	}
}
