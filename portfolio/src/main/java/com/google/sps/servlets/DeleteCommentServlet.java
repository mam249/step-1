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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/* Servlet that in the Post request deletes all comments if the logged in user is admin */
@WebServlet("/delete-mine")
public class DeleteCommentServlet extends HttpServlet {
  private static final String ENTITY_COMMENT = "Comment";
  private static final String PARAMETER_COMMENT_ID = "commentId";
  private static final String PROPERTY_COMMENT_ID = "ID/Name";
  private static final String PROPERTY_USER_ID = "userId";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    if (!userService.isUserLoggedIn()) {
      String urlToRedirectToAfterUserLogsIn = "/index.html#comments";
      String loginUrl = userService.createLoginURL(urlToRedirectToAfterUserLogsIn);
      response.sendRedirect(loginUrl);
      return;
    }

    response.setContentType("application/json;");
    Gson gson = new Gson();
    String commentId = request.getParameter(PARAMETER_COMMENT_ID);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key key = KeyFactory.createKey(ENTITY_COMMENT, Long.parseLong(commentId));
    try {
      Entity entity = datastore.get(key);
      if (userService.getCurrentUser().getUserId().equals((String) entity.getProperty(PROPERTY_USER_ID))
          || userService.isUserAdmin()) {
        datastore.delete(entity.getKey());
        System.out.println("here");
        response.getWriter().println(gson.toJson(true));
        response.sendRedirect("/index.html#comments");
      }
    } catch (Exception e) {
      response.getWriter().println(gson.toJson(false));
      response.sendRedirect("/index.html#comments");
    }
  }
}
