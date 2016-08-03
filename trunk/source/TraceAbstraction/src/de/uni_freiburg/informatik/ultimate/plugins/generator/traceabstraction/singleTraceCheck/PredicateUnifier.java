/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2013-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE TraceAbstraction plug-in.
 * 
 * The ULTIMATE TraceAbstraction plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE TraceAbstraction plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE TraceAbstraction plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE TraceAbstraction plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE TraceAbstraction plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.AnnotatedTerm;
import de.uni_freiburg.informatik.ultimate.logic.Annotation;
import de.uni_freiburg.informatik.ultimate.logic.CheckClosedTerm;
import de.uni_freiburg.informatik.ultimate.logic.QuantifiedFormula;
import de.uni_freiburg.informatik.ultimate.logic.QuotedObject;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Boogie2SmtSymbolTable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.IHoareTripleChecker.Validity;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.CommuhashNormalForm;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.ContainsQuantifier;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils.SimplicationTechnique;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils.XnfConversionTechnique;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.BinaryNumericRelation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.BinaryRelation.NoRelationOfThisKindException;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.BinaryRelation.RelationSymbol;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.PrenexNormalForm;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.QuantifierSequence;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.normalForms.Cnf;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicateCoverageChecker;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.PredicateUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.TermVarsProc;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.util.DebugMessage;
import de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.HashRelation;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.NestedMap2;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Triple;
import de.uni_freiburg.informatik.ultimate.util.statistics.AStatisticsType;
import de.uni_freiburg.informatik.ultimate.util.statistics.Benchmark;
import de.uni_freiburg.informatik.ultimate.util.statistics.IStatisticsDataProvider;
import de.uni_freiburg.informatik.ultimate.util.statistics.IStatisticsElement;
import de.uni_freiburg.informatik.ultimate.util.statistics.IStatisticsType;

/**
 * Data structure that stores for each term a unique predicate. 
 * Initially a predicate unifier constructs a "true" predicate and a "false" 
 * predicate.
 * 
 * @author heizmann@informatik.uni-freiburg.de
 * 
 */
public class PredicateUnifier {
	
	private final SmtManager mSmtManager;
	private final Map<Term, IPredicate> mTerm2Predicates;
	private final List<IPredicate> mKnownPredicates = new ArrayList<IPredicate>();
	private final Map<IPredicate, IPredicate> mDeprecatedPredicates = new HashMap<>();
	private final CoverageRelation mCoverageRelation = new CoverageRelation();
	private final ILogger mLogger;
	private final IUltimateServiceProvider mServices;
	private final Script mScript;
	private final Boogie2SmtSymbolTable mSymbolTable;
	private final SimplicationTechnique mSimplificationTechnique;
	private final XnfConversionTechnique mXnfConversionTechnique;
	
	private final IPredicate mTruePredicate;
	private final IPredicate mFalsePredicate;
	
	private final PredicateUnifierStatisticsGenerator mPredicateUnifierBenchmarkGenerator;

	public PredicateUnifier(final IUltimateServiceProvider services, final SmtManager smtManager, 
			final SimplicationTechnique simplificationTechnique, final XnfConversionTechnique xnfConversionTechnique, 
			final IPredicate... initialPredicates) {
		mPredicateUnifierBenchmarkGenerator = new PredicateUnifierStatisticsGenerator();
		mSimplificationTechnique = simplificationTechnique;
		mXnfConversionTechnique = xnfConversionTechnique;
		mSmtManager = smtManager;
		mScript = smtManager.getScript();
		mSymbolTable = smtManager.getBoogie2Smt().getBoogie2SmtSymbolTable();
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(Activator.PLUGIN_ID);
		mTerm2Predicates = new HashMap<Term, IPredicate>();
		final Term trueTerm = mScript.term("true");
		IPredicate truePredicate = null;
		final Term falseTerm = mScript.term("false");
		IPredicate falsePredicate = null;
		for (final IPredicate pred : initialPredicates) {
			if (pred.getFormula().equals(trueTerm)) {
				truePredicate = pred;
			} else if (pred.getFormula().equals(falseTerm)) {
				falsePredicate = pred;
			}
		}
		if (truePredicate == null) {
			mTruePredicate = mSmtManager.getPredicateFactory().newPredicate(mScript.term("true"));
		} else {
			mTruePredicate = truePredicate;
		}
		if (falsePredicate == null) {
			mFalsePredicate = mSmtManager.getPredicateFactory().newPredicate(mScript.term("false"));
		} else {
			mFalsePredicate = falsePredicate;
		}
		declareTruePredicateAndFalsePredicate();
		for (final IPredicate pred : initialPredicates) {
			declarePredicate(pred);
		}
	}
	
	public IPredicate getTruePredicate() {
		return mTruePredicate;
	}

	public IPredicate getFalsePredicate() {
		return mFalsePredicate;
	}
	
	private void declareTruePredicateAndFalsePredicate() {
		final Map<IPredicate, Validity> impliedByTrue = Collections.emptyMap();
		final Map<IPredicate, Validity> expliedByTrue = Collections.emptyMap();
		addNewPredicate(mTruePredicate, mTruePredicate.getFormula(), 
				mTruePredicate.getFormula(), impliedByTrue, expliedByTrue);
		final Map<IPredicate, Validity> impliedByFalse = 
				Collections.singletonMap(mTruePredicate, Validity.VALID);
		final Map<IPredicate, Validity> expliedByFalse = 
				Collections.singletonMap(mTruePredicate, Validity.INVALID);
		addNewPredicate(mFalsePredicate, mFalsePredicate.getFormula(),
				mFalsePredicate.getFormula(), impliedByFalse, expliedByFalse);
	}


	/**
	 * Return true iff pred is the representative IPredicate for the Term
	 * pred.getFormula().
	 */
	boolean isRepresentative(final IPredicate pred) {
		final IPredicate representative = mTerm2Predicates.get(pred.getFormula());
		return pred == representative;
	}

