package de.uni_freiburg.informatik.ultimate.gui.actions;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import tw.ntu.svvrl.ultimate.scantu.ScantuGUI;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;

public class Scantu extends LoadPluginAction implements IWorkbenchAction {

	public Scantu(IWorkbenchWindow window, ICore<?> icore) {
		super(window, icore, "Scantu");
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public final void run() {
		System.out.println("Start Scantu Model Checker");
		ScantuGUI scantuModelChecker = new ScantuGUI();
		scantuModelChecker.open();
	}
	
}