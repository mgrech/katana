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
import io.katana.compiler.platform.TargetTriple;
import io.katana.compiler.sema.decl.SemaDeclFunction;
import io.katana.compiler.sema.decl.SemaDeclFunctionDef;
import io.katana.compiler.sema.stmt.SemaStmtLabel;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.Map;

public class MethodContext
{
	public final TargetTriple target;
	public final RuntimeUniverse universe;
	public final Map<SemaStmtLabel, Label> labels;
	public final Map<SemaDeclFunction.Param, Integer> paramIndices;
	public final Map<SemaDeclFunctionDef.Variable, Integer> varIndices;
	public final String binaryClassName;
	public final ClassWriter writer;
	public final MethodVisitor visitor;

	public MethodContext(TargetTriple target, RuntimeUniverse universe,
	                     Map<SemaStmtLabel, Label> labels, Map<SemaDeclFunction.Param, Integer> paramIndices,
	                     Map<SemaDeclFunctionDef.Variable, Integer> varIndices, String binaryClassName,
	                     ClassWriter writer, MethodVisitor visitor)
	{
		this.target = target;
		this.universe = universe;
		this.labels = labels;
		this.paramIndices = paramIndices;
		this.varIndices = varIndices;
		this.binaryClassName = binaryClassName;
		this.writer = writer;
		this.visitor = visitor;
	}
}
