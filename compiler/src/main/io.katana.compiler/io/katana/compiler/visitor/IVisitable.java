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

package io.katana.compiler.visitor;

import io.katana.compiler.utils.Rethrow;

import java.lang.reflect.InvocationTargetException;

public interface IVisitable
{
	default Object accept(IVisitor visitor, Object... args)
	{
		var classes = new Class[args.length + 1];
		classes[0] = getClass();

		for(var i = 0; i != args.length; ++i)
			classes[i + 1] = args[i] == null ? null : args[i].getClass();

		var invoke = new Object[args.length + 1];
		invoke[0] = this;
		System.arraycopy(args, 0, invoke, 1, args.length);

		try
		{
			var visit = ReflectionUtils.findMatchingMethod(visitor.getClass(), "visit", classes);
			visit.setAccessible(true);
			return visit.invoke(visitor, invoke);
		}
		catch(IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
		catch(InvocationTargetException e)
		{
			Rethrow.of(e.getTargetException());
		}

		throw new AssertionError("unreachable");
	}
}
