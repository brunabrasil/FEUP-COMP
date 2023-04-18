package pt.up.fe.comp2023;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;
public class OllirTemplates {

    public static String classTemplate(String classname,String extend){
        StringBuilder ollir = new StringBuilder();
        if (extend == null){
            ollir.append(classname).append(openBrackets());
        }
        else{
            ollir.append(String.format("%s extends %s",classname,extend)).append(openBrackets());
        }
        return ollir.toString();
    }

    public static String openBrackets() {
        return " {\n";
    }

    public static String closeBrackets() {
        return "\n}";
    }

    public static String fieldTemplate(Symbol variable) {
        return String.format(".field private %s;\n", variableTemplate(variable));
    }


    public static String variableTemplate(Symbol variable) {
        StringBuilder var = new StringBuilder(variable.getName());

        var.append(typeTemplate(variable.getType()));

        return var.toString();
    }


    public static String typeTemplate(Type type) {
        StringBuilder ollir = new StringBuilder();

        if (type.isArray()) ollir.append(".array");

        if (type.getName().equals("int")) {
            ollir.append(".i32");
        } else if (type.getName().equals("void")) {
            ollir.append(".V");
        } else if (type.getName().equals("boolean")) {
            ollir.append(".bool");
        } else {
            ollir.append(".").append(type.getName());
        }

        return ollir.toString();
    }

    public static String emptyConstructorTemplate(String className){
        StringBuilder ollir = new StringBuilder();
        ollir.append(".construct ").append(className).append("().V").append(openBrackets());
        ollir.append("invokespecial(this, \"<init>\").V;");
        ollir.append(closeBrackets());
        return ollir.toString();
    }


    public static String methodTemplate(String name, List<String> parameters, String returnType, boolean isStatic) {
        StringBuilder ollir = new StringBuilder(".method public ");

        if (isStatic) ollir.append("static ");

        // parameters
        ollir.append(name).append("(");
        ollir.append(String.join(", ", parameters));
        ollir.append(")");

        // return type
        ollir.append(returnType);

        ollir.append(openBrackets());

        return ollir.toString();
    }

    public static String putfieldTemplate(String variable, String value) {
        return String.format("putfield(this, %s, %s).V;", variable, value);
    }


    public static String getfieldTemplate(Symbol variable) {
        return String.format("getfield(this, %s)%s", variableTemplate(variable), typeTemplate(variable.getType()));
    }

    public static String invokestaticTemplate(String target, String method, Type returnType, String parameters) {
        if (parameters.equals(""))
            return String.format("invokestatic(%s, \"%s\")%s", target, method, typeTemplate(returnType));
        return String.format("invokestatic(%s, \"%s\", %s)%s", target, method, parameters, typeTemplate(returnType));
    }

    public static String invokevirtualTemplate(String var, String method, Type returnType, String parameters) {
        if (parameters.equals(""))
            return String.format("invokevirtual(%s, \"%s\")%s", var != null ? var : "this", method, typeTemplate(returnType));
        return String.format("invokevirtual(%s, \"%s\", %s)%s", var != null ? var : "this", method, parameters, typeTemplate(returnType));
    }

    public static String invokespecialTemplate(String var, String method, Type returnType, String parameters) {
        if (parameters.equals(""))
            return String.format("invokespecial(%s, \"%s\")%s", var != null ? var : "this", method, typeTemplate(returnType));
        return String.format("invokespecial(%s, \"%s\", %s)%s", var != null ? var : "this", method, parameters, typeTemplate(returnType));
    }

    public static String invokevirtualTemplate(String method, Type returnType, String parameters) {
        return invokevirtualTemplate(null, method, returnType, parameters);
    }


    public static String objectInitTemplate(String objectClass) {
        return String.format("new(%s).%s", objectClass, objectClass);
    }

    public static String objectInstanceTemplate(String name,String type) {
        return String.format("invokespecial(%s%s,\"<init>\").V;",name,type);
    }
}
