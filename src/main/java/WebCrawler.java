import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WebCrawler {

    public static final String browserAgent = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.7) Gecko/2009021910 Firefox/3.0.7";

    private HttpClient httpClient;

    WebCrawler(){
        this.httpClient = HttpClient.newHttpClient();
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        try(Scanner scanner = new Scanner(System.in)){
            System.out.println("Please input a search string: ");
            String searchTerm = scanner.next();
            System.out.println(searchTerm);

            //TODO: use Google Search API (must get API key and search engine ID)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://www.google.com/search?q=" + searchTerm))
                    .setHeader("User-Agent", browserAgent) // add request header
                    .GET()
                    .build();
            WebCrawler webCrawler = new WebCrawler();
            HttpResponse<String> response = webCrawler.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            //System.out.println(response.body());

            LinkedList<String> links = webCrawler.extractLinksFromPage(response.body());
            System.out.println(links);

            HttpClient httpClient2 = HttpClient.newBuilder().executor(Executors.newWorkStealingPool()).build();
            List<String> resultPages = webCrawler.downloadLinks(links, httpClient2);
            System.out.println(resultPages);

        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private HttpRequest getRequest(String targetLink) throws URISyntaxException {
        return  HttpRequest.newBuilder()
                .uri(new URI(targetLink))
                .GET()
                .build();
    }

    public LinkedList<String> extractLinksFromPage(String response) {
        LinkedList<String> links = new LinkedList<>();
        //TODO: improve pattern for main links ?!
        //TODO: how performant are Pattern and Matcher for large documents ?!
        Pattern pattern = Pattern.compile("href=\"\\/url\\?q=(.*?)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        while (matcher.find()) {
//            System.out.println(matcher.group(1));
            links.add(matcher.group(1));
        }
        System.out.println(links);
        System.out.println("Found " + links.size() + " links.");
        return links;
    }

    public List<String> downloadLinks(List<String> links, HttpClient client){
        List<CompletableFuture<String>> futures = links.stream()
                .map(target -> {
                    try {
                        return client.sendAsync(
                                     getRequest(target), HttpResponse.BodyHandlers.ofString())
                                .thenApply(response -> response.body());
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).collect(Collectors.toList());
        List<String> pages = futures.stream().map(future -> {
            try {
                return future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
        return pages;
    }
}
