package info.unterrainer.bulkmakemkv.syscommandexecutor;

import org.apache.commons.lang.StringEscapeUtils;

import info.unterrainer.java.tools.reporting.consoleprogressbar.ConsoleProgressBar;
import info.unterrainer.java.tools.reporting.consoleprogressbar.drawablecomponents.ProgressBar;
import info.unterrainer.java.tools.utils.NullUtils;
import lombok.Getter;
import lombok.Setter;

public class ConsoleLogDevice implements LogDevice {

	@Getter
	@Setter
	private boolean debugMode;

	private double progressCurrent;
	private double progressTotal;
	private double progressMax = 65000;

	private ProgressBar barComponentTotal = ProgressBar.builder().build();
	private ConsoleProgressBar barTotal = ConsoleProgressBar.builder()
			.width(10)
			.minValue(0d)
			.maxValue(progressMax)
			.component(barComponentTotal)
			.build();

	private ProgressBar barComponentCurrent = ProgressBar.builder().build();
	private ConsoleProgressBar barCurrent = ConsoleProgressBar.builder()
			.width(10)
			.minValue(0d)
			.maxValue(progressMax)
			.component(barComponentCurrent)
			.build();

	private boolean speedHintWrittenOnce;

	private void write(final String input) {
		removeProgressBars();
		System.out.println(input);
		drawProgressBars();
	}

	@Override
	public synchronized void log(String input) {
		if (input != null) {
			input = input.trim();
			if (!debugMode)
				input = unescape(process(NullUtils.noNull(input)));

			if (input != null && input.length() > 0 && !input.trim().equals("0"))
				write("  " + input.trim().replace("\r", "").replace("\n", "\n  ").trim());
		}
	}

	private String unescape(final String input) {
		if (input == null)
			return null;

		String result = input.trim();
		result = StringEscapeUtils.unescapeJava(result.trim());
		if (result.startsWith("\""))
			result = result.substring(1);
		if (result.endsWith("\""))
			result = result.substring(0, result.length() - 1);

		return result;
	}

	private String process(final String input) {
		String command = input.substring(0, input.indexOf(':'));
		String content = input.substring(input.indexOf(':') + 1);
		String[] contents = content.split(",");

		switch (command) {
		case "PRGV":
			if (contents.length == 3) {
				try {
					progressCurrent = Double.parseDouble(contents[0]);
					progressTotal = Double.parseDouble(contents[1]);
					progressMax = Double.parseDouble(contents[2]);
				} catch (NumberFormatException e) {
					System.out.println("  Progress message had wrong format.");
				}
				updateProgressBars();
			}
			return null;
		case "PRGT":
			String t = unescape(contents[2]);
			if (t != null && t.equals("Saving all titles to MKV files"))
				t = "Saving titles";
			removeProgressBars();
			barComponentTotal.setPrefix(t + ":");
			drawProgressBars();
			return null;
		case "PRGC":
			String c = unescape(contents[2]);
			if (c != null) {
				if (c.equals("Analyzing seamless segments"))
					c = "Analyzing Segments";
				if (c.equals("Saving to MKV file"))
					c = "Saving file";
			}
			removeProgressBars();
			barComponentCurrent.setPrefix(" " + c + ":");
			drawProgressBars();
			return null;
		case "DRV":
		case "TCOUT":
		case "CINFO":
		case "TINFO":
		case "SINFO":
			return null;
		case "MSG":
			// Return message suitable for output from message-array.
			if (contents.length > 3) {
				String s = unescape(contents[3]);
				if (s != null) {
					if (s.startsWith("DEBUG:"))
						return null;
					if (s.equals("Program reads data faster than it can write to disk"))
						if (speedHintWrittenOnce)
							return null;
						else
							speedHintWrittenOnce = true;
				}
				return s;
			}
		}

		return input;
	}

	private void updateProgressBars() {

		if (!barTotal.isDrawInitialized() && !barCurrent.isDrawInitialized()) {
			barTotal.draw(System.out);
			barCurrent.draw(System.out);
		}

		barTotal.getFader().setMaximalValue(progressMax);
		barTotal.updateValue(progressTotal);

		barCurrent.getFader().setMaximalValue(progressMax);
		barCurrent.updateValue(progressCurrent);

		if (barCurrent.isRedrawNecessary() || barTotal.isRedrawNecessary()) {
			removeProgressBars();
			drawProgressBars();
		}
	}

	private void removeProgressBars() {
		barCurrent.remove(System.out);
		barTotal.remove(System.out);
	}

	private void drawProgressBars() {
		barTotal.draw(System.out);
		barCurrent.draw(System.out);
	}
}
