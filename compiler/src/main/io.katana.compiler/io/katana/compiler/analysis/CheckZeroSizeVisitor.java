// Copyright 2017-2018 Markus Grech
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

package io.katana.compiler.analysis;

import io.katana.compiler.BuiltinType;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.sema.type.*;
import io.katana.compiler.visitor.IVisitor;

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
		return array.length == 0 || apply(array.type);
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

	private boolean visit(SemaTypeTuple tuple)
	{
		return tuple.layout.sizeof() == 0;
	}

	private boolean visit(SemaTypeStruct userDefined)
	{
		return Types.isZeroSized(userDefined.decl);
	}
}
