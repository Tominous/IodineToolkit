package iodine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.function.Predicate;
import java.util.zip.ZipFile;

public class ZipExtractor {

    private final Predicate<String> compatibleNamePredicate;
    private final ZipFile zipFile;
    private final File file;

    public ZipExtractor(Predicate<String> compatibleNamePredicate, File file) throws IOException {
        this.compatibleNamePredicate = compatibleNamePredicate;
        zipFile = new ZipFile(file);
        this.file = file;
    }

    public void extract(File outputDirectory, boolean deleteFile) {
        zipFile.stream().forEach((zipEntry) -> {
            if (!zipEntry.isDirectory() && compatibleNamePredicate.test(zipEntry.getName())) {
                File outputFile = new File(outputDirectory.getAbsolutePath() + File.separator +
                        zipEntry
                        .getName());
                outputFile.getParentFile().mkdirs();
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                    ReadableByteChannel streamChannel = Channels.newChannel(zipFile
                            .getInputStream(zipEntry));
                    fileOutputStream.getChannel().transferFrom(streamChannel, 0, Long.MAX_VALUE);
                    streamChannel.close();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        });
        try {
            zipFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.gc();
        if (deleteFile) {
            if (!file.delete()) {
                throw new RuntimeException("Failed to delete.");
            }
        }
    }


}
