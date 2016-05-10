/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE ReachingDefinitions plug-in.
 * 
 * The ULTIMATE ReachingDefinitions plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE ReachingDefinitions plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE ReachingDefinitions plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE ReachingDefinitions plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE ReachingDefinitions plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.analysis.reachingdefinitions.dataflowdag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.access.BaseObserver;
import de.uni_freiburg.informatik.ultimate.boogie.ast.AssumeStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.models.IElement;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.reachingdefinitions.annotations.IAnnotationProvider;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.reachingdefinitions.annotations.IndexedStatement;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.reachingdefinitions.annotations.ReachDefEdgeAnnotation;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.reachingdefinitions.annotations.ReachDefStatementAnnotation;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.reachingdefinitions.boogie.ScopedBoogieVar;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RCFGEdge;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootNode;

public class DataflowDAGGenerator extends BaseObserver {

	private final Logger mLogger;
	private final IAnnotationProvider<ReachDefStatementAnnotation> mStatementProvider;
//	private final IAnnotationProvider<ReachDefEdgeAnnotation> mEdgeProvider;
	private final LinkedHashMap<RCFGEdge, List<AssumeStatement>> mEdgesWithAssumes;
	private List<DataflowDAG<Statement>> mForest;

	public DataflowDAGGenerator(Logger logger, IAnnotationProvider<ReachDefStatementAnnotation> stmtProvider,
			IAnnotationProvider<ReachDefEdgeAnnotation> edgeProvider,
			LinkedHashMap<RCFGEdge, List<AssumeStatement>> edgesWithAssumes) {
		mLogger = logger;
		mStatementProvider = stmtProvider;
//		mEdgeProvider = edgeProvider;
		mEdgesWithAssumes = edgesWithAssumes;
	}

	@Override
	public boolean process(IElement root) throws Throwable {
		if (mEdgesWithAssumes == null || mEdgesWithAssumes.size() == 0) {
			return false;
		}

		if (root instanceof RootNode) {
			RootNode rootNode = (RootNode) root;
			mForest = process(rootNode);

			if (mLogger.isDebugEnabled()) {
				mLogger.debug("DataflowDAGGenerator results:");
				mLogger.debug("#" + mForest.size() + " trees generated");
				printDebugForest();
			}
		}
		return false;
	}

	public List<DataflowDAG<Statement>> getDAGs() {
		return mForest;
	}

	private List<DataflowDAG<Statement>> process(RootNode node) {
		List<DataflowDAG<Statement>> forest = new ArrayList<DataflowDAG<Statement>>();
		for (RCFGEdge edge : mEdgesWithAssumes.keySet()) {
			for (AssumeStatement assm : mEdgesWithAssumes.get(edge)) {
				forest.add(buildDAG(edge, assm));
			}
		}
		return forest;
	}

	private DataflowDAG<Statement> buildDAG(RCFGEdge edge, AssumeStatement assm) {
		LinkedList<DataflowDAG<Statement>> store = new LinkedList<>();

		DataflowDAG<Statement> current = new DataflowDAG<Statement>(assm);
		DataflowDAG<Statement> root = current;
		store.add(current);

		while (!store.isEmpty()) {
			current = store.removeFirst();
			Set<Entry<ScopedBoogieVar, HashSet<IndexedStatement>>> uses = getUse(current);
			for (Entry<ScopedBoogieVar, HashSet<IndexedStatement>> use : uses) {
				for (IndexedStatement stmt : use.getValue()) {
					DataflowDAG<Statement> next = new DataflowDAG<Statement>(stmt.getStatement());
					current.addOutgoingNode(next, use.getKey());
					store.addFirst(next); // use last for BFS
				}
			}
		}
		return root;

	}

	private Set<Entry<ScopedBoogieVar, HashSet<IndexedStatement>>> getUse(DataflowDAG<Statement> current) {
		ReachDefStatementAnnotation annot = mStatementProvider.getAnnotation(current.getNodeLabel());
		assert annot != null;
		HashMap<ScopedBoogieVar, HashSet<IndexedStatement>> use = annot.getUse();
		assert use != null;
		return use.entrySet();
	}

	private void printDebugForest() {
		if (mForest == null) {
			return;
		}

		for (DataflowDAG<Statement> dag : mForest) {
			dag.printGraphDebug(mLogger);
		}
	}

}
