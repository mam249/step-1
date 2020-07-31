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
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;
import com.google.gson.Gson;
import com.google.sps.data.Comment;
import com.google.sps.utils.Constants;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/* Servlet that: 
 * in Get request returns N comments from Datastore where N is a parameter called limit; 
 * in Post request adds a comment entity into the Datastore
 *          and if the user's nickname and input name are different
 *          updates the user info entity in the Datastore    
 */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    Query commentQuery = new Query(Constants.ENTITY_COMMENT).addSort(Constants.PROPERTY_TIMESTAMP, SortDirection.DESCENDING);
    int limit = Integer.parseInt(request.getParameter(Constants.PARAMETER_LIMIT));
    List<Entity> commentResults = datastore.prepare(commentQuery).asList(FetchOptions.Builder.withLimit(limit));
    List<Comment> comments = new ArrayList<>();
    if (!commentResults.isEmpty()) {
      HashMap<String, String> userNicknames = getUserNicknames(datastore);
      for (Entity entity : commentResults) {
        comments.add(getCommentFromEntity(entity, userNicknames));
      }
    }
    response.setContentType("application/json;");
    response.getWriter().println(new Gson().toJson(comments));
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    if (!userService.isUserLoggedIn()) {
      String redirectUrl = "/index.html#comments";
      String loginUrl = userService.createLoginURL(redirectUrl);
      response.sendRedirect(loginUrl);
      return;
    }

    Entity commentEntity = createCommentEntity(userService, request);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);
    maybeUpdateUserNickname(userService, request, datastore);
    response.sendRedirect("/index.html#comments");
  }

  /* Given a message returns its sentiment score rounded to 3 decimal places */
  private String getSentimentAnalysis(String message) throws IOException {
    Document doc =
        Document.newBuilder().setContent(message).setType(Document.Type.PLAIN_TEXT).build();
    LanguageServiceClient languageService = LanguageServiceClient.create();
    Sentiment sentiment = languageService.analyzeSentiment(doc).getDocumentSentiment();
    if (sentiment == null) {
        return "No sentiment found";
    }
    String score = new DecimalFormat("#.###").format(sentiment.getScore());
    languageService.close();
    return score;
  }

  /* Returns a HashMap of <userId, nickname> */
  private HashMap<String, String> getUserNicknames(DatastoreService datastore) {
    Query userInfoQuery = new Query(Constants.ENTITY_USER_INFO);
    PreparedQuery userInfoResults = datastore.prepare(userInfoQuery);
    HashMap<String, String> userNicknames = new HashMap<>();
    for (Entity entity : userInfoResults.asIterable()) {
      String userId = (String) entity.getProperty(Constants.PROPERTY_USER_ID);
      String nickname = (String) entity.getProperty(Constants.PROPERTY_NICKNAME);
      userNicknames.put(userId, nickname);
    }      
    return userNicknames;
  }

  private Comment getCommentFromEntity(Entity entity, HashMap<String, String> userNicknames) {
    long id = entity.getKey().getId();
    String userId = (String) entity.getProperty(Constants.PROPERTY_USER_ID);
    String name = userNicknames.get(userId);
    String commentText = (String) entity.getProperty(Constants.PROPERTY_COMMENT);
    long timestamp = (long) entity.getProperty(Constants.PROPERTY_TIMESTAMP);
    String sentiment = (String) entity.getProperty(Constants.PROPERTY_SENTIMENT);

    return new Comment(id, userId, name, commentText, timestamp, sentiment);
  }

  private Entity createCommentEntity(UserService userService, HttpServletRequest request) throws IOException{
    long timestamp = System.currentTimeMillis();
    String userId = userService.getCurrentUser().getUserId();
    String nickname = request.getParameter(Constants.PROPERTY_NICKNAME);
    String name = request.getParameter(Constants.PROPERTY_NAME);
    String commentText = request.getParameter(Constants.PROPERTY_COMMENT);
    String sentimentScore = getSentimentAnalysis(commentText);

    Entity commentEntity = new Entity(Constants.ENTITY_COMMENT);
    commentEntity.setProperty(Constants.PROPERTY_NAME, name);
    commentEntity.setProperty(Constants.PROPERTY_USER_ID, userId);
    commentEntity.setProperty(Constants.PROPERTY_COMMENT, commentText);
    commentEntity.setProperty(Constants.PROPERTY_TIMESTAMP, timestamp);
    commentEntity.setProperty(Constants.PROPERTY_SENTIMENT, sentimentScore);

    return commentEntity;
  }

  /* If the input name in the request is different from nickname, updates 
   * the nickname in datastore UserInfo
   */
  private void maybeUpdateUserNickname(UserService userService, HttpServletRequest request, DatastoreService datastore) {
    String nickname = request.getParameter(Constants.PROPERTY_NICKNAME);
    String updatedName = request.getParameter(Constants.PROPERTY_NAME);
    String userId = userService.getCurrentUser().getUserId();

    if (updatedName.equals(nickname)) {
      return;
    }
    Entity userInfoEntity = new Entity(Constants.ENTITY_USER_INFO, userId);
    userInfoEntity.setProperty(Constants.PROPERTY_USER_ID, userId);
    userInfoEntity.setProperty(Constants.PROPERTY_NICKNAME, updatedName);
    datastore.put(userInfoEntity);
  }
}
