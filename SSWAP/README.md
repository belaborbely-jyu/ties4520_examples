# iPlant Semantic Web Services, Simple Semantic Web Architecture and Protocol (SSWAP)

This software bundle contains the Java API for SSWAP. Java API is a lower level API than 
the other API for SSWAP (HTTP API). It provides more specialized services that are meant
to be called by users who are more comfortable with SSWAP internals ant internals of semantic
reasoning.

The source code contains a ant build file (build.xml), which can be used to compile Java API,
generate Javadoc documentation, prepare a packaged release, run automated tests, or clean
compiled artifacts.

## 1. Compile

To compile the source type:

``` shell
ant
```

It will create a "dist" directory that contains the results of the compilation, as well as all required
libraries. In particular, the results of the compilation are in "dist/lib", and are named sswap-api.jar 
and sswap-impl.jar The first one contains the API (interfaces), while the second one contains the implementation
of the API. The other subdirectories in "dist/lib" contain the libraries on which Java API depends.
The sources are packaged to "dist/src/sswap-api-src.jar".

## 2. Generate Javadoc documentation

To generate javadoc type:

``` shell
ant javadoc
```

The generated Javadoc documentation will be placed in dist/docs/javadoc

## 3. Prepare a packaged release

Type:

``` shell
ant zip
```

or preferably:

``` shell
ant clean release
```

This will both compile the source and generate the Javadoc documentation. At the end, it will 
create a zip file dist/sswap-X.Y.Z.zip (where X.Y.Z denote the current version of the API). The
zip file contains all the documentation, compressed sources, and the binaries.

## 4. Run automated tests

Type:

``` shell
ant test
```

This will run the automated test suite. If all tests pass, at the end, you should see "BUILD SUCCESSFUL"
message. 

Please note: automated tests try to dereference RDGs/PDGs and other SSWAP document published on the internet.
The tests will fail, if they cannot be retrieved (e.g., because of lack of network connection or because
the machines hosting these documents were down or otherwise unable to respond).

## 5. Clean all generated artifacts

Type:

``` shell
ant clean
```

This will remove all generated artifacts (compiled classes, documentation, zip files etc.)

