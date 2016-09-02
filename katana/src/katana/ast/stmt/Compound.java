package katana.ast.stmt;

import katana.ast.Stmt;

import java.util.ArrayList;
import java.util.List;

public class Compound extends Stmt
{
	public List<Stmt> body = new ArrayList<>();
}
