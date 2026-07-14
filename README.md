# Cautious Carnival
Challenge project.

## How To Run

### Latest
```bash
docker run --rm -p8080:8080 ghcr.io/osvetlik/cautious-carnival:latest
```

### Specific tag (`v1.0.0`)
```bash
docker run --rm -p8080:8080 ghcr.io/osvetlik/cautious-carnival:v1.0.0
```

### Configuration

Environment variables:
* `APP_DATA_SOURCE_TYPE` - default `classpath`
    - `classpath` - look for the classpath data source
    - `external` - use the external data source (file://, https://, ...)
* `APP_DATA_EXTERNAL` - default `https://raw.githubusercontent.com/mledoze/countries/master/countries.json`
* `APP_DATA_CLASSPATH` - default `countries.json`

To run against the provided external source, try:

```bash
docker run --rm -p8080:8080 -eAPP_DATA_SOURCE_TYPE=external ghcr.io/osvetlik/cautious-carnival:latest
```

## Probes

### Liveness
http://localhost:8080/actuator/health/liveness

Standard Spring Boot liveness probe, no extras.

### Readiness
http://localhost:8080/actuator/health/readiness

Standard Spring Boot readiness probe with the addition of routing data preparation. They are
loaded and parsed after the app starts. In case of a remote data source load failure, the app
keeps running, probing the data source periodically, liveness *UP*, readiness *OUT_OF_SERVICE*.

## Used algorithm

Considering the size and effective immutability of the domain data, I decided to forgo a route search
at every request. Instead, I create a next-hop map on startup, finding a path is then a simple matter
of following next hops from origin to destination.

This has several advantages:
* stable footprint
* fast execution

## Possible improvements
* Make loaded data imutable.
* Current next-hop map creation relies on data being consistent, there could be more sanity checking on load.
* Fill `ProblemDetail` better.
* Handle validation errors on incorrect inputs with grace.

