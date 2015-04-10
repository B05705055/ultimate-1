package de.uni_freiburg.informatik.ultimate.website.toolchains;

import java.util.ArrayList;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.website.Setting;
import de.uni_freiburg.informatik.ultimate.website.Tasks.TaskNames;
import de.uni_freiburg.informatik.ultimate.website.Tool;
import de.uni_freiburg.informatik.ultimate.website.WebToolchain;

public class BoogieLassoRankerTC extends WebToolchain {

	@Override
	protected String defineDescription() {
		return NameStrings.s_TOOL_LassoRanker;
	}

	@Override
	protected String defineName() {
		return NameStrings.s_TOOL_LassoRanker;
	}

	@Override
	protected String defineId() {
		return "boogieLassoRanker";
	}

	@Override
	protected TaskNames[] defineTaskName() {
		return new TaskNames[] { TaskNames.RANK_SYNTHESIS_BOOGIE };
	}

	@Override
	protected String defineLanguage() {
		return "boogie";
	}

	@Override
	protected String defineUserInfo() {
		return null;
	}
	
	@Override
	protected String defineInterfaceLayoutFontsize() {
		return PrefStrings.s_InterfaceLayoutFontsizeDefault;
	}

	@Override
	protected String defineInterfaceLayoutOrientation() {
		return PrefStrings.s_InterfaceLayoutOrientationDefault;
	}

	@Override
	protected String defineInterfaceLayoutTransitions() {
		return PrefStrings.s_InterfaceLayoutTransitionDefault;
	}

	@Override
	protected List<Tool> defineTools() {
		return boogieLassoRankerToolchain();
	}
	
	@Override
	protected List<Setting> defineAdditionalSettings() {
		return boogieLassoRankerAdditionalSettings();
	}

	/**
	 * List of tools required for LassoRanker on boogie code.
	 */
	static List<Tool> boogieLassoRankerToolchain() {
		List<Tool> tools = new ArrayList<Tool>();

		tools.add(new Tool(PrefStrings.s_boogiePreprocessor));
		tools.add(new Tool(PrefStrings.s_rcfgBuilder));
		tools.add(new Tool(PrefStrings.s_blockencoding));
		tools.add(new Tool(PrefStrings.s_lassoRanker));
		
		return tools;
	}

	static List<Setting> boogieLassoRankerAdditionalSettings() {
		List<Setting> rtr = new ArrayList<>();
//		rtr.add(new Setting(PrefStrings.s_BE_LABEL_STRATEGY, PrefStrings.s_BE_LABEL_STRATEGY,
//				new String[] { PrefStrings.s_BE_VALUE_DisjunctiveRating }, false, new String[] {
//						PrefStrings.s_BE_VALUE_DisjunctiveRating, PrefStrings.s_BE_VALUE_LargeBlock }, true));
		return rtr;
	}
	
	@Override
	protected String defineToolchainSettingsFile() {
		return "LassoRanker.epf";
	}
}
