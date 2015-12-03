/*
 * Copyright (C) 2013-2015 Alexander Nutz (nutz@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 Markus Lindenmann (lindenmm@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Oleksii Saukh (saukho@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Stefan Wissert
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
/**
 * An example of a Type-Handler implementation.
 */
package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IArrayType;
import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTTypedefNameSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.c.CPointerType;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.LocationFactory;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.SymbolTable;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.ExpressionTranslation.AExpressionTranslation;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.InferredType;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.InferredType.Type;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CArray;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CEnum;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CFunction;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CNamed;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPointer;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.PRIMITIVE;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CStruct;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CType;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CUnion;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.exception.IncorrectSyntaxException;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.exception.UnsupportedSyntaxException;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.CDeclaration;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.DeclarationResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.DeclaratorResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.ExpressionResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.Result;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.SkipResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.TypesResult;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.BoogieASTUtil;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.SFO;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.Dispatcher;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.ITypeHandler;
import de.uni_freiburg.informatik.ultimate.model.acsl.ACSLNode;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.ASTType;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.ArrayType;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Attribute;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Declaration;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.NamedType;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.PrimitiveType;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.StructLHS;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.StructType;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.TypeDeclaration;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VarList;
import de.uni_freiburg.informatik.ultimate.model.location.ILocation;
import de.uni_freiburg.informatik.ultimate.util.LinkedScopedHashMap;

/**
 * @author Markus Lindenmann
 * @author Oleksii Saukh
 * @author Stefan Wissert
 * @date 07.02.2012
 * @author Alexander Nutz
 */
public class TypeHandler implements ITypeHandler {
    /**
     * Maps the cIdentifier of a struct, enumeration, or union (when this is
     *  implemented) to the ResultType that represents this type at the moment
     */
    private final LinkedScopedHashMap<String, TypesResult> m_DefinedTypes;
    /**
     * Undefined struct types.
     */
    private LinkedHashSet<String> m_IncompleteType;
    /**
     * counting levels of struct declaration.
     */
    private int structCounter;
    
    /**
     * Contains all primitive types that occurred in program.
     */
    private final Set<CPrimitive.PRIMITIVE> m_OccurredPrimitiveTypes = new HashSet<>();
    
    /**
     * if true we translate CPrimitives whose general type is INT to int.
     * If false we translate CPrimitives whose general type is INT to 
     * identically named types,
     */
    private final boolean m_UseIntForAllIntegerTypes;
    
    /**
     * States if an ASTNode for the pointer type was constructed and hence
     * this type has to be declared.
     */
	private boolean m_PointerTypeNeeded = false;
    
    

    public Set<CPrimitive.PRIMITIVE> getOccurredPrimitiveTypes() {
		return m_OccurredPrimitiveTypes;
	}

	public boolean useIntForAllIntegerTypes() {
		return m_UseIntForAllIntegerTypes;
	}

	/**
     * Constructor.
	 * @param useIntForAllIntegerTypes 
     */
    public TypeHandler(boolean useIntForAllIntegerTypes) {
    	this.m_UseIntForAllIntegerTypes = useIntForAllIntegerTypes;
        this.m_DefinedTypes = new LinkedScopedHashMap<String, TypesResult>();
        this.m_IncompleteType = new LinkedHashSet<String>();
    }

    @Override
    public boolean isStructDeclaration() {
        assert structCounter >= 0;
        return structCounter != 0;
    }
    
    /**
     * for svcomp2014 hack
     */
    public int getStructCounter() {
    	return structCounter;
    }

    @Override
    public Result visit(Dispatcher main, IASTNode node) {
        String msg = "TypeHandler: Not yet implemented: " + node.toString();
        ILocation loc = LocationFactory.createCLocation(node);
        throw new UnsupportedSyntaxException(loc, msg);
    }

    /**
     * @deprecated is not supported in this handler! Do not use!
     */
    @Override
    public Result visit(Dispatcher main, ACSLNode node) {
        throw new UnsupportedOperationException(
                "Implementation Error: use ACSL handler for " + node.getClass());
    }

