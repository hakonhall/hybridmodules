module org.junit.jupiter.engine {
	requires org.apiguardian.api;
	requires org.junit.jupiter.api;
	requires org.junit.platform.commons;
	requires org.junit.platform.engine;
	requires org.opentest4j;

        // Need access to JupiterTestEngine
	exports org.junit.jupiter.engine;

	uses org.junit.jupiter.api.extension.Extension;

	provides org.junit.platform.engine.TestEngine
			with org.junit.jupiter.engine.JupiterTestEngine;

	opens org.junit.jupiter.engine.extension to org.junit.platform.commons;
}

// Original:
/*
 * Copyright 2015-2020 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
// ...
// module org.junit.jupiter.engine {
// 	requires org.apiguardian.api;
// 	requires org.junit.jupiter.api;
// 	requires org.junit.platform.commons;
// 	requires org.junit.platform.engine;
// 	requires org.opentest4j;

// 	// exports org.junit.jupiter.engine; // Constants...

// 	uses org.junit.jupiter.api.extension.Extension;

// 	provides org.junit.platform.engine.TestEngine
// 			with org.junit.jupiter.engine.JupiterTestEngine;

// 	opens org.junit.jupiter.engine.extension to org.junit.platform.commons;
// }
