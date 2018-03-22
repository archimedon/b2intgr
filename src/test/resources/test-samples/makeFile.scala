#!/usr/bin/env scala

import java.io.PrintWriter
import java.io.OutputStreamWriter
import java.io._
import java.nio.file.Paths;


if (args.length < 3) {
	System.err.println("\n\tUsage:  makeFile filename colwidth numlines\n");
	System.exit(0);
}
val filePath = args(0);
val colwidth: Int = Integer.parseInt(args(1));
val numlines: Int = Integer.parseInt(args(2));

val path = makeFile(filePath, colwidth, numlines);
val fsize = path.toFile().length();


System.err.println("\nCreated File:\n\tname: " + filePath + "\n\tsize (bytes): " + fsize + "\n");

def makeFile( filePath: String, width: Int , length: Int ) = {

	// initial hex number to write
	var letter = 0x0;
	
	// hex based
	val numBase = 0x10;
	
	val scribe = new PrintWriter(
		new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8")
		, true
	);

	for (h <- 0 to length - 1) {
	  letter = letter % numBase;
	  for (i <- 1 to width - 1) { scribe.print(letter.toHexString); }
	  if (length - h > 0) scribe.println(""); // ... + '\n'
	  letter = letter + 1;
	}
	
	scribe.close()
	Paths.get(filePath);
}
