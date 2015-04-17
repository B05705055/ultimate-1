package de.uni_freiburg.informatik.ultimate.boogie.procedureinliner;

import java.util.ArrayList;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.access.IObserver;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.ep.interfaces.IAnalysis;
import de.uni_freiburg.informatik.ultimate.model.GraphType;
import de.uni_freiburg.informatik.ultimate.boogie.preprocessor.TypeChecker;
import de.uni_freiburg.informatik.ultimate.boogie.procedureinliner.preferences.PreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.boogie.procedureinliner.preferences.PreferencesInlineSelector;

/**
 * Tool for inlining Boogie procedures.
 * Currently under construction. May contain some bugs.
 * 
 * @author schaetzc@informatik.uni-freiburg.de
 */
public class BoogieProcedureInliner implements IAnalysis {

	private IUltimateServiceProvider mServices;
	
	@Override
	public GraphType getOutputDefinition() {
		return null;
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
		return null;
	}

	@Override
	public void setInputDefinition(GraphType graphType) {

	}

	@Override
	public List<IObserver> getObservers() {
		ArrayList<IObserver> observers = new ArrayList<IObserver>();
		observers.add(new TypeChecker(mServices));
		observers.add(new Inliner(mServices, new PreferencesInlineSelector()));
//		observers.add(new TypeChecker(mServices)); // TODO remove (for debugging -- warns on wrong set types)
		return observers;
	}

	@Override
	public void setToolchainStorage(IToolchainStorage storage) {
	}

	@Override
	public void setServices(IUltimateServiceProvider services) {
		mServices = services;
	}

	@Override
	public void init() {
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
	public UltimatePreferenceInitializer getPreferences() {
		return new PreferenceInitializer();
	}

	@Override
	public void finish() {
	}

}
