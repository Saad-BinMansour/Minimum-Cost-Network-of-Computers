package com.project.utility;

import org.graphstream.algorithm.Algorithm;
import org.graphstream.algorithm.Toolkit;
import org.graphstream.algorithm.generator.*;
import org.graphstream.graph.Edge;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;

import java.util.*;
import java.util.List;

public class GraphGeneratorWorker implements Algorithm {
    private double benchmarkResult;
    private MyAlgorithm myAlgorithm;
    private final Generator GRAPH_GENERATOR;
    private final int NUMBER_OF_VERTICES;

    private Graph graph;

    public GraphGeneratorWorker(int numberOfVertices, MyAlgorithm myAlgorithm) {
        this.myAlgorithm = myAlgorithm;
        this.NUMBER_OF_VERTICES = numberOfVertices;

        GRAPH_GENERATOR = new GridGenerator(true, false, true);
    }

    public double getBenchmarkResult() {
        return benchmarkResult;
    }

    @Override
    public void init(Graph graph) {
        this.graph = graph;
    }

    @Override
    public void compute() {
        GRAPH_GENERATOR.addSink(graph);

        GRAPH_GENERATOR.begin();
        for (int i = 1; i < Math.ceil(Math.sqrt(NUMBER_OF_VERTICES * 1.2)); i++) {
            GRAPH_GENERATOR.nextEvents();
        }
        GRAPH_GENERATOR.end();

        try {
            if (graph.getNodeCount() > NUMBER_OF_VERTICES) {
                Random random = new Random();
                int numberOfRemoveIteration = graph.getNodeCount() - NUMBER_OF_VERTICES;
                for (int i = 0; i < numberOfRemoveIteration; i++) {
                    int randomNode = getRandomWithExclusion(
                            random,
                            0,
                            graph.getNodeCount(),
                            findArticulationPoints(graph));

                    if (graph.getNode(randomNode) != null) {
                        graph.removeNode(randomNode);
                    } else {
                        i--;
                    }
                }
            }
        } catch (ElementNotFoundException e) {
            e.printStackTrace();
        }

        assert (graph.getNodeCount() == NUMBER_OF_VERTICES);
        assert (Toolkit.isConnected(graph));

        graph.setAttribute("ui.stylesheet",
                "node {fill-mode: dyn-plain;}" +
                        "edge {fill-mode: dyn-plain;" +
                        "text-alignment: under; " +
                        "text-color: white; " +
                        "text-style: bold; " +
                        "text-background-mode: rounded-box; " +
                        "text-background-color: #222C; " +
                        "text-padding: 1px; " +
                        "text-offset: 0px, 2px;}");

        graph.setAttribute("ui.quality");
        graph.setAttribute("ui.antialias");

        double startTime = 0;
        double endTime = 0;

        try {
            myAlgorithm.init(graph);

            startTime = System.nanoTime();
            myAlgorithm.compute();
            endTime = System.nanoTime();

            List<Edge> mstArrayList = new ArrayList<>(myAlgorithm.getMstResult());
            int size = mstArrayList.size();
            for (int i = 0; i < size; i++) {
                if (mstArrayList.get(i).getId() != myAlgorithm.getSuperComputers().getId()) {
                    mstArrayList.get(i).setAttribute("ui.style", "fill-color: blue; size: 5px;");
                    mstArrayList.get(i).getNode0().setAttribute("ui.style", "fill-color: black;");
                    mstArrayList.get(i).getNode1().setAttribute("ui.style", "fill-color: black;");
                }
            }
            myAlgorithm.getSuperComputers().setAttribute("ui.style", "fill-color: red; size: 4px;");
            myAlgorithm.getSuperComputers().setAttribute("weight", myAlgorithm.getOriginalWeight());
        } catch (ElementNotFoundException e) {
            e.printStackTrace();
        }

        benchmarkResult = endTime - startTime;
    }

    private int getRandomWithExclusion(Random rnd, int start, int end, List<Integer> exclude) {
        int random = start + rnd.nextInt(end - start + 1 - exclude.size());
        for (int ex : exclude) {
            if (random < ex) {
                break;
            }
            random++;
        }
        return random;
    }

    private List<Integer> findArticulationPoints(Graph graph) {
        final int V = graph.getNodeCount();
        List<Integer> articulationPoints = new LinkedList<>();
        boolean[] visited = new boolean[V];
        int disc[] = new int[V];
        int low[] = new int[V];
        int parent[] = new int[V];
        boolean ap[] = new boolean[V];

        for (int i = 0; i < V; i++)
        {
            parent[i] = -1;
            visited[i] = false;
            ap[i] = false;
        }

        for (int i = 0; i < V; i++)
            if (visited[i] == false)
                APUtil(i, visited, disc, low, parent, ap, graph);

        for (int i = 0; i < V; i++) {
            if (ap[i] == true) {
                articulationPoints.add(i);
            }
        }

        return articulationPoints;
    }

    private int time = 0;

    void APUtil(int u, boolean visited[], int disc[],
                int low[], int parent[], boolean ap[], Graph graph) {
        int children = 0;

        visited[u] = true;

        disc[u] = low[u] = ++time;

        LinkedList<Integer> adjNodes = new LinkedList<>();

        graph.getNode(u).edges().forEach(edge -> {
            if (edge.getNode1().getIndex() != u)
                adjNodes.add(edge.getNode1().getIndex());
            else {
                adjNodes.add(edge.getNode0().getIndex());
            }
        });

        Iterator<Integer> i = adjNodes.iterator();
        while (i.hasNext())
        {
            int v = i.next();

            if (!visited[v])
            {
                children++;
                parent[v] = u;
                APUtil(v, visited, disc, low, parent, ap, graph);

                low[u]  = Math.min(low[u], low[v]);

                if (parent[u] == -1 && children > 1)
                    ap[u] = true;

                if (parent[u] != -1 && low[v] >= disc[u])
                    ap[u] = true;
            }

            else if (v != parent[u])
                low[u]  = Math.min(low[u], disc[v]);
        }
    }
}