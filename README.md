#Work in Progress
**Please note**: spiceJ is under **heavy development** at the moment. Some parts of this readme describe present features, some describe features planned for the near future.

# spiceJ

spiceJ is a network traffic shaping tool written in Java.

Its goal is the simulation of a few network properties:

1. Limited Bandwidth
1. Additional Latency

All properties will allow for jitter, and will be supported dynamically (ie. can change over simulation time).

spiceJ aims to support reproducible outcomes, meaning that a simulation with random elements like jitter will use a mechanism to persist all non-deterministic data and make the outcome purely deterministic.

spiceJ consists of Java Proxy Objects of InputStream and OutputStream, hiding the original stream and adding the desired properties. Furthermore, spiceJ offers wrappers for creating a transparent (standalone) TCP proxy, allowing users to simulate network shaping using any client application.

## Build

The project is using maven as a built tool, so simply running

``` mvn package ```

is enough to compile, test and package all source code.

## Usage

Currently, the only way to use spiceJ is to start it in (standalone) TCP proxy mode:

``` java -jar proxy-0.0.1-SNAPSHOT-jar-with-dependencies.jar -l <local-port> -h <remote-host> -p <remote-port> -r <bytes-per-second> ```

## Contributing

Feel free to contribute by creating pull requests and/or using the "Issues" section.

## History

- 2015-02-13: Project Started, initial development

## Credits

The readme file has been created using the template from [https://gist.github.com/zenorocha/4526327](https://gist.github.com/zenorocha/4526327)

## License

spiceJ is licensed under the [MIT License](http://opensource.org/licenses/MIT).

