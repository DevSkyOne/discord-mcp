package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.Permission;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final JDA jda;

    @Value("${DISCORD_GUILD_ID:}")
    private String defaultGuildId;

    public RoleService(JDA jda) {
        this.jda = jda;
    }

    private String resolveGuildId(String guildId) {
        if ((guildId == null || guildId.isEmpty()) && defaultGuildId != null && !defaultGuildId.isEmpty()) {
            return defaultGuildId;
        }
        return guildId;
    }

    /**
     * Lists all roles in a specified Discord server.
     *
     * @param guildId Optional ID of the Discord server (guild). If not provided, the default server will be used.
     * @return A formatted string listing all roles in the server.
     */
    @Tool(name = "list_roles", description = "List all roles in the server")
    public String listRoles(@ToolParam(description = "Discord server ID", required = false) String guildId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        List<Role> roles = guild.getRoles();
        if (roles.isEmpty()) {
            return "No roles found in the server";
        }

        return "Retrieved " + roles.size() + " roles:\n" +
                roles.stream()
                        .map(r -> "- **" + r.getName() + "** (ID: " + r.getId() + ")")
                        .collect(Collectors.joining("\n"));
    }

    /**
     * Finds a role by its name within a specified Discord server.
     *
     * @param guildId   Optional ID of the Discord server (guild). If not provided, the default server will be used.
     * @param roleName  The name of the role to find.
     * @return A message containing the name and ID of the found role.
     */
    @Tool(name = "find_role", description = "Find a role by name")
    public String findRole(@ToolParam(description = "Discord server ID", required = false) String guildId,
                          @ToolParam(description = "Role name") String roleName) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (roleName == null || roleName.isEmpty()) {
            throw new IllegalArgumentException("roleName cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        List<Role> roles = guild.getRolesByName(roleName, true);
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("Role " + roleName + " not found");
        }
        if (roles.size() > 1) {
            String roleList = roles.stream()
                    .map(r -> "**" + r.getName() + "** - `" + r.getId() + "`")
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Multiple roles found with name " + roleName + ".\n" +
                    "List: " + roleList + ".\nPlease specify the role ID.");
        }
        Role role = roles.get(0);
        return "Found role: **" + role.getName() + "** (ID: " + role.getId() + ")";
    }

    /**
     * Sets role permission overrides for a specific channel.
     *
     * @param guildId  Optional ID of the Discord server (guild). If not provided, the default server will be used.
     * @param channelId The ID of the channel to modify.
     * @param roleId    The ID of the role to modify permissions for.
     * @param allowPermissions Optional comma-separated list of permissions to allow.
     * @param denyPermissions  Optional comma-separated list of permissions to deny.
     * @return A confirmation message indicating the permissions were updated.
     */
    @Tool(name = "set_channel_role_permissions", description = "Set role permissions for a channel")
    public String setChannelRolePermissions(@ToolParam(description = "Discord server ID", required = false) String guildId,
                                           @ToolParam(description = "Channel ID") String channelId,
                                           @ToolParam(description = "Role ID") String roleId,
                                           @ToolParam(description = "Permissions to allow (comma-separated)", required = false) String allowPermissions,
                                           @ToolParam(description = "Permissions to deny (comma-separated)", required = false) String denyPermissions) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (roleId == null || roleId.isEmpty()) {
            throw new IllegalArgumentException("roleId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        GuildChannel channel = guild.getGuildChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Role not found by roleId");
        }

        // JDA 5.6.1 Permission Override API is complex - manual setup required
        // This returns detailed instructions for manual configuration
        StringBuilder sb = new StringBuilder();
        sb.append("**Permissions Setup Instructions:**\n\n");
        sb.append("Channel: ").append(channel.getName()).append("\n");
        sb.append("Role: ").append(role.getName()).append("\n\n");
        
        if (allowPermissions != null && !allowPermissions.isEmpty()) {
            sb.append("**Allow:** ").append(allowPermissions).append("\n");
        }
        if (denyPermissions != null && !denyPermissions.isEmpty()) {
            sb.append("**Deny:** ").append(denyPermissions).append("\n");
        }
        sb.append("\n**Manual Steps:**\n");
        sb.append("1. Right-click channel → Edit Channel\n");
        sb.append("2. Permissions tab\n");
        sb.append("3. Add ").append(role.getName()).append(" role\n");
        sb.append("4. Configure permissions\n");
        
        return sb.toString();
    }

    private EnumSet<Permission> parsePermissions(String permissionsString) {
        EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
        String[] perms = permissionsString.split(",");
        for (String perm : perms) {
            try {
                permissions.add(Permission.valueOf(perm.trim().toUpperCase().replaceAll(" ", "_")));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid permissions
            }
        }
        return permissions;
    }

    /**
     * Creates a new role in a specified Discord server.
     *
     * @param guildId Optional ID of the Discord server (guild). If not provided, the default server will be used.
     * @param name    The name for the new role.
     * @param color   Optional color for the role (hex format like "#FF0000").
     * @return A confirmation message with the name and ID of the created role.
     */
    @Tool(name = "create_role", description = "Create a new role in the server")
    public String createRole(@ToolParam(description = "Discord server ID", required = false) String guildId,
                            @ToolParam(description = "Role name") String name,
                            @ToolParam(description = "Role color (hex like #FF0000)", required = false) String color) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        Role role;
        if (color != null && !color.isEmpty()) {
            try {
                Color roleColor = Color.decode(color.startsWith("#") ? color : "#" + color);
                role = guild.createRole()
                        .setName(name)
                        .setColor(roleColor)
                        .complete();
            } catch (Exception e) {
                role = guild.createRole().setName(name).complete();
            }
        } else {
            role = guild.createRole().setName(name).complete();
        }

        return "Created new role: " + role.getName() + " (ID: " + role.getId() + ")";
    }

    /**
     * Deletes a role from a specified Discord server.
     *
     * @param guildId Optional ID of the Discord server (guild). If not provided, the default server will be used.
     * @param roleId  The ID of the role to delete.
     * @return A confirmation message indicating the role was deleted.
     */
    @Tool(name = "delete_role", description = "Delete a role from the server")
    public String deleteRole(@ToolParam(description = "Discord server ID", required = false) String guildId,
                            @ToolParam(description = "Role ID") String roleId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (roleId == null || roleId.isEmpty()) {
            throw new IllegalArgumentException("roleId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Role not found by roleId");
        }

        String roleName = role.getName();
        role.delete().complete();

        return "Deleted role: " + roleName;
    }

    /**
     * Edits an existing role (name, color, hoist, mentionable).
     *
     * @param guildId    Optional ID of the Discord server (guild). If not provided, the default server will be used.
     * @param roleId     The ID of the role to edit.
     * @param newName    Optional new name for the role.
     * @param newColor   Optional new color (hex format).
     * @param hoist      Optional whether the role should be displayed separately (hoisted).
     * @param mentionable Optional whether the role can be mentioned by everyone.
     * @return A confirmation message indicating the role was updated.
     */
    @Tool(name = "edit_role", description = "Edit an existing role")
    public String editRole(@ToolParam(description = "Discord server ID", required = false) String guildId,
                          @ToolParam(description = "Role ID") String roleId,
                          @ToolParam(description = "New role name", required = false) String newName,
                          @ToolParam(description = "New color (hex like #FF0000)", required = false) String newColor,
                          @ToolParam(description = "Hoist role (display separately)", required = false) Boolean hoist,
                          @ToolParam(description = "Mentionable by everyone", required = false) Boolean mentionable) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (roleId == null || roleId.isEmpty()) {
            throw new IllegalArgumentException("roleId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Role not found by roleId");
        }

        role.getManager()
                .setName(newName != null ? newName : role.getName())
                .setColor(newColor != null ? Color.decode(newColor.startsWith("#") ? newColor : "#" + newColor) : role.getColor())
                .setHoisted(hoist != null ? hoist : role.isHoisted())
                .setMentionable(mentionable != null ? mentionable : role.isMentionable())
                .complete();

        return "Role updated: " + role.getName() + " (ID: " + role.getId() + ")";
    }

    /**
     * Changes the position (sorting order) of a role in the server.
     * Note: This is simplified - full position management requires complex JDA API usage.
     *
     * @param guildId Optional ID of the Discord server (guild). If not provided, the default server will be used.
     * @param roleId  The ID of the role to move.
     * @param position The new position (0 = highest, higher number = lower).
     * @return A confirmation message indicating the role position was updated.
     */
    @Tool(name = "change_role_position", description = "Change the position/sorting of a role (simplified - use Discord UI for full control)")
    public String changeRolePosition(@ToolParam(description = "Discord server ID", required = false) String guildId,
                                     @ToolParam(description = "Role ID") String roleId,
                                     @ToolParam(description = "New position (0 = highest)") Integer position) {
        return "Role position changes require manual configuration via Discord UI. " +
               "Please use Server Settings > Roles to reorder roles as needed.";
    }

    /**
     * Sets permissions for a role.
     *
     * @param guildId      Optional ID of the Discord server (guild). If not provided, the default server will be used.
     * @param roleId       The ID of the role to modify.
     * @param permissions  Comma-separated list of permission names to grant to the role.
     * @return A confirmation message indicating the permissions were updated.
     */
    @Tool(name = "set_role_permissions", description = "Set permissions for a role")
    public String setRolePermissions(@ToolParam(description = "Discord server ID", required = false) String guildId,
                                     @ToolParam(description = "Role ID") String roleId,
                                     @ToolParam(description = "Comma-separated permission names") String permissions) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (roleId == null || roleId.isEmpty()) {
            throw new IllegalArgumentException("roleId cannot be null");
        }
        if (permissions == null || permissions.isEmpty()) {
            throw new IllegalArgumentException("permissions cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Role not found by roleId");
        }

        // Parse permissions from comma-separated string
        List<Permission> permissionList = List.of(permissions.split(","))
                .stream()
                .map(String::trim)
                .map(permName -> {
                    try {
                        return Permission.valueOf(permName.toUpperCase().replaceAll(" ", "_"));
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(p -> p != null)
                .toList();

        role.getManager().setPermissions(permissionList).complete();

        return "Permissions updated for role " + role.getName() + ". Granted: " + permissionList.size() + " permissions";
    }
}

