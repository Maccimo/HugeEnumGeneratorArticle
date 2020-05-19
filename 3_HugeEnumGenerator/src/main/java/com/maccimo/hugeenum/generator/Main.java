package com.maccimo.hugeenum.generator;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {

    private static final String DEFAULT_ALGORITHM_NAME = "ExtractMethod";

    private static final String FILE_SUFFIX_CLASS = ".class";
    private static final String MEMBER_NAME_FORMAT = "VALUE_%05d";

    @Argument(metaVar = "<enum name>", required = true)
    private String enumName;

    @Option(name = "-d", metaVar = "<directory>")
    private String outputPath;

    @Option(name = "-e", forbids = "-c", metaVar = "<item list file>")
    private String itemListPath;

    @Option(name = "-c", forbids = "-e", metaVar = "<count>")
    private Integer fieldCount;

    @Option(name = "-a", metaVar = "<algorithm>")
    private String algorithmName;

    @Option(name = "-h", aliases = { "-?" }, help = true)
    private boolean showHelp;

    public static void main(String... args) throws Exception {

        Main instance = new Main();

        instance.run(args);

    }

    private void run(String[] args) throws Exception {
        try {
            CmdLineParser parser = new CmdLineParser(this);

            parser.parseArgument(args);

            if (showHelp) {
                showHelp();
                System.exit(0);
            } else {
                assert (enumName != null) : "Enum name not initialized";

                String[] enumNameParts = enumName.split("\\.");

                if ((enumNameParts.length < 1) || !Stream.of(enumNameParts).allMatch(Main::isValidJavaIdentifier)) {
                    error("Invalid enum name: " + enumName);
                }

                String enumClassFileName = enumNameParts[enumNameParts.length - 1] + FILE_SUFFIX_CLASS;
                String binaryEnumClassName = String.join("/", enumNameParts);

                Path outputFilePath = Paths.get(enumClassFileName);
                if (outputPath != null) {
                    Path outputDirectoryPath = Paths.get(outputPath);

                    if (Files.exists(outputDirectoryPath)) {
                        outputFilePath = Paths.get(outputPath, enumClassFileName);
                    } else {
                        error("Invalid output directory path: " + outputPath);
                    }
                }

                if (algorithmName == null) {
                    algorithmName = DEFAULT_ALGORITHM_NAME;
                }

                IEnumGeneratorFactory enumGeneratorFactory = EnumGeneratorRegistry.INSTANCE.getById(algorithmName);

                if (enumGeneratorFactory == null) {
                    error("Unknown algorithm: " + algorithmName);
                } else {
                    List<String> elementNames;
                    if (itemListPath != null) {

                        Path path = Paths.get(itemListPath);

                        if (!Files.exists(path)) {
                            error(String.format("Item list file '%s' not found!", path));
                        }

                        elementNames = Files
                            .lines(path)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());

                        List<String> invalidItems = elementNames
                            .stream()
                            .filter(item -> !isValidJavaIdentifier(item))
                            .collect(Collectors.toList());

                        if (!invalidItems.isEmpty()) {
                            System.err.println("Invalid element names encountered:");
                            for (String invalidItem : invalidItems) {
                                System.err.println("\t" + invalidItem);
                            }
                            System.exit(-1);
                        }
                    } else {
                        if (fieldCount == null) {
                            fieldCount = enumGeneratorFactory.getDefaultElementCount();
                        }

                        elementNames = generateElementNames(fieldCount);
                    }

                    if (elementNames.size() > enumGeneratorFactory.getMaximumElementCount()) {
                        warning(String.format(
                            "Enum elements count (%d) exceeds maximum (%d), supported by selected algorithm (%s)",
                            elementNames.size(), enumGeneratorFactory.getMaximumElementCount(), algorithmName
                        ));
                    }

                    System.out.printf("Generating enum %s...%n", enumName);
                    System.out.printf("Algorithm used: %s%n", algorithmName);
                    System.out.printf("Element count: %d%n", elementNames.size());

                    IEnumGenerator enumGenerator = enumGeneratorFactory.create(binaryEnumClassName, elementNames);

                    System.out.printf("Writing file %s%n", outputFilePath);

                    byte[] classBytes = enumGenerator.generate();

                    Files.write(outputFilePath, classBytes);

                    System.out.println("Done.");
                }
            }
        } catch (CmdLineException e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println();

            showHelp();
            System.exit(-1);
        }
    }

    private static void error(String message) {
        System.err.print("Error: ");
        System.err.println(message);
        System.exit(-1);
    }

    private static void warning(String message) {
        System.out.println(message);
    }

    private static boolean isValidJavaIdentifier(String identifier) {
        return
            !identifier.isEmpty() &&
            Character.isJavaIdentifierStart(identifier.codePointAt(0)) &&
            identifier
                .codePoints()
                .skip(1)
                .allMatch(Character::isJavaIdentifierPart);
    }

    private static List<String> generateElementNames(int fieldCount) {
        return IntStream
            .range(1, fieldCount + 1)
            .mapToObj(number -> String.format(MEMBER_NAME_FORMAT, number))
            .collect(Collectors.toList());
    }

    private static void showHelp() {
        System.out.println();
        System.out.println("Huge enumeration generator");
        System.out.println();
        System.out.println("    https://github.com/Maccimo/HugeEnumGeneratorArticle");
        System.out.println();
        System.out.println("Additional information (in Russian):");
        System.out.println();
        System.out.println("    https://habr.com/ru/post/483392/");
        System.out.println("    https://habr.com/ru/post/501870/");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("    java -jar HugeEnumGen.jar [ <options> ] <enum name>");
        System.out.println();
        System.out.println("    <enum name>");
        System.out.println("        An enumeration class name.");
        System.out.println("        Should be a valid Java identifier. May contain package name.");
        System.out.println();
        System.out.println("Options:");
        System.out.println();
        System.out.println("    -d <directory>");
        System.out.println("        Output directory path.");
        System.out.println("        Current working directory by default.");
        System.out.println();
        System.out.println("    -e <item list file>");
        System.out.println("        Path to UTF8-encoded text file with list of enumeration item names.");
        System.out.println("        Item names will be autogenerated if absent.");
        System.out.println("        Mutually exclusive with the -c option.");
        System.out.println();
        System.out.println("    -c <count>");
        System.out.println("        Count of autogenerated enumeration item names.");
        System.out.println("        Mutually exclusive with the -e option.");
        System.out.println("        Default value: Algorithm-depended");
        System.out.println();
        System.out.println("    -a <algorithm>");
        System.out.println("        Enumeration generation algorithm.");
        System.out.println("        Supported algorithms:");

        printSupportedAlgorithms();

        System.out.println();
        System.out.println("        Default algorithm: " + DEFAULT_ALGORITHM_NAME);
        System.out.println();
        System.out.println("    -h / -?");
        System.out.println("        Show this help page.");
        System.out.println();
        System.out.println("Example:");
        System.out.println();
        System.out.println("    java -jar HugeEnumGen.jar -d ./bin -c 2020 com.habr.maccimo.HugeEnum2020");
        System.out.println();
    }

    private static void printSupportedAlgorithms() {
        Map<String, String> idAndDescription = EnumGeneratorRegistry.INSTANCE.getFactories()
            .stream()
            .collect(Collectors.toMap(IEnumGeneratorFactory::getId, IEnumGeneratorFactory::getDescription));

        int maxLen = idAndDescription
            .keySet()
            .stream()
            .mapToInt(String::length)
            .max()
            .orElse(0);

        String format = "          %" + (maxLen == 0 ? "" : "-" + maxLen) + "s  - %s%n";

        String message = idAndDescription
            .entrySet()
            .stream()
            .map(entry -> String.format(format, entry.getKey(), entry.getValue()))
            .collect(Collectors.joining());

        System.out.print(message);
    }

}
