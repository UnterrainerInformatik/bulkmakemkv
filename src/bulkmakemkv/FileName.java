package bulkmakemkv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileName {

	private final String	regExRoundBrackets		= "\\([^(]*?\\)";
	private final String	regExSquareBrackets		= "\\[[^\\]]*?\\]";
	private final String	regExEpisodesLong		= "s\\d\\de\\d\\d-e\\d\\d";
	private final String	regExEpisodesShort		= "s\\d\\de\\d\\d";

	private File			file;
	private long			size;
	private String			name;
	private String			extension;
	private String			calculatedCleanName;

	private boolean			bonusDisc;
	private Integer			year;

	private List<String>	roundBracketContents	= new ArrayList<String>();
	private List<String>	squareBracketContents	= new ArrayList<String>();
	private List<String>	episodesLongContents	= new ArrayList<String>();
	private List<String>	episodesShortContents	= new ArrayList<String>();

	public FileName(File file) {
		name = file.getName().substring(0, file.getName().lastIndexOf('.'));
		extension = file.getName().substring(file.getName().lastIndexOf('.') + 1).toLowerCase();
		try {
			size = Files.size(file.toPath());
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		this.file = file;
		calculate(name);
	}

	private void calculate(String name) {
		squareBracketContents = getPattern(name, regExSquareBrackets, 1);
		roundBracketContents = getPattern(name, regExRoundBrackets, 1);
		calculatedCleanName = name.replaceAll(regExSquareBrackets, "");
		calculatedCleanName = calculatedCleanName.replaceAll(regExRoundBrackets, "");
		calculatedCleanName = calculatedCleanName.trim();

		episodesLongContents = getPattern(calculatedCleanName, regExEpisodesLong, 0);
		calculatedCleanName = calculatedCleanName.replaceAll(regExEpisodesLong, "");
		calculatedCleanName = Tools.removeDashTrim(calculatedCleanName);

		episodesShortContents = getPattern(calculatedCleanName, regExEpisodesShort, 0);
		calculatedCleanName = calculatedCleanName.replaceAll(regExEpisodesShort, "");
		calculatedCleanName = Tools.removeDashTrim(calculatedCleanName);

		Calendar now = Calendar.getInstance();
		for (String s : roundBracketContents) {
			if (s.toLowerCase().equals("bonus")) {
				bonusDisc = true;
			}
			try {
				year = Integer.parseInt(s);
				if (year < 1800 || year > now.get(Calendar.YEAR)) {
					// Plausibility-check.
					year = null;
				}
			}
			catch (NumberFormatException e) {
			}
		}

		if (calculatedCleanName.toLowerCase().endsWith("bonus")) {
			bonusDisc = true;
		}
	}

	private List<String> getPattern(String name, String pattern, int cut) {
		List<String> result = new ArrayList<String>();

		Pattern regex = Pattern.compile(pattern);
		Matcher matcher = regex.matcher(name);
		while (matcher.find()) {
			result.add(matcher.group().substring(cut, matcher.group().length() - cut).trim());
		}

		return result;
	}

	@Override
	public String toString() {
		return calculatedCleanName
				+ " ex:["
				+ extension
				+ "] sz:["
				+ size
				+ "]"
				+ " rb:"
				+ roundBracketContents
				+ " sb:"
				+ squareBracketContents
				+ " el:"
				+ episodesLongContents
				+ " es:"
				+ episodesShortContents
				+ " yr:["
				+ year
				+ "] bd:["
				+ bonusDisc
				+ "]";
	}

	public String getFolderName() {
		String result = calculatedCleanName;
		if (!squareBracketContents.isEmpty()) {
			result += " [" + squareBracketContents.get(0) + "]";
		}
		if (year != null) {
			result += " (" + year + ")";
		}
		if (!episodesLongContents.isEmpty() || !episodesShortContents.isEmpty()) {
			String h = " - ";
			if (!episodesLongContents.isEmpty()) {
				h += episodesLongContents.get(0);
			}
			if (!episodesShortContents.isEmpty()) {
				h += episodesShortContents.get(0);
			}
			// This is a TV-series.
			result += h;
		}
		for (String s : roundBracketContents) {
			if (s.toLowerCase().startsWith("side")) {
				result += " (" + s + ")";
			}
		}
		for (String s : roundBracketContents) {
			if (s.toLowerCase().startsWith("part")) {
				result += " (" + s + ")";
			}
		}
		return result;
	}

	public String getFileName() {
		String result = calculatedCleanName;
		for (String s : roundBracketContents) {
			if (s.toLowerCase().startsWith("side")) {
				result += " (" + s + ")";
			}
		}
		for (String s : roundBracketContents) {
			if (s.toLowerCase().startsWith("part")) {
				result += " (" + s + ")";
			}
		}
		return result;
	}

	public File getFile() {
		return file;
	}

	public long getSize() {
		return size;
	}

	public String getName() {
		return name;
	}

	public String getExtension() {
		return extension;
	}

	public String getCalculatedCleanName() {
		return calculatedCleanName;
	}

	public List<String> getRoundBracketContents() {
		return roundBracketContents;
	}

	public List<String> getSquareBracketContents() {
		return squareBracketContents;
	}

	public List<String> getEpisodesLongContents() {
		return episodesLongContents;
	}

	public List<String> getEpisodesShortContents() {
		return episodesShortContents;
	}

	public boolean isBonusDisc() {
		return bonusDisc;
	}

	public Integer getYear() {
		return year;
	}
}
