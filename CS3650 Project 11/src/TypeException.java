
public class TypeException extends Exception{
	String expected = "";
	String actual = "";
	
	TypeException(String e, String a)
	{
		expected = e;
		actual = a;
	}
}