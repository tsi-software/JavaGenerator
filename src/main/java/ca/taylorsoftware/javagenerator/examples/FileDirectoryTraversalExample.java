package ca.taylorsoftware.javagenerator.examples;

import java.io.File;
import java.io.IOException;

import ca.taylorsoftware.javagenerator.ThreadedGenerator;


/**
 * 1) Extend one of the Java Generator variants. In this case: 'ThreadedGenerator'
 * 2) Implement the abstract 'generator()' method.
 *
 * To run this example:
 * mvn clean install exec:java -Dexec.mainClass="ca.taylorsoftware.javagenerator.examples.FileDirectoryTraversalExample"
 * 
 * @author Warren Taylor
 * Created: 2020-09-29
 * Copyright (c) 2020 Warren Taylor.  All right reserved.
 */
public class FileDirectoryTraversalExample extends ThreadedGenerator<File> {

    /**
     * Deceptively simple top level code to serially iterate through the values generated from a recursive algorithm.
     * @param args
     */
    public static void main(String[] args) {
        try (FileDirectoryTraversalExample generator = new FileDirectoryTraversalExample()) {
            for (File file : generator) {
                System.out.format("%s\n", file.getPath());
            }
        }
    }


    FileDirectoryTraversalExample() {
        // Allow the background thread to re-ahead by 32 files and/or directories.
        // ...because we can.
        super(32);
    }


    /**
     * From an abstract view, this generator is converting a slightly complicated recursive algorithm
     * into a "flat" serial iterator.
     */
    @Override
    protected void generator() throws InterruptedException {
        // The following is for example purposes only and would likely be better
        // implemented with either Files.walk(...) or Files.walkFileTree(...).
        // Or even one of several other open source libraries.
        File root = new File(".");
        recurseDirectory(root);
    }


    private void recurseDirectory(File dir) throws InterruptedException {
        try {
            // Process the Directory first...
            yieldReturn(dir.getCanonicalFile());

            File[] files = dir.listFiles();

            // Process the Files in the Directory second...
            for (File file : files) {
                if (file.isFile()) {
                    yieldReturn(file.getCanonicalFile());
                }
            }

            // Finally, recurse into sub-directories...
            for (File file : files) {
                if (file.isDirectory()) {
                    recurseDirectory(file);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
