/*
 * Copyright (C) 2014-2015 Alexander Nutz (nutz@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.preferences;

import de.uni_freiburg.informatik.ultimate.core.model.preferences.UltimatePreferenceItem;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.BaseUltimatePreferenceItem.PreferenceType;
import de.uni_freiburg.informatik.ultimate.core.preferences.RcpPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.TranslationMode;

/**
 * Defines preference page for C translation. 
 * 
 * Check https://wiki.debian.org/ArchitectureSpecificsMemo to find our which
 * setting for typesizes you want to use.
 * @author Matthias Heizmann
 *
 */
public class CACSLPreferenceInitializer extends RcpPreferenceInitializer {

	@Override
	protected UltimatePreferenceItem<?>[] initDefaultPreferences() {

		return new UltimatePreferenceItem<?>[] {
				new UltimatePreferenceItem<TranslationMode>(LABEL_MODE,
						TranslationMode.SV_COMP14, PreferenceType.Radio,
						TranslationMode.values()),
				new UltimatePreferenceItem<String>(LABEL_MAINPROC, "main",
						PreferenceType.String),
				new UltimatePreferenceItem<Boolean>(LABEL_CHECK_SVCOMP_ERRORFUNCTION,
						true, PreferenceType.Boolean),
				new UltimatePreferenceItem<POINTER_CHECKMODE>(
						LABEL_CHECK_POINTER_VALIDITY,
						POINTER_CHECKMODE.ASSERTandASSUME,
						PreferenceType.Combo, POINTER_CHECKMODE.values()),
				new UltimatePreferenceItem<POINTER_CHECKMODE>(
						LABEL_CHECK_POINTER_ALLOC,
						POINTER_CHECKMODE.ASSERTandASSUME,
						PreferenceType.Combo, POINTER_CHECKMODE.values()),
				new UltimatePreferenceItem<POINTER_CHECKMODE>(
						LABEL_CHECK_ARRAYACCESSOFFHEAP,
						POINTER_CHECKMODE.ASSERTandASSUME,
						PreferenceType.Combo, POINTER_CHECKMODE.values()),
				new UltimatePreferenceItem<Boolean>(LABEL_CHECK_FREE_VALID,
						true, PreferenceType.Boolean),
				new UltimatePreferenceItem<Boolean>(
						LABEL_CHECK_MemoryLeakInMain, false,
						PreferenceType.Boolean),
				new UltimatePreferenceItem<MEMORY_MODEL>(
						LABEL_MEMORY_MODEL,
						MEMORY_MODEL.HoenickeLindenmann_Original,
						PreferenceType.Combo, MEMORY_MODEL.values()),
				new UltimatePreferenceItem<POINTER_INTEGER_CONVERSION>(
						LABEL_POINTER_INTEGER_CONVERSION,
						POINTER_INTEGER_CONVERSION.NonBijectiveMapping,
						PreferenceType.Combo, POINTER_INTEGER_CONVERSION.values()),
				new UltimatePreferenceItem<Boolean>(
						LABEL_REPORT_UNSOUNDNESS_WARNING, false,
						PreferenceType.Boolean),
				new UltimatePreferenceItem<POINTER_CHECKMODE>(
						LABEL_CHECK_POINTER_SUBTRACTION_AND_COMPARISON_VALIDITY,
						POINTER_CHECKMODE.ASSERTandASSUME,
						PreferenceType.Combo, POINTER_CHECKMODE.values()),
				new UltimatePreferenceItem<UNSIGNED_TREATMENT>(
						LABEL_UNSIGNED_TREATMENT,
						UNSIGNED_TREATMENT.WRAPAROUND,
						PreferenceType.Combo, UNSIGNED_TREATMENT.values()),
				new UltimatePreferenceItem<POINTER_CHECKMODE>(
						LABEL_CHECK_DIVISION_BY_ZERO,
						POINTER_CHECKMODE.ASSERTandASSUME,
						PreferenceType.Combo, POINTER_CHECKMODE.values()),
				new UltimatePreferenceItem<Boolean>(
						LABEL_CHECK_SIGNED_INTEGER_BOUNDS,
						false,
						PreferenceType.Boolean),
				new UltimatePreferenceItem<Boolean>(
						LABEL_ASSUME_NONDET_VALUES_IN_RANGE,
						true,
						PreferenceType.Boolean),
				new UltimatePreferenceItem<Boolean>(
						LABEL_BITVECTOR_TRANSLATION,
						false,
						PreferenceType.Boolean),
				new UltimatePreferenceItem<Boolean>(
						LABEL_OVERAPPROXIMATE_FLOATS,
						false,
						PreferenceType.Boolean),
				new UltimatePreferenceItem<Boolean>(
						LABEL_SMT_BOOL_ARRAYS_WORKAROUND,
						true,
						PreferenceType.Boolean),

				// typesize stuff
				new UltimatePreferenceItem<Boolean>(
						LABEL_USE_EXPLICIT_TYPESIZES, true,
						PreferenceType.Boolean),
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_BOOL, 1, PreferenceType.Integer),
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_CHAR, 1, PreferenceType.Integer),
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_SHORT, 2, PreferenceType.Integer),
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_INT, 4, PreferenceType.Integer),
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_LONG, 8, PreferenceType.Integer),
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_LONGLONG, 8, PreferenceType.Integer),
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_FLOAT, 4, PreferenceType.Integer),
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_DOUBLE, 8, PreferenceType.Integer),
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_LONGDOUBLE, 16, PreferenceType.Integer),
				new UltimatePreferenceItem<Integer>(
						LABEL_EXPLICIT_TYPESIZE_POINTER, 8,	PreferenceType.Integer),
				// more exotic types
