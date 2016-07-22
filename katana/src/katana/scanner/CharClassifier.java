package katana.scanner;

public class CharClassifier
{
	public static boolean isWhitespace(int cp)
	{
		return cp == ' ' || cp == '\t' || cp == '\r';
	}

	public static boolean isLineBreak(int cp)
	{
		return cp == '\n';
	}

	public static boolean isDigit(int cp)
	{
		return cp >= '0' && cp <= '9';
	}

	public static boolean isHexDigit(int cp)
	{
		return isDigit(cp) || (cp >= 'a' && cp <= 'f') || (cp >= 'A' && cp <= 'F');
	}

	public static boolean isIdentifierStart(int cp)
	{
		return cp == '_' || Character.isLetter(cp);
	}

	public static boolean isIdentifierChar(int cp)
	{
		return isIdentifierStart(cp)
		    || Character.isDigit(cp)
		    || Character.getType(cp) == Character.NON_SPACING_MARK
		    || Character.getType(cp) == Character.COMBINING_SPACING_MARK;
	}
}
