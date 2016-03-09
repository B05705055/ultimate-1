/*
 * Copyright (C) 2013-2015 Alexander Nutz (nutz@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 Markus Lindenmann (lindenmm@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.cHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTDesignatedInitializer;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFieldDesignator;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.LocationFactory;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.ExpressionTranslation.AExpressionTranslation;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPointer;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CStruct;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CType;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CUnion;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.exception.IncorrectSyntaxException;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.exception.UnsupportedSyntaxException;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.ExpressionListRecResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.ExpressionResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.HeapLValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.LRValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.LocalLValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.RValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.Result;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.SFO;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.Dispatcher;
import de.uni_freiburg.informatik.ultimate.model.annotation.Overapprox;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Declaration;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.StructAccessExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.StructConstructor;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.StructLHS;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VariableDeclaration;
import de.uni_freiburg.informatik.ultimate.model.location.ILocation;

/**
 * Class that handles translation of Structs.
 * 
 * @authors Markus Lindenmann, Alexander Nutz, Matthias Heizmann
 * @date 12.10.2012
 * modified (a lot) by Alexander Nutz in later 2013/early 2014
 */
public class StructHandler {
	
	private final MemoryHandler m_MemoryHandler;
	private final TypeSizeAndOffsetComputer m_TypeSizeAndOffsetComputer;
	private final AExpressionTranslation m_ExpressionTranslation;
	
	

	public StructHandler(MemoryHandler memoryHandler, 
			TypeSizeAndOffsetComputer typeSizeAndOffsetComputer, 
			AExpressionTranslation expressionTranslation) {
		super();
		m_MemoryHandler = memoryHandler;
		m_TypeSizeAndOffsetComputer = typeSizeAndOffsetComputer;
		m_ExpressionTranslation = expressionTranslation;
	}


	/**
	 * Handle IASTFieldReference.
	 * 
	 * @param main
	 *            a reference to the main dispatcher.
	 * @param node
	 *            the node to translate.
	 * @param m_MemoryHandler 
	 * @return the translation results.
	 */
	public Result handleFieldReference(Dispatcher main, IASTFieldReference node) {
		ILocation loc = LocationFactory.createCLocation(node);
		String field = node.getFieldName().toString();
		
		ExpressionResult fieldOwner = (ExpressionResult) main.dispatch(node.getFieldOwner());

		LRValue newValue = null;
		Map<StructLHS, CType> unionFieldToCType = fieldOwner.unionFieldIdToCType;

		CType foType = fieldOwner.lrVal.getCType().getUnderlyingType();
		
		foType = (node.isPointerDereference() ?
				((CPointer)foType).pointsToType :
					foType);
		
		CStruct cStructType = (CStruct) foType.getUnderlyingType();
		CType cFieldType = cStructType.getFieldType(field);

		if (node.isPointerDereference()) {
			ExpressionResult rFieldOwnerRex = fieldOwner.switchToRValueIfNecessary(main, m_MemoryHandler, this, loc);
			Expression address = rFieldOwnerRex.lrVal.getValue();
			fieldOwner = new ExpressionResult(rFieldOwnerRex.stmt, new HeapLValue(address, rFieldOwnerRex.lrVal.getCType()), 
					rFieldOwnerRex.decl, rFieldOwnerRex.auxVars, rFieldOwnerRex.overappr);
		}

		if (fieldOwner.lrVal instanceof HeapLValue) {
			HeapLValue fieldOwnerHlv = (HeapLValue) fieldOwner.lrVal;

			//TODO: different calculations for unions
			Expression startAddress = fieldOwnerHlv.getAddress();
			Expression newStartAddressBase = null;
			Expression newStartAddressOffset = null;
			if (startAddress instanceof StructConstructor) {
				newStartAddressBase = ((StructConstructor) startAddress).getFieldValues()[0];
				newStartAddressOffset = ((StructConstructor) startAddress).getFieldValues()[1];
			} else {
				newStartAddressBase = MemoryHandler.getPointerBaseAddress(startAddress, loc);
				newStartAddressOffset = MemoryHandler.getPointerOffset(startAddress, loc);
			}
			Expression fieldOffset = m_TypeSizeAndOffsetComputer.constructOffsetForField(loc, cStructType, field);
			Expression sumOffset = m_ExpressionTranslation.constructArithmeticExpression(loc, 
					IASTBinaryExpression.op_plus, newStartAddressOffset, 
					m_ExpressionTranslation.getCTypeOfPointerComponents(), fieldOffset, m_ExpressionTranslation.getCTypeOfPointerComponents());
			Expression newPointer = MemoryHandler.constructPointerFromBaseAndOffset(
					newStartAddressBase, sumOffset, loc);
			newValue = new HeapLValue(newPointer, cFieldType);
		} else if (fieldOwner.lrVal instanceof RValue) {
			RValue rVal = (RValue) fieldOwner.lrVal;
			StructAccessExpression sexpr = new StructAccessExpression(loc, 
					rVal.getValue(), field);
			newValue = new RValue(sexpr, cFieldType);
		} else { 
			LocalLValue lVal = (LocalLValue) fieldOwner.lrVal;
			StructLHS slhs = new StructLHS(loc,
					lVal.getLHS(), field);
			newValue = new LocalLValue(slhs, cFieldType);
			
			//only here -- assuming the RValue case means that no write is taking place..
			if (foType instanceof CUnion) {
				if (unionFieldToCType == null)
					unionFieldToCType = new LinkedHashMap<StructLHS, CType>();
				for (String fieldId : ((CUnion) foType).getFieldIds()) {
					if (!fieldId.equals(field)) {
						StructLHS havocSlhs = new StructLHS(loc, lVal.getLHS(), fieldId);
						unionFieldToCType.put(havocSlhs, ((CUnion) foType).getFieldType(fieldId));
					}
				}
			}
		}
	
		return new ExpressionResult(fieldOwner.stmt, newValue, fieldOwner.decl, fieldOwner.auxVars, 
				fieldOwner.overappr, unionFieldToCType);
	}


