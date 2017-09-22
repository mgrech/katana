module io.katana
{
	requires airline;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;

	exports katana.cli.cmd to airline;
	exports katana.project to com.fasterxml.jackson.databind;
}
