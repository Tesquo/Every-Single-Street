import java.util.*;
import java.util.stream.Collectors;

public class Algorithm {
    private Graph graph;
    private List<Graph.Edge> visitedEdges;
    private GraphVisualiser visualiser;
    private Graph.Node depot;

    public Algorithm(Graph graph, GraphVisualiser visualiser, long depot) {
        this.graph = graph;
        this.visitedEdges = new ArrayList<>();
        this.visualiser = visualiser;
        this.depot = getNodebyID(depot);
    }
    public List<List<Graph.Node>> ModiDijkstra() {
        List<List<Graph.Node>> dailyRoutes = new ArrayList<>();
        Set<Graph.Edge> remainingEdges = new HashSet<>(graph.getAllEdges());

        while (!remainingEdges.isEmpty()) {
            List<Graph.Node> dailyRoute = new ArrayList<>();
            dailyRoute.add(depot);
            double currentDistance = 0;
            Graph.Node currentNode = depot;
            Set<Graph.Edge> todayEdges = new HashSet<>();
            while (currentDistance < Tuning.MAX_DISTANCE && !remainingEdges.isEmpty()) {
                Graph.Edge nearestEdge = findNearestUnvisitedEdge(currentNode, remainingEdges);
                if (nearestEdge == null) {
                    for (Graph.Edge edge : remainingEdges) {
                        List<Graph.Node> path = dijkstra(currentNode, edge.x);
                        if (!path.isEmpty()) {
                            nearestEdge = edge;
                            break;
                        }
                        path = dijkstra(currentNode, edge.y);
                        if (!path.isEmpty()) {
                            nearestEdge = new Graph.Edge(edge.y, edge.x, edge.distance);
                            break;
                        }
                    }
                    if (nearestEdge == null) break;
                }
                List<Graph.Node> pathToEdge = dijkstra(currentNode, nearestEdge.x);
                if (pathToEdge.isEmpty()) {
                    pathToEdge = dijkstra(currentNode, nearestEdge.y);
                    if (!pathToEdge.isEmpty()) {
                        nearestEdge = new Graph.Edge(nearestEdge.y, nearestEdge.x, nearestEdge.distance);
                    } else {
                        break;
                    }
                }
                double pathDistance = calculatePathDistance(pathToEdge) + nearestEdge.distance;
                double returnDistance = calculatePathDistance(dijkstra(nearestEdge.y, depot));

                if (currentDistance + pathDistance + returnDistance > Tuning.MAX_DISTANCE) {
                    break;
                }
                if (!pathToEdge.isEmpty()) {
                    dailyRoute.addAll(pathToEdge.subList(1, pathToEdge.size()));
                    currentDistance += calculatePathDistance(pathToEdge);
                }
                dailyRoute.add(nearestEdge.y);
                currentDistance += nearestEdge.distance;
                visitedEdges.add(nearestEdge);
                todayEdges.add(nearestEdge);
                Graph.Edge reverseEdge = graph.getEdge(nearestEdge.y, nearestEdge.x);
                if (reverseEdge != null) {
                    todayEdges.add(reverseEdge);
                }
                currentNode = nearestEdge.y;
                if (visualiser != null) {
                    visualiser.edgeVisited(nearestEdge);
                }
            }
            if (!currentNode.equals(depot)) {
                List<Graph.Node> returnPath = dijkstra(currentNode, depot);
                if (!returnPath.isEmpty()) {
                    dailyRoute.addAll(returnPath.subList(1, returnPath.size()));
                    currentDistance += calculatePathDistance(returnPath);
                }
            }
            remainingEdges.removeAll(todayEdges);
            for (Graph.Edge edge : new HashSet<>(remainingEdges)) {
                Graph.Edge reverse = graph.getEdge(edge.y, edge.x);
                if (todayEdges.contains(reverse)) {
                    remainingEdges.remove(edge);
                }
            }
            dailyRoutes.add(dailyRoute);
            printDailyRouteSummary(dailyRoutes);
        }
        return dailyRoutes;
    }

