//a struct for holding all the data that goes into the symbol table
//the name is the key of the table's hashmap
//this is the value
public class TableData {
	String type;
	String kind;
	int index;
	
	TableData(String t, String k, int i)
	{
		type = t;
		kind = k;
		index = i;
	}
}
