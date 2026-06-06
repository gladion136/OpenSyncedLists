package eu.schmidt.systems.opensyncedlists.helpers;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import androidx.preference.PreferenceManager;

import java.util.Locale;

public class LocaleHelper
{
    public static Context attachBaseContext(Context context)
    {
        String languageCode =
            PreferenceManager.getDefaultSharedPreferences(context)
                .getString("language", "system");

        Locale locale = getLocale(languageCode);
        return setLocale(context, locale);
    }

    private static Context setLocale(Context context, Locale locale)
    {
        Locale.setDefault(locale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            Configuration config = context.getResources().getConfiguration();
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        }
        else
        {
            Configuration config = context.getResources().getConfiguration();
            config.locale = locale;
            context.getResources().updateConfiguration(config,
                context.getResources().getDisplayMetrics());
            return context;
        }
    }

    public static Locale getLocale(String languageCode)
    {
        if ("system".equals(languageCode))
        {
            return Locale.getDefault();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            return new Locale.Builder().setLanguage(languageCode).build();
        }

        return new Locale(languageCode);
    }
}
