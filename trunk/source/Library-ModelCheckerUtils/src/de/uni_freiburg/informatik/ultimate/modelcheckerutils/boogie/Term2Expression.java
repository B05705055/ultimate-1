/*
 * Copyright (C) 2012-2015 Evren Ermis
 * Copyright (C) 2012-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE ModelCheckerUtils Library.
 * 
 * The ULTIMATE ModelCheckerUtils Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE ModelCheckerUtils Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE ModelCheckerUtils Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE ModelCheckerUtils Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE ModelCheckerUtils Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.boogie.BoogieVar;
import de.uni_freiburg.informatik.ultimate.boogie.DeclarationInformation;
import de.uni_freiburg.informatik.ultimate.boogie.DeclarationInformation.StorageClass;
import de.uni_freiburg.informatik.ultimate.boogie.ast.ArrayAccessExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.ArrayStoreExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Attribute;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BinaryExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BitVectorAccessExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BitvecLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BooleanLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.FunctionApplication;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IdentifierExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IfThenElseExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IntegerLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.QuantifierExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.RealLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.StringLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Trigger;
import de.uni_freiburg.informatik.ultimate.boogie.ast.UnaryExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.VarList;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BinaryExpression.Operator;
import de.uni_freiburg.informatik.ultimate.boogie.type.BoogieType;
import de.uni_freiburg.informatik.ultimate.core.model.models.ILocation;
import de.uni_freiburg.informatik.ultimate.core.model.models.IType;
import de.uni_freiburg.informatik.ultimate.logic.AnnotatedTerm;
import de.uni_freiburg.informatik.ultimate.logic.Annotation;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.ConstantTerm;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.LetTerm;
import de.uni_freiburg.informatik.ultimate.logic.QuantifiedFormula;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.BitvectorUtils;
import de.uni_freiburg.informatik.ultimate.util.datastructures.ScopedHashMap;

/**
 * Translates SMT Terms to Boogie Expressions. 
 * @author heizmann@informatik.uni-freiburg.de
 *
 */
public class Term2Expression implements Serializable {

	private static final long serialVersionUID = -4519646474900935398L;


	private final Script m_Script;
	
	private final ScopedHashMap<TermVariable, VarList> m_QuantifiedVariables =
			new ScopedHashMap<TermVariable, VarList>();

	
	private int m_freshIdentiferCounter = 0;


	private final TypeSortTranslator m_TypeSortTranslator;
	
	private final Boogie2SmtSymbolTable m_Boogie2SmtSymbolTable;
	

	
	
	public Term2Expression(TypeSortTranslator tsTranslation, 
			Boogie2SmtSymbolTable boogie2SmtSymbolTable) {

		m_TypeSortTranslator = tsTranslation;
		m_Boogie2SmtSymbolTable = boogie2SmtSymbolTable;
		m_Script = boogie2SmtSymbolTable.getScript();
	}

	Set<IdentifierExpression> m_freeVariables = new HashSet<IdentifierExpression>();
	
	private String getFreshIdenfier() {
		return "freshIdentifier" + m_freshIdentiferCounter++;
	}
	
	public Expression translate(Term term) {
		Expression result;
		if (term instanceof AnnotatedTerm) {
			result = translate( (AnnotatedTerm) term);
		}else if (term instanceof ApplicationTerm) {
			return translate( (ApplicationTerm) term);
		}else if (term instanceof ConstantTerm) {
			result = translate( (ConstantTerm) term);
		}else if (term instanceof LetTerm) {
			result = translate( (LetTerm) term);
		}else if (term instanceof QuantifiedFormula) {
			result = translate( (QuantifiedFormula) term);
		}else if (term instanceof TermVariable) {
			result = translate( (TermVariable) term);
		}else {
			throw new UnsupportedOperationException("unknown kind of Term");
		}
		assert (result != null);
		return result;
	}
	
	private Expression translate(AnnotatedTerm term) {
		throw new UnsupportedOperationException(
					"annotations not supported yet" + term);
	}
	
