# Cautious Carnival
Challenge project.

## How To Run

```bash
docker run --rm -p8080:8080 ghcr.io/osvetlik/cautious-carnival:latest
```

Environment variables:
* `APP_DATA_SOURCE_TYPE` - default `classpath`
    - `classpath` - look for the classpath data source
    - `external` - use the external data source (file://, https://, ...)
* `APP_DATA_EXTERNAL` - default `https://raw.githubusercontent.com/mledoze/countries/master/countries.json`
* `APP_DATA_CLASSPATH` - default `countries.json`

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
