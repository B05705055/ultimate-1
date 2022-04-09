package de.uni_freiburg.informatik.ultimate.gui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.gui.GuiController;

public abstract class LoadPluginAction extends Action {
	
	protected final ICore<RunDefinition> mCore;
	protected final IWorkbenchWindow mWindow;
	protected final GuiController mController;
	protected final ILogger mLogger;
	
	public LoadPluginAction(final IWorkbenchWindow window, final ICore<RunDefinition> icore,
			final GuiController controller, final ILogger logger, final String label) {
		super();
		
		setId(getClass().getName());
		setText(label);
		mWindow = window;
		mCore = icore;
		mController = controller;
		mLogger = logger;
	}
	
	@Override
	public void run() {
		
	}

}