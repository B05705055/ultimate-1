/**
 * Holds example files.
 */
package de.uni_freiburg.informatik.ultimate.website;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;

import de.uni_freiburg.informatik.ultimate.website.Tasks.TaskNames;

/**
 * @author Markus Lindenmann
 * @author Oleksii Saukh
 * @author Stefan Wissert
 * @date 14.02.2012
 */
public class Example {
	/**
	 * The files content.
	 */
	private String mFileContent;
	/**
	 * The files name; a String describing the file - this is the name shown to
	 * the web user in a dropdown box.
	 */
	private String mFileName;
	/**
	 * The path to the file - actually the files real name without an absolute
	 * path! This is used by the Class Loader to find the file!
	 */
	private String mFilePath;
	/**
	 * The examples identifier used to uniquely identify the example on
	 * communication between client and server.
	 */
	private String mId;
	/**
	 * The taskNames for which these example is applicable.
	 */
	private Tasks.TaskNames[] mTaskNames;
	/**
	 * The map of examples - generated automatically and holding the
	 * representations of the examples, sorted by the corresponding task(s).
	 * This implies, that examples with multiple tasks are contained multiply!
	 */
	private static final Map<Tasks.TaskNames, ArrayList<Example>> mExamplesByTask = new HashMap<Tasks.TaskNames, ArrayList<Example>>();
	/**
	 * Set of IDs - used to verify, that the ids are really unique!
	 */
	private static final Set<String> mIds = new HashSet<String>();
	/**
	 * List of examples, sorted by their ids - each example is therefore
	 * contained only once!
	 */
	private static final Map<String, Example> mExamplesById = new HashMap<String, Example>();

	/**
	 * Initialize the Example lists.
	 */
	private static void init() {
		SimpleLogger.log("Initializing Examples.");
		ArrayList<Example> list = new ArrayList<Example>();
		list.add(new Example("Example C File", "exampleCFile", "exampleFile.c",
				new Tasks.TaskNames[] { Tasks.TaskNames.VerifyC }));
		list.add(new Example("F-91", "f91", "f91.c", new Tasks.TaskNames[] { Tasks.TaskNames.VerifyC }));
		// list.add(new Example("addition correct", "additionCorrect",
		// "additionCorrect.bpl",
		// new Tasks.TaskNames[] { Tasks.TaskNames.VerifyBoogie }));
		// list.add(new Example("addition incorrect", "additionIncorrect",
		// "additionIncorrect.bpl",
		// new Tasks.TaskNames[] { Tasks.TaskNames.VerifyBoogie }));
		// list.add(new Example("Moscow.bpl", "Moscow", "Moscow.bpl",
		// new Tasks.TaskNames[] { Tasks.TaskNames.RANK_SYNTHESIS_BOOGIE }));
		// list.add(new Example("Bangalore.c", "BangaloreC", "Bangalore.c",
		// new Tasks.TaskNames[] { Tasks.TaskNames.RANK_SYNTHESIS_C }));
		Tasks.TaskNames[] rankBoogie = { Tasks.TaskNames.RANK_SYNTHESIS_BOOGIE };
		addAllFilesInExamplesSubfolder(list, "rankBoogie/", rankBoogie);
		Tasks.TaskNames[] rankC = { Tasks.TaskNames.RANK_SYNTHESIS_C };
		addAllFilesInExamplesSubfolder(list, "rankC/", rankC);
		Tasks.TaskNames[] terminatonC = { Tasks.TaskNames.TERMINATION_C };
		addAllFilesInExamplesSubfolder(list, "terminationC/", terminatonC);
		Tasks.TaskNames[] terminationBoogie = { Tasks.TaskNames.TERMINATION_BOOGIE };
		addAllFilesInExamplesSubfolder(list, "terminationBoogie/", terminationBoogie);
		Tasks.TaskNames[] verifyC = { Tasks.TaskNames.VerifyC };
		addAllFilesInExamplesSubfolder(list, "verifyC/", verifyC);
		Tasks.TaskNames[] automataScript = { Tasks.TaskNames.AUTOMATA_SCRIPT };
		addAllFilesInExamplesSubfolder(list, "AUTOMATA_SCRIPT/", automataScript);
		Tasks.TaskNames[] verifyBoogie = { Tasks.TaskNames.VerifyBoogie };
		addAllFilesInExamplesSubfolder(list, "verifyBoogie/", verifyBoogie);
		Tasks.TaskNames[] verifyConcurrentBoogie = { Tasks.TaskNames.VerifyConcurrentBoogie };
		addAllFilesInExamplesSubfolder(list, "verifyConcurrentBoogie/", verifyConcurrentBoogie);
		// TODO : add more/new examples here
		for (Example e : list) {
			try {
				new Example(e.mFileName, e.mId, e.mTaskNames, readFile(e.mFilePath));
			} catch (IOException ex) {
				/* file cannot be parsed -> skip it! */
				ex.printStackTrace();
			} catch (IllegalArgumentException ex) {
				ex.printStackTrace();
				/* file not valid -> skip it! */
			}
		}
	}

