package github.nighter.smartspawner.logging.discord;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Discord embed structure.
 */
public class DiscordEmbed {
    private String title;
    private String description;
    private int color;
    private Footer footer;
    private Thumbnail thumbnail;
    private String timestamp;
    private final List<Field> fields;
    
    public DiscordEmbed() {
        this.fields = new ArrayList<>();
        this.color = 0x5865F2; // Default Discord blurple
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setColor(int color) {
        this.color = color;
    }
    
    public void setFooter(String text, String iconUrl) {
        this.footer = new Footer(text, iconUrl);
    }
    
    public void setThumbnail(String url) {
        this.thumbnail = new Thumbnail(url);
    }
    
    public void setTimestamp(Instant instant) {
        this.timestamp = DateTimeFormatter.ISO_INSTANT.format(instant);
    }
    
    public void addField(String name, String value, boolean inline) {
        this.fields.add(new Field(name, value, inline));
    }
    
    /**
     * Convert the embed to Discord webhook JSON format.
     */
    public String toJson() {
        StringBuilder json = new StringBuilder("{\"embeds\":[{");
        
        if (title != null && !title.isEmpty()) {
            json.append("\"title\":\"").append(escapeJson(title)).append("\",");
        }
        
        if (description != null && !description.isEmpty()) {
            json.append("\"description\":\"").append(escapeJson(description)).append("\",");
        }
        
        json.append("\"color\":").append(color).append(",");
        
        if (footer != null) {
            json.append("\"footer\":{\"text\":\"").append(escapeJson(footer.text)).append("\"");
            if (footer.iconUrl != null) {
                json.append(",\"icon_url\":\"").append(escapeJson(footer.iconUrl)).append("\"");
            }
            json.append("},");
        }
        
        if (thumbnail != null) {
            json.append("\"thumbnail\":{\"url\":\"").append(escapeJson(thumbnail.url)).append("\"},");
        }
        
        if (timestamp != null) {
            json.append("\"timestamp\":\"").append(timestamp).append("\",");
        }
        
        if (!fields.isEmpty()) {
            json.append("\"fields\":[");
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) json.append(",");
                Field field = fields.get(i);
                json.append("{\"name\":\"").append(escapeJson(field.name)).append("\",");
                json.append("\"value\":\"").append(escapeJson(field.value)).append("\",");
                json.append("\"inline\":").append(field.inline).append("}");
            }
            json.append("],");
        }
        
        // Remove trailing comma if present
        if (json.charAt(json.length() - 1) == ',') {
            json.setLength(json.length() - 1);
        }
        
        json.append("}]}");
        return json.toString();
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    private static class Footer {
        String text;
        String iconUrl;
        
        Footer(String text, String iconUrl) {
            this.text = text;
            this.iconUrl = iconUrl;
        }
    }
    
    private static class Thumbnail {
        String url;
        
        Thumbnail(String url) {
            this.url = url;
        }
    }
    
    private static class Field {
        String name;
        String value;
        boolean inline;
        
        Field(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }
    }
}
