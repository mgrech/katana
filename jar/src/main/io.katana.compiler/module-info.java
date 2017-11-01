module io.katana.compiler
{
	requires airline;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;

	exports io.katana.compiler.cli.cmd to airline;
	exports io.katana.compiler.project to com.fasterxml.jackson.databind;
}
