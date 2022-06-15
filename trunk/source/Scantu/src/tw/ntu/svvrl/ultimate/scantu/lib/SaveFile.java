package tw.ntu.svvrl.ultimate.scantu.lib;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbenchWindow;

import tw.ntu.svvrl.ultimate.scantu.views.ProgramView;

public class SaveFile {
	
	public static void save(IWorkbenchWindow window) {
		while (true) {
			DirectoryDialog dd = new DirectoryDialog(window.getShell());
			String selectedDir = dd.open();
		
			File[] targetFile = ProgramView.getInputFile();
			ArrayList<String> fileContent = ProgramView.readInputFile(targetFile[0]);
			String newFileName = ProgramView.getFileName();
			if (!ProgramView.whetherSaved()) {
				newFileName = newFileName.substring(1);
			}
			File checkExist = new File(selectedDir + "\\" + newFileName);
			if (checkExist.exists()) {
				MessageBox dialog =
						new MessageBox(window.getShell(), SWT.ICON_WARNING | SWT.YES| SWT.NO);
				dialog.setText("Warning");
				dialog.setMessage("File \"" + newFileName + "\" already exists, do you want to overwrite it?");
				int returnCode = dialog.open();
				if (returnCode == SWT.NO) {
					continue;
				}
			}
		
			try {
				FileWriter myWriter = new FileWriter(selectedDir + "\\" + newFileName);
				for (String str : fileContent) {
					myWriter.write(str + System.lineSeparator());
				}
				myWriter.close();
				ProgramView.setFileName(newFileName);
				ProgramView.changeSaveState(true);
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		}
	}
}