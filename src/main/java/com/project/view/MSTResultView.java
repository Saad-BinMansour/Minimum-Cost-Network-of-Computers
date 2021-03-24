package com.project.view;

import com.project.utility.GraphGeneratorWorker;
import com.project.utility.MinimumSpanningTreeKruskal;
import com.project.utility.MinimumSpanningTreePrim;
import com.project.utility.MyAlgorithm;
import org.graphstream.algorithm.Toolkit;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class MSTResultView extends SwingWorker<Void, Void> {
    private SwingViewer swingViewer;
    private View view;

    private MyAlgorithm.algorithms algorithm;

    private JTabbedPane tabbedPane;
    private JPanel graphPanel;

    private int currentGraphIndex;
    private int currentTabIndex;
    private int numberOfVertexPerStep, maxNumberOfIteration, iterationPerStep;

    private List<JPanel> tabList;
    private List<GraphGeneratorWorker> graphGenerators;
    private List<List<Graph>> graphLists;

    public MSTResultView(JPanel graphPanel, MyAlgorithm.algorithms algorithm) {
        this.algorithm = algorithm;
        this.graphPanel = graphPanel;

        tabbedPane = new JTabbedPane();
        graphGenerators = new LinkedList<>();
    }

    public void init(int numberOfVertexPerStep, int maxNumberOfIteration, int iterationPerStep) {
        this.numberOfVertexPerStep = numberOfVertexPerStep;
        this.maxNumberOfIteration = maxNumberOfIteration;
        this.iterationPerStep = iterationPerStep;

        currentGraphIndex = 0;
        currentTabIndex = 0;

        tabList = new ArrayList<>(maxNumberOfIteration / numberOfVertexPerStep);
        graphLists = new ArrayList<>(maxNumberOfIteration / numberOfVertexPerStep);

        for (int i = 0; i < maxNumberOfIteration / numberOfVertexPerStep; i++) {
            graphLists.add(new ArrayList<>(iterationPerStep));
        }

        for (int i = 0; i < maxNumberOfIteration / numberOfVertexPerStep; i++) {
            JPanel topPanel = new JPanel();
            topPanel.setLayout(new FlowLayout());

            JLabel pageNumber = new JLabel(String.format("Page %d of %d", 1, iterationPerStep));

            JButton nextButton = new JButton("Next");
            JButton prevButton = new JButton("Prev");

            nextButton.addActionListener(e -> {
                if (currentGraphIndex < iterationPerStep - 1) {
                    changeGraph(graphLists.get(currentTabIndex).get(++currentGraphIndex));
                    pageNumber.setText(String.format("Page %d of %d", currentGraphIndex + 1, iterationPerStep));
                }
            });

            prevButton.addActionListener(e -> {
                if (currentGraphIndex > 0) {
                    changeGraph(graphLists.get(currentTabIndex).get(--currentGraphIndex));
                    pageNumber.setText(String.format("Page %d of %d", currentGraphIndex + 1, iterationPerStep));
                }
            });

            topPanel.add(prevButton);
            topPanel.add(nextButton);
            topPanel.add(pageNumber);

            JPanel tab = new JPanel();
            tab.setLayout(new BorderLayout());
            tab.add(topPanel, BorderLayout.NORTH);

            tabList.add(tab);
            tabbedPane.addTab(String.valueOf((i + 1) * numberOfVertexPerStep), tab);

            tabbedPane.addChangeListener(e -> {
                currentTabIndex = Integer.parseInt(tabbedPane.getTitleAt(tabbedPane.getSelectedIndex())) / numberOfVertexPerStep - 1;
                currentGraphIndex = 0;
                changeGraph(graphLists.get(currentTabIndex).get(currentGraphIndex));
                tabList.get(currentTabIndex).add((Component) view);
                graphPanel.validate();
                graphPanel.repaint();
            });
        }

        int i = 0;
        for (List<Graph> graphList : graphLists) {
            for (int j = 0; j < iterationPerStep; j++) {
                Graph graph = new SingleGraph(String.valueOf((i + 1) * numberOfVertexPerStep));
                graphList.add(graph);
                graphGenerators.add(new GraphGeneratorWorker(
                        (i + 1) * numberOfVertexPerStep,
                        (algorithm == MyAlgorithm.algorithms.KRUSKAL
                                ? new MinimumSpanningTreeKruskal()
                                : new MinimumSpanningTreePrim())));
            }
            i++;
        }
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public double[] getResults() {
        double[] worstResult = new double[maxNumberOfIteration / numberOfVertexPerStep];

        for (int i = 0; i < worstResult.length; i++) {
            double value = 0;

            for (int j = 0; j < iterationPerStep; j++) {
               value += graphGenerators.get((i * iterationPerStep) + j).getBenchmarkResult() / 1000000;
            }

            worstResult[i] = value / iterationPerStep;
        }

        return worstResult;
    }

    @Override
    protected Void doInBackground() throws Exception {
        try {
            graphGenerators.forEach(graphGeneratorWorker -> {
                for (int i = 0; i < maxNumberOfIteration / numberOfVertexPerStep; i++) {
                    for (int j = 0; j < iterationPerStep; j++) {
                        graphGenerators
                                .get((i * iterationPerStep) + j)
                                .init(graphLists.get(i).get(j));
                    }
                }

                graphGeneratorWorker.compute();
            });
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void done() {
        swingViewer = new SwingViewer(graphLists
                .get(currentTabIndex)
                .get(currentGraphIndex), Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);

        view = swingViewer.addDefaultView(false);

        JPanel tab =  tabList.stream().findFirst().get();
        tab.add((Component) view);
        tab.validate();
        tab.repaint();
    }

    private void changeGraph(Graph graph) {
        assert (graph.getNodeCount() == graphLists.get(currentTabIndex).get(currentGraphIndex).getNodeCount());

        swingViewer.getGraphicGraph().clear();

        swingViewer.getGraphicGraph().setAttribute("ui.stylesheet",
                "node {fill-mode: dyn-plain;}" +
                        "edge {fill-mode: dyn-plain;" +
                        "text-alignment: under; " +
                        "text-color: white; " +
                        "text-style: bold; " +
                        "text-background-mode: rounded-box; " +
                        "text-background-color: #222C; " +
                        "text-padding: 1px; " +
                        "text-offset: 0px, 2px;}");

        try {
            int size = graph.getNodeCount();
            for (int i = 0; i < size; i++) {
                Node currentNode = graph.getNode(i);
                double[] xyz = Toolkit.nodePosition(currentNode);
                Node changedNode = swingViewer.getGraphicGraph().addNode(currentNode.getId());
                changedNode.setAttribute("ui.style", currentNode.getAttribute("ui.style"));
                changedNode.setAttribute("xyz", xyz[0], xyz[1], xyz[2]);
            }

            size = graph.getEdgeCount();
            for (int i = 0; i < size; i++) {
                Edge currentEdge = graph.getEdge(i);
                Edge changedEdge = swingViewer.getGraphicGraph().addEdge(currentEdge.getId(), currentEdge.getNode0().getId(), currentEdge.getNode1().getId());
                changedEdge.setAttribute("ui.style", currentEdge.getAttribute("ui.style"));
                changedEdge.setAttribute("ui.label", currentEdge.getAttribute("ui.label"));
            }
        } catch (IndexOutOfBoundsException | IdAlreadyInUseException | EdgeRejectedException | ElementNotFoundException e) {
            System.exit(0);
            e.printStackTrace();
        }

        swingViewer.getGraphicGraph().setAttribute("ui.quality");
        swingViewer.getGraphicGraph().setAttribute("ui.antialias");

        view.getCamera().setAutoFitView(true);

        assert (swingViewer.getGraphicGraph().getNodeCount() == graphLists.get(currentTabIndex).get(currentGraphIndex).getNodeCount());
    }
}