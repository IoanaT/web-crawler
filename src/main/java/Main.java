import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class Main {

    public static void main(String[] args) {

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Please input a search string: ");
            String searchTerm = scanner.nextLine();
            searchTerm = searchTerm.replaceAll(" ", "+");

            HttpRequest googleSearchRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.google.com/search?q=" + searchTerm))
                    .setHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36")
                    .setHeader("accept-language", " en-US,en;q=0.9,de;q=0.8,ro;q=0.7")
                    .GET()
                    .build();
            WebCrawler webCrawler = new WebCrawler();
            HttpResponse<String> response = webCrawler.getHttpClient().send(googleSearchRequest, HttpResponse.BodyHandlers.ofString());

            LinkedList<String> links = webCrawler.extractLinksFromPage(response.body());

            List<String> allLibraries = webCrawler.downloadLinksAndParseJsLibraries(links, webCrawler.getHttpClient());
            Set<JsLibrary> uniqueLibraries = webCrawler.deduplicateJsLibraries(allLibraries);
            webCrawler.printTopFiveMostUsedJsLibraries(allLibraries);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
