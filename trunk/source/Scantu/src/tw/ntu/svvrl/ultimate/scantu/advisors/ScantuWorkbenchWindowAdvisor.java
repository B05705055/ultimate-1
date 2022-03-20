package tw.ntu.svvrl.ultimate.scantu.advisors;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.gui.preferencepages.UltimatePreferencePageFactory;
import de.uni_freiburg.informatik.ultimate.gui.views.LoggingView;
import tw.ntu.svvrl.ultimate.scantu.ScantuController;
import tw.ntu.svvrl.ultimate.scantu.ScantuTrayIconNotifier;

public class ScantuWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {
	
	private final ICore<RunDefinition> mCore;
	private final ScantuController mController;
	private final ILogger mLogger;
	
	public ScantuWorkbenchWindowAdvisor(final IWorkbenchWindowConfigurer configurer,
			final ICore<RunDefinition> icc, final ScantuTrayIconNotifier notifier, final ScantuController controller,
			final ILogger logger) {
		super(configurer);
		mCore = icc;
		mController = controller;
		mLogger = logger;
	}
	
	public ActionBarAdvisor createActionBarAdvisor(final IActionBarConfigurer configurer) {
		return new ScantuActionBarAdvisor(configurer, mCore, mController, mLogger);
	}
	
	@Override
	public void preWindowOpen() {
		final IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setTitle("Scantu");
		configurer.setInitialSize(new Point(1024, 768));
		configurer.setShowMenuBar(true);
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowPerspectiveBar(true);
		configurer.setShowProgressIndicator(true);
		new UltimatePreferencePageFactory(mCore).createPreferencePages();

	}
	
	@Override
	public void postWindowCreate() {
		super.postWindowCreate();
		final IWorkbenchWindow window = getWindowConfigurer().getWindow();
		final IViewPart view = window.getActivePage().findView(LoggingView.ID);
		if (view instanceof LoggingView) {
			final LoggingView lv = (LoggingView) view;
			lv.initializeLogging(mController.getLoggingService());
			mLogger.info("This is Ultimate GUI " + mCore.getUltimateVersionString());
		}
	}
}