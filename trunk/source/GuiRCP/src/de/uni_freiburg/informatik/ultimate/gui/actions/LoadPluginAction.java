package de.uni_freiburg.informatik.ultimate.gui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;

public abstract class LoadPluginAction extends Action {
	
	private final ICore<?> mCore;
	private final IWorkbenchWindow mWindow;
	
	public LoadPluginAction(final IWorkbenchWindow window, final ICore<?> icore, final String label) {
		super();
		
		setId(getClass().getName());
		setText(label);
		mCore = icore;
		mWindow = window;
	}
	
	@Override
	public final void run() {
		
	}

}