    @Override
    public Result visit(Dispatcher main, IASTSimpleDeclSpecifier node) {
    	// we have model.boogie.ast.PrimitiveType, which should
    	// only contain BOOL, INT, REAL ...
    	ILocation loc = LocationFactory.createCLocation(node);
    	switch (node.getType()) {
    	case IASTSimpleDeclSpecifier.t_void: {
    		// there is no void in Boogie,
    		// so we simply have no result variable.
    		CPrimitive cvar = new CPrimitive(node);
    		return (new TypesResult(null, false, true, cvar));
    	}
    	case IASTSimpleDeclSpecifier.t_unspecified:
    	{
    		String msg = "unspecified type, defaulting to int";
    		main.warn(loc, msg);
    	}
    	case IASTSimpleDeclSpecifier.t_bool:
    	case IASTSimpleDeclSpecifier.t_char:
    	case IASTSimpleDeclSpecifier.t_int: {
    		// so int is also a primitive type
    		// NOTE: in a extended implementation we should
    		// handle here different types of int (short, long,...)
    		CPrimitive cvar = new CPrimitive(node);
    		return (new TypesResult(cPrimitive2asttype(loc, cvar), node.isConst(), false, cvar));
    	}
    	case IASTSimpleDeclSpecifier.t_double:
    	case IASTSimpleDeclSpecifier.t_float: {
    		// floating point number are not supported by Ultimate,
    		// somehow we treat it here as REALs
    		CPrimitive cvar = new CPrimitive(node);
    		return (new TypesResult(new PrimitiveType(loc, SFO.REAL), node.isConst(), false, cvar));
    	}
    	case IASTSimpleDeclSpecifier.t_typeof: {
    		/*
    		 * https://gcc.gnu.org/onlinedocs/gcc/Typeof.html :
    		 * The syntax of using of this keyword looks like sizeof, but the construct acts semantically like a type name defined with typedef.
    		 * There are two ways of writing the argument to typeof: with an expression or with a type. Here is an example with an expression:
    		 *     typeof (x[0](1))
    		 * This assumes that x is an array of pointers to functions; the type described is that of the values of the functions.
    		 * Here is an example with a typename as the argument:
    		 *     typeof (int *)
    		 * Here the type described is that of pointers to int.  
    		 */
    		Result opRes = main.dispatch(node.getDeclTypeExpression());
    		if (opRes instanceof ExpressionResult) {
    			CType cType = ((ExpressionResult) opRes).lrVal.getCType();
    			return new TypesResult(ctype2asttype(loc,  cType), node.isConst(), false, cType);
    		} else if (opRes instanceof DeclaratorResult) {
    			CType cType = ((DeclaratorResult) opRes).getDeclaration().getType();
    			return new TypesResult(ctype2asttype(loc,  cType), node.isConst(), false, cType);
    		}
    	}
    	default:
    		// long, long long, and short are the same as int, iff there are
    		// no restrictions / asserts in boogie
    		if (node.isLongLong() || node.isLong() || node.isShort()
    				|| node.isUnsigned()) {
    			CPrimitive cvar = new CPrimitive(node);
    			return (new TypesResult(new PrimitiveType(
    					loc, SFO.INT), node.isConst(),
    					false, cvar));
    		}
    		// if we do not find a type we cancel with Exception
    		String msg = "TypeHandler: We do not support this type!"
    				+ node.getType();
    		throw new UnsupportedSyntaxException(loc, msg);
    	}
    }

    @Override
    public Result visit(Dispatcher main, IASTNamedTypeSpecifier node) {
        ILocation loc = LocationFactory.createCLocation(node);
        if (node instanceof CASTTypedefNameSpecifier) {
            node = (CASTTypedefNameSpecifier) node;
            String cId = node.getName().toString();
            String bId = main.cHandler.getSymbolTable().get(cId, loc).getBoogieName();
            return new TypesResult(new NamedType(loc, bId, null), false, false, //TODO: replace constants
            		new CNamed(bId, m_DefinedTypes.get(bId).cType));
        }
        String msg = "Unknown or unsupported type! " + node.toString();
        throw new UnsupportedSyntaxException(loc, msg);
    }

