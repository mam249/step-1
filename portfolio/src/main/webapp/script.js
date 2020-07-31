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

/**
 * On mobile, collapses the navbar if a link is clicked
 */
$('.navbar-nav>li>a').on('click', function () {
  $('.navbar-collapse').collapse('hide');
});

/* On the first load of the page, 
 * if localStorage has an item "limit" with value N, 
 * then fetches N comments 
 * else fetches the default number set by the page
 */
async function getComments() {
  const isLimitSet = null !== localStorage.getItem("limit");
  const limit = isLimitSet ? localStorage.getItem("limit") :
            document.getElementById('comment-limit').value;
  localStorage.setItem("limit", limit);
  document.getElementById("comment-limit").value = limit;
  fetchComments(limit);
}

/* On change of selected limit of displayed comments, 
 * sets the new limit in localStorage and fetches the selected number of comments
 */
async function getCommentsWithLimit() {
  const limit = document.getElementById('comment-limit').value;
  localStorage.setItem("limit", limit);
  fetchComments(limit);
}

/* Given a limit N, fetches N comments from /data and puts results into comments-text element */
async function fetchComments(limit) {
  document.getElementById('comments-spinner').style.display = "block";
  const commentsElement = document.getElementById('comments-text');
  const response = await fetch('/data?limit=' + limit);
  const comments = await response.json();
  commentsElement.innerHTML = '';
  for (let i = 0; i < comments.length; i++) {
    commentsElement.appendChild(createComment(comments[i]));
  }
  if (comments.length === 0) {
    commentsElement.innerText = "No comments";
  }
  document.getElementById('comments-spinner').style.display = "none";
}

/* Creates <div> element for a comment in format: "name: comment (delete button if the comment owner)" */
function createComment(comment) {
  const form = document.createElement('form');
  setAttributes(form,{'action': '/delete-mine','method': 'POST'});

  const div = document.createElement('div');
  div.setAttribute('class', 'row justify-content-between');

  const paragraph = document.createElement('p');
  paragraph.setAttribute('class', 'card-text');
  paragraph.innerText = comment.name + ": " + comment.comment;

  const sentiment = document.createElement('p');
  sentiment.setAttribute('class', 'card-text');
  sentiment.innerText = "[Sentiment: " + getSentiment(comment.sentiment) + "]";

  div.appendChild(paragraph);
  div.appendChild(sentiment);
  if (comment.userId === localStorage.getItem("userId") || localStorage.getItem("isAdmin")) {
    const deleteButton = document.createElement('button');
    setAttributes(deleteButton, {'type': 'submit', 'class': 'btn btn-dark delete-comment btn-sm'})
    deleteButton.innerText = "X";
    div.appendChild(deleteButton);

    const commentId = document.createElement('input');
    setAttributes(commentId, {'type': 'hidden', 'name': 'commentId', 'value': comment.id});
    div.appendChild(commentId);
  }

  form.appendChild(div);
  return form;
}

/* Deletes all comments by calling /delete-data and refreshes comments-text element */
async function deleteComments() {
  const request = new Request('/delete-all', {method: 'POST'});
  const response = await fetch(request);
  const isDeleted = await response.json();

  if (isDeleted) {
    getComments();
  } else {
    $('#adminModal').modal('toggle');
  }
}

/* If user is logged in: display comment submission form,
 * If user is logged in and admin: display comment submission form 
 *                                   and delete all comments button,
 * else: display login link
 */
async function displayCommentsForm() {
  document.getElementById('comments-form-spinner').style.display = "block";
  const response = await fetch("/login-status");
  const loginInfo = await response.json();
  const loginForm = document.getElementById('login-form');
  const commentForm = document.getElementById('comment-form');
  const submitForm = document.getElementById('submit-form');
  const nicknameForm = document.getElementById('nickname-form');

  if (loginInfo.isLoggedIn) {
    loginForm.style.display = "none";
    commentForm.style.display = "block";
    document.getElementById('logout-url').href = loginInfo.url;
  } else {
    loginForm.style.display = "block";
    commentForm.style.display = "none";
    document.getElementById('login-url').href = loginInfo.url; 
  }

  document.getElementById("inputNickname").value = loginInfo.nickname;
  if (loginInfo.nickname === "") {
    nicknameForm.style.display = "block";
    submitForm.style.display = "none";
  } else {
    submitForm.style.display = "block";      
  }

  if (loginInfo.userId) {
    localStorage.setItem("userId", loginInfo.userId);
  } else {
    localStorage.removeItem("userId");
  }

  document.getElementById('comments-form-spinner').style.display = "none";

  document.getElementById('comments-spinner').style.display = "block";
  const deleteCommentsButton = document.getElementById('delete-comments-btn');
  if (loginInfo.isAdmin) {
    deleteCommentsButton.style.display = "block";
    localStorage.setItem("isAdmin", true);
  } else {
    deleteCommentsButton.style.display = "none";
    localStorage.removeItem("isAdmin");
  }
  document.getElementById('comments-spinner').style.display = "none";
}

async function bodyOnLoad() {
  displayCommentsForm();
  getComments();
}

function setAttributes(element, attributes) {
  Object.keys(attributes).forEach(function(key) {
    element.setAttribute(key, attributes[key]);
  })
}

async function translateText() {
  await getComments();
  const languageCode = document.getElementById('language').value;
  if (languageCode === "en") {
    return;
  }
  document.getElementById('comments-spinner').style.display = "block";
  const textElement =  document.getElementById("comments-text");
  const response = await fetch('/translate?languageCode=' + languageCode + '&text=' + encodeURIComponent(textElement.innerHTML));
  const translatedHTML = await response.text();
  textElement.innerHTML = translatedHTML;
  document.getElementById('comments-spinner').style.display = "none";
}

function getSentiment(score) {
  const value = parseFloat(score);

  const clearly_positive = 0.8;
  const positive = 0.4;
  const neutral = 0;
  const negative = -0.4;

  if (isNaN(value)) {
    return "❔";  
  } else if (value >= clearly_positive) {
    return "😍";
  } else if (value >= positive) {
    return "😃";
  } else if (value >= neutral) {
    return "😐";
  } else if (value >= negative) {
    return "😒";
  } else {
    return "😤";
  }
}
