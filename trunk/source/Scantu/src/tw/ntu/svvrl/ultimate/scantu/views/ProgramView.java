package tw.ntu.svvrl.ultimate.scantu.views;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

import tw.ntu.svvrl.ultimate.scantu.lib.CACSLCodeBeautifier;

public class ProgramView extends ViewPart {
	
	private static ListViewer viewer;
	private static File[] inputFile = null;
	
	public static void setInputFile(File[] file) {
		inputFile = file;
		viewer.setInput(inputFile);
	}

	@Override
	public void createPartControl(Composite parent) {
		
		/*Text text = new Text(parent, SWT.BORDER);
		text.setText("The program view.");*/
		
		viewer = new ListViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new IStructuredContentProvider() {

			@Override
			public Object[] getElements(Object inputElement) {
				ArrayList<String> fileContent = new ArrayList<String>();
				fileContent = readInputFile(((File[]) inputElement)[0]);
				ArrayList<String> prettyFileContent = CACSLCodeBeautifier.codeBeautify(fileContent);
				return prettyFileContent.toArray();
				// return fileContent.toArray();
			}
			
		});
		
		viewer.setInput(inputFile);
	}
	
	private static ArrayList<String> readInputFile(File inputFile) {
		ArrayList<String> fileContent = new ArrayList<String>();
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			String line = reader.readLine();
			while (line != null) {
				fileContent.add(line);
				System.out.println(line);
				line = reader.readLine();
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileContent;
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}
	
}