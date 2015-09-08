/*
 * Copyright (C) 2015 Christian Ortolf
 * Copyright (C) 2008-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2009-2015 Jürgen Christ (christj@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE DebugGUI plug-in.
 * 
 * The ULTIMATE DebugGUI plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE DebugGUI plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE DebugGUI plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE DebugGUI plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE DebugGUI plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.gui.advisors;

import de.uni_freiburg.informatik.ultimate.ep.interfaces.ICore;
import de.uni_freiburg.informatik.ultimate.gui.GuiController;
import de.uni_freiburg.informatik.ultimate.gui.actions.LoadSettingsAction;
import de.uni_freiburg.informatik.ultimate.gui.actions.LoadSourceFilesAction;
import de.uni_freiburg.informatik.ultimate.gui.actions.ResetAndRedoToolChainAction;
import de.uni_freiburg.informatik.ultimate.gui.actions.ResetAndRedoToolChainNewTCAction;
import de.uni_freiburg.informatik.ultimate.gui.actions.ResetAndRedoToolChainOldTCAction;
import de.uni_freiburg.informatik.ultimate.gui.actions.SaveSettingsAction;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

/**
 * the class that handles the actions and fills the action bars.
 * 
 * @author Christian Ortolf
 * 
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	private ICore mCore;
	private GuiController mController;

	private IWorkbenchAction exitAction;

	private IWorkbenchAction aboutAction;

	private IWorkbenchAction preferenceAction;

	// custom actions
	private IWorkbenchAction loadSourceFiles;
	private IWorkbenchAction resetAndReRun;
	private IWorkbenchAction resetAndReRunNewTC, resetAndReRunOldTC;
	private IWorkbenchAction loadSettings, saveSettings;
	private Logger mLogger;

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer, ICore icc, GuiController controller, Logger logger) {
		super(configurer);
		this.mCore = icc;
		mController = controller;
		mLogger = logger;
	}

	/**
	 * called by Workbench to create our actions.
	 * 
	 * @param window
	 *            the workbench window we are in
	 */
	protected final void makeActions(final IWorkbenchWindow window) {
		exitAction = ActionFactory.QUIT.create(window);
		register(exitAction);
		aboutAction = ActionFactory.ABOUT.create(window);
		register(aboutAction);
		preferenceAction = ActionFactory.PREFERENCES.create(window);
		register(preferenceAction);

		// openPreferencesDialog = new OpenPreferencesDialogAction(window);
		// register(openPreferencesDialog);

		// openDottyGraphFromFile = new OpenDottyGraphFromFileAction(window);
		// register(openDottyGraphFromFile);

		loadSourceFiles = new LoadSourceFilesAction(window, mCore, mController, mLogger);
		register(loadSourceFiles);
		resetAndReRun = new ResetAndRedoToolChainAction(window, mCore, mController, mLogger);
		register(resetAndReRun);
		resetAndReRunNewTC = new ResetAndRedoToolChainNewTCAction(window, mCore, mController, mLogger);
		register(resetAndReRunNewTC);
		resetAndReRunOldTC = new ResetAndRedoToolChainOldTCAction(window, mCore, mController, mLogger);
		register(resetAndReRunOldTC);
		loadSettings = new LoadSettingsAction(window, mCore);
		register(loadSettings);
		saveSettings = new SaveSettingsAction(window, mCore);
		register(saveSettings);
	}

	/**
	 * called by workbench the menu.
	 * 
	 * @param menuBar
	 */
	protected void fillMenuBar(IMenuManager menuBar) {
		final MenuManager fileMenu = new MenuManager("&File", "file");

		fileMenu.add(loadSourceFiles);
		// fileMenu.add(openDottyGraphFromFile);

		// fileMenu.add(preferenceAction);
		fileMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		fileMenu.add(new Separator());
		fileMenu.add(exitAction);

		MenuManager settingsMenu = new MenuManager("&Settings", "settings");
		settingsMenu.add(preferenceAction);
		settingsMenu.add(new Separator());
		settingsMenu.add(loadSettings);
		settingsMenu.add(saveSettings);

		MenuManager helpMenu = new MenuManager("&Help", "help");
		helpMenu.add(aboutAction);

		menuBar.add(fileMenu);
		menuBar.add(settingsMenu);
		menuBar.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menuBar.add(helpMenu);

	}

	protected void fillCoolBar(ICoolBarManager coolBar) {
		IToolBarManager toolBar = new ToolBarManager(coolBar.getStyle());
		coolBar.add(toolBar);

		toolBar.add(loadSourceFiles);
		toolBar.add(new Separator());
		toolBar.add(resetAndReRun);
		toolBar.add(resetAndReRunNewTC);
		toolBar.add(resetAndReRunOldTC);
		toolBar.add(loadSettings);
		toolBar.add(saveSettings);
		// toolBar.add(openDottyGraphFromFile);

		toolBar.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

	}
}
