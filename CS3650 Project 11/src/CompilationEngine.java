import java.io.*;
import java.util.HashSet;
import java.util.HashMap;

public class CompilationEngine {
	VMWriter writer = null;
	JackTokenizer tokenizer = null;
	//int tabs = 0; //keeps track of how many calls deep we are
	int labels = 0; //keeps track of how many branches have been encountered
	
	//project 11 additions
	SymbolTable classTable;
	SymbolTable subroutineTable;
	String className;
	
	HashMap<String, String> translator = new HashMap<String, String>();
	
	CompilationEngine(File in, File out)
	{
		try {
			writer = new VMWriter(out);
		} catch (Exception e) {
			System.out.println("Problem with instantiating the compiler");
			e.printStackTrace();
		}
		
		tokenizer = new JackTokenizer(in);
		
		classTable = new SymbolTable();
		subroutineTable = new SymbolTable();
		
		translator.put("STATIC", "static");
		translator.put("FIELD", "local");
		translator.put("ARG", "argument");
		translator.put("VAR","local");
	}
	
	//commented after each method declaration is 2 digits
	//the first digit is how far advanced the method expects to start (0 before first token, 1 on)
	//the second digit is how far advanced the method will end (0 on, 1 past)
	
	//i suspect this whole assignment will be MUCH more coherent if i can get everything to be consistent
	//you can't really go down, but you can always advance, so lets try to make everything 11
	//except the compileclass routine, which will be 01
	
	//update: i did it, this is much easier to deal with
	
	//"class" className "{" classVarDec* subroutineDec* "}"
	void compileClass()//01
	{
		try {
			tokenizer.advance();
			processCurrentKeyword("class");
			className = processCurrentIdentifier();
			processCurrentSymbol('{');
			
			//if the next token is a keyword
			if (tokenizer.tokenType().equals("KEYWORD"))
			{
				String keyword = tokenizer.keyword();
				
				//first compile all variable declarations
				while (keyword.equals("FIELD") || keyword.equals("STATIC"))
				{
					compileClassVarDec();
					keyword = tokenizer.keyword();
				}
				
				//then compile all constructors
				while (keyword.equals("CONSTRUCTOR"))
				{
					compileSubroutine();
					keyword = tokenizer.keyword();
				}
				
				//then compile all functions and methods
				while (keyword.equals("FUNCTION") || keyword.equals("METHOD"))
				{
					compileSubroutine();
					keyword = tokenizer.keyword();
				}
				
				//once exhausting all those possibilities, if there is still a keyword, something went wrong
				if (tokenizer.tokenType().equals("KEYWORD"))
					throw new TypeException("FIELD/STATIC/CONSTRUCTOR/FUNCTION/METHOD", tokenizer.keyword());
			}
			
			//then process the last symbol if not
			processCurrentSymbol('}');
			
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("Error: Expected type " + te.expected + "but got " + te.actual);
		}
	}
	
	//("static" | "field") type varName (',' varName)* ';'
	void compileClassVarDec() //11
	{		
		//the token will already be on either static or field
		//printToken("keyword", tokenizer.keyword().toLowerCase());
		String kind = tokenizer.keyword();
		tokenizer.advance();
		
		//process the type
		String type = processCurrentType();
			
		//process the variable name (there will be at least one)
		String name = processCurrentIdentifier();
			
		//add to the symbol table
		classTable.define(name, type, kind);
		//check if there is a comma, if so, add the next variable
		while (tokenizer.symbol() == ',')
		{
			//printToken("symbol", ",");
			tokenizer.advance();
			name = processCurrentIdentifier();
			
			classTable.define(name, type, kind);
		}
	
		//process that last semicolon
		processCurrentSymbol(';'); 
	}