	/**
	 * Add predicate. Store this predicate without further simplification. Throw
	 * an exception if this PredicateUnifier stores already an equivalent
	 * predicate.
	 */
	void declarePredicate(final IPredicate predicate) {
		final PredicateComparison pc = new PredicateComparison(predicate.getFormula(), predicate.getVars(), null, null);
		if (pc.isEquivalentToExistingPredicateWithLeqQuantifiers()) {
			if (pc.getEquivalantLeqQuantifiedPredicate() != predicate) {
				throw new AssertionError("There is already an" + " equivalent predicate");
			}
		} else if (pc.isEquivalentToExistingPredicateWithGtQuantifiers()) {
			if (pc.getEquivalantGtQuantifiedPredicate() != predicate) {
				throw new AssertionError("There is already an" + " equivalent predicate");
			}
		} else {
			addNewPredicate(predicate, predicate.getFormula(), predicate.getFormula(), 
					pc.getImpliedPredicates(), pc.getExpliedPredicates());
		}
		mPredicateUnifierBenchmarkGenerator.incrementDeclaredPredicates();
	}

	/**
	 * GetOrConstruct a predicate that is a conjunction of IPredicates that were
	 * construction by (resp. declared in) this PredicateUnifier. 
	 */
	public IPredicate getOrConstructPredicateForConjunction(final Collection<IPredicate> conjunction) {
		final Set<IPredicate> minimalSubset = computeMinimalEquivalentSubset_Conjunction(conjunction);
		if (minimalSubset.size() == 1) {
			return minimalSubset.iterator().next();
		} else {
			final HashMap<IPredicate, Validity> impliedPredicates = new HashMap<IPredicate, Validity>();
			final HashMap<IPredicate, Validity> expliedPredicates = new HashMap<IPredicate, Validity>();
			for (final IPredicate conjunct : minimalSubset) {
				for (final IPredicate knownPredicate : mKnownPredicates) {
					{
						// if (conjunct ==> knownPredicate) then the conjunction
						// will also imply the knownPredicate
						final Validity validity = getCoverageRelation().isCovered(conjunct, knownPredicate);
						if (validity == Validity.VALID) {
							impliedPredicates.put(knownPredicate, Validity.VALID);
						}
					}
					{
						// if !(knownPredicate ==> conjunct) then knownPredicate
						// will also not imply the conjunction
						final Validity validity = getCoverageRelation().isCovered(knownPredicate, conjunct);
						if (validity == Validity.INVALID) {
							expliedPredicates.put(knownPredicate, Validity.INVALID);
						}
					}
				}
			}
			final Term term = mSmtManager.getPredicateFactory().and(minimalSubset);
			return getOrConstructPredicate(term, 
					impliedPredicates, expliedPredicates);
		}
	}
	
	/**
	 * GetOrConstruct a predicate that is a disjunction of IPredicates that were
	 * constructed by (resp. declared in) this PredicateUnifier. 
	 */
	public IPredicate getOrConstructPredicateForDisjunction(final Collection<IPredicate> disjunction) {
		final Set<IPredicate> minimalSubset = computeMinimalEquivalentSubset_Disjunction(disjunction);
		if (minimalSubset.size() == 1) {
			return minimalSubset.iterator().next();
		} else {
			final HashMap<IPredicate, Validity> impliedPredicates = new HashMap<IPredicate, Validity>();
			final HashMap<IPredicate, Validity> expliedPredicates = new HashMap<IPredicate, Validity>();
			for (final IPredicate disjunct : minimalSubset) {
				for (final IPredicate knownPredicate : mKnownPredicates) {
					{
						// if (knownPredicate ==> disjunct) then the knownPredicate
						// will also imply the disjunction
						final Validity validity = getCoverageRelation().isCovered(knownPredicate, disjunct);
						if (validity == Validity.VALID) {
							expliedPredicates.put(knownPredicate, Validity.VALID);
						}
					}
					{
						// if !(disjunct ==> knownPredicate) then disjunction
						// will also not imply the knownPredicate
						final Validity validity = getCoverageRelation().isCovered(disjunct, knownPredicate);
						if (validity == Validity.INVALID) {
							impliedPredicates.put(knownPredicate, Validity.INVALID);
						}
					}
				}
			}
			final Term term = mSmtManager.getPredicateFactory().or(false, minimalSubset);
			return getOrConstructPredicate(term, 
					impliedPredicates, expliedPredicates);
		}
	}

	
	/**
	 * Compute a minimal subset of IPredicates for a given conjunction in the
	 * following sense. The conjunction of the subset is equivalent to the
	 * input conjunction and no two elements in the subset imply each other.
	 * I.e., if a predicate of in input conjunction is implied by another 
	 * predicate it is removed.
	 * @param conjunction of predicates that was constructed by this predicate unifier. 
	 * @return
	 */
	private Set<IPredicate> computeMinimalEquivalentSubset_Conjunction(final Collection<IPredicate> conjunction) {
		final List<IPredicate> list = new ArrayList<IPredicate>(conjunction);
		final Set<IPredicate> minimalSubset = new HashSet<IPredicate>(conjunction);
		for (int i=0; i<list.size(); i++) {
			final IPredicate predi = list.get(i);
			if (!mKnownPredicates.contains(predi)) {
				throw new IllegalArgumentException(predi + " not constructed by this predicate unifier");
			}
			final Set<IPredicate> coveredByPredi = getCoverageRelation().getCoveredPredicates(predi);
			for (int j=i+1; j<list.size(); j++) {
				final IPredicate predj= list.get(j);
				if (coveredByPredi.contains(predj)) {
					minimalSubset.remove(predi);
					continue;
				}
			}
		}
		return minimalSubset;
	}
	
