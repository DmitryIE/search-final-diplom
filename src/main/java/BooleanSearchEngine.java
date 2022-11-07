import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;

public class BooleanSearchEngine implements SearchEngine {

    private Map<String, List<PageEntry>> allWordsMap = new HashMap<>();
    private Set<String> stopWords = new TreeSet<>();
    private static final String stopWordsFileName = "stop-ru.txt";

    public BooleanSearchEngine(File pdfsDir) throws Exception {
        indexingWords(pdfsDir);
        indexingStopWords(stopWordsFileName);
    }

    public Map<String, List<PageEntry>> getAllWordsMap() {
        return allWordsMap;
    }

    public Set<String> getStopWords() {
        return stopWords;
    }

    public void indexingStopWords(String stopWordsFileName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(stopWordsFileName));
        while (reader.ready()) {
            stopWords.add(reader.readLine().toLowerCase());
        }
    }

    public void indexingWords(File pdfsDir) throws Exception {
        File[] allFiles = pdfsDir.listFiles();
        for (File file : allFiles) {
            var doc = new PdfDocument(new PdfReader(file));
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                var text = PdfTextExtractor.getTextFromPage(doc.getPage(i));
                var words = text.toLowerCase().split("\\P{IsAlphabetic}+");
                Map<String, Integer> wordsOnPageCount = countingWordsOnPage(words);
                String pdfFileName = file.getName();
                for (String word : wordsOnPageCount.keySet()) {
                    int page = i;
                    int matches = wordsOnPageCount.get(word);
                    PageEntry pageEntry = new PageEntry(pdfFileName, page, matches);
                    if (allWordsMap.containsKey(word)) {
                        allWordsMap.get(word).add(pageEntry);
                    } else {
                        List<PageEntry> entryList = new ArrayList<>();
                        entryList.add(pageEntry);
                        allWordsMap.put(word, entryList);
                    }
                }
            }
        }
    }

    public Map<String, Integer> countingWordsOnPage(String[] words) {
        Map<String, Integer> map = new HashMap<>();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            map.put(word, map.getOrDefault(word, 0) + 1);
        }
        return map;
    }

    @Override
    public List<PageEntry> search(String word) {

        List<String> wordsForSearching = Arrays.stream(word.toLowerCase().split("\\P{IsAlphabetic}+"))
                .filter(s -> !stopWords.contains(s))
                .collect(Collectors.toList());

        List<PageEntry> result = new ArrayList<>();

        // добавляем все PageEntry по встречающимся словам в wordsForSearching в result
        for (String value : wordsForSearching) {
            allWordsMap.entrySet()
                    .stream()
                    .filter(s -> s.getKey().equals(value))
                    .flatMap(s -> s.getValue().stream())
                    .sorted(Comparator.comparing(PageEntry::getPdfName)
                            .thenComparing(PageEntry::getPage))
                    .forEach(s -> result.add(s));
        }

        // объединяем данные по кол-ву повторений слов по PageEntry с одинаковыми названиями файлов
        // и номерами страниц
        for (int i = result.size() - 1; i > 0; i--) {
            if (result.get(i).getPdfName().equals(result.get(i - 1).getPdfName())
                    && result.get(i).getPage() == result.get(i - 1).getPage()) {
                result.set(i - 1,
                        new PageEntry(result.get(i).getPdfName(),
                                result.get(i).getPage(),
                                result.get(i).getCount() + result.get(i - 1).getCount()));
                result.remove(i);
            }
        }
        return result.stream().sorted().collect(Collectors.toList());
    }
}
