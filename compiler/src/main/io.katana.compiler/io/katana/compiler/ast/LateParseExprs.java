// Copyright 2016-2019 Markus Grech
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

package io.katana.compiler.ast;

import io.katana.compiler.ast.expr.AstExpr;
import io.katana.compiler.ast.expr.AstExprOpInfixList;
import io.katana.compiler.ast.expr.AstExprOpPostfixSeq;
import io.katana.compiler.ast.expr.AstExprOpPrefixSeq;

import java.util.IdentityHashMap;
import java.util.function.Consumer;

public class LateParseExprs implements Cloneable
{
	public IdentityHashMap<AstExprOpPrefixSeq, Consumer<AstExpr>> prefixSeqs = new IdentityHashMap<>();
	public IdentityHashMap<AstExprOpInfixList, Consumer<AstExpr>> infixLists = new IdentityHashMap<>();
	public IdentityHashMap<AstExprOpPostfixSeq, Consumer<AstExpr>> postfixSeqs = new IdentityHashMap<>();

	public LateParseExprs()
	{}

	private LateParseExprs(IdentityHashMap<AstExprOpPrefixSeq, Consumer<AstExpr>> prefixSeqs,
	                       IdentityHashMap<AstExprOpInfixList, Consumer<AstExpr>> infixLists,
	                       IdentityHashMap<AstExprOpPostfixSeq, Consumer<AstExpr>> postfixSeqs)
	{
		this.prefixSeqs = prefixSeqs;
		this.infixLists = infixLists;
		this.postfixSeqs = postfixSeqs;
	}

	@Override
	public LateParseExprs clone()
	{
		return new LateParseExprs(new IdentityHashMap<>(prefixSeqs), new IdentityHashMap<>(infixLists), new IdentityHashMap<>(postfixSeqs));
	}
}
