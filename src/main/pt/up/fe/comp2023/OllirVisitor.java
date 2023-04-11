package pt.up.fe.comp2023;

import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;

import java.util.*;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
public class OllirVisitor extends AJmmVisitor<String,String > {
    private JmmSymbolTable table;
    private List<Report> reports;
    private String scope;
    public OllirVisitor(JmmSymbolTable table, List<Report> reports){
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
    }

    private String dealWithProgram(JmmNode jmmNode, String s) {
        String ret="";
        for(JmmNode child : jmmNode.getChildren()){
            ret += visit(child,"");
            ret +="\n";
        }
        return ret;
    }

    private String dealWithImport(JmmNode jmmNode, String s) {

        for(String importstate : this.table.getImports()){
            s+=String.format("%s;\n",importstate);
        }
        s+="\n";

        return s;
    }

    private String dealWithClassDeclaration(JmmNode jmmNode, String s) {

        List<String> fields = new ArrayList<>();
        List<String> classBody = new ArrayList<>();

        this.scope="CLASS";

        StringBuilder ollir = new StringBuilder();

        ollir.append(OllirTemplates.classTemplate(this.table.getClassName(),this.table.getSuper()));


        ollir.append("\n");
        ollir.append(OllirTemplates.emptyConstructorTemplate(this.table.getClassName()));

        // Methods
        for(JmmNode child : jmmNode.getChildren()){
            if (!child.getKind().equals("VarDeclaration")){
                classBody.add(visit(child,""));
            }
        }

        ollir.append(String.join("\n\n", classBody));

        ollir.append(OllirTemplates.closeBrackets());

        s+=ollir.toString();
        return s;
    }

    private String dealWithVarDeclaration(JmmNode jmmNode, String s) {

        return null;
    }

    private String dealWithMainMethodDeclaration(JmmNode jmmNode, String s) {
        scope="METHOD";
        Type mainType=new Type("void",false);
        Symbol mainSymbol=new Symbol(mainType,"main");
        List<Symbol> parameters = new ArrayList<>();
        parameters.add(mainSymbol);

        if(!this.table.methodExists("main",mainType,parameters)){
            // TODO report error
            return null;
        }

        StringBuilder ollir = new StringBuilder();
        ollir.append(OllirTemplates.methodTemplate("main",
                this.table.parametersToOllir("main"),
                OllirTemplates.typeTemplate(this.table.getReturnType("main")),true));

        List<String> body = new ArrayList<>();

        this.scope="METHOD";
        for (JmmNode child : jmmNode.getChildren()) {
            String ollirChild = visit(child, "");
            body.add(ollirChild);
        }

        ollir.append(String.join("\n", body));


        ollir.append("\nret.V;");
        ollir.append(OllirTemplates.closeBrackets());
        s+=ollir.toString();
        return s;

    }

    private String dealWithNormalMethodDeclaration(JmmNode jmmNode, String s) {
        scope="METHOD";
        String methodName = jmmNode.get("methodName");
        Type methodType=dealWithType(jmmNode.getJmmChild(0));

        List<Symbol> methodParams = getMethodParameters(jmmNode);


        if(!this.table.methodExists(methodName,methodType,methodParams)){
            // TODO report error
            return null;
        }

        StringBuilder ollir = new StringBuilder();
        ollir.append(OllirTemplates.methodTemplate(methodName,
                this.table.parametersToOllir(methodName),
                OllirTemplates.typeTemplate(this.table.getReturnType(methodName)),false));

        List<String> body = new ArrayList<>();

        for (JmmNode child : jmmNode.getChildren()) {
            if(child.getKind().equals("Type"))
                continue;
            body.add( visit(child,""));
        }

        ollir.append(String.join("\n", body));

        ollir.append(OllirTemplates.closeBrackets());

        s+=ollir.toString();


        return s;
    }

    private Type dealWithType(JmmNode jmmNode) {
        var typename=jmmNode.get("name");
        var isArray=(Boolean) jmmNode.getObject("isArray");
        Type type=new Type(typename,isArray);
        return  type;
    }

    private List<Symbol> getMethodParameters(JmmNode jmmNode){
        List<Symbol> parameters = new ArrayList<>();
        List<String> paramNames=(List<String>) jmmNode.getObject("parameters");
        var i=0;
        for(JmmNode child : jmmNode.getChildren()){
            // Ignore the first "type"
            if(i==0){
                i++;
                continue;
            }
            // Method Parameters
            if(child.getKind().equals("Type")){
                Type paramType=dealWithType(child);
                String paramName=paramNames.get(i-1);
                Symbol paramSymbol=new Symbol(paramType,paramName);
                parameters.add(paramSymbol);
            }
        }
        return parameters;
    }

}