	//("constructor"|"function"|"method") ("void"|type) subroutineName '(' parameterList ')' subroutineBody
	void compileSubroutine() //11
	{		
		//keyword processed before, token should already be on it
		//printToken("keyword", tokenizer.keyword().toLowerCase());
		subroutineTable.reset(); //reset the subroutine table
		boolean method = tokenizer.keyword().equals("METHOD");
		boolean constructor = tokenizer.keyword().equals("CONSTRUCTOR");
		
		if (method) //add to the table if this is a method
			subroutineTable.define("this", className, "ARG");
		
		tokenizer.advance();
		
		//process the type
		//this is the same as processType except void is valid so it can't be in the same function
		//type checking, but void is allowed
		//make sure its a keyword
		//String type = "VOID"; //jack is weakly typed, this is not necessary
		//if its a void you can skip over it, otherwise, make sure it is a valid type
		if (!tokenizer.keyword().equals("VOID"))
			processCurrentType();
		else
			tokenizer.advance();
		
		//process the name
		String name = processCurrentIdentifier();
		
		//process the opening parenthesis
		processCurrentSymbol('(');
		
		//compile the list of parameters
		//building up the symbol table for args
		//this also does the function declaration
		compileParameterList();
		
		//process the closing parenthesis
		processCurrentSymbol(')');
		
		//subroutineBody
		processCurrentSymbol('{');
		
		//var declarations
		//System.out.println(tokenizer.tokenType());
		while(tokenizer.keyword().equals("VAR"))
			compileVarDec();
		
		//write the function name
		writer.writeFunction(className + '.' + name, subroutineTable.varCount("VAR"));
		
		//align the base pointer with that of the object
		if (method)
		{
			writer.writePush("argument", 0);
			writer.writePop("pointer", 0);
		}
		
		if (constructor)
		{
			//allocate memory of the correct size
			writer.writePush("constant", classTable.varCount("FIELD"));
			writer.writeCall("Memory.alloc", 1); 
			
			//align in memory
			writer.writePop("pointer", 0); //set this to the base address
		}
		
		//compile the subroutine body
		compileSubroutineBody();	
	}
	
	//((type varName) (',' type varName)*)?
	void compileParameterList() //11
	{
		try {
			boolean skip = false;
			String kind = "ARG";
			String name;
			
			//process the first type if it exists
			//typechecking, except in this case no type is a type
			//make sure its a keyword
			String type = tokenizer.tokenType();
			if (type.equals("KEYWORD"))
			{
				type = tokenizer.keyword();
				if (!(type.equals("INT") || type.equals("BOOLEAN") || type.equals("CHAR")))
					throw new TypeException("INT/BOOLEAN/CHAR", type);
				else
					//printToken("keyword", keyword.toLowerCase());
					tokenizer.advance();
			}
			//if not, assume its a class
			else if (type.equals("IDENTIFIER"))
			{
				//printToken("identifier", tokenizer.identifier());
				type = tokenizer.identifier();
				tokenizer.advance();
			}
			//anything else, the parameter list must be empty, as that means there is no type,
			//this means we don't actually want to check for a comma OR add anything to the symbol table
			else
			{
				skip = true;
			}
			
			
			if (!skip)
			{
				//grab the name
				name = processCurrentIdentifier();
				//add to the symbol table
				subroutineTable.define(name, type, kind);
				//check if there is a comma, if so, add the next variable to the symbol table
				while (tokenizer.symbol() == ',')
				{
					//printToken("symbol", ",");
					tokenizer.advance();
					type = processCurrentType();
					name = processCurrentIdentifier();
					subroutineTable.define(name, type, kind);
				}
			}
			
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("Error: Expected type " + te.expected + "but got " + te.actual);
		}
		
	}
	
	//'{' varDec* statements '}'
	void compileSubroutineBody() //11
	{		
		//move these up to the subroutine dec
		/*processCurrentSymbol('{');
		
		//var declarations
		//System.out.println(tokenizer.tokenType());
		while(tokenizer.keyword().equals("VAR"))
			compileVarDec();*/
			
		compileStatements(); //this ends one after the statements block
			
		processCurrentSymbol('}');
	}
	
