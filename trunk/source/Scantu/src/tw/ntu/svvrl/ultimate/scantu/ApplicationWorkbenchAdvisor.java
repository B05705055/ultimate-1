package tw.ntu.svvrl.ultimate.scantu;

import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private static final String PERSPECTIVE_ID = "tw.ntu.svvrl.ultimate.scantu.perspective";
	
	private ILogger mLogger;
	private ICore<RunDefinition> mCore;
	private ScantuController mController;
	
	public void init(final ICore<RunDefinition> icc, final ScantuController controller,
			final ILogger logger) {
		mLogger = logger;
		mCore = icc;
		mController = controller;
	}

	@Override
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor(configurer, mCore, mController, mLogger);
	}
	
	@Override
	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}

}
