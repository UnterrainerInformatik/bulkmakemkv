package info.unterrainer.bulkmakemkv.syscommandexecutor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import lombok.Getter;
import lombok.Setter;

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

	public AsyncStreamReader(final InputStream inputStream, final StringBuffer buffer, final LogDevice logDevice,
			final String threadId) {
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
			if (debug)
				ex.printStackTrace();
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
		if (debug)
			System.out.println("END OF: " + threadId);
	}

	public void stopReading() {
		stop = true;
	}

	private void printToDisplayDevice(final String line) {
		logDevice.log(line);
	}
}