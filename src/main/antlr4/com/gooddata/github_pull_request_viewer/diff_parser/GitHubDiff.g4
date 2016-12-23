grammar GitHubDiff;

options {
	language = Java;
}

import LexerRules;

root_statement         : diff+;

diff                   : diff_header hunk+;

diff_header            : declaration_line file_mode_line? index_line source_file_line target_file_line;

declaration_line       : DIFF WS (OPTION WS)* path WS path NEWLINE;
index_line             : INDEX WS compare_hashes (WS NUMBER)? NEWLINE;
source_file_line       : MINUS3 WS path NEWLINE;
target_file_line       : PLUS3 WS path NEWLINE;
file_mode_line         : new_file_line | deleted_file_line;
new_file_line          : NEW WS FILE WS MODE WS NUMBER NEWLINE;
deleted_file_line      : DELETED WS FILE WS MODE WS NUMBER NEWLINE;

hunk                   : hunk_line line+;
line                   : added_line | removed_line | neutral_line | missing_newline_line;

compare_hashes         : hash DOTDOT hash;
hunk_line              : ATAT WS MINUS range WS PLUS range WS ATAT text NEWLINE;
range                  : NUMBER COMMA NUMBER;
added_line             : PLUS text end;
removed_line           : MINUS text end;
missing_newline_line   : BACKSLASH text end;
neutral_line           : WS text end;

end                    : NEWLINE | EOF ;
hash                   : WORD | NUMBER ;
path                   : word;
text                   : ~(NEWLINE)* ;
word                   : ~(WS | NEWLINE)+ ;