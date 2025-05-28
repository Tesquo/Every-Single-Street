import java.util.*;
import java.util.stream.Collectors;

public class Graph {
    private List<Node> nodes;
    private final List<Edge> edges;
    private List<List<Edge>> adjacencyList;
    private Preprocessor preprocessor;

    public Graph(Overpasser overpasser, Preprocessor preprocessor) {
        this.nodes = new ArrayList<>();
        this.adjacencyList = new ArrayList<>();
        this.edges = new ArrayList<>();
        createGraph(overpasser, preprocessor);
    }

    public static class Node {
        double lat;
        double lon;
        long id;
        boolean Intermediary;

        Node (double lat, double lon, long id, boolean Intermediary) {
            this.lat = lat;
            this.lon = lon;
            this.id = id;
            this.Intermediary = Intermediary;
        }

        Node(double lat, double lon, long id) {
            this.lat = lat;
            this.lon = lon;
            this.id = id;
        }

    }

    public static class Way {
        String name;
        long id;
        List<Long> nodes;
        Map<String, String> tags;

        Way (String name, long id, List<Long> nodes, Map<String, String> tags) {
            this.name = name;
            this.id = id;
            this.nodes = nodes;
            this.tags = tags;
        }

        public Map<String, String> getTags() {return tags;}
    }

    public static class processedWay {
        String name;
        long id;
        List<Node> nodes;
        Map<String, String> tags;
        double distance;

        processedWay(String name, long id, List<Node> nodes, Map<String, String> tags) {
            this.name = name;
            this.id = id;
            this.nodes = nodes;
            this.tags = tags;
        }
        processedWay(String name, long id, List<Node> nodes, Map<String, String> tags, double distance) {
            this.name = name;
            this.id = id;
            this.nodes = nodes;
            this.tags = tags;
            this.distance = distance;
        }
    }

    public static class Edge {
        Node x;
        Node y;
        double distance;
        boolean visited;

        Edge (Node x, Node y, double distance) {
            this.x = x;
            this.y = y;
            this.distance = distance;
            this.visited = false;
        }
    }

