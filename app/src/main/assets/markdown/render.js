(function () {
  "use strict";

  function b64ToUtf8(b64) {
    var bin = atob(b64);
    var bytes = new Uint8Array(bin.length);
    for (var i = 0; i < bin.length; i++) {
      bytes[i] = bin.charCodeAt(i);
    }
    try {
      return new TextDecoder("utf-8").decode(bytes);
    } catch (e) {
      return bin;
    }
  }

  function sanitize(root) {
    root.querySelectorAll("script,iframe,object,embed,link,meta,style").forEach(function (n) {
      n.remove();
    });

    root.querySelectorAll("*").forEach(function (el) {
      Array.prototype.slice.call(el.attributes).forEach(function (attr) {
        var name = attr.name.toLowerCase();
        if (name.indexOf("on") === 0) el.removeAttribute(attr.name);
      });
    });

    root.querySelectorAll("a").forEach(function (a) {
      var href = a.getAttribute("href") || "";
      if (/^(https?:|app:|mailto:|#)/i.test(href)) {
        a.setAttribute("rel", "noopener noreferrer");
      } else {
        a.setAttribute("href", "#");
      }
    });
  }

  window.renderMarkdown = function (b64) {
    var root = document.getElementById("root");
    if (!root) return;
    if (!b64) { root.innerHTML = ""; return; }

    var md = b64ToUtf8(b64);
    var html = marked.parse(md, { gfm: true, breaks: true });
    root.innerHTML = html;
    sanitize(root);
  };
})();