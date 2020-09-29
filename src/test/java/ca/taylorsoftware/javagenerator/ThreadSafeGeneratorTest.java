package ca.taylorsoftware.javagenerator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import org.junit.jupiter.api.Test;


class ThreadSafeGeneratorTest {

    //-------------------------------------------------------------------------
    private static class EmptyGenerator extends ThreadSafeGenerator<String> {
        @Override
        protected void generator() throws InterruptedException {
        }
    }

    @Test
    void testEmptyGenerator() {
        List<String> expected = new ArrayList<>();
        try (EmptyGenerator iter = new EmptyGenerator()) {
            assertIterableEquals(expected, iter);
        }

        try (EmptyGenerator iter = new EmptyGenerator()) {
            assertThat(iter, not(contains("one")));
        }
    }



    //-------------------------------------------------------------------------
    private static class SingleGenerator extends ThreadSafeGenerator<String> {
        @Override
        protected void generator() throws InterruptedException {
            yieldReturn("one");
        }
    }

    @Test
    void testSingleGenerator() {
        List<String> expected = Arrays.asList("one");
        try (SingleGenerator iter = new SingleGenerator()) {
            assertIterableEquals(expected, iter);
        }

        try (SingleGenerator iter = new SingleGenerator()) {
            assertThat(iter, not(contains("one", "two")));
        }
    }



    //-------------------------------------------------------------------------
    private static class SimpleGenerator extends ThreadSafeGenerator<String> {
        @Override
        protected void generator() throws InterruptedException {
            yieldReturn("one");
            yieldReturn("two");
            yieldReturn("three");
        }
    }

    @Test
    void testSimpleGenerator() {
        List<String> expected = Arrays.asList("one", "two", "three");
        try (SimpleGenerator iter = new SimpleGenerator()) {
            assertIterableEquals(expected, iter);
        }

        try (SimpleGenerator iter = new SimpleGenerator()) {
            assertThat(iter, not(contains("one", "two", "three", "four")));
        }

        try (SimpleGenerator iter = new SimpleGenerator()) {
            assertThat(iter, not(contains("two", "three")));
        }
    }

    @Test
    void testSimplePrematureClose() {
        try (SimpleGenerator iter = new SimpleGenerator()) {
            int counter = 0;
            for (String it : iter) {
                SimpleGenerator.message("%s\n", it);
                ++counter;
                if (counter == 2) {
                    iter.close();
                }
            }
        }

    }



    //-------------------------------------------------------------------------
    private static class PrematureCloseGenerator extends ThreadSafeGenerator<String> {
        @Override
        protected void generator() throws InterruptedException {
            yieldReturn("one");
            close();
            yieldReturn("two");
        }
    }

    @Test
    void testPrematureCloseGenerator() {
        List<String> expected = Arrays.asList("one");
        try (PrematureCloseGenerator iter = new PrematureCloseGenerator()) {
            assertIterableEquals(expected, iter);
        }
        try (PrematureCloseGenerator iter = new PrematureCloseGenerator()) {
            assertThat(iter, contains("one"));
        }
        try (PrematureCloseGenerator iter = new PrematureCloseGenerator()) {
            assertThat(iter, not(contains("one", "two")));
        }
    }



    //-------------------------------------------------------------------------
    private static class RandomDelayGenerator extends ThreadSafeGenerator<Integer> {
        volatile boolean isForegroundRunning = true;
        volatile boolean isBackgroundRunning = false;

        @Override
        protected void generator() throws InterruptedException {
            setBackgroundRunning(true);

            final long maxDelay = 100; //milliseconds.
            final long totalDuration = 2000; // Test for 2 seconds.

            int counter = 0;
            long startTime = System.nanoTime();

            // Random delay before the first call to 'yieldReturn(...)'.
            long millis = ThreadLocalRandom.current().nextLong(0, maxDelay);
            Thread.sleep(millis);

            while (System.nanoTime()-startTime < totalDuration*1000000) {
                ++counter;

                setBackgroundRunning(false);
                yieldReturn(counter);
                setBackgroundRunning(true);

                randomDelay(counter, maxDelay);
            }

            setBackgroundRunning(false);
        }

        void setForegroundRunning(boolean isRunning) throws InterruptedException {
            if (isBackgroundRunning) {
                String msg = "The Background is running when NOT expected!";
                throw new InterruptedException(msg);
            }
            isForegroundRunning = isRunning;
        }

        void setBackgroundRunning(boolean isRunning) throws InterruptedException {
            if (isForegroundRunning) {
                String msg = "The Foreground is running when NOT expected!";
                throw new InterruptedException(msg);
            }
            isBackgroundRunning = isRunning;
        }

        static void randomDelay(final int counter, final long maxDelay) throws InterruptedException {
            long millis;

            // The first 3 iterations are of fixed time, the remaining iterations are of random time.
            switch (counter) {
            case 1:
                millis = 0;
                break;
            case 2:
                millis = 10;
                break;
            case 3:
                millis = 100;
                break;
            default:
                millis = ThreadLocalRandom.current().nextLong(0, maxDelay);
                break;
            }
            Thread.sleep(millis);
        }
    }

    @Test
    void testRandomDelayGenerator() throws InterruptedException {
        try (RandomDelayGenerator iter = new RandomDelayGenerator()) {
            int counter = 0;
            iter.setForegroundRunning(false);
            for (Integer it : iter) {
                iter.setForegroundRunning(true);

                ++counter;
                assertEquals(counter, it);
                RandomDelayGenerator.message("%d\n", it);
                RandomDelayGenerator.randomDelay(counter, 100);

                iter.setForegroundRunning(false);
            }
        }
    }

}
