package app.weather.sharan.com.myweatherapp;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.google.api.translate.Language;
import com.google.api.translate.Translator;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }
    // "Bună ziua!"
   // https://translate.googleapis.com/translate_a/single?client=gtx&sl="+ English+ "&tl=" + Kannada+ "&dt=t&q=" + encodeURI("Bengaluru")
}