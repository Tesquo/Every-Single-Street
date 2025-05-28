import java.util.*;
import java.util.stream.Collectors;

public class Preprocessor {
    private Overpasser overpasser;
    public Preprocessor(Overpasser overpasser) {
        this.overpasser = overpasser;
    }

// START OF REFACTORING NOT COMPLETED
// =========================================================================================================
//    public List<Graph.Node> copyNodes() {
//        List<Graph.Node> nodes = overpasser.getNodes();
//        List<Graph.Node> newNodes = new ArrayList<>();
//        for (Graph.Node node : nodes) {
//            newNodes.add(new Graph.Node(node.lat, node.lon, node.id));
//        }
//        return newNodes;
//    }
//
//    public List<Graph.Way> copyWays() {
//        List<Graph.Way> ways = overpasser.getWays();
//        List<Graph.Way> newWays = new ArrayList<>();
//        for (Graph.Way way : ways) {
//            newWays.add(new Graph.Way(way.name, way.id, new ArrayList<>(way.nodes), way.tags));
//        }
//        return newWays;
//    }
//
//    public void findIntermediaries(List<Graph.Node> nodes, List<Graph.Way> ways) {
//        Map<Long, Integer> nodeOccurrence = new HashMap<>();
//        for (Graph.Way way : ways) {
//            for (Long nodeID : way.nodes) {
//                nodeOccurrence.put(nodeID, nodeOccurrence.getOrDefault(nodeID, 0) + 1);
//            }
//        }
//        //System.out.println("Node Ocurrences: " + nodeOccurrence);
//        for (Graph.Node node : nodes) {
//            if (nodeOccurrence.getOrDefault(node.id, 0) > 1) {
//                node.Intermediary = true;
//            } else {
//                node.Intermediary = false;
//            }
//        }
//    }
// =========================================================================================================
// END OF REFACTORING NOT COMPLETED

    public List<Graph.Node> findIntermediaries() {
        List<Graph.Node> nodes = overpasser.getNodes();
        ArrayList<Graph.Node> newNodesList = new ArrayList<>();
        Map<Long, Integer> nodeOccurrence = new HashMap<>();
        for (Graph.Way way : overpasser.getWays()) {
            for (Long nodeID : way.nodes) {
                nodeOccurrence.put(nodeID, nodeOccurrence.getOrDefault(nodeID, 0) + 1);
            }
        }
        //System.out.println("Node Ocurrences: " + nodeOccurrence);
        for (Graph.Node node : nodes) {
            if (nodeOccurrence.getOrDefault(node.id, 0) > 1) {
                newNodesList.add(new Graph.Node(node.lat, node.lon, node.id, true));
            } else {
                newNodesList.add(new Graph.Node(node.lat, node.lon, node.id, false));
            }
        }
        return newNodesList;
    }

    private List<Graph.processedWay> splitWayByIntermediaries(Graph.Way way, Map<Long, Graph.Node> intermediateNodeMap) {
        List<Graph.processedWay> newWays = new ArrayList<>();
        List<Graph.Node> currentSegment = new ArrayList<>();
        for (Long nodeId : way.nodes) {
            Graph.Node node = intermediateNodeMap.get(nodeId);
            if (node.Intermediary) {
                currentSegment.add(node);
                newWays.add(new Graph.processedWay(way.name, generateNewWayId(), new ArrayList<>(currentSegment), way.tags));
                currentSegment.clear();
                currentSegment.add(node);
            } else {
                currentSegment.add(new Graph.Node(node.lat, node.lon, node.id, node.Intermediary));
            }
        }
        if (!currentSegment.isEmpty()) {
            newWays.add(new Graph.processedWay(way.name, generateNewWayId(), new ArrayList<>(currentSegment), way.tags));
        }
        return newWays;
    }

    public List<Graph.processedWay> processWaysWithIntermediaries() {
        List<Graph.Node> intermediateNodes = findIntermediaries();
        List<Graph.processedWay> newWays = new ArrayList<>();
        Map<Long, Graph.Node> intermediateNodeMap = intermediateNodes.stream()
                .collect(Collectors.toMap(node -> node.id, node -> node));
        for (Graph.Way way : overpasser.getWays()) {
            newWays.addAll(splitWayByIntermediaries(way, intermediateNodeMap));
        }
        return newWays;
    }

