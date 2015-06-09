/*
 * Project:	CoreRCP
 * Package:	de.uni_freiburg.informatik.ultimate.logging
 * File:	UltimateLoggers.java created on Feb 23, 2010 by Björn Buchhold
 *
 */
package de.uni_freiburg.informatik.ultimate.core.services;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggerRepository;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import de.uni_freiburg.informatik.ultimate.core.coreplugin.Activator;
import de.uni_freiburg.informatik.ultimate.core.coreplugin.preferences.CorePreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceStore;

/**
 * UltimateLoggers
 * 
 * @author Björn Buchhold
 * @author dietsch@informatik.uni-freiburg.de
 * 
 */
public final class LoggingService implements IStorable, ILoggingService {

	private static final String LOGGER_NAME_CONTROLLER = "controller";
	private static final String LOGGER_NAME_PLUGINS = "plugins";
	private static final String LOGGER_NAME_TOOLS = "tools";
	private static final String sKey = "LoggingService";

	private UltimatePreferenceStore mPreferenceStore;
	private List<String> mLiveLoggerIds;
	private FileAppender mFileAppender;
	private ConsoleAppender mConsoleAppender;
	private IPreferenceChangeListener mRefreshingListener;

	private HashSet<Appender> mAdditionalAppenders;

	private String mCurrentControllerName;

	private LoggingService() {

		mPreferenceStore = new UltimatePreferenceStore(Activator.s_PLUGIN_ID);
		mAdditionalAppenders = new HashSet<Appender>();

		// we remove the initial log4j console appender because we want to
		// replace it with our own
		Logger.getRootLogger().removeAppender("ConsoleAppender");

		Enumeration<?> forgeinAppenders = Logger.getRootLogger().getAllAppenders();
		while (forgeinAppenders.hasMoreElements()) {
			Appender appender = (Appender) forgeinAppenders.nextElement();
			mAdditionalAppenders.add(appender);
		}
		for (Appender app : mAdditionalAppenders) {
			Logger.getRootLogger().removeAppender(app);
		}

		initializeAppenders();
		refreshPropertiesLoggerHierarchie();
		refreshPropertiesAppendLogFile();

		mRefreshingListener = new RefreshingPreferenceChangeListener();
		mPreferenceStore.removePreferenceChangeListener(mRefreshingListener);
		mPreferenceStore.addPreferenceChangeListener(mRefreshingListener);
	}

	public void refreshLoggingService() {
		initializeAppenders();
		refreshPropertiesLoggerHierarchie();
		refreshPropertiesAppendLogFile();
		getLoggerById(Activator.s_PLUGIN_ID).debug("Logger refreshed");
	}

	public void addAppender(Appender appender) {
		mAdditionalAppenders.add(appender);
		Logger.getRootLogger().addAppender(appender);
	}

	public void removeAppender(Appender appender) {
		mAdditionalAppenders.remove(appender);
		Logger.getRootLogger().removeAppender(appender);
	}

	private void initializeAppenders() {
		try {
			// clear all old appenders
			Logger.getRootLogger().removeAppender(mConsoleAppender);
			// we remove the initial log4j console appender because we want to
			// replace it with our own
			Logger.getRootLogger().removeAppender("ConsoleAppender");

			for (Appender appender : mAdditionalAppenders) {
				Logger.getRootLogger().removeAppender(appender);
			}

			// first, handle console appender as we also configure it
			// defining format of logging output
			PatternLayout layout = new PatternLayout(
					mPreferenceStore.getString(CorePreferenceInitializer.LABEL_LOG4J_PATTERN));

			// attaching output to console (stout)
			mConsoleAppender = new ConsoleAppender(layout);
			mConsoleAppender.setName("ConsoleAppender");
			Logger.getRootLogger().addAppender(mConsoleAppender);

			for (Appender appender : mAdditionalAppenders) {
				// then, re-add all the other appenders
				Logger.getRootLogger().addAppender(appender);
			}

		} catch (Exception ex) {
			System.err.println("Error while initializing logger: " + ex);
			ex.printStackTrace();
		}
	}

