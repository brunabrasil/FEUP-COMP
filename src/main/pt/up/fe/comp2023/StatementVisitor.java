package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

public class StatementVisitor extends AJmmVisitor<String, Type> {
    private JmmSymbolTable table;
    private List<Report> reports;

    public StatementVisitor(JmmSymbolTable table, List<Report> reports){
        this.table=table;
        this.reports=reports;
    }
    @Override
    protected void buildVisitor() {
        addVisit("IfElseStmt", this::dealWithIfElseStmt);
        addVisit("WhileStmt", this::dealWithWhileStmt);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("Stmt", this::dealWithStmt);
        addVisit("ArrayAssignment", this::dealWithArrayAssignment);
        addVisit("Expr", this::dealWithExpr);


    }

    private Type dealWithExpr(JmmNode jmmNode, String s) {
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(table, reports);
        return expressionVisitor.visit(jmmNode.getJmmChild(0), "");
    }

    private Type dealWithArrayAssignment(JmmNode jmmNode, String s) {
        return null;
    }

    private Type dealWithStmt(JmmNode jmmNode, String s) {
        for(JmmNode child: jmmNode.getChildren()){
            visit(child);
        }
        return null;
    }

    private Type dealWithAssignment(JmmNode jmmNode, String s) {
        Type child = new Type("", false);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(table, reports);
        child = expressionVisitor.visit(jmmNode.getJmmChild(0), "");
        String var = jmmNode.get("var"); //ver tipo do var
        JmmNode parent = jmmNode.getJmmParent();
        Type varType= new Type("", false);

        while(!parent.getKind().equals("NormalMethod") && !parent.getKind().equals("MainMethod")) {
            parent = parent.getJmmParent();
        }

        //see if var is a field
        List<Symbol> fields = table.getFields();
        if(fields != null){
            for (int i = 0; i < fields.size(); i++){
                if(fields.get(i).getName().equals(var)){
                    varType = fields.get(i).getType();
                    break;
                }
            }
        }

        //see if var is a parameter
        List<Symbol> parameters = table.getParameters(parent.get("methodName"));
        if(parameters != null){
            for (int i = 0; i < parameters.size(); i++){
                if(parameters.get(i).getName().equals(var)){
                    varType = parameters.get(i).getType();
                    break;
                }
            }
        }

        //see if var is a local variable
        List<Symbol> localVariables = table.getLocalVariables(parent.get("methodName"));
        if(localVariables != null){
            for (int i = 0; i < localVariables.size(); i++){
                if(localVariables.get(i).getName().equals(var)){
                    varType = localVariables.get(i).getType();
                    break;
                }
            }
        }

        //check if left is super and right class
        if(varType.getName().equals(table.getSuper()) && child.getName().equals(table.getClassName())){
            return child;
        }
        //if both are imported
        if(table.getImports().contains(varType.getName()) && table.getImports().contains(child.getName())){
            return child;
        }

        int line = Integer.valueOf(jmmNode.getJmmChild(0).get("lineStart"));
        int col = Integer.valueOf(jmmNode.getJmmChild(0).get("colStart"));
        if(!child.getName().equals(varType.getName())){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Type of the assignee must be compatible with the assigned "));

        }
        return child;
    }

    private Type dealWithWhileStmt(JmmNode jmmNode, String s) {
        Type child = new Type("", false);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(table, reports);
        child = expressionVisitor.visit(jmmNode.getJmmChild(0), "");

        int line = Integer.valueOf(jmmNode.getJmmChild(0).get("lineStart"));
        int col = Integer.valueOf(jmmNode.getJmmChild(0).get("colStart"));
        if(!child.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Expressions in conditions (while) must be of type boolean"));
        }
        return child;
    }

    private Type dealWithIfElseStmt(JmmNode jmmNode, String s) {
        Type child = new Type("", false);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(table, reports);
        child = expressionVisitor.visit(jmmNode.getJmmChild(0), "");

        int line = Integer.valueOf(jmmNode.getJmmChild(0).get("lineStart"));
        int col = Integer.valueOf(jmmNode.getJmmChild(0).get("colStart"));
        //Expressions in conditions must return a boolean
        if(!child.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Expressions in conditions (if) must be of type boolean"));
        }
        return child;
    }


}
