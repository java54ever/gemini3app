import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MeteoFranceApp {

    private static final HttpClient client = HttpClient.newHttpClient();
    private JTextArea logArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MeteoFranceApp().createAndShowGUI());
    }

    public void createAndShowGUI() {
        JFrame frame = new JFrame("🇫🇷 Météo-France AROME Preview");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLayout(new BorderLayout());

        // 1. Input Panel
        JPanel topPanel = new JPanel();
        JTextField cityInput = new JTextField("Paris", 15);
        JButton searchButton = new JButton("Get Forecast");
        topPanel.add(new JLabel("City:"));
        topPanel.add(cityInput);
        topPanel.add(searchButton);
        frame.add(topPanel, BorderLayout.NORTH);

        // 2. Table & Log Split
        String[] columnNames = {"Time", "Condition", "Temp (°C)", "Rain (mm)", "Humidity (%)"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(tableModel);
        table.setRowHeight(25);
        JScrollPane tableScroll = new JScrollPane(table);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setForeground(new Color(0, 100, 0)); // Dark Green for logs
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Status Logs"));
        logScroll.setPreferredSize(new Dimension(900, 150));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, logScroll);
        splitPane.setResizeWeight(0.7);
        frame.add(splitPane, BorderLayout.CENTER);

        // 3. Action Logic
        searchButton.addActionListener(e -> {
            String city = cityInput.getText().trim();
            if (city.isEmpty()) return;

            searchButton.setEnabled(false);
            tableModel.setRowCount(0);
            logArea.setText("--- STARTING ---\n");

            new Thread(() -> {
                try {
                    // Step A: Geocode
                    log("1. Locating: " + city);
                    double[] coords = getCoordinates(city);
                    if (coords == null) {
                        log("ERROR: City not found.");
                        SwingUtilities.invokeLater(() -> searchButton.setEnabled(true));
                        return;
                    }
                    log("   Found: " + coords[0] + ", " + coords[1]);

                    // Step B: Fetch Data (CORRECTED URL)
                    log("2. Fetching AROME Data...");
                    String jsonResponse = getWeatherData(coords[0], coords[1]);
                    
                    if (jsonResponse == null) {
                        log("ERROR: Failed to fetch weather data.");
                        SwingUtilities.invokeLater(() -> searchButton.setEnabled(true));
                        return;
                    }

                    // Step C: Parse
                    log("3. Parsing data...");
                    parseAndPopulate(jsonResponse, tableModel);
                    log("SUCCESS: Data loaded.");

                } catch (Exception ex) {
                    log("CRITICAL ERROR: " + ex.getMessage());
                    ex.printStackTrace();
                } finally {
                    SwingUtilities.invokeLater(() -> searchButton.setEnabled(true));
                }
            }).start();
        });

        frame.setVisible(true);
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }

    // --- NETWORK ---

    private double[] getCoordinates(String cityName) throws Exception {
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" 
                     + cityName.replace(" ", "+") 
                     + "&count=1&language=fr&format=json";
        
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            log("   Geocoding Error: " + response.statusCode());
            return null;
        }

        String body = response.body();
        if (!body.contains("\"results\"")) return null;

        double lat = Double.parseDouble(extractSimpleValue(body, "latitude"));
        double lon = Double.parseDouble(extractSimpleValue(body, "longitude"));
        return new double[]{lat, lon};
    }

    private String getWeatherData(double lat, double lon) throws Exception {
        // FIXED: Changed "meteofrance_arome" to "meteofrance_arome_france"
        String url = String.format(Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
            "&hourly=temperature_2m,relative_humidity_2m,weather_code,precipitation" +
            "&timezone=auto&forecast_days=1" +
            "&models=meteofrance_arome_france", // <--- THIS WAS THE FIX
            lat, lon);

        log("   URL: " + url); // Log URL to be sure

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            log("   Weather API Error: " + response.statusCode());
            log("   Response: " + response.body());
            return null;
        }
        return response.body();
    }

    // --- PARSING ---

    private void parseAndPopulate(String json, DefaultTableModel model) {
        int hourlyIndex = json.indexOf("\"hourly\"");
        if (hourlyIndex == -1) {
            log("ERROR: JSON format unexpected (missing 'hourly').");
            return;
        }

        List<String> times = extractArray(json, "time", hourlyIndex);
        List<String> temps = extractArray(json, "temperature_2m", hourlyIndex);
        List<String> humids = extractArray(json, "relative_humidity_2m", hourlyIndex);
        List<String> rains = extractArray(json, "precipitation", hourlyIndex);
        List<String> codes = extractArray(json, "weather_code", hourlyIndex);

        int limit = Math.min(times.size(), 24);

        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < limit; i++) {
                String rawTime = times.get(i).replace("\"", "");
                String formattedTime = rawTime.contains("T") ? rawTime.split("T")[1] : rawTime;

                String codeStr = codes.get(i);
                String icon = getWeatherIcon(codeStr);
                
                model.addRow(new Object[]{
                    formattedTime, icon, temps.get(i), rains.get(i), humids.get(i)
                });
            }
        });
    }

    private String extractSimpleValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\":\\s*([0-9.-]+)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "0";
    }

    private List<String> extractArray(String json, String key, int startIndex) {
        List<String> result = new ArrayList<>();
        String keySearch = "\"" + key + "\":";
        int keyPos = json.indexOf(keySearch, startIndex);
        if (keyPos == -1) return result;

        int arrayStart = json.indexOf("[", keyPos);
        int arrayEnd = json.indexOf("]", arrayStart);
        
        if (arrayStart != -1 && arrayEnd != -1) {
            String content = json.substring(arrayStart + 1, arrayEnd);
            if (!content.trim().isEmpty()) {
                String[] items = content.split(",");
                for (String item : items) result.add(item.trim());
            }
        }
        return result;
    }

    private String getWeatherIcon(String codeStr) {
        try {
            int code = Integer.parseInt(codeStr);
            if (code == 0) return "☀️";
            if (code <= 3) return "⛅";
            if (code <= 48) return "🌫️";
            if (code <= 65) return "🌧️";
            if (code <= 75) return "❄️";
            if (code >= 95) return "⚡";
        } catch (Exception e) { return "?"; }
        return "🌡️";
    }
}
