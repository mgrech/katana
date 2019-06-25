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

import io.katana.compiler.BuiltinType;
import io.katana.compiler.ast.AstPath;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.eval.codegen.DeclCodegen;
import io.katana.compiler.platform.TargetTriple;
import io.katana.compiler.project.BuildOptions;
import io.katana.compiler.sema.SemaModule;
import io.katana.compiler.sema.decl.SemaDecl;
import io.katana.compiler.sema.decl.SemaDeclFunctionDef;
import io.katana.compiler.sema.expr.SemaExpr;
import io.katana.compiler.sema.expr.SemaExprLitBool;
import io.katana.compiler.sema.expr.SemaExprLitInt;
import io.katana.compiler.sema.stmt.SemaStmtReturn;
import io.katana.compiler.sema.type.SemaTypeBuiltin;
import io.katana.compiler.utils.Maybe;
import io.katana.compiler.utils.Rethrow;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class Evaluator
{
	private static SemaExpr toLiteral(Object value, BuiltinType expectedType)
	{
		if(value instanceof Boolean)
			return SemaExprLitBool.of((Boolean)value);

		if(value instanceof Byte)
			return new SemaExprLitInt(BigInteger.valueOf((byte)value), expectedType);

		if(value instanceof Short)
			return new SemaExprLitInt(BigInteger.valueOf((short)value), expectedType);

		if(value instanceof Integer)
			return new SemaExprLitInt(BigInteger.valueOf((int)value), expectedType);

		if(value instanceof Long)
			return new SemaExprLitInt(BigInteger.valueOf((long)value), expectedType);

		// TODO: float/double
		throw new AssertionError("invalid value: " + value);
	}

	private static SemaDecl createEvalFunction(SemaExpr expr, RuntimeUniverse universe)
	{
		var module = new SemaModule("", new AstPath(), null);
		var decl = new SemaDeclFunctionDef(module, null, universe.uniqueMemberName());
		decl.returnType = expr.type();
		decl.body = List.of(new SemaStmtReturn(Maybe.some(expr)));
		return decl;
	}

	public static SemaExpr eval(SemaExpr expr, TargetTriple target, RuntimeUniverse universe, BuildOptions options)
	{
		var expectedType = ((SemaTypeBuiltin)expr.type()).which;

		var className = universe.uniqueClassName();
		var binaryClassName = className.replace('.', '/');

		var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		var flags = ACC_PUBLIC | ACC_STATIC | ACC_FINAL | ACC_STRICT;
		writer.visit(V12, flags, binaryClassName, null, "java/lang/Object", null);

		var evalMethod = createEvalFunction(expr, universe);
		DeclCodegen.generate(evalMethod, target, universe, binaryClassName, writer);

		writer.visitEnd();
		var bytes = writer.toByteArray();

		if(options.evalDumpClassFiles)
		{
			try
			{
				Files.write(Paths.get(className + ".class"), bytes);
			}
			catch(IOException ex)
			{
				throw new RuntimeException(ex);
			}
		}

		var clazz = RuntimeClassLoader.defineClass(className, bytes);

		try
		{
			var value = clazz.getMethod(evalMethod.name()).invoke(null);
			return toLiteral(value, expectedType);
		}
		catch(InvocationTargetException ex)
		{
			var tex = ex.getTargetException();

			if(tex instanceof CompileException)
				throw (CompileException)tex;
			else
			{
				Rethrow.of(tex);
				throw new AssertionError("unreachable");
			}
		}
		catch(ReflectiveOperationException ex)
		{
			throw new RuntimeException(ex);
		}
	}
}
