package bulkmakemkv;

import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import bulkmakemkv.syscommandexecutor.SysCommandExecutor;

public class BulkMakeMkv {

	private static Configuration config;

	public static void main(String[] args) {
		try {
			config = new PropertiesConfiguration(new File("config.properties"));
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}

		System.out.println(config.getBoolean("isEnabled"));
		System.out.println(config.getString("sourceDir"));
		System.out.println(config.getString("tempDir"));
		System.out.println(config.getString("destDir"));

		SysCommandExecutor cmdExecutor = new SysCommandExecutor();
		int exitStatus = 0;
		try {
			exitStatus = cmdExecutor.runCommand(config.getString("command"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		String cmdError = cmdExecutor.getCommandError();
		String cmdOutput = cmdExecutor.getCommandOutput();
		
		System.out.println(exitStatus);
		System.out.println(cmdOutput);
		System.out.println(cmdError);
	}
}
