/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2014-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE BuchiAutomizer plug-in.
 * 
 * The ULTIMATE BuchiAutomizer plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE BuchiAutomizer plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE BuchiAutomizer plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE BuchiAutomizer plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE BuchiAutomizer plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.core.lib.results.ResultUtil;
import de.uni_freiburg.informatik.ultimate.core.lib.results.TerminationArgumentResult;
import de.uni_freiburg.informatik.ultimate.core.model.services.IBacktranslationService;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RcfgElement;
import de.uni_freiburg.informatik.ultimate.util.csv.ICsvProvider;
import de.uni_freiburg.informatik.ultimate.util.csv.ICsvProviderProvider;
import de.uni_freiburg.informatik.ultimate.util.csv.SimpleCsvProvider;

public class BuchiAutomizerModuleDecompositionBenchmark implements ICsvProviderProvider<String> {

	private final TreeMap<Integer, Integer> m_ModuleSizeTrivial = new TreeMap<Integer, Integer>();
	private final TreeMap<Integer, Integer> m_ModuleSizeDeterministic = new TreeMap<Integer, Integer>();
	private final TreeMap<Integer, Integer> m_ModuleSizeNondeterministic = new TreeMap<Integer, Integer>();
	private final TreeMap<Integer, String> m_RankingFunction = new TreeMap<Integer, String>();
	/**
	 * Is there a remainder module? A remainder module contains remaining traces if decomposition into modules failed.
	 * Null if yet unknown.
	 */
	private Boolean m_HasRemainderModule;
	private int m_RemainderModuleLocations;
	private boolean m_RemainderModuleNonterminationKnown;
	private IBacktranslationService mBacktranslationService;

	public BuchiAutomizerModuleDecompositionBenchmark(IBacktranslationService service) {
		mBacktranslationService = service;
	}

	void reportTrivialModule(Integer iteration, Integer size) {
		m_ModuleSizeTrivial.put(iteration, size);
	}

	void reportDeterminsticModule(Integer iteration, Integer size) {
		m_ModuleSizeDeterministic.put(iteration, size);
	}

	void reportNonDeterminsticModule(Integer iteration, Integer size) {
		m_ModuleSizeNondeterministic.put(iteration, size);
	}

	void reportRankingFunction(Integer iteration, TerminationArgumentResult<RcfgElement, Expression> tar) {
		m_RankingFunction.put(iteration, prettyPrintRankingFunction(tar));
	}

	void reportRemainderModule(int numberLocations, boolean nonterminationKnown) {
		assert m_HasRemainderModule == null : "remainder module already reported";
		m_HasRemainderModule = true;
		m_RemainderModuleLocations = numberLocations;
		m_RemainderModuleNonterminationKnown = nonterminationKnown;
	}

	void reportNoRemainderModule() {
		assert m_HasRemainderModule == null : "remainder module already reported";
		m_HasRemainderModule = false;
	}

	private String prettyPrintRankingFunction(TerminationArgumentResult<RcfgElement, Expression> tar) {
		return tar.getRankingFunctionDescription() + " ranking function " + ResultUtil
				.translateExpressionToString(mBacktranslationService, Expression.class, tar.getRankingFunction());
	}

	@Override
	public String toString() {
		if (m_HasRemainderModule == null) {
			return "Decomposition not yet finished";
		}
		int modules = m_ModuleSizeTrivial.size() + m_ModuleSizeDeterministic.size()
				+ m_ModuleSizeNondeterministic.size();
		if (modules == 0) {
			if (m_HasRemainderModule) {
				if (m_RemainderModuleNonterminationKnown) {
					return "Trivial decomposition into one nonterminating module.";
				} else {
					return "Trivial decomposition into one module whose termination is unknown.";
				}
			} else {
				return "Trivial decomposition. There is no loop in your program.";
			}
		}
		int maxNumberOfStatesOfModuleWithTrivialRankingFunction = 0;
		StringBuilder sb = new StringBuilder();
		sb.append("Your program was decomposed into ");
		sb.append(modules);
		sb.append(" terminating modules ");
		sb.append("(");
		sb.append(m_ModuleSizeTrivial.size());
		sb.append(" trivial, ");
		sb.append(m_ModuleSizeDeterministic.size());
		sb.append(" deterministic, ");
		sb.append(m_ModuleSizeNondeterministic.size());
		sb.append(" nondeterministic)");
		if (m_HasRemainderModule) {
			if (m_RemainderModuleNonterminationKnown) {
				sb.append(" and one nonterminating remainder module.");
			} else {
				sb.append(" and one module whose termination is unknown.");
			}
		} else {
			sb.append(". ");
		}
		for (Entry<Integer, Integer> entry : m_ModuleSizeDeterministic.entrySet()) {
			sb.append("One deterministic module has ");
			sb.append(m_RankingFunction.get(entry.getKey()));
			sb.append(" and consists of ");
			sb.append(entry.getValue());
			sb.append(" locations. ");
		}
		for (Entry<Integer, Integer> entry : m_ModuleSizeNondeterministic.entrySet()) {
			sb.append("One nondeterministic module has ");
			sb.append(m_RankingFunction.get(entry.getKey()));
			sb.append(" and consists of ");
			sb.append(entry.getValue());
			sb.append(" locations. ");
		}
		for (Entry<Integer, Integer> entry : m_ModuleSizeTrivial.entrySet()) {
			if (entry.getValue() > maxNumberOfStatesOfModuleWithTrivialRankingFunction) {
				maxNumberOfStatesOfModuleWithTrivialRankingFunction = entry.getValue();
			}
		}
		if (m_ModuleSizeTrivial.size() > 0) {
			sb.append(m_ModuleSizeTrivial.size());
			sb.append(" modules have a trivial ranking function, the largest among these consists of ");
			sb.append(maxNumberOfStatesOfModuleWithTrivialRankingFunction);
			sb.append(" locations.");
		}
		if (m_HasRemainderModule) {
			sb.append(" The remainder module has ");
			sb.append(m_RemainderModuleLocations);
			sb.append(" locations.");
		}
		return sb.toString();
	}

