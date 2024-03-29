grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INT : [0] | [1-9][0-9]* ;

ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;

COMMENT : ('/*' .*? '*/'| '//' ~[\r\n]*) -> skip;

program
    : (importDeclaration)* classDeclaration EOF
    ;

importDeclaration
    : 'import' importName=ID ('.' vars+=ID)* ';' #Import
    ;

classDeclaration
    : 'class' className=ID ( 'extends' extendsName=ID )? '{' ( varDeclaration )* ( methodDeclaration )* '}' #Class
    ;

varDeclaration
    : type varName=ID ';' #Declaration
    ;

methodDeclaration
    : ('public')? type methodName=ID '(' ( type parameters+=ID ( ',' type parameters+=ID )* )? ')'
      '{' ( varDeclaration )* ( statement )* 'return' expression ';' '}' #NormalMethod
    | ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' parameter=ID ')'
      '{' ( varDeclaration )* ( statement )* '}' #MainMethod
    ;

type locals [boolean isArray = false]
    : name='int' ('['']'{$isArray = true;})?
    | name='boolean'
    | name='int'
    | name='String'
    | name=ID
    ;
statement
    : '{' ( statement )* '}' #Stmt
    | 'if' '(' expression ')' statement 'else' statement #IfElseStmt
    | 'while' '(' expression ')' statement #WhileStmt
    | expression ';' #Expr
    | var=ID '=' expression ';' #Assignment
    | var=ID '[' expression ']' '=' expression ';' #AssignmentArray
    ;

expression
    : '(' expression ')' #Parenthesis
    | expression '[' expression ']' #Indexing
    | expression '.' op='length' #Length
    | expression '.' name=ID '(' ( expression ( ',' expression )* )? ')' #CallMethod
    | op='!' expression #UnaryOp
    | expression op=( '*' | '/' ) expression #BinaryOp
    | expression op=( '+' | '-' ) expression #BinaryOp
    | expression op='<' expression #BinaryOp
    | expression op='&&' expression #BinaryOp
    | 'new' 'int' '[' expression ']' #NewIntArray
    | 'new' name=ID '(' ')' #NewObject
    | value=INT #Integer
    | value='true' #Boolean
    | value='false' #Boolean
    | value=ID #Identifier
    | value='this' #This
    ;
