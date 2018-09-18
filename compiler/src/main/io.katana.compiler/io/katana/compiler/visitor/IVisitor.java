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

import java.lang.reflect.Method;
import java.util.IdentityHashMap;

public abstract class IVisitor<R>
{
	private static final IdentityHashMap<Class<?>, Method[]> CLASS_TO_VISITS = new IdentityHashMap<>();

	private final Method[] visits = CLASS_TO_VISITS.computeIfAbsent(getClass(), ReflectionUtils::findVisitMethods);

	@SuppressWarnings("unchecked")
	protected R invokeSelf(Object... args)
	{
		return (R)ReflectionUtils.resolveAndInvoke(visits, this, args);
	}
}
