package katana.ast.type;

import katana.ast.Type;

public class Builtin extends Type
{
	public enum Which
	{
		BOOL,
		INT8,
		INT16,
		INT32,
		INT64,
		INT,
		PINT,
		UINT8,
		UINT16,
		UINT32,
		UINT64,
		UINT,
		UPINT,
		FLOAT32,
		FLOAT64,
		PTR,
	}

	public static final Builtin BOOL    = new Builtin(Which.BOOL);
	public static final Builtin INT8    = new Builtin(Which.INT8);
	public static final Builtin INT16   = new Builtin(Which.INT16);
	public static final Builtin INT32   = new Builtin(Which.INT32);
	public static final Builtin INT64   = new Builtin(Which.INT64);
	public static final Builtin INT     = new Builtin(Which.INT);
	public static final Builtin PINT    = new Builtin(Which.PINT);
	public static final Builtin UINT8   = new Builtin(Which.UINT8);
	public static final Builtin UINT16  = new Builtin(Which.UINT16);
	public static final Builtin UINT32  = new Builtin(Which.UINT32);
	public static final Builtin UINT64  = new Builtin(Which.UINT64);
	public static final Builtin UINT    = new Builtin(Which.UINT);
	public static final Builtin UPINT   = new Builtin(Which.UPINT);
	public static final Builtin FLOAT32 = new Builtin(Which.FLOAT32);
	public static final Builtin FLOAT64 = new Builtin(Which.FLOAT64);
	public static final Builtin PTR     = new Builtin(Which.PTR);

	private Builtin(Which which)
	{
		this.which = which;
	}

	public final Which which;

	@Override
	public String toString()
	{
		return which.toString().toLowerCase();
	}
}
