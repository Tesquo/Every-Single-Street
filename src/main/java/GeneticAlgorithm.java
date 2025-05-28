import java.util.*;
import java.util.stream.Collectors;


public class GeneticAlgorithm {
    private Graph graph;
    private GraphVisualiser visualiser;
    private int populationSize;
    private double mutationRate;
    private List<Path> population;
    private Random random;
    private int generationCount = 0;
    private Path bestPath;
    private Set<Graph.Edge> globallyCoveredEdges = new HashSet<>();
    private int currentDay = 1;
    private List<Path> dailyPaths = new ArrayList<>();
    private Map<Graph.Edge, Integer> edgeVisitCount = new HashMap<>();

    public GeneticAlgorithm(Graph graph, int populationSize, double mutationRate, GraphVisualiser visualiser) {
        this.graph = graph;
        this.populationSize = populationSize;
        this.mutationRate = mutationRate;
        this.visualiser = visualiser;
        this.random = new Random();
        initializePopulation();
    }

    public class Path {
        List<Graph.Node> nodes;
        Set<Graph.Edge> coveredEdges;
        double fitness;
        double totalDistance;
        double maxCumulativeDistance;

        public Path(List<Graph.Node> nodes) {
            this.nodes = nodes;
            calculateCoveredEdges();
            calculateFitness();
            calculateDistanceMetrics();
        }

        private void calculateCoveredEdges() {
            coveredEdges = new HashSet<>();
            for (int i = 0; i < nodes.size() - 1; i++) {
                Graph.Node x = nodes.get(i);
                Graph.Node y = nodes.get(i + 1);
                Graph.Edge edge = graph.getEdge(x, y);
                if (edge != null) {
                    coveredEdges.add(edge);
                    edge.visited = true;
                    edgeVisitCountUpdate(edge);
                    if (visualiser != null) {
                        visualiser.addPathEdge(edge);
                    }
                }
            }
        }

        private void calculateFitness() {
            fitness = 0.0;
            int totalEdges = graph.getAllEdges().size();
            int newEdges = (int) coveredEdges.stream()
                    .filter(e -> !globallyCoveredEdges.contains(e))
                    .count();
            for (int i = 0; i < newEdges; i++) {
                fitness += 0.1;
            }
            if (totalDistance > Tuning.MAX_DISTANCE) {
                fitness *= 0.1;
            }
            double avgEdgeVisits = coveredEdges.stream()
                    .mapToInt(e -> edgeVisitCount.getOrDefault(e, 0))
                    .average()
                    .orElse(0);
            if (avgEdgeVisits > 5) {
                fitness *= 0.1;
            }
            int uniqueDistance = calculateUniqueDistance();
            if (uniqueDistance > 0) {
                fitness += 0.1;
            }
            if (uniqueDistance > 5) {
                fitness *= 1.5;
            }
            if (uniqueDistance > 15) {
                fitness *= 2;
            }
            fitness = Math.max(0, fitness);
        }

        private int calculateUniqueDistance() {
            totalDistance = 0;
            for (Graph.Edge edge : coveredEdges) {
                totalDistance += edge.distance;
            }
            return 0;
        }

        private void calculateDistanceMetrics() {
            totalDistance = 0;
            maxCumulativeDistance = 0;
            double cumulativeDistance = 0;

            for (int i = 0; i < nodes.size() - 1; i++) {
                Graph.Edge edge = graph.getEdge(nodes.get(i), nodes.get(i+1));
                if (edge != null) {
                    totalDistance += edge.distance;
                    cumulativeDistance += edge.distance;
                    maxCumulativeDistance = Math.max(maxCumulativeDistance, cumulativeDistance);
                }
            }
        }

        public String toString() {
            return String.format("Path[Nodes: %d, Edges: %d/%d (%.1f%%), Distance: %.2f]",
                    nodes.size(),
                    coveredEdges.size(),
                    graph.getAllEdges().size(),
                    fitness * 100,
                    totalDistance);
        }

        public String getNodeSequence() {
            return nodes.stream()
                    .map(node -> String.valueOf(node.id))
                    .collect(Collectors.joining(" -> "));
        }

