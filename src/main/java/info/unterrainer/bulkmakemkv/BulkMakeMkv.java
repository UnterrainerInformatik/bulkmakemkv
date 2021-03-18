package info.unterrainer.bulkmakemkv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import info.unterrainer.bulkmakemkv.filevisitors.DirectoryNameEqualsVisitor;
import info.unterrainer.bulkmakemkv.filevisitors.ScanVisitor;
import info.unterrainer.bulkmakemkv.syscommandexecutor.ConsoleLogDevice;
import info.unterrainer.bulkmakemkv.syscommandexecutor.SysCommandExecutor;
import info.unterrainer.java.tools.utils.NullUtils;
import info.unterrainer.java.tools.utils.StringUtils;

public class BulkMakeMkv {

	private static final String fallbackConfigFn = "config.properties";
	public static final String regExMakeMkvFailedTracks = "Copy complete\\. [\\d+] titles saved"
			+ ", ([\\d+]) failed\\.";

	private static String os;
	private static String mode;
	private static List<String> isoDirs;
	private static List<String> isoRegExps;
	private static String tempDir;
	private static String mkvDir;
	private static List<String> observeMkvDirs;

	private static boolean convertShows;
	private static boolean convertMovies;

	private static String isoFileExtension;
	private static String mkvFileExtension;

	private static String makeMkvCommand;
	private static String makeMkvTempFileExtension;

	private static List<Path> cache;

