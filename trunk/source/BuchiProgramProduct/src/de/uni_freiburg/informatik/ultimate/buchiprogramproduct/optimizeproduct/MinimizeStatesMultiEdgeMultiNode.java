/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE BuchiProgramProduct plug-in.
 * 
 * The ULTIMATE BuchiProgramProduct plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE BuchiProgramProduct plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE BuchiProgramProduct plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE BuchiProgramProduct plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE BuchiProgramProduct plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.buchiprogramproduct.optimizeproduct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.model.structure.BaseMultigraphEdge;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula.Infeasibility;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ProgramPoint;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RCFGEdge;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RCFGNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.StatementSequence;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.StatementSequence.Origin;

/**
 * Most aggressive minimization. Tries to remove states no matter what.
 * 
 * @author dietsch@informatik.uni-freiburg.de
 * 
 */
public class MinimizeStatesMultiEdgeMultiNode extends BaseMinimizeStates {

	public MinimizeStatesMultiEdgeMultiNode(RootNode product, IUltimateServiceProvider services,
			IToolchainStorage storage) {
		super(product, services, storage);
	}

	@Override
	protected Collection<? extends RCFGNode> processCandidate(RootNode root, ProgramPoint target, Set<RCFGNode> closed) {
		// we have the incoming edges
		// ei = (qi,sti,q) in EI
		// and the outgoing edges
		// ej = (q,stj,qj) in EO
		// and we will try to replace them by |EI| * |EO| edges

		final List<RCFGNode> incomingNodes = target.getIncomingNodes();
		final List<RCFGNode> outgoingNodes = target.getOutgoingNodes();

		if (!incomingNodes.isEmpty() && !outgoingNodes.isEmpty() && !checkTargetNode(target)
				&& !checkAllNodes(incomingNodes, outgoingNodes)) {
			// the nodes do not fulfill the conditions, return
			return target.getOutgoingNodes();
		}

		if (!checkEdgePairs(target.getIncomingEdges(), target.getOutgoingEdges())) {
			// the edges do not fulfill the conditions, return
			return target.getOutgoingNodes();
		}

		// we will not change the acceptance conditions, so we can start
		// with creating new edges
		// for each ei from EI, for each ej from EO
		// we add a new edge (qi,sti;stj,qj)

		if (mLogger.isDebugEnabled()) {
			mLogger.debug("    will try to remove " + target.getLocationName());
		}

		final List<RCFGEdge> predEdges = new ArrayList<RCFGEdge>(target.getIncomingEdges());
		final List<RCFGEdge> succEdges = new ArrayList<RCFGEdge>(target.getOutgoingEdges());

		// collect information for new edges beforehand (because
		// SequentialComposition disconnects the edges and we wont get their
		// source/target information afterwards)
		final List<EdgeConstructionInfo> infos = new ArrayList<>();
		final StatementExtractor extractor = new StatementExtractor(mLogger);

		final Iterator<RCFGEdge> predIter = predEdges.iterator();
		boolean canRemoveSuccEdges = true;
		boolean canRemovePredEdges = true;
		while (predIter.hasNext()) {
			final RCFGEdge predEdge = predIter.next();

			final CodeBlock predCB = (CodeBlock) predEdge;
			if (predCB.getTransitionFormula().isInfeasible() == Infeasibility.INFEASIBLE) {
				if (mLogger.isDebugEnabled()) {
					mLogger.debug("    already infeasible: " + predCB);
				}
				continue;
			}
			final List<Statement> first = extractor.process(predCB);
			if (extractor.hasSummary()) {
				// we cannot remove or use this edge, it is a summary
				predIter.remove();
				canRemoveSuccEdges = false;
				if (mLogger.isDebugEnabled()) {
					mLogger.debug("    skipping because it contains summaries: " + predCB);
				}
				continue;
			}

			// during processing of successor edges, we decide if we are allowed
			// to remove the predecessor edges
			canRemovePredEdges = processSuccessorEdges(succEdges, extractor, infos, predEdge, first);
		}

		int removedEdges = 0;
		if (canRemoveSuccEdges) {
			// if one of the successor edges is a summary edge, we are not
			// allowed to remove the predecessor edges
			removedEdges += disconnectEdges(predEdges);
		}
		if (canRemovePredEdges) {
			// if one of the predecessor edges is a summary edge, we are not
			// allowed to remove the successor edges
			removedEdges += disconnectEdges(succEdges);
		}

		final Set<RCFGNode> rtr = new HashSet<>();
		
		//add new edges 
		for (final EdgeConstructionInfo info : infos) {
			final StatementSequence ss = mCbf.constructStatementSequence(info.getSource(), info.getTarget(),
					info.getStatements(), Origin.IMPLEMENTATION);
			generateTransFormula(root, ss);
			// we changed the edges of the predecessor, we have to re-check
			// them. We therefore need to remove them from the closed set.
			rtr.add(ss.getSource());
			closed.remove(ss.getSource());
		}

		if (!canRemoveSuccEdges) {
			// if we did not remove all pred edges, we have to add all possible
			// successors of the node we wanted to remove
			rtr.addAll(target.getOutgoingNodes());
			if (mLogger.isDebugEnabled()) {
				mLogger.debug("    could not remove " + target.getLocationName()
						+ ", because some incoming edges are left");
			}
		}

		if (mLogger.isDebugEnabled()) {
			mLogger.debug("    removed " + removedEdges + ", added " + rtr.size() + " edges");
		}

		mRemovedEdges += removedEdges;
		return rtr;
	}