    private void createGraph(Overpasser overpasser, Preprocessor preprocessor) {
        List<processedWay> processedWays = preprocessor.getBiggestComponent();
        Map<Long, Node> nodeMap = new HashMap<>();
        for (processedWay way : processedWays) {
            for (Node node : way.nodes) {
                nodeMap.putIfAbsent(node.id, node);
            }
        }
        nodes = new ArrayList<>(nodeMap.values());
        adjacencyList = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            adjacencyList.add(new ArrayList<>());
        }
        for (processedWay way : processedWays) {
            List<Node> wayNodes = way.nodes;
            for (int i = 0; i < wayNodes.size() - 1; i++) {
                Node x = wayNodes.get(i);
                Node y = wayNodes.get(i + 1);
                double distance = preprocessor.calculateDistance(x, y);

                int xIndex = nodes.indexOf(x);
                int yIndex = nodes.indexOf(y);

                if (xIndex >= 0 && yIndex >= 0) {
                    Edge forwardEdge = new Edge(x, y, distance);
                    Edge backwardEdge = new Edge(y, x, distance);

                    adjacencyList.get(xIndex).add(forwardEdge);
                    adjacencyList.get(yIndex).add(backwardEdge);

                    edges.add(forwardEdge);
                }
            }
        }
        System.out.println("Nodes after construction: " + getNodes().size());
        compute2Core();
        System.out.println("Nodes after 2-core: " + getNodes().size());
        System.out.println("Graph is connected: " + isFullyConnected());
    }

    public void compute2Core() {
        Queue<Integer> degreeOneNodes = new LinkedList<>();
        int[] degree = new int[nodes.size()];

        for (int i = 0; i < nodes.size(); i++) {
            degree[i] = adjacencyList.get(i).size();
            if (degree[i] < 2) {
                degreeOneNodes.add(i);
            }
        }

        while (!degreeOneNodes.isEmpty()) {
            int nodeIndex = degreeOneNodes.poll();
            if (degree[nodeIndex] == 0) {continue;}
            for (Edge edge : adjacencyList.get(nodeIndex)) {
                int neighborIndex = nodes.indexOf(edge.y);
                if (neighborIndex == 0) {continue;}
                edge.visited = true;
                Edge mirrorEdge = findMirrorEdge(edge);
                if (mirrorEdge != null) {
                    mirrorEdge.visited = true;
                }
                if (--degree[neighborIndex] == 1) {
                    degreeOneNodes.add(neighborIndex);
                }
            }
            degree[nodeIndex] = 0;
        }

        List<Node> newNodes = new ArrayList<>();
        List<List<Edge>> newAdjacencyList = new ArrayList<>();
        Map<Integer, Integer> oldToNewIndex = new HashMap<>();

        for (int i = 0; i < nodes.size(); i++) {
            if (degree[i] >= 2) {
                oldToNewIndex.put(i, newNodes.size());
                newNodes.add(nodes.get(i));
                newAdjacencyList.add(new ArrayList<>());
            }
        }

        for (int i = 0; i < nodes.size(); i++) {
            if (degree[i] >= 2) {
                int newIndex = oldToNewIndex.get(i);
                for (Edge edge : adjacencyList.get(i)) {
                    int neighborOldIndex = nodes.indexOf(edge.y);
                    if (degree[neighborOldIndex] >= 2) {
                        newAdjacencyList.get(newIndex).add(edge);
                    }
                }
            }
        }

        this.nodes = newNodes;
        this.adjacencyList = newAdjacencyList;
    }

    public Edge findMirrorEdge(Edge edge) {
        int neighbourIndex = nodes.indexOf(edge.y);
        for (Edge e : adjacencyList.get(neighbourIndex)) {
            if (e.y.equals(edge.x)) {
                return e;
            }
        }
        return null;
    }

    public List<List<Edge>> getAdjacencyList() {
        return adjacencyList;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Edge> getNeighbours(Node node) {
        int index = nodes.indexOf(node);
        return (index >= 0 && index < adjacencyList.size())
                ? adjacencyList.get(index)
                : Collections.emptyList();
    }

    public Node getNodeById(long id) {
        for (Node node : nodes) {
            if (node.id == id) {
                return node;
            }
        }
        return null;
    }

    public Edge getEdge(Node x, Node y) {
        int index = nodes.indexOf(x);
        if (index == -1) {
            return null;
        }
        return adjacencyList.get(index).stream()
                .filter(edge -> edge.y.equals(y))
                .findFirst()
                .orElse(null);
    }
    public Set<Edge> getAllEdges() {
        Set<Edge> uniqueEdges = new HashSet<>();
        for (Edge edge : edges) {
            uniqueEdges.add(edge);
        }
        return uniqueEdges;
    }

    public Map<Node, Double> initialiseDistances(Node x) {
        Map<Node, Double> distances = new HashMap<>();
        for (Node node : nodes) {
            distances.put(node, Double.MAX_VALUE);
        }
        distances.put(x, 0.0);
        return distances;
    }

    public double getDistance(Node x, Node y) {
        for (Edge edge : getNeighbours(x)) {
            if (edge.y.equals(y)) {
                return edge.distance;
            }
        }
        return Double.MIN_VALUE;
    }

    public boolean isFullyConnected() {
        if (nodes.isEmpty()) return true;
        Set<Node> visited = new HashSet<>();
        Queue<Node> queue = new LinkedList<>();
        queue.add(nodes.get(0));
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (!visited.contains(node)) {
                visited.add(node);
                for (Edge edge : getNeighbours(node)) {
                    if (!visited.contains(edge.y)) {
                        queue.add(edge.y);
                    }
                }
            }
        }
        return visited.size() == nodes.size();
    }

    public boolean areConnected(Node x, Node y) {
        return getEdge(x, y) != null;
    }

    public Node getRandomNode() {
        Random random = new Random();
        int index = random.nextInt(nodes.size());
        return nodes.get(index);
    }
    public double calculateDistance(Graph.Node node1, Graph.Node node2) {
        double lat1 = node1.lat;
        double lon1 = node1.lon;
        double lat2 = node2.lat;
        double lon2 = node2.lon;
        int r = 6371;
        double p = Math.PI / 180;
        double a = 0.5 - Math.cos((lat2 - lat1) * p) / 2 + Math.cos(lat1 * p) * Math.cos(lat2 * p) * (1 - Math.cos((lon2 - lon1) * p)) / 2;
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    public static void main(String[] args) {
        Overpasser overpasser = new Overpasser();
        overpasser.request(overpasser.query);
        Preprocessor preprocessor = new Preprocessor(overpasser);
        Graph graph = new Graph(overpasser, preprocessor);
        List<List<Edge>> adjacencyList = graph.getAdjacencyList();
        for (List<Edge> edges : adjacencyList) {
            System.out.println(edges);
        }
    }
}