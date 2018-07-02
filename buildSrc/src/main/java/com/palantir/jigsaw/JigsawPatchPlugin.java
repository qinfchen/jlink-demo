package com.palantir.jigsaw;

import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.file.IdentityFileResolver;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.java.archives.internal.DefaultManifest;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Exec;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JigsawPatchPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        NeedsPatchingExtension extension = project.getExtensions().create("needsPatching", NeedsPatchingExtension.class);

        project.getConfigurations().create("needsPatching");
        DefaultTask patchJars = project.getTasks().create("patchJars", DefaultTask.class);
        project.afterEvaluate(p -> {
            extension.getModules().stream().forEach(entry -> {
                File jarFile = JigsawPatchPlugin.getJarPath(
                        project, "needsPatching", Objects.toString(entry));

                // hack hack use guava
                String jarName = jarFile.getName().replace(".jar", "");
                String[] nameSegs= jarName.split("-");
                String jarUpperCamelCase = nameSegs[0].substring(0, 1).toUpperCase() + nameSegs[0].substring(1)
                        + nameSegs[1].substring(0, 1).toUpperCase() + nameSegs[1].substring(1);

                Copy unpack = project.getTasks().create("unpack" + jarUpperCamelCase, Copy.class);
                JdepsTask jdeps = project.getTasks().create("jdeps" + jarUpperCamelCase, JdepsTask.class);
                Copy merge = project.getTasks().create("merge" + jarUpperCamelCase, Copy.class);

                CompileModuleInfoTask compileModuleInfo = project.getTasks().create(
                        "compile" + jarUpperCamelCase, CompileModuleInfoTask.class);
                Copy copyJarForPatching = project.getTasks().create("copy" + jarUpperCamelCase, Copy.class);
                Exec patchJar  = project.getTasks().create("patch" + jarUpperCamelCase, Exec.class);
                unpack.from(project.zipTree(jarFile));
                unpack.into(new File(String.format("%s/unpack/%s", project.getBuildDir(), jarName)));

                File input = new File(String.format("%s/unpack/%s/module-info.java",
                        project.getBuildDir(), jarName));
                File outputDir = new File(String.format("%s/compileModuleInfo/%s",
                        project.getBuildDir(), jarName));

                File manifestFile = new File(String.format("%s/META-INF/MANIFEST.MF",
                        unpack.getDestinationDir()));

                // compileModuleInfo tasks
                compileModuleInfo.setModule(() -> {
                    Manifest manifest = new DefaultManifest(manifestFile, new IdentityFileResolver());
                    return (String) manifest.getAttributes().get("Automatic-Module-Name");
                });
                compileModuleInfo.dependsOn(merge);
                compileModuleInfo.setJarName(() -> jarName);
                compileModuleInfo.setOutputDir(outputDir);
                compileModuleInfo.getOutputs().dir(outputDir);
                compileModuleInfo.setModuleInfo(input);

                // jdeps
                File jdepsOutputDir = new File(String.format("%s/jdeps/%s", project.getBuildDir(), jarName));
                jdeps.setJarFile(jarFile);
                jdeps.setJarName(() -> jarName);
                jdeps.getOutputs().dir(jdepsOutputDir);

                // merge
                merge.dependsOn(unpack, jdeps);
                merge.from(jdeps.getOutputs().getFiles());
                merge.into(new File(String.format("%s/unpack/%s", project.getBuildDir(), jarName)));
                merge.setIncludeEmptyDirs(false);
                merge.eachFile(details -> details.setPath(details.getName()));

                copyJarForPatching.from(jarFile);
                copyJarForPatching.into(Paths.get(project.getBuildDir().getAbsolutePath(), "patchJar"));

                patchJar.dependsOn(copyJarForPatching);
                patchJar.dependsOn(compileModuleInfo);

                File jarToPatch = new File(String.format("%s/patchJar/%s.jar", project.getBuildDir(), jarName));
                File classFile = new File(String.format("%s/compileModuleInfo/%s/module-info.class", project.getBuildDir(), jarName));
                File changeDir = new File(String.format("%s/compileModuleInfo/%s", project.getBuildDir(), jarName));

                List<String> patchJarCommands = new ArrayList<>();
                patchJarCommands.add("jar");
                patchJarCommands.add("uf");
                patchJarCommands.add(jarToPatch.getAbsolutePath());
                patchJarCommands.add("-C");
                patchJarCommands.add(changeDir.getAbsolutePath());
                patchJarCommands.add(classFile.getName());
                patchJar.setCommandLine(patchJarCommands);
                patchJar.getOutputs().file(jarToPatch);

                // hack hack ensure previous jars are patched
                patchJars.getDependsOn().stream().map(task -> jdeps.dependsOn(task));
                patchJars.getDependsOn().stream().map(task -> patchJar.dependsOn(task));
                patchJars.dependsOn(patchJar);
            });
        });
    }

    public static File getJarPath(Project project, String config, String maven) {
        DependencySpec dependencySpec = new DependencySpec(Objects.toString(maven));
        return project.getConfigurations().maybeCreate(config).getResolvedConfiguration()
                .getFiles(dependencySpec).stream().findFirst().get();
    }

    public static class DependencySpec implements Spec<Dependency> {
        private final String group;
        private final String name;

        public DependencySpec(String maven) {
            String[] mavenCoordinates = maven.split(":");
            if (mavenCoordinates.length == 2) {
                group = mavenCoordinates[0];
                name = mavenCoordinates[1];
            } else {
                group = "";
                name = "";
            }
        }

        @Override
        public boolean isSatisfiedBy(Dependency dependency) {
            if (dependency.getGroup().equals(group)
                    && dependency.getName().equals(name)) {
                return true;
            }
            return false;
        }
    }

}
