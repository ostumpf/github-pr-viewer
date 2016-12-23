lexer grammar LexerRules;

// *****************************************************************************
// *** LEXER                                                                 ***
// *****************************************************************************

fragment A_ :	'a' | 'A';
fragment B_ :	'b' | 'B';
fragment C_ :	'c' | 'C';
fragment D_ :	'd' | 'D';
fragment E_ :	'e' | 'E';
fragment F_ :	'f' | 'F';
fragment G_ :	'g' | 'G';
fragment H_ :	'h' | 'H';
fragment I_ :	'i' | 'I';
fragment J_ :	'j' | 'J';
fragment K_ :	'k' | 'K';
fragment L_ :	'l' | 'L';
fragment M_ :	'm' | 'M';
fragment N_ :	'n' | 'N';
fragment O_ :	'o' | 'O';
fragment P_ :	'p' | 'P';
fragment Q_ :	'q' | 'Q';
fragment R_ :	'r' | 'R';
fragment S_ :	's' | 'S';
fragment T_ :	't' | 'T';
fragment U_ :	'u' | 'U';
fragment V_ :	'v' | 'V';
fragment W_ :	'w' | 'W';
fragment X_ :	'x' | 'X';
fragment Y_ :	'y' | 'Y';
fragment Z_ :	'z' | 'Z';

WS                     : (' ' | '\t')+;
DIFF                   : D_ I_ F_ F_;
INDEX                  : I_ N_ D_ E_ X_;
NEW                    : N_ E_ W_;
FILE                   : F_ I_ L_ E_;
MODE                   : M_ O_ D_ E_;
DELETED                : D_ E_ L_ E_ T_ E_ D_;
MINUS3                 : '---';
PLUS3                  : '+++';
OPTION                 : '--' WORD;
DOTDOT                 : '..';
ATAT                   : '@@';
MINUS                  : '-';
PLUS                   : '+';
COMMA                  : ',';
SLASH                  : '/';
BACKSLASH              : '\\';
NEWLINE                : '\r'? '\n';
NUMBER                 : [0-9]+;
WORD                   : [a-zA-Z0-9]+;
ANYTHING               : . ;