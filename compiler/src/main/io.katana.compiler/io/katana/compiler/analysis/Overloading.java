// Copyright 2018-2019 Markus Grech
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

import io.katana.compiler.Inlining;
import io.katana.compiler.ast.expr.AstExpr;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.diag.TypeString;
import io.katana.compiler.sema.decl.SemaDeclFunction;
import io.katana.compiler.sema.expr.SemaExpr;
import io.katana.compiler.sema.expr.SemaExprDirectFunctionCall;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.utils.Maybe;

import java.util.*;
import java.util.function.BiFunction;

public class Overloading
{
	private static boolean match(SemaDeclFunction function, List<AstExpr> args, List<Maybe<SemaExpr>> result,
	                             BiFunction<AstExpr, SemaType, SemaExpr> validate)
	{
		var failed = false;

		for(var i = 0; i != function.params.size(); ++i)
		{
			var paramType = function.params.get(i).type;
			var paramTypeNoConst = Types.removeConst(paramType);

			try
			{
				var arg = validate.apply(args.get(i), paramTypeNoConst);
				var argTypeNoConst = Types.removeConst(arg.type());

				if(!Types.equal(paramTypeNoConst, argTypeNoConst))
					failed = true;

				result.add(Maybe.some(arg));
			}
			catch(CompileException e)
			{
				failed = true;
				result.add(Maybe.none());
			}
		}

		return !failed;
	}

	private static void appendSignature(StringBuilder builder, SemaDeclFunction overload)
	{
		builder.append(overload.name());
		builder.append('(');

		if(!overload.params.isEmpty())
		{
			builder.append(TypeString.of(overload.params.get(0).type));

			for(var i = 1; i != overload.params.size(); ++i)
			{
				builder.append(", ");
				builder.append(TypeString.of(overload.params.get(i).type));
			}
		}

		builder.append(')');
	}

	private static void appendArg(StringBuilder builder, Maybe<SemaExpr> arg)
	{
		if(arg.isSome())
			builder.append(TypeString.of(arg.unwrap().type()));
		else
			builder.append("<deduction-failed>");
	}

	private static void appendArgs(StringBuilder builder, List<Maybe<SemaExpr>> args)
	{
		builder.append('(');

		if(!args.isEmpty())
		{
			appendArg(builder, args.get(0));

			for(var i = 1; i != args.size(); ++i)
			{
				builder.append(", ");
				appendArg(builder, args.get(i));
			}
		}

		builder.append(')');
	}

	private static void appendDeducedOverloads(StringBuilder builder, IdentityHashMap<SemaDeclFunction, List<Maybe<SemaExpr>>> overloads)
	{
		for(var entry : overloads.entrySet())
		{
			builder.append("\t\t");
			appendSignature(builder, entry.getKey());
			builder.append(" with arguments deduced to ");
			appendArgs(builder, entry.getValue());
			builder.append('\n');
		}
	}

	private static void appendOverloadInfo(StringBuilder builder, IdentityHashMap<SemaDeclFunction, List<Maybe<SemaExpr>>> failed, Set<SemaDeclFunction> invalidNumArgs)
	{
		if(!failed.isEmpty())
		{
			builder.append("\tmatching failed for the following overloads:\n");
			appendDeducedOverloads(builder, failed);
		}

		if(!invalidNumArgs.isEmpty())
		{
			builder.append("\tthe following overloads have a non-matching number of parameters:\n");

			for(var overload : invalidNumArgs)
			{
				builder.append("\t\t");
				appendSignature(builder, overload);
				builder.append('\n');
			}
		}
	}

	public static SemaExpr resolve(List<SemaDeclFunction> set, String name, List<AstExpr> args, Inlining inline,
	                               BiFunction<AstExpr, SemaType, SemaExpr> validate)
	{
		var candidates = new IdentityHashMap<SemaDeclFunction, List<Maybe<SemaExpr>>>();
		var failed = new IdentityHashMap<SemaDeclFunction, List<Maybe<SemaExpr>>>();
		var other = Collections.newSetFromMap(new IdentityHashMap<SemaDeclFunction, Boolean>());

		for(var overload : set)
		{
			if(overload.params.size() != args.size())
			{
				other.add(overload);
				continue;
			}

			var semaArgs = new ArrayList<Maybe<SemaExpr>>();

			if(match(overload, args, semaArgs, validate))
				candidates.put(overload, semaArgs);
			else
				failed.put(overload, semaArgs);
		}

		if(candidates.isEmpty())
		{
			var builder = new StringBuilder();
			builder.append(String.format("no matching function for call to '%s' out of %s overloads:\n", name, set.size()));
			appendOverloadInfo(builder, failed, other);
			throw new CompileException(builder.toString());
		}

		if(candidates.size() > 1)
		{
			var builder = new StringBuilder();
			builder.append(String.format("ambiguous call to function '%s', %s candidates:\n", name, candidates.size()));

			builder.append("\tmatching succeeded for the following overloads:\n");
			appendDeducedOverloads(builder, candidates);

			appendOverloadInfo(builder, failed, other);

			throw new CompileException(builder.toString());
		}

		var first = candidates.entrySet().iterator().next();

		var semaArgs = new ArrayList<SemaExpr>();

		for(var arg : first.getValue())
			semaArgs.add(arg.unwrap().asRValue());

		return new SemaExprDirectFunctionCall(first.getKey(), semaArgs, inline);
	}
}
