import java.util.*;

public class Dijkstra {
    private Graph graph;
    private List<Graph.Edge> visitedEdges;
    private GraphVisualiser visualiser;

    public Dijkstra(Graph graph, GraphVisualiser visualiser) {
        this.graph = graph;
        this.visitedEdges = new ArrayList<>();
        this.visualiser = visualiser;
    }

    public List<Graph.Node> dijkstra(Graph.Node x, Graph.Node y) {
        if (visualiser != null) visualiser.clearVisited();
        Map<Graph.Node, Double> distances = graph.initialiseDistances(x);
        PriorityQueue<Graph.Node> pq = new PriorityQueue<>(Comparator.comparingDouble(node -> distances.getOrDefault(node, Double.MAX_VALUE)));
        Map<Graph.Node, Graph.Node> predecessors = new HashMap<>();
        pq.offer(x);
        Set<Graph.Edge> edgesToVisit = new HashSet<>(graph.getAllEdges());
        List<Graph.Node> shortestPath = new ArrayList<>();
        while (!pq.isEmpty() && !edgesToVisit.isEmpty()) {
            Graph.Node current = pq.poll();
            for (Graph.Edge edge : graph.getNeighbours(current)) {
                visitedEdges.add(edge);
                if (visualiser != null) {
                    visualiser.edgeVisited(edge);
                }
                double newDistance = distances.get(current) + edge.distance;
                if (newDistance < distances.getOrDefault(edge.y, Double.MAX_VALUE)) {
                    distances.put(edge.y, newDistance);
                    predecessors.put(edge.y, current);
                    pq.offer(edge.y);
                }
            }
            if (current.equals(y) && shortestPath.isEmpty()) {
                shortestPath = reconstructPath(predecessors, x, y);
            }
        }
        if (visualiser != null) {
            visualisePath(predecessors, x, y);
        }
        return shortestPath;
    }

    private List<Graph.Node> reconstructPath(Map<Graph.Node, Graph.Node> predecessors, Graph.Node x, Graph.Node y) {
        List<Graph.Node> path = new LinkedList<>();
        Graph.Node current = y;
        while (current != null && !current.equals(x)) {
            path.add(0, current);
            current = predecessors.get(current);
        }
        if (current != null) {
            path.add(0, x);
        }
        return path;
    }

    private void visualisePath(Map<Graph.Node, Graph.Node> predecessors, Graph.Node x, Graph.Node y) {
        Graph.Node current = y;
        while (current != null && !current.equals(x)) {
            Graph.Node predecessor = predecessors.get(current);
            if (predecessor != null) {
                for (Graph.Edge edge : graph.getNeighbours(predecessor)) {
                    if (edge.y.equals(current)) {
                        visualiser.addPathEdge(edge);
                    }
                }
            }
            current = predecessor;
        }
    }

    public List<Graph.Edge> getVisitedEdges() {
        return visitedEdges;
    }

    public boolean allEdgesVisited() {
        int count = 0;
        Set<Graph.Edge> allEdges = new HashSet<>(graph.getAllEdges());
        for (Graph.Edge edge : allEdges) {
            count++;
        }
        System.out.println("ALL EDGES\n" + count);
        Set<Graph.Edge> visitedEdgeSet = new HashSet<>(visitedEdges);
        count = 0;
        for (Graph.Edge edge: visitedEdgeSet) {
            count++;
        }
        System.out.println("VISITED EDGES\n" + count);
        for (Graph.Edge edge : allEdges) {
            if (!visitedEdgeSet.contains(edge)) {
                System.out.println("Edge not visited: " + edge);
                return false;
            }
        }
        return true;
    }
}