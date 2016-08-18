/*
 * Copyright (C) 2016 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2016 University of Freiburg
 *
 * This file is part of the ULTIMATE CACSL2BoogieTranslator plug-in.
 *
 * The ULTIMATE CACSL2BoogieTranslator plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE CACSL2BoogieTranslator plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE CACSL2BoogieTranslator plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE CACSL2BoogieTranslator plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE CACSL2BoogieTranslator plug-in grant you additional permission
 * to convey the resulting work.
 */

package de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.witness;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.cdt.core.dom.ast.ASTGenericVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.Activator;
import de.uni_freiburg.informatik.ultimate.witnessparser.graph.WitnessEdge;
import de.uni_freiburg.informatik.ultimate.witnessparser.graph.WitnessEdgeAnnotation;
import de.uni_freiburg.informatik.ultimate.witnessparser.graph.WitnessNode;
import de.uni_freiburg.informatik.ultimate.witnessparser.graph.WitnessNodeAnnotation;
import de.uni_freiburg.informatik.ultimate.witnessparser.preferences.WitnessParserPreferences;

/**
 *
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 */
public class CorrectnessWitnessExtractor {

	private final IUltimateServiceProvider mServices;
	private final ILogger mLogger;
	private final boolean mCheckOnlyLoopInvariants;

	private WitnessNode mWitnessNode;
	private IASTTranslationUnit mTranslationUnit;
	private Map<IASTNode, ExtractedWitnessInvariant> mAST2Invariant;

	public CorrectnessWitnessExtractor(final IUltimateServiceProvider service) {
		mServices = service;
		mLogger = mServices.getLoggingService().getLogger(Activator.PLUGIN_ID);
		mCheckOnlyLoopInvariants = WitnessParserPreferences.getPrefs(service)
				.getBoolean(WitnessParserPreferences.LABEL_CW_USE_ONLY_LOOPINVARIANTS);
	}

	public void setWitness(final WitnessNode wnode) {
		mWitnessNode = wnode;
	}

	public void setAST(final IASTTranslationUnit inputTU) {
		mTranslationUnit = inputTU;
	}

	public boolean isReady() {
		return mWitnessNode != null && mTranslationUnit != null;
	}

	public void clear() {
		mWitnessNode = null;
		mTranslationUnit = null;
	}

	/**
	 * @return a map from {@link IASTNode} to an {@link ExtractedWitnessInvariant}. The
	 *         {@link ExtractedWitnessInvariant} represents a witness invariant which has to hold before, at, or after
	 *         the {@link IASTNode}.
	 */
	public Map<IASTNode, ExtractedWitnessInvariant> getCorrectnessWitnessInvariants() {
		if (mAST2Invariant == null) {
			mAST2Invariant = extract();
		}
		return mAST2Invariant;
	}

	private Map<IASTNode, ExtractedWitnessInvariant> extract() {
		if (!isReady()) {
			mLogger.warn("Cannot extract witness if there is no witness");
			return null;
		}
		if (mCheckOnlyLoopInvariants) {
			mLogger.info("Only extracting loop invariants from correctness witness");
		} else {
			mLogger.info("Extracting all invariants from correctness witness");
		}

		Map<IASTNode, ExtractedWitnessInvariant> rtr = new HashMap<>();

		final ExtractionStatistics stats = new ExtractionStatistics();
		final Deque<WitnessNode> worklist = new ArrayDeque<>();
		final Set<WitnessNode> closed = new HashSet<>();
		worklist.add(mWitnessNode);
		while (!worklist.isEmpty()) {
			final WitnessNode current = worklist.remove();
			if (!closed.add(current)) {
				continue;
			}
			worklist.addAll(current.getOutgoingNodes());
			final Map<IASTNode, ExtractedWitnessInvariant> match = matchWitnessToAstNode(current, stats);
			rtr = mergeMatchesIfNecessary(rtr, match);
		}

		mLogger.info("Processed " + closed.size() + " nodes");
		mLogger.info("WitnessExtracted=" + stats.getSuccess());
		mLogger.info("WitnessTotal=" + stats.getTotal());
		if (stats.getFailure() > 0) {
			mLogger.info("Could not extract " + stats.getFailure() + " invariants");
		} else {
			mLogger.info("Extracted all invariants");
		}
		printResults(rtr);

		return rtr;
	}

