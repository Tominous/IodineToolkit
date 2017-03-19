package iodine.maven;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GradleScriptBuilder {

    private String groupId = null;
    private String version = null;

    private boolean compileRootProject = false;

    private final Map<DependencyData, String> dependencies = new HashMap<>();
    private final List<RepositoryData> repositories = new ArrayList<>();

    private final List<String> plugins = new ArrayList<>();

    public void addDependency(String scope, DependencyData dependencyData) {
        dependencies.put(dependencyData, scope);
    }

    public GradleScriptBuilder addPlugin(String plugin) {
        plugins.add(plugin);
        return this;
    }

    public GradleScriptBuilder addDependency(String scope, String groupId, String artifactId, String version) {
        addDependency(scope, new DependencyData(groupId, artifactId, version));
        return this;
    }

    public GradleScriptBuilder addCompileDependency(DependencyData dependencyData) {
        addDependency("compile", dependencyData);
        return this;
    }

    public GradleScriptBuilder addCompileDependency(String groupId, String artifactId, String version) {
        addCompileDependency(new DependencyData(groupId, artifactId, version));
        return this;
    }

    public GradleScriptBuilder addRepository(RepositoryData repositoryData) {
        repositories.add(repositoryData);
        return this;
    }

    public GradleScriptBuilder addRepository(String name, String url) {
        addRepository(new RepositoryData(name, url));
        return this;
    }

    public GradleScriptBuilder setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public GradleScriptBuilder setVersion(String version) {
        this.version = version;
        return this;
    }

    public GradleScriptBuilder setCompileRootProject(boolean bool) {
        this.compileRootProject = bool;
        return this;
    }

    public GradleScriptBuilder importPom(POMData pomData) {
        pomData.getRepositories().forEach(this::addRepository);
        pomData.getDependencies().forEach(this::addCompileDependency);
        this.groupId = pomData.getGroupId();
        this.version = pomData.getVersion();
        addPlugin("java");
        return this;
    }

    public static void main(String... args) throws Exception {
        POMData pomData = new POMData("net.minecraft.server", "nms", "1.11", Arrays.asList
                (DependencyData.DEFAULT_DEPENDENCIES), Collections.singletonList(RepositoryData
                .MOJANG_REPOSITORY));
        System.out.println(new GradleScriptBuilder().importPom(pomData).create());
    }

    public String create() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        printWriter.println("group = '" + this.groupId + "'");
        printWriter.println("version = '" + this.version + "'");
        printWriter.println();
        plugins.forEach((pluginName) -> printWriter.println("apply plugin: '" + pluginName + "'"));
        printWriter.println();
        printWriter.println("repositories {");
        printWriter.println("    mavenCentral()");
        repositories.forEach((repositoryData -> {
            printWriter.println("    maven {");
            printWriter.println("       name = '" + repositoryData.id + "'");
            printWriter.println("       url = '" + repositoryData.url + "'");
            printWriter.println("    }");
        }));
        printWriter.println("}");
        printWriter.println();
        printWriter.println("dependencies {");
        if (compileRootProject) {
            printWriter.println("    " + "compile rootProject");
        }
        dependencies.forEach(((dependencyData, scope) -> printWriter.println("    " + scope + " " +
                "'" +
                dependencyData.groupId + ":" +
        dependencyData
                .artifactId + ":" + dependencyData.version + "'")));
        printWriter.println("}");
        return stringWriter.toString();
    }

}
