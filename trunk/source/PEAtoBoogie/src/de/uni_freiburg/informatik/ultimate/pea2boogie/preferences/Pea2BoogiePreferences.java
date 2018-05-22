/*
 * Copyright (C) 2018 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2018 University of Freiburg
 *
 * This file is part of the ULTIMATE PEAtoBoogie plug-in.
 *
 * The ULTIMATE PEAtoBoogie plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE PEAtoBoogie plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE PEAtoBoogie plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE PEAtoBoogie plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE PEAtoBoogie plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.pea2boogie.preferences;

import de.uni_freiburg.informatik.ultimate.core.lib.preferences.UltimatePreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.BaseUltimatePreferenceItem.PreferenceType;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceProvider;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.UltimatePreferenceItem;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.UltimatePreferenceItem.IUltimatePreferenceItemValidator;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.pea2boogie.Activator;

/**
 *
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 */
public class Pea2BoogiePreferences extends UltimatePreferenceInitializer {

	public static final String LABEL_CHECK_VACUITY = "Check vacuity";
	private static final boolean DEF_CHECK_VACUITY = true;
	private static final String DESC_CHECK_VACUITY = null;

	public static final String LABEL_CHECK_CONSISTENCY = "Check consistency";
	private static final boolean DEF_CHECK_CONSISTENCY = true;
	private static final String DESC_CHECK_CONSISTENCY = null;

	public static final String LABEL_CHECK_RT_INCONSISTENCY = "Check rt-inconsistency";
	private static final boolean DEF_CHECK_RT_INCONSISTENCY = true;
	private static final String DESC_CHECK_RT_INCONSISTENCY = null;

	public static final String LABEL_RT_INCONSISTENCY_RANGE = "Rt-inconsistency range";
	private static final int DEF_RT_INCONSISTENCY_RANGE = 2;
	private static final String DESC_RT_INCONSISTENCY_RANGE =
			"How many requirements should be checked for rt-inconsistency at the same time? "
					+ "Allows only values larger or equal to 2. "
					+ "Note: This value increases the runtime exponentially!";
	public static final String LABEL_REPORT_TRIVIAL_RT_CONSISTENCY = "Report trivial rt-consistency";
	private static final boolean DEF_REPORT_TRIVIAL_RT_CONSISTENCY = false;
	private static final String DESC_REPORT_TRIVIAL_RT_CONSISTENCY = null;

	public Pea2BoogiePreferences() {
		super(Activator.PLUGIN_ID, Activator.PLUGIN_NAME);
	}

	@Override
	protected UltimatePreferenceItem<?>[] initDefaultPreferences() {
		return new UltimatePreferenceItem<?>[] {

				new UltimatePreferenceItem<>(LABEL_CHECK_VACUITY, DEF_CHECK_VACUITY, DESC_CHECK_VACUITY,
						PreferenceType.Boolean),
				new UltimatePreferenceItem<>(LABEL_CHECK_CONSISTENCY, DEF_CHECK_CONSISTENCY, DESC_CHECK_CONSISTENCY,
						PreferenceType.Boolean),
				new UltimatePreferenceItem<>(LABEL_CHECK_RT_INCONSISTENCY, DEF_CHECK_RT_INCONSISTENCY,
						DESC_CHECK_RT_INCONSISTENCY, PreferenceType.Boolean),
				new UltimatePreferenceItem<>(LABEL_REPORT_TRIVIAL_RT_CONSISTENCY, DEF_REPORT_TRIVIAL_RT_CONSISTENCY,
						DESC_REPORT_TRIVIAL_RT_CONSISTENCY, PreferenceType.Boolean),
				new UltimatePreferenceItem<>(LABEL_RT_INCONSISTENCY_RANGE, DEF_RT_INCONSISTENCY_RANGE,
						DESC_RT_INCONSISTENCY_RANGE, PreferenceType.Integer,
						IUltimatePreferenceItemValidator.GEQ_TWO), };
	}

	public static IPreferenceProvider getPreferenceProvider(final IUltimateServiceProvider services) {
		return services.getPreferenceProvider(Activator.PLUGIN_ID);
	}
}
