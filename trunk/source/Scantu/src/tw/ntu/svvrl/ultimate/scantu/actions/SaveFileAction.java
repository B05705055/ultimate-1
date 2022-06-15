package tw.ntu.svvrl.ultimate.scantu.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import tw.ntu.svvrl.ultimate.scantu.ScantuController;
import tw.ntu.svvrl.ultimate.scantu.lib.SaveFile;
import tw.ntu.svvrl.ultimate.scantu.views.ProgramView;

public class SaveFileAction extends Action implements IWorkbenchAction {
	
	private final String IMAGE_PATH = "icons/SaveFile.png";
	
	protected final IWorkbenchWindow mWorkbenchWindow;
	
	public SaveFileAction(final IWorkbenchWindow window) {
		setId(getClass().getName());
		setText("Save As ..");
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(ScantuController.PLUGIN_ID, IMAGE_PATH));
		mWorkbenchWindow = window;
	}
	
	@Override
	public void run() {
		if (ProgramView.whetherSaved()) {
			return;
		}
		SaveFile.save(mWorkbenchWindow);
	}

	@Override
	public void dispose() {
	}
}