	private void printResults(final Map<IASTNode, ExtractedWitnessInvariant> result) {
		if (result.isEmpty()) {
			mLogger.info("Witness did not contain any usable invariants.");
			return;
		}
		mLogger.info("Found the following invariants in the witness:");
		for (final Entry<IASTNode, ExtractedWitnessInvariant> entry : result.entrySet()) {
			assert entry.getKey() == entry.getValue().getRelatedAstNode();
			mLogger.info(entry.getValue().toStringWithWitnessNodeLabel());
		}
	}

	private Map<IASTNode, ExtractedWitnessInvariant> matchWitnessToAstNode(final WitnessNode current,
			final ExtractionStatistics stats) {
		final WitnessNodeAnnotation annot = WitnessNodeAnnotation.getAnnotation(current);
		if (annot == null) {
			return Collections.emptyMap();
		}
		final String invariant = annot.getInvariant();
		if (invariant == null || invariant.equalsIgnoreCase("true")) {
			return Collections.emptyMap();
		}
		final Map<IASTNode, ExtractedWitnessInvariant> ast2invariant = matchWitnessToAstNode(current);
		if (ast2invariant == null || ast2invariant.isEmpty()) {
			mLogger.error("Could not match witness node to any AST node: " + current);
			stats.fail();
			return Collections.emptyMap();
		}
		stats.success();
		return ast2invariant;
	}

	private Map<IASTNode, ExtractedWitnessInvariant> matchWitnessToAstNode(final WitnessNode wnode) {
		final Set<DecoratedWitnessEdge> edges = new HashSet<>();
		edges.addAll(convertAndFilterEdges(wnode.getIncomingEdges(), true));
		edges.addAll(convertAndFilterEdges(wnode.getOutgoingEdges(), false));

		if (edges.isEmpty()) {
			mLogger.error("No line numbers found for " + wnode);
			return null;
		}
		final DecoratedWitnessNode dwnode = new DecoratedWitnessNode(wnode);
		final Set<MatchedASTNode> candidateNodes = matchLines(edges);

		boolean printlabel = false;
		final Set<DecoratedWitnessEdge> incomingEdges = getIncomingSet(edges);
		if (candidateNodes.stream().allMatch(a -> !a.isIncoming()) && !incomingEdges.isEmpty()) {
			mLogger.warn(
					"Could not match AST node to invariant before witness lines " + toStringCollection(incomingEdges));
			printlabel = true;
		}

		final Set<DecoratedWitnessEdge> outgoingEdges = getOutgoingSet(edges);
		if (candidateNodes.stream().allMatch(a -> a.isIncoming()) && !outgoingEdges.isEmpty()) {
			mLogger.warn(
					"Could not match AST node to invariant after witness lines " + toStringCollection(outgoingEdges));
			printlabel = true;
		}
		if (printlabel) {
			mLogger.warn("  Witness node label is " + dwnode);
		}
		final Map<IASTNode, ExtractedWitnessInvariant> possibleMatches = extractInvariants(dwnode, candidateNodes);
		return possibleMatches;
	}

	private Map<IASTNode, ExtractedWitnessInvariant> extractInvariants(final DecoratedWitnessNode dwnode,
			final Set<MatchedASTNode> candidateNodes) {

		// filter matches s.t. edges that cross procedure boundaries are eliminated
		filterScopeChanges(dwnode, candidateNodes);

		Map<IASTNode, ExtractedWitnessInvariant> rtr = new HashMap<>();
		rtr = mergeMatchesIfNecessary(rtr, extractLoopInvariants(dwnode, candidateNodes));

		if (mCheckOnlyLoopInvariants) {
			// we already extracted all the loop invariants
			mLogger.warn("The following possible matches for " + dwnode
					+ " were ignored because we could not match them to a loop:");
			candidateNodes.stream().forEach(a -> mLogger.warn("  " + a.toStringSimple()));
			return rtr;
		}
		rtr = mergeMatchesIfNecessary(rtr, extractStatementInvariants(dwnode, candidateNodes));
		return rtr;
	}

