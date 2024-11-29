package DuplicateFinder;

import java.util.Set;

public class Main {
    public static void main(String[] args) {
        DuplicateFinder duplicateFinder = new DuplicateFinder();

        // Загрузка текстов из файлов
        String directoryPath = "./input";
        duplicateFinder.loadTexts(directoryPath);

        // Канонизация текстов
        Set<String> stopWords = Set.of("и", "на", "в", "с"); // Пример стоп-слов
        duplicateFinder.canonicalizeTexts(stopWords);

        // Построение шинглов
        int shingleSize = 2;
        duplicateFinder.buildShinglesForAllTexts(shingleSize);

        // Вычисление MinHash
        int hashFunctions = 100;
        duplicateFinder.computeMinHashesForAllTexts(hashFunctions);

        // Поиск дубликатов
        duplicateFinder.findDuplicates();
    }
}
