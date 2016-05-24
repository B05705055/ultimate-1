/*
 * Copyright (C) 2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates;

import java.util.Set;

import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.ICallAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IInternalAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IReturnAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.HoareTripleCheckerStatisticsGenerator;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.IHoareTripleChecker;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.PredicateUnifier;
import de.uni_freiburg.informatik.ultimate.util.relation.NestedMap3;
import de.uni_freiburg.informatik.ultimate.util.relation.NestedMap4;

/**
 * IHoareTripleChecker that caches already computed results.
 * Also tries to use these results for more intelligent checks.
 * @author Matthias Heizmann
 *
 */
public class CachingHoareTripleChecker implements IHoareTripleChecker {
	
	private final IHoareTripleChecker mComputingHoareTripleChecker;
	private final PredicateUnifier mPredicateUnifer;
	private final NestedMap3<IPredicate, IInternalAction, IPredicate, Validity> mInternalCache =
			new NestedMap3<>();
	private final NestedMap3<IPredicate, CodeBlock, IPredicate, Validity> mCallCache =
			new NestedMap3<>();
	private final NestedMap4<IPredicate, IPredicate, CodeBlock, IPredicate, Validity> mReturnCache =
			new NestedMap4<>();
	private final boolean mUnknownIfSomeExtendedCacheCheckIsUnknown = true;
	
	public CachingHoareTripleChecker(
			IHoareTripleChecker protectedHoareTripleChecker,
			PredicateUnifier predicateUnifer) {
		super();
		mComputingHoareTripleChecker = protectedHoareTripleChecker;
		mPredicateUnifer = predicateUnifer;
	}

	@Override
	public Validity checkInternal(IPredicate pre, IInternalAction act, IPredicate succ) {
		Validity result = mInternalCache.get(pre, act, succ);
		if (result == null) {
			result = extendedCacheCheckInternal(pre,act,succ);
			if (result == null) {
				result = mComputingHoareTripleChecker.checkInternal(pre, act, succ);
			}
			mInternalCache.put(pre, act, succ, result);
		}
		return result;
	}

	private Validity extendedCacheCheckInternal(IPredicate pre, IInternalAction act, IPredicate succ) {
		boolean someResultWasUnknown = false;
		{
			Set<IPredicate> strongerThanPre = mPredicateUnifer.getCoverageRelation().getCoveredPredicates(pre);
			Set<IPredicate> weakerThanSucc = mPredicateUnifer.getCoverageRelation().getCoveringPredicates(succ);
			for (IPredicate strengthenedPre : strongerThanPre) {
				for (IPredicate weakenedSucc : weakerThanSucc) {
					Validity result = mInternalCache.get(strengthenedPre, act, weakenedSucc);
					if (result != null) {
						switch (result) {
						case VALID:
							break;
						case UNKNOWN:
							someResultWasUnknown = true;
							break;
						case INVALID:
							return result;
						case NOT_CHECKED:
							break;
//						throw new IllegalStateException("use protective Hoare triple checker");
						default:
							throw new AssertionError("unknown case");
						}
					}
				}
			}
		}
		{
			Set<IPredicate> weakerThanPre = mPredicateUnifer.getCoverageRelation().getCoveringPredicates(pre);
			Set<IPredicate> strongerThanSucc = mPredicateUnifer.getCoverageRelation().getCoveredPredicates(succ);
			for (IPredicate weakenedPre : weakerThanPre) {
				for (IPredicate strengthenedSucc : strongerThanSucc) {
					Validity result = mInternalCache.get(weakenedPre, act, strengthenedSucc);
					if (result != null) {
						switch (result) {
						case VALID:
							return result;
						case UNKNOWN:
							someResultWasUnknown = true;
							break;
						case INVALID:
							break;
						case NOT_CHECKED:
							break;
//						throw new IllegalStateException("use protective Hoare triple checker");
						default:
							throw new AssertionError("unknown case");
						}
					}
				}
			}
		}
		if (mUnknownIfSomeExtendedCacheCheckIsUnknown && someResultWasUnknown) {
			return Validity.UNKNOWN;
		} else {
			return null;
		}
	}

	@Override
	public Validity checkCall(IPredicate pre, ICallAction act, IPredicate succ) {
		return mComputingHoareTripleChecker.checkCall(pre, act, succ);
	}

	@Override
	public Validity checkReturn(IPredicate preLin, IPredicate preHier,
			IReturnAction act, IPredicate succ) {
		return mComputingHoareTripleChecker.checkReturn(preLin, preHier, act, succ);
	}
	
	

	@Override
	public HoareTripleCheckerStatisticsGenerator getEdgeCheckerBenchmark() {
		return mComputingHoareTripleChecker.getEdgeCheckerBenchmark();
	}

	public IHoareTripleChecker getProtectedHoareTripleChecker() {
		return mComputingHoareTripleChecker;
	}

	@Override
	public void releaseLock() {
		mComputingHoareTripleChecker.releaseLock();
	}
	
	

}