	//"var" type varName (',' varName)* ';'
	void compileVarDec() //11
	{		
		//this will only be called if "var" is read
		//printToken("keyword", "var");
		String kind = "VAR";
		tokenizer.advance();
			
		//process the type
		String type = processCurrentType();
			
		//process varName
		String name = processCurrentIdentifier();
		//add to the symbol table
		subroutineTable.define(name, type, kind);
		//if there is a comma add the variable following it to the symbol table
		while (tokenizer.symbol() == ',')
		{
			//printToken("symbol", ",");
			tokenizer.advance();
			name = processCurrentIdentifier();
			subroutineTable.define(name, type, kind);
		}
		//process the final semicolon
		processCurrentSymbol(';');
	}

	void compileStatements() //11
	{
		try {
			//tokenizer should be advanced to the first statement by this block
			while (tokenizer.tokenType().equals("KEYWORD"))
			{
				if (tokenizer.keyword().equals("LET"))
					compileLet();
				else if (tokenizer.keyword().equals("IF"))
					compileIf();
				else if (tokenizer.keyword().equals("WHILE"))
					compileWhile();
				else if (tokenizer.keyword().equals("DO"))
					compileDo();
				else if (tokenizer.keyword().equals("RETURN"))
					compileReturn();
				else
					throw new TypeException("LET/IF/WHLILE/DO/RETURN", tokenizer.keyword());
			}
		
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("Error: Expected type " + te.expected + "but got " + te.actual);
		}
	}
	
	//"let" varName ('[' expression ']')? '=' expression ';'
	void compileLet() //11
	{
		try {
			/*writeTabs();
			writer.write("<letStatement>\n");
			tabs++;*/
			
			//the tokenizer is already sitting at the let statement
			//printToken("keyword", "let");
			tokenizer.advance();
			
			//process the varname
			int offset;
			String kind;
			String name = processCurrentIdentifier();
			if (!subroutineTable.kindOf(name).equals("NONE")) //look at the subroutine first
			{
				offset = subroutineTable.indexOf(name);
				kind = subroutineTable.kindOf(name);
			}
			else if (!classTable.kindOf(name).equals("NONE"))//if its not there its gonna be here
			{
				offset = classTable.indexOf(name);
				kind = classTable.kindOf(name);
			}
			else 
				throw new Exception();
			
			//TODO add array support
			//the next token will either be a [ or an = or something is wrong
			boolean array = tokenizer.symbol() == '[';
			if (array)
			{
				//process that entire possibility to get to the equals
				//printToken("symbol", "[");
				tokenizer.advance();
				writer.writePush(translator.get(kind), offset);
				compileExpression();
				processCurrentSymbol(']');
				
				//save the expression value in temp 0
				writer.writePop("temp", 0);
			}
			//processing equals is a bit different so i just handle it inhouse
			//it says it should be in the book but it really doesn't look that way in vscode
			//and it really doesn't matter if i'm not printing it
			processCurrentSymbol('=');
			//else
				//throw new SymbolException('=', tokenizer.symbol());
			
			compileExpression();
			
			if (array)
			{
				writer.writePop("pointer", 1);
				writer.writePush("temp", 0);
				writer.writePop("that", 0);
			}
			else
				writer.writePop(translator.get(kind), offset);
			
			processCurrentSymbol(';');
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: attempting to use an uninstantiated variable");
		}
	}
	//"if" '(' expression ')' '{' statements '}' ("else" '{' statements '}')?
	void compileIf()//11
	{		
		//tokenizer should already be pointing at the if
		//printToken("keyword", "if");
		tokenizer.advance();
		
		//expression being evaluated in parentheses
		processCurrentSymbol('(');
		compileExpression();
		processCurrentSymbol(')');
		
		writer.writeArithmetic("not");
		writer.writeIf("L" + labels);
		
		//statements block
		processCurrentSymbol('{');
		compileStatements();
		processCurrentSymbol('}');
		
		writer.writeGoto("L" + (labels+1));
			
		writer.writeLabel("L" + labels++);
		//check if there is an else block
		if (tokenizer.keyword().equals("ELSE"))
		{
			//if so, process the else block
			//printToken("keyword", "else");
			tokenizer.advance();
			processCurrentSymbol('{');
			compileStatements();
			processCurrentSymbol('}');
		}
		
		writer.writeLabel("L" + labels++);
	}
	//"while" '(' expression ')' '{' statements '}'
	void compileWhile()//11
	{		
		//token should already be pushed to the while
		//printToken("keyword", "while");
		tokenizer.advance();
			
		writer.writeLabel("L" + labels);
		//write the expression block
		processCurrentSymbol('(');
		compileExpression();
		processCurrentSymbol(')'); //expression pushes past into the next token
		
		writer.writeArithmetic("not");
		
		writer.writeIf("L" + (labels + 1));
		
		//write the statements block
		processCurrentSymbol('{');
		compileStatements();
		processCurrentSymbol('}');
		
		writer.writeGoto("L" + 	labels++);
		writer.writeLabel("L" + labels++);
	}
	//"do" subRoutineCall ';'
	//it says to process this as "do" expression ';'
	void compileDo()//11
	{		
			//tokenizer should be pointed at the do
			//printToken("keyword", "do");
			tokenizer.advance();
			
			//compile the expression
			compileExpression();
			
			//get rid of that value that was calculated
			writer.writePop("temp", 0);
			
			//process the last semicolon
			processCurrentSymbol(';');
	}
	//"return" expression? ';'
	void compileReturn()//11
	{		
		//return has already been read
		//printToken("keyword", "return");
		tokenizer.advance();
			
		//check if the next part is a symbol, if not, assume it is an expression
		//this means that expression will start at the first token of the expression
		//put the expression on the stack
		if (tokenizer.symbol() != ';')
			compileExpression();
		else
			writer.writePush("constant", 0); //for void functions
			
		//return
		writer.writeReturn();
			
		//ensure there is a semicolon
		processCurrentSymbol(';');
	}
	