	/**
	 * Returns a map of example files sorted by their corresponding task. This
	 * map
	 * 
	 * @return a map of example files.
	 */
	public static Map<Tasks.TaskNames, ArrayList<Example>> getExamples() {
		if (mExamplesByTask.isEmpty()) {
			SimpleLogger.log("sortedMap is empty");
			init();
		}
		return mExamplesByTask;
	}

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            the file name to be shown in the Website
	 * @param taskNames
	 *            the corresponding tasks, where this example can be used
	 * @param content
	 *            the file content
	 * @param id
	 *            the id
	 */
	private Example(String name, String id, Tasks.TaskNames[] taskNames, String content) {
		if (name == null || name.equals("") || name.length() > 30) {
			throw new IllegalArgumentException("Not a valid file name!");
		}
		if (content == null || content.equals("")) {
			throw new IllegalArgumentException("File content empty!");
		}
		if (taskNames.length == 0) {
			throw new IllegalArgumentException("Example must be assign to at least one TaskName!");
		}
		if (mIds.contains(id)) {
			throw new IllegalArgumentException("ID must be unique! " + "Not unique: " + name + " " + id);
		}
		if (id == null || id.equals("")) {
			throw new IllegalArgumentException("ID cannot be empty!");
		}
		if (id.length() > 30) {
			throw new IllegalArgumentException("ID cannot be longer than 30 characters!");
		}
		if (!id.matches("[a-zA-Z0-9]*")) {
			throw new IllegalArgumentException("ID must be element of (a-Z0-9)*");
		}
		this.mFileName = name;
		this.mFileContent = content;
		this.mId = id;
		mIds.add(id);
		for (Tasks.TaskNames tn : taskNames) {
			if (!mExamplesByTask.containsKey(tn)) {
				mExamplesByTask.put(tn, new ArrayList<Example>());
			}
			mExamplesByTask.get(tn).add(this);
		}
		mExamplesById.put(id, this);
	}

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            the file name to be shown in the Website
	 * @param path
	 *            the path to the file
	 * @param taskNames
	 *            the corresponding tasks, where this example can be used
	 * @param id
	 *            the id
	 */
	private Example(String name, String id, String path, Tasks.TaskNames[] taskNames) {
		this.mFileName = name;
		this.mFilePath = path;
		this.mTaskNames = taskNames;
		this.mId = id;
	}

