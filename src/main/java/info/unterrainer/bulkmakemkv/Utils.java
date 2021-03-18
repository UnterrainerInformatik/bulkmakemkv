package info.unterrainer.bulkmakemkv;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Utils {

	public static String normalizeDirectory(final String input) {
		if (input == null || input.equals(""))
			return input;
		return input.endsWith("/") ? input : input + "/";
	}

	public static String removeDashTrim(final String text) {
		if (text == null || text.equals(""))
			return text;

		String result = text;
		result = result.trim();
		while (result.endsWith("-"))
			result = result.substring(0, result.length() - 1).trim();
		return result;
	}

	public static List<Match> getPattern(final String name, final String pattern, final int cut) {
		List<Match> result = new ArrayList<>();

		Pattern regex = Pattern.compile(pattern);
		Matcher matcher = regex.matcher(name);
		while (matcher.find()) {
			String match = matcher.group().substring(cut, matcher.group().length() - cut).trim();
			List<String> groups = new ArrayList<>();
			for (int i = 0; i < matcher.groupCount(); i++)
				groups.add(matcher.group(i + 1));
			result.add(new Match(match, groups));
		}

		return result;
	}

	public static EpisodeNumber scanEpisodeNumber(final String input) {
		List<Match> episodesLongContents = Utils.getPattern(input, FileName.regExEpisodesLong, 0);
		List<Match> episodesShortContents = Utils.getPattern(input, FileName.regExEpisodesShort, 0);

		int start = 0;
		int end = 0;
		boolean ok = false;
		if (!episodesLongContents.isEmpty())
			try {
				start = Integer.parseInt(episodesLongContents.get(0).getMatch().substring(4, 6));
				end = Integer.parseInt(episodesLongContents.get(0).getMatch().substring(8, 10));
				ok = true;
			} catch (NumberFormatException ignored) {
			}
		if (!ok && !episodesShortContents.isEmpty())
			try {
				start = Integer.parseInt(episodesShortContents.get(0).getMatch().substring(4, 6));
				end = start;
				ok = true;
			} catch (NumberFormatException ignored) {
			}
		if (!ok)
			return null;
		return new EpisodeNumber(start, end, end - start + 1);
	}

	public static void sysoutNN(final String input) {
		if (input != null)
			sysout(input);
	}

	public static void sysoutNNNE(final String input) {
		if (input != null && !input.equals(""))
			sysout(input);
	}

	public static void sysout(final String input) {
		System.out.println(input);
	}

	public static void sysout(final String[] input) {
		for (int i = 0; i < input.length; i++) {
			if (i > 0)
				System.out.print(" ");
			System.out.print(input[i]);
		}
		System.out.println();
	}

	public static void sysout() {
		System.out.println();
	}
}
