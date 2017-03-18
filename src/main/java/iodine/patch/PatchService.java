package iodine.patch;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PatchService {

    private final File patchDirectory;
    private final File sourceDirectory;
    private final Git git;

    public PatchService(File patchDirectory, File sourceDirectory) {
        this.patchDirectory = patchDirectory;
        this.sourceDirectory = sourceDirectory;
        if (!hasGitFolder()) {
            try {
                this.git = Git.init().setDirectory(sourceDirectory).call();
                git.add().addFilepattern(".").call();
                git.commit().setMessage("src").call();
            } catch (GitAPIException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                this.git = Git.open(sourceDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void generatePatches() throws Exception {
        git.add().addFilepattern(".").call();
        git.commit().setMessage("generate patches").call();
        DiffFormatter formatter = new DiffFormatter(null);
        formatter.setRepository(git.getRepository());
        formatter.setDiffComparator(RawTextComparator.DEFAULT);
        formatter.setDetectRenames(true);
        List<DiffEntry> entries = formatter.scan(getSrcCommit().getTree(), getHead());
        getChangedFiles().forEach((path) -> {
            for (DiffEntry entry : entries) {
                Path diffPath = Paths.get(entry.getPath(DiffEntry.Side.NEW));
                if (diffPath.equals(path)) {
                    File patchFile = new File(this.patchDirectory, diffPath.getFileName() + ".patch");
                    try {
                        DiffFormatter diffFormatter = new DiffFormatter(new FileOutputStream
                                (patchFile));
                        diffFormatter.setRepository(git.getRepository());
                        diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
                        diffFormatter.setContext(3);
                        diffFormatter.format(entry);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void applyPatches() throws Exception {
        git.reset().setMode(ResetCommand.ResetType.HARD).setRef(getSrcCommit().getName()).call();
        File[] patchFiles = patchDirectory.listFiles(((dir, name) -> name.endsWith(".patch"))
        ) != null ? patchDirectory.listFiles(((dir, name) -> name.endsWith(".patch"))) : new
                File[0];
        for (File patchFile : patchFiles) {
            git.apply().setPatch(new FileInputStream(patchFile)).call();
        }
    }

    public InputStream getOriginalFile(Path relativeFilePath) throws IOException {
        RevTree branch = getSrcCommit().getTree();
        TreeWalk fileTree = new TreeWalk(git.getRepository());
        fileTree.addTree(branch);
        fileTree.setRecursive(true);
        fileTree.setFilter(PathFilter.create(relativeFilePath.toString()));
        if (!fileTree.next()) {
            throw new FileNotFoundException("File not found!");
        }
        ObjectId fileId = fileTree.getObjectId(0);
        ObjectLoader commitFileLoader = git.getRepository().open(fileId);
        return commitFileLoader.openStream();
    }

    public List<String> getOriginalFileLines(Path relativeFilePth) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getOriginalFile
                (relativeFilePth)));
        return bufferedReader.lines().collect(Collectors.toList());
    }

    public String getOriginalFileString(Path relativePath) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        getOriginalFileLines(relativePath).forEach(string -> stringBuilder.append(string).append
                (System.lineSeparator()));
        return stringBuilder.toString();
    }

    public Git getGitAccess() {
        return git;
    }

    public File getPatchDirectory() {
        return patchDirectory;
    }

    public File getSourceDirectory() {
        return sourceDirectory;
    }

    public RevCommit getSrcCommit() {
        try {
            RevWalk revWalk = new RevWalk(git.getRepository());
            AnyObjectId headId = git.getRepository().resolve(Constants.HEAD);
            RevCommit root = revWalk.parseCommit(headId);
            revWalk.sort(RevSort.REVERSE);
            revWalk.markStart(root);
            return revWalk.next();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RevCommit getHead() throws IOException {
        RevWalk revWalk = new RevWalk(git.getRepository());
        AnyObjectId headId = git.getRepository().resolve(Constants.HEAD);
        RevCommit head = revWalk.parseCommit(headId);
        return head;
    }

    public List<Path> getChangedFiles() throws IOException {
        List<Path> changedFiles = new ArrayList<>();
        RevWalk revWalk = new RevWalk(git.getRepository());
        AnyObjectId headId = git.getRepository().resolve(Constants.HEAD);
        RevCommit head = revWalk.parseCommit(headId);
        DiffFormatter formatter = new DiffFormatter(null);
        formatter.setRepository(git.getRepository());
        formatter.setDiffComparator(RawTextComparator.DEFAULT);
        formatter.setDetectRenames(true);
        List<DiffEntry> entries = formatter.scan(getSrcCommit().getTree(), head);
        entries.forEach((diffEntry -> changedFiles.add(Paths.get(diffEntry.getPath(DiffEntry.Side.NEW)))));
        return changedFiles;
    }

    private boolean hasGitFolder() {
        if (new File(sourceDirectory, ".git").exists()) {
            return true;
        }
        return false;
    }
}
