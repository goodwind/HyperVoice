package io.github.goodwind.hypervoice

/** The page a phone gets: one field for the Whisper (Groq) key, in the language of the phone (the same 20 as the app). */
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
          background: #242427; color: #fff; font: 16px ui-monospace, monospace; direction: ltr; }
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
// lead, save, checking, ok, warn, invalid, empty, fail
var TEXTS = {"en": ["Paste your Groq key for Whisper. You can get one for free at {link}.", "Save the key", "Checking…", "Done: the key is saved on the TV.", "The key is saved, but could not be checked (Groq is not reachable).", "Groq did not accept this key. Make sure you copied all of it.", "Enter the key.", "Could not reach the TV. Open the QR code on its screen again."], "ru": ["Вставьте ключ Groq для Whisper. Получить его можно бесплатно на {link}.", "Сохранить ключ", "Проверяю…", "Готово: ключ сохранён на ТВ.", "Ключ сохранён, но проверить его не удалось (нет связи с Groq).", "Groq не принял этот ключ. Проверьте, что скопировали его целиком.", "Введите ключ.", "Не получилось связаться с ТВ. Откройте QR-код на экране заново."], "uk": ["Вставте ключ Groq для Whisper. Отримати його безкоштовно можна на {link}.", "Зберегти ключ", "Перевіряю…", "Готово: ключ збережено на ТВ.", "Ключ збережено, але перевірити його не вдалося (немає зв’язку з Groq).", "Groq не прийняв цей ключ. Перевірте, що ви скопіювали його повністю.", "Введіть ключ.", "Не вдалося зв’язатися з ТВ. Відкрийте QR-код на екрані знову."], "kk": ["Whisper үшін Groq кілтін қойыңыз. Оны {link} сайтынан тегін алуға болады.", "Кілтті сақтау", "Тексеріп жатырмын…", "Дайын: кілт теледидарға сақталды.", "Кілт сақталды, бірақ оны тексеру мүмкін болмады (Groq-қа қосылу жоқ).", "Groq бұл кілтті қабылдамады. Оны толық көшіргеніңізді тексеріңіз.", "Кілтті енгізіңіз.", "Теледидарға қосылу мүмкін болмады. Экрандағы QR-кодты қайта ашыңыз."], "zhCN": ["粘贴您用于 Whisper 的 Groq 密钥。可在 {link} 免费获取。", "保存密钥", "正在检查…", "完成：密钥已保存到电视。", "密钥已保存，但无法验证（无法连接 Groq）。", "Groq 不接受此密钥。请确认已完整复制。", "请输入密钥。", "无法连接电视。请重新打开电视屏幕上的二维码。"], "zhTW": ["貼上您用於 Whisper 的 Groq 金鑰。可在 {link} 免費取得。", "儲存金鑰", "正在檢查…", "完成：金鑰已儲存到電視。", "金鑰已儲存，但無法驗證（無法連線 Groq）。", "Groq 不接受此金鑰。請確認已完整複製。", "請輸入金鑰。", "無法連線電視。請重新開啟電視螢幕上的 QR 碼。"], "es": ["Pega tu clave de Groq para Whisper. Puedes conseguir una gratis en {link}.", "Guardar la clave", "Comprobando…", "Listo: la clave se ha guardado en la TV.", "La clave se ha guardado, pero no se pudo comprobar (Groq no responde).", "Groq no aceptó esta clave. Comprueba que la copiaste entera.", "Introduce la clave.", "No se pudo contactar con la TV. Vuelve a abrir el código QR de su pantalla."], "pt": ["Cole sua chave do Groq para o Whisper. Você pode obter uma grátis em {link}.", "Salvar a chave", "Verificando…", "Pronto: a chave foi salva na TV.", "A chave foi salva, mas não foi possível verificá-la (o Groq não responde).", "O Groq não aceitou esta chave. Confirme que você a copiou inteira.", "Informe a chave.", "Não foi possível alcançar a TV. Abra de novo o código QR na tela dela."], "fr": ["Collez votre clé Groq pour Whisper. Vous pouvez en obtenir une gratuitement sur {link}.", "Enregistrer la clé", "Vérification…", "Terminé : la clé est enregistrée sur la TV.", "La clé est enregistrée, mais n’a pas pu être vérifiée (Groq est injoignable).", "Groq n’a pas accepté cette clé. Vérifiez que vous l’avez copiée en entier.", "Saisissez la clé.", "Impossible de joindre la TV. Rouvrez le code QR sur son écran."], "de": ["Fügen Sie Ihren Groq-Schlüssel für Whisper ein. Einen kostenlosen erhalten Sie unter {link}.", "Schlüssel speichern", "Wird geprüft…", "Fertig: Der Schlüssel ist auf dem Fernseher gespeichert.", "Der Schlüssel ist gespeichert, konnte aber nicht geprüft werden (Groq ist nicht erreichbar).", "Groq hat diesen Schlüssel nicht akzeptiert. Prüfen Sie, ob Sie ihn vollständig kopiert haben.", "Geben Sie den Schlüssel ein.", "Der Fernseher ist nicht erreichbar. Öffnen Sie den QR-Code auf seinem Bildschirm erneut."], "it": ["Incolla la tua chiave Groq per Whisper. Puoi ottenerne una gratis su {link}.", "Salva la chiave", "Verifica in corso…", "Fatto: la chiave è salvata sulla TV.", "La chiave è salvata, ma non è stato possibile verificarla (Groq non è raggiungibile).", "Groq non ha accettato questa chiave. Controlla di averla copiata per intero.", "Inserisci la chiave.", "Impossibile raggiungere la TV. Riapri il codice QR sul suo schermo."], "ja": ["Whisper 用の Groq キーを貼り付けてください。{link} で無料で取得できます。", "キーを保存", "確認中…", "完了：キーをテレビに保存しました。", "キーは保存されましたが、確認できませんでした（Groq に接続できません）。", "Groq がこのキーを受け付けませんでした。全体をコピーしたか確認してください。", "キーを入力してください。", "テレビに接続できません。テレビ画面の QR コードを開き直してください。"], "ko": ["Whisper용 Groq 키를 붙여넣으세요. {link}에서 무료로 받을 수 있습니다.", "키 저장", "확인 중…", "완료: 키가 TV에 저장되었습니다.", "키는 저장되었지만 확인하지 못했습니다(Groq에 연결할 수 없음).", "Groq가 이 키를 받아들이지 않았습니다. 전체를 복사했는지 확인하세요.", "키를 입력하세요.", "TV에 연결할 수 없습니다. TV 화면의 QR 코드를 다시 여세요."], "ar": ["الصق مفتاح Groq الخاص بـ Whisper. يمكنك الحصول عليه مجانًا من {link}.", "حفظ المفتاح", "جارٍ التحقق…", "تم: حُفظ المفتاح على التلفاز.", "حُفظ المفتاح لكن تعذّر التحقق منه (تعذّر الوصول إلى Groq).", "لم يقبل Groq هذا المفتاح. تأكد من نسخه كاملًا.", "أدخل المفتاح.", "تعذّر الوصول إلى التلفاز. افتح رمز QR على شاشته مرة أخرى."], "hi": ["Whisper के लिए अपनी Groq कुंजी चिपकाएँ। इसे {link} पर मुफ़्त पाया जा सकता है।", "कुंजी सहेजें", "जाँच रहा है…", "हो गया: कुंजी टीवी पर सहेज ली गई।", "कुंजी सहेज ली गई, लेकिन उसकी जाँच नहीं हो सकी (Groq तक पहुँच नहीं)।", "Groq ने यह कुंजी स्वीकार नहीं की। जाँचें कि आपने उसे पूरा कॉपी किया है।", "कुंजी दर्ज करें।", "टीवी तक पहुँच नहीं हो सकी। उसकी स्क्रीन पर QR कोड फिर से खोलें।"], "tr": ["Whisper için Groq anahtarınızı yapıştırın. Ücretsiz bir tane {link} adresinden alabilirsiniz.", "Anahtarı kaydet", "Denetleniyor…", "Tamam: anahtar TV’ye kaydedildi.", "Anahtar kaydedildi ancak denetlenemedi (Groq’a ulaşılamıyor).", "Groq bu anahtarı kabul etmedi. Tamamını kopyaladığınızdan emin olun.", "Anahtarı girin.", "TV’ye ulaşılamadı. Ekranındaki QR kodunu yeniden açın."], "pl": ["Wklej swój klucz Groq dla Whisper. Darmowy zdobędziesz na {link}.", "Zapisz klucz", "Sprawdzam…", "Gotowe: klucz zapisano w telewizorze.", "Klucz zapisano, ale nie udało się go sprawdzić (brak połączenia z Groq).", "Groq nie przyjął tego klucza. Upewnij się, że skopiowano go w całości.", "Wpisz klucz.", "Nie udało się połączyć z telewizorem. Otwórz ponownie kod QR na jego ekranie."], "id": ["Tempel kunci Groq Anda untuk Whisper. Anda bisa mendapatkannya gratis di {link}.", "Simpan kunci", "Memeriksa…", "Selesai: kunci tersimpan di TV.", "Kunci tersimpan, tetapi tidak dapat diperiksa (Groq tidak terjangkau).", "Groq tidak menerima kunci ini. Pastikan Anda menyalinnya secara utuh.", "Masukkan kunci.", "Tidak dapat menjangkau TV. Buka kembali kode QR di layarnya."], "vi": ["Dán khóa Groq của bạn cho Whisper. Bạn có thể lấy miễn phí tại {link}.", "Lưu khóa", "Đang kiểm tra…", "Xong: khóa đã được lưu trên TV.", "Khóa đã được lưu nhưng không kiểm tra được (không kết nối được Groq).", "Groq không chấp nhận khóa này. Hãy chắc chắn bạn đã sao chép đầy đủ.", "Hãy nhập khóa.", "Không kết nối được với TV. Hãy mở lại mã QR trên màn hình TV."], "nl": ["Plak uw Groq-sleutel voor Whisper. U kunt er gratis een krijgen op {link}.", "Sleutel opslaan", "Controleren…", "Klaar: de sleutel is op de tv opgeslagen.", "De sleutel is opgeslagen, maar kon niet worden gecontroleerd (Groq is niet bereikbaar).", "Groq heeft deze sleutel niet geaccepteerd. Controleer of u hem volledig hebt gekopieerd.", "Voer de sleutel in.", "De tv is niet bereikbaar. Open de QR-code op het scherm opnieuw."]};
var tag = (navigator.language || 'en').toLowerCase();
var code = tag.indexOf('zh') === 0 ? (/tw|hk|mo|hant/.test(tag) ? 'zhTW' : 'zhCN') : tag.slice(0, 2);
if (code === 'in') code = 'id';
var t = TEXTS[code] || TEXTS.en;
if (code === 'ar') document.documentElement.dir = 'rtl';
var link = '<a href="https://console.groq.com/keys" rel="noreferrer">console.groq.com/keys</a>';
document.getElementById('lead').innerHTML = t[0].replace('{link}', link);
var go = document.getElementById('go'), msg = document.getElementById('msg'), key = document.getElementById('key');
go.textContent = t[1];
function say(text, cls) { msg.textContent = text; msg.className = cls; }
go.onclick = function () {
  var v = key.value.trim();
  if (!v) { say(t[6], 'bad'); return; }
  go.disabled = true; say(t[2], '');
  var x = new XMLHttpRequest();
  x.open('POST', '/${token}/key');
  x.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
  x.onload = function () {
    go.disabled = false;
    try {
      var r = JSON.parse(x.responseText);
      if (r.ok) { key.value = ''; say(r.warning ? t[4] : t[3], r.warning ? 'warn' : 'ok'); }
      else say(r.error === 'empty' ? t[6] : t[5], 'bad');
    } catch (e) { say(t[7], 'bad'); }
  };
  x.onerror = function () { go.disabled = false; say(t[7], 'bad'); };
  x.send('key=' + encodeURIComponent(v));
};
</script></body></html>"""
}
