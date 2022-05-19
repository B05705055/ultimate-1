package tw.ntu.svvrl.ultimate.scantu.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

public class RunToolChainAction extends Action implements IWorkbenchAction {
	
	protected final IWorkbenchWindow mWorkbenchWindow;
	
	public RunToolChainAction(final IWorkbenchWindow window, final String id, final String label) {
		setId(id);
		setText(label);
		mWorkbenchWindow = window;
	}
	
	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
	
}