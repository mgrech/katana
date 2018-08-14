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

import io.katana.compiler.sema.type.*;
import io.katana.compiler.visitor.IVisitor;

import java.util.List;

@SuppressWarnings("unused")
public class TypesEqualVisitor implements IVisitor
{
	private static final TypesEqualVisitor INSTANCE = new TypesEqualVisitor();

	private TypesEqualVisitor() {}

	public static boolean apply(SemaType left, SemaType right)
	{
		if(left.getClass() != right.getClass())
			return false;

		return (boolean)left.accept(INSTANCE, right);
	}

	private boolean visit(SemaTypeSlice left, SemaTypeSlice right)
	{
		return apply(left.type, right.type);
	}

	private boolean visit(SemaTypeArray left, SemaTypeArray right)
	{
		return left.length == right.length && apply(left.type, right.type);
	}

	private boolean visit(SemaTypeBuiltin left, SemaTypeBuiltin right)
	{
		return left.which == right.which;
	}

	private boolean visit(SemaTypeConst left, SemaTypeConst right)
	{
		return apply(left.type, right.type);
	}

	private boolean visit(SemaTypeFunction left, SemaTypeFunction right)
	{
		if(!apply(left.ret, right.ret))
			return false;

		return typeListsEqual(left.params, right.params);
	}

	private boolean visit(SemaTypeNonNullablePointer left, SemaTypeNonNullablePointer right)
	{
		return apply(left.type, right.type);
	}

	private boolean visit(SemaTypeNullablePointer left, SemaTypeNullablePointer right)
	{
		return apply(left.type, right.type);
	}

	private boolean visit(SemaTypeStruct left, SemaTypeStruct right)
	{
		return left.decl == right.decl;
	}

	private boolean visit(SemaTypeTuple left, SemaTypeTuple right)
	{
		if(left.types.size() != right.types.size())
			return false;

		return typeListsEqual(left.types, right.types);
	}

	private static boolean typeListsEqual(List<SemaType> left, List<SemaType> right)
	{
		if(left.size() != right.size())
			return false;

		for(var i = 0; i != left.size(); ++i)
			if(!apply(left.get(i), right.get(i)))
				return false;

		return true;
	}
}