	private void refreshPropertiesAppendLogFile() {
		// if log-file should be used, it will be appended here

		if (mPreferenceStore.getBoolean(CorePreferenceInitializer.LABEL_LOGFILE)) {
			// if there is already a log file, we remove the corresponding
			// appender!
			if (mFileAppender != null) {
				Logger.getRootLogger().removeAppender(mFileAppender);
				mFileAppender = null;
			}
			String logName = mPreferenceStore.getString(CorePreferenceInitializer.LABEL_LOGFILE_NAME);
			String logDir = mPreferenceStore.getString(CorePreferenceInitializer.LABEL_LOGFILE_DIR);

			try {
				PatternLayout layout = new PatternLayout(
						mPreferenceStore.getString(CorePreferenceInitializer.LABEL_LOG4J_PATTERN));
				boolean append = mPreferenceStore.getBoolean(CorePreferenceInitializer.LABEL_APPEXLOGFILE);
				mFileAppender = new FileAppender(layout, logDir + File.separator + logName + ".log", append);
				Logger.getRootLogger().addAppender(mFileAppender);
			} catch (IOException e) {
				System.err.println("Error while appending log file to logger: " + e);
				e.printStackTrace();
			}
		} else {
			if (mFileAppender != null) {
				Logger.getRootLogger().removeAppender(mFileAppender);
				mFileAppender = null;
			}
		}
	}

	/**
	 * UltimateLoggerFactory getInstance getter for the singleton. lazily
	 * creates the object
	 * 
	 * @return the singleton instance of the UltimateLoggerFactory
	 */
	static LoggingService getService(IToolchainStorage storage) {
		assert storage != null;
		IStorable rtr = storage.getStorable(sKey);
		if (rtr == null) {
			rtr = new LoggingService();
			storage.putStorable(sKey, rtr);
		}
		return (LoggingService) rtr;
	}

	public static String getServiceKey() {
		return sKey;
	}

	/**
	 * Logger getLoggerById
	 * 
	 * @param id
	 *            Internal logger id.
	 * @return Logger for this id.
	 */
	public Logger getLoggerById(String id) {
		return lookupLoggerInHierarchie(id);
	}

	/**
	 * boolean isExternalTool
	 * 
	 * @param id
	 *            Internal logger id.
	 * @return <code>true</code> if and only if this id denotes an external
	 *         tool.
	 */
	private boolean isExternalTool(String id) {
		return id.startsWith(CorePreferenceInitializer.EXTERNAL_TOOLS_PREFIX);
	}

	/**
	 * Logger lookupLoggerInHierarchie
	 * 
	 * @param id
	 *            Internal logger id.
	 * @return Logger for this internal id.
	 */
	private Logger lookupLoggerInHierarchie(String id) {
		// it is core
		if (id.equals(Activator.s_PLUGIN_ID)) {
			return Logger.getLogger(Activator.s_PLUGIN_ID);
		}
		// it is a controller
		assert mCurrentControllerName != null;
		if (id.equals(mCurrentControllerName)) {
			return Logger.getLogger(LOGGER_NAME_CONTROLLER);
		}
		// it is something that wants the contoller logger
		if (id.equals(LOGGER_NAME_CONTROLLER)) {
			return Logger.getLogger(LOGGER_NAME_CONTROLLER);
		}
		// it is a declared one for no tool
		if (mLiveLoggerIds.contains(LOGGER_NAME_PLUGINS + "." + id) && !isExternalTool(id)) {
			return Logger.getLogger(LOGGER_NAME_PLUGINS + "." + id);
		}
		// it is a declared one for a tool
		if (mLiveLoggerIds.contains(LOGGER_NAME_TOOLS + "." + id) && isExternalTool(id)) {
			return Logger.getLogger(LOGGER_NAME_TOOLS + "." + id);
		}
		// it is an external tool with no logger specified
		if (isExternalTool(id)) {
			return Logger.getLogger(LOGGER_NAME_TOOLS);
		}
		// otherwise it has to be some plug-in with no logger specified
		return Logger.getLogger(LOGGER_NAME_PLUGINS);
	}

