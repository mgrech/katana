// Copyright 2017 Markus Grech
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package katana.analysis;

import katana.BuiltinType;
import katana.diag.CompileException;
import katana.sema.type.*;
import katana.visitor.IVisitor;

import java.math.BigInteger;

@SuppressWarnings("unused")
public class CheckZeroSizeVisitor implements IVisitor
{
	private static final CheckZeroSizeVisitor INSTANCE = new CheckZeroSizeVisitor();

	private CheckZeroSizeVisitor() {}

	public static boolean apply(SemaType type)
	{
		return (boolean)type.accept(INSTANCE);
	}

	private boolean visit(SemaTypeArray array)
	{
		return array.length.equals(BigInteger.ZERO) || apply(array.type);
	}

	private boolean visit(SemaTypeBuiltin builtin)
	{
		return builtin.which == BuiltinType.VOID || builtin.which == BuiltinType.NULL;
	}

	private boolean visit(SemaTypeConst const_)
	{
		return apply(const_.type);
	}

	private boolean visit(SemaTypeFunction function)
	{
		throw new CompileException("sizeof applied to function type");
	}

	private boolean visit(SemaTypeNonNullablePointer pointer)
	{
		return false;
	}

	private boolean visit(SemaTypeNullablePointer pointer)
	{
		return false;
	}

	private boolean visit(SemaTypeOpaque opaque)
	{
		return opaque.size.equals(BigInteger.ZERO);
	}

	private boolean visit(SemaTypeTuple tuple)
	{
		for(SemaType type : tuple.types)
			if(!apply(type))
				return false;

		return true;
	}

	private boolean visit(SemaTypeUserDefined userDefined)
	{
		return Types.isZeroSized(userDefined.decl);
	}
}
