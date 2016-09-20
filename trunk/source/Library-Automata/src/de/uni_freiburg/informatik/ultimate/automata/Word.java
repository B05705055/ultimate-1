/*
 * Copyright (C) 2011-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2009-2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE Automata Library.
 * 
 * The ULTIMATE Automata Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE Automata Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Automata Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Automata Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE Automata Library grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.automata;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A finite word, i.e., a finite sequence of symbols.
 * 
 * @author Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * @author Christian Schilling (schillic@informatik.uni-freiburg.de)
 * @param <LETTER>
 *            symbol type
 */
public class Word<LETTER> implements Iterable<LETTER> {
	/**
	 * The word.
	 */
	protected LETTER[] mWord;
	
	/**
	 * Construct word consisting of a sequence of symbols.
	 * 
	 * @param symbols
	 *            sequence of symbols
	 */
	@SafeVarargs
	public Word(final LETTER... symbols) {
		mWord = symbols;
	}
	
	/**
	 * @return The length of the word is 0 for the empty word, 1 for the
	 *         word that consists of one symbol, etc.
	 */
	public int length() {
		return mWord.length;
	}
	
	/**
	 * A list view of the symbols.
	 * 
	 * @return list of symbols
	 */
	public List<LETTER> asList() {
		return Arrays.asList(mWord);
	}
	
	/**
	 * The symbol at the given position.
	 * 
	 * @param position
	 *            position in word
	 * @return symbol at the given position
	 */
	public LETTER getSymbol(final int position) {
		if (position < 0 || position >= length()) {
			throw new IllegalArgumentException("index out of range");
		}
		return mWord[position];
	}
	
	/**
	 * @param otherWord
	 *            other word
	 * @return concatenation 'this.otherWord'
	 */
	public Word<LETTER> concatenate(final Word<LETTER> otherWord) {
		final int lengthWord1 = this.length();
		final int lengthWord2 = otherWord.length();
		@SuppressWarnings("unchecked")
		final LETTER[] concatenationSymbols = (LETTER[]) new Object[lengthWord1 + lengthWord2];
		
		for (int i = 0; i < lengthWord1; i++) {
			concatenationSymbols[i] = this.getSymbol(i);
		}
		for (int i = 0; i < lengthWord2; i++) {
			concatenationSymbols[lengthWord1 + i] = otherWord.getSymbol(i);
		}
		return new Word<>(concatenationSymbols);
	}
	
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append('[');
		for (int i = 0; i < length(); i++) {
			builder.append(getSymbol(i));
		}
		builder.append(']');
		return builder.toString();
	}
	
	@Override
	public Iterator<LETTER> iterator() {
		return asList().iterator();
	}
}
