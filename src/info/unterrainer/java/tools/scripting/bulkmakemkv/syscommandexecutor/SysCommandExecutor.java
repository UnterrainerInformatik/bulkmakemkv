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

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;

import info.unterrainer.java.tools.utils.NullUtils;
import lombok.experimental.ExtensionMethod;

/**
 * Usage of following class can go as ... <br>
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
 * OR <br>
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

@ExtensionMethod({ NullUtils.class })
public class SysCommandExecutor {
	@Nullable
	private LogDevice fOuputLogDevice;
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

	public SysCommandExecutor(@Nullable final LogDevice fOuputLogDevice, @Nullable final LogDevice fErrorLogDevice) {
		super();
		this.fOuputLogDevice = fOuputLogDevice;
		this.fErrorLogDevice = fErrorLogDevice;
	}

	public void setOutputLogDevice(final LogDevice logDevice) {
		fOuputLogDevice = logDevice;
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

		fEnvironmentVarList.noNull().add(new EnvironmentVar(name, value));
	}

	public String getCommandOutput() {
		if (fCmdOutput == null)
			return StringUtils.EMPTY;
		return fCmdOutput.noNull().toString();
	}

	public String getCommandError() {
		if (fCmdError == null)
			return StringUtils.EMPTY;
		return fCmdError.noNull().toString();
	}

	public int runCommand(final String commandLine) throws Exception {
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

	private Process runCommandHelper(final String commandLine) throws IOException {
		Process process = null;
		if (fWorkingDirectory == null)
			process = Runtime.getRuntime().exec(commandLine, getEnvTokens());
		else
			process = Runtime.getRuntime().exec(commandLine, getEnvTokens(), new File(fWorkingDirectory));

		return process;
	}

	private void startOutputAndErrorReadThreads(final InputStream processOut, final InputStream processErr) {
		fCmdOutput = new StringBuffer();
		fCmdOutputThread = new AsyncStreamReader(processOut, fCmdOutput.noNull(), fOuputLogDevice, "OUTPUT");
		fCmdOutputThread.start();

		fCmdError = new StringBuffer();
		fCmdErrorThread = new AsyncStreamReader(processErr, fCmdError.noNull(), fErrorLogDevice, "ERROR");
		fCmdErrorThread.start();
	}

	private void notifyOutputAndErrorReadThreadsToStopReading() {
		fCmdOutputThread.noNull().stopReading();
		fCmdErrorThread.noNull().stopReading();
	}

	@Nullable
	private String[] getEnvTokens() {
		if (fEnvironmentVarList == null)
			return new String[0];

		String[] envTokenArray = new String[fEnvironmentVarList.noNull().size()];
		Iterator<EnvironmentVar> envVarIter = fEnvironmentVarList.noNull().iterator();
		int nEnvVarIndex = 0;
		while (envVarIter.hasNext() == true) {
			EnvironmentVar envVar = envVarIter.next();
			String envVarToken = envVar.fName + "=" + envVar.fValue;
			envTokenArray[nEnvVarIndex++] = envVarToken;
		}

		return envTokenArray;
	}
}