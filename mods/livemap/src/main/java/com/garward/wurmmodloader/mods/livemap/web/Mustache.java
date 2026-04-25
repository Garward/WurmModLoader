package com.garward.wurmmodloader.mods.livemap.web;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiny Mustache-subset substitution for the WurmMapGen index.html template.
 * Supports only what their template actually uses:
 * <ul>
 *   <li>{@code &#123;&#123;key&#125;&#125;} — variable substitution</li>
 *   <li>{@code &#123;&#123;#flag&#125;&#125;...&#123;&#123;/flag&#125;&#125;} — section, rendered when flag is truthy</li>
 *   <li>{@code &#123;&#123;^flag&#125;&#125;...&#123;&#123;/flag&#125;&#125;} — inverted section, rendered when flag is falsy</li>
 * </ul>
 * Truthy = non-null + not "false" + not empty string.
 */
public final class Mustache {

    private Mustache() {}

    public static String render(String template, Map<String, String> ctx) {
        String s = template;
        // Loop until stable so nested sections render fully — each pass peels
        // one nesting level, and the outer pass's output may expose new
        // sections that need rendering.
        for (int i = 0; i < 8; i++) {
            String before = s;
            s = renderSections(s, ctx, false);
            s = renderSections(s, ctx, true);
            if (s.equals(before)) break;
        }
        s = renderVariables(s, ctx);
        return s;
    }

    private static String renderSections(String template, Map<String, String> ctx, boolean inverted) {
        char marker = inverted ? '^' : '#';
        Pattern p = Pattern.compile(
                "\\{\\{" + marker + "(\\w+)\\}\\}([\\s\\S]*?)\\{\\{/\\1\\}\\}");
        Matcher m = p.matcher(template);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String body = m.group(2);
            boolean truthy = isTruthy(ctx.get(key));
            boolean keep = inverted ? !truthy : truthy;
            m.appendReplacement(out, Matcher.quoteReplacement(keep ? body : ""));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String renderVariables(String template, Map<String, String> ctx) {
        Pattern p = Pattern.compile("\\{\\{(\\w+)\\}\\}");
        Matcher m = p.matcher(template);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String val = ctx.getOrDefault(key, "");
            m.appendReplacement(out, Matcher.quoteReplacement(val));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static boolean isTruthy(String v) {
        return v != null && !v.isEmpty() && !"false".equalsIgnoreCase(v);
    }
}
