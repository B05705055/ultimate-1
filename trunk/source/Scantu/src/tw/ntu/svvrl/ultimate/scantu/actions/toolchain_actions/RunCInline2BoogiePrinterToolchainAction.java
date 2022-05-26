package tw.ntu.svvrl.ultimate.scantu.actions.toolchain_actions;

import org.eclipse.ui.IWorkbenchWindow;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import tw.ntu.svvrl.ultimate.scantu.ScantuController;
import tw.ntu.svvrl.ultimate.scantu.actions.RunToolchainAction;

public class RunCInline2BoogiePrinterToolchainAction extends RunToolchainAction {
	
	private static final String LABEL = "Run CInline2BoogiePrinter";
	private static final String TOOLCHAIN_NAME = "CInline2BoogiePrinter";

	public RunCInline2BoogiePrinterToolchainAction(ICore<RunDefinition> icore, ILogger logger, ScantuController controller,
			IWorkbenchWindow window) {
		super(icore, logger, controller, window, RunSvvrlDebugToolchainAction.class.getName(), LABEL, TOOLCHAIN_NAME);
	}
	
}