module io.katana.compiler
{
	requires airline;
	requires jtoml;

	exports io.katana.compiler.cli.cmd to airline;
	exports io.katana.compiler.project.toml to jtoml;
}
