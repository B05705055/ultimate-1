/**
 * C TraceAbstraction toolchain.
 */
package de.uni_freiburg.informatik.ultimate.website.toolchains;

import java.util.ArrayList;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.website.Setting;
import de.uni_freiburg.informatik.ultimate.website.Setting.SettingType;
import de.uni_freiburg.informatik.ultimate.website.Tasks.TaskNames;
import de.uni_freiburg.informatik.ultimate.website.Tool;
import de.uni_freiburg.informatik.ultimate.website.WebToolchain;

/**
 * @author Markus Lindenmann
 * @author Oleksii Saukh
 * @author Stefan Wissert
 * @author Matthias Heizmann
 * @date 14.02.2012
 */
public class CAutomizerTC extends WebToolchain {

	@Override
	protected String defineDescription() {
		return NameStrings.s_TOOL_Automizer;
	}

	@Override
	protected String defineName() {
		return NameStrings.s_TOOL_Automizer;
	}

	@Override
	protected String defineId() {
		return "cAutomizer";
	}

	@Override
	protected TaskNames[] defineTaskName() {
		return new TaskNames[] { TaskNames.AUTOMIZER_C };
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
		List<Tool> tools = new ArrayList<Tool>();

		tools.add(new Tool(PrefStrings.s_cacsl2boogietranslator));
		tools.addAll(BoogieAutomizerTC.boogieAutomizerTools());

		return tools;
	}

	@Override
	protected List<Setting> defineAdditionalSettings() {
		List<Setting> rtr = BoogieAutomizerTC.boogieAutomizerAdditionalSettings();
		rtr.add(new Setting(PrefStrings.s_CACSL_LABEL_MemoryLeak, SettingType.BOOLEAN, "Check for memory leak in main procedure", "true", true));
		rtr.add(new Setting(PrefStrings.s_CACSL_LABEL_SignedIntegerOverflow, SettingType.BOOLEAN, "Check for overflows of signed integers", "true", true));
		return rtr;
	}
	
	@Override
	protected String defineToolchainSettingsFile() {
		return "Automizer.epf";
	}

	@Override
	protected String defineLanguage() {
		return "c";
	}

}
