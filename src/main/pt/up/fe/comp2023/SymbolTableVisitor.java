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
        addVisit("MainMethod", this::dealWithMainMethodDeclaration);
        addVisit("Type", this::dealWithType);
        /*addVisit("Stmt", this::dealWithStmt);
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
        addVisit("This", this::dealWithProgram );*/
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
        return "";
    }

    private String dealWithClassDeclaration(JmmNode jmmNode, String s) {
        String classname=jmmNode.get("className");
        table.setClassName(classname);
        scope="CLASS";

        try {
            String supername= jmmNode.get("extendsName");
            table.setSuperName(supername);
        } catch (NullPointerException ignored) {

        }

        for(JmmNode child : jmmNode.getChildren()){
            visit(child,"");
        }

        return  "";
    }

    private String dealWithMainMethodDeclaration(JmmNode jmmNode, String s) {
        scope="METHOD";
        String methodName = "main";
        Type methodType=new Type("void",false);
        table.addMethod(methodName,methodType);


        List<Symbol> methodParam=new ArrayList<>();

        // Getting parameters
        var paramName=jmmNode.get("parameter");
        Type paramType=new Type("String",true);
        Symbol paramSymbol=new Symbol(paramType,paramName);
        methodParam.add(paramSymbol);
        table.addMethodParameters(methodName,methodParam);

        // Getting localVariables
        for(JmmNode child : jmmNode.getChildren()){
            if(child.getKind().equals("Declaration")){
                visit(child,"");
            }
        }
        table.addLocalVariables(methodName);
        return  "";
    }


    private String dealWithNormalMethodDeclaration(JmmNode jmmNode, String s) {
        scope="METHOD";
        String methodName = jmmNode.get("methodName");
        String typename=visit(jmmNode.getJmmChild(0));
        Boolean isArray=typename.contains("[");
        Type methodType=new Type(typename,isArray);
        table.addMethod(methodName,methodType);

        List<Symbol> methodParams=new ArrayList<>();

        List<String> paramNames=(List<String>) jmmNode.getObject("parameters");
        int i=0;
        for(JmmNode child : jmmNode.getChildren()){
            // Ignore the first "type"
            if(i==0){
                i++;
                continue;
            }
            // Method Parameters
            if(child.getKind().equals("type")){
                String paramTypeName=visit(child,"");
                Boolean paramIsArray=paramTypeName.contains("[");
                Type paramType=new Type(paramTypeName,paramIsArray);
                String paramName=paramNames.get(i-1);
                Symbol paramSymbol=new Symbol(paramType,paramName);
                methodParams.add(paramSymbol);
            }
            //
            if(child.getKind().equals("Declaration")){
                visit(child,"");
            }
        }

        table.addLocalVariables(methodName);
        table.addMethodParameters(methodName,methodParams);


        return  "";


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
            table.addTempLocalVariable(symbol);
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
