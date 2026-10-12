package com.hookrelay.hookrelay;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "hookrelay.worker.enabled=false")
class HookrelayApplicationTests {

	static {
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
	}

	@Test
	void contextLoads() {
	}
}