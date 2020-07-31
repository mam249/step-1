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

/* Servlet that in the Post request deletes all comments if the logged in user is admin */
@WebServlet("/update-nickname")
public class UpdateNicknameServlet extends HttpServlet {

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    String userId = userService.getCurrentUser().getUserId();
    String newNickname = request.getParameter(Constants.PROPERTY_NICKNAME);

    Entity userInfoEntity = new Entity(Constants.ENTITY_USER_INFO, userId);
    userInfoEntity.setProperty(Constants.PROPERTY_USER_ID, userId);
    userInfoEntity.setProperty(Constants.PROPERTY_NICKNAME, newNickname);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(userInfoEntity);
    response.sendRedirect("/index.html#comments");
  }
}
