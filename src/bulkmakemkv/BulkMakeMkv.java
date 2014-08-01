package bulkmakemkv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import bulkmakemkv.syscommandexecutor.SysCommandExecutor;

public class BulkMakeMkv {

	private static Configuration	config;

	private static String			isoDir;
	private static String			tempDir;
	private static String			mkvDir;

	private static String			makeMkvCommand;
	private static String			makeMkvTempFileStart;
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

		makeMkvCommand = config.getString("makeMkvCommand");
		makeMkvTempFileStart = config.getString("makeMkvTempFileStart");
		makeMkvTempFileExtension = config.getString("makeMkvTempFileExtension");

		checkTempDir();

		File[] isoFiles = new File(isoDir).listFiles();
		for (File file : isoFiles) {
			if (!file.isDirectory()) {
				FileName name = FileName.create(file);
				System.out.println(name);
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
				FileName name = FileName.create(file);
				if (name.getExtension().equals(makeMkvTempFileExtension)
						&& name.getName().startsWith(makeMkvTempFileStart)) {
					System.out.println("Severe error: There already where temporary files in your temp-directory!");
					System.exit(1);
				}
			}
		}
	}

	private static void doIt(String command) {
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
