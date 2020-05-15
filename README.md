# fhir-client-example
# FHIR Client Example


[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

This `fhir-client-example` project demonstrates very basic functionality
using the [HAPI FHIR](http://hapifhir.io/) library to do a search for patients
against a FHIR server while using HAPI FHIR interceptors along with some
rudimentary error handling.

## System requirements

Maven and JDK 8.

## How to build

1. Build the Maven project:

```
mvn clean package
```

## Run the search against a FHIR server

Once the project is built, it can be ran using the Exec Maven plugin
(`exec`).
A FHIR base server URL (e.g., `http://test.fhir.org/r4` or
`http://localhost:8080/hapi-fhir-jpaserver/fhir`) must be provided as a
command line argument. Also (optionally), the logging level can be
changed from its default of `INFO` via the
`-Dorg.slf4j.simpleLogger.defaultLogLevel` property.

For example, to build the project and run it against a FHIR server with a
logging level of `DEBUG` for this project's code, run the following:

```
mvn clean package exec:java -Dexec.args="http://test.fhir.org/r4" -Dorg.slf4j.simpleLogger.log.com.binu.fhirclientr4=DEBUG
```

## License

This FHIR Client Example is licensed under the terms of the
[MIT License](LICENSE.txt).