	private Map<IASTNode, ExtractedWitnessInvariant> extractLoopInvariants(final DecoratedWitnessNode dwnode,
			final Set<MatchedASTNode> candidateNodes) {
		final Set<MatchedASTNode> loopHeads =
				candidateNodes.stream().filter(a -> a.isLoopHead()).collect(Collectors.toSet());
		candidateNodes.removeAll(loopHeads);

		final Map<IASTNode, ExtractedWitnessInvariant> down = tryMatchLoopInvariantsDownwards(dwnode, loopHeads);
		if (loopHeads.isEmpty()) {
			// downward matching matched everything, nothing more to do.
			return down;
		}
		// try to match the remaining loop heads upwards
		final Map<IASTNode, ExtractedWitnessInvariant> up = tryMatchLoopInvariantsUpwards(dwnode, loopHeads);
		if (!loopHeads.isEmpty()) {
			mLogger.warn("Could not match the following loop heads:");
			loopHeads.stream().forEach(a -> mLogger.warn("  " + a.toStringSimple()));
			candidateNodes.addAll(loopHeads);
		}
		return mergeMatchesIfNecessary(down, up);
	}

	private Map<IASTNode, ExtractedWitnessInvariant> tryMatchLoopInvariantsDownwards(final DecoratedWitnessNode dwnode,
			final Set<MatchedASTNode> loopHeads) {
		final Map<IASTNode, ExtractedWitnessInvariant> rtr = new HashMap<>();
		final Iterator<MatchedASTNode> iter = loopHeads.iterator();
		while (iter.hasNext()) {
			final MatchedASTNode loopHead = iter.next();
			final Set<IASTStatement> loopStatements = new LoopStatementExtractor().run(loopHead.getNode());
			if (loopStatements.isEmpty()) {
				continue;
			} else {
				mLogger.info("Matched downward: " + loopHead.toStringSimple());
				loopStatements.stream()
						.map(a -> new ExtractedWitnessInvariant(dwnode.getInvariant(),
								Collections.singletonList(dwnode.getName()), a, false, false, true))
						.forEach(a -> rtr.put(a.getRelatedAstNode(), a));
				iter.remove();
			}
		}
		return rtr;
	}

	private Map<IASTNode, ExtractedWitnessInvariant> tryMatchLoopInvariantsUpwards(final DecoratedWitnessNode dwnode,
			final Set<MatchedASTNode> loopHeads) {
		final Map<IASTNode, ExtractedWitnessInvariant> rtr = new HashMap<>();
		final IASTNode commonParent = findCommonParentStatement(loopHeads);
		if (commonParent == null) {
			// if there is no common parent, we cannot match upwards
			return rtr;
		}
		mLogger.info("Matched remaining loop heads upward to common parent of type "
				+ commonParent.getClass().getSimpleName());

		// check if the common parent or a parent of the common parent is a loop
		IASTNode currentParent = commonParent;
		Set<IASTStatement> loopStatements = Collections.emptySet();
		while (currentParent != null && currentParent instanceof IASTStatement) {
			loopStatements = new LoopStatementExtractor().run(currentParent);
			if (!loopStatements.isEmpty()) {
				break;
			}
			currentParent = currentParent.getParent();
		}
		if (loopStatements.isEmpty()) {
			return rtr;
		}
		loopStatements.stream()
				.map(a -> new ExtractedWitnessInvariant(dwnode.getInvariant(),
						Collections.singletonList(dwnode.getName()), a, false, false, true))
				.forEach(a -> rtr.put(a.getRelatedAstNode(), a));
		loopHeads.clear();
		return rtr;
	}

	private Map<IASTNode, ExtractedWitnessInvariant> extractStatementInvariants(final DecoratedWitnessNode dwnode,
			final Set<MatchedASTNode> candidateNodes) {
		final Map<IASTNode, ExtractedWitnessInvariant> rtr = new HashMap<>();
		candidateNodes.stream()
				.map(a -> new ExtractedWitnessInvariant(dwnode.getInvariant(),
						Collections.singletonList(dwnode.getName()), a.getNode(), !a.isIncoming(), a.isIncoming(),
						false))
				.forEach(a -> rtr.put(a.getRelatedAstNode(), a));
		candidateNodes.clear();
		return rtr;
	}

