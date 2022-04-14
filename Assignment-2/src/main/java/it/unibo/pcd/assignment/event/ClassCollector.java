package it.unibo.pcd.assignment.event;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.List;

public class ClassCollector extends VoidVisitorAdapter<ClassReportImpl> {
    @Override
    public void visit(ClassOrInterfaceDeclaration dec, ClassReportImpl collector) {
        super.visit(dec, collector);
        // name
        collector.setFullClassName(dec.getNameAsString());
        // src path
        collector.setSrcFullFileName(dec.getFullyQualifiedName().get());
        // info on methods
        List<MethodInfoImpl> methodInfoList = new ArrayList<>();
        dec.getMethods().forEach(m -> {
            MethodInfoImpl methodInfo = new MethodInfoImpl();
            methodInfo.setModifiers(m.getModifiers().toString());
            methodInfo.setBeginLine(m.getBegin().get().line);
            methodInfo.setEndBeginLine(m.getEnd().get().line);
            methodInfo.setName(m.getNameAsString());
            methodInfoList.add(methodInfo);
        });
        // info on fields
        List<FieldInfoImpl> fieldInfoList = new ArrayList<>();
        dec.getFields().forEach(f -> {
            FieldInfoImpl fieldInfo = new FieldInfoImpl();
            fieldInfo.setName(f.getVariable(0).getName().asString());
            // getVariable() returns a list because it's possible to declare more fields in the same line.
            fieldInfo.setType(f.getElementType().asString());
            fieldInfoList.add(fieldInfo);
        });
        // settato qui perchè si deve richiamare solo quando ha completato il class report
        methodInfoList.forEach(m -> m.setParentClass(collector));
        fieldInfoList.forEach(f -> f.setParentClass(collector));
        collector.setMethodsInfo(methodInfoList);
        collector.setFieldsInfo(fieldInfoList);

    }
}