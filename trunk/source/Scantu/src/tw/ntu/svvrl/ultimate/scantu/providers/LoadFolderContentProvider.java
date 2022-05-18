package tw.ntu.svvrl.ultimate.scantu.providers;

import org.eclipse.jface.viewers.IStructuredContentProvider;

import tw.ntu.svvrl.ultimate.scantu.data.Folder;

public class LoadFolderContentProvider implements IStructuredContentProvider {
	
	Folder input;

	@Override
	public Object[] getElements(Object inputElement) {
		// TODO Auto-generated method stub
		Object[] returnValue = {input.getDir()};
		return returnValue;
	}
	
	public String getText(Object element) {
		// TODO Auto-generated method stub
		return ((Folder)element).getDir();
	}
	
}