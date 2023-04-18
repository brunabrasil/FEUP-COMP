package pt.up.fe.comp2023;


import org.specs.comp.ollir.*;

public class JasminGenerator {

    private ClassUnit classUnit;

    public JasminGenerator(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public String dealWithClass() {
        StringBuilder stringBuilder = new StringBuilder("");

        // Declaration of the class
        stringBuilder.append(".class ").append(classUnit.getClassName()).append("\n");

        // Declaration of the extends
        if (classUnit.getSuperClass() != null) {
            stringBuilder.append(".super ").append(classUnit.getSuperClass()).append("\n");
        }
        else {
            stringBuilder.append(".super java/lang/Object\n");
        }

        // Declaration of the fields
        for (Field fi: classUnit.getFields()) {
            stringBuilder.append(".field ").append(fi.getFieldName()).append("' ").append(this.convertType(fi.getFieldType())).append("\n");
        }

        return stringBuilder.toString();
    }

    private String convertType(Type type) {
        ElementType elementType = type.getTypeOfElement();
        String stringBuilder = "";

        if (elementType == ElementType.ARRAYREF) {
            elementType = ((ArrayType) type).getTypeOfElements();
            stringBuilder += "[";
        }

        switch (elementType) {
            case INT32:
                return stringBuilder + "I";
            case BOOLEAN:
                return stringBuilder + "Z";
            case STRING:
                return stringBuilder + "Ljava/lang/String;";
            case OBJECTREF:
                String className = ((ClassType) type).getName();
                return stringBuilder + "L" + this.getOjectClassName(className) + ";";
            case CLASS:
                className = ((ClassType) type).getName();
                return "L" + this.getOjectClassName(className) + ";";
            case VOID:
                return "V";
            default:
                return "Error converting ElementType";
        }
    }

    private String getOjectClassName(String className) {
        for (String _import : classUnit.getImports()) {
            if (_import.endsWith("." + className)) {
                return _import.replaceAll("\\.", "/");
            }
        }
        return className;


    }
}