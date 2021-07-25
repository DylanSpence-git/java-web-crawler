import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.util.regex.*;

/**
 * Purpose:
 *  Web crawler to search a webpage for anchor tags and finds the href values to keep crawling through the pages links
 *  until MAX_DEPTH has been reached or no new href values were found.
 */
public class WebCrawler{
    // Max depth for the crawler
    private int MAX_DEPTH = 3;
    // Current depth
    private int depth = 0;
    // Website URI object for the crawler
    private  URI website;
    private static HashMap<String, Boolean> sites = new HashMap<String, Boolean>();
    private static ArrayList<URI> urls = new ArrayList<URI>();

    public static void main(String[] args){
        if(args.length < 1){
            System.out.println("webCrawler requires at least one argument.");
            System.exit(-1);
        }
        WebCrawler webCrawler = new WebCrawler(args[0]);
        webCrawler.crawl();
    }

    /**
     * Purpose:
     *      Takes a string argument and attempts to create a URI with it.
     *      If successful, it adds the website to the list of urls to crawl through.
     * 
     * @param url = the user inputted string representing the desired site to crawl through.
     * 
     *  @exception IllegalArgumentException If the string the user passed in is invalid the function 
     *             will print the error and close the program.
     */
    private WebCrawler(String url){
        try{
            website = URI.create("http://" + url);
            urls.add(website);
        }catch(IllegalArgumentException ex){
            System.err.println(ex);
            System.out.println("Closing WebCrawler");
            System.exit(-1);
        }
    }

    /**
     * Purpose:
     *      Function to use regex to search through the pages body for the href value in the
     *      anchor tags and adds them to the next sites to finish if not already there. 
     *      If Something goes wrong, the function will close and the crawler will continue with any previous valid urls received. 
     * 
     * @param string = A string representing the body of the HTML page.
     * @exception Exception Catch any unexpected errors thrown by the matcher.group() or pattern.compile() then calls getUrls to update the pages to visit.
     */
    private void regex(String string){
        try{
            Pattern pattern = Pattern.compile("<a\\s[^>]*href=\"([^\"]*)\">", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(string);
            while(matcher.find()){
                if(!sites.containsKey(matcher.group(1))){
                    sites.put(matcher.group(1), true);
                    urls.add((URI.create(website + url)));
                }
            }
        }catch(Exception ex){
            System.err.println(ex);
        }
    }


    /**
     * Purpose:
     *      Prints out the sites that the crawler have found and if sites is empty will print [/].
     */
    private void print(){
        System.out.print("[");
        for(String url: sites.keySet()){
            System.out.print(url + ", ");
        }
        System.out.println("/]");
    }


    /**
     * Purpose:
     *      Cycles through the sites keyset and if the value of the 
     *      key value pair is false it adds the site to the url ArrayList in order to crawl the new urls.
     *      After adding to the urls to visit it sets the value in the map to true to indicate that it will be visited.
     * 
     *  @exception IllegalArgumentException If the url grabbed is not valid the function will continue to check the following urls.
     */
    private void getUrls(){
            for (String url : urls.keySet()) {
                try{
                    if(sites.get(url)){
                        urls.add((URI.create(website + url)));
                        sites.replace(url, true);
                    }
                } catch(IllegalArgumentException ex){
                    System.err.println(ex);
                }
            }
    }


    /**
     * Purpose:
     *      Main function for the crawler. This function streams the current urls ArrayList,
     *      grabs the HttpResponse body from the websites, passes the body string to the regex function,
     *      waits for all of the websites are finished being crawled, calls getUrls(), and prints the visited urls if there are more urls to visit.
     *      This is looped until no new websites are found or it reached the MAX_DEPTH.
     * 
     *  @exception Exception catches any exceptions thrown by the .toArray(), the sendAsync(), the build(), and the newBuilder() methods.
     */
    private void crawl(){
        while(depth < MAX_DEPTH && urls.size() > 0){
            try{
            List<HttpRequest> requests = urls.stream().map(url -> HttpRequest.newBuilder(url)).map(reqBuilder -> reqBuilder.build()).collect(Collectors.toList());
            HttpClient client = HttpClient.newHttpClient();
            urls.clear();
            CompletableFuture<?>[] asyncs = requests.stream().map( req -> client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                                                                        .thenApply(HttpResponse::body).thenAccept(this::regex)).toArray(CompletableFuture<?>[]::new);
            CompletableFuture.allOf(asyncs).join();
            //getUrls();
            if(urls.size() != 0){
                print();
            }
            depth++;
        }catch(Exception ex){
            System.err.println(ex);
        }
        }
        System.out.println("Closing webCrawler.");
        System.exit(1);
    }
}