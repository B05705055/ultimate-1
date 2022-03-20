package tw.ntu.svvrl.ultimate.scantu.advisors;

import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import tw.ntu.svvrl.ultimate.scantu.ScantuController;

public class ScantuActionBarAdvisor extends ActionBarAdvisor {
	
	private final ICore<RunDefinition> mCore;
	private final ScantuController mController;
	private final ILogger mLogger;

	private IWorkbenchAction mExitAction;
	private IWorkbenchAction mAboutAction;
	private IWorkbenchAction mPreferenceAction;

	// custom actions
	/*private IWorkbenchAction mLoadSourceFiles;
	private IWorkbenchAction mResetAndReRun;
	private IWorkbenchAction mResetAndReRunNewTC;
	private IWorkbenchAction mResetAndReRunOldTC;
	private IWorkbenchAction mLoadSettings;
	private IWorkbenchAction mSaveSettings;
	private IWorkbenchAction mResetSettings;
	private IWorkbenchAction mCancelToolchain;*/

	public ScantuActionBarAdvisor(final IActionBarConfigurer configurer, final ICore<RunDefinition> icc,
			final ScantuController controller, final ILogger logger) {
		super(configurer);
		mCore = icc;
		mController = controller;
		mLogger = logger;
	}
	
	@Override
	protected final void makeActions(final IWorkbenchWindow window) {
		mExitAction = registerAction(ActionFactory.QUIT.create(window));
		mAboutAction = registerAction(ActionFactory.ABOUT.create(window));
		mPreferenceAction = registerAction(ActionFactory.PREFERENCES.create(window));

		/*mLoadSourceFiles = registerAction(new LoadSourceFilesAction(window, mCore, mController, mLogger));
		mResetAndReRun = registerAction(new ResetAndRedoToolChainAction(window, mCore, mController, mLogger));
		mResetAndReRunNewTC = registerAction(new ResetAndRedoToolChainNewTCAction(window, mCore, mController, mLogger));
		mResetAndReRunOldTC = registerAction(new ResetAndRedoToolChainOldTCAction(window, mCore, mController, mLogger));
		mLoadSettings = registerAction(new LoadSettingsAction(window, mCore));
		mSaveSettings = registerAction(new SaveSettingsAction(window, mCore));
		mResetSettings = registerAction(new ResetSettingsAction(mCore));
		mCancelToolchain = registerAction(new CancelToolchainAction(window, mController, mLogger));*/
	}
	
	private IWorkbenchAction registerAction(final IWorkbenchAction action) {
		register(action);
		return action;
	}
	
	@Override
	protected void fillMenuBar(final IMenuManager menuBar) {
		final MenuManager fileMenu = new MenuManager("&File", "file");

		/*fileMenu.add(mLoadSourceFiles);*/
		// fileMenu.add(openDottyGraphFromFile);

		// fileMenu.add(preferenceAction);
		fileMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		fileMenu.add(new Separator());
		fileMenu.add(mExitAction);

		final MenuManager settingsMenu = new MenuManager("&Settings", "settings");
		settingsMenu.add(mPreferenceAction);
		settingsMenu.add(new Separator());
		/*settingsMenu.add(mLoadSettings);
		settingsMenu.add(mSaveSettings);
		settingsMenu.add(mResetSettings);
		settingsMenu.add(mCancelToolchain);*/

		final MenuManager helpMenu = new MenuManager("&Help", "help");
		helpMenu.add(mAboutAction);

		menuBar.add(fileMenu);
		menuBar.add(settingsMenu);
		menuBar.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menuBar.add(helpMenu);

	}

	@Override
	protected void fillCoolBar(final ICoolBarManager coolBar) {
		final IToolBarManager toolBar = new ToolBarManager(SWT.PUSH);
		coolBar.add(toolBar);

		/*toolBar.add(mLoadSourceFiles);
		toolBar.add(new Separator());
		toolBar.add(mResetAndReRun);
		toolBar.add(mResetAndReRunNewTC);
		toolBar.add(mResetAndReRunOldTC);
		toolBar.add(new Separator());
		toolBar.add(mLoadSettings);
		toolBar.add(mSaveSettings);
		toolBar.add(mResetSettings);
		toolBar.add(mCancelToolchain);*/
	}
}