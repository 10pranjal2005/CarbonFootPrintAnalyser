package com.example.calculator;

// --- THE FIX IS HERE: ALL NECESSARY IMPORTS HAVE BEEN ADDED ---
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// --- END OF FIX ---

public class CarbonFootprintHandler implements HttpHandler {

    private static class Recommendation {
        String category, text, difficulty;
        int savings;
        Recommendation(String category, String text, int savings, String difficulty) { this.category = category; this.text = text; this.savings = savings; this.difficulty = difficulty; }
        JSONObject toJSON() { JSONObject json = new JSONObject(); json.put("category", this.category); json.put("text", this.text); json.put("savings", this.savings); json.put("difficulty", this.difficulty); return json; }
    }

    private static final List<Recommendation> ALL_RECOMMENDATIONS = Arrays.asList(
        new Recommendation("Energy Consumption", "Switch to LED lighting for all your bulbs.", 150, "Easy"),
        new Recommendation("Energy Consumption", "Install a smart thermostat to optimize heating and cooling.", 250, "Medium"),
        new Recommendation("Travel & Transport", "Carpool or use public transport for your daily commute at least twice a week.", 300, "Medium"),
        new Recommendation("Travel & Transport", "Consider switching to an Electric Vehicle (EV) for your next car purchase.", 1500, "Hard"),
        new Recommendation("Food Habits", "Adopt a 'Meatless Monday' (or any other day) to reduce meat consumption.", 120, "Easy"),
        new Recommendation("Food Habits", "Switch from dairy milk to a plant-based alternative like oat or soy milk.", 80, "Easy"),
        new Recommendation("Consumer Goods & Waste", "Commit to using reusable shopping bags and a reusable water bottle.", 50, "Easy"),
        new Recommendation("Consumer Goods & Waste", "Reduce new clothing purchases by 50% for a year by repairing or buying second-hand.", 200, "Medium"),
        new Recommendation("Digital Footprint", "Lower video streaming quality from HD to SD when possible.", 30, "Easy")
    );
    
