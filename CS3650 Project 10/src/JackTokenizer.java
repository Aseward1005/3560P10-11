import java.util.Scanner;
import java.io.*;
import java.util.HashSet;
import java.util.LinkedList;

//tokenizer for .jack compiler
public class JackTokenizer {
	File file = null;
	Scanner reader = null;
	String token = "";
	String curr = null;
	
	LinkedList<String> tokenQueue = new LinkedList<String>();
	
	HashSet<String> keywords = new HashSet<String>();
	HashSet<Character> symbols = new HashSet<Character>();
	
	//last couple of projects i did this as the string for some reason
	//this time im just doing the file
	//i'd say i'm gonna go back and fix the last ones
	//but i wont if i'm being honest
	JackTokenizer(File input)
	{
		try {
			file = input;
			reader = new Scanner(file);
		} catch (FileNotFoundException e) {
			System.out.println("The file could not be found");
			e.printStackTrace();
		}
		
		//tokenize the input
		//reader.tokens();
		
		//initialize the keywords hashset
		initializeKeywords();
		//initialize the symbols hashset
		initializeSymbols();
		
	}
	
	boolean hasMoreTokens()
	{
		return (reader.hasNext() || !tokenQueue.isEmpty());
	}
	
	void advance()
	{
		//ensure there are no symbols
		//if (curr.contains(symbolre))
		//hmm ok different idea.
		//have one string be the token read from the file
		//have a second string (curr) be the part of the token that you want to read
		//make that a QUEUE of strings. Make a queue of strings for every token.
		
		//jump over whitespace and comments
		//if the queue is empty, build the queue and grab the first element
		if (tokenQueue.isEmpty())
		{
			//System.out.println("empty queue");
			token = reader.next();
		
			//skip line comments
			while (token.startsWith("//"))
			{
				reader.nextLine();
				//grab the first token of the next line
				token = reader.next(); 
			}
		
			//skip general comments
			if (token.startsWith("/*"))
			{
				//progress all the way to the end of the comment
				while (!(token.endsWith("*/")))
				{
					token = reader.next();
				}
			
				//skip past to right after the end of the comment
				token = reader.next();
			}
			//System.out.println(token);
			//create the token queue
			createQueue();
		}
		
		//grab the current element
		curr = tokenQueue.poll();
		//System.out.println(curr);
	}
	
	String tokenType()
	{
		String type = null;
		
		if (keywords.contains(curr))
			type = "KEYWORD";
		else if (symbols.contains(curr.charAt(0)))
			type = "SYMBOL";
		else if (curr.charAt(0) == '"')
			type = "STRING_CONST";
		else if (curr.matches("\\d+"))
			type = "INT_CONST";
		else
			type = "IDENTIFIER";
		
		//System.out.println("type: " + type);
		return type;
	}
	
	String keyword()
	{
		return curr.toUpperCase();
	}
	
	char symbol()
	{
		return curr.charAt(0);
	}
	
	String identifier()
	{
		return curr;
	}
	
	int intVal()
	{
		return Integer.parseInt(curr);
	}
	
	String stringVal()
	{
		return curr.substring(1, curr.length() - 1);
	}
	
	private void initializeKeywords()
	{
		keywords.add("class");
		keywords.add("constructor");
		keywords.add("function");
		keywords.add("method");
		keywords.add("field");
		keywords.add("static");
		keywords.add("var");
		keywords.add("int");
		keywords.add("char");
		keywords.add("boolean");
		keywords.add("void");
		keywords.add("true");
		keywords.add("false");
		keywords.add("null");
		keywords.add("this");
		keywords.add("let");
		keywords.add("do");
		keywords.add("if");
		keywords.add("else");
		keywords.add("while");
		keywords.add("return");
	}
	
	private void initializeSymbols()
	{
		symbols.add('{');
		symbols.add('}');
		symbols.add('(');
		symbols.add(')');
		symbols.add('[');
		symbols.add(']');
		symbols.add('.');
		symbols.add(',');
		symbols.add(';');
		symbols.add('+');
		symbols.add('-');
		symbols.add('*');
		symbols.add('/');
		symbols.add('&');
		symbols.add('|');
		symbols.add('<');
		symbols.add('>');
		symbols.add('=');
		symbols.add('~');
	}
	
	//need to fix string constants.
	//use a boolean and a not to alternate between not tracking and tracking strings
	//when tracking strings, ignore symbols
	//the whole string should be a token.
	//godspeed soldier, I would hurt you more than help you right now or else I'd start writing code.
	
	//update: who woulda thought, tired anthony wasn't on his a game
	//the dude forgot that all string constants will START with "
	//if the current token starts with ", use different logic, where you look for the last "
	
	//update: tired anthony wins again, just because it starts with " doesn't mean that will be the first character
	private void createQueue()
	{
		//for parsing string constants
		boolean parsingString = false;
		/*
		if (token.contains("\""))
		{
			//first parse everything before the "
			int parseTo = token.indexOf('"');
		}
		else*/
		//{
			//2 pointers method
			int start = 0;
			//int end = 0; //end point is tracked by i
			for (int i = 0; i < token.length(); i++)
			{
				if (token.charAt(i) == '"')
				{
					//System.out.println(token);
					//this first if handles the case where both quotations are in the same string
					if (parsingString) //if you were parsing a string and just read the last quote
						tokenQueue.add(token.substring(start, i)); //add to the queue the quote
					else //otherwise, set the start to this quote being read 
						start = i;
					parsingString = !parsingString; //either way, flip the parsing string flag
				}
				//if the current character is a symbol
				//and you are not parsing a string already
				if (symbols.contains(token.charAt(i)) && !parsingString)
				{
					//add everything before the symbol, but not if that is an empty string
					if ((i - start) > 0)
					tokenQueue.add(token.substring(start, i));
					//add the symbol itself
					tokenQueue.add(Character.toString(token.charAt(i)));
					//update the start point
					start = i+1;
				}
			}
			
			//if after parsing that whole token you are still parsing a string
			if (parsingString)
			{
				//first set the current string being read to the part of the string that has been read so far
				//start should be the first quote read
				String current = token.substring(start);
				
				//then set the delimiter to a quotation and read to that point, and append that last quote
				reader.useDelimiter("\"");
				current += reader.next() + '"';
				tokenQueue.add(current);
				
				//then advance past said quote
				reader.useDelimiter(".");
				reader.next();
				
				//then reset the delimiter to whitespace and hopefully its business as usual from there?
				reader.reset();
			}
			//if the last element was not a symbol and we aren't parsing a string
			else if (start < token.length())
			{
				//add the last chunk of the token to the queue
				tokenQueue.add(token.substring(start));
			}
		//}
	}
}
