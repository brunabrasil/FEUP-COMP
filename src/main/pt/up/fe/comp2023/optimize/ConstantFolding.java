package pt.up.fe.comp2023.optimize;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

public class ConstantFolding extends AJmmVisitor<String, String> {
    private boolean hasChanged = false;

    @Override
    protected void buildVisitor() {
        addVisit("BinaryOp", this::foldBinaryOp);
        addVisit("UnaryOp", this::foldUnaryOp);
        setDefaultVisit(this::defaultVisit);

    }
    public void foldConstantIf(JmmNode expr, JmmNode jmmNode) {
        replaceNode(jmmNode, expr);
    }

    public static void replaceNode (JmmNode nodeToReplace, JmmNode newNode) {
        JmmNode parent = nodeToReplace.getJmmParent();
        if (parent == null){
            return;
        }
        int index = parent.getChildren().indexOf(nodeToReplace);
        parent.removeJmmChild(nodeToReplace);
        parent.add(newNode, index);
        newNode.setParent(parent);
    }

    private String foldUnaryOp(JmmNode node, String s) {
        JmmNode child = node.getJmmChild(0);
        JmmNode newLiteral = new JmmNodeImpl("Boolean");
        newLiteral.put("value", child.get("value").equals("true") ? "false" : "true");
        replaceNode(node, newLiteral);
        hasChanged = true;
        return "";
    }

    public String foldBinaryOp(JmmNode node, String s) {
        JmmNode left = node.getJmmChild(0);
        JmmNode right = node.getJmmChild(1);
        if(left.getKind().equals("Integer") && right.getKind().equals("Integer")){
            int leftValue = Integer.parseInt(left.get("value"));
            int rightValue = Integer.parseInt(right.get("value"));
            boolean resultBoolean = false;
            int result = 0;
            switch (node.get("op")){
                case "+" -> result = leftValue + rightValue;
                case "-" -> result = leftValue - rightValue;
                case "*" -> result = leftValue * rightValue;
                case "/" -> result =  leftValue / rightValue;
                case "<" -> resultBoolean = leftValue < rightValue;
            }
            JmmNode newLiteral;
            if(node.get("op").equals("<")){
                newLiteral = new JmmNodeImpl("Boolean");
                newLiteral.put("value", Boolean.toString(resultBoolean));

            }
            else{
                newLiteral = new JmmNodeImpl("Integer");
                newLiteral.put("value", Integer.toString(result));

            }
            replaceNode(node, newLiteral);
            hasChanged = true;

        }
        else if(left.getKind().equals("Boolean") && right.getKind().equals("Boolean")){
            if(node.get("op").equals("&&")){
                JmmNode newLiteral;
                newLiteral = new JmmNodeImpl("Boolean");
                newLiteral.put("value", Boolean.toString(left.get("value").equals("1") && right.get("value").equals("1")));
                replaceNode(node, newLiteral);
                hasChanged = true;

            }

        }
        return "";
    }
    private String defaultVisit(JmmNode jmmNode, String dummy) {
        for(JmmNode child : jmmNode.getChildren()){
            visit(child, dummy);
        }
        return "";
    }

    public boolean hasChanged(){
        return hasChanged;
    }

}
