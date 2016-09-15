// Copyright 2016 Markus Grech
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

package katana;

import java.io.InputStream;
import java.util.Properties;

public class Version
{
	public static final String GROUP_ID = "katana";
	public static final String ARTIFACT_ID = "katana";
	public static final String AUTHOR = "Markus Grech";

	public static String asString()
	{
		try {
			InputStream is = Version.class.getClassLoader().getResourceAsStream(String.format("META-INF/maven/%s/%s/pom.properties", GROUP_ID, ARTIFACT_ID));

			if (is != null) {
				Properties properties = new Properties();
				properties.load(is);

				return properties.getProperty("version", "development");
			} else {
				return "development";
			}
		} catch (Exception ex) {
			return "SomethingIsVeryWrong-Version";
		}
	}
}
