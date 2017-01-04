/*
 * Copyright (C) 2016 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2016 University of Freiburg
 *
 * This file is part of the ULTIMATE CLI plug-in.
 *
 * The ULTIMATE CLI plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE CLI plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE CLI plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE CLI plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE CLI plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.cli;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.xml.sax.SAXException;

import de.uni_freiburg.informatik.ultimate.cli.exceptions.InvalidFileArgumentException;
import de.uni_freiburg.informatik.ultimate.cli.options.CommandLineOptions;
import de.uni_freiburg.informatik.ultimate.cli.options.OptionBuilder;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.IToolchainData;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceProvider;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Pair;

/**
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 */
public class ParsedParameter {

	private final CommandLine mCli;
	private final ICore<RunDefinition> mCore;
	private final ILogger mLogger;
	private final OptionBuilder mOptionFactory;

	ParsedParameter(final ICore<RunDefinition> core, final CommandLine cli, final OptionBuilder optionFactory) {
		mCore = core;
		mCli = cli;
		mOptionFactory = optionFactory;
		mLogger = core.getCoreLoggingService().getControllerLogger();
	}

	public void applyCliSettings(final IUltimateServiceProvider services) throws ParseException {
		for (final Option op : mCli.getOptions()) {
			applyCliSetting(op, services);
		}
	}

	private void applyCliSetting(final Option op, final IUltimateServiceProvider services) throws ParseException {
		final String optName = op.getLongOpt();
		final Pair<String, String> prefName = mOptionFactory.getUltimatePreference(optName);
		if (prefName == null) {
			return;
		}
		final IPreferenceProvider preferences = services.getPreferenceProvider(prefName.getFirst());
		final Object value = getParsedOption(optName);
		mLogger.info(
				"Applying setting for plugin " + prefName.getFirst() + ": " + prefName.getSecond() + " -> " + value);
		preferences.put(prefName.getSecond(), String.valueOf(value));
	}

	public boolean isHelpRequested() {
		return mCli.hasOption(CommandLineOptions.OPTION_NAME_HELP);
	}

	public boolean isVersionRequested() {
		return mCli.hasOption(CommandLineOptions.OPTION_NAME_VERSION);
	}

	public boolean showExperimentals() {
		return mCli.hasOption(CommandLineOptions.OPTION_NAME_EXPERIMENTAL);
	}

	public String getSettingsFile() throws ParseException, InvalidFileArgumentException {
		final File file = getParsedOption(CommandLineOptions.OPTION_NAME_SETTINGS);
		checkFileExists(file, CommandLineOptions.OPTION_LONG_NAME_SETTINGS);
		return file.getAbsolutePath();
	}

	public boolean hasSettings() {
		return mCli.hasOption(CommandLineOptions.OPTION_NAME_SETTINGS);
	}

	public boolean hasToolchain() {
		return mCli.hasOption(CommandLineOptions.OPTION_NAME_TOOLCHAIN);
	}

	public boolean hasInputFiles() {
		return mCli.hasOption(CommandLineOptions.OPTION_NAME_INPUTFILES);
	}

	public File getToolchainFile() throws ParseException, InvalidFileArgumentException {
		final File file = getParsedOption(CommandLineOptions.OPTION_NAME_TOOLCHAIN);
		checkFileExists(file, CommandLineOptions.OPTION_LONG_NAME_TOOLCHAIN);
		return file;
	}

	public IToolchainData<RunDefinition> createToolchainData() throws InvalidFileArgumentException, ParseException {
		final File toolchainFile = getToolchainFile();
		try {
			return mCore.createToolchainData(toolchainFile.getAbsolutePath());
		} catch (final FileNotFoundException e1) {
			throw new InvalidFileArgumentException(
					"Toolchain file not found at specified path: " + toolchainFile.getAbsolutePath());
		} catch (final SAXException | JAXBException e1) {
			throw new InvalidFileArgumentException(
					"Toolchain file at path " + toolchainFile.getAbsolutePath() + " was malformed: " + e1.getMessage());
		}
	}

	public File[] getInputFiles() throws InvalidFileArgumentException, ParseException {
		final File[] inputFilesArgument = getInputFileArgument();
		if (inputFilesArgument == null || inputFilesArgument.length == 0) {
			throw new InvalidFileArgumentException("No input file specified");
		}

		// for (final File file : inputFilesArgument) {
		// checkFileExists(file, CommandLineOptions.OPTION_LONG_NAME_INPUTFILES);
		// }
		return inputFilesArgument;
	}

	private File[] getInputFileArgument() {
		final String[] values = mCli.getOptionValues(CommandLineOptions.OPTION_NAME_INPUTFILES);
		final File[] files = new File[values.length];

		for (int i = 0; i < values.length; ++i) {
			files[i] = new File(values[i]);
		}

		return files;
	}

	private static void checkFileExists(final File file, final String argumentName)
			throws InvalidFileArgumentException {
		if (file == null) {
			throw new IllegalArgumentException("file");
		}
		if (!file.exists()) {
			throw new InvalidFileArgumentException("Argument of \"" + argumentName + "\" has invalid value \""
					+ file.getAbsolutePath() + "\": File does not exist");
		}
		if (!file.canRead()) {
			throw new InvalidFileArgumentException("Argument of \"" + argumentName + "\" has invalid value \""
					+ file.getAbsolutePath() + "\": File cannot be read");
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T getParsedOption(final String optionName) throws ParseException {
		final Object obj = mCli.getParsedOptionValue(optionName);
		return (T) obj;
	}

}
