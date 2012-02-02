package __TOP_LEVEL_PACKAGE__.client.scaffold.place;

import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.text.shared.Renderer;

import java.util.Collection;

/**
 * A renderer for Collections that is parameterized by another renderer.
 */
public class CollectionRenderer<E, R extends Renderer<E>, T extends Collection<E>> extends AbstractRenderer<T> implements Renderer<T> {

	public static <E, R extends Renderer<E>, T extends Collection<E>> CollectionRenderer<E, R, T> of(R r) {
		return new CollectionRenderer<E, R, T>(r);
	}

	private R elementRenderer;

	public CollectionRenderer(R elementRenderer) {
		this.elementRenderer = elementRenderer;
	}

	@Override
	public String render(T t) {
		StringBuilder toReturn = new StringBuilder();
		boolean first = true;
		if (t != null) {
			for (E e : t) {
				if (!first) {
					toReturn.append(',');
				} else {
					first = false;
				}
				toReturn.append(elementRenderer.render(e));
			}
		}
		return toReturn.toString();
	}
}
