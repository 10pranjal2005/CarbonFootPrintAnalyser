package com.example.calculator;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        CarbonFootprintHandler sharedHandler = new CarbonFootprintHandler();

        // This ensures all four endpoints are correctly routed
        server.createContext("/calculate-quarter", sharedHandler);
        server.createContext("/get-annual-result", sharedHandler);
        server.createContext("/get-all-data", sharedHandler);
        server.createContext("/get-green-shift", sharedHandler);

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("Server is listening on port " + port);
    }
}