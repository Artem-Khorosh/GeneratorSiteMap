import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ForkJoinPool;


public class SiteMapGenerator {
    public static final String START_URL = "https://www.seznam.cz";
    public static final String OUTPUT_FILE = "site_map.txt";

    public static void main(String[] args) {
        ForkJoinPool pool = new ForkJoinPool();

        LinkParserTask rootTask = new LinkParserTask(START_URL, 0);
        List<String> siteMap = pool.invoke(rootTask);

        try {
            Files.write(Paths.get(OUTPUT_FILE), siteMap);
            System.out.println("SITE saved: " + OUTPUT_FILE);
        } catch (IOException e) {
            System.err.println("ERROR : " + OUTPUT_FILE);
            e.printStackTrace();
        }
    }
}
