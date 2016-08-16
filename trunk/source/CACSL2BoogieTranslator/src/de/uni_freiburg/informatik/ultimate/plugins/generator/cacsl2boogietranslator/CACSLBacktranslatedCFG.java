package de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.CACSLLocation;
import de.uni_freiburg.informatik.ultimate.core.lib.translation.BacktranslatedCFG;
import de.uni_freiburg.informatik.ultimate.core.model.models.IExplicitEdgesMultigraph;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.witnessprinter.CorrectnessWitnessGenerator;

/**
 *
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 */
public class CACSLBacktranslatedCFG extends BacktranslatedCFG<String, CACSLLocation> {

	private final ILogger mLogger;
	private final IUltimateServiceProvider mServices;

	public CACSLBacktranslatedCFG(final String filename,
			final List<? extends IExplicitEdgesMultigraph<?, ?, String, CACSLLocation, ?>> cfgs,
			final Class<CACSLLocation> clazz, final ILogger logger, final IUltimateServiceProvider services) {
		super(filename, cfgs, clazz);
		mLogger = logger;
		mServices = services;
	}

	@Override
	public String getSVCOMPWitnessString() {
		return new CorrectnessWitnessGenerator<CACSLLocation, IASTExpression>(this,
				new CACSLBacktranslationValueProvider(), mLogger, mServices).makeGraphMLString();
	}
}
