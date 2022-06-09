package tw.ntu.svvrl.ultimate.scantu.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class AddAnnotationDialog extends Dialog {
	
	protected Shell shell;
	protected String result = "Finish Dig";
	private Text editArea;

	public AddAnnotationDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		setText("Annotation Dialog");
	}
	
	public String open() {
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}
	
	private void createContents() {
		shell = new Shell(getParent(), getStyle());
		shell.setSize(900, 600);
		shell.setText(getText());
		
		shell.setLayout(new GridLayout(8, true));
		
		editArea = new Text(shell, SWT.MULTI | SWT.V_SCROLL);
		editArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 8));
		
		new Label(shell, SWT.NONE);
		Text title1 = new Text(shell, SWT.READ_ONLY);
		title1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		title1.setText("Parsing Annotation");
		
		Button oneLineAnnotation = new Button(shell, SWT.NONE);
		oneLineAnnotation.setLayoutData(new GridData(80, SWT.DEFAULT));
		oneLineAnnotation.setText("//@");
		oneLineAnnotation.addSelectionListener(createAdapter("//@ "));
		
		Button multiLinesAnnotation = new Button(shell, SWT.NONE);
		multiLinesAnnotation.setLayoutData(new GridData(80, SWT.DEFAULT));
		multiLinesAnnotation.setText("/*@ ... */");
		multiLinesAnnotation.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editArea.insert("/*@ ");
				int pos = editArea.getCaretPosition();
				editArea.insert("\n*/");
				editArea.setSelection(pos);
				editArea.setFocus();
			}
		});
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		
		Text title2 = new Text(shell, SWT.READ_ONLY);
		title2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		title2.setText("Statement Annotation");
		
		Button assertion = new Button(shell, SWT.NONE);
		assertion.setLayoutData(new GridData(80, SWT.DEFAULT));
		assertion.setText("assert");
		assertion.addSelectionListener(createAdapter("assert "));
	}
	
	private SelectionAdapter createAdapter(String annotation) {
		SelectionAdapter adapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editArea.insert(annotation);
				editArea.setFocus();
			}
		};
		return adapter;
	}
}