package karn.minecraftsnap.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TextTemplateResolverTest {
	@Test
	void formatUiDisablesItalicStyle() {
		var resolver = new TextTemplateResolver();
		var text = resolver.formatUi("&a테스트");

		assertEquals("테스트", text.getString());
		assertFalse(Boolean.TRUE.equals(text.getStyle().isItalic()));
	}
}
