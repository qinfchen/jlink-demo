package cli;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) throws IOException {
        System.out.println("Hello, world!");
        log.info("This is an info message: {}", 1);
        log.warn("This is a warning message: {}", 2);

        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(Map.of("boom", 1)));

        Foo foo = mapper.readValue("{\"boom\":2, \"other\": 3}", Foo.class);
        System.out.println(foo.someField);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Foo {
        @JsonProperty("boom")
        int someField;
    }
}
