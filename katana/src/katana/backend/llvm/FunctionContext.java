package katana.backend.llvm;

public class FunctionContext
{
	public String allocateSSA()
	{
		return "%" + ssaCounter++;
	}

	public GeneratedLabel allocateLabel(String name)
	{
		return new GeneratedLabel(String.format("gl$%s$%s", name, labelCounter++));
	}

	private int ssaCounter = 1;
	private int labelCounter = 1;
}
