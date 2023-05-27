package pt.up.fe.comp2023;

import org.specs.comp.ollir.*;

import java.util.HashMap;
import java.util.Map;

public class JasminGenerator {

    private int conditional;
    private int stackCounter;
    private int maxCounter;
    private ClassUnit classUnit;

    public JasminGenerator(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public String dealWithClass() {
        StringBuilder strBuilder = new StringBuilder();

        // Declaration of the class
        strBuilder.append(".class ");
        strBuilder.append(classUnit.getClassName()).append("\n");

        // Declaration of the extends
        if (classUnit.getSuperClass() != null) {
            strBuilder.append(".super ");
            strBuilder.append(classUnit.getSuperClass()).append("\n");
        }
        else {
            strBuilder.append(".super java/lang/Object\n");
        }

        // Declaration of the fields
        for (Field fi: classUnit.getFields()) {
            strBuilder.append(".field ");
            strBuilder.append(fi.getFieldName()).append(" ");
            strBuilder.append(this.convertType(fi.getFieldType())).append("\n");
        }

        // Declaration of the methods
        for (Method m : classUnit.getMethods()) {
            this.stackCounter = 0;
            this.maxCounter = 0;

            strBuilder.append(this.addMethodHeader(m));
            String instructions = this.addMethodInstructions(m);

            if (!m.isConstructMethod()) {
                strBuilder.append(this.dealWithMethodLimits(m));
                strBuilder.append(instructions);
            }
        }

        return strBuilder.toString();
    }

    private String convertType(Type type) {
        ElementType elementType = type.getTypeOfElement();
        String strBuilder = "";

        if (elementType == ElementType.ARRAYREF) {
            elementType = ((ArrayType) type).getTypeOfElements();
            strBuilder += "[";
        }

        switch (elementType) {
            case INT32 -> {
                return strBuilder + "I";
            }
            case BOOLEAN -> {
                return strBuilder + "Z";
            }
            case STRING -> {
                return strBuilder + "Ljava/lang/String;";
            }
            case OBJECTREF -> {
                String className = ((ClassType) type).getName();
                return strBuilder + "L" + this.getOjectClassName(className) + ";";
            }
            case CLASS -> {
                return "CLASS";
            }
            case VOID -> {
                return "V";
            }
            default -> {
                return "Error converting ElementType";
            }
        }
    }

    private String getOjectClassName(String className) {
        for (String i : classUnit.getImports()) {
            if (i.endsWith("." + className)) {
                return i.replaceAll("\\.", "/");
            }
        }
        return className;
    }

    private String addMethodHeader(Method method) {
        if (method.isConstructMethod()) {
            String classSuper = "java/lang/Object";
            if (classUnit.getSuperClass() != null) {
                classSuper = classUnit.getSuperClass();
            }

            return "\n.method public <init>()V\naload_0\ninvokespecial " + classSuper +  ".<init>()V\nreturn\n.end method\n";
        }

        StringBuilder strBuilder = new StringBuilder("\n.method").append(" ").append(method.getMethodAccessModifier().name().toLowerCase()).append(" ");

        if (method.isFinalMethod()) {
            strBuilder.append("final ");
        }
        else if (method.isStaticMethod()) {
            strBuilder.append("static ");
        }

        // Parameters type
        strBuilder.append(method.getMethodName());

        strBuilder.append("(");
        for (Element el: method.getParams()) {
            strBuilder.append(convertType(el.getType()));
        }
        strBuilder.append(")");

        // Return type
        strBuilder.append(this.convertType(method.getReturnType()));
        strBuilder.append("\n");

        return strBuilder.toString();
    }

    private String addMethodInstructions(Method method) {
        StringBuilder strBuilder = new StringBuilder();
        method.getVarTable();

        for (Instruction i : method.getInstructions()) {
            strBuilder.append(dealWithInstruction(i, method.getVarTable(), method.getLabels()));
            if (i instanceof CallInstruction && ((CallInstruction) i).getReturnType().getTypeOfElement() != ElementType.VOID) {
                strBuilder.append("pop\n");
                this.decrementStackCounter(1);
            }
        }

        strBuilder.append("\n.end method\n");
        return strBuilder.toString();
    }

    private String dealWithInstruction(Instruction instruction, HashMap<String, Descriptor> varTable, HashMap<String, Instruction> methodLabels) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Instruction> entry : methodLabels.entrySet()) {
            if (entry.getValue().equals(instruction)){
                stringBuilder.append(entry.getKey()).append(":\n");
            }
        }

        InstructionType inst = instruction.getInstType();

