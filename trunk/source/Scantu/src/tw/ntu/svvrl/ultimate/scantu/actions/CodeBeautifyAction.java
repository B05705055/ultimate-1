package tw.ntu.svvrl.ultimate.scantu.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import tw.ntu.svvrl.ultimate.scantu.lib.CACSLCodeBeautifier;
import tw.ntu.svvrl.ultimate.scantu.views.FolderView;
import tw.ntu.svvrl.ultimate.scantu.views.ProgramView;

public class CodeBeautifyAction extends Action implements IWorkbenchAction {
	
	protected final IWorkbenchWindow mWorkbenchWindow;
	public String selectedDir;
	private File[] inputFile;
	
	public CodeBeautifyAction(final IWorkbenchWindow window) {
		setId(getClass().getName());
		setText("Code Beautify");
		mWorkbenchWindow = window;
	}
	
	@Override
	public void run() {
		inputFile = ProgramView.getInputFile();
		ArrayList<String> fileContent = new ArrayList<String>();
		fileContent = ProgramView.readInputFile(inputFile[0]);
		ArrayList<String> prettyFileContent = CACSLCodeBeautifier.codeBeautify(fileContent);
		
		try {
			FileWriter myWriter = new FileWriter(FolderView.cFileDir + "\\beautifiedCode.c");
			for (String str : prettyFileContent) {
				myWriter.write(str + System.lineSeparator());
			}
			myWriter.close();
			File[] prettyFile = {new File(FolderView.cFileDir + "\\beautifiedCode.c")};
			ProgramView.setInputFile(prettyFile);
			FolderView.setDir(FolderView.getDir());
			FolderView.refreshDir();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*File myObj = new File(FolderView.cFileDir + "\\temp.c");
	    Scanner myReader;
		try {
			myReader = new Scanner(myObj);
			while (myReader.hasNextLine()) {
		        String data = myReader.nextLine();
		        System.out.println(data);
		      }
		      myReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}*/
	}

	@Override
	public void dispose() {
	}
}