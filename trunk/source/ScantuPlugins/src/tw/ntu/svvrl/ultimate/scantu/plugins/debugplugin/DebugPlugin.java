package tw.ntu.svvrl.ultimate.scantu.plugins.debugplugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tw.ntu.svvrl.ultimate.scantu.plugins.debugplugin.DebugPluginObserver;

import de.uni_freiburg.informatik.ultimate.core.model.IGenerator;
import de.uni_freiburg.informatik.ultimate.core.model.models.IElement;
import de.uni_freiburg.informatik.ultimate.core.model.models.ModelType;
import de.uni_freiburg.informatik.ultimate.core.model.observers.IObserver;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;

public class DebugPlugin implements IGenerator {
	
	private IUltimateServiceProvider mServices;
	private DebugPluginObserver mObserver;
	
	private boolean mUseDebugPluginObserver;

	@Override
	public ModelType getOutputDefinition() {
		final List<String> filenames = new ArrayList<>();
		filenames.add("Currently no model");
		return new ModelType(Activator.PLUGIN_ID, ModelType.Type.OTHER, filenames);
	}

	@Override
	public boolean isGuiRequired() {
		return false;
	}

	@Override
	public ModelQuery getModelQuery() {
		return ModelQuery.ALL;
	}

	@Override
	public List<String> getDesiredToolIds() {
		return Collections.emptyList();
	}

	@Override
	public void setInputDefinition(ModelType graphType) {
		switch (graphType.getCreator()) {
		case "de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder":
		case "de.uni_freiburg.informatik.ultimate.ltl2aut":
			mUseDebugPluginObserver = true;
			break;
		default:
			mUseDebugPluginObserver = false;
			break;
		}
	}

	@Override
	public List<IObserver> getObservers() {
		if (mUseDebugPluginObserver) {
			if (mObserver == null) {
				mObserver = new DebugPluginObserver(mServices);
			}
			return Collections.singletonList(mObserver);
		}
		return Collections.emptyList();
	}

	@Override
	public void setServices(IUltimateServiceProvider services) {
		mServices = services;
	}

	@Override
	public void init() {
		mUseDebugPluginObserver = false;
	}

	@Override
	public void finish() {
		// do nothing
	}

	@Override
	public String getPluginName() {
		return Activator.PLUGIN_NAME;
	}

	@Override
	public String getPluginID() {
		return Activator.PLUGIN_ID;
	}

	@Override
	public IPreferenceInitializer getPreferences() {
		return null;
	}

	@Override
	public IElement getModel() {
		return null;
	}
	
}