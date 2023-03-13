package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.*;

public class JmmAnalyser implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        JmmSymbolTable symbolTable=new JmmSymbolTable();
        List<Report> reports = new ArrayList<>();

        SymbolTableVisitor visitor= new SymbolTableVisitor(symbolTable,reports);
        visitor.visit(jmmParserResult.getRootNode(),"");

        return null;
    }
}