	/**
	 * Compute a minimal subset of IPredicates for a given disjunction in the
	 * following sense. The disjunction of the subset is equivalent to the
	 * input disjunction and no two elements in the subset imply each other.
	 * I.e., if a predicate of in input disjunction is implies another 
	 * predicate it is removed.
	 * @param disjunction of predicates that was constructed by this predicate unifier. 
	 * @return
	 */
	private Set<IPredicate> computeMinimalEquivalentSubset_Disjunction(final Collection<IPredicate> disjunction) {
		final List<IPredicate> list = new ArrayList<IPredicate>(disjunction);
		final Set<IPredicate> minimalSubset = new HashSet<IPredicate>(disjunction);
		for (int i=0; i<list.size(); i++) {
			final IPredicate predi = list.get(i);
			if (!mKnownPredicates.contains(predi)) {
				throw new IllegalArgumentException(predi + " not constructed by this predicate unifier");
			}
			final Set<IPredicate> coveringPredi = getCoverageRelation().getCoveringPredicates(predi);
			for (int j=i+1; j<list.size(); j++) {
				final IPredicate predj= list.get(j);
				if (coveringPredi.contains(predj)) {
					minimalSubset.remove(predi);
					continue;
				}
			}
		}
		return minimalSubset;
	}

	/**
	 * Returns true iff each free variables corresponds to a BoogieVar in vars.
	 * Throws an Exception otherwise.
	 */
	private boolean varsIsSupersetOfFreeTermVariables(final Term term, final Set<IProgramVar> vars) {
		for (final TermVariable tv : term.getFreeVars()) {
			final IProgramVar bv = mSymbolTable.getBoogieVar(tv);
			if (bv == null) {
				throw new AssertionError("Variable " + tv + " has no corresponding BoogieVar, hence seems "
						+ "to be some auxiliary variable and may not "
						+ "occur unquantified in the formula of a predicate");
			} else {
				if (!vars.contains(bv)) {
					throw new AssertionError("Variable " + tv + " does not occur in vars");
				}
			}
		}
		return true;
	}

	/**
	 * Get the predicate for term. If there is not yet a predicate for term,
	 * construct the predicate using vars.
	 * 
	 * @param vars
	 *            The BoogieVars of the TermVariables contained in term.
	 * @param proc
	 *            All procedures of which vars contains local variables.
	 */
	public IPredicate getOrConstructPredicate(final Term term) {
		return getOrConstructPredicate(term, null, null);
	}
	
	/**
	 * Variant of getOrConstruct methods where we can provide information
	 * about implied/explied predicates.
	 */
	private IPredicate getOrConstructPredicate(final Term term,
			final HashMap<IPredicate, Validity> impliedPredicates, 
			final HashMap<IPredicate, Validity> expliedPredicates) {

		final TermVarsProc tvp = TermVarsProc.computeTermVarsProc(term, mScript, mSymbolTable);
		mPredicateUnifierBenchmarkGenerator.continueTime();
		mPredicateUnifierBenchmarkGenerator.incrementGetRequests();
		assert varsIsSupersetOfFreeTermVariables(term, tvp.getVars());
		final Term withoutAnnotation = stripAnnotation(term);

		{
			IPredicate p = mTerm2Predicates.get(withoutAnnotation);
			if (p != null) {
				if (mDeprecatedPredicates.containsKey(p)) {
					p = mDeprecatedPredicates.get(p);
				}
				mPredicateUnifierBenchmarkGenerator.incrementSyntacticMatches();
				mPredicateUnifierBenchmarkGenerator.stopTime();
				return p;
			}
		}
		final Term commuNF = (new CommuhashNormalForm(mServices, mScript)).transform(withoutAnnotation);
		{
			IPredicate p = mTerm2Predicates.get(commuNF);
			if (p != null) {
				if (mDeprecatedPredicates.containsKey(p)) {
					p = mDeprecatedPredicates.get(p);
				}
				mPredicateUnifierBenchmarkGenerator.incrementSyntacticMatches();
				mPredicateUnifierBenchmarkGenerator.stopTime();
				return p;
			}
		}
		
		final PredicateComparison pc = new PredicateComparison(commuNF, tvp.getVars(), 
				impliedPredicates, expliedPredicates);
		if (pc.isEquivalentToExistingPredicateWithLeqQuantifiers()) {
			mPredicateUnifierBenchmarkGenerator.incrementSemanticMatches();
			mPredicateUnifierBenchmarkGenerator.stopTime();
			return pc.getEquivalantLeqQuantifiedPredicate();
		}
		final IPredicate result;
		assert !SmtUtils.isTrue(commuNF) : "illegal predicate: true";
		assert !SmtUtils.isFalse(commuNF) : "illegal predicate: false";
		assert !mTerm2Predicates.containsKey(commuNF);
		final Term simplifiedTerm;
		if (pc.isIntricatePredicate()) {
			simplifiedTerm = commuNF;
		} else {
			try {
				final Term tmp = SmtUtils.simplify(mScript, commuNF, mServices, mSimplificationTechnique, mSmtManager.getBoogie2Smt().getVariableManager());
				simplifiedTerm  = (new CommuhashNormalForm(mServices, mScript)).transform(tmp);
			} catch (final ToolchainCanceledException tce) {
				throw new ToolchainCanceledException(getClass(), tce.getRunningTaskInfo() + " while unifying predicates");
			}
		}
		result = mSmtManager.getPredicateFactory().newPredicate(simplifiedTerm);
		if (pc.isEquivalentToExistingPredicateWithGtQuantifiers()) {
			mDeprecatedPredicates.put(pc.getEquivalantGtQuantifiedPredicate(), result);
			mPredicateUnifierBenchmarkGenerator.incrementDeprecatedPredicates();
		}
		addNewPredicate(result, term, simplifiedTerm, pc.getImpliedPredicates(), pc.getExpliedPredicates());
		assert new CheckClosedTerm().isClosed(result.getClosedFormula());
		assert varsIsSupersetOfFreeTermVariables(result.getFormula(), result.getVars());
		mPredicateUnifierBenchmarkGenerator.incrementConstructedPredicates();
		mPredicateUnifierBenchmarkGenerator.stopTime();
		return result;
	}

