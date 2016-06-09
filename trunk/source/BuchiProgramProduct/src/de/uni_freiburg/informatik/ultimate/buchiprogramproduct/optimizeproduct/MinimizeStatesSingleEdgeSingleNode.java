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

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ProgramPoint;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RCFGEdge;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RCFGNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootNode;

/**
 * Least aggressive minimization (besides no attempt). Tries to remove states
 * that have only one incoming and one outgoing edge.
 * 
 * @author dietsch@informatik.uni-freiburg.de
 * 
 */
public class MinimizeStatesSingleEdgeSingleNode extends BaseMinimizeStates {

	public MinimizeStatesSingleEdgeSingleNode(RootNode product, IUltimateServiceProvider services, IToolchainStorage storage) {
		super(product, services, storage);
	}

	@Override
	protected Collection<? extends RCFGNode> processCandidate(RootNode root, ProgramPoint target,
			Set<RCFGNode> closed) {

		if (target.getIncomingEdges().size() != 1 || target.getOutgoingEdges().size() != 1) {
			return target.getOutgoingNodes();
		}

		// this node has exactly one incoming and one outgoing edge,
		// so we have the two edges
		// e1 = (q1,st1,q2)
		// e2 = (q2,st2,q3)
		final RCFGEdge predEdge = target.getIncomingEdges().get(0);
		final RCFGEdge succEdge = target.getOutgoingEdges().get(0);

		final ProgramPoint pred = (ProgramPoint) predEdge.getSource();
		final ProgramPoint succ = (ProgramPoint) succEdge.getTarget();

		if (!checkTargetNode(target) && !checkNodePair(pred, succ)) {
			// the nodes do not fulfill the conditions, return
			return target.getOutgoingNodes();
		}

		if (!checkEdgePair(predEdge, succEdge)) {
			// the edges do not fulfill the conditions, return
			return target.getOutgoingNodes();
		}

		// all conditions are met so we can start with creating new edges
		// we delete e1 and e2 and q2 and add the new edge (q1,st1;st2,q3)

		if (mLogger.isDebugEnabled()) {
			mLogger.debug("    will remove " + target.getPosition());
		}

		predEdge.disconnectSource();
		predEdge.disconnectTarget();
		succEdge.disconnectSource();
		succEdge.disconnectTarget();
		mRemovedEdges += 2;

		mCbf.constructSequentialComposition(pred, succ, false, false, 
				Arrays.asList(new CodeBlock[] { (CodeBlock) predEdge, (CodeBlock) succEdge }));

		if (mLogger.isDebugEnabled()) {
			mLogger.debug("    removed 2, added 2 edges");
		}
		// we added new edges to pred, we have to recheck them now
		return pred.getOutgoingNodes();

	}
}
