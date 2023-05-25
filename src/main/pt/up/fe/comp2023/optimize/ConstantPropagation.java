package pt.up.fe.comp2023.optimize;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ConstantPropagation extends AJmmVisitor<HashMap<String, JmmNode>, String> {
    private boolean hasChanged = false;

    @Override
    protected void buildVisitor() {
        addVisit("NormalMethod", this::methodVisit);
        addVisit("MainMethod", this::methodVisit);
        addVisit("Assignment", this::assignmentVisit);
        addVisit("Identifier", this::identifierVisit);
        addVisit("WhileStmt", this::WhileStmtVisit);
        addVisit("IfElseStmt", this::ifElseStmtVisit);
        setDefaultVisit(this::defaultVisit);

    }

    private String WhileStmtVisit(JmmNode jmmNode, HashMap<String, JmmNode> constMap) {
        JmmNode statement = jmmNode.getJmmChild(1);
        Set<String> variablesInsideWhile = getVariablesAssigned(statement);
        System.out.println("Variables assigned" + variablesInsideWhile);
        for(String variable: variablesInsideWhile){
            constMap.remove(variable);
        }
        JmmNode expression = jmmNode.getJmmChild(0);
        visit(expression,constMap);
        return "";
    }

    private Set<String> getVariablesAssigned(JmmNode whileNode){
        Set<String> variablesAssigned = new HashSet<>();
        for(JmmNode child : whileNode.getChildren()){
            if(child.getKind().equals("Assignment")){
                variablesAssigned.add(child.get("var"));
            }
        }
        return variablesAssigned;
    }

    private String ifElseStmtVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        JmmNode condition = jmmNode.getJmmChild(0);
        JmmNode thenStatement = jmmNode.getJmmChild(1);
        JmmNode elseStatement = jmmNode.getJmmChild(2);


        if(condition.getKind().equals("Boolean")){
            boolean expressionValue = Boolean.parseBoolean(condition.get("value"));
            if(expressionValue){
                visit(thenStatement, constantsMap);
            }else{
                visit(elseStatement, constantsMap);
            }
        }else{
            Set<String> variablesIf = getVariablesAssigned(thenStatement);
            Set<String> variablesElse = getVariablesAssigned(elseStatement);
            variablesIf.addAll(variablesElse);
            for(String variable: variablesIf){
                constantsMap.remove(variable);
            }
        }
        return "";
    }

    private String identifierVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        JmmNode constant = constantsMap.get(jmmNode.get("value"));
        //if it is in the constmap
        if (constant != null) {
            JmmNode newLiteral = new JmmNodeImpl(constant.getKind());
            newLiteral.put("value", constant.get("value"));
            ConstantFolding.replaceNode(jmmNode, newLiteral);
            hasChanged = true;
            return "changed";
        }
        return "";
    }

    private String assignmentVisit(JmmNode node, HashMap<String, JmmNode> constantsMap) {
        JmmNode rightSide = node.getChildren().get(0);
        if(rightSide.getKind().equals("Integer") || rightSide.getKind().equals("Boolean")){
            constantsMap.put(node.get("var"), rightSide);
            node.getJmmParent().removeJmmChild(node);
        }else{
            visit(rightSide, constantsMap);
        }
        return "";
    }

    private String methodVisit(JmmNode node, HashMap<String, JmmNode> constantsMap) {
        HashMap<String, JmmNode> newConstantsMap = new HashMap<>();
        for (int i = 0; i < node.getChildren().size(); i++) {
            JmmNode child = node.getJmmChild(i);
            visit(child, newConstantsMap);
        }
        return "";

    }

    private String defaultVisit(JmmNode jmmNode, HashMap<String, JmmNode> constMap) {
        for(JmmNode child : jmmNode.getChildren()){
            visit(child, constMap);
        }
        return "";
    }

    public boolean hasChanged() {
        return hasChanged;
    }
}
