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
        addVisit("AssignmentArray", this::dealWithAssignmentArray);
        addVisit("Expr", this::dealWithExpr);
    }

    private Type dealWithExpr(JmmNode jmmNode, String s) {
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(table, reports);
        return expressionVisitor.visit(jmmNode.getJmmChild(0), "");
    }

    private Type dealWithAssignmentArray(JmmNode jmmNode, String s) {
        String varLeft = jmmNode.get("var");

        int line = Integer.parseInt(jmmNode.get("lineStart"));
        int col = Integer.parseInt(jmmNode.get("colStart"));

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(table, reports);
        Type indexType = expressionVisitor.visit(jmmNode.getJmmChild(0), "");
        Type rightType = expressionVisitor.visit(jmmNode.getJmmChild(1), "");

        JmmNode parent = jmmNode.getJmmParent();
        Type leftType= new Type("", false);

        while(!parent.getKind().equals("NormalMethod") && !parent.getKind().equals("MainMethod")) {
            parent = parent.getJmmParent();
        }

        String methodName;
        if(parent.getKind().equals("NormalMethod")){
            methodName = parent.get("methodName");
        }
        else{
            methodName = "main";
        }
        List<Symbol> fields = table.getFields();
        if(fields != null){
            for (int i = 0; i < fields.size(); i++){
                if(fields.get(i).getName().equals(varLeft)){
                    if(methodName.equals("main")){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Fields cannot be used in main method"));
                    }
                    leftType = fields.get(i).getType();
                    break;
                }
            }
        }

        //see if var is a parameter
        List<Symbol> parameters = table.getParameters(methodName);

        if(parameters != null){
            for (int i = 0; i < parameters.size(); i++){
                if(parameters.get(i).getName().equals(varLeft)){
                    leftType = parameters.get(i).getType();
                    break;
                }
            }
        }

        //see if var is a local variable
        List<Symbol> localVariables = table.getLocalVariables(methodName);

        if(localVariables != null){
            for (int i = 0; i < localVariables.size(); i++){
                if(localVariables.get(i).getName().equals(varLeft)){
                    leftType = localVariables.get(i).getType();
                    break;
                }
            }
        }

        if(!leftType.isArray()){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Var is not an array"));
        }
        if(!indexType.getName().equals("int")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Index must be of type int"));
        }
        if(!rightType.getName().equals("int")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Assigned must be of type int"));
        }

        return null;
    }

    private Type dealWithStmt(JmmNode jmmNode, String s) {
        for(JmmNode child: jmmNode.getChildren()){
            visit(child);
        }
        return null;
    }

    private Type dealWithAssignment(JmmNode jmmNode, String s) {
        int line = Integer.parseInt(jmmNode.getJmmChild(0).get("lineStart"));
        int col = Integer.parseInt(jmmNode.getJmmChild(0).get("colStart"));

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(table, reports);
        Type right = expressionVisitor.visit(jmmNode.getJmmChild(0), "");

        String left = jmmNode.get("var"); //ver tipo do var (left)

        JmmNode parent = jmmNode.getJmmParent();
        Type leftType= new Type("", false);

        while(!parent.getKind().equals("NormalMethod") && !parent.getKind().equals("MainMethod")) {
            parent = parent.getJmmParent();
        }

        String methodName;
        if(parent.getKind().equals("NormalMethod")){
            methodName = parent.get("methodName");
        }
        else{
            methodName = "main";
        }

        //see if var is a local variable
        List<Symbol> localVariables = table.getLocalVariables(methodName);

        if(localVariables != null){
            for (int i = 0; i < localVariables.size(); i++){
                if(localVariables.get(i).getName().equals(left)){
                    leftType = localVariables.get(i).getType();
                    break;
                }
            }
        }
        //se nao for uma local variable, é que se vai verificar se é um parametro
        if(leftType.getName().equals("")){
            //see if var is a parameter
            List<Symbol> parameters = table.getParameters(methodName);

            if(parameters != null){
                for (int i = 0; i < parameters.size(); i++){
                    if(parameters.get(i).getName().equals(left)){
                        leftType = parameters.get(i).getType();
                        break;
                    }
                }
            }
            //se ainda nao tiver encontrado, é que se vai verificar se é um field
            if(leftType.getName().equals("")) {
                //see if var is a field
                List<Symbol> fields = table.getFields();
                if(fields != null){
                    for (int i = 0; i < fields.size(); i++){
                        if(fields.get(i).getName().equals(left)){
                            if(methodName.equals("main")){
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Fields cannot be used in main method"));
                            }
                            leftType = fields.get(i).getType();
                            break;
                        }
                    }
                }
            }
        }


        //check if left (assignee) is superclass and right (assigned) is the current class
        if(leftType.getName().equals(table.getSuper()) && right.getName().equals(table.getClassName())){
            return right;
        }
        //if both are of type that are imported
        else if(table.getImports().contains(leftType.getName()) && table.getImports().contains(right.getName())){
            return right;
        }
        else if(right.getName().equals("CORRECT")){
            return right;
        }
        else if(!right.getName().equals(leftType.getName())){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Type of the assignee must be compatible with the assigned "));
            return new Type("ERROR", false);

        }
        return right;
    }

    private Type dealWithWhileStmt(JmmNode jmmNode, String s) {
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(table, reports);
        Type child = expressionVisitor.visit(jmmNode.getJmmChild(0), "");

        int line = Integer.parseInt(jmmNode.getJmmChild(0).get("lineStart"));
        int col = Integer.parseInt(jmmNode.getJmmChild(0).get("colStart"));
        if(!child.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Expressions in conditions (while) must be of type boolean"));
        }
        visit(jmmNode.getJmmChild(1));
        return child;
    }

    private Type dealWithIfElseStmt(JmmNode jmmNode, String s) {
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(table, reports);
        Type child = expressionVisitor.visit(jmmNode.getJmmChild(0), "");

        int line = Integer.parseInt(jmmNode.getJmmChild(0).get("lineStart"));
        int col = Integer.parseInt(jmmNode.getJmmChild(0).get("colStart"));
        //Expressions in conditions must return a boolean
        if(!child.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Expressions in conditions (if) must be of type boolean"));
        }
        visit(jmmNode.getJmmChild(1));
        visit(jmmNode.getJmmChild(2));
        return child;
    }

}
