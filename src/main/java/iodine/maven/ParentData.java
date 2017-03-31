package iodine.maven;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.io.FileReader;

public class ParentData {

    private Parent parent;

    public ParentData(File parentPom) {
        try {
            parent = new Parent();
            Model model = new MavenXpp3Reader().read(new FileReader(parentPom));
            parent.setVersion(model.getVersion());
            parent.setArtifactId(model.getArtifactId());
            parent.setGroupId(model.getGroupId());
        } catch (Exception e) {
            parent = null;
        }
    }

    public Parent getParent() {
        return parent;
    }

    void write(Model model) {
        if (parent == null) {
            return;
        }
        model.setParent(parent);
    }

}