    @Override
    public Result visit(Dispatcher main, IASTEnumerationSpecifier node) {
        ILocation loc = LocationFactory.createCLocation(node);
        String cId = node.getName().toString();
        // values of enum have type int
        CPrimitive intType = new CPrimitive(PRIMITIVE.INT);
        String enumId = main.nameHandler.getUniqueIdentifier(node, node.getName().toString(),
        		main.cHandler.getSymbolTable().getCompoundCounter(), false, intType);
        int nrFields = node.getEnumerators().length;
        String[] fNames = new String[nrFields];
        Expression[] fValues = new Expression[nrFields];
        for (int i = 0; i < nrFields; i++) {
            IASTEnumerator e = node.getEnumerators()[i];
            fNames[i] = e.getName().toString();
            if (e.getValue() != null) {
            	ExpressionResult rex = (ExpressionResult) main.dispatch(e.getValue());
            	fValues[i] = (Expression) rex.lrVal.getValue();
//            	assert (fValues[i] instanceof IntegerLiteral) || 
//            		(fValues[i] instanceof BitvecLiteral) : 
//            			"assuming that only IntegerLiterals or BitvecLiterals can occur while translating an enum constant";
            } else {
            	fValues[i] = null;
            }
        }
        CEnum cEnum = new CEnum(enumId, fNames, fValues);
        ASTType at = cPrimitive2asttype(loc, intType); 
        TypesResult result = new TypesResult(at, false, false, cEnum);
       
        String incompleteTypeName = "ENUM~" + cId;
        if (m_IncompleteType.contains(incompleteTypeName)) {
            m_IncompleteType.remove(incompleteTypeName);
            TypesResult incompleteType = m_DefinedTypes.get(cId);
            CEnum incompleteEnum = (CEnum) incompleteType.cType;
            //search for any typedefs that were made for the incomplete type
            //typedefs are made globally, so the CHandler has to do this
            ((CHandler) main.cHandler).completeTypeDeclaration(incompleteEnum, cEnum);

            incompleteEnum.complete(cEnum);
        }

        if (!enumId.equals(SFO.EMPTY)) {
            m_DefinedTypes.put(cId, result);
        }
        
        return result;
    }
    
    @Override
    public Result visit(Dispatcher main, IASTElaboratedTypeSpecifier node) {
    	ILocation loc = LocationFactory.createCLocation(node);
    	if (node.getKind() == IASTElaboratedTypeSpecifier.k_struct
    			|| node.getKind() == IASTElaboratedTypeSpecifier.k_enum
    			|| node.getKind() == IASTElaboratedTypeSpecifier.k_union) {
    		String type = node.getName().toString();
    		

    		//            if (m_DefinedTypes.containsKey(type)) {
    		TypesResult originalType = m_DefinedTypes.get(type);
//    		if (originalType == null && node.getKind() == IASTElaboratedTypeSpecifier.k_enum)
//    			// --> we have an incomplete enum --> do nothing 
//    			//(i cannot think of an effect of an incomplete enum declaration right now..)
//    			return new ResultSkip();
    		if (originalType != null) {
    			// --> we have a normal struct, union or enum declaration
    			TypesResult withoutBoogieTypedef = new TypesResult(
    					originalType.getType(), originalType.isConst, 
    					originalType.isVoid, originalType.cType);
    			return withoutBoogieTypedef;
    		} else {
    			// --> This is a definition of an incomplete struct, enum or union.
    			String incompleteTypeName;
    			if (node.getKind() == IASTElaboratedTypeSpecifier.k_struct) {
    				incompleteTypeName = "STRUCT~" + type;
    			} else if (node.getKind() == IASTElaboratedTypeSpecifier.k_union) {
    				incompleteTypeName = "UNION~" + type;
    			} else {
    				incompleteTypeName = "ENUM~" + type; 
    			}

    			m_IncompleteType.add(incompleteTypeName);
    			// 			FIXME : not sure, if null is a good idea!
    			//            ResultTypes r = new ResultTypes(new NamedType(loc, name,
    			//                    new ASTType[0]), false, false, null);
    			CType ctype;
    			if (node.getKind() == IASTElaboratedTypeSpecifier.k_struct) {
    				ctype = new CStruct(type);
    			} else if (node.getKind() == IASTElaboratedTypeSpecifier.k_union) {
    				ctype = new CUnion(type);
    			} else {
    				ctype = new CEnum(type);
    			}
    			TypesResult r = new TypesResult(new NamedType(loc, incompleteTypeName,
    					new ASTType[0]), false, false, ctype);


    			m_DefinedTypes.put(type, r);

    			return r;
    		}
    	}
    	String msg = "Not yet implemented: Spec [" + node.getKind() + "] of "
    			+ node.getClass();
    	throw new UnsupportedSyntaxException(loc, msg);
    }
    
  
    
