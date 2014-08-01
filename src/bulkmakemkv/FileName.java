package bulkmakemkv;

import java.io.File;

public class FileName {

	private final String	regExRemoveParenthesis	= "\\([^(]*?\\)";
	private final String	regExRemoveBrackets		= "\\[[^\\]]*?\\]";

	private String			name;
	private String			extension;

	private String			nameWithoutBrackets;
	private String			nameWithoutParenthesis;
	private String			nameWithoutBracketsAndParenthesis;

	public FileName(String name, String extension) {
		super();
		this.name = name;
		this.extension = extension;
		calculate(name);
	}

	public static FileName create(File file) {
		String name = file.getName().substring(0, file.getName().lastIndexOf('.'));
		String extension = file.getName().substring(file.getName().lastIndexOf('.') + 1).toLowerCase();
		return new FileName(name, extension);
	}

	private void calculate(String name) {
		nameWithoutBrackets = name.replaceAll(regExRemoveBrackets, "");
		nameWithoutParenthesis = name.replace(regExRemoveParenthesis, "");
		nameWithoutBracketsAndParenthesis = nameWithoutBrackets.replace(regExRemoveParenthesis, "");
	}

	@Override
	public String toString() {
		return name + "." + extension;
	}

	public String getName() {
		return name;
	}

	public String getExtension() {
		return extension;
	}

	public String getNameWithoutBrackets() {
		return nameWithoutBrackets;
	}

	public String getNameWithoutParenthesis() {
		return nameWithoutParenthesis;
	}

	public String getNameWithoutBracketsAndParenthesis() {
		return nameWithoutBracketsAndParenthesis;
	}
}
