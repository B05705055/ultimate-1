package tw.ntu.svvrl.ultimate.scantu.actions.toolchain_actions;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import org.eclipse.ui.IWorkbenchWindow;
import org.xml.sax.SAXException;

import de.uni_freiburg.informatik.ultimate.core.coreplugin.toolchain.BasicToolchainJob;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import tw.ntu.svvrl.ultimate.scantu.ScantuController;
import tw.ntu.svvrl.ultimate.scantu.actions.RunToolchainAction;
import tw.ntu.svvrl.ultimate.scantu.actions.ScantuToolchainJob;

public class RunSvvrlDebugToolchainAction extends RunToolchainAction {
	
	private static final String LABEL = "Run SvvrlDebug";
	private static final String TOOLCHAIN_PATH = "C:\\Users\\user\\Documents\\GitHub\\ultimate-1\\trunk\\source\\Scantu\\toolchains\\CInline2BoogiePrinter.xml";

	public RunSvvrlDebugToolchainAction(ICore<RunDefinition> icore, ILogger logger, ScantuController controller,
			IWorkbenchWindow window) {
		super(icore, logger, controller, window, RunSvvrlDebugToolchainAction.class.getName(), LABEL);
	}
	
	@Override
	public final void run() {
		final File[] fp = getInputFile();
		if(fp == null) {
			System.out.println("You have not chosen the C file to analyze!");
			return;
		}
		
		try {
			mController.setToolchain(TOOLCHAIN_PATH);
		} catch (FileNotFoundException | JAXBException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		final BasicToolchainJob tcj = new ScantuToolchainJob("Toolchain Name:", mCore, mController, mLogger, fp);
		tcj.schedule();
	}
	
}