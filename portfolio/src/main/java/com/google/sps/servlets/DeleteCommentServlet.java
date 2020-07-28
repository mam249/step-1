// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.sps.utils.Constants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/* Servlet that in the Post request deletes a comment with id that was passed as a parameter
 * The comment will be deleted only if the current user is the comment owner or admin */
@WebServlet("/delete-mine")
public class DeleteCommentServlet extends HttpServlet {

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    if (!userService.isUserLoggedIn()) {
      String redirectUrl = "/index.html#comments";
      String loginUrl = userService.createLoginURL(redirectUrl);
      response.sendRedirect(loginUrl);
      return;
    }

    response.setContentType("application/json;");
    Gson gson = new Gson();
    String commentId = request.getParameter(Constants.PARAMETER_COMMENT_ID);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key key = KeyFactory.createKey(Constants.ENTITY_COMMENT, Long.parseLong(commentId));
    try {
      Entity entity = datastore.get(key);
      if (userService.getCurrentUser().getUserId().equals((String) entity.getProperty(Constants.PROPERTY_USER_ID))
          || userService.isUserAdmin()) {
        datastore.delete(entity.getKey());
        response.getWriter().println(gson.toJson(true));
        response.sendRedirect("/index.html#comments");
      }
    } catch (Exception e) {
      response.getWriter().println(gson.toJson(false));
      response.sendRedirect("/index.html#comments");
    }
  }
}
