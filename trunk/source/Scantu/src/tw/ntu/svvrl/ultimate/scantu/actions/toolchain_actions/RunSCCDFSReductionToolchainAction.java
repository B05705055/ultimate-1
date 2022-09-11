package tw.ntu.svvrl.ultimate.scantu.actions.toolchain_actions;

import org.eclipse.ui.IWorkbenchWindow;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import tw.ntu.svvrl.ultimate.scantu.ScantuController;
import tw.ntu.svvrl.ultimate.scantu.actions.RunToolchainAction;


/**
 * The SCC-based DFS algorithm with reduction has not yet completed
 */
public class RunSCCDFSReductionToolchainAction extends RunToolchainAction {
	
	private static final String LABEL = "Run SCC-based DFS with Reduction";
	private static final String TOOLCHAIN_NAME = "SCCDFSReduction";

	public RunSCCDFSReductionToolchainAction(ICore<RunDefinition> icore, ILogger logger, ScantuController controller,
			IWorkbenchWindow window) {
		super(icore, logger, controller, window, RunSCCDFSReductionToolchainAction.class.getName(), LABEL, TOOLCHAIN_NAME);
	}
	
}