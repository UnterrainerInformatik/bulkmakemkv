/**************************************************************************
 * <pre>
 *
 * Copyright (c) Unterrainer Informatik OG.
 * This source is subject to the Microsoft Public License.
 *
 * See http://www.microsoft.com/opensource/licenses.mspx#Ms-PL.
 * All other rights reserved.
 *
 * (In other words you may copy, use, change and redistribute it without
 * any restrictions except for not suing me because it broke something.)
 *
 * THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR
 * PURPOSE.
 *
 * </pre>
 ***************************************************************************/
package info.unterrainer.java.tools.scripting.bulkmakemkv.syscommandexecutor;

import info.unterrainer.java.tools.utils.NullUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Usage of following class can go as ...
 * <P>
 *
 * <PRE>
 * <CODE>
 * 		SysCommandExecutor cmdExecutor = new SysCommandExecutor();
 * 		cmdExecutor.setOutputLogDevice(new LogDevice());
 * 		cmdExecutor.setErrorLogDevice(new LogDevice());
 * 		int exitStatus = cmdExecutor.runCommand(commandLine);
 * </CODE>
 * </PRE>
 * </P>
 * OR
 * <P>
 *
 * <PRE>
 * <CODE>
 * 		SysCommandExecutor cmdExecutor = new SysCommandExecutor();
 * 		int exitStatus = cmdExecutor.runCommand(commandLine);
 *
 * 		String cmdError = cmdExecutor.getCommandError();
 * 		String cmdOutput = cmdExecutor.getCommandOutput();
 * </CODE>
 * </PRE>
 * </P>
 */

public class SysCommandExecutor {
	@Nullable
	private LogDevice fOutputLogDevice;
	@Nullable
	private LogDevice fErrorLogDevice;
	@Nullable
	private String fWorkingDirectory;
	@Nullable
	private List<EnvironmentVar> fEnvironmentVarList;

	@Nullable
	private StringBuffer fCmdOutput;
	@Nullable
	private StringBuffer fCmdError;
	@Nullable
	private AsyncStreamReader fCmdOutputThread;
	@Nullable
	private AsyncStreamReader fCmdErrorThread;

	public SysCommandExecutor(@Nullable LogDevice fOutputLogDevice, @Nullable LogDevice fErrorLogDevice) {
		super();
		this.fOutputLogDevice = fOutputLogDevice;
		this.fErrorLogDevice = fErrorLogDevice;
	}

	public void setOutputLogDevice(LogDevice logDevice) {
		fOutputLogDevice = logDevice;
	}

	public void setErrorLogDevice(LogDevice logDevice) {
		fErrorLogDevice = logDevice;
	}

	public void setWorkingDirectory(String workingDirectory) {
		fWorkingDirectory = workingDirectory;
	}

	public void setEnvironmentVar(String name, String value) {
		if (fEnvironmentVarList == null) {
			fEnvironmentVarList = new ArrayList<>();
		}

		NullUtils.noNull(fEnvironmentVarList).add(new EnvironmentVar(name, value));
	}

	public String getCommandOutput() {
		if (fCmdOutput == null) {
			return StringUtils.EMPTY;
		}
		return NullUtils.noNull(fCmdOutput).toString();
	}

	public String getCommandError() {
		if (fCmdError == null) {
			return StringUtils.EMPTY;
		}
		return NullUtils.noNull(fCmdError).toString();
	}

	public int runCommand(String commandLine) throws Exception {
		/* run command */
		Process process = runCommandHelper(commandLine);

		/* start output and error read threads */
		startOutputAndErrorReadThreads(process.getInputStream(), process.getErrorStream());

		/* wait for command execution to terminate */
		int exitStatus = -1;
		try {
			exitStatus = process.waitFor();

		} catch (Throwable ex) {
			throw new Exception(ex.getMessage());

		} finally {
			/* notify output and error read threads to stop reading */
			notifyOutputAndErrorReadThreadsToStopReading();
		}

		return exitStatus;
	}

	private Process runCommandHelper(String commandLine) throws IOException {
		Process process;
		if (fWorkingDirectory == null) {
			process = Runtime.getRuntime().exec(commandLine, getEnvTokens());
		} else {
			process = Runtime.getRuntime().exec(commandLine, getEnvTokens(), new File(fWorkingDirectory));
		}

		return process;
	}

	private void startOutputAndErrorReadThreads(InputStream processOut, InputStream processErr) {
		fCmdOutput = new StringBuffer();
		fCmdOutputThread = new AsyncStreamReader(processOut, NullUtils.noNull(fCmdOutput), NullUtils.orNoNull(fOutputLogDevice, new ConsoleLogDevice()),
				"OUTPUT");
		fCmdOutputThread.start();

		fCmdError = new StringBuffer();
		fCmdErrorThread = new AsyncStreamReader(processErr, NullUtils.noNull(fCmdError), NullUtils.orNoNull(fErrorLogDevice, new ConsoleLogDevice()), "ERROR");
		fCmdErrorThread.start();
	}

	private void notifyOutputAndErrorReadThreadsToStopReading() {
		NullUtils.noNull(fCmdOutputThread).stopReading();
		NullUtils.noNull(fCmdErrorThread).stopReading();
	}

	@Nullable
	private String[] getEnvTokens() {
		if (fEnvironmentVarList == null) {
			return new String[0];
		}

		String[] envTokenArray = new String[NullUtils.noNull(fEnvironmentVarList).size()];
		Iterator<EnvironmentVar> envVarIter = NullUtils.noNull(fEnvironmentVarList).iterator();
		int nEnvVarIndex = 0;
		while (envVarIter.hasNext()) {
			EnvironmentVar envVar = envVarIter.next();
			String envVarToken = envVar.fName + "=" + envVar.fValue;
			envTokenArray[nEnvVarIndex++] = envVarToken;
		}

		return envTokenArray;
	}
}