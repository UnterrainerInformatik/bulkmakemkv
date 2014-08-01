package bulkmakemkv;

public class Tools {

	public static String normalizeDirectory(String input) {
		String result = input.replace('\\', '/');
		return (result.endsWith("/")) ? result : result + "/";
	}
}
