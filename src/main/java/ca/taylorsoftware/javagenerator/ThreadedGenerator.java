package ca.taylorsoftware.javagenerator;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;


public abstract class ThreadedGenerator<T> implements AutoCloseable, Iterable<T>, Iterator<T>, Runnable {
    /** The default maximum number of elements allowed in yieldReturnQueue. */
    private static final int DEFAULT_MAX_QUEUE_SIZE = 1;

    /** The maximum number of elements allowed in yieldReturnQueue. */
    private final int maxQueueSize;

    /** To reduce complexity carry the value from when 'hasNext()' is called to when 'next()' is called. */
    private T nextForegroundValue;

    /** The Producer/Consumer queue. */
    private final Deque<T> yieldReturnQueue = new LinkedList<T>();

    /** The background thread. */
    private final Thread thread;

    /** Used to determine if the background thread needs to quit. */
    private volatile boolean isThreadCancelled = false;

    /** Is foreground process cancelled. */
    private volatile boolean isClosed = false;


    public ThreadedGenerator() {
        this(DEFAULT_MAX_QUEUE_SIZE);
    }

    public ThreadedGenerator(int maxQueueSize) {
        if (maxQueueSize < 1) {
            String msg = "ThreadedGenerator(maxQueueSize): maxQueueSize must be greater than or equal to 1!";
            throw new IllegalArgumentException(msg);
        }

        this.maxQueueSize = maxQueueSize;
        thread = new Thread(this);
        thread.start();
    }


    @Override
    public void close() {
        if (Thread.currentThread() == thread) {
            // Background thread is calling 'close()'.
            if (!isThreadCancelled) {
                synchronized (yieldReturnQueue) {
                    isThreadCancelled = true;
                    yieldReturnQueue.notifyAll();
                }
            }
        } else {
            // Foreground thread is calling 'close()'.
            if (!isClosed) {
                isClosed = true;

                if (!isThreadCancelled) {
                    synchronized (yieldReturnQueue) {
                        isThreadCancelled = true;
                        yieldReturnQueue.notifyAll();
                    }
                }
            }
        }
    }


    @Override
    public Iterator<T> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {
        if (isClosed) {
            return false;
        }

        boolean result = false;
        nextForegroundValue = null;

        synchronized (yieldReturnQueue) {
            // If necessary, wait for an item to be added to the queue.
            while (yieldReturnQueue.isEmpty()) {
                if (isClosed || isThreadCancelled) {
                    // If 'close()' has been called then drop out of this loop and return false.
                    // If the background thread has finished then don't wait for anything else
                    //  to be added by yieldReturn(...).
                    break;
                }

                try {
                    yieldReturnQueue.wait();
                } catch (InterruptedException ex) {
                    // Ignore the InterruptedException and continue looping.
                }
            }

            result = !isClosed && !yieldReturnQueue.isEmpty();
            if (result) {
                // Capture the next value here because we have already gone through
                // the effort of synchronizing and verifying...
                nextForegroundValue = yieldReturnQueue.removeFirst();
                yieldReturnQueue.notifyAll();
            }
        }//synchronized

        return result;
    }


    @Override
    public T next() {
        if (isClosed) {
            throw new NoSuchElementException();
        }
        return nextForegroundValue;
    }


    @Override
    public void run() {
        try {
            //TODO: consider providing a mechanism to pass any exception caught here
            //      up to the hasNext() method on the foreground thread.
            generator();
        } catch (InterruptedException ex) {
            // Ignore the InterruptedException and continue looping.
        } finally {
            synchronized (yieldReturnQueue) {
                isThreadCancelled = true;
                yieldReturnQueue.notifyAll();
            }
        }
    }


    protected boolean canKeepGoing() {
        return !isThreadCancelled;
    }


    /**
     * The descendant implementation of this method is where all the work happens.
     * <br>
     * <b>Important! This method runs in a background thread.</b><br>
     * Keep things thread safe in the descendant implementation of this method.
     * @throws InterruptedException 
     */
    protected abstract void generator() throws InterruptedException;


    protected void yieldReturn(T item) throws InterruptedException {
        // Enforce that this method is only called from the background 'thread'.
        if (Thread.currentThread() != thread) {
            String msg = "yieldReturn(...) must only be called from the background generator thread!";
            throw new InterruptedException(msg);
        }

        if (isThreadCancelled) {
            throw new InterruptedException();
        }

        synchronized (yieldReturnQueue) {
            // If necessary, wait until space becomes available in the queue.
            while (yieldReturnQueue.size() >= maxQueueSize) {
                if (isThreadCancelled) {
                    break;
                }
                try {
                    yieldReturnQueue.wait();
                } catch (InterruptedException ex) {
                    // Ignore the InterruptedException and continue looping.
                }
            }

            if (isThreadCancelled) {
                throw new InterruptedException();
            }
            yieldReturnQueue.addLast(item);
            yieldReturnQueue.notifyAll();
        }
    }

}
