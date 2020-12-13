package info.unterrainer.bulkmakemkv.filevisitors;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.List;

import info.unterrainer.bulkmakemkv.EpisodeNumber;
import info.unterrainer.bulkmakemkv.Utils;

public class ScanVisitor extends SimpleFileVisitor<Path> {

	private List<String> emptyDirectories = new ArrayList<>();
	private List<String> emptyFiles = new ArrayList<>();
	private List<String> wrongNumberOfEpisodes = new ArrayList<>();
	private String mkvFileExtension;

	public ScanVisitor(final String mkvFileExtension) {
		this.mkvFileExtension = mkvFileExtension.toLowerCase();
	}

	@Override
	public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) {
		File[] files = dir.toFile().listFiles();
		if (files == null || files.length == 0)
			emptyDirectories.add(dir.toString());
		else {
			EpisodeNumber ep = Utils.scanEpisodeNumber(dir.getFileName().toString());
			if (ep != null) {
				int count = 0;
				for (File file : files) {
					EpisodeNumber fep = Utils.scanEpisodeNumber(file.getName());
					if (fep != null)
						count += fep.getCount();
				}
				if (count != ep.getCount())
					wrongNumberOfEpisodes.add(dir.toString());
			}
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(final Path file, final java.nio.file.attribute.BasicFileAttributes attrs)
			throws IOException {
		if (Files.size(file) == 0) {
			String extension = file.toFile()
					.getName()
					.substring(file.toFile().getName().lastIndexOf('.') + 1)
					.toLowerCase();
			if (extension.equals(mkvFileExtension))
				emptyFiles.add(file.toString());
		}
		return FileVisitResult.CONTINUE;
	}

	// If there is some error accessing the file, let the user know. If you don't
	// override this method and an error
	// occurs, an IOException is thrown.
	@Override
	public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
		exc.printStackTrace(System.err);
		return FileVisitResult.CONTINUE;
	}

	public List<String> getEmptyDirectories() {
		return emptyDirectories;
	}

	public List<String> getEmptyFiles() {
		return emptyFiles;
	}

	public List<String> getWrongNumberOfEpisodes() {
		return wrongNumberOfEpisodes;
	}
}