	private Map<IASTNode, ExtractedWitnessInvariant> mergeMatchesIfNecessary(
			final Map<IASTNode, ExtractedWitnessInvariant> mapA, final Map<IASTNode, ExtractedWitnessInvariant> mapB) {
		if (mapA == null || mapA.isEmpty()) {
			return mapB;
		}
		if (mapB == null || mapB.isEmpty()) {
			return mapA;
		}

		final Map<IASTNode, ExtractedWitnessInvariant> rtr = new HashMap<>(mapA.size());
		for (final Entry<IASTNode, ExtractedWitnessInvariant> entryB : mapB.entrySet()) {
			final ExtractedWitnessInvariant aWitnessInvariant = mapA.get(entryB.getKey());
			if (aWitnessInvariant == null) {
				rtr.put(entryB.getKey(), entryB.getValue());
			} else {
				rtr.put(entryB.getKey(), mergeAndWarn(aWitnessInvariant, entryB.getValue()));
			}
		}
		for (final Entry<IASTNode, ExtractedWitnessInvariant> entryA : mapA.entrySet()) {
			final ExtractedWitnessInvariant aWitnessInvariant = mapB.get(entryA.getKey());
			if (aWitnessInvariant == null) {
				rtr.put(entryA.getKey(), entryA.getValue());
			} else {
				// we already merged in the earlier pass
			}
		}
		return rtr;
	}

	private ExtractedWitnessInvariant mergeAndWarn(final ExtractedWitnessInvariant invA,
			final ExtractedWitnessInvariant invB) {
		final ExtractedWitnessInvariant newInvariant = invA.merge(invB);
		mLogger.warn("Merging invariants");
		mLogger.warn("  Invariant A is " + invA.toString());
		mLogger.warn("  Invariant B is " + invB.toString());
		mLogger.warn("  New match: " + newInvariant.toString());
		return newInvariant;
	}

	private Set<MatchedASTNode> matchLines(final Set<DecoratedWitnessEdge> edges) {
		final Set<MatchedASTNode> rtr = new HashSet<>();
		for (final DecoratedWitnessEdge edge : edges) {
			final LineMatchingVisitor matcher = new LineMatchingVisitor(edge);
			matcher.run(mTranslationUnit);
			rtr.addAll(matcher.getMatchedNodes());
		}
		// removeSubtreeMatches(rtr);
		return rtr;
	}

	/**
	 * Remove all witness edges that have no linenumber and convert the remaining to {@link DecoratedWitnessEdge}s.
	 */
	private Set<DecoratedWitnessEdge> convertAndFilterEdges(final List<WitnessEdge> edges, final boolean isIncoming) {
		return edges.stream().map(a -> new DecoratedWitnessEdge(a, isIncoming)).filter(a -> !a.hasNoLineNumber())
				.collect(Collectors.toSet());
	}

	/**
	 * Remove all {@link IASTNode}s from the <code>before</code> list if there is a node in the <code>after</code> list
	 * that has a different scope, except if the "before" node is in the global scope.
	 */
	private void filterScopeChanges(final DecoratedWitnessNode wnode, final Collection<MatchedASTNode> nodes) {

		final Set<MatchedASTNode> after = getOutgoingSet(nodes);
		final Set<MatchedASTNode> before = getIncomingSet(nodes);

		// collect all scopes from the after list
		final Set<IASTDeclaration> afterScopes =
				after.stream().map(a -> determineScope(a.getNode())).filter(a -> a != null).collect(Collectors.toSet());

		// iterate over before list and remove all matches that would lead to a scope change
		final Iterator<MatchedASTNode> beforeIter = before.iterator();
		while (beforeIter.hasNext()) {
			final MatchedASTNode beforeCurrent = beforeIter.next();
			final IASTDeclaration beforeScope = determineScope(beforeCurrent.getNode());
			if (beforeScope == null) {
				// its the global scope
				continue;
			}
			final Optional<IASTDeclaration> scopeChange = afterScopes.stream().filter(a -> a != beforeScope).findAny();
			if (scopeChange.isPresent()) {
				mLogger.warn("Removing invariant match " + wnode + ": " + wnode.getInvariant()
						+ " because scopes differ: " + toLogString(beforeScope, scopeChange.get()));
				beforeIter.remove();
			}
		}
	}

	private IASTDeclaration determineScope(final IASTNode current) {
		IASTNode parent = current.getParent();
		while (parent != null) {
			if (parent instanceof IASTFunctionDefinition) {
				return (IASTDeclaration) parent;
			}
			if (parent instanceof IASTTranslationUnit) {
				return null;
			}
			parent = parent.getParent();
		}

		return null;
	}

