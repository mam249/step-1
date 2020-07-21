// Copyright 2019 Google LLC
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

import java.io.IOException;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns fun facts */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
    private List<String> facts;

  @Override
  public void init() {
    facts = new ArrayList<>();
    facts.add("I once ate fried crickets! They tasted like fried food. "
    + "Nothing special.");
    facts.add("My favourite animal is the panda :)");
    facts.add("I finished music school and my instrument was piano. Funny enough, "
    + "I only started liking playing it once I left the music school.");
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Gson gson = new Gson();
    String json = gson.toJson(facts);
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }
}
