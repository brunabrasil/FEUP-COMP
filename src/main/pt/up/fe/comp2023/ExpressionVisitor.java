package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class ExpressionVisitor extends AJmmVisitor<String,String > {
    private JmmSymbolTable table;
    private List<Report> reports;

    public ExpressionVisitor(JmmSymbolTable table, List<Report> reports){
        this.table=table;
        this.reports=reports;
    }
    @Override
    protected void buildVisitor() {
        addVisit("Stmt", this::dealWithStmt);
        addVisit("IfElseStmt", this::dealWithIfElseStmt);
        addVisit("WhileStmt", this::dealWithWhileStmt);
        addVisit("Expr", this::dealWithExpr);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("Parenthesis", this::dealWithParenthesis);
        addVisit("Index", this::dealWithIndex);
        addVisit("CallMethod", this::dealWithCallMethod);
        addVisit("Unary", this::dealWithUnaryOp );
        addVisit("BinaryOp", this::dealWithBinOp);
        addVisit("Instantiation", this::dealWithInstantion);
        // addVisit("Integer", this::dealWith???);
        // addVisit("This", this::dealWith???? );

    }

    private String dealWithUnaryOp(JmmNode jmmNode, String s) {
        return "";
    }

    private String dealWithInstantion(JmmNode jmmNode, String s) {
        return "";
    }

    private String dealWithBinOp(JmmNode jmmNode, String s) {
        return "";
    }

    private String dealWithParenthesis(JmmNode jmmNode, String s) {
        return "";
    }

    private String dealWithCallMethod(JmmNode jmmNode, String s) {
        return "";
    }


    private String dealWithWhileStmt(JmmNode jmmNode, String s) {
        return  null;
    }

    private String dealWithIfElseStmt(JmmNode jmmNode, String s) {
        return  null;
    }

    private String dealWithStmt(JmmNode jmmNode, String s) {
        return  null;
    }


    private String dealWithIndex(JmmNode jmmNode, String s) {
        return  null;
    }

    private String dealWithAssignment(JmmNode jmmNode, String s) {

        String ret= s+"int "+ jmmNode.get("var")
                + " = "+ visit(jmmNode.getJmmChild(0), "")
                +";";
        return  null;
    }

    private String dealWithExpr ( JmmNode jmmNode , String s) {
        String ret= s+visit(jmmNode.getJmmChild(0), "")+";";
        return null;
    }
}
