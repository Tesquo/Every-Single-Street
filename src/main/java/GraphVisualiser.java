import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class GraphVisualiser extends JFrame {
    private Graph graph;
    private double minLat, maxLat, minLon, maxLon;
    private double latRange, lonRange;
    private double scalar;
    private static final int MARGIN = 20;
    private final Set<Graph.Edge> visitedEdges = Collections.synchronizedSet(new HashSet<>());
    private Set<Graph.Edge> allEdges = new HashSet<>();
    private final Set<Graph.Edge> globallyCoveredEdges = Collections.synchronizedSet(new HashSet<>());
    private Set<Graph.Edge> currentBestPathEdges = new HashSet<>();
    private final Object lock = new Object();

    public GraphVisualiser(Graph graph) {
        this.graph = graph;
        findLatLonRange();
        calculateScalar();
        setTitle("Graph Visualiser");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(new GraphPanel());
        setLocationRelativeTo(null);
    }
    private int getXCoordinate(double lon) {
        return (int) ((lon - minLon) * scalar) + MARGIN;
    }
    private int getYCoordinate(double lat) {
        return (int) ((maxLat - lat) * scalar) + MARGIN;
    }
    private void findLatLonRange() {
        minLat = Double.MAX_VALUE;
        maxLat = Double.MIN_VALUE;
        minLon = Double.MAX_VALUE;
        maxLon = Double.MIN_VALUE;
        for (Graph.Node node : graph.getNodes()) {
            if (node.lat < minLat) minLat = node.lat;
            if (node.lat > maxLat) maxLat = node.lat;
            if (node.lon < minLon) minLon = node.lon;
            if (node.lon > maxLon) maxLon = node.lon;
        }
        //System.out.printf("Lat range: [%f, %f], Lon range: [%f, %f]\n", minLat, maxLat, minLon, maxLon);
    }

    private void calculateScalar() {
        double latRange = maxLat - minLat;
        double lonRange = maxLon - minLon;
        double minRange = Math.min(latRange, lonRange);
        scalar = (800 - 2 * MARGIN) / minRange;
    }

    private class GraphPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Set<Graph.Edge> edgesToDraw;
            Set<Graph.Edge> currentPathToDraw;
            Set<Graph.Edge> allEdges = graph.getAllEdges();
            synchronized (lock) {
                edgesToDraw = new HashSet<>(visitedEdges);
                currentPathToDraw = new HashSet<>(currentBestPathEdges);
            }
            g.setColor(Color.GREEN);
            for (Graph.Edge edge : allEdges) {
                drawEdge(g, edge);
            }

            // Draw visited edges in blue
            g.setColor(Color.RED);
            for (Graph.Edge edge : edgesToDraw) {
                drawEdge(g, edge);
            }

            // Draw current path edges in red
            g.setColor(Color.BLUE);
            for (Graph.Edge edge : currentPathToDraw) {
                drawEdge(g, edge);
            }

            // Draw nodes
            g.setColor(Color.BLACK);
            for (Graph.Node node : graph.getNodes()) {
                int x = getXCoordinate(node.lon);
                int y = getYCoordinate(node.lat);
                g.fillOval(x - 2, y - 2, 4, 4);
            }
        }

        private void drawEdge(Graphics g, Graph.Edge edge) {
            int x1 = getXCoordinate(edge.x.lon);
            int y1 = getYCoordinate(edge.x.lat);
            int x2 = getXCoordinate(edge.y.lon);
            int y2 = getYCoordinate(edge.y.lat);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    public void edgeVisited(final Graph.Edge edge) {
        SwingUtilities.invokeLater(() -> {
            visitedEdges.add(edge);
            repaint();
        });
    }

    public void updateVisitedEdges(Set<Graph.Edge> newlyVisited) {
        visitedEdges.addAll(newlyVisited);
        repaint();
    }


    public void markEdgeVisited(Graph.Edge edge) {
        synchronized (lock) {
            visitedEdges.add(edge);
        }
        SwingUtilities.invokeLater(this::repaint);
    }
    public void markEdgesVisited(Collection<Graph.Edge> edges) {
        synchronized (lock) {
            visitedEdges.addAll(edges);
        }
        SwingUtilities.invokeLater(this::repaint);
    }
    public void addPathEdge(Graph.Edge edge) {
        synchronized (lock) {
            allEdges.add(edge);
        }
        SwingUtilities.invokeLater(this::repaint);
    }

    public void clearVisited() {
        synchronized (lock) {
            visitedEdges.clear();
            allEdges.clear();
        }
        SwingUtilities.invokeLater(this::repaint);
    }

    public void updateCurrentPath(List<Graph.Node> pathNodes) {
        synchronized (lock) {
            currentBestPathEdges.clear();
            for (int i = 0; i < pathNodes.size() - 1; i++) {
                Graph.Edge edge = graph.getEdge(pathNodes.get(i), pathNodes.get(i + 1));
                if (edge != null) {
                    currentBestPathEdges.add(edge);
                }
            }
        }
        SwingUtilities.invokeLater(this::repaint);
    }

    public static void main(String[] args) {
        Overpasser overpasser = new Overpasser();
        overpasser.request(overpasser.query);
        Preprocessor preprocessor = new Preprocessor(overpasser);
        Graph graph = new Graph(overpasser, preprocessor);
        SwingUtilities.invokeLater(() -> {
            GraphVisualiser graphVisualiser = new GraphVisualiser(graph);
            graphVisualiser.setVisible(true);
        });
        System.out.println(graph.isFullyConnected());
    }
}
