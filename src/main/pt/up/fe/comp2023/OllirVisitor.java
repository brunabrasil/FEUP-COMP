package pt.up.fe.comp2023;

import org.antlr.v4.runtime.misc.Pair;
import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
public class OllirVisitor extends AJmmVisitor<String,String > {
    private JmmSymbolTable table;
    private List<Report> reports;
    private String scope;
    private String currentMethodName;
    private Integer tempcount=0;
    private String currentAssignmentType;
    private String currentCallMethodName;
    private Type assignType;
    private List<String> tempList= new ArrayList<>();
    private final List<String> statements = Arrays.asList("Stmt","IfElseStmt","WhileStmt","Expr","Assignment","ArrayAssignment");
    public OllirVisitor(JmmSymbolTable table, List<Report> reports){
        this.table=table;
        this.reports=reports;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram );
        addVisit("Class", this::dealWithClassDeclaration);
        addVisit("Declaration", this::dealWithVarDeclaration);
        addVisit("NormalMethod", this::dealWithNormalMethodDeclaration);
        addVisit("MainMethod", this::dealWithMainMethodDeclaration);

        // Statements
        addVisit("Assignment",this::dealWithAssignment);
        addVisit("Expr",this::dealWithExpr);
        // Expressions
        addVisit("CallMethod",this::dealWithCallMethod);
        addVisit("Length",this::dealWithLength);
        addVisit("BinaryOp",this::dealWithBinaryOp);
        addVisit("NewObject",this::dealWithNewObject);
        addVisit("Integer",this::dealWithInteger);
        addVisit("Boolean",this::dealWithBoolean);
        addVisit("Identifier",this::dealWithIdentifier);
        addVisit("This",this::dealWithThis);