	private boolean processSuccessorEdges(Collection<RCFGEdge> succEdges, StatementExtractor extractor,
			Collection<EdgeConstructionInfo> infos, BaseMultigraphEdge<RCFGNode, RCFGEdge> predEdge,
			List<Statement> first) {
		final Iterator<RCFGEdge> succIter = succEdges.iterator();
		boolean canRemovePredEdges = true;
		while (succIter.hasNext()) {
			final RCFGEdge succEdge = succIter.next();
			final CodeBlock succCB = (CodeBlock) succEdge;

			if (succCB.getTransitionFormula().isInfeasible() == Infeasibility.INFEASIBLE) {
				if (mLogger.isDebugEnabled()) {
					mLogger.debug("    already infeasible: " + succCB);
				}
				continue;
			}

			final List<Statement> second = extractor.process(succCB);
			if (extractor.hasSummary()) {
				// we cannot remove or use this edge, it is a summary
				succIter.remove();
				// if the successor edges contain a summary, we cannot remove
				// any predecessor edge
				canRemovePredEdges = false;
				if (mLogger.isDebugEnabled()) {
					mLogger.debug("    skipping because it contains summaries: " + succCB);
				}
				continue;
			}

			// we will construct a new edge going from the source of the
			// predecessor edge to the target of the successor edge and being
			// labeled with the statements of the first edge followed by the
			// statements of the second edge.
			infos.add(new EdgeConstructionInfo((ProgramPoint) predEdge.getSource(),
					(ProgramPoint) succEdge.getTarget(), first, second));

		}
		return canRemovePredEdges;
	}

	private int disconnectEdges(Collection<RCFGEdge> edges) {
		int removedEdges = 0;
		for (final RCFGEdge succEdge : edges) {
			succEdge.disconnectSource();
			succEdge.disconnectTarget();
			removedEdges++;
		}
		return removedEdges;
	}

	private static final class EdgeConstructionInfo {
		private final ProgramPoint mSource;
		private final ProgramPoint mTarget;
		private final List<Statement> mFirst;
		private final List<Statement> mSecond;

		private EdgeConstructionInfo(ProgramPoint source, ProgramPoint target, List<Statement> first,
				List<Statement> second) {
			mSource = source;
			mTarget = target;
			mFirst = first;
			mSecond = second;
		}

		private ProgramPoint getSource() {
			return mSource;
		}

		private ProgramPoint getTarget() {
			return mTarget;
		}

		private List<Statement> getStatements() {
			final List<Statement> rtr = new ArrayList<>();
			rtr.addAll(mFirst);
			rtr.addAll(mSecond);
			return rtr;
		}
	}
}
