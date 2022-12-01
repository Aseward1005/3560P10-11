import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class JackCompiler {

	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);
		
		System.out.println("Enter a .jack file name:");
		String filename = input.nextLine();
		input.close();
		
		File in = new File(filename);
		
		//create the compiler(s)
		//make compilers into a list so it's iterable
		ArrayList<CompilationEngine> compilers = new ArrayList<CompilationEngine>();
		File compiled; //make a destination file to assign later
		
		String destinationName = "";
		if (in.isFile()) //if the thing passed in is a file
		{
			//construct the output file name
			destinationName = filename.substring(0, filename.indexOf(".jack")) + ".vm";
			compiled = new File(destinationName);
			
			//add this file to the list of things to be compiled
			compilers.add(new CompilationEngine(in, compiled));
		}
		else //if the thing passed in is a directory
		{
			for (File f : in.listFiles()) //check all the files
			{
				if (f.getName().endsWith(".jack")) //for all the jack files, do what we did above
				{
					//construct the output file
					filename = f.getAbsolutePath();
					destinationName = filename.substring(0, filename.indexOf(".jack")) + ".vm";
					compiled = new File(destinationName);
					
					//add this file to the list of things to be compiled
					compilers.add(new CompilationEngine(f, compiled));
				}
			}
		}
		
		for (CompilationEngine compiler : compilers)
		{
			compiler.compileClass();
			compiler.close();
		}
		
		System.out.println("done");
	}

}
