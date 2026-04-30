package com.reveny.virtualinject.model;

public class CloneConfig {
    private final String packageName;
    private String soFilePath; // null = no library

    public CloneConfig(String packageName, String soFilePath) {
        this.packageName = packageName;
        this.soFilePath = soFilePath;
    }

    public String getPackageName() { return packageName; }

    public String getSoFilePath() { return soFilePath; }

    public void setSoFilePath(String soFilePath) { this.soFilePath = soFilePath; }

    public boolean hasSo() { return soFilePath != null && !soFilePath.isEmpty(); }
}
