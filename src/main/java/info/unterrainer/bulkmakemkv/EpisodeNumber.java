package info.unterrainer.bulkmakemkv;

public class EpisodeNumber {
	private int start;
	private int end;
	private int count;

	public EpisodeNumber(final int start, final int end, final int count) {
		super();
		this.start = start;
		this.end = end;
		this.count = count;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public int getCount() {
		return count;
	}
}
