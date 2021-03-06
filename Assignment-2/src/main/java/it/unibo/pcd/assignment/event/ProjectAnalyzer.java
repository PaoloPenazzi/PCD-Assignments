package it.unibo.pcd.assignment.event;

import hu.webarticum.treeprinter.SimpleTreeNode;
import io.vertx.core.Future;
import it.unibo.pcd.assignment.parser.ProjectElem;
import it.unibo.pcd.assignment.parser.report.ClassReport;
import it.unibo.pcd.assignment.parser.report.InterfaceReport;
import it.unibo.pcd.assignment.parser.report.PackageReport;
import it.unibo.pcd.assignment.parser.report.ProjectReport;

import java.util.function.Consumer;

public interface ProjectAnalyzer {

    /**
     * Async method to retrieve the report about a specific interface,
     * given the full path of the interface source file
     *
     * @param srcInterfacePath
     * @return
     */
    Future<InterfaceReport> getInterfaceReport(String interfacePath, SimpleTreeNode fatherTreeNode);

    /**
     * Async method to retrieve the report about a specific class,
     * given the full path of the class source file
     *
     * @param srcClassPath
     * @return
     */
    Future<ClassReport> getClassReport(String classPath, SimpleTreeNode fatherTreeNode);

    /**
     * Async method to retrieve the report about a package,
     * given the full path of the package folder
     *
     * @param srcPackagePath
     * @return
     */
    Future<PackageReport> getPackageReport(String packagePath, SimpleTreeNode fatherTreeNode);

    /**
     * Async method to retrieve the report about a project,
     * given the full path of the project folder
     *
     * @param srcProjectpath
     * @return
     */
    Future<ProjectReport> getProjectReport(String projectPath);

    /**
     * Async function that analyze a project given the full path of the project folder,
     * executing the callback each time a project element is found
     *
     * @param srcProjectFolderName
     * @param callback
     */
    void analyzeProject(String projectFolderName, Consumer<ProjectElem> callback);
}
