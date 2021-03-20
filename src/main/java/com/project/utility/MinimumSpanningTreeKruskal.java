package com.project.utility;

import com.project.model.Subset;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

import java.util.*;
import java.util.stream.Collectors;

public class MinimumSpanningTreeKruskal extends MyAlgorithm {
    private int originalWeight;

    private Graph mstGraph;
    private Edge superComputers;
    private List<Edge> mstResult;
    private List<Edge> edgeList;

    public List<Edge> getMstResult() {
        return mstResult;
    }

    public Graph getMstGraph() {
        return mstGraph;
    }

    public Edge getSuperComputers() {
        return superComputers;
    }

    @Override
    public void init(Graph graph) {
        super.graph = graph;

        Random random = new Random();
        mstResult = new LinkedList<>();
        mstGraph = new SingleGraph(graph.getId());

        for (int i = 0; i < graph.getNodeCount(); i++) {
            mstGraph.addNode(graph.getNode(i).getId());
        }

        for (Edge edge : graph.edges().collect(Collectors.toList())) {
            mstGraph.addEdge(edge.getId(), edge.getNode0().getId(), edge.getNode1().getId());
        }

        mstGraph.setAttribute("ui.quality");
        mstGraph.setAttribute("ui.antialias");

        edgeList = mstGraph.edges().collect(Collectors.toList());

        int minWeight = 1;
        int maxWeight = 500;
        for (Edge edge : edgeList) {
            int weight = minWeight + random.nextInt(maxWeight);
            edge.setAttribute("weight", weight);
            edge.setAttribute("ui.label", weight);
            edge.setAttribute("ui.style", "fill-color: gray;");
        }

        int randomEdge = random.nextInt(edgeList.size());
        superComputers = edgeList.get(randomEdge);

        originalWeight = (int) superComputers.getAttribute("weight");
        superComputers.setAttribute("weight", 0);
        superComputers.setAttribute("ui.style", "fill-color: blue;");
        superComputers.getNode0().setAttribute("ui.style", "fill-color: red;");
        superComputers.getNode1().setAttribute("ui.style", "fill-color: red;");
    }

    @Override
    protected void computeBody() {
        edgeList.sort(Comparator.comparingInt(edge -> ((int) edge.getAttribute("weight"))));

        Subset[] subsets = new Subset[graph.getNodeCount()];

        for (int i = 0; i < subsets.length; i++) {
            subsets[i] = new Subset();
            subsets[i].setParent(i);
            subsets[i].setRank(0);
        }

        for (Edge edge : edgeList) {
            int x = SetUtil.find(subsets, Integer.parseInt(edge.getNode0().getId()));
            int y = SetUtil.find(subsets, Integer.parseInt(edge.getNode1().getId()));

            if (x != y) {
                mstResult.add(edge);
                SetUtil.union(subsets, x, y);
            }
        }

        superComputers.setAttribute("weight", originalWeight);
    }
}