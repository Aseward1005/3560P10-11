import java.util.HashMap;

public class SymbolTable {
	HashMap<String, TableData> table;
	HashMap<String, Integer> indices;
	
	SymbolTable()
	{
		indices = new HashMap<String, Integer>();
		reset();
	}
	
	void reset()
	{
		table = new HashMap<String, TableData>();
		indices.put("STATIC", 0);
		indices.put("FIELD", 0);
		indices.put("ARG", 0);
		indices.put("VAR", 0);
	}
	
	void define(String name, String type, String kind)
	{
		//add to the table
		TableData data = new TableData(type, kind, indices.get(kind));
		table.put(name, data);
		
		//increase the index of the kind
		indices.put(kind, (indices.get(kind) + 1));
	}
	
	int varCount(String kind)
	{
		return indices.get(kind);
	}
	
	String kindOf(String name)
	{
		if (table.containsKey(name))
			return table.get(name).kind;
		else
			return "NONE";
	}
	
	String typeOf(String name)
	{
		return table.get(name).type;
	}
	
	int indexOf(String name)
	{
		return table.get(name).index;
	}
}
