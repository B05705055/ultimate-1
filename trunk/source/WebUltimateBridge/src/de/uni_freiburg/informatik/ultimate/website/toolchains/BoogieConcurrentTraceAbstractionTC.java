package de.uni_freiburg.informatik.ultimate.website.toolchains;

import java.util.ArrayList;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.website.Setting;
import de.uni_freiburg.informatik.ultimate.website.Tasks.TaskNames;
import de.uni_freiburg.informatik.ultimate.website.Tool;
import de.uni_freiburg.informatik.ultimate.website.WebToolchain;

/**
 * @author Markus Lindenmann
 * @author Oleksii Saukh
 * @author Stefan Wissert
 * @author Matthias Heizmann
 */
public class BoogieConcurrentTraceAbstractionTC extends WebToolchain {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uni_freiburg.informatik.ultimate.website.Toolchain#setDescription()
	 */
	@Override
	protected String setDescription() {
		return "Concurrent trace abstraction toolchain";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uni_freiburg.informatik.ultimate.website.Toolchain#setName()
	 */
	@Override
	protected String setName() {
		return "Concurrent Trace Abstraction";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uni_freiburg.informatik.ultimate.website.Toolchain#setId()
	 */
	@Override
	protected String setId() {
		return "boogieConcurrentTraceAbstr";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uni_freiburg.informatik.ultimate.website.Toolchain#setTaskName()
	 */
	@Override
	protected TaskNames[] setTaskName() {
		return new TaskNames[] { TaskNames.CONCURRENT_TRACE_ABSTRACTION_BOOGIE };
	}

    /*
     * (non-Javadoc)
     * 
     * @see de.uni_freiburg.informatik.ultimate.website.WebToolchain#setLanguage()
     */
    @Override
    protected String setLanguage() {
        return "boogie";
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.uni_freiburg.informatik.ultimate.website.WebToolchain#setUserInfo()
     */
    @Override
    protected String setUserInfo() {
        return null;
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uni_freiburg.informatik.ultimate.website.Toolchain#setTools()
	 */
	@Override
	protected List<Tool> setTools() {
		List<Tool> tools = new ArrayList<Tool>();
		
		List<Setting> oPre = new ArrayList<Setting>();
		List<Setting> mPre = new ArrayList<Setting>();
		tools.add(new Tool(PrefStrings.s_boogiePreprocessor,
				oPre, mPre, LoggingLevel.WARN));
		
		List<Setting> oRCFGB = new ArrayList<Setting>();
		List<Setting> mRCFGB = new ArrayList<Setting>();
		tools.add(new Tool(PrefStrings.s_rcfgBuilder, oRCFGB, mRCFGB, LoggingLevel.WARN));
        oRCFGB.add(new Setting(PrefStrings.s_RCFG_LABEL_Solver, PrefStrings.s_RCFG_LABEL_Solver,
        		new String[] { PrefStrings.s_RCFG_VALUE_SMTInterpol }, false, new String[] {
        		PrefStrings.s_RCFG_VALUE_SMTInterpol, PrefStrings.s_RCFG_VALUE_ExternalDefMo }, false));
        mRCFGB.add(new Setting(PrefStrings.s_RCFG_LABEL_BlockSize, PrefStrings.s_RCFG_LABEL_BlockSize,
        		new String[] { PrefStrings.s_RCFG_VALUE_Single }, false, new String[] {
        		PrefStrings.s_RCFG_VALUE_Single, PrefStrings.s_RCFG_VALUE_Seq, PrefStrings.s_RCFG_VALUE_Block }, true));
		List<Setting> oTrConcur = new ArrayList<Setting>();
		List<Setting> mTrConcur = new ArrayList<Setting>();
        tools.add(new Tool(PrefStrings.s_traceAbstractionConcurrent, oTrConcur, mTrConcur,
                LoggingLevel.WARN));
		return tools;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uni_freiburg.informatik.ultimate.website.Toolchain#setPluginsLoggingLevel
	 * ()
	 */
	@Override
	protected LoggingLevel setPluginsLoggingLevel() {
		return LoggingLevel.INFO;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uni_freiburg.informatik.ultimate.website.Toolchain#setToolsLoggingLevel
	 * ()
	 */
	@Override
	protected LoggingLevel setToolsLoggingLevel() {
		return LoggingLevel.INFO;
	}
}
