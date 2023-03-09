package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

public class SymbolTableVisitor extends PreorderJmmVisitor<String, String>{

    public SymbolTableVisitor(JmmSymbolTable table, List<Report> reports) {

        addVisit("ImportDeclaration", this::dealWithImport);
        addVisit("ImportAux", this::dealWithImportAux);
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("MainMethod", this::dealWithMainDeclaration);
        addVisit("ClassMethod", this::dealWithMethodDeclaration);
        addVisit("Param", this::dealWithParameter);
        addVisit("VarDeclaration", this::dealWithVarDeclaration);

        setDefaultVisit(this::defaultVisit);
    }
}