    @Override
    public Result visit(Dispatcher main, IASTCompositeTypeSpecifier node) {
        ILocation loc = LocationFactory.createCLocation(node);
        ArrayList<VarList> fields = new ArrayList<VarList>();
        // TODO : include inactives? what are inactives?
        ArrayList<String> fNames = new ArrayList<String>();
        ArrayList<CType> fTypes = new ArrayList<CType>();
        structCounter++;
        for (IASTDeclaration dec : node.getDeclarations(false)) {
            Result r = main.dispatch(dec);
            if (r instanceof DeclarationResult) {
            	DeclarationResult rdec = (DeclarationResult) r;
            	for (CDeclaration declaration : rdec.getDeclarations()) {
            		fNames.add(declaration.getName());
            		fTypes.add(declaration.getType());
            		fields.add(new VarList(loc, new String[] {declaration.getName()},
            				this.ctype2asttype(loc, declaration.getType())));
            	}
            } else if (r instanceof SkipResult) { // skip ;)
            } else {
                String msg = "Unexpected syntax in struct declaration!";
                throw new UnsupportedSyntaxException(loc, msg);
            }
        }
        structCounter--;

        String cId = node.getName().toString();

        CStruct cvar;
        String name = null;
        if (node.getKey() == IASTCompositeTypeSpecifier.k_struct) {
        	name = "STRUCT~" + cId;
        	cvar = new CStruct(fNames.toArray(new String[0]),
                    fTypes.toArray(new CType[0]));
        } else if (node.getKey() == IASTCompositeTypeSpecifier.k_union) {
        	name = "UNION~" + cId;
        	cvar = new CUnion(fNames.toArray(new String[0]),
                    fTypes.toArray(new CType[0]));
        } else {
        	throw new UnsupportedOperationException();
        }
        
        NamedType namedType = new NamedType(loc, name,
                new ASTType[0]);
        ASTType type = namedType;
        TypesResult result = new TypesResult(type, false, false, cvar);
       
        if (m_IncompleteType.contains(name)) {
            m_IncompleteType.remove(name);
            TypesResult incompleteType = m_DefinedTypes.get(cId);
            CStruct incompleteStruct = (CStruct) incompleteType.cType;
            //search for any typedefs that were made for the incomplete type
            //typedefs are made globally, so the CHandler has to do this
            ((CHandler) main.cHandler).completeTypeDeclaration(incompleteStruct, cvar);

            incompleteStruct.complete(cvar);
        }
        
        if (!cId.equals(SFO.EMPTY)) {
            m_DefinedTypes.put(cId, result);
        }
        return result;
    }

    @Override
    public InferredType visit(Dispatcher main,
            org.eclipse.cdt.core.dom.ast.IType type) {
    	if (type instanceof CPointerType) {
    		return new InferredType(Type.Pointer);
    	} else {
    		// Handle the generic case of IType, if the specific case is not yet
    		// implemented
    		String msg = "TypeHandler: Not yet implemented: "
    				+ type.getClass().toString();
    		// TODO : no idea what location should be set to ...
    		main.unsupportedSyntax(null, msg);
    		return new InferredType(Type.Unknown);
    	}
    }

