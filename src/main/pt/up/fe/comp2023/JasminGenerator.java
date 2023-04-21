package pt.up.fe.comp2023;

import org.specs.comp.ollir.*;

import java.util.*;

public class JasminGenerator {
    private final ClassUnit classUnit;
    private int conditional;
    private int stack_counter;
    private int max_counter;

    public JasminGenerator(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public String dealWithClass() {
        StringBuilder stringBuilder = new StringBuilder();

        // class declaration
        stringBuilder.append(".class ").append(classUnit.getClassName()).append("\n");

        // extends declaration
        if (classUnit.getSuperClass() != null) {
            stringBuilder.append(".super ").append(classUnit.getSuperClass()).append("\n");
        }
        else {
            stringBuilder.append(".super java/lang/Object\n");
        }

        // fields declaration
        for (Field f : classUnit.getFields()) {
            stringBuilder.append(".field '").append(f.getFieldName()).append("' ").append(this.getStringType(f.getFieldType())).append("\n");
        }

        for (Method method : classUnit.getMethods()) {
            this.stack_counter = 0;
            this.max_counter = 0;

            stringBuilder.append(this.dealWithMethodHeader(method));
            String instructions = this.dealtWithMethodInstructions(method);
            if (!method.isConstructMethod()) {
                // stringBuilder.append(this.dealWithMethodLimits(method));
                stringBuilder.append(instructions);
            }
        }

        return stringBuilder.toString();
    }

    private String dealWithMethodHeader(Method method) {
        if (method.isConstructMethod()) {
            String classSuper;
            if (classUnit.getSuperClass() != null) {
                classSuper = classUnit.getSuperClass();
            }else{
                classSuper = "java/lang/Object";
            }

            return "\n.method public <init>()V\naload_0\ninvokespecial " + classSuper +  ".<init>()V\nreturn\n.end method\n";
        }

        StringBuilder stringBuilder = new StringBuilder("\n.method").append(" ").append(method.getMethodAccessModifier().name().toLowerCase()).append(" ");

        if (method.isStaticMethod()) {
            stringBuilder.append("static ");
        }
        else if (method.isFinalMethod()) {
            stringBuilder.append("final ");
        }

        // Parameters type
        stringBuilder.append(method.getMethodName()).append("(");
        for (Element element: method.getParams()) {
            stringBuilder.append(getStringType(element.getType()));
        }
        // Return type
        stringBuilder.append(")").append(this.getStringType(method.getReturnType())).append("\n");

        return stringBuilder.toString();
    }

    private String dealWithMethodLimits(Method method) {
        StringBuilder stringBuilder = new StringBuilder();

        int localCount = method.getVarTable().size();
        if (!method.isStaticMethod()) localCount++;
        stringBuilder.append(".limit locals ").append(localCount).append("\n");
        stringBuilder.append(".limit stack ").append(max_counter).append("\n");

        return stringBuilder.toString();
    }

    private String dealtWithMethodInstructions(Method method) {
        StringBuilder stringBuilder = new StringBuilder();
        method.getVarTable();
        for (Instruction instruction : method.getInstructions()) {
            stringBuilder.append(dealWithInstruction(instruction, method.getVarTable(), method.getLabels()));
            if (instruction instanceof CallInstruction && ((CallInstruction) instruction).getReturnType().getTypeOfElement() != ElementType.VOID) {
                stringBuilder.append("pop\n");
                this.decrementStackCounter(1);
            }
        }

        stringBuilder.append("\n.end method\n");
        return stringBuilder.toString();
    }

    private String dealWithInstruction(Instruction instruction, HashMap<String, Descriptor> varTable, HashMap<String, Instruction> methodLabels) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Instruction> entry : methodLabels.entrySet()) {
            if (entry.getValue().equals(instruction)){
                stringBuilder.append(entry.getKey()).append(":\n");
            }
        }