    private static final String SPREADSHEET_ID = "10Af4s1rYvjACHgwEl9OCMAtEWXX4Wr1CkC8yuBpepv4";
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String SHEET_NAME = "Sheet1";
    private static Sheets sheetsService;
    private static final double ELECTRICITY_FACTOR = 0.82; private static final double NATURAL_GAS_FACTOR = 5.5; private static final double LPG_CYLINDER_FACTOR = 42.6; private static final double WATER_HEATING_FACTOR = 0.82; private static final double GASOLINE_FACTOR = 2.3; private static final double FLIGHTS_SHORT_FACTOR = 200; private static final double FLIGHTS_LONG_FACTOR = 150; private static final double PUBLIC_TRANSPORT_FACTOR = 0.04; private static final double TAXI_FACTOR = 0.15; private static final double MEAT_FACTOR = 27.0; private static final double DAIRY_FACTOR = 2.8; private static final double DIET_REDUCTION_FACTOR = 150; private static final double WASTE_FACTOR = 0.25; private static final double E_WASTE_FACTOR = 1.7; private static final double CLOTHES_FACTOR = 7.0; private static final double BOTTLED_WATER_FACTOR = 0.2; private static final double STREAMING_FACTOR = 0.06; private static final double CLOUD_STORAGE_FACTOR = 0.5;
    private static final Map<String, Double> TWO_WHEELER_FACTORS = Map.ofEntries(Map.entry("hero_splendor", 55.0), Map.entry("hero_hf_deluxe", 52.0), Map.entry("hero_passion", 60.0), Map.entry("hero_glamour", 65.0), Map.entry("hero_xpulse", 90.0), Map.entry("honda_activa", 65.0), Map.entry("honda_shine", 58.0), Map.entry("honda_dio", 68.0), Map.entry("honda_unicorn", 75.0), Map.entry("honda_hornet", 85.0), Map.entry("tvs_jupiter", 68.0), Map.entry("tvs_apache", 82.0), Map.entry("tvs_raider", 70.0), Map.entry("tvs_ntorq", 75.0), Map.entry("tvs_iqube", 9.0), Map.entry("bajaj_pulsar", 80.0), Map.entry("bajaj_platina", 50.0), Map.entry("bajaj_chetak", 10.0), Map.entry("bajaj_dominar", 150.0), Map.entry("re_classic", 120.0), Map.entry("re_bullet", 125.0), Map.entry("re_himalayan", 140.0), Map.entry("ather_450x", 8.0), Map.entry("ola_s1", 7.0));
    private static final Map<String, Double> FOUR_WHEELER_FACTORS = Map.ofEntries(Map.entry("ms_swift", 250.0), Map.entry("ms_baleno", 240.0), Map.entry("ms_dzire", 260.0), Map.entry("ms_brezza", 300.0), Map.entry("ms_ertiga", 350.0), Map.entry("hy_creta_d", 380.0), Map.entry("hy_venue_p", 290.0), Map.entry("hy_i20", 260.0), Map.entry("hy_verna", 310.0), Map.entry("tata_nexon_p", 270.0), Map.entry("tata_punch", 230.0), Map.entry("tata_harrier", 420.0), Map.entry("tata_nexon_ev", 40.0), Map.entry("tata_tiago_ev", 30.0), Map.entry("ma_xuv700_d", 450.0), Map.entry("ma_scorpio", 480.0), Map.entry("ma_thar", 470.0), Map.entry("kia_seltos_d", 390.0), Map.entry("kia_sonet_p", 280.0), Map.entry("to_innova", 500.0), Map.entry("to_fortuner", 550.0));
    private static final Map<String, Double> MOBILE_FACTORS = Map.ofEntries(Map.entry("apple_iphone_15_pro", 7.0), Map.entry("apple_iphone_15", 6.0), Map.entry("apple_iphone_se", 4.5), Map.entry("samsung_s24_ultra", 8.0), Map.entry("samsung_s24", 7.0), Map.entry("samsung_z_fold5", 9.0), Map.entry("samsung_a55", 5.0), Map.entry("samsung_m34", 4.0), Map.entry("google_pixel_8_pro", 7.5), Map.entry("google_pixel_8", 6.5), Map.entry("google_pixel_7a", 5.5), Map.entry("oneplus_12", 7.0), Map.entry("oneplus_12r", 6.5), Map.entry("oneplus_nord_ce4", 5.0), Map.entry("xiaomi_14", 7.0), Map.entry("redmi_note_13_pro", 5.5), Map.entry("poco_x6_pro", 5.8));
    private static final Map<String, Double> LAPTOP_FACTORS = Map.ofEntries(Map.entry("apple_macbook_air_m3", 15.0), Map.entry("apple_macbook_pro_14", 25.0), Map.entry("dell_xps_15", 28.0), Map.entry("dell_inspiron_15", 20.0), Map.entry("dell_alienware_m16", 45.0), Map.entry("hp_spectre_x360", 26.0), Map.entry("hp_pavilion_15", 22.0), Map.entry("hp_victus", 40.0), Map.entry("lenovo_yoga_slim", 24.0), Map.entry("lenovo_ideapad_3", 18.0), Map.entry("lenovo_legion", 42.0), Map.entry("asus_zenbook_14", 22.0), Map.entry("asus_vivobook_15", 19.0), Map.entry("asus_rog_strix", 48.0));
    private static final Map<String, Double> REGION_HOME_FACTORS = Map.of("delhi_ncr", 0.9 * 2.5, "mumbai_mmr", 0.85 * 2.8, "bangalore", 0.7 * 3.0, "chennai", 0.75 * 3.2, "kolkata", 0.95 * 2.2, "uttar_pradesh", 1.1 * 2.0, "himachal_pradesh", 0.1 * 1.8);