	@Override
	public ICsvProvider<String> createCvsProvider() {

		ArrayList<String> header = new ArrayList<String>();
		header.add("Modules");
		header.add("Trivial modules");
		header.add("Deterministic modules");
		header.add("Nondeterministic modules");
		header.add("Remainer module");
		header.add("Min Locs trivial modules");
		header.add("Avg Locs trivial modules");
		header.add("Max Locs trivial modules");
		header.add("Min Locs deterministic modules");
		header.add("Avg Locs deterministic modules");
		header.add("Max Locs deterministic modules");
		header.add("Min Locs nondeterministic modules");
		header.add("Avg Locs nondeterministic modules");
		header.add("Max Locs nondeterministic modules");

		int modules = m_ModuleSizeTrivial.size() + m_ModuleSizeDeterministic.size()
				+ m_ModuleSizeNondeterministic.size();

		ArrayList<String> row = new ArrayList<String>();
		row.add(String.valueOf(modules));
		if (modules == 0) {
			row.add(null);
			row.add(null);
			row.add(null);
			if (m_HasRemainderModule == null) {
				row.add("Decomposition not yet finished");
			} else if (m_HasRemainderModule) {
				if (m_RemainderModuleNonterminationKnown) {
					row.add("Nonterminating");
				} else {
					row.add("Unknown");
				}
			} else {
				row.add("No loop");
			}
			row.add(null);
			row.add(null);
			row.add(null);
			row.add(null);
			row.add(null);
			row.add(null);
			row.add(null);
			row.add(null);
			row.add(null);
		} else {
			row.add(String.valueOf(m_ModuleSizeTrivial.size()));
			row.add(String.valueOf(m_ModuleSizeDeterministic.size()));
			row.add(String.valueOf(m_ModuleSizeNondeterministic.size()));
			if (m_HasRemainderModule == null) {
				row.add("Decomposition not yet finished");
			} else if (m_HasRemainderModule) {
				if (m_RemainderModuleNonterminationKnown) {
					row.add("Nonterminating");
				} else {
					row.add("Unknown");
				}
			} else {
				row.add(null);
			}

			MinAvgMax triv = getMinAvgMax(m_ModuleSizeTrivial);
			MinAvgMax determinisic = getMinAvgMax(m_ModuleSizeDeterministic);
			MinAvgMax nondet = getMinAvgMax(m_ModuleSizeNondeterministic);
			row.add(String.valueOf(triv.min));
			row.add(String.valueOf(triv.avg));
			row.add(String.valueOf(triv.max));
			row.add(String.valueOf(determinisic.min));
			row.add(String.valueOf(determinisic.avg));
			row.add(String.valueOf(determinisic.max));
			row.add(String.valueOf(nondet.min));
			row.add(String.valueOf(nondet.avg));
			row.add(String.valueOf(nondet.max));

		}
		ICsvProvider<String> rtr = new SimpleCsvProvider<>(header);
		rtr.addRow(row);
		return rtr;
	}

	private MinAvgMax getMinAvgMax(TreeMap<Integer, Integer> map) {
		MinAvgMax rtr = new MinAvgMax();

		if (map == null || map.entrySet().size() == 0) {
			rtr.min = 0;
			rtr.avg = 0;
			rtr.max = 0;
			return rtr;
		}

		for (Entry<Integer, Integer> entry : map.entrySet()) {
			Integer current = entry.getValue();
			if (current < rtr.min) {
				rtr.min = current;
			}
			if (current > rtr.max) {
				rtr.max = current;
			}
			rtr.avg += current;
		}
		rtr.avg = rtr.avg / (double) map.entrySet().size();

		return rtr;
	}

	private class MinAvgMax {
		int min = Integer.MAX_VALUE;
		double avg = 0;
		int max = Integer.MIN_VALUE;
	}

}
