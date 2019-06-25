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

import io.katana.compiler.analysis.Types;
import io.katana.compiler.eval.RuntimeUniverse;
import io.katana.compiler.platform.TargetTriple;
import io.katana.compiler.sema.decl.SemaDecl;
import io.katana.compiler.sema.decl.SemaDeclFunction;
import io.katana.compiler.sema.decl.SemaDeclFunctionDef;
import io.katana.compiler.sema.stmt.SemaStmtLabel;
import io.katana.compiler.visitor.IVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

import java.util.IdentityHashMap;

import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("unused")
public class DeclCodegen extends IVisitor
{
	private static final DeclCodegen INSTANCE = new DeclCodegen();

	public static void generate(SemaDecl decl, TargetTriple target, RuntimeUniverse ctx, String binaryClassName, ClassWriter writer)
	{
		INSTANCE.invokeSelf(decl, target, ctx, binaryClassName, writer);
	}

	private void visit(SemaDeclFunctionDef decl, TargetTriple target, RuntimeUniverse universe, String binaryClassName, ClassWriter writer)
	{
		var name = JvmNameMangling.of(decl);
		var md = Descriptors.ofMethod(decl, universe);
		var methodVisitor = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, name, md, null, null);

		var startLabel = new Label();
		var endLabel = new Label();

		var currentIndex = 0;
		var paramIndices = new IdentityHashMap<SemaDeclFunction.Param, Integer>();

		for(var param : decl.fixedParams)
			if(!Types.isZeroSized(param.type))
			{
				methodVisitor.visitParameter(param.name, 0);
				paramIndices.put(param, currentIndex++);

				var paramType = universe.classOf(param.type);

				if(paramType == long.class || paramType == double.class)
					++currentIndex;
			}

		var varIndices = new IdentityHashMap<SemaDeclFunctionDef.Variable, Integer>();

		for(var var : decl.variables)
			if(!Types.isZeroSized(var.type))
			{
				var descriptor = Descriptors.ofType(var.type, universe);
				methodVisitor.visitLocalVariable(var.name, descriptor, null, startLabel, endLabel, var.index);
				varIndices.put(var, currentIndex++);

				var varType = universe.classOf(var.type);

				if(varType == long.class || varType == double.class)
					++currentIndex;
			}

		var labels = new IdentityHashMap<SemaStmtLabel, Label>();

		for(var label : decl.labels.entrySet())
			labels.put(label.getValue(), new Label());

		methodVisitor.visitCode();
		methodVisitor.visitLabel(startLabel);

		var ctx = new MethodContext(target, universe, labels, paramIndices, varIndices, binaryClassName, writer, methodVisitor);

		for(var stmt : decl.body)
			StmtCodegen.generate(stmt, ctx);

		methodVisitor.visitLabel(endLabel);
		methodVisitor.visitMaxs(0, 0);
		methodVisitor.visitEnd();
	}
}
