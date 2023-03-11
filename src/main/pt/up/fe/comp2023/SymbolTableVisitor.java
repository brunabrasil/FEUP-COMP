package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;

import java.util.*;
public class SymbolTableVisitor extends AJmmVisitor<String,String >{

    private JmmSymbolTable table;
    private String scope;
    private List<Report> reports;

    public SymbolTableVisitor(JmmSymbolTable table, List<Report> reports) {
        this.table=table;
        this.reports=reports;

    }


    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram );
        addVisit("Import", this::dealWithImport);
        addVisit("Class", this::dealWithClassDeclaration);
        addVisit("Declaration", this::dealWithVarDeclaration);
        addVisit("NormalMethod", this::dealWithNormalMethodDeclaration);
        addVisit("StaticMethod", this::dealWithStaticMethodDeclaration);
        addVisit("Type", this::dealWithType);
        addVisit("Stmt", this::dealWithStmt);
        addVisit("IfElseStmt", this::dealWithIfElseStmt);
        addVisit("WhileStmt", this::dealWithWhileStmt);
        addVisit("Expr", this::dealWithExpr);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("Parenthesis", this::dealWithType);
        addVisit("Index", this::dealWithIndex);
        addVisit("CallMethod", this::dealWithVarDeclaration);
        addVisit("Unary", this::dealWithProgram );
        addVisit("BinaryOp", this::dealWithVarDeclaration);
        addVisit("Instantiation", this::dealWithProgram );
        addVisit("Integer", this::dealWithProgram );
        addVisit("This", this::dealWithProgram );
       // setDefaultVisit(this::defaultVisit);
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

    private String dealWithProgram(JmmNode jmmNode, String s) {
        s =(s!= null ?s:"");
        String ret="";
        for(JmmNode child : jmmNode.getChildren()){
            ret += visit(child,"");
            ret +="\n";
        }

        return ret;
    }

    private String dealWithImport(JmmNode jmmNode, String s){
        String ret=s+"import "+jmmNode.get("importName");
        List<String> vars= (List<String>) jmmNode.getObject("vars");
        if (vars!=null) {
            for (String names : vars) {
                ret += "." + names;
            }
        }
        table.addImport(ret);
        return null;
    }



    private String dealWithMainDeclaration(JmmNode jmmNode, String s) {
        return  null;
    }

    private String dealWithClassDeclaration(JmmNode jmmNode, String s) {
        String classname=jmmNode.get("className");
        table.setClassName(classname);

        try {
            String supername= jmmNode.get("extendsName");
            table.setSuperName(supername);
        } catch (NullPointerException ignored) {

        }

        for(JmmNode child : jmmNode.getChildren()){
            visit(child,"");
        }
        scope="CLASS";
        return  null;
    }
    private String dealWithNormalMethodDeclaration(JmmNode jmmNode, String s) {

        return  null;
    }
    private String dealWithStaticMethodDeclaration(JmmNode jmmNode, String s) {

        return  null;
    }
    private String dealWithVarDeclaration(JmmNode jmmNode, String s) {
        var typename=visit(jmmNode.getJmmChild(0),"");
        var isArray=typename.contains("[");
        var varname=jmmNode.get("varName");
        Type type=new Type(typename,isArray);
        Symbol symbol=new Symbol(type,varname);
        if(scope=="CLASS")
            table.addField(symbol);
        if(scope=="METHOD")
            table.addLocalVariable(symbol);
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
    private String dealWithType(JmmNode jmmNode, String s) {
        var type=jmmNode.get("name");
        return  type;
    }
}
