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
    private List<Symbol> fields;
    private List<Symbol> templocalvariables;
    private Map<String,Type> methods=new HashMap<>();
    private List<Symbol> parameters;
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
        List<String> methods;

        return methods;
    }

    @Override
    public Type getReturnType(String s) {
        return null;
    }


    public void addMethodParameters(String methodname,List<Symbol> params){
        this.methodParameters.put(methodname,params);
    }
    @Override
    public List<Symbol> getParameters(String s) {
        return methodParameters.get(s);
    }


    public void addTempLocalVariable(Symbol var){
        this.templocalvariables.add(var);
    }
    public void addLocalVariables(String methodname){
        this.localVariables.put(methodname,templocalvariables);
        this.templocalvariables.clear();
    }
    @Override
    public List<Symbol> getLocalVariables(String s) {
        return localVariables.get(s);
    }



}
