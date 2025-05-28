import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
//        Overpasser overpasser = new Overpasser();
//        overpasser.request(overpasser.query);
//        Preprocessor preprocessor = new Preprocessor(overpasser);
//        Graph graph = new Graph(overpasser, preprocessor);
//        System.out.println("Graph statistics:");
//        System.out.println("Nodes: " + graph.getNodes().size());
//        System.out.println("Unique edges: " + (graph.getAllEdges().size() / 2));
//        SwingUtilities.invokeLater(() -> {
//            GraphVisualiser visualiser = new GraphVisualiser(graph);
//            visualiser.setVisible(true);
//            visualiser.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            visualiser.setSize(1000, 800);
//            visualiser.setLocationRelativeTo(null);
//            Dijkstra algorithm = new Dijkstra(graph, visualiser);
//            new Thread(() -> {
//                Graph.Node start = graph.getNodeById(65296337);
//                Graph.Node end = graph.getNodes().get(graph.getNodes().size() - 1);
//                List<Graph.Node> solution = algorithm.dijkstra(start, end);
//                System.out.println("Shortest path: " + solution.stream().map(node -> node.id).collect(Collectors.toList()));
//                System.out.println("Path length: " + solution.size());
//                System.out.println("Visited edges: " + algorithm.getVisitedEdges().size());
//                System.out.println("All edges visited: " + algorithm.allEdgesVisited());
//                System.out.println("Path contains: " + solution.size() + " nodes");
//                System.out.println("Algorithm finished");
//            }).start();
//        });
//            GraphVisualiser visualiser = new GraphVisualiser(graph);
//            visualiser.setVisible(true);
//            visualiser.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            visualiser.setSize(1000, 800);
//            visualiser.setLocationRelativeTo(null);
//            Dijkstra algorithm = new Dijkstra(graph, visualiser);
//            new Thread(() -> {
//                Graph.Node start = graph.getNodeById(65296337);
//                Graph.Node end = graph.getNodes().get(graph.getNodes().size() - 1);
//                List<List<Graph.Node>> solution = algorithm.dijkstra(start, end);
//                List<Graph.Node> solution = algorithm.dijkstra(start, end);
//                System.out.println("Shortest path: " + solution.stream().map(node -> node.id).collect(Collectors.toList()));
//                System.out.println("Path length: " + solution.size());
//                System.out.println("Visited edges: " + algorithm.getVisitedEdges().size());
//                System.out.println("All edges visited: " + algorithm.allEdgesVisited());
//                System.out.println("Path contains: " + solution.size() + " nodes");
//                algorithm.validateSolution(solution);
//                algorithm.printDailyRouteSummary(solution);
//                System.out.println("Algorithm finished");
//            }).start();
//        });// 567557898 65296337
//        Graph.Node start = graph.getNodes().get(0);
//        Graph.Node end = graph.getNodes().get(graph.getNodes().size() - 1);
//        if (start != null && end != null) {
//            double totalDistance = 0;
//            double distanceVisited = 0;
//            List<Graph.Node> shortestPath = algorithm.dijkstra(start, end);
//            System.out.println("Visited " + algorithm.getVisitedEdges().size() + " edges");
//            System.out.println("All edges visited: " + algorithm.allEdgesVisited());
//            System.out.println("Path contains: " + shortestPath.size() + " nodes");
//            for (Graph.Edge edge : graph.getAllEdges()) {
//                totalDistance += edge.distance;
//            }
//            for (Graph.Edge edge : algorithm.getVisitedEdges()) {
//                distanceVisited += edge.distance;
//            }
//            System.out.println("Total distance: " + totalDistance);
//            System.out.println("Path distance: " + distanceVisited);
//            visualiser.repaint();
//        } else {
//            System.out.println("Start/End node not found in graph");
//        }
        Overpasser overpasser = new Overpasser();
        overpasser.request(overpasser.query);
        Preprocessor preprocessor = new Preprocessor(overpasser);
        Graph graph = new Graph(overpasser, preprocessor);
        SwingUtilities.invokeLater(() -> {
            GraphVisualiser visualiser = new GraphVisualiser(graph);
            visualiser.setVisible(true);
            GeneticAlgorithm ga = new GeneticAlgorithm(graph, Tuning.POPULATION_SIZE, Tuning.MUTATION_RATE,visualiser);
            new Thread(() -> {
                ga.multiDaySolve(100);
            }).start();
        });
//        for (Graph.Node node : graph.getNodes()) {
//            if (node.id == 65296337) {
//                System.out.println("Node found: " + node);
//                System.out.println("Node ID: " + node.id);
//                System.out.println("Node Latitude: " + node.lat);
//                System.out.println("Node Longitude: " + node.lon);
//            }
//        }
    }
}
