module cli {
    requires org.slf4j;
    requires org.slf4j.simple;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    opens cli to com.fasterxml.jackson.databind;
}
