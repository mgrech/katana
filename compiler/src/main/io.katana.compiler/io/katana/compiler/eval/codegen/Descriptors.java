// Copyright 2019 Markus Grech
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

package io.katana.compiler.eval.codegen;

import io.katana.compiler.eval.RuntimeUniverse;
import io.katana.compiler.sema.decl.SemaDeclFunction;
import io.katana.compiler.sema.type.SemaType;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Descriptors
{
	public static String ofType(Class<?> clazz)
	{
		return Type.getDescriptor(clazz);
	}

	public static String ofType(SemaType type, RuntimeUniverse ctx)
	{
		return ofType(ctx.classOf(type));
	}

	public static String ofMethod(SemaDeclFunction decl, RuntimeUniverse ctx)
	{
		var paramTypes = decl.fixedParams.stream()
		                                 .map(p -> p.type)
		                                 .map(ctx::classOf)
		                                 .collect(Collectors.toList());

		if(decl.isVariadic)
		{
			var allParamTypes = new ArrayList<>(paramTypes);
			allParamTypes.add(Object[].class);
			paramTypes = allParamTypes;
		}

		return ofMethodType(ctx.classOf(decl.returnType), paramTypes.toArray(new Class<?>[0]));
	}

	public static String ofMethodType(Class<?> returnClass, Class<?>... paramClasses)
	{
		var paramTypes = Arrays.stream(paramClasses)
		                       .map(Type::getType)
		                       .toArray(Type[]::new);

		return Type.getMethodDescriptor(Type.getType(returnClass), paramTypes);
	}
}