	private Expression translate(ApplicationTerm term) {
		FunctionSymbol symb = term.getFunction();
		IType type = m_TypeSortTranslator.getType(symb.getReturnSort());
		Term[] termParams = term.getParameters();
		if (symb.isIntern() && symb.getName().equals("select")) {
			return translateSelect(term);
		} else if (symb.isIntern() && symb.getName().equals("store")) {
			return translateStore(term);
		} else if (BitvectorUtils.isBitvectorConstant(symb)) {
			return translateBitvectorConstant(term);
		}
		Expression[] params = new Expression[termParams.length];
		for (int i=0; i<termParams.length; i++) {
			params[i] = translate(termParams[i]);
		}
		if (symb.getParameterSorts().length == 0) {
			if (term == m_Script.term("true")) {
				IType booleanType = m_TypeSortTranslator.getType(m_Script.sort("Bool"));
				return new BooleanLiteral(null, booleanType, true);
			}
			if (term == m_Script.term("false")) {
				IType booleanType = m_TypeSortTranslator.getType(m_Script.sort("Bool"));
				return new BooleanLiteral(null, booleanType, false);
			}
			BoogieConst boogieConst = m_Boogie2SmtSymbolTable.getBoogieConst(term);
			if (boogieConst != null) {
				IdentifierExpression ie = new IdentifierExpression(null, 
						m_TypeSortTranslator.getType(term.getSort()),
						boogieConst.getIdentifier(),
						new DeclarationInformation(StorageClass.GLOBAL, null));
				return ie;
			} else {
				throw new IllegalArgumentException();
			}
		} else if (symb.getName().equals("ite")) {
				return new IfThenElseExpression(null, type, params[0], params[1], params[2]); 
		} else if (symb.isIntern()) {
			if (symb.getParameterSorts().length > 0 && BitvectorUtils.isBitvectorSort(symb.getParameterSorts()[0])
					&& !symb.getName().equals("=") && !symb.getName().equals("distinct")) {
				if (symb.getName().equals("extract")) {
					return translateBitvectorAccess(type, term);
				} else if (m_Boogie2SmtSymbolTable.getSmtFunction2BoogieFunction().containsKey(symb.getName())) {
					return translateWithSymbolTable(symb, type, termParams); 
				} else {
					throw new UnsupportedOperationException("translation of " + symb + 
							" not yet implemented, please contact Matthias");
				}
			} else if (symb.getParameterSorts().length == 1) {
				if (symb.getName().equals("not")) {
					Expression param = translate(term.getParameters()[0]);
					return new UnaryExpression(null, type, de.uni_freiburg.informatik.ultimate.boogie.ast.UnaryExpression.Operator.LOGICNEG,
							param);
				} else if (symb.getName().equals("-")) {
					Expression param = translate(term.getParameters()[0]);
					return new UnaryExpression(null, type, de.uni_freiburg.informatik.ultimate.boogie.ast.UnaryExpression.Operator.ARITHNEGATIVE,
							param);
				}else {
					throw new IllegalArgumentException("unknown symbol " + symb);
				}
			}
			else {
				if (symb.getName().equals("xor")) {
					return xor(params);
				} else if (symb.getName().equals("mod")) {
					return mod(params);
				}
				Operator op = getBinaryOperator(symb);
				if (symb.isLeftAssoc()) {
					return leftAssoc(op, type, params);
				} else if (symb.isRightAssoc()) {
					return rightAssoc(op, type, params);
				} else if (symb.isChainable()) {
					return chainable(op, type, params);
				} else if (symb.isPairwise()) {
					return pairwise(op, type, params);
				} else {
					throw new UnsupportedOperationException("don't know symbol" +
							" which is neither leftAssoc, rightAssoc, chainable, or pairwise.");
				}
			}
		} else if (m_Boogie2SmtSymbolTable.getSmtFunction2BoogieFunction().containsKey(symb.getName())) {
			return translateWithSymbolTable(symb, type, termParams); 
		} else {
			throw new UnsupportedOperationException("translation of " + symb + 
					" not yet implemented, please contact Matthias");
		}
	}

	private Expression translateBitvectorAccess(IType type, ApplicationTerm term) {
		assert term.getFunction().getName().equals("extract") : "no extract";
		assert term.getParameters().length == 1;
		assert term.getFunction().getIndices().length == 2;
		Expression bitvector = translate(term.getParameters()[0]);
		int start = term.getFunction().getIndices()[1].intValueExact();
		int end = term.getFunction().getIndices()[0].intValueExact();
		return new BitVectorAccessExpression(null, type, bitvector, end, start);
	}

