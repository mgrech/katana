module io.katana.compiler
{
	requires com.github.rvesse.airline;
	requires jtoml;
	requires org.objectweb.asm;

	exports io.katana.compiler.cli.cmd to com.github.rvesse.airline;
	exports io.katana.compiler.eval.exports;
}