	//term (op term)*
	void compileExpression() //11
	{		
		//it will already start on the first term
		//and push that onto the stack
		compileTerm();
		
		//make a hash table for all the ops cuz theres no way
		HashMap<Character, String> ops = new HashMap<Character, String>();
		ops.put('+', "add");
		ops.put('-', "sub");
		ops.put('*', "Math.multiply 2");
		ops.put('/', "Math.divide 2");
		ops.put('&', "and");
		ops.put('|', "or");
		ops.put('<', "lt");
		ops.put('>', "gt");
		ops.put('=', "eq");
		
		//check for the ops
		while (ops.containsKey(tokenizer.symbol()))
		{
			//hold onto the symbol so you can perform the op after compiling the next one
			char symbol = tokenizer.symbol();
			//printToken("symbol", Character.toString(tokenizer.symbol()));
			//System.out.println("advancing"); //expect to see op, then advancing, then next term, then compiing term
			tokenizer.advance();
			//compile the next term, pushing it onto the stack
			compileTerm();
			
			//write the arithmetic operation
			writer.writeArithmetic(ops.get(symbol));
		}
	}
	
	//terms are the parts of the expression that we push
	void compileTerm()
	{
		try {
			//we push terms.
			//System.out.println("compiling term: " + tokenizer.identifier());
			String type = tokenizer.tokenType();
			if (type.equals("INT_CONST"))
			{
				//System.out.println("processing int: " + tokenizer.intVal());
				//printToken("integerConstant", Integer.toString(tokenizer.intVal()));
				//push the integer constant and advance
				writer.writePush("constant", tokenizer.intVal());
				tokenizer.advance();
			}
			else if (type.equals("STRING_CONST"))
			{
				//printToken("stringConstant", tokenizer.stringVal());
				//assemble the string
				String str = tokenizer.stringVal();
				
				//first push the length of the string
				writer.writePush("constant", str.length());
				//then call the string constructor
				writer.writeCall("String.new", 1);
				//then actually assemble the string
				for (int i = 0; i < str.length(); i++)
				{
					//push the unicode character at the current index as an int
					writer.writePush("constant", Character.getNumericValue(str.charAt(i)));
					//call appendChar to add it to the string
					writer.writeCall("String.appendChar", 2);
				}
				
				tokenizer.advance();
			}
			else if (type.equals("KEYWORD"))
			{
				//is this efficient, no.
				//is it nicer than the alternative, yes.
				HashSet<String> consts = new HashSet<String>();
				consts.add("TRUE");
				consts.add("FALSE");
				consts.add("NULL");
				consts.add("THIS");
				
				if (consts.contains(tokenizer.keyword()))
				{
					//process true
					if (tokenizer.keyword().equals("TRUE"))
						writer.writePush("constant", 1);
					else if (tokenizer.keyword().equals("FALSE") || tokenizer.keyword().equals("NULL"))
						writer.writePush("constant", 0);
					else if (tokenizer.keyword().equals("THIS"))
						writer.writePush("pointer", 0);
	
					//printToken("keywordConstants", tokenizer.keyword().toLowerCase());
					tokenizer.advance();
				}
				else
					throw new TypeException("TRUE/FALSE/NULL/THIS", tokenizer.keyword());
					
			}
			else if (type.equals("IDENTIFIER"))
			{
				//variables\
				//System.out.println("processing identifier:" + tokenizer.identifier());
				String name = processCurrentIdentifier();
				String kind = null;
				boolean known = false;
				int offset = 0;
				//change it from identifier to something the vm knows (kind and offset)
				if (!subroutineTable.kindOf(name).equals("NONE")) //look at the subroutine first
				{
					offset = subroutineTable.indexOf(name);
					kind = subroutineTable.kindOf(name);
					known = true;
				}
				else if (!classTable.kindOf(name).equals("NONE"))//if its not there its gonna be here
				{
					offset = classTable.indexOf(name);
					kind = classTable.kindOf(name);
					known = true;
				}
				//better hope its one of these 2 or else things are gonna break
				/*else 
					throw new Exception();*/
				
				//if the variable is an array
				if (tokenizer.symbol() == '[')
				{
					//printToken("symbol", "[");
					tokenizer.advance();
					writer.writePush(kind, offset);
					compileExpression();
					processCurrentSymbol(']');
					
					//calculate the memory address of the array
					writer.writeArithmetic("add");
					writer.writePop("pointer", 1);
					//dereference and get the data
					writer.writePush("that",0);
				}
				//if the variable is a subroutine call
				else 
				{
					//only come in here if not an array
					char symbol = tokenizer.symbol();
					
					//class.method()
					if (symbol == '.')
					{
						int nArgs = 0;
						//push the class
						if (known)
						{
							writer.writePush(kind, offset);
							nArgs++;
						}
						tokenizer.advance();
						String func = processCurrentIdentifier();
						//System.out.println("processing (");
						processCurrentSymbol('(');
						//System.out.println("compiling expressions");
						nArgs += compileExpressionList(); //count how many arguments there are
						processCurrentSymbol(')');
						
						//call the function
						writer.writeCall(name + '.' + func, nArgs);
					}
					//function()
					else if (symbol == '(')
					{
						tokenizer.advance();
						int nArgs = compileExpressionList();
						processCurrentSymbol(')');
						
						//call the function
						writer.writeCall(className + '.' + name, nArgs);
					}
					//not a call
					else
					{
						//System.out.println("not a call");
						writer.writePush(translator.get(kind), offset);
					}
				}
			}
			else if (type.equals("SYMBOL"))
			{
				//System.out.println("symbol");
				if (tokenizer.symbol() == '(')
				{
					//printToken("symbol", "(");
					tokenizer.advance();
					compileExpression();
					processCurrentSymbol(')');
				}
				else if (tokenizer.symbol() == '-' || tokenizer.symbol() == '~')
				{
					//hold on to this symbol
					char symbol = tokenizer.symbol();
					tokenizer.advance();
					//compile the next term
					compileTerm();
					
					//negation
					if (symbol == '-')
						writer.writeArithmetic("neg");
					//logical not
					else
						writer.writeArithmetic("not");
				}
			}
			//those are the only 5 options, anything else and i think i give up coding forever haha jk
			//forreal tho
				
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("EXPECTED " + te.expected + " but got "  + te.actual);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Variable used before declaration");
		}
	}
	
