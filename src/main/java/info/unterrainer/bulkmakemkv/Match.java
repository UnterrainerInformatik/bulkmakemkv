package info.unterrainer.bulkmakemkv;

import java.util.ArrayList;
import java.util.List;

public class Match {
	String match;
	List<String> groups = new ArrayList<>();

	public Match(final String match, final List<String> groups) {
		super();
		this.match = match;
		this.groups = groups;
	}

	public String getMatch() {
		return match;
	}

	public List<String> getGroups() {
		return groups;
	}

	@Override
	public String toString() {
		return match;
	}
}
