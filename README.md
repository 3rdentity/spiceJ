# spiceJ

spiceJ is a network traffic shaping tool written in Java.

Its goal is the simulation of a few network properties:

1. Limited Bandwidth
1. Additional Latency (**not yet implemented**)

All properties allow for jitter (**not yet implemented**), and are dynamic (ie. can change during simulation time).

spiceJ aims to support reproducible outcomes, meaning that a simulation is purely deterministic and contains no random elements. To achieve completely reproducible simulation, use `SimulationTickSource` instead of `RealTimeTickSource`.

spiceJ consists of Java Proxy Objects of InputStream and OutputStream, hiding the original stream and adding the desired properties. Furthermore, spiceJ offers wrappers for creating a transparent (stand-alone) TCP proxy, allowing users to simulate network shaping using any client application.

## Build

The project is using maven as a built tool, so simply running

    mvn package

is enough to compile, test and package all source code. The stand-alone proxy is packaged as a runnable JAR `proxy/target/proxy-X.Y.Z-jar-with-dependencies.jar`, where `X.Y.Z` is the current version.

## Usage

You can use spiceJ either by including it into your project and using its classes, or in standalone mode.

### As a library

To use spiceJ as a library, take a look at the `Streams` class, it contains helper methods for using spiceJ's main features. It is also a good entry point for exploring the javadoc present in code. All public API members are documented in-code.

### Stand-Alone 

To start spiceJ in stand-alone TCP proxy mode, use the executable jar built from the `proxy` project. To get usage info, issue `--help`:

    $ java -jar path/to/proxy.jar -help

To start a TCP proxy with a given rate limit of bytes per second, use the following command:

    $ java -jar path/to/proxy.jar -l <local-port> -h <remote-host> -p <remote-port> -r <byterate>

You can also specify different upstream and downstream rates:

    $ java -jar path/to/proxy.jar -l <local-port> -h <remote-host> -p <remote-port> -a <upstream> -b <donwstream>

spiceJ creates a proxy listening on port `<local-port>` which connects to `<remote-host>:<remote-port>` whenever an inbound connection is accepted. It then relays data in both directions while respecting the upstream and downstream rates.

## Testing

### Try it!

You can try the bandwidth limiting functionality using `nc` and `pv` (pipe viewer, a tool available for many Linux distributions). Using three terminals, do the following:

In terminal 1 (listening terminal), launch:

    $ nc -vvvlp 1235 | pv > /dev/null

In terminal 2 (proxy), launch one of the following::

    $ java -jar path/to/proxy.jar -l 1234 -p 1235 -r 1000      # for    1 kB/s =    0.97 kiB
    $ java -jar path/to/proxy.jar -l 1234 -p 1235 -r 10000     # for   10 kB/s =    9.76 kiB
    $ java -jar path/to/proxy.jar -l 1234 -p 1235 -r 100000    # for  100 kB/s =   97.65 kiB
    $ java -jar path/to/proxy.jar -l 1234 -p 1235 -r 1000000   # for    1 MB/s =  976.56 kiB = 0.95 MiB 
    $ java -jar path/to/proxy.jar -l 1234 -p 1235 -r 10000000  # for   10 MB/s = 9765.62 MiB = 9.53 MiB

Finally, in terminal 3 (sending terminal), launch:

    $ cat /dev/zero | nc -vvv localhost 1234

In terminal 1, `pv` should now show roughly the described byte rate. Note that `pv` uses binary prefixes (kiB, MiB, etc.), which denote multiples of 1024, and we specified powers of 10 in bytes per second, so the result deviates from the intuitively expected value.

There is almost no lower bound; spiceJ is designed to work with byte rates well below 1 B/s. The actual lower bound is caused by the way spiceJ internally generates low-rate traffic, but this bound is below 0.000000001 B/s  or 1/1000000000 B/s (this is 1 byte in 31.7 years). Also note that `pv` (in its current implementation) has a resolution of 1 s, which means that byte rates lower than 1 B/s are harder to measure. A natural upper bound of the byte rate is your system throughput, to which spiceJ naturally adds some overhead. The highest theoretically representable byterate is a bit lower than `Float.MAX_VALUE`, and its value is around 340090000000000000000000000 TB/s

### Unit Tests

Unit tests are present for most classes. In detail, the classes providing the external outcome (`RateLimitInputStream` and `RateLimitOutputStream`) are tested. Untested classes are simple utility methods, getters/setters and user interface (command line parsing) classes.

### Real-Time Tests

The performance of real-time components can't trivially be tested using Unit Tests, but there is a dedicated module (`real-time-tests`) containing test-like classes which can be executed and report their results. Its purpose is solely to verify that the real-time performance is within a certain error margin, it has no production value (hence the rather low code quality). In-code-documentation should suffice to understand how to interpret the results.

## Contributing

Feel free to contribute by creating pull requests and/or using the "Issues" section.

## History

- 2015-02-13: Started project development

## Credits

The readme file has been created using the template from [https://gist.github.com/zenorocha/4526327](https://gist.github.com/zenorocha/4526327)

## License

spiceJ is developed by Michael Borkowski.

spiceJ is licensed under the [MIT License](http://opensource.org/licenses/MIT).

