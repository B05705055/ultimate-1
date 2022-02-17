package tw.ntu.svvrl.ultimate.scantu;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class ScantuGUI {

	protected Shell shlScantu;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			ScantuGUI window = new ScantuGUI();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shlScantu.open();
		shlScantu.layout();
		while (!shlScantu.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shlScantu = new Shell();
		shlScantu.setSize(720, 480);
		shlScantu.setText("Source Code Analyzer NTU");

	}

}
