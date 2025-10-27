package dev.saseq.services;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for advanced message classification and filtering
 * Extends MessageService with Intent-based reading capabilities
 */
@Service
public class MessageClassificationService {

    private final net.dv8tion.jda.api.JDA jda;

    public MessageClassificationService(net.dv8tion.jda.api.JDA jda) {
        this.jda = jda;
    }

    /**
     * Classifies a message into intent categories based on content analysis
     * 
     * @param messageContent The message content to classify
     * @return The intent category (question, answer, project_showcase, bug_report, feedback, general)
     */
    public String classifyIntent(String messageContent) {
        if (messageContent == null || messageContent.trim().isEmpty()) {
            return "general";
        }
        
        String lowerContent = messageContent.toLowerCase();
        
        // Pattern-based classification
        if (isQuestion(lowerContent)) {
            return "question";
        } else if (isAnswer(lowerContent)) {
            return "answer";
        } else if (isProjectShowcase(lowerContent)) {
            return "project_showcase";
        } else if (isBugReport(lowerContent)) {
            return "bug_report";
        } else if (isFeedback(lowerContent)) {
            return "feedback";
        } else {
            return "general";
        }
    }

    /**
     * Read messages from a channel filtered by intent/theme
     * 
     * @param channelId The ID of the channel to read from
     * @param intent The intent to filter by: 'question', 'answer', 'project_showcase', 'bug_report', 'feedback', 'general'
     * @param timespanDays Number of days to search back (estimated by message count)
     * @param limit Maximum number of messages to return
     * @return Formatted list of messages matching the intent
     */
    @Tool(name = "read_messages_by_intent", description = "Read messages from a channel filtered by intent/theme")
    public String readMessagesByIntent(@ToolParam(description = "Discord channel ID") String channelId,
                                       @ToolParam(description = "Intent type: 'question', 'project_showcase', 'bug_report', 'feedback', 'general'") String intent,
                                       @ToolParam(description = "Search back N days (estimated)", required = false) String timespanDays,
                                       @ToolParam(description = "Max messages to return", required = false) String limit) {
        
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (intent == null || intent.isEmpty()) {
            throw new IllegalArgumentException("intent cannot be null");
        }

        // Parse optional parameters
        int dayEstimate = 30; // ~100 messages per day estimate
        if (timespanDays != null && !timespanDays.isEmpty()) {
            dayEstimate = Integer.parseInt(timespanDays);
        }
        int maxResults = 50;
        if (limit != null && !limit.isEmpty()) {
            maxResults = Integer.parseInt(limit);
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        // Retrieve messages (estimate based on timespan)
        int messageLimit = dayEstimate * 100; // Rough estimate
        List<Message> allMessages = textChannelById.getHistory()
                .retrievePast(messageLimit)
                .complete();

        // Filter by intent
        List<Message> filteredMessages = allMessages.stream()
                .filter(msg -> classifyIntent(msg.getContentDisplay()).equalsIgnoreCase(intent))
                .limit(maxResults)
                .collect(Collectors.toList());

        // Format results
        if (filteredMessages.isEmpty()) {
            return String.format("No messages found with intent '%s' in channel (searched %d messages)", intent, allMessages.size());
        }

        List<String> formattedMessages = formatMessages(filteredMessages);
        return String.format("**Retrieved %d messages with intent '%s':** \n%s", 
                            filteredMessages.size(), intent, String.join("\n", formattedMessages));
    }

    /**
     * Search messages by content/keywords
     * 
     * @param channelId The ID of the channel to search in
     * @param query Keywords to search for
     * @param limit Maximum number of messages to return
     * @return Formatted list of messages containing the keywords
     */
    @Tool(name = "search_messages_by_content", description = "Search messages by content/keywords")
    public String searchMessagesByContent(@ToolParam(description = "Discord channel ID") String channelId,
                                         @ToolParam(description = "Search keywords") String query,
                                         @ToolParam(description = "Max messages to return", required = false) String limit) {
        
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("query cannot be null");
        }

        int maxResults = 50;
        if (limit != null && !limit.isEmpty()) {
            maxResults = Integer.parseInt(limit);
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        // Retrieve messages
        List<Message> messages = textChannelById.getHistory().retrievePast(500).complete();

        // Filter by keyword
        String[] keywords = query.toLowerCase().split(" ");
        List<Message> matchingMessages = messages.stream()
                .filter(msg -> {
                    String content = msg.getContentDisplay().toLowerCase();
                    for (String keyword : keywords) {
                        if (content.contains(keyword)) {
                            return true;
                        }
                    }
                    return false;
                })
                .limit(maxResults)
                .collect(Collectors.toList());

        if (matchingMessages.isEmpty()) {
            return String.format("No messages found matching '%s' in channel", query);
        }

        List<String> formattedMessages = formatMessages(matchingMessages);
        return String.format("**Found %d messages matching '%s':** \n%s", 
                            matchingMessages.size(), query, String.join("\n", formattedMessages));
    }

    /**
     * Get message statistics for a channel
     * 
     * @param channelId The ID of the channel to analyze
     * @param timespanDays Number of days to analyze
     * @return Statistics about message types and activity
     */
    @Tool(name = "analyze_channel_stats", description = "Analyze message statistics for a channel")
    public String analyzeChannelStats(@ToolParam(description = "Discord channel ID") String channelId,
                                      @ToolParam(description = "Number of days to analyze", required = false) String timespanDays) {
        
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        int dayEstimate = 30;
        if (timespanDays != null && !timespanDays.isEmpty()) {
            dayEstimate = Integer.parseInt(timespanDays);
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        List<Message> messages = textChannelById.getHistory().retrievePast(dayEstimate * 100).complete();

        // Count by intent
        long questions = messages.stream().filter(m -> "question".equals(classifyIntent(m.getContentDisplay()))).count();
        long answers = messages.stream().filter(m -> "answer".equals(classifyIntent(m.getContentDisplay()))).count();
        long showcases = messages.stream().filter(m -> "project_showcase".equals(classifyIntent(m.getContentDisplay()))).count();
        long bugReports = messages.stream().filter(m -> "bug_report".equals(classifyIntent(m.getContentDisplay()))).count();
        long feedback = messages.stream().filter(m -> "feedback".equals(classifyIntent(m.getContentDisplay()))).count();
        long general = messages.stream().filter(m -> "general".equals(classifyIntent(m.getContentDisplay()))).count();

        return String.format("**Channel Statistics (last %d days, %d messages):**\n" +
                "- Questions: %d (%.1f%%)\n" +
                "- Answers: %d (%.1f%%)\n" +
                "- Project Showcases: %d (%.1f%%)\n" +
                "- Bug Reports: %d (%.1f%%)\n" +
                "- Feedback: %d (%.1f%%)\n" +
                "- General: %d (%.1f%%)",
                dayEstimate, messages.size(),
                questions, (questions * 100.0 / messages.size()),
                answers, (answers * 100.0 / messages.size()),
                showcases, (showcases * 100.0 / messages.size()),
                bugReports, (bugReports * 100.0 / messages.size()),
                feedback, (feedback * 100.0 / messages.size()),
                general, (general * 100.0 / messages.size()));
    }

    // Helper methods for intent classification
    private boolean isQuestion(String content) {
        return content.contains("?") || 
               content.startsWith("wie") || content.startsWith("warum") || 
               content.startsWith("wieso") || content.startsWith("welche") ||
               content.startsWith("was ist") || content.startsWith("kann ");
    }

    private boolean isAnswer(String content) {
        return content.startsWith("you can") || content.startsWith("du kannst") ||
               content.contains("answer") || content.contains("antwort") ||
               content.contains("solution") || content.contains("lösung") ||
               (content.length() > 100 && (content.contains("try") || content.contains("versuch")));
    }

    private boolean isProjectShowcase(String content) {
        return content.contains("github.com") || content.contains("gitlab.com") ||
               content.contains("check out my") || content.contains("checke aus") ||
               content.contains("my project") || content.contains("mein projekt") ||
               content.contains("https://") && (content.contains("project") || content.contains("projekt"));
    }

    private boolean isBugReport(String content) {
        return content.contains("error") || content.contains("fehler") ||
               content.contains("bug") || content.contains("doesn't work") ||
               content.contains("funktioniert nicht") || content.contains("nicht funktioniert") ||
               content.contains("exception") || content.contains("crash") ||
               content.contains("fix") && content.length() > 50;
    }

    private boolean isFeedback(String content) {
        return content.toLowerCase().contains("feedback") || 
               content.toLowerCase().contains("vorschlag") ||
               content.toLowerCase().contains("suggestion") ||
               content.toLowerCase().contains("should") && (content.contains("be able") || content.length() > 50);
    }

    private List<String> formatMessages(List<Message> messages) {
        return messages.stream()
                .map(m -> {
                    String authorName = m.getAuthor().getName();
                    String timestamp = m.getTimeCreated().toString();
                    String content = m.getContentDisplay();
                    String messageId = m.getId();
                    String intent = classifyIntent(content);

                    return String.format("- (ID: %s) [Intent: %s] **[%s]** `%s`: ```%s```", 
                                        messageId, intent, authorName, timestamp, content);
                }).collect(Collectors.toList());
    }
}

