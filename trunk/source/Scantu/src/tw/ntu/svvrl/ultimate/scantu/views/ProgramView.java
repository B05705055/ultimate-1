package tw.ntu.svvrl.ultimate.scantu.views;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

public class ProgramView extends ViewPart {
	
	private static Text text;
	private static ListViewer viewer;
	private static File[] inputFile = null;
	private static String fileName = "No file selected.";
	private static int selectedStringIdx = -1;
	private static boolean SAVED = true;
	
	public static void setInputFile(File[] file) {
		if (file != null) {
			setFileName(file[0].getName());
		}
		selectedStringIdx = -1;
		inputFile = file;
		viewer.setInput(inputFile);
	}
	public static File[] getInputFile() {
		return inputFile;
	}
	
	public static String getFileName() {
		return fileName;
	}
	public static void setFileName(String newFileName) {
		fileName = newFileName;
		text.setText(fileName);
	}
	
	public static int getSelectedStringIdx() {
		return selectedStringIdx;
	}
	
	public static boolean whetherSaved() {
		return SAVED;
	}
	public static void changeSaveState(boolean newSaveState) {
		SAVED = newSaveState;
	}

	@Override
	public void createPartControl(Composite parent) {
		
		parent.setLayout(new GridLayout(1, false));
		
		text = new Text(parent, SWT.MULTI | SWT.READ_ONLY);
		text.setText(fileName);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		//new Label(parent, SWT.NONE);
		
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
		
		/*List list = (List) viewer.getControl();
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				String selectedString = (String) selection.getFirstElement();
				System.out.println(list.indexOf(selectedString));
			}
		});*/
		
		
		List list = (List) viewer.getControl();
		list.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedStringIdx = list.getSelectionIndex();
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
	
	public static void addAnnotation(String annotation, String type, boolean above) {
		ArrayList<String> fileContent = new ArrayList<String>();
		fileContent = readInputFile(inputFile[0]);
		
		String[] annotationArray = annotation.split("\n");
		for (int i = 0; i < annotationArray.length; i++) {
			if (!above) {
				selectedStringIdx++;
			}
			String annotationLine = annotationArray[i].replaceAll("\r", "");
			if (type.equals("MULTI")) {
				if (i == 0) {
					annotationLine = "/*@ " + annotationLine;
				}
				else {
					annotationLine = " *@ " + annotationLine;
				}
			}
			else {
				annotationLine = "//@ " + annotationLine;
			}
			fileContent.add(selectedStringIdx, annotationLine);
			if (above) {
				selectedStringIdx++;
			}
		}
		if (type.equals("MULTI")) {
			if (!above) {
				selectedStringIdx++;
			}
			fileContent.add(selectedStringIdx, " */");
		}
		selectedStringIdx = -1;
		
		try {
			FileWriter myWriter = new FileWriter(FolderView.cFileDir + "\\tempFile.c");
			for (String str : fileContent) {
				myWriter.write(str + System.lineSeparator());
			}
			myWriter.close();
			File[] newFile = {new File(FolderView.cFileDir + "\\tempFile.c")};
			
			String originalFileName = getFileName();
			setInputFile(newFile);
			if (whetherSaved()) {
				setFileName("*" + originalFileName);
			}
			else {
				setFileName(originalFileName);
			}
			changeSaveState(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}
	
}