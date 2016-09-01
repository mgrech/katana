package katana.backend.llvm;

public class FunctionContext
{
	public String allocateSSA()
	{
		return "%" + ssaCounter++;
	}

	private int ssaCounter = 1;
}
