# JavaGenerator
*Copyright &copy; 2020 Warren Taylor.  All right reserved.*

So, Java apparently does not have a built-in implementation of the Generator Pattern like, for example, Python and C#.
After searching the Internet for a while I found a few Java Libraries but they didn't exactly fit my needs.

## Features
My bucket list includes:

- AutoCloseable - _(also known as the try-with-resources statement)_ when the generator is finished, or there is an exception, I just want it to clean-up after itself.
- Iterable&lt;T&gt; - To keep code simple I want to be able to use the "for each" operator.
- Iterator&lt;T&gt; - The essence of a generator.

## ThreadSafeGenerator&lt;T&gt;
I took a second look at what I actually wanted and realized that
the Generator Pattern (as implemented in Python and C#) is an __*emulation*__ of
multi-threaded behavior but done in a thread safe manner.

OK, so simply write a threaded Producer/Consumer implementation
where, at any given point in time, one thread is active and the other thread blocked waiting.
Then switch atomically at the appropriate time ... guaranteeing thread-safe execution.
It turned out to be a bit more complicated than that, but not too bad.

## ThreadedGenerator&lt;T&gt;
Recalling that other Generator Pattern implementations are an __*emulation*__ of multi-threaded behavior,
why not write a truly multi-threaded Generator.
This led to the second variant, which has a queue of a predetermined maximum size
allowing the background generator thread to run on ahead of the foreground thread but by a controlled amount.
This variant requires the extra care and effort of writing your generator code in a thread-safe manner
but with potentially faster running code.

## Implementation Details
I was originally hoping to implement both of these variants with a single base class
containing the common functionality and inheriting classes with specific functionality.
However, this turned out to be a lot more complicated than writing the two classes separately.

## Usage
The whole point of a generator is to convert a more complicated algorithm
into a "flat" serial iterator.

On the surface ThreadSafeGenerator and ThreadedGenerator are called in the same manner
and could be used interchangeably.
However, your ThreadedGenerator code must be thread safe, otherwise bad and unexpected things will happen.

Creating you generator class is straight forward:
```java
public class SimpleGeneratorExample extends ThreadSafeGenerator<String> { ... }
```

In the business end of the generator you write your code and call 'yieldReturn(...)'
whenever you want to pass data over to your iterator in the foreground thread:
```java
    @Override
    protected void generator() throws InterruptedException {
        yieldReturn("one");
        yieldReturn("two");
        yieldReturn("three");
    }
```

To make use of your generator,
open it using a "try-with-resources" statement
then iterate using a "for each" statement.
```java
  try (SimpleGeneratorExample generator = new SimpleGeneratorExample()) {
    for (String item : generator) {
      System.out.format("%s\n", item);
    }
  }
```

Or you could do it the protracted way:
```java
    ProtractedGeneratorExample generator = new ProtractedGeneratorExample();
    try {
        Iterator<String> iter = generator.iterator();
        while (iter.hasNext()) {
            System.out.format("%s\n", iter.next());
        }
    } finally {
        generator.close();
    }
  }
```
