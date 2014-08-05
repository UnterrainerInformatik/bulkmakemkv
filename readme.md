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
 
# Disclaimer

This section of our GIT repository is free. You may copy, use or rewrite every single one of its contained projects to your hearts content.
In order to get help with basic GIT commands you may try [the GIT cheat-sheet][2] on our [homepage][1].

# BulkMakeMKV

MakeMKV is a program that extracts MKV-files from DVD/BR-rips that are saved as an image (ISO) or a DVD/BR in general (a mounted one, preferably).

You may get it from here [makeMKV][3]. You will need it in order to use this tool.  

The program is used by starting the jar-file (located in the `target` directory of the project). The program is configurated using the file `config.properties` which it will expect to find next to itself (the same directory or on the path).

Start the program, as you would start any jar by typing:
```
java -jar bulkMakeMkv.jar
```
or put these lines in a batch-file (should be more convenient):
```
java -jar bulkMakeMkv.jar
pause
```

## The Config File  

Here is an example of a config-file. I like to think it is very self-descriptive:

``` properties
# The directory where your ISO-files are located.
isoDir = \\\\computername\\f$\\DVDs 

# This is the temporary directory which is used when makeMKV is working.
# Be sure it doesn't contain files named "title<NN>.mkv" (the program 
# checks for any mkv-files starting with "title") since that would collide 
# with the files makeMKV wants to make.
# Other that that you may chose any directory.
tempDir = c:\\temp

# This is the directory where the program will create the right directories 
# in containing the resulting MKV-files.
mkvDir = D:\\Movies\\new

# The location of your local makeMKV-installation. Be sure to contain the 
# name of the runtime as well, not only the directory.
makeMkvCommand = C:/Program Files (x86)/MakeMKV/makemkvcon.exe

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

Examples:  

#### Movies/TV-Series
*ISO-file name:* (normal DVD/BR)  
`A Crime [A Crime - Späte Rache] (o).iso`  
*Resulting target-folder / file-name:*  
`\A Crime [A Crime - Späte Rache]\A Crime.mkv`  

*ISO-file name:* (normal DVD/BR)  
`Dogma (c) (german).iso`  
*Resulting target-folder / file-name:*  
`\Dogma (german)\Dogma (german).mkv`  

*ISO-file name:* (two-sided DVD/BR)  
`Analyze This [Reine Nervensache] (side A) (o).iso`  
*Resulting target-folder / file-name:*  
`\Analyze This [Reine Nervensache] (side A)\Analyze This (side A).mkv`  

#### TV-Series Only  
*ISO-file name:* (multiple episodes on disc)  
`Avatar - The Last Airbender - s01e05-e08 (c).iso`  
*Resulting target-folder / file-names:*  
`\Avatar - The Last Airbender - s01e05-e08\Avatar - The Last Airbender - s01e05.mkv`  
`\Avatar - The Last Airbender - s01e05-e08\Avatar - The Last Airbender - s01e06.mkv`  
`\Avatar - The Last Airbender - s01e05-e08\Avatar - The Last Airbender - s01e07.mkv`  
`\Avatar - The Last Airbender - s01e05-e08\Avatar - The Last Airbender - s01e08.mkv`  

*ISO-file name:* (single episode on disc)  
`Avatar - The Last Airbender - s01e14 (c).iso`  
*Resulting target-folder / file-name:*  
`\Avatar - The Last Airbender - s01e14\Avatar - The Last Airbender - s01e014.mkv`  

Sometimes, when dealing with series, makeMKV does a 'catch-all' track containing all other tracks on the DVD/BR. The script recognizes this and removes this 'catch-all' track automatically.  

In addition to that the script doesn't convert bonus-discs. A bonus disc is identified by a trailing `bonus` or `Bonus` at the end of the regular file-name. When the script encounters that it displays a proper message and skipps the file. Just as it would do when the encountered file-name results in a folder-name that already exists.

[1]: http://www.unterrainer.info
[2]: http://www.unterrainer.info/Home/Coding
[3]: http://www.makemkv.com/