    @Override
    public InferredType visit(Dispatcher main, ITypedef type) {
    	assert false : "I don't think this should still be used";
        if (!m_DefinedTypes.containsKey(type.getName())) {
            String msg = "Unknown C typedef: " + type.getName();
            // TODO : no idea what location should be set to ...
            throw new IncorrectSyntaxException(null, msg);
        }
        return new InferredType(m_DefinedTypes.get(type.getName()).getType());
    }

    @Override
    public InferredType visit(final Dispatcher main, final IBasicType type) {
        switch (type.getKind()) {
            case eBoolean:
                return new InferredType(Type.Boolean);
            case eChar:
            case eChar16:
            case eChar32:
            case eInt:
                return new InferredType(Type.Integer);
            case eDouble:
            case eFloat:
                return new InferredType(Type.Real);
            case eWChar: // TODO : verify! Not sure what WChar means!
                return new InferredType(Type.String);
            case eVoid:
                return new InferredType(Type.Void);
            case eUnspecified:
            default:
                return new InferredType(Type.Unknown);
        }
    }

    @Override
    public ASTType getTypeOfStructLHS(final SymbolTable sT,
            final ILocation loc, final StructLHS lhs) {
        String[] flat = BoogieASTUtil.getLHSList(lhs);
        String leftMostId = flat[0];
        assert leftMostId.equals(BoogieASTUtil.getLHSId(lhs));
        assert sT.containsBoogieSymbol(leftMostId);
        String cId = sT.getCID4BoogieID(leftMostId, loc);
        assert sT.containsKey(cId);
        ASTType t = this.ctype2asttype(loc, sT.get(cId, loc).getCVariable());
        return traverseForType(loc, t, flat, 1);
    }

    /**
     * Returns the type of the field in the struct.
     * 
     * @param loc
     *            the location, where errors should be set, if there are any!
     * @param t
     *            the type to process.
     * @param flat
     *            the flattend LHS.
     * @param i
     *            index in flat[].
     * @return the type of the field.
     */
    private static ASTType traverseForType(final ILocation loc,
            final ASTType t, final String[] flat, final int i) {
        assert i > 0 && i <= flat.length;
        if (i >= flat.length)
            return t;
        if (t instanceof ArrayType)
            return traverseForType(loc, ((ArrayType) t).getValueType(), flat, i);
        if (t instanceof StructType) {
            for (VarList vl : ((StructType) t).getFields()) {
                assert vl.getIdentifiers().length == 1;
                // should hold by construction!
                if (vl.getIdentifiers()[0].equals(flat[i])) {
                    // found the field!
                    return traverseForType(loc, vl.getType(), flat, i + 1);
                }
            }
            String msg = "Field '" + flat[i] + "' not found in " + t;
            throw new IncorrectSyntaxException(loc, msg);
        }
        String msg = "Something went wrong while determining types!";
        throw new UnsupportedSyntaxException(loc, msg);
    }

    @Override
    public InferredType visit(Dispatcher main, IArrayType type) {
        return main.dispatch(type.getType());
    }
    
    @Override
    public  LinkedScopedHashMap<String,TypesResult> getDefinedTypes() {
        return m_DefinedTypes;
    }
    
    @Override
    public Set<String> getUndefinedTypes() {
        return m_IncompleteType;
    }

