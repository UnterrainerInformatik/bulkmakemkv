```
/**************************************************************************
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
 ***************************************************************************/
```
[![Travis branch](https://img.shields.io/travis/UnterrainerInformatik/bulkmakemkv/develop.svg?maxAge=2592000)]()


# Disclaimer

This section of our GIT repository is free. You may copy, use or rewrite every single one of its contained projects to your hearts content.
In order to get help with basic GIT commands you may try [the GIT cheat-sheet][coding] on our [homepage][homepage].  

This repository is private since this is the master- and release-branch. You may clone it, but it will be read-only.  
If you want to contribute to our repository (push, open pull requests), please use the copy on github located here: [the public github repository][github]

All of our projects facilitate the [Project Lombok][lombok]. So please download it and 'install' it in your preferred IDE by clicking on the downloaded jar-file. Then all compile-errors should vanish.  

**Beware:** Project Lombok currently doesn't play well with Eclipse Mars (4.5). You'll get build-errors using the extension methods as of now.

# [![BulkMakeMKV Icon](http://unterrainer.info/logos/bulkmakemkv128.png)] BulkMakeMKV

MakeMKV is a program that extracts MKV-files from DVD/BR-rips that are saved as an image (ISO) or a DVD/BR in general (a mounted one, preferably).

You may get it from here [makeMKV][makemkv]. You will need it in order to use this tool.  

The program BulkMakeMKV eases the task of bulk-converting those rips from various directories into various other directories. It checks if those MKV-files exist for existing rips, converting only missing ones on startup.  
The program is used by starting the jar-file (located in the `target` directory of the project). The program is configurated using the file `config.properties` which it will expect to find next to itself (the same directory or on the path).

First, you'll need the Java **JRE version 1.8** (**Java SE 8**) or higher.
Make sure that java.exe is on your path, or start the following commands with the path to your java installation (the `/bin` directory of the one you've just downloaded).

Start the program, as you would start any jar by typing:
```
java -jar bulk-makemkv-0.2-jar-with-dependencies.jar
```
or put these lines in a batch-file (should be more convenient):
```
java -jar bulk-makemkv-0.2-jar-with-dependencies.jar
pause
```

## The Config File  

Here is an example of a config-file. I like to think it is very self-descriptive:

``` properties
# Here you can specify how the program behaves. We have several modes
# at your disposal which are described in detail below.
#
# Currently supported modes are:
#  scan            Don't do a conversion but only scan target-directories
#    			for peculiar things (zero-size files, empty directories,
#				etc...) and display a summary at the end.
#  convert		Do a real conversion of all files and scan afterwards.
#
# You may add the word 'debug' to any of these modes in order to get the full
# output of the makeMKV tool when it is called to convert. The order of the
# words doesn't matter.
mode = scan
#mode = convert
#mode = debug scan
#mode = convert debug

# This option manipulates escaping when it comes to paths. Use windows or mac.
os = windows

# The directories where your ISO-files are located.
#
# IMPORTANT FOR ALL PATH-SPECIFICATIONS:
# Always use forward slashes ('/') like in the examples below!
# Don't ever use backslashes ('\') since there are multiple issues with the
# JVM on different systems (non-windows) causing the command-line calls
# to fail!
#
# If you want to specify more than one, just add another isoDirs-parameter
# (same name) below the current one.
# try:
# isoDirs = /path to dir
# for absolute paths on linux based systems like mac.
isoDirs = //computername/f$

# This is the temporary directory which is used when makeMKV is working.
# Be sure it doesn't contain files named "title<NN>.mkv" (the program 
# checks for any mkv-files starting with "title") since that would collide 
# with the files makeMKV wants to make.
# Other that that you may chose any directory.
# It stands to reason for this directory to be on the same drive as the
# mkvDir, since the copy-process won't involve any real data-transfer this
# way.
tempDir = //computername/Movies1/temp

# This is the directory where the program will create the right directories 
# in containing the resulting MKV-files.
mkvDir = d:/temp/VIDEO/mkv

# This is a list of directories the program will look for already converted
# MKV-files in. The directories will be searched recursively.
# The mkvDir is in this list by default. No need to add it manually.
#
# If you want to specify more than one, just add another observeMkvDirs-
# parameter (same name) below the current one.
observeMkvDirs = //computername/Movies1/SERIES
observeMkvDirs = //computername/Movies1/SERIES_18
observeMkvDirs = //computername/Movies1/MOVIES
observeMkvDirs = //computername/Movies1/MOVIES_18
observeMkvDirs = //computername/Movies2/SERIES
observeMkvDirs = //computername/Movies2/SERIES_18
observeMkvDirs = //computername/Movies2/MOVIES
observeMkvDirs = //computername/Movies2/MOVIES_18

# If set to true, then all series are converted. If not present or set to
# false, all series are omitted.
convertShows = true

# If set to true, then all movies are converted. If not present or set to
# false, all movies are omitted.
convertMovies = true

# The location of your local makeMKV-installation. Be sure to contain the 
# name of the runtime as well, not only the directory.
makeMkvCommand = D:/Program Files (x86)/MakeMKV/makemkvcon.exe

makeMkvTempFileExtension = mkv
mkvFileExtension = mkv
isoFileExtension = iso
```

## The Naming Conventions For Source-Files  

In order to use several functions of this converter you need to honor a naming-style in your ISO source files.  

Basically it strips everything within braces (round or square) with a few exceptions. These are (always in round braces. *EVERYTHING* within square braces gets removed):

 - `side <c>`  
   Where `<c>` is a character like in `side A`.  
 - `part <n>`
   Where `<n>` is a positive integer like in `part 1`.
 - `<year>`  
   Like in `1967`.
 - `english`
 - `german`

### Real-Life Examples  

#### Movies/TV-Series
The script doesn't convert bonus-discs. A bonus disc is identified by a trailing `bonus` or `Bonus` at the end of the regular file-name. When the script encounters that it displays a proper message and skipps the file. Just as it would do when the encountered file-name results in a folder-name that already exists.

*ISO-file name:* (normal DVD/BR)  
`A Crime [A Crime - Spaete Rache] (o).iso`  
*Resulting target-folder / file-name:*  
`/A Crime [A Crime - Spaete Rache]/A Crime.mkv`  

*ISO-file name:* (normal DVD/BR bonus disc)  
`A Crime [A Crime - Spaete Rache] - bonus (o).iso` or  
`A Crime Bonus [A Crime - Spaete Rache] (o).iso`  
*Resulting target-folder / file-name:*  
`/A Crime [A Crime - Spaete Rache]/A Crime.mkv`  

*ISO-file name:* (normal DVD/BR)  
`Dogma (c) (german).iso`  
*Resulting target-folder / file-name:*  
`/Dogma (german)/Dogma (german).mkv`  

*ISO-file name:* (two-sided DVD/BR)  
`Analyze This [Reine Nervensache] (side A) (o).iso`  
*Resulting target-folder / file-name:*  
`/Analyze This [Reine Nervensache] (side A)/Analyze This (side A).mkv`  

#### TV-Series Only  
In order for the program to recognize a file as part of a series, you have to add a string in the form of:
`s<nn>e<nn>-e<nn>` if the ISO file contains multiple episodes, or `s<nn>e<nn>` if it only contains a single episode. This is important since it will react differently after ripping the MKV-files.  
Sometimes, when dealing with series, makeMKV sometimes does a 'catch-all' track containing all other tracks on the DVD/BR. The script recognizes this and removes this 'catch-all' track automatically.  

*ISO-file name:* (multiple episodes on disc)  
`Avatar - The Last Airbender - s01e05-e08 (c).iso`  
*Resulting target-folder / file-names:*  
`/Avatar - The Last Airbender - s01e05-e08/Avatar - The Last Airbender - s01e05.mkv`  
`/Avatar - The Last Airbender - s01e05-e08/Avatar - The Last Airbender - s01e06.mkv`  
`/Avatar - The Last Airbender - s01e05-e08/Avatar - The Last Airbender - s01e07.mkv`  
`/Avatar - The Last Airbender - s01e05-e08/Avatar - The Last Airbender - s01e08.mkv`  

*ISO-file name:* (single episode on disc)  
`Avatar - The Last Airbender - s01e14 (c).iso`  
*Resulting target-folder / file-name:*  
`/Avatar - The Last Airbender - s01e14/Avatar - The Last Airbender - s01e014.mkv`  

---
This program is brought to you by [Unterrainer Informatik][homepage]  
Project lead is [Gerald Unterrainer][geraldmail]

[geraldmail]: mailto:gerald@unterrainer.info
[homepage]: http://www.unterrainer.info
[coding]: http://www.unterrainer.info/Home/Coding
[makemkv]: http://www.makemkv.com/
[lombok]: https://projectlombok.org
[github]: https://github.com/UnterrainerInformatik/bulkmakemkv
