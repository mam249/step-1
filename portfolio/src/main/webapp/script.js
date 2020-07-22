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

/**
 * On mobile, collapses the navbar if a link is clicked
 */
$('.navbar-nav>li>a').on('click', function () {
  $('.navbar-collapse').collapse('hide');
});

/**
 * Fetches a random fact from the server and adds it to the DOM.
 */
function getComments() {
  const commentsElement = document.getElementById('comments-text');
  const limit = document.getElementById('commentLimit').value;
  fetch('/data?limit=' + limit).then(response => response.json()).then((comments) => {
    commentsElement.innerHTML = '';
    for (let i = 0; i < comments.length; i++) {
      commentsElement.appendChild(createParagraph(comments[i]));
    }
  });
}

/** Creates an <li> element containing text. */
function createParagraph(text) {
  const paragraph = document.createElement('p');
  paragraph.setAttribute('class', 'card-text');
  paragraph.innerText = text.name + ": " + text.comment;
  return paragraph;
}

async function deleteComments() {
  const request = new Request('/delete-data', {method: 'POST'});
  const response = await fetch(request);
  const text = await response.text();
  console.log(text);
  getComments();
}
