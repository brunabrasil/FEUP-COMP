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

    private String WhileStmtVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        JmmNode statement = jmmNode.getJmmChild(1);
        visit(statement,constantsMap);

        for(JmmNode child: statement.getChildren()){
            if(child.getKind().equals("Assignment")){
                constantsMap.remove(child.get("var"));
            }
        }

        JmmNode expression = jmmNode.getJmmChild(0);
        visit(expression,constantsMap);
        return "";
    }

    private String ifElseStmtVisit(JmmNode node, HashMap<String, JmmNode> constantsMap) {
        JmmNode condition = node.getJmmChild(0);
        JmmNode thenStatement = node.getJmmChild(1);
        JmmNode elseStatement = node.getJmmChild(2);

        visit(condition,constantsMap);
        visit(thenStatement,constantsMap);
        visit(elseStatement,constantsMap);

        for(JmmNode child : thenStatement.getChildren()){
            if(child.getKind().equals("Assignment")){
                constantsMap.remove(child.get("var"));
            }
        }

        for(JmmNode child : elseStatement.getChildren()){
            if(child.getKind().equals("Assignment")){
                constantsMap.remove(child.get("var"));
            }
        }
        return "";
    }

    private String identifierVisit(JmmNode node, HashMap<String, JmmNode> constantsMap) {
        JmmNode constant = constantsMap.get(node.get("value"));

        //if constant is in the map
        if (constant != null) {
            JmmNode newLiteral = new JmmNodeImpl(constant.getKind());
            newLiteral.put("value", constant.get("value"));
            //replace node with the value
            replaceNode(node, newLiteral);
            return "changed";
        }
        return "";
    }

    private String assignmentVisit(JmmNode node, HashMap<String, JmmNode> constantsMap) {
        JmmNode rightSide = node.getChildren().get(0);
        visit(rightSide, constantsMap);

        //if it is a constant, add to the map
        if(rightSide.getKind().equals("Integer") || rightSide.getKind().equals("Boolean")){
            constantsMap.put(node.get("var"), rightSide);
        }
        //if not, remove it
        else{
            constantsMap.remove(node.get("var"));
        }
        return "";
    }

    private String methodVisit(JmmNode node, HashMap<String, JmmNode> constantsMap) {
        HashMap<String, JmmNode> newMap = new HashMap<>();
        for (int i = 0; i < node.getChildren().size(); i++) {
            JmmNode child = node.getJmmChild(i);
            visit(child, newMap);
        }
        return "";

    }

    private String defaultVisit(JmmNode node, HashMap<String, JmmNode> constantsMap) {
        for(JmmNode child : node.getChildren()){
            visit(child, constantsMap);
        }
        return "";
    }

    private void replaceNode(JmmNode oldNode, JmmNode newNode){
        JmmNode parent = oldNode.getJmmParent();
        if(parent == null){
            return;
        }

        //get index of the node
        int index = parent.getChildren().indexOf(oldNode);

        //replace
        parent.setChild(newNode,index);
        hasChanged = true;

    }

    public boolean hasChanged() {
        return hasChanged;
    }
}
