package tw.ntu.svvrl.ultimate.scantu;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import tw.ntu.svvrl.ultimate.scantu.actions.LoadFolderAction;
import tw.ntu.svvrl.ultimate.scantu.actions.toolchain_actions.RunSvvrlDebugToolchainAction;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of
 * the actions added to a workbench window. Each window will be populated with
 * new actions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	// Actions - important to allocate these only in makeActions, and then use
	// them
	// in the fill methods. This ensures that the actions aren't recreated
	// when fillActionBars is called with FILL_PROXY.
	
	private final ICore<RunDefinition> mCore;
	private final ScantuController mController;
	private final ILogger mLogger;
	
	private IWorkbenchAction mLoadFolderAction;
	
	private IWorkbenchAction mRunSvvrlDebugToolchainAction;

	public ApplicationActionBarAdvisor(final IActionBarConfigurer configurer, final ICore<RunDefinition> icc,
			final ScantuController controller, final ILogger logger) {
		super(configurer);
		mCore = icc;
		mController = controller;
		mLogger = logger;
	}
	
	protected void makeActions(IWorkbenchWindow window) {
		
		mLoadFolderAction = registerAction(new LoadFolderAction(window));
		mRunSvvrlDebugToolchainAction = registerAction(new RunSvvrlDebugToolchainAction(mCore, mLogger, mController, window));
    }
	
	private IWorkbenchAction registerAction(final IWorkbenchAction action) {
		register(action);
		return action;
	}

    protected void fillMenuBar(IMenuManager menuBar) {
    	final MenuManager fileMenu = new MenuManager("&File", "file");
    	
    	fileMenu.add(mLoadFolderAction);
    	fileMenu.add(mRunSvvrlDebugToolchainAction);
    	
    	menuBar.add(fileMenu);
    }

}
