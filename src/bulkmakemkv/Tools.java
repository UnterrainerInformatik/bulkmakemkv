package bulkmakemkv;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tools {

	public static String normalizeDirectory(String input) {
		if (input == null || input.equals("")) {
			return input;
		}

		String result = input.replace('\\', '/');
		return (result.endsWith("/")) ? result : result + "/";
	}

	public static String removeDashTrim(String text) {
		if (text == null || text.equals("")) {
			return text;
		}

		String result = text;
		result = result.trim();
		while (result.endsWith("-")) {
			result = result.substring(0, result.length() - 1).trim();
		}
		return result;
	}

	public static List<String> getPattern(String name, String pattern, int cut) {
		List<String> result = new ArrayList<String>();

		Pattern regex = Pattern.compile(pattern);
		Matcher matcher = regex.matcher(name);
		while (matcher.find()) {
			result.add(matcher.group().substring(cut, matcher.group().length() - cut).trim());
		}

		return result;
	}

	public static EpisodeNumber scanEpisodeNumber(String input) {
		List<String> episodesLongContents = Tools.getPattern(input, FileName.regExEpisodesLong, 0);
		List<String> episodesShortContents = Tools.getPattern(input, FileName.regExEpisodesShort, 0);

		int start = 0;
		int end = 0;
		boolean ok = false;
		if (!episodesLongContents.isEmpty()) {
			try {
				start = Integer.parseInt(episodesLongContents.get(0).substring(4, 6));
				end = Integer.parseInt(episodesLongContents.get(0).substring(8, 10));
				ok = true;
			}
			catch (NumberFormatException e) {
			}
		}
		if (!ok && !episodesShortContents.isEmpty()) {
			try {
				start = Integer.parseInt(episodesShortContents.get(0).substring(4, 6));
				end = start;
				ok = true;
			}
			catch (NumberFormatException e) {
			}
		}
		if (!ok) {
			return null;
		}
		return new EpisodeNumber(start, end, end - start + 1);
	}
}
