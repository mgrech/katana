// Copyright 2017 Markus Grech
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

package katana.diag;

public class StackTrace
{
	private final StackTraceElement[] trace;

	private StackTrace(StackTraceElement[] trace)
	{
		this.trace = trace;
	}

	public static StackTrace get()
	{
		// http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6375302
		return new StackTrace(new Exception().getStackTrace());
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();

		for(StackTraceElement element : trace)
		{
			builder.append(String.format("at %s.%s", element.getClassName(), element.getMethodName()));

			if(element.isNativeMethod())
				builder.append(" (native)");
			else
				builder.append(String.format(" (%s:%s)", element.getFileName(), element.getLineNumber()));

			builder.append('\n');
		}

		return builder.toString();
	}
}
