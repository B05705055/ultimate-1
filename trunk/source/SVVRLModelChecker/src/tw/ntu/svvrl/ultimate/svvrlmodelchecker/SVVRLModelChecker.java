package tw.ntu.svvrl.ultimate.svvrlmodelchecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.core.model.IGenerator;
import de.uni_freiburg.informatik.ultimate.core.model.models.IElement;
import de.uni_freiburg.informatik.ultimate.core.model.models.ModelType;
import de.uni_freiburg.informatik.ultimate.core.model.observers.IObserver;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.preferences.CACSLPreferenceInitializer;

/**
 * @author Markus Lindenmann
 * @author Oleksii Saukh
 * @author Stefan Wissert
 * @date 03.02.2012
 */
public class CACSL2BoogieTranslator implements IGenerator {
	private CACSL2BoogieTranslatorObserver mObserver;
	private ACSLObjectContainerObserver mAdditionalAnnotationObserver;
	private ModelType mInputDefinition;
	private IUltimateServiceProvider mServices;

	@Override
	public String getPluginName() {
		return Activator.PLUGIN_NAME;
	}

	@Override
	public String getPluginID() {
		return Activator.PLUGIN_ID;
	}

	@Override
	public void init() {
		mAdditionalAnnotationObserver =
				new ACSLObjectContainerObserver(mServices.getLoggingService().getLogger(Activator.PLUGIN_ID));
		mObserver = new CACSL2BoogieTranslatorObserver(mServices, mAdditionalAnnotationObserver);

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
	public void setInputDefinition(final ModelType graphType) {
		mInputDefinition = graphType;
	}

	@Override
	public List<IObserver> getObservers() {
		final ArrayList<IObserver> observer = new ArrayList<>();
		observer.add(mAdditionalAnnotationObserver);
		observer.add(mObserver);
		return observer;
	}

	@Override
	public ModelType getOutputDefinition() {
		return new ModelType(Activator.PLUGIN_ID, mInputDefinition.getType(), mInputDefinition.getFileNames());
	}

	@Override
	public IElement getModel() {
		if (mAdditionalAnnotationObserver.waitForMe()) {
			return null;
		}
		return mObserver.getRoot();
	}

	@Override
	public boolean isGuiRequired() {
		return false;
	}

	@Override
	public IPreferenceInitializer getPreferences() {
		return new CACSLPreferenceInitializer();
	}

	@Override
	public void setServices(final IUltimateServiceProvider services) {
		mServices = services;
	}

	@Override
	public void finish() {
		// no cleanup needed
	}
}