	private void refreshPropertiesLoggerHierarchie() {
		mLiveLoggerIds = new LinkedList<String>();
		Logger rootLogger = Logger.getRootLogger();
		String level = mPreferenceStore.getString(CorePreferenceInitializer.LABEL_ROOT_PREF);
		rootLogger.setLevel(Level.toLevel(level));

		// now create children of the rootLogger

		// plug-ins
		LoggerRepository rootRepos = rootLogger.getLoggerRepository();
		Logger pluginsLogger = rootRepos.getLogger(LOGGER_NAME_PLUGINS);
		mLiveLoggerIds.add(LOGGER_NAME_PLUGINS);
		String pluginslevel = mPreferenceStore.getString(CorePreferenceInitializer.LABEL_PLUGINS_PREF);
		if (!pluginslevel.isEmpty()) {
			pluginsLogger.setLevel(Level.toLevel(pluginslevel));
		}

		// external tools
		Logger toolslog = rootRepos.getLogger(LOGGER_NAME_TOOLS);
		mLiveLoggerIds.add(LOGGER_NAME_TOOLS);
		String toolslevel = mPreferenceStore.getString(CorePreferenceInitializer.LABEL_TOOLS_PREF);
		if (!toolslevel.isEmpty()) {
			toolslog.setLevel(Level.toLevel(toolslevel));
		}

		// controller
		Logger controllogger = rootRepos.getLogger(LOGGER_NAME_CONTROLLER);
		String controllevel = mPreferenceStore.getString(CorePreferenceInitializer.LABEL_CONTROLLER_PREF);
		if (!controllevel.isEmpty()) {
			controllogger.setLevel(Level.toLevel(controllevel));
		}
		mLiveLoggerIds.add(LOGGER_NAME_CONTROLLER);

		// core
		Logger corelogger = rootRepos.getLogger(Activator.s_PLUGIN_ID);
		String corelevel = mPreferenceStore.getString(CorePreferenceInitializer.LABEL_CORE_PREF);
		if (!corelevel.isEmpty()) {
			corelogger.setLevel(Level.toLevel(corelevel));
		}
		mLiveLoggerIds.add(Activator.s_PLUGIN_ID);

		// create children for plug-ins
		LoggerRepository piRepos = pluginsLogger.getLoggerRepository();
		String[] plugins = getDefinedLogLevels();

		for (String plugin : plugins) {
			Logger logger = piRepos.getLogger(LOGGER_NAME_PLUGINS + "." + plugin);
			logger.setLevel(Level.toLevel(getLogLevel(plugin)));
			mLiveLoggerIds.add(logger.getName());
		}

		// create child loggers for external tools
		LoggerRepository toolRepos = toolslog.getLoggerRepository();
		String[] tools = getDefinedLogLevels();
		for (String tool : tools) {
			Logger logger = toolRepos.getLogger(LOGGER_NAME_TOOLS + "." + tool);
			logger.setLevel(Level.toLevel(getLogLevel(tool)));
			mLiveLoggerIds.add(logger.getName());
		}
	}

	/**
	 * String getLogLevel gets a log level for a certain plug-in
	 * 
	 * @param id
	 *            the id of the plug in
	 * @return the log level or null if no log-level is directly associated
	 */
	private String getLogLevel(String id) {
		String[] pref = getLoggingDetailsPreference();
		for (String string : pref) {
			if (string.startsWith(id + "=")) {
				return string.substring(string.lastIndexOf("=") + 1);
			}
		}
		return null;
	}

	private String[] getLoggingDetailsPreference() {
		return convert(mPreferenceStore.getString(CorePreferenceInitializer.PREFID_DETAILS));
	}

	private String[] getDefinedLogLevels() {
		final String[] pref = convert(mPreferenceStore.getString(CorePreferenceInitializer.PREFID_DETAILS));
		final String[] retVal = new String[pref.length];
		for (int i = 0; i < retVal.length; i++) {
			retVal[i] = pref[i].substring(0, pref[i].lastIndexOf("="));
		}
		return retVal;
	}