	private Term stripAnnotation(final Term term) {
		final Term withoutAnnotation;
		if (term instanceof AnnotatedTerm) {
			final AnnotatedTerm annotatedTerm = (AnnotatedTerm) term;
			final Annotation[] annotations = annotatedTerm.getAnnotations();
			if (annotations.length == 1) {
				if (annotations[0].getKey().equals(":quoted")) {
					withoutAnnotation = annotatedTerm.getSubterm();
				} else {
					throw new UnsupportedOperationException();
				}
			} else {
				throw new UnsupportedOperationException();
			}
		} else {
			withoutAnnotation = term;
		}
		return withoutAnnotation;
	}

	
	/**
	 * Add a new predicate. 
	 * @param pred
	 * @param simplifiedTerm 
	 * @param term 
	 * @param implied 
	 * 	Set of pairs (p,val) such that val is the validity of the implication pred ==> p.
	 * @param explied
	 *  Set of pairs (p,val) such that val is the validity of the explication pred <== p.
	 */
	private void addNewPredicate(final IPredicate pred, final Term term, final Term simplifiedTerm, 
			final Map<IPredicate, Validity> implied, final Map<IPredicate, Validity> explied) {
		mTerm2Predicates.put(term, pred);
		mTerm2Predicates.put(simplifiedTerm, pred);
		mCoverageRelation.addPredicate(pred, implied, explied);
		assert !mKnownPredicates.contains(pred) : "predicate already known";
		mKnownPredicates.add(pred);
	}


//	private IPredicate compareWithExistingPredicates(Term term, Set<BoogieVar> vars,
//			HashMap<IPredicate, Validity> impliedPredicats, HashMap<IPredicate, Validity> expliedPredicates) {
//		Term closedTerm = PredicateUtils.computeClosedFormula(term, vars, mScript);
//		assert impliedPredicats.isEmpty();
//		assert expliedPredicates.isEmpty();
//		mSmtManager.lock(this);
//		mScript.echo(new QuotedObject("begin unification"));
//		for (Term interpolantTerm : mTerm2Predicates.keySet()) {
//			IPredicate interpolant = mTerm2Predicates.get(interpolantTerm);
//			Term interpolantClosedTerm = interpolant.getClosedFormula();
//			Validity implies = mSmtManager.isCovered(this, closedTerm, interpolantClosedTerm);
//			impliedPredicats.put(interpolant, implies);
//			Validity explies = mSmtManager.isCovered(this, interpolantClosedTerm, closedTerm);
//			expliedPredicates.put(interpolant, explies);
//			if (implies == Validity.VALID && explies == Validity.VALID) {
//				mScript.echo(new QuotedObject("end unification"));
//				mSmtManager.unlock(this);
//				return interpolant;
//			}
//		}
//		mScript.echo(new QuotedObject("end unification"));
//		mSmtManager.unlock(this);
//		return null;
//	}
	
	
	/**
	 * Compare Term term whose free variables represent the BoogieVars vars with
	 * all predicates that this Predicate unifier knows. If there exists a
	 * predicate for which we can prove that it is equivalent to term, this
	 * predicate is returned. Otherwise we return null and HashMaps
	 * impliedPredicats and expliedPredicates are filled with information about
	 * implications between term and existing Predicates.
	 * ImpliedPredicates will be filled with all IPredicates implied by term.
	 * ImpliedPredicates will be filled with all IPredicates that imply term.
	 * @return
	 */
	private class PredicateComparison {
		private final Term mClosedTerm;
		private final boolean mTermContainsQuantifiers;
		private final HashMap<IPredicate, Validity> mImpliedPredicates;
		private final HashMap<IPredicate, Validity> mExpliedPredicates;
		private final IPredicate mEquivalentLeqQuantifiedPredicate;
		private IPredicate mEquivalentGtQuantifiedPredicate;
		private boolean mIsIntricatePredicate;
		
		public Term getClosedTerm() {
			if (mEquivalentLeqQuantifiedPredicate != null) {
				throw new IllegalAccessError("not accessible, we found an equivalent predicate");
			}
			return mClosedTerm;
		}

		public HashMap<IPredicate, Validity> getImpliedPredicates() {
			if (mEquivalentLeqQuantifiedPredicate != null) {
				throw new IllegalAccessError("not accessible, we found an equivalent predicate");
			}
			return mImpliedPredicates;
		}

		public HashMap<IPredicate, Validity> getExpliedPredicates() {
			if (mEquivalentLeqQuantifiedPredicate != null) {
				throw new IllegalAccessError("not accessible, we found an equivalent predicate");
			}
			return mExpliedPredicates;
		}

		public IPredicate getEquivalantLeqQuantifiedPredicate() {
			if (mEquivalentLeqQuantifiedPredicate == null) {
				throw new IllegalAccessError("accessible only if equivalent to existing predicate");
			}
			return mEquivalentLeqQuantifiedPredicate;
		}
		
		public IPredicate getEquivalantGtQuantifiedPredicate() {
			if (mEquivalentGtQuantifiedPredicate == null) {
				throw new IllegalAccessError("accessible only if equivalent to existing predicate");
			}
			return mEquivalentGtQuantifiedPredicate;
		}

		public boolean isIntricatePredicate() {
			if (mEquivalentLeqQuantifiedPredicate != null) {
				throw new IllegalAccessError("not accessible, we found an equivalent predicate");
			}
			return mIsIntricatePredicate;
		}
		
		public boolean isEquivalentToExistingPredicateWithLeqQuantifiers() {
			return mEquivalentLeqQuantifiedPredicate != null;
		}
		
		public boolean isEquivalentToExistingPredicateWithGtQuantifiers() {
			return mEquivalentGtQuantifiedPredicate != null;
		}


