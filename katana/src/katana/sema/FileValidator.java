package katana.sema;

import katana.Maybe;
import katana.ast.Path;
import katana.backend.PlatformContext;
import katana.sema.decl.Data;
import katana.sema.decl.ExternFunction;
import katana.sema.decl.Function;
import katana.sema.decl.Global;
import katana.visitor.IVisitor;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class FileValidator implements IVisitor
{
	public FileValidator(PlatformContext context, Program program)
	{
		this.context = context;
		this.program = program;
	}

	public void visit(katana.ast.decl.Data data)
	{
		declsSeen = true;
		requireModule();

		Data semaData = new Data(currentModule, data.name);

		for(katana.ast.decl.Data.Field field : data.fields)
		{
			Type type = TypeLookup.find(currentModule, field.type, null, null);

			if(!semaData.defineField(field.name, type))
				throw new RuntimeException("duplicate field '" + field.name + "' in data '" + data.name + "'");
		}

		if(!currentModule.defineData(semaData))
			throw new RuntimeException("redefinition of symbol '" + data.name + "'");
	}

	public void visit(katana.ast.decl.ExternFunction function)
	{
		declsSeen = true;
		requireModule();

		Maybe<Type> ret = function.ret.map((type) -> TypeLookup.find(currentModule, type, null, null));
		ExternFunction semaFunction = new ExternFunction(currentModule, function.externName, function.name, ret);

		for(katana.ast.decl.Function.Param param : function.params)
		{
			Type type = TypeLookup.find(currentModule, param.type, null, null);

			if(!semaFunction.defineParam(type, param.name))
				throw new RuntimeException("duplicate parameter '" + param.name + "' in function '" + function.name + "'");
		}

		if(!currentModule.defineExternFunction(semaFunction))
			throw new RuntimeException("redefinition of symbol '" + function.name + "'");
	}

	public void visit(katana.ast.decl.Function function)
	{
		declsSeen = true;
		requireModule();

		Maybe<Type> ret = function.ret.map((type) -> TypeLookup.find(currentModule, type, null, null));
		Function semaFunction = new Function(currentModule, function.name, ret);

		for(katana.ast.decl.Function.Param param : function.params)
		{
			Type type = TypeLookup.find(currentModule, param.type, null, null);

			if(!semaFunction.defineParam(param.name, type))
				throw new RuntimeException("duplicate parameter '" + param.name + "' in function '" + function.name + "'");
		}

		for(katana.ast.decl.Function.Local local : function.locals)
		{
			if(semaFunction.paramsByName.get(local.name) != null)
				throw new RuntimeException("redefinition of local '" + local.name + "'");

			Type type = TypeLookup.find(currentModule, local.type, null, null);

			if(!semaFunction.defineLocal(local.name, type))
				throw new RuntimeException("duplicate local '" + local.name + "' in function '" + function.name + "'");
		}

		if(!currentModule.defineFunction(semaFunction))
			throw new RuntimeException("redefinition of symbol '" + function.name + "'");

		for(katana.ast.Stmt stmt : function.body)
		{
			Stmt semaStmt = StmtValidator.validate(stmt, semaFunction, context);
			semaFunction.add(semaStmt);
		}
	}

	public void visit(katana.ast.decl.Global global)
	{
		declsSeen = true;
		requireModule();

		Type type = TypeLookup.find(currentModule, global.type, null, null);
		Global semaGlobal = new Global(currentModule, global.name, type);

		if(!currentModule.defineGlobal(semaGlobal))
			throw new RuntimeException("redefinition of symbol '" + global.name + "'");
	}

	public void visit(katana.ast.decl.Import import_)
	{
		if(declsSeen)
			throw new RuntimeException("imports must go before other decls");

		if(!imports.add(import_.path))
			throw new RuntimeException("duplicate import");
	}

	public void visit(katana.ast.decl.Module module_)
	{
		declsSeen = true;
		currentModule = program.findOrCreateModule(module_.path);
	}

	public Set<Path> imports()
	{
		return imports;
	}

	private void requireModule()
	{
		if(currentModule == null)
			throw new RuntimeException("no module defined");
	}

	private PlatformContext context;
	private Program program;
	private Module currentModule = null;
	private boolean declsSeen = false;
	private Set<Path> imports = new HashSet<>();
}
