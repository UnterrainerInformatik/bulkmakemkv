package info.unterrainer.java.tools.scripting.bulkmakemkv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import info.unterrainer.java.tools.scripting.bulkmakemkv.filevisitors.DirectoryNameEqualsVisitor;
import info.unterrainer.java.tools.scripting.bulkmakemkv.filevisitors.ScanVisitor;
import info.unterrainer.java.tools.scripting.bulkmakemkv.syscommandexecutor.SysCommandExecutor;

public class BulkMakeMkv {

	public static final String		regExMakeMkvFailedTracks	= "Copy complete\\. [\\d+] titles saved"
																		+ ", ([\\d+]) failed\\.";

	private static Configuration	config;

	private static String			mode;
	private static List<String>		isoDirs;
	private static List<String>		isoRegExps;
	private static String			tempDir;
	private static String			mkvDir;
	private static List<String>		observeMkvDirs;

	private static boolean			convertShows;
	private static boolean			convertMovies;

	private static String			isoFileExtension;
	private static String			mkvFileExtension;

	private static String			makeMkvCommand;
	private static String			makeMkvTempFileExtension;

	private static List<Path>		cache;

	public static void main(String[] args) {
		try {
			config = new PropertiesConfiguration(new File("config.properties"));
		}
		catch (ConfigurationException e) {
			e.printStackTrace();
		}

		mode = config.getString("mode");
		if (mode == null || mode.isEmpty()) {
			Utils.sysout("You have to specify a valid mode!");
			System.exit(1);
		}
		mode = mode.toLowerCase();
		if (!mode.equals("convert") && !mode.equals("scan")) {
			Utils.sysout("You have to specify a valid mode!");
			System.exit(1);
		}

		// Get parameter isoDirs.
		String[] t = config.getStringArray("isoDirs");
		isoDirs = new ArrayList<String>();
		if (t != null) {
			for (String s : t) {
				if (s != null && !s.equals("")) {
					isoDirs.add(Utils.normalizeDirectory(s));
				}
			}
		}
		else {
			Utils.sysout("You have to specify at least a single valid isoDirs value!");
			System.exit(1);
		}

		// Get parameter isoRegExps.
		t = config.getStringArray("isoRegExps");
		isoRegExps = new ArrayList<String>();
		if (t != null) {
			for (String s : t) {
				if (s != null) {
					isoRegExps.add(s);
				}
			}
		}

		tempDir = Utils.normalizeDirectory(config.getString("tempDir"));
		mkvDir = Utils.normalizeDirectory(config.getString("mkvDir"));

		// Get parameter observeMkvDirs.
		t = config.getStringArray("observeMkvDirs");
		observeMkvDirs = new ArrayList<String>();
		observeMkvDirs.add(mkvDir);
		if (t != null) {
			for (String s : t) {
				if (s != null && !s.equals("")) {
					observeMkvDirs.add(Utils.normalizeDirectory(s));
				}
			}
		}
		else {
			Utils.sysout("You have to specify at least a single valid observeMkvDirs value!");
			System.exit(1);
		}

		isoFileExtension = config.getString("isoFileExtension");
		mkvFileExtension = config.getString("mkvFileExtension");

		convertShows = config.getBoolean("convertShows");
		convertMovies = config.getBoolean("convertMovies");

		makeMkvCommand = config.getString("makeMkvCommand");
		makeMkvTempFileExtension = config.getString("makeMkvTempFileExtension");

		checkTempDir();
		checkMkvDir();

		if (!mode.equals("scan")) {
			checkExists(isoDirs, "isoDirs");
		}
		checkExists(observeMkvDirs, "observeMkvDirs");

		if (mode.equals("convert")) {
			convert();
			scan();
		}

		if (mode.equals("scan")) {
			scan();
		}
		Utils.sysout("Done.");
	}

