import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

public class Main {
    private static final String URL_STRING = "https://your-site.ru"; // Замените на нужный сайт
    private static final ConcurrentHashMap<String, Boolean> visitedLinks = new ConcurrentHashMap<>();
    private static final String SITEMAP_FILENAME = "Multithreading/ForkJoinPool/sitemap.txt"; // Обновленный путь

    public static void main(String[] args) {
        // Инициализация корня дерева
        Node root = new Node(null, URL_STRING);

        // Инициализируем ForkJoinPool
        ForkJoinPool pool = new ForkJoinPool();

        // Запускаем рекурсивный процесс сбора ссылок
        RecursiveCrawler crawler = new RecursiveCrawler(root);
        pool.invoke(crawler);

        // Сохраняем результат в файл
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(SITEMAP_FILENAME)))) {
            String siteMapContent = root.toString();
            if (siteMapContent.isEmpty()) {
                System.err.println("Карта сайта пуста. Возможно, не удалось собрать ссылки.");
            } else {
                writer.write(siteMapContent);
                System.out.println("Карта сайта успешно сохранена в файл: " + SITEMAP_FILENAME);
                System.out.println("Количество ссылок: " + visitedLinks.size());
            }
        } catch (IOException e) {
            System.err.println("Ошибка при записи в файл: " + e.getMessage());
        }
    }

    // Метод для получения всех ссылок на странице
    private static List<String> getLinks(String url) {
        try {
            Thread.sleep(100 + (long) (Math.random() * 50)); // Пауза от 100 до 150 мс
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Поток был прерван", e);
        }

        Document doc;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException e) {
            System.err.println("Ошибка при запросе страницы: " + url + " - " + e.getMessage());
            // Записываем ошибочный URL в файл
            writeErrorUrlToFile(url);
            return new ArrayList<>();
        }

        return doc.select("a")
                .stream()
                .map(element -> {
                    String link = element.attr("href");
                    if (link.startsWith("/") && link.length() > 1) {
                        link = URL_STRING + link;
                    }
                    return link;
                })
                .filter(link -> link.startsWith(URL_STRING) && !link.contains("#"))
                .collect(Collectors.toList());
    }

    // Метод для записи ошибочного URL в файл
    private static void writeErrorUrlToFile(String url) {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(SITEMAP_FILENAME),
                java.nio.file.StandardOpenOption.APPEND))) {
            writer.println(url);
        } catch (IOException e) {
            System.err.println("Ошибка при записи ошибочного URL в файл: " + e.getMessage());
        }
    }

    // Рекурсивная задача для обхода ссылок
    private static class RecursiveCrawler extends RecursiveTask<Void> {
        private final Node currentNode;

        public RecursiveCrawler(Node currentNode) {
            this.currentNode = currentNode;
        }

        @Override
        protected Void compute() {
            String currentUrl = currentNode.getValue();
            if (visitedLinks.containsKey(currentUrl)) {
                return null; // Если ссылка уже посещена, пропускаем
            }
            visitedLinks.put(currentUrl, true); // Помечаем ссылку как посещённую

            List<String> links = getLinks(currentUrl);

            List<RecursiveCrawler> subtasks = new ArrayList<>();
            for (String link : links) {
                if (!visitedLinks.containsKey(link)) {
                    Node childNode = currentNode.addChildIfAbsent(link);
                    if (childNode != null) {
                        RecursiveCrawler task = new RecursiveCrawler(childNode);
                        subtasks.add(task);
                        task.fork();
                    }
                }
            }

            for (RecursiveCrawler task : subtasks) {
                task.join();
            }
            return null;
        }
    }
}
