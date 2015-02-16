package de.uni_freiburg.informatik.ultimate.buchiprogramproduct.optimizercfg;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.access.BaseObserver;
import de.uni_freiburg.informatik.ultimate.buchiprogramproduct.Activator;
import de.uni_freiburg.informatik.ultimate.buchiprogramproduct.ProductBacktranslator;
import de.uni_freiburg.informatik.ultimate.buchiprogramproduct.preferences.PreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceStore;
import de.uni_freiburg.informatik.ultimate.core.services.IStorable;
import de.uni_freiburg.informatik.ultimate.core.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.model.IElement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssumeStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.model.boogie.output.BoogiePrettyPrinter;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.normalforms.BoogieConditionWrapper;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.normalforms.ConditionTransformer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlockFactory;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ProgramPoint;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RCFGEdge;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.StatementSequence;

/**
 * Observer that performs small block encoding of a single statement RCFG.
 * 
 * The small block encoding works like this:
 * <ul>
 * <li>For each edge e := (loc,assume expr,loc') in RCFG
 * <li>Convert expr to DNF with disjuncts d1..dn
 * <li>If n>1 then for each disjunct di insert new edge (loc,assume di,loc')
 * <li>If n>1 then remove e
 * </ul>
 * 
 * @author dietsch@informatik.uni-freiburg.de
 * 
 */
public class SmallBlockEncoder extends BaseObserver {

	private final Logger mLogger;
	private final ProductBacktranslator mBacktranslator;
	private final boolean mRewriteAssumes;
	private final CodeBlockFactory mCbf;

	public SmallBlockEncoder(Logger logger, ProductBacktranslator backtranslator, IToolchainStorage mStorage) {
		mLogger = logger;
		mCbf = (CodeBlockFactory) mStorage.getStorable(CodeBlockFactory.s_CodeBlockFactoryKeyInToolchainStorage);
		mBacktranslator = backtranslator;
		mRewriteAssumes = new UltimatePreferenceStore(Activator.PLUGIN_ID)
				.getBoolean(PreferenceInitializer.OPTIMIZE_SBE_REWRITENOTEQUALS);
	}

	@Override
	public boolean process(IElement elem) throws Throwable {
		if (elem instanceof RootNode) {
			RootNode root = (RootNode) elem;

			int countDisjunctiveAssumes = 0;
			int countNewEdges = 0;

			ArrayDeque<RCFGEdge> edges = new ArrayDeque<>();
			HashSet<RCFGEdge> closed = new HashSet<>();

			ConditionTransformer<Expression> ct = new ConditionTransformer<>(new BoogieConditionWrapper());

			edges.addAll(root.getOutgoingEdges());

			while (!edges.isEmpty()) {
				RCFGEdge current = edges.removeFirst();
				if (closed.contains(current)) {
					continue;
				}
				closed.add(current);
				edges.addAll(current.getTarget().getOutgoingEdges());
				if (mLogger.isDebugEnabled()) {
					mLogger.debug("Processing edge " + current.hashCode() + ":");
					mLogger.debug("    " + current);
				}

				if (current instanceof StatementSequence) {
					StatementSequence ss = (StatementSequence) current;
					if (ss.getStatements().size() != 1) {
						throw new UnsupportedOperationException("StatementSequence has " + ss.getStatements().size()
								+ " statements, but SingleStatement should enforce that there is only 1.");
					}
					Statement stmt = ss.getStatements().get(0);
					if (stmt instanceof AssumeStatement) {
						AssumeStatement assume = (AssumeStatement) stmt;
						Expression expr = assume.getFormula();
						if (mRewriteAssumes) {
							expr = ct.rewriteNotEquals(expr);
						}

						if (mLogger.isDebugEnabled()) {
							mLogger.debug("    has assume " + BoogiePrettyPrinter.print(assume.getFormula()));
							if (mRewriteAssumes) {
								mLogger.debug("    after rewrite " + BoogiePrettyPrinter.print(expr));
							}
						}
						Collection<Expression> disjuncts = ct.toDnfDisjuncts(expr);
						if (mLogger.isDebugEnabled()) {
							if (disjuncts.size() > 1) {
								StringBuilder sb = new StringBuilder();
								sb.append("{");
								for (Expression dis : disjuncts) {
									sb.append(BoogiePrettyPrinter.print(dis)).append(", ");
								}
								sb.delete(sb.length() - 2, sb.length()).append("}");
								mLogger.debug("    converted to disjuncts " + sb.toString());
							} else {
								mLogger.debug("    only one disjunct "
										+ BoogiePrettyPrinter.print(disjuncts.iterator().next()));
							}
						}
						if (disjuncts.size() > 1) {
							countDisjunctiveAssumes++;
							for (Expression disjunct : disjuncts) {
								StatementSequence newss = mCbf.constructStatementSequence((ProgramPoint) current.getSource(),
										(ProgramPoint) current.getTarget(), new AssumeStatement(assume.getLocation(),
												disjunct));
								closed.add(newss);
								countNewEdges++;
								mBacktranslator.mapEdges(newss, current);
							}
							current.disconnectSource();
							current.disconnectTarget();
						}
					}
				}
			}
			mLogger.info("Small block encoding converted " + countDisjunctiveAssumes + " assume edges to "
					+ countNewEdges + " new edges with only one disjunct");
			return false;
		}
		return true;
	}
}
