package de.uni_freiburg.informatik.ultimate.gui.actions;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import de.uni_freiburg.informatik.ultimate.core.model.ICore;

public class Scantu extends LoadPluginAction implements IWorkbenchAction {

	public Scantu(IWorkbenchWindow window, ICore<?> icore) {
		super(window, icore, "Scantu");
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
	
}