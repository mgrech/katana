package katana.platform;

public class TargetTriple
{
	public final Arch arch;
	public final Vendor vendor;
	public final Os os;
	public final Environment env;

	public TargetTriple(Arch arch, Vendor vendor, Os os, Environment env)
	{
		this.arch = arch;
		this.vendor = vendor;
		this.os = os;
		this.env = env;
	}

	@Override
	public String toString()
	{
		if(env == Environment.UNKNOWN)
			return String.format("%s-%s-%s", arch, vendor, os);

		return String.format("%s-%s-%s-%s", arch, vendor, os, env);
	}

	public static final TargetTriple NATIVE = detectNativeTarget();

	private static TargetTriple detectNativeTarget()
	{
		Arch arch = Arch.UNKNOWN;
		Vendor vendor = Vendor.UNKNOWN;
		Os os = Os.UNKNOWN;
		Environment env = Environment.UNKNOWN;

		String osArch = System.getProperty("os.arch").toLowerCase();
		String osName = System.getProperty("os.name").toLowerCase();

		if(osArch.equals("x86"))
			arch = Arch.X86;
		else if(osArch.equals("amd64"))
			arch = Arch.AMD64;

		if(osName.startsWith("windows"))
		{
			vendor = Vendor.PC;
			os = Os.WINDOWS;
			env = Environment.MSVC;
		}

		else if(osName.startsWith("mac"))
		{
			vendor = Vendor.APPLE;
			os = Os.MACOS;
		}

		else if(osName.startsWith("linux"))
		{
			os = Os.LINUX;
			env = Environment.GNU;
		}

		return new TargetTriple(arch, vendor, os, env);
	}
}
