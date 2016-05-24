/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2008-2015 Jochen Hoenicke (hoenicke@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE BoogiePreprocessor plug-in.
 * 
 * The ULTIMATE BoogiePreprocessor plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE BoogiePreprocessor plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE BoogiePreprocessor plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE BoogiePreprocessor plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE BoogiePreprocessor plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.boogie.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Stack;

import de.uni_freiburg.informatik.ultimate.boogie.ast.ASTType;
import de.uni_freiburg.informatik.ultimate.boogie.ast.ArrayType;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Declaration;
import de.uni_freiburg.informatik.ultimate.boogie.ast.NamedType;
import de.uni_freiburg.informatik.ultimate.boogie.ast.PrimitiveType;
import de.uni_freiburg.informatik.ultimate.boogie.ast.StructType;
import de.uni_freiburg.informatik.ultimate.boogie.ast.TypeDeclaration;
import de.uni_freiburg.informatik.ultimate.boogie.type.BoogieType;
import de.uni_freiburg.informatik.ultimate.boogie.type.TypeConstructor;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;

public class TypeManager {

	private ILogger mLogger;

	private HashMap<String, TypeConstructor> typeConstructors = new HashMap<String, TypeConstructor>();
	private HashMap<String, TypeDeclaration> declarations = new HashMap<String, TypeDeclaration>();
	private Stack<String> visiting = new Stack<String>();
	private Stack<TypeParameters> typeParamScopes = new Stack<TypeParameters>();

	public TypeManager(Declaration[] decls, ILogger logger) {
		mLogger = logger;
		for (Declaration d : decls) {
			if (d instanceof TypeDeclaration) {
				TypeDeclaration td = (TypeDeclaration) d;
				declarations.put(td.getIdentifier(), td);
			}
		}
	}

	public void pushTypeScope(TypeParameters typeParams) {
		typeParamScopes.push(typeParams);
	}

	public void popTypeScope() {
		typeParamScopes.pop();
	}

	public BoogieType getPrimitiveType(String typeName) {
		if (typeName.equals("int"))
			return BoogieType.TYPE_INT;
		else if (typeName == "real")
			return BoogieType.TYPE_REAL;
		else if (typeName == "bool")
			return BoogieType.TYPE_BOOL;
		else if (typeName.startsWith("bv")) {
			int length = Integer.parseInt(typeName.substring(2));
			return BoogieType.createBitvectorType(length);
		} else {
			mLogger.fatal("getPrimitiveType called with unknown type " + typeName + "!");
			return BoogieType.TYPE_ERROR;
		}
	}

	public BoogieType resolveNamedType(NamedType type, boolean markUsed) {
		String name = type.getName();
		int numParam = type.getTypeArgs().length;

		ListIterator<TypeParameters> it = typeParamScopes.listIterator(typeParamScopes.size());
		int increment = 0;
		while (it.hasPrevious()) {
			TypeParameters tp = it.previous();
			BoogieType placeholderType = tp.findType(name, increment, markUsed);
			if (placeholderType != null) {
				if (numParam != 0) {
					mLogger.error("Bounded type " + name + " used with arguments.");
					return BoogieType.TYPE_ERROR;
				}
				return placeholderType;
			}
			increment += tp.getCount();
		}

		if (!typeConstructors.containsKey(name)) {
			TypeDeclaration decl = declarations.get(name);
			if (decl == null) {
				mLogger.error("Type " + name + " is never defined.");
				return BoogieType.TYPE_ERROR;
			}
			resolve(decl);
		}
		TypeConstructor tc = typeConstructors.get(name);
		if (tc == null) /* cyclic definition, already reported */
			return BoogieType.TYPE_ERROR;

		if (tc.getParamCount() != numParam) {
			mLogger.error("Type " + name + " used with wrong number of arguments.");
			return BoogieType.TYPE_ERROR;
		}
		BoogieType[] typeArgs = new BoogieType[numParam];
		for (int i : tc.getParamOrder()) {
			typeArgs[i] = resolveType(type.getTypeArgs()[i], markUsed);
		}
		for (int i = 0; i < numParam; i++) {
			/*
			 * Resolve the other type arguments without marking place holders as
			 * used. Place holders are actually instantiated as tError.
			 */
			if (typeArgs[i] == null)
				typeArgs[i] = resolveType(type.getTypeArgs()[i], false);
		}
		return BoogieType.createConstructedType(tc, typeArgs);
	}

	public BoogieType resolveArrayType(ArrayType type, boolean markUsed) {
		TypeParameters typeParams = new TypeParameters(type.getTypeParams());
		pushTypeScope(typeParams);
		int numIndices = type.getIndexTypes().length;
		BoogieType[] indexTypes = new BoogieType[numIndices];
		for (int i = 0; i < numIndices; i++) {
			indexTypes[i] = resolveType(type.getIndexTypes()[i], markUsed);
		}
		if (!typeParams.fullyUsed())
			mLogger.error("ArrayType generics not used in index types: " + type);
		BoogieType resultType = resolveType(type.getValueType(), markUsed);
		popTypeScope();

		return BoogieType.createArrayType(type.getTypeParams().length, indexTypes, resultType);
	}

	private BoogieType resolveStructType(StructType type, boolean markUsed) {
		ArrayList<String> names = new ArrayList<String>(type.getFields().length);
		ArrayList<BoogieType> types = new ArrayList<BoogieType>(type.getFields().length);

		for (int i = 0; i < type.getFields().length; i++) {
			BoogieType fieldType = resolveType(type.getFields()[i].getType(), markUsed);
			for (String id : type.getFields()[i].getIdentifiers()) {
				names.add(id);
				types.add(fieldType);
			}
		}
		String[] fNames = names.toArray(new String[names.size()]);
		BoogieType[] fTypes = types.toArray(new BoogieType[types.size()]);
		return BoogieType.createStructType(fNames, fTypes);
	}

	public BoogieType resolveType(ASTType type, boolean markUsed) {
		BoogieType boogieType;
		if (type instanceof PrimitiveType)
			boogieType = getPrimitiveType(((PrimitiveType) type).getName());
		else if (type instanceof NamedType)
			boogieType = resolveNamedType((NamedType) type, markUsed);
		else if (type instanceof ArrayType)
			boogieType = resolveArrayType((ArrayType) type, markUsed);
		else if (type instanceof StructType)
			boogieType = resolveStructType((StructType) type, markUsed);
		else {
			mLogger.fatal("Unknown ASTType " + type);
			boogieType = BoogieType.TYPE_ERROR;
		}
		type.setBoogieType(boogieType);
		return boogieType;
	}

	public BoogieType resolveType(ASTType type) {
		return resolveType(type, true);
	}

	public void resolve(TypeDeclaration td) {
		if (visiting.contains(td.getIdentifier())) {
			mLogger.fatal("Cyclic type definition: " + visiting);
			typeConstructors.put(td.getIdentifier(), null);
		}
		visiting.push(td.getIdentifier());
		String name = td.getIdentifier();
		String[] typeParams = td.getTypeParams();
		BoogieType synonym = null;
		int[] order;
		if (td.getSynonym() != null) {
			TypeParameters tp = new TypeParameters(typeParams, true);
			pushTypeScope(tp);
			synonym = resolveType(td.getSynonym());
			order = new int[tp.getNumUsed()];
			System.arraycopy(tp.getOrder(), 0, order, 0, order.length);
			popTypeScope();
		} else {
			order = new int[typeParams.length];
			for (int i = 0; i < order.length; i++)
				order[i] = i;
		}
		visiting.pop();
		typeConstructors.put(name, new TypeConstructor(name, td.isFinite(), typeParams.length, order, synonym));
	}

	public void init() {
		for (TypeDeclaration td : declarations.values()) {
			if (typeConstructors.containsKey(td.getIdentifier()))
				continue;
			resolve(td);
		}
	}

}
