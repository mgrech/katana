package katana;

public enum BuiltinType
{
	BOOL   (Kind.BOOL),
	INT8   (Kind.INT),
	INT16  (Kind.INT),
	INT32  (Kind.INT),
	INT64  (Kind.INT),
	INT    (Kind.INT),
	PINT   (Kind.INT),
	UINT8  (Kind.UINT),
	UINT16 (Kind.UINT),
	UINT32 (Kind.UINT),
	UINT64 (Kind.UINT),
	UINT   (Kind.UINT),
	UPINT  (Kind.UINT),
	FLOAT32(Kind.FLOAT),
	FLOAT64(Kind.FLOAT),
	PTR    (Kind.PTR);

	public enum Kind
	{
		BOOL,
		INT,
		UINT,
		FLOAT,
		PTR,
	}

	BuiltinType(Kind kind)
	{
		this.kind = kind;
	}

	public Kind kind;
}
