package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationV2;

import de.uni_freiburg.informatik.ultimate.access.IUnmanagedObserver;
import de.uni_freiburg.informatik.ultimate.access.WalkerOptions;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.model.GraphType;
import de.uni_freiburg.informatik.ultimate.model.IElement;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootNode;

public class AbstractInterpretationObserver implements IUnmanagedObserver {

	private final IUltimateServiceProvider mServices;

	public AbstractInterpretationObserver(IUltimateServiceProvider services) {
		mServices = services;
	}

	@Override
	public void init(GraphType modelType, int currentModelIndex, int numberOfModels) throws Throwable {
		// TODO Auto-generated method stub

	}

	@Override
	public void finish() throws Throwable {
		// TODO Auto-generated method stub

	}

	@Override
	public WalkerOptions getWalkerOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean performedChanges() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean process(IElement root) throws Throwable {
		if (root instanceof RootNode) {
			AbstractInterpreter abstractInterpreter = new AbstractInterpreter(mServices);
			abstractInterpreter.processRcfg((RootNode) root);
			
			return false;
		}
		return true;
	}
}
