package info.unterrainer.bulkmakemkv.filevisitors;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.List;

import info.unterrainer.bulkmakemkv.Utils;

public class DirectoryNameEqualsVisitor extends SimpleFileVisitor<Path> {

	private List<Path> cache = new ArrayList<>();
	private List<String> result = new ArrayList<>();
	private String dirName;

	public DirectoryNameEqualsVisitor(final String dn) {
		dirName = Utils.normalizeDirectory(dn);
		if (dirName != null)
			dirName = dirName.replace("/", "\\");
	}

	@Override
	public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) {
		cache.add(dir);
		String curr = Utils.normalizeDirectory(dir.getFileName().toString());
		if (curr != null) {
			curr = curr.replace("/", "\\");
			if (dirName.toLowerCase().equals(curr.toLowerCase()))
				result.add(dir.toString());
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

	public List<String> getResult() {
		return result;
	}

	public List<Path> getCache() {
		return cache;
	}
}
