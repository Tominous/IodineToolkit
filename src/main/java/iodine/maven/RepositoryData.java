package iodine.maven;

import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;

public class RepositoryData {

    public static final RepositoryData MOJANG_REPOSITORY = new RepositoryData("minecraft",
            "https://libraries.minecraft.net/");

    public final String id;
    public final String url;

    public RepositoryData(String id, String url) {
        this.id = id;
        this.url = url;
    }

    void write(Model model) {
        Repository repository = new Repository();
        repository.setId(this.id);
        repository.setUrl(this.url);
        model.addRepository(repository);
    }

}
