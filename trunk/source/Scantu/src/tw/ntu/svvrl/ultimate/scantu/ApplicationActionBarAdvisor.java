package tw.ntu.svvrl.ultimate.scantu;

import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import tw.ntu.svvrl.ultimate.scantu.actions.*;
import tw.ntu.svvrl.ultimate.scantu.actions.toolchain_actions.*;

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
	private IWorkbenchAction mSaveFileAction;
	
	private IWorkbenchAction mCodeBeautifyAction;
	private IWorkbenchAction mAddAnnotationAboveAction;
	private IWorkbenchAction mAddAnnotationBelowAction;
	
	private IWorkbenchAction mRunSvvrlDebugToolchainAction;
	private IWorkbenchAction mRunDoubleDFSReductionToolchainAction;
	
	private IWorkbenchAction mRunCInline2BoogiePrinterToolchainAction;

	public ApplicationActionBarAdvisor(final IActionBarConfigurer configurer, final ICore<RunDefinition> icc,
			final ScantuController controller, final ILogger logger) {
		super(configurer);
		mCore = icc;
		mController = controller;
		mLogger = logger;
	}
	
	protected void makeActions(IWorkbenchWindow window) {
		
		mLoadFolderAction = registerAction(new LoadFolderAction(window));
		mSaveFileAction = registerAction(new SaveFileAction(window));
		
		mCodeBeautifyAction = registerAction(new CodeBeautifyAction(window));
		mAddAnnotationAboveAction = registerAction(new AddAnnotationAction(window, true));
		mAddAnnotationBelowAction = registerAction(new AddAnnotationAction(window, false));
		
		mRunSvvrlDebugToolchainAction = registerAction(
				new RunSvvrlDebugToolchainAction(mCore, mLogger, mController, window));
		mRunDoubleDFSReductionToolchainAction = registerAction(
				new RunDoubleDFSReductionToolchainAction(mCore, mLogger, mController, window));
		
		mRunCInline2BoogiePrinterToolchainAction = registerAction(
				new RunCInline2BoogiePrinterToolchainAction(mCore, mLogger, mController, window));
    }
	
	private IWorkbenchAction registerAction(final IWorkbenchAction action) {
		register(action);
		return action;
	}

	@Override
    protected void fillMenuBar(IMenuManager menuBar) {
    	final MenuManager fileMenu = new MenuManager("&File", "file");
    	fileMenu.add(mLoadFolderAction);
    	fileMenu.add(mSaveFileAction);
    	
    	final MenuManager editMenu = new MenuManager("&Edit", "edit");
    	editMenu.add(mCodeBeautifyAction);
    	editMenu.add(mAddAnnotationAboveAction);
    	editMenu.add(mAddAnnotationBelowAction);
    	
    	final MenuManager modelCheckerMenu = new MenuManager("&Model Checker", "model checker");
    	modelCheckerMenu.add(mRunSvvrlDebugToolchainAction);
    	modelCheckerMenu.add(mRunDoubleDFSReductionToolchainAction);
    	
    	final MenuManager toolMenu = new MenuManager("&Tool", "tool");
    	toolMenu.add(mRunCInline2BoogiePrinterToolchainAction);
    	
    	menuBar.add(fileMenu);
    	menuBar.add(editMenu);
    	menuBar.add(modelCheckerMenu);
    	menuBar.add(toolMenu);
    }
	
	@Override
	protected void fillCoolBar(final ICoolBarManager coolBar) {
		final IToolBarManager toolBar = new ToolBarManager(SWT.PUSH);
		toolBar.add(mLoadFolderAction);
		toolBar.add(mSaveFileAction);
		toolBar.add(new Separator());
		toolBar.add(mCodeBeautifyAction);
		toolBar.add(mAddAnnotationAboveAction);
		toolBar.add(mAddAnnotationBelowAction);
		
		coolBar.add(toolBar);
	}

}
