package iodine.maven;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GradleScriptBuilder {

    private String groupId = null;
    private String version = null;

    private final Map<String, DependencyData> dependencies = new HashMap<>();
    private final List<RepositoryData> repositories = new ArrayList<>();

    private final List<String> plugins = new ArrayList<>();

    public void addDependency(String scope, DependencyData dependencyData) {
        dependencies.put(scope, dependencyData);
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

    public GradleScriptBuilder importPom(POMData pomData) {
        pomData.getRepositories().forEach(this::addRepository);
        pomData.getDependencies().forEach(this::addCompileDependency);
        this.groupId = pomData.getGroupId();
        this.version = pomData.getVersion();
        return this;
    }

    public String create() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        printWriter.println("group = '" + this.groupId + "'");
        printWriter.println("version = '" + this.version + "'");
        plugins.forEach((pluginName) -> printWriter.println("apply plugin: '" + pluginName + "'"));
        printWriter.println("repositories {");
        repositories.forEach((repositoryData -> {
            printWriter.println("maven {");
            printWriter.println("   name = '" + repositoryData.id + "'");
            printWriter.println("   url = '" + repositoryData.url + "'");
            printWriter.println("}");
        }));
        printWriter.println("}");
        printWriter.println("dependencies {");
        dependencies.forEach(((scope, dependencyData) -> {
            printWriter.println(scope + " " + "'" + dependencyData.groupId + ":" + dependencyData
                    .artifactId + ":" + dependencyData.version + "'");
        }));
        printWriter.println("}");
        return stringWriter.toString();
    }

}
