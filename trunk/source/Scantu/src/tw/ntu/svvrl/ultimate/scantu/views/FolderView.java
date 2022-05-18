package tw.ntu.svvrl.ultimate.scantu.views;

import java.io.File;
import java.nio.file.Files;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.ViewPart;

import org.apache.commons.io.FilenameUtils;

public class FolderView extends ViewPart {
	
	private static String TREE_DATA_FILE = "FILE";
	private static String currentDir = "C:\\Users\\user\\Documents\\GitHub\\ultimate-1\\trunk\\source";
	
	private static Tree dirTree;
	
	public static void createDirectoryTree (Composite parent, int width, int height) {
		dirTree = new Tree(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		dirTree.setSize(width, height);
		dirTree.addListener(SWT.Expand, new Listener() {
			@Override
			public void handleEvent(Event event) {
				TreeItem item = (TreeItem) event.item;
				File path = (File) item.getData(TREE_DATA_FILE);
				item.removeAll();
				File[] files = path.listFiles();
				for(File file : files) {
					if(Files.isReadable(file.toPath())
							&& !Files.isSymbolicLink(file.toPath())) {
						addChildToDirectoryTree(item, file);
					}
				}
			}
		});
		/*
		dirTree.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				// TODO Auto-generated method stub
				
			}
		});*/

		//File[] roots = File.listRoots();
		String mcurrentDir = getDir();
		File path = new File(mcurrentDir);
		File[] roots = path.listFiles();
		for(File root : roots) {
			addChildToDirectoryTree(dirTree, root);
		}
	}
	
	public static void refreshDirectoryTree() {
		dirTree.removeAll();
		String mcurrentDir = getDir();
		File path = new File(mcurrentDir);
		File[] roots = path.listFiles();
		for(File root : roots) {
			addChildToDirectoryTree(dirTree, root);
		}
	}
	
	private static void addChildToDirectoryTree (Widget parent, File data) {
		if (!(data.isDirectory() || FilenameUtils.getExtension(data.getPath()).equals("c"))) {
			return;
		}
		
		TreeItem dirItem = null;

		if (parent instanceof Tree) {
			dirItem = new TreeItem((Tree) parent, 0);
			//dirItem.setText(data.getAbsolutePath());
			dirItem.setText(data.getName());
		}
		else {
			dirItem = new TreeItem((TreeItem) parent, 0);
			dirItem.setText(data.getName());
		}

		dirItem.setData(TREE_DATA_FILE, data);
		
		if (!FilenameUtils.getExtension(data.getPath()).equals("c")) {
			TreeItem fakeChild = new TreeItem(dirItem, 0);
		}
		//TreeItem fakeChild = new TreeItem(dirItem, 0);
	}
	
	public static void setDir(String selectedDir) {
		currentDir = selectedDir;
	}
	
	public static String getDir() {
		return currentDir;
	}
	
	@Override
	public void createPartControl(Composite parent) {	
		//System.out.println(File.listRoots().getClass().getSimpleName());
		createDirectoryTree(parent, 432, 432); 
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}
	
}