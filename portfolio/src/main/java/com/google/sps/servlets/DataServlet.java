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
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.gson.Gson;
import com.google.sps.data.Comment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/* Servlet that: 
 * in Get request returns N comments from Datastore where N is a parameter called limit; 
 * in Post request adds a comment entity into the Datastore 
 */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  private static final String PROPERTY_NAME = "name";
  private static final String PROPERTY_COMMENT = "comment";
  private static final String PROPERTY_TIMESTAMP = "timestamp";
  private static final String ENTITY_COMMENT = "Comment";
  private static final String PARAMETER_LIMIT = "limit";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query query = new Query(ENTITY_COMMENT).addSort(PROPERTY_TIMESTAMP, SortDirection.DESCENDING);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    int limit = Integer.parseInt(request.getParameter(PARAMETER_LIMIT));
    List<Entity> results = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(limit));

    List<Comment> comments = new ArrayList<>();
    for (Entity entity : results) {
      long id = entity.getKey().getId();
      String name = (String) entity.getProperty(PROPERTY_NAME);
      String cmt = (String) entity.getProperty(PROPERTY_COMMENT);
      long timestamp = (long) entity.getProperty(PROPERTY_TIMESTAMP);

      Comment comment = new Comment(id, name, cmt, timestamp);
      comments.add(comment);
    }

    Gson gson = new Gson();

    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(comments));
  }

   @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Entity commentEntity = new Entity(ENTITY_COMMENT);
    long timestamp = System.currentTimeMillis();

    commentEntity.setProperty(PROPERTY_NAME, request.getParameter(PROPERTY_NAME));
    commentEntity.setProperty(PROPERTY_COMMENT, request.getParameter(PROPERTY_COMMENT));
    commentEntity.setProperty(PROPERTY_TIMESTAMP, timestamp);
    
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);
    response.sendRedirect("/index.html#comments");
  }
}
