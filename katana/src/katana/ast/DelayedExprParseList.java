// Copyright 2016-2017 Markus Grech
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

package katana.ast;

import katana.ast.expr.AstExpr;
import katana.ast.expr.AstExprOpInfixList;
import katana.ast.expr.AstExprOpPostfixSeq;
import katana.ast.expr.AstExprOpPrefixSeq;

import java.util.IdentityHashMap;
import java.util.function.Consumer;

public class DelayedExprParseList
{
	public IdentityHashMap<AstExprOpPrefixSeq, Consumer<AstExpr>> prefixSeqs = new IdentityHashMap<>();
	public IdentityHashMap<AstExprOpInfixList, Consumer<AstExpr>> infixLists = new IdentityHashMap<>();
	public IdentityHashMap<AstExprOpPostfixSeq, Consumer<AstExpr>> postfixSeqs = new IdentityHashMap<>();
}