		/**
		 * Compare a new term/vars with all known predicates of this 
		 * PredicateUnifier.
		 * Information about predicates that are implied/explied by term can
		 * be provided as an input by the Maps impliedPredicates/expliedPredicates
		 * both maps will be modified by (new predicates added) by this method. 
		 */
		private PredicateComparison(final Term term, final Set<IProgramVar> vars, 
				final HashMap<IPredicate, Validity> impliedPredicates, 
				final HashMap<IPredicate, Validity> expliedPredicates) {
			if (impliedPredicates == null) {
				if (expliedPredicates != null) {
					throw new IllegalArgumentException("both or none null");
				}
				mImpliedPredicates = new HashMap<IPredicate, Validity>();
				mExpliedPredicates = new HashMap<IPredicate, Validity>();
			} else {
				mImpliedPredicates = impliedPredicates;
				mExpliedPredicates = expliedPredicates;
			}
			
			mClosedTerm = PredicateUtils.computeClosedFormula(term, vars, mScript);
			mTermContainsQuantifiers = new ContainsQuantifier().containsQuantifier(term);
			if (mSmtManager.isLocked()) {
				mSmtManager.requestLockRelease();
			}
			mSmtManager.lock(this);
			mScript.echo(new QuotedObject("begin unification"));
			
			mEquivalentLeqQuantifiedPredicate = compare();

			mScript.echo(new QuotedObject("end unification"));
			mSmtManager.unlock(this);
		}
		
		
		private IPredicate compare() {
			// check if false
			final Validity impliesFalse = mSmtManager.isCovered(this, mClosedTerm, mFalsePredicate.getFormula());
			switch (impliesFalse) {
			case VALID:
				return mFalsePredicate;
			case INVALID:
				mImpliedPredicates.put(mFalsePredicate, Validity.INVALID);
				break;
			case UNKNOWN:
				mLogger.warn(new DebugMessage("unable to proof that {0} is different from false", mClosedTerm));
				mImpliedPredicates.put(mFalsePredicate, Validity.UNKNOWN);
				mIsIntricatePredicate = true;
				break;
			case NOT_CHECKED:
				throw new AssertionError("we wanted it checked");
			default:
				throw new AssertionError("unknown case");
			}
			// every predicate is implied by false
			mExpliedPredicates.put(mFalsePredicate, Validity.VALID);
			
			// check if true
			final Validity impliedByTrue = mSmtManager.isCovered(this, mTruePredicate.getClosedFormula(), mClosedTerm);
			switch (impliedByTrue) {
			case VALID:
				return mTruePredicate;
			case INVALID:
				mExpliedPredicates.put(mTruePredicate, Validity.INVALID);
				break;
			case UNKNOWN:
				mLogger.warn(new DebugMessage("unable to proof that {0} is different from true", mClosedTerm));
				mExpliedPredicates.put(mTruePredicate, Validity.UNKNOWN);
				mIsIntricatePredicate = true;
				break;
			case NOT_CHECKED:
				throw new AssertionError("we wanted it checked");
			default:
				throw new AssertionError("unknown case");
			}
			// every predicate implies true
			mImpliedPredicates.put(mTruePredicate, Validity.VALID);
			
			// if predicate is intricate we do not compare against others
			if (mIsIntricatePredicate) {
				for (final IPredicate other : mKnownPredicates) {
					if (other == mTruePredicate || other == mFalsePredicate) {
						continue;
					}
					mImpliedPredicates.put(other, Validity.NOT_CHECKED);
					mExpliedPredicates.put(other, Validity.NOT_CHECKED);
					continue;
				}
				mPredicateUnifierBenchmarkGenerator.incrementIntricatePredicates();
				return null;
			}
			
			for (final IPredicate other : mKnownPredicates) {
				if (other == mTruePredicate || other == mFalsePredicate) {
					continue;
				}
				// we do not compare against intricate predicates
				if (PredicateUnifier.this.isIntricatePredicate(other)) {
					mImpliedPredicates.put(other, Validity.NOT_CHECKED);
					mExpliedPredicates.put(other, Validity.NOT_CHECKED);
					continue;
				}
				checkTimeout(mClosedTerm);
				final Term otherClosedTerm = other.getClosedFormula();
				Validity implies = mImpliedPredicates.get(other);
				if (implies == null) {
					implies = mSmtManager.isCovered(this, mClosedTerm, otherClosedTerm);
					if (implies == Validity.VALID) {
						// if (this ==> other) and (other ==> impliedByOther) then
						// we conclude (this ==> impliedByOther)
						for (final IPredicate impliedByOther : getCoverageRelation().getCoveringPredicates(other)) {
							if (impliedByOther != other) {
								final Validity oldValue = mImpliedPredicates.put(impliedByOther, Validity.VALID);
								if (oldValue == null || oldValue == Validity.UNKNOWN) {
									mPredicateUnifierBenchmarkGenerator.incrementImplicationChecksByTransitivity();
								} else {
									assert oldValue == Validity.VALID : 
										"implication result by transitivity: " + Validity.VALID +
										" old implication result: " + oldValue;
								}
							}
						}
					} else if (implies == Validity.INVALID) {
						// if !(this ==> other) and (expliedbyOther ==> other)
						// we conclude !(this ==> expliedbyOther)
						for (final IPredicate expliedByOther : getCoverageRelation().getCoveredPredicates(other)) {
							if (expliedByOther != other) {
								final Validity oldValue = mImpliedPredicates.put(expliedByOther, Validity.INVALID);
								if (oldValue == null || oldValue == Validity.UNKNOWN) {
									mPredicateUnifierBenchmarkGenerator.incrementImplicationChecksByTransitivity();
								} else {
									assert oldValue == Validity.INVALID : 
										"implication result by transitivity: " + Validity.INVALID +
										" old implication result: " + oldValue;
								}
							}
						}
					}
					mImpliedPredicates.put(other, implies);
				}
				Validity explies = mExpliedPredicates.get(other);
				if (explies == null) {
					explies = mSmtManager.isCovered(this, otherClosedTerm, mClosedTerm);
					if (explies == Validity.VALID) {
						// if (other ==> this) and (expliedByOther ==> other)
						// we conclude (expliedByOther ==> this)
						for (final IPredicate expliedByOther : getCoverageRelation().getCoveredPredicates(other)) {
							if (expliedByOther != other) {
								final Validity oldValue = mExpliedPredicates.put(expliedByOther, Validity.VALID);
								if (oldValue == null || oldValue == Validity.UNKNOWN) {
									mPredicateUnifierBenchmarkGenerator.incrementImplicationChecksByTransitivity();
								} else {
									assert oldValue == Validity.VALID : 
										"explication result by transitivity: " + Validity.VALID +
										" old explication result: " + oldValue;
								}
							}
						}						
					} else if (explies == Validity.INVALID) {
						// if !(other ==> this) and (other ==> impliedByOther)
						// we conclude !(impliedByOther ==> this)
						for (final IPredicate impliedByOther : getCoverageRelation().getCoveringPredicates(other)) {
							if (impliedByOther != other) {
								final Validity oldValue = mExpliedPredicates.put(impliedByOther, Validity.INVALID);
								if (oldValue == null || oldValue == Validity.UNKNOWN) {
									mPredicateUnifierBenchmarkGenerator.incrementImplicationChecksByTransitivity();
								} else {
									assert oldValue == Validity.INVALID : 
										"explication result by transitivity: " + Validity.INVALID +
										" old explication result: " + oldValue;
								}
							}
						}
					}
					mExpliedPredicates.put(other, explies);
				}
				if (implies == Validity.VALID && explies == Validity.VALID) {
					if (mDeprecatedPredicates.containsKey(other)) {
						return mDeprecatedPredicates.get(other);
					}
					final boolean otherContainsQuantifiers = 
							(new ContainsQuantifier()).containsQuantifier(other.getFormula());
					if (!otherContainsQuantifiers || 
							(mTermContainsQuantifiers && !thisIsLessQuantifiedThanOther(mClosedTerm, otherClosedTerm))) {
						return other;
					} else {
						if (mEquivalentGtQuantifiedPredicate == null) {
							mEquivalentGtQuantifiedPredicate = other;
						} else {
							throw new AssertionError("at most one deprecated predicate");
						}
					}
				}
			}
			// no predicate was equivalent
			return null;
		}

