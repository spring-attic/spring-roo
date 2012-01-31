package org.springframework.roo.addon.cloud.foundry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listener for Shell events to support automatic Git repository commits.
 * 
 * @author James Tyrrell
 * @since 1.1.3
 */
public class ShellTableRenderer {

    private static final int COLUMN_PADDING = 5;

    private final Map<Integer, List<String>> columnMap = new HashMap<Integer, List<String>>();
    private final String title;

    /**
     * Constructor
     * 
     * @param title
     * @param headings
     */
    public ShellTableRenderer(final String title, final String... headings) {
        this.title = title;
        for (final String heading : headings) {
            addColumn(heading);
        }
    }

    public void addColumn(final String heading) {
        final int position = columnMap.isEmpty() ? 0 : columnMap.size();
        final List<String> list = new ArrayList<String>();
        list.add(heading);
        list.add(getUnderline(heading.length()));
        columnMap.put(position, list);
    }

    public void addRow(final String... values) {
        for (int i = 0; i < columnMap.size(); i++) {
            final String value = values[i];
            final List<String> list = columnMap.get(i);
            list.add(value);
        }
    }

    public int getColumnWidth(final int columnNumber) {
        final List<String> stringList = columnMap.get(columnNumber);
        if (stringList == null) {
            return 0;
        }
        int largestValue = 0;
        for (final String value : stringList) {
            if (value.length() > largestValue) {
                largestValue = value.length();
            }
        }
        return largestValue + COLUMN_PADDING;
    }

    public String getOutput() {
        final StringBuilder table = new StringBuilder();
        int i = 0;
        int longestRow = 0;
        while (true) {
            final StringBuilder entry = new StringBuilder();
            boolean timeToStop = false;
            for (int j = 0; j < columnMap.size(); j++) {
                final List<String> list = columnMap.get(j);
                if (i >= list.size()) {
                    timeToStop = true;
                    continue;
                }
                final int columnWidth = getColumnWidth(j);
                final String text = columnMap.get(j).get(i);
                entry.append(text);
                if (j < columnMap.size() - 1) {
                    entry.append(getPadding(columnWidth - text.length()));
                }
            }
            i++;
            if (entry.length() > longestRow) {
                longestRow = entry.length();
            }
            entry.append("\n");
            table.append(entry);
            if (timeToStop) {
                break;
            }
        }
        final StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append("\n");
        if (longestRow > title.length() + 2) {
            final int titleLengthPlusPadding = title.length() + 2;
            final String padding = " ";
            final String emphasis = getRepeatingChars('=',
                    (longestRow - titleLengthPlusPadding) / 2);
            String extra = "";
            if (titleLengthPlusPadding % 2 != 0) {
                extra = "=";
            }
            titleBuilder.append(emphasis).append(padding).append(title)
                    .append(padding).append(emphasis).append(extra)
                    .append("\n\n");
        }
        else {
            titleBuilder.append("= ").append(title).append(" =").append("\n\n");
        }
        titleBuilder.append(table);
        return titleBuilder.toString();
    }

    private String getPadding(final int paddingRequired) {
        return getRepeatingChars(' ', paddingRequired);
    }

    private String getRepeatingChars(final char c, final int repeat) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < repeat; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private String getUnderline(final int underlineRequired) {
        return getRepeatingChars('-', underlineRequired);
    }
}
