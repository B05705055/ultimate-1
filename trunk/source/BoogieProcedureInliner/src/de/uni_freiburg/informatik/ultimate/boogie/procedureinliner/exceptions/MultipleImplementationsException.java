package de.uni_freiburg.informatik.ultimate.boogie.procedureinliner.exceptions;

import de.uni_freiburg.informatik.ultimate.boogie.procedureinliner.Activator;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Procedure;
import de.uni_freiburg.informatik.ultimate.result.AbstractResult;
import de.uni_freiburg.informatik.ultimate.result.UnsupportedSyntaxResult;

public class MultipleImplementationsException extends CancelToolchainException {

	private static final long serialVersionUID = -8103254229768742587L;

	public MultipleImplementationsException(Procedure procedure) {
		super("Multiple implementations aren't supported: " + procedure.getIdentifier(), procedure.getLocation());
	}

	@Override
	protected AbstractResult createResult(String pluginId) {
		return new UnsupportedSyntaxResult<>(Activator.PLUGIN_ID, getLocation(), getMessage());
	}
}
