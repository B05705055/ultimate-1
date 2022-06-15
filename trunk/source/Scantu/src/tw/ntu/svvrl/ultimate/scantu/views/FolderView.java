package tw.ntu.svvrl.ultimate.scantu.views;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import tw.ntu.svvrl.ultimate.scantu.lib.SaveFile;

import org.apache.commons.io.FilenameUtils;

public class FolderView extends ViewPart {
	
	private static TreeViewer viewer;
	//private static String currentDir = "C:\\Users\\user\\Downloads";
	public static String cFileDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().getParent().toString()
			+ "\\source\\Scantu\\cFile";
	private static String currentDir = cFileDir;
	private static File[] inputFile;
	
	public static void setDir(String selectedDir) {
		currentDir = selectedDir;
	}
	
	public static String getDir() {
		return currentDir;
	}
	
	public static void refreshDir() {
		File path = new File(currentDir);
        viewer.setInput(path.listFiles());
	}
	
	public void setInputFile(File[] file) {
		inputFile = file;
	}
	
	public static File[] getInputFile() {
		return inputFile;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setContentProvider(new ViewContentProvider());
        viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
                new ViewLabelProvider()));
        File path = new File(cFileDir);
        viewer.setInput(path.listFiles());
        //viewer.setInput(File.listRoots());
        
        /*Tree tree = (Tree) viewer.getControl();
        tree.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
              TreeItem item = (TreeItem) e.item;
                if (item.getItemCount() > 0) {
                    item.setExpanded(!item.getExpanded());
                    viewer.refresh();
                }
                else if (item.getItemCount() == 0) {
                	final List<File> files = new ArrayList<>();
                	files.add((File) item.getData());
                	setInputFile(((File[]) files.toArray(new File[files.size()])));
                	//System.out.println(getInputFile()[0]);
                	ProgramView.setInputFile(getInputFile());
                }
            }
        });*/
        
        viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				TreeViewer viewer = (TreeViewer) event.getViewer();
		        IStructuredSelection thisSelection = (IStructuredSelection) event.getSelection();
		        File selectedNode = (File) thisSelection.getFirstElement();
		        if (selectedNode.isDirectory() == true) {
		        	 viewer.setExpandedState(selectedNode, !viewer.getExpandedState(selectedNode));
		        }
		        else {
		        	if (!ProgramView.whetherSaved()) {
		        		MessageBox dialog =
		    				    new MessageBox(parent.getShell(), SWT.ICON_WARNING | SWT.YES| SWT.NO);
		    			dialog.setText("Warning");
		    			dialog.setMessage("Your edited file is unsaved, do you want to save it?");
		    			int returnCode = dialog.open();
		    			if (returnCode == SWT.YES) {
		    				SaveFile.save(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		    			}
		    			ProgramView.changeSaveState(true);
		        	}
		        	File[] files = {selectedNode};
		        	setInputFile(files);
		        	ProgramView.setInputFile(getInputFile());
		        }
			}
        });
	}
	
	private ImageDescriptor createImageDescriptor(String fileType) {
        Bundle bundle = FrameworkUtil.getBundle(ViewLabelProvider.class);
        URL url = null;
        if(fileType.equals("FOLDER")) {
        	url = FileLocator.find(bundle, new Path("icons/folder.png"), null);
        }
        else if(fileType.equals("C_FILE")) {
        	url = FileLocator.find(bundle, new Path("icons/c_file.png"), null);
        }
        return ImageDescriptor.createFromURL(url);
    }
	
	class ViewContentProvider implements ITreeContentProvider {
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public Object[] getElements(Object inputElement) {
        	File[] files = (File[]) inputElement;
        	ArrayList<File> specificFiles = new ArrayList<File>();
        	for(File file : files) {
        		if(file.isDirectory() ||
        			FilenameUtils.getExtension(file.getPath()).equals("c")) {
        			specificFiles.add(file);
        		}
        	}
        	File[] returnFiles = new File[specificFiles.size()];
        	returnFiles = specificFiles.toArray(returnFiles);        	
        	return returnFiles;
            //return (File[]) inputElement;
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            File parentFile = (File) parentElement;
            File[] files = parentFile.listFiles();
            ArrayList<File> specificFiles = new ArrayList<File>();
        	for(File file : files) {
        		if(file.isDirectory() ||
        			FilenameUtils.getExtension(file.getPath()).equals("c")) {
        			specificFiles.add(file);
        		}
        	}
        	File[] returnFiles = new File[specificFiles.size()];
        	returnFiles = specificFiles.toArray(returnFiles);        	
        	return returnFiles;
            //return file.listFiles();
        }

        @Override
        public Object getParent(Object element) {
            File file = (File) element;
            return file.getParentFile();
        }

        @Override
        public boolean hasChildren(Object element) {
            File file = (File) element;
            if (file.isDirectory()) {
                return true;
            }
            return false;
        }

    }
	
	class ViewLabelProvider extends LabelProvider implements IStyledLabelProvider {

        private ResourceManager resourceManager;

        public ViewLabelProvider() {
        	
        }

        @Override
        public StyledString getStyledText(Object element) {
            if(element instanceof File) {
                File file = (File) element;
                StyledString styledString = new StyledString(getFileName(file));
                String[] files = file.list();
                if (files != null) {
                    styledString.append(" ( " + files.length + " ) ",
                            StyledString.COUNTER_STYLER);
                }
                return styledString;
            }
            return null;
        }

        @Override
        public Image getImage(Object element) {
            if(element instanceof File) {
                if(((File) element).isDirectory()) {
                    return getResourceManager().createImage(createImageDescriptor("FOLDER"));
                }
                else if(FilenameUtils.getExtension(((File) element).getPath()).equals("c")) {
                	return getResourceManager().createImage(createImageDescriptor("C_FILE"));
                }
            }

            return super.getImage(element);
        }

        @Override
        public void dispose() {
            // garbage collect system resources
            if(resourceManager != null) {
                resourceManager.dispose();
                resourceManager = null;
            }
        }

        protected ResourceManager getResourceManager() {
            if(resourceManager == null) {
                resourceManager = new LocalResourceManager(JFaceResources.getResources());
            }
            return resourceManager;
        }

        private String getFileName(File file) {
            String name = file.getName();
            return name.isEmpty() ? file.getPath() : name;
        }
    }

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}
	
}