	private String[] convert(String preferenceValue) {
		final StringTokenizer tokenizer = new StringTokenizer(preferenceValue,
				CorePreferenceInitializer.VALUE_DELIMITER_LOGGING_PREF);
		final int tokenCount = tokenizer.countTokens();
		final String[] elements = new String[tokenCount];
		for (int i = 0; i < tokenCount; i++) {
			elements[i] = tokenizer.nextToken();
		}

		return elements;
	}

	public void setCurrentControllerID(String name) {
		mCurrentControllerName = name;
	}

	@Override
	public void destroy() {
		mPreferenceStore.removePreferenceChangeListener(mRefreshingListener);
	}

	@Override
	public Logger getLogger(String pluginId) {
		return getLoggerById(pluginId);
	}

	@Override
	public Logger getLoggerForExternalTool(String id) {
		return getLoggerById(CorePreferenceInitializer.EXTERNAL_TOOLS_PREFIX + id);
	}

	@Override
	public Logger getControllerLogger() {
		return getLoggerById(LoggingService.LOGGER_NAME_CONTROLLER);
	}

	private final class RefreshingPreferenceChangeListener implements IPreferenceChangeListener {
		// FIXME: Care! Check which properties are relevant for logging and
		// exactly when we have to reload
		// we do not care what property changes, we just reload the logging
		// stuff every time

		private RefreshingPreferenceChangeListener() {

		}

		@Override
		public void preferenceChange(PreferenceChangeEvent event) {
			// do things if it concerns the loggers
			String ek = event.getKey();
			Object newValue = event.getNewValue();
			Object oldValue = event.getOldValue();

			if (newValue == null && oldValue == null) {
				return;
			}

			if (newValue != null && newValue.equals(oldValue)) {
				return;
			}
			if (ek.equals(CorePreferenceInitializer.LABEL_LOG4J_PATTERN)
					|| ek.equals(CorePreferenceInitializer.LABEL_LOGFILE)
					|| ek.equals(CorePreferenceInitializer.LABEL_LOGFILE_NAME)
					|| ek.equals(CorePreferenceInitializer.LABEL_LOGFILE_DIR)
					|| ek.equals(CorePreferenceInitializer.LABEL_APPEXLOGFILE)
					|| ek.equals(CorePreferenceInitializer.EXTERNAL_TOOLS_PREFIX)
					|| ek.equals(CorePreferenceInitializer.PREFID_ROOT)
					|| ek.equals(CorePreferenceInitializer.PREFID_PLUGINS)
					|| ek.equals(CorePreferenceInitializer.PREFID_TOOLS)
					|| ek.equals(CorePreferenceInitializer.PREFID_CONTROLLER)
					|| ek.equals(CorePreferenceInitializer.PREFID_CORE)
					|| ek.equals(CorePreferenceInitializer.PREFID_DETAILS)
					|| ek.equals(CorePreferenceInitializer.LABEL_ROOT_PREF)
					|| ek.equals(CorePreferenceInitializer.LABEL_TOOLS_PREF)
					|| ek.equals(CorePreferenceInitializer.LABEL_CORE_PREF)
					|| ek.equals(CorePreferenceInitializer.LABEL_CONTROLLER_PREF)
					|| ek.equals(CorePreferenceInitializer.LABEL_PLUGINS_PREF)
					|| ek.equals(CorePreferenceInitializer.LABEL_PLUGIN_DETAIL_PREF)
					|| ek.equals(CorePreferenceInitializer.LABEL_COLOR_DEBUG)
					|| ek.equals(CorePreferenceInitializer.LABEL_COLOR_INFO)
					|| ek.equals(CorePreferenceInitializer.LABEL_COLOR_WARNING)
					|| ek.equals(CorePreferenceInitializer.LABEL_COLOR_ERROR)
					|| ek.equals(CorePreferenceInitializer.LABEL_COLOR_FATAL)) {
				// its relevant
			} else {
				// it does not concern us, just break
				return;
			}
			refreshLoggingService();
		}
	}
}