    public CarbonFootprintHandler() { try { sheetsService = createSheetsService(); } catch (Exception e) { e.printStackTrace(); System.exit(1); } }
    private Sheets createSheetsService() throws IOException, GeneralSecurityException { final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport(); GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(CREDENTIALS_FILE_PATH)).createScoped(SCOPES); return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials)).setApplicationName("Carbon Footprint Calculator").build(); }
    @Override public void handle(HttpExchange exchange) throws IOException { exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*"); exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS"); exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization"); if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { exchange.sendResponseHeaders(204, -1); return; } String path = exchange.getRequestURI().getPath(); if ("/calculate-quarter".equals(path)) { handleQuarterlyCalculation(exchange); } else if ("/get-annual-result".equals(path)) { handleAnnualCalculation(exchange); } else if ("/get-all-data".equals(path)) { handleGetAllData(exchange); } else if ("/get-green-shift".equals(path)) { handleGreenShift(exchange); } else { sendResponse(exchange, 404, "{\"error\":\"Not Found\"}"); } }
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException { exchange.getResponseHeaders().set("Content-Type", "application/json"); exchange.sendResponseHeaders(statusCode, response.getBytes().length); try (OutputStream os = exchange.getResponseBody()) { os.write(response.getBytes()); } }

    private void handleGreenShift(HttpExchange exchange) throws IOException {
        System.out.println("\n--- Generating Green Shift Recommendations ---");
        List<Recommendation> personalizedFixes = new ArrayList<>();
        try {
            String range = SHEET_NAME + "!A:AB";
            ValueRange response = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, range).execute();
            List<List<Object>> values = response.getValues();
            if (values == null || values.size() < 2) { sendResponse(exchange, 200, "{\"error\":\"Not enough data to generate recommendations.\"}"); return; }
            List<Object> latestRow = values.get(values.size() - 1);
            Map<String, Double> latestData = new HashMap<>();
            List<Object> header = values.get(0);
            for(int i = 0; i < header.size() && i < latestRow.size(); i++) { try { latestData.put(header.get(i).toString(), Double.parseDouble(latestRow.get(i).toString())); } catch (NumberFormatException e) { /* ignore */ } }
            if (latestData.getOrDefault("Electricity", 0.0) > 500) { personalizedFixes.add(ALL_RECOMMENDATIONS.get(0)); personalizedFixes.add(ALL_RECOMMENDATIONS.get(1)); }
            if (latestData.getOrDefault("Gasoline", 0.0) > 100) { personalizedFixes.add(ALL_RECOMMENDATIONS.get(2)); personalizedFixes.add(ALL_RECOMMENDATIONS.get(3)); }
            if (latestData.getOrDefault("Meat", 0.0) > 10) { personalizedFixes.add(ALL_RECOMMENDATIONS.get(4)); personalizedFixes.add(ALL_RECOMMENDATIONS.get(5)); }
            if (latestData.getOrDefault("Clothes", 0.0) > 10) { personalizedFixes.add(ALL_RECOMMENDATIONS.get(6)); personalizedFixes.add(ALL_RECOMMENDATIONS.get(7)); }
            if (latestData.getOrDefault("Streaming", 0.0) > 100) { personalizedFixes.add(ALL_RECOMMENDATIONS.get(8)); }
            if (personalizedFixes.isEmpty()) { personalizedFixes.add(ALL_RECOMMENDATIONS.get(0)); personalizedFixes.add(ALL_RECOMMENDATIONS.get(4)); personalizedFixes.add(ALL_RECOMMENDATIONS.get(6)); }
            JSONArray jsonArray = new JSONArray();
            for (Recommendation rec : personalizedFixes) { jsonArray.put(rec.toJSON()); }
            sendResponse(exchange, 200, jsonArray.toString());
        } catch (Exception e) { e.printStackTrace(); sendResponse(exchange, 500, "{\"error\":\"Could not generate recommendations.\"}"); }
    }

    private void handleGetAllData(HttpExchange exchange) throws IOException { System.out.println("\n--- Received request for All Data ---"); try { String range = SHEET_NAME + "!A:AC"; ValueRange response = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, range).execute(); List<List<Object>> values = response.getValues(); if (values == null || values.isEmpty()) { sendResponse(exchange, 200, "[]"); return; } JSONArray jsonArray = new JSONArray(); List<Object> header = values.get(0); for (int i = 1; i < values.size(); i++) { List<Object> row = values.get(i); JSONObject rowJson = new JSONObject(); for (int j = 0; j < header.size() && j < row.size(); j++) { rowJson.put(header.get(j).toString(), row.get(j)); } jsonArray.put(rowJson); } sendResponse(exchange, 200, jsonArray.toString()); } catch (Exception e) { e.printStackTrace(); sendResponse(exchange, 500, "{\"error\":\"Failed to retrieve data\"}"); } }
    private void handleQuarterlyCalculation(HttpExchange exchange) throws IOException { String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); JSONObject json = new JSONObject(requestBody); int quarter = json.getInt("quarter"); double electricity = json.getDouble("electricity"); double naturalGas = json.getDouble("naturalGas"); double lpgCylinders = json.getDouble("lpgCylinders"); double waterHeating = json.getDouble("waterHeating"); double gasoline = json.getDouble("gasoline"); double flightsShort = json.getDouble("flightsShort"); double flightsLong = json.getDouble("flightsLong"); double publicTransport = json.getDouble("publicTransport"); double taxi = json.getDouble("taxi"); String twoWheeler = json.getString("twoWheeler"); String fourWheeler = json.getString("fourWheeler"); double meat = json.getDouble("meat"); double dairy = json.getDouble("dairy"); double vegetarianMonths = json.getDouble("vegetarianMonths"); double veganMonths = json.getDouble("veganMonths"); double waste = json.getDouble("waste"); double eWaste = json.getDouble("eWaste"); double clothes = json.getDouble("clothes"); double bottledWater = json.getDouble("bottledWater"); double streaming = json.getDouble("streaming"); double cloudStorage = json.getDouble("cloudStorage"); String mobile = json.getString("mobile"); String laptop = json.getString("laptop"); String region = json.getString("region"); double homeSize = json.getDouble("homeSize"); double householdFootprint = (electricity * ELECTRICITY_FACTOR) + (naturalGas * NATURAL_GAS_FACTOR) + (lpgCylinders * LPG_CYLINDER_FACTOR) + (waterHeating * 3 * WATER_HEATING_FACTOR); double transportFootprint = (gasoline * GASOLINE_FACTOR) + (flightsShort * FLIGHTS_SHORT_FACTOR) + (flightsLong * FLIGHTS_LONG_FACTOR) + (publicTransport * PUBLIC_TRANSPORT_FACTOR) + (taxi * TAXI_FACTOR) + TWO_WHEELER_FACTORS.getOrDefault(twoWheeler, 0.0) + FOUR_WHEELER_FACTORS.getOrDefault(fourWheeler, 0.0); double dietFootprint = (meat * MEAT_FACTOR) + (dairy * 3 * DAIRY_FACTOR) - (vegetarianMonths * DIET_REDUCTION_FACTOR) - (veganMonths * DIET_REDUCTION_FACTOR * 1.5); double lifestyleFootprint = (waste * WASTE_FACTOR) + (eWaste / 4 * E_WASTE_FACTOR) + (clothes * CLOTHES_FACTOR) + (bottledWater * BOTTLED_WATER_FACTOR); double digitalFootprint = (streaming * STREAMING_FACTOR) + (cloudStorage * 3 * CLOUD_STORAGE_FACTOR) + MOBILE_FACTORS.getOrDefault(mobile, 0.0) + LAPTOP_FACTORS.getOrDefault(laptop, 0.0); double homeFootprint = homeSize * REGION_HOME_FACTORS.getOrDefault(region, 0.0); double totalQuarterlyFootprint = householdFootprint + transportFootprint + dietFootprint + lifestyleFootprint + digitalFootprint + homeFootprint; appendToSheet(quarter, electricity, naturalGas, lpgCylinders, waterHeating, gasoline, flightsShort, flightsLong, publicTransport, taxi, twoWheeler, fourWheeler, meat, dairy, vegetarianMonths, veganMonths, waste, eWaste, clothes, bottledWater, streaming, cloudStorage, mobile, laptop, region, homeSize, totalQuarterlyFootprint); String response = String.format("{\"quarter\": %d, \"footprint\": %.2f}", quarter, totalQuarterlyFootprint); sendResponse(exchange, 200, response); }
    private void appendToSheet(Object... args) { try { ValueRange result = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, SHEET_NAME + "!A1:A1").execute(); if (result.getValues() == null || result.getValues().isEmpty()) { List<Object> header = Arrays.asList("Timestamp", "Quarter", "Electricity", "NaturalGas", "LPG", "WaterHeating", "Gasoline", "FlightsShort", "FlightsLong", "PublicTransport", "Taxi", "TwoWheeler", "FourWheeler", "Meat", "Dairy", "VegMonths", "VeganMonths", "Waste", "EWaste", "Clothes", "BottledWater", "Streaming", "CloudStorage", "Mobile", "Laptop", "Region", "HomeSize", "Quarterly_Footprint", "Annual_Footprint"); sheetsService.spreadsheets().values().update(SPREADSHEET_ID, SHEET_NAME + "!A1", new ValueRange().setValues(Collections.singletonList(header))).setValueInputOption("RAW").execute(); } List<Object> rowData = Arrays.asList(args); List<Object> finalRow = new java.util.ArrayList<>(); finalRow.add(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))); finalRow.addAll(rowData); sheetsService.spreadsheets().values().append(SPREADSHEET_ID, SHEET_NAME, new ValueRange().setValues(Collections.singletonList(finalRow))).setValueInputOption("USER_ENTERED").execute(); } catch (Exception e) { e.printStackTrace(); } }
    private void handleAnnualCalculation(HttpExchange exchange) throws IOException { System.out.println("\n--- Annual Calculation ---"); double annualFootprint = 0; int lastDataRow = 0; try { String range = SHEET_NAME + "!AB:AB"; ValueRange response = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, range).execute(); List<List<Object>> values = response.getValues(); if (values != null && !values.isEmpty()) { lastDataRow = values.size(); for (int i = 1; i < values.size(); i++) { List<Object> row = values.get(i); if (row != null && !row.isEmpty()) { try { annualFootprint += Double.parseDouble(row.get(0).toString()); } catch (NumberFormatException e) { /* ignore */ } } } } if (lastDataRow > 0) { String updateCell = SHEET_NAME + "!AC" + lastDataRow; List<Object> cellValue = Collections.singletonList(annualFootprint); ValueRange body = new ValueRange().setValues(Collections.singletonList(cellValue)); sheetsService.spreadsheets().values().update(SPREADSHEET_ID, updateCell, body).setValueInputOption("USER_ENTERED").execute(); System.out.println("--- Wrote annual total " + annualFootprint + " to cell " + updateCell + " ---"); } } catch (Exception e) { e.printStackTrace(); } System.out.println("--- Final Total: " + annualFootprint + " ---"); sendResponse(exchange, 200, String.format("{\"annualFootprint\": %.2f}", annualFootprint)); }
}