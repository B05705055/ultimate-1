package tw.ntu.svvrl.ultimate.scantu;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import de.uni_freiburg.informatik.ultimate.gui.views.LoggingView;

public class ScantuDefaultPerspective implements IPerspectiveFactory {
	
	public static final String ID = "tw.ntu.svvrl.ultimate.scantu.ScantuDefaultPerspective";
	public static final String FOLDER_RIGHT = ID + ".FolderRight";
	public static final String FOLDER_LEFT = ID + ".FolderLeft";
	public static final String FOLDER_BOTTOM = ID + ".FolderBottom";

	@Override
	public void createInitialLayout(IPageLayout layout) {

		final String editorAreaId = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);
		layout.createPlaceholderFolder(FOLDER_RIGHT, IPageLayout.RIGHT, 0.8f, editorAreaId);
		layout.createPlaceholderFolder(FOLDER_LEFT, IPageLayout.LEFT, 0.8f, editorAreaId);
		layout.createPlaceholderFolder(FOLDER_BOTTOM, IPageLayout.BOTTOM, 0.8f, editorAreaId);

		layout.addView(LoggingView.ID, IPageLayout.BOTTOM, 0.5f, FOLDER_BOTTOM);
		
	}
}