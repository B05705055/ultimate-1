package swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class DialogSWT extends Dialog {

	protected Object result;
	protected Shell shell;
	
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
		
		Text editArea = new Text(shell, SWT.MULTI | SWT.V_SCROLL);
		editArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 8));
		
		new Label(shell, SWT.NONE);
		Text title1 = new Text(shell, SWT.READ_ONLY);
		title1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		title1.setText("Parsing Annotation");
		
		Button oneLineAnnotation = new Button(shell, SWT.NONE);
		oneLineAnnotation.setLayoutData(new GridData(80, SWT.DEFAULT));
		oneLineAnnotation.setText("//@");
		
		Button multiLinesAnnotation = new Button(shell, SWT.NONE);
		multiLinesAnnotation.setLayoutData(new GridData(80, SWT.DEFAULT));
		multiLinesAnnotation.setText("/*@ ... */");
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		
		Text title2 = new Text(shell, SWT.READ_ONLY);
		title2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		title2.setText("Parsing Annotation");
		
		/*new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);*/

	}

}
