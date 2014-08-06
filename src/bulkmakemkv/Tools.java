package bulkmakemkv;

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
}
