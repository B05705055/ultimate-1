package tw.ntu.svvrl.ultimate.scantu;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.IController;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.ISource;
import de.uni_freiburg.informatik.ultimate.core.model.ITool;
import de.uni_freiburg.informatik.ultimate.core.model.IToolchainData;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.model.results.IResult;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILoggingService;
import tw.ntu.svvrl.ultimate.scantu.services.ScantuService;

public class ScantuController implements IController<RunDefinition> {
	
	public static final String PLUGIN_ID = ScantuController.class.getPackage().getName();
	public static final String PLUGIN_NAME = "Scantu Controller";
	
	private Display mDisplay;
	
	private ILogger mLogger;
	private ICore<RunDefinition> mCore;
	private ILoggingService mLoggingService;

	@Override
	public String getPluginName() {
		return PLUGIN_NAME;
	}

	@Override
	public String getPluginID() {
		return PLUGIN_ID;
	}

	@Override
	public IPreferenceInitializer getPreferences() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int init(ICore<RunDefinition> core) {
		
		mLoggingService = core.getCoreLoggingService();
		mLogger = mLoggingService.getControllerLogger();
		mCore = core;
		
		mDisplay = PlatformUI.createDisplay();
		final ApplicationWorkbenchAdvisor workbenchAdvisor = new ApplicationWorkbenchAdvisor();
		workbenchAdvisor.init(mCore, this, mLogger);
		
		try {
			int returnCode = PlatformUI.createAndRunWorkbench(mDisplay, workbenchAdvisor);
			if (returnCode == PlatformUI.RETURN_RESTART) {
				return IApplication.EXIT_RESTART;
			}
			return IApplication.EXIT_OK;
		} finally {
			mDisplay.dispose();
		}
	}

	@Override
	public ISource selectParser(Collection<ISource> parser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IToolchainData<RunDefinition> selectTools(List<ITool> tools) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> selectModel(List<String> modelNames) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IToolchainData<RunDefinition> prerun(IToolchainData<RunDefinition> tcData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void displayToolchainResults(IToolchainData<RunDefinition> toolchain, Map<String, List<IResult>> results) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void displayException(IToolchainData<RunDefinition> toolchain, String description, Throwable ex) {
		// TODO Auto-generated method stub
		
	}
	
}