    public List<Graph.processedWay> filterSingleNodeWays() {
        List<Graph.processedWay> filteredWays = new ArrayList<>();
        for (Graph.processedWay way : processWaysWithIntermediaries()) {
            if (!(way.nodes.size() < 2)) {
                filteredWays.add(way);
            } else {
                //System.out.println("NAME: " + way.name + "\nTAGS: " + way.tags + "\n" + way.nodes.size() + "\n");

            }
        }
//        for (Graph.processedWay way : filteredWays) {
//            System.out.println("NAME: " + way.name + "\nTAGS: " + way.tags + "\n" + way.nodes.size() + "\n");
//        }
        return filteredWays;
    }

    private long generateNewWayId() {
        return 0;
    }

    public List<Graph.processedWay> addDistancesToWays() {
        List<Graph.processedWay> newWays = new ArrayList<>();
        for (Graph.processedWay way : filterSingleNodeWays()) {
            double distance = 0;
            for (int i = 0; i < way.nodes.size() - 1; i++) {
                Graph.Node current = way.nodes.get(i);
                Graph.Node next = way.nodes.get(i + 1);
                distance += calculateDistance(current, next);
            }
            Graph.processedWay newWay = new Graph.processedWay(way.name, way.id, way.nodes, way.tags, distance);
            //System.out.println("Processed Way: " + newWay.id + " distance: " + newWay.distance);
            newWays.add(newWay);
        }
        return newWays;
    }

    public List<Graph.processedWay> removeAllButFirstAndLast() {
        List<Graph.processedWay> newWays = new ArrayList<>();
        for (Graph.processedWay way : addDistancesToWays()) {
            Graph.Node first = way.nodes.getFirst();
            Graph.Node last = way.nodes.getLast();
            List<Graph.Node> newNodesList = new ArrayList<>();
            newNodesList.add(first);
            newNodesList.add(last);
            newWays.add(new Graph.processedWay(way.name, way.id, newNodesList, way.tags, way.distance));
            //System.out.println(way.nodes.getFirst().id + " " + way.nodes.getLast().id + " " + way.distance);
        }
        return newWays;
    }

    public List<Graph.processedWay> getBiggestComponent() {
        List<Graph.processedWay> currentWays = removeAllButFirstAndLast();
        Set<Long> wayNodeIds = getNodeIdsFromWays(currentWays);
        Map<Long, Integer> nodeIdToIndex = new HashMap<>();
        int index = 0;
        for (Long nodeId : wayNodeIds) {
            nodeIdToIndex.put(nodeId, index++);
        }
        Union uf = new Union(nodeIdToIndex.size());
        for (Graph.processedWay way : currentWays) {
            Long nodeAId = way.nodes.getFirst().id;
            Long nodeBId = way.nodes.getLast().id;
            int idxA = nodeIdToIndex.get(nodeAId);
            int idxB = nodeIdToIndex.get(nodeBId);
            uf.unionFind(idxA, idxB);
        }
        int maxSize = 0;
        int largestRoot = -1;
        Map<Integer, Integer> rootSizes = new HashMap<>();
        for (Long nodeId : wayNodeIds) {
            int idx = nodeIdToIndex.get(nodeId);
            int root = uf.find(idx);
            int size = rootSizes.getOrDefault(root, 0) + 1;
            rootSizes.put(root, size);
            if (size > maxSize) {
                maxSize = size;
                largestRoot = root;
            }
        }
        Set<Long> largestComponentNodeIds = new HashSet<>();
        for (Long nodeId : wayNodeIds) {
            int idx = nodeIdToIndex.get(nodeId);
            if (uf.find(idx) == largestRoot) {
                largestComponentNodeIds.add(nodeId);
            }
        }
        List<Graph.processedWay> filteredWays = currentWays.stream()
                .filter(way -> largestComponentNodeIds.contains(way.nodes.getFirst().id) &&
                        largestComponentNodeIds.contains(way.nodes.getLast().id))
                .collect(Collectors.toList());

        return filteredWays;
    }

    private Set<Long> getNodeIdsFromWays(List<Graph.processedWay> ways) {
        Set<Long> nodeIds = new HashSet<>();
        for (Graph.processedWay way : ways) {
            nodeIds.add(way.nodes.getFirst().id);
            nodeIds.add(way.nodes.getLast().id);
        }
        return nodeIds;
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
}
