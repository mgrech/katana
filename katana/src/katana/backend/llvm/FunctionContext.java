package katana.backend.llvm;

public class FunctionContext
{
	public int nextTemporary()
	{
		return temporaryCounter++;
	}

	private int temporaryCounter = 1;
}
