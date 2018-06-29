package com.palantir.jigsaw;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Copy;

import java.io.File;
import java.util.Set;

public class JigsawPatchPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getExtensions().create("needsPatching", NeedsPatchingExtension.class);

        project.getConfigurations().create("needsPatching");

        Copy unpack = project.getTasks().create("unpack", Copy.class);

        project.afterEvaluate(p -> {
           unpack.from(project.zipTree(JigsawPatchPlugin.getJarPath(project)));
           unpack.into(new File("build/unpack/" + JigsawPatchPlugin.getJarName(project)));
        });
    }

    public static File getJarPath(Project project) {
        Set<File> files = project.getConfigurations().getByName("needsPatching").getResolvedConfiguration().getFiles();
        return files.stream().findFirst().get();
    }

    public static String getJarName(Project project) {
        return getJarPath(project).getName().replace(".jar", "");
    }
}
