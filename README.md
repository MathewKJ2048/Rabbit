# Rabbit


An open-source recursive-descent parser generator which supports syntax directed translation of LL(1) grammars, written in and compatible with java.

## Requirements:

- Java version 17 or higher is required. 
- A headless JRE may be used since Rabbit uses a CLI. 


## Installation and Use

### For Linux:

1) Download **rabbit.jar** from [here](https://github.com/MathewKJ2048/Rabbit/blob/main/downloads/rabbit.jar?raw=true).
2) Write the grammar in a file named **grammar.sdt** and store it along with **rabbit.jar** in the same folder.
3) Run the following command to generate the parser:
```
java -jar rabbit.jar grammar.sdt Parser.java
```
4) Run the following commands to run the parser:
```
javac Parser.java
java Parser
```


### For Windows:

1) Download [rabbit.exe] from [here](https://github.com/MathewKJ2048/Rabbit/blob/main/downloads/rabbit.exe?raw=true).
2) Write the grammar in a file named **grammar.sdt** and store it along with **rabbit.exe** in the same folder.
3) Run the following command to generate the parser:
```
./rabbit.exe grammar.sdt Parser.java
```
4) Run the following commands to run the parser:
```
javac Parser.java
java Parser
```

Alternatively, the same procedure used for Linux can be followed.


### For OSX:
~~go pound sand~~ follow the same procedure as Linux.

## Syntax

### Template:

```
{
	// import statements go here
}
{
	// global code goes here
	public static void main(String[] args)
	{
		String input = "";
		//accept input
		Parser p = new Parser(input);
		if(p._parse())
		{
			//accept condition
		}
		else
		{
			//reject condition
		}
	}
}
{
	// type declarations for non-terminals go here
}

// grammar goes here
```

### Writing a Grammar:
- A terminal is represented by a valid java identifier which does not use an underscore as the leading character.
- A non-terminal is enclosed in double quotes, and represents the string inside the quotes.  
    - ϵ (the null string) is represented by `""`.
    - double quotes, when used in the string, need to be escaped like so: `"\""`
    - backslashes, when used in the string, need to be escaped like so: `"\\"`
- A rule is of the form `<S> <S> <S> <S> ... { // production code } ` where: 
    - `<S>` is either a terminal or non-terminal. 
    - `{ // production code }` is optional.
- A production is of the form `<NT> -> <R> | <R> | <R> ... | <R> ;` where:
    - The non-terminal `<NT>` is the head of the production.
    - Every `<R>` is a rule for derivation of `<NT>`.
    - `|` represents the union operator.
    - `;` represents the end of the production.
- Every non-terminal must have at least one production.
- The head of the first production is the start symbol for the grammar.
- If the grammar is not an LL(1) grammar, the parser may give incorrect results or enter an infinite recursion.

#### Example grammars

1) The string "rabbit"  
```
S -> "rabbit" ;
```
2) Strings made up of only 'R's   
```
S -> "R" S {System.out.println("An R has been encountered");} | "" ;
```
3) 0<sup>n</sup>1<sup>n</sup> where n is a whole number
```
S -> "0" S "1" | "" ;
```
4) arithmetic expressions with associativity and precedence
```
S -> E
E -> T "+" E | T "-" E | T ;
T -> F "*" T | F "/" T | F ;
F -> "(" E ")" | NUM ;
NUM -> DIGIT NUM | DIGIT ;
DIGIT -> "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
```

### Type declarations:

- Each non-terminal has an associated object in the parse tree generated by the parser.
- If unspecified, the type defaults to the Object class as defined in java.
- Types may be specified as follows:
```
<T1> <NT11> <NT12> <NT13> ... ;
<T2> <NT21> <NT22> <NT23> ... ;
...
```
Here, the object associated with non-terminal `<NTij>` is of type `<Ti>`.
- Each type is a java class, which may be pre-existing or defined by the user in the global code section.

### Global Code:

- This code is inserted straight into the Parser class.
- In addition to standard java syntax, an additional rule applies here - no identifier may begin with an underscore.
- The main() method resides here (if it is needed). It is recommended that it have the header `public static void main(String[] args)`, though any header may be used. 
- Available features:
    - `Parser(String)` is a constructor which creates an object of class Parser and sets the string to be parsed.
    - `boolean _parse()` is a function which builds the parse tree and returns `true` if the parsing is successful.
    -`String _parse_tree()` is a function which returns a stringified representation of the parse tree.
    - `String _derivation(boolean)` is a function which returns a stringified derivation of the input. If the argument passed is `true`, a rightmost derivation is returned. If not, a leftmost derivation is returned.
    - `_DELIMITER` is a global String variable used to determine the separator between symbols in the stringified derivation produced by `_derivation()`.
    - `_OPEN`, `_CLOSE` and `_SPACE` are global String variables which control the representation of the stringified representation of the parse tree produced by `_parse_tree()`.

### Production code:

- This code is associated with the corresponding rule of the production where it is written.
- It is executed when the corresponding rule is successfully used during parsing.
- Available features:
  - `$$` is a reference to the object associated with the head of the production.
  - `$i` is a reference to the object associated with the i<sup>th</sup> symbol of the corresponding rule, provided that it is a non-terminal. The value of i begins from 1.


## Credits:
Author: [Mathew K J](https://github.com/MathewKJ2048)  
Testing: [Mathew K J](https://github.com/MathewKJ2048), [Abijith Shaji](https://github.com/Saangetheya)  
Art: <a href="https://www.flaticon.com/free-icons/rabbit" title="rabbit icons">Rabbit icons created by Freepik - Flaticon</a>
