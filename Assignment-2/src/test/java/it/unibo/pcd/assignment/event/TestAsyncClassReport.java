package it.unibo.pcd.assignment.event;

import hu.webarticum.treeprinter.SimpleTreeNode;

public class TestAsyncClassReport {

    public static void main(String[] args) {
        ProjectAnalyzer projectAnalyzer = new ProjectAnalyzerImpl();
        projectAnalyzer.getClassReport("src/main/java/it/unibo/pcd/assignment/event/report/MethodInfoImpl.java", new SimpleTreeNode("Test")).onComplete(
                                res -> System.out.println(res.result())
        );

    }

}
