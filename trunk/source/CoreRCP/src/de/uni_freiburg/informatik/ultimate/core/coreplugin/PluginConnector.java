/*
 * Copyright (C) 2008-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE Core.
 * 
 * The ULTIMATE Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Core. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Core, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE Core grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.core.coreplugin;

import java.util.ArrayList;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.core.coreplugin.modelwalker.CFGWalker;
import de.uni_freiburg.informatik.ultimate.core.coreplugin.modelwalker.DFSTreeWalker;
import de.uni_freiburg.informatik.ultimate.core.coreplugin.modelwalker.IWalker;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.ToolchainData;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.ToolchainListType;
import de.uni_freiburg.informatik.ultimate.core.model.IController;
import de.uni_freiburg.informatik.ultimate.core.model.IGenerator;
import de.uni_freiburg.informatik.ultimate.core.model.ITool;
import de.uni_freiburg.informatik.ultimate.core.model.IToolchainPlugin;
import de.uni_freiburg.informatik.ultimate.core.model.models.IElement;
import de.uni_freiburg.informatik.ultimate.core.model.models.ModelType;
import de.uni_freiburg.informatik.ultimate.core.model.observers.IObserver;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;

//@formatter:off
/**
 * PluginConnector executes observers of a single {@link ITool} in a
 * {@link ToolchainData}. It uses the following live cycle:
 * <ul>
 * <li>Select all desired models according to {@link ITool#getModelQuery()}
 * <li>foreach model
 * <ul>
 * <li> {@link ITool#setInputDefinition(ModelType)}
 * <li> {@link ITool#getObservers()}
 * <li>execute each {@link IObserver} on the current model with
 * <ul>
 * <li> {@link IObserver#getWalkerOptions()}
 * <li> {@link IObserver#init(ModelType, int, int)}
 * <li>use an {@link IWalker} to run {@link IObserver} on model
 * <li> {@link IObserver#finish()}
 * <li>if tool is an instance of {@link IGenerator}, store model in the
 * ModelManager by first calling {@link IGenerator#getModel()} and then calling
 * {@link IGenerator#getOutputDefinition()}
 * </ul>
 * </ul>
 * </ul>
 * 
 * @author dietsch
 */
// @formatter:on
public class PluginConnector {

	private ILogger mLogger;

	private IModelManager mModelManager;
	private IController<ToolchainListType> mController;
	private ITool mTool;

	private boolean mHasPerformedChanges;

	private int mCurrent;
	private int mMax;

	private IToolchainStorage mStorage;
	private IUltimateServiceProvider mServices;

	public PluginConnector(IModelManager modelmanager, ITool tool, IController<ToolchainListType> control,
			IToolchainStorage storage, IUltimateServiceProvider services) {
		assert storage != null;
		assert control != null;
		assert modelmanager != null;
		assert tool != null;
		assert services != null;

		mModelManager = modelmanager;
		mController = control;
		mTool = tool;
		mStorage = storage;
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(Activator.PLUGIN_ID);
		init();
	}

	private void init() {
		mHasPerformedChanges = false;
		mCurrent = mMax = 0;
	}

	public void run() throws Throwable {
		mLogger.info("------------------------" + mTool.getPluginName() + "----------------------------");
		init();
		initializePlugin(mLogger, mTool, mServices, mStorage);
		List<ModelType> models = selectModels();
		if (models.isEmpty()) {
			IllegalArgumentException ex = new IllegalArgumentException();
			mLogger.error("Tool did not select a valid model", ex);
			throw ex;
		}
		mMax = models.size();
		mCurrent = 1;

		for (int i = mMax - 1; i >= 0; --i) {
			ModelType currentModel = models.get(i);
			mTool.setInputDefinition(currentModel);
			List<IObserver> observers = mTool.getObservers();
			runTool(observers, currentModel, mMax - i - 1, mMax);
			++mCurrent;
		}
		mTool.finish();
		mLogger.info("------------------------ END " + mTool.getPluginName() + "----------------------------");
	}

	public boolean hasPerformedChanges() {
		return mHasPerformedChanges;
	}

	@Override
	public String toString() {
		return mTool.getPluginName();
	}