    private Graph.Edge findNearestUnvisitedEdge(Graph.Node from, Set<Graph.Edge> remainingEdges) {
        Graph.Edge nearestEdge = null;
        double minDistance = Double.MAX_VALUE;

        for (Graph.Edge edge : remainingEdges) {
            List<Graph.Node> pathToX = dijkstra(from, edge.x);
            List<Graph.Node> pathToY = dijkstra(from, edge.y);
            if (!pathToX.isEmpty()) {
                double distance = calculatePathDistance(pathToX);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestEdge = edge;
                }
            }
            if (!pathToY.isEmpty()) {
                double distance = calculatePathDistance(pathToY);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestEdge = new Graph.Edge(edge.y, edge.x, edge.distance); // Reverse edge
                }
            }
        }
        return nearestEdge;
    }

    private double calculatePathDistance(List<Graph.Node> path) {
        if (path == null || path.size() < 2) return 0;

        double distance = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Graph.Node from = path.get(i);
            Graph.Node to = path.get(i+1);
            for (Graph.Edge edge : graph.getNeighbours(from)) {
                if (edge.y.equals(to)) {
                    distance += edge.distance;
                    break;
                }
            }
        }
        return distance;
    }

    public List<Graph.Node> dijkstra(Graph.Node x, Graph.Node y) {
        if (visualiser != null) visualiser.clearVisited();
        Map<Graph.Node, Double> distances = graph.initialiseDistances(x);
        PriorityQueue<Graph.Node> pq = new PriorityQueue<>(Comparator.comparingDouble(node -> distances.getOrDefault(node, Double.MAX_VALUE)));
        Map<Graph.Node, Graph.Node> predecessors = new HashMap<>();
        Set<Graph.Node> visited = new HashSet<>();
        pq.offer(x);
        while (!pq.isEmpty()) {
            Graph.Node current = pq.poll();
            if (current.equals(y)) {
                break;
            }
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);
            for (Graph.Edge edge : graph.getNeighbours(current)) {
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
        }
        if (visualiser != null) {
            visualisePath(predecessors, x, y);
        }
        return reconstructPath(predecessors, x, y);
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

    private Graph.Node getNodebyID(long id) {
        return graph.getNodeById(id);
    }
    public void printDailyRouteSummary(List<List<Graph.Node>> dailyRoutes) {
        System.out.println("\n=== DAILY ROUTE SUMMARY ===");
        System.out.printf("Total days required: %d\n", dailyRoutes.size());

        for (int day = 0; day < dailyRoutes.size(); day++) {
            List<Graph.Node> route = dailyRoutes.get(day);
            double distance = calculatePathDistance(route);
            int edgesCovered = countEdgesInRoute(route);

            System.out.printf("\nDay %d:\n", day + 1);
            System.out.printf("- Distance traveled: %.2f km (%.1f%% of max)\n",
                    distance, (distance/Tuning.MAX_DISTANCE)*100);
            System.out.printf("- Edges covered: %d\n", edgesCovered);
            System.out.printf("- Route nodes: %d\n", route.size());
            System.out.printf("- Start/End: %s -> %s\n",
                    route.get(0).id, route.get(route.size()-1).id);

            // Optional: Print first and last 3 nodes if route is long
            if (route.size() > 6) {
                System.out.print("- Path: [" + route.get(0).id);
                for (int i = 1; i < 3; i++) System.out.print(", " + route.get(i).id);
                System.out.print(", ..., " + route.get(route.size()-3).id);
                System.out.print(", " + route.get(route.size()-2).id);
                System.out.println(", " + route.get(route.size()-1).id + "]");
            } else {
                System.out.println("- Path: " + route.stream()
                        .map(node -> String.valueOf(node.id))
                        .collect(Collectors.joining(" -> ")));
            }
        }
    }

    private int countEdgesInRoute(List<Graph.Node> route) {
        int count = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            Graph.Node from = route.get(i);
            Graph.Node to = route.get(i+1);
            if (graph.getEdge(from, to) != null) {
                count++;
            }
        }
        return count;
    }

    public void validateSolution(List<List<Graph.Node>> dailyRoutes) {
        System.out.println("\n=== VALIDATING SOLUTION ===");
        Set<Graph.Edge> allEdges = new HashSet<>(graph.getAllEdges());
        Set<Graph.Edge> coveredEdges = new HashSet<>();

        for (List<Graph.Node> route : dailyRoutes) {
            for (int i = 0; i < route.size() - 1; i++) {
                Graph.Edge edge = graph.getEdge(route.get(i), route.get(i+1));
                if (edge != null) coveredEdges.add(edge);
            }
        }
        allEdges.removeAll(coveredEdges);
        if (allEdges.isEmpty()) {
            System.out.println("✓ All edges covered!");
        } else {
            System.out.printf("✗ Missing %d edges:\n", allEdges.size());
            allEdges.forEach(e -> System.out.printf("  %d->%d (%.2f km)\n",
                    e.x.id, e.y.id, e.distance));
        }
        boolean distanceValid = true;
        for (int i = 0; i < dailyRoutes.size(); i++) {
            double distance = calculatePathDistance(dailyRoutes.get(i));
            if (distance > Tuning.MAX_DISTANCE) {
                System.out.printf("✗ Day %d exceeds max distance: %.2f > %.2f km\n",
                        i+1, distance, Tuning.MAX_DISTANCE);
                distanceValid = false;
            }
        }
        if (distanceValid) {
            System.out.println("✓ All days within distance limit");
        }
        boolean depotValid = true;
        for (List<Graph.Node> route : dailyRoutes) {
            if (!route.get(0).equals(depot)) {
                System.out.printf("✗ Day starts at wrong node: %d (should be %d)\n",
                        route.get(0).id, depot.id);
                depotValid = false;
            }
            if (!route.get(route.size()-1).equals(depot)) {
                System.out.printf("✗ Day ends at wrong node: %d (should be %d)\n",
                        route.get(route.size()-1).id, depot.id);
                depotValid = false;
            }
        }
        if (depotValid) {
            System.out.println("✓ All days start/end at depot");
        }
    }
}
