import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.RecursiveTask;

public class LinkParserTask extends RecursiveTask<List<String>> {

    private final String url;
    private final int depth;
    private static final String DOMAIN = "https://www.seznam.cz";
    public static final Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());
    public static final int SLEEP_MIN = 100;
    public static final int SLEEP_MAX = 150;

    public LinkParserTask(String url, int depth) {
        this.url = url;
        this.depth = depth;
    }

    @Override
    protected List<String> compute() {
        List<String> result = new ArrayList<>();

        if (!visitedUrls.add(url)) {
            return result;
        }

        result.add("\t".repeat(depth) + url);

        try {
            delay();

            Document doc = fetchDocumentWithRetries(url, 3);
            if (doc == null) {
                return result;
            }

            Elements links = doc.select("a[href]");
            List<LinkParserTask> subTasks = new ArrayList<>();

            for (Element link : links) {
                String linkUrl = link.absUrl("href");

                if (isValidUrl(linkUrl)) {
                    LinkParserTask task = new LinkParserTask(linkUrl, depth + 1);
                    subTasks.add(task);
                }
            }

            invokeAll(subTasks);
            subTasks.forEach(task -> result.addAll(task.join()));

        } catch (Exception e) {
            System.err.println("Error fetching URL: " + url);
            e.printStackTrace();
        }

        return result;
    }

    private Document fetchDocumentWithRetries(String url, int maxRetries) throws InterruptedException {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                return Jsoup.connect(url).get();
            } catch (SocketTimeoutException e) {
                attempt++;
                System.err.println("Warning: Read timeout for URL: " + url + ". Retrying " + attempt + "/" + maxRetries);
                delay();
            } catch (UnsupportedMimeTypeException e) {
                System.err.println("Warning: Skipping URL due to unhandled content type: " + e.getMimeType());
                return null;
            } catch (HttpStatusException e) {
                System.err.println("Warning: Skipping URL due to HTTP error: " + e.getStatusCode() + ", URL: " + e.getUrl());
                return null;
            } catch (IOException e) {
                System.err.println("Error fetching URL: " + url);
                e.printStackTrace();
            }
        }
        return null;
    }

    private void delay() throws InterruptedException {
        Thread.sleep(new Random().nextInt(SLEEP_MAX - SLEEP_MIN) + SLEEP_MIN);
    }

    private boolean isValidUrl(String url) {
        return url.startsWith(DOMAIN) && !url.contains("#") && !url.contains("?");
    }
}