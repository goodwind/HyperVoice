package io.github.goodwind.hypervoice

/** The page a phone gets: one field for the Whisper (Groq) key, in the language of the phone. */
object SetupPage {
    fun html(token: String): String = """<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="dark">
<title>HyperVoice</title>
<style>
  body { margin: 0; padding: 24px 16px; background: #111113; color: #fff; font: 17px/1.45 system-ui, sans-serif; }
  main { max-width: 480px; margin: 0 auto; }
  h1 { font-size: 26px; margin: 0 0 4px; }
  p { color: #a5a5ad; margin: 8px 0 18px; }
  a { color: #8ab4ff; }
  input { width: 100%; box-sizing: border-box; padding: 14px; border-radius: 12px; border: 1px solid #343438;
          background: #242427; color: #fff; font: 16px ui-monospace, monospace; }
  button { width: 100%; margin-top: 14px; padding: 14px; border: 0; border-radius: 14px; background: #f5f5f7;
           color: #1d1d1f; font: 600 17px system-ui, sans-serif; }
  button:disabled { opacity: .5; }
  #msg { min-height: 1.5em; margin-top: 14px; }
  .ok { color: #32d74b; } .bad { color: #ff453a; } .warn { color: #ff9f0a; }
</style></head><body><main>
<h1>HyperVoice</h1>
<p id="lead"></p>
<input id="key" type="password" autocomplete="off" autocapitalize="off" spellcheck="false" placeholder="gsk_…">
<button id="go"></button>
<div id="msg"></div>
</main>
<script>
var ru = (navigator.language || '').toLowerCase().indexOf('ru') === 0;
var T = ru ? {
  lead: 'Вставьте ключ Groq для Whisper. Получить его можно бесплатно на <a href="https://console.groq.com/keys" rel="noreferrer">console.groq.com/keys</a>.',
  go: 'Сохранить ключ', busy: 'Проверяю…', ok: 'Готово: ключ сохранён на ТВ.',
  warn: 'Ключ сохранён, но проверить его не удалось (нет связи с Groq).',
  invalid: 'Groq не принял этот ключ. Проверьте, что скопировали его целиком.',
  empty: 'Введите ключ.', fail: 'Не получилось связаться с ТВ. Откройте QR-код на экране заново.'
} : {
  lead: 'Paste your Groq key for Whisper. You can get one for free at <a href="https://console.groq.com/keys" rel="noreferrer">console.groq.com/keys</a>.',
  go: 'Save the key', busy: 'Checking…', ok: 'Done: the key is saved on the TV.',
  warn: 'The key is saved, but could not be checked (Groq is not reachable).',
  invalid: 'Groq did not accept this key. Make sure you copied all of it.',
  empty: 'Enter the key.', fail: 'Could not reach the TV. Open the QR code on its screen again.'
};
document.getElementById('lead').innerHTML = T.lead;
var go = document.getElementById('go'), msg = document.getElementById('msg'), key = document.getElementById('key');
go.textContent = T.go;
function say(text, cls) { msg.textContent = text; msg.className = cls; }
go.onclick = function () {
  var v = key.value.trim();
  if (!v) { say(T.empty, 'bad'); return; }
  go.disabled = true; say(T.busy, '');
  var x = new XMLHttpRequest();
  x.open('POST', '/$token/key');
  x.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
  x.onload = function () {
    go.disabled = false;
    try {
      var r = JSON.parse(x.responseText);
      if (r.ok) { key.value = ''; say(r.warning ? T.warn : T.ok, r.warning ? 'warn' : 'ok'); }
      else say(r.error === 'empty' ? T.empty : T.invalid, 'bad');
    } catch (e) { say(T.fail, 'bad'); }
  };
  x.onerror = function () { go.disabled = false; say(T.fail, 'bad'); };
  x.send('key=' + encodeURIComponent(v));
};
</script></body></html>"""
}