        return switch (instruction.getInstType()) {
            case ASSIGN ->
                    stringBuilder.append(dealWithAssignment((AssignInstruction) instruction, varTable)).toString();
            case NOPER ->
                    stringBuilder.append(dealWithSingleOpInstruction((SingleOpInstruction) instruction, varTable)).toString();
            case BINARYOPER ->
                    stringBuilder.append(dealWithBinaryOpInstruction((BinaryOpInstruction) instruction, varTable)).toString();
            case CALL ->
                    stringBuilder.append(dealWithCallInstruction((CallInstruction) instruction, varTable)).toString();
            case PUTFIELD ->
                    stringBuilder.append(dealWithPutFieldInstruction((PutFieldInstruction) instruction, varTable)).toString();
            case GETFIELD ->
                    stringBuilder.append(dealWithGetFieldInstruction((GetFieldInstruction) instruction, varTable)).toString();
            case RETURN ->
                    stringBuilder.append(dealWithReturnInstruction((ReturnInstruction) instruction, varTable)).toString();
            default -> "Error in Instructions";
        };
    }

    private String dealWithAssignment(AssignInstruction inst, HashMap<String, Descriptor> varTable) {
        String stringBuilder = "";
        Operand operand = (Operand) inst.getDest();
        if (operand instanceof ArrayOperand) {
            ArrayOperand aoperand = (ArrayOperand) operand;

            // Load array
            stringBuilder += String.format("aload%s\n", this.getVirtualReg(aoperand.getName(), varTable));
            this.incrementStackCounter(1);

            // Load index
            stringBuilder += loadElement(aoperand.getIndexOperands().get(0), varTable);
        }

        stringBuilder += dealWithInstruction(inst.getRhs(), varTable, new HashMap<>());
        if(!(operand.getType().getTypeOfElement().equals(ElementType.OBJECTREF) && inst.getRhs() instanceof CallInstruction)) { //if it's a new object call does not store yet
            stringBuilder += this.storeElement(operand, varTable);
        }

        return stringBuilder;
    }

    private String dealWithSingleOpInstruction(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        return loadElement(instruction.getSingleOperand(), varTable);
    }

    private String dealWithBinaryOpInstruction(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        return switch (instruction.getOperation().getOpType()) {
            case ADD, SUB, MUL, DIV -> this.dealWithIntOperation(instruction, varTable);
            default -> "Error in BinaryOpInstruction";
        };
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

    private String dealWithCallInstruction(CallInstruction instruction, HashMap<String, Descriptor> varTable) {
        String stringBuilder = "";
        CallType callType = instruction.getInvocationType();

        switch (callType) {
            case invokespecial:
                stringBuilder += this.dealWithInvoke(instruction, varTable, callType, ((ClassType)instruction.getFirstArg().getType()).getName());
                break;
            case invokevirtual:
                stringBuilder += this.dealWithInvoke(instruction, varTable, callType, ((ClassType)instruction.getFirstArg().getType()).getName());
                break;
            case invokestatic:
                stringBuilder += this.dealWithInvoke(instruction, varTable, callType, ((Operand)instruction.getFirstArg()).getName());
                break;
            case NEW:
                stringBuilder += this.dealWithNewObject(instruction);
                break;
            default:
                return "Erro in CallInstruction";
        }

        return stringBuilder;
    }

    private String dealWithInvoke(CallInstruction instruction, HashMap<String, Descriptor> varTable, CallType callType, String className){
        StringBuilder stringBuilder = new StringBuilder();

        String functionLiteral = ((LiteralElement) instruction.getSecondArg()).getLiteral();
        StringBuilder parameters = new StringBuilder();

        if (!functionLiteral.equals("\"<init>\"")) {  //does not load element because its a new object, its already done in dealWithNewObject with new and dup
            stringBuilder.append(this.loadElement(instruction.getFirstArg(), varTable));
        }

        int num_params = 0;
        for (Element element : instruction.getListOfOperands()) {
            stringBuilder.append(this.loadElement(element, varTable));
            parameters.append(this.getStringType(element.getType()));
            num_params++;
        }

        if (!instruction.getInvocationType().equals(CallType.invokestatic)) {
            num_params += 1;
        }
        this.decrementStackCounter(num_params);
        if (instruction.getReturnType().getTypeOfElement() != ElementType.VOID) {
            this.incrementStackCounter(1);
        }

        stringBuilder.append(callType.name()).append(" ").append(this.getObjectClassName(className)).append(".").append(functionLiteral.replace("\"", "")).append("(").append(parameters).append(")").append(this.getStringType(instruction.getReturnType())).append("\n");

        if (functionLiteral.equals("\"<init>\"") && !className.equals("this")) {
            stringBuilder.append(this.storeElement((Operand) instruction.getFirstArg(), varTable));
        }

        return stringBuilder.toString();
    }

    private String dealWithNewObject(CallInstruction instruction){
        Element e = instruction.getFirstArg();
        String stringBuilder = "";

        if (e.getType().getTypeOfElement().equals(ElementType.OBJECTREF)){
            this.incrementStackCounter(2);

            stringBuilder += "new " + this.getObjectClassName(((Operand)e).getName()) + "\ndup\n";
        }

        return stringBuilder;
    }

    private String dealWithPutFieldInstruction(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        String stringBuilder = "";
        Operand obj = (Operand)instruction.getFirstOperand();
        Operand var = (Operand)instruction.getSecondOperand();
        Element value = instruction.getThirdOperand();

        stringBuilder += this.loadElement(obj, varTable); //push object (Class ref) onto the stack

        stringBuilder += this.loadElement(value, varTable); //store const element on stack

        this.decrementStackCounter(2);

        return stringBuilder + "putfield " + classUnit.getClassName() + "/" + var.getName() + " " + getStringType(var.getType()) + "\n";
    }

    private String dealWithGetFieldInstruction(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        String jasminCode = "";
        Operand obj = (Operand)instruction.getFirstOperand();
        Operand var = (Operand)instruction.getSecondOperand();

        jasminCode += this.loadElement(obj, varTable); //push object (Class ref) onto the stack

        return jasminCode + "getfield " + classUnit.getClassName() + "/" + var.getName() + " " + getStringType(var.getType()) +  "\n";
    }

    private String dealWithReturnInstruction(ReturnInstruction instruction, HashMap<String, Descriptor> varTable) {
        if(!instruction.hasReturnValue()) return "return";
        String returnString = "";

        switch (instruction.getOperand().getType().getTypeOfElement()) {
            case VOID -> returnString = "return";
            case INT32, BOOLEAN -> {
                returnString = loadElement(instruction.getOperand(), varTable);

                this.decrementStackCounter(1);
                returnString += "ireturn";
            }
            case OBJECTREF -> {
                returnString = loadElement(instruction.getOperand(), varTable);

                this.decrementStackCounter(1);
                returnString += "areturn";
            }
            default -> {
            }
        }

        return returnString;
    }

    private String getStringType(Type type) {
        ElementType elementType = type.getTypeOfElement();
        String stringBuilder = "";

        if (elementType == ElementType.ARRAYREF) {
            elementType = ((ArrayType) type).getTypeOfElements();
            stringBuilder += "[";
        }

        switch (elementType) {
            case INT32 -> {
                return stringBuilder + "I";
            }
            case BOOLEAN -> {
                return stringBuilder + "Z";
            }
            case STRING -> {
                return stringBuilder + "Ljava/lang/String;";
            }
            case OBJECTREF -> {
                String className = ((ClassType) type).getName();
                return stringBuilder + "L" + this.getObjectClassName(className) + ";";
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

    private String getObjectClassName(String className) {
        for (String _import : classUnit.getImports()) {
            if (_import.endsWith("." + className)) {
                return _import.replaceAll("\\.", "/");
            }
        }
        return className;
    }

    private String loadElement(Element element, HashMap<String, Descriptor> varTable) {
        if (element instanceof LiteralElement) {
            String num = ((LiteralElement) element).getLiteral();
            this.incrementStackCounter(1);
            return this.selectConstType(num) + "\n";
        }
        else if (element instanceof ArrayOperand operand) {

            // Load array
            String stringBuilder = String.format("aload%s\n", this.getVirtualReg(operand.getName(), varTable));
            this.incrementStackCounter(1);

            // Load index
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
                case OBJECTREF -> {
                    this.incrementStackCounter(1);
                    return String.format("aload%s\n", this.getVirtualReg(operand.getName(), varTable));
                }
                case CLASS -> {
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
            case OBJECTREF -> {
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
        if (virtualReg > 3) {
            return " " + virtualReg;
        }
        return "_" + virtualReg;
    }

    private String getTrueLabel() {
        return "myTrue" + this.conditional;
    }

    private String getEndIfLabel() {
        return "myEndIf" + this.conditional;
    }

    private String selectConstType(String literal){
        return Integer.parseInt(literal) < -1 || Integer.parseInt(literal) > 5 ?
                Integer.parseInt(literal) < -128 || Integer.parseInt(literal) > 127 ?
                        Integer.parseInt(literal) < -32768 || Integer.parseInt(literal) > 32767 ?
                                "ldc " + literal :
                                "sipush " + literal :
                        "bipush " + literal :
                "iconst_" + literal;
    }

    private void incrementStackCounter(int add) {
        this.stack_counter += add;
        if (this.stack_counter > this.max_counter) this.max_counter = stack_counter;
    }

    private void decrementStackCounter(int sub) {
        this.stack_counter -= sub;
    }
}