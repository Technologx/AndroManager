package com.technologx.andromanager.utils.theme;

public interface AppThemeManagerInterface {
    AppTheme getAppTheme();

    AppThemeManagerInterface setAppTheme(AppTheme appTheme);

    AppThemeManagerInterface save();
}
