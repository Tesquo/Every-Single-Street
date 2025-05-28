import com.google.gson.*;
import okhttp3.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Overpasser {
    private static final String OVERPASS_API_URL = "https://overpass-api.de/api/interpreter";
    private static final List<String> desiredTags = Arrays.asList("secondary", "primary", "tertiary", "residential", "unclassified", "living_street", "roundabout");
    List<Graph.Node> nodes = new ArrayList<>();
    List<Graph.Way> ways = new ArrayList<>();
    String query = """
            [out:json];
            area["ISO3166-1"="GB"][admin_level=2]->.country;
            area["name"="San Francisco"]->.a;
            (way(area.a)["highway"];
            node(w);
            );
            out meta;
            """;

    public void request(String query) {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
            Request request = new Request.Builder()
                    .url(OVERPASS_API_URL)
                    .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8)))
                    .build();
            try {
                Response response = client.newCall(request).execute();
                convertJSON(response.body().string());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    public void convertJSON(String jsonResponse) {
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonElement root = parser.parse(jsonResponse);
        if (root.isJsonObject() && root.getAsJsonObject().has("elements")) {
            JsonArray jsonArray = root.getAsJsonObject().getAsJsonArray("elements");
            for (JsonElement element : jsonArray) {
                JsonObject jsonobj = element.getAsJsonObject();
                if (jsonobj.has("type")) {
                    String type = jsonobj.get("type").getAsString();
                    if ("node".equals(type)) {
                        Graph.Node node = gson.fromJson(jsonobj, Graph.Node.class);
                        nodes.add(node);
                        //System.out.println(node.id + "\n" + node.lat + "\n" + node.lon + "\n");
                    }
                    else if ("way".equals(type)) {
                        Graph.Way way = gson.fromJson(jsonobj, Graph.Way.class);
                        if (tagMatches(way, desiredTags)) {
                            ways.add(way);
//                            System.out.println("Way ID: " + way.id);
//                            System.out.println("Tags: " + way.tags);
//                            System.out.println("Number of nodes: " + way.nodes.size());
//                            System.out.println("First node ID: " + (way.nodes.isEmpty() ? "N/A" : way.nodes.get(0)));
//                            System.out.println();
                        }
                    }
                }
            }
        } else {System.err.println("Invalid JSON format: Expected an array.");}
    }

    private boolean tagMatches(Graph.Way way, List<String> desiredTags) {
        String highwayTag = way.getTags().get("highway");
        return highwayTag != null && desiredTags.contains(highwayTag);
    }

    public List<Graph.Way> getWays() {
        return new ArrayList<>(ways);
    }

    public List<Graph.Node> getNodes() {
        return nodes;
    }

    public String query() {
        return "";
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        Overpasser op = new Overpasser();
        op.request(op.query);
        List<Graph.Way> ways = op.getWays();
        List<Long> ids = new ArrayList<>();
        for (Graph.Way way : ways) {
            for (long nodeID : way.nodes) {
                ids.add(nodeID);
            }
        }
        Collections.sort(ids);
        for (long nodeID : ids) {
            //System.out.println(nodeID);
        }
    }
}
