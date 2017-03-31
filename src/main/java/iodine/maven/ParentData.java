package iodine.maven;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.io.FileReader;

public class ParentData {

    private final Parent parent = new Parent();

    public ParentData(File parentPom) {
        try {
            Model model = new MavenXpp3Reader().read(new FileReader(parentPom));
            parent.setVersion(model.getVersion());
            parent.setArtifactId(model.getArtifactId());
            parent.setGroupId(model.getGroupId());
        } catch (Exception e) {

        }
    }

    void write(Model model) {
        model.setParent(parent);
    }

}
