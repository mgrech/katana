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

import io.katana.compiler.Builtin;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.eval.exports.Builtins;
import io.katana.compiler.sema.expr.*;
import io.katana.compiler.sema.type.SemaTypeBuiltin;
import io.katana.compiler.visitor.IVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import java.util.EnumMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("unused")
public class ExprCodegen extends IVisitor
{
	private static final ExprCodegen INSTANCE = new ExprCodegen();

	private static final Map<Builtin, Map<Class<?>, Integer>> OPCODES_BY_BUILTIN_AND_TYPE = new EnumMap<>(Builtin.class);

	static
	{
		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.NEG, Map.of(byte.class,   INEG,
		                                                    short.class,  INEG,
		                                                    int.class,    INEG,
		                                                    long.class,   LNEG,
		                                                    float.class,  FNEG,
		                                                    double.class, DNEG));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.ADD, Map.of(byte.class,   IADD,
		                                                    short.class,  IADD,
		                                                    int.class,    IADD,
		                                                    long.class,   LADD,
		                                                    float.class,  FADD,
		                                                    double.class, DADD));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.SUB, Map.of(byte.class,   ISUB,
		                                                    short.class,  ISUB,
		                                                    int.class,    ISUB,
		                                                    long.class,   LSUB,
		                                                    float.class,  FSUB,
		                                                    double.class, DSUB));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.MUL, Map.of(byte.class,   IMUL,
		                                                    short.class,  IMUL,
		                                                    int.class,    IMUL,
		                                                    long.class,   LMUL,
		                                                    float.class,  FMUL,
		                                                    double.class, DMUL));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.DIV, Map.of(byte.class,   IDIV,
		                                                    short.class,  IDIV,
		                                                    int.class,    IDIV,
		                                                    long.class,   LDIV,
		                                                    float.class,  FDIV,
		                                                    double.class, DDIV));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.REM, Map.of(byte.class,   IREM,
		                                                    short.class,  IREM,
		                                                    int.class,    IREM,
		                                                    long.class,   LREM,
		                                                    float.class,  FREM,
		                                                    double.class, DREM));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.DIV_POW2, Map.of(byte.class,  ISHR,
		                                                         short.class, ISHR,
		                                                         int.class,   ISHR,
		                                                         long.class,  LSHR));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.AND, Map.of(byte.class,  IAND,
		                                                    short.class, IAND,
		                                                    int.class,   IAND,
		                                                    long.class,  LAND));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.OR, Map.of(byte.class,  IOR,
		                                                   short.class, IOR,
		                                                   int.class,   IOR,
		                                                   long.class,  LOR));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.XOR, Map.of(byte.class,  IXOR,
		                                                    short.class, IXOR,
		                                                    int.class,   IXOR,
		                                                    long.class,  LXOR));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.SHL, Map.of(byte.class,  ISHL,
		                                                   short.class,  ISHL,
		                                                   int.class,    ISHL,
		                                                   long.class,   LSHL));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.SHR, Map.of(byte.class,  IUSHR,
		                                                    short.class, IUSHR,
		                                                    int.class,   IUSHR,
		                                                    long.class,  LUSHR));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.CMP_EQ, Map.of(byte.class,  IF_ICMPEQ,
		                                                       short.class, IF_ICMPEQ,
		                                                       int.class,   IF_ICMPEQ,
		                                                       long.class,  IFEQ));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.CMP_NEQ, Map.of(byte.class,  IF_ICMPNE,
		                                                        short.class, IF_ICMPNE,
		                                                        int.class,   IF_ICMPNE,
		                                                        long.class,  IFNE));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.CMP_LT, Map.of(byte.class,  IF_ICMPLT,
		                                                       short.class, IF_ICMPLT,
		                                                       int.class,   IF_ICMPLT,
		                                                       long.class,  IFLT));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.CMP_LTE, Map.of(byte.class,  IF_ICMPLE,
		                                                        short.class, IF_ICMPLE,
		                                                        int.class,   IF_ICMPLE,
		                                                        long.class,  IFLT));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.CMP_GT, Map.of(byte.class,  IF_ICMPGT,
		                                                       short.class, IF_ICMPGT,
		                                                       int.class,   IF_ICMPGT,
		                                                       long.class,  IFGT));

		OPCODES_BY_BUILTIN_AND_TYPE.put(Builtin.CMP_GTE, Map.of(byte.class,  IF_ICMPGE,
		                                                        short.class, IF_ICMPGE,
		                                                        int.class,   IF_ICMPGE,
		                                                        long.class,  IFGE));
	}

	private ExprCodegen() {}

	public static void generate(SemaExpr expr, MethodContext ctx)
	{
		INSTANCE.invokeSelf(expr, ctx);
	}

	private void visit(SemaExprArrayIndexAccess expr, MethodContext ctx)
	{
		throw new RuntimeException("wtf");
	}

	private void visit(SemaExprAssign expr, MethodContext ctx)
	{
		ExprCodegen.generate(expr.right, ctx);
	}

	private void visit(SemaExprBuiltinCall expr, MethodContext ctx)
	{
		for(var argExpr : expr.argExprs)
			generate(argExpr, ctx);

		var arg0Type = expr.argExprs.isEmpty() ? null : (SemaTypeBuiltin)expr.argExprs.get(0).type();
		var mappedArg0Type = arg0Type == null ? null : ctx.universe.classOf(arg0Type);
		var opcode = OPCODES_BY_BUILTIN_AND_TYPE.get(expr.builtin).get(mappedArg0Type);

		switch(expr.builtin)
		{
		case NEG, ADD, SUB, MUL, DIV, REM, DIV_POW2, AND, OR, XOR, SHL, SHR:
			ctx.visitor.visitInsn(opcode);
			break;

		case NOT:
			ctx.visitor.visitLdcInsn(mappedArg0Type == long.class ? Long.valueOf(-1) : Integer.valueOf(-1));
			ctx.visitor.visitInsn(OPCODES_BY_BUILTIN_AND_TYPE.get(Builtin.XOR).get(mappedArg0Type));
			break;

		case CMP_EQ, CMP_NEQ, CMP_LT, CMP_LTE, CMP_GT, CMP_GTE:
			if(mappedArg0Type != long.class)
			{
				var trueLabel = new Label();
				var endLabel = new Label();
				ctx.visitor.visitJumpInsn(opcode, trueLabel);
				ctx.visitor.visitInsn(ICONST_0);
				ctx.visitor.visitJumpInsn(GOTO, endLabel);
				ctx.visitor.visitLabel(trueLabel);
				ctx.visitor.visitInsn(ICONST_1);
				ctx.visitor.visitLabel(endLabel);
			}
			else
			{
				var trueLabel = new Label();
				var endLabel = new Label();
				ctx.visitor.visitInsn(LCMP);
				ctx.visitor.visitJumpInsn(opcode, trueLabel);
				ctx.visitor.visitInsn(ICONST_0);
				ctx.visitor.visitJumpInsn(GOTO, endLabel);
				ctx.visitor.visitLabel(trueLabel);
				ctx.visitor.visitInsn(ICONST_1);
				ctx.visitor.visitLabel(endLabel);
			}

			break;

		case CLZ, CTZ, POPCNT:
			var className = Type.getInternalName(Builtins.class);
			var methodName = expr.builtin.name().toLowerCase();
			var desc = Descriptors.ofMethodType(mappedArg0Type, mappedArg0Type);
			ctx.visitor.visitMethodInsn(INVOKESTATIC, className, methodName, desc, false);
			break;

		case MEMCPY, MEMMOVE, MEMSET:
			throw new CompileException("unsupported builtin: " + expr.builtin.sourceName);
		}
	}

	private void visit(SemaExprDirectFunctionCall expr, MethodContext ctx)
	{
		var binaryClassName = ctx.universe.binaryClassNameOf(expr.function);

		if(binaryClassName == null)
		{
			binaryClassName = ctx.binaryClassName;
			ctx.universe.declare(expr.function, ctx.binaryClassName);
			DeclCodegen.generate(expr.function, ctx.target, ctx.universe, ctx.binaryClassName, ctx.writer);
		}

		for(var argExpr : expr.args)
			invokeSelf(argExpr, ctx);

		var methodName = JvmNameMangling.of(expr.function);
		var desc = Descriptors.ofMethod(expr.function, ctx.universe);
		ctx.visitor.visitMethodInsn(INVOKESTATIC, binaryClassName, methodName, desc, false);
	}

	private static int loadInstructionForType(Class<?> clazz)
	{
		if(clazz == boolean.class || clazz == byte.class || clazz == short.class || clazz == int.class)
			return ILOAD;

		if(clazz == long.class)
			return LLOAD;

		if(clazz == float.class)
			return FLOAD;

		if(clazz == double.class)
			return DLOAD;

		return ALOAD;
	}

	private static int arrayLoadInstructionForType(Class<?> clazz)
	{
		if(clazz == boolean.class || clazz == byte.class)
			return BALOAD;

		if(clazz == short.class)
			return SALOAD;

		if(clazz == int.class)
			return IALOAD;

		if(clazz == long.class)
			return LALOAD;

		if(clazz == float.class)
			return FALOAD;

		if(clazz == double.class)
			return DALOAD;

		return AALOAD;
	}

	private void visit(SemaExprImplicitConversionLValueToRValue expr, MethodContext ctx)
	{
		if(expr.nestedExpr instanceof SemaExprNamedParam)
		{
			var param = ((SemaExprNamedParam)expr.nestedExpr).decl;
			var mappedIndex = ctx.paramIndices.get(param);
			var mappedType = ctx.universe.classOf(param.type);
			var instr = loadInstructionForType(mappedType);
			ctx.visitor.visitVarInsn(instr, mappedIndex);
		}
		else if(expr.nestedExpr instanceof SemaExprNamedVar)
		{
			var var = ((SemaExprNamedVar)expr.nestedExpr).decl;
			var mappedIndex = ctx.varIndices.get(var);
			var mappedType = ctx.universe.classOf(var.type);
			var instr = loadInstructionForType(mappedType);
			ctx.visitor.visitVarInsn(instr, mappedIndex);
		}
		else if(expr.nestedExpr instanceof SemaExprArrayIndexAccess)
		{
			var accessExpr = (SemaExprArrayIndexAccess)expr.nestedExpr;
			generate(new SemaExprImplicitConversionLValueToRValue(accessExpr.arrayExpr), ctx);
			generate(accessExpr.indexExpr, ctx);
			var mappedType = ctx.universe.classOf(accessExpr.type());
			var instr = arrayLoadInstructionForType(mappedType);
			ctx.visitor.visitInsn(instr);
		}
		else
			throw new AssertionError("nyi: " + expr.nestedExpr);
	}

	private static void generateNewArray(Class<?> clazz, MethodContext ctx)
	{
		if(clazz == boolean.class)
			ctx.visitor.visitIntInsn(NEWARRAY, T_BOOLEAN);
		else if(clazz == byte.class)
			ctx.visitor.visitIntInsn(NEWARRAY, T_BYTE);
		else if(clazz == short.class)
			ctx.visitor.visitIntInsn(NEWARRAY, T_SHORT);
		else if(clazz ==  int.class)
			ctx.visitor.visitIntInsn(NEWARRAY, T_INT);
		else if(clazz ==  long.class)
			ctx.visitor.visitIntInsn(NEWARRAY, T_LONG);
		else if(clazz ==  float.class)
			ctx.visitor.visitIntInsn(NEWARRAY, T_FLOAT);
		else if(clazz ==  double.class)
			ctx.visitor.visitIntInsn(NEWARRAY, T_DOUBLE);
		else
			ctx.visitor.visitTypeInsn(ANEWARRAY, Descriptors.ofType(clazz));
	}

	private static int arrayStoreInstructionForType(Class<?> clazz)
	{
		if(clazz == boolean.class || clazz == byte.class)
			return BASTORE;

		if(clazz == short.class)
			return SASTORE;

		if(clazz == int.class)
			return IASTORE;

		if(clazz == long.class)
			return LASTORE;

		if(clazz == float.class)
			return FASTORE;

		if(clazz == double.class)
			return DASTORE;

		return AASTORE;
	}

	private void visit(SemaExprLitArray expr, MethodContext ctx)
	{
		var elementClass = ctx.universe.classOf(expr.elementType);
		ctx.visitor.visitLdcInsn(expr.elementExprs.size());
		generateNewArray(elementClass, ctx);

		for(var i = 0; i != expr.elementExprs.size(); ++i)
		{
			ctx.visitor.visitInsn(DUP);
			ctx.visitor.visitLdcInsn(i);
			generate(expr.elementExprs.get(i), ctx);
			var instr = arrayStoreInstructionForType(elementClass);
			ctx.visitor.visitInsn(instr);
		}
	}

	private void visit(SemaExprLitBool expr, MethodContext ctx)
	{
		ctx.visitor.visitLdcInsn(expr.value);
	}

	private void visit(SemaExprLitInt expr, MethodContext ctx)
	{
		var value = switch(expr.type)
		{
			case INT8  -> (Object)expr.value.byteValue();
			case INT16 -> (Object)expr.value.shortValue();
			case INT32 -> (Object)expr.value.intValue();
			case INT64 -> (Object)expr.value.longValue();
			case INT   ->
				switch((int)ctx.target.arch.pointerSize)
				{
					case 1 -> (Object)expr.value.byteValue();
					case 2 -> (Object)expr.value.shortValue();
					case 4 -> (Object)expr.value.intValue();
					case 8 -> (Object)expr.value.longValue();
					default -> throw new AssertionError("unreachable");
				};
			default -> throw new AssertionError("unreachable");
		};

		ctx.visitor.visitLdcInsn(value);
	}
}
