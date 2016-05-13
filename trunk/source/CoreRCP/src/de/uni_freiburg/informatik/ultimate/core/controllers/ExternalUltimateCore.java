/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.core.controllers;

import java.io.File;
import java.util.concurrent.Semaphore;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.uni_freiburg.informatik.ultimate.core.coreplugin.Activator;
import de.uni_freiburg.informatik.ultimate.core.coreplugin.UltimateCore;
import de.uni_freiburg.informatik.ultimate.core.coreplugin.toolchain.DefaultToolchainJob;
import de.uni_freiburg.informatik.ultimate.core.model.IController;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.toolchain.ToolchainListType;
import de.uni_freiburg.informatik.ultimate.core.services.model.ILogger;
import de.uni_freiburg.informatik.ultimate.core.services.model.ILoggingService;

/**
 * 
 * Note: Not thread-safe (will build another one which is thread-safe and keeps one UltimateCore instance)
 * 
 * <ol>
 * <li>{@link #runUltimate()}</li>
 * <li>Delegate your callback to {@link IController#init(ICore, ILoggingService)} to
 * {@link #init(ICore, ILoggingService, File)}, which will start a toolchain</li>
 * <li>Get results or whatever you want</li>
 * <li>Call {@link #complete()}</li>
 * <li>Throw this instance away</li>
 * </ol>
 * 
 * @author dietsch
 * 
 */
public class ExternalUltimateCore {

	private UltimateCore mCurrentUltimateInstance;
	private Throwable mUltimateThrowable;
	private volatile boolean mReachedInit;

	private final Semaphore mUltimateExit;
	private final Semaphore mStarterContinue;
	private final IController<ToolchainListType> mController;

	protected ManualReleaseToolchainJob mJob;

	private volatile IStatus mReturnStatus;

	public ExternalUltimateCore(IController<ToolchainListType> controller) {
		mUltimateExit = new Semaphore(0);
		mStarterContinue = new Semaphore(0);
		mController = controller;
		mReachedInit = false;
	}

	public IStatus runUltimate() throws Throwable {
		if (mCurrentUltimateInstance != null) {
			throw new Exception("You must call complete() before re-using this instance ");
		}
		mCurrentUltimateInstance = new UltimateCore();
		mUltimateThrowable = null;

		final ActualUltimateRunnable runnable = new ActualUltimateRunnable();

		Thread thread = new Thread(runnable, "ActualUltimateInstance");

		thread.start();
		mStarterContinue.acquireUninterruptibly();
		if (mUltimateThrowable != null) {
			throw mUltimateThrowable;
		}
		return mReturnStatus;
	}

	public IStatus init(ICore<ToolchainListType> core, ILoggingService loggingService) {
		return init(core, loggingService, null, 0, null);
	}

	public IStatus init(ICore<ToolchainListType> core, ILoggingService loggingService, File[] inputFiles) {
		return init(core, loggingService, null, 0, inputFiles);
	}

	public IStatus init(ICore<ToolchainListType> core, ILoggingService loggingService, File settingsFile, long deadline,
			File[] inputFiles) {
		ILogger logger = null;
		try {
			mReachedInit = true;
			if (core == null || loggingService == null) {
				return new Status(Status.ERROR, Activator.PLUGIN_ID, Status.ERROR, "Initialization failed", null);
			}

			logger = getLogger(loggingService);

			if (settingsFile != null) {
				core.loadPreferences(settingsFile.getAbsolutePath());
			}
			mJob = getToolchainJob(core, mController, logger, inputFiles);
			if (deadline > 0) {
				mJob.setDeadline(deadline);
			}
			mJob.schedule();
			mJob.join();
			mReturnStatus = mJob.getResult();
		} catch (Throwable e) {
			logger.error("Exception during toolchain execution.", e);
		} finally {
			mStarterContinue.release();
			mUltimateExit.acquireUninterruptibly();
		}
		return mReturnStatus;
	}

	protected ILogger getLogger(ILoggingService loggingService) {
		return loggingService.getControllerLogger();
	}

	protected ManualReleaseToolchainJob getToolchainJob(ICore<ToolchainListType> core,
			IController<ToolchainListType> controller, ILogger logger, File[] inputFiles) {
		return new ManualReleaseToolchainJob("Processing Toolchain", core, controller, logger, inputFiles);
	}

	public void complete() {
		if (mJob != null) {
			// mJob may be null if Ultimate did not complete its init phase
			mJob.releaseToolchainManually();
		}
		mUltimateExit.release();
	}

	private final class ActualUltimateRunnable implements Runnable {

		@Override
		public void run() {
			try {
				mCurrentUltimateInstance.start(mController, false);
			} catch (Throwable e) {
				mUltimateThrowable = e;
			}

			if (!mReachedInit) {
				if (mUltimateThrowable == null) {
					mUltimateThrowable = new LivecycleException(
							"Ultimate terminated before calling init(...) on ExternalUltimateCore");
				}
				mStarterContinue.release();
			}
		}
	}

	protected class ManualReleaseToolchainJob extends DefaultToolchainJob {

		public ManualReleaseToolchainJob(String name, ICore<ToolchainListType> core,
				IController<ToolchainListType> controller, ILogger logger, File[] inputs) {
			super(name, core, controller, logger, inputs);
		}

		@Override
		protected void releaseToolchain() {
			// we use a manual release
		}

		public void releaseToolchainManually() {
			super.releaseToolchain();
		}

	}

}