	private static void scan() {
		Utils.sysout("Scanning... (this may take a while depending on the number and size of your "
				+ "observeMkvDirs directories)");

		List<String> notYetConvertedIsos = new ArrayList<String>();
		boolean resetCache = true;
		for (String isoDir : isoDirs) {
			File iso = new File(isoDir);
			File[] isoFiles = iso.listFiles();

			for (File file : isoFiles) {
				if (!file.isDirectory()) {
					FileName name = new FileName(file);
					if (name.getExtension().toLowerCase().equals("iso")) {
						if (name.isBonusDisc()) {
							continue;
						}
						List<String> p = exists(name, resetCache);
						resetCache = false;
						if (p.isEmpty()) {
							notYetConvertedIsos.add(name.getFile().getPath());
						}
					}
				}
			}
		}

		List<String> emptyDirectories = new ArrayList<String>();
		List<String> emptyFiles = new ArrayList<String>();
		List<String> wrongNumberOfEpisodes = new ArrayList<String>();

		for (String s : observeMkvDirs) {
			ScanVisitor v = new ScanVisitor(mkvFileExtension);
			try {
				Files.walkFileTree(new File(s).toPath(), v);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			emptyDirectories.addAll(v.getEmptyDirectories());
			emptyFiles.addAll(v.getEmptyFiles());
			wrongNumberOfEpisodes.addAll(v.getWrongNumberOfEpisodes());
		}

		Utils.sysout("######### SCAN RESULTS: #########");
		Utils.sysout("Not yet converted ISOs:");
		printList(notYetConvertedIsos, "  ");
		Utils.sysout();
		Utils.sysout("Empty directories:");
		printList(emptyDirectories, "  ");
		Utils.sysout();
		Utils.sysout("Empty files (files with size zero):");
		printList(emptyFiles, "  ");
		Utils.sysout();
		Utils.sysout("Wrong number of episodes in directory:");
		printList(wrongNumberOfEpisodes, "  ");
		Utils.sysout();

		Utils.sysout("Done scanning.");
		Utils.sysout("#################################");
	}

	private static void printList(List<String> list, String prefix) {
		for (String s : list) {
			Utils.sysout(prefix + s);
		}
	}

	private static void convert() {
		Utils.sysout("Converting... (this will definitely take a while depending on the number of "
				+ "unconverted files in your observeMkvDirs directories)");
		boolean resetCache = true;
		for (String isoDir : isoDirs) {
			File iso = new File(isoDir);
			File[] isoFiles = iso.listFiles();

			for (File file : isoFiles) {
				if (!file.isDirectory()) {
					FileName name = new FileName(file);
					if (name.getExtension().toLowerCase().equals("iso")) {
						if (name.isBonusDisc()) {
							Utils.sysout("skipping: " + name.getName() + " (is bonus disc)");
							continue;
						}
						List<String> p = exists(name, resetCache);
						resetCache = false;
						if (!p.isEmpty()) {
							String out = "skipping: " + name.getName() + ". Already exists in... ";
							for (String s : p) {
								out += "\n    " + s;
							}
							Utils.sysout(out);
							continue;
						}
						if (!name.getEpisodesLongContents().isEmpty() || !name.getEpisodesShortContents().isEmpty()) {
							// This is a TV-show.
							if (!convertShows) {
								continue;
							}
						}
						else {
							// This is a movie.
							if (!convertMovies) {
								continue;
							}
						}
						boolean convert = isoRegExps.isEmpty();
						if (!convert) {
							for (String rex : isoRegExps) {
								List<Match> matches = Utils.getPattern(name.getFile().getName(), rex, 0);
								if (!matches.isEmpty()) {
									convert = true;
									break;
								}
							}
						}
						if (!convert) {
							Utils.sysout("skipping (no regexp match): " + name.getName());
							continue;
						}
						Utils.sysout("converting: " + name.getName());
						boolean conversionResult = doConvert(name, isoDir);
						List<FileName> tempFiles = scanTempDirectory();
						if (!conversionResult) {
							Utils.sysout("  FAILED. Deleting temporary files. NOT copying. Source is omitted!");
							for (FileName f : tempFiles) {
								try {
									Files.delete(f.getFile().toPath());
								}
								catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						else {
							moveAndRename(name, tempFiles);
							resetCache = true;
						}
					}
					else {
						Utils.sysout("skipping: "
								+ name.getName()
								+ " (not an ."
								+ isoFileExtension.toLowerCase()
								+ " file)");
					}
				}
			}
		}
		Utils.sysout("Done converting.");
	}

	private static void checkExists(List<String> dirs, String parameterName) {
		for (String dir : dirs) {
			File d = new File(dir);
			if (!d.exists()) {
				Utils.sysout("The " + parameterName + " you specified [" + d.toString() + "] doesn't exist.");
				System.exit(1);
			}
		}
	}

	private static List<String> exists(FileName file, boolean resetCache) {
		List<String> result = new ArrayList<String>();
		boolean reset = resetCache;
		if (cache == null) {
			cache = new ArrayList<Path>();
			reset = true;
		}

		if (reset) {
			cache.clear();
			for (String s : observeMkvDirs) {
				DirectoryNameEqualsVisitor v = new DirectoryNameEqualsVisitor(file.getFolderName());
				try {
					Files.walkFileTree(new File(s).toPath(), v);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				result.addAll(v.getResult());
				cache.addAll(v.getCache());
			}
		}
		else {
			String dirName = Utils.normalizeDirectory(file.getFolderName()).replace("/", "\\");
			for (Path dir : cache) {
				String curr = Utils.normalizeDirectory(dir.getFileName().toString()).replace("/", "\\");
				if (dirName.toLowerCase().equals(curr.toLowerCase())) {
					result.add(dir.toString());
				}
			}
		}
		return result;
	}

	private static boolean doConvert(FileName file, String isoDir) {
		String command = "\""
				+ makeMkvCommand
				+ "\" mkv iso:\""
				+ isoDir
				+ file.getName()
				+ "."
				+ file.getExtension()
				+ "\" all \""
				+ tempDir
				+ "\"";
		Utils.sysout(command);
		return doCommand(command);
	}

	private static List<FileName> scanTempDirectory() {
		List<FileName> result = new ArrayList<FileName>();
		File[] tempFiles = new File(tempDir).listFiles();
		for (File f : tempFiles) {
			if (!f.isDirectory() && f.getName().toLowerCase().endsWith(makeMkvTempFileExtension.toLowerCase())) {
				result.add(new FileName(f));
			}
		}
		return result;
	}

	private static void moveAndRename(FileName file, List<FileName> tempFiles) {
		if (tempFiles.size() == 0) {
			return;
		}

		long sizeSum = 0;
		FileName biggest = null;
		for (FileName f : tempFiles) {
			sizeSum += f.getSize();
			if (biggest == null || biggest.getSize() <= f.getSize()) {
				biggest = f;
			}
		}

		File d = new File((mkvDir + file.getFolderName() + "/").replace("/", "\\"));
		String dString = (d.toPath() + "/").replace("/", "\\");
		try {
			Files.createDirectory(d.toPath());
		}
		catch (IOException e1) {
			e1.printStackTrace();
			System.exit(1);
		}

		if (!file.getEpisodesLongContents().isEmpty() || !file.getEpisodesShortContents().isEmpty()) {
			Utils.sysout("This is a TV-show.");

			if (sizeSum > file.getSize()) {
				Utils.sysout("There is more data than in the source-file. Deleting biggest one.");
				// Probably makeMKV has added a 'catch-all'-MKV file containing all data.
				try {
					Files.delete(biggest.getFile().toPath());
				}
				catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				tempFiles.remove(biggest);

				// Update analysis data.
				sizeSum = 0;
				for (FileName f : tempFiles) {
					sizeSum += f.getSize();
					if (biggest == null || biggest.getSize() <= f.getSize()) {
						biggest = f;
					}
				}
			}

			int i = 1;
			String start = "";
			if (!file.getEpisodesLongContents().isEmpty()) {
				try {
					start = "- s" + file.getEpisodesLongContents().get(0).getGroups().get(0) + "e";
					i = Integer.parseInt(file.getEpisodesLongContents().get(0).getGroups().get(1));
				}
				catch (NumberFormatException e) {
				}
			}
			if (!file.getEpisodesShortContents().isEmpty()) {
				try {
					start = "- s" + file.getEpisodesShortContents().get(0).getGroups().get(0) + "e";
					i = Integer.parseInt(file.getEpisodesShortContents().get(0).getGroups().get(1));
				}
				catch (NumberFormatException e) {
				}
			}
			for (FileName f : tempFiles) {
				try {
					Files.move(f.getFile().toPath(), (new File(dString
							+ file.getFileName()
							+ " "
							+ start
							+ makeTwoDigits(i)
							+ "."
							+ mkvFileExtension)).toPath());
					i++;
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		else {
			Utils.sysout("This is a movie.");
			// Just copy the biggest one.
			String year = "";
			if (file.getYear() != null) {
				year = " (" + file.getYear() + ")";
			}
			try {
				Files.move(biggest.getFile().toPath(), (new File(dString
						+ file.getFileName()
						+ year
						+ "."
						+ mkvFileExtension)).toPath());
				tempFiles.remove(biggest);

				for (FileName f : tempFiles) {
					Files.delete(f.getFile().toPath());
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static String makeTwoDigits(int i) {
		if (i < 10) {
			return "0" + i;
		}
		return i + "";
	}

	private static void checkMkvDir() {
		File tDir = new File(mkvDir);
		if (!tDir.exists()) {
			try {
				Files.createDirectory(tDir.toPath());
			}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	private static void checkTempDir() {
		File tDir = new File(tempDir);
		if (!tDir.exists()) {
			try {
				Files.createDirectory(tDir.toPath());
			}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		File[] isoFiles = tDir.listFiles();
		for (File file : isoFiles) {
			if (!file.isDirectory()) {
				FileName name = new FileName(file);
				if (name.getExtension().equals(makeMkvTempFileExtension)) {
					Utils.sysout("Severe error: There already where temporary files in your temp-directory!");
					System.exit(1);
				}
			}
		}
	}

	private static boolean doCommand(String command) {
		SysCommandExecutor cmdExecutor = new SysCommandExecutor();
		int exitStatus = 0;
		try {
			exitStatus = cmdExecutor.runCommand(command);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		String cmdError = cmdExecutor.getCommandError();
		String cmdOutput = cmdExecutor.getCommandOutput();

		List<Match> e = null;
		List<Match> o = null;
		if (cmdError != null && !cmdError.equals("")) {
			cmdError = "  " + cmdError.trim().replace("\n", "\n  ").trim();
			e = Utils.getPattern(cmdError, regExMakeMkvFailedTracks, 0);
		}
		if (cmdOutput != null && !cmdOutput.equals("")) {
			cmdOutput = "  " + cmdOutput.trim().replace("\n", "\n  ").trim();
			o = Utils.getPattern(cmdOutput, regExMakeMkvFailedTracks, 0);
		}

		Utils.sysout("  " + exitStatus + "");
		Utils.sysoutNNNE(cmdOutput);
		Utils.sysoutNNNE(cmdError);

		return !testFailed(e) && !testFailed(o);
	}

	private static boolean testFailed(List<Match> input) {
		if (input != null && !input.isEmpty() && !input.get(0).getGroups().isEmpty()) {
			String t = input.get(0).getGroups().get(0);
			int i = 0;
			try {
				i = Integer.parseInt(t);
				if (i > 0) {
					return true;
				}
			}
			catch (NumberFormatException ex) {
			}
		}
		return false;
	}
}