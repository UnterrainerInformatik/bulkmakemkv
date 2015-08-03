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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class AsyncStreamReader extends Thread {
	private StringBuffer fBuffer;
	private InputStream fInputStream;
	private boolean fStop;
	private LogDevice fLogDevice;

	private String fNewLine;

	public AsyncStreamReader(InputStream inputStream, StringBuffer buffer, LogDevice logDevice, String threadId) {
		fInputStream = inputStream;
		fBuffer = buffer;
		fLogDevice = logDevice;

		fNewLine = System.getProperty("line.separator");
	}

	public String getBuffer() {
		return fBuffer.toString();
	}

	@Override
	public void run() {
		try {
			readCommandOutput();
		} catch (Exception ex) {
			// ex.printStackTrace(); //DEBUG
		}
	}

	private void readCommandOutput() throws IOException {
		BufferedReader bufOut = new BufferedReader(new InputStreamReader(fInputStream));
		String line = null;
		while (fStop == false && (line = bufOut.readLine()) != null) {
			fBuffer.append(line + fNewLine);
			printToDisplayDevice(line);
		}
		bufOut.close();
		// System.out.println("END OF: " + fThreadId); //DEBUG
	}

	public void stopReading() {
		fStop = true;
	}

	private void printToDisplayDevice(String line) {
		fLogDevice.log(line);
	}
}