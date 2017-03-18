package iodine.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

public class DependencyData {

    public static final DependencyData[] DEFAULT_DEPENDENCIES = new DependencyData[] {
            new DependencyData("it.unimi.dsi", "fastutil", "7.1.0"),
            new DependencyData("javax.annotation", "javax.annotation-api", "1.2"),
            new DependencyData("io.netty", "netty-all", "4.0.36.Final"),
            new DependencyData("com.mojang", "authlib", "1.5.17"),
            new DependencyData("com.google.code.gson", "gson", "2.6.2"),
            new DependencyData("com.google.guava", "guava", "19.0"),
            new DependencyData("org.apache.commons", "commons-lang3", "3.4"),
            new DependencyData("commons-io", "commons-io", "2.5"),
            new DependencyData("org.apache.logging.log4j", "log4j-api", "2.0beta9"),
            new DependencyData("org.apache.logging.log4j", "log4j-core", "2.0beta9")
    };

    public final String groupId;
    public final String artifactId;
    public final String version;

    public DependencyData(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    void write(Model model) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        model.addDependency(dependency);
    }

}