        setDefaultVisit(this::defaultVisit);
    }


    private String defaultVisit(JmmNode jmmNode,String s){
        //System.out.println(jmmNode.getKind());
        return "";
    }

    private String dealWithProgram(JmmNode jmmNode, String s) {
        String ret="";
        for(JmmNode child : jmmNode.getChildren()){
            ret += visit(child,"");
            //ret +="\n";
        }
        System.out.println(ret);

        return ret;
    }


    private String dealWithClassDeclaration(JmmNode jmmNode, String s) {
        List<String> fields = new ArrayList<>();
        List<String> classBody = new ArrayList<>();

        this.scope="CLASS";

        StringBuilder ollir = new StringBuilder();

        // IMPORTS
        for(String importstate : this.table.getImports()){
            ollir.append(String.format("import %s;\n",importstate));
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
        scope="METHOD";
        currentMethodName="main";
        tempList.clear();
        StringBuilder ollir = new StringBuilder();
        // Appending the Main Method Template to the final string
        ollir.append(OllirTemplates.methodTemplate("main",
                this.table.parametersToOllir("main"),
                OllirTemplates.typeTemplate(this.table.getReturnType("main")),true));

        // Stores the string of the childs of the method
        List<String> body = new ArrayList<>();

        this.scope="METHOD";
        for (JmmNode child : jmmNode.getChildren()) {
            String ollirChild = visit(child, "");
            if(ollirChild!="")
                body.add(ollirChild);
        }
        //Separates everything in the body with an \n
        ollir.append(String.join("\n", body));

        // The return is always void
        //ollir.append("\nret.V;");
        ollir.append(OllirTemplates.closeBrackets());
        s+=ollir.toString();
        return s;

    }

    private String dealWithNormalMethodDeclaration(JmmNode jmmNode, String s) {
        scope="METHOD";
        String methodName = jmmNode.get("methodName");
        currentMethodName=methodName;
        tempList.clear();

        StringBuilder ollir = new StringBuilder();
        // Appending the Normal Method Template to the final string
        ollir.append(OllirTemplates.methodTemplate(methodName,
                this.table.parametersToOllir(methodName),
                OllirTemplates.typeTemplate(this.table.getReturnType(methodName)),false));

        List<String> body = new ArrayList<>();
        // Mini template for the return
        String returnString=String.format("ret%s ",OllirTemplates.typeTemplate(this.table.getReturnType(methodName)));

        for (JmmNode child : jmmNode.getChildren()) {
            // Types are skipped because they are parameters
            if(child.getKind().equals("Type"))
                continue;
            else if(statements.contains(child.getKind()))
                body.add( visit(child,""));
            else{
                String temp=visit(child,"return");
                if(temp=="this"){
                    returnString+=temp+OllirTemplates.typeTemplate(this.table.getReturnType(methodName));
                }
                else{
                    returnString+=temp;
                }
            }

        }
        returnString+=";";

        //Separates everything in the body with an \n
        ollir.append(String.join("\n", body));

        // If the tempList is not empty, it's because it's created at least one variable in the return
        if(tempList.size()>0){
            ollir.append(String.join("\n", tempList)).append("\n");
        }
        ollir.append(returnString);

        ollir.append(OllirTemplates.closeBrackets());

        s+=ollir.toString();


        return s;
    }
    private String dealWithAssignment(JmmNode jmmNode,String s){

        scope="ASSIGNMENT";
        String varName=jmmNode.get("var");
        boolean classField=false;
        boolean isMethodParameter=false;
        Symbol variable;

        /*
        if((variable=this.table.getVariableInMethod(currentMethodName,varName))==null){
            System.out.println("NOT A VAR");
            if((variable=this.table.getParameterInMethod(currentMethodName,varName))==null){
                variable=this.table.getFieldByName(varName);
                classField=true;
            }
        }*/
        variable=this.table.getVariableInMethod(currentMethodName,varName);
        if(variable==null){
            variable=this.table.getParameterInMethod(currentMethodName,varName);
            isMethodParameter=true;
            if(variable==null){
                variable=this.table.getFieldByName(varName);
                isMethodParameter=false;
                classField=true;
            };
        }

/*
        // See if it's a field of the class
        if((variable=this.table.getFieldByName(varName))!=null){
            classField=true;
        }
        // see if it's a method parameter
        else if((variable=this.table.getParameterInMethod(currentMethodName,varName))==null){
            // If it's not a parameter then its a LocalVariable
            variable=this.table.getVariableInMethod(currentMethodName,varName);
        }
*/
        // Storing the ollir variable and type template into variables
        String ollirVariable;
        if(isMethodParameter){
            ollirVariable=this.table.parameterNumber(currentMethodName,varName)+"."+OllirTemplates.variableTemplate(variable);
        }
        else{
            ollirVariable=OllirTemplates.variableTemplate(variable);
        }

        String ollirType=OllirTemplates.typeTemplate(variable.getType());

        StringBuilder ollir = new StringBuilder();
        this.currentAssignmentType=ollirType;
        this.assignType=variable.getType();
        // Expression stores what comes after "="
        String expression=visit(jmmNode.getJmmChild(0),"");
        // Check if temporary variables were created
        if(tempList.size()>0){
            ollir.append(String.join("\n", tempList));
        }
        // It's cleared so it doesn't mess up in the future visits

        if(classField){
            // The equals to CallMethod is included so there isn't an extra space when callmethod is visited
            if(tempList.size()>0 && !jmmNode.getJmmChild(0).getKind().equals("CallMethod"))
                ollir.append("\n");
            ollir.append(OllirTemplates.putfieldTemplate(ollirVariable,expression));
        }
        else{
            // If it's a NewObject we need to create a temporary variable
            if(jmmNode.getJmmChild(0).getKind().equals("NewObject")){
                String tempName="temp_"+tempcount;
                tempcount++;
                // Dont need to add the temporary to tempList because we are appending the string alredy right bellow
                ollir.append(String.format("%s%s :=%s %s;",tempName,ollirType,ollirType,expression)).append("\n");
                ollir.append(OllirTemplates.objectInstanceTemplate(tempName,ollirType)).append("\n");
                ollir.append(String.format("%s :=%s %s%s;",ollirVariable,ollirType,tempName,ollirType)).append("\n");
            }
            else{
                ollir.append(String.format("%s :=%s %s;\n",ollirVariable,ollirType,expression));

            }

        }
        tempList.clear();
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
        Symbol integer=new Symbol(new Type("int",false),jmmNode.get("value"));
        return OllirTemplates.variableTemplate(integer);
    }

    private String dealWithThis(JmmNode jmmNode, String s) {
        return "this";
    }

    private String dealWithIdentifier(JmmNode jmmNode, String s) {
        String val = jmmNode.get("value");


        Symbol variable;
        // It's a method local variable
        variable=this.table.getVariableInMethod(currentMethodName,val);
        if(variable!=null){
            return OllirTemplates.variableTemplate(variable);
        }
        // It's a method parameter
        else if((variable=this.table.getParameterInMethod(currentMethodName,val))!=null){
            return this.table.parameterNumber(currentMethodName,val)+"."+OllirTemplates.variableTemplate(variable);
        }
        // It's a field
        else if((variable=this.table.getFieldByName(val))!=null){
            if(s=="parameter" || s=="return" || jmmNode.getJmmParent().getKind().equals("BinaryOp")){
                String tempName="temp_"+tempcount;
                tempcount++;
                StringBuilder ollir=new StringBuilder();
                ollir.append(String.format("%s%s :=%s %s;\n",tempName,OllirTemplates.typeTemplate(variable.getType()),OllirTemplates.typeTemplate(variable.getType()),OllirTemplates.getfieldTemplate(variable)));
                tempList.add(ollir.toString());
                return tempName+OllirTemplates.typeTemplate(variable.getType());
            }
            return OllirTemplates.getfieldTemplate(variable);
        }

        // It's a method caller, Ex:for "io.println(a)" its "io"
        return val;

    }

    private String dealWithBoolean(JmmNode jmmNode, String s) {
        String boolvalue="1";
        if(jmmNode.get("value")=="false")
            boolvalue="0";
        Symbol bool=new Symbol(new Type("boolean",false),boolvalue);

        return OllirTemplates.variableTemplate(bool);
    }
    private String dealWithBinaryOp(JmmNode jmmNode, String s) {
        String op=jmmNode.get("op");
        Type returnType=new Type("int",false);
        if(op=="<" || op=="&&"){
            returnType=new Type("boolean",false);
        }
        String ollirType=OllirTemplates.typeTemplate(returnType);

        boolean needsTemp=false;
        var parent=jmmNode.getJmmParent();
        if(scope=="ASSIGNMENT"){
            if(!parent.getKind().equals("Assignment"))
                needsTemp=true;
            else if(parent.getKind().equals("Assignment")){
                if(this.table.getFieldByName(parent.get("var"))!=null)
                    needsTemp=true;
            }
        }
        else if(scope=="METHOD"){
            if(!parent.getKind().equals("NormalMethod"))
                needsTemp=true;
        }


        String left=visit(jmmNode.getJmmChild(0));
        String right= visit(jmmNode.getJmmChild(1));

        StringBuilder ollir=new StringBuilder();
        if(needsTemp){
            String tempName="temp_"+tempcount;
            tempcount++;
            ollir.append(String.format("%s%s :=%s %s %s%s %s;\n",tempName,ollirType,ollirType,left,op,ollirType,right));
            tempList.add(ollir.toString());
            return tempName+ollirType;
        }


        ollir.append(String.format("%s %s%s %s",left,op,ollirType,right));
        return ollir.toString();
    }

    private String dealWithExpr(JmmNode jmmNode, String s) {

        StringBuilder ollir=new StringBuilder();
        // TODO: SEE IF ONLY METHODCALLS CAN BE USED HERE
        String temp=visit(jmmNode.getJmmChild(0));

        if(tempList.size()>0){
            if(tempNotContained(temp)){
                ollir.append(String.join("\n", tempList));
            }
        }
        tempList.clear();
        ollir.append(temp);

        return ollir.toString();
    }

    private String dealWithCallMethod(JmmNode jmmNode, String s) {

        StringBuilder ollir = new StringBuilder();
        var parent=jmmNode.getJmmParent();
        Boolean needsTemp=false;;
        if(!parent.getKind().equals("Expr"))
            needsTemp=true;
        String caller= visit(jmmNode.getJmmChild(0));
        Boolean isIntance=checkInstance(caller);
        String functionName=jmmNode.get("name");

        Type functionType;

        var children=jmmNode.getChildren();

        String parameters=getParametersList(children,functionName);
        if(tempList.size()>0){
            ollir.append(String.join("", tempList));
        }

        if(isIntance){
            // Its either a normal type like int or the own class type
            if(isClassVariable(caller))
                functionType=this.table.getMethodType(functionName);
            // Its a variable thats from an import or from the extended class
            else{
                functionType=new Type("void",false);
                if(parent.getKind().equals("Assignment") || parent.getKind().equals("BinaryOp") || parent.getKind().equals("CallMethod")){
                    functionType=assignType;
                    if(parent.getKind().equals("CallMethod")){
                        functionType=this.table.getParamFromNumber(currentCallMethodName, parent.getChildren().indexOf(jmmNode));
                    }
                }
            }
            if(needsTemp){
                String tempName="temp_"+tempcount;
                tempcount++;
                tempList.add(String.format("%s%s :=%s %s;\n",tempName,OllirTemplates.typeTemplate(functionType),OllirTemplates.typeTemplate(functionType),OllirTemplates.invokevirtualTemplate(caller,functionName,functionType,parameters)));
                return tempName+OllirTemplates.typeTemplate(functionType);
            }
            ollir.append(OllirTemplates.invokevirtualTemplate(caller,functionName,functionType,parameters));
        }
        else {
            Boolean needsType=false;
            Type returnType=assignType;
            if(parent.getKind().equals("Assignment") || parent.getKind().equals("BinaryOp") || parent.getKind().equals("CallMethod")){
                needsType=true;
                if(parent.getKind().equals("CallMethod")){
                    returnType=this.table.getParamFromNumber(currentCallMethodName, parent.getChildren().indexOf(jmmNode));
                }
            }
            if(this.table.getClassName().equals(caller)){
                returnType=this.table.getMethodType(functionName);
            }
            if(needsTemp){
                String tempName="temp_"+tempcount;
                tempcount++;
                if (needsType){
                    tempList.add(String.format("%s%s :=%s %s;\n",tempName,OllirTemplates.typeTemplate(returnType),OllirTemplates.typeTemplate(returnType),OllirTemplates.invokestaticTemplate(caller,functionName,returnType,parameters)));
                    return tempName+OllirTemplates.typeTemplate(returnType);
                }
                else{
                    tempList.add(String.format("%s%s :=%s %s;\n",tempName,OllirTemplates.typeTemplate(new Type("void",false)),OllirTemplates.typeTemplate(new Type("void",false)),OllirTemplates.invokestaticTemplate(caller,functionName,new Type("void",false),parameters)));
                    return tempName+OllirTemplates.typeTemplate(new Type("void",false));
                }
            }

            if(needsType){
                ollir.append(OllirTemplates.invokestaticTemplate(caller,functionName,returnType,parameters));
            }
            else{
                if(this.table.getClassName().equals(caller)){
                    ollir.append(OllirTemplates.invokestaticTemplate(caller,functionName,returnType,parameters));
                }
                else{
                    ollir.append(OllirTemplates.invokestaticTemplate(caller,functionName,new Type("void",false),parameters));
                }

            }

        }
        ollir.append(";\n");

        return ollir.toString();
    }

    private String dealWithNewObject(JmmNode jmmNode, String s) {
        String objectName=jmmNode.get("name");
        return OllirTemplates.objectInitTemplate(objectName);
    }


    private String getParametersList(List<JmmNode> children,String functionName){

        List<String> params=new ArrayList<>();
        List<String> paramsOllir = new ArrayList<>();
        for(var child : children){
            //It's not a parameter so we can skip it
            if(children.indexOf(child)==0){
                continue;
            }
            Type type;
            switch(child.getKind()){
                case"Integer":
                    type= new Type("int",false);
                    paramsOllir.add(String.format("%s%s",child.get("value"),OllirTemplates.typeTemplate(type)));
                    break;
                case "Boolean":
                    type= new Type("boolean",false);
                    paramsOllir.add(String.format("%s%s",child.get("value"),OllirTemplates.typeTemplate(type)));
                    break;
                case "Identifier":
                    paramsOllir.add(visit(child,"parameter"));
                    break;
                case "This":
                    paramsOllir.add("this");
                    break;
                case "BinaryOp":
                    paramsOllir.add(visit(child,""));
                    break;
                case "CallMethod":
                    currentCallMethodName=functionName;
                    paramsOllir.add(visit(child,""));
                    break;
                case "Length":
                    currentCallMethodName=functionName;
                    paramsOllir.add(visit(child,""));
                    break;
            }
        }


        return String.join(", ", paramsOllir);
    }
    private String dealWithLength(JmmNode jmmNode, String s) {
        scope="LENGTH";
        String caller=visit(jmmNode.getJmmChild(0));
        StringBuilder ollir=new StringBuilder();

        var parent=jmmNode.getJmmParent();
        Boolean needsTemp=false;

        Type returnType=new Type("int",false);
        if(!parent.getKind().equals("Expr"))
            needsTemp=true;

        if(checkInstance(caller)){
            if(needsTemp){
                String tempName="temp_"+tempcount;
                tempcount++;
                tempList.add(String.format("%s%s :=%s %s;\n",tempName,OllirTemplates.typeTemplate(returnType),OllirTemplates.typeTemplate(returnType),OllirTemplates.invokevirtualTemplate(caller,"length",returnType,"")));
                return tempName+OllirTemplates.typeTemplate(returnType);
            }
            ollir.append(OllirTemplates.invokevirtualTemplate(caller,"length",returnType,""));
        }
        else {
            Boolean needsType=false;
            returnType=assignType;
            if(parent.getKind().equals("Assignment") || parent.getKind().equals("BinaryOp") || parent.getKind().equals("CallMethod")){
                needsType=true;
                if(parent.getKind().equals("CallMethod"))
                    returnType=this.table.getParamFromNumber(currentCallMethodName, parent.getChildren().indexOf(jmmNode));
            }
            if(needsTemp){
                String tempName="temp_"+tempcount;
                tempcount++;
                if (needsType){
                    tempList.add(String.format("%s%s :=%s %s;\n",tempName,OllirTemplates.typeTemplate(returnType),OllirTemplates.typeTemplate(returnType),OllirTemplates.invokestaticTemplate(caller,"length",returnType,"")));
                    return tempName+OllirTemplates.typeTemplate(returnType);
                }
                else{
                    tempList.add(String.format("%s%s :=%s %s;\n",tempName,OllirTemplates.typeTemplate(new Type("void",false)),OllirTemplates.typeTemplate(new Type("void",false)),OllirTemplates.invokestaticTemplate(caller,"length",new Type("void",false),"")));
                    return tempName+OllirTemplates.typeTemplate(new Type("void",false));
                }
            }

            if(needsType){
                ollir.append(OllirTemplates.invokestaticTemplate(caller,"length",returnType,""));
            }
            else{
                ollir.append(OllirTemplates.invokestaticTemplate(caller,"length",new Type("void",false),""));
            }

        }
        ollir.append(";\n");

        return ollir.toString();

    }
    private Boolean checkInstance(String caller){
        if (this.table.getImports().contains(caller) || this.table.getClassName().equals(caller)) {
            return false;
        }
        if(this.table.getSuper()!=null){
            if(this.table.getSuper().equals(caller))
                return false;
        }
        return  true;
    }

    private Boolean isClassVariable(String caller){
        // Need to check right away because this doesn´t have a type
        if(caller=="this")
            return true;

        // Caller can be like a.i32 or a.ClassName
        String[] splitCaller=caller.split("\\.");

        String type="";
        // If its a method paramether they are like $1.a.int
        if(splitCaller.length>2)
            type=splitCaller[2];
        else
            type=splitCaller[1];

        // Its a varible form
        if(this.table.getImports().contains(type))
            return false;

        if(this.table.getSuper()!=null)
            if(this.table.getSuper().equals(caller))
                return false;

        return true;
    }


    // Checks if the code needed to generate a temp_X variable is already included
    private boolean tempNotContained(String code) {
        String[] instructions=code.split("\n");
        for (int i = 0; i < instructions.length; i++) {
            instructions[i] += "\n";
        }

        for(var inst: instructions){
            for(var temp: tempList){
                if(inst.equals(temp))
                    return false;
            }
        }
        return true;
    }

}
