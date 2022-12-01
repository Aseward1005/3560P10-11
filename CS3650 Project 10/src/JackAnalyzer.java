import java.io.*;
import java.util.Scanner;

public class JackAnalyzer {

	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);
		
		System.out.println("Enter a .jack file name:");
		String filename = input.nextLine();
		input.close();
		
		File in = new File(filename);
		
		//the xml file needs to go into another folder.
		String destinationName = in.getParent();
		destinationName += "/Answers";
		
		//compile the full name of the final file name
		destinationName += "/" + in.getName().substring(0, in.getName().indexOf(".jack")) + ".xml";
		String tokenizedName = destinationName.substring(0, destinationName.indexOf(".xml")) + "T.xml";
		
		File tokens = new File(tokenizedName);
		File compiled = new File(destinationName);
		
		JackCompiler tokenCompiler = new JackCompiler(in, tokens);
		tokenCompiler.compileTokens();
		tokenCompiler.close();
		
		JackCompiler actualCompiler = new JackCompiler(in, compiled);
		actualCompiler.compileClass();
		actualCompiler.close();
		
		System.out.println("done");
	}

}
