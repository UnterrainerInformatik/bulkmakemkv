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

import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class AsyncStreamReader extends Thread {
	private StringBuffer buffer;
	private InputStream inputStream;
	private boolean stop;
	private LogDevice logDevice;
	private String threadId;

	@Getter
	@Setter
	private boolean debug;

	private String newLine;

	public AsyncStreamReader(InputStream inputStream, StringBuffer buffer, LogDevice logDevice, String threadId) {
		this.inputStream = inputStream;
		this.buffer = buffer;
		this.logDevice = logDevice;
		this.threadId = threadId;

		newLine = System.getProperty("line.separator");
	}

	public String getBuffer() {
		return buffer.toString();
	}

	@Override
	public void run() {
		try {
			readCommandOutput();
		} catch (Exception ex) {
			if(debug){
				ex.printStackTrace();
			}
		}
	}

	private void readCommandOutput() throws IOException {
		BufferedReader bufOut = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		while (!stop && (line = bufOut.readLine()) != null) {
			buffer.append(line).append(newLine);
			printToDisplayDevice(line);
		}
		bufOut.close();
		if(debug){
			System.out.println("END OF: " + threadId);
		}
	}

	public void stopReading() {
		stop = true;
	}

	private void printToDisplayDevice(String line) {
		logDevice.log(line);
	}
}