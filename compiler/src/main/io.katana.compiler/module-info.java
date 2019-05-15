module io.katana.compiler
{
	requires com.github.rvesse.airline;
	requires jtoml;

	exports io.katana.compiler.cli.cmd to com.github.rvesse.airline;
}
