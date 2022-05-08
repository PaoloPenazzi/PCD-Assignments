package it.unibo.pcd.assignment.reactive.model;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.utils.SourceRoot;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import it.unibo.pcd.assignment.event.ProjectElem;
import it.unibo.pcd.assignment.event.collector.ClassCollector;
import it.unibo.pcd.assignment.event.collector.InterfaceCollector;
import it.unibo.pcd.assignment.event.collector.PackageCollector;
import it.unibo.pcd.assignment.event.collector.ProjectCollector;
import it.unibo.pcd.assignment.event.report.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ReactiveAnalyzerImpl implements ReactiveAnalyzer {
    private final Subject<Integer> packageNumberObservable = PublishSubject.create();
    private final Subject<Integer> classNumberObservable = PublishSubject.create();
    private final Subject<Integer> interfaceNumberObservable = PublishSubject.create();
    private final Subject<ProjectElem> reportObservable = PublishSubject.create();
    private int packageNumber;
    private int classNumber;
    private int interfaceNumber;
    private String analysisPath;
    private String analysisType;

    @Override
    public void analyzeProject(String srcProjectFolderName) {
        SourceRoot sourceRoot = new SourceRoot(Paths.get(srcProjectFolderName)).setParserConfiguration(new ParserConfiguration());
        List<ParseResult<CompilationUnit>> parseResultList = sourceRoot.tryToParseParallelized();
        List<PackageDeclaration> allCus = parseResultList.stream().filter(r -> r.getResult().isPresent() && r.isSuccessful()).map(r -> r.getResult().get()).filter(c -> c.getPackageDeclaration().isPresent()).map(c -> c.getPackageDeclaration().get()).distinct().collect(Collectors.toList());
        for (PackageDeclaration packageDeclaration : allCus) {
            this.analyzePackage(packageDeclaration);
        }
    }

    private void analyzePackage(PackageDeclaration packageDeclaration) {
        String packagePath = packageDeclaration.getNameAsString();
        String sourceRootPath = this.analysisPath;
        List<CompilationUnit> classesOrInterfacesUnit = this.createParsedFileList(packageDeclaration, sourceRootPath).stream().filter(r -> r.isSuccessful() && r.getResult().isPresent()).map(r -> r.getResult().get()).collect(Collectors.toList());
        incrementPackageNumber(1);
        for (CompilationUnit cu : classesOrInterfacesUnit) {
            List<ClassOrInterfaceDeclaration> declarationList = cu.getTypes().stream().map(TypeDeclaration::asTypeDeclaration).filter(BodyDeclaration::isClassOrInterfaceDeclaration).map(ClassOrInterfaceDeclaration.class::cast).collect(Collectors.toList());
            for (ClassOrInterfaceDeclaration declaration : declarationList) {
                if (declaration.isInterface()) {
                    this.createInterfaceReport(packagePath, declaration);
                } else {
                    this.createClassReport(packagePath, declaration);
                }
            }
        }
    }

    private List<ParseResult<CompilationUnit>> createParsedFileList(PackageDeclaration packageDeclaration, String sourceRootPath) {
        SourceRoot sourceRoot = new SourceRoot(Paths.get(sourceRootPath)).setParserConfiguration(new ParserConfiguration());
        List<ParseResult<CompilationUnit>> parseResultList;
        parseResultList = sourceRoot.tryToParseParallelized(packageDeclaration.getNameAsString());
        return parseResultList;
    }

    @Override
    public void getProjectReport(String projectPath) {
        ProjectReportImpl projectReport = new ProjectReportImpl();
        ProjectCollector projectCollector = new ProjectCollector();
        projectCollector.visit(projectPath, projectReport);
        incrementPackageNumber(projectReport.getPackageReport().size());
        for (PackageReport packageReport : projectReport.getPackageReport()) {
            incrementInterfaceNumber(packageReport.getInterfacesReport().size());
            incrementClassNumber(packageReport.getClassesReport().size());
        }
        this.addReport(projectReport);
    }

    @Override
    public void getPackageReport(String packagePath) {
        String path = packagePath;
        String sourceRoot = "";
        System.out.println(path);
        if (path.contains("\\")) {
            path = path.replace("\\", "/");
        }
        if (packagePath.contains("src/main/java")) {
            path = packagePath.replaceAll(".*src/main/java/", "");
            path = path.replaceAll("/", ".");
            sourceRoot = packagePath.replaceAll("src/main/java.*", "");
            sourceRoot = sourceRoot + "src/main/java";
        } else {
            throw new IllegalStateException("Wrong Path");
        }
        PackageDeclaration packageDeclaration = StaticJavaParser.parsePackageDeclaration("package " + path + ";");
        PackageReportImpl packageReport = new PackageReportImpl();
        PackageCollector packageCollector = new PackageCollector();
        packageCollector.setPath(sourceRoot);
        packageCollector.visit(packageDeclaration, packageReport);
        this.addReport(packageReport);
        this.incrementPackageNumber(1);
        this.incrementClassNumber(packageReport.getClassesReport().size());
        this.incrementInterfaceNumber(packageReport.getInterfacesReport().size());
    }

    @Override
    public void getClassReport(String classPath) {
        CompilationUnit compilationUnit = this.parseSingleFile(classPath);
        ClassOrInterfaceDeclaration declaration = compilationUnit.getTypes().stream().map(TypeDeclaration::asTypeDeclaration)
                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                .map(ClassOrInterfaceDeclaration.class::cast).findFirst().get();
        this.createClassReport(compilationUnit.getPackageDeclaration().get().getNameAsString(), declaration);
    }

    @Override
    public void getInterfaceReport(String interfacePath) {
        CompilationUnit compilationUnit = this.parseSingleFile(interfacePath);
        ClassOrInterfaceDeclaration declaration = compilationUnit.getTypes().stream().map(TypeDeclaration::asTypeDeclaration)
                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                .map(ClassOrInterfaceDeclaration.class::cast).findFirst().get();
        this.createInterfaceReport(compilationUnit.getPackageDeclaration().get().getNameAsString(), declaration);
    }

    private void createClassReport(String packageName, ClassOrInterfaceDeclaration declaration) {
        if (this.isTheRightPackage(packageName, declaration)) {
            ClassReportImpl classReport = new ClassReportImpl();
            ClassCollector classCollector = new ClassCollector();
            classCollector.visit(declaration, classReport);
            addReport(classReport);
            incrementClassNumber(1);
        }
    }

    private void createInterfaceReport(String packageName, ClassOrInterfaceDeclaration declaration) {
        if (this.isTheRightPackage(packageName, declaration)) {
            InterfaceReportImpl interfaceReport = new InterfaceReportImpl();
            InterfaceCollector interfaceCollector = new InterfaceCollector();
            interfaceCollector.visit(declaration, interfaceReport);
            addReport(interfaceReport);
            incrementInterfaceNumber(1);
        }
    }

    private CompilationUnit parseSingleFile(String srcFilePath) {
        CompilationUnit compilationUnit;
        try {
            compilationUnit = StaticJavaParser.parse(new File(srcFilePath));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return compilationUnit;
    }

    private boolean isTheRightPackage(String packageName, ClassOrInterfaceDeclaration declaration) {
        String classFullName = declaration.getFullyQualifiedName().get();
        String className = declaration.getNameAsString();
        classFullName = classFullName.replace("." + className, "");
        return classFullName.equals(packageName);
    }

    private void buildClassReport(ClassOrInterfaceDeclaration declaration) {
        /*String className = declaration.getFullyQualifiedName().get();
        String fieldsString = "";
        String methodsString = "";
        List<FieldDeclaration> fieldDeclarationList = declaration.getFields();
        for (FieldDeclaration fieldDeclaration : fieldDeclarationList) {
            fieldsString = fieldsString.concat("\t\t" + "FIELD:  " + fieldDeclaration.toString() + "\n");
        }
        List<MethodDeclaration> methodDeclarationList = declaration.getMethods();
        for (MethodDeclaration methodDeclaration : methodDeclarationList) {
            methodsString = methodsString.concat("\t\t" + "METHOD:  " + methodDeclaration.getDeclarationAsString(true, false, true) + "\n");
        }
        String report = "\t" + "CLASS:  " + className + "\n" + fieldsString + methodsString;
        addReport(report);*/
    }

    private void buildInterfaceReport(ClassOrInterfaceDeclaration declaration) {
        /*String interfaceName = declaration.getFullyQualifiedName().get();
        String methodsString = "";
        List<MethodDeclaration> methodDeclarationList = declaration.getMethods();
        for (MethodDeclaration methodDeclaration : methodDeclarationList) {
            methodsString = methodsString.concat("\t\t" + "METHOD:  " + methodDeclaration.getDeclarationAsString(true, false, true) + "\n");
        }
        String report = "\t" + "INTERFACE:  " + interfaceName + "\n" + methodsString;
        addReport(report);*/
    }

    public void incrementPackageNumber(int variation) {
        this.packageNumberObservable.onNext(this.packageNumber += variation);
    }

    public void incrementClassNumber(int variation) {
        this.classNumberObservable.onNext(this.classNumber += variation);
    }

    public void incrementInterfaceNumber(int variation) {
        this.interfaceNumberObservable.onNext(this.interfaceNumber += variation);
    }

    public void addReport(ProjectElem report) {
        reportObservable.onNext(report);
    }

    public Observable<Integer> getPackageNumberObservable() {
        return packageNumberObservable;
    }

    public Observable<Integer> getClassNumberObservable() {
        return classNumberObservable;
    }

    public Observable<Integer> getInterfaceNumberObservable() {
        return interfaceNumberObservable;
    }

    public Observable<ProjectElem> getReportObservable() {
        return reportObservable;
    }

    public String getAnalysisPath() {
        return this.analysisPath;
    }

    public void setAnalysisPath(String newPath) {
        this.analysisPath = newPath;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public void resetCounters() {
        this.packageNumber = 0;
        this.classNumber = 0;
        this.interfaceNumber = 0;
        this.packageNumberObservable.onNext(this.packageNumber);
        this.classNumberObservable.onNext(this.classNumber);
        this.interfaceNumberObservable.onNext(this.interfaceNumber);
    }
}
