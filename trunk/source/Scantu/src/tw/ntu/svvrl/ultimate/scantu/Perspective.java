package tw.ntu.svvrl.ultimate.scantu;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import de.uni_freiburg.informatik.ultimate.gui.views.LoggingView;

public class Perspective implements IPerspectiveFactory {
	
	public static final String VIEW_LOGGING = "logging window";

	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);
		
		final String editorAreaId = layout.getEditorArea();
		layout.createPlaceholderFolder(VIEW_LOGGING, IPageLayout.BOTTOM, 0.8f, editorAreaId);
		
		layout.addView(LoggingView.ID, IPageLayout.BOTTOM, 0.5f, VIEW_LOGGING);
	}

}
