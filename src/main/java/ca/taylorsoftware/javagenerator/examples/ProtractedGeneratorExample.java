package ca.taylorsoftware.javagenerator.examples;

import java.util.Iterator;

import ca.taylorsoftware.javagenerator.ThreadedGenerator;

public class ProtractedGeneratorExample extends ThreadedGenerator<String> {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try (ProtractedGeneratorExample generator = new ProtractedGeneratorExample()) {
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
            // The try-with-resources statement will properly clean-up.
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
