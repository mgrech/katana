// Copyright 2018 Markus Grech
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

import io.katana.compiler.Builtin;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.utils.Maybe;

import java.util.List;
import java.util.stream.IntStream;

public class OverloadedBuiltinDecl extends BuiltinDecl
{
	public static class Signature
	{
		public final List<SemaType> paramTypes;
		public final SemaType returnType;

		public Signature(List<SemaType> paramTypes, SemaType returnType)
		{
			this.paramTypes = paramTypes;
			this.returnType = returnType;
		}
	}

	public final List<Signature> signatures;

	public OverloadedBuiltinDecl(Builtin which, List<Signature> signatures)
	{
		super(which);
		this.signatures = signatures;
	}

	@Override
	public Maybe<SemaType> validateCall(List<SemaType> argTypes)
	{
		for(var signature : signatures)
		{
			if(signature.paramTypes.size() != argTypes.size())
				continue;

			if(!IntStream.range(0, argTypes.size())
			             .allMatch(i -> Types.equal(signature.paramTypes.get(i), Types.removeConst(argTypes.get(i)))))
				continue;

			return Maybe.some(signature.returnType);
		}

		return Maybe.none();
	}
}
