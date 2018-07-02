package com.palantir.jigsaw;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NeedsPatchingExtension {

    private final Set<String> modules = new HashSet<String>();

    public final void implementation(String module) {
        modules.add(module);
    }

    public final Set<String> getModules() {
        return Collections.unmodifiableSet(modules);
    }
}
