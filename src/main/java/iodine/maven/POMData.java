package iodine.maven;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

public class POMData {

    private final String groupId;
    private final String artifactId;
    private final String version;

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getVersion() {
        return version;
    }

    private final List<DependencyData> dependencies;
    private final List<RepositoryData> repositories;

    public List<DependencyData> getDependencies() {
        return dependencies;
    }

    public List<RepositoryData> getRepositories() {
        return repositories;
    }

    private final String xml;

    public String getXml() {
        return xml;
    }

    public POMData(String groupId, String artifactId, String versionId, List<DependencyData>
            dependencies, List<RepositoryData> repositories) throws IOException {
        Model model = new Model();
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(versionId);
        model.setModelVersion("4.0.0");
        dependencies.forEach((dependencyData -> dependencyData.write(model)));
        repositories.forEach((repositoryData -> repositoryData.write(model)));
        this.dependencies = dependencies;
        this.repositories = repositories;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = versionId;
        StringWriter writer = new StringWriter();
        new MavenXpp3Writer().write(writer, model);
        this.xml = writer.toString();
    }

    public static void main(String... args) throws Exception {
        System.out.println(new POMData("meow", "meow", "meow", Collections.emptyList(),
                Collections.emptyList()));
    }

    @Override
    public String toString() {
        return this.xml;
    }

}
