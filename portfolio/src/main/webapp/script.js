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
function getRandomFacts() {
    const funFactElement = document.getElementById('fun-fact');
    fetch('/data').then(response => response.json()).then((facts) => {
    funFactElement.innerHTML = '<ul>';
    for (let i = 0; i < facts.length; i++) {
        funFactElement.appendChild(createListElement(facts[i]));
      }
    funFactElement.appendChild('</ul>');    
  });
}

/** Creates an <li> element containing text. */
function createListElement(text) {
  const liElement = document.createElement('li');
  liElement.innerText = text;
  return liElement;
}
