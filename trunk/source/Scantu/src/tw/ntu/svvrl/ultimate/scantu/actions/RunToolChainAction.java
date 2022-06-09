package tw.ntu.svvrl.ultimate.scantu.actions;

import java.io.File;
import java.io.FileNotFoundException;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import javax.xml.bind.JAXBException;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.xml.sax.SAXException;

import de.uni_freiburg.informatik.ultimate.core.coreplugin.toolchain.BasicToolchainJob;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import tw.ntu.svvrl.ultimate.scantu.ScantuController;
import tw.ntu.svvrl.ultimate.scantu.views.FolderView;

public abstract class RunToolchainAction extends Action implements IWorkbenchAction {
	
	protected String mToolchainName;
	protected String mToolchainPath;
	
	protected final ICore<RunDefinition> mCore;
	protected final ILogger mLogger;
	protected final ScantuController mController;
	
	protected final IWorkbenchWindow mWorkbenchWindow;
	
	public RunToolchainAction(final ICore<RunDefinition> icore, final ILogger logger,
		final ScantuController controller, final IWorkbenchWindow window, final String id,
		final String label, final String toolchainName) {
		setId(id);
		setText(label);
		mLogger = logger;
		mCore = icore;
		mController = controller;
		mWorkbenchWindow = window;
		
		mToolchainName = toolchainName;
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		String toolchainPath= workspace.getRoot().getLocation().toFile().getParent().toString()
				+ "\\source\\Scantu\\toolchains\\" + toolchainName + ".xml";
		mToolchainPath = toolchainPath;
	}

	@Override
	public final void run() {		
		final File[] fp = FolderView.getInputFile();
		if(fp == null) {
			mLogger.fatal("You have not selected a C file to analyze!");
			return;
		}
		
		mLogger.info("-------------The current toolchain is: " + mToolchainName + "-------------");
		
		try {
			mController.setTools(mToolchainPath);
		} catch (FileNotFoundException | JAXBException | SAXException e) {
			e.printStackTrace();
		}
		
		final BasicToolchainJob tcj = new ScantuToolchainJob("Processing Toolchain", mCore, mController, mLogger, fp);
		tcj.schedule();
	}
	
	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
	
}