
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WebCrawler {

    private HttpClient httpClient;

    WebCrawler() {
        this.httpClient = HttpClient.newBuilder().executor(Executors.newWorkStealingPool()).build();
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public static void main(String[] args) throws Exception {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Please input a search string: ");
            String searchTerm = scanner.nextLine();
            searchTerm = searchTerm.replaceAll(" ", "+");
            System.out.println(searchTerm);

            //TODO: use Google Search API (must get API key and search engine ID)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.google.com/search?q=" + searchTerm))
                    .setHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36")
                    .setHeader("accept-language", " en-US,en;q=0.9,de;q=0.8,ro;q=0.7")
                    .GET()
                    .build();
            WebCrawler webCrawler = new WebCrawler();
            HttpResponse<String> response = webCrawler.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            LinkedList<String> links = webCrawler.extractLinksFromPage(response.body());
            System.out.println(links);

            HttpClient httpClient2 = HttpClient.newBuilder().executor(Executors.newWorkStealingPool()).build();
            List<String> allLibraries = webCrawler.downloadLinksAndParseJsLibraries(links, httpClient2);
            Set<JsLibrary> uniqueLibraries = webCrawler.deduplicateJsLibraries(allLibraries);
            webCrawler.printTopFiveMostUsedJsLibraries(allLibraries);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private HttpRequest getRequest(String targetLink) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(targetLink))
                .GET()
                .build();
    }

    public LinkedList<String> extractRegexPattern(String regex, String text) {
        LinkedList<String> links = new LinkedList<>();
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            links.add(matcher.group(2));
        }
        return links;
    }

    public LinkedList<String> extractLinksFromPage(String response) {
        LinkedList<String> links = extractRegexPattern("<div class=\"r\"><a\\s+href=([\"'])(.*?)\\1", response);
        System.out.println(links);
        System.out.println("Found " + links.size() + " links.");
        return links;
    }

    public String downloadLink(String link, HttpClient client) throws URISyntaxException, ExecutionException, InterruptedException {
        CompletableFuture<String> future = client.sendAsync(getRequest(link), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.body());
        return future.get();
    }

    public List<String> downloadLinkAndParseJsLibraries(String link, HttpClient client) throws URISyntaxException, ExecutionException, InterruptedException {
        CompletableFuture<List<String>> future = client.sendAsync(getRequest(link), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.body())
                .thenApply(page -> extractRegexPattern("<script.*src=([\\\"'])(?=.*js)(.*?)\\1", page).stream()
                        .filter(str -> str.contains(".js") && str.contains("http")).collect(Collectors.toList()));
        return future.get();
    }

    public List<String> downloadLinksAndParseJsLibraries(List<String> links, HttpClient client) throws Exception {
        Instant start = Instant.now();

        List<String> jsLibraries = new LinkedList<>();
        for (String link : links) {

            List<String> libraries = downloadLinkAndParseJsLibraries(link, client);
            jsLibraries.addAll(libraries);
        }

        System.out.println("Found JS libraries list: " + jsLibraries);
        Instant end = Instant.now();
        System.out.println("Downloaded and parsed links for JS libraries in: " + Duration.between(start, end).getSeconds() + " seconds.");
        return jsLibraries;
    }

    public Set<JsLibrary> deduplicateJsLibraries(List<String> libraries) {
        Set<JsLibrary> jsLibraries = new HashSet<>();
        Set<JsLibrary> librarySet = libraries.stream().map(library -> {
            try {
                return new JsLibrary(library, downloadLink(library, httpClient));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toSet());

        jsLibraries.addAll(librarySet);
        System.out.println("Unique JS libraries list: " + jsLibraries);
        return jsLibraries;
    }

    public void printTopFiveMostUsedJsLibraries(List<String> libraries) {
        Map<String, Long> frequencyMap = new HashMap<>();

        libraries.stream().parallel().map(library -> {
            try {
                return new JsLibrary(library, downloadLink(library, httpClient));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.groupingBy(JsLibrary::getMd5, () -> frequencyMap,
                        Collectors.counting()));
        Map<String, Long> sortedMap = new HashMap<>();
        frequencyMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
        sortedMap.entrySet().stream().limit(5).forEach(x -> System.out.println(x.getKey()));
    }

}
