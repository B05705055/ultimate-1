package tw.ntu.svvrl.ultimate.scantu;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;

import de.uni_freiburg.informatik.ultimate.core.coreplugin.preferences.CorePreferenceInitializer;
import tw.ntu.svvrl.ultimate.scantu.advisors.ScantuWorkbenchAdvisor;

public class ScantuTrayIconNotifier {
	
	private final ScantuWorkbenchAdvisor mWorkbenchAdvisor;
	private final boolean mIsResultDisplayActive;

	ScantuTrayIconNotifier(ScantuWorkbenchAdvisor workbenchAdvisor) {
		mWorkbenchAdvisor = workbenchAdvisor;
		mIsResultDisplayActive = false;
	}

	public boolean isResultDisplayActive() {
		return mIsResultDisplayActive;
	}

	private boolean isTrayBalloonEnabled() {
		final IPreferencesService prefService = Platform.getPreferencesService();
		if (prefService == null) {
			return false;
		}
		return prefService.getBoolean("UltimateCore", CorePreferenceInitializer.LABEL_SHOWRESULTNOTIFIERPOPUP,
				CorePreferenceInitializer.VALUE_SHOWRESULTNOTIFIERPOPUP_DEFAULT, null);
	}

	void showTrayBalloon(final String shortMessage, final String longMessage, final int style) {
		if (!isTrayBalloonEnabled()) {
			return;
		}
	}
}