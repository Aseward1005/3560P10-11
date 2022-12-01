import java.io.*;

public class VMWriter {

	FileWriter writer;
	
	VMWriter(File out)
	{
		try {
			writer = new FileWriter(out);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem initializing vmwriter");
		}
	}
	
	void writePush(String segment, int index)
	{
		try {
			writer.write("push " + segment.toLowerCase() + " " + index + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem writing push statement");
		}
	}
	
	void writePop(String segment, int index)
	{
		try {
			writer.write("pop " + segment.toLowerCase() + " " + index + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem writing pop statement");
		}
	}
	
	void writeArithmetic(String command)
	{
		try {
			writer.write(command + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem writing arithmetic statement");
		}
	}
	
	void writeLabel(String label)
	{
		try {
			writer.write("label " + label + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem writing label statement");
		}
	}
	
	void writeGoto(String label)
	{
		try {
			writer.write("goto " + label + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem writing goto statement");
		}
	}
	
	void writeIf(String label)
	{
		try {
			writer.write("if-goto " + label + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem writing if-goto statement");
		}
	}
	
	void writeCall(String name, int nArgs)
	{
		try {
			writer.write("call " + name + " " + nArgs + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem writing call statement");
		}
	}
	
	void writeFunction(String name, int nVars)
	{
		try {
			writer.write("function " + name + " " + nVars + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem writing function statement");
		}
	}
	
	void writeReturn() 
	{
		try {
			writer.write("return\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem writing return statement");
		}
	}
	
	void close()
	{
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("problem closing the file");
		}
	}
}