	/**
	 * Use symbol table to translate a SMT function application into a
	 * Boogie function application.
	 */
	private Expression translateWithSymbolTable(FunctionSymbol symb, IType type, Term[] termParams) {
		String identifier = m_Boogie2SmtSymbolTable.getSmtFunction2BoogieFunction().get(symb.getName());
		Expression[] arguments = new Expression[termParams.length];
		for (int i=0; i<termParams.length; i++) {
			arguments[i] = translate(termParams[i]);
		}
		return new FunctionApplication(null,type, identifier, arguments);
	}
	
	/**
	 * Translate term in case it is a bitvector constant as defined as an 
	 * extension of the BV logic.
	 */
	private Expression translateBitvectorConstant(ApplicationTerm term) {
		assert term.getSort().getIndices().length == 1;
		String name = term.getFunction().getName();
		assert name.startsWith("bv");
		String decimalValue = name.substring(2, name.length());
		IType type = m_TypeSortTranslator.getType(term.getSort());
		BigInteger length = term.getSort().getIndices()[0];
		return new BitvecLiteral(null, type , decimalValue, length.intValue());
	}

	private Expression mod(Expression[] params) {
		if (params.length != 2) {
			throw new AssertionError("mod has two parameters");
		}
		return new BinaryExpression(null, BoogieType.intType, Operator.ARITHMOD, params[0], params[1]);
	}

	private ArrayStoreExpression translateStore(ApplicationTerm term) {
		Expression array = translate(term.getParameters()[0]);
		Expression index = translate(term.getParameters()[1]);
		Expression[] indices = { index };
		Expression value = translate(term.getParameters()[2]);
		return new ArrayStoreExpression(null, array, indices, value);
	}

	/**
	 * Translate a single select expression to an ArrayAccessExpression.
	 * If we have nested select expressions this leads to nested
	 * ArrayAccessExpressions, hence arrays which do not occur in the boogie
	 * program. 
	 */
	private ArrayAccessExpression translateSelect(ApplicationTerm term) {
		Expression array = translate(term.getParameters()[0]);
		Expression index = translate(term.getParameters()[1]);
		Expression[] indices = { index };
		return new ArrayAccessExpression(null, array, indices);
	}

	/**
	 * Translate a nested sequence of select expressions to a single 
	 * ArrayAccessExpression. (see translateSelect why this might be useful) 
	 */
	private ArrayAccessExpression translateArray(ApplicationTerm term) {
		List<Expression> reverseIndices = new ArrayList<Expression>();
		while (term.getFunction().getName().equals("select") && 
				(term.getParameters()[0] instanceof ApplicationTerm)) {
			assert (term.getParameters().length == 2);
			Expression index = translate(term.getParameters()[1]);
			reverseIndices.add(index);
			term = (ApplicationTerm) term.getParameters()[0];
		}
		assert (term.getParameters().length == 2);
		Expression index = translate(term.getParameters()[1]);
		reverseIndices.add(index);

		Expression array = translate(term.getParameters()[0]);
		Expression[] indices = new Expression[reverseIndices.size()];
		for (int i=0; i<indices.length; i++) {
			indices[i] = reverseIndices.get(indices.length-1-i);
		}
		return new ArrayAccessExpression(null, array, indices);
	}


	private Expression translate(ConstantTerm term) {
		Object value = term.getValue();
		IType type = m_TypeSortTranslator.getType(term.getSort());
		if (term.getSort().getRealSort().getName().equals("BitVec")) {
			BigInteger[] indices = term.getSort().getIndices();
			if (indices.length !=1) {
				throw new AssertionError("BitVec has exactly one index");
			}
			int length = indices[0].intValue();
			BigInteger decimalValue;
			if (value.toString().startsWith("#x")) {
				decimalValue = new BigInteger(value.toString().substring(2), 16);
			} else if (value.toString().startsWith("#b")) {
				decimalValue = new BigInteger(value.toString().substring(2), 2);
			} else {
				throw new UnsupportedOperationException(
						"only hexadecimal values and boolean values supported yet");			
			}
			return new BitvecLiteral(null, type, String.valueOf(decimalValue), length);
		}
		if (value instanceof String) {
			return new StringLiteral(null, type, value.toString());
		} else if (value instanceof BigInteger) {
			return new IntegerLiteral(null, type, value.toString());
		} else if (value instanceof BigDecimal) {
			return new RealLiteral(null, type, value.toString());
		} else if (value instanceof Rational) {
			if (term.getSort().getName().equals("Int")) {
				return new IntegerLiteral(null, type, value.toString());
			} else if (term.getSort().getName().equals("Real")) {
				return new RealLiteral(null, type, value.toString());
			} else {
				throw new UnsupportedOperationException("unknown Sort");
			}
		} else {
			throw new UnsupportedOperationException("unknown kind of Term");
		}
	}
	
