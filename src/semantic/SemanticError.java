package semantic;

import lexer.token.Token;
import parser.ast.NodeBase;

public class SemanticError {

    private final Type type;
    private final NodeBase node;
    private final Token token;
    private final String details;

    public enum Type{
        // main
        NO_MAIN_FUNCTION("Main function is missing"),
        MULTIPLE_MAIN_FUNCTIONS("Multiple main functions declared"),

        // simboli
        UNDECLARED_SYMBOL("Use of undeclared symbol"),
        DUPLICATE_SYMBOL("Duplicate symbol in the same scope"),

        // kastovanje
        INVALID_CAST("Invalid type cast"),

        // type mismatch u assignovanju
        INVALID_ASSIGNMENT("Type mismatch in assignment"),

        // aritmetika
        ARITHMETIC_TYPE_MISMATCH("Arithmetic operands must be all integers or all reals"),

        // relacioni operatori
        RELATIONAL_OPERAND_NOT_NUMBER("Relational operators require numeric operands"),

        // logicki izrazi
        LOGICAL_OPERAND_NOT_BOOLEAN("Logical operators require boolean operands"),

        // if / while
        INVALID_CONDITION("Condition expression must be boolean"),

        // nizovi
        ARRAY_ELEMENT_TYPE_MISMATCH("Assigned value does not match array element type"),
        INVALID_INDEXING("Indexing is not supported for this type"),

        // funkcije
        INVALID_RETURN_TYPE("Return type does not match function return type"),
        INVALID_ARGUMENT_COUNT("Invalid number of arguments in function call"),
        INVALID_ARGUMENT_TYPE("Invalid argument type in function call"),
        INVALID_FUNCTION_USAGE("Symbol is not callable as a function"),
        INVALID_FUNCTION_RETURN_USAGE("Function return type does not match required expression type");

        private final String message;

        Type(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public SemanticError(Type type, NodeBase node, Token token, String details) {
        this.type = type;
        this.node = node;
        this.token = token;
        this.details = details;
    }

    public String format() {
        String location = token != null
                ? "(line " + token.line + ", col " + token.colStart + ")"
                : "(unknown location)";

        String msg = type.getMessage();
        if (details != null && !details.isEmpty()) {
            msg += ": " + details;
        }

        String header = "SEMANTIC ERROR [" + type.name() + "]";
        String underline = "-".repeat(header.length());

        return "\n" + underline + "\n"
                + header + "\n"
                + underline + "\n"
                + "Location : " + location + "\n"
                + "Message  : " + msg;
    }
}
