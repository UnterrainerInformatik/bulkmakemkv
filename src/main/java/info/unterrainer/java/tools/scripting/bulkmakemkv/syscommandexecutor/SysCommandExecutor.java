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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import info.unterrainer.java.tools.utils.NullUtils;

/**
 * Usage of following class can go as ...
 *
 * <PRE>
 * <CODE>
 * 		SysCommandExecutor cmdExecutor = new SysCommandExecutor();
 * 		cmdExecutor.setOutputLogDevice(new LogDevice());
 * 		cmdExecutor.setErrorLogDevice(new LogDevice());
 * 		int exitStatus = cmdExecutor.runCommand(commandLine);
 * </CODE>
 * </PRE>
 *
 * OR
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
 */

public class SysCommandExecutor {

	private LogDevice fOutputLogDevice;

	private LogDevice fErrorLogDevice;

	private String fWorkingDirectory;

	private List<EnvironmentVar> fEnvironmentVarList;

	private StringBuffer fCmdOutput;

	private StringBuffer fCmdError;

	private AsyncStreamReader fCmdOutputThread;

	private AsyncStreamReader fCmdErrorThread;

	public SysCommandExecutor(final LogDevice fOutputLogDevice, final LogDevice fErrorLogDevice) {
		super();
		this.fOutputLogDevice = fOutputLogDevice;
		this.fErrorLogDevice = fErrorLogDevice;
	}

	public void setOutputLogDevice(final LogDevice logDevice) {
		fOutputLogDevice = logDevice;
	}

	public void setErrorLogDevice(final LogDevice logDevice) {
		fErrorLogDevice = logDevice;
	}

	public void setWorkingDirectory(final String workingDirectory) {
		fWorkingDirectory = workingDirectory;
	}

	public void setEnvironmentVar(final String name, final String value) {
		if (fEnvironmentVarList == null)
			fEnvironmentVarList = new ArrayList<>();

		NullUtils.noNull(fEnvironmentVarList).add(EnvironmentVar.builder().name(name).value(value).build());
	}

	public String getCommandOutput() {
		if (fCmdOutput == null)
			return StringUtils.EMPTY;
		return NullUtils.noNull(fCmdOutput).toString();
	}

	public String getCommandError() {
		if (fCmdError == null)
			return StringUtils.EMPTY;
		return NullUtils.noNull(fCmdError).toString();
	}

	public int runCommand(final String commandLine, final boolean isLinux) throws Exception {
		/* run command */
		Process process = runCommandHelper(commandLine, isLinux);

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

	private Process runCommandHelper(final String commandLine, final boolean isLinux) throws IOException {
		Process process;
		if (fWorkingDirectory == null)
			if (isLinux)
				process = Runtime.getRuntime().exec(new String[] { "bash", "-c", commandLine }, getEnvTokens());
			else
				process = Runtime.getRuntime().exec(commandLine, getEnvTokens());
		else if (isLinux)
			process = Runtime.getRuntime()
					.exec(new String[] { "bash", "-c", commandLine }, getEnvTokens(), new File(fWorkingDirectory));
		else
			process = Runtime.getRuntime().exec(commandLine, getEnvTokens(), new File(fWorkingDirectory));

		return process;
	}

	private void startOutputAndErrorReadThreads(final InputStream processOut, final InputStream processErr) {
		fCmdOutput = new StringBuffer();
		fCmdOutputThread = new AsyncStreamReader(processOut, NullUtils.noNull(fCmdOutput),
				NullUtils.orNoNull(fOutputLogDevice, new ConsoleLogDevice()), "OUTPUT");
		fCmdOutputThread.setDebug(true);
		fCmdOutputThread.start();

		fCmdError = new StringBuffer();
		fCmdErrorThread = new AsyncStreamReader(processErr, NullUtils.noNull(fCmdError),
				NullUtils.orNoNull(fErrorLogDevice, new ConsoleLogDevice()), "ERROR");
		fCmdErrorThread.setDebug(true);
		fCmdErrorThread.start();
	}

	private void notifyOutputAndErrorReadThreadsToStopReading() {
		NullUtils.noNull(fCmdOutputThread).stopReading();
		NullUtils.noNull(fCmdErrorThread).stopReading();
	}

	private String[] getEnvTokens() {
		if (fEnvironmentVarList == null)
			return new String[0];

		String[] envTokenArray = new String[NullUtils.noNull(fEnvironmentVarList).size()];
		Iterator<EnvironmentVar> envVarIter = NullUtils.noNull(fEnvironmentVarList).iterator();
		int nEnvVarIndex = 0;
		while (envVarIter.hasNext()) {
			EnvironmentVar envVar = envVarIter.next();
			String envVarToken = envVar.name() + "=" + envVar.value();
			envTokenArray[nEnvVarIndex++] = envVarToken;
		}

		return envTokenArray;
	}
}