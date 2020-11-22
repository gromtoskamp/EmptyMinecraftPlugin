package me.rutger.jirachest;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class IssueFactory {

    JiraRequest jiraRequest;

    public IssueFactory() {
        jiraRequest = new JiraRequest();
    }

    public ItemStack createIssue(String key, String summary, String description, String author) {
        // Build new written book
        ItemStack writtenBook = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) writtenBook.getItemMeta();

        // Add title
        String title = key + " " + summary;
        // Trim title after 32 chars, this is a Minecraft limit
        title = title.substring(0, Math.min(title.length(), 32));
        bookMeta.setTitle(title);

        // Add Jira reporter as author
        bookMeta.setAuthor(author);
        // Add page to book
        ComponentBuilder page = createPage(key, summary, description);
        bookMeta.spigot().addPage(page.create());
        // Finish up book
        writtenBook.setItemMeta(bookMeta);

        return writtenBook;
    }

    private ComponentBuilder createPage(String key, String summary, String description) {
        // Define page
        ComponentBuilder page = new ComponentBuilder();

        // Add title to page in Bold
        page.append(formatTitle(key, summary));
        // Add Jira link
        page.append(formatJiraLink(key));
        // Add description
        page.append(formatDescription(description));

        page.create();
        return page;
    }

    private TextComponent formatTitle(String key, String summary) {
        return new TextComponent(TextComponent.fromLegacyText("§l"+key + " " + summary + "§r"));
    }

    private TextComponent formatJiraLink(String key) {
        // Define clickable link to Jira page
        final TextComponent issuelink = new TextComponent();
        final TextComponent link = new TextComponent(TextComponent.fromLegacyText("\n\n » View on Jira\n\n"));
        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, jiraRequest.getUri("/browse/"+key)));
        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("Click to visit Jira!")));
        link.setUnderlined(true);
        link.setColor( net.md_5.bungee.api.ChatColor.BLUE );

        issuelink.addExtra(link);
        return issuelink;
    }

    private TextComponent formatDescription(String description) {
        // Define description
        return new TextComponent(TextComponent.fromLegacyText(description));
    }
}
