package com.example.calculator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticFileHandler implements HttpHandler {
    private final String webRoot = "frontend"; // The folder where your website lives

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        // If root path, serve index.html
        if (path.equals("/")) {
            path = "/index.html";
        }

        File file = new File(webRoot + path).getCanonicalFile();

        // Basic security check to prevent accessing files outside the web root
        if (!file.getPath().startsWith(new File(webRoot).getCanonicalPath())) {
            sendError(exchange, 403, "Forbidden");
            return;
        }

        if (!file.isFile()) {
            // If file not found, try serving index.html for SPA-like navigation
            File indexFile = new File(webRoot + "/index.html").getCanonicalFile();
            if (indexFile.isFile()){
                file = indexFile;
            } else {
                sendError(exchange, 404, "Not Found");
                return;
            }
        }

        // Set the correct Content-Type header
        Path filePath = Paths.get(file.getPath());
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            if (path.endsWith(".js")) contentType = "text/javascript";
            else if (path.endsWith(".css")) contentType = "text/css";
            else contentType = "application/octet-stream";
        }
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, file.length());

        try (OutputStream os = exchange.getResponseBody(); FileInputStream fs = new FileInputStream(file)) {
            final byte[] buffer = new byte[4096];
            int count;
            while ((count = fs.read(buffer)) > 0) {
                os.write(buffer, 0, count);
            }
        }
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(code, message.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
    }
}