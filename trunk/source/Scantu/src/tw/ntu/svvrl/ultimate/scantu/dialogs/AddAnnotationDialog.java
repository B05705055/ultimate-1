package tw.ntu.svvrl.ultimate.scantu.dialogs;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class AddAnnotationDialog extends Dialog {
	
	protected Shell shell;
	//protected String result;
	protected ArrayList<String> result;
	private Text editArea;
	private Text annotationAreaEnd;

	public AddAnnotationDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		setText("Annotation Dialog");
	}
	
	public ArrayList<String> open() {
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
		
		annotationAreaEnd = new Text(editComposite, SWT.LEFT | SWT.READ_ONLY);
		annotationAreaEnd.setLayoutData(new GridData(50, 20));
		annotationAreaEnd.setText("");
		
		Composite operateComposite = new Composite(shell, SWT.NONE);
		operateComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		operateComposite.setLayout(new GridLayout(3, true));
		
		Text title1 = new Text(operateComposite, SWT.READ_ONLY);
		title1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		title1.setText("Parsing Annotation");
		
		Button oneLineAnnotation = new Button(operateComposite, SWT.RADIO);
		oneLineAnnotation.setSelection(true);
		oneLineAnnotation.setText("//@");
		oneLineAnnotation.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (oneLineAnnotation.getSelection()) {
					annotationArea.setLayoutData(new GridData(50, 20));
					annotationArea.setText("//@");
					annotationAreaEnd.dispose();
					editArea.dispose();
					editArea = new Text(editComposite, SWT.SINGLE);
					editArea.setLayoutData(new GridData(450, 20));
					editArea.getParent().layout();
				}
			}
		});
		//oneLineAnnotation.addSelectionListener(createAdapter("//@ "));
		
		Button multiLinesAnnotation = new Button(operateComposite, SWT.RADIO);
		multiLinesAnnotation.setText("/*@ ... */");
		multiLinesAnnotation.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (multiLinesAnnotation.getSelection()) {
					annotationArea.setLayoutData(new GridData(50, 100));
					annotationArea.setText("/*@\n*@\n*@\n*@\n*@");
					editArea.dispose();
					editArea = new Text(editComposite, SWT.MULTI);
					editArea.setLayoutData(new GridData(450, 100));
					editArea.getParent().layout();
					
					annotationAreaEnd.dispose();
					annotationAreaEnd = new Text(editComposite, SWT.RIGHT | SWT.READ_ONLY);
					annotationAreaEnd.setLayoutData(new GridData(50, 20));
					annotationAreaEnd.setText("*/  ");
					annotationAreaEnd.getParent().layout();
				}
			}
		});
		new Label(operateComposite, SWT.NONE);
		
		new Label(operateComposite, SWT.NONE);
		
		Text title2 = new Text(operateComposite, SWT.READ_ONLY);
		title2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		title2.setText("LTL Property Annotation");
		
		Button LTL_inv_pos = new Button(operateComposite, SWT.NONE);
		LTL_inv_pos.setLayoutData(new GridData(80, SWT.DEFAULT));
		LTL_inv_pos.setText("ltl inv pos");
		LTL_inv_pos.addSelectionListener(createAdapter("ltl invariant positive: "));
		Button AP = new Button(operateComposite, SWT.NONE);
		AP.setLayoutData(new GridData(80, SWT.DEFAULT));
		AP.setText("AP");
		AP.addSelectionListener(createAdapter("AP()"));
		new Label(operateComposite, SWT.NONE);
		
		Button always = new Button(operateComposite, SWT.NONE);
		always.setLayoutData(new GridData(80, SWT.DEFAULT));
		always.setText("[]");
		always.addSelectionListener(createAdapter("[]"));
		Button eventually = new Button(operateComposite, SWT.NONE);
		eventually.setLayoutData(new GridData(80, SWT.DEFAULT));
		eventually.setText("<>");
		eventually.addSelectionListener(createAdapter("<>"));
		Button bracket = new Button(operateComposite, SWT.NONE);
		bracket.setLayoutData(new GridData(80, SWT.DEFAULT));
		bracket.setText("()");
		bracket.addSelectionListener(createAdapter("()"));
		
		Button next = new Button(operateComposite, SWT.NONE);
		next.setLayoutData(new GridData(80, SWT.DEFAULT));
		next.setText("X");
		next.addSelectionListener(createAdapter("X"));
		Button until = new Button(operateComposite, SWT.NONE);
		until.setLayoutData(new GridData(80, SWT.DEFAULT));
		until.setText("U");
		until.addSelectionListener(createAdapter("U "));
		Button release = new Button(operateComposite, SWT.NONE);
		release.setLayoutData(new GridData(80, SWT.DEFAULT));
		release.setText("R");
		release.addSelectionListener(createAdapter("R "));
		
		Button and = new Button(operateComposite, SWT.NONE);
		and.setLayoutData(new GridData(80, SWT.DEFAULT));
		and.setText("&&&&");
		and.addSelectionListener(createAdapter("&& "));
		Button or = new Button(operateComposite, SWT.NONE);
		or.setLayoutData(new GridData(80, SWT.DEFAULT));
		or.setText("||");
		or.addSelectionListener(createAdapter("|| "));
		Button negative = new Button(operateComposite, SWT.NONE);
		negative.setLayoutData(new GridData(80, SWT.DEFAULT));
		negative.setText("!");
		negative.addSelectionListener(createAdapter("!"));
		
		Button imply = new Button(operateComposite, SWT.NONE);
		imply.setLayoutData(new GridData(80, SWT.DEFAULT));
		imply.setText("==>");
		imply.addSelectionListener(createAdapter("==> "));
		Button iff = new Button(operateComposite, SWT.NONE);
		iff.setLayoutData(new GridData(80, SWT.DEFAULT));
		iff.setText("<==>");
		iff.addSelectionListener(createAdapter("<==> "));
		new Label(operateComposite, SWT.NONE);
		
		new Label(operateComposite, SWT.NONE);
		
		Text title3 = new Text(operateComposite, SWT.READ_ONLY);
		title3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		title3.setText("Operator");
		
		Button equal = new Button(operateComposite, SWT.NONE);
		equal.setLayoutData(new GridData(80, SWT.DEFAULT));
		equal.setText("==");
		equal.addSelectionListener(createAdapter("== "));
		Button geq = new Button(operateComposite, SWT.NONE);
		geq.setLayoutData(new GridData(80, SWT.DEFAULT));
		geq.setText(">=");
		geq.addSelectionListener(createAdapter(">= "));
		Button leq = new Button(operateComposite, SWT.NONE);
		leq.setLayoutData(new GridData(80, SWT.DEFAULT));
		leq.setText("<=");
		leq.addSelectionListener(createAdapter("<= "));
		
		Button neq = new Button(operateComposite, SWT.NONE);
		neq.setLayoutData(new GridData(80, SWT.DEFAULT));
		neq.setText("!=");
		neq.addSelectionListener(createAdapter("!= "));
		Button greater = new Button(operateComposite, SWT.NONE);
		greater.setLayoutData(new GridData(80, SWT.DEFAULT));
		greater.setText(">");
		greater.addSelectionListener(createAdapter("> "));
		Button less = new Button(operateComposite, SWT.NONE);
		less.setLayoutData(new GridData(80, SWT.DEFAULT));
		less.setText("<");
		less.addSelectionListener(createAdapter("< "));
		
		new Label(operateComposite, SWT.NONE);
		
		Text title4 = new Text(operateComposite, SWT.READ_ONLY);
		title4.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		title4.setText("Statement Annotation");
		
		Button assertion = new Button(operateComposite, SWT.NONE);
		assertion.setLayoutData(new GridData(80, SWT.DEFAULT));
		assertion.setText("assert");
		assertion.addSelectionListener(createAdapter("assert "));
		Button loop_inv = new Button(operateComposite, SWT.NONE);
		loop_inv.setLayoutData(new GridData(80, SWT.DEFAULT));
		loop_inv.setText("loop inv");
		loop_inv.addSelectionListener(createAdapter("loop invariant "));
		
		Label emptyLabel = new Label(operateComposite, SWT.NONE);
		emptyLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		
		Button btn_OK = new Button(operateComposite, SWT.NONE);
		btn_OK.setLayoutData(new GridData(80, SWT.DEFAULT));
		btn_OK.setText("OK");
		btn_OK.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				result = new ArrayList<String>();
				if (oneLineAnnotation.getSelection()) {
					result.add("SINGLE");
				}
				else {
					result.add("MULTI");
				}
				result.add(editArea.getText());
				shell.close();
			}
		});
		
		Button btn_Cancel = new Button(operateComposite, SWT.NONE);
		btn_Cancel.setLayoutData(new GridData(80, SWT.DEFAULT));
		btn_Cancel.setText("Cancel");
		btn_Cancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});
	}
	
	private SelectionAdapter createAdapter(String annotation) {
		SelectionAdapter adapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editArea.insert(annotation);
				editArea.setFocus();
				//System.out.println(editArea.getText());
			}
		};
		return adapter;
	}
}