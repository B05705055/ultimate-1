package de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.preferences;

import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceItem;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceItem.PreferenceType;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.Activator;

public class RcfgPreferenceInitializer extends UltimatePreferenceInitializer {

	@Override
	protected UltimatePreferenceItem<?>[] initDefaultPreferences() {
		return new UltimatePreferenceItem<?>[] {
				new UltimatePreferenceItem<Boolean>(LABEL_ASSUME_FOR_ASSERT,
						DEF_ASSUME_FOR_ASSERT, PreferenceType.Boolean),
				new UltimatePreferenceItem<Solver>(LABEL_Solver,
						DEF_Solver, PreferenceType.Combo, Solver.values()),
				new UltimatePreferenceItem<String>(LABEL_ExtSolverCommand,
						DEF_ExtSolverCommand_Z3, PreferenceType.String),
				new UltimatePreferenceItem<String>(LABEL_ExtSolverLogic,
						DEF_ExtSolverLogic, PreferenceType.String),
				new UltimatePreferenceItem<Boolean>(LABEL_BitvectorWorkaround,
						false, PreferenceType.Boolean),	
				new UltimatePreferenceItem<CodeBlockSize>(LABEL_CodeBlockSize,
						DEF_CodeBlockSize, PreferenceType.Combo, CodeBlockSize.values()),
				new UltimatePreferenceItem<Boolean>(LABEL_RemoveGotoEdges,
						false, PreferenceType.Boolean),
				new UltimatePreferenceItem<Boolean>(LABEL_Simplify,
						false, PreferenceType.Boolean),
				new UltimatePreferenceItem<Boolean>(LABEL_CNF,
						true, PreferenceType.Boolean),
				new UltimatePreferenceItem<Boolean>(LABEL_DumpToFile,
						false, PreferenceType.Boolean),
				new UltimatePreferenceItem<Boolean>(LABEL_DumpUnsatCoreTrackBenchmark,
						false, PreferenceType.Boolean),
				new UltimatePreferenceItem<Boolean>(LABEL_DumpMainTrackBenchmark,
						false, PreferenceType.Boolean),
				new UltimatePreferenceItem<String>(LABEL_Path,
						DEF_Path, PreferenceType.Directory),
		};
	}

	@Override
	protected String getPlugID() {
		return Activator.PLUGIN_ID;
	}

	@Override
	public String getPreferencePageTitle() {
		return "RCFG Builder";
	}
	
	public enum Solver { Internal_SMTInterpol, External_PrincessInterpolationMode, External_SMTInterpolInterpolationMode, External_Z3InterpolationMode, External_DefaultMode };
	/*
	 * new preferences that belong to the RCFG Builder 
	 */
	public static final String LABEL_ASSUME_FOR_ASSERT = "Add additional assume for each assert";
	public static final boolean DEF_ASSUME_FOR_ASSERT = false;
	public static final String LABEL_Solver = "SMT solver";
	public static final Solver DEF_Solver = Solver.External_DefaultMode;
//	public static final Solver DEF_Solver = Solver.Internal_SMTInterpol;
	public static final String LABEL_ExtSolverCommand = "Command for external solver";
//	public static final String DEF_ExtSolverCommand_Z3 = "z3 SMTLIB2_COMPLIANT=true -memory:1024 -smt2 -in -t:12000 auto_config=false smt.array.extensional=false";
//	public static final String DEF_ExtSolverCommand_Z3 = "z3 SMTLIB2_COMPLIANT=true -memory:1024 -smt2 -in -t:12000 auto_config=false smt.mbqi=false";
	public static final String DEF_ExtSolverCommand_Z3 = "z3 SMTLIB2_COMPLIANT=true -memory:1024 -smt2 -in -t:12000";
//	public static final String DEF_ExtSolverCommand_CVC4 = "cvc4-2014-07-03-x86_64-linux-opt --incremental --print-success";
	public static final String DEF_ExtSolverCommand_CVC4 = "cvc4-2015-03-15-x86_64-linux-opt-proofs --tear-down-incremental --print-success --lang smt --tlimit-per=12000";
	public static final String DEF_ExtSolverCommand_Princess = "princess +incremental +stdin -timeout=12000";
	public static final String LABEL_ExtSolverLogic = "Logic for external solver";
	public static final String DEF_ExtSolverLogic = "AUFNIRA";

	
	
	public static final String LABEL_CodeBlockSize = "Size of a code block";
	public enum CodeBlockSize { SingleStatement, SequenceOfStatements, LoopFreeBlock };
	public static final CodeBlockSize DEF_CodeBlockSize = CodeBlockSize.LoopFreeBlock;
	public static final String LABEL_Simplify = "Simplify code blocks";
	public static final String LABEL_CNF = "Convert code blocks to CNF";
	public static final String LABEL_RemoveGotoEdges = "Remove goto edges from RCFG";
	public static final String LABEL_DumpToFile = "Dump SMT script to file";
	public static final String LABEL_DumpUnsatCoreTrackBenchmark = "Dump unsat core track benchmark to file";
	public static final String LABEL_DumpMainTrackBenchmark = "Dump main track benchmark to file";
	public static final String LABEL_Path = "To the following directory";
	public static final String DEF_Path = "";
	public static final String LABEL_BitvectorWorkaround = "Translate Boogie integers to SMT bitvectors";
	
}