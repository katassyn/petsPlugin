
package pl.yourserver;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Koloruje tekst używając & kodów oraz hex kolorów (&#RRGGBB)
     */
    public static String colorize(String text) {
        if (text == null) return "";

        // Najpierw konwertuj hex kolory
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer(text.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + group).toString());
        }
        matcher.appendTail(buffer);
        text = buffer.toString();

        // Następnie konwertuj standardowe kody kolorów
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Koloruje listę tekstów
     */
    public static List<String> colorize(List<String> texts) {
        List<String> colored = new ArrayList<>();
        for (String text : texts) {
            colored.add(colorize(text));
        }
        return colored;
    }

    /**
     * Usuwa kolory z tekstu
     */
    public static String stripColors(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(text);
    }

    /**
     * Wysyła kolorowaną wiadomość do gracza
     */
    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }

    /**
     * Wysyła kolorowaną wiadomość do gracza z prefixem
     */
    public static void sendMessage(CommandSender sender, String prefix, String message) {
        sender.sendMessage(colorize(prefix + message));
    }

    /**
     * Centruje tekst w czacie
     */
    public static String centerText(String text) {
        int maxWidth = 80;
        text = stripColors(text);

        int spaces = (maxWidth - text.length()) / 2;
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < spaces; i++) {
            builder.append(" ");
        }

        return builder.toString() + text;
    }

    /**
     * Tworzy pasek postępu
     */
    public static String createProgressBar(double current, double max, int length, char symbol, ChatColor completedColor, ChatColor notCompletedColor) {
        double percent = current / max;
        int completed = (int) (length * percent);

        StringBuilder builder = new StringBuilder();
        builder.append(completedColor);

        for (int i = 0; i < length; i++) {
            if (i == completed) {
                builder.append(notCompletedColor);
            }
            builder.append(symbol);
        }

        return builder.toString();
    }

    /**
     * Formatuje liczby z separatorami tysięcy
     */
    public static String formatNumber(long number) {
        return String.format("%,d", number);
    }

    /**
     * Formatuje liczby zmiennoprzecinkowe
     */
    public static String formatDouble(double number, int decimalPlaces) {
        return String.format("%." + decimalPlaces + "f", number);
    }

    /**
     * Konwertuje sekundy na format czasu
     */
    public static String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    /**
     * Sprawdza czy tekst jest pusty
     */
    public static boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * Zamienia pierwszą literę na wielką
     */
    public static String capitalize(String text) {
        if (isEmpty(text)) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    /**
     * Powtarza tekst określoną ilość razy
     */
    public static String repeat(String text, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(text);
        }
        return builder.toString();
    }
}