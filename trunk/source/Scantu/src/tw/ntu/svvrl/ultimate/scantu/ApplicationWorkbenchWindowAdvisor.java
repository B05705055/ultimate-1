package tw.ntu.svvrl.ultimate.scantu;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.gui.views.LoggingView;
import tw.ntu.svvrl.ultimate.scantu.lib.SaveFile;
import tw.ntu.svvrl.ultimate.scantu.views.ProgramView;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {
	
	private final ICore<RunDefinition> mCore;
	private final ScantuController mController;
	private final ILogger mLogger;

	public ApplicationWorkbenchWindowAdvisor(final IWorkbenchWindowConfigurer configurer,
			final ICore<RunDefinition> icc, final ScantuController controller, final ILogger logger) {
		super(configurer);
		mCore = icc;
		mController = controller;
		mLogger = logger;
	}
	
	@Override
	public ActionBarAdvisor createActionBarAdvisor(
			IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer, mCore, mController, mLogger);
	}

	@Override
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(1080, 720));
		configurer.setShowMenuBar(true);
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowPerspectiveBar(true);
		configurer.setShowProgressIndicator(true);
		configurer.setTitle("SCANTU - Source Code Analyzer from NTU");
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		String settingsFilePath= workspace.getRoot().getLocation().toFile().getParent().toString()
				+ "\\source\\Scantu\\toolchains\\SvvrlDebug.epf";
		mCore.loadPreferences(settingsFilePath, false);
	}
	
	@Override
	public void postWindowCreate() {
		super.postWindowCreate();
		final IWorkbenchWindow window = getWindowConfigurer().getWindow();
		final IViewPart view = window.getActivePage().findView(LoggingView.ID);
		if (view instanceof LoggingView) {
			final LoggingView lv = (LoggingView) view;
			lv.initializeLogging(mController.getLoggingService());
			mLogger.info("This is the syslog referenced from \"de.uni_freiburg.informatik.ultimate.gui.views.LoggingView\"");
		}
	}
	
	@Override
	public boolean preWindowShellClose() {
		if (!ProgramView.whetherSaved()) {
			final IWorkbenchWindow window = getWindowConfigurer().getWindow();
			final Shell shell = window.getShell();
			MessageBox dialog =
				    new MessageBox(shell, SWT.ICON_WARNING | SWT.YES| SWT.NO);
			dialog.setText("Warning");
			dialog.setMessage("Your edited file is unsaved, do you want to save it?");
			int returnCode = dialog.open();
			if (returnCode == SWT.YES) {
				SaveFile.save(window);
			}
		}
		return true;
	}
}
