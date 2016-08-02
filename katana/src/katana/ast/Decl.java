package katana.ast;

public abstract class Decl
{
	protected Decl(boolean exported, boolean opaque)
	{
		this.exported = exported;
		this.opaque = opaque;
	}

	public boolean exported;
	public boolean opaque;

	@Override
	public String toString()
	{
		if(!exported && !opaque)
			return getClass().getName() + '\n';

		StringBuilder builder = new StringBuilder(" [");

		if(exported)
			builder.append("exported");

		if(opaque)
			builder.append(", opaque");

		builder.append("]");

		return getClass().getName() + builder.toString() + '\n';
	}
}
