package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2;

import java.util.Collections;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.access.IObserver;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.ep.interfaces.IAnalysis;
import de.uni_freiburg.informatik.ultimate.model.GraphType;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.AIActivator;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.AbstractInterpretationObserver;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationMk2.preferences.AbstractInterpretationPreferenceInitializer;

/**
 * Main class of Plug-In AbstractInterpretation
 * 
 * @author Jan H�ttig
 */
public class AbstractInterpretation implements IAnalysis {

	private AbstractInterpretationObserver m_Observer;
	private GraphType m_InputDefinition;

	private IUltimateServiceProvider mServices;

	@Override
	public GraphType getOutputDefinition() {
		return new GraphType(AIActivator.s_PLUGIN_ID,
				m_InputDefinition.getType(), m_InputDefinition.getFileNames());
	}

	@Override
	public boolean isGuiRequired() {
		return false;
	}

	@Override
	public QueryKeyword getQueryKeyword() {
		return QueryKeyword.LAST;
	}

	@Override
	public List<String> getDesiredToolID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setInputDefinition(GraphType graphType) {
		this.m_InputDefinition = graphType;
	}

	@Override
	public List<IObserver> getObservers() {
		return Collections.singletonList((IObserver) m_Observer);
	}

	@Override
	public void init() {
		m_Observer = new AbstractInterpretationObserver(mServices);
	}

	@Override
	public String getPluginName() {
		return AIActivator.s_PLUGIN_NAME;
	}

	@Override
	public String getPluginID() {
		return AIActivator.s_PLUGIN_ID;
	}

	@Override
	public UltimatePreferenceInitializer getPreferences() {
		return new AbstractInterpretationPreferenceInitializer();
	}

	@Override
	public void setToolchainStorage(IToolchainStorage storage) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setServices(IUltimateServiceProvider services) {
		mServices = services;
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub

	}

}
