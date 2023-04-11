package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.OllirVisitor;

import java.util.Arrays;
import java.util.ArrayList;

public class OllirOptimization implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        JmmNode node= jmmSemanticsResult.getRootNode();

        OllirVisitor visitor=new OllirVisitor((JmmSymbolTable) jmmSemanticsResult.getSymbolTable(),jmmSemanticsResult.getReports());
        String ollircode=(String) visitor.visit(node, Arrays.asList("DEFAULT_VISIT")).get(0);

        return new OllirResult(jmmSemanticsResult,ollircode,jmmSemanticsResult.getReports());
    }
}
