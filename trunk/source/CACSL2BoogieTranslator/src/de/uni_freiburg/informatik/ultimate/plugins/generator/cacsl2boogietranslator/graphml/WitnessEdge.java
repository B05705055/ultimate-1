package de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.graphml;

import java.math.BigDecimal;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.CACSLLocation;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.CACSLProgramExecutionStringProvider;
import de.uni_freiburg.informatik.ultimate.result.IProgramExecution.AtomicTraceElement;
import de.uni_freiburg.informatik.ultimate.result.IProgramExecution.AtomicTraceElement.StepInfo;
import de.uni_freiburg.informatik.ultimate.result.IProgramExecution.ProgramState;

/**
 * 
 * @author dietsch@informatik.uni-freiburg.de
 * 
 */
public class WitnessEdge {

	private final String mId;
	private final AtomicTraceElement<CACSLLocation> mATE;
	private final ProgramState<IASTExpression> mState;

	WitnessEdge(AtomicTraceElement<CACSLLocation> traceElement, ProgramState<IASTExpression> state, long currentEdgeId) {
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
		return String.valueOf(mATE.getStep().getStartLine());
	}
	
	public String getEndLineNumber() {
		if (!hasStep()) {
			return null;
		}
		return String.valueOf(mATE.getStep().getEndLine());
	}

	public String getAssumption() {
		if (!hasAssumption()) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (IASTExpression var : mState.getVariables()) {
			for (IASTExpression val : mState.getValues(var)) {
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
		// if(mATE.hasStepInfo(StepInfo.PROC_CALL)){
		// CACSLLocation currentStep = mATE.getStep();
		// if (currentStep instanceof CLocation) {
		// IASTNode currentStepNode = ((CLocation) currentStep).getNode();
		// String str = currentStepNode.getRawSignature();
		// }
		// }
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
		CACSLProgramExecutionStringProvider stringProvider = new CACSLProgramExecutionStringProvider();
		String stepAsString = stringProvider.getStringFromStep(mATE.getStep());
		StringBuilder sb = new StringBuilder();

		boolean isConditional = (mATE.hasStepInfo(StepInfo.CONDITION_EVAL_FALSE) || mATE
				.hasStepInfo(StepInfo.CONDITION_EVAL_TRUE));

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

	private void appendValidExpression(IASTExpression var, IASTExpression val, StringBuilder sb) {

		String varStr = var.getRawSignature();
		String valStr = val.getRawSignature();

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
		sb.append("=");
		sb.append(valStr);
		sb.append(";");
	}

}
