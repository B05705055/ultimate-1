package tw.ntu.svvrl.ultimate.scantu.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

public class OperationView extends ViewPart {

	@Override
	public void createPartControl(Composite parent) {
		Text text = new Text(parent, SWT.BORDER);
		text.setText("The operation view.");
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}
	
}