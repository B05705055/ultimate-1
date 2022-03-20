package tw.ntu.svvrl.ultimate.scantu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import de.uni_freiburg.informatik.ultimate.core.lib.results.ResultSummarizer;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.IController;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.ISource;
import de.uni_freiburg.informatik.ultimate.core.model.ITool;
import de.uni_freiburg.informatik.ultimate.core.model.IToolchain;
import de.uni_freiburg.informatik.ultimate.core.model.IToolchainData;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.model.results.IResult;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILoggingService;
import tw.ntu.svvrl.ultimate.scantu.advisors.ScantuWorkbenchAdvisor;

public class ScantuController implements IController<RunDefinition> {
	
	public static final String PLUGIN_ID = ScantuController.class.getPackage().getName();
	public static final String PLUGIN_NAME = "Scantu Controller";
	
	private ILogger mLogger;
	private Display mDisplay;

	private volatile ISource mParser;
	private volatile IToolchainData<RunDefinition> mTools;
	private volatile List<String> mModels;

	private ICore<RunDefinition> mCore;
	private ScantuTrayIconNotifier mTrayIconNotifier;
	private IToolchain<RunDefinition> mCurrentToolchain;
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
		return null;
	}

	@Override
	public int init(ICore<RunDefinition> core) {
		if (core == null) {
			throw new IllegalArgumentException("core may not be null");
		}
		mLoggingService = core.getCoreLoggingService();
		mLogger = mLoggingService.getControllerLogger();
		mCore = core;
		mDisplay = PlatformUI.createDisplay();
		if (mDisplay == null) {
			mLogger.fatal("PlatformUI.createDisplay() delivered null-value, cannot create workbench, exiting...");
			return -1;
		}
		
		mParser = null;
		mModels = new ArrayList<>();
		if (mLogger.isDebugEnabled()) {
			mLogger.debug("[Scantu] Creating Workbench ...");
			mLogger.debug("--------------------------------------------------------------------------------");
		}
		int returnCode = -1;
		
		
		final ScantuWorkbenchAdvisor workbenchAdvisor = new ScantuWorkbenchAdvisor();
		mTrayIconNotifier = new ScantuTrayIconNotifier(workbenchAdvisor);
		workbenchAdvisor.init(mCore, mTrayIconNotifier, this, mLogger);
		try {
			returnCode = PlatformUI.createAndRunWorkbench(mDisplay, workbenchAdvisor);
			if (mLogger.isDebugEnabled()) {
				mLogger.debug("GUI return code: " + returnCode);
			}
			return returnCode;
		} catch (final Exception ex) {
			mLogger.fatal("An exception occured", ex);
			return returnCode;
		} finally {
			setAndReleaseToolchain(null);
			mDisplay.dispose();
		}
	}
	
	public void setAndReleaseToolchain(final IToolchain<RunDefinition> toolchain) {
		if (mCurrentToolchain != null && !mCurrentToolchain.equals(toolchain)) {
			mCore.releaseToolchain(mCurrentToolchain);
		}
		setCurrentToolchain(toolchain);
	}
	
	public void setCurrentToolchain(final IToolchain<RunDefinition> toolchain) {
		mCurrentToolchain = toolchain;
	}
	
	public IToolchain<RunDefinition> getCurrentToolchain() {
		return mCurrentToolchain;
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
		return tcData;
	}

	@Override
	public void displayToolchainResults(IToolchainData<RunDefinition> toolchain, Map<String, List<IResult>> results) {
		// TODO Auto-generated method stub
		final ResultSummarizer summarizer = new ResultSummarizer(results);
		switch (summarizer.getResultSummary()) {
		case CORRECT:
			mTrayIconNotifier.showTrayBalloon("Program is correct", "Ultimate proved your program to be correct!",
					SWT.ICON_INFORMATION);
			break;
		case INCORRECT:
			mTrayIconNotifier.showTrayBalloon("Program is incorrect", "Ultimate proved your program to be incorrect!",
					SWT.ICON_WARNING);
			break;
		default:
			mTrayIconNotifier.showTrayBalloon("Program could not be checked",
					"Ultimate could not prove your program: " + summarizer.getResultDescription(),
					SWT.ICON_INFORMATION);
			break;
		}
	}

	@Override
	public void displayException(IToolchainData<RunDefinition> toolchain, String description, Throwable ex) {
		// TODO Auto-generated method stub
		
	}
	
	public ILoggingService getLoggingService() {
		return mLoggingService;
	}
	
	
}