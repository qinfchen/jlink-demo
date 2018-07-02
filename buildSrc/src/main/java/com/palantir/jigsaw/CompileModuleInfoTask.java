package com.palantir.jigsaw;

import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CompileModuleInfoTask extends Exec {

    private Supplier<String> module;
    private Supplier<String> jarName;
    private File moduleInfo;
    private File outputDir;


    public void setModule(Supplier<String> module) {
        this.module = module;
    }


    @Input
    public String getModule() {
        return this.module.get();
    }

    public void setModuleInfo(File moduleInfo) {
        this.moduleInfo = moduleInfo;
    }

    @Input
    public File getModuleInfo() {
        return this.moduleInfo;
    }

    public void setJarName(Supplier<String> jarName) {
        this.jarName = jarName;
    }

    @Input
    public String getJarName() {
        return this.jarName.get();
    }



    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    @OutputDirectory
    public File getOutputDir() {
        return outputDir;
    }

    @TaskAction
    @Override
    protected void exec() {
        File modulePath = new File(getProject().getBuildDir(), "patchJar");

        List<String> commands = new ArrayList<>();
        commands.add("javac");
        commands.add("-d");
        commands.add(getOutputDir().toString());
        if (modulePath.exists()) {
            commands.add("--module-path");
            commands.add(modulePath.getAbsolutePath());
        }
        commands.add("--add-modules");
        commands.add(getModule());
        commands.add("--patch-module");
        commands.add(String.format("%s=%s/unpack/%s", getModule(), getProject().getBuildDir(), getJarName()));
        commands.add(getModuleInfo().getAbsolutePath());

        super.setCommandLine(commands);
        super.exec();
    }
}
