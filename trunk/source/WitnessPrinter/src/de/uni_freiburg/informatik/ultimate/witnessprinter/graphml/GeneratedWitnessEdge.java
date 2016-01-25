/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
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
package de.uni_freiburg.informatik.ultimate.witnessprinter.graphml;

import java.math.BigDecimal;

import de.uni_freiburg.informatik.ultimate.result.AtomicTraceElement;
import de.uni_freiburg.informatik.ultimate.result.AtomicTraceElement.StepInfo;
import de.uni_freiburg.informatik.ultimate.result.model.IBacktranslationValueProvider;
import de.uni_freiburg.informatik.ultimate.result.model.IProgramExecution.ProgramState;

/**
 * 
 * @author dietsch@informatik.uni-freiburg.de
 * 
 */
public class GeneratedWitnessEdge<TE, E> {

	private final String mId;
	private final AtomicTraceElement<TE> mATE;
	private final ProgramState<E> mState;
	private final IBacktranslationValueProvider<TE, E> mStringProvider;

	GeneratedWitnessEdge(final AtomicTraceElement<TE> traceElement, final ProgramState<E> state,
			final IBacktranslationValueProvider<TE, E> stringProvider, long currentEdgeId) {
		assert stringProvider != null;
		mStringProvider = stringProvider;
		mId = "E" + String.valueOf(currentEdgeId);
		mATE = traceElement;
		mState = state;
	}

	public boolean isDummy() {
		return mATE == null && mState == null;
	}

	public String getName() {
		return mId;
	}

	public boolean hasAssumption() {
		return mState != null;
	}

	public boolean isInitial() {
		return mState != null && mATE == null;
	}

	public boolean hasStep() {
		return mATE != null;
	}

	public String getControl() {
		if (!hasStep()) {
			return null;
		}
		if (mATE.hasStepInfo(StepInfo.CONDITION_EVAL_FALSE)) {
			return "condition-false";
		} else if (mATE.hasStepInfo(StepInfo.CONDITION_EVAL_FALSE)) {
			return "condition-true";
		} else {
			return null;
		}
	}

	public String getStartLineNumber() {
		if (!hasStep()) {
			return null;
		}
		return String.valueOf(mStringProvider.getStartLineNumberFromStep(mATE.getStep()));
	}

	public String getEndLineNumber() {
		if (!hasStep()) {
			return null;
		}
		return String.valueOf(mStringProvider.getEndLineNumberFromStep(mATE.getStep()));
	}

	public String getAssumption() {
		if (!hasAssumption()) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (E var : mState.getVariables()) {
			for (E val : mState.getValues(var)) {
				appendValidExpression(var, val, sb);
			}
		}
		if (sb.length() > 0) {
			return sb.toString();
		}
		return null;
	}

	public String getEnterFunction() {
		if (!hasStep()) {
			return null;
		}
		return null;
	}

	public String getReturnFunction() {
		if (!hasStep()) {
			return null;
		}
		return null;
	}

	public String getSourceCode() {
		if (!hasStep()) {
			return null;
		}
		final String stepAsString = mStringProvider.getStringFromStep(mATE.getStep());
		final StringBuilder sb = new StringBuilder();

		boolean isConditional = (mATE.hasStepInfo(StepInfo.CONDITION_EVAL_FALSE)
				|| mATE.hasStepInfo(StepInfo.CONDITION_EVAL_TRUE));

		if (isConditional) {
			sb.append("[");
		}
		if (mATE.hasStepInfo(StepInfo.CONDITION_EVAL_FALSE)) {
			sb.append("!(");
			sb.append(stepAsString);
			sb.append(")");
		} else {
			sb.append(stepAsString);
		}

		if (isConditional) {
			sb.append("]");
		}

		return sb.toString();
	}

	private void appendValidExpression(E var, E val, StringBuilder sb) {

		final String varStr = mStringProvider.getStringFromExpression(var);
		final String valStr = mStringProvider.getStringFromExpression(val);

		if (varStr.contains("\\") || varStr.contains("&")) {
			// is something like read, old, etc.
			return;
		}

		try {
			new BigDecimal(valStr);
		} catch (Exception ex) {
			// this is no valid number literal, maybe its true or false?
			if (!valStr.equalsIgnoreCase("true") && !valStr.equalsIgnoreCase("false")) {
				// nope, give up
				return;
			}
		}

		sb.append(varStr);
		sb.append("==");
		sb.append(valStr);
		sb.append(";");
	}

}
