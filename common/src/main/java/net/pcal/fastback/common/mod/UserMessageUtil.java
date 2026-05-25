/*
 * FastBack - Fast, incremental Minecraft backups powered by Git.
 * Copyright (C) 2022 pcal.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package net.pcal.fastback.common.mod;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.pcal.fastback.common.logging.UserMessage;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.ChatFormatting.GRAY;
import static net.minecraft.ChatFormatting.GREEN;
import static net.minecraft.ChatFormatting.RED;
import static net.minecraft.ChatFormatting.YELLOW;
import static net.minecraft.network.chat.Style.EMPTY;

/**
 * Utility for converting {@link UserMessage} to Minecraft {@link Component}.
 *
 * @author pcal
 * @since 0.2.0
 */
public class UserMessageUtil {

    private static final Map<String, String> TRANSLATIONS = new HashMap<>();

    static {
        loadTranslations();
    }

    private static void loadTranslations() {
        try {
            InputStreamReader reader = new InputStreamReader(
                UserMessageUtil.class.getResourceAsStream("/assets/fastback/lang/en_us.json")
            );
            JsonObject json = new Gson().fromJson(reader, JsonObject.class);
            json.entrySet().forEach(entry ->
                TRANSLATIONS.put(entry.getKey(), entry.getValue().getAsString())
            );
            reader.close();
        } catch (IOException | NullPointerException e) {
            // Silently fail if translations can't be loaded
            // This can happen in edge cases
        }
    }

    public static Component messageToText(final UserMessage m) {
        MutableComponent out;
        if (m.raw() != null) {
            out = Component.literal(m.raw());
        } else if (m.localized() != null) {
            // Look up the translation and format with parameters
            String template = TRANSLATIONS.get(m.localized().key());
            if (template != null) {
                try {
                    // Convert Java MessageFormat style {0} from %s style if needed
                    String formatted = formatMessage(template, m.localized().params());
                    out = Component.literal(formatted);
                } catch (Exception e) {
                    // Fallback if formatting fails
                    out = Component.literal(template);
                }
            } else {
                // Fallback if key not found - show the key so we know what's missing
                out = Component.literal(m.localized().key());
            }
        } else {
            out = Component.literal("");
        }
        switch (m.style()) {
            case ERROR -> out.setStyle(EMPTY.withColor(TextColor.fromLegacyFormat(RED)));
            case WARNING -> out.setStyle(EMPTY.withColor(TextColor.fromLegacyFormat(YELLOW)));
            case JGIT -> out.setStyle(EMPTY.withColor(TextColor.fromLegacyFormat(GRAY)));
            case NATIVE_GIT -> out.setStyle(EMPTY.withColor(TextColor.fromLegacyFormat(GREEN)));
        }
        return out;
    }

    /**
     * Format a message template with parameters.
     * Handles both %s style (old minecraft) and {0} style (MessageFormat)
     */
    private static String formatMessage(String template, Object[] params) {
        if (params == null || params.length == 0) {
            return template;
        }
        
        // Try %s style formatting first (used in en_us.json)
        try {
            return String.format(template, params);
        } catch (Exception e) {
            // Fall back to {0} style
            try {
                return MessageFormat.format(template, params);
            } catch (Exception e2) {
                // If all else fails, return template as-is
                return template;
            }
        }
    }

    private UserMessageUtil() {}
}