	//(expression (',' expression)*)?
	int compileExpressionList()
	{
		int expressions = 0; //count the amount of expressions found
		//increment every time an expression is compiled	
		
		//all expression lists are closed with ), so we will leverage that
		while (tokenizer.symbol() != ')')
		{
			compileExpression();
			expressions++;
				
			//if it is a comma here, it must be followed by an expression, so it will loop back
			//if not, it better be a close paren cuz if not we have an infinite loop until there is
			//System.out.println(tokenizer.symbol());
			if (tokenizer.symbol() == ',')
				tokenizer.advance();
		}
		
		return expressions;
	}
	
	void close()
	{
		writer.close();
	}
	
	// the process function now just checks input and advances
	private void processCurrentKeyword(String keyword)//11
	{
		try {
			//ensure the type is keyword
			if (!tokenizer.tokenType().equals("KEYWORD"))
				throw new TypeException("KEYWORD", tokenizer.tokenType());
			
			//the word itself
			if (!tokenizer.keyword().equals(keyword.toUpperCase()))
				throw new TypeException(keyword.toUpperCase(), tokenizer.keyword());
			
			//printToken("keyword", keyword);
			tokenizer.advance();
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("Error: Expected type " + te.expected + "but got " + te.actual);
		}
	}
	
	private String processCurrentIdentifier()//11
	{
		String token = "";
		try {
			//ensure the type is identifier
			if (!tokenizer.tokenType().equals("IDENTIFIER"))
				throw new TypeException("IDENTIFIER", tokenizer.tokenType());
			
			//printToken("identifier", tokenizer.identifier());
			token = tokenizer.identifier();
			tokenizer.advance();
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("Error: Expected type " + te.expected + "but got " + te.actual);
		}
		
		return token;
	}
	
