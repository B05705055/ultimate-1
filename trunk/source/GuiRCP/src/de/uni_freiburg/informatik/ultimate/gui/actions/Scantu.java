package de.uni_freiburg.informatik.ultimate.gui.actions;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.gui.GuiController;
import tw.ntu.svvrl.ultimate.scantu.ScantuGUI;

public class Scantu extends LoadPluginAction implements IWorkbenchAction {

	public Scantu(final IWorkbenchWindow window, final ICore<RunDefinition> icore,
			final GuiController controller, final ILogger logger) {
		super(window, icore, controller, logger, "Scantu");
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public final void run() {
		System.out.println("Start Scantu Model Checker");
		ScantuGUI scantuModelChecker = new ScantuGUI(mCore, mLogger);
		scantuModelChecker.open();
	}
	
}