grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INT : [0-9]+ ;

ID : [a-zA-Z_][a-zA-Z_0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDeclaration)* classDeclaration EOF
    ;

importDeclaration
    : 'import' ID ('.' ID)* ';' #Import
    ;

classDeclaration
    : 'class' ID ( 'extends' ID )? '{' ( varDeclaration )* ( methodDeclaration )* '}' #Class
    ;

varDeclaration
    : type value=ID ';' #varDeclaration
    ;

methodDeclaration
    : ('public')? type name=ID '(' ( type parameter=ID ( ',' type parameter=ID )* )? ')'
      '{' ( varDeclaration )* ( statement )* 'return' expression ';' '}' #Method
    | ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' ID ')'
      '{' ( varDeclaration )* ( statement )* '}' #Method
    ;

type
    : 'int' '[' ']'
    | 'boolean'
    | 'int'
    | 'String'
    | ID
    ;
statement
    : '{' ( statement )* '}' #Stmt
    | 'if' '(' expression ')' statement 'else' statement #IfElseStmt
    | 'while' '(' expression ')' statement #WhileStmt
    | expression ';' #Expr
    | var=ID '=' expression ';' #Assignment
    | var=ID '[' expression ']' '=' expression ';' #Assignment
    ;

expression
    : '(' expression ')' #Parenthesis
    | expression '[' expression ']' #Index
    | expression '.' op='length' #Length
    | expression '.' ID '(' ( expression ( ',' expression )* )? ')' #CallMethod
    | op='!' expression #Unary
    | expression op=( '*' | '/' ) expression #BinaryOp
    | expression op=( '+' | '-' ) expression #BinaryOp
    | expression op='<' expression #BinaryOp
    | expression op='&&' expression #BinaryOp
    | 'new' 'int' '[' expression ']' #Instantiation
    | 'new' ID '(' ')' #Instantiation
    | value=INT #Integer
    | value='true' #Boolean
    | value='false' #Boolean
    | value=ID #Identifier
    | value='this' #This
    ;