	private Expression translate(LetTerm term) {
		throw new IllegalArgumentException("unlet Term first");
	}
	
	private Expression translate(QuantifiedFormula term) {
		m_QuantifiedVariables.beginScope();
		VarList[] parameters = new VarList[term.getVariables().length];
		int offset = 0;
		for (TermVariable tv : term.getVariables()) {
			IType type = m_TypeSortTranslator.getType(tv.getSort());
			String[] identifiers = { tv.getName() };
			//FIXME: Matthias: How can I get the ASTType of type?
			VarList varList = new VarList(null, identifiers, null);
			parameters[offset] = varList;
			m_QuantifiedVariables.put(tv, varList);
			offset++;
		}
		IType type = m_TypeSortTranslator.getType(term.getSort());
		assert (term.getQuantifier() == QuantifiedFormula.FORALL || 
							term.getQuantifier() == QuantifiedFormula.EXISTS);
		boolean isUniversal = term.getQuantifier() == QuantifiedFormula.FORALL;
		String[] typeParams = new String[0];
		Attribute[] attributes;
		Term subTerm = term.getSubformula();
		if (subTerm instanceof AnnotatedTerm) {
				assert ((AnnotatedTerm) subTerm).getAnnotations()[0].getKey().equals(":pattern");
			Annotation[] annotations = ((AnnotatedTerm) subTerm).getAnnotations();
			//FIXME: does not have to be the case, allow several annotations
			assert (annotations.length == 1) : "expecting only one annotation at a time";
			Annotation annotation = annotations[0];
			Object value = annotation.getValue();
			assert (value instanceof Term[]) : "expecting Term[]" + value;
			Term[] pattern = (Term[]) value;
			subTerm = ((AnnotatedTerm) subTerm).getSubterm();
			Expression[] triggers = new Expression[pattern.length];
			for (int i=0; i<pattern.length; i++) {
				triggers[i] = translate(pattern[i]);
			}
			Trigger trigger = new Trigger(null, triggers);
			attributes = new Attribute[1];
			attributes[0] = trigger;
		} else {
			attributes = new Attribute[0];			
		}
		Expression subformula = translate(subTerm);
		QuantifierExpression result = new QuantifierExpression(null, type, 
					isUniversal, typeParams, parameters, attributes, subformula);
		m_QuantifiedVariables.endScope();
		return result;
	}
	
	private Expression translate(TermVariable term) {
		Expression result;
		IType type = m_TypeSortTranslator.getType(term.getSort());
		if (m_QuantifiedVariables.containsKey(term)) {
			VarList varList = m_QuantifiedVariables.get(term);
			assert varList.getIdentifiers().length == 1;
			String id = varList.getIdentifiers()[0];
			result = new IdentifierExpression(null, type, id,
					new DeclarationInformation(StorageClass.QUANTIFIED, null));
		} else if (m_Boogie2SmtSymbolTable.getBoogieVar(term) == null) {
			//Case where term contains some auxilliary variable that was 
			//introduced during model checking. 
			//TODO: Matthias: I think we want closed expressions, we should
			//quantify auxilliary variables
			result = new IdentifierExpression(null, type, getFreshIdenfier(),
					new DeclarationInformation(StorageClass.QUANTIFIED, null));
			m_freeVariables.add((IdentifierExpression) result);
		}
		else {
			BoogieVar bv = m_Boogie2SmtSymbolTable.getBoogieVar(term);
			ILocation loc = m_Boogie2SmtSymbolTable.getAstNode(bv).getLocation();
			DeclarationInformation declInfo = 
					m_Boogie2SmtSymbolTable.getDeclarationInformation(bv);
			result = new IdentifierExpression(loc, type, bv.getIdentifier(), 
					declInfo);
			if (bv.isOldvar()) {
				assert(bv.isGlobal());
				result = new UnaryExpression(loc, type, 
						UnaryExpression.Operator.OLD, result);
			}
		}
		return result;
	}


