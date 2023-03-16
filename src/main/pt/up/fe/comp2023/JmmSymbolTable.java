package pt.up.fe.comp2023;


import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.parser.JmmParserResult;

import java.util.*;

public class JmmSymbolTable implements SymbolTable {
    private final List<String> imports = new ArrayList<>();
    private String className;
    private String superName;
    private List<Symbol> fields =new ArrayList<>();
    private List<Symbol> templocalvariables=new ArrayList<>();
    private Map<String,Type> methods=new HashMap<>();
    private List<Symbol> parameters=new ArrayList<>();
   // private List<Symbol> localVariables;
    private Map<String,List<Symbol>> methodParameters=new HashMap<>();
    private Map<String,List<Symbol>> localVariables=new HashMap<>(); // Map<MethodName,Variables>

    public void addImport(String importState){
        imports.add(importState);
    }
    @Override
    public List<String> getImports() {
        return imports;
    }

    public void setClassName(String name){
        this.className=name;
    }
    @Override
    public String getClassName() {
        return className;
    }

    public void setSuperName(String name){
        this.superName=name;
    }
    @Override
    public String getSuper() {
        return superName;
    }

    public void addField(Symbol field){
        this.fields.add(field);
    }
    @Override
    public List<Symbol> getFields() {
        return fields;
    }


    public void addMethod(String methodname,Type methodtype){
        this.methods.put(methodname,methodtype);
    }

    @Override
    public List<String> getMethods() {
         List<String> methodsList=new ArrayList<>();
        for ( String method : methods.keySet() ) {
            methodsList.add(method);
        }

        return methodsList;
    }

    @Override
    public Type getReturnType(String s) {
        return this.methods.get(s);
    }


    public void addMethodParameters(String methodname,List<Symbol> params){
        this.methodParameters.put(methodname,params);
    }
    @Override
    public List<Symbol> getParameters(String s) {
        return this.methodParameters.get(s);
    }

    public void addTempLocalVariable(Symbol var){
        //System.out.println("IN TEMP LOCAL VARIABLES="+var);
        this.templocalvariables.add(var);
    }

    public void addLocalVariables(String methodname){
        List<Symbol> temp=new ArrayList<>();
        for(var i : this.templocalvariables){
            temp.add(i);
        }
        this.localVariables.put(methodname,temp);
        this.templocalvariables.clear();
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        return this.localVariables.get(s);
    }



}