		private void checkTimeout(final Term closedTerm) {
			if (!mServices.getProgressMonitorService().continueProcessing()) {
				final String quantifierInformation = generateQuantifierInformation(closedTerm);
				throw new ToolchainCanceledException(this.getClass(),
						"PredicateUnifier was comparing new predicate (" + 
						quantifierInformation + ") to " + 
						mKnownPredicates.size() + " known predicates");
			}
		}

		private String generateQuantifierInformation(final Term closedTerm) {
			final String result;
			final Term pnf = new PrenexNormalForm(mScript, mSmtManager.getVariableManager()).transform(closedTerm);
			if (pnf instanceof QuantifiedFormula) {
				final QuantifierSequence qs = new QuantifierSequence(mScript, pnf);
				result = "quantified with " + (qs.getNumberOfQuantifierBlocks()-1) + "quantifier alternations";
			} else {
				result = "quantifier-free";
			}
			return result;
		}
	}
	
	// Matthias 2016-11-4: at the moment we believe that for the backward
	// predicates universal quantification is better than existential 
	// quantification.
	private boolean thisIsLessQuantifiedThanOther(final Term thisTerm, final Term otherTerm) {
		final ContainsQuantifier thisQuantifierCheck = new ContainsQuantifier();
		thisQuantifierCheck.containsQuantifier(thisTerm);
		final ContainsQuantifier otherQuantifierCheck = new ContainsQuantifier();
		otherQuantifierCheck.containsQuantifier(otherTerm);
		return thisQuantifierCheck.getFirstQuantifierFound() == QuantifiedFormula.FORALL &&
				otherQuantifierCheck.getFirstQuantifierFound() == QuantifiedFormula.EXISTS;
	}
	
	public String collectPredicateUnifierStatistics() {
		final StringBuilder builder = new StringBuilder();
		builder.append(PredicateUnifierStatisticsType.getInstance().
				prettyprintBenchmarkData(mPredicateUnifierBenchmarkGenerator));
		builder.append(mCoverageRelation.getCoverageRelationStatistics());
		return builder.toString();
	}
	
	
	/**
	 * We call a predicate "intricate" if we were unable to find our if it is
	 * equivalent to "true" or if we were unable to find out it it is equivalent
	 * to "false".
	 */
	public boolean isIntricatePredicate(final IPredicate pred) {
		final Validity equivalentToTrue = getCoverageRelation().isCovered(mTruePredicate, pred);
		final Validity equivalentToFalse = getCoverageRelation().isCovered(pred, mFalsePredicate);
		if (equivalentToTrue == Validity.UNKNOWN || equivalentToFalse == Validity.UNKNOWN) {
			return true;
		} else {
			return false;
		}
	}


	/**
	 * Given a term "cut up" all its conjuncts. We bring the term in CNF and
	 * return an IPredicate for each conjunct.
	 */
	public Set<IPredicate> cannibalize(final boolean splitNumericEqualities, final Term term) {
		final Set<IPredicate> result = new HashSet<IPredicate>();
		final Term cnf = (new Cnf(mScript, mServices, mSmtManager.getVariableManager())).transform(term);
		Term[] conjuncts;
		if (splitNumericEqualities) {
			conjuncts = splitNumericEqualities(SmtUtils.getConjuncts(cnf));
		} else {
			conjuncts = SmtUtils.getConjuncts(cnf);
		}
		for (final Term conjunct : conjuncts) {
			final IPredicate predicate = getOrConstructPredicate(conjunct);
			result.add(predicate);
		}
		return result;
	}

