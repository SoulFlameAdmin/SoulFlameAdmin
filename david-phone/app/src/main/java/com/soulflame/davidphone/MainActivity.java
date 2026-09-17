package com.soulflame.davidphone;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final int REQ_PERMISSIONS = 7001;
    private final String[] CORE_PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA
    };

    private TextView statusView;
    private TextView heardView;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean torchOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        tts = new TextToSpeech(this, this);
        buildUi();
        setStatus("DAVID LOCAL ENGINE · READY");
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(34), dp(22), dp(30));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("DAVID PHONE");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidth(dp(56)));

        TextView version = new TextView(this);
        version.setText("v0.1 · LOCAL COMMAND ENGINE · NO GPT");
        version.setTextColor(Color.rgb(0, 255, 170));
        version.setTextSize(13);
        version.setGravity(Gravity.CENTER);
        root.addView(version, fullWidth(dp(36)));

        statusView = new TextView(this);
        statusView.setTextColor(Color.LTGRAY);
        statusView.setTextSize(16);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, dp(14), 0, dp(14));
        root.addView(statusView, fullWidth(dp(70)));

        Button mic = new Button(this);
        mic.setText("🎙  ГОВОРИ");
        mic.setTextSize(22);
        mic.setAllCaps(false);
        mic.setOnClickListener(v -> startListening());
        root.addView(mic, fullWidth(dp(76)));

        Button permissions = new Button(this);
        permissions.setText("Разреши основните функции");
        permissions.setAllCaps(false);
        permissions.setOnClickListener(v -> requestCorePermissions());
        LinearLayout.LayoutParams pp = fullWidth(dp(60));
        pp.topMargin = dp(12);
        root.addView(permissions, pp);

        heardView = new TextView(this);
        heardView.setText("Кажи например:\n\n• Отвори Google\n• Отвори Edge\n• Потърси Hover H5\n• Звънни на Борко\n• Прати SMS на Борко: ще закъснея\n• Отвори обажданията\n• Пусни фенерчето\n• Спри фенерчето\n• Увеличи звука\n• Начало");
        heardView.setTextColor(Color.WHITE);
        heardView.setTextSize(17);
        heardView.setPadding(0, dp(28), 0, 0);
        root.addView(heardView, new LinearLayout.LayoutParams(-1, -2));

        setContentView(scroll);
    }

    private LinearLayout.LayoutParams fullWidth(int height) {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void requestCorePermissions() {
        List<String> missing = new ArrayList<>();
        for (String permission : CORE_PERMISSIONS) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) missing.add(permission);
        }
        if (missing.isEmpty()) {
            setStatus("Всички основни разрешения са активни.");
            speak("Готов съм.");
            return;
        }
        requestPermissions(missing.toArray(new String[0]), REQ_PERMISSIONS);
    }

    private boolean hasPermission(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void startListening() {
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_PERMISSIONS);
            setStatus("Разреши микрофона и натисни ГОВОРИ отново.");
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            setStatus("На телефона няма налична Speech Recognition услуга.");
            return;
        }

        if (recognizer != null) recognizer.destroy();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { setStatus("Слушам..."); }
            @Override public void onBeginningOfSpeech() { setStatus("Чувам те..."); }
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { setStatus("Разбирам командата..."); }
            @Override public void onError(int error) { setStatus("Не те разбрах. Натисни ГОВОРИ и повтори."); }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
            @Override public void onResults(Bundle results) {
                ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (list == null || list.isEmpty()) {
                    setStatus("Няма разпозната команда.");
                    return;
                }
                String heard = list.get(0);
                heardView.setText("Чух: \"" + heard + "\"");
                handleCommand(heard);
            }
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bg-BG");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bg-BG");
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        recognizer.startListening(intent);
    }

    private void handleCommand(String raw) {
        String command = normalize(raw);
        command = command.replaceFirst("^(дейвид|девид|david)[, ]*", "").trim();

        if (command.contains("потърси")) {
            String query = command.substring(command.indexOf("потърси") + "потърси".length()).trim();
            if (!query.isEmpty()) {
                openSearch(query);
                return;
            }
        }

        if (command.equals("отвори google") || command.equals("отвори гугъл") || command.equals("отвори браузъра") || command.equals("отвори браузър")) {
            openUrl("https://www.google.com");
            done("Отварям Google.");
            return;
        }

        if (command.startsWith("звънни на ") || command.startsWith("обади се на ") || command.startsWith("набери ")) {
            String name = command.replaceFirst("^(звънни на |обади се на |набери )", "").trim();
            callContact(name);
            return;
        }

        if (command.startsWith("прати sms на ") || command.startsWith("прати смс на ") || command.startsWith("изпрати sms на ") || command.startsWith("изпрати смс на ")) {
            sendSmsCommand(command);
            return;
        }

        if (command.equals("отвори обажданията") || command.equals("отвори телефона") || command.equals("отвори набиране")) {
            startActivity(new Intent(Intent.ACTION_DIAL));
            done("Отварям обажданията.");
            return;
        }

        if (command.equals("пусни фенерчето") || command.equals("включи фенерчето") || command.equals("фенерче")) {
            setTorch(true);
            return;
        }

        if (command.equals("спри фенерчето") || command.equals("изключи фенерчето")) {
            setTorch(false);
            return;
        }

        if (command.equals("увеличи звука") || command.equals("по-силно")) {
            adjustVolume(AudioManager.ADJUST_RAISE);
            done("Увеличавам звука.");
            return;
        }

        if (command.equals("намали звука") || command.equals("по-тихо")) {
            adjustVolume(AudioManager.ADJUST_LOWER);
            done("Намалявам звука.");
            return;
        }

        if (command.equals("начало") || command.equals("отвори началния екран") || command.equals("отвори home")) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
            done("Начален екран.");
            return;
        }

        if (command.equals("затвори david") || command.equals("затвори дейвид") || command.equals("затвори приложението")) {
            finishAndRemoveTask();
            return;
        }

        if (command.startsWith("отвори ")) {
            String target = command.substring("отвори ".length()).trim();
            if (openKnownOrInstalledApp(target)) return;
        }

        setStatus("Командата още не е в локалната карта: " + raw);
        speak("Тази команда още не е добавена.");
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(new Locale("bg", "BG")).trim()
                .replace('–', '-')
                .replaceAll("\\s+", " ");
    }

    private void openSearch(String query) {
        openUrl("https://www.google.com/search?q=" + Uri.encode(query));
        done("Търся: " + query);
    }

    private void openUrl(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(i);
    }

    private void callContact(String name) {
        if (!hasPermission(Manifest.permission.READ_CONTACTS) || !hasPermission(Manifest.permission.CALL_PHONE)) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE}, REQ_PERMISSIONS);
            setStatus("Разреши Контакти и Телефон, после повтори командата.");
            return;
        }
        ContactPhone cp = findContact(name);
        if (cp == null) {
            setStatus("Не намерих контакт: " + name);
            speak("Не намерих този контакт.");
            return;
        }
        try {
            Intent call = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(cp.phone)));
            startActivity(call);
            done("Звъня на " + cp.name + ".");
        } catch (Exception e) {
            setStatus("Не успях да стартирам обаждането: " + e.getMessage());
        }
    }

    private ContactPhone findContact(String name) {
        ContentResolver resolver = getContentResolver();
        String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
        String[] args = new String[]{"%" + name + "%"};
        try (Cursor c = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                args,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {
            if (c != null && c.moveToFirst()) {
                return new ContactPhone(c.getString(0), c.getString(1));
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void sendSmsCommand(String command) {
        if (!hasPermission(Manifest.permission.READ_CONTACTS) || !hasPermission(Manifest.permission.SEND_SMS)) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS}, REQ_PERMISSIONS);
            setStatus("Разреши Контакти и SMS, после повтори командата.");
            return;
        }

        int onIndex = command.indexOf(" на ");
        if (onIndex < 0) {
            smsFormatHelp();
            return;
        }
        String rest = command.substring(onIndex + 4).trim();
        int separator = rest.indexOf(':');
        if (separator < 0) separator = rest.indexOf(" - ");
        if (separator < 0) {
            smsFormatHelp();
            return;
        }

        String name = rest.substring(0, separator).trim();
        String message = rest.substring(separator + (rest.charAt(separator) == ':' ? 1 : 3)).trim();
        if (name.isEmpty() || message.isEmpty()) {
            smsFormatHelp();
            return;
        }

        ContactPhone cp = findContact(name);
        if (cp == null) {
            setStatus("Не намерих контакт: " + name);
            speak("Не намерих този контакт.");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("DAVID · SMS")
                .setMessage("До: " + cp.name + "\n\n" + message)
                .setNegativeButton("Отказ", null)
                .setPositiveButton("Изпрати", (dialog, which) -> sendSms(cp, message))
                .show();
    }

    private void smsFormatHelp() {
        setStatus("Кажи: Прати SMS на Борко: ще закъснея");
        speak("Кажи името, после двоеточие и текста.");
    }

    private void sendSms(ContactPhone cp, String message) {
        try {
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(message);
            if (parts.size() > 1) sms.sendMultipartTextMessage(cp.phone, null, parts, null, null);
            else sms.sendTextMessage(cp.phone, null, message, null, null);
            done("SMS е изпратен до " + cp.name + ".");
        } catch (Exception e) {
            setStatus("SMS грешка: " + e.getMessage());
        }
    }

    private boolean openKnownOrInstalledApp(String target) {
        String packageName = null;
        if (target.contains("edge") || target.contains("едж")) packageName = "com.microsoft.emmx";
        else if (target.contains("spotify") || target.contains("спотифай")) packageName = "com.spotify.music";
        else if (target.equals("maps") || target.contains("google maps") || target.contains("карти")) packageName = "com.google.android.apps.maps";
        else if (target.contains("youtube") || target.contains("ютуб")) packageName = "com.google.android.youtube";
        else if (target.contains("whatsapp") || target.contains("уатсап") || target.contains("ватсап")) packageName = "com.whatsapp";
        else if (target.contains("viber") || target.contains("вайбър")) packageName = "com.viber.voip";

        if (packageName != null) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch != null) {
                startActivity(launch);
                done("Отварям " + target + ".");
                return true;
            }
        }

        PackageManager pm = getPackageManager();
        String wanted = normalize(target);
        try {
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            ApplicationInfo best = null;
            for (ApplicationInfo app : apps) {
                CharSequence labelCs = pm.getApplicationLabel(app);
                String label = normalize(labelCs == null ? "" : labelCs.toString());
                if (label.equals(wanted)) { best = app; break; }
                if (best == null && (label.contains(wanted) || wanted.contains(label))) best = app;
            }
            if (best != null) {
                Intent launch = pm.getLaunchIntentForPackage(best.packageName);
                if (launch != null) {
                    startActivity(launch);
                    done("Отварям " + pm.getApplicationLabel(best) + ".");
                    return true;
                }
            }
        } catch (Exception ignored) {}

        setStatus("Не намерих приложение: " + target);
        speak("Не намерих приложението.");
        return false;
    }

    private void setTorch(boolean enabled) {
        if (!hasPermission(Manifest.permission.CAMERA)) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_PERMISSIONS);
            setStatus("Разреши Камера за управление на фенерчето и повтори.");
            return;
        }
        try {
            CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            for (String id : cm.getCameraIdList()) {
                Boolean flash = cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (Boolean.TRUE.equals(flash)) {
                    cm.setTorchMode(id, enabled);
                    torchOn = enabled;
                    done(enabled ? "Фенерчето е включено." : "Фенерчето е изключено.");
                    return;
                }
            }
            setStatus("Телефонът не отчита налична светкавица.");
        } catch (Exception e) {
            setStatus("Фенерче грешка: " + e.getMessage());
        }
    }

    private void adjustVolume(int direction) {
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI);
    }

    private void done(String message) {
        setStatus(message);
        speak(message);
    }

    private void setStatus(String text) {
        runOnUiThread(() -> statusView.setText(text));
    }

    private void speak(String text) {
        if (ttsReady && tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "david");
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("bg", "BG"));
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
        }
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) recognizer.destroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private static class ContactPhone {
        final String name;
        final String phone;
        ContactPhone(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }
    }
}