	private void runTool(List<IObserver> observers, ModelType currentModel, int currentModelIndex, int numberOfModels)
			throws Throwable {
		IElement entryNode = getEntryPoint(currentModel);

		if (mTool instanceof IGenerator) {
			IGenerator tool = (IGenerator) mTool;
			for (IObserver observer : observers) {
				runObserver(observer, currentModel, entryNode, currentModelIndex, numberOfModels);
				retrieveModel(tool, observer.toString());
			}
		} else {
			for (IObserver observer : observers) {
				runObserver(observer, currentModel, entryNode, currentModelIndex, numberOfModels);
			}
		}
	}

	private void runObserver(IObserver observer, ModelType currentModel, IElement entryNode, int currentModelIndex,
			int numberOfModels) throws Throwable {
		logObserverRun(observer, currentModel);
		IWalker walker = selectWalker(currentModel);
		walker.addObserver(observer);
		observer.init(currentModel, currentModelIndex, numberOfModels);
		walker.run(entryNode);
		observer.finish();
		mHasPerformedChanges = mHasPerformedChanges || observer.performedChanges();
	}

	private void logObserverRun(IObserver observer, ModelType model) {
		StringBuilder sb = new StringBuilder();
		sb.append("Executing the observer ");
		sb.append(observer.getClass().getSimpleName());
		sb.append(" from plugin ");
		sb.append(mTool.getPluginName());
		sb.append(" for \"");
		sb.append(model);
		sb.append("\" (");
		sb.append(mCurrent);
		sb.append("/");
		sb.append(mMax);
		sb.append(") ...");
		mLogger.info(sb.toString());
	}

	private IElement getEntryPoint(ModelType definition) {
		IElement n = null;
		try {
			n = mModelManager.getRootNode(definition);
		} catch (GraphNotFoundException e) {
			mLogger.error("Graph not found!", e);
		}
		return n;
	}

	private void retrieveModel(IGenerator tool, String observer) {
		IElement element = tool.getModel();
		ModelType type = tool.getOutputDefinition();
		if (element != null && type != null) {
			mModelManager.addItem(element, type);
		} else {
			mLogger.debug(
					String.format("%s did return invalid model for observer %s, skipping insertion in model container",
							tool.getPluginName(), observer));
		}
	}

	private List<ModelType> selectModels() {
		List<ModelType> models = new ArrayList<ModelType>();

		switch (mTool.getModelQuery()) {
		case ALL:
			models.addAll(mModelManager.getItemKeys());
			break;
		case USER:
			if (mModelManager.size() > 1) {
				for (String s : mController.selectModel(mModelManager.getItemNames())) {
					ModelType t = mModelManager.getGraphTypeById(s);
					if (t != null) {
						models.add(t);
					}
				}
			} else {
				models.add(mModelManager.getItemKeys().get(0));
			}
			break;
		case LAST:
			models.add(mModelManager.getLastAdded());
			break;
		case SOURCE:
			models.addAll(mModelManager.getItemKeys());
			for (ModelType t : models) {
				if (!t.isFromSource()) {
					models.remove(t);
				}
			}
			break;
		case TOOL:
			List<String> desiredToolIDs = mTool.getDesiredToolID();
			if (desiredToolIDs == null || desiredToolIDs.size() == 0) {
				break;
			} else {
				models.addAll(mModelManager.getItemKeys());
				List<ModelType> removeModels = new ArrayList<ModelType>();
				for (ModelType t : models) {
					if (!desiredToolIDs.contains(t.getCreator()))
						removeModels.add(t);
				}
				models.removeAll(removeModels);
				break;
			}
		default:
			IllegalStateException ex = new IllegalStateException("Unknown Query type");
			mLogger.fatal("Unknown Query type", ex);
			throw ex;
		}
		if (models.isEmpty()) {
			mLogger.warn("no suitable model selected, skipping...");
		}
		return models;
	}

	private IWalker selectWalker(ModelType currentModel) {
		if (currentModel.getType().name().equals("CFG")) {
			return new CFGWalker(mLogger);
		}
		return new DFSTreeWalker(mLogger);
	}

	static void initializePlugin(ILogger logger, IToolchainPlugin plugin, IUltimateServiceProvider services,
			IToolchainStorage storage) {
		logger.info("Initializing " + plugin.getPluginName() + "...");
		try {
			plugin.setServices(services);
			plugin.setToolchainStorage(storage);
			plugin.init();
			logger.info(plugin.getPluginName() + " initialized");
		} catch (Exception ex) {
			logger.fatal("Exception during initialization of " + plugin.getPluginName());
			throw ex;
		}
	}
}
