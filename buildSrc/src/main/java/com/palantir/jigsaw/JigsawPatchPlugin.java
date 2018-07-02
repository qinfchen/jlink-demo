package com.palantir.jigsaw;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class JigsawPatchPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        NeedsPatchingExtension extension = project.getExtensions().create("needsPatching", NeedsPatchingExtension.class);

        project.getConfigurations().create("needsPatching");
        Copy unpack = project.getTasks().create("unpack", Copy.class);
        Exec jdeps = project.getTasks().create("jdeps", Exec.class);
        Copy merge = project.getTasks().create("merge", Copy.class);
        Exec compileModuleInfo = project.getTasks().create("compileModuleInfo", Exec.class);
        Copy copyJarForPatching = project.getTasks().create("copyJarForPatching", Copy.class);
        Exec patchJar  = project.getTasks().create("patchJar", Exec.class);

        project.afterEvaluate(p -> {
            extension.getModules().stream().forEach(entry -> {
                File jarFile = JigsawPatchPlugin.getJarPath(
                        project, "needsPatching", Objects.toString(entry));
                String jarName = jarFile.getName().replace(".jar", "");
                unpack.from(project.zipTree(jarFile));
                unpack.into(new File(String.format("%s/unpack/%s", project.getBuildDir(), jarName)));

                File input = new File(String.format("%s/unpack/%s/module-info.java",
                        project.getBuildDir(), jarName));
                File manifestFile = new File(String.format("%s/unpack/%s/META-INF/MANIFEST.MF",
                        project.getBuildDir(), jarName));
                File outputDir = new File(String.format("%s/compileModuleInfo/%s",
                        project.getBuildDir(), jarName));


                // compileModuleInfo tasks
                compileModuleInfo.dependsOn(merge);
                Manifest manifest = new DefaultManifest(manifestFile, new IdentityFileResolver());
                Object module = manifest.getAttributes().get("Automatic-Module-Name");
                Logger.getGlobal().log(Level.INFO, module.toString());

                List<String> commands = new ArrayList<>();
                commands.add("javac");
                commands.add("-d");
                commands.add(outputDir.toString());
                commands.add("--module-path");
                commands.add("../jackson-core/build/patchJar/jackson-core-2.9.5.jar:../jackson-annotations/build/patchJar/jackson-annotations-2.9.5.jar");
                commands.add("--add-modules");
                commands.add((String) module);
                commands.add("--patch-module");
                commands.add(String.format("%s=%s/unpack/%s", module, project.getBuildDir(),jarName));
                commands.add(input.toString());

                compileModuleInfo.setCommandLine(commands);
                compileModuleInfo.getOutputs().dir(outputDir);

                // jdeps
                File jdepsOutputDir = new File(String.format("%s/jdeps/%s", project.getBuildDir(), jarName));
                List<String> jdepsCommands = new ArrayList<>();
                jdepsCommands.add("jdeps");
                jdepsCommands.add("--module-path");
                jdepsCommands.add("../jackson-core/build/patchJar/jackson-core-2.9.5.jar:../jackson-annotations/build/patchJar/jackson-annotations-2.9.5.jar");
                jdepsCommands.add("--generate-module-info");
                jdepsCommands.add(outputDir.toString());
                jdepsCommands.add(jarFile.getAbsolutePath());
                jdeps.setCommandLine(jdepsCommands);
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
