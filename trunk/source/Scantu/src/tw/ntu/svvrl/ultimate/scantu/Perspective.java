package tw.ntu.svvrl.ultimate.scantu;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import de.uni_freiburg.informatik.ultimate.gui.views.LoggingView;

public class Perspective implements IPerspectiveFactory {
	
	public static final String ID = "tw.ntu.svvrl.ultimate.scantu.views.perspective";
	public static final String FOLDER_LEFT = ID + ".FolderLeft";

	@Override
	public void createInitialLayout(IPageLayout layout) {
		
		layout.createPlaceholderFolder(FOLDER_LEFT, IPageLayout.LEFT, 0.2f, IPageLayout.ID_EDITOR_AREA);
		
		// final String editorAreaId = layout.getEditorArea();
		// layout.createPlaceholderFolder(VIEW_LOGGING, IPageLayout.BOTTOM, 0.8f, editorAreaId);
		
		layout.addView("tw.ntu.svvrl.ultimate.scantu.views.FolderView", IPageLayout.TOP, 0.6f, FOLDER_LEFT);
		layout.addView("tw.ntu.svvrl.ultimate.scantu.views.OperationView", IPageLayout.BOTTOM, 0.4f, FOLDER_LEFT);
		layout.addView(LoggingView.ID, IPageLayout.BOTTOM, 0.7f, IPageLayout.ID_EDITOR_AREA);
		layout.addView("tw.ntu.svvrl.ultimate.scantu.views.ProgramView", IPageLayout.RIGHT, 0.5f, IPageLayout.ID_EDITOR_AREA);
	}

}
