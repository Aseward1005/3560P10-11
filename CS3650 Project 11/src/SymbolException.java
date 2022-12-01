
public class SymbolException extends Exception{
	char expected;
	char actual;
	
	SymbolException(char e, char a)
	{
		expected = e;
		actual = a;
	}
}