package tw.ntu.svvrl.ultimate.scantu.actions;

import java.io.File;

import de.uni_freiburg.informatik.ultimate.core.coreplugin.toolchain.DefaultToolchainJob;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import tw.ntu.svvrl.ultimate.scantu.ScantuController;

public class ScantuToolchainJob extends DefaultToolchainJob {

	public ScantuToolchainJob(String name, ICore<RunDefinition> core, ScantuController controller,
			ILogger logger, File[] input) {
		super(name, core, controller, logger, input);
	}
	
}