	/**
	 * Reads a file and returns its content in a String
	 * 
	 * @param name
	 *            the path to the file
	 * @return the file content in a String
	 * @throws IOException
	 *             care for it ...
	 */
	private static final String readFile(String name) throws IOException {
		String example = "/resources/examples/" + name;
		InputStream inStream = Example.class.getResourceAsStream(example);
		if (inStream == null) {
			return "// File not found!";
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
		String inputLine;
		String eol = System.getProperty("line.separator");
		StringBuilder fileContents = new StringBuilder();
		try {
			while ((inputLine = in.readLine()) != null) {
				fileContents.append(inputLine + eol);
			}
			return fileContents.toString();
		} finally {
			in.close();
		}
	}

	/**
	 * Return the filenames of the files in the folder /resources/examples/ +
	 * dirSuffix (path given relative to root of this package).
	 * 
	 * We use the classloader to get the URL of this folder. We support only
	 * URLs with protocol <i>file</i> and <i>bundleresource</i>. At the moment
	 * these are the only ones that occur in Website and WebsiteEclipseBridge.
	 */
	static private String[] filenamesOfFilesInSubdirectory(String dirSuffix) {
		String dir = "/resources/examples/" + dirSuffix;
		SimpleLogger.log("Trying to get all files in directory " + dir);
		URL dirURL = Example.class.getClassLoader().getResource(dir);
		if (dirURL == null) {
			SimpleLogger.log("Folder " + dir + " does not exist");
			return new String[0];
		}
		SimpleLogger.log("URL " + dirURL);
		String protocol = dirURL.getProtocol();
		File dirFile = null;
		if (protocol.equals("file")) {
			try {
				dirFile = new File(dirURL.toURI());
			} catch (URISyntaxException e) {
				e.printStackTrace();
				SimpleLogger.log("Failed");
				return new String[0];
			}
		} else if (protocol.equals("bundleresource")) {
			try {
				URL fileURL = FileLocator.toFileURL(dirURL);
				dirFile = new File(fileURL.toURI());
			} catch (Exception e) {
				e.printStackTrace();
				SimpleLogger.log("Failed");
				return new String[0];
			}
		} else {
			SimpleLogger.log("Unkown protocol " + protocol);
			return new String[0];
		}
		String[] content = dirFile.list();
		if (content == null) {
			SimpleLogger.log("No folder:" + dirURL);
			return new String[0];
		} else {
			Arrays.sort(content);
			return content;
		}
	}

	/**
	 * Add all files in subfolder of /resource/example as Example to list for
	 * taskNames. The id of the examples is defined by hash codes.
	 */
	static private void addAllFilesInExamplesSubfolder(List<Example> list, String subfolder, TaskNames[] taskNames) {
		String[] filesInSubfolder = filenamesOfFilesInSubdirectory(subfolder);
		for (String filename : filesInSubfolder) {
			String exPath = subfolder + filename;
			String id = String.valueOf(Math.abs(exPath.hashCode()));
			// crop name to 30 characters
			String name;
			if (filename.length() > 30) {
				name = filename.substring(0, Math.min(27, filename.length())).concat("...");
			} else {
				name = filename;
			}
			list.add(new Example(name, id, exPath, taskNames));
			System.out.println("Added example " + filename + " and assigned id " + id);
		}
	}

	/**
	 * Getter for file content.
	 * 
	 * @return the fileContent
	 */
	public String getFileContent() {
		return mFileContent;
	}

	/**
	 * Getter for file name.
	 * 
	 * @return the fileName
	 */
	public String getFileName() {
		return mFileName;
	}

	/**
	 * Getter for task name.
	 * 
	 * @return the taskNames
	 */
	public Tasks.TaskNames[] getTaskNames() {
		return mTaskNames;
	}

	/**
	 * Getter for the examples id.
	 * 
	 * @return the id
	 */
	public String getId() {
		return mId;
	}

	public String getInfoString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Example ").append(getId()).append(" from File ").append(getFileName()).append(" (Tasks [");

		if (getTaskNames() != null) {
			for (TaskNames taskName : getTaskNames()) {
				sb.append(taskName).append(", ");
			}
			if (getTaskNames().length > 0) {
				// sb.delete(sb.length() - 2, sb.length());
			}
		}
		sb.append("])");
		return sb.toString();
	}

	/**
	 * Get the example with the identifier id.
	 * 
	 * @param id
	 *            the identifier
	 * @return the corresponding example or null, if ID not found!
	 */
	public static final Example get(String id) {
		if (mExamplesById.isEmpty()) {
			SimpleLogger.log("Example list is empty, initializing...");
			init();
		}
		Example ex = mExamplesById.get(id);
		if (ex != null) {
			SimpleLogger.log("Get example returns " + ex.getInfoString());
		} else {
			SimpleLogger.log("Get example returns NULL because ID " + id + " was not found");
		}

		return ex;
	}
}
