/*
Simple Web Server in Java which allows you to call
localhost:9000/ and show you the root.html webpage from the www/root.html folder
You can also do some other simple GET requests:
1) /random shows you a random picture (well random from the set defined)
2) json shows you the response as JSON for /random instead the html page
3) /file/filename shows you the raw file (not as HTML)
4) /multiply?num1=3&num2=4 multiplies the two inputs and responses with the result
5) /github?query=users/amehlhase316/repos (or other GitHub repo owners) will lead to receiving
   JSON which will for now only be printed in the console. See the todo below

The reading of the request is done "manually", meaning no library that helps making things a
little easier is used. This is done so you see exactly how to pars the request and
write a response back
*/

package funHttpServer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Random;
import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.charset.Charset;

class WebServer {
  public static void main(String args[]) {
    WebServer server = new WebServer(9000);
  }

  private static List<String> story = new ArrayList<>();

  /**
   * Main thread
   * @param port to listen on
   */
  public WebServer(int port) {
    ServerSocket server = null;
    Socket sock = null;
    InputStream in = null;
    OutputStream out = null;

    try {
      server = new ServerSocket(port);
      while (true) {
        sock = server.accept();
        out = sock.getOutputStream();
        in = sock.getInputStream();
        byte[] response = createResponse(in);
        out.write(response);
        out.flush();
        in.close();
        out.close();
        sock.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (sock != null) {
        try {
          server.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Used in the "/random" endpoint
   */
  private final static HashMap<String, String> _images = new HashMap<>() {
    {
      put("streets", "https://iili.io/JV1pSV.jpg");
      put("bread", "https://iili.io/Jj9MWG.jpg");
    }
  };

  private Random random = new Random();

  /**
   * Reads in socket stream and generates a response
   * @param inStream HTTP input stream from socket
   * @return the byte encoded HTTP response
   */
  public byte[] createResponse(InputStream inStream) {

    byte[] response = null;
    BufferedReader in = null;

    try {

      // Read from socket's input stream. Must use an
      // InputStreamReader to bridge from streams to a reader
      in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));

      // Get header and save the request from the GET line:
      // example GET format: GET /index.html HTTP/1.1

      String request = null;

      boolean done = false;
      while (!done) {
        String line = in.readLine();

        System.out.println("Received: " + line);

        // find end of header("\n\n")
        if (line == null || line.equals(""))
          done = true;
        // parse GET format ("GET <path> HTTP/1.1")
        else if (line.startsWith("GET")) {
          int firstSpace = line.indexOf(" ");
          int secondSpace = line.indexOf(" ", firstSpace + 1);

          // extract the request, basically everything after the GET up to HTTP/1.1
          request = line.substring(firstSpace + 2, secondSpace);
        }

      }
      System.out.println("FINISHED PARSING HEADER\n");

      // Generate an appropriate response to the user
      if (request == null) {
        response = "<html>Illegal request: no GET</html>".getBytes();
      } else {
        // create output buffer
        StringBuilder builder = new StringBuilder();
        // NOTE: output from buffer is at the end

        if (request.length() == 0) {
          // shows the default directory page

          // opens the root.html file
          String page = new String(readFileInBytes(new File("www/root.html")));
          // performs a template replacement in the page
          page = page.replace("${links}", buildFileList());

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(page);

        } else if (request.equalsIgnoreCase("json")) {
          // shows the JSON of a random image and sets the header name for that image

          // pick a index from the map
          int index = random.nextInt(_images.size());

          // pull out the information
          String header = (String) _images.keySet().toArray()[index];
          String url = _images.get(header);

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: application/json; charset=utf-8\n");
          builder.append("\n");
          builder.append("{");
          builder.append("\"header\":\"").append(header).append("\",");
          builder.append("\"image\":\"").append(url).append("\"");
          builder.append("}");

        } else if (request.equalsIgnoreCase("random")) {
          // opens the random image page

          // open the index.html
          File file = new File("www/index.html");

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(new String(readFileInBytes(file)));

        } else if (request.contains("file/")) {
          // tries to find the specified file and shows it or shows an error

          // take the path and clean it. try to open the file
          File file = new File(request.replace("file/", ""));

          // Generate response
          if (file.exists()) { // success
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Would theoretically be a file but removed this part, you do not have to do anything with it for the assignment");
          } else { // failure
            builder.append("HTTP/1.1 404 Not Found\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("File not found: " + file);
          }
        } else if (request.contains("multiply?")) {
          // This multiplies two numbers, there is NO error handling, so when
          // wrong data is given this just crashes

          builder.setLength(0); // clear any old content
          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          // extract path parameters
          query_pairs = splitQuery(request.replace("multiply?", ""));
          Integer num1 = null;
          Integer num2 = null;

          // extract required fields from parameters
          try {
            if (!query_pairs.containsKey("num1") || query_pairs.get("num1").isEmpty())
              throw new IllegalArgumentException("num1 is missing");
            num1 = Integer.parseInt(query_pairs.get("num1"));
          } catch (Exception e) {
            // Handle invalid or missing num1 with appropriate error code
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n\n");
            builder.append("<h1>Error: Invalid num1 – " + e.getMessage() + "</h1>");
            return builder.toString().getBytes();
          }

          try {
            if (!query_pairs.containsKey("num2") || query_pairs.get("num2").isEmpty())
              throw new IllegalArgumentException("num2 is missing");
            num2 = Integer.parseInt(query_pairs.get("num2"));
          } catch (Exception e) {
            // Handle invalid or missing num2 with appropriate error code
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n\n");
            builder.append("<h1>Error: Invalid num2 – " + e.getMessage() + "</h1>");
            return builder.toString().getBytes();
          }

          // do math
          Integer result = num1 * num2;

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n\n");
          builder.append("Result is: " + result);
          response = builder.toString().getBytes();

        } else if (request.contains("github?")) {
          // pulls the query from the request and runs it with GitHub's REST API
          // check out https://docs.github.com/rest/reference/
          //
          // HINT: REST is organized by nesting topics. Figure out the biggest one first,
          //     then drill down to what you care about
          // "Owner's repo is named RepoName. Example: find RepoName's contributors" translates to
          //     "/repos/OWNERNAME/REPONAME/contributors"

          builder.setLength(0); // clear old content

          try {
            // extract the query parameter
            Map<String, String> query_pairs = splitQuery(request.replace("github?", ""));
            String query = query_pairs.get("query");
            if (query == null || query.isEmpty())
              throw new IllegalArgumentException("Missing query parameter");

            // fetch the JSON from GitHub
            String json = fetchURL("https://api.github.com/" + query);
            if (json == null || json.isEmpty())
              throw new IOException("Empty response from GitHub");

            // Simple parsing: split JSON array into individual repo strings
            String[] repos = json.split("\\},\\{"); // rough split for each repo
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n\n");
            builder.append("<h1>GitHub Repos:</h1>");

            // iterate over each repo and extract desired fields
            for (String repo : repos) {
              // pull full_name
              String full_name = repo.contains("\"full_name\"") ? repo.split("\"full_name\":\"")[1].split("\"")[0] : "N/A";
              // pull id
              String id = repo.contains("\"id\"") ? repo.split("\"id\":")[1].split(",")[0] : "N/A";
              // pull owner login
              String owner = repo.contains("\"owner\"") && repo.contains("\"login\"") ? repo.split("\"login\":\"")[1].split("\"")[0] : "N/A";

              // add to HTML response
              builder.append("Full Name: " + full_name + "<br>");
              builder.append("ID: " + id + "<br>");
              builder.append("Owner: " + owner + "<br><br>");
            }

          } catch (Exception e) {
            // TODO: Parse the JSON returned by your fetch and create an appropriate
            // response based on what the assignment document asks for
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n\n");
            builder.append("<h1>Error fetching GitHub repos: " + e.getMessage() + "</h1>");
          }

          response = builder.toString().getBytes();

        } else if (request.startsWith("addline")) {
          // Parse the query parameters
          Map<String, String> query_pairs = splitQuery(request.replace("addline?", ""));
          String line = query_pairs.get("text"); // get ?text=...

          if (line != null && !line.isEmpty()) {
            story.add(line); // add the line to the story
          }

          // Build HTML response to show the updated story
          builder.setLength(0); // clear any old content
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append("<h1>Updated Story</h1>\n");
          for(String s : story){
            builder.append(s + "<br>\n");
          }

          return builder.toString().getBytes();

        } else if (request.equals("story")) {
          // Build HTML response to show the story
          builder.setLength(0); // clear any old content
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append("<h1>Current Story</h1>\n");
          for (String s : story) {
            builder.append(s + "<br>\n");
          }

          return builder.toString().getBytes();

        } else if (request.startsWith("reverse")) {
          try {
            // Parse query parameters
            Map<String, String> query_pairs = splitQuery(request.replace("reverse?", ""));
            String text = query_pairs.get("text"); // get the ?text=...

            // Clear builder before building response
            builder.setLength(0);

            if (text == null || text.isEmpty()) {
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("<h1>Error: No text provided to reverse</h1>");
            } else {
              // Reverse the text
              String reversed = new StringBuilder(text).reverse().toString();

              // Build response
              builder.append("HTTP/1.1 200 OK\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("<h1>Original:</h1>");
              builder.append(text + "<br>");
              builder.append("<h1>Reversed:</h1>");
              builder.append(reversed);
            }

            response = builder.toString().getBytes();
          } catch (UnsupportedEncodingException e) {
            // Handle the unlikely exception gracefully
            builder.setLength(0);
            builder.append("HTTP/1.1 500 Internal Server Error\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("<h1>Encoding error occurred</h1>");
            response = builder.toString().getBytes();
          }
        } else if (request.startsWith("palindrome?")) {
            // Check if the provided text is a palindrome
            Map<String, String> query_pairs = splitQuery(request.replace("palindrome?", ""));
            String text = query_pairs.get("text");
            String ignoreCaseParam = query_pairs.getOrDefault("ignoreCase", "true");

            builder.setLength(0); // clear old content

            if (text == null || text.isEmpty()) {
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n\n");
              builder.append("<h1>Error: Missing 'text' parameter</h1>");
            } else {
              boolean ignoreCase = ignoreCaseParam.equalsIgnoreCase("true");

              String processed = ignoreCase ? text.toLowerCase() : text;
              String reversed = new StringBuilder(processed).reverse().toString();
              boolean isPalindrome = processed.equals(reversed);

              builder.append("HTTP/1.1 200 OK\n");
              builder.append("Content-Type: text/html; charset=utf-8\n\n");
              builder.append("<h1>Palindrome Check</h1>");
              builder.append("Text: " + text + "<br>");
              builder.append("Ignore Case: " + ignoreCase + "<br>");
              builder.append("Result: " + (isPalindrome ? "Yes, it's a palindrome!" : "No, not a palindrome"));
            }

            response = builder.toString().getBytes();
        } else if (request.startsWith("fibonacci?")) {
          // Generate a Fibonacci sequence starting from 'start' index for 'count' numbers
          Map<String, String> query_pairs = splitQuery(request.replace("fibonacci?", ""));
          builder.setLength(0);

          try {
            int start = Integer.parseInt(query_pairs.get("start"));
            int count = Integer.parseInt(query_pairs.get("count"));

            if (start < 0 || count <= 0) {
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n\n");
              builder.append("<h1>Error: 'start' must be >= 0 and 'count' must be > 0</h1>");
            } else {
              builder.append("HTTP/1.1 200 OK\n");
              builder.append("Content-Type: text/html; charset=utf-8\n\n");
              builder.append("<h1>Fibonacci Sequence</h1>");
              long a = 0, b = 1;

              // Generate up to the 'start' index
              for (int i = 0; i < start; i++) {
                long temp = a + b;
                a = b;
                b = temp;
              }

              builder.append("Start index: " + start + "<br>Sequence: ");
              for (int i = 0; i < count; i++) {
                builder.append(a + " ");
                long temp = a + b;
                a = b;
                b = temp;
              }
            }
          } catch (NumberFormatException e) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n\n");
            builder.append("<h1>Error: Invalid number format for 'start' or 'count'</h1>");
          }

          response = builder.toString().getBytes();
        } else {
          // if the request is not recognized at all

          builder.append("HTTP/1.1 400 Bad Request\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append("I am not sure what you want me to do...");
        }

        // Output
        response = builder.toString().getBytes();
      }
    } catch (IOException e) {
      e.printStackTrace();
      response = ("<html>ERROR: " + e.getMessage() + "</html>").getBytes();
    }

    return response;
  }

  /**
   * Method to read in a query and split it up correctly
   * @param query parameters on path
   * @return Map of all parameters and their specific values
   * @throws UnsupportedEncodingException If the URLs aren't encoded with UTF-8
   */
  public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
    // "q=hello+world%2Fme&bob=5"
    String[] pairs = query.split("&");
    // ["q=hello+world%2Fme", "bob=5"]
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
          URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    }
    // {{"q", "hello world/me"}, {"bob","5"}}
    return query_pairs;
  }

  /**
   * Builds an HTML file list from the www directory
   * @return HTML string output of file list
   */
  public static String buildFileList() {
    ArrayList<String> filenames = new ArrayList<>();

    // Creating a File object for directory
    File directoryPath = new File("www/");
    filenames.addAll(Arrays.asList(directoryPath.list()));

    if (filenames.size() > 0) {
      StringBuilder builder = new StringBuilder();
      builder.append("<ul>\n");
      for (var filename : filenames) {
        builder.append("<li>" + filename + "</li>");
      }
      builder.append("</ul>\n");
      return builder.toString();
    } else {
      return "No files in directory";
    }
  }

  /**
   * Read bytes from a file and return them in the byte array. We read in blocks
   * of 512 bytes for efficiency.
   */
  public static byte[] readFileInBytes(File f) throws IOException {

    FileInputStream file = new FileInputStream(f);
    ByteArrayOutputStream data = new ByteArrayOutputStream(file.available());

    byte buffer[] = new byte[512];
    int numRead = file.read(buffer);
    while (numRead > 0) {
      data.write(buffer, 0, numRead);
      numRead = file.read(buffer);
    }
    file.close();

    byte[] result = data.toByteArray();
    data.close();

    return result;
  }

  /**
   *
   * a method to make a web request. Note that this method will block execution
   * for up to 20 seconds while the request is being satisfied. Better to use a
   * non-blocking request.
   *
   * @param aUrl the String indicating the query url for the OMDb api search
   * @return the String result of the http request.
   *
   **/
  public String fetchURL(String aUrl) {
    StringBuilder sb = new StringBuilder();
    URLConnection conn = null;
    InputStreamReader in = null;
    try {
      URL url = new URL(aUrl);
      conn = url.openConnection();
      if (conn != null)
        conn.setReadTimeout(20 * 1000); // timeout in 20 seconds
      if (conn != null && conn.getInputStream() != null) {
        in = new InputStreamReader(conn.getInputStream(), Charset.defaultCharset());
        BufferedReader br = new BufferedReader(in);
        if (br != null) {
          int ch;
          // read the next character until end of reader
          while ((ch = br.read()) != -1) {
            sb.append((char) ch);
          }
          br.close();
        }
      }
      in.close();
    } catch (Exception ex) {
      System.out.println("Exception in url request:" + ex.getMessage());
    }
    return sb.toString();
  }
}
