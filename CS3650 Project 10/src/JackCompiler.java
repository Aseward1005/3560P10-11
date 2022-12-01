import java.io.*;
import java.util.Scanner;
import java.util.HashSet;

public class JackCompiler {
	FileWriter writer = null;
	Scanner reader = null;
	JackTokenizer tokenizer = null;
	int tabs = 0; //keeps track of how many calls deep we are
	
	JackCompiler(File in, File out)
	{
		try {
			writer = new FileWriter(out);
		} catch (Exception e) {
			System.out.println("Problem with instantiating the compiler");
			e.printStackTrace();
		}
		
		tokenizer = new JackTokenizer(in);
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
			writer.write("<class>\n");
			tabs++;
			
			tokenizer.advance();
			processCurrentKeyword("class");
			processCurrentIdentifier();
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
			
			tabs--;
			writer.write("</class>\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem when compiling class");
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("Error: Expected type " + te.expected + "but got " + te.actual);
		}
	}
	
	//compiles all the tokens into an xml file
	//for tokenizer testing
	void compileTokens()
	{
		try {
			writer.write("<tokens>\n");
			//if you have an empty file, don't even bother.
			if (tokenizer.hasMoreTokens())
			{
				boolean plus1 = false;
				tokenizer.advance();
				while (tokenizer.hasMoreTokens() || plus1)
				{
					String type = tokenizer.tokenType();
					
					if (type.equals("IDENTIFIER"))
						printToken("identifier", tokenizer.identifier());
					else if (type.equals("INT_CONST"))
						printToken("integerConstant", Integer.toString(tokenizer.intVal()));
					else if (type.equals("STRING_CONST"))
						printToken("StringConstant", tokenizer.stringVal());
					else if (type.equals("KEYWORD"))
						printToken("keyword", tokenizer.keyword().toLowerCase());
					else if (type.equals("SYMBOL"))
					{
						char symbol = tokenizer.symbol();
						if (symbol == '<')
							printToken("symbol", "&lt");
						else if (symbol == '>')
							printToken("symbol", "&gt");
						else if (symbol == '=')
							printToken("symbol", "&eq");
						else
							printToken("symbol", Character.toString(symbol));
					}
					//this is possible on the very last iteration
					if (!tokenizer.hasMoreTokens())
						plus1 = !plus1;
					
					//System.out.println(plus1);
				}
			}
				writer.write("</tokens>\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem when printing tokens");
		}
	}
	
	//("static" | "field") type varName (',' varName)* ';'
	void compileClassVarDec() //11
	{
		try {
			writeTabs();
			writer.write("<classVarDec>\n");
			tabs++;
		
			//the token will already be on either static or field
			printToken("keyword", tokenizer.keyword().toLowerCase());
		
			//process the type
			processCurrentType();
			
			//process the variable name (there will be at least one)
			processCurrentIdentifier();
			
			//check if there is a comma, if so, add it and the next variable
			while (tokenizer.symbol() == ',')
			{
				printToken("symbol", ",");
				processCurrentIdentifier();
			}
		
			//process that last semicolon
			processCurrentSymbol(';');
			
			tabs--;
			writeTabs();
			writer.write("</classVarDec>\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem when compiling classvardec");
		} 
	}

	//("constructor"|"function"|"method") ("void"|type) subroutineName '(' parameterList ')' subroutineBody
	void compileSubroutine() //11
	{
		try {
			writeTabs();
			writer.write("<subroutineDec>\n");
			tabs++;
			
			//keyword processed before, token should already be on it
			printToken("keyword", tokenizer.keyword().toLowerCase());
			
			//process the type
			//this is the same as processType except void is valid so it can't be in the same function
			//type checking, but void is allowed
			//make sure its a keyword
			String type = tokenizer.tokenType();
			if (type.equals("KEYWORD"))
			{
				String keyword = tokenizer.keyword();
				if (!(keyword.equals("INT") || keyword.equals("BOOLEAN") || keyword.equals("CHAR") || keyword.equals("VOID")))
					throw new TypeException("INT/BOOLEAN/CHAR/VOID", keyword);
				else
					printToken("keyword", keyword.toLowerCase());
			}
			//if not, assume its a class
			else if (type.equals("IDENTIFIER"))
				printToken("identifier", tokenizer.identifier());
			//anything else, throw an exception
			else
				throw new TypeException("KEYWORD/IDENTIFIER", tokenizer.tokenType());
			
			//process the name
			processCurrentIdentifier();
			
			//process the opening parenthesis
			processCurrentSymbol('(');
			
			//compile the list of parameters
			compileParameterList();
			
			//process the closing parenthesis
			processCurrentSymbol(')');
			
			//compile the subroutine body
			compileSubroutineBody();
			
			tabs--;
			writeTabs();
			writer.write("</subroutineDec>\n");
			
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem when compiling subroutine");
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("Error: Expected type " + te.expected + "but got " + te.actual);
		}
	}
	
	//((type varName) (',' type varName)*)?
	void compileParameterList() //11
	{
		try {
			writeTabs();
			writer.write("<parameterList>\n");
			tabs++;
		
			boolean skip = false;
			//process the first type if it exists
			//typechecking, except in this case no type is a type
			//make sure its a keyword
			String type = tokenizer.tokenType();
			if (type.equals("KEYWORD"))
			{
				String keyword = tokenizer.keyword();
				if (!(keyword.equals("INT") || keyword.equals("BOOLEAN") || keyword.equals("CHAR")))
					throw new TypeException("INT/BOOLEAN/CHAR", keyword);
				else
					printToken("keyword", keyword.toLowerCase());
			}
			//if not, assume its a class
			else if (type.equals("IDENTIFIER"))
			{
				printToken("identifier", tokenizer.identifier());
			}
			//anything else, the parameter list must be empty, as that means there is no type,
			//this means we don't actually want to check for a comma.
			else
			{
				skip = true;
			}
			
			//check if there is a comma, if so, add it and the next type and the next variable
			if (!skip)
			{
				while (tokenizer.symbol() == ',')
				{
					printToken("symbol", ",");
					processCurrentType();
					processCurrentIdentifier();
				}
			}
		
			tabs--;
			writeTabs();
			writer.write("</parameterList>\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem when compiling parameter list");
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("Error: Expected type " + te.expected + "but got " + te.actual);
		}
		
	}
	
	//'{' varDec* statements '}'
	void compileSubroutineBody() //11
	{
		try {
			writeTabs();
			writer.write("<subroutineBody>\n");
			tabs++;
			
			processCurrentSymbol('{');
			
			//var declarations
			//System.out.println(tokenizer.tokenType());
			while(tokenizer.keyword().equals("VAR"))
				compileVarDec();
			
			compileStatements(); //this ends one after the statements block
			
			processCurrentSymbol('}');
			
			tabs--;
			writeTabs();
			writer.write("</subroutineBody>\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem when compiling subroutine body");
		}
	}
	
	//"var" type varName (',' varName)* ';'
	void compileVarDec() //11
	{
		try {
			writeTabs();
			writer.write("<varDec>\n");
			tabs++;
			
			//this will only be called if "var" is read
			printToken("keyword", "var");
			
			//process the type
			processCurrentType();
			
			//process varName
			processCurrentIdentifier();
			
			//if there is a comma, process it and the variable name following it
			while (tokenizer.symbol() == ',')
			{
				printToken("symbol", ",");
				processCurrentIdentifier();
			}
			
			//process the final semicolon
			processCurrentSymbol(';');
			
			tabs--;
			writeTabs();
			writer.write("</varDec>\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem when compiling variable declaration");
		}
	}

	void compileStatements() //11
	{
		try {
			writeTabs();
			writer.write("<statements>\n");
			tabs++;
			
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
		
			tabs--;
			writeTabs();
			writer.write("</statements>\n");
		} catch (IOException e){
			e.printStackTrace();
			System.out.println("problem when compiling statements");
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("Error: Expected type " + te.expected + "but got " + te.actual);
		}
	}
	
	//"let" varName ('[' expression ']')? '=' expression ';'
	void compileLet() //11
	{
		try {
			writeTabs();
			writer.write("<letStatement>\n");
			tabs++;
			
			//the tokenizer is already sitting at the let statement
			printToken("keyword", "let");
			
			//process the varname
			processCurrentIdentifier();
			
			//the next token will either be a [ or an = or something is wrong
			if (tokenizer.symbol() == '[')
			{
				//process that entire possibility to get to the equals
				printToken("symbol", "[");
				compileExpression();
				processCurrentSymbol(']');
			}
			//processing equals is a bit different so i just handle it inhouse
			//it says it should be in the book but it really doesn't look that way in vscode
			//if (tokenizer.symbol() == '=')
			//{
				//printToken("symbol", "&eq");
			//}
			processCurrentSymbol('=');
			//else
				//throw new SymbolException('=', tokenizer.symbol());
			
			compileExpression();
			
			processCurrentSymbol(';');
			
			tabs--;
			writeTabs();
			writer.write("</letStatement>\n");
		} catch (IOException e){
			e.printStackTrace();
			System.out.println("problem when compiling let statement");
		} /*catch (SymbolException se) {
			se.printStackTrace();
			System.out.println("Error: Expected symbol " + se.expected + "but got " + se.actual);
		}*/
	}
	//"if" '(' expression ')' '{' statements '}' ("else" '{' statements '}')?
	void compileIf()//11
	{
		try {
			writeTabs();
			writer.write("<ifStatement>\n");
			tabs++;
			
			//tokenizer should already be pointing at the if
			printToken("keyword", "if");
			
			//expression being evaluated in parentheses
			processCurrentSymbol('(');
			compileExpression();
			processCurrentSymbol(')');
			
			//statements block
			processCurrentSymbol('{');
			compileStatements();
			processCurrentSymbol('}');
			
			//check if there is an else block
			if (tokenizer.keyword().equals("ELSE"))
			{
				//if so, process the else block
				printToken("keyword", "else");
				processCurrentSymbol('{');
				compileStatements();
				processCurrentSymbol('}');
			}
			
			tabs--;
			writeTabs();
			writer.write("</ifStatement>\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem when compiling if statement");
		}
	}
	//"while" '(' expression ')' '{' statements '}'
	void compileWhile()//11
	{
		try {
			writeTabs();
			writer.write("<whileStatement>\n");
			tabs++;
			
			//token should already be pushed to the while
			printToken("keyword", "while");
			
			//write the expression block
			processCurrentSymbol('(');
			compileExpression();
			processCurrentSymbol(')'); //expression pushes past into the next token
			
			//write the statements block
			processCurrentSymbol('{');
			compileStatements();
			processCurrentSymbol('}');
			
			tabs--;
			writeTabs();
			writer.write("</whileStatement>\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem when compiling while statement");
		}
	}
	//"do" subRoutineCall ';'
	//it says to process this as "do" expression ';'
	void compileDo()//11
	{
		try {
			writeTabs();
			writer.write("<doStatement>\n");
			tabs++;
			
			//tokenizer should be pointed at the do
			printToken("keyword", "do");
			
			//compile the expression
			compileExpression();
			
			//process the last semicolon
			processCurrentSymbol(';');
			
			tabs--;
			writeTabs();
			writer.write("</doStatement>\n");
			
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem when compiling do statement");
		}
	}
	//"return" expression? ';'
	void compileReturn()//11
	{
		try {
			writeTabs();
			writer.write("<returnStatement>\n");
			tabs++;
			
			//return has already been read
			printToken("keyword", "return");
			
			//check if the next part is a symbol, if not, assume it is an expression
			//this means that expression will start at the first token of the expression
			if (tokenizer.symbol() != ';')
			{
				compileExpression();
			}
			
			processCurrentSymbol(';');
			
			tabs--;
			writeTabs();
			writer.write("</returnStatement>\n");
			
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem when compiling return statement");
		} 
	}
	
	//term (op term)*
	void compileExpression() //11
	{
		try {
		writeTabs();
		writer.write("<expression>\n");
		tabs++;
		
		//it will already start on the first term
		compileTerm();
		
		//make a hash table for all the ops cuz theres no way
		HashSet<Character> ops = new HashSet<Character>();
		ops.add('+');
		ops.add('-');
		ops.add('*');
		ops.add('/');
		ops.add('&');
		ops.add('|');
		ops.add('<');
		ops.add('>');
		ops.add('=');
		
		//check for the ops
		while (ops.contains(tokenizer.symbol()))
		{
			//handle <, >, and =
			/*if(tokenizer.symbol() == '<')
				printToken("symbol", "&lt");
			else if (tokenizer.symbol() == '>')
				printToken("symbol", "&gt");
			else if (tokenizer.symbol() == '=')
				printToken("symbol", "&eq");
			else */
				printToken("symbol", Character.toString(tokenizer.symbol()));
			
			//compile the next term as well
			compileTerm();
		}
		
		//this means compileExpression is going to end one after the expression
		//this also means i have to go fix everything else
		//i hate this assignment
		//i got to like 800 lines of code before deciding the way the book handles this is stupid
		//introducing the numbers at the end of the method declarations
		
		tabs--;
		writeTabs();
		writer.write("</expression>\n");
		
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem when compiling return statement");
		}
		
	}
	
	void compileTerm()
	{
		try {
			writeTabs();
			writer.write("<term>\n");
			tabs++;
			
			String type = tokenizer.tokenType();
			if (type.equals("INT_CONST"))
				printToken("integerConstant", Integer.toString(tokenizer.intVal()));
			else if (type.equals("STRING_CONST"))
				printToken("stringConstant", tokenizer.stringVal());
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
					printToken("keywordConstants", tokenizer.keyword().toLowerCase());
			}
			else if (type.equals("IDENTIFIER"))
			{
				//variables
				processCurrentIdentifier();
				
				//if the variable is an array
				if (tokenizer.symbol() == '[')
				{
					printToken("symbol", "[");
					compileExpression();
					processCurrentSymbol(']');
				}
				//if the variable is a subroutine call
				else 
				{
					//only come in here if not an array
					char symbol = tokenizer.symbol();
					if (symbol == '(')
					{
						printToken("symbol", "(");
						compileExpressionList();
						processCurrentSymbol(')');
						//force entry into the next block
						symbol = '.';
					}
					else if (symbol == '.')
					{
						processCurrentSymbol('.'); //catch imposters (if entry was forced but it doesn't belong here)
						processCurrentIdentifier();
						processCurrentSymbol('(');
						compileExpressionList();
						processCurrentSymbol(')');
					}
				}
			}
			else if (type.equals("SYMBOL"))
			{
				if (tokenizer.symbol() == '(')
				{
					printToken("symbol", "(");
					compileExpression();
					processCurrentSymbol(')');
				}
				else if (tokenizer.symbol() == '-' || tokenizer.symbol() == '~')
				{
					printToken("symbol", Character.toString(tokenizer.symbol()));
					compileTerm();
				}
			}
			//those are the only 5 options, anything else and i think i give up coding forever haha jk
			//forreal tho
				
			tabs--;
			writeTabs();
			writer.write("</term>\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("there was a problem compiling term");
		} 
	}
	
	//(expression (',' expression)*)?
	int compileExpressionList()
	{
		int expressions = 0; //count the amount of expressions found
		//increment every time an expression is compiled
		
		try {
			writeTabs();
			writer.write("<expressionList>\n");
			tabs++;
		
			//all expression lists are closed with ), so we will leverage that
			while (tokenizer.symbol() != ')')
			{
				compileExpression();
				expressions++;
				
				//if it is a comma here, it must be followed by an expression, so it will loop back
				//if not, it better be a close paren cuz if not we have an infinite loop until there is
				if (tokenizer.symbol() == ',')
					printToken("symbol", ",");
			}
		
			tabs--;
			writeTabs();
			writer.write("</expressionList>\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("there was a problem compiling expression list");
		}
		
		return expressions;
	}
	
	void close()
	{
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem closing the output file");
		}
	}
	//a true homie function, let him be
	private void writeTabs() //10
	{
		for (int i = 0; i < tabs; i++)
		{
			try {
				writer.write("  ");
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("problem when printing tabs in xml");
			}
		}		
	}
	
	//this kinda just does exactly what you tell it, it's neither digit for the first but ig its 10
	//for all intents and purposes
	//but we can just make it advance at the end to make it 11
	private void printToken(String type, String token)//11
	{
		try {
			writeTabs();
			writer.write("<" + type + "> ");
			writer.write(token + " ");
			writer.write("</" + type + ">\n");
			if (tokenizer.hasMoreTokens())
				tokenizer.advance();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Problem printing token");
		}
	}
	
	private void processCurrentKeyword(String keyword)//11
	{
		try {
			//ensure the type is keyword
			if (!tokenizer.tokenType().equals("KEYWORD"))
				throw new TypeException("KEYWORD", tokenizer.tokenType());
			
			//the word itself
			if (!tokenizer.keyword().equals(keyword.toUpperCase()))
				throw new TypeException(keyword.toUpperCase(), tokenizer.keyword());
			
			printToken("keyword", keyword);
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("Error: Expected type " + te.expected + "but got " + te.actual);
		}
	}
	
	private void processCurrentIdentifier()//11
	{
		try {
			//ensure the type is identifier
			if (!tokenizer.tokenType().equals("IDENTIFIER"))
				throw new TypeException("IDENTIFIER", tokenizer.tokenType());
			
			printToken("identifier", tokenizer.identifier());
			
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("Error: Expected type " + te.expected + "but got " + te.actual);
		}
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
			printToken("symbol", Character.toString(symbol));
			
		} catch (SymbolException se) {
			se.printStackTrace();
			System.out.println("Error: Expected symbol " + se.expected + "but got " + se.actual);
			
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("Error: Expected type " + te.expected + "but got " + te.actual);
		}
	}
	
	private void processCurrentType() //11
	{
		try {
			//make sure its a keyword
			String type = tokenizer.tokenType();
			if (type.equals("KEYWORD"))
			{
				String keyword = tokenizer.keyword();
				if (!(keyword.equals("INT") || keyword.equals("BOOLEAN") || keyword.equals("CHAR")))
					throw new TypeException("INT/BOOLEAN/CHAR", keyword);
				else
					printToken("keyword", keyword.toLowerCase());
			}
			//if not, assume its a class
			else if (type.equals("IDENTIFIER"))
			{
				printToken("identifier", tokenizer.identifier());
			}
			//anything else, throw an exception
			else
				throw new TypeException("KEYWORD/IDENTIFIER", tokenizer.tokenType());
			
		} catch (TypeException te) {
			te.printStackTrace();
			System.out.println("Error: Expected type " + te.expected + "but got " + te.actual);
		}
	}
}
