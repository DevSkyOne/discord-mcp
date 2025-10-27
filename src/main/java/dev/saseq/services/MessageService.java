package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessageService {

    private final JDA jda;

    public MessageService(JDA jda) {
        this.jda = jda;
    }

    /**
     * Sends a message to a specified Discord channel.
     *
     * @param channelId The ID of the channel where the message will be sent.
     * @param message   The content of the message to be sent.
     * @return A confirmation message with a link to the sent message.
     */
    @Tool(name = "send_message", description = "Send a message to a specific channel")
    public String sendMessage(@ToolParam(description = "Discord channel ID") String channelId,
                              @ToolParam(description = "Message content") String message) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("message cannot be null");
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Message sentMessage = textChannelById.sendMessage(message).complete();
        return "Message sent successfully. Message link: " + sentMessage.getJumpUrl();
    }

    /**
     * Edits an existing message in a specified Discord channel.
     *
     * @param channelId  The ID of the channel containing the message.
     * @param messageId  The ID of the message to be edited.
     * @param newMessage The new content for the message.
     * @return A confirmation message with a link to the edited message.
     */
    @Tool(name = "edit_message", description = "Edit a message from a specific channel")
    public String editMessage(@ToolParam(description = "Discord channel ID") String channelId,
                              @ToolParam(description = "Specific message ID") String messageId,
                              @ToolParam(description = "New message content") String newMessage) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }
        if (newMessage == null || newMessage.isEmpty()) {
            throw new IllegalArgumentException("newMessage cannot be null");
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Message messageById = textChannelById.retrieveMessageById(messageId).complete();
        if (messageById == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }
        Message editedMessage = messageById.editMessage(newMessage).complete();
        return "Message edited successfully. Message link: " + editedMessage.getJumpUrl();
    }

    /**
     * Deletes a message from a specified Discord channel.
     *
     * @param channelId The ID of the channel containing the message.
     * @param messageId The ID of the message to be deleted.
     * @return A confirmation message indicating the message was deleted successfully.
     */
    @Tool(name = "delete_message", description = "Delete a message from a specific channel")
    public String deleteMessage(@ToolParam(description = "Discord channel ID") String channelId,
                                @ToolParam(description = "Specific message ID") String messageId) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Message messageById = textChannelById.retrieveMessageById(messageId).complete();
        if (messageById == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }
        messageById.delete().queue();
        return "Message deleted successfully";
    }

    /**
     * Reads the recent message history from a specified Discord channel.
     *
     * @param channelId The ID of the channel from which to read messages.
     * @param count     Optional number of messages to retrieve (default is 100).
     * @return A formatted string containing the retrieved messages.
     */
    @Tool(name = "read_messages", description = "Read recent message history from a specific channel")
    public String readMessages(@ToolParam(description = "Discord channel ID") String channelId,
                               @ToolParam(description = "Number of messages to retrieve", required = false) String count) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        int limit = 100;
        if (count != null) {
            limit = Integer.parseInt(count);
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        List<Message> messages = textChannelById.getHistory().retrievePast(limit).complete();
        List<String> formatedMessages = formatMessages(messages);
        return "**Retrieved " + messages.size() + " messages:** \n" + String.join("\n", formatedMessages);
    }

    /**
     * Adds a reaction (emoji) to a specific message in a Discord channel.
     *
     * @param channelId The ID of the channel containing the message.
     * @param messageId The ID of the message to which the reaction will be added.
     * @param emoji     The emoji to add as a reaction (can be a Unicode character or a custom emoji string).
     * @return A confirmation message with a link to the message that was reacted to.
     */
    @Tool(name = "add_reaction", description = "Add a reaction (emoji) to a specific message")
    public String addReaction(@ToolParam(description = "Discord channel ID") String channelId,
                              @ToolParam(description = "Discord message ID") String messageId,
                              @ToolParam(description = "Emoji (Unicode or string)") String emoji) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }
        if (emoji == null || emoji.isEmpty()) {
            throw new IllegalArgumentException("emoji cannot be null");
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Message message = textChannelById.retrieveMessageById(messageId).complete();
        if (message == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }
        message.addReaction(Emoji.fromUnicode(emoji)).queue();
        return "Added reaction successfully. Message link: " + message.getJumpUrl();
    }

    /**
     * Removes a specified reaction (emoji) from a message in a Discord channel.
     *
     * @param channelId The ID of the channel containing the message.
     * @param messageId The ID of the message from which the reaction will be removed.
     * @param emoji     The emoji to remove from the message (can be a Unicode character or a custom emoji string).
     * @return A confirmation message with a link to the message.
     */
    @Tool(name = "remove_reaction", description = "Remove a specified reaction (emoji) from a message")
    public String removeReaction(@ToolParam(description = "Discord channel ID") String channelId,
                                 @ToolParam(description = "Discord message ID") String messageId,
                                 @ToolParam(description = "Emoji (Unicode or string)") String emoji) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }
        if (emoji == null || emoji.isEmpty()) {
            throw new IllegalArgumentException("emoji cannot be null");
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Message message = textChannelById.retrieveMessageById(messageId).complete();
        if (message == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }
        message.removeReaction(Emoji.fromUnicode(emoji)).queue();
        return "Added reaction successfully. Message link: " + message.getJumpUrl();
    }

    /**
     * Sends a rich embed message to a specified Discord channel with advanced features.
     *
     * @param channelId   The ID of the channel where the embed will be sent.
     * @param title       The title of the embed.
     * @param description The description/content of the embed.
     * @param color       Optional color for the embed (hex format like "#FF0000" or color name).
     * @param footer      Optional footer text for the embed.
     * @param author      Optional author name (with optional authorIcon URL).
     * @param thumbnail   Optional thumbnail image URL.
     * @param image       Optional full image URL.
     * @param fields      Optional fields in format "name|value|inline" separated by semicolons.
     * @param timestamp   Optional timestamp to display (true/false or specific date).
     * @return A confirmation message with a link to the sent message.
     */
    @Tool(name = "send_embed", description = "Send a rich embed message to a specific channel")
    public String sendEmbed(@ToolParam(description = "Discord channel ID") String channelId,
                           @ToolParam(description = "Embed title") String title,
                           @ToolParam(description = "Embed description/content") String description,
                           @ToolParam(description = "Embed color (hex like #FF0000 or color name)", required = false) String color,
                           @ToolParam(description = "Footer text", required = false) String footer,
                           @ToolParam(description = "Author name", required = false) String author,
                           @ToolParam(description = "Author icon URL", required = false) String authorIcon,
                           @ToolParam(description = "Thumbnail image URL", required = false) String thumbnail,
                           @ToolParam(description = "Full image URL", required = false) String image,
                           @ToolParam(description = "Fields in format 'name|value|inline' separated by semicolons", required = false) String fields,
                           @ToolParam(description = "Show timestamp (true/false)", required = false) String timestamp) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("title cannot be null");
        }
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("description cannot be null");
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(description);

        // Set color if provided
        if (color != null && !color.isEmpty()) {
            try {
                if (color.startsWith("#")) {
                    embedBuilder.setColor(Color.decode(color));
                } else {
                    // Try to parse as color name
                    embedBuilder.setColor(Color.decode("#" + color));
                }
            } catch (Exception e) {
                // Use default color if parsing fails
                embedBuilder.setColor(0x5865F2); // Discord blurple
            }
        } else {
            embedBuilder.setColor(0x5865F2); // Discord blurple default
        }

        // Set author if provided
        if (author != null && !author.isEmpty()) {
            if (authorIcon != null && !authorIcon.isEmpty()) {
                embedBuilder.setAuthor(author, null, authorIcon);
            } else {
                embedBuilder.setAuthor(author);
            }
        }

        // Set thumbnail if provided
        if (thumbnail != null && !thumbnail.isEmpty()) {
            embedBuilder.setThumbnail(thumbnail);
        }

        // Set image if provided
        if (image != null && !image.isEmpty()) {
            embedBuilder.setImage(image);
        }

        // Set fields if provided
        if (fields != null && !fields.isEmpty()) {
            String[] fieldArray = fields.split(";");
            for (String field : fieldArray) {
                String[] parts = field.split("\\|");
                if (parts.length >= 2) {
                    String fieldName = parts[0].trim();
                    String fieldValue = parts[1].trim();
                    boolean inline = parts.length > 2 && parts[2].trim().equalsIgnoreCase("true");
                    embedBuilder.addField(fieldName, fieldValue, inline);
                }
            }
        }

        // Set timestamp if requested
        if (timestamp != null && !timestamp.isEmpty()) {
            if (timestamp.equalsIgnoreCase("true")) {
                embedBuilder.setTimestamp(java.time.Instant.now());
            }
        }

        // Set footer if provided
        if (footer != null && !footer.isEmpty()) {
            embedBuilder.setFooter(footer);
        }

        Message sentMessage = textChannelById.sendMessageEmbeds(embedBuilder.build()).complete();
        return "Embed sent successfully. Message link: " + sentMessage.getJumpUrl();
    }

    /**
     * Sends a message with interactive components (buttons, dropdowns) to a Discord channel.
     *
     * @param channelId The channel ID to send the message to
     * @param content   The message content (optional, can be empty)
     * @param buttons   Button definitions in format "label|style|customId|url" separated by semicolons. 
     *                  Style: primary, secondary, success, danger, link. url only for link buttons.
     * @param selectMenu SelectMenu in format "label|value;label2|value2" (max 25 options, comma separates options)
     * @param placeholder SelectMenu placeholder text
     * @return Confirmation message with link to sent message
     */
    @Tool(name = "send_message_with_components", description = "Send a message with interactive components (buttons, dropdowns) to a channel")
    public String sendMessageWithComponents(@ToolParam(description = "Discord channel ID") String channelId,
                                           @ToolParam(description = "Message content (optional)", required = false) String content,
                                           @ToolParam(description = "Buttons: 'label|style|customId;label2|style2|customId2' (style: primary, secondary, success, danger, link)", required = false) String buttons,
                                           @ToolParam(description = "SelectMenu options: 'label|value;label2|value2'", required = false) String selectMenu,
                                           @ToolParam(description = "SelectMenu placeholder text", required = false) String placeholder) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        MessageCreateBuilder messageBuilder = new MessageCreateBuilder();

        // Set content if provided
        if (content != null && !content.isEmpty()) {
            messageBuilder.setContent(content);
        }

        List<ActionRow> actionRows = new ArrayList<>();

        // Parse and add buttons if provided
        if (buttons != null && !buttons.isEmpty()) {
            List<Button> buttonList = parseButtons(buttons);
            if (!buttonList.isEmpty()) {
                actionRows.add(ActionRow.of(buttonList));
            }
        }

        // Parse and add select menu if provided
        if (selectMenu != null && !selectMenu.isEmpty()) {
            StringSelectMenu.Builder selectMenuBuilder = StringSelectMenu.create("custom_select_menu");
            
            if (placeholder != null && !placeholder.isEmpty()) {
                selectMenuBuilder.setPlaceholder(placeholder);
            }

            String[] options = selectMenu.split(";");
            for (String option : options) {
                String[] parts = option.split("\\|");
                if (parts.length >= 2) {
                    String label = parts[0].trim();
                    String value = parts[1].trim();
                    String description = parts.length > 2 ? parts[2].trim() : null;
                    
                    if (description != null && !description.isEmpty()) {
                        selectMenuBuilder.addOption(label, value, description);
                    } else {
                        selectMenuBuilder.addOption(label, value);
                    }
                }
            }
            
            actionRows.add(ActionRow.of(selectMenuBuilder.build()));
        }

        // Add all components to message
        messageBuilder.setComponents(actionRows);

        Message sentMessage = channel.sendMessage(messageBuilder.build()).complete();
        return "Message with components sent successfully. Message link: " + sentMessage.getJumpUrl();
    }

    /**
     * Parses button definitions and creates Button objects.
     * Format: "label|style|customId;label2|style2|customId2" or "label|style|customId|url" for link buttons
     */
    private List<Button> parseButtons(String buttonsString) {
        List<Button> buttonList = new ArrayList<>();
        String[] buttonStrings = buttonsString.split(";");

        for (String buttonStr : buttonStrings) {
            String[] parts = buttonStr.split("\\|");
            if (parts.length >= 3) {
                String label = parts[0].trim();
                String style = parts[1].trim().toLowerCase();
                String customId = parts[2].trim();
                String url = parts.length > 3 ? parts[3].trim() : null;

                Button button;
                try {
                    switch (style) {
                        case "primary":
                            button = Button.primary(customId, label);
                            break;
                        case "secondary":
                            button = Button.secondary(customId, label);
                            break;
                        case "success":
                            button = Button.success(customId, label);
                            break;
                        case "danger":
                            button = Button.danger(customId, label);
                            break;
                        case "link":
                            if (url == null || url.isEmpty()) {
                                throw new IllegalArgumentException("URL required for link buttons");
                            }
                            button = Button.link(url, label);
                            break;
                        default:
                            button = Button.secondary(customId, label);
                    }
                    buttonList.add(button);
                } catch (Exception e) {
                    // Skip invalid buttons
                }
            }
        }

        return buttonList;
    }

    private List<String> formatMessages(List<Message> messages) {
        return messages.stream()
                .map(m -> {
                    String authorName = m.getAuthor().getName();
                    String timestamp = m.getTimeCreated().toString();
                    String content = m.getContentDisplay();
                    String messageId = m.getId();

                    return String.format("- (ID: %s) **[%s]** `%s`: ```%s```", messageId, authorName, timestamp, content);
                }).toList();
    }
}