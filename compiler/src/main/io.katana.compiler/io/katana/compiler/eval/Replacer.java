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

import io.katana.compiler.platform.TargetTriple;
import io.katana.compiler.project.BuildOptions;
import io.katana.compiler.sema.SemaProgram;
import io.katana.compiler.sema.expr.*;
import io.katana.compiler.sema.visitor.Visitors;
import io.katana.compiler.visitor.IVisitor;

@SuppressWarnings("unused")
public class Replacer extends IVisitor<SemaExpr>
{
	private final TargetTriple target;
	private final RuntimeUniverse universe;
	private final BuildOptions options;

	private Replacer(TargetTriple target, RuntimeUniverse universe, BuildOptions options)
	{
		this.target = target;
		this.universe = universe;
		this.options = options;
	}

	public static void apply(SemaProgram program, TargetTriple target, BuildOptions options)
	{
		var visitor = new Replacer(target, new RuntimeUniverse(target), options);
		Visitors.transformExprsFlat(program, visitor);
	}

	private SemaExpr visit(SemaExprAddressof expr)
	{
		expr.pointeeExpr = invokeSelf(expr.pointeeExpr);
		return expr;
	}

	private SemaExpr visit(SemaExprAlignofExpr expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprAlignofType expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprArrayGetLength expr)
	{
		expr.arrayExpr = invokeSelf(expr.arrayExpr);
		return expr;
	}

	private SemaExpr visit(SemaExprArrayGetPointer expr)
	{
		return new SemaExprArrayGetPointer(invokeSelf(expr.arrayExpr));
	}

	private SemaExpr visit(SemaExprArrayGetSlice expr)
	{
		return new SemaExprArrayGetSlice(invokeSelf(expr.arrayExpr));
	}

	private SemaExpr visit(SemaExprArrayIndexAccess expr)
	{
		expr.arrayExpr = invokeSelf(expr.arrayExpr);
		expr.indexExpr = invokeSelf(expr.indexExpr);
		return expr;
	}

	private SemaExpr visit(SemaExprAssign expr)
	{
		expr.left = invokeSelf(expr.left);
		expr.right = invokeSelf(expr.right);
		return expr;
	}

	private SemaExpr visit(SemaExprBuiltinCall expr)
	{
		for(var i = 0; i != expr.argExprs.size(); ++i)
			expr.argExprs.set(i, invokeSelf(expr.argExprs.get(i)));

		return expr;
	}

	private SemaExpr visit(SemaExprCast expr)
	{
		expr.nestedExpr = invokeSelf(expr.nestedExpr);
		return expr;
	}

	private SemaExpr visit(SemaExprConst expr)
	{
		expr.nestedExpr = invokeSelf(expr.nestedExpr);
		return expr;
	}

	private SemaExpr visit(SemaExprDeref expr)
	{
		expr.pointerExpr = invokeSelf(expr.pointerExpr);
		return expr;
	}

	private SemaExpr visit(SemaExprDirectFunctionCall expr)
	{
		for(var i = 0; i != expr.args.size(); ++i)
			expr.args.set(i, invokeSelf(expr.args.get(i)));

		return expr;
	}

	private SemaExpr visit(SemaExprEval eval)
	{
		return Evaluator.eval(eval.nestedExpr, target, universe, options);
	}

	private SemaExpr visit(SemaExprFieldAccess expr)
	{
		expr.structExpr = invokeSelf(expr.structExpr);
		return expr;
	}

	private SemaExpr visit(SemaExprImplicitConversionArrayPointerToByteSlice expr)
	{
		return new SemaExprImplicitConversionArrayPointerToByteSlice(invokeSelf(expr.nestedExpr), expr.targetType);
	}

	private SemaExpr visit(SemaExprImplicitConversionArrayPointerToPointer expr)
	{
		return new SemaExprImplicitConversionArrayPointerToPointer(invokeSelf(expr.nestedExpr));
	}

	private SemaExpr visit(SemaExprImplicitConversionArrayPointerToSlice expr)
	{
		return new SemaExprImplicitConversionArrayPointerToSlice(invokeSelf(expr.nestedExpr), expr.targetType);
	}

	private SemaExpr visit(SemaExprImplicitConversionLValueToRValue expr)
	{
		expr.nestedExpr = invokeSelf(expr.nestedExpr);
		return expr;
	}

	private SemaExpr visit(SemaExprImplicitConversionNonNullablePointerToNullablePointer expr)
	{
		return new SemaExprImplicitConversionNonNullablePointerToNullablePointer(invokeSelf(expr.nestedExpr), expr.targetType);
	}

	private SemaExpr visit(SemaExprImplicitConversionNullToNullablePointer expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprImplicitConversionNullToSlice expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprImplicitConversionPointerToBytePointer expr)
	{
		expr.nestedExpr = invokeSelf(expr.nestedExpr);
		return expr;
	}

	private SemaExpr visit(SemaExprImplicitConversionPointerToNonConstToPointerToConst expr)
	{
		expr.nestedExpr = invokeSelf(expr.nestedExpr);
		return expr;
	}

	private SemaExpr visit(SemaExprImplicitConversionSliceToByteSlice expr)
	{
		return new SemaExprImplicitConversionSliceToByteSlice(invokeSelf(expr.nestedExpr), expr.targetType);
	}

	private SemaExpr visit(SemaExprImplicitConversionSliceToSliceOfConst expr)
	{
		return new SemaExprImplicitConversionSliceToSliceOfConst(invokeSelf(expr.nestedExpr));
	}

	private SemaExpr visit(SemaExprImplicitConversionWiden expr)
	{
		expr.nestedExpr = invokeSelf(expr.nestedExpr);
		return expr;
	}

	private SemaExpr visit(SemaExprIndirectFunctionCall expr)
	{
		for(var i = 0; i != expr.argExprs.size(); ++i)
			expr.argExprs.set(i, invokeSelf(expr.argExprs.get(i)));

		return expr;
	}

	private SemaExpr visit(SemaExprLitArray expr)
	{
		for(var i = 0; i != expr.elementExprs.size(); ++i)
			expr.elementExprs.set(i, invokeSelf(expr.elementExprs.get(i)));

		return expr;
	}

	private SemaExpr visit(SemaExprLitBool expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprLitFloat expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprLitInt expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprLitNull expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprLitString expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprNamedFunc expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprNamedGlobal expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprNamedImportedOverloadSet expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprNamedOverloadSet expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprNamedParam expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprNamedRenamedImport expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprNamedVar expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprOffsetof expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprSizeofExpr expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprSizeofType expr)
	{
		return expr;
	}

	private SemaExpr visit(SemaExprSliceGetLength expr)
	{
		expr.sliceExpr = invokeSelf(expr.sliceExpr);
		return expr;
	}

	private SemaExpr visit(SemaExprSliceGetPointer expr)
	{
		return new SemaExprSliceGetPointer(invokeSelf(expr));
	}

	private SemaExpr visit(SemaExprSliceIndexAccess expr)
	{
		return new SemaExprSliceIndexAccess(invokeSelf(expr.sliceExpr), invokeSelf(expr.indexExpr));
	}
}
