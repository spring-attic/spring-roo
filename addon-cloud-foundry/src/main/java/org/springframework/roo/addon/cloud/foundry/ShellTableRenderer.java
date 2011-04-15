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
	private Map<Integer, List<String>> columnMap = new HashMap<Integer, List<String>>();
	private String title;
	private static final int COLUMN_PADDING = 5;

	public ShellTableRenderer(String title, String... headings) {
		this.title = title;
		for (String heading : headings) {
			addColumn(heading);
		}
	}

	public void addColumn(String heading) {
		int position = columnMap.size() == 0 ? 0 : columnMap.size();
		List<String> list = new ArrayList<String>();
		list.add(heading);
		list.add(getUnderline(heading.length()));
		columnMap.put(position, list);
	}

	public void addRow(String... values) {
		for (int i = 0; i < columnMap.size(); i++) {
			String value = values[i];
			List<String> list = columnMap.get(i);
			list.add(value);
		}
	}

	public int getColumnWidth(int columnNumber) {
		List<String> stringList = columnMap.get(columnNumber);
		if (stringList == null) {
			return 0;
		}
		int largestValue = 0;
		for (String value : stringList) {
			if (value.length() > largestValue) {
				largestValue = value.length();
			}
		}
		return largestValue + COLUMN_PADDING;
	}

	public String getOutput() {
		StringBuilder table = new StringBuilder();
		int i = 0;
		int longestRow = 0;
		while (true) {
			StringBuilder entry = new StringBuilder();
			boolean timeToStop = false;
			for (int j = 0; j < columnMap.size(); j++) {
				List<String> list = columnMap.get(j);
				if (i >= list.size()) {
					timeToStop = true;
					continue;
				}
				int columnWidth = getColumnWidth(j);
				String text = columnMap.get(j).get(i);
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
		StringBuilder titleBuilder = new StringBuilder();
		titleBuilder.append("\n");
		if (longestRow > title.length() + 2) {
			int titleLengthPlusPadding = title.length() + 2;
			String padding = " ";
			String emphasis = getRepeatingChars('=', (longestRow - titleLengthPlusPadding) / 2);
			String extra = "";
			if (titleLengthPlusPadding % 2 == 1) {
				extra = "=";
			}
			titleBuilder.append(emphasis).append(padding).append(title).append(padding).append(emphasis).append(extra).append("\n\n");
		} else {
			titleBuilder.append("= ").append(title).append(" =").append("\n\n");
		}
		titleBuilder.append(table);
		return titleBuilder.toString();
	}

	private String getPadding(int paddingRequired) {
		return getRepeatingChars(' ', paddingRequired);
	}

	private String getUnderline(int underlineRequired) {
		return getRepeatingChars('-', underlineRequired);
	}

	private String getRepeatingChars(char c, int repeat) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < repeat; i++) {
			sb.append(c);
		}
		return sb.toString();
	}
}
