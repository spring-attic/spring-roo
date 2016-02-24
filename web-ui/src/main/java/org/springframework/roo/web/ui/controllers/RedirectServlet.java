package org.springframework.roo.web.ui.controllers;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Redirect HTTP request to the given URL.
 * 
 * @author Juan Carlos García
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 2.0
 */
public class RedirectServlet extends HttpServlet {

  /**  */
  private static final long serialVersionUID = 1L;

  private String redirectToURL;

  /**
   * Configure with the URL to which this servlet must redirect.
   * 
   * @param url URL to redirect to.
   */
  public RedirectServlet(String url) {
    this.redirectToURL = url;
  }

  /**
   * Redirect to {@link #redirectToURL}
   * 
   * @param request
   * @param response
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    // Set response content type
    response.setContentType("text/html");

    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    response.setHeader("Location", this.redirectToURL);
  }

  /**
   * Delegates on {@link #doGet(HttpServletRequest, HttpServletResponse)}
   * 
   * @param request
   * @param response
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doGet(request, response);
  }
}
