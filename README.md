# Standalone Java distribution using `jlink`

_This repo illustrates how to use Java 9's new `jlink` tool by bundling a simple CLI that depends on Jackson._

`jlink` is essentially tree-shaking for the JRE.  It copies the bits of the JRE your app uses and drops the bits it doesn't use (e.g. swing).  Notably this is not Ahead-of-Time compilation - garbage collector still runs as normal.  Achieving this requires a whole different way of thinking about dependencies.  The concept of a 'classpath' is thrown out and instead we use a 'modulepath'.

## Results

CircleCI builds a distribution in an environment where Java is available, producing a 28MB tgz.  This dist is entirely self-contained and can now run on different, completely bare docker image!

```
./cli/build/mydist/bin/cli
Hello, world!
[main] INFO cli.Main - This is an info message: 1
[main] WARN cli.Main - This is a warning message: 2
{"boom":1}
2
```

The dist is not a single executable, but rather a folder containing an init script and some JVM internal files:

```
build/mydist
├── bin
│   ├── appletviewer
│   ├── cli
│   ├── java
│   └── keytool
├── conf
│   ├── logging.properties
│   ├── security
│   └── ...
├── legal
│   ├── java.base
│   └── ...
├── lib
│   ├── classlist
│   ├── jli
│   ├── jrt-fs.jar
│   ├── jspawnhelper
│   ├── jvm.cfg
│   ├── libawt.dylib
│   ├── libawt_lwawt.dylib
│   ├── psfont.properties.ja
│   ├── psfontj2d.properties
│   ├── security
│   ├── server
│   └── ...
└── release
```

## Developing locally

I installed [Java 10](http://www.oracle.com/technetwork/java/javase/downloads/jdk10-downloads-4416644.html) from Oracle. Ensure your have environment variables set up correctly to use Oracle's CLIs:

```
$ export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-10.0.1.jdk/Contents/Home
$ java -version
java version "10.0.1" 2018-04-17
Java(TM) SE Runtime Environment 18.3 (build 10.0.1+10)
Java HotSpot(TM) 64-Bit Server VM 18.3 (build 10.0.1+10, mixed mode)

$ export PATH=$PATH:$JAVA_HOME/bin
$ jlink --version
10.0.1
```

The workflow in IntelliJ is not seamless, but still workable.  Use _File_ -> _Open_ and then select the root `build.gradle` file. You should open this as a new project.  When you open `Main.java`, you will probably be prompted to _Setup SDK_. You want `10` here, so click _Configure..._ to add this for the first time.  All red underlines should disappear and

## Gotchas


**Patched jars** - As of version 2.9.5, Jackson does not yet publish a `module-info` file.  In order to use `jlink`, the build script expands these jars, uses the `jdeps` tool to generate a `module-info` and then re-compiles them.

**Platform dependence** - A distribution built on CI will not work on a mac.

