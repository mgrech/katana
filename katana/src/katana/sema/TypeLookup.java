package katana.sema;

import katana.Maybe;
import katana.sema.decl.Data;
import katana.sema.type.*;
import katana.visitor.IVisitor;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class TypeLookup implements IVisitor
{
	private TypeLookup(Module currentModule)
	{
		this.currentModule = currentModule;
	}

	private Type visit(katana.ast.type.Builtin builtin)
	{
		switch(builtin.which)
		{
		case INT8:    return Builtin.INT8;
		case UINT8:   return Builtin.UINT8;
		case INT16:   return Builtin.INT16;
		case UINT16:  return Builtin.UINT16;
		case INT32:   return Builtin.INT32;
		case UINT32:  return Builtin.UINT32;
		case INT64:   return Builtin.INT64;
		case UINT64:  return Builtin.UINT64;
		case INT:     return Builtin.INT;
		case UINT:    return Builtin.UINT;
		case PINT:    return Builtin.PINT;
		case UPINT:   return Builtin.UPINT;
		case PTR:     return Builtin.PTR;
		case BOOL:    return Builtin.BOOL;
		case FLOAT32: return Builtin.FLOAT32;
		case FLOAT64: return Builtin.FLOAT64;

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private Type visit(katana.ast.type.Opaque opaque)
	{
		return new Opaque(opaque.size, opaque.alignment);
	}

	private Type visit(katana.ast.type.Array array)
	{
		return new Array(array.size, find(currentModule, array.type));
	}

	private Type visit(katana.ast.type.Function function)
	{
		ArrayList<Type> params = new ArrayList<>();

		for(katana.ast.Type param : function.params)
			params.add(find(currentModule, param));

		Maybe<Type> ret = function.ret.map((type) -> find(currentModule, type));

		return new Function(ret, params);
	}

	private Type visit(katana.ast.type.UserDefined user)
	{
		Maybe<Decl> decl = currentModule.findSymbol(user.name);

		if(decl.isNone())
			throw new RuntimeException("undeclared type '" + user.name + "'");

		if(!(decl.get() instanceof Data))
			throw new RuntimeException("symbol '" + user.name + "' is not a data");

		return new UserDefined((Data)decl.get());
	}

	private Module currentModule;

	public static Type find(Module currentModule, katana.ast.Type type)
	{
		TypeLookup translator = new TypeLookup(currentModule);
		return (Type)type.accept(translator);
	}
}
