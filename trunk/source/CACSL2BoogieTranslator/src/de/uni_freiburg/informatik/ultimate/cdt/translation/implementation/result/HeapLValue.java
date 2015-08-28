package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CType;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;

public class HeapLValue extends LRValue {

	/**
	 * A Value inside a ResultExpression that represents a position on the
	 * heap. Its value may be either the address or the contents of the heap
	 * position.
	 * @param address
	 * @param addressIsValue determines whether the value of this is currently an address
	 * or a Expression
	 */
	public HeapLValue(Expression address, CType cType) {
		this(address, cType, false);
	}

	public HeapLValue(Expression address, CType cType, boolean isIntFromPtr) {
		this.address = address;
		this.setCType(cType);
		this.setIntFromPointer(isIntFromPtr);
	}
	Expression address;
	
	public Expression getAddress() {
		return this.address;
	}
	
	public Expression getValue() {
		throw new AssertionError("HeapLValues must be converted to RValue before their value can be queried.");
	}
}
