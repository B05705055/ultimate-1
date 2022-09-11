package tw.ntu.svvrl.ultimate.scantu.actions.toolchain_actions;

import org.eclipse.ui.IWorkbenchWindow;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import tw.ntu.svvrl.ultimate.scantu.ScantuController;
import tw.ntu.svvrl.ultimate.scantu.actions.RunToolchainAction;


/**
 * The SCC-based DFS algorithm has not yet completed
 */
public class RunSCCDFSToolchainAction extends RunToolchainAction {
	
	private static final String LABEL = "Run SCC-based DFS";
	private static final String TOOLCHAIN_NAME = "SCCDFS";

	public RunSCCDFSToolchainAction(ICore<RunDefinition> icore, ILogger logger, ScantuController controller,
			IWorkbenchWindow window) {
		super(icore, logger, controller, window, RunSCCDFSToolchainAction.class.getName(), LABEL, TOOLCHAIN_NAME);
	}
	
}