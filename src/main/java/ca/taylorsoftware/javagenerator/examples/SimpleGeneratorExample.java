package ca.taylorsoftware.javagenerator.examples;

import ca.taylorsoftware.javagenerator.ThreadSafeGenerator;

/**
 * @author Warren Taylor
 * Created: 2020-09-29
 * Copyright (c) 2020 Warren Taylor.  All right reserved.
 */
public class SimpleGeneratorExample extends ThreadSafeGenerator<String> {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try (SimpleGeneratorExample generator = new SimpleGeneratorExample()) {
            for (String item : generator) {
                System.out.format("%s\n", item);
            }
        }
    }


    @Override
    protected void generator() throws InterruptedException {
        yieldReturn("one");
        yieldReturn("two");
        yieldReturn("three");
    }

}
