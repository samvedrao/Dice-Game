import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class DiceToDieForServer {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/", new StaticHandler("public/index.html"));
        server.createContext("/styles.css", new StaticHandler("public/styles.css"));

        // Game Routes
        server.createContext("/guess", new GameHandler("guess"));
       
        server.createContext("/overunder", new GameHandler("overunder"));
        server.createContext("/whoScoresMore", new GameHandler("whoscore"));

        server.setExecutor(null);
        server.start();
        
        System.out.println("Server running at http://localhost:8080");
    }

    // Static file handler
    static class StaticHandler implements HttpHandler {
        private final String path;

        public StaticHandler(String path) {
            this.path = path;
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            File file = new File(path);
            if (!file.exists()) {
                String notFound = "404 Not Found";
                ex.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
            } else {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String contentType = path.endsWith(".css") ? "text/css" : "text/html";
                ex.getResponseHeaders().add("Content-Type", contentType);
                ex.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    // Game logic handler
    static class GameHandler implements HttpHandler {
        private final String mode;

        public GameHandler(String mode) {
            this.mode = mode;
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                sendHtml(ex, loadPage(null, false)); 
                return;
            }

            Map<String, String> form = parseForm(ex.getRequestBody());
            sendHtml(ex, loadPage(form, true)); 
        }

        private String loadPage(Map<String, String> form, boolean showResult) throws IOException {
            String html = new String(Files.readAllBytes(new File("public/" + mode + ".html").toPath()));

            if (!showResult) {
                return html.replace("{{result}}", "");
            }

            Random rand = new Random();
            int roll1 = rand.nextInt(6) + 1;
            int computer = rand.nextInt(6) + 1;
            switch (mode) {
                case "guess" -> System.err.println("hello");
                
                
                case "overunder" -> System.err.println("good");
                case "whoscore" -> System.err.println("alright");

                default -> throw new AssertionError();

            }

            return switch (mode) {
                case "guess" -> html.replace("{{result}}", "You rolled " + roll1 + ", computer rolled " + computer +
                        "<br><b>" + (roll1 == computer ? "Matched!" : "Didn't match") + "</b>");
                                  
                case "overunder" -> {
                    int ref = rand.nextInt(6) + 1;
                    yield html.replace("{{result}}", "You rolled " + roll1 + ", reference number was " + ref +
                            "<br><b>" + (roll1 > ref ? "Over!" : roll1 < ref ? "Under!" : "Exactly!") + "</b>");
                }
                case "whoscore" -> {
                    if (form == null || !form.containsKey("player")) {
                        yield html.replace("{{result}}", "Submit a guess to begin.");
                    }
                    int actual = rand.nextInt(6) + 1;
                    String guess = form.get("player");
                    boolean correct = guess.equals(String.valueOf(actual));
                    yield html.replace("{{result}}", "You guessed " + guess + ", dice rolled " + actual +
                            "<br><b>" + (correct ? "You win!" : "Wrong guess!") + "</b>");
                }
                default -> "Invalid mode!";
            };
        }

        private void sendHtml(HttpExchange ex, String response) throws IOException {
            ex.getResponseHeaders().add("Content-Type", "text/html");
            ex.sendResponseHeaders(200, response.length());
            try (OutputStream os = ex.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        private Map<String, String> parseForm(InputStream in) throws IOException {
            Map<String, String> map = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                for (String pair : sb.toString().split("&")) {
                    String[] kv = pair.split("=");
                    if (kv.length == 2) {
                        map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                                URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                    }
                }
            }
            return map;
        }
    }
}