        public String getEdgeCoverageReport() {
            Set<Graph.Edge> allEdges = graph.getAllEdges();
            StringBuilder report = new StringBuilder();
            report.append("Edge Coverage Report:\n");
            report.append(String.format("Covered %d/%d edges\n",
                    coveredEdges.size(), allEdges.size()));

//            report.append("\nMissing edges:\n");
//            for (Graph.Edge edge : allEdges) {
//                if (!coveredEdges.contains(edge)) {
//                    report.append(String.format("  %d -> %d (%.2f)\n",
//                            edge.x.id, edge.y.id, edge.distance));
//                }
//            }

            return report.toString();
        }
    }
    private void edgeVisitCountUpdate(Graph.Edge edge) {
        edgeVisitCount.put(edge, edgeVisitCount.getOrDefault(edge, 0) + 1);
    }
    private void initializePopulation() {
        population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            population.add(generateRandomPath());
        }
    }

    private Path generateRandomPath() {
        List<Graph.Node> path = new ArrayList<>();
        path.add(getDailyStartNode(currentDay));
        Graph.Node current = path.get(0);
        double currentDistance = 0;
        double maxCumulativeDistance = 0;
        Set<Graph.Edge> covered = new HashSet<>();

        while (currentDistance < Tuning.MAX_DISTANCE * 0.9) {
            List<Graph.Edge> neighbours = graph.getNeighbours(current);
            if (neighbours.isEmpty()) break;
            neighbours.sort(Comparator.comparingInt((Graph.Edge e) ->
                            edgeVisitCount.getOrDefault(e, 0))
                    .thenComparingDouble(e -> e.distance));
            Graph.Edge nextEdge = null;
            for (Graph.Edge edge : neighbours) {
                if (currentDistance + edge.distance <= Tuning.MAX_DISTANCE) {
                    nextEdge = edge;
                    break;
                }
            }
            if (nextEdge == null) break;
            path.add(nextEdge.y);
            covered.add(nextEdge);
            currentDistance += nextEdge.distance;
            current = nextEdge.y;
            visualiser.markEdgesVisited(covered);
        }
        return new Path(path);
    }

    private List<Graph.Node> findPathTowardStart(Graph.Node from, double maxDistance) {
        List<Graph.Node> path = new ArrayList<>();
        Graph.Node current = from;
        double remaining = maxDistance;

        while (remaining > 0 && !current.equals(getDailyStartNode(currentDay))) {
            List<Graph.Edge> neighbours = graph.getNeighbours(current);
            if (neighbours.isEmpty()) {break;}
            neighbours.sort(Comparator.comparingDouble(
                    e -> graph.calculateDistance(getDailyStartNode(currentDay), e.y)));
            Graph.Edge best = neighbours.get(0);
            if (best.distance > remaining) {break;}
            path.add(best.y);
            remaining -= best.distance;
            current = best.y;
        }
        return path;
    }

    public void evolve(int generations) {
        bestPath = getFittest();
        System.out.println("Initial best: " + bestPath);
        int explorationGens = (int)(generations * 0.2);

        for (generationCount = 0; generationCount < generations; generationCount++) {
            double currentMutationRate = generationCount < explorationGens ? mutationRate * 1.5 : mutationRate;
            List<Path> newPopulation = new ArrayList<>();
            newPopulation.add(getFittest());

            while (newPopulation.size() < populationSize) {
                Path parent1 = selectParent();
                Path parent2 = selectParent();
                Path child = crossover(parent1, parent2);
                if (random.nextDouble() < currentMutationRate) {
                    mutate(child);
                }
                newPopulation.add(child);
                visualiser.markEdgesVisited(bestPath.coveredEdges);
            }
            population = newPopulation;
            Path currentBest = getFittest();
            if (currentBest.fitness > bestPath.fitness) {
                bestPath = new Path(new ArrayList<>(currentBest.nodes));
            }
            printGenerationStats();
            if (hasFullCoverage()) {
                System.out.println("\nFound perfect solution in generation " + generationCount);
                break;
            }
        }
        printFinalReport();
    }

    private Path selectParent() {
        int tournamentSize = Tuning.TOURNAMENT_SIZE;
        List<Path> candidates = new ArrayList<>();
        for (int i = 0; i < tournamentSize; i++) {
            Path candidate = population.get(random.nextInt(populationSize));
            candidates.add(candidate);
        }
        return candidates.stream()
                .max(Comparator.comparingDouble(p -> {
                    double avgVisits = p.coveredEdges.stream()
                            .mapToInt(e -> edgeVisitCount.getOrDefault(e, 0))
                            .average()
                            .orElse(0);
                    double uniqueness = 1.0 / (1.0 + avgVisits);
                    return p.fitness * (1.0 + uniqueness * 0.5);
                }))
                .orElse(candidates.get(0));
    }

    private Path crossover(Path parent1, Path parent2) {
        double crossoverProb = Tuning.CROSSOVER_RATE;
        if (random.nextDouble() > crossoverProb) {
            return random.nextBoolean() ?
                    new Path(new ArrayList<>(parent1.nodes)) :
                    new Path(new ArrayList<>(parent2.nodes));
        }
        Set<Graph.Node> commonNodes = new HashSet<>(parent1.nodes);
        commonNodes.retainAll(new HashSet<>(parent2.nodes));
        if (commonNodes.isEmpty()) {
            return random.nextBoolean() ?
                    new Path(new ArrayList<>(parent1.nodes)) :
                    new Path(new ArrayList<>(parent2.nodes));
        }
        Graph.Node crossoverPoint = commonNodes.stream()
                .min(Comparator.comparingInt(node ->
                        Math.abs(parent1.nodes.indexOf(node) - parent2.nodes.indexOf(node))))
                .orElse(parent1.nodes.get(0));
        int idx1 = parent1.nodes.indexOf(crossoverPoint);
        int idx2 = parent2.nodes.indexOf(crossoverPoint);
        if (idx1 == -1 || idx2 == -1) {
            return random.nextBoolean() ?
                    new Path(new ArrayList<>(parent1.nodes)) :
                    new Path(new ArrayList<>(parent2.nodes));
        }
        List<Graph.Node> childNodes = new ArrayList<>();
        childNodes.addAll(parent1.nodes.subList(0, idx1 + 1));
        childNodes.addAll(parent2.nodes.subList(idx2 + 1, parent2.nodes.size()));
        return new Path(childNodes);
    }

    private void mutate(Path path) {
        if (random.nextDouble() > Tuning.MUTATION_RATE) {return;}
        if (random.nextDouble() < 0.7) {
            List<Graph.Edge> possibleEdges = new ArrayList<>();
            for (Graph.Node node : path.nodes) {
                possibleEdges.addAll(graph.getNeighbours(node).stream()
                        .filter(e -> !path.coveredEdges.contains(e))
                        .collect(Collectors.toList()));
            }
            if (!possibleEdges.isEmpty()) {
                possibleEdges.sort(Comparator.comparingInt(e ->
                        edgeVisitCount.getOrDefault(e, 0)));

                Graph.Edge newEdge = possibleEdges.get(0);
                int insertPos = path.nodes.indexOf(newEdge.x);
                if (insertPos >= 0 && insertPos < path.nodes.size() - 1) {
                    path.nodes.add(insertPos + 1, newEdge.y);
                }
            }
        } else {
            if (path.nodes.size() > 2) {
                int i = random.nextInt(path.nodes.size() - 1);
                int j = random.nextInt(path.nodes.size() - 1);
                Graph.Node nodeI = path.nodes.get(i);
                Graph.Node nodeJ = path.nodes.get(j);
                if (graph.areConnected(nodeI, nodeJ)) {
                    Collections.swap(path.nodes, i, j);
                }
            }
        }
        path.calculateCoveredEdges();
        path.calculateDistanceMetrics();
        path.calculateFitness();
    }

    private double calculatePathDistance(List<Graph.Node> nodes) {
        double distance = 0;
        for (int i = 0; i < nodes.size() - 1; i++) {
            Graph.Edge edge = graph.getEdge(nodes.get(i), nodes.get(i+1));
            if (edge != null) distance += edge.distance;
        }
        return distance;
    }

    private void extendPathTowardFarthestPoint(Path path) {
        if (path.nodes.size() < 2) return;
        Graph.Node farthest = path.nodes.stream()
                .max(Comparator.comparingDouble(n ->
                        graph.calculateDistance(getDailyStartNode(currentDay), n)))
                .orElse(path.nodes.get(0));

        List<Graph.Edge> neighbours = graph.getNeighbours(farthest);
        if (!neighbours.isEmpty()) {
            neighbours.sort((e1, e2) -> Double.compare(
                    graph.calculateDistance(getDailyStartNode(currentDay), e2.y),
                    graph.calculateDistance(getDailyStartNode(currentDay), e1.y)));

            Graph.Edge extension = neighbours.get(0);
            int insertPos = path.nodes.lastIndexOf(farthest) + 1;
            if (insertPos <= path.nodes.size()) {
                path.nodes.add(insertPos, extension.y);
            }
        }
    }

    public Path getFittest() {
        return Collections.max(population, Comparator.comparingDouble(p -> p.fitness));
    }

    public boolean hasFullCoverage() {
        return globallyCoveredEdges.size() >= graph.getAllEdges().size();
    }

    private void printGenerationStats() {

        Path currentBest = getFittest();
        System.out.printf("\nGeneration %d:\n", generationCount);
        System.out.printf("  Current best: %s\n", currentBest);
        System.out.printf("  Population fitness: Avg=%.3f, Max=%.3f, Min=%.3f\n",
                calculateAverageFitness(),
                currentBest.fitness,
                getWorstFitness());

        if (generationCount == Tuning.MAX_GENERATIONS) {
            System.out.println("\nCurrent best path details:");
            System.out.println("  Node sequence: " +
                    currentBest.nodes.stream()
                            .limit(10)
                            .map(n -> String.valueOf(n.id))
                            .collect(Collectors.joining(" -> ")) +
                    (currentBest.nodes.size() > 10 ? " -> ..." : ""));

            System.out.println("  Coverage details:");
            System.out.println("    Unique edges covered: " + currentBest.coveredEdges.size());
            System.out.println("    Total distance: " + currentBest.totalDistance);
        }
    }

    private void printFinalReport() {
        System.out.println("\n=== FINAL REPORT ===");
        System.out.println("Best solution found after " + generationCount + " generations:");
        System.out.println(bestPath);

        System.out.println("\nPath details:");
        System.out.println("  Total nodes: " + bestPath.nodes.size());
        System.out.println("  Total edges traversed: " + (bestPath.nodes.size() - 1));
        System.out.println("  Unique edges covered: " + bestPath.coveredEdges.size());
        System.out.println("  Total distance: " + bestPath.totalDistance);

        System.out.println("\nFirst 20 nodes in path:");
        System.out.println(bestPath.nodes.stream()
                .limit(20)
                .map(n -> String.valueOf(n.id))
                .collect(Collectors.joining(" -> ")));

        System.out.println("\n" + bestPath.getEdgeCoverageReport());
    }

    private double calculateAverageFitness() {
        return population.stream()
                .mapToDouble(p -> p.fitness)
                .average()
                .orElse(0);
    }

    private double getWorstFitness() {
        return population.stream()
                .mapToDouble(p -> p.fitness)
                .min()
                .orElse(0);
    }

    public void multiDaySolve(int maxDays) {
        while (currentDay <= maxDays && !hasFullCoverage()) {
            System.out.println("\n=== DAY " + currentDay + " ===");
            edgeVisitCount.clear();
            initializePopulation();
            evolve(Tuning.MAX_GENERATIONS);
            Path bestDailyPath = getFittest();
            dailyPaths.add(bestDailyPath);
            globallyCoveredEdges.addAll(bestDailyPath.coveredEdges);
            visualiser.markEdgesVisited(globallyCoveredEdges);
            visualiser.updateCurrentPath(getFittest().nodes);
            currentDay++;
        }
        printFinalMultiDayReport();
    }

    private void printFinalMultiDayReport() {
        System.out.println("\n=== MULTI-DAY SOLUTION ===");
        System.out.printf("Completed in %d days\n", currentDay - 1);
        System.out.printf("Total edges covered: %d/%d (%.1f%%)\n",
                globallyCoveredEdges.size(),
                graph.getAllEdges().size(),
                (double) globallyCoveredEdges.size() / graph.getAllEdges().size() * 100);

        System.out.println("\nDaily breakdown:");
        for (int i = 0; i < dailyPaths.size(); i++) {
            Path dayPath = dailyPaths.get(i);
            System.out.printf("Day %d: %d edges, %.2f km\n",
                    i + 1,
                    dayPath.coveredEdges.size(),
                    dayPath.totalDistance);
        }
    }

    private Graph.Node getDailyStartNode(int day) {
        return graph.getNodeById(65296337);
    }
}