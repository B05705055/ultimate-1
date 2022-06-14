package swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

public class DialogSWT extends Dialog {

	protected Object result;
	protected Shell shell;
	private Text editArea;
	
	public DialogSWT(Shell parent, int style) {
		super(parent, style);
		setText("SWT Dialog");
	}

	public Object open() {
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

Composite editComposite = new Composite(shell, SWT.NONE);
editComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));
editComposite.setLayout(new GridLayout(2, false));

Text annotationArea = new Text(editComposite, SWT.RIGHT | SWT.READ_ONLY);
annotationArea.setLayoutData(new GridData(50, 20));
annotationArea.setText("//@");

editArea = new Text(editComposite, SWT.SINGLE);
editArea.setLayoutData(new GridData(450, 20));

Text annotationAreaEnd = new Text(editComposite, SWT.LEFT | SWT.READ_ONLY);
annotationAreaEnd.setLayoutData(new GridData(50, 20));
annotationAreaEnd.setText("*/");
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
Composite composite = new Composite(editComposite, SWT.NONE);
new Label(editComposite, SWT.NONE);
		
		Composite operateComposite = new Composite(shell, SWT.NONE);
		operateComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		operateComposite.setLayout(new GridLayout(3, true));
		
		
		Text title1 = new Text(operateComposite, SWT.READ_ONLY);
		title1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		title1.setText("Parsing Annotation");
		
		Button oneLineAnnotation = new Button(operateComposite, SWT.NONE);
		oneLineAnnotation.setLayoutData(new GridData(80, SWT.DEFAULT));
		oneLineAnnotation.setText("//@");
		oneLineAnnotation.addSelectionListener(createAdapter("//@ "));
		
		Button multiLinesAnnotation = new Button(operateComposite, SWT.NONE);
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
		new Label(operateComposite, SWT.NONE);
		new Label(operateComposite, SWT.NONE);
		new Label(operateComposite, SWT.NONE);
		new Label(operateComposite, SWT.NONE);
		
		Text title2 = new Text(operateComposite, SWT.READ_ONLY);
		title2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		title2.setText("Statement Annotation");
		
		Button assertion = new Button(operateComposite, SWT.NONE);
		assertion.setLayoutData(new GridData(80, SWT.DEFAULT));
		assertion.setText("assert");
		new Label(operateComposite, SWT.NONE);
		new Label(operateComposite, SWT.NONE);
		
		Button btnRadioButton = new Button(operateComposite, SWT.RADIO);
		btnRadioButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
		btnRadioButton.setText("Radio Button");
		new Label(operateComposite, SWT.NONE);
		new Label(operateComposite, SWT.NONE);
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
