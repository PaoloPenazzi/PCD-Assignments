package it.unibo.pcd.assignment.event.collector;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.Pair;
import com.github.javaparser.utils.SourceRoot;
import it.unibo.pcd.assignment.event.report.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PackageCollector extends VoidVisitorAdapter<PackageReportImpl> {

    @Override
    public void visit(PackageDeclaration dec, PackageReportImpl collector) {
        super.visit(dec, collector);
        // name
        collector.setFullPackageName(dec.getNameAsString());
        // classes report
        SourceRoot sourceRoot = new SourceRoot(Paths.get("src/main/java/"));
        sourceRoot.setParserConfiguration(new ParserConfiguration());
        List<ParseResult<CompilationUnit>> parseResultList;
        try {
            parseResultList = sourceRoot.tryToParse(dec.getNameAsString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<CompilationUnit> classesOrInterfacesUnit = parseResultList.stream()
                                                                       .filter(ParseResult::isSuccessful)
                                                                       .map(r -> r.getResult().get())
                                                                       .collect(Collectors.toList());

        ClassReportImpl classReport = new ClassReportImpl();
        ClassCollector classCollector = new ClassCollector();
        List<ClassReport> classReports = new ArrayList<>();

        InterfaceReportImpl interfaceReport = new InterfaceReportImpl();
        InterfaceCollector interfaceCollector = new InterfaceCollector();
        List<InterfaceReport> interfaceReports = new ArrayList<>();

        for(CompilationUnit cu : classesOrInterfacesUnit){
            List<ClassOrInterfaceDeclaration> tipi = cu.getTypes().stream()
                                                      .map(TypeDeclaration::asTypeDeclaration)
                                                      .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                                                      .map(x -> (ClassOrInterfaceDeclaration) x)
                                                      .collect(Collectors.toList());
            for(ClassOrInterfaceDeclaration declaration : tipi){
                if(declaration.isInterface()){
                    interfaceCollector.visit(cu, interfaceReport);
                    interfaceReports.add(interfaceReport);
                } else {
                    classCollector.visit(cu, classReport);
                    classReports.add(classReport);
                }
            }
        }
    }
}
