package tw.ntu.svvrl.ultimate.scantu.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class AddAnnotationDialog extends Dialog {
	
	protected Shell shell;
	protected String result = "Finish Dig";

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
	}
}