	public Result readFieldInTheStructAtAddress(Dispatcher main,
			ILocation loc, int fieldIndex,
			Expression structAddress, CStruct structType) {
		Expression addressBaseOfFieldOwner;
		Expression addressOffsetOfFieldOwner;
		
		addressBaseOfFieldOwner = new StructAccessExpression(loc, 
				structAddress, SFO.POINTER_BASE);
		addressOffsetOfFieldOwner = new StructAccessExpression(loc, 
				structAddress, SFO.POINTER_OFFSET);

		Expression newOffset = computeStructFieldOffset(m_MemoryHandler, loc,
				fieldIndex, addressOffsetOfFieldOwner, structType);
		
		StructConstructor newPointer = 
				MemoryHandler.constructPointerFromBaseAndOffset(addressBaseOfFieldOwner, newOffset, loc);

		CType resultType = structType.getFieldTypes()[fieldIndex];

		ExpressionResult call = 
				m_MemoryHandler.getReadCall(newPointer, resultType);
		ArrayList<Statement> stmt = new ArrayList<Statement>();
		ArrayList<Declaration> decl = new ArrayList<Declaration>();
		Map<VariableDeclaration, ILocation> auxVars = 
				new LinkedHashMap<VariableDeclaration, ILocation>();
		List<Overapprox> overappr = new ArrayList<Overapprox>();
		stmt.addAll(call.stmt);
		decl.addAll(call.decl);
		auxVars.putAll(call.auxVars);
		overappr.addAll(call.overappr);
		ExpressionResult result = new ExpressionResult(stmt,
		        new RValue(call.lrVal.getValue(), resultType), decl, auxVars,
		        overappr);
		return result;
	}


	Expression computeStructFieldOffset(MemoryHandler memoryHandler,
			ILocation loc, int fieldIndex, Expression addressOffsetOfFieldOwner,
			CStruct structType) {
		if (structType == null || !(structType instanceof CStruct)) {
			String msg = "Incorrect or unexpected field owner!";
			throw new IncorrectSyntaxException(loc, msg);
		}
		boolean fieldOffsetIsZero = isOffsetZero(structType, fieldIndex);
		if (fieldOffsetIsZero) {
			return addressOffsetOfFieldOwner;
		} else {
			Expression fieldOffset = m_TypeSizeAndOffsetComputer.
					constructOffsetForField(loc, structType, fieldIndex);
			Expression result = m_ExpressionTranslation.constructArithmeticExpression(
					loc, 
					IASTBinaryExpression.op_plus, addressOffsetOfFieldOwner, 
					m_TypeSizeAndOffsetComputer.getSize_T(), fieldOffset, m_TypeSizeAndOffsetComputer.getSize_T());
			return result;
		}
	}

	private boolean isOffsetZero(CStruct cStruct, int fieldIndex) {
		return (fieldIndex == 0) || (cStruct instanceof CUnion);
	}

//
//	public static IdentifierExpression getStructOrUnionOffsetConstantExpression(
//			ILocation loc, MemoryHandler memoryHandler, String fieldId, CType structCType) {
//		String offset = SFO.OFFSET + structCType.toString() + "~" + fieldId;
//		IdentifierExpression additionalOffset = new IdentifierExpression(loc, offset);
//		memoryHandler.calculateSizeOf(loc, structCType);//needed such that offset constants are declared
//		return additionalOffset;
//	}

	/**
	 * Handle IASTDesignatedInitializer.
	 * 
	 * @param main
	 *            a reference to the main dispatcher.
	 * @param node
	 *            the node to translate.
	 * @return the translation result.
	 */
	public Result handleDesignatedInitializer(Dispatcher main,
			MemoryHandler memoryHandler, StructHandler structHandler,
			CASTDesignatedInitializer node) {
		ILocation loc = LocationFactory.createCLocation(node);
		assert node.getDesignators().length == 1;
		assert node.getDesignators()[0] instanceof CASTFieldDesignator;
		CASTFieldDesignator fr = (CASTFieldDesignator) node.getDesignators()[0];
		String id = fr.getName().toString();
		Result r = main.dispatch(node.getOperand());
		if (r instanceof ExpressionListRecResult) {
			ExpressionListRecResult relr = (ExpressionListRecResult) r;
			if (!relr.list.isEmpty()) {
				assert relr.stmt.isEmpty();
				//                assert relr.expr == null;//TODO??
				assert relr.lrVal == null;
				assert relr.decl.isEmpty();
				ExpressionListRecResult named = new ExpressionListRecResult(id);
				named.list.addAll(relr.list);
				return named;
			}
			return new ExpressionListRecResult(id, relr.stmt, relr.lrVal,
					relr.decl, relr.auxVars, relr.overappr).switchToRValueIfNecessary(
					        main, memoryHandler, structHandler, loc);
		} else if (r instanceof ExpressionResult) {
			ExpressionResult rex = (ExpressionResult) r;
			return rex.switchToRValueIfNecessary(main, memoryHandler, structHandler, loc);
		} else {
			String msg = "Unexpected result";
			throw new UnsupportedSyntaxException(loc, msg);
		}
	}



}
