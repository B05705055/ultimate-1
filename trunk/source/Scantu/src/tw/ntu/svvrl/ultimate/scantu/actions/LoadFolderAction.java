package tw.ntu.svvrl.ultimate.scantu.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import tw.ntu.svvrl.ultimate.scantu.views.FolderView;

public class LoadFolderAction extends Action implements IWorkbenchAction {
	
	protected final IWorkbenchWindow mWorkbenchWindow;
	public String selectedDir;
	
	public LoadFolderAction(final IWorkbenchWindow window) {
		setId(getClass().getName());
		setText("Load Folder ..");
		mWorkbenchWindow = window;
	}
	
	@Override
	public void run() {
		
		//IWorkbenchPage activePage =
			//PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		
		//IViewPart folderView = activePage.findView("tw.ntu.svvrl.ultimate.scantu.views.FolderView");
		//FolderView mfolderView = (FolderView) folderView;
		
		final DirectoryDialog dd = new DirectoryDialog(mWorkbenchWindow.getShell());
		selectedDir = dd.open();
		
		FolderView.setDir(selectedDir);
		FolderView.refreshDir();
	}

	@Override
	public void dispose() {
	}
}