    @Override
	public ASTType ctype2asttype(ILocation loc, CType cType) {
		if (cType instanceof CPrimitive) {
			return cPrimitive2asttype(loc, (CPrimitive) cType);
		} else if (cType instanceof CPointer) {
			return constructPointerType(loc);
		} else if (cType instanceof CArray) {
			CArray cart = (CArray) cType;
			ASTType[] indexTypes = new ASTType[cart.getDimensions().length];
			String[] typeParams = new String[0]; //new String[cart.getDimensions().length];
			for (int i = 0; i < cart.getDimensions().length; i++) {
				indexTypes[i] = ctype2asttype(loc, cart.getDimensions()[i].getCType());
			}
			return new ArrayType(loc, typeParams, indexTypes, this.ctype2asttype(loc, cart.getValueType()));
		} else if (cType instanceof CStruct) {
			CStruct cstruct = (CStruct) cType;
			if (cstruct.isIncomplete())
				return null;
			VarList[] fields = new VarList[cstruct.getFieldCount()];
			for (int i = 0; i < cstruct.getFieldCount(); i++) {
				fields[i] = new VarList(loc, 
						new String[] {cstruct.getFieldIds()[i]}, 
						this.ctype2asttype(loc, cstruct.getFieldTypes()[i])); 
			}
			return new StructType(loc, fields);
		} else if (cType instanceof CNamed) {
			//should work as we save the unique typename we computed in CNamed, not the name from the source c file
			return new NamedType(loc, ((CNamed) cType).getName(), new ASTType[0]);
		} else if (cType instanceof CFunction) {
//				throw new UnsupportedSyntaxException(loc, "how to translate function type?");
//			return null; 
			return constructPointerType(loc);
		} else if (cType instanceof CEnum) {
//			return new NamedType(loc, ((CEnum) cType).getIdentifier(), new ASTType[0]);
			return cPrimitive2asttype(loc, new CPrimitive(PRIMITIVE.INT));
		}
		throw new UnsupportedSyntaxException(loc, "unknown type");
	}
    
    private ASTType cPrimitive2asttype(ILocation loc, CPrimitive cPrimitive) {
		switch (cPrimitive.getGeneralType()) {
		case VOID:
			return null; //(alex:) seems to be lindemm's convention, see FunctionHandler.isInParamVoid(..)
		case INTTYPE:
			if (m_UseIntForAllIntegerTypes) {
				return new PrimitiveType(loc, SFO.INT);
			} else {
				return new NamedType(loc, "C_" + cPrimitive.getType().toString(), new ASTType[0]);
			}
		case FLOATTYPE:
			return new PrimitiveType(loc, SFO.REAL);
		default:
			throw new UnsupportedSyntaxException(loc, "unknown primitive type");
		}
    }
    
    public void beginScope() {
    	m_DefinedTypes.beginScope();
    }
    
    public void endScope() {
    	m_DefinedTypes.endScope();
    }
    
    @Override
    public void addDefinedType(String id, TypesResult type) {
    	m_DefinedTypes.put(id, type);
    }

	@Override
	public ASTType constructPointerType(ILocation loc) {
		m_PointerTypeNeeded = true;
		return new NamedType(null, SFO.POINTER, new ASTType[0]);
	}
	
	/**
	 * Construct list of type declarations that are needed because the 
	 * corresponding types are introduced by the translation, e.g., pointers. 
	 */
	public ArrayList<Declaration> constructTranslationDefiniedDelarations(ILocation tuLoc, 
			AExpressionTranslation expressionTranslation) {
		ArrayList<Declaration> decl = new ArrayList<Declaration>();
		if (m_PointerTypeNeeded) {
			VarList fBase = new VarList(tuLoc, new String[] { SFO.POINTER_BASE }, 
					ctype2asttype(tuLoc, expressionTranslation.getCTypeOfPointerComponents()));
			VarList fOffset = new VarList(tuLoc, new String[] { SFO.POINTER_OFFSET }, 
					ctype2asttype(tuLoc, expressionTranslation.getCTypeOfPointerComponents()));
			VarList[] fields = new VarList[] { fBase, fOffset };
			ASTType pointerType = new StructType(tuLoc, fields);
			// Pointer is non-finite, right? (ZxZ)..
			decl.add(new TypeDeclaration(tuLoc, new Attribute[0], false, 
					SFO.POINTER, new String[0], pointerType));
		}
		return decl;
	}
	
	
}
