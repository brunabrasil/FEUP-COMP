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

public class ExpressionVisitor extends AJmmVisitor<String, Type> {
    private JmmSymbolTable table;
    private List<Report> reports;

    public ExpressionVisitor(JmmSymbolTable table, List<Report> reports){
        this.table=table;
        this.reports=reports;
    }
    @Override
    protected void buildVisitor() {
        addVisit("Parenthesis", this::dealWithParenthesis);
        addVisit("Indexing", this::dealWithIndex);
        addVisit("CallMethod", this::dealWithCallMethod);
        addVisit("UnaryOp", this::dealWithUnaryOp );
        addVisit("BinaryOp", this::dealWithBinOp);
        addVisit("NewObject", this::dealWithNewObject);
        addVisit("NewIntArray", this::dealWithNewIntArray);
        addVisit("Identifier", this::dealWithIdentifier);
        addVisit("This", this::dealWithThis);
        addVisit("Boolean", this::dealWithBoolean);
        addVisit("Integer", this::visitInteger);
        addVisit("Length", this::visitLength);

    }

    private Type dealWithNewIntArray(JmmNode jmmNode, String s) {
        Type child = visit(jmmNode.getJmmChild(0), "");
        Integer line = Integer.valueOf(jmmNode.getJmmChild(0).get("lineStart"));
        Integer col = Integer.valueOf(jmmNode.getJmmChild(0).get("colStart"));

        if (!child.getName().equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col,
                    "Initializing a new array requires an integer size"));
        }
        return new Type("int", true);
    }

    private Type dealWithNewObject(JmmNode jmmNode, String s) {
        return new Type(jmmNode.get("name"), false);
    }

    private Type visitLength(JmmNode jmmNode, String s) {
        int line = Integer.valueOf(jmmNode.getJmmChild(0).get("lineStart"));
        int col = Integer.valueOf(jmmNode.getJmmChild(0).get("colStart"));
        Type type = visit(jmmNode.getJmmChild(0));
        if(!type.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, jmmNode.getJmmChild(0).get("name") + " is not an array and can not use length method"));
            return new Type("ERROR", false);
        }
        return new Type("int", false);
    }

    private Type visitInteger(JmmNode jmmNode, String s) {
        return new Type("int", false);
    }

    private Type dealWithBoolean(JmmNode jmmNode, String s) {
        return new Type("boolean", false);
    }

    private Type dealWithIdentifier(JmmNode jmmNode, String s) {
        String val = jmmNode.get("value");
        int line = Integer.valueOf(jmmNode.get("lineStart"));
        int col = Integer.valueOf(jmmNode.get("colStart"));
        JmmNode parent = jmmNode.getJmmParent();

        while(!parent.getKind().equals("NormalMethod") && !parent.getKind().equals("MainMethod")) {
            parent = parent.getJmmParent();
        }
        if(parent.getKind().equals("NormalMethod")) {
            String method = parent.get("methodName");
            List<Symbol> locals = table.getLocalVariables(method);
            if(locals != null){
                for(Symbol local : locals) {
                    if(local.getName().equals(val)) {
                        return local.getType();
                    }
                }
            }

            List<Symbol> params = table.getParameters(method);
            if(params != null){
                for(Symbol param : params) {
                    if(param.getName().equals(val)) {
                        return param.getType();
                    }
                }
            }
            List<Symbol> fields = table.getFields();
            if(fields != null){
                for(Symbol field : fields) {
                    if(field.getName().equals(val)) {
                        if (parent.getKind().equals("MainMethod")) { //ver istoo INUTILLL
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col,
                                    val + " is a field and can't be used in main method"));
                        }
                        return field.getType();
                    }
                }
            }


            if((table.getImports() == null || !table.getImports().contains(val))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Variable not declared"));
            }
        }
        return new Type("ERROR", false);
    }

    private Type dealWithThis(JmmNode jmmNode, String s) {
        JmmNode parent = jmmNode.getJmmParent();
        Integer line = Integer.valueOf(jmmNode.get("lineStart"));
        Integer col = Integer.valueOf(jmmNode.get("colStart"));
        while(!parent.getKind().equals("MethodDeclaration")) { //import declaration??
            parent = parent.getJmmParent();
        }
        //THIS expression cannot be used in a static method
        if (parent.getKind().equals("MainMethod")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col,
                        "THIS can't be applied to static methods"));
        }

        return new Type(this.table.getClassName(), false);
    }

    private Type dealWithUnaryOp(JmmNode jmmNode, String s) {
        int line = Integer.valueOf(jmmNode.get("lineStart"));
        int col = Integer.valueOf(jmmNode.get("colStart"));

        Type right = visit(jmmNode.getJmmChild(0),"");

        if (!right.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Only boolean types can be used with the not operator"));
        }

        return new Type("boolean", false);
    }

    private Type dealWithCallMethod(JmmNode jmmNode, String s) {
        String methodName = jmmNode.get("name");
        Type classType = visit(jmmNode.getJmmChild(0),"");

        //The class calling the method is the current class ( the method is being called in the format: this.method() per example)
        if(classType.getName().equals(table.getClassName())){
            //verify if method exists
            if(table.getMethods().contains(methodName)){
                //verify type of arguments
                List<Symbol> params = table.getParameters(methodName);
                //check if number of parameters equals number of arguments
                if(params.size() != jmmNode.getNumChildren() -1){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Wrong number of parameters"));
                }
                else{
                    for (int i = 1; i < jmmNode.getNumChildren(); i++){
                        Type argType = visit(jmmNode.getJmmChild(i),"");
                        // (i-1) because arguments starts with i=1
                        if(!params.get(i - 1).getType().equals(argType)){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Type of " + jmmNode.getJmmChild(i).get("value") + " of the call is not compatible with the type in the method declaration"));
                        }
                    }
                }

            }
            //checks if current class extends a super class
            else if(table.getSuper() == null || !table.getImports().contains(table.getSuper())){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Method undeclared"));
            }
        }
        else{
            //checks if class is imported (assuming method is being called correctly) ( the method is being called in the format: otherClassName.method() per example)
            if(!table.getImports().contains(classType.getName())){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Class not imported"));
            }
        }
        //if method return null
        if(table.getReturnType(methodName) == null){
            //checks if class is imported, assume it is correct
            if(table.getImports().contains(classType.getName())){
                return new Type("CORRECT",false);
            }
            else {
                return new Type("ERROR", false);
            }
        }
        return table.getReturnType(methodName);
    }

    private Type dealWithIndex(JmmNode jmmNode, String s) {
        Type left = visit(jmmNode.getJmmChild(0), "");
        Type right = visit(jmmNode.getJmmChild(1), "");

        int lineL = Integer.valueOf(jmmNode.getJmmChild(0).get("lineStart"));
        int colL = Integer.valueOf(jmmNode.getJmmChild(0).get("colStart"));
        int lineR = Integer.valueOf(jmmNode.getJmmChild(1).get("lineStart"));
        int colR = Integer.valueOf(jmmNode.getJmmChild(1).get("colStart"));

        if(!left.isArray()){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineL, colL, "You cannot use indexing on a non-array variable"));
        }

        else if(!right.getName().equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineR, colR, "Index value must be of type int"));
        }

        return new Type("int", false);
    }

    private Type dealWithParenthesis(JmmNode jmmNode, String s) {

        return visit(jmmNode.getJmmChild(0),"");
    }

    private Type dealWithBinOp(JmmNode jmmNode, String s) {

        String op = jmmNode.get("op");
        Type left = visit(jmmNode.getJmmChild(0));
        Type right = visit(jmmNode.getJmmChild(1)); //ver sobre o metodo

        int lineLeft = Integer.valueOf(jmmNode.getJmmChild(0).get("lineStart"));
        int colLeft = Integer.valueOf(jmmNode.getJmmChild(0).get("colStart"));
        int lineRight = Integer.valueOf(jmmNode.getJmmChild(1).get("lineStart"));
        int colRight = Integer.valueOf(jmmNode.getJmmChild(1).get("colStart"));

        if(!left.getName().equals(right.getName())){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineRight, colRight, "Operands have different types"));
        }
        else if((left.isArray() || right.isArray()) && (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("<"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineLeft, colLeft,"Array cannot be used in arithmetic operations"));
        }
        else if(!right.getName().equals("int") && ( op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("<") ) ) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineLeft, colLeft, op + " operator expects two integers! Types not compatible"));
        }
        else if(!right.getName().equals("boolean") && op.equals("&&")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineLeft, lineLeft, op + " operator expects booleans. Types not compatible"));
        }
        else if( op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") ) {
            return new Type("int", false);
        }
        else if( op.equals("&&") || op.equals("<") ) {
            return new Type("boolean", false);
        }

        return new Type(right.getName(), right.isArray());
    }


}
