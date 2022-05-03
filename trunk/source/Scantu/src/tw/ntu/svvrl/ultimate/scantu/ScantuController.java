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

public class ScantuController implements IController<RunDefinition> {
	
	private Display mDisplay;

	@Override
	public String getPluginName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPluginID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPreferenceInitializer getPreferences() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int init(ICore<RunDefinition> core) {
		mDisplay = PlatformUI.createDisplay();
		try {
			int returnCode = PlatformUI.createAndRunWorkbench(mDisplay, new ApplicationWorkbenchAdvisor());
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