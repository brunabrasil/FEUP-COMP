package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class JmmAnalyser implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        if (jmmParserResult.getRootNode() == null) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "AST root node is null");
            return new JmmSemanticsResult(jmmParserResult, null, Arrays.asList(errorReport));
        }

        JmmNode node = jmmParserResult.getRootNode();

        JmmSymbolTable symbolTable=new JmmSymbolTable();
        List<Report> reports = new ArrayList<>();

        SymbolTableVisitor visitor= new SymbolTableVisitor(symbolTable, reports);
        visitor.visit(node,"");

        System.out.println(jmmParserResult.getRootNode().toTree());

        //ProgramVisitor startVisitor = new ProgramVisitor(symbolTable, reports);

        //startVisitor.visit(node, "");

        return new JmmSemanticsResult(jmmParserResult, symbolTable, reports);
    }
}