	private IASTNode findCommonParentStatement(final Collection<MatchedASTNode> list) {
		if (list.isEmpty()) {
			throw new IllegalArgumentException("Empty collection cannot have a parent");
		}
		if (list.size() == 1) {
			// singleton is always its parent
			return list.iterator().next().getNode();
		}

		// check if any element is already the parent of the others
		final Iterator<MatchedASTNode> iter = list.iterator();
		while (iter.hasNext()) {
			final MatchedASTNode currentParent = iter.next();
			if (list.stream()
					.allMatch(a -> a == currentParent || isContainedInSubtree(a.getNode(), currentParent.getNode()))) {
				return currentParent.getNode();
			}
		}

		// if this is not the case, we know that one node in the path from the first element to the root has to be the
		// parent, or there is no common parent

		IASTNode possibleParent = list.iterator().next().getNode().getParent();
		while (possibleParent != null) {
			if (!(possibleParent instanceof IASTStatement)) {
				return null;
			}
			final IASTNode pParent = possibleParent;
			if (list.stream().allMatch(a -> a.getNode() == pParent || isContainedInSubtree(a.getNode(), pParent))) {
				return pParent;
			}
			possibleParent = possibleParent.getParent();
		}
		return null;
	}

	private boolean isContainedInSubtree(final IASTNode candidate, final IASTNode possibleParent) {
		final SubtreeChecker sc = new SubtreeChecker(candidate);
		possibleParent.accept(sc);
		return sc.isContainedInSubtree();
	}

	private <T extends IHasIncoming> Stream<T> getIncomingStream(final Collection<T> edges) {
		return edges.stream().filter(a -> a.isIncoming());
	}

	private <T extends IHasIncoming> Set<T> getIncomingSet(final Collection<T> edges) {
		return getIncomingStream(edges).collect(Collectors.toSet());
	}

	private <T extends IHasIncoming> Stream<T> getOutgoingStream(final Collection<T> edges) {
		return edges.stream().filter(a -> !a.isIncoming());
	}

	private <T extends IHasIncoming> Set<T> getOutgoingSet(final Collection<T> edges) {
		return getOutgoingStream(edges).collect(Collectors.toSet());
	}

	private String toLogString(final IASTNode bScope, final IASTNode aScope) {
		final String bScopeId = bScope == null ? "Global" : ("L" + bScope.getFileLocation().getStartingLineNumber());
		final String aScopeId = aScope == null ? "Global" : ("L" + aScope.getFileLocation().getStartingLineNumber());
		return "B=" + bScopeId + ", A=" + aScopeId;
	}

	private String toStringCollection(final Collection<?> stream) {
		return toStringCollection(stream.stream());
	}

	private String toStringCollection(final Stream<?> stream) {
		return String.join(", ", stream.map(a -> String.valueOf(a)).collect(Collectors.toList()));
	}

	private static final class LoopStatementExtractor extends ASTGenericVisitor {
		private Set<IASTStatement> mStatements;

		public LoopStatementExtractor() {
			super(true);
		}

		public Set<IASTStatement> run(final IASTNode subtreeRoot) {
			mStatements = new HashSet<>();
			subtreeRoot.accept(this);
			return mStatements;
		}

		@Override
		public int visit(final IASTStatement statement) {
			if (statement instanceof IASTGotoStatement || statement instanceof IASTDoStatement
					|| statement instanceof IASTWhileStatement || statement instanceof IASTForStatement) {
				mStatements.add(statement);
				return PROCESS_SKIP;
			}
			return PROCESS_CONTINUE;
		}
	}

	private static final class LineMatchingVisitor extends ASTGenericVisitor {

		private final DecoratedWitnessEdge mEdge;
		private final Set<MatchedASTNode> mMatchedNodes;

		public LineMatchingVisitor(final DecoratedWitnessEdge edge) {
			super(true);
			mEdge = edge;
			mMatchedNodes = new HashSet<>();
		}

		public void run(final IASTTranslationUnit translationUnit) {
			translationUnit.accept(this);
		}

		private Set<MatchedASTNode> getMatchedNodes() {
			return mMatchedNodes;
		}

		@Override
		protected int genericVisit(final IASTNode node) {
			if (match(node)) {
				// skip the subtree if a match occurred, but continue with siblings.
				return PROCESS_SKIP;
			}
			return PROCESS_CONTINUE;
		}

