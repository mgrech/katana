// Copyright 2016-2018 Markus Grech
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

package io.katana.compiler.backend;

import io.katana.compiler.sema.type.*;
import io.katana.compiler.visitor.IVisitor;

@SuppressWarnings("unused")
public class TypeNameMangling implements IVisitor
{
	private static final TypeNameMangling INSTANCE = new TypeNameMangling();

	private TypeNameMangling() {}

	public static String of(SemaType type)
	{
		return (String)type.accept(INSTANCE);
	}

	private String visit(SemaTypeSlice slice)
	{
		return String.format("slice-%s", of(slice.type));
	}

	private String visit(SemaTypeArray array)
	{
		return String.format("array-%s-%s", array.length, of(array.type));
	}

	private String visit(SemaTypeBuiltin builtin)
	{
		return builtin.which.toString().toLowerCase();
	}

	private String visit(SemaTypeConst const_)
	{
		return String.format("const-%s", of(const_.type));
	}

	private String visit(SemaTypeFunction function)
	{
		throw new AssertionError("unreachable");
	}

	private String visit(SemaTypeNonNullablePointer pointer)
	{
		return String.format("pointer-%s", of(pointer.type));
	}

	private String visit(SemaTypeNullablePointer pointer)
	{
		return String.format("npointer-%s", of(pointer.type));
	}

	private String visit(SemaTypeStruct user)
	{
		return user.decl.qualifiedName().toString();
	}

	private String visit(SemaTypeTuple tuple)
	{
		var builder = new StringBuilder();
		builder.append("tuple-");

		builder.append(tuple.types.size());

		for(var type : tuple.types)
		{
			builder.append('-');
			builder.append(of(type));
		}

		return builder.toString();
	}
}