package karn.minecraftsnap.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;

public class TextTemplateResolver {
	public Text format(String template) {
		return format(template, Map.of());
	}

	public Text format(String template, Map<String, String> placeholders) {
		return formatInternal(template, placeholders, false);
	}

	public Text formatUi(String template) {
		return formatUi(template, Map.of());
	}

	public Text formatUi(String template, Map<String, String> placeholders) {
		return formatInternal(template, placeholders, true);
	}

	private Text formatInternal(String template, Map<String, String> placeholders, boolean disableItalic) {
		var replaced = template;
		for (var entry : placeholders.entrySet()) {
			replaced = replaced.replace(entry.getKey(), entry.getValue());
		}

		return parseLegacyColors(replaced, disableItalic);
	}

	private Text parseLegacyColors(String input, boolean disableItalic) {
		MutableText root = Text.empty();
		Style currentStyle = Style.EMPTY;
		StringBuilder buffer = new StringBuilder();

		for (int i = 0; i < input.length(); i++) {
			char current = input.charAt(i);
			if (current == '&' && i + 1 < input.length()) {
				var formatting = Formatting.byCode(input.charAt(i + 1));
				if (formatting != null) {
					flush(root, buffer, currentStyle);
					currentStyle = formatting == Formatting.RESET
						? Style.EMPTY
						: currentStyle.withFormatting(formatting);
					if (disableItalic) {
						currentStyle = currentStyle.withItalic(false);
					}
					i++;
					continue;
				}
			}

			buffer.append(current);
		}

		flush(root, buffer, disableItalic ? currentStyle.withItalic(false) : currentStyle);
		return root;
	}

	private void flush(MutableText root, StringBuilder buffer, Style style) {
		if (buffer.isEmpty()) {
			return;
		}

		root.append(Text.literal(buffer.toString()).setStyle(style));
		buffer.setLength(0);
	}
}
