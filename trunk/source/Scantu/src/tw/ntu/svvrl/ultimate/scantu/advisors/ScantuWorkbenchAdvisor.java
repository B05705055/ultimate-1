package tw.ntu.svvrl.ultimate.scantu.advisors;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import tw.ntu.svvrl.ultimate.scantu.ScantuController;
import tw.ntu.svvrl.ultimate.scantu.ScantuDefaultPerspective;
import tw.ntu.svvrl.ultimate.scantu.ScantuTrayIconNotifier;

public class ScantuWorkbenchAdvisor extends WorkbenchAdvisor {
	
	private ILogger mLogger;
	private ICore<RunDefinition> mCore;
	private ScantuWorkbenchWindowAdvisor mApplicationWorkbenchWindowAdvisor;
	private ScantuTrayIconNotifier mTrayIconNotifier;
	private ScantuController mController;
	
	public void init(final ICore<RunDefinition> icc, final ScantuTrayIconNotifier notifier, final ScantuController controller,
			final ILogger logger) {
		mLogger = logger;
		mCore = icc;
		mTrayIconNotifier = notifier;
		mController = controller;
	}
	
	@Override
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(final IWorkbenchWindowConfigurer configurer) {
		mLogger.debug("Requesting WorkbenchWindowAdvisor");

		if (mCore == null || mTrayIconNotifier == null) {
			throw new IllegalStateException("mCore or mTrayIconNotifier are null; maybe you did not call init()?");
		}

		if (mApplicationWorkbenchWindowAdvisor == null) {
			mLogger.debug("Creating WorkbenchWindowAdvisor...");
			mApplicationWorkbenchWindowAdvisor =
					new ScantuWorkbenchWindowAdvisor(configurer, mCore, mTrayIconNotifier, mController, mLogger);
		}
		return mApplicationWorkbenchWindowAdvisor;
	}

	@Override
	public String getInitialWindowPerspectiveId() {
		return ScantuDefaultPerspective.ID;
	}
	
	public void initialize(final IWorkbenchConfigurer configurer) {
		super.initialize(configurer);
		configurer.setSaveAndRestore(!Platform.inDevelopmentMode());
	}
}