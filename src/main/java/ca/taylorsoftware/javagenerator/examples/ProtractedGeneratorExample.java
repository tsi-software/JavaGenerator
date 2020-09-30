package ca.taylorsoftware.javagenerator.examples;

import java.util.Iterator;

import ca.taylorsoftware.javagenerator.ThreadedGenerator;

/**
 * @author Warren Taylor
 * Created: 2020-09-29
 * Copyright (c) 2020 Warren Taylor.  All right reserved.
 */
public class ProtractedGeneratorExample extends ThreadedGenerator<String> {

    /**
     * @param args
     */
    public static void main(String[] args) {
        ProtractedGeneratorExample generator = new ProtractedGeneratorExample();
        try {
            Iterator<String> iter = generator.iterator();

            if (iter.hasNext()) {
                // Expected: "one"
                System.out.format("%s\n", iter.next());
            }

            if (iter.hasNext()) {
                // Expected: "two"
                System.out.format("%s\n", iter.next());
            }

            if (iter.hasNext()) {
                // Expected: "three"
                System.out.format("%s\n", iter.next());
            }

            // Note: we don't have to iterate through to completion if we don't want to.
            // The 'generator.close();' statement below will clean-up properly.

        } finally {
            generator.close();
        }
    }


    @Override
    protected void generator() throws InterruptedException {
        yieldReturn("one");
        yieldReturn("two");
        yieldReturn("three");
        yieldReturn("four");
    }

}
