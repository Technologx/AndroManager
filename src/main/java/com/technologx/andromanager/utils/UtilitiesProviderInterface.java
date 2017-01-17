package com.technologx.andromanager.utils;

import com.technologx.andromanager.utils.color.ColorPreference;
import com.technologx.andromanager.utils.theme.AppTheme;
import com.technologx.andromanager.utils.theme.AppThemeManagerInterface;

/**
 * Created by Rémi Piotaix <remi.piotaix@gmail.com> on 2016-10-17.
 */
public interface UtilitiesProviderInterface {
    Futils getFutils();

    ColorPreference getColorPreference();

    AppTheme getAppTheme();

    AppThemeManagerInterface getThemeManager();
}
