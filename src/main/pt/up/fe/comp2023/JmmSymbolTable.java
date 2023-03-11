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
    private List<String> methods;
    private List<Symbol> parameters;
    private List<Symbol> localVariables;


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

    @Override
    public List<String> getMethods() {
        return methods;
    }

    @Override
    public Type getReturnType(String s) {
        return null;
    }

    @Override
    public List<Symbol> getParameters(String s) {
        return parameters;
    }

    public void addLocalVariable(Symbol variable){
        this.localVariables.add(variable);
    }
    @Override
    public List<Symbol> getLocalVariables(String s) {
        return localVariables;
    }



}
