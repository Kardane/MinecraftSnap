package karn.minecraftsnap.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BossBarFormatterTest {
	@Test
	void placeholderFormatted() {
		assertEquals(
			"레드 5 | ⌛08:05 | 다음 라인 공개: 03:00 | 블루 7",
			BossBarFormatter.format("레드 {red_score} | ⌛{time} | 다음 라인 공개: {next_reveal_time} | 블루 {blue_score}", 485, 5, 7, "03:00")
		);
	}
}