//				new UltimatePreferenceItem<Integer>(
//						LABEL_EXPLICIT_TYPESIZE_CHAR16, 2, PreferenceType.Integer),
//				new UltimatePreferenceItem<Integer>(
//						LABEL_EXPLICIT_TYPESIZE_CHAR32, 4, PreferenceType.Integer),
				new UltimatePreferenceItem<SIGNEDNESS>(
						LABEL_SIGNEDNESS_CHAR,
						SIGNEDNESS.SIGNED,
						PreferenceType.Combo, SIGNEDNESS.values()),
			};
	}

	@Override
	protected String getPlugID() {
		return Activator.PLUGIN_ID;
	}

	@Override
	public String getPreferenceTitle() {
		return "C+ACSL to Boogie Translator";
	}

	public enum POINTER_CHECKMODE {
		IGNORE, ASSUME, ASSERTandASSUME
	}

	public enum UNSIGNED_TREATMENT {
		IGNORE, ASSUME_SOME, ASSUME_ALL, WRAPAROUND
	}
	
	public enum SIGNEDNESS {
		SIGNED, UNSIGNED
	}
	
	public enum MEMORY_MODEL {
		HoenickeLindenmann_Original, // one data array for each boogie type
		HoenickeLindenmann_1ByteResolution, 
		HoenickeLindenmann_2ByteResolution,
		HoenickeLindenmann_4ByteResolution,
		HoenickeLindenmann_8ByteResolution,
	}
	
	public enum POINTER_INTEGER_CONVERSION {
		Overapproximate,
		NonBijectiveMapping,
		NutzBijection,
		IdentityAxiom,
	}

	public static final String LABEL_MODE = "Translation Mode:";
	public static final String LABEL_MAINPROC = "Checked method. Library mode if empty.";
	public static final String LABEL_CHECK_SVCOMP_ERRORFUNCTION = "Check unreachability of error function in SV-COMP mode";
	public static final String LABEL_CHECK_POINTER_VALIDITY = "Pointer base address is valid at dereference";
	public static final String LABEL_CHECK_POINTER_ALLOC = "Pointer to allocated memory at dereference";
	public static final String LABEL_CHECK_FREE_VALID = "Check if freed pointer was valid";
	public static final String LABEL_CHECK_MemoryLeakInMain = "Check for the main procedure if all allocated memory was freed";
	public static final String LABEL_MEMORY_MODEL = "Memory model";
	public static final String LABEL_POINTER_INTEGER_CONVERSION = "Pointer-integer casts";
	public static final String LABEL_CHECK_ARRAYACCESSOFFHEAP = "Check array bounds for arrays that are off heap";
	public static final String LABEL_REPORT_UNSOUNDNESS_WARNING = "Report unsoundness warnings";
	public static final String LABEL_CHECK_POINTER_SUBTRACTION_AND_COMPARISON_VALIDITY = "If two pointers are subtracted or compared they have the same base address";
	public static final String LABEL_UNSIGNED_TREATMENT = "How to treat unsigned ints differently from normal ones";
	public static final String LABEL_CHECK_DIVISION_BY_ZERO = "Check division by zero";
	public static final String LABEL_CHECK_SIGNED_INTEGER_BOUNDS = "Check absence of signed integer overflows";
	public static final String LABEL_ASSUME_NONDET_VALUES_IN_RANGE = "Assume nondeterminstic values are in range";
	public static final String LABEL_BITVECTOR_TRANSLATION = "Use bitvectors instead of ints";
	public static final String LABEL_OVERAPPROXIMATE_FLOATS = "Overapproximate operations of floating types";
	public static final String LABEL_SMT_BOOL_ARRAYS_WORKAROUND = "SMT bool arrays workaround";
						

	// typesize stuff
	public static final String LABEL_USE_EXPLICIT_TYPESIZES = "Use the constants given below as storage sizes for the correponding types";
	public static final String LABEL_EXPLICIT_TYPESIZE_BOOL = "sizeof _Bool";
	public static final String LABEL_EXPLICIT_TYPESIZE_CHAR = "sizeof char";
	public static final String LABEL_EXPLICIT_TYPESIZE_SHORT = "sizeof short";
	public static final String LABEL_EXPLICIT_TYPESIZE_INT = "sizeof int";
	public static final String LABEL_EXPLICIT_TYPESIZE_LONG = "sizeof long";
	public static final String LABEL_EXPLICIT_TYPESIZE_LONGLONG = "sizeof long long";
	public static final String LABEL_EXPLICIT_TYPESIZE_FLOAT = "sizeof float";
	public static final String LABEL_EXPLICIT_TYPESIZE_DOUBLE = "sizeof double";
	public static final String LABEL_EXPLICIT_TYPESIZE_LONGDOUBLE = "sizeof long double";
	public static final String LABEL_EXPLICIT_TYPESIZE_POINTER = "sizeof POINTER";
//	public static final String LABEL_EXPLICIT_TYPESIZE_CHAR16 = "sizeof char16";
//	public static final String LABEL_EXPLICIT_TYPESIZE_CHAR32 = "sizeof char32";
	public static final String LABEL_SIGNEDNESS_CHAR = "signedness of char";
}
