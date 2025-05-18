package com.example.routerkonfiguralo;

import java.util.ArrayList;
import java.util.List;

public class Router {
    private String name;
    private String ipAddress;
    private String username;
    private String password;
    private String model;
    private String firmwareVersion;
    private List<String> connectedDevices;
    private boolean isOnline;
    private String id;

    public Router() {
        this.connectedDevices = new ArrayList<>();
        this.isOnline = false;
    }

    public Router(String name, String ipAddress, String username, String password, String model) {
        this();
        this.name = name;
        this.ipAddress = ipAddress;
        this.username = username;
        this.password = password;
        this.model = model;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public List<String> getConnectedDevices() {
        return connectedDevices;
    }

    public void addConnectedDevice(String device) {
        if (!connectedDevices.contains(device)) {
            connectedDevices.add(device);
        }
    }

    public void removeConnectedDevice(String device) {
        connectedDevices.remove(device);
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Router{" +
                "name='" + name + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", model='" + model + '\'' +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", isOnline=" + isOnline +
                '}';
    }
} 