	private void processCurrentSymbol(char symbol) //11
	{
		try {
			//ensure the type is symbol
			if (!tokenizer.tokenType().equals("SYMBOL"))
				throw new TypeException("SYMBOL", tokenizer.tokenType());
			
			//the word itself
			if (!(tokenizer.symbol() == symbol))
				throw new SymbolException(symbol, tokenizer.symbol());
			
			//print the xml
			//printToken("symbol", Character.toString(symbol));
			if (tokenizer.hasMoreTokens())
				tokenizer.advance();
			
		} catch (SymbolException se) {
			se.printStackTrace();
			System.out.println("Error: Expected symbol " + se.expected + "but got " + se.actual);
			
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("Error: Expected type " + te.expected + "but got " + te.actual);
		}
	}
	
	private String processCurrentType() //11
	{
		String type = "";
		try {
			//make sure its a keyword
			type = tokenizer.tokenType();
			if (type.equals("KEYWORD"))
			{
				String keyword = tokenizer.keyword();
				if (!(keyword.equals("INT") || keyword.equals("BOOLEAN") || keyword.equals("CHAR")))
					throw new TypeException("INT/BOOLEAN/CHAR", keyword);
				else
					//printToken("keyword", keyword.toLowerCase());
					tokenizer.advance();
			}
			//if not, assume its a class
			else if (type.equals("IDENTIFIER"))
			{
				//printToken("identifier", tokenizer.identifier());
				tokenizer.advance();
			}
			//anything else, throw an exception
			else
				throw new TypeException("KEYWORD/IDENTIFIER", tokenizer.tokenType());
			
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("Error: Expected type " + te.expected + "but got " + te.actual);
		}
		return type;
	}
}
