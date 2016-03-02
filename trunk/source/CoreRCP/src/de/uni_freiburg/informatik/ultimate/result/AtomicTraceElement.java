/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE Core.
 * 
 * The ULTIMATE Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Core. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Core, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE Core grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.result;

import java.util.EnumSet;

import de.uni_freiburg.informatik.ultimate.core.util.IToString;

/**
 * An atomic trace element in the sense of a debugger trace of a program. It
 * consists of an {@link AtomicTraceElement#getTraceElement() trace element} ,
 * which is probably a statement of some program, and the currently evaluated
 * {@link AtomicTraceElement#getStep() part of this statement}.
 * 
 * This class is used to display an error trace for the user.
 * 
 * @author dietsch@informatik.uni-freiburg.de
 * 
 * @param <TE>
 *            The type of the trace element and the step.
 */
public class AtomicTraceElement<TE> {

	/**
	 * StepInfo provides additional information for
	 * {@link AtomicTraceElement#getStep()}.
	 * 
	 * This may be replaced by an actual object later, but for now it should be
	 * sufficient.
	 * 
	 * @author dietsch@informatik.uni-freiburg.de
	 * 
	 */
	public enum StepInfo {
		NONE("NONE"), CONDITION_EVAL_TRUE("COND TRUE"), CONDITION_EVAL_FALSE("COND FALSE"), PROC_CALL("CALL"), PROC_RETURN(
				"RET"), ARG_EVAL("ARG"), EXPR_EVAL("EXPR"), FUNC_CALL("FCALL");

		private final String mText;

		private StepInfo(final String text) {
			mText = text;
		}

		@Override
		public String toString() {
			return mText;
		}
	}

	private final TE mElement;
	private final TE mStep;
	private final IToString<TE> mToStringFunc;
	private EnumSet<AtomicTraceElement.StepInfo> mStepInfo;

	/**
	 * Creates an instance where the trace element is evaluated atomically (i.e.
	 * {@link #getTraceElement()} == {@link #getStep()}).
	 */
	public AtomicTraceElement(TE element) {
		this(element, element, StepInfo.NONE);
	}

	public AtomicTraceElement(TE element, IToString<TE> toStringProvider) {
		this(element, element, StepInfo.NONE, toStringProvider);
	}

	/**
	 * Creates an instance where the trace element is not necessarily evaluated
	 * atomically (i.e. {@link #getTraceElement()} != {@link #getStep()} is
	 * allowed)
	 * 
	 * @param element
	 * @param step
	 * @param info
	 *            provides additional information about the step, e.g. if its a
	 *            condition that evaluated to true or false, if it is a call or
	 *            a return, etc.
	 */
	public AtomicTraceElement(TE element, TE step, AtomicTraceElement.StepInfo info) {
		this(element, step, EnumSet.of(info));
	}

	public AtomicTraceElement(TE element, TE step, AtomicTraceElement.StepInfo info, IToString<TE> toStringProvider) {
		this(element, step, EnumSet.of(info), toStringProvider);
	}

	public AtomicTraceElement(TE element, TE step, EnumSet<AtomicTraceElement.StepInfo> info) {
		this(element, step, info, a -> a.toString());
	}

	public AtomicTraceElement(TE element, TE step, EnumSet<AtomicTraceElement.StepInfo> info,
			IToString<TE> toStringProvider) {
		assert element != null;
		assert step != null;
		mElement = element;
		mStep = step;
		mStepInfo = info;
		if (info.size() > 1 && info.contains(StepInfo.NONE)) {
			throw new IllegalArgumentException("You cannot combine NONE with other values");
		}
		if (toStringProvider == null) {
			throw new IllegalArgumentException("toStringProvider may not be null");
		}
		mToStringFunc = toStringProvider;
	}

	/**
	 * @return The statement which is currently executed. Is never null.
	 */
	public TE getTraceElement() {
		return mElement;
	}

	/**
	 * @return An expression or statement which is evaluated atomically as part
	 *         of the evaluation of {@link #getTraceElement()} or a statement
	 *         that is equal to {@link #getTraceElement()} when
	 *         {@link #getTraceElement()} itself is evaluated atomically.
	 * 
	 *         This is always a reference to some subtree of
	 *         {@link #getTraceElement()}.
	 */
	public TE getStep() {
		return mStep;
	}

	public boolean hasStepInfo(AtomicTraceElement.StepInfo info) {
		return mStepInfo.contains(info);
	}

	public EnumSet<AtomicTraceElement.StepInfo> getStepInfo() {
		return EnumSet.copyOf(mStepInfo);
	}

	@Override
	public String toString() {
		if (mStepInfo.contains(StepInfo.NONE)) {
			return mToStringFunc.toString(getTraceElement());
		} else {
			return String.format("%s  %s", getStepInfo(), mToStringFunc.toString(getStep()));
		}

	}
}
