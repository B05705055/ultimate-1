package tw.ntu.svvrl.ultimate.scantu.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import tw.ntu.svvrl.ultimate.scantu.ScantuController;
import tw.ntu.svvrl.ultimate.scantu.dialogs.AddAnnotationDialog;

public class AddAnnotationAction extends Action implements IWorkbenchAction {
	
private final String IMAGE_PATH = "icons/LoadFolder.png";
	
	protected final IWorkbenchWindow mWorkbenchWindow;
	
	public AddAnnotationAction(final IWorkbenchWindow window) {
		setId(getClass().getName());
		setText("Add Annotation");
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(ScantuController.PLUGIN_ID, IMAGE_PATH));
		mWorkbenchWindow = window;
	}
	
	@Override
	public void run() {
		AddAnnotationDialog mAddAnnotationDialog = new AddAnnotationDialog(mWorkbenchWindow.getShell());
		String result = mAddAnnotationDialog.open();
	}

	@Override
	public void dispose() {
	}
}