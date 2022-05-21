package tw.ntu.svvrl.ultimate.scantu.actions;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import de.uni_freiburg.informatik.ultimate.core.coreplugin.toolchain.BasicToolchainJob;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import tw.ntu.svvrl.ultimate.scantu.ScantuController;
import tw.ntu.svvrl.ultimate.scantu.views.FolderView;

public abstract class RunToolchainAction extends Action implements IWorkbenchAction {
	
	protected final ICore<RunDefinition> mCore;
	protected final ILogger mLogger;
	protected final ScantuController mController;
	
	protected final IWorkbenchWindow mWorkbenchWindow;
	
	public RunToolchainAction(final ICore<RunDefinition> icore, final ILogger logger,
		final ScantuController controller, final IWorkbenchWindow window, final String id, final String label) {
		setId(id);
		setText(label);
		mLogger = logger;
		mCore = icore;
		mController = controller;
		mWorkbenchWindow = window;
	}
	
	protected File[] getInputFile() {
		return FolderView.getInputFile();
	}
	
	@Override
	public final void run() {
		final File[] fp = getInputFile();
		if(fp == null) {
			System.out.println("You have not chosen the C file to analyze!");
			return;
		}
		final BasicToolchainJob tcj = new ScantuToolchainJob("Toolchain Name:", mCore, mController, mLogger, fp);
		tcj.schedule();
	}
	
	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
	
}