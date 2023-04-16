package pt.up.fe.comp2023;

import org.antlr.v4.runtime.misc.Pair;
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
    private String currentMethodName;
    private Integer tempcount=1;
    private String currentAssignmentType;
    private List<String> tempList= new ArrayList<>();
    private final List<String> statements = Arrays.asList("Stmt","IfElseStmt","WhileStmt","Expr","Assignment","ArrayAssignment");
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

        addVisit("Assignment",this::dealWithAssignment);
        addVisit("Integer",this::dealWithInteger);
        addVisit("Boolean",this::dealWithBoolean);
        addVisit("Identifier",this::dealWithIdentifier);
        addVisit("This",this::dealWithThis);

        addVisit("BinaryOp",this::dealWithBinaryOp);
        setDefaultVisit(this::defaultVisit);
    }



    private String defaultVisit(JmmNode jmmNode,String s){
        //System.out.println(jmmNode.getKind());
        return "";
    }

    private String dealWithProgram(JmmNode jmmNode, String s) {
        System.out.println("Program size:"+jmmNode.getChildren().size());
        String ret="";
        for(JmmNode child : jmmNode.getChildren()){
            ret += visit(child,"");
            //ret +="\n";
        }
        return ret;
    }

    private String dealWithImport(JmmNode jmmNode, String s) {
        System.out.println("Import index:"+jmmNode.getIndexOfSelf());
        /*
        for(String importstate : this.table.getImports()){
            s+=String.format("%s;\n",importstate);
        }
        s+="\n";
        */
        return "";
    }

    private String dealWithClassDeclaration(JmmNode jmmNode, String s) {
        System.out.println("Class index:"+jmmNode.getIndexOfSelf());
        List<String> fields = new ArrayList<>();
        List<String> classBody = new ArrayList<>();

        this.scope="CLASS";

        StringBuilder ollir = new StringBuilder();

        // IMPORTS
        for(String importstate : this.table.getImports()){
            ollir.append(String.format("%s;\n",importstate));
        }
        ollir.append("\n");

        // CLASS NAME AND EXXTEND
        ollir.append(OllirTemplates.classTemplate(this.table.getClassName(),this.table.getSuper()));


        ollir.append("\n");

        this.scope="CLASS";
        // Getting methods and fields
        for(JmmNode child : jmmNode.getChildren()){
            if (child.getKind().equals("Declaration")){
                fields.add(visit(child,""));
            }
            else {
                classBody.add(visit(child,""));
            }
            //System.out.println(child.getKind());
        }

        // Fields
        ollir.append(String.join("", fields)).append("\n\n");

        // EMPTY CONSTRUCTOR
        ollir.append(OllirTemplates.emptyConstructorTemplate(this.table.getClassName())).append("\n\n");

        // Methods
        ollir.append(String.join("\n\n", classBody));

        ollir.append(OllirTemplates.closeBrackets());

        s+=ollir.toString();
        return s;
    }

    private String dealWithVarDeclaration(JmmNode jmmNode, String s) {
        System.out.println("VarDeclaration index:"+jmmNode.getIndexOfSelf());
        var varname=jmmNode.get("varName");
        Type type=dealWithType(jmmNode.getJmmChild(0));
        Symbol symbol=new Symbol(type,varname);
        if(scope=="CLASS"){
            if(this.table.checkFieldExistance(symbol)){
                return OllirTemplates.fieldTemplate(symbol);
            }
        }


        return "";
    }

    private String dealWithMainMethodDeclaration(JmmNode jmmNode, String s) {
        System.out.println("MainMethod index:"+jmmNode.getIndexOfSelf());
        scope="METHOD";
        currentMethodName="main";

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
        System.out.println("NormalMethod index:"+jmmNode.getIndexOfSelf());
        scope="METHOD";
        String methodName = jmmNode.get("methodName");

        currentMethodName=methodName;



        StringBuilder ollir = new StringBuilder();
        ollir.append(OllirTemplates.methodTemplate(methodName,
                this.table.parametersToOllir(methodName),
                OllirTemplates.typeTemplate(this.table.getReturnType(methodName)),false));

        List<String> body = new ArrayList<>();
        String returnString=String.format("ret%s ",OllirTemplates.typeTemplate(this.table.getReturnType(methodName)));

        for (JmmNode child : jmmNode.getChildren()) {
            if(child.getKind().equals("Type"))
                continue;
            else if(statements.contains(child.getKind()))
                body.add( visit(child,""));
            else
                returnString+=visit(child,"");
        }

        returnString+=";";
        ollir.append(String.join("\n", body));

        if(tempList.size()>0){
            ollir.append(String.join("\n", tempList)).append("\n");
        }
        ollir.append(returnString);

        ollir.append(OllirTemplates.closeBrackets());

        s+=ollir.toString();


        return s;
    }
    private String dealWithAssignment(JmmNode jmmNode,String s){

        System.out.println("Assignemt index:"+jmmNode.getIndexOfSelf());
        String varName=jmmNode.get("var");
        boolean classField=false;
        Symbol variable;
        if((variable=this.table.getFieldByName(varName))!=null){
            classField=true;
        }
        else if((variable=this.table.getParameterInMethod(currentMethodName,varName))==null){
            variable=this.table.getVariableInMethod(currentMethodName,varName);
        }


        String ollirVariable=OllirTemplates.variableTemplate(variable);
        String ollirType=OllirTemplates.typeTemplate(variable.getType());

        StringBuilder ollir = new StringBuilder();
        this.currentAssignmentType=ollirType;

        String expression=visit(jmmNode.getJmmChild(0),"");
        if(tempList.size()>0){
            ollir.append(String.join("\n", tempList));
        }
        tempList.clear();
        if(classField){

            ollir.append(OllirTemplates.putfieldTemplate(ollirVariable,expression));
        }
        else{
            ollir.append(String.format("%s :=%s %s;\n",ollirVariable,ollirType,expression));
        }




        s+=ollir.toString();
        return s;

    }




    private Type dealWithType(JmmNode jmmNode) {
        var typename=jmmNode.get("name");
        var isArray=(Boolean) jmmNode.getObject("isArray");
        Type type=new Type(typename,isArray);
        return  type;
    }


    private String dealWithInteger(JmmNode jmmNode,String s){
        System.out.println("DealWithInteger index"+jmmNode.getIndexOfSelf());
        Symbol integer=new Symbol(new Type("int",false),jmmNode.get("value"));
        return OllirTemplates.variableTemplate(integer);
    }

    private String dealWithThis(JmmNode jmmNode, String s) {
        return "";
    }

    private String dealWithIdentifier(JmmNode jmmNode, String s) {
        System.out.println("DealWitIdentifier index="+jmmNode.getIndexOfSelf());
        String val = jmmNode.get("value");
        Symbol variable;
        // It's a class field
        if((variable=this.table.getFieldByName(val))!=null){
            return OllirTemplates.getfieldTemplate(variable);
        }
        // It's a method parameter
        else if((variable=this.table.getParameterInMethod(currentMethodName,val))!=null){
            return this.table.parameterNumber(currentMethodName,val)+"."+OllirTemplates.variableTemplate(variable);
        }
        // It's a method local variable
        else{
            variable=this.table.getVariableInMethod(currentMethodName,val);
        }


        return OllirTemplates.variableTemplate(variable);
    }

    private String dealWithBoolean(JmmNode jmmNode, String s) {
        System.out.println("DealWitBollean index="+jmmNode.getIndexOfSelf());
        String boolvalue="1";
        if(jmmNode.get("value")=="false")
            boolvalue="0";
        Symbol bool=new Symbol(new Type("boolean",false),boolvalue);

        return OllirTemplates.variableTemplate(bool);
    }
    private String dealWithBinaryOp(JmmNode jmmNode, String s) {

        String op=jmmNode.get("op");
        if(op=="<" || op=="&&"){
            // TODO: In the future i think
            return  null;
        }

        boolean needsTemp=false;
        var parent=jmmNode.getJmmParent();
        if(!parent.getKind().equals("Assignment") || !parent.getKind().equals("NormalMethod") ){
          needsTemp=true;
        }


        Integer leftChildNumber=jmmNode.getJmmChild(0).getChildren().size();
        Integer rightChildNumber=jmmNode.getJmmChild(1).getChildren().size();
        String left="";
        String right= "";
        if (leftChildNumber<rightChildNumber){
            right= visit(jmmNode.getJmmChild(1));
            left=visit(jmmNode.getJmmChild(0));
        }
        else{
            left=visit(jmmNode.getJmmChild(0));
            right= visit(jmmNode.getJmmChild(1));
        }

        StringBuilder ollir=new StringBuilder();
        if(needsTemp){
            String tempName="temporary"+tempcount;
            tempcount++;
            ollir.append(String.format("%s%s :=%s %s %s%s %s;",tempName,currentAssignmentType,currentAssignmentType,left,op,currentAssignmentType,right));
            tempList.add(ollir.toString());
            return tempName+currentAssignmentType;
        }


        ollir.append(String.format("%s %s%s %s",left,op,currentAssignmentType,right));
        return ollir.toString();
    }

}
