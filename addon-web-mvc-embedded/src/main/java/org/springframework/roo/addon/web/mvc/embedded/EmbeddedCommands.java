package org.springframework.roo.addon.web.mvc.embedded;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.embedded.provider.DocumentEmbeddedProvider.DocumentProvider;
import org.springframework.roo.addon.web.mvc.embedded.provider.PhotoEmbeddedProvider.PhotoProvider;
import org.springframework.roo.addon.web.mvc.embedded.provider.VideoEmbeddedProvider.VideoProvider;
import org.springframework.roo.addon.web.mvc.embedded.provider.VideoStreamEmbeddedProvider.VideoStreamProvider;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;

/**
 * Commands for 'web mvc embed'.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class EmbeddedCommands implements CommandMarker {

	@Reference private EmbeddedOperations operations;
	@Reference private StaticFieldConverter fieldConverter;
	
	protected void activate(ComponentContext context) {
		fieldConverter.add(VideoProvider.class);
		fieldConverter.add(DocumentProvider.class);
		fieldConverter.add(VideoStreamProvider.class);
		fieldConverter.add(PhotoProvider.class);
	}

	protected void deactivate(ComponentContext context) {
		fieldConverter.remove(VideoProvider.class);
		fieldConverter.remove(DocumentProvider.class);
		fieldConverter.remove(VideoStreamProvider.class);
		fieldConverter.remove(PhotoProvider.class);
	}
	
	@CliAvailabilityIndicator({"web mvc embed generic", "web mvc embed wave", "web mvc embed map", "web mvc embed document", "web mvc embed video", "web mvc embed photos", "web mvc embed stream video", "web mvc embed finances", "web mvc embed twitter"})
	public boolean isPropertyAvailable() {
		 return operations.isCommandAvailable();
	}
	
	@CliCommand(value="web mvc embed generic", help="Embed media by URL into your WEB MVC application")
	public void embed(@CliOption(key="url", mandatory=true, help="The url of the source to be embedded") String url, 
			@CliOption(key="viewName", mandatory=false, help="The name of the jspx view") String viewName) {
		operations.embed(url, viewName);
	}
	
	@CliCommand(value="web mvc embed wave", help="Embed Google wave integration for your WEB MVC application")
	public void embedWave(@CliOption(key="waveId", mandatory=true, help="The key of the wave") String key, 
			@CliOption(key="viewName", mandatory=false, help="The name of the jspx view") String viewName) {
		Map<String, String> options = new HashMap<String, String>();
		options.put("provider", "GOOGLE_WAVE");
		options.put("id", key);
		operations.install(viewName, options);
	}
	
	@CliCommand(value="web mvc embed document", help="Embed a document for your WEB MVC application")
	public void embedDocument(@CliOption(key="provider", mandatory=true, help="The id of the document") DocumentProvider provider,
			@CliOption(key="documentId", mandatory=true, help="The id of the document") String id, 
			@CliOption(key="viewName", mandatory=false, help="The name of the jspx view") String viewName) {
		Map<String, String> options = new HashMap<String, String>();
		options.put("provider", provider.name());
		options.put("id", id);
		operations.install(viewName, options);
	}
	
	@CliCommand(value="web mvc embed video", help="Embed a video for your WEB MVC application")
	public void embedVideo(@CliOption(key="provider", mandatory=true, help="The id of the video") VideoProvider provider,
			@CliOption(key="videoId", mandatory=true, help="The id of the video") String id, 
			@CliOption(key="viewName", mandatory=false, help="The name of the jspx view") String viewName) {
		Map<String, String> options = new HashMap<String, String>();
		options.put("provider", provider.name());
		options.put("id", id);
		operations.install(viewName, options); 
	}
	
	@CliCommand(value="web mvc embed map", help="Embed a map for your WEB MVC application")
	public void embedMap(@CliOption(key="location", mandatory=true, help="The location of the map (ie \"Sydney, Australia\")") String location, 
			@CliOption(key="viewName", mandatory=false, help="The name of the jspx view") String viewName) {
		Map<String, String> options = new HashMap<String, String>();
		options.put("provider", "GOOGLE_MAPS");
		options.put("location", location);
		operations.install(viewName, options);
	}
	
	@CliCommand(value="web mvc embed photos", help="Embed a photo gallery for your WEB MVC application")
	public void embedPhotos(@CliOption(key="provider", mandatory=true, help="The provider of the photo gallery") PhotoProvider provider,
			@CliOption(key="userId", mandatory=true, help="The user id") String userId, 
			@CliOption(key="albumId", mandatory=true, help="The album id") String albumId, 
			@CliOption(key="viewName", mandatory=false, help="The name of the jspx view") String viewName) {
		Map<String, String> options = new HashMap<String, String>();
		options.put("provider", provider.name());
		options.put("userId", userId);
		options.put("albumId", albumId);
		operations.install(viewName, options);
	}
	
	@CliCommand(value="web mvc embed stream video", help="Embed a video stream into your WEB MVC application")
	public void embedVideoStream(@CliOption(key="provider", mandatory=true, help="The provider of the video stream") VideoStreamProvider provider,
			@CliOption(key="streamId", mandatory=true, help="The stream id") String streamId, 
			@CliOption(key="viewName", mandatory=false, help="The name of the jspx view") String viewName) {
		Map<String, String> options = new HashMap<String, String>();
		options.put("provider", provider.name());
		options.put("id", streamId);
		operations.install(viewName, options); 
	}
	
	// TODO: disabled due to ROO-2562
//	@CliCommand(value="web mvc embed finances", help="Embed a stock ticker into your WEB MVC application")
//	public void embedFinance(@CliOption(key="stockSymbol", mandatory=true, help="The stock symbol") String stockSymbol, 
//			@CliOption(key="viewName", mandatory=false, help="The name of the jspx view") String viewName) {
//		Map<String, String> options = new HashMap<String, String>();
//		options.put("provider", "FINANCES");
//		options.put("stockSymbol", stockSymbol);
//		operations.install(viewName, options); 
//	}
	
	@CliCommand(value="web mvc embed twitter", help="Embed twitter messages into your WEB MVC application")
	public void embedTwitter(@CliOption(key="searchTerm", mandatory=true, help="The search term to display results for") String searchTerm, 
			@CliOption(key="viewName", mandatory=false, help="The name of the jspx view") String viewName) {
		Map<String, String> options = new HashMap<String, String>();
		options.put("provider", "TWITTER");
		options.put("searchTerm", searchTerm);
		operations.install(viewName, options); 
	}
}