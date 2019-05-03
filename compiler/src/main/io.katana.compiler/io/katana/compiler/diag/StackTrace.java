// Copyright 2017-2019 Markus Grech
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

package io.katana.compiler.diag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StackTrace
{
	private final List<StackTraceElement> trace;

	private StackTrace(StackTraceElement[] trace)
	{
		this.trace = new ArrayList<>(Arrays.asList(trace));
	}

	public static StackTrace get()
	{
		// http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6375302
		var trace = new StackTrace(new Exception().getStackTrace());

		// trim this stack frame
		trace.trimInnermostFrame();

		trace.trace.removeIf((elem) -> elem.getClassName().contains("reflect") || elem.getClassName().contains("junit"));

		return trace;
	}

	public void trimInnermostFrame()
	{
		trace.remove(0);
	}

	@Override
	public String toString()
	{
		var builder = new StringBuilder();

		for(var element : trace)
		{
			builder.append(String.format("\tat %s.%s", element.getClassName(), element.getMethodName()));

			if(element.isNativeMethod())
				builder.append("(native)");
			else
				builder.append(String.format("(%s:%s)", element.getFileName(), element.getLineNumber()));

			builder.append('\n');
		}

		return builder.toString();
	}
}
