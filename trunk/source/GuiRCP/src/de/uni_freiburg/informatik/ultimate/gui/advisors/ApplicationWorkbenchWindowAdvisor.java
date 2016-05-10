/*
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

import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.gui.GuiController;
import de.uni_freiburg.informatik.ultimate.gui.TrayIconNotifier;
import de.uni_freiburg.informatik.ultimate.gui.interfaces.IImageKeys;
import de.uni_freiburg.informatik.ultimate.gui.preferencepages.UltimatePreferencePageFactory;

import org.apache.log4j.Logger;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * 
 * @author dietsch
 * 
 */
public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	private final ICore mCore;
	private final GuiController mController;
	private TrayItem mTrayItem;
	private Image mTrayImage;
	private TrayIconNotifier mTrayIconNotifier;
	private Logger mLogger;

	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer, ICore icc,
			TrayIconNotifier notifier, GuiController controller, Logger logger) {
		super(configurer);
		mCore = icc;
		mTrayIconNotifier = notifier;
		mController = controller;
		mLogger = logger;

	}

	public TrayItem getTrayItem() {
		return mTrayItem;
	}

	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer, mCore, mController, mLogger);
	}

	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setTitle("Ultimate");
		configurer.setInitialSize(new Point(1024, 768));
		configurer.setShowMenuBar(true);
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowPerspectiveBar(true);
		configurer.setShowProgressIndicator(true);
		new UltimatePreferencePageFactory(mCore).createPreferencePages();

	}

	public void postWindowOpen() {
		super.postWindowOpen();
		// Deactivate the tray icon for now since it does not work correctly on
		// linux and nobody seems to have a good idea how to fix it and nobody
		// really wants to have a tray icon.
//		final IWorkbenchWindow window = getWindowConfigurer().getWindow();
//		if (initTaskItem(window)) {
//			hookMinimized(window);
//		}

	}

	public void dispose() {
		if (mTrayImage != null) {
			mTrayImage.dispose();
		}
		if (mTrayItem != null) {
			mTrayItem.dispose();
		}
	}

	private void hookMinimized(final IWorkbenchWindow window) {
		// This listener leads to a bug on linux where the window never
		// reappears after it has been minimized into the tray.  The bug stems
		// from the fact that the listener is called while processing
		// shell.setVisible(true)
//		window.getShell().addShellListener(new ShellAdapter() {
//			public void shellIconified(ShellEvent e) {
//				if (!mTrayIconNotifier.isResultDisplayActive()) {
//					window.getShell().setVisible(false);
//				}
//			}
//		});
		mTrayItem.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event e) {
				Shell shell = window.getShell();
				// Modified this event handler for the case where we never hide
				// the shell (by a call to setVisible(false)).
//				if (!shell.isVisible()) {
					shell.setVisible(true);
					shell.setMinimized(false);
					shell.setActive();
//					shell.setFocus();
//				} else {
//					shell.setMinimized(false);
					shell.forceActive();
					shell.setFocus();
//				}
			}
		});
	}

	/**
	 * Returns true if the tray icon was initialized successfully, false
	 * otherwise
	 * 
	 * @param window
	 * @return
	 */
	private boolean initTaskItem(IWorkbenchWindow window) {
		final Tray tray = window.getShell().getDisplay().getSystemTray();
		if (tray == null) {
			return false;
		}
		mTrayItem = new TrayItem(tray, SWT.NONE);
		ImageDescriptor id = AbstractUIPlugin.imageDescriptorFromPlugin(GuiController.sPLUGINID, IImageKeys.TRAYICON);
		mTrayImage = id.createImage();
		mTrayItem.setImage(mTrayImage);
		mTrayItem.setToolTipText("Ultimate Model Checker");

		final Menu menu = new Menu(window.getShell(), SWT.POP_UP);
		final MenuItem exit = new MenuItem(menu, SWT.PUSH);
		exit.setText("Exit Ultimate");
		exit.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				exit.dispose();
				menu.dispose();
				getWindowConfigurer().getWorkbenchConfigurer().getWorkbench().close();
			}
		});
		mTrayItem.addListener(SWT.MenuDetect, new Listener() {
			public void handleEvent(Event event) {
				menu.setVisible(true);
			}
		});

		return true;
	}

}
