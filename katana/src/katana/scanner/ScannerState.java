package katana.scanner;

public class ScannerState implements Cloneable
{
	public int line = 1;
	public int currentOffset = 0;
	public int tokenOffset = 0;
	public int currentColumn = 1;
	public int tokenColumn = 1;

	public Token token = Token.BEGIN;

	public ScannerState() {}

	private ScannerState(int line, int currentOffset, int tokenOffset, int currentColumn, int tokenColumn, Token token)
	{
		this.line = line;
		this.currentOffset = currentOffset;
		this.tokenOffset = tokenOffset;
		this.currentColumn = currentColumn;
		this.tokenColumn = tokenColumn;
		this.token = token;
	}

	@Override
	public ScannerState clone()
	{
		return new ScannerState(line, currentOffset, tokenOffset, currentColumn, tokenColumn, token);
	}
}
