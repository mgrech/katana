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

package io.katana.compiler.eval;

import io.katana.compiler.analysis.Types;
import io.katana.compiler.diag.TypeString;
import io.katana.compiler.platform.TargetTriple;
import io.katana.compiler.sema.decl.SemaDecl;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.sema.type.SemaTypeBuiltin;
import org.objectweb.asm.Type;

import java.util.IdentityHashMap;
import java.util.Map;

public class RuntimeUniverse
{
	private final Map<SemaType, Class<?>> typeMappings = new IdentityHashMap<>();
	private final Map<SemaDecl, String> classMap = new IdentityHashMap<>();
	private int uniqueClassNameCounter = 0;
	private int uniqueMethodNameCounter = 0;

	private Class<?> arrayOf(Class<?> clazz)
	{
		try
		{
			return Class.forName("[" + Type.getDescriptor(clazz));
		}
		catch(ClassNotFoundException ex)
		{
			throw new RuntimeException(ex);
		}
	}

	public RuntimeUniverse(TargetTriple target)
	{
		define(SemaTypeBuiltin.VOID, void.class);
		define(SemaTypeBuiltin.BOOL, boolean.class);
		define(SemaTypeBuiltin.BYTE, byte.class);
		define(SemaTypeBuiltin.INT8, byte.class);
		define(SemaTypeBuiltin.INT16, short.class);
		define(SemaTypeBuiltin.INT32, int.class);
		define(SemaTypeBuiltin.INT64, long.class);
		define(SemaTypeBuiltin.UINT8, byte.class);
		define(SemaTypeBuiltin.UINT16, short.class);
		define(SemaTypeBuiltin.UINT32, int.class);
		define(SemaTypeBuiltin.UINT64, long.class);
		define(SemaTypeBuiltin.FLOAT32, float.class);
		define(SemaTypeBuiltin.FLOAT64, double.class);

		var intType = typeMappings.get(SemaTypeBuiltin.ofSignedInteger(target.arch.pointerSize));
		define(SemaTypeBuiltin.INT, intType);
		define(SemaTypeBuiltin.UINT, intType);
	}

	public String uniqueClassName()
	{
		uniqueMethodNameCounter = 0;
		return String.format("%s.$%s", RuntimeUniverse.class.getPackageName(), uniqueClassNameCounter++);
	}

	public String uniqueMemberName()
	{
		return String.format("$%s", uniqueMethodNameCounter++);
	}

	public void declare(SemaDecl decl, String className)
	{
		classMap.put(decl, className);
	}

	public String binaryClassNameOf(SemaDecl decl)
	{
		return classMap.get(decl);
	}

	public void define(SemaType type, Class<?> clazz)
	{
		typeMappings.put(type, clazz);
	}

	public Class<?> classOf(SemaType type)
	{
		if(Types.isArray(type))
			return arrayOf(classOf(Types.removeArray(type)));

		return typeMappings.get(type);
	}
}