		private boolean match(final IASTNode node) {
			final IASTFileLocation loc = node.getFileLocation();
			if (loc == null) {
				return false;
			}

			if ((mEdge.getLineNumber() == loc.getEndingLineNumber() && mEdge.isIncoming())
					|| (mEdge.getLineNumber() == loc.getStartingLineNumber() && !mEdge.isIncoming())) {
				mMatchedNodes.add(new MatchedASTNode(node, mEdge));
				return true;
			}
			return false;
		}
	}

	private static final class SubtreeChecker extends ASTGenericVisitor {

		private final IASTNode mCandidate;
		private boolean mIsContainedInSubtree;

		public SubtreeChecker(final IASTNode candidate) {
			super(true);
			mCandidate = candidate;
			mIsContainedInSubtree = false;
		}

		public boolean isContainedInSubtree() {
			return mIsContainedInSubtree;
		}

		@Override
		protected int genericVisit(final IASTNode child) {
			if (mIsContainedInSubtree) {
				return PROCESS_ABORT;
			}
			if (child == mCandidate) {
				mIsContainedInSubtree = true;
				return PROCESS_ABORT;
			}
			return PROCESS_CONTINUE;
		}
	}

	private static final class ExtractionStatistics {
		private int mSuccess;
		private int mFailure;

		private ExtractionStatistics() {
			mSuccess = 0;
			mFailure = 0;
		}

		private void success() {
			mSuccess++;
		}

		private void fail() {
			mFailure++;
		}

		public int getSuccess() {
			return mSuccess;
		}

		public int getFailure() {
			return mFailure;
		}

		public int getTotal() {
			return mSuccess + mFailure;
		}

		@Override
		public String toString() {
			return "T/S/F " + getTotal() + "/" + getSuccess() + "/" + getFailure();
		}
	}

	private static final class DecoratedWitnessEdge implements IHasIncoming {
		private final WitnessEdge mEdge;
		private final WitnessEdgeAnnotation mAnnotation;
		private final boolean mIsIncoming;

		public DecoratedWitnessEdge(final WitnessEdge edge, final boolean isIncoming) {
			mIsIncoming = isIncoming;
			mEdge = edge;
			mAnnotation = WitnessEdgeAnnotation.getAnnotation(edge);
		}

		public boolean hasNoLineNumber() {
			return getLineNumber() == -1;
		}

		@Override
		public boolean isIncoming() {
			return mIsIncoming;
		}

		public int getLineNumber() {
			if (mIsIncoming) {
				return mEdge.getLocation().getEndLine();
			} else {
				return mEdge.getLocation().getStartLine();
			}
		}

		public boolean isEnteringLoop() {
			return mAnnotation.getEnterLoopHead();
		}

		@Override
		public String toString() {
			return mEdge.toString() + " (inc=" + isIncoming() + ", isEnteringLoop=" + isEnteringLoop() + ")";
		}
	}

	private static final class DecoratedWitnessNode {
		private final WitnessNode mNode;
		private final WitnessNodeAnnotation mAnnotation;

		public DecoratedWitnessNode(final WitnessNode node) {
			mNode = node;
			mAnnotation = WitnessNodeAnnotation.getAnnotation(node);
		}

		public String getName() {
			return mNode.getName();
		}

		public String getInvariant() {
			return mAnnotation.getInvariant();
		}

		@Override
		public String toString() {
			return mNode.toString();
		}
	}

	private static final class MatchedASTNode implements IHasIncoming {
		private final IASTNode mNode;
		private final DecoratedWitnessEdge mEdge;

		private MatchedASTNode(final IASTNode node, final DecoratedWitnessEdge edge) {
			mNode = node;
			mEdge = edge;
		}

		private IASTNode getNode() {
			return mNode;
		}

		@Override
		public boolean isIncoming() {
			return mEdge.isIncoming();
		}

		public boolean isLoopHead() {
			return mEdge.isEnteringLoop() && isIncoming();
		}

		@Override
		public String toString() {
			return toStringSimple() + " " + mNode.getRawSignature();
		}

		public String toStringSimple() {
			return getLinenumberString() + " " + mEdge;
		}

		private String getLinenumberString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("[L");
			sb.append(mNode.getFileLocation().getStartingLineNumber());
			if (mNode.getFileLocation().getStartingLineNumber() != mNode.getFileLocation().getEndingLineNumber()) {
				sb.append('-');
				sb.append(mNode.getFileLocation().getEndingLineNumber());
			}
			sb.append("]");
			return sb.toString();
		}
	}

	private interface IHasIncoming {
		boolean isIncoming();
	}
}
