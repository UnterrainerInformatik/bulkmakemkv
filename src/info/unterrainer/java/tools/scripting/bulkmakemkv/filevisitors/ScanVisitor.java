/**************************************************************************
 * <pre>
 *
 * Copyright (c) Unterrainer Informatik OG.
 * This source is subject to the Microsoft Public License.
 *
 * See http://www.microsoft.com/opensource/licenses.mspx#Ms-PL.
 * All other rights reserved.
 *
 * (In other words you may copy, use, change and redistribute it without
 * any restrictions except for not suing me because it broke something.)
 *
 * THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR
 * PURPOSE.
 *
 * </pre>
 ***************************************************************************/
package info.unterrainer.java.tools.scripting.bulkmakemkv.filevisitors;

import info.unterrainer.java.tools.scripting.bulkmakemkv.EpisodeNumber;
import info.unterrainer.java.tools.scripting.bulkmakemkv.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.List;

@javax.annotation.ParametersAreNonnullByDefault({})
public class ScanVisitor extends SimpleFileVisitor<Path> {

	private List<String> emptyDirectories = new ArrayList<String>();
	private List<String> emptyFiles = new ArrayList<String>();
	private List<String> wrongNumberOfEpisodes = new ArrayList<String>();
	private String mkvFileExtension;

	public ScanVisitor(String mkvFileExtension) {
		this.mkvFileExtension = mkvFileExtension.toLowerCase();
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
		File[] files = dir.toFile().listFiles();
		if (files == null || files.length == 0) {
			emptyDirectories.add(dir.toString());
		} else {
			EpisodeNumber ep = Utils.scanEpisodeNumber(dir.getFileName().toString());
			if (ep != null) {
				int count = 0;
				for (File file : files) {
					EpisodeNumber fep = Utils.scanEpisodeNumber(file.getName());
					if (fep != null) {
						count += fep.getCount();
					}
				}
				if (count != ep.getCount()) {
					wrongNumberOfEpisodes.add(dir.toString());
				}
			}
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
		if (Files.size(file) == 0) {
			String extension = file.toFile().getName().substring(file.toFile().getName().lastIndexOf('.') + 1).toLowerCase();
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

	public List<String> getWrongNumberOfEpisodes() {
		return wrongNumberOfEpisodes;
	}
}
