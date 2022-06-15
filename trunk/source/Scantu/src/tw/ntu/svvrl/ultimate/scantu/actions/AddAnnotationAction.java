package tw.ntu.svvrl.ultimate.scantu.actions;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import tw.ntu.svvrl.ultimate.scantu.ScantuController;
import tw.ntu.svvrl.ultimate.scantu.dialogs.AddAnnotationDialog;
import tw.ntu.svvrl.ultimate.scantu.views.ProgramView;

public class AddAnnotationAction extends Action implements IWorkbenchAction {
	
	private final String IMAGE_PATH_ABOVE = "icons/InsertAbove.png";
	private final String IMAGE_PATH_BELOW = "icons/InsertBelow.png";
	
	protected final IWorkbenchWindow mWorkbenchWindow;
	protected final boolean above;
	
	public AddAnnotationAction(final IWorkbenchWindow window, final boolean whetherAbove) {
		setId(getClass().getName());
		if (whetherAbove) {
			setText("Add Annotation Above");
			setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(ScantuController.PLUGIN_ID, IMAGE_PATH_ABOVE));
		}
		else {
			setText("Add Annotation Below");
			setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(ScantuController.PLUGIN_ID, IMAGE_PATH_BELOW));
		}
		mWorkbenchWindow = window;
		above = whetherAbove;
	}
	
	@Override
	public void run() {
		if (ProgramView.getSelectedStringIdx() == -1) {
			MessageBox dialog =
					new MessageBox(mWorkbenchWindow.getShell(), SWT.ICON_ERROR | SWT.OK);
			dialog.setText("Error");
			dialog.setMessage("You haven't selected a line of code to insert your annotation.");
			dialog.open();
			return;
		}
		AddAnnotationDialog mAddAnnotationDialog = new AddAnnotationDialog(mWorkbenchWindow.getShell());
		ArrayList<String> result = mAddAnnotationDialog.open();
		ProgramView.addAnnotation(result.get(1), result.get(0), above);
	}

	@Override
	public void dispose() {
	}
}