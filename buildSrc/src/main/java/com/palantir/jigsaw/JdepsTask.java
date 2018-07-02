package com.palantir.jigsaw;

import org.gradle.api.tasks.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class JdepsTask extends Exec {
    private Supplier<String> jarName;
    private File jarFile;

    public void setJarName(Supplier<String> jarName) {
        this.jarName = jarName;
    }

    @Input
    public String getJarName() {
        return this.jarName.get();
    }

    public void setJarFile(File jarFile) {
        this.jarFile = jarFile;
    }

    @InputFile
    public File getJarFile() {
        return jarFile;
    }

    @TaskAction
    @Override
    protected void exec() {
        File jdepsOutputDir = new File(String.format("%s/jdeps/%s", getProject().getBuildDir(), getJarName()));
        List<String> jdepsCommands = new ArrayList<>();
        jdepsCommands.add("jdeps");
//        jdepsCommands.add("-verbose");
        String files = getProject().fileTree(new File(getProject().getBuildDir().getAbsolutePath(), "patchJar")).getAsPath();
        if (files != null && !files.isEmpty()) {
            jdepsCommands.add("--module-path");
            jdepsCommands.add(files);
        }
        jdepsCommands.add("--generate-module-info");
        jdepsCommands.add(jdepsOutputDir.toString());
        jdepsCommands.add(jarFile.getAbsolutePath());

        super.setCommandLine(jdepsCommands);

        super.exec();
    }
}
