# Java Generator
*Copyright &copy; 2021 Warren Taylor.  All right reserved.*

https://github.com/tsi-software/JavaGenerator

If you happen to be proficient with programming languages like C# or Python then you may have used their native implementations of the Generator Pattern. If you're also a Java programmer you may know that Java does not natively implement the Generator Pattern.

I was recently working on a Java project that screamed out for the Generator Pattern. So, after searching the Internet for a while I found a few Java libraries and gave them a try. Unfortunately, they weren’t exactly what I was looking for.

## Features
The features I needed:

- AutoCloseable _(also known as the try-with-resources statement)_ - when the generator finishes, or there is an exception, it simply cleans up after itself.
- Iterable&lt;T&gt; - To keep code simple I want to be able to use the "for each" operator.
- Iterator&lt;T&gt; - This is the essence of the generator pattern.
- No special codes or hacks required to mark the end-of-data.
- Simple and robust to use.

## What is a Generator?
Lets take a step back. What is a generator and why did it become a thing?
Ok, I’m not going into a full history lesson (that's what Google is for) but here’s the deal.
Multi-threaded programming is hard.
It’s hard because you can end up putting exponential time and effort into making sure each thread does not “step on” what each of the other threads are doing.
In other words, making threads play nice is like herding cats.

One of the reasons Generators became a thing, is they allow code to take advantage of multi-threaded like behavior but still guarantee thread safety.
With Generators you don’t have to spend that exponential time and effort making sure the threads play nice (i.e. there is only one cat to herd).

The astute reader
_(this is not light reading so you must be astute to have gotten this far)_
will now be asking the question “when and why would I need this?”
Because there are times when we need to generate data independently of the main thread,
otherwise, the code complexity goes through the roof.
For example, you have a complex input filtering process that needs to skip and/or modify data based on the previous, current, and next records.
And you have a main process that jumps around a lot but still requires a deterministic serialized input stream.

## Insight
After having taken a second look at what I actually wanted it became clear that
the Generator Pattern (as implemented in Python and C#), at its most abstract,
is simply a thread safe __*emulation*__ of multi-threaded behavior
using something similar to cooperative multitasking.
Ok, so maybe that’s not entirely simple.

# Implementation
## ThreadSafeGenerator&lt;T&gt;
This class is most easily described as a two threaded implementation of the Producer/Consumer pattern where,
at any given point in time, one thread is active and one thread blocked waiting for the other.
Then atomically swap active/blocking at the appropriate time ... guaranteeing thread-safe execution.
It turned out to be a bit more complicated than that, but not too bad.

## ThreadedGenerator&lt;T&gt;
Recalling that other Generator Pattern implementations are an __*emulation*__ of multi-threaded behavior,
why not write a truly multi-threaded Generator.
This led to the second variant, which has a queue of a predetermined maximum size
allowing the background generator thread to run on ahead of the foreground thread but by a controlled amount.
This variant requires the extra care and effort of writing your generator code in a thread-safe manner
but with potentially much faster running code.

## Implementation Details
I was originally hoping to implement both of these variants with a single base class
containing the common functionality and inheriting classes with specific functionality.
However, this turned out to be a lot more complicated than writing the two classes separately.

## Usage
The whole point of a generator is to convert an arbitrarily complicated algorithm
into a "flat" serial iterator.
On the surface ThreadSafeGenerator&lt;T&gt; and ThreadedGenerator&lt;T&gt; are called in the same manner
and could be used interchangeably.
However, your ThreadedGenerator&lt;T&gt; code must be thread safe, otherwise bad and unexpected things will happen.

Creating your generator class is straight forward:
```java
public class SimpleGeneratorExample extends ThreadSafeGenerator<String> {...}
```

In the business end of the generator you write your code and call 'yieldReturn(...);'
whenever you want to pass data over to your iterator in the foreground thread:
```java
    @Override
    protected void generator() throws InterruptedException {
        yieldReturn("one");
        yieldReturn("two");
        yieldReturn("three");
    }
```
_Note: 'yieldReturn(...)' is inherited from
ThreadSafeGenerator&lt;T&gt; and ThreadedGenerator&lt;T&gt;._

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
    SimpleGeneratorExample generator = new SimpleGeneratorExample();
    try {
        Iterator<String> iter = generator.iterator();
        while (iter.hasNext()) {
            System.out.format("%s\n", iter.next());
        }
    } finally {
        generator.close();
    }
```

https://github.com/tsi-software/JavaGenerator
