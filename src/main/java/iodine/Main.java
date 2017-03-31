package iodine;

import net.md_5.specialsource.SpecialSource;
import net.techcable.srglib.FieldData;
import net.techcable.srglib.MethodData;
import net.techcable.srglib.format.MappingsFormat;
import net.techcable.srglib.mappings.Mappings;

import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import iodine.maven.DependencyData;
import iodine.maven.GradleScriptBuilder;
import iodine.maven.POMData;
import iodine.maven.ParentData;
import iodine.maven.RepositoryData;
import iodine.obf.CSVReader;
import iodine.obf.SRGReader;
import iodine.patch.PatchService;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import static java.util.Arrays.asList;

public class Main {

    private static void recursiveDelete(File file) {
        if (!file.isDirectory()) {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return;
        }
        try {
            Files.walk(file.toPath()).sorted(Comparator.reverseOrder()).forEach((path) -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        file.delete();
    }

    public static void main(String... args) throws Exception {
        OptionParser parser = new OptionParser();
        OptionSpec<Integer> mappingVersion = parser.acceptsAll(asList("mpv", "mappingsversion",
                "mappingversion", "mv", "mversion"), "Sets the mapping's version for MCPBot.")
                .withRequiredArg().ofType(Integer.class).required();
        OptionSpec<String> minecraftVersion = parser.acceptsAll(asList("mcv", "minecraftversion",
         "mcversion"), "Sets the mapping's minecraft version for MCPBot and Forge.")
                .withRequiredArg().ofType(String.class).required();
        OptionSpec<String> releaseType = parser.acceptsAll(asList("reltype", "r", "rt",
                "releasetype", "releaset", "rtype"), "Sets the mapping's release type.")
                .withRequiredArg().ofType
                (String.class)
                .required();
        OptionSpec<Void> help = parser.acceptsAll(asList("?", "help", "h"), "Shows the help menu" +
                ".").forHelp();
        OptionSpec<File> brokenSrg = parser.acceptsAll(asList("brokensrg", "joinedsrg", "jsrg",
                "bsrg"), "Manually sets the joined.srg to remap using CSV.").withRequiredArg()
                .ofType(File.class).forHelp();
        OptionSpec<Void> genPatches = parser.acceptsAll(asList("genpatch", "gpatch", "gpatches",
                "genp"), "Generates patches on NMS.").forHelp();
        OptionSpec<Void> applyPatches = parser.acceptsAll(asList("apatch", "apatches",
                "applypatch", "applypatches"), "Applies patches on NMS.").forHelp();
        OptionSpec<Void> clean = parser.acceptsAll(asList("clean", "c")).forHelp();
        OptionSpec<Void> compileRootProject = parser.acceptsAll(asList("compileroot", "cr", "r",
                "root"), "When generated build.gradle, it allows to compile the root gradle " +
                "project; if exists.");
        OptionSpec<Void> noCsv = parser.acceptsAll(asList("nocsv", "nc"), "Sets mappings to " +
                "not be remapped using CSV.");
        OptionSpec<File> parentPom = parser.acceptsAll(asList("pom", "p"), "Sets the parent POM" +
                " for the generated POM.xml").withRequiredArg().ofType(File.class).forHelp();
        OptionSet parsedArgs = parser.parse(args);
        if (parsedArgs.has(help)) {
            parser.printHelpOn(System.out);
            return;
        }
        boolean shouldClean = parsedArgs.has(clean);
        System.out.println("Structuring directories...");
        File mainDir = new File("iodine-sdk");
        if (parsedArgs.has(genPatches) || parsedArgs.has(applyPatches)) {
            if (!mainDir.exists()) throw new RuntimeException("No main directory!");
            File patches = new File(mainDir, "patches");
            File srcMainJava = new File(mainDir, "src/main/java");
            if (!patches.exists()) throw new RuntimeException("No patches directory!");
            if (!srcMainJava.exists()) throw new RuntimeException("No src/main/java directory!");
            PatchService patchService = new PatchService(patches, srcMainJava);
            if (parsedArgs.has(genPatches)) {
                System.out.println("Generating patches...");
                patchService.generatePatches();
                System.out.println("Patches generated!");
                return;
            }
            System.out.println("Applying patches...");
            patchService.applyPatches();
            System.out.println("Patches applied!");
            return;
        }
        if (!mainDir.exists()) mainDir.mkdir();
        File srcMainJava = new File(mainDir, "src/main/java");
        if (shouldClean && srcMainJava.exists()) recursiveDelete(srcMainJava);
        if (srcMainJava.exists()) throw new RuntimeException("src/main/java exists! Previous " +
                "installation possible!");
        File srcMainResources = new File(mainDir, "src/main/resources");
        if (shouldClean && srcMainResources.exists()) recursiveDelete(srcMainResources);
        if (srcMainResources.exists()) throw new RuntimeException("src/main/resources exists! " +
                "Previous installation possible!");
        File pomXml = new File(mainDir, "pom.xml");
        if (shouldClean && pomXml.exists()) recursiveDelete(pomXml);
        if (pomXml.exists()) throw new RuntimeException("pom.xml exists! Previous installation " +
                "possible!");
        File buildGradle = new File(mainDir, "build.gradle");
        if (shouldClean && buildGradle.exists()) recursiveDelete(buildGradle);
        if (buildGradle.exists()) throw new RuntimeException("build.gradle exists! Previous " +
                "installation possible!");
        File settingsGradle = new File(mainDir, "settings.gradle");
        if (shouldClean && settingsGradle.exists()) recursiveDelete(settingsGradle);
        if (settingsGradle.exists()) throw new RuntimeException("settings.gradle exists! Previous" +
                " installation possible!");
        File patches = new File(mainDir, "patches");
        if (!patches.exists()) patches.mkdir();
        File bin = new File(mainDir, "bin");
        if (bin.exists() && shouldClean) recursiveDelete(bin);
        if (bin.exists()) throw new RuntimeException("bin exists! Previous installation possible!");
        bin.mkdir();
        srcMainJava.mkdirs();
        srcMainResources.mkdirs();
        System.out.println("Downloading minecraft-server...");
        File minecraftFile = new File(bin, "minecraft-server.jar");
        String minecraftUrl = "https://s3.amazonaws.com/Minecraft" +
                ".Download/versions/potato/minecraft_server.potato.jar";
        minecraftUrl = minecraftUrl.replace("potato", minecraftVersion.value(parsedArgs));
        if (!SRGReader.isValidUrl(minecraftUrl)) throw new RuntimeException("Invalid minecraft " +
                "version!");
        download(minecraftUrl, minecraftFile);
        File remappedFile = new File(bin, "minecraft-server-remapped.jar");
        copy(minecraftFile, remappedFile);
        System.out.println("Generating srg...");
        URL csv = new URL("http://export.mcpbot.bspk" +
                ".rs/mcp_" + releaseType.value(parsedArgs) +
                "/" + mappingVersion.value(parsedArgs) +
                "-" + minecraftVersion.value(parsedArgs) + "/mcp_" + releaseType
                .value(parsedArgs) + "-" + mappingVersion.value(parsedArgs) +  "-" +
                minecraftVersion.value(parsedArgs) + ".zip");
        if (minecraftVersion.value(parsedArgs).equals("1.11.2")) {
            System.out.println("Oh, your using 1.11.2! MCPBot is weird, so we're going to use a " +
                    "special format for retrieving the CSV.");
            csv = new URL("http://export.mcpbot.bspk" +
                    ".rs/mcp_" + releaseType.value(parsedArgs) +
                    "/" + mappingVersion.value(parsedArgs) +
                    "-1.11/mcp_" + releaseType
                    .value(parsedArgs) + "-" + mappingVersion.value(parsedArgs) +  "-1.11.zip");
        }
        Mappings csvMappings = CSVReader.readMCPBotPackage(csv);
        if (parsedArgs.has(noCsv)) {
            csvMappings = Mappings.createRenamingMappings((type) -> type, MethodData::getName,
                    (FieldData::getName));
        }
        Mappings brokenJoinedMappings = null;
        if (parsedArgs.has(brokenSrg)) {
            brokenJoinedMappings = new SRGReader(brokenSrg.value(parsedArgs).toURI().toURL()
                    .openStream()).getMappings();
        } else {
            brokenJoinedMappings = SRGReader.getSrgFor(minecraftVersion.value(parsedArgs));
        }
        if (brokenJoinedMappings == null) throw new RuntimeException("broken mappings nulll.");
        Mappings chainedMappings = Mappings.chain(brokenJoinedMappings, csvMappings);
        File outputSrg = new File(bin, "fixed.srg");
        MappingsFormat.SEARGE_FORMAT.writeToFile(chainedMappings, outputSrg);
        System.out.println("Remapping minecraft-server...");
        SpecialSource.main(new String[] {
                "-o", remappedFile.getAbsolutePath(),
                "-i", minecraftFile.getAbsolutePath(),
                "-m", outputSrg.getAbsolutePath(),
                "-q"
        });
        File binSrc = new File(bin, "src");
        if (!binSrc.exists()) binSrc.mkdir();
        ZipExtractor classExtractor = new ZipExtractor((name) -> name.endsWith(".class") && name
                .startsWith("net"), remappedFile);
        classExtractor.extract(binSrc, false);
        ZipExtractor assetExtractor = new ZipExtractor((name) -> !name.endsWith(".class") && !name
                .startsWith("META-INF") && !name.endsWith(".java") && !name.endsWith(".txt") &&
                !name.endsWith(".dat"),
                remappedFile);
        assetExtractor.extract(srcMainResources, false);
        System.out.println("Decompiling minecraft-server...");
        ConsoleDecompiler.main(new String[] {
                "-dgs=1",
                "-hdc=0",
                "-asc=1",
                "-udv=0",
                "-din=1",
                "-rbr=0",
                "-rsy=1",
                "-log=WARN",
                binSrc.getAbsolutePath(),
                srcMainJava.getAbsolutePath()
        });
        System.out.println("Generating POM...");
        ParentData parentData = new ParentData(null);
        if (parsedArgs.has(parentPom)) {
            parentData = new ParentData(parentPom.value(parsedArgs));
        }
        POMData pomData = new POMData(parentData, "net.minecraft.server", "nms", minecraftVersion
                .value
                (parsedArgs), Arrays.asList(DependencyData.DEFAULT_DEPENDENCIES), Collections
                .singletonList(RepositoryData.MOJANG_REPOSITORY));
        Files.write(pomXml.toPath(), pomData.getXml().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Creating build.gradle...");
        String gradleScript = new GradleScriptBuilder().importPom(pomData).setCompileRootProject
                (parsedArgs.has(compileRootProject)).create();
        Files.write(buildGradle.toPath(), gradleScript.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        System.out.println("Creating settings.gradle...");
        Files.write(settingsGradle.toPath(), "rootProject.name = 'nms'".getBytes(StandardCharsets
                .UTF_8), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        System.out.println("Creating patch service...");
        new PatchService(patches, srcMainJava).getGitAccess().close();
        System.out.println("Finished!");
    }

    private static void copy(File file, File file1) throws Exception {
        ReadableByteChannel ori = Channels.newChannel(new FileInputStream(file));
        FileOutputStream fileOutputStream = new FileOutputStream(file1);
        fileOutputStream.getChannel().transferFrom(ori, 0, Long.MAX_VALUE);
        ori.close();
        fileOutputStream.close();
    }

    private static void download(String url, File output) throws Exception {
        URL urlObject = new URL(url);
        ReadableByteChannel readableByteChannel = Channels.newChannel(urlObject.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(output);
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        readableByteChannel.close();
        fileOutputStream.close();
    }

}
