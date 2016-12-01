package katana.analysis;

import katana.BuiltinType;
import katana.backend.PlatformContext;
import katana.sema.expr.SemaExprCast;
import katana.sema.type.SemaType;

public class CastValidator
{
	private static boolean isValidSignCast(SemaType sourceType, SemaType targetType, PlatformContext context)
	{
		if(!TypeHelper.isIntegerType(sourceType) || !TypeHelper.isIntegerType(targetType))
			return false;

		return sourceType.sizeof(context).equals(sourceType.sizeof(context));
	}

	private static boolean isOneOf(SemaType type, BuiltinType... builtinTypes)
	{
		for(BuiltinType builtinType : builtinTypes)
			if(TypeHelper.isBuiltinType(type, builtinType))
				return true;

		return false;
	}

	private static boolean isValidWidenCast(SemaType sourceType, SemaType targetType, PlatformContext context)
	{
		if(isOneOf(sourceType, BuiltinType.INT, BuiltinType.UINT, BuiltinType.PINT, BuiltinType.UPINT)
		|| isOneOf(targetType, BuiltinType.INT, BuiltinType.UINT, BuiltinType.PINT, BuiltinType.UPINT))
			return false;

		if(TypeHelper.isIntegerType(sourceType) && TypeHelper.isIntegerType(targetType))
			return sourceType.sizeof(context).compareTo(targetType.sizeof(context)) != 1;

		return TypeHelper.isFloatingPointType(sourceType) && TypeHelper.isBuiltinType(targetType, BuiltinType.FLOAT64);
	}

	private static boolean isValidNarrowCast(SemaType sourceType, SemaType targetType, PlatformContext context)
	{
		if(isOneOf(sourceType, BuiltinType.INT, BuiltinType.UINT, BuiltinType.PINT, BuiltinType.UPINT)
		|| isOneOf(targetType, BuiltinType.INT, BuiltinType.UINT, BuiltinType.PINT, BuiltinType.UPINT))
			return false;

		if(TypeHelper.isIntegerType(sourceType) && TypeHelper.isIntegerType(targetType))
			return sourceType.sizeof(context).compareTo(targetType.sizeof(context)) != -1;

		if(TypeHelper.isBuiltinType(sourceType, BuiltinType.FLOAT32) && TypeHelper.isBuiltinType(targetType, BuiltinType.FLOAT64))
			return false;

		return TypeHelper.isFloatingPointType(sourceType) && TypeHelper.isFloatingPointType(targetType);
	}

	public static boolean isValidCast(SemaType sourceType, SemaType targetType, SemaExprCast.Kind kind, PlatformContext context)
	{
		switch(kind)
		{
		case SIGN_CAST:   return isValidSignCast(sourceType, targetType, context);
		case WIDEN_CAST:  return isValidWidenCast(sourceType, targetType, context);
		case NARROW_CAST: return isValidNarrowCast(sourceType, targetType, context);
		default: break;
		}

		throw new AssertionError("unreachable");
	}
}
