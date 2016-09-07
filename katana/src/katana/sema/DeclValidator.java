package katana.sema;

import katana.backend.PlatformContext;
import katana.sema.decl.Data;
import katana.sema.decl.ExternFunction;
import katana.sema.decl.Function;
import katana.sema.decl.Global;
import katana.visitor.IVisitor;

@SuppressWarnings("unused")
public class DeclValidator implements IVisitor
{
	private DeclValidator(Module module, PlatformContext context)
	{
		this.module = module;
		this.context = context;
	}

	private void visit(Data semaData, katana.ast.decl.Data data)
	{
		for(katana.ast.decl.Data.Field field : data.fields)
		{
			Type type = TypeLookup.find(module, field.type, null, null);

			if(!semaData.defineField(field.name, type))
				throw new RuntimeException("duplicate field '" + field.name + "' in data '" + semaData.name() + "'");
		}
	}

	private void visit(Function semaFunction, katana.ast.decl.Function function)
	{
		semaFunction.ret = function.ret.map((type) -> TypeLookup.find(module, type, null, null));

		for(katana.ast.decl.Function.Param param : function.params)
		{
			Type type = TypeLookup.find(module, param.type, null, null);

			if(!semaFunction.defineParam(param.name, type))
				throw new RuntimeException("duplicate parameter '" + param.name + "' in function '" + function.name + "'");
		}

		for(katana.ast.decl.Function.Local local : function.locals)
		{
			if(semaFunction.paramsByName.get(local.name) != null)
				throw new RuntimeException("redefinition of local '" + local.name + "'");

			Type type = TypeLookup.find(module, local.type, null, null);

			if(!semaFunction.defineLocal(local.name, type))
				throw new RuntimeException("duplicate local '" + local.name + "' in function '" + function.name + "'");
		}

		StmtValidator validator = new StmtValidator(semaFunction, context);

		for(katana.ast.Stmt stmt : function.body)
		{
			Stmt semaStmt = validator.validate(stmt);
			semaFunction.add(semaStmt);
		}

		validator.finalizeValidation();
	}

	private void visit(ExternFunction semaFunction, katana.ast.decl.ExternFunction function)
	{
		semaFunction.ret = function.ret.map((type) -> TypeLookup.find(module, type, null, null));

		for(katana.ast.decl.Function.Param param : function.params)
		{
			Type type = TypeLookup.find(module, param.type, null, null);

			if(!semaFunction.defineParam(type, param.name))
				throw new RuntimeException("duplicate parameter '" + param.name + "' in function '" + function.name + "'");
		}
	}

	private void visit(Global semaGlobal, katana.ast.decl.Global global)
	{
		semaGlobal.type = TypeLookup.find(module, global.type, null, null);
	}

	public static void apply(Decl semaDecl, katana.ast.Decl decl, Module module, PlatformContext context)
	{
		DeclValidator validator = new DeclValidator(module, context);
		semaDecl.accept(validator, decl);
	}

	private Module module;
	private PlatformContext context;
}