	private Operator getBinaryOperator(FunctionSymbol symb) {
		if (symb.getName().equals("and")) {
			return Operator.LOGICAND;
		}
		else if (symb.getName().equals("or")) {
			return Operator.LOGICOR;
		}
		else if (symb.getName().equals("=>")) {
			return Operator.LOGICIMPLIES;
		}
		else if (symb.getName().equals("=") && 
				symb.getParameterSort(0).getName().equals("bool")) {
			return Operator.LOGICIFF;
		}
		else if (symb.getName().equals("=")) {
			return Operator.COMPEQ;
		}
		else if (symb.getName().equals("distinct")) {
			return Operator.COMPNEQ;
		}
		else if (symb.getName().equals("<=")) {
			return Operator.COMPLEQ;
		}
		else if (symb.getName().equals(">=")) {
			return Operator.COMPGEQ;
		}
		else if (symb.getName().equals("<")) {
			return Operator.COMPLT;
		}
		else if (symb.getName().equals(">")) {
			return Operator.COMPGT;
		}
		else if (symb.getName().equals("+")) {
			return Operator.ARITHPLUS;
		}
		else if (symb.getName().equals("-")) {
			return Operator.ARITHMINUS;
		}
		else if (symb.getName().equals("*")) {
			return Operator.ARITHMUL;
		}
		else if (symb.getName().equals("/")) {
			return Operator.ARITHDIV;
		}
		else if (symb.getName().equals("div")) {
			return Operator.ARITHDIV;
		}
		else if (symb.getName().equals("mod")) {
			return Operator.ARITHMOD;
		}
		else if (symb.getName().equals("ite")) {
			throw new UnsupportedOperationException("not yet implemented");
		}
		else if (symb.getName().equals("abs")) {
			throw new UnsupportedOperationException("not yet implemented");
		}		
		else {
			throw new IllegalArgumentException("unknown symbol " + symb);
		}
	}

	private Expression leftAssoc(Operator op, IType type, Expression[] params) {
		Expression result = params[0];
		for (int i=0; i<params.length-1; i++) {
			result = new BinaryExpression(null, type, op, result, params[i+1]);
		}
		return result;
	}
	
	
	private Expression rightAssoc(Operator op, IType type, Expression[] params) {
		Expression result = params[params.length-1];
		for (int i=params.length-1; i>0; i--) {
			result = new BinaryExpression(null, type, op, params[i-1],result);
		}
		return result;
	}
	
	private Expression chainable(Operator op, IType type, Expression[] params) {
		assert(type == BoogieType.boolType);
		Expression result = new BinaryExpression(null, type, op, params[0], params[1]);
		Expression chain;
		for (int i=1;i<params.length-1; i++) {
			chain = new BinaryExpression(null, type, op, params[i], params[i+1]);
			result = new BinaryExpression(null, BoogieType.boolType, Operator.LOGICAND, result, chain);
		}
		return result;
	}

	private Expression pairwise(Operator op, IType type, Expression[] params) {
		assert(type == BoogieType.boolType);
		Expression result = new BinaryExpression(null, type, op, params[0], params[1]);
		Expression neq;
			for (int i=0; i<params.length-1; i++) {
				for (int j=i+1; j<params.length-1; j++) {
					if (i==0 && j==1) {
						continue;
					}
					neq = new BinaryExpression(null, type, op, params[j], params[j+1]);
					result = new BinaryExpression(null, BoogieType.boolType, Operator.LOGICAND, result, neq);
				}
			}
		return result;
	}
	
	
	private Expression xor(Expression[] params) {
		IType type = BoogieType.boolType;
		Operator iff = Operator.LOGICIFF;
		UnaryExpression.Operator neg = UnaryExpression.Operator.LOGICNEG;
		Expression result = params[0];
		for (int i=0; i<params.length-1; i++) {
			result = new BinaryExpression(null, type, iff, params[i+1],result);
			result = new UnaryExpression(null, type, neg, result);
		}
		return result;
	}
	



}
