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

``` mvn package ```

is enough to compile, test and package all source code.

## Usage

You can use spiceJ either by including it into your project and using its classes, or in standalone mode.

### As a library

To use spiceJ as a library, take a look at the `Streams` class, it contains helper methods for using spiceJ's main features. It is also a good entry point for exploring the javadoc present in code. All public API members are documented in-code.

### Stand-Alone 

To start spiceJ in stand-alone TCP proxy mode, use the executable jar built from the `proxy` project. To get usage info, issue `--help`:

``` java -jar path/to/proxy.jar -help ```

To start a TCP proxy with a given rate limit of bytes per second, use the following command:

``` java -jar path/to/proxy.jar -l <local-port> -h <remote-host> -p <remote-port> -r <byterate> ```

You can also specify different upstream and downstream rates:

``` java -jar path/to/proxy.jar -l <local-port> -h <remote-host> -p <remote-port> -a <upstream> -b <donwstream> ```

spiceJ creates a proxy listening on port `<local-port>` which connects to `<remote-host>:<remote-port>` whenever an inbound connection is accepted. It then relays data in both directions while respecting the upstream and downstream rates.

## Contributing

Feel free to contribute by creating pull requests and/or using the "Issues" section.

## History

- 2015-02-14: Added documentation
- 2015-02-13: Project Started, initial development

## Credits

The readme file has been created using the template from [https://gist.github.com/zenorocha/4526327](https://gist.github.com/zenorocha/4526327)

## License

spiceJ is developed by Michael Borkowski.

spiceJ is licensed under the [MIT License](http://opensource.org/licenses/MIT).