        return switch (inst) {
            case ASSIGN -> stringBuilder.append(dealWithASSIGN((AssignInstruction) instruction, varTable)).toString();
            case CALL -> stringBuilder.append(dealWithCALL((CallInstruction) instruction, varTable)).toString();
            case RETURN -> stringBuilder.append(dealWithRETURN((ReturnInstruction) instruction, varTable)).toString();
            case PUTFIELD ->
                    stringBuilder.append(dealWithPUTFIELD((PutFieldInstruction) instruction, varTable)).toString();
            case GETFIELD ->
                    stringBuilder.append(dealWithGETFIELD((GetFieldInstruction) instruction, varTable)).toString();
            case BINARYOPER  ->
                    stringBuilder.append(dealWithBINARYOPER((BinaryOpInstruction) instruction, varTable)).toString();
            case NOPER -> stringBuilder.append(loadElement(((SingleOpInstruction) instruction).getSingleOperand(), varTable)).toString();
            case BRANCH->
                    stringBuilder.append(dealWithBRANCH((CondBranchInstruction) instruction, varTable)).toString();
            case GOTO ->
                    stringBuilder.append(dealWithGOTO((GotoInstruction) instruction, varTable)).toString();
            default -> "Error";
        };
    }
    private String dealWithBRANCH(CondBranchInstruction instruction, HashMap<String, Descriptor> varTable) {

        String stringBuilder = this.loadElement(instruction.getOperands().get(0), varTable) +
                "ifeq " +
                instruction.getLabel() +
                "\n";
        
        this.decrementStackCounter(1);

        return stringBuilder;
    }
    private String dealWithGOTO(GotoInstruction instruction, HashMap<String, Descriptor> varTable) {
        return String.format("goto %s\n", instruction.getLabel());
    }
    private String dealWithASSIGN(AssignInstruction instruction, HashMap<String, Descriptor> varTable) {
        String stringBuilder = "";
        Operand operand = (Operand) instruction.getDest();

        if (operand instanceof ArrayOperand) {
            ArrayOperand aoperand = (ArrayOperand) operand;

            stringBuilder += String.format("aload%s\n", this.getVirtualReg(aoperand.getName(), varTable));
            this.incrementStackCounter(1);

            stringBuilder += loadElement(aoperand.getIndexOperands().get(0), varTable);
        }

        stringBuilder += dealWithInstruction(instruction.getRhs(), varTable, new HashMap<String, Instruction>());

        // If it is an object reference we should not update the table
        if(!(operand.getType().getTypeOfElement().equals(ElementType.OBJECTREF) && instruction.getRhs() instanceof CallInstruction)) {
            stringBuilder += this.storeElement(operand, varTable);
        }

        return stringBuilder;
    }

    private String dealWithCALL(CallInstruction instruction, HashMap<String, Descriptor> varTable) {
        String stringBuilder = "";
        CallType callType = instruction.getInvocationType();

        switch (callType) {
            case invokespecial, invokevirtual ->
                    stringBuilder += this.dealWithInvoke(instruction, varTable, callType, ((ClassType) instruction.getFirstArg().getType()).getName());
            case invokestatic ->
                    stringBuilder += this.dealWithInvoke(instruction, varTable, callType, ((Operand) instruction.getFirstArg()).getName());
            case arraylength -> {
                    stringBuilder += this.loadElement(instruction.getFirstArg(), varTable);
                    stringBuilder += "arraylength\n";
            }
            case NEW ->
                    stringBuilder += this.dealWithNewObject(instruction, varTable);
            default -> {
                return "Error in CallInstruction";
            }
        }

        return stringBuilder;
    }

    private String dealWithRETURN(ReturnInstruction instruction, HashMap<String, Descriptor> varTable) {
        if(!instruction.hasReturnValue()) return "return";
        String returnString = "";

        switch (instruction.getOperand().getType().getTypeOfElement()) {
            case VOID -> returnString = "return";
            case INT32, BOOLEAN -> {
                returnString = loadElement(instruction.getOperand(), varTable);

                this.decrementStackCounter(1);
                returnString += "ireturn";
            }
            case ARRAYREF, OBJECTREF -> {
                returnString = loadElement(instruction.getOperand(), varTable);

                this.decrementStackCounter(1);
                returnString += "areturn";
            }
            default -> {
            }
        }

        return returnString;
    }

    private String dealWithPUTFIELD(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        String stringBuilder = "";
        Operand obj = (Operand)instruction.getFirstOperand();
        Operand var = (Operand)instruction.getSecondOperand();
        Element value = instruction.getThirdOperand();

        stringBuilder += this.loadElement(obj, varTable); //push object (Class ref) onto the stack

        stringBuilder += this.loadElement(value, varTable); //store const element on stack

        this.decrementStackCounter(2);
        return stringBuilder + "putfield " + classUnit.getClassName() + "/" + var.getName() + " " + convertType(var.getType()) + "\n";
    }

    private String dealWithGETFIELD(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        String jasminCode = "";
        Operand obj = (Operand)instruction.getFirstOperand();
        Operand var = (Operand)instruction.getSecondOperand();

        return jasminCode + this.loadElement(obj, varTable) + "getfield " + classUnit.getClassName() + "/" + var.getName() + " " + convertType(var.getType()) +  "\n";
    }


    private String dealWithBINARYOPER(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        return switch (instruction.getOperation().getOpType()) {
            case ADD, SUB, MUL, DIV -> this.dealWithIntOperation(instruction, varTable);
            case LTH, GTE, ANDB, NOTB -> this.dealWithBooleanOperation(instruction, varTable);
            default -> "Error in BinaryOpInstruction";
        };
    }

    private String dealWithNewObject(CallInstruction instruction, HashMap<String, Descriptor> varTable){
        Element e = instruction.getFirstArg();
        String stringBuilder = "";

        if (e.getType().getTypeOfElement().equals(ElementType.ARRAYREF)) {
            stringBuilder += this.loadElement(instruction.getListOfOperands().get(0), varTable);

            stringBuilder += "newarray int\n";
        }
        else if (e.getType().getTypeOfElement().equals(ElementType.OBJECTREF)){
            this.incrementStackCounter(2);

            stringBuilder += "new " + this.getOjectClassName(((Operand)e).getName()) + "\ndup\n";
        }

        return stringBuilder;
    }

    private String dealWithIntOperation(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        String leftOperand = loadElement(instruction.getLeftOperand(), varTable);
        String rightOperand = loadElement(instruction.getRightOperand(), varTable);
        String operation;

        switch (instruction.getOperation().getOpType()) {
            case ADD -> operation = "iadd\n";
            case SUB -> operation = "isub\n";
            case MUL -> operation = "imul\n";
            case DIV -> operation = "idiv\n";
            default -> {
                return "Error in IntOperation\n";
            }
        }

        this.decrementStackCounter(1);
        return leftOperand + rightOperand + operation;
    }

    private String dealWithBooleanOperation(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        OperationType ot = instruction.getOperation().getOpType();
        StringBuilder stringBuilder = new StringBuilder();

        switch (instruction.getOperation().getOpType()) {
            case LTH, GTE -> {
                String leftOperand = loadElement(instruction.getLeftOperand(), varTable);
                String rightOperand = loadElement(instruction.getRightOperand(), varTable);

                stringBuilder.append(leftOperand)
                        .append(rightOperand)
                        .append(this.dealWithRelationalOperation(ot, this.getTrueLabel()))
                        .append("iconst_1\n")
                        .append("goto ").append(this.getEndIfLabel()).append("\n")
                        .append(this.getTrueLabel()).append(":\n")
                        .append("iconst_0\n")
                        .append(this.getEndIfLabel()).append(":\n");

                this.decrementStackCounter(1);
            }
            case ANDB -> {
                String ifeq = "ifeq " + this.getTrueLabel() + "\n";

                // Compare left operand
                stringBuilder.append(loadElement(instruction.getLeftOperand(), varTable)).append(ifeq);
                stringBuilder.append(loadElement(instruction.getRightOperand(), varTable)).append(ifeq);

                stringBuilder.append("iconst_1\n")
                        .append("goto ").append(this.getEndIfLabel()).append("\n")
                        .append(this.getTrueLabel()).append(":\n")
                        .append("iconst_0\n")
                        .append(this.getEndIfLabel()).append(":\n");

                this.decrementStackCounter(1);
            }
            case NOTB -> {
                String operand = loadElement(instruction.getLeftOperand(), varTable);

                stringBuilder.append(operand)
                        .append("ifne ").append(this.getTrueLabel()).append("\n")
                        .append("iconst_1\n")
                        .append("goto ").append(this.getEndIfLabel()).append("\n")
                        .append(this.getTrueLabel()).append(":\n")
                        .append("iconst_0\n")
                        .append(this.getEndIfLabel()).append(":\n");
            }
            default -> {
                return "Error in BooleansOperations\n";
            }
        }

        this.conditional++;
        return stringBuilder.toString();
    }

    private String getTrueLabel() {
        return "myTrue" + this.conditional;
    }

    private String getEndIfLabel() {
        return "myEndIf" + this.conditional;
    }
    private String dealWithRelationalOperation(OperationType opType, String tLabel) {
        return switch (opType) {
            case LTH -> String.format("if_icmpge %s\n", tLabel);
            case GTE -> String.format("if_icmplt %s\n", tLabel);
            default -> "Error in RelationalOperations\n";
        };
    }
    private String dealWithInvoke(CallInstruction instruction, HashMap<String, Descriptor> varTable, CallType callType, String className){
        StringBuilder stringBuilder = new StringBuilder();

        String functionLiteral = ((LiteralElement) instruction.getSecondArg()).getLiteral();
        StringBuilder parameters = new StringBuilder();

        if (!functionLiteral.equals("\"<init>\"")) {  //does not load element because its a new object, its already done in dealWithNewObject with new and dup
            stringBuilder.append(this.loadElement(instruction.getFirstArg(), varTable));
        }

        int numParams = 0;
        for (Element element : instruction.getListOfOperands()) {
            stringBuilder.append(this.loadElement(element, varTable));
            parameters.append(this.convertType(element.getType()));
            numParams++;
        }

        if (!instruction.getInvocationType().equals(CallType.invokestatic)) {
            numParams += 1;
        }
        this.decrementStackCounter(numParams);
        if (instruction.getReturnType().getTypeOfElement() != ElementType.VOID) {
            this.incrementStackCounter(1);
        }

        stringBuilder.append(callType.name()).append(" ").append(this.getOjectClassName(className)).append(".").append(functionLiteral.replace("\"", "")).append("(").append(parameters).append(")").append(this.convertType(instruction.getReturnType())).append("\n");

        if (functionLiteral.equals("\"<init>\"") && !className.equals("this")) {
            stringBuilder.append(this.storeElement((Operand) instruction.getFirstArg(), varTable));
        }

        return stringBuilder.toString();
    }

    private String loadElement(Element element, HashMap<String, Descriptor> varTable) {
        if (element instanceof LiteralElement) {
            String num = ((LiteralElement) element).getLiteral();
            this.incrementStackCounter(1);

            return (Integer.parseInt(num) < -1 || Integer.parseInt(num) > 5 ?
                    Integer.parseInt(num) < -128 || Integer.parseInt(num) > 127 ?
                            Integer.parseInt(num) < -32768 || Integer.parseInt(num) > 32767 ?
                                    "ldc " + num :
                                    "sipush " + num :
                            "bipush " + num :
                    "iconst_" + num) + "\n";
        }
        else if (element instanceof ArrayOperand operand) {
            String stringBuilder = String.format("aload%s\n", this.getVirtualReg(operand.getName(), varTable));
            this.incrementStackCounter(1);

            stringBuilder += loadElement(operand.getIndexOperands().get(0), varTable);
            this.decrementStackCounter(1);

            return stringBuilder + "iaload\n";
        }
        else if (element instanceof Operand operand) {
            switch (operand.getType().getTypeOfElement()) {
                case THIS -> {
                    this.incrementStackCounter(1);
                    return "aload_0\n";
                }
                case INT32, BOOLEAN -> {
                    this.incrementStackCounter(1);
                    return String.format("iload%s\n", this.getVirtualReg(operand.getName(), varTable));
                }
                case ARRAYREF, OBJECTREF -> {
                    this.incrementStackCounter(1);
                    return String.format("aload%s\n", this.getVirtualReg(operand.getName(), varTable));
                }
                case CLASS -> { //TODO deal with class
                    return "";
                }
                default -> {
                    return "Error in operand loadElements\n";
                }
            }
        }
        System.out.println(element);
        return "Error in loadElements\n";
    }

    private String storeElement(Operand operand, HashMap<String, Descriptor> varTable) {
        if (operand instanceof ArrayOperand) {
            this.decrementStackCounter(3);
            return "iastore\n";
        }

        switch (operand.getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> {
                this.decrementStackCounter(1);
                return String.format("istore%s\n", this.getVirtualReg(operand.getName(), varTable));
            }
            case ARRAYREF, OBJECTREF -> {
                this.decrementStackCounter(1);
                return String.format("astore%s\n", this.getVirtualReg(operand.getName(), varTable));
            }
            default -> {
                return "Error in storeElements";
            }
        }
    }

    private String getVirtualReg(String varName, HashMap<String, Descriptor> varTable) {
        int virtualReg = varTable.get(varName).getVirtualReg();
        if (virtualReg <= 3) {
            return "_" + virtualReg;
        } else {
            return " " + virtualReg;
        }
    }

    private void incrementStackCounter(int add) {
        this.stackCounter += add;
        if (this.stackCounter > this.maxCounter) this.maxCounter = stackCounter;
    }

    private void decrementStackCounter(int sub) {
        this.stackCounter -= sub;
    }

    private String dealWithMethodLimits(Method method) {
        StringBuilder stringBuilder = new StringBuilder();

        int localCount = method.getVarTable().size();
        if (!method.isStaticMethod()) localCount++;
        stringBuilder.append(".limit locals ").append(localCount).append("\n");
        stringBuilder.append(".limit stack ").append(maxCounter).append("\n");

        return stringBuilder.toString();
    }
}
