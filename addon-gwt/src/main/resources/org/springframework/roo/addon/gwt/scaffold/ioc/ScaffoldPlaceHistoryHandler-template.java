package __TOP_LEVEL_PACKAGE__.gwt.scaffold.ioc;

import com.google.gwt.app.place.PlaceHistoryHandlerWithFactory;

/**
 * This interface is the hub of your application's navigation system. It links
 * the {@link com.google.gwt.app.place.Place Place}s your user navigates to with
 * the browser history system &mdash; that is, it makes the browser's back and
 * forth buttons work for you, and also makes each spot in your app
 * bookmarkable. The instance used by this app is available via
 * {@link PlaceHistoryFactory#getPlaceHistoryHandler()}.
 * 
 * <p>
 * The simplest way to make new {@link com.google.gwt.app.place.Place Place}
 * types available to your app is to uncomment the {@literal @}WithTokenizers
 * annotation below and list their corresponding
 * {@link com.google.gwt.app.place.PlaceTokenizer PlaceTokenizer}s.
 * 
 * <p>
 * This code generated object looks to both the {@literal @}WithTokenizers
 * annotation and the factory to infer the types of
 * {@link com.google.gwt.app.place.Place Place}s your app can navigate to. In
 * this case it will find the {@link PlaceHistoryFactory#getProxyListPlaceTokenizer()} and
 * {@link PlaceHistoryFactory#getProxyPlaceTokenizer()} methods, and so be able to handle
 * {@link com.google.gwt.app.place.ProxyListPlace ProxyListPlace}s (which show
 * all entities of a particular type) and
 * {@link com.google.gwt.app.place.ProxyPlace ProxyPlace}s (which give access to
 * an individual entity).
 */
// @WithTokenizers({MyNewPlace.Tokenizer, MyOtherNewPlace.Tokenizer})
interface ScaffoldPlaceHistoryHandler extends PlaceHistoryHandlerWithFactory<PlaceHistoryFactory> {
}
