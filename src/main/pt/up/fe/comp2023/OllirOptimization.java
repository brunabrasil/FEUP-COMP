package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.OllirVisitor;
import pt.up.fe.comp2023.optimize.ConstantFolding;
import pt.up.fe.comp2023.optimize.ConstantPropagation;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

public class OllirOptimization implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        JmmNode node = jmmSemanticsResult.getRootNode();
        if(jmmSemanticsResult.getReports().size()!=0)
            System.out.println(jmmSemanticsResult.getReports().toString());
        OllirVisitor visitor=new OllirVisitor((JmmSymbolTable) jmmSemanticsResult.getSymbolTable(),jmmSemanticsResult.getReports());
        String ollircode= visitor.visit(node,"");

        return new OllirResult(jmmSemanticsResult,ollircode,jmmSemanticsResult.getReports());
    }
    public JmmSemanticsResult optimize(JmmSemanticsResult jmmSemanticsResult){
        if (jmmSemanticsResult.getConfig().getOrDefault("optimize", "false").equals("false")) {
            return jmmSemanticsResult;
        }

        ConstantPropagation constantPropagation;
        ConstantFolding constantFolding;
        //enquanto houver mudanças continuar a verificar
        do{
            constantPropagation = new ConstantPropagation();
            constantFolding = new ConstantFolding();
            constantPropagation.visit(jmmSemanticsResult.getRootNode(), new HashMap<>());
            constantFolding.visit(jmmSemanticsResult.getRootNode(),"");
        }while(constantPropagation.hasChanged() || constantFolding.hasChanged());

        return jmmSemanticsResult;

    }
}
