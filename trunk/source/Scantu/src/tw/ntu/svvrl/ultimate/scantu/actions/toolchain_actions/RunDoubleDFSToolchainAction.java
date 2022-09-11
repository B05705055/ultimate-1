package tw.ntu.svvrl.ultimate.scantu.actions.toolchain_actions;

import org.eclipse.ui.IWorkbenchWindow;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import tw.ntu.svvrl.ultimate.scantu.ScantuController;
import tw.ntu.svvrl.ultimate.scantu.actions.RunToolchainAction;

public class RunDoubleDFSToolchainAction extends RunToolchainAction {
	
	private static final String LABEL = "Run Double DFS";
	private static final String TOOLCHAIN_NAME = "DoubleDFS";

	public RunDoubleDFSToolchainAction(ICore<RunDefinition> icore, ILogger logger, ScantuController controller,
			IWorkbenchWindow window) {
		super(icore, logger, controller, window, RunDoubleDFSToolchainAction.class.getName(), LABEL, TOOLCHAIN_NAME);
	}
	
}