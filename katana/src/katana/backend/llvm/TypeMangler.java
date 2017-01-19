// Copyright 2016-2017 Markus Grech
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

package katana.backend.llvm;

import katana.sema.type.*;
import katana.visitor.IVisitor;

@SuppressWarnings("unused")
public class TypeMangler implements IVisitor
{
	private static final TypeMangler MANGLER = new TypeMangler();

	private TypeMangler() {}

	public static String mangle(SemaType type)
	{
		return (String)type.accept(MANGLER);
	}

	private String visit(SemaTypeBuiltin builtin)
	{
		return builtin.which.toString().toLowerCase();
	}

	private String visit(SemaTypeConst const_)
	{
		return String.format("const-%s", mangle(const_.type));
	}

	private String visit(SemaTypeArray array)
	{
		return String.format("array-%s-%s", array.length, mangle(array.type));
	}

	private String visit(SemaTypeOpaque opaque)
	{
		return String.format("opaque-%s-%s", opaque.size, opaque.alignment);
	}

	private String visit(SemaTypeUserDefined user)
	{
		return user.decl.qualifiedName().toString();
	}

	private String visit(SemaTypeFunction function)
	{
		throw new AssertionError("unreachable");
	}

	private String visit(SemaTypeNullablePointer pointer)
	{
		return String.format("npointer-%s", mangle(pointer.type));
	}

	private String visit(SemaTypePointer pointer)
	{
		return String.format("pointer-%s", mangle(pointer.type));
	}
}
