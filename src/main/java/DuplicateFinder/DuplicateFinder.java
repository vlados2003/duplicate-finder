package DuplicateFinder;

import java.io.IOException;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

import Entities.FileData;
import org.apache.commons.codec.digest.MurmurHash3;

public class DuplicateFinder {
    private static final Logger LOGGER = Logger.getLogger(DuplicateFinder.class.getName());

    // Хранение файловых данных: ключ — относительный путь, значение — данные файла
    private final Map<String, FileData> fileDataMap = new HashMap<>();

    // Загрузка текстов из файлов
    public void loadTexts(String directoryPath) {
        Path basePath = Paths.get(directoryPath);
        try (Stream<Path> paths = Files.walk(basePath)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".txt"))
                    .forEach(path -> readFileContent(basePath, path));
        } catch (IOException e) {
            LOGGER.severe(String.format("Ошибка при обходе файловой системы: %s", e.getMessage()));
        }
    }

    private void readFileContent(Path basePath, Path path) {
        try {
            String relativePath = basePath.relativize(path).toString();
            String content = Files.readString(path);
            fileDataMap.put(relativePath, new FileData(content));
        } catch (IOException e) {
            LOGGER.severe(String.format("Ошибка чтения файла %s: %s", path, e.getMessage()));
        }
    }

    // Канонизация текста
    private static String canonicalize(String text, Set<String> stopWords) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Arrays.stream(Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                        .split("\\s+"))
                .filter(word -> !stopWords.contains(word))
                .reduce((w1, w2) -> w1 + " " + w2)
                .orElse("")
                .trim();
    }

    public void canonicalizeTexts(Set<String> stopWords) {
        fileDataMap.values().forEach(fileData -> {
            String canonicalizedText = canonicalize(fileData.getText(), stopWords);
            fileData.setText(canonicalizedText);
        });
    }

    // Построение шинглов
    public void buildShinglesForAllTexts(int shingleSize) {
        fileDataMap.values().forEach(fileData -> {
            Set<String> shingles = buildShingles(fileData.getText(), shingleSize);
            fileData.setShingles(shingles);
        });
    }

    private Set<String> buildShingles(String text, int shingleSize) {
        if (text.isEmpty() || shingleSize <= 0) {
            throw new IllegalArgumentException("Шингл должен быть положительным и текст не пустым.");
        }

        Set<String> shingles = new HashSet<>();
        String[] words = text.split(" ");
        if (words.length < shingleSize) {
            return shingles; // Вернуть пустой набор, если текст слишком короткий
        }

        for (int i = 0; i <= words.length - shingleSize; i++) {
            shingles.add(String.join(" ", Arrays.copyOfRange(words, i, i + shingleSize)));
        }
        return shingles;
    }

    // Вычисление MinHash
    public void computeMinHashesForAllTexts(int numHashes) {
        fileDataMap.values().forEach(fileData -> {
            int[] minHash = computeMinHash(fileData.getShingles(), numHashes);
            fileData.setMinHash(minHash);
        });
    }

    private int[] computeMinHash(Set<String> shingles, int numHashes) {
        int[] minHashes = new int[numHashes];
        Arrays.fill(minHashes, Integer.MAX_VALUE);

        for (String shingle : shingles) {
            byte[] shingleBytes = shingle.getBytes();
            for (int i = 0; i < numHashes; i++) {
                int hash = MurmurHash3.hash32x86(shingleBytes, 0, shingleBytes.length, i);
                minHashes[i] = Math.min(minHashes[i], hash);
            }
        }
        return minHashes;
    }

    // Сравнение MinHash
    public void compareHashes() {
        List<String> relativePaths = new ArrayList<>(fileDataMap.keySet());
        for (int i = 0; i < relativePaths.size(); i++) {
            for (int j = i + 1; j < relativePaths.size(); j++) {
                compareFileHashes(relativePaths.get(i), relativePaths.get(j));
            }
        }
    }

    private void compareFileHashes(String path1, String path2) {
        double similarity = calculateSimilarity(
                fileDataMap.get(path1).getMinHash(),
                fileDataMap.get(path2).getMinHash()
        );
        System.out.printf("Similarity between \"%s\" and \"%s\": %.2f%%%n", path1, path2, similarity * 100);
    }

    private double calculateSimilarity(int[] hash1, int[] hash2) {
        int identicalHashes = 0;
        for (int i = 0; i < hash1.length; i++) {
            if (hash1[i] == hash2[i]) {
                identicalHashes++;
            }
        }
        return (double) identicalHashes / hash1.length;
    }

    // Поиск дубликатов
    public void findDuplicates() {
        compareHashes();
    }
}