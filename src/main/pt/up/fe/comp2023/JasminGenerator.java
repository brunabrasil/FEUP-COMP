package pt.up.fe.comp2023;


import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Field;

public class JasminGenerator {

    private ClassUnit classUnit;

    public JasminGenerator(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public String dealWithClass() {
        StringBuilder stringBuilder = new StringBuilder("");

        // class declaration
        stringBuilder.append(".class ").append(classUnit.getClassName()).append("\n");

        // extends declaration
        stringBuilder.append(".super ").append(classUnit.getSuperClass()).append("\n");

        return stringBuilder.toString();
    }

}