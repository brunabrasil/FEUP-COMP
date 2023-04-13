package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.Map;

import java.util.List;

public class ProgramVisitor extends AJmmVisitor<String, Type> {
    private JmmSymbolTable table;
    private List<Report> reports;

    public ProgramVisitor(JmmSymbolTable table, List<Report> reports){
        this.table=table;
        this.reports=reports;
    }
    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("Class", this::dealWithClass);
        addVisit("Import", this::dealWithImport);
        addVisit("Declaration", this::dealWithDeclaration);
        addVisit("NormalMethod", this::dealWithMethod);
        addVisit("MainMethod", this::dealWithMethod);

        addVisit("Type", this::dealWithType);

    }

    private Type dealWithType(JmmNode jmmNode, String s) {
        return null;
    }

    private Type dealWithMethod(JmmNode jmmNode, String s) {

        Type type = new Type("", false);
        List<JmmNode>children= jmmNode.getChildren();
        if(children != null){
            for (int i = 0; i < children.size(); i++){
                System.out.println(children.get(i));

                switch (children.get(i).getKind()){
                    case "Stmt":
                    case "IfElseStmt":
                    case "WhileStmt":
                    case "Expr":
                    case "Assignment":
                    case "AssignmentArray":
                        StatementVisitor statementAnalyser = new StatementVisitor(table,reports);
                        type = statementAnalyser.visit(children.get(i), "");
                        break;
                    case "Parenthesis":
                    case "Indexing":
                    case "Length":
                    case "CallMethod":
                    case "UnaryOp":
                    case "BinaryOp":
                    case "NewIntArray":
                    case "NewObject":
                    case "Integer":
                    case "Boolean":
                    case "Identifier":
                    case "This":
                        ExpressionVisitor expressionAnalyser = new ExpressionVisitor(table,reports);
                        type = expressionAnalyser.visit(children.get(i), "");
                        break;
                }
                if(jmmNode.getKind().equals("NormalMethod")){
                    String methodName = jmmNode.get("methodName");
                    if(children.size()-1 == i){
                        if(!type.getName().equals(table.getReturnType(methodName).getName())){
                            int line = Integer.valueOf(children.get(i).get("lineStart"));
                            int col = Integer.valueOf(children.get(i).get("colStart"));
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Return type"));

                        }
                    }
                }

            }
        }

        return null;
    }

    private Type dealWithDeclaration(JmmNode jmmNode, String s) {
        System.out.println("DeclarationName:" + jmmNode.get("varName"));

        for(JmmNode child: jmmNode.getChildren()){
            System.out.println("Declaration:" + child.getKind());
            visit(child, "");
        }
        return  null;
    }

    private Type dealWithClass(JmmNode jmmNode, String s) {
        for(JmmNode child: jmmNode.getChildren()){
            if(child.getKind().equals("NormalMethod")||child.getKind().equals("MainMethod")){
                visit(child, "");
            }

        }
        return null;
    }

    private Type dealWithImport(JmmNode jmmNode, String s) {
        return null;
    }

    private Type dealWithProgram(JmmNode jmmNode, String s) {
        for(JmmNode child: jmmNode.getChildren()){
            if(child.getKind().equals("Class")){
                visit(child,"");
            }

        }
        return null;
    }


}
