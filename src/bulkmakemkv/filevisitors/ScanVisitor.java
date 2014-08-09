package bulkmakemkv.filevisitors;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.List;

public class ScanVisitor extends SimpleFileVisitor<Path> {

	private List<String>	emptyDirectories	= new ArrayList<String>();
	private List<String>	emptyFiles			= new ArrayList<String>();
	private String			mkvFileExtension;

	public ScanVisitor(String mkvFileExtension) {
		this.mkvFileExtension = mkvFileExtension.toLowerCase();
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
		File[] files = dir.toFile().listFiles();
		if (files == null || files.length == 0) {
			emptyDirectories.add(dir.toString());
			return FileVisitResult.CONTINUE;
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
		if (Files.size(file) == 0) {
			String extension = file.toFile().getName().substring(file.toFile().getName().lastIndexOf('.') + 1)
					.toLowerCase();
			if (extension.equals(mkvFileExtension)) {
				emptyFiles.add(file.toString());
			}
		}
		return FileVisitResult.CONTINUE;
	};

	// If there is some error accessing the file, let the user know. If you don't override this method and an error
	// occurs, an IOException is thrown.
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) {
		System.err.println(exc);
		return FileVisitResult.CONTINUE;
	}

	public List<String> getEmptyDirectories() {
		return emptyDirectories;
	}

	public List<String> getEmptyFiles() {
		return emptyFiles;
	}
}