	public static void main(final String[] args) {

		if (args.length > 1)
			wrongNumberOfArguments("bulkmakemkv", fallbackConfigFn);
		String configFileName = parseArg(args, 0);
		Configuration config = readConfigurationFile(configFileName, fallbackConfigFn);

		mode = config.getString("mode");
		if (mode == null || mode.isEmpty()) {
			Utils.sysout("You have to specify a valid mode!");
			System.exit(1);
		}
		mode = mode.toLowerCase();
		if (!mode.contains("convert") && !mode.contains("scan")) {
			Utils.sysout("You have to specify a valid mode! It has to contain either 'convert' or 'scan'.");
			System.exit(1);
		}
		if (mode.contains("convert") && mode.contains("scan")) {
			Utils.sysout("You have to specify a valid mode! It has to contain either 'convert' or 'scan'. Not both.");
			System.exit(1);
		}
		os = config.getString("os");
		if (os == null || os.isEmpty() || !os.equals("mac") && !os.equals("linux"))
			os = "windows";

		// Get parameter isoDirs.
		String[] t = config.getStringArray("isoDirs");
		isoDirs = new ArrayList<>();
		if (t != null) {
			for (String s : t)
				if (s != null && !s.equals(""))
					isoDirs.add(Utils.normalizeDirectory(s));
		} else {
			Utils.sysout("You have to specify at least a single valid isoDirs value!");
			System.exit(1);
		}

		// Get parameter isoRegExps.
		t = config.getStringArray("isoRegExps");
		isoRegExps = new ArrayList<>();
		if (t != null)
			for (String s : t)
				if (s != null)
					isoRegExps.add(s);

		tempDir = Utils.normalizeDirectory(config.getString("tempDir"));
		mkvDir = Utils.normalizeDirectory(config.getString("mkvDir"));

		// Get parameter observeMkvDirs.
		t = config.getStringArray("observeMkvDirs");
		observeMkvDirs = new ArrayList<>();
		observeMkvDirs.add(mkvDir);
		if (t != null) {
			for (String s : t)
				if (s != null && !s.equals(""))
					observeMkvDirs.add(Utils.normalizeDirectory(s));
		} else {
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
		makeDirOrExitOnFailure(new File(mkvDir));

		if (!mode.contains("scan"))
			checkExists(isoDirs, "isoDirs");
		checkExists(observeMkvDirs, "observeMkvDirs");

		if (mode.contains("convert")) {
			convert();
			scan();
		}

		if (mode.contains("scan"))
			scan();
		Utils.sysout("Done.");
	}

	private static void wrongNumberOfArguments(final String program, final String fallbackConfigFn) {
		Utils.sysout("Wrong number of arguments. Usage:\n" + program + "\n" + "or\n" + program
				+ " <configFilePathAndName>\n\n"
				+ "If you specify a config file, it has to be a valid apache-configuration file. "
				+ "If you don't, the program will try to fall back on a file named '" + fallbackConfigFn
				+ "' located in the directory you started the application from.");
		System.exit(1);
	}

	/**
	 * Parses the arguments array at the position 'index' and returns the value as a
	 * string.
	 *
	 * @param args  the argument-array
	 * @param index the index of the argument to retrieve
	 * @return the argument at position index or null if an error occurred
	 */
	private static String parseArg(final String[] args, final int index) {
		if (args.length <= index)
			return null;

		String result = args[index];
		if (!StringUtils.isBlank(result))
			result = StringUtils.stripQuotes(result);
		return result;
	}

	/**
	 * After executing this part, the global variable config is either set, or the
	 * application exited.
	 */
	private static Configuration readConfigurationFile(final String fn, final String fallbackFn) {
		Configuration result = null;
		if (fn != null)
			result = loadConfigurationFile(fn,
					"The file you specified via the parameter '%s' is missing.\nTrying to fall back to config.properties in execution directory.",
					"The file you specified via the parameter '%s' is not a valid config file.\nTrying to fall back to property file in execution directory.");
		if (result == null)
			result = loadConfigurationFile(fallbackFn,
					"Config file not found.\nSee to it that there is a proper config file called '%s' in the execution directory or take any other config "
							+ "file and start the program with the path (to that file) and name as a commandline argument.",
					"Config file '%s' is not a valid config file.");
		if (result == null)
			System.exit(1);
		return result;
	}

	private static Configuration loadConfigurationFile(final String fn, final String errorMessageNotFound,
			final String errorMessageWrongFormat) {
		Configuration result = null;
		File f = new File(fn);
		if (f.exists())
			try {
				result = new PropertiesConfiguration(f);
			} catch (ConfigurationException e) {
				Utils.sysout(String.format(errorMessageWrongFormat, fn));
			}
		else
			Utils.sysout(String.format(errorMessageNotFound, fn));
		return result;
	}

	private static void scan() {
		Utils.sysout("Scanning... (this may take a while depending on the number and size of your "
				+ "observeMkvDirs directories)");

		List<String> notYetConvertedIsos = new ArrayList<>();
		boolean resetCache = true;
		for (String isoDir : isoDirs) {
			File iso = new File(isoDir);
			File[] isoFiles = iso.listFiles();

			if (isoFiles != null)
				for (File file : isoFiles)
					if (!file.isDirectory()) {
						FileName name = new FileName(file);
						if (name.getExtension().toLowerCase().equals("iso")) {
							if (name.isBonusDisc())
								continue;
							List<String> p = exists(name, resetCache);
							resetCache = false;
							if (p.isEmpty())
								notYetConvertedIsos.add(name.getFile().getPath());
						}
					}
		}

		List<String> emptyDirectories = new ArrayList<>();
		List<String> emptyFiles = new ArrayList<>();
		List<String> wrongNumberOfEpisodes = new ArrayList<>();

		for (String s : observeMkvDirs) {
			ScanVisitor v = new ScanVisitor(mkvFileExtension);
			try {
				Files.walkFileTree(new File(s).toPath(), v);
			} catch (IOException e) {
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

	private static void printList(final List<String> list, final String prefix) {
		for (String s : list)
			Utils.sysout(prefix + s);
	}

	private static void convert() {
		Utils.sysout("Converting... (this will definitely take a while depending on the number of "
				+ "unconverted files in your observeMkvDirs directories)");
		boolean resetCache = true;
		for (String isoDir : isoDirs) {
			File iso = new File(isoDir);
			File[] isoFiles = iso.listFiles();

			if (isoFiles != null)
				for (File file : isoFiles)
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
								for (String s : p)
									out += "\n    " + s;
								Utils.sysout(out);
								continue;
							}
							if (!name.getEpisodesLongContents().isEmpty()
									|| !name.getEpisodesShortContents().isEmpty()) {
								// This is a TV-show.
								if (!convertShows)
									continue;
							} else // This is a movie.
							if (!convertMovies)
								continue;
							boolean convert = isoRegExps.isEmpty();
							if (!convert)
								for (String rex : isoRegExps) {
									List<Match> matches = Utils.getPattern(name.getFile().getName(), rex, 0);
									if (!matches.isEmpty()) {
										convert = true;
										break;
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
								for (FileName f : tempFiles)
									try {
										Files.delete(f.getFile().toPath());
									} catch (IOException e) {
										e.printStackTrace();
									}
							} else {
								moveAndRename(name, tempFiles);
								resetCache = true;
							}
						} else
							Utils.sysout("skipping: " + name.getName() + " (not an ." + isoFileExtension.toLowerCase()
									+ " file)");
					}
		}
		Utils.sysout("Done converting.");
	}

	private static void checkExists(final List<String> dirs, final String parameterName) {
		for (String dir : dirs) {
			File d = new File(dir);
			if (!d.exists()) {
				Utils.sysout("The " + parameterName + " you specified [" + d.toString() + "] doesn't exist.");
				System.exit(1);
			}
		}
	}

	private static List<String> exists(final FileName file, final boolean resetCache) {
		List<String> result = new ArrayList<>();
		boolean reset = resetCache;
		if (cache == null) {
			cache = new ArrayList<>();
			reset = true;
		}

		if (reset) {
			cache.clear();
			for (String s : observeMkvDirs) {
				DirectoryNameEqualsVisitor v = new DirectoryNameEqualsVisitor(NullUtils.noNull(file.getFolderName()));
				try {
					Files.walkFileTree(new File(s).toPath(), v);
				} catch (IOException e) {
					e.printStackTrace();
				}
				result.addAll(v.getResult());
				cache.addAll(v.getCache());
			}
		} else {
			String dirName = Utils.normalizeDirectory(file.getFolderName());
			for (Path dir : cache) {
				String curr = Utils.normalizeDirectory(dir.getFileName().toString());
				if (curr != null && dirName != null && dirName.toLowerCase().equals(curr.toLowerCase()))
					result.add(dir.toString());
			}
		}
		return result;
	}

	private static boolean doConvert(final FileName file, final String isoDir) {
		String command = "\"" + makeMkvCommand + "\" mkv iso:\"" + isoDir + file.getName() + "." + file.getExtension()
				+ "\" all  -r --progress=-same \"" + tempDir + "\"";
		if (os.equals("mac"))
			command = makeMkvCommand.replace(" ", "\\ ") + " mkv iso:"
					+ (isoDir + file.getName() + "." + file.getExtension()).replace(" ", "\\ ")
					+ " all -r --progress=-same " + tempDir.replace(" ", "\\ ");
		if (os.equals("linux"))
			command = "'" + makeMkvCommand + "'" + " mkv iso:" + "'" + isoDir + file.getName() + "."
					+ file.getExtension() + "'" + " all -r --progress=-same " + "'" + tempDir + "'";

		Utils.sysout(command);
		return doCommand(command);
	}

	private static List<FileName> scanTempDirectory() {
		List<FileName> result = new ArrayList<>();
		File[] tempFiles = new File(tempDir).listFiles();
		if (tempFiles != null)
			for (File f : tempFiles)
				if (!f.isDirectory() && f.getName().toLowerCase().endsWith(makeMkvTempFileExtension.toLowerCase()))
					result.add(new FileName(f));
		return result;
	}

	private static void moveAndRename(final FileName file, final List<FileName> tempFiles) {
		if (tempFiles.size() == 0)
			return;

		long sizeSum = 0;
		FileName biggest = null;
		for (FileName f : tempFiles) {
			sizeSum += f.getSize();
			if (biggest == null || biggest.getSize() <= f.getSize())
				biggest = f;
		}

		String dString = mkvDir + file.getFolderName() + "/";
		File d = new File(dString);
		try {
			Files.createDirectory(d.toPath());
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(1);
		}

		if (!file.getEpisodesLongContents().isEmpty() || !file.getEpisodesShortContents().isEmpty()) {
			Utils.sysout("This is a TV-show.");

			if (sizeSum > file.getSize()) {
				Utils.sysout("There is more data than in the source-file. Deleting biggest one.");
				// Probably makeMKV has added a 'catch-all'-MKV file containing all data.
				try {
					if (biggest != null)
						Files.delete(biggest.getFile().toPath());
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				tempFiles.remove(biggest);

				// Update analysis data.
				sizeSum = 0;
				for (FileName f : tempFiles) {
					sizeSum += f.getSize();
					if (biggest == null || biggest.getSize() <= f.getSize())
						biggest = f;
				}
			}

			int i = 1;
			String start = "";
			if (!file.getEpisodesLongContents().isEmpty())
				try {
					start = "- s" + file.getEpisodesLongContents().get(0).getGroups().get(0) + "e";
					i = Integer.parseInt(file.getEpisodesLongContents().get(0).getGroups().get(1));
				} catch (NumberFormatException ignored) {
				}
			if (!file.getEpisodesShortContents().isEmpty())
				try {
					start = "- s" + file.getEpisodesShortContents().get(0).getGroups().get(0) + "e";
					i = Integer.parseInt(file.getEpisodesShortContents().get(0).getGroups().get(1));
				} catch (NumberFormatException ignored) {
				}
			for (FileName f : tempFiles)
				try {
					Path source = f.getFile().toPath();
					String targetString = dString + file.getFileName() + " " + start + makeTwoDigits(i) + "."
							+ mkvFileExtension;
					Path target = new File(targetString).toPath();
					Files.move(source, target);
					i++;
				} catch (IOException e) {
					e.printStackTrace();
				}
		} else {
			Utils.sysout("This is a movie.");
			// Just copy the biggest one.
			String year = "";
			if (file.getYear() != null)
				year = " (" + file.getYear() + ")";
			try {
				if (biggest != null) {
					Path source = biggest.getFile().toPath();
					String targetString = dString + file.getFileName() + year + "." + mkvFileExtension;
					Path target = new File(targetString).toPath();
					Files.move(source, target);
					tempFiles.remove(biggest);
				}
				for (FileName f : tempFiles)
					Files.delete(f.getFile().toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static String makeTwoDigits(final int i) {
		if (i < 10)
			return "0" + i;
		return i + "";
	}

	private static void makeDirOrExitOnFailure(final File tDir) {
		if (!tDir.exists())
			try {
				Files.createDirectory(tDir.toPath());
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
	}

	private static void checkTempDir() {
		File tDir = new File(tempDir);
		makeDirOrExitOnFailure(tDir);
		File[] isoFiles = tDir.listFiles();
		if (isoFiles != null)
			for (File file : isoFiles)
				if (!file.isDirectory()) {
					FileName name = new FileName(file);
					if (name.getExtension().equals(makeMkvTempFileExtension)) {
						Utils.sysout("Severe error: There already where temporary files in your temp-directory!");
						System.exit(1);
					}
				}
	}

	private static boolean doCommand(final String command) {
		ConsoleLogDevice outputLog = new ConsoleLogDevice();
		outputLog.setDebugMode(mode.contains("debug"));

		ConsoleLogDevice errorLog = new ConsoleLogDevice();
		errorLog.setDebugMode(mode.contains("debug"));

		SysCommandExecutor cmdExecutor = new SysCommandExecutor(outputLog, errorLog);
		int exitStatus = 0;
		try {
			exitStatus = cmdExecutor.runCommand(command, os.equals("linux"));
		} catch (Exception e) {
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
		if (!cmdOutput.equals("")) {
			cmdOutput = "  " + cmdOutput.trim().replace("\n", "\n  ").trim();
			o = Utils.getPattern(cmdOutput, regExMakeMkvFailedTracks, 0);
		}

		Utils.sysout("  exitValue: " + exitStatus + "");
		return !testFailed(e) && !testFailed(o);
	}

	private static boolean testFailed(final List<Match> input) {
		if (input != null && !input.isEmpty() && !input.get(0).getGroups().isEmpty()) {
			String t = input.get(0).getGroups().get(0);
			try {
				int i = Integer.parseInt(t);
				if (i > 0)
					return true;
			} catch (NumberFormatException ignored) {
			}
		}
		return false;
	}
}