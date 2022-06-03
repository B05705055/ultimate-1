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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

import tw.ntu.svvrl.ultimate.scantu.lib.CACSLCodeBeautifier;

public class ProgramView extends ViewPart {
	
	private static Text text;
	private static ListViewer viewer;
	private static File[] inputFile = null;
	private static String fileName = "No file selected.";
	
	public static void setInputFile(File[] file) {
		if (file != null) {
			fileName = file[0].getName();
			text.setText(fileName);
		}
		inputFile = file;
		viewer.setInput(inputFile);
	}
	
	public static File[] getInputFile() {
		return inputFile;
	}

	@Override
	public void createPartControl(Composite parent) {
		
		parent.setLayout(new GridLayout(1, false));
		
		text = new Text(parent, SWT.MULTI);
		text.setText(fileName);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(parent, SWT.NONE);
		
		viewer = new ListViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		viewer.setContentProvider(new IStructuredContentProvider() {

			@Override
			public Object[] getElements(Object inputElement) {
				ArrayList<String> fileContent = new ArrayList<String>();
				fileContent = readInputFile(((File[]) inputElement)[0]);
				//ArrayList<String> prettyFileContent = CACSLCodeBeautifier.codeBeautify(fileContent);
				//return prettyFileContent.toArray();
				return fileContent.toArray();
			}
			
		});
		
		viewer.setInput(inputFile);
	}
	
	public static ArrayList<String> readInputFile(File inputFile) {
		ArrayList<String> fileContent = new ArrayList<String>();
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			String line = reader.readLine();
			while (line != null) {
				fileContent.add(line);
				//System.out.println(line);
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