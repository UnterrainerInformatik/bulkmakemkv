package bulkmakemkv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import bulkmakemkv.syscommandexecutor.SysCommandExecutor;

public class BulkMakeMkv {

	private static Configuration	config;

	private static String			isoDir;
	private static String			tempDir;
	private static String			mkvDir;
	private static List<String>		observeMkvDirs;

	private static boolean			convertShows;
	private static boolean			convertMovies;

	private static String			isoFileExtension;
	private static String			mkvFileExtension;

	private static String			makeMkvCommand;
	private static String			makeMkvTempFileExtension;

	public static void main(String[] args) {
		try {
			config = new PropertiesConfiguration(new File("config.properties"));
		}
		catch (ConfigurationException e) {
			e.printStackTrace();
		}

		isoDir = Tools.normalizeDirectory(config.getString("isoDir"));
		tempDir = Tools.normalizeDirectory(config.getString("tempDir"));
		mkvDir = Tools.normalizeDirectory(config.getString("mkvDir"));

		String[] t = config.getStringArray("observeMkvDirs");
		observeMkvDirs = new ArrayList<String>();
		observeMkvDirs.add(mkvDir);
		if (t != null) {
			for (String s : t) {
				if (s != null && !s.equals("")) {
					observeMkvDirs.add(Tools.normalizeDirectory(s));
				}
			}
		}

		isoFileExtension = config.getString("isoFileExtension");
		mkvFileExtension = config.getString("mkvFileExtension");

		convertShows = config.getBoolean("convertShows");
		convertMovies = config.getBoolean("convertMovies");

		makeMkvCommand = config.getString("makeMkvCommand");
		makeMkvTempFileExtension = config.getString("makeMkvTempFileExtension");

		checkTempDir();
		checkMkvDir();

		File iso = new File(isoDir);
		if (!iso.exists()) {
			System.out.println("The isoDir you specified [" + iso.toString() + "] doesn't exist.");
			System.exit(1);
		}
		File[] isoFiles = iso.listFiles();

		for (File file : isoFiles) {
			if (!file.isDirectory()) {
				FileName name = new FileName(file);
				if (name.getExtension().toLowerCase().equals("iso")) {
					if (name.isBonusDisc()) {
						System.out.println("skipping: " + name.getName() + " (is bonus disc)");
						continue;
					}
					List<String> p = exists(name);
					if (!p.isEmpty()) {
						String out = "skipping: " + name.getName() + ". Already exists in... ";
						for (String s : p) {
							out += "\n    " + s;
						}
						System.out.println(out);
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
					System.out.println("converting: " + name.getName());
					convert(name);
					List<FileName> tempFiles = scanTempDirectory();
					moveAndRename(name, tempFiles);
				}
				else {
					System.out.println("skipping: "
							+ name.getName()
							+ " (not an ."
							+ isoFileExtension.toLowerCase()
							+ " file)");
				}
			}
		}
	}

	private static List<String> exists(FileName file) {
		List<String> result = new ArrayList<String>();
		for (String s : observeMkvDirs) {
			DirectoryNameEqualsVisitor v = new DirectoryNameEqualsVisitor(file.getFolderName());
			try {
				Files.walkFileTree(new File(s).toPath(), v);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			result.addAll(v.getResult());
		}
		return result;
	}

	private static void convert(FileName file) {
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
		System.out.println(command);
		doCommand(command);
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
			System.out.println("This is a TV-show.");

			if (sizeSum > file.getSize()) {
				System.out.println("There is more data than in the source-file. Deleting biggest one.");
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
					start = "- " + file.getEpisodesLongContents().get(0).substring(0, 4);
					i = Integer.parseInt(file.getEpisodesLongContents().get(0).substring(4, 6));
				}
				catch (NumberFormatException e) {
				}
			}
			if (!file.getEpisodesShortContents().isEmpty()) {
				try {
					start = "- " + file.getEpisodesShortContents().get(0).substring(0, 4);
					i = Integer.parseInt(file.getEpisodesShortContents().get(0).substring(4, 6));
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
			System.out.println("This is a movie.");
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
					System.out.println("Severe error: There already where temporary files in your temp-directory!");
					System.exit(1);
				}
			}
		}
	}

	private static void doCommand(String command) {
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

		System.out.println(exitStatus);
		System.out.println(cmdOutput);
		System.out.println(cmdError);
	}
}
