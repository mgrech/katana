package katana.sema.stmt;

import katana.sema.Stmt;

import java.util.ArrayList;
import java.util.List;

public class Compound extends Stmt
{
	public List<Stmt> body = new ArrayList<>();
}
