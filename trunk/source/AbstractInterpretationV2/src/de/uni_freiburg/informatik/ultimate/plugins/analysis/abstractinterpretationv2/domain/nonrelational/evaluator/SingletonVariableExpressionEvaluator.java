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

package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.IBoogieVar;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.BooleanValue;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.INonrelationalValue;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.INonrelationalValueFactory;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.NonrelationalEvaluationResult;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.NonrelationalState;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.evaluator.EvaluatorUtils.EvaluatorType;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.util.TypeUtils.TypeUtils;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Triple;

/**
 * Evaluator for singleton varaible expressions in nonrelational abstract domains.
 *
 * @author Marius Greitschus (greitsch@informatik.uni-freiburg.de)
 *
 * @param <VALUE>
 *            The type of abstract domain values.
 * @param <STATE>
 *            The type of abstract domain states.
 */
public class SingletonVariableExpressionEvaluator<VALUE extends INonrelationalValue<VALUE>, STATE extends NonrelationalState<STATE, VALUE>>
		implements IEvaluator<VALUE, STATE, CodeBlock> {

	private final Set<IBoogieVar> mVariableSet;
	private final IBoogieVar mVar;
	private final INonrelationalValueFactory<VALUE> mNonrelationalValueFactory;

	private boolean mContainsBoolean = false;
	private final EvaluatorType mType;

	public SingletonVariableExpressionEvaluator(final IBoogieVar var,
			final INonrelationalValueFactory<VALUE> nonrelationalValueFactory) {
		mVar = var;
		mVariableSet = Collections.singleton(var);
		mNonrelationalValueFactory = nonrelationalValueFactory;
		mType = EvaluatorUtils.getEvaluatorType(mVar);
	}

	@Override
	public List<IEvaluationResult<VALUE>> evaluate(final STATE currentState) {
		assert currentState != null;

		final List<IEvaluationResult<VALUE>> returnList = new ArrayList<>();

		VALUE val;
		BooleanValue returnBool = BooleanValue.TOP;

		// TODO: Add array support.
		final Function<IBoogieVar, Triple<VALUE, BooleanValue, Boolean>> varFunction =
				var -> new Triple<>(currentState.getValue(var), BooleanValue.TOP, false);
		final Function<IBoogieVar, Triple<VALUE, BooleanValue, Boolean>> boolFunction =
				var -> new Triple<>(mNonrelationalValueFactory.createTopValue(), currentState.getBooleanValue(var),
						true);

		final Triple<VALUE, BooleanValue, Boolean> valueTriple =
				TypeUtils.applyVariableFunction(varFunction, boolFunction, null, mVar);

		val = valueTriple.getFirst();
		if (valueTriple.getThird()) {
			returnBool = valueTriple.getSecond();
			mContainsBoolean = true;
		}

		if (val.isBottom() || returnBool.isBottom()) {
			returnList.add(new NonrelationalEvaluationResult<>(mNonrelationalValueFactory.createBottomValue(),
					BooleanValue.BOTTOM));
		} else {
			returnList.add(new NonrelationalEvaluationResult<>(val, returnBool));
		}

		assert !returnList.isEmpty();
		return returnList;
	}

	@Override
	public List<STATE> inverseEvaluate(final IEvaluationResult<VALUE> computedValue, final STATE currentState) {
		assert computedValue != null;
		assert currentState != null;

		final List<STATE> returnList = new ArrayList<>();

		if (mContainsBoolean) {
			returnList.add(currentState.setBooleanValue(mVar, computedValue.getBooleanValue()));
		} else {
			returnList.add(currentState.setValue(mVar, computedValue.getValue()));
		}

		return returnList;
	}

	@Override
	public void addSubEvaluator(final IEvaluator<VALUE, STATE, CodeBlock> evaluator) {
		throw new UnsupportedOperationException(
				"Cannot add a subevaluator to singleton variable expression evaluators.");
	}

	@Override
	public Set<IBoogieVar> getVarIdentifiers() {
		return mVariableSet;
	}

	@Override
	public boolean hasFreeOperands() {
		return false;
	}

	@Override
	public boolean containsBool() {
		return mContainsBoolean;
	}

	@Override
	public String toString() {
		return mVar.getGloballyUniqueId();
	}

	@Override
	public EvaluatorType getType() {
		return mType;
	}
}