	private Term[] splitNumericEqualities(final Term[] conjuncts) {
		final ArrayList<Term> result = new ArrayList<>(conjuncts.length * 2);
		for (final Term conjunct : conjuncts) {
			try {
				final BinaryNumericRelation bnr = new BinaryNumericRelation(conjunct);
				if (bnr.getRelationSymbol() == RelationSymbol.EQ) {
					final Term leq = mScript.term("<=", bnr.getLhs(), bnr.getRhs());
					result.add(leq);
					final Term geq = mScript.term(">=", bnr.getLhs(), bnr.getRhs());
					result.add(geq);
				} else {
					result.add(conjunct);
				}
			} catch (final NoRelationOfThisKindException e) {
				result.add(conjunct);
			}
		}
		return result.toArray(new Term[result.size()]);
	}

	public Set<IPredicate> cannibalizeAll(final boolean splitNumericEqualities, final IPredicate... predicates) {
		final Set<IPredicate> result = new HashSet<IPredicate>();
		for (final IPredicate pred : predicates) {
			result.addAll(cannibalize(splitNumericEqualities, pred.getFormula()));
		}
		return result;
	}

	public IPredicateCoverageChecker getCoverageRelation() {
		return mCoverageRelation;
	}

	public class CoverageRelation implements IPredicateCoverageChecker {

		NestedMap2<IPredicate, IPredicate, Validity> mLhs2RhsValidity = new NestedMap2<IPredicate, IPredicate, Validity>();
		HashRelation<IPredicate, IPredicate> mImpliedPredicates = new HashRelation<IPredicate, IPredicate>();
		HashRelation<IPredicate, IPredicate> mExpliedPredicates = new HashRelation<IPredicate, IPredicate>();
		
		void addPredicate(final IPredicate pred, final Map<IPredicate, Validity> implied, final Map<IPredicate, Validity> explied) {
			assert !mKnownPredicates.contains(pred) : "predicate already known";
			assert coverageMapIsComplete();
			for (final IPredicate known : mKnownPredicates) {
				final Validity implies = implied.get(known);
				assert implies != null : "unknown implies for " + known;
				final Validity explies = explied.get(known);
				assert explies != null : "unknown explies for " + known;
				final Validity oldimpl = mLhs2RhsValidity.put(pred, known, implies);
				assert oldimpl == null : "entry existed !";
				final Validity oldexpl = mLhs2RhsValidity.put(known, pred, explies);
				assert oldexpl == null : "entry existed !";
				if (implies == Validity.VALID) {
					mImpliedPredicates.addPair(pred, known);
					mExpliedPredicates.addPair(known, pred);
				}
				if (explies == Validity.VALID) {
					mImpliedPredicates.addPair(known, pred);
					mExpliedPredicates.addPair(pred, known);
				}
			}
			mImpliedPredicates.addPair(pred, pred);
			mExpliedPredicates.addPair(pred, pred);
			assert coverageMapIsComplete();
		}

		@Override
		public Validity isCovered(final IPredicate lhs, final IPredicate rhs) {
			if (lhs == rhs) {
				return Validity.VALID;
			}
			final Validity result = mLhs2RhsValidity.get(lhs, rhs);
			if (result == null) {
				throw new AssertionError("at least one of both input predicates is unknown");
			}
			return result;
		}
		

		@Override
		public Set<IPredicate> getCoveringPredicates(final IPredicate pred) {
			return mImpliedPredicates.getImage(pred);
		}
		

		@Override
		public Set<IPredicate> getCoveredPredicates(final IPredicate pred) {
			return mExpliedPredicates.getImage(pred);
		}
		
		public CoverageRelationStatistics getCoverageRelationStatistics() {
			return new CoverageRelationStatistics(mLhs2RhsValidity);
		}
		
		private boolean coverageMapIsComplete() {
			boolean nothingMissing = true;
			for (final IPredicate p1 : mKnownPredicates) {
				for (final IPredicate p2 : mKnownPredicates) {
					if (p1 != p2) {
						final Validity validity = mLhs2RhsValidity.get(p1, p2);
						assert (validity != null) : "value missing for pair " + p1 + ", " + p2;
						if (validity == null) {
							nothingMissing = false;
						}
					}
				}
			}
			return nothingMissing;
		}
	}
	
	public class CoverageRelationStatistics {
		private final int mValidCoverageRelations;
		private final int mInvalidCoverageRelations;
		private final int mUnknownCoverageRelations;
		private final int mNotCheckedCoverageRelations;

		public CoverageRelationStatistics(
				final NestedMap2<IPredicate, IPredicate, Validity> lhs2RhsValidity) {
			int invalid = 0; int valid = 0; int unknown = 0; int notChecked = 0;
			for (final Triple<IPredicate, IPredicate, Validity> entry : lhs2RhsValidity.entrySet()) {
				switch (entry.getThird()) {
				case INVALID:
					invalid++;
					break;
				case NOT_CHECKED:
					notChecked++;
					break;
				case UNKNOWN:
					unknown++;
					break;
				case VALID:
					valid++;
					break;
				default:
					throw new AssertionError();
				}
			}
			mValidCoverageRelations = valid;
			mInvalidCoverageRelations = invalid;
			mUnknownCoverageRelations = unknown;
			mNotCheckedCoverageRelations = notChecked;
		}

		@Override
		public String toString() {
			return String.format("CoverageRelationStatistics Valid=%s, Invalid=%s, Unknown=%s, NotChecked=%s, Total=%s",
							mValidCoverageRelations,
							mInvalidCoverageRelations,
							mUnknownCoverageRelations,
							mNotCheckedCoverageRelations,
							mValidCoverageRelations + mInvalidCoverageRelations + 
							mUnknownCoverageRelations + mNotCheckedCoverageRelations);
		}
		
		
	}
	
	
	
	public enum PredicateUniferStatisticsDefinitions implements IStatisticsElement {
		
