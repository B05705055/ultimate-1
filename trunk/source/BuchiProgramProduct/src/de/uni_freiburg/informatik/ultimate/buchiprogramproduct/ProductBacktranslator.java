/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.buchiprogramproduct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.core.lib.translation.DefaultTranslator;
import de.uni_freiburg.informatik.ultimate.core.model.translation.AtomicTraceElement;
import de.uni_freiburg.informatik.ultimate.core.model.translation.IProgramExecution;
import de.uni_freiburg.informatik.ultimate.core.model.translation.IProgramExecution.ProgramState;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RCFGEdge;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.util.RcfgProgramExecution;

/**
 *
 * @author dietsch@informatik.uni-freiburg.de
 *
 */
public class ProductBacktranslator extends DefaultTranslator<RCFGEdge, RCFGEdge, Term, Term, String, String> {

	private final HashMap<RCFGEdge, RCFGEdge> mEdgeMapping;

	public ProductBacktranslator(final Class<RCFGEdge> traceElementType, final Class<Term> expressionType) {
		super(traceElementType, traceElementType, expressionType, expressionType);
		mEdgeMapping = new HashMap<>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public IProgramExecution<RCFGEdge, Term>
			translateProgramExecution(final IProgramExecution<RCFGEdge, Term> programExecution) {

		Map<TermVariable, Boolean>[] oldBranchEncoders = null;
		if (programExecution instanceof RcfgProgramExecution) {
			oldBranchEncoders = ((RcfgProgramExecution) programExecution).getBranchEncoders();
		}

		final ArrayList<RCFGEdge> newTrace = new ArrayList<>();
		final Map<Integer, ProgramState<Term>> newValues = new HashMap<>();
		final ArrayList<Map<TermVariable, Boolean>> newBranchEncoders = new ArrayList<>();

		addProgramState(-1, newValues, programExecution.getInitialProgramState());

		for (int i = 0; i < programExecution.getLength(); ++i) {
			final AtomicTraceElement<RCFGEdge> currentATE = programExecution.getTraceElement(i);
			final RCFGEdge mappedEdge = mEdgeMapping.get(currentATE.getTraceElement());
			if (mappedEdge == null || !(mappedEdge instanceof CodeBlock)) {
				// skip this, its not worth it.
				continue;
			}
			newTrace.add(mappedEdge);
			addProgramState(i, newValues, programExecution.getProgramState(i));
			if (oldBranchEncoders != null) {
				newBranchEncoders.add(oldBranchEncoders[i]);
			}
		}

		return new RcfgProgramExecution(newTrace, newValues,
				newBranchEncoders.toArray(new Map[newBranchEncoders.size()]));
	}

	private void addProgramState(final int i, final Map<Integer, ProgramState<Term>> newValues,
			final ProgramState<Term> programState) {
		newValues.put(i, programState);
	}

	@Override
	public List<RCFGEdge> translateTrace(final List<RCFGEdge> trace) {
		return super.translateTrace(trace);
	}

	@Override
	public Term translateExpression(final Term expression) {
		return super.translateExpression(expression);
	}

	public void mapEdges(final RCFGEdge newEdge, final RCFGEdge originalEdge) {
		final RCFGEdge realOriginalEdge = mEdgeMapping.get(originalEdge);
		if (realOriginalEdge != null) {
			// this means we replaced an edge which we already replaced again
			// with something new, we have to map this to the real original
			mEdgeMapping.put(newEdge, realOriginalEdge);
		} else {
			mEdgeMapping.put(newEdge, originalEdge);
		}

	}

}
