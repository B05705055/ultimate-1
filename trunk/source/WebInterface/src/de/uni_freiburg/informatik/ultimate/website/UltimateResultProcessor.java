package de.uni_freiburg.informatik.ultimate.website;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.uni_freiburg.informatik.ultimate.core.lib.results.AllSpecificationsHoldResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.BenchmarkResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.CounterExampleResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.ExceptionOrErrorResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.InvariantResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.NoResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.NonterminatingLassoResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.PositiveResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.ProcedureContractResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.SyntaxErrorResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.TerminationAnalysisResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.TerminationArgumentResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.TimeoutResultAtElement;
import de.uni_freiburg.informatik.ultimate.core.lib.results.TypeErrorResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.UnprovableResult;
import de.uni_freiburg.informatik.ultimate.core.lib.results.UnsupportedSyntaxResult;
import de.uni_freiburg.informatik.ultimate.core.model.models.ILocation;
import de.uni_freiburg.informatik.ultimate.core.model.results.IResult;
import de.uni_freiburg.informatik.ultimate.core.model.results.IResultWithLocation;
import de.uni_freiburg.informatik.ultimate.core.model.results.IResultWithSeverity;
import de.uni_freiburg.informatik.ultimate.core.model.results.IResultWithSeverity.Severity;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;

public class UltimateResultProcessor {

	public static void processUltimateResults(IUltimateServiceProvider services, JSONObject json) throws JSONException {
		// get Result from Ultimate
		Map<String, List<IResult>> results = services.getResultService().getResults();
		// add result to the json object
		ArrayList<JSONObject> resultList = new ArrayList<JSONObject>();
		for (List<IResult> rList : results.values()) {
			for (IResult r : rList) {
				SimpleLogger.log("processing result " + r.getShortDescription());
				String type = "UNDEF";
				UltimateResult packagedResult = new UltimateResult();
				if (r instanceof ExceptionOrErrorResult) {
					type = "ExceptionOrError";
					packagedResult.logLvl = "error";
				} else if (r instanceof CounterExampleResult) {
					type = "counter";
					packagedResult.logLvl = "error";
				} else if (r instanceof ProcedureContractResult) {
					type = "invariant";
					packagedResult.logLvl = "info";
				} else if (r instanceof InvariantResult) {
					type = "invariant";
					packagedResult.logLvl = "info";
				} else if (r instanceof PositiveResult) {
					type = "positive";
					packagedResult.logLvl = "info";
				} else if (r instanceof BenchmarkResult) {
					type = "benchmark";
					packagedResult.logLvl = "info";
				} else if (r instanceof TerminationArgumentResult) {
					type = "invariant";
					packagedResult.logLvl = "info";
				} else if (r instanceof NonterminatingLassoResult<?, ?, ?>) {
					type = "invariant";
					packagedResult.logLvl = "info";
				} else if (r instanceof AllSpecificationsHoldResult) {
					type = "invariant";
					packagedResult.logLvl = "info";
				} else if (r instanceof UnprovableResult) {
					type = "unprovable";
					packagedResult.logLvl = "warning";
				} else if (r instanceof SyntaxErrorResult) {
					type = "syntaxError";
					packagedResult.logLvl = "error";
				} else if (r instanceof UnsupportedSyntaxResult) {
					type = "syntaxUnsupported";
					packagedResult.logLvl = "error";
				} else if (r instanceof TimeoutResultAtElement) {
					type = "timeout";
					packagedResult.logLvl = "error";
				} else if (r instanceof TypeErrorResult<?>) {
					type = "typeError";
					packagedResult.logLvl = "error";
				} else if (r instanceof TerminationAnalysisResult) {
					type = "positive";
					packagedResult.logLvl = "info";
				} else if (r instanceof IResultWithSeverity) {
					IResultWithSeverity rws = (IResultWithSeverity) r;
					if (rws.getSeverity().equals(Severity.ERROR)) {
						type = "error";
						packagedResult.logLvl = "error";
					} else if (rws.getSeverity().equals(Severity.WARNING)) {
						type = "warning";
						packagedResult.logLvl = "warning";
					} else if (rws.getSeverity().equals(Severity.INFO)) {
						type = "info";
						packagedResult.logLvl = "info";
					} else {
						throw new IllegalArgumentException("Unknown kind of severity.");
					}
				} else if (r instanceof NoResult<?>) {
					type = "noResult";
					packagedResult.logLvl = "warning";
				}
				// TODO : Add new "Out of resource" result here ...
				if (r instanceof IResultWithLocation) {
					ILocation loc = ((IResultWithLocation) r).getLocation();
					if (((IResultWithLocation) r).getLocation() == null) {
						throw new IllegalArgumentException("Location is null");
					}
					packagedResult.startLNr = loc.getStartLine();
					packagedResult.endLNr = loc.getEndLine();
					packagedResult.startCol = loc.getStartColumn();
					packagedResult.endCol = loc.getEndColumn();
				} else {
					packagedResult.startLNr = -1;
					packagedResult.endLNr = -1;
					packagedResult.startCol = -1;
					packagedResult.endCol = -1;
				}
				packagedResult.shortDesc = String.valueOf(r.getShortDescription());
				packagedResult.longDesc = String.valueOf(r.getLongDescription());
				packagedResult.type = type;
				resultList.add(new JSONObject(packagedResult));
				SimpleLogger.log("added result: " + packagedResult.toString());
			}
			json.put("results", new JSONArray(resultList.toArray(new JSONObject[0])));
		}
	}

}
