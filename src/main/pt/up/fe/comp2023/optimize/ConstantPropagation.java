package pt.up.fe.comp2023.optimize;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.HashMap;

public class ConstantPropagation extends AJmmVisitor<HashMap<String, JmmNode>, String> {
    private boolean hasChanged = false;

    @Override
    protected void buildVisitor() {
        addVisit("NormalMethod", this::methodVisit);
        addVisit("MainMethod", this::methodVisit);
        addVisit("Assignment", this::assignmentVisit);
        addVisit("Identifier", this::identifierVisit);
    }

    private String identifierVisit(JmmNode jmmNode, HashMap<String, JmmNode> constantsMap) {
        JmmNode constant = constantsMap.get(jmmNode.get("name"));
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

    public boolean hasChanged() {
        return hasChanged;
    }
}