		DeclaredPredicates(Integer.class, AStatisticsType.s_IntegerAddition, AStatisticsType.s_DataBeforeKey),
		GetRequests(Integer.class, AStatisticsType.s_IntegerAddition, AStatisticsType.s_DataBeforeKey),
		
		SyntacticMatches(Integer.class, AStatisticsType.s_IntegerAddition, AStatisticsType.s_DataBeforeKey),
		SemanticMatches(Integer.class, AStatisticsType.s_IntegerAddition, AStatisticsType.s_DataBeforeKey),
		ConstructedPredicates(Integer.class, AStatisticsType.s_IntegerAddition, AStatisticsType.s_DataBeforeKey),
		IntricatePredicates(Integer.class, AStatisticsType.s_IntegerAddition, AStatisticsType.s_DataBeforeKey),
		DeprecatedPredicates(Integer.class, AStatisticsType.s_IntegerAddition, AStatisticsType.s_DataBeforeKey),
		ImplicationChecksByTransitivity(Integer.class, AStatisticsType.s_IntegerAddition, AStatisticsType.s_DataBeforeKey),
		Time(Integer.class, AStatisticsType.s_LongAddition, AStatisticsType.s_TimeBeforeKey),
		;
		
		private final Class<?> mClazz;
		private final Function<Object, Function<Object, Object>> mAggr;
		private final Function<String, Function<Object, String>> mPrettyprinter;
		
		PredicateUniferStatisticsDefinitions(final Class<?> clazz, 
				final Function<Object, Function<Object, Object>> aggr, 
				final Function<String, Function<Object, String>> prettyprinter) {
			mClazz = clazz;
			mAggr = aggr;
			mPrettyprinter = prettyprinter;
		}

		@Override
		public Object aggregate(final Object o1, final Object o2) {
			return mAggr.apply(o1).apply(o2);
		}

		@Override
		public String prettyprint(final Object o) {
			return mPrettyprinter.apply(name()).apply(o);
		}

		@Override
		public Class<?> getDataType() {
			return mClazz;
		}
	}
	
	
	public static class PredicateUnifierStatisticsType extends AStatisticsType<PredicateUniferStatisticsDefinitions> implements IStatisticsType {
		
		public PredicateUnifierStatisticsType() {
			super(PredicateUniferStatisticsDefinitions.class);
		}

		private static final PredicateUnifierStatisticsType s_Instance = new PredicateUnifierStatisticsType();
		
		public static PredicateUnifierStatisticsType getInstance() {
			return s_Instance;
		}
		
	}
	
	
	
	public class PredicateUnifierStatisticsGenerator implements IStatisticsDataProvider {
		
		private int mDeclaredPredicates = 0;
		private int mGetRequests = 0;
		private int mSyntacticMatches = 0;
		private int mSemanticMatches = 0;
		private int mConstructedPredicates = 0;
		private int mIntricatePredicates = 0;
		private int mDeprecatedPredicates = 0;
		private int mImplicationChecksByTransitivity = 0;
		protected final Benchmark mBenchmark;

		protected boolean mRunning = false;

		public PredicateUnifierStatisticsGenerator() {
			mBenchmark = new Benchmark();
			mBenchmark.register(String.valueOf(PredicateUniferStatisticsDefinitions.Time));
		}

		public void incrementDeclaredPredicates() {
			mDeclaredPredicates++;
		}
		public void incrementGetRequests() {
			mGetRequests++;
		}
		public void incrementSyntacticMatches() {
			mSyntacticMatches++;
		}
		public void incrementSemanticMatches() {
			mSemanticMatches++;
		}
		public void incrementConstructedPredicates() {
			mConstructedPredicates++;
		}
		public void incrementIntricatePredicates() {
			mIntricatePredicates++;
		}
		public void incrementDeprecatedPredicates() {
			mDeprecatedPredicates++;
			assert mDeprecatedPredicates == PredicateUnifier.this.mDeprecatedPredicates.size() 
					: "number of deprecated predicates inconsistent";
		}
		public void incrementImplicationChecksByTransitivity() {
			mImplicationChecksByTransitivity++;
		}



		public long getTime() {
			return (long) mBenchmark.getElapsedTime(String.valueOf(PredicateUniferStatisticsDefinitions.Time), TimeUnit.NANOSECONDS);
		}
		public void continueTime() {
			assert mRunning == false : "Timing already running";
			mRunning = true;
			mBenchmark.unpause(String.valueOf(PredicateUniferStatisticsDefinitions.Time));
		}
		public void stopTime() {
			assert mRunning == true : "Timing not running";
			mRunning = false;
			mBenchmark.pause(String.valueOf(PredicateUniferStatisticsDefinitions.Time));
		}
		@Override
		public Collection<String> getKeys() {
			return PredicateUnifierStatisticsType.getInstance().getKeys();
		}
		@Override
		public Object getValue(final String key) {
			final PredicateUniferStatisticsDefinitions keyEnum = Enum.valueOf(PredicateUniferStatisticsDefinitions.class, key);
			switch (keyEnum) {
			case DeclaredPredicates:
				return mDeclaredPredicates;
			case GetRequests:
				return mGetRequests;
			case SyntacticMatches:
				return mSyntacticMatches;
			case SemanticMatches:
				return mSemanticMatches;
			case ConstructedPredicates:
				return mConstructedPredicates;
			case IntricatePredicates:
				return mIntricatePredicates;
			case DeprecatedPredicates:
				return mDeprecatedPredicates;
			case ImplicationChecksByTransitivity:
				return mImplicationChecksByTransitivity;
			case Time:
				return getTime();
			default:
				throw new AssertionError("unknown key");
			}
		}

		@Override
		public IStatisticsType getBenchmarkType() {
			return PredicateUnifierStatisticsType.getInstance();
		}
	}

	public IStatisticsDataProvider getPredicateUnifierBenchmark() {
		return mPredicateUnifierBenchmarkGenerator;
	}

}
