package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import __TOP_LEVEL_PACKAGE__.client.scaffold.request.RequestEvent;

/**
 * A request event handler which listens to every request and reacts if there
 * is an authentication problem. Note that the server side code is responsible
 * for making sure that no sensitive information is returned in case of
 * authentication issues. This handler is just responsible for making such
 * failures user friendly.
 */
public class AuthenticationFailureHandler implements RequestEvent.Handler {
  private String lastSeenUser = null;
  
  public void onRequestEvent(RequestEvent requestEvent) {
//    if (requestEvent.getState() == State.RECEIVED) {
//      Response response = requestEvent.getResponse();
//      if (response == null) {
//        // We should only get to this state if the RPC failed, in which
//        // case we went through the RequestCallback.onError() code path
//        // already and we don't need to do any additional error handling
//        // here, but we don't want to throw further exceptions.
//        return;
//      }
//      if (Response.SC_UNAUTHORIZED == response.getStatusCode()) {
//        String loginUrl = response.getHeader("login");
//        Location.replace(loginUrl);
//      }
//      String newUser = response.getHeader("userId");
//      if (lastSeenUser == null) {
//        lastSeenUser = newUser;
//      } else if (!lastSeenUser.equals(newUser)) {
//        // A new user has logged in, just reload the app and start over
//        Location.reload();
//      }
//    }
  }

}
