/*
 * Copyright (C) 2014-2015 Jan Leike (leike@informatik.uni-freiburg.de)
 * Copyright (C) 2014-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE LassoRanker Library.
 * 
 * The ULTIMATE LassoRanker Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE LassoRanker Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE LassoRanker Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE LassoRanker Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE LassoRanker Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors;

import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.TransFormulaLR;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.Util;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.IFreshTermVariableConstructor;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.normalForms.Dnf;


/**
 * Convert a formula into disjunctive normal form, i.e., a formula of the
 * form
 * 
 * <pre>OR ( AND ( NOT? inequality ) )</pre>
 * 
 * This includes a negation normal form (negations only occur before atoms).
 * 
 * @author Jan Leike
 */
public class DNF extends TransitionPreprocessor {
	private final IUltimateServiceProvider mServices;
	private final IFreshTermVariableConstructor mFreshTermVariableConstructor;
	
	public static final String s_Description = 
			"Transform into disjunctive normal form";
	
	public DNF(IUltimateServiceProvider services, 
			IFreshTermVariableConstructor freshTermVariableConstructor) {
		super();
		mServices = services;
		mFreshTermVariableConstructor = freshTermVariableConstructor;
	}
	
	@Override
	public String getDescription() {
		return s_Description;
	}
	
	@Override
	protected boolean checkSoundness(Script script, TransFormulaLR oldTF,
			TransFormulaLR newTF) {
		final Term old_term = oldTF.getFormula();
		final Term new_term = newTF.getFormula();
		return LBool.SAT != Util.checkSat(script,
				script.term("distinct", old_term, new_term));
	}
	
	@Override
	public TransFormulaLR process(Script script, TransFormulaLR tf) throws TermException {
		final Dnf dnf = new Dnf(script, mServices, mFreshTermVariableConstructor);
		tf.setFormula(dnf.transform(tf.getFormula()));
		return tf;
	}
}
