In order to make jackson work with jlink I think we need to define a module-info.java.

Seems like this can be auto-generated using `jdeps`:

    $ jdeps --generate-module-info ~/Downloads path/to/jackson-core-2.9.5.jar

    module com.fasterxml.jackson.core {
        exports com.fasterxml.jackson.core;
        exports com.fasterxml.jackson.core.async;
        exports com.fasterxml.jackson.core.base;
        exports com.fasterxml.jackson.core.filter;
        exports com.fasterxml.jackson.core.format;
        exports com.fasterxml.jackson.core.io;
        exports com.fasterxml.jackson.core.json;
        exports com.fasterxml.jackson.core.json.async;
        exports com.fasterxml.jackson.core.sym;
        exports com.fasterxml.jackson.core.type;
        exports com.fasterxml.jackson.core.util;

        provides com.fasterxml.jackson.core.JsonFactory with
            com.fasterxml.jackson.core.JsonFactory;

    }

inspired by: https://github.com/codetojoy/easter_eggs_for_java_9/blob/master/egg_34_stack_overflow_47727869/run.sh

